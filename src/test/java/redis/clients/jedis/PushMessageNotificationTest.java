package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.util.RedisVersionCondition;

/**
 * Tests for Redis RESP3 push notifications functionality.
 */
@SinceRedisVersion("6.0.0")
public class PushMessageNotificationTest {

  private static final EndpointConfig endpoint = Endpoints.getRedisEndpoint("standalone0");

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(() -> endpoint);

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
   * Helper method to modify a key using a separate connection to trigger a push message.
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
    CommandObject<String> set = new CommandObject<>(comArgs.key(testKey).add(initialValue),
        BuilderFactory.STRING);
    String setResult = connection.executeCommand(set);
    assertEquals("OK", setResult);

    // Get the key to track it
    CommandObject<String> get = new CommandObject<>(new CommandArguments(Command.GET).key(testKey),
        BuilderFactory.STRING);
    String getResponse = connection.executeCommand(get);
    assertEquals(initialValue, getResponse);

    // Modify the key from another connection to trigger invalidation
    triggerKeyInvalidation(testKey, modifiedValue);

    // Send PING and expect to receive invalidation message first, then PONG
    CommandObject<String> ping = new CommandObject<>(new CommandArguments(Command.PING),
        BuilderFactory.STRING);
    String pingResponse = connection.executeCommand(ping);
    assertEquals("PONG", pingResponse);
  }

  @Test
  public void testConnectionResp3PushNotificationsWithCustomListener() {
    // Create a list to store received push messages
    List<PushMessage> receivedMessages = new ArrayList<>();

    // Create a custom push listener
    PushConsumer listener = new PushConsumer() {
      @Override
      public PushConsumerContext handle(PushConsumerContext context) {
        receivedMessages.add(context.getMessage());

        return context;
      }
    };

    // Create connection with RESP3 protocol
    connection = new Connection(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().protocol(RedisProtocol.RESP3).build());
    connection.connect();

    // Set the push listener
    connection.addPushConsumer(listener);

    // Enable client tracking
    enableClientTracking(connection);

    // Set and get a key to track it
    CommandArguments setArgs = new CommandArguments(Command.SET);
    CommandObject<String> setCmd = new CommandObject<>(setArgs.key(testKey).add(initialValue),
        BuilderFactory.STRING);
    connection.executeCommand(setCmd);

    CommandObject<String> getCmd = new CommandObject<>(
        new CommandArguments(Command.GET).key(testKey), BuilderFactory.STRING);
    connection.executeCommand(getCmd);

    // Modify the key from another connection to trigger push message
    triggerKeyInvalidation(testKey, modifiedValue);

    // Send a command to trigger processing of any pending push messages
    CommandObject<String> pingCmd = new CommandObject<>(new CommandArguments(Command.PING),
        BuilderFactory.STRING);
    String pingResponse = connection.executeCommand(pingCmd);
    assertEquals("PONG", pingResponse);

    // Verify we received at least one push message
    assertTrue(!receivedMessages.isEmpty(), "Should have received at least one push message");

    // Verify the message is an invalidation message
    PushMessage pushMessage = receivedMessages.get(0);
    assertNotNull(pushMessage);
    assertEquals("invalidate", pushMessage.getType());
  }

}
