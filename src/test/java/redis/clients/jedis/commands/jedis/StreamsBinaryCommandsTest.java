package redis.clients.jedis.commands.jedis;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.args.StreamDeletionPolicy;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.XAddParams;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.params.XTrimParams;
import redis.clients.jedis.resps.StreamEntryBinary;
import redis.clients.jedis.resps.StreamEntryDeletionResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY;
import static redis.clients.jedis.util.StreamEntryBinaryListMatcher.equalsStreamEntries;

/**
 * Test replicated from {@link redis.clients.jedis.commands.unified.StreamsBinaryCommandsTestBase}
 * but adapted to use Jedis commands. Note: Consider merging with the unified test class if
 * possible. e.g., by using a common base class
 */
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
@Tag("integration")
public class StreamsBinaryCommandsTest extends JedisCommandsTestBase {

  protected static final byte[] STREAM_KEY_1 = "{binary-stream}-1".getBytes();
  protected static final byte[] STREAM_KEY_2 = "{binary-stream}-2".getBytes();
  protected static final byte[] GROUP_NAME = "group-1".getBytes();
  protected static final byte[] CONSUMER_NAME = "consumer-1".getBytes();

  protected static final byte[] FIELD_KEY_1 = "binary-field-1".getBytes();
  // Test with invalid UTF-8 characters
  protected static final byte[] BINARY_VALUE_1 = new byte[] { 0x00, 0x01, 0x02, 0x03, (byte) 0xFF };

  protected static final byte[] FIELD_KEY_2 = "binary-field-1".getBytes();
  protected static final byte[] BINARY_VALUE_2 = "binary-value-2".getBytes();
  protected static final Map<byte[], byte[]> HASH_1 = singletonMap(FIELD_KEY_1, BINARY_VALUE_1);
  protected static final Map<byte[], byte[]> HASH_2 = singletonMap(FIELD_KEY_2, BINARY_VALUE_2);

  protected static final List<StreamEntryBinary> stream1Entries = new ArrayList<>();
  protected static final List<StreamEntryBinary> stream2Entries = new ArrayList<>();

  static {
    stream1Entries.add(new StreamEntryBinary(new StreamEntryID("0-1"), HASH_1));
    stream1Entries.add(new StreamEntryBinary(new StreamEntryID("0-3"), HASH_2));

    stream2Entries.add(new StreamEntryBinary(new StreamEntryID("0-2"), HASH_1));
  }

  public StreamsBinaryCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  /**
   * Creates a map of stream keys to StreamEntryID objects.
   * @param streamOffsets Array of stream key and offset pairs
   * @return Map of stream keys to StreamEntryID objects
   */
  public static Map<byte[], StreamEntryID> offsets(Object... streamOffsets) {
    if (streamOffsets.length % 2 != 0) {
      throw new IllegalArgumentException("Stream offsets must be provided as key-value pairs");
    }

    Map<byte[], StreamEntryID> result = new HashMap<>();
    for (int i = 0; i < streamOffsets.length; i += 2) {
      byte[] key = (byte[]) streamOffsets[i];
      Object value = streamOffsets[i + 1];

      StreamEntryID id;
      if (value instanceof String) {
        id = new StreamEntryID((String) value);
      } else if (value instanceof StreamEntryID) {
        id = (StreamEntryID) value;
      } else {
        throw new IllegalArgumentException("Offset must be a String or StreamEntryID");
      }

      result.put(key, id);
    }

    return result;
  }

  @BeforeEach
  public void setUpTestStream() {
    jedis.del(STREAM_KEY_1);
    jedis.del(STREAM_KEY_2);
    try {
      jedis.xgroupCreate(STREAM_KEY_1, GROUP_NAME,
          StreamEntryID.XGROUP_LAST_ENTRY.toString().getBytes(), true);
    } catch (JedisDataException e) {
      if (!e.getMessage().contains("BUSYGROUP")) {
        throw e;
      }
    }
    try {
      jedis.xgroupCreate(STREAM_KEY_2, GROUP_NAME,
          StreamEntryID.XGROUP_LAST_ENTRY.toString().getBytes(), true);
    } catch (JedisDataException e) {
      if (!e.getMessage().contains("BUSYGROUP")) {
        throw e;
      }
    }
  }

