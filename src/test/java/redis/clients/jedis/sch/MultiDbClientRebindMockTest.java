package redis.clients.jedis.sch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.MultiDbClient;
import redis.clients.jedis.MultiDbConfig;
import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.mcf.MultiDbConnectionProvider;
import redis.clients.jedis.util.ReflectionTestUtil;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * {@link MultiDbClient} : Verify SCH rebind behavior for a single maintenance-enabled database.
 */
public class MultiDbClientRebindMockTest extends AbstractRebindBehaviorTest {

  @Override
  protected UnifiedJedis createClient(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      MaintenanceNotificationsConfig maintConfig, GenericObjectPoolConfig<Connection> poolConfig) {
    DatabaseConfig database = DatabaseConfig.builder(hostAndPort, clientConfig).weight(1.0f)
        .healthCheckEnabled(false).connectionPoolConfig(poolConfig)
        .maintenanceNotificationsConfig(maintConfig).build();
    MultiDbConfig multiDbConfig = MultiDbConfig.builder().database(database).build();
    return MultiDbClient.builder().multiDbConfig(multiDbConfig).build();
  }

  @Override
  protected ConnectionPool poolOf(UnifiedJedis client) {
    MultiDbConnectionProvider provider = ReflectionTestUtil.getField(client, "provider");
    return provider.getDatabase().getConnectionPool();
  }

  @Test
  public void eachDatabaseHandlesItsOwnMovingAfterFailoverRebuild() throws IOException {
    TcpMockServer dbBServer = new TcpMockServer();
    dbBServer.start();
    TcpMockServer dbBMovingTarget = new TcpMockServer();
    dbBMovingTarget.start();
    HostAndPort dbBAddress = new HostAndPort("127.0.0.1", dbBServer.getPort());
    HostAndPort dbBTargetAddress = new HostAndPort("127.0.0.1", dbBMovingTarget.getPort());
    try {
      ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
      poolConfig.setMaxTotal(1);

      // Both databases maintenance-enabled. A (server1) is the higher-weight initial active.
      MultiDbConfig multiDbConfig = MultiDbConfig.builder().fastFailover(true)
          .database(maintenanceDatabase(server1Address, poolConfig, 100.0f))
          .database(maintenanceDatabase(dbBAddress, poolConfig, 1.0f)).build();

      try (MultiDbClient client = MultiDbClient.builder().multiDbConfig(multiDbConfig).build()) {
        // A (active) handles its own MOVING on its original pool.
        assertActiveDatabaseRedirectsOnMoving(client, mockServer1, server2Address, mockServer2);

        // Transition to B: B (active) handles its own MOVING on its original pool.
        client.setActiveDatabase(dbBAddress);
        assertActiveDatabaseRedirectsOnMoving(client, dbBServer, dbBTargetAddress, dbBMovingTarget);

        // Transition back to A: A's pool is now rebuilt via from() — it must STILL handle its own
        // MOVING
        client.setActiveDatabase(server1Address);
        assertActiveDatabaseRedirectsOnMoving(client, mockServer1, server2Address, mockServer2);
      }
    } finally {
      dbBServer.stop();
      dbBMovingTarget.stop();
    }
  }

  private DatabaseConfig maintenanceDatabase(HostAndPort address,
      GenericObjectPoolConfig<Connection> poolConfig, float weight) {
    return DatabaseConfig.builder(address, clientConfig).weight(weight).healthCheckEnabled(false)
        .connectionPoolConfig(poolConfig).maintenanceNotificationsConfig(maintConfig).build();
  }

  /** Sends MOVING on the active database's node and asserts its connections move to the target. */
  private void assertActiveDatabaseRedirectsOnMoving(MultiDbClient client, TcpMockServer node,
      HostAndPort movingTarget, TcpMockServer movingTargetServer) {
    assertEquals("PONG", client.ping());
    node.sendPushMessageToAll("MOVING", 30, 60, movingTarget.toString());
    assertEquals("PONG", client.ping());
    assertEquals(0, node.getConnectedClientCount(),
      "active pool dropped maintenance: connections still dial the original endpoint");
    assertEquals(1, movingTargetServer.getConnectedClientCount());
  }
}
