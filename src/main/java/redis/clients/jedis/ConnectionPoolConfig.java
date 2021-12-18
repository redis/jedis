package redis.clients.jedis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;

public class ConnectionPoolConfig extends GenericObjectPoolConfig<Connection> {

  public ConnectionPoolConfig() {
    // defaults to make your life with connection pool easier :)
    setTestWhileIdle(true);
    setMinEvictableIdleTime(Duration.ofMillis(60000));
    setTimeBetweenEvictionRuns(Duration.ofMillis(30000));
    setNumTestsPerEvictionRun(-1);
  }
}
