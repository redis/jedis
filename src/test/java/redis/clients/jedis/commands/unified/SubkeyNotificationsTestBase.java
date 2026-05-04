package redis.clients.jedis.commands.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.PubSubHelpers;
import redis.clients.jedis.util.PubSubHelpers.CapturingPubSub;
import redis.clients.jedis.util.PubSubHelpers.Notification;

/**
 * Integration tests for the Redis 8.8 Subkey Notifications feature against {@link UnifiedJedis}.
 */
@Tag("integration")
public abstract class SubkeyNotificationsTestBase extends UnifiedJedisCommandsTestBase {

  static final String CHANNEL_PREFIX_SUBKEYSPACE = "__subkeyspace@0__:";
  static final String CHANNEL_PREFIX_SUBKEYEVENT = "__subkeyevent@0__:";
  static final String CHANNEL_PREFIX_SUBKEYSPACEITEM = "__subkeyspaceitem@0__:";
  static final String CHANNEL_PREFIX_SUBKEYSPACEEVENT = "__subkeyspaceevent@0__:";

  private String originalNotifyConfig;
  private UnifiedJedis subscriber;
  private CapturingPubSub pubSub;
  private Thread subscriberThread;

  public SubkeyNotificationsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @BeforeEach
  public void enableSubkeyNotifications() {
    try (Jedis control = new Jedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().build())) {
      Map<String, String> current = control.configGet("notify-keyspace-events");
      originalNotifyConfig = current.getOrDefault("notify-keyspace-events", "");
    }
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

  // -------------------------------------------------------------------- subkeyspace (flag S)

  @Test
  public void subkeyspace_singleHashField() throws InterruptedException {
    String hashKey = "subkeyspace-basic-" + System.nanoTime();
    String channel = CHANNEL_PREFIX_SUBKEYSPACE + hashKey;
    subscribeChannels(channel);

    jedis.hset(hashKey, "fld", "v1");

    Notification n = pubSub.expectMessageOn(channel);
    assertThat(n.pattern, nullValue());
    assertThat(n.message, equalTo("hset|3:fld"));
  }

  @Test
  public void subkeyspace_multipleFields() throws InterruptedException {
    String hashKey = "subkeyspace-multi-" + System.nanoTime();
    String channel = CHANNEL_PREFIX_SUBKEYSPACE + hashKey;
    subscribeChannels(channel);

    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("f1", "v1");
    fields.put("f22", "v2");
    jedis.hset(hashKey, fields);

    Notification n = pubSub.expectMessageOn(channel);
    assertThat(n.pattern, nullValue());
    assertThat(n.message, anyOf(equalTo("hset|2:f1,3:f22"), equalTo("hset|3:f22,2:f1")));
  }

  @Test
  public void subkeyspace_psubscribePrefix() throws InterruptedException {
    String prefix = "subkeyspace-pattern-" + System.nanoTime() + "-";
    String hashKey1 = prefix + "a";
    String hashKey2 = prefix + "b";
    String pattern = CHANNEL_PREFIX_SUBKEYSPACE + prefix + "*";
    subscribePatterns(pattern);

    jedis.hset(hashKey1, "fld", "v1");
    jedis.hset(hashKey2, "fld", "v2");

    Notification n1 = pubSub.expectMessageOn(CHANNEL_PREFIX_SUBKEYSPACE + hashKey1);
    Notification n2 = pubSub.expectMessageOn(CHANNEL_PREFIX_SUBKEYSPACE + hashKey2);
    assertThat(n1.pattern, equalTo(pattern));
    assertThat(n2.pattern, equalTo(pattern));
    assertThat(n1.message, equalTo("hset|3:fld"));
    assertThat(n2.message, equalTo("hset|3:fld"));
  }

  // -------------------------------------------------------------------- subkeyevent (flag T)

  @Test
  public void subkeyevent_singleHashField() throws InterruptedException {
    String hashKey = "subkeyevent-basic-" + System.nanoTime();
    String channel = CHANNEL_PREFIX_SUBKEYEVENT + "hset";
    subscribeChannels(channel);

    jedis.hset(hashKey, "fld", "v1");

    Notification n = pubSub.expectMessageOn(channel);
    assertThat(n.pattern, nullValue());
    assertThat(n.message, equalTo(hashKey.length() + ":" + hashKey + "|3:fld"));
  }

  @Test
  public void subkeyevent_multipleFields() throws InterruptedException {
    String hashKey = "subkeyevent-multi-" + System.nanoTime();
    String channel = CHANNEL_PREFIX_SUBKEYEVENT + "hset";
    subscribeChannels(channel);

    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("f1", "v1");
    fields.put("f22", "v2");
    jedis.hset(hashKey, fields);

    Notification n = pubSub.expectMessageOn(channel);
    assertThat(n.pattern, nullValue());
    String prefix = hashKey.length() + ":" + hashKey + "|";
    assertThat(n.message, anyOf(equalTo(prefix + "2:f1,3:f22"), equalTo(prefix + "3:f22,2:f1")));
  }

