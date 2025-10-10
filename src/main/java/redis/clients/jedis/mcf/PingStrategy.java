package redis.clients.jedis.mcf;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.Connection;
import redis.clients.jedis.Endpoint;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.MultiDbConfig.StrategySupplier;

public class PingStrategy implements HealthCheckStrategy {
  private static final int MAX_HEALTH_CHECK_POOL_SIZE = 2;

  private final UnifiedJedis jedis;
  private final HealthCheckStrategy.Config config;

  public PingStrategy(HostAndPort hostAndPort, JedisClientConfig jedisClientConfig) {
    this(hostAndPort, jedisClientConfig, HealthCheckStrategy.Config.create());
  }

  public PingStrategy(HostAndPort hostAndPort, JedisClientConfig jedisClientConfig,
      HealthCheckStrategy.Config config) {
    GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(MAX_HEALTH_CHECK_POOL_SIZE);
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
    return "PONG".equals(jedis.ping()) ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
  }

  @Override
  public void close() {
    jedis.close();
  }

  public static final StrategySupplier DEFAULT = PingStrategy::new;

}
