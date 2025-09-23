package redis.clients.jedis.providers;

import org.awaitility.Durations;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Endpoint;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiClusterClientConfig;
import redis.clients.jedis.MultiClusterClientConfig.ClusterConfig;
import redis.clients.jedis.mcf.JedisFailoverException;
import redis.clients.jedis.mcf.SwitchReason;
import redis.clients.jedis.mcf.JedisFailoverException.JedisPermanentlyNotAvailableException;
import redis.clients.jedis.mcf.JedisFailoverException.JedisTemporarilyNotAvailableException;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * Tests for how getMaxNumFailoverAttempts and getDelayInBetweenFailoverAttempts impact
 * MultiClusterPooledConnectionProvider behaviour when no healthy clusters are available.
 */
public class MultiClusterFailoverAttemptsConfigTest {

  private HostAndPort endpoint0 = new HostAndPort("purposefully-incorrect", 0000);
  private HostAndPort endpoint1 = new HostAndPort("purposefully-incorrect", 0001);

  private MultiClusterPooledConnectionProvider provider;

  @BeforeEach
  void setUp() throws Exception {
    JedisClientConfig clientCfg = DefaultJedisClientConfig.builder().build();

    ClusterConfig[] clusterConfigs = new ClusterConfig[] {
        ClusterConfig.builder(endpoint0, clientCfg).weight(1.0f).healthCheckEnabled(false).build(),
        ClusterConfig.builder(endpoint1, clientCfg).weight(0.5f).healthCheckEnabled(false)
            .build() };

    MultiClusterClientConfig.Builder builder = new MultiClusterClientConfig.Builder(clusterConfigs);

    // Use small values by default for tests unless overridden per-test via reflection
    setBuilderFailoverConfig(builder, /* maxAttempts */ 10, /* delayMs */ 12000);

    provider = new MultiClusterPooledConnectionProvider(builder.build());

    // Disable both clusters to force handleNoHealthyCluster path
    provider.getCluster(endpoint0).setDisabled(true);
    provider.getCluster(endpoint1).setDisabled(true);
  }

  @AfterEach
  void tearDown() {
    if (provider != null) provider.close();
  }

  @Test
  void delayBetweenFailoverAttempts_gatesCounterIncrementsWithinWindow() throws Exception {
    // Configure: small max (2) with a large non-zero delay window to ensure rapid calls stay within
    // window
    setProviderFailoverConfig(/* maxAttempts */ 2, /* delayMs */ 5000);

    assertEquals(2, getProviderMaxAttempts());
    assertEquals(5000, getProviderDelayMs());
    assertEquals(0, getProviderAttemptCount());

    // First call: should throw temporary and start the freeze window, incrementing attempt count to
    // 1
    assertThrows(JedisTemporarilyNotAvailableException.class,
      () -> provider.iterateActiveCluster(SwitchReason.HEALTH_CHECK, provider.getCluster()));
    int afterFirst = getProviderAttemptCount();
    assertEquals(1, afterFirst);

    // Many rapid subsequent calls within the delay window should continue to throw temporary
    // and should NOT increment the attempt count beyond 1
    for (int i = 0; i < 50; i++) {
      assertThrows(JedisTemporarilyNotAvailableException.class,
        () -> provider.iterateActiveCluster(SwitchReason.HEALTH_CHECK, provider.getCluster()));
      assertEquals(1, getProviderAttemptCount());
    }
  }

  @Test
  void delayBetweenFailoverAttempts_permanentExceptionAfterAttemptsExhausted() throws Exception {
    // Configure: small max (2) with a large non-zero delay window to ensure rapid calls stay within
    // window
    setProviderFailoverConfig(/* maxAttempts */ 2, /* delayMs */ 10);

    assertEquals(2, getProviderMaxAttempts());
    assertEquals(10, getProviderDelayMs());
    assertEquals(0, getProviderAttemptCount());

    // First call: should throw temporary and start the freeze window, incrementing attempt count to
    // 1
    assertThrows(JedisTemporarilyNotAvailableException.class,
      () -> provider.iterateActiveCluster(SwitchReason.HEALTH_CHECK, provider.getCluster()));
    int afterFirst = getProviderAttemptCount();
    assertEquals(1, afterFirst);

    // Many rapid subsequent calls within the delay window should continue to throw temporary
    // and should NOT increment the attempt count beyond 1
    for (int i = 0; i < 50; i++) {
      assertThrows(JedisTemporarilyNotAvailableException.class,
        () -> provider.iterateActiveCluster(SwitchReason.HEALTH_CHECK, provider.getCluster()));
      assertEquals(1, getProviderAttemptCount());
    }

    await().atMost(Durations.TWO_HUNDRED_MILLISECONDS).pollInterval(Duration.ofMillis(10))
        .until(() -> {
          Exception e = assertThrows(JedisFailoverException.class,
            () -> provider.iterateActiveCluster(SwitchReason.HEALTH_CHECK, provider.getCluster()));
          return e instanceof JedisPermanentlyNotAvailableException;
        });
  }

