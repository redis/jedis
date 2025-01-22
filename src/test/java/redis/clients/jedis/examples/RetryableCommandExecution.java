package redis.clients.jedis.examples;

import java.time.Duration;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.providers.PooledConnectionProvider;

/**
 * It is possible to retry command executions in case of connection failures in UnifiedJedis class.
 *
 * The retry-ability comes through RetryableCommandExecutor class. It is also possible to directly provide
 * RetryableCommandExecutor as a parameter.
 *
 * Note: RetryableCommandExecutor should not be considered for
 * <a href="https://redis.io/docs/reference/cluster-spec/">Open Source Redis Cluster mode</a> because it requires to
 * handle more than connection failures. These are done in ClusterCommandExecutor.
 */
public class RetryableCommandExecution {

  public static void main(String[] args) {

    // Connection and pool parameters
    HostAndPort hostAndPort = new HostAndPort("127.0.0.1", 6379);
    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().user("myuser").password("mypassword").build();
    GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();

    PooledConnectionProvider provider = new PooledConnectionProvider(hostAndPort, clientConfig, poolConfig);

    // Retry parameters
    int maxAttempts = 5;
    Duration maxTotalRetriesDuration = Duration.ofSeconds(2);

    UnifiedJedis jedis = new UnifiedJedis(provider, maxAttempts, maxTotalRetriesDuration);

    jedis.set("foo", "bar");
    jedis.get("foo");

    jedis.close();
  }
}
