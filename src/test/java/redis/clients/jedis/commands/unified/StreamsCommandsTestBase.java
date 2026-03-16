package redis.clients.jedis.commands.unified;

import io.redis.test.annotations.EnabledOnCommand;
import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.args.StreamDeletionPolicy;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.redis.test.utils.RedisVersion.V8_4_0_STRING;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("integration")
public abstract class StreamsCommandsTestBase extends UnifiedJedisCommandsTestBase {

  protected static final String STREAM_KEY_1 = "{stream}-1";
  protected static final String STREAM_KEY_2 = "{stream}-2";
  protected static final String GROUP_NAME = "group-1";
  protected static final String CONSUMER_NAME = "consumer-1";

  protected static final String FIELD_KEY_1 = "field-1";
  protected static final String VALUE_1 = "value-1";
  protected static final String FIELD_KEY_2 = "field-2";
  protected static final String VALUE_2 = "value-2";
  protected static final Map<String, String> HASH_1 = singletonMap(FIELD_KEY_1, VALUE_1);
  protected static final Map<String, String> HASH_2 = singletonMap(FIELD_KEY_2, VALUE_2);

  public StreamsCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  /**
   * Populates a test stream with values using the i-0 format
   * @param streamKey The stream key to populate
   * @param count Number of entries to add
   * @param map Map of field-value pairs for each entry
   */
  protected void populateTestStreamWithValues(String streamKey, int count,
      Map<String, String> map) {
    for (int i = 1; i <= count; i++) {
      jedis.xadd(streamKey, XAddParams.xAddParams().id(new StreamEntryID(i + "-0")), map);
    }
    assertEquals(count, jedis.xlen(streamKey));
  }

  @BeforeEach
  public void setUp() {
    setUpTestStream();
  }

  private void setUpTestStream() {
    setUpTestStream(StreamEntryID.XGROUP_LAST_ENTRY);
  }

  private void setUpTestStream(StreamEntryID startId) {
    jedis.del(STREAM_KEY_1);
    jedis.del(STREAM_KEY_2);
    try {
      jedis.xgroupCreate(STREAM_KEY_1, GROUP_NAME, startId, true);
    } catch (JedisDataException e) {
      if (!e.getMessage().contains("BUSYGROUP")) {
        throw e;
      }
    }
    try {
      jedis.xgroupCreate(STREAM_KEY_2, GROUP_NAME, startId, true);
    } catch (JedisDataException e) {
      if (!e.getMessage().contains("BUSYGROUP")) {
        throw e;
      }
    }
  }

  // ========== XADD Command Tests ==========

