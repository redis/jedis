package redis.clients.jedis.pubsub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.pubsub.util.PubSubTestHelper;

/**
 * Abstract base class for RedisClient pub/sub tests.
 * Contains all test scenarios that will be executed against both:
 * - Real Redis server (Integration tests)
 * - RedisServerStub (Mock tests)
 * <p>
 * Each test class instance runs with a single RESP protocol (either RESP2 or RESP3).
 * Clients are created once per test class and reused across all tests.
 * </p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class RedisClientPubSubTestBase {

  private static final long THREAD_JOIN_TIMEOUT_MS = 5000;

  protected RedisProtocol protocol;
  protected RedisClient client;
  protected RedisClient publisherClient;

  /**
   * Subclasses must implement this to create a RedisClient instance.
   */
  protected abstract RedisClient createClient(RedisProtocol protocol);

  /**
   * Subclasses must provide the protocol for this test instance.
   */
  protected abstract RedisProtocol getProtocol();

  @BeforeAll
  public void setUpClients() {
    // Initialize protocol and create clients once for all tests
    this.protocol = getProtocol();
    this.client = createClient(protocol);
    this.publisherClient = createClient(protocol);
  }

  @AfterAll
  public void tearDownClients() {
    // Close clients after all tests complete
    if (client != null) {
      client.close();
    }
    if (publisherClient != null) {
      publisherClient.close();
    }
  }

  @Test
  public void subscribeToSingleChannel() throws Exception {

    PubSubTestHelper.MessageCapture subscriber = new PubSubTestHelper.MessageCapture(1, 0);

    Thread subscriberThread = new Thread(() -> {
      client.subscribe(subscriber, "test-channel");
    });
    subscriberThread.start();

    assertTrue(subscriber.awaitSubscription(), "Should subscribe successfully");

    List<String> subscribedChannels = subscriber.getChannels();
    assertEquals(1, subscribedChannels.size(), "Should have 1 subscription");
    assertEquals("test-channel", subscribedChannels.get(0), "Should subscribe to test-channel");

    subscriber.unsubscribe();
    subscriberThread.join(THREAD_JOIN_TIMEOUT_MS);
  }

  @Test
  public void subscribeToMultipleChannels() throws Exception {

    PubSubTestHelper.MessageCapture subscriber = new PubSubTestHelper.MessageCapture(3, 0);

    Thread subscriberThread = new Thread(() -> {
      client.subscribe(subscriber, "channel1", "channel2", "channel3");
    });
    subscriberThread.start();

    assertTrue(subscriber.awaitSubscription(), "Should subscribe to all channels");

    List<String> subscribedChannels = subscriber.getChannels();
    assertEquals(3, subscribedChannels.size(), "Should have 3 subscriptions");
    assertTrue(subscribedChannels.contains("channel1"), "Should subscribe to channel1");
    assertTrue(subscribedChannels.contains("channel2"), "Should subscribe to channel2");
    assertTrue(subscribedChannels.contains("channel3"), "Should subscribe to channel3");

    subscriber.unsubscribe();
    subscriberThread.join(THREAD_JOIN_TIMEOUT_MS);
  }

  @Test
  public void patternSubscription() throws Exception {

    PubSubTestHelper.MessageCapture subscriber = new PubSubTestHelper.MessageCapture(0, 1, 2);

    Thread subscriberThread = new Thread(() -> {
      client.psubscribe(subscriber, "test.*");
    });
    subscriberThread.start();

    assertTrue(subscriber.awaitPSubscription(), "Should subscribe to pattern");

    List<String> subscribedPatterns = subscriber.getPatterns();
    assertEquals(1, subscribedPatterns.size(), "Should have 1 pattern subscription");
    assertEquals("test.*", subscribedPatterns.get(0), "Should subscribe to test.* pattern");

    // Publish to matching channels
    publisherClient.publish("test.foo", "message1");
    publisherClient.publish("test.bar", "message2");

    assertTrue(subscriber.awaitMessages(), "Should receive messages from pattern");

    List<String> messages = subscriber.getMessages();
    assertEquals(2, messages.size(), "Should receive 2 messages");
    assertTrue(messages.contains("message1"), "Should contain message1");
    assertTrue(messages.contains("message2"), "Should contain message2");

    subscriber.punsubscribe();
    subscriberThread.join(THREAD_JOIN_TIMEOUT_MS);
  }

  @Test
  public void publishAndReceiveMessage() throws Exception {

    PubSubTestHelper.MessageCapture subscriber = new PubSubTestHelper.MessageCapture(1, 1);

    Thread subscriberThread = new Thread(() -> {
      client.subscribe(subscriber, "test-channel");
    });
    subscriberThread.start();

    assertTrue(subscriber.awaitSubscription(), "Should subscribe successfully");

    // Publish message
    long numReceivers = publisherClient.publish("test-channel", "Hello World");
    assertTrue(numReceivers >= 1, "Should have at least 1 receiver");

    assertTrue(subscriber.awaitMessages(), "Should receive message");

    List<String> messages = subscriber.getMessages();
    assertEquals(1, messages.size(), "Should receive 1 message");
    assertEquals("Hello World", messages.get(0), "Should receive correct message");

    subscriber.unsubscribe();
    subscriberThread.join(THREAD_JOIN_TIMEOUT_MS);
  }

  @Test
  public void unsubscribeFromChannel() throws Exception {

    PubSubTestHelper.MessageCapture subscriber = new PubSubTestHelper.MessageCapture(2, 1);
    subscriber.expectUnsubscribe(1); // Expect 1 unsubscribe event

    Thread subscriberThread = new Thread(() -> {
      client.subscribe(subscriber, "channel1", "channel2");
    });
    subscriberThread.start();

    assertTrue(subscriber.awaitSubscription(), "Should subscribe to channels");

    List<String> subscribedChannels = subscriber.getChannels();
    assertEquals(2, subscribedChannels.size(), "Should have 2 subscriptions");
    assertTrue(subscribedChannels.contains("channel1"), "Should subscribe to channel1");
    assertTrue(subscribedChannels.contains("channel2"), "Should subscribe to channel2");

    // Unsubscribe from one channel
    subscriber.unsubscribe("channel1");

    // Wait for unsubscribe confirmation before publishing
    assertTrue(subscriber.awaitUnsubscribe(), "Should confirm unsubscribe from channel1");

    // Publish to both channels
    publisherClient.publish("channel1", "message1");
    publisherClient.publish("channel2", "message2");

    assertTrue(subscriber.awaitMessages(), "Should receive message from channel2");

    List<String> messages = subscriber.getMessages();
    assertEquals(1, messages.size(), "Should only receive message from channel2");
    assertEquals("message2", messages.get(0), "Should receive correct message");

    subscriber.unsubscribe();
    subscriberThread.join(THREAD_JOIN_TIMEOUT_MS);
  }

  @Test
  public void autoUnsubscribeAfterMessage() throws Exception {

    PubSubTestHelper.AutoUnsubscriber subscriber = new PubSubTestHelper.AutoUnsubscriber();

    Thread subscriberThread = new Thread(() -> {
      client.subscribe(subscriber, "test-channel");
    });
    subscriberThread.start();

    assertTrue(subscriber.awaitSubscription(), "Should subscribe successfully");

    // Publish message - subscriber will auto-unsubscribe in onMessage callback
    long numReceivers = publisherClient.publish("test-channel", "Auto-unsubscribe test");
    assertTrue(numReceivers >= 1, "Should have at least 1 receiver");

    assertTrue(subscriber.awaitMessage(), "Should receive message");
    assertEquals("Auto-unsubscribe test", subscriber.getReceivedMessage(),
        "Should receive correct message");

    // Thread should exit cleanly because subscriber auto-unsubscribed
    subscriberThread.join(THREAD_JOIN_TIMEOUT_MS);
  }

  @Test
  public void pingDuringSubscription() throws Exception {

    PubSubTestHelper.PingPongSubscriber subscriber = new PubSubTestHelper.PingPongSubscriber();

    Thread subscriberThread = new Thread(() -> {
      client.subscribe(subscriber, "test-channel");
    });
    subscriberThread.start();

    assertTrue(subscriber.awaitSubscription(), "Should subscribe to channel");

    // Send ping
    subscriber.ping();

    assertTrue(subscriber.awaitPong(), "Should receive pong");

    subscriberThread.join(THREAD_JOIN_TIMEOUT_MS);
  }

  // NOTE: Binary subscriptions not tested as RedisClient.subscribe() only accepts String channels
  // Binary pub/sub would need to be tested with Connection or Jedis directly
}

