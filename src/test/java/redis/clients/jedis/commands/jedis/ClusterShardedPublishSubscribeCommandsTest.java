package redis.clients.jedis.commands.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import io.redis.test.annotations.SinceRedisVersion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.BinaryJedisShardedPubSub;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardedPubSub;
import redis.clients.jedis.PubSubConnectionCleanupTestSupport;
import redis.clients.jedis.PubSubConnectionCleanupTestSupport.ExitMode;
import redis.clients.jedis.PubSubConnectionCleanupTestSupport.SubscriptionType;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.util.JedisClusterCRC16;
import redis.clients.jedis.util.SafeEncoder;

@SinceRedisVersion(value = "7.0.0", message = "Sharded Pub/Sub")
@Tag("integration")
public class ClusterShardedPublishSubscribeCommandsTest extends ClusterJedisCommandsTestBase {

  private static Stream<Arguments> shardedConnectionCleanupCases() {
    return Stream.of(SubscriptionType.SSUBSCRIBE, SubscriptionType.LEGACY_SSUBSCRIBE)
        .flatMap(type -> Stream.of(RedisProtocol.RESP2, RedisProtocol.RESP3).flatMap(protocol ->
            Arrays.stream(ExitMode.values()).map(exit -> Arguments.of(type, protocol, exit))));
  }

  @ParameterizedTest(name = "{0} {1} {2}")
  @MethodSource("shardedConnectionCleanupCases")
  void shardedConnectionCleanup(SubscriptionType type, RedisProtocol protocol, ExitMode exit)
      throws Exception {
    PubSubConnectionCleanupTestSupport.assertPooledCleanup(endpoint, type, protocol, exit);
  }

  private void publishOne(final String channel, final String message) {
    Thread t = new Thread(() -> cluster.spublish(channel, message));
    t.start();
  }

  @Test
  public void subscribe() throws InterruptedException {
    cluster.ssubscribe(new JedisShardedPubSub() {
      @Override public void onSMessage(String channel, String message) {
        assertEquals("foo", channel);
        assertEquals("exit", message);
        sunsubscribe();
      }

      @Override public void onSSubscribe(String channel, int subscribedChannels) {
        assertEquals("foo", channel);
        assertEquals(1, subscribedChannels);

        // now that I'm subscribed... publish
        publishOne("foo", "exit");
      }

      @Override public void onSUnsubscribe(String channel, int subscribedChannels) {
        assertEquals("foo", channel);
        assertEquals(0, subscribedChannels);
      }
    }, "foo");
  }

  @Test
  public void subscribeMany() {
    cluster.ssubscribe(new JedisShardedPubSub() {
      @Override public void onSMessage(String channel, String message) {
        sunsubscribe(channel);
      }

      @Override public void onSSubscribe(String channel, int subscribedChannels) {
        publishOne(channel, "exit");
      }

    }, "{foo}", "{foo}bar");
  }

  @Test
  public void pubSubChannels() {
    cluster.ssubscribe(new JedisShardedPubSub() {
      private int count = 0;

      @Override public void onSSubscribe(String channel, int subscribedChannels) {
        count++;
        // All channels are subscribed
        if (count == 3) {
          try (Connection conn = cluster.getConnectionFromSlot(JedisClusterCRC16.getSlot("testchan"));
              Jedis jedis = new Jedis(conn)) {
            assertThat(jedis.pubsubShardChannels(),
                hasItems("{testchan}1", "{testchan}2", "{testchan}3"));
          }
          sunsubscribe();
        }
      }
    }, "{testchan}1", "{testchan}2", "{testchan}3");
  }

  @Test
  public void pubSubChannelsWithPattern() {
    cluster.ssubscribe(new JedisShardedPubSub() {
      private int count = 0;

      @Override public void onSSubscribe(String channel, int subscribedChannels) {
        count++;
        // All channels are subscribed
        if (count == 3) {
          try (Connection conn = cluster.getConnectionFromSlot(JedisClusterCRC16.getSlot("testchan"));
              Jedis otherJedis = new Jedis(conn)) {
            assertThat(otherJedis.pubsubShardChannels("*testchan*"),
                hasItems("{testchan}1", "{testchan}2", "{testchan}3"));
          }
          sunsubscribe();
        }
      }
    }, "{testchan}1", "{testchan}2", "{testchan}3");
  }

  @Test
  public void pubSubNumSub() {
    final Map<String, Long> expectedNumSub = new HashMap<>();
    expectedNumSub.put("{testchannel}1", 1L);
    expectedNumSub.put("{testchannel}2", 1L);

    cluster.ssubscribe(new JedisShardedPubSub() {
      private int count = 0;

      @Override public void onSSubscribe(String channel, int subscribedChannels) {
        count++;
        if (count == 2) {
          try (Connection conn = cluster.getConnectionFromSlot(JedisClusterCRC16.getSlot("testchannel"));
              Jedis otherJedis = new Jedis(conn)) {
            Map<String, Long> numSub = otherJedis.pubsubShardNumSub("{testchannel}1", "{testchannel}2");
            assertEquals(expectedNumSub, numSub);
          }
          sunsubscribe();
        }
      }
    }, "{testchannel}1", "{testchannel}2");
  }

  @Test
  public void binarySubscribe() {
    cluster.ssubscribe(new BinaryJedisShardedPubSub() {
      @Override public void onSMessage(byte[] channel, byte[] message) {
        assertArrayEquals(SafeEncoder.encode("foo"), channel);
        assertArrayEquals(SafeEncoder.encode("exit"), message);
        sunsubscribe();
      }

      @Override public void onSSubscribe(byte[] channel, int subscribedChannels) {
        assertArrayEquals(SafeEncoder.encode("foo"), channel);
        assertEquals(1, subscribedChannels);
        publishOne(SafeEncoder.encode(channel), "exit");
      }

      @Override public void onSUnsubscribe(byte[] channel, int subscribedChannels) {
        assertArrayEquals(SafeEncoder.encode("foo"), channel);
        assertEquals(0, subscribedChannels);
      }
    }, SafeEncoder.encode("foo"));
  }

  @Test
  public void binarySubscribeMany() {
    cluster.ssubscribe(new BinaryJedisShardedPubSub() {
      @Override public void onSMessage(byte[] channel, byte[] message) {
        sunsubscribe(channel);
      }

      @Override public void onSSubscribe(byte[] channel, int subscribedChannels) {
        publishOne(SafeEncoder.encode(channel), "exit");
      }
    }, SafeEncoder.encode("{foo}"), SafeEncoder.encode("{foo}bar"));
  }

}
