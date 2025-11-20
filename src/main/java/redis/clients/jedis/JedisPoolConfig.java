package redis.clients.jedis;

import java.time.Duration;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * Configuration class for {@link JedisPool} connection pooling.
 *
 * @deprecated JedisPoolConfig is used with the deprecated {@link JedisPool} and {@link JedisSentinelPool} classes.
 *             Use {@link ConnectionPoolConfig} instead, which is designed for the modern {@link RedisClient}
 *             and {@link RedisSentinelClient} classes. ConnectionPoolConfig provides the same pooling configuration
 *             options with better integration into the new client architecture.
 */
@Deprecated
public class JedisPoolConfig extends GenericObjectPoolConfig<Jedis> {

  public JedisPoolConfig() {
    // defaults to make your life with connection pool easier :)
    setTestWhileIdle(true);
    setMinEvictableIdleTime(Duration.ofMillis(60000));
    setTimeBetweenEvictionRuns(Duration.ofMillis(30000));
    setNumTestsPerEvictionRun(-1);
  }
}
