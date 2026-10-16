package redis.clients.jedis.sch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.JedisShardedPubSub;
import redis.clients.jedis.MaintenanceNotificationsConfig.Mode;
import redis.clients.jedis.RedisProtocol;

/**
 * Section G (pub/sub) of the cluster maintenance test plan, against a three-node
 * {@code TcpMockServer} cluster. The cases are defined in ClusterMaintenanceIT.md, in this package
 * directory.
 */
public class ClusterMaintenancePubSubMockTest extends AbstractClusterMaintenanceMockTest {

  /** Case G1, section G, in ClusterMaintenanceIT.md. */
  @Test
  public void subscribedConnectionConsumesMaintenancePushesInline() throws Exception {
    // infinite blocking timeout: the subscription loop waits for messages indefinitely
    openClient(clientConfig(RedisProtocol.RESP3, SO_TIMEOUT_MS, 0), maintConfig(Mode.AUTO));
    String channel = keyOnA();
    CountDownLatch subscribed = new CountDownLatch(1);
    List<String> messages = new CopyOnWriteArrayList<>();
    JedisShardedPubSub pubSub = new JedisShardedPubSub() {
      @Override
      public void onSSubscribe(String ch, int subscribedChannels) {
        subscribed.countDown();
      }

      @Override
      public void onSMessage(String ch, String message) {
        messages.add(message);
        if (messages.size() == 2) {
          sunsubscribe();
        }
      }
    };
    Thread loop = new Thread(() -> client.ssubscribe(pubSub, channel), "ssubscribe");
    loop.start();
    assertTrue(subscribed.await(5, TimeUnit.SECONDS));

    serverA.sendPushMessageToAll("smessage", channel, "first");
    serverA.sendPushMessageToAll(migrating(1L, "0-100")); // between two pub/sub frames
    serverA.sendPushMessageToAll("smessage", channel, "second");
    loop.join(5000);
    assertFalse(loop.isAlive(), "the loop ended on the unsubscribe reply");

    assertEquals(Arrays.asList("first", "second"), messages);
    try (Connection b = client.getConnectionFromSlot(B_FROM)) {
      assertTrue(b.ping());
      assertTrue(relaxed(b), "the opener consumed inline opened the client-wide gate");
      assertEquals(RELAXED_TIMEOUT_MS, soTimeout(b));
    }
    try (Connection a = client.getConnectionFromSlot(A_FROM)) {
      assertTrue(a.ping(), "the former subscription connection is back in service");
      assertTrue(relaxed(a));
    }
  }
}
