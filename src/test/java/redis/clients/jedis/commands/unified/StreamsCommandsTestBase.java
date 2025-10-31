package redis.clients.jedis.commands.unified;

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
import redis.clients.jedis.params.XAddParams;
import redis.clients.jedis.params.XPendingParams;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.params.XTrimParams;
import redis.clients.jedis.resps.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    jedis.del(STREAM_KEY_1);
    jedis.del(STREAM_KEY_2);
    try {
      jedis.xgroupCreate(STREAM_KEY_1, GROUP_NAME, StreamEntryID.XGROUP_LAST_ENTRY, true);
    } catch (JedisDataException e) {
      if (!e.getMessage().contains("BUSYGROUP")) {
        throw e;
      }
    }
    try {
      jedis.xgroupCreate(STREAM_KEY_2, GROUP_NAME, StreamEntryID.XGROUP_LAST_ENTRY, true);
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
}
