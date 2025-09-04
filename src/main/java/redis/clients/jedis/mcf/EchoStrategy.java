package redis.clients.jedis.mcf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Endpoint;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.MultiClusterClientConfig.StrategySupplier;

public class EchoStrategy implements HealthCheckStrategy {
  private static final Logger log = LoggerFactory.getLogger(EchoStrategy.class);

  private final UnifiedJedis jedis;
  private final HealthCheckStrategy.Config config;

  public EchoStrategy(HostAndPort hostAndPort, JedisClientConfig jedisClientConfig) {
    this(hostAndPort, jedisClientConfig, new HealthCheckStrategy.Config(1000, 1000, 3));
  }

  public EchoStrategy(HostAndPort hostAndPort, JedisClientConfig jedisClientConfig,
      HealthCheckStrategy.Config config) {
    this.jedis = new UnifiedJedis(hostAndPort, jedisClientConfig);
    this.config = config;
  }

  @Override
  public int getInterval() {
    return config.getInterval();
  }

  @Override
  public int getTimeout() {
    return config.getTimeout();
  }

  @Override
  public int getNumberOfRetries() {
    return config.getNumberOfRetries();
  }

  @Override
  public HealthStatus doHealthCheck(Endpoint endpoint) {
    return "HealthCheck".equals(jedis.echo("HealthCheck")) ? HealthStatus.HEALTHY
        : HealthStatus.UNHEALTHY;
  }

  @Override
  public void close() {
    jedis.close();
  }

  public static final StrategySupplier DEFAULT = EchoStrategy::new;

}
