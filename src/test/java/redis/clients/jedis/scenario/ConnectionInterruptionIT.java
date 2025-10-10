package redis.clients.jedis.scenario;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.providers.PooledConnectionProvider;
import redis.clients.jedis.util.SafeEncoder;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tags({ @Tag("scenario") })
public class ConnectionInterruptionIT {

  private static final Logger log = LoggerFactory.getLogger(ConnectionInterruptionIT.class);

  private static EndpointConfig endpoint;

  private final FaultInjectionClient faultClient = new FaultInjectionClient();

  @BeforeAll
  public static void beforeClass() {
    try {
      ConnectionInterruptionIT.endpoint = HostAndPorts.getRedisEndpoint("re-standalone");
    } catch (IllegalArgumentException e) {
      log.warn("Skipping test because no Redis endpoint is configured");
      assumeTrue(false);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "dmc_restart", "network_failure" })
  public void testWithPool(String triggerAction) {
    ConnectionProvider connectionProvider = new PooledConnectionProvider(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().build(), RecommendedSettings.poolConfig);

    UnifiedJedis client = new UnifiedJedis(connectionProvider, RecommendedSettings.MAX_RETRIES,
        RecommendedSettings.MAX_TOTAL_RETRIES_DURATION);
    String keyName = "counter";
    client.set(keyName, "0");
    assertEquals("0", client.get(keyName));

    AtomicLong commandsExecuted = new AtomicLong();

    // Start thread that imitates an application that uses the client
    FakeApp fakeApp = new FakeApp(client, (UnifiedJedis c) -> {
      assertTrue(client.incr(keyName) > 0);
      long currentCount = commandsExecuted.getAndIncrement();
      log.info("Command executed {}", currentCount);
      return true;
    });
    fakeApp.setKeepExecutingForSeconds(RecommendedSettings.DEFAULT_TIMEOUT_MS / 1000 * 2);
    Thread t = new Thread(fakeApp);
    t.start();

    HashMap<String, Object> params = new HashMap<>();
    params.put("bdb_id", endpoint.getBdbId());

    FaultInjectionClient.TriggerActionResponse actionResponse = null;

    try {
      log.info("Triggering {}", triggerAction);
      actionResponse = faultClient.triggerAction(triggerAction, params);
    } catch (IOException e) {
      fail("Fault Injection Server error:" + e.getMessage());
    }

    log.info("Action id: {}", actionResponse.getActionId());
    fakeApp.setAction(actionResponse);

    try {
      t.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    log.info("Commands executed: {}", commandsExecuted.get());
    assertEquals(commandsExecuted.get(), Long.parseLong(client.get(keyName)));
    assertTrue(fakeApp.capturedExceptions().isEmpty());

    client.close();
  }

  @ParameterizedTest
  @ValueSource(strings = { "dmc_restart", "network_failure" })
  public void testWithPubSub(String triggerAction) {
    ConnectionProvider connectionProvider = new PooledConnectionProvider(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().build(), RecommendedSettings.poolConfig);

    UnifiedJedis client = new UnifiedJedis(connectionProvider, RecommendedSettings.MAX_RETRIES,
        RecommendedSettings.MAX_TOTAL_RETRIES_DURATION);

    AtomicLong messagesSent = new AtomicLong();
    AtomicLong messagesReceived = new AtomicLong();

    final Thread subscriberThread = getSubscriberThread(messagesReceived, connectionProvider);

    // Start thread that imitates a publisher that uses the client
    FakeApp fakeApp = new FakeApp(client, (UnifiedJedis c) -> {
      log.info("Publishing message");
      long consumed = client.publish("test", String.valueOf(messagesSent.getAndIncrement()));
      return consumed > 0;
    });
    fakeApp.setKeepExecutingForSeconds(10);
    Thread t = new Thread(fakeApp);
    t.start();

    HashMap<String, Object> params = new HashMap<>();
    params.put("bdb_id", endpoint.getBdbId());

    FaultInjectionClient.TriggerActionResponse actionResponse = null;

    try {
      log.info("Triggering {}", triggerAction);
      actionResponse = faultClient.triggerAction(triggerAction, params);
    } catch (IOException e) {
      fail("Fault Injection Server error:" + e.getMessage());
    }

    log.info("Action id: {}", actionResponse.getActionId());
    fakeApp.setAction(actionResponse);

    try {
      t.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    if (subscriberThread.isAlive()) subscriberThread.interrupt();

    assertEquals(messagesSent.get() - 1, messagesReceived.get());
    assertTrue(fakeApp.capturedExceptions().isEmpty());

    client.close();
  }

  private static Thread getSubscriberThread(AtomicLong messagesReceived,
      ConnectionProvider connectionProvider) {
    final JedisPubSubBase<String> pubSub = new JedisPubSubBase<String>() {

      @Override
      public void onMessage(String channel, String message) {
        messagesReceived.incrementAndGet();
        log.info("Received message: {}", message);
      }

      @Override
      protected String encode(byte[] raw) {
        return SafeEncoder.encode(raw);
      }
    };

    final Thread subscriberThread = new Thread(() -> {
      try {
        pubSub.proceed(connectionProvider.getConnection(), "test");
        fail("PubSub should have been interrupted");
      } catch (JedisConnectionException e) {
        log.info("Expected exception in Subscriber: {}", e.getMessage());
        assertTrue(e.getMessage().contains("Unexpected end of stream."));
      }
    });
    subscriberThread.start();
    return subscriberThread;
  }
}
