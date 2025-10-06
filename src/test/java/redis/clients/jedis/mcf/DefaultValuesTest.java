package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiDbConfig;

public class DefaultValuesTest {

  HostAndPort fakeEndpoint = new HostAndPort("fake", 6379);
  JedisClientConfig config = DefaultJedisClientConfig.builder().build();

  @Test
  void testDefaultValuesInConfig() {

    MultiDbConfig.DatabaseConfig clusterConfig = MultiDbConfig.DatabaseConfig
        .builder(fakeEndpoint, config).build();
    MultiDbConfig multiConfig = new MultiDbConfig.Builder(
        new MultiDbConfig.DatabaseConfig[] { clusterConfig }).build();

    // check for grace period
    assertEquals(60000, multiConfig.getGracePeriod());

    // check for cluster config
    assertEquals(clusterConfig, multiConfig.getDatabaseConfigs()[0]);

    // check healthchecks enabled
    assertNotNull(clusterConfig.getHealthCheckStrategySupplier());

    // check default healthcheck strategy is echo
    assertEquals(EchoStrategy.DEFAULT, clusterConfig.getHealthCheckStrategySupplier());

    // check number of probes
    assertEquals(3,
      clusterConfig.getHealthCheckStrategySupplier().get(fakeEndpoint, config).getNumProbes());

    assertEquals(500, clusterConfig.getHealthCheckStrategySupplier().get(fakeEndpoint, config)
        .getDelayInBetweenProbes());

    assertEquals(ProbingPolicy.BuiltIn.ALL_SUCCESS,
      clusterConfig.getHealthCheckStrategySupplier().get(fakeEndpoint, config).getPolicy());

    // check health check interval
    assertEquals(5000,
      clusterConfig.getHealthCheckStrategySupplier().get(fakeEndpoint, config).getInterval());

    // check lag aware tolerance
    LagAwareStrategy.Config lagAwareConfig = LagAwareStrategy.Config
        .builder(fakeEndpoint, config.getCredentialsProvider()).build();
    assertEquals(Duration.ofMillis(5000), lagAwareConfig.getAvailabilityLagTolerance());

    // TODO: check CB number of failures threshold -- 1000
    // assertEquals(1000, multiConfig.circuitBreakerMinNumOfFailures());

    // check CB failure rate threshold
    assertEquals(10, multiConfig.getCircuitBreakerFailureRateThreshold());

    // check CB sliding window size
    assertEquals(2, multiConfig.getCircuitBreakerSlidingWindowSize());

    // check failback check interval
    assertEquals(120000, multiConfig.getFailbackCheckInterval());

    // check failover max attempts before give up
    assertEquals(10, multiConfig.getMaxNumFailoverAttempts());

    // check delay between failover attempts
    assertEquals(12000, multiConfig.getDelayInBetweenFailoverAttempts());

  }
}
