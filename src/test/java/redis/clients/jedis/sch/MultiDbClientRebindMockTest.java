package redis.clients.jedis.sch;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.Connection;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.MultiDbClient;
import redis.clients.jedis.MultiDbConfig;
import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.mcf.MultiDbConnectionProvider;
import redis.clients.jedis.util.Pool;
import redis.clients.jedis.util.ReflectionTestUtil;

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
  protected Pool<Connection> poolOf(UnifiedJedis client) {
    MultiDbConnectionProvider provider = ReflectionTestUtil.getField(client, "provider");
    return provider.getDatabase().getConnectionPool();
  }
}