  @Test
  void maxNumFailoverAttempts_zeroDelay_leadsToPermanentAfterExceeding() throws Exception {
    // Configure: maxAttempts = 2, delay = 0 so each call increments the counter immediately
    setProviderFailoverConfig(/* maxAttempts */ 2, /* delayMs */ 0);

    assertEquals(2, getProviderMaxAttempts());
    assertEquals(0, getProviderDelayMs());
    assertEquals(0, getProviderAttemptCount());

    // Expect exactly 'maxAttempts' temporary exceptions, then a permanent one
    assertThrows(JedisTemporarilyNotAvailableException.class,
      () -> provider.iterateActiveCluster(SwitchReason.HEALTH_CHECK, provider.getCluster())); // attempt
                                                                                              // 1
    assertThrows(JedisTemporarilyNotAvailableException.class,
      () -> provider.iterateActiveCluster(SwitchReason.HEALTH_CHECK, provider.getCluster())); // attempt
                                                                                              // 2

    // Next should exceed max and become permanent
    assertThrows(JedisPermanentlyNotAvailableException.class,
      () -> provider.iterateActiveCluster(SwitchReason.HEALTH_CHECK, provider.getCluster())); // attempt
                                                                                              // 3
                                                                                              // ->
                                                                                              // permanent
  }

  // ======== Test helper methods (reflection) ========

  private static void setBuilderFailoverConfig(MultiClusterClientConfig.Builder builder,
      int maxAttempts, int delayMs) throws Exception {
    Field fMax = builder.getClass().getDeclaredField("maxNumFailoverAttempts");
    fMax.setAccessible(true);
    fMax.setInt(builder, maxAttempts);

    Field fDelay = builder.getClass().getDeclaredField("delayInBetweenFailoverAttempts");
    fDelay.setAccessible(true);
    fDelay.setInt(builder, delayMs);
  }

  private void setProviderFailoverConfig(int maxAttempts, int delayMs) throws Exception {
    // Access the underlying MultiClusterClientConfig inside provider and adjust fields for this
    // test
    Field cfgField = provider.getClass().getDeclaredField("multiClusterClientConfig");
    cfgField.setAccessible(true);
    Object cfg = cfgField.get(provider);

    Field fMax = cfg.getClass().getDeclaredField("maxNumFailoverAttempts");
    fMax.setAccessible(true);
    fMax.setInt(cfg, maxAttempts);

    Field fDelay = cfg.getClass().getDeclaredField("delayInBetweenFailoverAttempts");
    fDelay.setAccessible(true);
    fDelay.setInt(cfg, delayMs);
  }

  private int getProviderMaxAttempts() throws Exception {
    Field cfgField = provider.getClass().getDeclaredField("multiClusterClientConfig");
    cfgField.setAccessible(true);
    Object cfg = cfgField.get(provider);
    Field fMax = cfg.getClass().getDeclaredField("maxNumFailoverAttempts");
    fMax.setAccessible(true);
    return fMax.getInt(cfg);
  }

  private int getProviderDelayMs() throws Exception {
    Field cfgField = provider.getClass().getDeclaredField("multiClusterClientConfig");
    cfgField.setAccessible(true);
    Object cfg = cfgField.get(provider);
    Field fDelay = cfg.getClass().getDeclaredField("delayInBetweenFailoverAttempts");
    fDelay.setAccessible(true);
    return fDelay.getInt(cfg);
  }

  private int getProviderAttemptCount() throws Exception {
    Field f = provider.getClass().getDeclaredField("failoverAttemptCount");
    f.setAccessible(true);
    AtomicInteger val = (AtomicInteger) f.get(provider);
    return val.get();
  }
}