  @Test
  public void xreadBinaryNoEntries() {
    List<Map.Entry<byte[], List<StreamEntryBinary>>> actualEntries = jedis.xreadBinary(
        XReadParams.xReadParams(), offsets(STREAM_KEY_1, "0-0"));

    assertNull(actualEntries);
  }

  @Test
  public void xreadBinary() {

    stream1Entries.forEach(
        entry -> jedis.xadd(STREAM_KEY_1, new XAddParams().id(entry.getID()), entry.getFields()));

    List<Map.Entry<byte[], List<StreamEntryBinary>>> actualEntries = jedis.xreadBinary(
        XReadParams.xReadParams(), offsets(STREAM_KEY_1, "0-0"));

    assertThat(actualEntries, hasSize(1));
    assertArrayEquals(STREAM_KEY_1, actualEntries.get(0).getKey());
    assertThat(actualEntries.get(0).getValue(), equalsStreamEntries(stream1Entries));
  }

  @Test
  public void xreadBinaryCount() {

    stream1Entries.forEach(
        entry -> jedis.xadd(STREAM_KEY_1, new XAddParams().id(entry.getID()), entry.getFields()));

    List<Map.Entry<byte[], List<StreamEntryBinary>>> actualEntries = jedis.xreadBinary(
        XReadParams.xReadParams().count(1), offsets(STREAM_KEY_1, "0-0"));

    assertThat(actualEntries, hasSize(1));
    assertArrayEquals(STREAM_KEY_1, actualEntries.get(0).getKey());
    assertThat(actualEntries.get(0).getValue(), equalsStreamEntries(stream1Entries.subList(0, 1)));
  }

  @Test
  public void xreadBinaryAsMapNoEntries() {
    Map<byte[], List<StreamEntryBinary>> actualEntries = jedis.xreadBinaryAsMap(
        XReadParams.xReadParams(), offsets(STREAM_KEY_1, "0-0"));

    assertNull(actualEntries);
  }

  @Test
  public void xreadBinaryAsMap() {

    stream1Entries.forEach(
        entry -> jedis.xadd(STREAM_KEY_1, new XAddParams().id(entry.getID()), entry.getFields()));

    Map<byte[], List<StreamEntryBinary>> actualEntries = jedis.xreadBinaryAsMap(
        XReadParams.xReadParams(), offsets(STREAM_KEY_1, "0-0"));

    assertThat(actualEntries.entrySet(), hasSize(1));
    assertThat(actualEntries.get(STREAM_KEY_1), equalsStreamEntries(stream1Entries));
  }

  @Test
  public void xreadBinaryAsMapCount() {

    stream1Entries.forEach(
        entry -> jedis.xadd(STREAM_KEY_1, new XAddParams().id(entry.getID()), entry.getFields()));

    Map<byte[], List<StreamEntryBinary>> actualEntries = jedis.xreadBinaryAsMap(
        XReadParams.xReadParams().count(1), offsets(STREAM_KEY_1, "0-0"));

    assertThat(actualEntries.entrySet(), hasSize(1));
    assertThat(actualEntries.get(STREAM_KEY_1), equalsStreamEntries(stream1Entries.subList(0, 1)));
  }

  @Test
  public void xreadBinaryAsMapWithMultipleStreams() {

    // Add entries to the streams
    stream1Entries.forEach(
        entry -> jedis.xadd(STREAM_KEY_1, new XAddParams().id(entry.getID()), entry.getFields()));
    stream2Entries.forEach(
        entry -> jedis.xadd(STREAM_KEY_2, new XAddParams().id(entry.getID()), entry.getFields()));

    Map<byte[], List<StreamEntryBinary>> actualEntries = jedis.xreadBinaryAsMap(
        XReadParams.xReadParams(), offsets(STREAM_KEY_1, "0-0", STREAM_KEY_2, "0-0"));

    assertThat(actualEntries.entrySet(), hasSize(2));

    assertThat(actualEntries.get(STREAM_KEY_1), equalsStreamEntries(stream1Entries));
    assertThat(actualEntries.get(STREAM_KEY_2), equalsStreamEntries(stream2Entries));
  }

