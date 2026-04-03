package redis.clients.jedis.pubsub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.pubsub.util.PubSubTestHelper;
import redis.server.stub.MaintenanceEvent;
import redis.server.stub.RedisServerStub;

/**
 * Mock tests for RedisClient pub/sub functionality using RedisServerStub.
 * <p>
 * These tests run against RedisServerStub (mock Redis) **ONLY with RESP3** since
 * RedisServerStub only supports RESP3 protocol.
 * </p>
 * <p>
 * RedisServerStub allows testing scenarios that are difficult to simulate with a real Redis server,
 * such as arbitrary push notifications sent during subscribed connections, connection failures,
 * and timing-sensitive behaviors.
 * </p>
 */
public class RedisClientPubSubStubTest {

  private static RedisServerStub serverStub;

  @BeforeAll
  public static void setUpAll() throws Exception {
    serverStub = new RedisServerStub();
    serverStub.start();
  }

  @AfterAll
  public static void tearDownAll() throws Exception {
    if (serverStub != null) {
      serverStub.stop();
    }
  }

  /**
   * RESP3 tests - RedisServerStub only supports RESP3.
   */
  @Nested
  public class Resp3Tests extends RedisClientPubSubTestBase {

    @Override
    protected RedisClient createClient(RedisProtocol protocol) {
      return RedisClient.builder()
          .hostAndPort(new HostAndPort("localhost", serverStub.getPort()))
          .clientConfig(DefaultJedisClientConfig.builder()
              .protocol(protocol)
              .build())
          .build();
    }

    @Override
    protected RedisProtocol getProtocol() {
      return RedisProtocol.RESP3;
    }

    /**
     * Test that maintenance notifications during subscription don't cause errors.
     */
    @Test
    public void maintenanceNotificationDuringSubscription() throws Exception {
      PubSubTestHelper.MessageCapture subscriber = new PubSubTestHelper.MessageCapture(1, 1);

      Thread subscriberThread = new Thread(() -> {
        client.subscribe(subscriber, "test-channel");
      });
      subscriberThread.start();

      assertTrue(subscriber.awaitSubscription(), "Should subscribe successfully");

      // Send server maintenance notification (migrating event for shards)
      serverStub.sendPushMessageToAll(MaintenanceEvent.migrating(1, 30, "shard-001", "shard-002"));

      // Verify subscription still works after maintenance notification
      long numReceivers = publisherClient.publish("test-channel", "test-message");
      assertTrue(numReceivers >= 1, "Should have at least 1 receiver");

      assertTrue(subscriber.awaitMessages(), "Should receive pub/sub message");

      List<String> messages = subscriber.getMessages();
      assertEquals(1, messages.size(), "Should receive 1 message");
      assertEquals("test-message", messages.get(0), "Should receive correct message");

      subscriber.unsubscribe();
      subscriberThread.join(THREAD_JOIN_TIMEOUT_MS);
    }
  }
}

