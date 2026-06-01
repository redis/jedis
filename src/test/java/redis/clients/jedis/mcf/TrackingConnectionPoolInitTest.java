package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;

public class TrackingConnectionPoolInitTest {

  /**
   * Verifies that a {@link Connection} borrowed from a {@code TrackingConnectionPool} is
   * initialized exactly once.
   */
  @Test
  public void pooledConnectionInitializedExactlyOnce() {
    DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();
    HostAndPort hostAndPort = new HostAndPort("localhost", 6379);

    // Mock the constructor so the borrow runs without opening a socket.
    try (MockedConstruction<Connection> mocked = mockConstruction(Connection.class)) {
      try (TrackingConnectionPool pool = TrackingConnectionPool.builder().hostAndPort(hostAndPort)
          .clientConfig(config).build()) {

        pool.getResource();

        assertEquals(1, mocked.constructed().size());
        Connection pooledConnection = mocked.constructed().get(0);
        verify(pooledConnection, times(1)).initializeFromClientConfig();
      }
    }
  }
}