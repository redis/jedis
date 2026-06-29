package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.MultiDbClient;
import redis.clients.jedis.MultiDbConfig;
import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.Response;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.ClientTestUtil;

/**
 * Integration tests for {@link MultiDbTransaction} against a running Redis standalone instance.
 * Verifies MULTI/EXEC/DISCARD/WATCH semantics and that pool connections are returned to the pool
 * after {@link MultiDbTransaction#exec()} / {@link MultiDbTransaction#discard()} /
 * {@link MultiDbTransaction#close()}.
 */
@Tag("integration")
public class MultiDbTransactionIT {

  private static EndpointConfig endpoint;
  private MultiDbClient client;

  @BeforeAll
  public static void prepareEndpoints() {
    endpoint = Endpoints.getRedisEndpoint("standalone0");
  }

  @BeforeEach
  public void setUp() {
    DatabaseConfig db = DatabaseConfig
        .builder(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build()).weight(1.0f)
        .build();
    client = MultiDbClient.builder()
        .multiDbConfig(new MultiDbConfig.Builder(new DatabaseConfig[] { db }).build()).build();
    client.flushAll();
  }

  @AfterEach
  public void tearDown() {
    if (client != null) {
      try {
        client.flushAll();
      } catch (Exception ignore) {
      }
      client.close();
    }
  }

  private ConnectionPool activePool() {
    MultiDbConnectionProvider p = ClientTestUtil.getConnectionProvider(client);
    return p.getDatabase().getConnectionPool();
  }

  @Test
  public void exec_runsQueuedCommands_andReturnsResults() {
    Response<String> setResp;
    Response<String> getResp;
    List<Object> formatted;

    try (MultiDbTransaction tx = client.multi()) {
      setResp = tx.set("a", "1");
      getResp = tx.get("a");
      formatted = tx.exec();
    }

    assertNotNull(formatted);
    assertEquals(2, formatted.size());
    assertEquals("OK", formatted.get(0));
    assertEquals("1", formatted.get(1));
    assertEquals("OK", setResp.get());
    assertEquals("1", getResp.get());
    assertEquals("1", client.get("a"));
  }

  @Test
  public void discard_rollsBackQueuedCommands() {
    try (MultiDbTransaction tx = client.multi()) {
      tx.set("k", "v");
      assertEquals("OK", tx.discard());
    }
    assertNull(client.get("k"));
  }

  @Test
  public void execWithoutMulti_throws() {
    try (MultiDbTransaction tx = client.transaction(false)) {
      assertThrows(IllegalStateException.class, tx::exec);
    }
  }

  @Test
  public void closeInMulti_callsDiscard_andReleasesConnection() {
    try (MultiDbTransaction tx = client.multi()) {
      tx.set("k", "v");
    }
    // discard happened during close; key should not be set
    assertNull(client.get("k"));
    assertEquals(0, activePool().getNumActive());
  }

  @Test
  public void exec_releasesConnectionBackToPool() {
    ConnectionPool pool = activePool();

    try (MultiDbTransaction tx = client.multi()) {
      tx.set("a", "1");
      tx.get("a");
      tx.exec();
    }

    assertEquals(0, pool.getNumActive(), "no active connections after EXEC/close");
  }

  @Test
  public void discard_releasesConnectionBackToPool() {
    ConnectionPool pool = activePool();

    try (MultiDbTransaction tx = client.multi()) {
      tx.set("a", "1");
      tx.discard();
    }

    assertEquals(0, pool.getNumActive(), "no active connections after DISCARD/close");
  }

  @Test
  public void watch_then_multi_then_exec() {
    try (MultiDbTransaction tx = client.transaction(false)) {
      assertEquals("OK", tx.watch("a"));
      tx.multi();
      tx.set("a", "1");
      List<Object> res = tx.exec();
      assertNotNull(res);
      assertEquals("OK", res.get(0));
    }
    assertEquals("1", client.get("a"));
    assertEquals(0, activePool().getNumActive());
  }

  @Test
  public void exec_queuedCommandRejectedByServer_surfacesServerError() {
    ConnectionPool pool = activePool();

    try (MultiDbTransaction tx = client.multi()) {
      // GET with no arguments: Redis rejects it at queue time with an arity error instead of
      // replying QUEUED. Connection.getMany stores that error reply inline as a JedisDataException.
      tx.appendCommand(
        new CommandObject<>(new CommandArguments(Protocol.Command.GET), BuilderFactory.RAW_OBJECT));

      // the original server error must propagate, not a ClassCastException
      JedisDataException ex = assertThrows(JedisDataException.class, tx::exec);
      assertTrue(ex.getMessage().contains("EXECABORT"),
        "expected arity error, got: " + ex.getMessage());

      assertTrue(ex.getSuppressed().length > 0);
      assertTrue(ex.getSuppressed()[0].getMessage().contains("wrong number of arguments"));
    }

    // connection is released back to the pool on the error path
    assertEquals(0, pool.getNumActive(), "no active connections after a failed exec");
  }

  @Test
  public void failedExec_doesNotPoisonPooledConnection() {
    // A queue-time rejection aborts exec() before EXEC/DISCARD is sent, leaving the server
    // mid-MULTI on the borrowed connection.
    try (MultiDbTransaction tx = client.multi()) {
      tx.appendCommand(
        new CommandObject<>(new CommandArguments(Protocol.Command.GET), BuilderFactory.RAW_OBJECT));
      assertThrows(JedisDataException.class, tx::exec);
    }

    // The connection that handled the failed transaction is back in the pool. A subsequent command
    // must execute normally; if the connection were returned mid-MULTI, SET would reply QUEUED.
    assertEquals("OK", client.set("x", "y"));
    assertEquals("y", client.get("x"));
  }
}
