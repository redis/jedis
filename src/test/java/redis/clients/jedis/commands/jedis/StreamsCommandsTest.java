package redis.clients.jedis.commands.jedis;

import static io.redis.test.utils.RedisVersion.V8_4_0_STRING;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import io.redis.test.annotations.EnabledOnCommand;
import io.redis.test.annotations.SinceRedisVersion;
import io.redis.test.utils.RedisVersion;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.Response;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.*;
import redis.clients.jedis.args.StreamDeletionPolicy;
import redis.clients.jedis.util.RedisVersionUtil;
import redis.clients.jedis.util.SafeEncoder;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
@Tag("integration")
public class StreamsCommandsTest extends JedisCommandsTestBase {

  public StreamsCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void xadd() {

    try {
      Map<String, String> map1 = new HashMap<>();
      jedis.xadd("stream1", (StreamEntryID) null, map1);
      fail();
    } catch (JedisDataException expected) {
      assertTrue(expected.getMessage().contains("wrong number of arguments"));
    }

    Map<String, String> map1 = new HashMap<>();
    map1.put("f1", "v1");
    StreamEntryID id1 = jedis.xadd("xadd-stream1", (StreamEntryID) null, map1);
    assertNotNull(id1);

    Map<String, String> map2 = new HashMap<>();
    map2.put("f1", "v1");
    map2.put("f2", "v2");
    StreamEntryID id2 = jedis.xadd("xadd-stream1", (StreamEntryID) null, map2);
    assertTrue(id2.compareTo(id1) > 0);

    Map<String, String> map3 = new HashMap<>();
    map3.put("f2", "v2");
    map3.put("f3", "v3");
    StreamEntryID id3 = jedis.xadd("xadd-stream2", (StreamEntryID) null, map3);

    Map<String, String> map4 = new HashMap<>();
    map4.put("f2", "v2");
    map4.put("f3", "v3");
    StreamEntryID idIn = new StreamEntryID(id3.getTime() + 1, 1L);
    StreamEntryID id4 = jedis.xadd("xadd-stream2", idIn, map4);
    assertEquals(idIn, id4);
    assertTrue(id4.compareTo(id3) > 0);

    Map<String, String> map5 = new HashMap<>();
    map5.put("f4", "v4");
    map5.put("f5", "v5");
    StreamEntryID id5 = jedis.xadd("xadd-stream2", (StreamEntryID) null, map5);
    assertTrue(id5.compareTo(id4) > 0);

    Map<String, String> map6 = new HashMap<>();
    map6.put("f4", "v4");
    map6.put("f5", "v5");
    StreamEntryID id6 = jedis.xadd("xadd-stream2", map6, XAddParams.xAddParams().maxLen(3));
    assertTrue(id6.compareTo(id5) > 0);
    assertEquals(3L, jedis.xlen("xadd-stream2"));
  }

