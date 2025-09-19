package redis.clients.jedis.mcf;

import java.util.function.Function;

import redis.clients.jedis.Endpoint;

public class TestHealthCheckStrategy implements HealthCheckStrategy {

  private int interval;
  private int timeout;
  private int probes;
  private int delay;
  private Function<Endpoint, HealthStatus> healthCheck;
  private ProbePolicy policy;

  public TestHealthCheckStrategy(int interval, int timeout, int probes, ProbePolicy policy,
      int delay, Function<Endpoint, HealthStatus> healthCheck) {
    this.interval = interval;
    this.timeout = timeout;
    this.probes = probes;
    this.delay = delay;
    this.healthCheck = healthCheck;
    this.policy = policy;
  }

  public TestHealthCheckStrategy(HealthCheckStrategy.Config config,
      Function<Endpoint, HealthStatus> healthCheck) {
    this(config.getInterval(), config.getTimeout(), config.getNumProbes(), config.getPolicy(),
        config.getDelayInBetweenProbes(), healthCheck);
  }

  public TestHealthCheckStrategy(Function<Endpoint, HealthStatus> healthCheck) {
    this(HealthCheckStrategy.Config.create(), healthCheck);
  }

  @Override
  public int getInterval() {
    return interval;
  }

  @Override
  public int getTimeout() {
    return timeout;
  }

  @Override
  public int getNumProbes() {
    return probes;
  }

  @Override
  public ProbePolicy getPolicy() {
    return policy;
  }

  @Override
  public int getDelayInBetweenProbes() {
    return delay;
  }

  @Override
  public HealthStatus doHealthCheck(Endpoint endpoint) {
    return healthCheck.apply(endpoint);
  }

};