  @Test
  public void xaddBasic() {
    setUpTestStream();

    // Test basic XADD with auto-generated ID
    StreamEntryID id1 = jedis.xadd(STREAM_KEY_1, StreamEntryID.NEW_ENTRY, HASH_1);
    assertNotNull(id1);
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));

    // Test XADD with multiple fields
    Map<String, String> multiFieldHash = new HashMap<>();
    multiFieldHash.put("field1", "value1");
    multiFieldHash.put("field2", "value2");
    multiFieldHash.put("field3", "value3");

    StreamEntryID id2 = jedis.xadd(STREAM_KEY_1, StreamEntryID.NEW_ENTRY, multiFieldHash);
    assertNotNull(id2);
    assertTrue(id2.compareTo(id1) > 0);
    assertEquals(2L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  public void xaddWithSpecificId() {
    setUpTestStream();

    // Test XADD with specific ID
    StreamEntryID specificId = new StreamEntryID("1000-0");
    StreamEntryID resultId = jedis.xadd(STREAM_KEY_1, specificId, HASH_1);
    assertEquals(specificId, resultId);
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));

    // Test XADD with ID that must be greater than previous
    StreamEntryID nextId = new StreamEntryID("1001-0");
    StreamEntryID resultId2 = jedis.xadd(STREAM_KEY_1, nextId, HASH_2);
    assertEquals(nextId, resultId2);
    assertEquals(2L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  public void xaddWithParams() {
    setUpTestStream();

    // Test XADD with maxLen parameter
    populateTestStreamWithValues(STREAM_KEY_1, 5, HASH_1);

    // Add with maxLen=3, should trim to 3 entries
    StreamEntryID id6 = jedis.xadd(STREAM_KEY_1,
      XAddParams.xAddParams().id(new StreamEntryID("6-0")).maxLen(3), HASH_2);
    assertNotNull(id6);
    assertEquals(3L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  public void xaddErrorCases() {
    setUpTestStream();

    // Test XADD with empty hash should fail
    try {
      Map<String, String> emptyHash = new HashMap<>();
      jedis.xadd(STREAM_KEY_1, StreamEntryID.NEW_ENTRY, emptyHash);
      fail("Should throw JedisDataException for empty hash");
    } catch (JedisDataException expected) {
      assertTrue(expected.getMessage().contains("wrong number of arguments"));
    }

    // Test XADD with noMkStream on non-existent stream
    StreamEntryID result = jedis.xadd("non-existent-stream", XAddParams.xAddParams().noMkStream(),
      HASH_1);
    assertNull(result);
  }

  @ParameterizedTest
  @CsvSource({ "KEEP_REFERENCES,3", "DELETE_REFERENCES,0" })
  @SinceRedisVersion("8.1.240")
  public void xaddWithTrimmingMode(StreamDeletionPolicy trimMode, int expected) {
    setUpTestStream();
    Map<String, String> map = singletonMap("field", "value");

    // Add initial entries to the stream
    populateTestStreamWithValues(STREAM_KEY_1, 5, map);

    // Create consumer group and read messages to create PEL entries
    Map<String, StreamEntryID> streamQuery = singletonMap(STREAM_KEY_1,
      StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    jedis.xreadGroup(GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(3),
      streamQuery);

    // Verify PEL has entries
    List<StreamPendingEntry> pendingBefore = jedis.xpending(STREAM_KEY_1, GROUP_NAME,
      XPendingParams.xPendingParams().count(10));
    assertEquals(3, pendingBefore.size());

    // Add new entry with maxLen=3 and KEEP_REFERENCES mode
    StreamEntryID newId = jedis.xadd(STREAM_KEY_1,
      XAddParams.xAddParams().id(new StreamEntryID("6-0")).maxLen(3).trimmingMode(trimMode), map);
    assertNotNull(newId);

    // Stream should be trimmed to 3 entries
    assertEquals(3L, jedis.xlen(STREAM_KEY_1));

    List<StreamPendingEntry> pendingAfter = jedis.xpending(STREAM_KEY_1, GROUP_NAME,
      XPendingParams.xPendingParams().count(10));
    assertEquals(expected, pendingAfter.size());
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xaddWithTrimmingModeAcknowledged() {
    setUpTestStream();
    Map<String, String> map = singletonMap("field", "value");

    // Add initial entries to the stream
    populateTestStreamWithValues(STREAM_KEY_1, 5, map);

    // Create consumer group and read messages
    Map<String, StreamEntryID> streamQuery = singletonMap(STREAM_KEY_1,
      StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    List<Map.Entry<String, List<StreamEntry>>> messages = jedis.xreadGroup(GROUP_NAME,
      CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(3), streamQuery);

    // Acknowledge the first 2 messages
    StreamEntryID id1 = messages.get(0).getValue().get(0).getID();
    StreamEntryID id2 = messages.get(0).getValue().get(1).getID();
    jedis.xack(STREAM_KEY_1, GROUP_NAME, id1, id2);

    // Verify PEL state
    List<StreamPendingEntry> pendingBefore = jedis.xpending(STREAM_KEY_1, GROUP_NAME,
      XPendingParams.xPendingParams().count(10));
    assertEquals(1, pendingBefore.size()); // Only 1 unacknowledged message

    // Add new entry with maxLen=3 and ACKNOWLEDGED mode
    StreamEntryID newId = jedis.xadd(STREAM_KEY_1, XAddParams.xAddParams()
        .id(new StreamEntryID("6-0")).maxLen(3).trimmingMode(StreamDeletionPolicy.ACKNOWLEDGED),
      map);
    assertNotNull(newId);

    // Stream length should respect acknowledgment status
    long streamLen = jedis.xlen(STREAM_KEY_1);
    assertEquals(4, streamLen); // Should not trim unacknowledged entries aggressively

    // PEL should still contain unacknowledged entries
    List<StreamPendingEntry> pendingAfter = jedis.xpending(STREAM_KEY_1, GROUP_NAME,
      XPendingParams.xPendingParams().count(10));
    assertEquals(1, pendingAfter.size()); // Unacknowledged entries should remain
  }

  // ========== XTRIM Command Tests ==========

  @Test
  public void xtrimBasic() {
    setUpTestStream();

    // Add test entries
    populateTestStreamWithValues(STREAM_KEY_1, 5, HASH_1);

    // Test basic XTRIM with maxLen
    long trimmed = jedis.xtrim(STREAM_KEY_1, 3, false);
    assertEquals(2L, trimmed); // Should trim 2 entries
    assertEquals(3L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  public void xtrimWithParams() {
    setUpTestStream();

    // Add test entries with specific IDs
    populateTestStreamWithValues(STREAM_KEY_1, 5, HASH_1);

    // Test XTRIM with XTrimParams and exact trimming
    long trimmed = jedis.xtrim(STREAM_KEY_1, XTrimParams.xTrimParams().maxLen(3).exactTrimming());
    assertEquals(2L, trimmed);
    assertEquals(3L, jedis.xlen(STREAM_KEY_1));

    // Test XTRIM with minId - use "4-0" since we have entries 1-0, 2-0, 3-0, 4-0, 5-0
    long trimmed2 = jedis.xtrim(STREAM_KEY_1,
      XTrimParams.xTrimParams().minId("4-0").exactTrimming());
    assertEquals(1L, trimmed2); // Should trim entries with ID < 4-0 (only 3-0 should be trimmed)
    assertEquals(2L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  public void xtrimApproximate() {
    setUpTestStream();

    // Add many entries
    populateTestStreamWithValues(STREAM_KEY_1, 10, HASH_1);

    // Test approximate trimming
    long trimmed = jedis.xtrim(STREAM_KEY_1, 5, true);
    assertTrue(trimmed >= 0); // Approximate trimming may trim different amounts
    assertTrue(jedis.xlen(STREAM_KEY_1) <= 10); // Should not exceed original length
  }

  @ParameterizedTest
  @CsvSource({ "KEEP_REFERENCES,3", "DELETE_REFERENCES,1" })
  @SinceRedisVersion("8.1.240")
  public void xaddWithMinIdTrimmingMode(StreamDeletionPolicy trimMode, int expected) {
    setUpTestStream();
    Map<String, String> map = singletonMap("field", "value");

    // Add initial entries with specific IDs
    populateTestStreamWithValues(STREAM_KEY_1, 5, map);

    // Create consumer group and read messages to create PEL entries
    Map<String, StreamEntryID> streamQuery = singletonMap(STREAM_KEY_1,
      StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    jedis.xreadGroup(GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(3),
      streamQuery);

    // Verify PEL has entries
    List<StreamPendingEntry> pendingBefore = jedis.xpending(STREAM_KEY_1, GROUP_NAME,
      XPendingParams.xPendingParams().count(10));
    assertEquals(3, pendingBefore.size());

    // Add new entry with minId="3-0" and specified trimming mode
    StreamEntryID newId = jedis.xadd(STREAM_KEY_1,
      XAddParams.xAddParams().id(new StreamEntryID("6-0")).minId("3-0").trimmingMode(trimMode),
      map);
    assertNotNull(newId);

    // Stream should have entries >= 3-0 plus the new entry
    long streamLen = jedis.xlen(STREAM_KEY_1);
    assertTrue(streamLen >= 3);

    // Check PEL entries based on trimming mode
    List<StreamPendingEntry> pendingAfter = jedis.xpending(STREAM_KEY_1, GROUP_NAME,
      XPendingParams.xPendingParams().count(10));
    assertEquals(expected, pendingAfter.size());
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xaddWithApproximateTrimmingAndTrimmingMode() {
    setUpTestStream();
    Map<String, String> map = singletonMap("field", "value");

    // Add initial entries
    populateTestStreamWithValues(STREAM_KEY_1, 10, map);

    // Create consumer group and read messages
    Map<String, StreamEntryID> streamQuery = singletonMap(STREAM_KEY_1,
      StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    jedis.xreadGroup(GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(5),
      streamQuery);

    // Add new entry with approximate trimming and KEEP_REFERENCES mode
    StreamEntryID newId = jedis.xadd(STREAM_KEY_1,
      XAddParams.xAddParams().id(new StreamEntryID("11-0")).maxLen(5).approximateTrimming()
          .trimmingMode(StreamDeletionPolicy.KEEP_REFERENCES),
      map);
    assertNotNull(newId);

    // With approximate trimming, the exact length may vary but should be around the target
    long streamLen = jedis.xlen(STREAM_KEY_1);
    assertTrue(streamLen >= 5); // Should be approximately 5, but may be more due to approximation

    // PEL should preserve references
    List<StreamPendingEntry> pendingAfter = jedis.xpending(STREAM_KEY_1, GROUP_NAME,
      XPendingParams.xPendingParams().count(10));
    assertEquals(5, pendingAfter.size()); // All read messages should remain in PEL
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xaddWithExactTrimmingAndTrimmingMode() {
    setUpTestStream();
    Map<String, String> map = singletonMap("field", "value");

    // Add initial entries
    populateTestStreamWithValues(STREAM_KEY_1, 5, map);

    // Create consumer group and read messages
    Map<String, StreamEntryID> streamQuery = singletonMap(STREAM_KEY_1,
      StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    jedis.xreadGroup(GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(3),
      streamQuery);

    // Add new entry with exact trimming and DELETE_REFERENCES mode
    StreamEntryID newId = jedis.xadd(STREAM_KEY_1,
      XAddParams.xAddParams().id(new StreamEntryID("6-0")).maxLen(3).exactTrimming()
          .trimmingMode(StreamDeletionPolicy.DELETE_REFERENCES),
      map);
    assertNotNull(newId);

    // With exact trimming, stream should be exactly 3 entries
    assertEquals(3L, jedis.xlen(STREAM_KEY_1));

    // PEL references should be cleaned up for trimmed entries
    List<StreamPendingEntry> pendingAfter = jedis.xpending(STREAM_KEY_1, GROUP_NAME,
      XPendingParams.xPendingParams().count(10));
    // Only entries that still exist in the stream should remain in PEL
    assertTrue(pendingAfter.size() <= 3);
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xaddWithLimitAndTrimmingMode() {
    setUpTestStream();
    Map<String, String> map = singletonMap("field", "value");

    // Add initial entries
    populateTestStreamWithValues(STREAM_KEY_1, 10, map);

    Map<String, StreamEntryID> streamQuery = singletonMap(STREAM_KEY_1,
      StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    jedis.xreadGroup(GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(5),
      streamQuery);

    // Add new entry with limit and KEEP_REFERENCES mode (limit requires approximate trimming)
    StreamEntryID newId = jedis.xadd(STREAM_KEY_1,
      XAddParams.xAddParams().id(new StreamEntryID("11-0")).maxLen(5).approximateTrimming() // Required
                                                                                            // for
                                                                                            // limit
                                                                                            // to
                                                                                            // work
          .limit(2) // Limit the number of entries to examine for trimming
          .trimmingMode(StreamDeletionPolicy.KEEP_REFERENCES),
      map);
    assertNotNull(newId);

    // With limit, trimming may be less aggressive
    long streamLen = jedis.xlen(STREAM_KEY_1);
    assertTrue(streamLen >= 5); // Should be at least 5, but may be more due to limit

    // PEL should preserve references
    List<StreamPendingEntry> pendingAfter = jedis.xpending(STREAM_KEY_1, GROUP_NAME,
      XPendingParams.xPendingParams().count(10));
    assertEquals(5, pendingAfter.size()); // All read messages should remain in PEL
  }

  // ========== XACK Command Tests ==========

  @Test
  public void xackBasic() {
    setUpTestStream();

    // Add a message to the stream
    StreamEntryID messageId = jedis.xadd(STREAM_KEY_1, StreamEntryID.NEW_ENTRY, HASH_1);
    assertNotNull(messageId);

    // Consumer group already created in setUpTestStream(), just read message
    Map<String, StreamEntryID> streams = singletonMap(STREAM_KEY_1,
      StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    List<Map.Entry<String, List<StreamEntry>>> messages = jedis.xreadGroup(GROUP_NAME,
      CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(1), streams);

    assertEquals(1, messages.size());
    assertEquals(1, messages.get(0).getValue().size());
    StreamEntryID readMessageId = messages.get(0).getValue().get(0).getID();

    // Test XACK
    long acked = jedis.xack(STREAM_KEY_1, GROUP_NAME, readMessageId);
    assertEquals(1L, acked);
  }

  @Test
  public void xackMultipleMessages() {
    setUpTestStream();

    // Add multiple messages
    StreamEntryID id1 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("1-0"), HASH_1);
    StreamEntryID id2 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("2-0"), HASH_2);

    // Consumer group already created in setUpTestStream(), just read messages
    Map<String, StreamEntryID> streams = singletonMap(STREAM_KEY_1,
      StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    List<Map.Entry<String, List<StreamEntry>>> messages = jedis.xreadGroup(GROUP_NAME,
      CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(2), streams);

    assertEquals(1, messages.size());
    assertEquals(2, messages.get(0).getValue().size());

    // Test XACK with multiple IDs
    StreamEntryID readId1 = messages.get(0).getValue().get(0).getID();
    StreamEntryID readId2 = messages.get(0).getValue().get(1).getID();
    long acked = jedis.xack(STREAM_KEY_1, GROUP_NAME, readId1, readId2);
    assertEquals(2L, acked);
  }

  @Test
  public void xackNonExistentMessage() {
    setUpTestStream();

    // Consumer group already created in setUpTestStream()
    // Test XACK with non-existent message ID
    StreamEntryID nonExistentId = new StreamEntryID("999-0");
    long acked = jedis.xack(STREAM_KEY_1, GROUP_NAME, nonExistentId);
    assertEquals(0L, acked); // Should return 0 for non-existent message
  }

  // ========== XDEL Command Tests ==========

  @Test
  public void xdelBasic() {
    setUpTestStream();

    // Add test entries
    StreamEntryID id1 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("1-0"), HASH_1);
    StreamEntryID id2 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("2-0"), HASH_2);
    assertEquals(2L, jedis.xlen(STREAM_KEY_1));

    // Test XDEL with single ID
    long deleted = jedis.xdel(STREAM_KEY_1, id1);
    assertEquals(1L, deleted);
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  public void xdelMultipleEntries() {
    setUpTestStream();

    // Add test entries
    StreamEntryID id1 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("1-0"), HASH_1);
    StreamEntryID id2 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("2-0"), HASH_2);
    StreamEntryID id3 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("3-0"), HASH_1);
    assertEquals(3L, jedis.xlen(STREAM_KEY_1));

    // Test XDEL with multiple IDs
    long deleted = jedis.xdel(STREAM_KEY_1, id1, id3);
    assertEquals(2L, deleted);
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  public void xdelNonExistentEntries() {
    setUpTestStream();

    // Add one entry
    StreamEntryID id1 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("1-0"), HASH_1);
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));

    // Test XDEL with mix of existing and non-existent IDs
    StreamEntryID nonExistentId = new StreamEntryID("999-0");
    long deleted = jedis.xdel(STREAM_KEY_1, id1, nonExistentId);
    assertEquals(1L, deleted); // Should only delete the existing entry
    assertEquals(0L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  public void xdelEmptyStream() {
    setUpTestStream();

    // Test XDEL on empty stream
    StreamEntryID nonExistentId = new StreamEntryID("1-0");
    long deleted = jedis.xdel(STREAM_KEY_1, nonExistentId);
    assertEquals(0L, deleted);
  }

  // ========== XACKDEL Command Tests ==========

  @Test
  @SinceRedisVersion("8.1.240")
  public void xackdelBasic() {
    setUpTestStream();

    // Add a message to the stream
    StreamEntryID messageId = jedis.xadd(STREAM_KEY_1, new StreamEntryID("1-0"), HASH_1);
    assertNotNull(messageId);

    // Consumer group already created in setUpTestStream(), read message
    Map<String, StreamEntryID> streams = singletonMap(STREAM_KEY_1,
      StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    List<Map.Entry<String, List<StreamEntry>>> messages = jedis.xreadGroup(GROUP_NAME,
      CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(1), streams);

    assertEquals(1, messages.size());
    assertEquals(1, messages.get(0).getValue().size());
    StreamEntryID readMessageId = messages.get(0).getValue().get(0).getID();

    // Test XACKDEL - should acknowledge and delete the message
    List<StreamEntryDeletionResult> results = jedis.xackdel(STREAM_KEY_1, GROUP_NAME,
      readMessageId);
    assertThat(results, hasSize(1));
    assertEquals(StreamEntryDeletionResult.DELETED, results.get(0));

    // Verify message is deleted from stream
    assertEquals(0L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xackdelWithTrimMode() {
    setUpTestStream();

    // Add multiple messages
    StreamEntryID id1 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("1-0"), HASH_1);
    StreamEntryID id2 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("2-0"), HASH_2);

    // Consumer group already created, read messages
    Map<String, StreamEntryID> streams = singletonMap(STREAM_KEY_1,
      StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    List<Map.Entry<String, List<StreamEntry>>> messages = jedis.xreadGroup(GROUP_NAME,
      CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(2), streams);

    assertEquals(1, messages.size());
    assertEquals(2, messages.get(0).getValue().size());

    // Test XACKDEL with KEEP_REFERENCES mode
    StreamEntryID readId1 = messages.get(0).getValue().get(0).getID();
    List<StreamEntryDeletionResult> results = jedis.xackdel(STREAM_KEY_1, GROUP_NAME,
      StreamDeletionPolicy.KEEP_REFERENCES, readId1);
    assertThat(results, hasSize(1));
    assertEquals(StreamEntryDeletionResult.DELETED, results.get(0));

    // Verify one message is deleted
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xackdelUnreadMessages() {
    setUpTestStream();

    // Add test entries but don't read them
    StreamEntryID id1 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("1-0"), HASH_1);

    // Test XACKDEL on unread messages - should return NOT_FOUND for PEL
    List<StreamEntryDeletionResult> results = jedis.xackdel(STREAM_KEY_1, GROUP_NAME, id1);

    assertThat(results, hasSize(1));
    // Should return NOT_FOUND because message was never read by the consumer group
    assertEquals(StreamEntryDeletionResult.NOT_FOUND, results.get(0));

    // Stream should still contain the message
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xackdelMultipleMessages() {
    setUpTestStream();

    // Add multiple messages
    StreamEntryID id1 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("1-0"), HASH_1);
    StreamEntryID id2 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("2-0"), HASH_2);
    StreamEntryID id3 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("3-0"), HASH_1);

    // Read all messages
    Map<String, StreamEntryID> streams = singletonMap(STREAM_KEY_1,
      StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    List<Map.Entry<String, List<StreamEntry>>> messages = jedis.xreadGroup(GROUP_NAME,
      CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(3), streams);

    assertEquals(1, messages.size());
    assertEquals(3, messages.get(0).getValue().size());

    // Test XACKDEL with multiple IDs
    StreamEntryID readId1 = messages.get(0).getValue().get(0).getID();
    StreamEntryID readId2 = messages.get(0).getValue().get(1).getID();
    List<StreamEntryDeletionResult> results = jedis.xackdel(STREAM_KEY_1, GROUP_NAME, readId1,
      readId2);
    assertThat(results, hasSize(2));
    assertEquals(StreamEntryDeletionResult.DELETED, results.get(0));
    assertEquals(StreamEntryDeletionResult.DELETED, results.get(1));

    // Verify two messages are deleted
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));
  }

  // ========== XDELEX Command Tests ==========

  @Test
  @SinceRedisVersion("8.1.240")
  public void xdelexBasic() {
    setUpTestStream();

    // Add test entries
    StreamEntryID id1 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("1-0"), HASH_1);
    StreamEntryID id2 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("2-0"), HASH_2);
    assertEquals(2L, jedis.xlen(STREAM_KEY_1));

    // Test basic XDELEX without parameters (should behave like XDEL with KEEP_REFERENCES)
    List<StreamEntryDeletionResult> results = jedis.xdelex(STREAM_KEY_1, id1);
    assertThat(results, hasSize(1));
    assertEquals(StreamEntryDeletionResult.DELETED, results.get(0));

    // Verify entry is deleted from stream
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xdelexWithTrimMode() {
    setUpTestStream();

    // Add test entries
    StreamEntryID id1 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("1-0"), HASH_1);
    StreamEntryID id2 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("2-0"), HASH_2);

    // Test XDELEX with DELETE_REFERENCES mode
    List<StreamEntryDeletionResult> results = jedis.xdelex(STREAM_KEY_1,
      StreamDeletionPolicy.DELETE_REFERENCES, id1);
    assertThat(results, hasSize(1));
    assertEquals(StreamEntryDeletionResult.DELETED, results.get(0));

    // Verify entry is deleted from stream
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xdelexMultipleEntries() {
    setUpTestStream();

    // Add test entries
    StreamEntryID id1 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("1-0"), HASH_1);
    StreamEntryID id2 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("2-0"), HASH_2);
    StreamEntryID id3 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("3-0"), HASH_1);
    assertEquals(3L, jedis.xlen(STREAM_KEY_1));

    // Test XDELEX with multiple IDs
    List<StreamEntryDeletionResult> results = jedis.xdelex(STREAM_KEY_1, id1, id3);
    assertThat(results, hasSize(2));
    assertEquals(StreamEntryDeletionResult.DELETED, results.get(0));
    assertEquals(StreamEntryDeletionResult.DELETED, results.get(1));

    // Verify two entries are deleted
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xdelexNonExistentEntries() {
    setUpTestStream();

    // Add one entry
    StreamEntryID id1 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("1-0"), HASH_1);
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));

    // Test XDELEX with mix of existing and non-existent IDs
    StreamEntryID nonExistentId = new StreamEntryID("999-0");
    List<StreamEntryDeletionResult> results = jedis.xdelex(STREAM_KEY_1, id1, nonExistentId);
    assertThat(results, hasSize(2));
    assertEquals(StreamEntryDeletionResult.DELETED, results.get(0)); // Existing entry
    assertEquals(StreamEntryDeletionResult.NOT_FOUND, results.get(1)); // Non-existent entry

    // Verify existing entry is deleted
    assertEquals(0L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xdelexWithConsumerGroups() {
    setUpTestStream();

    // Add test entries
    StreamEntryID id1 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("1-0"), HASH_1);
    StreamEntryID id2 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("2-0"), HASH_2);

    // Read messages to add them to PEL
    Map<String, StreamEntryID> streams = singletonMap(STREAM_KEY_1,
      StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    List<Map.Entry<String, List<StreamEntry>>> messages = jedis.xreadGroup(GROUP_NAME,
      CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(2), streams);

    assertEquals(1, messages.size());
    assertEquals(2, messages.get(0).getValue().size());

    // Acknowledge only the first message
    StreamEntryID readId1 = messages.get(0).getValue().get(0).getID();
    StreamEntryID readId2 = messages.get(0).getValue().get(1).getID();
    jedis.xack(STREAM_KEY_1, GROUP_NAME, readId1);

    // Test XDELEX with ACKNOWLEDGED mode - should only delete acknowledged entries
    List<StreamEntryDeletionResult> results = jedis.xdelex(STREAM_KEY_1,
      StreamDeletionPolicy.ACKNOWLEDGED, readId1, readId2);
    assertThat(results, hasSize(2));
    assertEquals(StreamEntryDeletionResult.DELETED, results.get(0)); // id1 was acknowledged
    assertEquals(StreamEntryDeletionResult.NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED,
      results.get(1)); // id2 not
    // acknowledged

    // Verify only acknowledged entry was deleted
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xdelexEmptyStream() {
    setUpTestStream();

    // Test XDELEX on empty stream
    StreamEntryID nonExistentId = new StreamEntryID("1-0");
    List<StreamEntryDeletionResult> results = jedis.xdelex(STREAM_KEY_1, nonExistentId);
    assertThat(results, hasSize(1));
    assertEquals(StreamEntryDeletionResult.NOT_FOUND, results.get(0));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void xdelexNotAcknowledged() {
    setUpTestStream();

    String groupName = "test_group";

    // Add initial entries and create consumer group
    Map<String, String> entry1 = singletonMap("field1", "value1");
    jedis.xadd(STREAM_KEY_1, new StreamEntryID("1-0"), entry1);
    jedis.xgroupCreate(STREAM_KEY_1, groupName, new StreamEntryID("0-0"), true);

    // Read one message to create PEL entry
    String consumerName = "consumer1";
    Map<String, StreamEntryID> streamQuery = singletonMap(STREAM_KEY_1,
      StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    jedis.xreadGroup(groupName, consumerName, XReadGroupParams.xReadGroupParams().count(1),
      streamQuery);

    // Add a new entry that was never delivered to any consumer
    Map<String, String> entry2 = singletonMap("field4", "value4");
    StreamEntryID id2 = jedis.xadd(STREAM_KEY_1, new StreamEntryID("2-0"), entry2);

    // Verify initial state
    StreamPendingSummary pending = jedis.xpending(STREAM_KEY_1, groupName);
    assertEquals(1L, pending.getTotal()); // Only id1 is in PEL

    StreamInfo info = jedis.xinfoStream(STREAM_KEY_1);
    assertEquals(2L, info.getLength()); // Stream has 2 entries

    // Test XDELEX with ACKNOWLEDGED policy on entry that was never delivered
    // This should return NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED since id2 was never
    // delivered to any consumer
    List<StreamEntryDeletionResult> result = jedis.xdelex(STREAM_KEY_1,
      StreamDeletionPolicy.ACKNOWLEDGED, id2);
    assertThat(result, hasSize(1));
    assertEquals(StreamEntryDeletionResult.NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED,
      result.get(0));
  }

  // ========== XREADGROUP CLAIM Tests ==========

  private static final String CONSUMER_1 = "consumer-1";
  private static final String CONSUMER_2 = "consumer-2";
  private static final long IDLE_TIME_MS = 5;

  Map<String, StreamEntryID> beforeEachClaimTest() throws InterruptedException {
    setUpTestStream(new StreamEntryID("0-0"));

    // Produce two entries
    jedis.xadd(STREAM_KEY_1, StreamEntryID.NEW_ENTRY, HASH_1);
    jedis.xadd(STREAM_KEY_1, StreamEntryID.NEW_ENTRY, HASH_1);
    Map<String, StreamEntryID> streams = singletonMap(STREAM_KEY_1,
        StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    jedis.xreadGroup(GROUP_NAME, CONSUMER_1, XReadGroupParams.xReadGroupParams().count(10),
        streams);

    // Ensure idle time so entries are claimable
    Thread.sleep(IDLE_TIME_MS);
    return streams;
  }

  @Test
  @SinceRedisVersion(V8_4_0_STRING)
  public void xreadgroupClaimReturnsMetadataOrdered() throws InterruptedException {
    Map<String, StreamEntryID> streams = beforeEachClaimTest();

    // Produce fresh entries that are NOT claimed (not pending)
    jedis.xadd(STREAM_KEY_1, StreamEntryID.NEW_ENTRY, HASH_1);
    jedis.xadd(STREAM_KEY_1, StreamEntryID.NEW_ENTRY, HASH_1);

    // Read with consumer-2 using CLAIM
    List<Map.Entry<String, List<StreamEntry>>> consumer2Result = jedis.xreadGroup(GROUP_NAME,
        CONSUMER_2, XReadGroupParams.xReadGroupParams().claim(IDLE_TIME_MS).count(10), streams);

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
    assertEquals(HASH_1, first.getFields());

    // Fresh entries
    assertFalse(third.isClaimed());
    assertFalse(fourth.isClaimed());
    assertEquals(Long.valueOf(0), third.getDeliveredCount());
    assertEquals(Long.valueOf(0), fourth.getDeliveredCount());
    assertEquals(Long.valueOf(0), third.getMillisElapsedFromDelivery());
    assertEquals(Long.valueOf(0), fourth.getMillisElapsedFromDelivery());
    assertEquals(HASH_1, fourth.getFields());
  }

  @Test
  @SinceRedisVersion(V8_4_0_STRING)
  public void xreadgroupClaimMovesPendingFromC1ToC2AndRemainsPendingUntilAck()
      throws InterruptedException {
    Map<String, StreamEntryID> streams = beforeEachClaimTest();

    // Verify pending belongs to consumer-1
    StreamPendingSummary before = jedis.xpending(STREAM_KEY_1, GROUP_NAME);
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

    // After claim: entries are pending for consumer-2 (moved), not acked yet
    StreamPendingSummary afterClaim = jedis.xpending(STREAM_KEY_1, GROUP_NAME);
    assertEquals(2L, afterClaim.getTotal());
    assertEquals(0L, afterClaim.getConsumerMessageCount().getOrDefault(CONSUMER_1, 0L).longValue());
    assertEquals(2L, afterClaim.getConsumerMessageCount().getOrDefault(CONSUMER_2, 0L).longValue());

    // XACK the claimed entries -> PEL should become empty
    long acked = jedis.xack(STREAM_KEY_1, GROUP_NAME, entries.get(0).getID(),
        entries.get(1).getID());
    assertEquals(2, acked);

    StreamPendingSummary afterAck = jedis.xpending(STREAM_KEY_1, GROUP_NAME);
    assertEquals(0L, afterAck.getTotal());
  }

  @Test
  @SinceRedisVersion(V8_4_0_STRING)
  public void xreadgroupClaimWithNoackDoesNotCreatePendingAndRemovesClaimedFromPel()
      throws InterruptedException {
    Map<String, StreamEntryID> streams = beforeEachClaimTest();

    // Verify pending belongs to consumer-1
    StreamPendingSummary before = jedis.xpending(STREAM_KEY_1, GROUP_NAME);
    assertEquals(2L, before.getTotal());
    assertEquals(2L, before.getConsumerMessageCount().getOrDefault(CONSUMER_1, 0L).longValue());
    assertEquals(0L, before.getConsumerMessageCount().getOrDefault(CONSUMER_2, 0L).longValue());

    // Also produce fresh entries that should not be added to PEL when NOACK is set
    jedis.xadd(STREAM_KEY_1, StreamEntryID.NEW_ENTRY, HASH_1);
    jedis.xadd(STREAM_KEY_1, StreamEntryID.NEW_ENTRY, HASH_1);

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

    // After NOACK read, previously pending entries remain pending (NOACK does not remove them)
    StreamPendingSummary afterNoack = jedis.xpending(STREAM_KEY_1, GROUP_NAME);
    assertEquals(2L, afterNoack.getTotal());

    // Claimed entries remain pending and are now owned by consumer-2 (CLAIM reassigns ownership).
    // Fresh entries were not added to PEL.
    assertEquals(0L, afterNoack.getConsumerMessageCount().getOrDefault(CONSUMER_1, 0L).longValue());
    assertEquals(2L, afterNoack.getConsumerMessageCount().getOrDefault(CONSUMER_2, 0L).longValue());
  }

  @Test
  public void xreadGroupPreservesFieldOrder() {
    String streamKey = "field-order-stream";
    String groupName = "field-order-group";
    String consumerName = "field-order-consumer";

    // Use LinkedHashMap to ensure insertion order: a, z, m
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("a", "1");
    fields.put("z", "4");
    fields.put("m", "2");

    jedis.xadd(streamKey, StreamEntryID.NEW_ENTRY, fields);
    jedis.xgroupCreate(streamKey, groupName, new StreamEntryID("0-0"), false);

    Map<String, StreamEntryID> streamQuery = singletonMap(streamKey, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    List<Map.Entry<String, List<StreamEntry>>> result = jedis.xreadGroup(groupName, consumerName,
        XReadGroupParams.xReadGroupParams().count(1), streamQuery);

    assertEquals(1, result.size());
    assertEquals(1, result.get(0).getValue().size());

    StreamEntry entry = result.get(0).getValue().get(0);
    Map<String, String> returnedFields = entry.getFields();

    // Verify field order is preserved - this will fail with HashMap, pass with LinkedHashMap
    String[] expectedOrder = {"a", "z", "m"};
    String[] actualOrder = returnedFields.keySet().toArray(new String[0]);

    assertEquals(expectedOrder.length, actualOrder.length, "Field count should match");
    for (int i = 0; i < expectedOrder.length; i++) {
      assertEquals(expectedOrder[i], actualOrder[i],
          String.format("Field order mismatch at position %d: expected '%s' but got '%s'. " +
              "Full order: expected [a, z, m], actual %s",
              i, expectedOrder[i], actualOrder[i], java.util.Arrays.toString(actualOrder)));
    }
  }

  @Test
  public void xreadAsMapPreservesStreamOrder() {
    // Test that xreadAsMap preserves the order of streams when reading from multiple streams
    String streamKey1 = "{stream-order}-test-1";
    String streamKey2 = "{stream-order}-test-2";
    String streamKey3 = "{stream-order}-test-3";

    // Add entries to streams in specific order
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("field", "value1");
    jedis.xadd(streamKey1, StreamEntryID.NEW_ENTRY, fields);

    fields.put("field", "value2");
    jedis.xadd(streamKey2, StreamEntryID.NEW_ENTRY, fields);

    fields.put("field", "value3");
    jedis.xadd(streamKey3, StreamEntryID.NEW_ENTRY, fields);

    // Read from multiple streams in specific order
    Map<String, StreamEntryID> streams = new LinkedHashMap<>();
    streams.put(streamKey1, new StreamEntryID("0-0"));
    streams.put(streamKey2, new StreamEntryID("0-0"));
    streams.put(streamKey3, new StreamEntryID("0-0"));

    Map<String, List<StreamEntry>> result = jedis.xreadAsMap(
        XReadParams.xReadParams().count(10), streams);

    assertNotNull(result);
    assertEquals(3, result.size());

    // Verify that the order of streams in the result matches the order in the request
    String[] expectedOrder = {streamKey1, streamKey2, streamKey3};
    String[] actualOrder = result.keySet().toArray(new String[0]);

    assertEquals(expectedOrder.length, actualOrder.length, "Stream count should match");
    for (int i = 0; i < expectedOrder.length; i++) {
      assertEquals(expectedOrder[i], actualOrder[i],
          String.format("Stream order mismatch at position %d: expected '%s' but got '%s'",
              i, expectedOrder[i], actualOrder[i]));
    }
  }

  // ========== Idempotent Producer Tests ==========

  @Test
  @EnabledOnCommand("XCFGSET")
  public void testXaddIdmpAuto() {

    // Add entry with IDMPAUTO
    Map<String, String> message = new HashMap<>();
    message.put("order", "12345");
    message.put("amount", "100.00");

    StreamEntryID id1 = jedis.xadd(STREAM_KEY_1, XAddParams.xAddParams().idmpAuto("producer-1"),
        message);
    assertNotNull(id1);
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));

    // Add same message again with same producer - should be rejected as duplicate
    StreamEntryID id2 = jedis.xadd(STREAM_KEY_1, XAddParams.xAddParams().idmpAuto("producer-1"),
        message);
    assertEquals(id1, id2); // Duplicate returns same ID
    assertEquals(1L, jedis.xlen(STREAM_KEY_1)); // Stream length unchanged

    // Add same message with different producer - should succeed
    StreamEntryID id3 = jedis.xadd(STREAM_KEY_1, XAddParams.xAddParams().idmpAuto("producer-2"),
        message);
    assertNotNull(id3);
    assertEquals(2L, jedis.xlen(STREAM_KEY_1));

    // Add different message with same producer - should succeed
    Map<String, String> message2 = new HashMap<>();
    message2.put("order", "67890");
    message2.put("amount", "200.00");

    StreamEntryID id4 = jedis.xadd(STREAM_KEY_1, XAddParams.xAddParams().idmpAuto("producer-1"),
        message2);
    assertNotNull(id4);
    assertEquals(3L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  @EnabledOnCommand("XCFGSET")
  public void testXaddIdmp() {

    // Add entry with explicit idempotent ID
    StreamEntryID id1 = jedis.xadd(STREAM_KEY_1,
        XAddParams.xAddParams().idmp("producer-1", "iid-001"), HASH_1);
    assertNotNull(id1);
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));

    // Add with same producer and idempotent ID - should be rejected
    StreamEntryID id2 = jedis.xadd(STREAM_KEY_1,
        XAddParams.xAddParams().idmp("producer-1", "iid-001"),
        HASH_2); // Different content, but same IDs
    assertEquals(id1, id2); // Duplicate returns same ID
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));

    // Add with same producer but different idempotent ID - should succeed
    StreamEntryID id3 = jedis.xadd(STREAM_KEY_1,
        XAddParams.xAddParams().idmp("producer-1", "iid-002"), HASH_1);
    assertNotNull(id3);
    assertEquals(2L, jedis.xlen(STREAM_KEY_1));

    // Add with different producer but same idempotent ID - should succeed
    StreamEntryID id4 = jedis.xadd(STREAM_KEY_1,
        XAddParams.xAddParams().idmp("producer-2", "iid-001"), HASH_1);
    assertNotNull(id4);
    assertEquals(3L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  @EnabledOnCommand("XCFGSET")
  public void testXcfgset() {
    // Configure idempotent producer settings
    String result = jedis.xcfgset(STREAM_KEY_1,
        redis.clients.jedis.params.XCfgSetParams.xCfgSetParams().idmpDuration(1000)
            .idmpMaxsize(500));
    assertEquals("OK", result);

    // Verify settings via XINFO STREAM
    StreamInfo info = jedis.xinfoStream(STREAM_KEY_1);
    assertEquals(Long.valueOf(1000), info.getIdmpDuration());
    assertEquals(Long.valueOf(500), info.getIdmpMaxsize());
  }

  @Test
  @EnabledOnCommand("XCFGSET")
  public void testXinfoStreamIdempotentFields() {
    // Configure idempotent settings
    jedis.xcfgset(STREAM_KEY_1,
        redis.clients.jedis.params.XCfgSetParams.xCfgSetParams().idmpDuration(100)
            .idmpMaxsize(100));

    // Add some entries with idempotent IDs
    jedis.xadd(STREAM_KEY_1, XAddParams.xAddParams().idmp("producer-1", "iid-001"), HASH_1);
    jedis.xadd(STREAM_KEY_1, XAddParams.xAddParams().idmp("producer-1", "iid-002"), HASH_2);
    jedis.xadd(STREAM_KEY_1, XAddParams.xAddParams().idmp("producer-2", "iid-001"), HASH_1);

    // Try to add a duplicate
    jedis.xadd(STREAM_KEY_1, XAddParams.xAddParams().idmp("producer-1", "iid-001"), HASH_2);

    // Check XINFO STREAM response
    StreamInfo info = jedis.xinfoStream(STREAM_KEY_1);

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
    StreamEntryID id1 = jedis.xadd(STREAM_KEY_1,
        XAddParams.xAddParams().idmpAuto("producer-1").maxLen(2), HASH_1);
    assertNotNull(id1);

    // Add duplicate - should return same ID and not add new entry
    StreamEntryID id2 = jedis.xadd(STREAM_KEY_1,
        XAddParams.xAddParams().idmpAuto("producer-1").maxLen(2), HASH_1);
    assertEquals(id1, id2); // Duplicate returns same ID
    assertEquals(1, jedis.xlen(STREAM_KEY_1)); // Still 1 entry

    // Add different message - should add new entry and trim
    StreamEntryID id3 = jedis.xadd(STREAM_KEY_1,
        XAddParams.xAddParams().idmpAuto("producer-1").maxLen(2), HASH_2);
    assertNotNull(id3);
    assertNotEquals(id1, id3); // Different IDs
    assertEquals(2, jedis.xlen(STREAM_KEY_1)); // Now 2 entries
  }

  @Test
  @EnabledOnCommand("XCFGSET")
  public void testXcfgsetDefaults() {
    jedis.xadd(STREAM_KEY_1, StreamEntryID.NEW_ENTRY, HASH_1);

    // Verify default values
    StreamInfo info = jedis.xinfoStream(STREAM_KEY_1);
    assertEquals(100L, info.getIdmpDuration());
    assertEquals(100L, info.getIdmpMaxsize());

    assertEquals("OK", jedis.xcfgset(STREAM_KEY_1,
        XCfgSetParams.xCfgSetParams().idmpDuration(200).idmpMaxsize(200)));

    StreamInfo infoAfter = jedis.xinfoStream(STREAM_KEY_1);
    assertEquals(200L, infoAfter.getIdmpDuration());
    assertEquals(200L, infoAfter.getIdmpMaxsize());
  }
}
