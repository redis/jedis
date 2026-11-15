package redis.clients.jedis.providers;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisClusterOperationException;

class ClusterConnectionProviderTest {

  private static ClusterConnectionProvider providerWithRealMethods() {
    // Instantiate without running the constructor: no live cluster is needed to
    // exercise the keyless-command guards, which throw before touching the cache.
    return mock(ClusterConnectionProvider.class, CALLS_REAL_METHODS);
  }

  @Test
  void getReplicaConnectionRejectsKeylessCommand() {
    ClusterConnectionProvider provider = providerWithRealMethods();
    CommandArguments keyless = new CommandArguments(Protocol.Command.PING);
    assertThrows(JedisClusterOperationException.class,
      () -> provider.getReplicaConnection(keyless));
  }

  @Test
  void getConnectionRejectsKeylessCommand() {
    ClusterConnectionProvider provider = providerWithRealMethods();
    CommandArguments keyless = new CommandArguments(Protocol.Command.PING);
    assertThrows(JedisClusterOperationException.class, () -> provider.getConnection(keyless));
  }

  @Test
  void getReplicaConnectionRoutesKeyedCommandToReplicaConnection() {
    ClusterConnectionProvider provider = providerWithRealMethods();
    Connection connection = mock(Connection.class);
    doReturn(connection).when(provider).getReplicaConnectionFromSlot(anyInt());
    CommandArguments keyed = new CommandArguments(Protocol.Command.GET).key("key");
    assertSame(connection, provider.getReplicaConnection(keyed));
  }

  @Test
  void getConnectionRoutesKeyedCommandToSlotConnection() {
    ClusterConnectionProvider provider = providerWithRealMethods();
    Connection connection = mock(Connection.class);
    doReturn(connection).when(provider).getConnectionFromSlot(anyInt());
    CommandArguments keyed = new CommandArguments(Protocol.Command.GET).key("key");
    assertSame(connection, provider.getConnection(keyed));
  }
}
