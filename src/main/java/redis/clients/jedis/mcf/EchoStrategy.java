package redis.clients.jedis.mcf;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.Connection;
import redis.clients.jedis.Endpoint;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.MultiClusterClientConfig.StrategySupplier;

public class EchoStrategy implements HealthCheckStrategy {

  private final UnifiedJedis jedis;
  private final HealthCheckStrategy.Config config;

  public EchoStrategy(HostAndPort hostAndPort, JedisClientConfig jedisClientConfig) {
    this(hostAndPort, jedisClientConfig, HealthCheckStrategy.Config.builder().build());
  }

  public EchoStrategy(HostAndPort hostAndPort, JedisClientConfig jedisClientConfig,
      HealthCheckStrategy.Config config) {
    GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(2);
    this.jedis = new JedisPooled(hostAndPort, jedisClientConfig, poolConfig);
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
  public int getNumProbes() {
    return config.getNumProbes();
  }

  @Override
  public ProbingPolicy getPolicy() {
    return config.getPolicy();
  }

  @Override
  public int getDelayInBetweenProbes() {
    return config.getDelayInBetweenProbes();
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
