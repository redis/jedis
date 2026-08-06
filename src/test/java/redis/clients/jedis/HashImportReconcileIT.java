package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.redis.test.annotations.EnabledOnCommand;
import redis.clients.jedis.util.EnabledOnCommandCondition;

/**
 * Queued {@code HIMPORT DISCARD}s reconcile right before the connection's next command — on every
 * path, including the ones without pool activation: a direct (unpooled) {@link Jedis} and the
 * legacy {@link JedisPool}.
 */
@Tag("integration")
public class HashImportReconcileIT {

  private static EndpointConfig endpoint;

  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(
      () -> Endpoints.getRedisEndpoint("standalone0"));

  @BeforeAll
  public static void setUp() {
    endpoint = Endpoints.getRedisEndpoint("standalone0");
  }

  @Test
  @EnabledOnCommand("HIMPORT")
  public void directJedisDiscardsBeforeNextCommand() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().build())) {
      HashImport fs = HashImport.of("f");
      assertEquals("OK", jedis.himportSet("himport:reconcile:direct", fs, "v"));
      Connection connection = jedis.getConnection();
      assertTrue(connection.himportState().isPrepared(fs.name()));

      fs.close();
      // deferred: nothing touches the connection until its next command
      assertTrue(connection.himportState().isPrepared(fs.name()));

      assertEquals("v", jedis.hget("himport:reconcile:direct", "f"));
      assertFalse(connection.himportState().isPrepared(fs.name()));
    }
  }

  @Test
  @EnabledOnCommand("HIMPORT")
  public void batchesAllQueuedDiscardsBeforeOneCommand() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().build())) {
      HashImport fs1 = HashImport.of("a");
      HashImport fs2 = HashImport.of("b");
      HashImport fs3 = HashImport.of("c");
      assertEquals("OK", jedis.himportSet("himport:reconcile:batch:1", fs1, "1"));
      assertEquals("OK", jedis.himportSet("himport:reconcile:batch:2", fs2, "2"));
      assertEquals("OK", jedis.himportSet("himport:reconcile:batch:3", fs3, "3"));
      Connection connection = jedis.getConnection();

      fs1.close();
      fs2.close();
      fs3.close();

      // one command drains all three queued DISCARDs; its correct reply proves the packed
      // write/read left the socket in sync
      assertEquals("1", jedis.hget("himport:reconcile:batch:1", "a"));
      assertFalse(connection.himportState().isPrepared(fs1.name()));
      assertFalse(connection.himportState().isPrepared(fs2.name()));
      assertFalse(connection.himportState().isPrepared(fs3.name()));
    }
  }

  @Test
  @EnabledOnCommand("HIMPORT")
  public void legacyJedisPoolDiscardsBeforeNextCommand() {
    GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(1);
    try (JedisPool pool = new JedisPool(poolConfig, endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().build())) {
      HashImport fs = HashImport.of("f");
      Connection connection;
      try (Jedis borrowed = pool.getResource()) {
        assertEquals("OK", borrowed.himportSet("himport:reconcile:pooled", fs, "v"));
        connection = borrowed.getConnection();
        assertTrue(connection.himportState().isPrepared(fs.name()));
      }

      fs.close();

      try (Jedis borrowed = pool.getResource()) {
        // maxTotal=1: the same pooled Jedis, and so the same socket, is borrowed again
        assertSame(connection, borrowed.getConnection());
        assertEquals("v", borrowed.hget("himport:reconcile:pooled", "f"));
        assertFalse(connection.himportState().isPrepared(fs.name()));
      }
    }
  }
}