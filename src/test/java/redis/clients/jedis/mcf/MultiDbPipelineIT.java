package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.MultiDbClient;
import redis.clients.jedis.MultiDbConfig;
import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.Response;
import redis.clients.jedis.util.ClientTestUtil;

/**
 * Integration tests for {@link MultiDbPipeline} against a running Redis standalone instance.
 * Verifies command execution semantics and that pool connections are returned to the pool after
 * {@link MultiDbPipeline#sync()} / {@link MultiDbPipeline#close()}.
 */
@Tag("integration")
public class MultiDbPipelineIT {

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
  public void pipeline_executesQueuedCommands_andReturnsExpectedValues() {
    int count = 10;
    for (int i = 0; i < count; i++) {
      client.set("k" + i, "v" + i);
    }

    List<Response<String>> getResponses = new ArrayList<>(count);
    List<Response<List<String>>> lrangeResponses = new ArrayList<>(count);

    try (MultiDbPipeline p = client.pipelined()) {
      for (int i = 0; i < count; i++) {
        getResponses.add(p.get("k" + i));
      }
      for (int i = 0; i < count; i++) {
        p.rpush("l" + i, "a" + i, "b" + i);
        lrangeResponses.add(p.lrange("l" + i, 0, -1));
      }
      p.sync();
    }

    for (int i = 0; i < count; i++) {
      assertEquals("v" + i, getResponses.get(i).get());
      assertEquals(Arrays.asList("a" + i, "b" + i), lrangeResponses.get(i).get());
    }
  }

  @Test
  public void pipeline_returnsConnectionToPool_afterSync() {
    ConnectionPool pool = activePool();
    int beforeIdle = pool.getNumIdle();

    try (MultiDbPipeline p = client.pipelined()) {
      p.set("x", "y");
      p.get("x");
      p.sync();
    }

    assertEquals(0, pool.getNumActive(), "no active connections after pipeline sync/close");
    assertEquals(Math.max(beforeIdle, 1), pool.getNumIdle(), "connection returned to idle pool");
  }

  @Test
  public void pipeline_closeOnEmptyPipeline_doesNotBorrowConnection() {
    ConnectionPool pool = activePool();
    int idleBefore = pool.getNumIdle();
    int activeBefore = pool.getNumActive();

    try (MultiDbPipeline p = client.pipelined()) {
      assertEquals(activeBefore, pool.getNumActive());
    }

    assertEquals(activeBefore, pool.getNumActive());
    assertEquals(idleBefore, pool.getNumIdle());
  }

  @Test
  public void pipeline_responsesAreNotResolvedBeforeSync() {
    try (MultiDbPipeline p = client.pipelined()) {
      Response<String> r = p.set("a", "b");
      // before sync, .get() on the response should throw IllegalStateException
      IllegalStateException ex = assertThrows(IllegalStateException.class, r::get);
      assertTrue(ex.getMessage()
          .contains("Please close pipeline or multi block before calling this method"));
      p.sync();
      assertEquals("OK", r.get());
    }
  }

  @Test
  public void pipeline_multipleSyncs_onSamePipeline_areSafe() {
    try (MultiDbPipeline p = client.pipelined()) {
      Response<String> r1 = p.set("a", "1");
      p.sync();
      assertEquals("OK", r1.get());

      Response<String> r2 = p.set("b", "2");
      p.sync();
      assertEquals("OK", r2.get());

      assertNotNull(client.get("a"));
      assertNotNull(client.get("b"));
    }

    assertEquals(0, activePool().getNumActive());
  }
}
