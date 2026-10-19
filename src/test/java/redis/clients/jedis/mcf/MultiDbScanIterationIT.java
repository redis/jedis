package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.MultiDbClient;
import redis.clients.jedis.MultiDbConfig;
import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.util.ClientTestUtil;
import redis.clients.jedis.util.TestKeyRegistry;

/**
 * Integration tests for {@code scanIteration} on a {@link MultiDbClient}: the pooled connection is
 * returned to the pool once the iteration ends.
 */
public class MultiDbScanIterationIT {

  private static EndpointConfig endpoint;
  private MultiDbClient client;
  private TestKeyRegistry keys;

  @BeforeAll
  public static void prepareEndpoints() {
    endpoint = Endpoints.getRedisEndpoint("standalone0");
  }

  @BeforeEach
  public void setUp(TestInfo testInfo) {
    keys = TestKeyRegistry.create(testInfo);
    DatabaseConfig db = DatabaseConfig
        .builder(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build()).weight(1.0f)
        .build();
    client = MultiDbClient.builder()
        .multiDbConfig(new MultiDbConfig.Builder(new DatabaseConfig[] { db }).build()).build();
  }

  @AfterEach
  public void tearDown() {
    if (client != null) {
      try {
        keys.cleanup(client);
      } catch (Exception ignore) {
      }
      client.close();
    }
  }

  private ConnectionPool activePool() {
    MultiDbConnectionProvider p = ClientTestUtil.getConnectionProvider(client);
    return p.getDatabase().getConnectionPool();
  }

  /** Regression test for #4799: scanIteration must return its pooled connection. */
  @Test
  public void scanIteration_releasesConnection() {
    for (int i = 0; i < 50; i++) {
      client.set(keys.key("key" + i), "v");
    }
    ConnectionPool pool = activePool();

    List<String> found = new ArrayList<>(
        client.scanIteration(10, keys.name("*")).collect(new ArrayList<>()));

    assertEquals(50, found.size(), "iteration must run to completion");
    assertEquals(0, pool.getNumActive(), "iteration must not keep a pooled connection");
  }
}
