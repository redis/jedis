package redis.clients.jedis.examples;

import java.time.Duration;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.RedisClient;

/**
 * RedisClient includes retry logic by default (5 attempts, 10 seconds total).
 * The retry parameters can be customized via the builder's {@code maxAttempts} and
 * {@code maxTotalRetriesDuration} methods.
 * Note: For Redis Cluster mode, use {@link redis.clients.jedis.RedisClusterClient} which handles
 * cluster-specific concerns (MOVED/ASK redirections, slot cache renewal) in addition to retries.
 */
public class RetryableCommandExecution {

  public static void main(String[] args) {

    GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();

    // RedisClient retries on connection failures by default.
    // Custom retry parameters can be set via maxAttempts() and maxTotalRetriesDuration().
    RedisClient jedis = RedisClient.builder()
        .hostAndPort("127.0.0.1", 6379)
        .clientConfig(DefaultJedisClientConfig.builder()
            .user("myuser").password("mypassword").build())
        .poolConfig(poolConfig)
        .maxAttempts(5)
        .maxTotalRetriesDuration(Duration.ofSeconds(2))
        .build();

    jedis.set("foo", "bar");
    jedis.get("foo");

    jedis.close();
  }
}
