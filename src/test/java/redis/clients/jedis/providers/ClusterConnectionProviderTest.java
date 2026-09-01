package redis.clients.jedis.providers;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.JedisClusterCRC16;

public class ClusterConnectionProviderTest {

  @Test
  public void keylessCommandGetsAConnectionFromAnyNode() {
    ClusterConnectionProvider provider = mock(ClusterConnectionProvider.class, CALLS_REAL_METHODS);
    Connection anyNode = mock(Connection.class);
    doReturn(anyNode).when(provider).getConnection();

    assertSame(anyNode, provider.getConnection(new CommandArguments(Protocol.Command.PING)));
  }

  @Test
  public void keylessCommandGetsAReplicaConnectionFromAReplicaOfTheTopology() {
    ClusterConnectionProvider provider = mock(ClusterConnectionProvider.class, CALLS_REAL_METHODS);
    Connection replicaConnection = mock(Connection.class);
    ConnectionPool replicaPool = mock(ConnectionPool.class);
    doReturn(replicaConnection).when(replicaPool).getResource();
    doReturn(Collections.singletonMap("127.0.0.1:7001", replicaPool)).when(provider)
        .getReplicaNodes();

    assertSame(replicaConnection,
      provider.getReplicaConnection(new CommandArguments(Protocol.Command.PING)));
  }

  @Test
  public void keylessReplicaCommandFallsBackToAPrimaryWithoutRenewingTheSlotCache() {
    ClusterConnectionProvider provider = mock(ClusterConnectionProvider.class, CALLS_REAL_METHODS);
    Connection anyNode = mock(Connection.class);
    doReturn(Collections.<String, ConnectionPool> emptyMap()).when(provider).getReplicaNodes();
    doReturn(anyNode).when(provider).getConnection();

    assertSame(anyNode, provider.getReplicaConnection(new CommandArguments(Protocol.Command.PING)));
    // a cluster without replicas would otherwise renew the whole topology on every keyless call
    verify(provider, never()).renewSlotCache();
  }

  @Test
  public void keyedCommandGetsTheConnectionForItsSlot() {
    ClusterConnectionProvider provider = mock(ClusterConnectionProvider.class, CALLS_REAL_METHODS);
    Connection slotOwner = mock(Connection.class);
    doReturn(slotOwner).when(provider).getConnectionFromSlot(JedisClusterCRC16.getSlot("foo"));

    CommandArguments args = new CommandArguments(Protocol.Command.GET).key("foo");

    assertSame(slotOwner, provider.getConnection(args));
  }

  @Test
  public void keyedCommandGetsTheReplicaConnectionForItsSlot() {
    ClusterConnectionProvider provider = mock(ClusterConnectionProvider.class, CALLS_REAL_METHODS);
    Connection slotReplica = mock(Connection.class);
    doReturn(slotReplica).when(provider)
        .getReplicaConnectionFromSlot(JedisClusterCRC16.getSlot("foo"));

    CommandArguments args = new CommandArguments(Protocol.Command.GET).key("foo");

    assertSame(slotReplica, provider.getReplicaConnection(args));
  }
}
