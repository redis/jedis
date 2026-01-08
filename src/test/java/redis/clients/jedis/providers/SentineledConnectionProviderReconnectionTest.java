package redis.clients.jedis.providers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.providers.SentineledConnectionProvider.SentinelConnectionFactory;
import redis.clients.jedis.util.Delay;

/**
 * Unit tests for SentineledConnectionProvider reconnection logic. Tests connection provider's
 * ability to reconnect to sentinel nodes with configured delay.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class SentineledConnectionProviderReconnectionTest {

  private static final String MASTER_NAME = "mymaster";

  private static final HostAndPort SENTINEL_1 = new HostAndPort("localhost", 26379);

  private static final HostAndPort SENTINEL_2 = new HostAndPort("localhost", 26380);

  private static final HostAndPort MASTER = new HostAndPort("localhost", 6379);

  private Set<HostAndPort> sentinels;

  private JedisClientConfig masterConfig;

  private JedisClientConfig sentinelConfig;

  private SentineledConnectionProvider provider;

  @Mock
  private Jedis mockJedis;

  @Mock
  private SentinelConnectionFactory sentinelConnectionFactory;

  @Mock
  private Delay reconnectDelay;

  @BeforeEach
  void setUp() {
    sentinels = new HashSet<>();
    sentinels.add(SENTINEL_1);
    sentinels.add(SENTINEL_2);

    masterConfig = mock(JedisClientConfig.class);
    sentinelConfig = mock(JedisClientConfig.class);
  }

  @AfterEach
  void tearDown() {
    if (provider != null) {
      provider.close();
    }
  }

  @Test
  void testReconnectToSentinelWithConfiguredDelay() throws InterruptedException {
    // Capture delay values passed to sleeper
    CopyOnWriteArrayList<Long> capturedDelays = new CopyOnWriteArrayList<>();
    // await for 3 reconnect attempts
    CountDownLatch reconnectAttempts = new CountDownLatch(3);

    // Mock dependencies
    SentineledConnectionProvider.Sleeper capturingSleeper = millis -> {
      capturedDelays.add(millis);
      reconnectAttempts.countDown();
    };

    // Simulate sentinel connection failures (disconnect scenario)
    long expectedDelay = 100L;
    when(mockJedis.sentinelGetMasterAddrByName(MASTER_NAME))
        .thenReturn(Arrays.asList(MASTER.getHost(), String.valueOf(MASTER.getPort())));

    Jedis failingJedis = mock(Jedis.class);
    doThrow(new JedisConnectionException("Connection lost")).when(failingJedis)
        .subscribe(any(JedisPubSub.class), anyString(), anyString(), anyString(), anyString());
    when(sentinelConnectionFactory.createConnection(any(), any()))
        .thenAnswer(invocation -> mockJedis).thenAnswer(invocation -> failingJedis);
    when(reconnectDelay.delay(anyLong())).thenReturn(Duration.ofMillis(expectedDelay));

    // Create provider
    provider = new SentineledConnectionProvider(MASTER_NAME, masterConfig, null, null, sentinels,
        sentinelConfig, reconnectDelay, sentinelConnectionFactory, capturingSleeper);

    // Verify reconnection attempts happen with configured delay
    assertTrue(reconnectAttempts.await(100, TimeUnit.MILLISECONDS),
      "Should attempt to reconnect at least 3 times after disconnect");

    // Assert all delays match the configured value
    assertTrue(capturedDelays.size() >= 3, "Should have captured at least 3 delay values");
    for (Long delay : capturedDelays) {
      assertEquals(expectedDelay, delay,
        "Sleeper should be called with configured delay of " + expectedDelay + "ms");
    }
  }

}