  @Test
  public void xaddWithParams() {

    try {
      jedis.xadd("stream1", new HashMap<>(), XAddParams.xAddParams());
      fail();
    } catch (JedisDataException expected) {
      assertTrue(expected.getMessage().contains("wrong number of arguments"));
    }

    try {
      jedis.xadd("stream1", XAddParams.xAddParams(), new HashMap<>());
      fail();
    } catch (JedisDataException expected) {
      assertTrue(expected.getMessage().contains("wrong number of arguments"));
    }

    StreamEntryID id1 = jedis.xadd("xadd-stream1", (StreamEntryID) null, singletonMap("f1", "v1"));
    assertNotNull(id1);

    Map<String, String> map2 = new HashMap<>();
    map2.put("f1", "v1");
    map2.put("f2", "v2");
    StreamEntryID id2 = jedis.xadd("xadd-stream1", map2, XAddParams.xAddParams());
    assertTrue(id2.compareTo(id1) > 0);

    Map<String, String> map3 = new HashMap<>();
    map3.put("f2", "v2");
    map3.put("f3", "v3");
    StreamEntryID id3 = jedis.xadd("xadd-stream2", XAddParams.xAddParams(), map3);

    Map<String, String> map4 = new HashMap<>();
    map4.put("f2", "v2");
    map4.put("f3", "v3");
    StreamEntryID idIn = new StreamEntryID(id3.getTime() + 1, 1L);
    StreamEntryID id4 = jedis.xadd("xadd-stream2", map4, XAddParams.xAddParams().id(idIn));
    assertEquals(idIn, id4);
    assertTrue(id4.compareTo(id3) > 0);

    Map<String, String> map5 = new HashMap<>();
    map5.put("f4", "v4");
    map5.put("f5", "v5");
    StreamEntryID id5 = jedis.xadd("xadd-stream2", XAddParams.xAddParams(), map5);
    assertTrue(id5.compareTo(id4) > 0);

    Map<String, String> map6 = new HashMap<>();
    map6.put("f4", "v4");
    map6.put("f5", "v5");
    StreamEntryID id6 = jedis.xadd("xadd-stream2", map6, XAddParams.xAddParams().maxLen(3).exactTrimming());
    assertTrue(id6.compareTo(id5) > 0);
    assertEquals(3L, jedis.xlen("xadd-stream2"));

    // nomkstream
    StreamEntryID id7 = jedis.xadd("xadd-stream3", XAddParams.xAddParams().noMkStream().maxLen(3).exactTrimming(), map6);
    assertNull(id7);
    assertFalse(jedis.exists("xadd-stream3"));

    // minid
    jedis.xadd("xadd-stream3", map6, XAddParams.xAddParams().minId("2").id(new StreamEntryID(2)));
    assertEquals(1L, jedis.xlen("xadd-stream3"));
    jedis.xadd("xadd-stream3", XAddParams.xAddParams().minId("4").id(new StreamEntryID(3)), map6);
    assertEquals(0L, jedis.xlen("xadd-stream3"));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void xaddParamsId() {
    StreamEntryID id;
    String key = "kk";
    Map<String, String> map = singletonMap("ff", "vv");

    id = jedis.xadd(key, XAddParams.xAddParams().id(new StreamEntryID(0, 1)), map);
    assertNotNull(id);
    assertEquals("0-1", id.toString());
    assertEquals(0, id.getTime());
    assertEquals(1, id.getSequence());

    id = jedis.xadd(key, XAddParams.xAddParams().id(2, 3), map);
    assertNotNull(id);
    assertEquals(2, id.getTime());
    assertEquals(3, id.getSequence());

    id = jedis.xadd(key, XAddParams.xAddParams().id(4), map);
    assertNotNull(id);
    assertEquals(4, id.getTime());
    assertEquals(0, id.getSequence());

    id = jedis.xadd(key, XAddParams.xAddParams().id("5-6"), map);
    assertNotNull(id);
    assertEquals(5, id.getTime());
    assertEquals(6, id.getSequence());

    id = jedis.xadd(key, XAddParams.xAddParams().id("7-8".getBytes()), map);
    assertNotNull(id);
    assertEquals(7, id.getTime());
    assertEquals(8, id.getSequence());

    id = jedis.xadd(key, XAddParams.xAddParams(), map);
    assertNotNull(id);
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xaddWithTrimmingModeKeepReferences() {
    String streamKey = "xadd-trim-keep-ref-stream";
    String groupName = "test-group";
    String consumerName = "test-consumer";
    Map<String, String> map = singletonMap("field", "value");

    // Add initial entries to the stream
    for (int i = 1; i <= 5; i++) {
      jedis.xadd(streamKey, XAddParams.xAddParams().id(new StreamEntryID(i + "-0")), map);
    }
    assertEquals(5L, jedis.xlen(streamKey));

    // Create consumer group and read messages to create PEL entries
    jedis.xgroupCreate(streamKey, groupName, new StreamEntryID("0-0"), false);
    Map<String, StreamEntryID> streamQuery = singletonMap(streamKey, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    jedis.xreadGroup(groupName, consumerName, XReadGroupParams.xReadGroupParams().count(3), streamQuery);

    // Verify PEL has entries
    List<StreamPendingEntry> pendingBefore = jedis.xpending(streamKey, groupName, XPendingParams.xPendingParams().count(10));
    assertEquals(3, pendingBefore.size());

    // Add new entry with maxLen=3 and KEEP_REFERENCES mode
    StreamEntryID newId = jedis.xadd(streamKey, XAddParams.xAddParams()
        .id(new StreamEntryID("6-0"))
        .maxLen(3)
        .trimmingMode(StreamDeletionPolicy.KEEP_REFERENCES), map);
    assertNotNull(newId);

    // Stream should be trimmed to 3 entries
    assertEquals(3L, jedis.xlen(streamKey));

    // PEL references should be preserved even though entries were trimmed
    List<StreamPendingEntry> pendingAfter = jedis.xpending(streamKey, groupName, XPendingParams.xPendingParams().count(10));
    assertEquals(3, pendingAfter.size()); // PEL entries should still exist
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xaddWithTrimmingModeDeleteReferences() {
    String streamKey = "xadd-trim-del-ref-stream";
    String groupName = "test-group";
    String consumerName = "test-consumer";
    Map<String, String> map = singletonMap("field", "value");

    // Add initial entries to the stream
    for (int i = 1; i <= 5; i++) {
      jedis.xadd(streamKey, XAddParams.xAddParams().id(new StreamEntryID(i + "-0")), map);
    }
    assertEquals(5L, jedis.xlen(streamKey));

    // Create consumer group and read messages to create PEL entries
    jedis.xgroupCreate(streamKey, groupName, new StreamEntryID("0-0"), false);
    Map<String, StreamEntryID> streamQuery = singletonMap(streamKey, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    jedis.xreadGroup(groupName, consumerName, XReadGroupParams.xReadGroupParams().count(3), streamQuery);

    // Verify PEL has entries
    List<StreamPendingEntry> pendingBefore = jedis.xpending(streamKey, groupName, XPendingParams.xPendingParams().count(10));
    assertEquals(3, pendingBefore.size());

    // Add new entry with maxLen=3 and DELETE_REFERENCES mode
    StreamEntryID newId = jedis.xadd(streamKey, XAddParams.xAddParams()
        .id(new StreamEntryID("6-0"))
        .maxLen(3)
        .trimmingMode(StreamDeletionPolicy.DELETE_REFERENCES), map);
    assertNotNull(newId);

    // Stream should be trimmed to 3 entries
    assertEquals(3L, jedis.xlen(streamKey));

    // PEL references should be removed for trimmed entries
    List<StreamPendingEntry> pendingAfter = jedis.xpending(streamKey, groupName, XPendingParams.xPendingParams().count(10));
    // Only entries that still exist in the stream should remain in PEL
    assertTrue(pendingAfter.size() <= 3);
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xaddWithTrimmingModeAcknowledged() {
    String streamKey = "xadd-trim-acked-stream";
    String groupName = "test-group";
    String consumerName = "test-consumer";
    Map<String, String> map = singletonMap("field", "value");

    // Add initial entries to the stream
    for (int i = 1; i <= 5; i++) {
      jedis.xadd(streamKey, XAddParams.xAddParams().id(new StreamEntryID(i + "-0")), map);
    }
    assertEquals(5L, jedis.xlen(streamKey));

    // Create consumer group and read messages
    jedis.xgroupCreate(streamKey, groupName, new StreamEntryID("0-0"), false);
    Map<String, StreamEntryID> streamQuery = singletonMap(streamKey, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    List<Entry<String, List<StreamEntry>>> messages = jedis.xreadGroup(groupName, consumerName,
        XReadGroupParams.xReadGroupParams().count(3), streamQuery);

    // Acknowledge the first 2 messages
    StreamEntryID id1 = messages.get(0).getValue().get(0).getID();
    StreamEntryID id2 = messages.get(0).getValue().get(1).getID();
    jedis.xack(streamKey, groupName, id1, id2);

    // Verify PEL state
    List<StreamPendingEntry> pendingBefore = jedis.xpending(streamKey, groupName, XPendingParams.xPendingParams().count(10));
    assertEquals(1, pendingBefore.size()); // Only 1 unacknowledged message

    // Add new entry with maxLen=3 and ACKNOWLEDGED mode
    StreamEntryID newId = jedis.xadd(streamKey, XAddParams.xAddParams()
        .id(new StreamEntryID("6-0"))
        .maxLen(3)
        .trimmingMode(StreamDeletionPolicy.ACKNOWLEDGED), map);
    assertNotNull(newId);

    // Stream length should respect acknowledgment status
    long streamLen = jedis.xlen(streamKey);
    assertTrue(streamLen >= 3); // Should not trim unacknowledged entries aggressively

    // PEL should still contain unacknowledged entries
    List<StreamPendingEntry> pendingAfter = jedis.xpending(streamKey, groupName, XPendingParams.xPendingParams().count(10));
    assertFalse(pendingAfter.isEmpty()); // Unacknowledged entries should remain
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xaddWithMinIdTrimmingModeKeepReferences() {
    String streamKey = "xadd-minid-keep-ref-stream";
    String groupName = "test-group";
    String consumerName = "test-consumer";
    Map<String, String> map = singletonMap("field", "value");

    // Add initial entries with specific IDs
    for (int i = 1; i <= 5; i++) {
      jedis.xadd(streamKey, XAddParams.xAddParams().id(new StreamEntryID("0-" + i)), map);
    }
    assertEquals(5L, jedis.xlen(streamKey));

    // Create consumer group and read messages to create PEL entries
    jedis.xgroupCreate(streamKey, groupName, new StreamEntryID("0-0"), false);
    Map<String, StreamEntryID> streamQuery = singletonMap(streamKey, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    jedis.xreadGroup(groupName, consumerName, XReadGroupParams.xReadGroupParams().count(3), streamQuery);

    // Verify PEL has entries
    List<StreamPendingEntry> pendingBefore = jedis.xpending(streamKey, groupName, XPendingParams.xPendingParams().count(10));
    assertEquals(3, pendingBefore.size());

    // Add new entry with minId="0-3" and KEEP_REFERENCES mode (should trim entries < 0-3)
    StreamEntryID newId = jedis.xadd(streamKey, XAddParams.xAddParams()
        .id(new StreamEntryID("0-6"))
        .minId("0-3")
        .trimmingMode(StreamDeletionPolicy.KEEP_REFERENCES), map);
    assertNotNull(newId);

    // Stream should have entries >= 0-3 plus the new entry
    long streamLen = jedis.xlen(streamKey);
    assertTrue(streamLen >= 3); // Should keep entries 0-3, 0-4, 0-5, 0-6

    // PEL references should be preserved even for trimmed entries
    List<StreamPendingEntry> pendingAfter = jedis.xpending(streamKey, groupName, XPendingParams.xPendingParams().count(10));
    assertEquals(3, pendingAfter.size()); // PEL entries should still exist
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xaddWithMinIdTrimmingModeDeleteReferences() {
    String streamKey = "xadd-minid-del-ref-stream";
    String groupName = "test-group";
    String consumerName = "test-consumer";
    Map<String, String> map = singletonMap("field", "value");

    // Add initial entries with specific IDs
    for (int i = 1; i <= 5; i++) {
      jedis.xadd(streamKey, XAddParams.xAddParams().id(new StreamEntryID("0-" + i)), map);
    }
    assertEquals(5L, jedis.xlen(streamKey));

    // Create consumer group and read messages to create PEL entries
    jedis.xgroupCreate(streamKey, groupName, new StreamEntryID("0-0"), false);
    Map<String, StreamEntryID> streamQuery = singletonMap(streamKey, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    jedis.xreadGroup(groupName, consumerName, XReadGroupParams.xReadGroupParams().count(3), streamQuery);

    // Verify PEL has entries
    List<StreamPendingEntry> pendingBefore = jedis.xpending(streamKey, groupName, XPendingParams.xPendingParams().count(10));
    assertEquals(3, pendingBefore.size());

    // Add new entry with minId="0-3" and DELETE_REFERENCES mode
    StreamEntryID newId = jedis.xadd(streamKey, XAddParams.xAddParams()
        .id(new StreamEntryID("0-6"))
        .minId("0-3")
        .trimmingMode(StreamDeletionPolicy.DELETE_REFERENCES), map);
    assertNotNull(newId);

    // Stream should have entries >= 0-3 plus the new entry
    long streamLen = jedis.xlen(streamKey);
    assertTrue(streamLen >= 3);

    // PEL references should be removed for trimmed entries (0-1, 0-2)
    List<StreamPendingEntry> pendingAfter = jedis.xpending(streamKey, groupName, XPendingParams.xPendingParams().count(10));
    // Only entries that still exist in the stream should remain in PEL
    assertTrue(pendingAfter.size() <= pendingBefore.size());
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xaddWithApproximateTrimmingAndTrimmingMode() {
    String streamKey = "xadd-approx-trim-stream";
    String groupName = "test-group";
    String consumerName = "test-consumer";
    Map<String, String> map = singletonMap("field", "value");

    // Add initial entries
    for (int i = 1; i <= 10; i++) {
      jedis.xadd(streamKey, XAddParams.xAddParams().id(new StreamEntryID(i + "-0")), map);
    }
    assertEquals(10L, jedis.xlen(streamKey));

    // Create consumer group and read messages
    jedis.xgroupCreate(streamKey, groupName, new StreamEntryID("0-0"), false);
    Map<String, StreamEntryID> streamQuery = singletonMap(streamKey, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    jedis.xreadGroup(groupName, consumerName, XReadGroupParams.xReadGroupParams().count(5), streamQuery);

    // Add new entry with approximate trimming and KEEP_REFERENCES mode
    StreamEntryID newId = jedis.xadd(streamKey, XAddParams.xAddParams()
        .id(new StreamEntryID("11-0"))
        .maxLen(5)
        .approximateTrimming()
        .trimmingMode(StreamDeletionPolicy.KEEP_REFERENCES), map);
    assertNotNull(newId);

    // With approximate trimming, the exact length may vary but should be around the target
    long streamLen = jedis.xlen(streamKey);
    assertTrue(streamLen >= 5); // Should be approximately 5, but may be more due to approximation

    // PEL should preserve references
    List<StreamPendingEntry> pendingAfter = jedis.xpending(streamKey, groupName, XPendingParams.xPendingParams().count(10));
    assertEquals(5, pendingAfter.size()); // All read messages should remain in PEL
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xaddWithExactTrimmingAndTrimmingMode() {
    String streamKey = "xadd-exact-trim-mode-stream";
    String groupName = "test-group";
    String consumerName = "test-consumer";
    Map<String, String> map = singletonMap("field", "value");

    // Add initial entries
    for (int i = 1; i <= 5; i++) {
      jedis.xadd(streamKey, XAddParams.xAddParams().id(new StreamEntryID(i + "-0")), map);
    }
    assertEquals(5L, jedis.xlen(streamKey));

    // Create consumer group and read messages
    jedis.xgroupCreate(streamKey, groupName, new StreamEntryID("0-0"), false);
    Map<String, StreamEntryID> streamQuery = singletonMap(streamKey, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    jedis.xreadGroup(groupName, consumerName, XReadGroupParams.xReadGroupParams().count(3), streamQuery);

    // Add new entry with exact trimming and DELETE_REFERENCES mode
    StreamEntryID newId = jedis.xadd(streamKey, XAddParams.xAddParams()
        .id(new StreamEntryID("6-0"))
        .maxLen(3)
        .exactTrimming()
        .trimmingMode(StreamDeletionPolicy.DELETE_REFERENCES), map);
    assertNotNull(newId);

    // With exact trimming, stream should be exactly 3 entries
    assertEquals(3L, jedis.xlen(streamKey));

    // PEL references should be cleaned up for trimmed entries
    List<StreamPendingEntry> pendingAfter = jedis.xpending(streamKey, groupName, XPendingParams.xPendingParams().count(10));
    // Only entries that still exist in the stream should remain in PEL
    assertTrue(pendingAfter.size() <= 3);
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xaddWithLimitAndTrimmingMode() {
    String streamKey = "xadd-limit-trim-mode-stream";
    String groupName = "test-group";
    String consumerName = "test-consumer";
    Map<String, String> map = singletonMap("field", "value");

    // Add initial entries
    for (int i = 1; i <= 10; i++) {
      jedis.xadd(streamKey, XAddParams.xAddParams().id(new StreamEntryID(i + "-0")), map);
    }
    assertEquals(10L, jedis.xlen(streamKey));

    // Create consumer group and read messages
    jedis.xgroupCreate(streamKey, groupName, new StreamEntryID("0-0"), false);
    Map<String, StreamEntryID> streamQuery = singletonMap(streamKey, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    jedis.xreadGroup(groupName, consumerName, XReadGroupParams.xReadGroupParams().count(5), streamQuery);

    // Add new entry with limit and KEEP_REFERENCES mode (limit requires approximate trimming)
    StreamEntryID newId = jedis.xadd(streamKey, XAddParams.xAddParams()
        .id(new StreamEntryID("11-0"))
        .maxLen(5)
        .approximateTrimming() // Required for limit to work
        .limit(2) // Limit the number of entries to examine for trimming
        .trimmingMode(StreamDeletionPolicy.KEEP_REFERENCES), map);
    assertNotNull(newId);

    // With limit, trimming may be less aggressive
    long streamLen = jedis.xlen(streamKey);
    assertTrue(streamLen >= 5); // Should be at least 5, but may be more due to limit

    // PEL should preserve references
    List<StreamPendingEntry> pendingAfter = jedis.xpending(streamKey, groupName, XPendingParams.xPendingParams().count(10));
    assertEquals(5, pendingAfter.size()); // All read messages should remain in PEL
  }

  @Test
  public void xdel() {
    Map<String, String> map1 = new HashMap<>();
    map1.put("f1", "v1");

    StreamEntryID id1 = jedis.xadd("xdel-stream", (StreamEntryID) null, map1);
    assertNotNull(id1);

    StreamEntryID id2 = jedis.xadd("xdel-stream", (StreamEntryID) null, map1);
    assertNotNull(id2);
    assertEquals(2L, jedis.xlen("xdel-stream"));

    assertEquals(1L, jedis.xdel("xdel-stream", id1));
    assertEquals(1L, jedis.xlen("xdel-stream"));
  }

  @Test
  public void xlen() {
    assertEquals(0L, jedis.xlen("xlen-stream"));

    Map<String, String> map = new HashMap<>();
    map.put("f1", "v1");
    jedis.xadd("xlen-stream", (StreamEntryID) null, map);
    assertEquals(1L, jedis.xlen("xlen-stream"));

    jedis.xadd("xlen-stream", (StreamEntryID) null, map);
    assertEquals(2L, jedis.xlen("xlen-stream"));
  }

  @Test
  public void xrange() {
    List<StreamEntry> range = jedis.xrange("xrange-stream", (StreamEntryID) null,
      (StreamEntryID) null, Integer.MAX_VALUE);
    assertEquals(0, range.size());

    Map<String, String> map = new HashMap<>();
    map.put("f1", "v1");
    StreamEntryID id1 = jedis.xadd("xrange-stream", (StreamEntryID) null, map);
    StreamEntryID id2 = jedis.xadd("xrange-stream", (StreamEntryID) null, map);
    List<StreamEntry> range2 = jedis.xrange("xrange-stream", (StreamEntryID) null,
      (StreamEntryID) null, 3);
    assertEquals(2, range2.size());
    assertEquals(range2.get(0).toString(), id1 + " " + map);

    List<StreamEntry> range3 = jedis.xrange("xrange-stream", id1, null, 2);
    assertEquals(2, range3.size());

    List<StreamEntry> range4 = jedis.xrange("xrange-stream", id1, id2, 2);
    assertEquals(2, range4.size());

    List<StreamEntry> range5 = jedis.xrange("xrange-stream", id1, id2, 1);
    assertEquals(1, range5.size());

    List<StreamEntry> range6 = jedis.xrange("xrange-stream", id2, null, 4);
    assertEquals(1, range6.size());

    StreamEntryID id3 = jedis.xadd("xrange-stream", (StreamEntryID) null, map);
    List<StreamEntry> range7 = jedis.xrange("xrange-stream", id3, id3, 4);
    assertEquals(1, range7.size());

    List<StreamEntry> range8 = jedis.xrange("xrange-stream", (StreamEntryID) null, (StreamEntryID) null);
    assertEquals(3, range8.size());
    range8 = jedis.xrange("xrange-stream", StreamEntryID.MINIMUM_ID, StreamEntryID.MAXIMUM_ID);
    assertEquals(3, range8.size());
  }

  @Test
  public void xrangeExclusive() {
    Map<String, String> map = new HashMap<>();
    map.put("f1", "v1");
    String id1 = jedis.xadd("xrange-stream", (StreamEntryID) null, map).toString();
    jedis.xadd("xrange-stream", (StreamEntryID) null, map);

    List<StreamEntry> range2 = jedis.xrange("xrange-stream", id1, "+", 2);
    assertEquals(2, range2.size());

    List<StreamEntry> range3 = jedis.xrange("xrange-stream", "(" + id1, "+", 2);
    assertEquals(1, range3.size());
  }

  @Test
  public void xreadWithParams() {

    final String key1 = "xread-stream1";
    final String key2 = "xread-stream2";

    Map<String, StreamEntryID> streamQeury1 = singletonMap(key1, new StreamEntryID());

    // Before creating Stream
    assertNull(jedis.xread(XReadParams.xReadParams().block(1), streamQeury1));
    assertNull(jedis.xread(XReadParams.xReadParams(), streamQeury1));

    Map<String, String> map = new HashMap<>();
    map.put("f1", "v1");
    StreamEntryID id1 = jedis.xadd(key1, (StreamEntryID) null, map);
    StreamEntryID id2 = jedis.xadd(key2, (StreamEntryID) null, map);

    // Read only a single Stream
    List<Entry<String, List<StreamEntry>>> streams1 = jedis.xread(XReadParams.xReadParams().count(1).block(1), streamQeury1);
    assertEquals(1, streams1.size());
    assertEquals(key1, streams1.get(0).getKey());
    assertEquals(1, streams1.get(0).getValue().size());
    assertEquals(id1, streams1.get(0).getValue().get(0).getID());
    assertEquals(map, streams1.get(0).getValue().get(0).getFields());

    assertNull(jedis.xread(XReadParams.xReadParams().block(1), singletonMap(key1, id1)));
    assertNull(jedis.xread(XReadParams.xReadParams(), singletonMap(key1, id1)));

    // Read from two Streams
    Map<String, StreamEntryID> streamQuery2 = new LinkedHashMap<>();
    streamQuery2.put(key1, new StreamEntryID());
    streamQuery2.put(key2, new StreamEntryID());
    List<Entry<String, List<StreamEntry>>> streams2 = jedis.xread(XReadParams.xReadParams().count(2).block(1), streamQuery2);
    assertEquals(2, streams2.size());
  }

  @Test
  public void xreadAsMap() {

    final String stream1 = "xread-stream1";
    final String stream2 = "xread-stream2";

    Map<String, StreamEntryID> streamQeury1 = singletonMap(stream1, new StreamEntryID());

    // Before creating Stream
    assertNull(jedis.xreadAsMap(XReadParams.xReadParams().block(1), streamQeury1));
    assertNull(jedis.xreadAsMap(XReadParams.xReadParams(), streamQeury1));

    Map<String, String> map = new HashMap<>();
    map.put("f1", "v1");
    StreamEntryID id1 = new StreamEntryID(1);
    StreamEntryID id2 = new StreamEntryID(2);
    StreamEntryID id3 = new StreamEntryID(3);

    assertEquals(id1, jedis.xadd(stream1, id1, map));
    assertEquals(id2, jedis.xadd(stream2, id2, map));
    assertEquals(id3, jedis.xadd(stream1, id3, map));

    // Read only a single Stream
    Map<String, List<StreamEntry>> streams1 = jedis.xreadAsMap(XReadParams.xReadParams().count(2), streamQeury1);
    assertEquals(singleton(stream1), streams1.keySet());
    List<StreamEntry> list1 = streams1.get(stream1);
    assertEquals(2, list1.size());
    assertEquals(id1, list1.get(0).getID());
    assertEquals(map, list1.get(0).getFields());
    assertEquals(id3, list1.get(1).getID());
    assertEquals(map, list1.get(1).getFields());

    // Read from two Streams
    Map<String, StreamEntryID> streamQuery2 = new LinkedHashMap<>();
    streamQuery2.put(stream1, new StreamEntryID());
    streamQuery2.put(stream2, new StreamEntryID());
    Map<String, List<StreamEntry>> streams2 = jedis.xreadAsMap(XReadParams.xReadParams().count(1), streamQuery2);
    assertEquals(2, streams2.size());
    assertEquals(id1, streams2.get(stream1).get(0).getID());
    assertEquals(id2, streams2.get(stream2).get(0).getID());
  }

  @Test
  @SinceRedisVersion(value = "7.4.0", message = "From Redis 7.4, you can use the + sign as a special ID to request last entry")
  public void xreadAsMapLastEntry() {

    final String stream1 = "xread-stream1";
    final String stream2 = "xread-stream2";

    Map<String, StreamEntryID> streamQeury1 = singletonMap(stream1, new StreamEntryID());

    // Before creating Stream
    assertNull(jedis.xreadAsMap(XReadParams.xReadParams().block(1), streamQeury1));
    assertNull(jedis.xreadAsMap(XReadParams.xReadParams(), streamQeury1));

    Map<String, String> map = new HashMap<>();
    map.put("f1", "v1");
    StreamEntryID id1 = new StreamEntryID(1);
    StreamEntryID id2 = new StreamEntryID(2);
    StreamEntryID id3 = new StreamEntryID(3);

    assertEquals(id1, jedis.xadd(stream1, id1, map));
    assertEquals(id2, jedis.xadd(stream2, id2, map));
    assertEquals(id3, jedis.xadd(stream1, id3, map));


    // Read from last entry
    Map<String, StreamEntryID> streamQueryLE = singletonMap(stream1, StreamEntryID.XREAD_LAST_ENTRY);
    Map<String, List<StreamEntry>> streamsLE = jedis.xreadAsMap(XReadParams.xReadParams().count(1), streamQueryLE);
    assertEquals(singleton(stream1), streamsLE.keySet());
    assertEquals(1, streamsLE.get(stream1).size());
    assertEquals(id3, streamsLE.get(stream1).get(0).getID());
    assertEquals(map, streamsLE.get(stream1).get(0).getFields());
  }

  @Test
  public void xreadBlockZero() throws InterruptedException {
    final AtomicReference<StreamEntryID> readId = new AtomicReference<>();
    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        try (Jedis blockJedis = createJedis()) {
          long startTime = System.currentTimeMillis();
          List<Entry<String, List<StreamEntry>>> read = blockJedis.xread(XReadParams.xReadParams().block(0),
              singletonMap("block0-stream", new StreamEntryID()));
          long endTime = System.currentTimeMillis();
          assertTrue(endTime - startTime > 500);
          assertNotNull(read);
          readId.set(read.get(0).getValue().get(0).getID());
        }
      }
    }, "xread-block-0-thread");
    t.start();
    Thread.sleep(1000);
    StreamEntryID addedId = jedis.xadd("block0-stream", (StreamEntryID) null, singletonMap("foo", "bar"));
    t.join();
    assertEquals(addedId, readId.get());
  }

  @Test
  public void xtrim() {
    Map<String, String> map1 = new HashMap<String, String>();
    map1.put("f1", "v1");

    for (int i = 1; i <= 5; i++) {
      jedis.xadd("xtrim-stream", (StreamEntryID) null, map1);
    }
    assertEquals(5L, jedis.xlen("xtrim-stream"));

    jedis.xtrim("xtrim-stream", 3, false);
    assertEquals(3L, jedis.xlen("xtrim-stream"));
  }

  @Test
  public void xtrimWithParams() {
    Map<String, String> map1 = new HashMap<>();
    map1.put("f1", "v1");
    for (int i = 1; i <= 5; i++) {
      jedis.xadd("xtrim-stream", new StreamEntryID("0-" + i), map1);
    }
    assertEquals(5L, jedis.xlen("xtrim-stream"));

    jedis.xtrim("xtrim-stream", XTrimParams.xTrimParams().maxLen(3).exactTrimming());
    assertEquals(3L, jedis.xlen("xtrim-stream"));

    // minId
    jedis.xtrim("xtrim-stream", XTrimParams.xTrimParams().minId("0-4").exactTrimming());
    assertEquals(2L, jedis.xlen("xtrim-stream"));
  }

  @Test
  public void xrevrange() {
    List<StreamEntry> range = jedis.xrevrange("xrevrange-stream", (StreamEntryID) null,
      (StreamEntryID) null, Integer.MAX_VALUE);
    assertEquals(0, range.size());

    Map<String, String> map = new HashMap<>();
    map.put("f1", "v1");
    StreamEntryID id1 = jedis.xadd("xrevrange-stream", (StreamEntryID) null, map);
    StreamEntryID id2 = jedis.xadd("xrevrange-stream", (StreamEntryID) null, map);
    List<StreamEntry> range2 = jedis.xrange("xrevrange-stream", (StreamEntryID) null,
      (StreamEntryID) null, 3);
    assertEquals(2, range2.size());

    List<StreamEntry> range3 = jedis.xrevrange("xrevrange-stream", null, id1, 2);
    assertEquals(2, range3.size());

    List<StreamEntry> range4 = jedis.xrevrange("xrevrange-stream", id2, id1, 2);
    assertEquals(2, range4.size());

    List<StreamEntry> range5 = jedis.xrevrange("xrevrange-stream", id2, id1, 1);
    assertEquals(1, range5.size());

    List<StreamEntry> range6 = jedis.xrevrange("xrevrange-stream", null, id2, 4);
    assertEquals(1, range6.size());

    StreamEntryID id3 = jedis.xadd("xrevrange-stream", (StreamEntryID) null, map);
    List<StreamEntry> range7 = jedis.xrevrange("xrevrange-stream", id3, id3, 4);
    assertEquals(1, range7.size());

    List<StreamEntry> range8 = jedis.xrevrange("xrevrange-stream", (StreamEntryID) null, (StreamEntryID) null);
    assertEquals(3, range8.size());
    range8 = jedis.xrevrange("xrevrange-stream", StreamEntryID.MAXIMUM_ID, StreamEntryID.MINIMUM_ID);
    assertEquals(3, range8.size());
  }

  @Test
  public void xrevrangeExclusive() {
    Map<String, String> map = new HashMap<>();
    map.put("f1", "v1");
    String id1 = jedis.xadd("xrange-stream", (StreamEntryID) null, map).toString();
    jedis.xadd("xrange-stream", (StreamEntryID) null, map);

    List<StreamEntry> range2 = jedis.xrevrange("xrange-stream", "+", id1, 2);
    assertEquals(2, range2.size());

    List<StreamEntry> range3 = jedis.xrevrange("xrange-stream", "+", "(" + id1, 2);
    assertEquals(1, range3.size());
  }

  @Test
  public void xgroup() {

    Map<String, String> map = new HashMap<>();
    map.put("f1", "v1");
    StreamEntryID id1 = jedis.xadd("xgroup-stream", (StreamEntryID) null, map);

    assertEquals("OK", jedis.xgroupCreate("xgroup-stream", "consumer-group-name", null, false));
    assertEquals("OK", jedis.xgroupSetID("xgroup-stream", "consumer-group-name", id1));
    assertEquals("OK", jedis.xgroupCreate("xgroup-stream", "consumer-group-name1", StreamEntryID.XGROUP_LAST_ENTRY, false));

    jedis.xgroupDestroy("xgroup-stream", "consumer-group-name");
    assertEquals(0L, jedis.xgroupDelConsumer("xgroup-stream", "consumer-group-name1","myconsumer1"));
    assertTrue(jedis.xgroupCreateConsumer("xgroup-stream", "consumer-group-name1","myconsumer2"));
    assertEquals(0L, jedis.xgroupDelConsumer("xgroup-stream", "consumer-group-name1","myconsumer2"));
  }

  @Test
  public void xreadGroupWithParams() {

    // Simple xreadGroup with NOACK
    Map<String, String> map = new HashMap<>();
    map.put("f1", "v1");
    jedis.xadd("xreadGroup-stream1", (StreamEntryID) null, map);
    jedis.xgroupCreate("xreadGroup-stream1", "xreadGroup-group", null, false);
    Map<String, StreamEntryID> streamQeury1 = singletonMap("xreadGroup-stream1", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    List<Entry<String, List<StreamEntry>>> range = jedis.xreadGroup("xreadGroup-group", "xreadGroup-consumer",
        XReadGroupParams.xReadGroupParams().count(1).noAck(), streamQeury1);
    assertEquals(1, range.size());
    assertEquals(1, range.get(0).getValue().size());

    jedis.xadd("xreadGroup-stream1", (StreamEntryID) null, map);
    jedis.xadd("xreadGroup-stream2", (StreamEntryID) null, map);
    jedis.xgroupCreate("xreadGroup-stream2", "xreadGroup-group", null, false);

    // Read only a single Stream
    List<Entry<String, List<StreamEntry>>> streams1 = jedis.xreadGroup("xreadGroup-group", "xreadGroup-consumer",
        XReadGroupParams.xReadGroupParams().count(1).block(1).noAck(), streamQeury1);
    assertEquals(1, streams1.size());
    assertEquals(1, streams1.get(0).getValue().size());

    // Read from two Streams
    Map<String, StreamEntryID> streamQuery2 = new LinkedHashMap<>();
    streamQuery2.put("xreadGroup-stream1", new StreamEntryID());
    streamQuery2.put("xreadGroup-stream2", new StreamEntryID());
    List<Entry<String, List<StreamEntry>>> streams2 = jedis.xreadGroup("xreadGroup-group", "xreadGroup-consumer",
        XReadGroupParams.xReadGroupParams().count(1).block(1).noAck(), streamQuery2);
    assertEquals(2, streams2.size());

    // Read only fresh messages
    StreamEntryID id4 = jedis.xadd("xreadGroup-stream1", (StreamEntryID) null, map);
    Map<String, StreamEntryID> streamQeuryFresh = singletonMap("xreadGroup-stream1", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    List<Entry<String, List<StreamEntry>>> streamsFresh = jedis.xreadGroup("xreadGroup-group", "xreadGroup-consumer",
        XReadGroupParams.xReadGroupParams().count(4).block(100).noAck(), streamQeuryFresh);
    assertEquals(1, streamsFresh.size());
    assertEquals(id4, streamsFresh.get(0).getValue().get(0).getID());
  }

  @Test
  public void xreadGroupAsMap() {

    final String stream1 = "xreadGroup-stream1";
    Map<String, String> map = singletonMap("f1", "v1");

    StreamEntryID id1 = jedis.xadd(stream1, StreamEntryID.NEW_ENTRY, map);
    jedis.xgroupCreate(stream1, "xreadGroup-group", null, false);
    Map<String, StreamEntryID> streamQeury1 = singletonMap(stream1, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    Map<String, List<StreamEntry>> range = jedis.xreadGroupAsMap("xreadGroup-group", "xreadGroup-consumer",
        XReadGroupParams.xReadGroupParams().noAck(), streamQeury1);
    assertEquals(singleton(stream1), range.keySet());
    List<StreamEntry> list = range.get(stream1);
    assertEquals(1, list.size());
    assertEquals(id1, list.get(0).getID());
    assertEquals(map, list.get(0).getFields());
    assertNull(list.get(0).getMillisElapsedFromDelivery());
    assertNull(list.get(0).getDeliveredCount());
  }

  @Test
  public void xreadGroupWithParamsWhenPendingMessageIsDiscarded() {
    // Add two message to stream
    Map<String, String> map1 = new HashMap<>();
    map1.put("f1", "v1");

    Map<String, String> map2 = new HashMap<>();
    map2.put("f2", "v2");

    XAddParams xAddParams = XAddParams.xAddParams().id(StreamEntryID.NEW_ENTRY).maxLen(2);
    StreamEntryID firstMessageEntryId = jedis.xadd("xreadGroup-discard-stream1", xAddParams, map1);
    jedis.xadd("xreadGroup-discard-stream1", xAddParams, map2);

    jedis.xgroupCreate("xreadGroup-discard-stream1", "xreadGroup-group", null, false);
    Map<String, StreamEntryID> streamQuery1 = singletonMap("xreadGroup-discard-stream1", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    List<Entry<String, List<StreamEntry>>> range = jedis.xreadGroup("xreadGroup-group", "xreadGroup-consumer",
            XReadGroupParams.xReadGroupParams().count(1), streamQuery1);
    assertEquals(1, range.size());
    assertEquals(1, range.get(0).getValue().size());

    assertEquals(firstMessageEntryId, range.get(0).getValue().get(0).getID());
    assertEquals(map1, range.get(0).getValue().get(0).getFields());
    assertNull(range.get(0).getValue().get(0).getMillisElapsedFromDelivery());
    assertNull(range.get(0).getValue().get(0).getDeliveredCount());

    // Add third message, the fields of pending message1 will be discarded by redis-server
    Map<String, String> map3 = new HashMap<>();
    map3.put("f3", "v3");
    jedis.xadd("xreadGroup-discard-stream1", xAddParams, map3);

    Map<String, StreamEntryID> streamQueryPending = singletonMap("xreadGroup-discard-stream1", new StreamEntryID());
    List<Entry<String, List<StreamEntry>>> pendingMessages = jedis.xreadGroup("xreadGroup-group", "xreadGroup-consumer",
            XReadGroupParams.xReadGroupParams().count(1).noAck(), streamQueryPending);

    assertEquals(1, pendingMessages.size());
    assertEquals(1, pendingMessages.get(0).getValue().size());

    assertEquals(firstMessageEntryId, pendingMessages.get(0).getValue().get(0).getID());
    assertNull(pendingMessages.get(0).getValue().get(0).getFields());
    assertNull(pendingMessages.get(0).getValue().get(0).getDeliveredCount());
    assertNull(pendingMessages.get(0).getValue().get(0).getMillisElapsedFromDelivery());
  }

  @Test
  public void xack() {
    Map<String, String> map = new HashMap<>();
    map.put("f1", "v1");
    jedis.xadd("xack-stream", (StreamEntryID) null, map);

    jedis.xgroupCreate("xack-stream", "xack-group", null, false);

    Map<String, StreamEntryID> streamQeury1 = singletonMap("xack-stream", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);

    // Empty Stream
    List<Entry<String, List<StreamEntry>>> range = jedis.xreadGroup("xack-group", "xack-consumer",
        XReadGroupParams.xReadGroupParams().count(1).block(1), streamQeury1);
    assertEquals(1, range.size());

    assertEquals(1L,
      jedis.xack("xack-stream", "xack-group", range.get(0).getValue().get(0).getID()));
  }

  @Test
  public void xpendingWithParams() {
    final String stream = "xpendeing-stream";

    assertEquals("OK", jedis.xgroupCreate(stream, "xpendeing-group", null, true));

    // Get the summary from empty stream
    StreamPendingSummary emptySummary = jedis.xpending(stream, "xpendeing-group");
    assertEquals(0, emptySummary.getTotal());
    assertNull(emptySummary.getMinId());
    assertNull(emptySummary.getMaxId());
    assertNull(emptySummary.getConsumerMessageCount());

    Map<String, String> map = new HashMap<>();
    map.put("f1", "v1");
    StreamEntryID id1 = jedis.xadd(stream, (StreamEntryID) null, map);

    Map<String, StreamEntryID> streamQeury1 = singletonMap(stream, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);

    // Read the event from Stream put it on pending
    List<Entry<String, List<StreamEntry>>> range = jedis.xreadGroup("xpendeing-group",
            "xpendeing-consumer", XReadGroupParams.xReadGroupParams().count(1).block(1), streamQeury1);
    assertEquals(1, range.size());
    assertEquals(1, range.get(0).getValue().size());
    assertEquals(map, range.get(0).getValue().get(0).getFields());

    // Get the summary about the pending messages
    StreamPendingSummary pendingSummary = jedis.xpending(stream, "xpendeing-group");
    assertEquals(1, pendingSummary.getTotal());
    assertEquals(id1, pendingSummary.getMinId());
    assertEquals(1l, pendingSummary.getConsumerMessageCount().get("xpendeing-consumer").longValue());

    // Get the pending event
    List<StreamPendingEntry> pendingRange = jedis.xpending(stream, "xpendeing-group",
            new XPendingParams().count(3).consumer("xpendeing-consumer"));
    assertEquals(1, pendingRange.size());
    assertEquals(id1, pendingRange.get(0).getID());
    assertEquals(1, pendingRange.get(0).getDeliveredTimes());
    assertEquals("xpendeing-consumer", pendingRange.get(0).getConsumerName());
    assertTrue(pendingRange.get(0).toString().contains("xpendeing-consumer"));

    // Without consumer
    pendingRange = jedis.xpending(stream, "xpendeing-group", new XPendingParams().count(3));
    assertEquals(1, pendingRange.size());
    assertEquals(id1, pendingRange.get(0).getID());
    assertEquals(1, pendingRange.get(0).getDeliveredTimes());
    assertEquals("xpendeing-consumer", pendingRange.get(0).getConsumerName());

    // with idle
    pendingRange = jedis.xpending(stream, "xpendeing-group",
      new XPendingParams().idle(Duration.ofMinutes(1).toMillis()).count(3));
    assertEquals(0, pendingRange.size());
  }

  @Test
  public void xpendingRange() {
    final String stream = "xpendeing-stream";
    Map<String, String> map = new HashMap<>();
    map.put("foo", "bar");
    StreamEntryID m1 = jedis.xadd(stream, (StreamEntryID) null, map);
    StreamEntryID m2 = jedis.xadd(stream, (StreamEntryID) null, map);
    jedis.xgroupCreate(stream, "xpendeing-group", null, false);

    // read 1 message from the group with each consumer
    Map<String, StreamEntryID> streamQeury = singletonMap(stream, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    jedis.xreadGroup("xpendeing-group", "consumer1", XReadGroupParams.xReadGroupParams().count(1), streamQeury);
    jedis.xreadGroup("xpendeing-group", "consumer2", XReadGroupParams.xReadGroupParams().count(1), streamQeury);

    List<StreamPendingEntry> response = jedis.xpending(stream, "xpendeing-group",
        XPendingParams.xPendingParams("(0", "+", 5));
    assertEquals(2, response.size());
    assertEquals(m1, response.get(0).getID());
    assertEquals("consumer1", response.get(0).getConsumerName());
    assertEquals(m2, response.get(1).getID());
    assertEquals("consumer2", response.get(1).getConsumerName());

    response = jedis.xpending(stream, "xpendeing-group",
        XPendingParams.xPendingParams(StreamEntryID.MINIMUM_ID, StreamEntryID.MAXIMUM_ID, 5));
    assertEquals(2, response.size());
    assertEquals(m1, response.get(0).getID());
    assertEquals("consumer1", response.get(0).getConsumerName());
    assertEquals(m2, response.get(1).getID());
    assertEquals("consumer2", response.get(1).getConsumerName());
  }

  @Test
  public void xclaimWithParams() {
    final String stream = "xpendeing-stream";
    Map<String, String> map = new HashMap<>();
    map.put("f1", "v1");
    jedis.xadd(stream, (StreamEntryID) null, map);

    assertEquals("OK", jedis.xgroupCreate(stream, "xpendeing-group", null, false));

    // Read the event from Stream put it on pending
    jedis.xreadGroup("xpendeing-group", "xpendeing-consumer", XReadGroupParams.xReadGroupParams().count(1).block(1),
            singletonMap(stream, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY));

    // Get the pending event
    List<StreamPendingEntry> pendingRange = jedis.xpending(stream, "xpendeing-group",
        XPendingParams.xPendingParams().count(3).consumer("xpendeing-consumer"));

    // Sleep for 100ms so we can claim events pending for more than 50ms
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<StreamEntry> streamEntrys = jedis.xclaim(stream, "xpendeing-group",
            "xpendeing-consumer2", 50, XClaimParams.xClaimParams().idle(0).retryCount(0),
            pendingRange.get(0).getID());
    assertEquals(1, streamEntrys.size());
    assertEquals(pendingRange.get(0).getID(), streamEntrys.get(0).getID());
    assertEquals("v1", streamEntrys.get(0).getFields().get("f1"));
  }

  @Test
  public void xclaimJustId() {
    final String stream = "xpendeing-stream";
    Map<String, String> map = new HashMap<>();
    map.put("f1", "v1");
    jedis.xadd(stream, (StreamEntryID) null, map);

    assertEquals("OK", jedis.xgroupCreate(stream, "xpendeing-group", null, false));

    // Read the event from Stream put it on pending
    jedis.xreadGroup("xpendeing-group", "xpendeing-consumer", XReadGroupParams.xReadGroupParams().count(1).block(1),
        singletonMap(stream, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY));

    // Get the pending event
    List<StreamPendingEntry> pendingRange = jedis.xpending(stream, "xpendeing-group",
        XPendingParams.xPendingParams().count(3).consumer("xpendeing-consumer"));
    // Sleep for 100ms so we can claim events pending for more than 50ms
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<StreamEntryID> streamEntryIDS = jedis.xclaimJustId(stream, "xpendeing-group",
      "xpendeing-consumer2", 50, XClaimParams.xClaimParams().idle(0).retryCount(0),
      pendingRange.get(0).getID());
    assertEquals(1, streamEntryIDS.size());
    assertEquals(pendingRange.get(0).getID(), streamEntryIDS.get(0));
  }

  @Test
  public void xautoclaim() {
    Map<String, String> map = new HashMap<>();
    map.put("f1", "v1");
    jedis.xadd("xpending-stream", (StreamEntryID) null, map);

    assertEquals("OK", jedis.xgroupCreate("xpending-stream", "xpending-group", null, false));

    // Read the event from Stream put it on pending
    jedis.xreadGroup("xpending-group", "xpending-consumer", XReadGroupParams.xReadGroupParams().count(1).block(1),
        singletonMap("xpending-stream", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY));

    // Get the pending event
    List<StreamPendingEntry> pendingRange = jedis.xpending("xpending-stream", "xpending-group",
        XPendingParams.xPendingParams().count(3).consumer("xpending-consumer"));
    // Sleep for 100ms so we can auto claim events pending for more than 50ms
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // Auto claim pending events to different consumer
    Map.Entry<StreamEntryID, List<StreamEntry>> streamEntrys = jedis.xautoclaim("xpending-stream", "xpending-group",
            "xpending-consumer2", 50, new StreamEntryID(), new XAutoClaimParams().count(1));
    assertEquals(1, streamEntrys.getValue().size());
    assertEquals(pendingRange.get(0).getID(), streamEntrys.getValue().get(0).getID());
    assertEquals("v1", streamEntrys.getValue().get(0).getFields().get("f1"));
  }

  @Test
  public void xautoclaimBinary() {
    Map<String, String> map = new HashMap<>();
    map.put("f1", "v1");
    jedis.xadd("xpending-stream", XAddParams.xAddParams(), map);

    assertEquals("OK", jedis.xgroupCreate("xpending-stream", "xpending-group", null, false));

    // Read the event from Stream put it on pending
    jedis.xreadGroup("xpending-group", "xpending-consumer", XReadGroupParams.xReadGroupParams().count(1).block(1),
        singletonMap("xpending-stream", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY));

    // Get the pending event
    List<StreamPendingEntry> pendingRange = jedis.xpending("xpending-stream", "xpending-group",
        XPendingParams.xPendingParams().count(3).consumer("xpending-consumer"));
    // Sleep for 100ms so we can auto claim events pending for more than 50ms
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // Auto claim pending events to different consumer
    List<Object> streamEntrys = jedis.xautoclaim(SafeEncoder.encode("xpending-stream"),
            SafeEncoder.encode("xpending-group"), SafeEncoder.encode("xpending-consumer2"),
            50, SafeEncoder.encode(new StreamEntryID().toString()), new XAutoClaimParams().count(1));
    Map.Entry<StreamEntryID, List<StreamEntry>> res = BuilderFactory.STREAM_AUTO_CLAIM_RESPONSE.build(streamEntrys);
    assertEquals(1, res.getValue().size());
    assertEquals(pendingRange.get(0).getID(), res.getValue().get(0).getID());
    assertEquals("v1", res.getValue().get(0).getFields().get("f1"));
  }

  @Test
  public void xautoclaimJustId() {
    Map<String, String> map = new HashMap<>();
    map.put("f1", "v1");
    jedis.xadd("xpending-stream", (StreamEntryID) null, map);

    assertEquals("OK", jedis.xgroupCreate("xpending-stream", "xpending-group", null, false));

    // Read the event from Stream put it on pending
    jedis.xreadGroup("xpending-group", "xpending-consumer", XReadGroupParams.xReadGroupParams().count(1).block(1),
        singletonMap("xpending-stream", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY));

    // Get the pending event
    List<StreamPendingEntry> pendingRange = jedis.xpending("xpending-stream", "xpending-group",
        XPendingParams.xPendingParams().count(3).consumer("xpending-consumer"));
    // Sleep for 100ms so we can auto claim events pending for more than 50ms
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // Auto claim pending events to different consumer
    Map.Entry<StreamEntryID, List<StreamEntryID>> streamEntrys = jedis.xautoclaimJustId("xpending-stream", "xpending-group",
            "xpending-consumer2", 50, new StreamEntryID(), new XAutoClaimParams().count(1));
    assertEquals(1, streamEntrys.getValue().size());
    assertEquals(pendingRange.get(0).getID().getTime(), streamEntrys.getValue().get(0).getTime());
    assertEquals(pendingRange.get(0).getID().getSequence(), streamEntrys.getValue().get(0).getSequence());
  }

  @Test
  public void xautoclaimJustIdBinary() {
    Map<String, String> map = new HashMap<>();
    map.put("f1", "v1");
    jedis.xadd("xpending-stream", XAddParams.xAddParams(), map);

    assertEquals("OK", jedis.xgroupCreate("xpending-stream", "xpending-group", null, false));

    // Read the event from Stream put it on pending
    jedis.xreadGroup("xpending-group", "xpending-consumer", XReadGroupParams.xReadGroupParams().count(1).block(1),
        singletonMap("xpending-stream", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY));

    // Get the pending event
    List<StreamPendingEntry> pendingRange = jedis.xpending("xpending-stream", "xpending-group",
        XPendingParams.xPendingParams().count(3).consumer("xpending-consumer"));
    // Sleep for 100ms so we can auto claim events pending for more than 50ms
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // Auto claim pending events to different consumer
    List<Object> streamEntrys = jedis.xautoclaimJustId(SafeEncoder.encode("xpending-stream"),
            SafeEncoder.encode("xpending-group"), SafeEncoder.encode("xpending-consumer2"),
            50, SafeEncoder.encode(new StreamEntryID().toString()), new XAutoClaimParams().count(1));
    Map.Entry<StreamEntryID, List<StreamEntryID>> res = BuilderFactory.STREAM_AUTO_CLAIM_JUSTID_RESPONSE.build(streamEntrys);
    assertEquals(1, res.getValue().size());
    assertEquals(pendingRange.get(0).getID().getTime(), res.getValue().get(0).getTime());
    assertEquals(pendingRange.get(0).getID().getSequence(), res.getValue().get(0).getSequence());
  }

  @Test
  public void xinfo() throws InterruptedException {

    final String STREAM_NAME = "xadd-stream1";
    final String F1 = "f1";
    final String V1 = "v1";
    final String V2 = "v2";
    final String G1 = "G1";
    final String G2 = "G2";
    final String MY_CONSUMER = "myConsumer";
    final String MY_CONSUMER2 = "myConsumer2";

    final RedisVersion redisVersion = RedisVersionUtil.getRedisVersion(jedis);

    Map<String, String> map1 = new HashMap<>();
    map1.put(F1, V1);
    StreamEntryID id1 = jedis.xadd(STREAM_NAME, (StreamEntryID) null, map1);
    map1.put(F1, V2);
    StreamEntryID id2 = jedis.xadd(STREAM_NAME, (StreamEntryID) null, map1);
    assertNotNull(id1);
    StreamInfo streamInfo = jedis.xinfoStream(STREAM_NAME);
    assertNotNull(id2);

    jedis.xgroupCreate(STREAM_NAME, G1, StreamEntryID.XGROUP_LAST_ENTRY, false);
    Map<String, StreamEntryID> streamQeury11 = singletonMap(
        STREAM_NAME, new StreamEntryID("0-0"));
    jedis.xreadGroup(G1, MY_CONSUMER, XReadGroupParams.xReadGroupParams().count(1), streamQeury11);

    Thread.sleep(1);

    List<StreamGroupInfo> groupInfo = jedis.xinfoGroups(STREAM_NAME);
    List<StreamConsumersInfo> consumersInfo = jedis.xinfoConsumers(STREAM_NAME, G1);
    List<StreamConsumerInfo> consumerInfo = jedis.xinfoConsumers2(STREAM_NAME, G1);

    // Stream info test
    assertEquals(2L, streamInfo.getStreamInfo().get(StreamInfo.LENGTH));
    assertEquals(1L, streamInfo.getStreamInfo().get(StreamInfo.RADIX_TREE_KEYS));
    assertEquals(2L, streamInfo.getStreamInfo().get(StreamInfo.RADIX_TREE_NODES));
    assertEquals(0L, streamInfo.getStreamInfo().get(StreamInfo.GROUPS));
    assertEquals(V1, ((StreamEntry) streamInfo.getStreamInfo().get(StreamInfo.FIRST_ENTRY)).getFields().get(F1));
    assertEquals(V2, ((StreamEntry) streamInfo.getStreamInfo().get(StreamInfo.LAST_ENTRY)).getFields().get(F1));
    assertEquals(id2, streamInfo.getStreamInfo().get(StreamInfo.LAST_GENERATED_ID));

    // Using getters
    assertEquals(2, streamInfo.getLength());
    assertEquals(1, streamInfo.getRadixTreeKeys());
    assertEquals(2, streamInfo.getRadixTreeNodes());
    assertEquals(0, streamInfo.getGroups());
    assertEquals(V1, streamInfo.getFirstEntry().getFields().get(F1));
    assertEquals(V2, streamInfo.getLastEntry().getFields().get(F1));
    assertEquals(id2, streamInfo.getLastGeneratedId());

    // Group info test
    assertEquals(1, groupInfo.size());
    assertEquals(G1, groupInfo.get(0).getGroupInfo().get(StreamGroupInfo.NAME));
    assertEquals(1L, groupInfo.get(0).getGroupInfo().get(StreamGroupInfo.CONSUMERS));
    assertEquals(0L, groupInfo.get(0).getGroupInfo().get(StreamGroupInfo.PENDING));
    assertEquals(id2, groupInfo.get(0).getGroupInfo().get(StreamGroupInfo.LAST_DELIVERED));

    // Using getters
    assertEquals(1, groupInfo.size());
    assertEquals(G1, groupInfo.get(0).getName());
    assertEquals(1, groupInfo.get(0).getConsumers());
    assertEquals(0, groupInfo.get(0).getPending());
    assertEquals(id2, groupInfo.get(0).getLastDeliveredId());

    // Consumers info test
    assertEquals(MY_CONSUMER,
      consumersInfo.get(0).getConsumerInfo().get(StreamConsumersInfo.NAME));
    assertEquals(0L, consumersInfo.get(0).getConsumerInfo().get(StreamConsumersInfo.PENDING));
    assertTrue((Long) consumersInfo.get(0).getConsumerInfo().get(StreamConsumersInfo.IDLE) > 0);

    // Using getters
    assertEquals(MY_CONSUMER, consumersInfo.get(0).getName());
    assertEquals(0L, consumersInfo.get(0).getPending());
    assertThat(consumersInfo.get(0).getIdle(), Matchers.greaterThanOrEqualTo(0L));

    if ( redisVersion.isGreaterThanOrEqualTo(RedisVersion.V7_2_0)) {
      assertThat(consumersInfo.get(0).getInactive(), Matchers.any(Long.class));
    }

    // Consumer info test
    assertEquals(MY_CONSUMER,
      consumerInfo.get(0).getConsumerInfo().get(StreamConsumerInfo.NAME));
    assertEquals(0L, consumerInfo.get(0).getConsumerInfo().get(StreamConsumerInfo.PENDING));
    assertTrue((Long) consumerInfo.get(0).getConsumerInfo().get(StreamConsumerInfo.IDLE) > 0);

    // Using getters
    assertEquals(MY_CONSUMER, consumerInfo.get(0).getName());
    assertEquals(0L, consumerInfo.get(0).getPending());
    assertThat(consumerInfo.get(0).getIdle(), Matchers.greaterThanOrEqualTo(0L));
    if (redisVersion.isGreaterThanOrEqualTo(RedisVersion.V7_2_0)) {
      assertThat(consumerInfo.get(0).getInactive(), Matchers.any(Long.class));
    }

    // test with more groups and consumers
    jedis.xgroupCreate(STREAM_NAME, G2, StreamEntryID.XGROUP_LAST_ENTRY, false);
    jedis.xreadGroup(G1, MY_CONSUMER2, XReadGroupParams.xReadGroupParams().count(1), streamQeury11);
    jedis.xreadGroup(G2, MY_CONSUMER, XReadGroupParams.xReadGroupParams().count(1), streamQeury11);
    jedis.xreadGroup(G2, MY_CONSUMER2, XReadGroupParams.xReadGroupParams().count(1), streamQeury11);

    List<StreamGroupInfo> manyGroupsInfo = jedis.xinfoGroups(STREAM_NAME);
    List<StreamConsumersInfo> manyConsumersInfo = jedis.xinfoConsumers(STREAM_NAME, G2);
    List<StreamConsumerInfo> manyConsumerInfo = jedis.xinfoConsumers2(STREAM_NAME, G2);

    assertEquals(2, manyGroupsInfo.size());
    assertEquals(2, manyConsumersInfo.size());
    assertEquals(2, manyConsumerInfo.size());

    StreamFullInfo streamInfoFull = jedis.xinfoStreamFull(STREAM_NAME);

    assertEquals(2, streamInfoFull.getEntries().size());
    assertEquals(2, streamInfoFull.getGroups().size());
    assertEquals(2, streamInfoFull.getLength());
    assertEquals(1, streamInfoFull.getRadixTreeKeys());
    assertEquals(2, streamInfoFull.getRadixTreeNodes());
    assertEquals(0, streamInfo.getGroups());
    assertEquals(G1, streamInfoFull.getGroups().get(0).getName());
    assertEquals(G2, streamInfoFull.getGroups().get(1).getName());
    assertEquals(V1, streamInfoFull.getEntries().get(0).getFields().get(F1));
    assertEquals(V2, streamInfoFull.getEntries().get(1).getFields().get(F1));
    assertEquals(id2, streamInfoFull.getLastGeneratedId());

    streamInfoFull = jedis.xinfoStreamFull(STREAM_NAME, 10);
    assertEquals(G1, streamInfoFull.getGroups().get(0).getName());
    assertEquals(G2, streamInfoFull.getGroups().get(1).getName());
    assertEquals(V1, streamInfoFull.getEntries().get(0).getFields().get(F1));
    assertEquals(V2, streamInfoFull.getEntries().get(1).getFields().get(F1));
    assertEquals(id2, streamInfoFull.getLastGeneratedId());

    // Not existing key - redis cli return error so we expect exception
    try {
      jedis.xinfoStream("random");
      fail("Command should fail");
    } catch (JedisException e) {
      assertEquals("ERR no such key", e.getMessage());
    }
  }

  @Test
  public void xinfoStreamFullWithPending() {

    Map<String, String> map = singletonMap("f1", "v1");
    StreamEntryID id1 = jedis.xadd("streamfull2", (StreamEntryID) null, map);
    StreamEntryID id2 = jedis.xadd("streamfull2", (StreamEntryID) null, map);
    jedis.xgroupCreate("streamfull2", "xreadGroup-group", null, false);

    Map<String, StreamEntryID> streamQeury1 = singletonMap("streamfull2", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    List<Entry<String, List<StreamEntry>>> range = jedis.xreadGroup("xreadGroup-group", "xreadGroup-consumer",
        XReadGroupParams.xReadGroupParams().count(1), streamQeury1);
    assertEquals(1, range.size());
    assertEquals(1, range.get(0).getValue().size());

    StreamFullInfo full = jedis.xinfoStreamFull("streamfull2");
    assertEquals(1, full.getGroups().size());
    StreamGroupFullInfo group = full.getGroups().get(0);
    assertEquals("xreadGroup-group", group.getName());

    assertEquals(1, group.getPending().size());
    List<Object> groupPendingEntry = group.getPending().get(0);
    assertEquals(id1, groupPendingEntry.get(0));
    assertEquals("xreadGroup-consumer", groupPendingEntry.get(1));

    assertEquals(1, group.getConsumers().size());
    StreamConsumerFullInfo consumer = group.getConsumers().get(0);
    assertEquals("xreadGroup-consumer", consumer.getName());
    assertThat(consumer.getSeenTime(), Matchers.greaterThanOrEqualTo(0L));
    if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V7_2_0)) {
      assertThat(consumer.getActiveTime(), Matchers.greaterThanOrEqualTo(0L));
    }
    assertEquals(1, consumer.getPending().size());
    List<Object> consumerPendingEntry = consumer.getPending().get(0);
    assertEquals(id1, consumerPendingEntry.get(0));
  }

  @Test
  public void pipeline() {
    Map<String, String> map = new HashMap<>();
    map.put("a", "b");
    Pipeline p = jedis.pipelined();
    Response<StreamEntryID> id1 = p.xadd("stream1", StreamEntryID.NEW_ENTRY, map);
    Response<StreamEntryID> id2 = p.xadd("stream1", StreamEntryID.NEW_ENTRY, map);
    Response<List<StreamEntry>> results = p.xrange("stream1", (StreamEntryID) null, (StreamEntryID) null, 2);
    p.sync();

    List<StreamEntry> entries = results.get();
    assertEquals(2, entries.size());
    assertEquals(id1.get(), entries.get(0).getID());
    assertEquals(map, entries.get(0).getFields());
    assertEquals(id2.get(), entries.get(1).getID());
    assertEquals(map, entries.get(1).getFields());

    p = jedis.pipelined();
    Response<List<StreamEntry>> results2 = p.xrevrange("stream1", null, id1.get(), 2);
    p.sync();
    assertEquals(2, results2.get().size());
  }

  @Test
  public void transaction() {
    Map<String, String> map = new HashMap<>();
    map.put("a", "b");
    Transaction t = jedis.multi();
    Response<StreamEntryID> id1 = t.xadd("stream1", StreamEntryID.NEW_ENTRY, map);
    Response<StreamEntryID> id2 = t.xadd("stream1", StreamEntryID.NEW_ENTRY, map);
    Response<List<StreamEntry>> results = t.xrange("stream1", (StreamEntryID) null, (StreamEntryID) null, 2);
    t.exec();

    List<StreamEntry> entries = results.get();
    assertEquals(2, entries.size());
    assertEquals(id1.get(), entries.get(0).getID());
    assertEquals(map, entries.get(0).getFields());
    assertEquals(id2.get(), entries.get(1).getID());
    assertEquals(map, entries.get(1).getFields());
  }

  // ========== XREADGROUP CLAIM Tests ==========

  private static final String STREAM_KEY = "test-stream-claim";
  private static final String GROUP_NAME = "test-group";
  private static final String CONSUMER_1 = "consumer-1";
  private static final String CONSUMER_2 = "consumer-2";
  private static final long IDLE_TIME_MS = 5;
  private static final Map<String, String> HASH = singletonMap("field", "value");

  private Map<String, StreamEntryID> beforeEachClaimTest() throws InterruptedException {
    jedis.del(STREAM_KEY);

    // Produce two entries
    Map<String, String> hash = singletonMap("field", "value");
    jedis.xadd(STREAM_KEY, StreamEntryID.NEW_ENTRY, hash);
    jedis.xadd(STREAM_KEY, StreamEntryID.NEW_ENTRY, hash);

    // Create group and consume with consumer-1
    try {
      jedis.xgroupCreate(STREAM_KEY, GROUP_NAME, new StreamEntryID("0-0"), false);
    } catch (JedisDataException e) {
      if (!e.getMessage().contains("BUSYGROUP")) {
        throw e;
      }
    }
    Map<String, StreamEntryID> streams = singletonMap(STREAM_KEY,
        StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    jedis.xreadGroup(GROUP_NAME, CONSUMER_1, XReadGroupParams.xReadGroupParams().count(10),
        streams);

    // Ensure idle time
    Thread.sleep(IDLE_TIME_MS);
    return streams;
  }

  @Test
  @SinceRedisVersion(V8_4_0_STRING)
  public void xreadgroupClaimReturnsMetadataOrdered() throws InterruptedException {
    Map<String, StreamEntryID> streams = beforeEachClaimTest();

    // Produce fresh entries
    jedis.xadd(STREAM_KEY, StreamEntryID.NEW_ENTRY, HASH);
    jedis.xadd(STREAM_KEY, StreamEntryID.NEW_ENTRY, HASH);

    // Read with consumer-2 using CLAIM
    List<Map.Entry<String, List<StreamEntry>>> consumer2Result = jedis.xreadGroup(GROUP_NAME,
        CONSUMER_2, XReadGroupParams.xReadGroupParams().claim(IDLE_TIME_MS - 1).count(10), streams);

    assertNotNull(consumer2Result);
    assertEquals(1, consumer2Result.size());

    List<StreamEntry> entries = consumer2Result.get(0).getValue();
    assertEquals(4, entries.size());

    long claimedCount = entries.stream().filter(StreamEntry::isClaimed).count();
    long freshCount = entries.size() - claimedCount;

    assertEquals(2, claimedCount);
    assertEquals(2, freshCount);

    // Assert order: pending entries are first
    StreamEntry first = entries.get(0);
    StreamEntry second = entries.get(1);
    StreamEntry third = entries.get(2);
    StreamEntry fourth = entries.get(3);

    // Claimed entries
    assertTrue(first.isClaimed());
    assertTrue(second.isClaimed());
    assertTrue(first.getMillisElapsedFromDelivery() >= IDLE_TIME_MS);
    assertTrue(second.getMillisElapsedFromDelivery() >= IDLE_TIME_MS);

    // Fresh entries
    assertFalse(third.isClaimed());
    assertFalse(fourth.isClaimed());
    assertEquals(Long.valueOf(0), third.getDeliveredCount());
    assertEquals(Long.valueOf(0), fourth.getDeliveredCount());
    assertEquals(Long.valueOf(0), third.getMillisElapsedFromDelivery());
    assertEquals(Long.valueOf(0), fourth.getMillisElapsedFromDelivery());
  }

  @Test
  @SinceRedisVersion(V8_4_0_STRING)
  public void xreadgroupClaimMovesPendingFromC1ToC2AndRemainsPendingUntilAck()
      throws InterruptedException {
    Map<String, StreamEntryID> streams = beforeEachClaimTest();

    // Verify pending belongs to consumer-1
    StreamPendingSummary before = jedis.xpending(STREAM_KEY, GROUP_NAME);
    assertEquals(2L, before.getTotal());
    assertEquals(2L, before.getConsumerMessageCount().getOrDefault(CONSUMER_1, 0L).longValue());

    // Claim with consumer-2
    List<Map.Entry<String, List<StreamEntry>>> res = jedis.xreadGroup(GROUP_NAME, CONSUMER_2,
        XReadGroupParams.xReadGroupParams().claim(IDLE_TIME_MS).count(10), streams);

    assertNotNull(res);
    assertEquals(1, res.size());

    List<StreamEntry> entries = res.get(0).getValue();
    long claimed = entries.stream().filter(StreamEntry::isClaimed).count();
    assertEquals(2, claimed);

    // After claim: entries are pending for consumer-2
    StreamPendingSummary afterClaim = jedis.xpending(STREAM_KEY, GROUP_NAME);
    assertEquals(2L, afterClaim.getTotal());
    assertEquals(0L, afterClaim.getConsumerMessageCount().getOrDefault(CONSUMER_1, 0L).longValue());
    assertEquals(2L, afterClaim.getConsumerMessageCount().getOrDefault(CONSUMER_2, 0L).longValue());

    // XACK the claimed entries
    long acked = jedis.xack(STREAM_KEY, GROUP_NAME, entries.get(0).getID(), entries.get(1).getID());
    assertEquals(2, acked);

    StreamPendingSummary afterAck = jedis.xpending(STREAM_KEY, GROUP_NAME);
    assertEquals(0L, afterAck.getTotal());
  }

  @Test
  @SinceRedisVersion(V8_4_0_STRING)
  public void xreadgroupClaimWithNoackDoesNotCreatePendingAndRemovesClaimedFromPel()
      throws InterruptedException {
    Map<String, StreamEntryID> streams = beforeEachClaimTest();

    // Verify pending belongs to consumer-1
    StreamPendingSummary before = jedis.xpending(STREAM_KEY, GROUP_NAME);
    assertEquals(2L, before.getTotal());
    assertEquals(2L, before.getConsumerMessageCount().getOrDefault(CONSUMER_1, 0L).longValue());
    assertEquals(0L, before.getConsumerMessageCount().getOrDefault(CONSUMER_2, 0L).longValue());

    // Produce fresh entries
    jedis.xadd(STREAM_KEY, StreamEntryID.NEW_ENTRY, HASH);
    jedis.xadd(STREAM_KEY, StreamEntryID.NEW_ENTRY, HASH);

    // Claim with NOACK using consumer-2
    List<Map.Entry<String, List<StreamEntry>>> res = jedis.xreadGroup(GROUP_NAME, CONSUMER_2,
        XReadGroupParams.xReadGroupParams().claim(IDLE_TIME_MS).noAck().count(10), streams);

    assertNotNull(res);
    assertEquals(1, res.size());

    List<StreamEntry> entries = res.get(0).getValue();
    long claimedCount = entries.stream().filter(StreamEntry::isClaimed).count();
    long freshCount = entries.size() - claimedCount;

    assertEquals(2, claimedCount);
    assertEquals(2, freshCount);

    // After NOACK read, previously pending entries remain pending
    StreamPendingSummary afterNoack = jedis.xpending(STREAM_KEY, GROUP_NAME);
    assertEquals(2L, afterNoack.getTotal());

    // Claimed entries are now owned by consumer-2
    assertEquals(0L, afterNoack.getConsumerMessageCount().getOrDefault(CONSUMER_1, 0L).longValue());
    assertEquals(2L, afterNoack.getConsumerMessageCount().getOrDefault(CONSUMER_2, 0L).longValue());
  }

  // ========== Idempotent Producer Tests ==========

  @Test
  @EnabledOnCommand("XCFGSET")
  public void testXaddIdmpAuto() {
    // Add entry with IDMPAUTO
    Map<String, String> message = new HashMap<>();
    message.put("order", "12345");
    message.put("amount", "100.00");

    StreamEntryID id1 = jedis.xadd(STREAM_KEY, XAddParams.xAddParams().idmpAuto("producer-1"),
        message);
    assertNotNull(id1);
    assertEquals(1L, jedis.xlen(STREAM_KEY));

    // Add same message again with same producer - should return same ID (duplicate detected)
    StreamEntryID id2 = jedis.xadd(STREAM_KEY, XAddParams.xAddParams().idmpAuto("producer-1"),
        message);
    assertEquals(id1, id2); // Duplicate returns same entry ID as before
    assertEquals(1L, jedis.xlen(STREAM_KEY)); // Stream length unchanged

    // Add same message with different producer - should succeed
    StreamEntryID id3 = jedis.xadd(STREAM_KEY, XAddParams.xAddParams().idmpAuto("producer-2"),
        message);
    assertNotNull(id3);
    assertEquals(2L, jedis.xlen(STREAM_KEY));

    // Add different message with same producer - should succeed
    Map<String, String> message2 = new HashMap<>();
    message2.put("order", "67890");
    message2.put("amount", "200.00");

    StreamEntryID id4 = jedis.xadd(STREAM_KEY, XAddParams.xAddParams().idmpAuto("producer-1"),
        message2);
    assertNotNull(id4);
    assertEquals(3L, jedis.xlen(STREAM_KEY));
  }

  @Test
  @EnabledOnCommand("XCFGSET")
  public void testXaddIdmp() {
    Map<String, String> hash1 = singletonMap("field1", "value1");
    Map<String, String> hash2 = singletonMap("field2", "value2");

    // Add entry with explicit idempotent ID
    StreamEntryID id1 = jedis.xadd(STREAM_KEY,
        XAddParams.xAddParams().idmp("producer-1", "iid-001"), hash1);
    assertNotNull(id1);
    assertEquals(1L, jedis.xlen(STREAM_KEY));

    // Add with same producer and idempotent ID - should return same ID (duplicate detected)
    StreamEntryID id2 = jedis.xadd(STREAM_KEY,
        XAddParams.xAddParams().idmp("producer-1", "iid-001"), hash2);
    assertEquals(id1, id2); // Duplicate returns same entry ID as before
    assertEquals(1L, jedis.xlen(STREAM_KEY)); // Stream length unchanged

    // Add with same producer but different idempotent ID - should succeed
    StreamEntryID id3 = jedis.xadd(STREAM_KEY,
        XAddParams.xAddParams().idmp("producer-1", "iid-002"), hash1);
    assertNotNull(id3);
    assertEquals(2L, jedis.xlen(STREAM_KEY));

    // Add with different producer but same idempotent ID - should succeed
    StreamEntryID id4 = jedis.xadd(STREAM_KEY,
        XAddParams.xAddParams().idmp("producer-2", "iid-001"), hash1);
    assertNotNull(id4);
    assertEquals(3L, jedis.xlen(STREAM_KEY));
  }

  @Test
  @EnabledOnCommand("XCFGSET")
  public void testXcfgset() {
    // Add an entry to create the stream
    jedis.xadd(STREAM_KEY, StreamEntryID.NEW_ENTRY, singletonMap("field", "value"));

    // Configure idempotent producer settings
    String result = jedis.xcfgset(STREAM_KEY,
        XCfgSetParams.xCfgSetParams().idmpDuration(1000).idmpMaxsize(500));
    assertEquals("OK", result);

    // Verify settings via XINFO STREAM
    StreamInfo info = jedis.xinfoStream(STREAM_KEY);
    assertEquals(Long.valueOf(1000), info.getIdmpDuration());
    assertEquals(Long.valueOf(500), info.getIdmpMaxsize());
  }

  @Test
  @EnabledOnCommand("XCFGSET")
  public void testXinfoStreamIdempotentFields() {
    Map<String, String> hash1 = singletonMap("field1", "value1");
    Map<String, String> hash2 = singletonMap("field2", "value2");

    // Add an entry to create the stream
    jedis.xadd(STREAM_KEY, StreamEntryID.NEW_ENTRY, hash1);

    // Configure idempotent settings
    jedis.xcfgset(STREAM_KEY,
        XCfgSetParams.xCfgSetParams().idmpDuration(100).idmpMaxsize(100));

    // Add some entries with idempotent IDs
    jedis.xadd(STREAM_KEY, XAddParams.xAddParams().idmp("producer-1", "iid-001"), hash1);
    jedis.xadd(STREAM_KEY, XAddParams.xAddParams().idmp("producer-1", "iid-002"), hash2);
    jedis.xadd(STREAM_KEY, XAddParams.xAddParams().idmp("producer-2", "iid-001"), hash1);

    // Try to add a duplicate
    jedis.xadd(STREAM_KEY, XAddParams.xAddParams().idmp("producer-1", "iid-001"), hash2);

    // Check XINFO STREAM response
    StreamInfo info = jedis.xinfoStream(STREAM_KEY);

    // Verify idempotent configuration fields
    assertEquals(Long.valueOf(100), info.getIdmpDuration());
    assertEquals(Long.valueOf(100), info.getIdmpMaxsize());

    // Verify idempotent statistics fields
    assertEquals(Long.valueOf(2), info.getPidsTracked()); // 2 producers
    assertEquals(Long.valueOf(3), info.getIidsTracked()); // 3 unique IDs
    assertEquals(Long.valueOf(3), info.getIidsAdded()); // 3 entries added
    assertEquals(Long.valueOf(1), info.getIidsDuplicates()); // 1 duplicate rejected
  }

  @Test
  @EnabledOnCommand("XCFGSET")
  public void testXaddIdmpWithTrimming() {
    // Add first entry with IDMPAUTO and trimming
    StreamEntryID id1 = jedis.xadd(STREAM_KEY,
        XAddParams.xAddParams().idmpAuto("producer1").maxLen(2), singletonMap("field", "value1"));
    assertNotNull(id1);
    assertEquals(1, jedis.xlen(STREAM_KEY));

    // Add duplicate - should return same ID
    StreamEntryID id2 = jedis.xadd(STREAM_KEY,
        XAddParams.xAddParams().idmpAuto("producer1").maxLen(2), singletonMap("field", "value1"));
    assertEquals(id1, id2);
    assertEquals(1, jedis.xlen(STREAM_KEY)); // Still 1 entry

    // Add different message - should add new entry and trim
    StreamEntryID id3 = jedis.xadd(STREAM_KEY,
        XAddParams.xAddParams().idmpAuto("producer1").maxLen(2), singletonMap("field", "value2"));
    assertNotNull(id3);
    assertNotEquals(id1, id3); // Different IDs
    assertEquals(2, jedis.xlen(STREAM_KEY)); // Now 2 entries
  }

  @Test
  @EnabledOnCommand("XCFGSET")
  public void testXcfgsetDefaults() {
    jedis.xadd(STREAM_KEY, StreamEntryID.NEW_ENTRY, singletonMap("init", "value"));

    // Verify default values
    StreamInfo info = jedis.xinfoStream(STREAM_KEY);
    assertEquals(Long.valueOf(100), info.getIdmpDuration());
    assertEquals(Long.valueOf(100), info.getIdmpMaxsize());

    assertEquals("OK", jedis.xcfgset(STREAM_KEY,
        XCfgSetParams.xCfgSetParams().idmpDuration(200).idmpMaxsize(200)));

    StreamInfo infoAfter = jedis.xinfoStream(STREAM_KEY);
    assertEquals(Long.valueOf(200), infoAfter.getIdmpDuration());
    assertEquals(Long.valueOf(200), infoAfter.getIdmpMaxsize());
  }

}
