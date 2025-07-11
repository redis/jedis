package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.util.RedisVersionCondition;

/**
 * Tests for Redis RESP3 push notifications functionality.
 */
@SinceRedisVersion("6.0.0")
public class PushMessageNotificationTest {

  private static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0");

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(endpoint);

  private Connection connection;
  private UnifiedJedis unifiedJedis;
  private final String testKey = "tracking:test:key";
  private final String initialValue = "initial";
  private final String modifiedValue = "modified";

  @BeforeEach
  public void setUp() {
    // Nothing to set up by default - connections are created in each test
  }

  @AfterEach
  public void tearDown() {
    if (connection != null) {
      connection.close();
      connection = null;
    }
    
    if (unifiedJedis != null) {
      try {
        unifiedJedis.sendCommand(Command.CLIENT, "TRACKING", "OFF");
      } catch (Exception e) {
        // Ignore exceptions during cleanup
      }
      unifiedJedis.close();
      unifiedJedis = null;
    }
  }
  
  /**
   * Helper method to modify a key using a separate connection to trigger invalidation.
   * 
   * @param key The key to modify
   * @param value The new value to set
   */
  private void triggerKeyInvalidation(String key, String value) {
    try (Jedis modifierClient = new Jedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().protocol(RedisProtocol.RESP3).build())) {
      modifierClient.set(key, value);
    }
  }
  
  /**
   * Helper method to enable client tracking on a connection.
   * 
   * @param connection The connection on which to enable tracking
   */
  private void enableClientTracking(Connection connection) {
    connection.sendCommand(Command.CLIENT, "TRACKING", "ON");
    assertEquals("OK", connection.getStatusCodeReply());
  }

  @Test
  public void testConnectionResp3PushNotifications() {
    connection = new Connection(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().protocol(RedisProtocol.RESP3).build());
    connection.connect();

    // Enable client tracking
    enableClientTracking(connection);

    // Set initial value
    CommandArguments comArgs = new CommandArguments(Command.SET);
    CommandObject<String> set = new CommandObject<>(comArgs.key(testKey).add(initialValue), BuilderFactory.STRING);
    String setResult = connection.executeCommand(set);
    assertEquals("OK", setResult);

    // Get the key to track it
    CommandObject<String> get = new CommandObject<>(new CommandArguments(Command.GET).key(testKey), BuilderFactory.STRING);
    String getResponse = connection.executeCommand(get);
    assertEquals(initialValue, getResponse);

    // Modify the key from another connection to trigger invalidation
    triggerKeyInvalidation(testKey, modifiedValue);

    // Send PING and expect to receive invalidation message first, then PONG
    CommandObject<String> ping = new CommandObject<>(new CommandArguments(Command.PING), BuilderFactory.STRING);
    String pingResponse = connection.executeCommand(ping);
    assertEquals("PONG", pingResponse);
  }

  @Test
  public void testUnifiedJedisResp3PushNotifications() {
    unifiedJedis = new UnifiedJedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().protocol(RedisProtocol.RESP3).build());
    
    // Enable client tracking
    unifiedJedis.sendCommand(Command.CLIENT, "TRACKING", "ON");
    
    // Set initial value
    unifiedJedis.set(testKey, initialValue);
    
    // Get the key to track it
    String getResponse = unifiedJedis.get(testKey);
    assertEquals(initialValue, getResponse);
    
    // Modify the key from another connection to trigger invalidation
    triggerKeyInvalidation(testKey, modifiedValue);

    // Send PING command
    String pingResponse = unifiedJedis.ping();
    // Next reply should be PONG
    assertEquals("PONG", pingResponse);
  }

  @Test
  public void testUnifiedJedisCustomPushListener() {
    List<PushMessage> receivedMessages = new ArrayList<>();
    PushHandlerImpl pushHandler = new PushHandlerImpl();
    pushHandler.addListener(receivedMessages::add);

    DefaultJedisClientConfig clientConfig = endpoint.getClientConfigBuilder()
        .pushHandler(pushHandler)
        .protocol(RedisProtocol.RESP3).build();

    unifiedJedis = new UnifiedJedis(endpoint.getHostAndPort(), clientConfig);


    // Enable client tracking
    unifiedJedis.sendCommand(Command.CLIENT, "TRACKING", "ON");

    // Set initial value
    unifiedJedis.set(testKey, initialValue);

    // Get the key to track it
    assertEquals(initialValue, unifiedJedis.get(testKey));

    // Modify the key from another connection to trigger invalidation
    triggerKeyInvalidation(testKey, modifiedValue);

    // Send PING command
    String pingResponse = unifiedJedis.ping();
    // Next reply should be PONG
    assertEquals("PONG", pingResponse);
    assertEquals(1, receivedMessages.size());
    assertEquals("invalidate", receivedMessages.get(0).getType());
  }

  @Test
  public void testJedisCustomPushListener() {
    List<PushMessage> receivedMessages = new ArrayList<>();
    PushHandlerImpl pushHandler = new PushHandlerImpl();
    pushHandler.addListener(receivedMessages::add);

    DefaultJedisClientConfig clientConfig = endpoint.getClientConfigBuilder()
        .pushHandler(pushHandler)
        .protocol(RedisProtocol.RESP3).build();

    Jedis jedis = new Jedis(endpoint.getHostAndPort(), clientConfig);

    // Enable client tracking
    jedis.sendCommand(Command.CLIENT, "TRACKING", "ON");

    // Set initial value
    jedis.set(testKey, initialValue);

    // Get the key to track it
    assertEquals(initialValue, jedis.get(testKey));

    // Modify the key from another connection to trigger invalidation
    triggerKeyInvalidation(testKey, modifiedValue);

    // Send PING command
    String pingResponse = jedis.ping();
    // Next reply should be PONG
    assertEquals("PONG", pingResponse);
    assertEquals(1, receivedMessages.size());
    assertEquals("invalidate", receivedMessages.get(0).getType());

    // Clean up
    jedis.close();
  }
  
  @Test
  public void testConnectionResp3PushNotificationsWithCustomListener() {
    // Create a list to store received push messages
    List<PushMessage> receivedMessages = new ArrayList<>();
    
    // Create a custom push listener
    PushConsumer listener = pushContext -> { receivedMessages.add(pushContext.getMessage());};

    // Create connection with RESP3 protocol
    connection = new Connection(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().protocol(RedisProtocol.RESP3).build());
    connection.connect();
    
    // Set the push listener
    connection.getPushConsumer().add(listener);
    
    // Enable client tracking
    enableClientTracking(connection);
    
    // Set and get a key to track it
    CommandArguments setArgs = new CommandArguments(Command.SET);
    CommandObject<String> setCmd = new CommandObject<>(setArgs.key(testKey).add(initialValue), BuilderFactory.STRING);
    connection.executeCommand(setCmd);
    
    CommandObject<String> getCmd = new CommandObject<>(new CommandArguments(Command.GET).key(testKey), BuilderFactory.STRING);
    connection.executeCommand(getCmd);
    
    // Modify the key from another connection to trigger invalidation
    triggerKeyInvalidation(testKey, modifiedValue);
    
    // Send a command to trigger processing of any pending push messages
    CommandObject<String> pingCmd = new CommandObject<>(new CommandArguments(Command.PING), BuilderFactory.STRING);
    String pingResponse = connection.executeCommand(pingCmd);
    assertEquals("PONG", pingResponse);
    
    // Verify we received at least one push message
    assertTrue(!receivedMessages.isEmpty(), "Should have received at least one push message");
    
    // Verify the message is an invalidation message
    PushMessage pushMessage = receivedMessages.get(0);
    assertNotNull(pushMessage);
    assertEquals("invalidate", pushMessage.getType());
  }

  @ParameterizedTest
  @MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
  public void testUnifiedJedisPubSubWithResp3PushNotifications(RedisProtocol protocol) throws InterruptedException {
    // Create a UnifiedJedis instance with RESP3 protocol for subscribing
    unifiedJedis = new UnifiedJedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().protocol(protocol).build());
    
    // Enable client tracking to generate push notifications
    unifiedJedis.sendCommand(Command.CLIENT, "TRACKING", "ON");
    
    // Set initial value to track
    unifiedJedis.set(testKey, initialValue);
    
    // Get the key to track it
    String getResponse = unifiedJedis.get(testKey);
    assertEquals(initialValue, getResponse);
    
    // Create a list to store received pub/sub messages
    final List<String> receivedMessages = new ArrayList<>();
    
    // Create an atomic counter to track received messages
    final AtomicInteger messageCounter = new AtomicInteger(0);
    
    // Create a latch to signal when subscription is ready
    final CountDownLatch subscriptionLatch = new CountDownLatch(1);
    
    // Create a JedisPubSub instance to handle pub/sub messages
    JedisPubSub pubSub = new JedisPubSub() {
      @Override
      public void onMessage(String channel, String message) {
        System.out.println("onMessage from " + channel + " : " + message);
        receivedMessages.add(message);
        
        // If we've received both messages, unsubscribe
        if (messageCounter.incrementAndGet() == 2) {
          this.unsubscribe("test-channel");
        }
      }

      @Override
      public void onUnsubscribe(String channel, int subscribedChannels) {
        // Signal that subscription is ready
        System.out.println("Unsubscribed from " + channel);
      }

      @Override
      public void onSubscribe(String channel, int subscribedChannels) {
        // Signal that subscription is ready
        subscriptionLatch.countDown();
      }
    };
    
    // Start a thread to handle the subscription
    Thread subscriberThread = new Thread(() -> {
      unifiedJedis.subscribe(pubSub, "test-channel");
    });
    
    // Start the subscriber thread
    subscriberThread.start();
    
    // Start a thread to publish messages and trigger key invalidation
    Thread publisherThread = new Thread(() -> {
      try (UnifiedJedis publisher = new UnifiedJedis(endpoint.getHostAndPort(),
          endpoint.getClientConfigBuilder().protocol(RedisProtocol.RESP3).build())) {
        
        // Wait for subscription to be ready
        try {
          if (!subscriptionLatch.await(5, TimeUnit.SECONDS)) {
            System.err.println("Timed out waiting for subscription to be ready");
            return;
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
        
        // Publish a message
        publisher.publish("test-channel", "test-message-1");
        
        // Trigger key invalidation to generate a push notification
        triggerKeyInvalidation(testKey, modifiedValue);
        
        // Publish another message
        publisher.publish("test-channel", "test-message-2");
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    
    // Start the publisher thread
    publisherThread.start();
    
    // Wait for the subscriber thread to complete (it will complete when unsubscribe is called)
    subscriberThread.join();
    
    // Wait for the publisher thread to complete
    publisherThread.join();
    
    // Verify that we received both pub/sub messages
    assertEquals(2, receivedMessages.size(), "Should have received both pub/sub messages");
    assertEquals("test-message-1", receivedMessages.get(0));
    assertEquals("test-message-2", receivedMessages.get(1));
    
    // Send a PING command to process any pending push messages
    String pingResponse = unifiedJedis.ping();
    assertEquals("PONG", pingResponse);
  }
}
