package redis.clients.jedis.mcf;

import org.awaitility.Durations;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiDbConfig;
import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.mcf.JedisFailoverException.JedisPermanentlyNotAvailableException;
import redis.clients.jedis.mcf.JedisFailoverException.JedisTemporarilyNotAvailableException;
import redis.clients.jedis.util.ReflectionTestUtil;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * Tests for how getMaxNumFailoverAttempts and getDelayInBetweenFailoverAttempts impact
 * MultiDbConnectionProvider behaviour when no healthy clusters are available.
 */
public class MultiClusterFailoverAttemptsConfigTest {

  private HostAndPort endpoint0 = new HostAndPort("purposefully-incorrect", 0000);
  private HostAndPort endpoint1 = new HostAndPort("purposefully-incorrect", 0001);

  private MultiDbConnectionProvider provider;

  @BeforeEach
  void setUp() throws Exception {
    JedisClientConfig clientCfg = DefaultJedisClientConfig.builder().build();

    DatabaseConfig[] databaseConfigs = new DatabaseConfig[] {
        DatabaseConfig.builder(endpoint0, clientCfg).weight(1.0f).healthCheckEnabled(false).build(),
        DatabaseConfig.builder(endpoint1, clientCfg).weight(0.5f).healthCheckEnabled(false)
            .build() };

    MultiDbConfig.Builder builder = new MultiDbConfig.Builder(databaseConfigs);

    // Use small values by default for tests unless overridden per-test via reflection
    setBuilderFailoverConfig(builder, /* maxAttempts */ 10, /* delayMs */ 12000);

    provider = new MultiDbConnectionProvider(builder.build());

    // Disable both clusters to force handleNoHealthyCluster path
    provider.getDatabase(endpoint0).setDisabled(true);
    provider.getDatabase(endpoint1).setDisabled(true);
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
    assertThrows(JedisTemporarilyNotAvailableException.class, () -> MultiDbConnectionProviderHelper
        .switchToHealthyCluster(provider, SwitchReason.HEALTH_CHECK, provider.getDatabase()));
    int afterFirst = getProviderAttemptCount();
    assertEquals(1, afterFirst);

    // Many rapid subsequent calls within the delay window should continue to throw temporary
    // and should NOT increment the attempt count beyond 1
    for (int i = 0; i < 50; i++) {
      assertThrows(JedisTemporarilyNotAvailableException.class,
        () -> MultiDbConnectionProviderHelper.switchToHealthyCluster(provider,
          SwitchReason.HEALTH_CHECK, provider.getDatabase()));
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
    assertThrows(JedisTemporarilyNotAvailableException.class, () -> MultiDbConnectionProviderHelper
        .switchToHealthyCluster(provider, SwitchReason.HEALTH_CHECK, provider.getDatabase()));
    int afterFirst = getProviderAttemptCount();
    assertEquals(1, afterFirst);

    // Many rapid subsequent calls within the delay window should continue to throw temporary
    // and should NOT increment the attempt count beyond 1
    for (int i = 0; i < 50; i++) {
      assertThrows(JedisTemporarilyNotAvailableException.class,
        () -> provider.switchToHealthyDatabase(SwitchReason.HEALTH_CHECK, provider.getDatabase()));
      assertEquals(1, getProviderAttemptCount());
    }

    await().atMost(Durations.TWO_HUNDRED_MILLISECONDS).pollInterval(Duration.ofMillis(10))
        .until(() -> {
          Exception e = assertThrows(JedisFailoverException.class, () -> provider
              .switchToHealthyDatabase(SwitchReason.HEALTH_CHECK, provider.getDatabase()));
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
      () -> provider.switchToHealthyDatabase(SwitchReason.HEALTH_CHECK, provider.getDatabase())); // attempt
    // 1
    assertThrows(JedisTemporarilyNotAvailableException.class,
      () -> provider.switchToHealthyDatabase(SwitchReason.HEALTH_CHECK, provider.getDatabase())); // attempt
    // 2

    // Next should exceed max and become permanent
    assertThrows(JedisPermanentlyNotAvailableException.class,
      () -> provider.switchToHealthyDatabase(SwitchReason.HEALTH_CHECK, provider.getDatabase())); // attempt
    // 3
    // ->
    // permanent
  }

  // ======== Test helper methods (reflection) ========

  private static void setBuilderFailoverConfig(MultiDbConfig.Builder builder, int maxAttempts,
      int delayMs) throws Exception {
    ReflectionTestUtil.setField(builder, "maxNumFailoverAttempts", maxAttempts);

    ReflectionTestUtil.setField(builder, "delayInBetweenFailoverAttempts", delayMs);
  }

  private void setProviderFailoverConfig(int maxAttempts, int delayMs) throws Exception {
    // Access the underlying MultiDbConfig inside provider and adjust fields for this
    // test
    Object cfg = ReflectionTestUtil.getField(provider, "MultiDbConfig");

    ReflectionTestUtil.setField(cfg, "maxNumFailoverAttempts", maxAttempts);

    ReflectionTestUtil.setField(cfg, "delayInBetweenFailoverAttempts", delayMs);
  }

  private int getProviderMaxAttempts() throws Exception {
    Object cfg = ReflectionTestUtil.getField(provider, "MultiDbConfig");

    return ReflectionTestUtil.getField(cfg, "maxNumFailoverAttempts");
  }

  private int getProviderDelayMs() throws Exception {
    Object cfg = ReflectionTestUtil.getField(provider, "MultiDbConfig");

    return ReflectionTestUtil.getField(cfg, "delayInBetweenFailoverAttempts");
  }

  private int getProviderAttemptCount() throws Exception {
    AtomicInteger val = ReflectionTestUtil.getField(provider, "failoverAttemptCount");
    return val.get();
  }
}
