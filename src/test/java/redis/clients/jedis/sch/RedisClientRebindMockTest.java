package redis.clients.jedis.sch;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.Connection;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.UnifiedJedis;

/**
 * Standalone {@link RedisClient} : Verify SCH rebind behavior for standalone client.
 */
public class RedisClientRebindMockTest extends AbstractRebindBehaviorTest {

  @Override
  protected UnifiedJedis createClient(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      MaintenanceNotificationsConfig maintConfig, GenericObjectPoolConfig<Connection> poolConfig) {
    return RedisClient.builder().poolConfig(poolConfig).clientConfig(clientConfig)
        .maintenanceNotifications(maintConfig).hostAndPort(hostAndPort).build();
  }

  @Override
  protected ConnectionPool poolOf(UnifiedJedis client) {
    return (ConnectionPool) ((RedisClient) client).getPool();
  }
}
