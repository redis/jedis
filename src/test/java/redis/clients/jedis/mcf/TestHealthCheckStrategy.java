package redis.clients.jedis.mcf;

import java.util.concurrent.Callable;

import redis.clients.jedis.Endpoint;
import redis.clients.jedis.exceptions.JedisException;

public class TestHealthCheckStrategy implements HealthCheckStrategy {

  private int interval;
  private int timeout;
  private int retries;
  private int delay;
  private Callable<HealthStatus> healthCheck;

  public TestHealthCheckStrategy(int interval, int timeout, int retries, int delay,
      Callable<HealthStatus> healthCheck) {
    this.interval = interval;
    this.timeout = timeout;
    this.retries = retries;
    this.delay = delay;
    this.healthCheck = healthCheck;
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
  public int getNumberOfRetries() {
    return retries;
  }

  @Override
  public int getDelayInBetweenRetries() {
    return delay;
  }

  @Override
  public HealthStatus doHealthCheck(Endpoint endpoint) {
    try {
      return healthCheck.call();
    } catch (JedisException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
};