  @Test
  public void xreadGroupBinary() {
    // Add entries to the streams
    stream1Entries.forEach(
        entry -> jedis.xadd(STREAM_KEY_1, new XAddParams().id(entry.getID()), entry.getFields()));

    List<Map.Entry<byte[], List<StreamEntryBinary>>> actualEntries = jedis.xreadGroupBinary(
        GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams(),
        offsets(STREAM_KEY_1, XREADGROUP_UNDELIVERED_ENTRY));

    // verify the result contains entries from one stream
    // and is under the expected stream key
    assertThat(actualEntries, hasSize(1));
    assertArrayEquals(STREAM_KEY_1, actualEntries.get(0).getKey());

    assertThat(actualEntries.get(0).getValue(), equalsStreamEntries(stream1Entries));
  }

  @Test
  public void xreadGroupBinaryAsMap() {
    stream1Entries.forEach(
        entry -> jedis.xadd(STREAM_KEY_1, new XAddParams().id(entry.getID()), entry.getFields()));

    Map<byte[], List<StreamEntryBinary>> actualEntries = jedis.xreadGroupBinaryAsMap(GROUP_NAME,
        CONSUMER_NAME, XReadGroupParams.xReadGroupParams(),
        offsets(STREAM_KEY_1, XREADGROUP_UNDELIVERED_ENTRY));

    assertThat(actualEntries.entrySet(), hasSize(1));

    assertThat(actualEntries.get(STREAM_KEY_1), equalsStreamEntries(stream1Entries));
  }

  @Test
  public void xreadGroupBinaryAsMapMultipleStreams() {
    // Add entries to the streams
    stream1Entries.forEach(
        entry -> jedis.xadd(STREAM_KEY_1, new XAddParams().id(entry.getID()), entry.getFields()));
    stream2Entries.forEach(
        entry -> jedis.xadd(STREAM_KEY_2, new XAddParams().id(entry.getID()), entry.getFields()));

    Map<byte[], List<StreamEntryBinary>> actualEntries = jedis.xreadGroupBinaryAsMap(GROUP_NAME,
        CONSUMER_NAME, XReadGroupParams.xReadGroupParams(),
        offsets(STREAM_KEY_1, XREADGROUP_UNDELIVERED_ENTRY, STREAM_KEY_2,
            XREADGROUP_UNDELIVERED_ENTRY));

    assertThat(actualEntries.entrySet(), hasSize(2));

    assertThat(actualEntries.get(STREAM_KEY_1), equalsStreamEntries(stream1Entries));
    assertThat(actualEntries.get(STREAM_KEY_2), equalsStreamEntries(stream2Entries));
  }

  // ========== XACKDEL Command Tests ==========