  // -------------------------------------------------------------------- subkeyspaceitem (flag I)

  @Test
  public void subkeyspaceitem_singleHashField() throws InterruptedException {
    String hashKey = "subkeyspaceitem-basic-" + System.nanoTime();
    String field = "fld";
    String channel = CHANNEL_PREFIX_SUBKEYSPACEITEM + hashKey + "\n" + field;
    subscribeChannels(channel);

    jedis.hset(hashKey, field, "v1");

    Notification n = pubSub.expectMessageOn(channel);
    assertThat(n.pattern, nullValue());
    assertThat(n.message, equalTo("hset"));
  }

  @Test
  public void subkeyspaceitem_filtersToTargetFieldOnly() throws InterruptedException {
    String hashKey = "subkeyspaceitem-filter-" + System.nanoTime();
    String targetChannel = CHANNEL_PREFIX_SUBKEYSPACEITEM + hashKey + "\nf2";
    subscribeChannels(targetChannel);

    jedis.hset(hashKey, "f1", "v1");
    jedis.hset(hashKey, "f2", "v2");
    jedis.hset(hashKey, "f3", "v3");

    Notification n = pubSub.expectMessageOn(targetChannel);
    assertThat(n.message, equalTo("hset"));
    assertThat(n.pattern, nullValue());

    pubSub.expectNoMessageOn(targetChannel, 250, TimeUnit.MILLISECONDS);
  }

  @Test
  public void subkeyspaceitem_multipleFieldsEmitOneEventPerField() throws InterruptedException {
    String hashKey = "subkeyspaceitem-multi-" + System.nanoTime();
    String pattern = CHANNEL_PREFIX_SUBKEYSPACEITEM + hashKey + "\n*";
    String channelF1 = CHANNEL_PREFIX_SUBKEYSPACEITEM + hashKey + "\nf1";
    String channelF22 = CHANNEL_PREFIX_SUBKEYSPACEITEM + hashKey + "\nf22";
    subscribePatterns(pattern);

    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("f1", "v1");
    fields.put("f22", "v2");
    jedis.hset(hashKey, fields);

    Notification n1 = pubSub.expectMessageOn(channelF1);
    Notification n2 = pubSub.expectMessageOn(channelF22);
    assertThat(n1.message, equalTo("hset"));
    assertThat(n2.message, equalTo("hset"));
    assertThat(n1.pattern, equalTo(pattern));
    assertThat(n2.pattern, equalTo(pattern));
  }

  // -------------------------------------------------------------------- subkeyspaceevent (flag V)

  @Test
  public void subkeyspaceevent_singleHashField() throws InterruptedException {
    String hashKey = "subkeyspaceevent-basic-" + System.nanoTime();
    String channel = CHANNEL_PREFIX_SUBKEYSPACEEVENT + "hset|" + hashKey;
    subscribeChannels(channel);

    jedis.hset(hashKey, "fld", "v1");

    Notification n = pubSub.expectMessageOn(channel);
    assertThat(n.pattern, nullValue());
    assertThat(n.message, equalTo("3:fld"));
  }

  @Test
  public void subkeyspaceevent_multipleFields() throws InterruptedException {
    String hashKey = "subkeyspaceevent-multi-" + System.nanoTime();
    String channel = CHANNEL_PREFIX_SUBKEYSPACEEVENT + "hset|" + hashKey;
    subscribeChannels(channel);

    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("f1", "v1");
    fields.put("f22", "v2");
    jedis.hset(hashKey, fields);

    Notification n = pubSub.expectMessageOn(channel);
    assertThat(n.pattern, nullValue());
    assertThat(n.message, anyOf(equalTo("2:f1,3:f22"), equalTo("3:f22,2:f1")));
  }

  // -------------------------------------------------------------------- helpers

  private void subscribeChannels(String... channels) throws InterruptedException {
    pubSub = new CapturingPubSub();
    subscriber = createTestClient();
    subscriberThread = new Thread(() -> subscriber.subscribe(pubSub, channels),
        "subkey-uj-sub-" + System.nanoTime());
    subscriberThread.setDaemon(true);
    subscriberThread.start();
    PubSubHelpers.awaitSubscribed(pubSub.subscribed);
  }

  private void subscribePatterns(String... patterns) throws InterruptedException {
    pubSub = new CapturingPubSub();
    subscriber = createTestClient();
    subscriberThread = new Thread(() -> subscriber.psubscribe(pubSub, patterns),
        "subkey-uj-psub-" + System.nanoTime());
    subscriberThread.setDaemon(true);
    subscriberThread.start();
    PubSubHelpers.awaitSubscribed(pubSub.subscribed);
  }
}
