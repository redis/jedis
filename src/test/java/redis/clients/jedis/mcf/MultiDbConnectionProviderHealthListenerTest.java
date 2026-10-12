package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockConstruction;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Endpoint;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiDbConfig;
import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.util.ReflectionTestUtil;

/**
 * Removing a database must unregister the provider's health status listener for that endpoint;
 * otherwise adding the endpoint back leaves two listeners and every health event is handled twice.
 */
public class MultiDbConnectionProviderHealthListenerTest {

  private static final HostAndPort ENDPOINT1 = new HostAndPort("localhost", 6879);
  private static final HostAndPort ENDPOINT2 = new HostAndPort("localhost", 6880);

  private final JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().build();

  private DatabaseConfig databaseConfig(HostAndPort endpoint, float weight) {
    return DatabaseConfig.builder(endpoint, clientConfig).weight(weight)
        .healthCheckStrategy(new TestHealthCheckStrategy(e -> HealthStatus.HEALTHY)).build();
  }

  private static MockedConstruction<TrackingConnectionPool> mockConnectionPool() {
    Connection connection = mock(Connection.class);
    return mockConstruction(TrackingConnectionPool.class, (pool, context) -> {
      when(pool.getResource()).thenReturn(connection);
      doNothing().when(pool).close();
    });
  }

  private static List<?> listenersOf(MultiDbConnectionProvider provider, Endpoint endpoint) {
    HealthStatusManager manager = ReflectionTestUtil.getField(provider, "healthStatusManager");
    Map<Endpoint, List<?>> listeners = ReflectionTestUtil.getField(manager, "endpointListeners");
    List<?> forEndpoint = listeners.get(endpoint);
    return forEndpoint == null ? Collections.emptyList() : forEndpoint;
  }

  @Test
  public void removeUnregistersHealthStatusListener() {
    try (MockedConstruction<TrackingConnectionPool> mockedPool = mockConnectionPool()) {
      MultiDbConfig config = MultiDbConfig.builder(
        new DatabaseConfig[] { databaseConfig(ENDPOINT1, 1.0f), databaseConfig(ENDPOINT2, 0.5f) })
          .build();

      try (MultiDbConnectionProvider provider = new MultiDbConnectionProvider(config)) {
        assertEquals(1, listenersOf(provider, ENDPOINT2).size());

        provider.remove(ENDPOINT2);
        assertEquals(0, listenersOf(provider, ENDPOINT2).size(),
          "removed database must not stay registered");

        provider.add(databaseConfig(ENDPOINT2, 0.5f));
        assertEquals(1, listenersOf(provider, ENDPOINT2).size(),
          "re-added database registers exactly once");
      }
    }
  }
}
