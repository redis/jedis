package redis.clients.jedis.commands.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static redis.clients.jedis.util.PubSubHelpers.concat;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.PubSubHelpers;
import redis.clients.jedis.util.PubSubHelpers.CapturingBinaryPubSub;

/** Binary integration tests for the Redis 8.8 Subkey Notifications feature. */
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
@Tag("integration")
public class SubkeyNotificationsBinaryIntegrationTest extends JedisCommandsTestBase {

  private String originalNotifyConfig;
  private Jedis subscriber;
  private CapturingBinaryPubSub pubSub;
  private Thread subscriberThread;

  public SubkeyNotificationsBinaryIntegrationTest(RedisProtocol protocol) {
    super(protocol);
  }

  @BeforeEach
  public void enableSubkeyNotifications() {
    Map<String, String> current = jedis.configGet("notify-keyspace-events");
    originalNotifyConfig = current.getOrDefault("notify-keyspace-events", "");
    try {
      jedis.configSet("notify-keyspace-events", "AKEhSTIV");
    } catch (JedisDataException e) {
      Assumptions
          .abort("Server does not support subkey notification flags (STIV): " + e.getMessage());
    }
  }

  @AfterEach
  public void closeSubscriber() {
    if (pubSub != null) {
      try {
        pubSub.unsubscribe();
      } catch (Exception ignore) {
        /* best-effort */ }
      try {
        pubSub.punsubscribe();
      } catch (Exception ignore) {
        /* best-effort */ }
    }
    if (subscriberThread != null) {
      try {
        subscriberThread.join(2_000L);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    if (subscriber != null) {
      try {
        subscriber.close();
      } catch (Exception ignore) {
        /* best-effort */ }
    }
    if (jedis != null && originalNotifyConfig != null) {
      try {
        jedis.configSet("notify-keyspace-events", originalNotifyConfig);
      } catch (Exception ignore) {
        /* best-effort */ }
    }
  }

  @Test
  public void subkeyspace_subkeyContainingPipeAndNonUtf8() throws InterruptedException {
    byte[] hashKey = ("bin-subkeyspace-" + System.nanoTime()).getBytes(StandardCharsets.UTF_8);
    byte[] field = new byte[] { 'a', '|', 'b', (byte) 0xFE, 'c' };
    byte[] channel = concat(prefix("__subkeyspace@0__:"), hashKey);
    subscribeChannels(channel);

    jedis.hset(hashKey, field, "v1".getBytes(StandardCharsets.UTF_8));

    byte[] payload = pubSub.expectMessageOn(channel);
    byte[] expected = concat("hset|".getBytes(StandardCharsets.UTF_8),
      (field.length + ":").getBytes(StandardCharsets.UTF_8), field);
    assertThat(payload, equalTo(expected));
  }

  @Test
  public void subkeyevent_keyAndSubkeyContainingPipe() throws InterruptedException {
    byte[] hashKey = new byte[] { 'h', '|', 'k', (byte) 0x80 };
    byte[] field = new byte[] { 'f', '|', 'd' };
    byte[] channel = "__subkeyevent@0__:hset".getBytes(StandardCharsets.UTF_8);
    subscribeChannels(channel);

    jedis.hset(hashKey, field, "v1".getBytes(StandardCharsets.UTF_8));

    byte[] payload = pubSub.expectMessageOn(channel);
    byte[] expected = concat((hashKey.length + ":").getBytes(StandardCharsets.UTF_8), hashKey,
      "|".getBytes(StandardCharsets.UTF_8), (field.length + ":").getBytes(StandardCharsets.UTF_8),
      field);
    assertThat(payload, equalTo(expected));
  }

  @Test
  public void subkeyspaceitem_channelContainsBinaryBytes() throws InterruptedException {
    byte[] hashKey = new byte[] { 'i', (byte) 0xC3, (byte) 0xA9 };
    byte[] field = new byte[] { 'f', (byte) 0x80, 'd' };
    byte[] pattern = concat(prefix("__subkeyspaceitem@0__:"), hashKey,
      "\n*".getBytes(StandardCharsets.UTF_8));
    byte[] expectedChannel = concat(prefix("__subkeyspaceitem@0__:"), hashKey,
      "\n".getBytes(StandardCharsets.UTF_8), field);
    subscribePatterns(pattern);

    jedis.hset(hashKey, field, "v1".getBytes(StandardCharsets.UTF_8));

    byte[] payload = pubSub.expectMessageOn(expectedChannel);
    assertThat(payload, equalTo("hset".getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  public void subkeyspaceevent_keyContainingPipeInChannel() throws InterruptedException {
    byte[] hashKey = new byte[] { 'k', '|', 'k', (byte) 0xFF };
    byte[] field = new byte[] { 'f', 'l', 'd' };
    byte[] channel = concat("__subkeyspaceevent@0__:hset|".getBytes(StandardCharsets.UTF_8),
      hashKey);
    subscribeChannels(channel);

    jedis.hset(hashKey, field, "v1".getBytes(StandardCharsets.UTF_8));

    byte[] payload = pubSub.expectMessageOn(channel);
    byte[] expected = concat((field.length + ":").getBytes(StandardCharsets.UTF_8), field);
    assertThat(payload, equalTo(expected));
  }

  @Test
  public void subkeyevent_keyAndSubkeyContainingNewline() throws InterruptedException {
    byte[] hashKey = new byte[] { 'h', '\n', 'k' };
    byte[] field = new byte[] { 'f', '\n', 'd' };
    byte[] channel = "__subkeyevent@0__:hset".getBytes(StandardCharsets.UTF_8);
    subscribeChannels(channel);

    jedis.hset(hashKey, field, "v1".getBytes(StandardCharsets.UTF_8));

    byte[] payload = pubSub.expectMessageOn(channel);
    byte[] expected = concat((hashKey.length + ":").getBytes(StandardCharsets.UTF_8), hashKey,
      "|".getBytes(StandardCharsets.UTF_8), (field.length + ":").getBytes(StandardCharsets.UTF_8),
      field);
    assertThat(payload, equalTo(expected));
  }

  // -------------------------------------------------------------------- helpers

  private void subscribeChannels(byte[]... channels) throws InterruptedException {
    pubSub = new CapturingBinaryPubSub();
    subscriber = createJedis();
    subscriberThread = new Thread(() -> subscriber.subscribe(pubSub, channels),
        "subkey-bin-sub-" + System.nanoTime());
    subscriberThread.setDaemon(true);
    subscriberThread.start();
    PubSubHelpers.awaitSubscribed(pubSub.subscribed);
  }

  private void subscribePatterns(byte[]... patterns) throws InterruptedException {
    pubSub = new CapturingBinaryPubSub();
    subscriber = createJedis();
    subscriberThread = new Thread(() -> subscriber.psubscribe(pubSub, patterns),
        "subkey-bin-psub-" + System.nanoTime());
    subscriberThread.setDaemon(true);
    subscriberThread.start();
    PubSubHelpers.awaitSubscribed(pubSub.subscribed);
  }

  private static byte[] prefix(String s) {
    return s.getBytes(StandardCharsets.UTF_8);
  }
}
