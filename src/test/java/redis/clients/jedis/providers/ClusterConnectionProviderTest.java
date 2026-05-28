package redis.clients.jedis.providers;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClusterInfoCache;
import redis.clients.jedis.exceptions.JedisConnectionException;

@Tag("unit")
class ClusterConnectionProviderTest {

  private static final HostAndPort START_NODE = new HostAndPort("127.0.0.1", 7000);
  private static final int SLOT = 42;

  @Test
  void getConnectionFromSlotRenewsSlotCacheAndRetriesWhenSlotPoolConnectionFails() {
    ConnectionPool stalePool = Mockito.mock(ConnectionPool.class);
    ConnectionPool refreshedPool = Mockito.mock(ConnectionPool.class);
    Connection refreshedConnection = Mockito.mock(Connection.class);

    try (
        MockedConstruction<JedisClusterInfoCache> mockedCaches = Mockito
            .mockConstruction(JedisClusterInfoCache.class);
        MockedConstruction<Connection> ignored = Mockito.mockConstruction(Connection.class)) {
      ClusterConnectionProvider provider = new ClusterConnectionProvider(
          Collections.singleton(START_NODE), DefaultJedisClientConfig.builder().build());
      JedisClusterInfoCache cache = mockedCaches.constructed().get(0);

      when(cache.getSlotPool(SLOT)).thenReturn(stalePool, refreshedPool);
      when(stalePool.getResource()).thenThrow(new JedisConnectionException("stale slot node"));
      when(refreshedPool.getResource()).thenReturn(refreshedConnection);

      Connection connection = provider.getConnectionFromSlot(SLOT);

      assertSame(refreshedConnection, connection);
      verify(cache, times(2)).getSlotPool(SLOT);
      verify(cache).renewClusterSlots(isNull());
      verify(stalePool).getResource();
      verify(refreshedPool).getResource();
    }
  }
}