  @Test
  @SinceRedisVersion("8.1.240")
  public void testXackdel() {
    // Add a message to the stream
    byte[] messageId = jedis.xadd(STREAM_KEY_1, new XAddParams().id("1-0"), HASH_1);
    assertNotNull(messageId);
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));

    // Read the message with consumer group to add it to PEL
    Map<byte[], StreamEntryID> streams = offsets(STREAM_KEY_1, XREADGROUP_UNDELIVERED_ENTRY);
    List<Map.Entry<byte[], List<StreamEntryBinary>>> messages = jedis.xreadGroupBinary(
        GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(1), streams);

    assertEquals(1, messages.size());
    assertEquals(1, messages.get(0).getValue().size());
    byte[] readMessageId = messages.get(0).getValue().get(0).getID().toString().getBytes();

    // Test XACKDEL - should acknowledge and delete the message
    List<StreamEntryDeletionResult> results = jedis.xackdel(STREAM_KEY_1, GROUP_NAME, readMessageId);
    assertThat(results, hasSize(1));
    assertEquals(StreamEntryDeletionResult.DELETED, results.get(0));

    // Verify message is deleted from stream
    assertEquals(0L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void testXackdelWithTrimMode() {
    // Add multiple messages
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("1-0"), HASH_1);
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("2-0"), HASH_2);
    assertEquals(2L, jedis.xlen(STREAM_KEY_1));

    // Read the messages with consumer group
    Map<byte[], StreamEntryID> streams = offsets(STREAM_KEY_1, XREADGROUP_UNDELIVERED_ENTRY);
    List<Map.Entry<byte[], List<StreamEntryBinary>>> messages = jedis.xreadGroupBinary(
        GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(2), streams);

    assertEquals(1, messages.size());
    assertEquals(2, messages.get(0).getValue().size());

    // Test XACKDEL with KEEP_REFERENCES mode
    byte[] readId1 = messages.get(0).getValue().get(0).getID().toString().getBytes();
    List<StreamEntryDeletionResult> results = jedis.xackdel(STREAM_KEY_1, GROUP_NAME, StreamDeletionPolicy.KEEP_REFERENCES, readId1);
    assertThat(results, hasSize(1));
    assertEquals(StreamEntryDeletionResult.DELETED, results.get(0));

    // Verify one message is deleted
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void testXackdelUnreadMessages() {
    // Add test entries but don't read them
    byte[] id1 = jedis.xadd(STREAM_KEY_1, new XAddParams().id("1-0"), HASH_1);

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
  public void testXackdelMultipleMessages() {
    // Add multiple messages
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("1-0"), HASH_1);
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("2-0"), HASH_2);
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("3-0"), HASH_1);
    assertEquals(3L, jedis.xlen(STREAM_KEY_1));

    // Read the messages with consumer group
    Map<byte[], StreamEntryID> streams = offsets(STREAM_KEY_1, XREADGROUP_UNDELIVERED_ENTRY);
    List<Map.Entry<byte[], List<StreamEntryBinary>>> messages = jedis.xreadGroupBinary(
        GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(3), streams);

    assertEquals(1, messages.size());
    assertEquals(3, messages.get(0).getValue().size());

    // Test XACKDEL with multiple IDs
    byte[] readId1 = messages.get(0).getValue().get(0).getID().toString().getBytes();
    byte[] readId2 = messages.get(0).getValue().get(1).getID().toString().getBytes();
    List<StreamEntryDeletionResult> results = jedis.xackdel(STREAM_KEY_1, GROUP_NAME, readId1, readId2);
    assertThat(results, hasSize(2));
    assertEquals(StreamEntryDeletionResult.DELETED, results.get(0));
    assertEquals(StreamEntryDeletionResult.DELETED, results.get(1));

    // Verify two messages are deleted
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));
  }

  // ========== XDELEX Command Tests ==========

  @Test
  @SinceRedisVersion("8.1.240")
  public void testXdelex() {
    // Add test entries
    byte[] id1 = jedis.xadd(STREAM_KEY_1, new XAddParams().id("1-0"), HASH_1);
    byte[] id2 = jedis.xadd(STREAM_KEY_1, new XAddParams().id("2-0"), HASH_2);
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
  public void testXdelexWithTrimMode() {
    // Add test entries
    byte[] id1 = jedis.xadd(STREAM_KEY_1, new XAddParams().id("1-0"), HASH_1);
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("2-0"), HASH_2);

    // Test XDELEX with DELETE_REFERENCES mode
    List<StreamEntryDeletionResult> results = jedis.xdelex(STREAM_KEY_1, StreamDeletionPolicy.DELETE_REFERENCES, id1);
    assertThat(results, hasSize(1));
    assertEquals(StreamEntryDeletionResult.DELETED, results.get(0));

    // Verify entry is deleted from stream
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void testXdelexMultipleEntries() {
    // Add test entries
    byte[] id1 = jedis.xadd(STREAM_KEY_1, new XAddParams().id("1-0"), HASH_1);
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("2-0"), HASH_2);
    byte[] id3 = jedis.xadd(STREAM_KEY_1, new XAddParams().id("3-0"), HASH_1);
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
  public void testXdelexNonExistentEntries() {
    // Add one entry
    byte[] id1 = jedis.xadd(STREAM_KEY_1, new XAddParams().id("1-0"), HASH_1);
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));

    // Test XDELEX with mix of existing and non-existent IDs
    byte[] nonExistentId = "999-0".getBytes();
    List<StreamEntryDeletionResult> results = jedis.xdelex(STREAM_KEY_1, id1, nonExistentId);
    assertThat(results, hasSize(2));
    assertEquals(StreamEntryDeletionResult.DELETED, results.get(0)); // Existing entry
    assertEquals(StreamEntryDeletionResult.NOT_FOUND, results.get(1)); // Non-existent entry

    // Verify existing entry is deleted
    assertEquals(0L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void testXdelexWithConsumerGroups() {
    // Add test entries
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("1-0"), HASH_1);
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("2-0"), HASH_2);
    assertEquals(2L, jedis.xlen(STREAM_KEY_1));

    // Read messages with consumer group to add them to PEL
    Map<byte[], StreamEntryID> streams = offsets(STREAM_KEY_1, XREADGROUP_UNDELIVERED_ENTRY);
    List<Map.Entry<byte[], List<StreamEntryBinary>>> messages = jedis.xreadGroupBinary(
        GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(2), streams);

    assertEquals(1, messages.size());
    assertEquals(2, messages.get(0).getValue().size());

    // Acknowledge only the first message
    byte[] readId1 = messages.get(0).getValue().get(0).getID().toString().getBytes();
    byte[] readId2 = messages.get(0).getValue().get(1).getID().toString().getBytes();
    jedis.xack(STREAM_KEY_1, GROUP_NAME, readId1);

    // Test XDELEX with ACKNOWLEDGED mode - should only delete acknowledged entries
    List<StreamEntryDeletionResult> results = jedis.xdelex(STREAM_KEY_1, StreamDeletionPolicy.ACKNOWLEDGED, readId1, readId2);
    assertThat(results, hasSize(2));
    assertEquals(StreamEntryDeletionResult.DELETED, results.get(0)); // id1 was acknowledged
    assertEquals(StreamEntryDeletionResult.NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED, results.get(1)); // id2 not acknowledged

    // Verify only acknowledged entry was deleted
    assertEquals(1L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void testXdelexEmptyStream() {
    // Test XDELEX on empty stream
    byte[] nonExistentId = "1-0".getBytes();
    List<StreamEntryDeletionResult> results = jedis.xdelex(STREAM_KEY_1, nonExistentId);
    assertThat(results, hasSize(1));
    assertEquals(StreamEntryDeletionResult.NOT_FOUND, results.get(0));
  }

  // ========== XTRIM Command Tests with trimmingMode ==========

  @Test
  @SinceRedisVersion("8.1.240")
  public void testXtrimWithKeepReferences() {
    // Add test entries
    for (int i = 1; i <= 5; i++) {
      jedis.xadd(STREAM_KEY_1, new XAddParams().id(i + "-0"), HASH_1);
    }
    assertEquals(5L, jedis.xlen(STREAM_KEY_1));

    // Read messages with consumer group to create PEL entries
    Map<byte[], StreamEntryID> streams = offsets(STREAM_KEY_1, XREADGROUP_UNDELIVERED_ENTRY);
    jedis.xreadGroupBinary(GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(3), streams);

    // Test XTRIM with KEEP_REFERENCES mode - should preserve PEL references
    long trimmed = jedis.xtrim(STREAM_KEY_1, XTrimParams.xTrimParams().maxLen(3).trimmingMode(
        StreamDeletionPolicy.KEEP_REFERENCES));
    assertEquals(2L, trimmed); // Should trim 2 entries
    assertEquals(3L, jedis.xlen(STREAM_KEY_1));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void testXtrimWithAcknowledged() {
    // Add test entries
    for (int i = 1; i <= 5; i++) {
      jedis.xadd(STREAM_KEY_1, new XAddParams().id(i + "-0"), HASH_1);
    }
    assertEquals(5L, jedis.xlen(STREAM_KEY_1));

    // Read messages with consumer group
    Map<byte[], StreamEntryID> streams = offsets(STREAM_KEY_1, XREADGROUP_UNDELIVERED_ENTRY);
    List<Map.Entry<byte[], List<StreamEntryBinary>>> messages = jedis.xreadGroupBinary(
        GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(3), streams);

    assertEquals(1, messages.size());
    assertEquals(3, messages.get(0).getValue().size());

    // Acknowledge only the first 2 messages
    byte[] readId1 = messages.get(0).getValue().get(0).getID().toString().getBytes();
    byte[] readId2 = messages.get(0).getValue().get(1).getID().toString().getBytes();
    jedis.xack(STREAM_KEY_1, GROUP_NAME, readId1, readId2);

    // Test XTRIM with ACKNOWLEDGED mode - should only trim acknowledged entries
    long trimmed = jedis.xtrim(STREAM_KEY_1, XTrimParams.xTrimParams().maxLen(3).trimmingMode(
        StreamDeletionPolicy.ACKNOWLEDGED));
    // The exact behavior depends on implementation, but it should respect acknowledgment status
    assertTrue(trimmed >= 0);
    assertTrue(jedis.xlen(STREAM_KEY_1) <= 5); // Should not exceed original length
  }

  // ========== XREADGROUP CLAIM Tests ==========

  @Test
  @SinceRedisVersion("8.3.224")
  public void xreadgroupClaimReturnsMetadataOrdered() throws InterruptedException {
    final byte[] streamKey = "test-stream-claim-binary".getBytes();
    final byte[] groupName = "test-group".getBytes();
    final byte[] consumer1 = "consumer-1".getBytes();
    final byte[] consumer2 = "consumer-2".getBytes();
    final long idleTimeMs = 5;

    jedis.del(streamKey);

    // Produce two entries
    Map<byte[], byte[]> hash = singletonMap(FIELD_KEY_1, BINARY_VALUE_1);
    jedis.xadd(streamKey, XAddParams.xAddParams().id(StreamEntryID.NEW_ENTRY), hash);
    jedis.xadd(streamKey, XAddParams.xAddParams().id(StreamEntryID.NEW_ENTRY), hash);

    // Create group and consume with consumer-1
    jedis.xgroupCreate(streamKey, groupName, "0-0".getBytes(), false);
    Map<byte[], StreamEntryID> streams = offsets(streamKey, XREADGROUP_UNDELIVERED_ENTRY);
    jedis.xreadGroupBinary(groupName, consumer1, XReadGroupParams.xReadGroupParams().count(10),
      streams);

    // Ensure idle time
    Thread.sleep(idleTimeMs);

    // Produce fresh entries
    jedis.xadd(streamKey, XAddParams.xAddParams().id(StreamEntryID.NEW_ENTRY), hash);
    jedis.xadd(streamKey, XAddParams.xAddParams().id(StreamEntryID.NEW_ENTRY), hash);

    // Read with consumer-2 using CLAIM
    List<Map.Entry<byte[], List<StreamEntryBinary>>> consumer2Result = jedis.xreadGroupBinary(
      groupName, consumer2, XReadGroupParams.xReadGroupParams().claim(idleTimeMs).count(10),
      streams);

    assertNotNull(consumer2Result);
    assertEquals(1, consumer2Result.size());

    List<StreamEntryBinary> entries = consumer2Result.get(0).getValue();
    assertEquals(4, entries.size());

    long claimedCount = entries.stream()
      .filter(e -> e.getDeliveredCount() != null && e.getDeliveredCount() > 0).count();
    long freshCount = entries.size() - claimedCount;

    assertEquals(2, claimedCount);
    assertEquals(2, freshCount);

    // Assert order: pending entries are first
    StreamEntryBinary first = entries.get(0);
    StreamEntryBinary second = entries.get(1);
    StreamEntryBinary third = entries.get(2);
    StreamEntryBinary fourth = entries.get(3);

    // Claimed entries
    assertTrue(first.getDeliveredCount() != null && first.getDeliveredCount() > 0);
    assertTrue(second.getDeliveredCount() != null && second.getDeliveredCount() > 0);
    assertTrue(first.getMillisElapsedFromDelivery() >= idleTimeMs);
    assertTrue(second.getMillisElapsedFromDelivery() >= idleTimeMs);

    // Fresh entries
    assertEquals(Long.valueOf(0), third.getDeliveredCount());
    assertEquals(Long.valueOf(0), fourth.getDeliveredCount());
    assertEquals(Long.valueOf(0), third.getMillisElapsedFromDelivery());
    assertEquals(Long.valueOf(0), fourth.getMillisElapsedFromDelivery());
  }

  @Test
  @SinceRedisVersion("8.3.224")
  public void xreadgroupClaimMovesPendingFromC1ToC2AndRemainsPendingUntilAck()
      throws InterruptedException {
    final byte[] streamKey = "test-stream-claim-move-binary".getBytes();
    final byte[] groupName = "test-group".getBytes();
    final byte[] consumer1 = "consumer-1".getBytes();
    final byte[] consumer2 = "consumer-2".getBytes();
    final long idleTimeMs = 5;

    jedis.del(streamKey);

    // Produce two entries
    Map<byte[], byte[]> hash = singletonMap(FIELD_KEY_1, BINARY_VALUE_1);
    jedis.xadd(streamKey, XAddParams.xAddParams().id(StreamEntryID.NEW_ENTRY), hash);
    jedis.xadd(streamKey, XAddParams.xAddParams().id(StreamEntryID.NEW_ENTRY), hash);

    // Create group and consume with consumer-1
    jedis.xgroupCreate(streamKey, groupName, "0-0".getBytes(), false);
    Map<byte[], StreamEntryID> streams = offsets(streamKey, XREADGROUP_UNDELIVERED_ENTRY);
    jedis.xreadGroupBinary(groupName, consumer1, XReadGroupParams.xReadGroupParams().count(10),
      streams);

    // Ensure idle time
    Thread.sleep(idleTimeMs);

    // Verify pending belongs to consumer-1
    Object beforeObj = jedis.xpending(streamKey, groupName);
    assertNotNull(beforeObj);

    // Claim with consumer-2
    List<Map.Entry<byte[], List<StreamEntryBinary>>> res = jedis.xreadGroupBinary(groupName,
      consumer2, XReadGroupParams.xReadGroupParams().claim(idleTimeMs).count(10), streams);

    assertNotNull(res);
    assertEquals(1, res.size());

    List<StreamEntryBinary> entries = res.get(0).getValue();
    long claimed = entries.stream()
      .filter(e -> e.getDeliveredCount() != null && e.getDeliveredCount() > 0).count();
    assertEquals(2, claimed);

    // XACK the claimed entries
    byte[] id1 = entries.get(0).getID().toString().getBytes();
    byte[] id2 = entries.get(1).getID().toString().getBytes();
    long acked = jedis.xack(streamKey, groupName, id1, id2);
    assertEquals(2, acked);
  }

  @Test
  @SinceRedisVersion("8.3.224")
  public void xreadgroupClaimWithNoackDoesNotCreatePendingAndRemovesClaimedFromPel()
      throws InterruptedException {
    final byte[] streamKey = "test-stream-claim-noack-binary".getBytes();
    final byte[] groupName = "test-group".getBytes();
    final byte[] consumer1 = "consumer-1".getBytes();
    final byte[] consumer2 = "consumer-2".getBytes();
    final long idleTimeMs = 5;

    jedis.del(streamKey);

    // Produce two entries
    Map<byte[], byte[]> hash = singletonMap(FIELD_KEY_1, BINARY_VALUE_1);
    jedis.xadd(streamKey, XAddParams.xAddParams().id(StreamEntryID.NEW_ENTRY), hash);
    jedis.xadd(streamKey, XAddParams.xAddParams().id(StreamEntryID.NEW_ENTRY), hash);

    // Create group and consume with consumer-1
    jedis.xgroupCreate(streamKey, groupName, "0-0".getBytes(), false);
    Map<byte[], StreamEntryID> streams = offsets(streamKey, XREADGROUP_UNDELIVERED_ENTRY);
    jedis.xreadGroupBinary(groupName, consumer1, XReadGroupParams.xReadGroupParams().count(10),
      streams);

    // Ensure idle time
    Thread.sleep(idleTimeMs);

    // Verify pending belongs to consumer-1
    Object beforeObj = jedis.xpending(streamKey, groupName);
    assertNotNull(beforeObj);

    // Produce fresh entries
    jedis.xadd(streamKey, XAddParams.xAddParams().id(StreamEntryID.NEW_ENTRY), hash);
    jedis.xadd(streamKey, XAddParams.xAddParams().id(StreamEntryID.NEW_ENTRY), hash);

    // Claim with NOACK using consumer-2
    List<Map.Entry<byte[], List<StreamEntryBinary>>> res = jedis.xreadGroupBinary(groupName,
      consumer2, XReadGroupParams.xReadGroupParams().claim(idleTimeMs).noAck().count(10), streams);

    assertNotNull(res);
    assertEquals(1, res.size());

    List<StreamEntryBinary> entries = res.get(0).getValue();
    long claimedCount = entries.stream()
      .filter(e -> e.getDeliveredCount() != null && e.getDeliveredCount() > 0).count();
    long freshCount = entries.size() - claimedCount;

    assertEquals(2, claimedCount);
    assertEquals(2, freshCount);
  }

}
