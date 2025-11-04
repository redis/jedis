package redis.clients.jedis.commands.unified.pipeline;

import io.redis.test.annotations.SinceRedisVersion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.Response;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.XAddParams;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.resps.StreamEntryBinary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.redis.test.utils.RedisVersion.V8_4_RC1;
import static io.redis.test.utils.RedisVersion.V8_4_RC1_STRING;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static redis.clients.jedis.StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY;
import static redis.clients.jedis.util.StreamEntryBinaryListMatcher.equalsStreamEntries;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class BinaryStreamsPipelineCommandsTest extends PipelineCommandsTestBase {
  protected static final byte[] STREAM_KEY_1 = "{binary-stream}-1".getBytes();
  protected static final byte[] STREAM_KEY_2 = "{binary-stream}-2".getBytes();
  protected static final byte[] GROUP_NAME = "group-1".getBytes();
  protected static final byte[] CONSUMER_NAME = "consumer-1".getBytes();

  protected static final byte[] FIELD_KEY_1 = "binary-field-1".getBytes();
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

  public BinaryStreamsPipelineCommandsTest(RedisProtocol protocol) {
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
  public void xreadBinary() {

    stream1Entries.forEach(
        entry -> jedis.xadd(STREAM_KEY_1, new XAddParams().id(entry.getID()), entry.getFields()));

    Response<List<Map.Entry<byte[], List<StreamEntryBinary>>>> response = pipe.xreadBinary(
        XReadParams.xReadParams(), offsets(STREAM_KEY_1, "0-0"));

    pipe.sync();
    List<Map.Entry<byte[], List<StreamEntryBinary>>> actualEntries = response.get();

    assertThat(actualEntries, hasSize(1));
    assertArrayEquals(STREAM_KEY_1, actualEntries.get(0).getKey());
    assertThat(actualEntries.get(0).getValue(), equalsStreamEntries(stream1Entries));
  }

  @Test
  public void xreadBinaryAsMap() {

    stream1Entries.forEach(
        entry -> jedis.xadd(STREAM_KEY_1, new XAddParams().id(entry.getID()), entry.getFields()));

    Response<Map<byte[], List<StreamEntryBinary>>> response = pipe.xreadBinaryAsMap(
        XReadParams.xReadParams(), offsets(STREAM_KEY_1, "0-0"));

    pipe.sync();
    Map<byte[], List<StreamEntryBinary>> actualEntries = response.get();

    assertThat(actualEntries.entrySet(), hasSize(1));
    assertThat(actualEntries.get(STREAM_KEY_1), equalsStreamEntries(stream1Entries));
  }

  @Test
  public void xreadBinaryAsMapWithMultipleStreams() {

    // Add entries to the streams
    stream1Entries.forEach(
        entry -> jedis.xadd(STREAM_KEY_1, new XAddParams().id(entry.getID()), entry.getFields()));
    stream2Entries.forEach(
        entry -> jedis.xadd(STREAM_KEY_2, new XAddParams().id(entry.getID()), entry.getFields()));

    Response<Map<byte[], List<StreamEntryBinary>>> response = pipe.xreadBinaryAsMap(
        XReadParams.xReadParams(), offsets(STREAM_KEY_1, "0-0", STREAM_KEY_2, "0-0"));

    pipe.sync();
    Map<byte[], List<StreamEntryBinary>> actualEntries = response.get();

    assertThat(actualEntries.entrySet(), hasSize(2));

    assertThat(actualEntries.get(STREAM_KEY_1), equalsStreamEntries(stream1Entries));
    assertThat(actualEntries.get(STREAM_KEY_2), equalsStreamEntries(stream2Entries));
  }

  @Test
  public void xreadGroupBinary() {
    // Add entries to the streams
    stream1Entries.forEach(
        entry -> jedis.xadd(STREAM_KEY_1, new XAddParams().id(entry.getID()), entry.getFields()));

    Response<List<Map.Entry<byte[], List<StreamEntryBinary>>>> response = pipe.xreadGroupBinary(
        GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams(),
        offsets(STREAM_KEY_1, XREADGROUP_UNDELIVERED_ENTRY));

    pipe.sync();
    List<Map.Entry<byte[], List<StreamEntryBinary>>> actualEntries = response.get();

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

    Response<Map<byte[], List<StreamEntryBinary>>> response = pipe.xreadGroupBinaryAsMap(
        GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams(),
        offsets(STREAM_KEY_1, XREADGROUP_UNDELIVERED_ENTRY));

    pipe.sync();
    Map<byte[], List<StreamEntryBinary>> actualEntries = response.get();

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

    Response<Map<byte[], List<StreamEntryBinary>>> response = pipe.xreadGroupBinaryAsMap(GROUP_NAME,
        CONSUMER_NAME, XReadGroupParams.xReadGroupParams(),
        offsets(STREAM_KEY_1, XREADGROUP_UNDELIVERED_ENTRY, STREAM_KEY_2,
            XREADGROUP_UNDELIVERED_ENTRY));

    pipe.sync();
    Map<byte[], List<StreamEntryBinary>> actualEntries = response.get();

    assertThat(actualEntries.entrySet(), hasSize(2));

    assertThat(actualEntries.get(STREAM_KEY_1), equalsStreamEntries(stream1Entries));
    assertThat(actualEntries.get(STREAM_KEY_2), equalsStreamEntries(stream2Entries));
  }

  @Test
  @SinceRedisVersion(V8_4_RC1_STRING)
  public void xreadGroupBinaryWithClaimReturnsPendingThenNewEntries_pipeline() throws InterruptedException {
    // Make 3 entries pending
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("1-0"), HASH_1);
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("2-0"), HASH_2);
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("3-0"), HASH_1);

    Response<List<Map.Entry<byte[], List<StreamEntryBinary>>>> resp = pipe.xreadGroupBinary(
        GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(3),
        offsets(STREAM_KEY_1, XREADGROUP_UNDELIVERED_ENTRY));
    pipe.sync();
    assertThat(resp.get(), hasSize(1));

    Thread.sleep(60);

    // Add two fresh entries
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("4-0"), HASH_1);
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("5-0"), HASH_2);

    Response<List<Map.Entry<byte[], List<StreamEntryBinary>>>> claimResp = pipe.xreadGroupBinary(
        GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(5).claim(50),
        offsets(STREAM_KEY_1, XREADGROUP_UNDELIVERED_ENTRY));
    pipe.sync();

    List<Map.Entry<byte[], List<StreamEntryBinary>>> messages = claimResp.get();
    assertThat(messages, hasSize(1));
    List<StreamEntryBinary> entries = messages.get(0).getValue();
    org.junit.jupiter.api.Assertions.assertEquals(5, entries.size());

    for (int i = 0; i < 3; i++) {
      org.junit.jupiter.api.Assertions.assertNotNull(entries.get(i).getIdleTime());
      org.junit.jupiter.api.Assertions.assertNotNull(entries.get(i).getDeliveredTimes());
    }
    for (int i = 3; i < 5; i++) {
      org.junit.jupiter.api.Assertions.assertNull(entries.get(i).getIdleTime());
      org.junit.jupiter.api.Assertions.assertNull(entries.get(i).getDeliveredTimes());
    }
  }

  @Test
  @SinceRedisVersion(V8_4_RC1_STRING)
  public void xreadGroupBinaryWithClaimNoEligiblePendingReturnsOnlyNewEntries_pipeline() {
    // Make 2 entries pending but below min-idle
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("1-0"), HASH_1);
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("2-0"), HASH_2);

    Response<List<Map.Entry<byte[], List<StreamEntryBinary>>>> resp = pipe.xreadGroupBinary(
        GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(2),
        offsets(STREAM_KEY_1, XREADGROUP_UNDELIVERED_ENTRY));
    pipe.sync();
    assertThat(resp.get(), hasSize(1));

    // Add fresh entries that should be returned
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("3-0"), HASH_1);
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("4-0"), HASH_2);

    Response<List<Map.Entry<byte[], List<StreamEntryBinary>>>> claimResp = pipe.xreadGroupBinary(
        GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(4).claim(500),
        offsets(STREAM_KEY_1, XREADGROUP_UNDELIVERED_ENTRY));
    pipe.sync();

    List<Map.Entry<byte[], List<StreamEntryBinary>>> messages = claimResp.get();
    assertThat(messages, hasSize(1));
    List<StreamEntryBinary> entries = messages.get(0).getValue();
    org.junit.jupiter.api.Assertions.assertEquals(2, entries.size());
    for (StreamEntryBinary e : entries) {
      org.junit.jupiter.api.Assertions.assertNull(e.getIdleTime());
      org.junit.jupiter.api.Assertions.assertNull(e.getDeliveredTimes());
    }
  }

  @Test
  @SinceRedisVersion(V8_4_RC1_STRING)
  public void xreadGroupBinaryWithClaimRespectsCountAndReturnsPendingFirst_pipeline() throws InterruptedException {
    // Make 3 entries pending
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("1-0"), HASH_1);
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("2-0"), HASH_2);
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("3-0"), HASH_1);

    Response<List<Map.Entry<byte[], List<StreamEntryBinary>>>> resp = pipe.xreadGroupBinary(
        GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(3),
        offsets(STREAM_KEY_1, XREADGROUP_UNDELIVERED_ENTRY));
    pipe.sync();
    assertThat(resp.get(), hasSize(1));

    Thread.sleep(60);

    // Add new entries
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("4-0"), HASH_1);
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("5-0"), HASH_2);

    Response<List<Map.Entry<byte[], List<StreamEntryBinary>>>> claimResp = pipe.xreadGroupBinary(
        GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(2).claim(50),
        offsets(STREAM_KEY_1, XREADGROUP_UNDELIVERED_ENTRY));
    pipe.sync();

    List<Map.Entry<byte[], List<StreamEntryBinary>>> messages = claimResp.get();
    assertThat(messages, hasSize(1));
    List<StreamEntryBinary> entries = messages.get(0).getValue();
    org.junit.jupiter.api.Assertions.assertEquals(2, entries.size());
    for (StreamEntryBinary e : entries) {
      org.junit.jupiter.api.Assertions.assertNotNull(e.getIdleTime());
      org.junit.jupiter.api.Assertions.assertNotNull(e.getDeliveredTimes());
    }
  }



  @Test
  @SinceRedisVersion(V8_4_RC1_STRING)
  public void xreadGroupBinaryWithClaimAndNoAckDoesNotAddNewEntriesToPEL_pipeline() throws InterruptedException {
    // Make 3 entries pending
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("1-0"), HASH_1);
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("2-0"), HASH_2);
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("3-0"), HASH_1);

    Response<List<Map.Entry<byte[], List<StreamEntryBinary>>>> first = pipe.xreadGroupBinary(
        GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(3),
        offsets(STREAM_KEY_1, XREADGROUP_UNDELIVERED_ENTRY));
    pipe.sync();
    assertThat(first.get(), hasSize(1));

    // Wait then add fresh entries
    Thread.sleep(60);
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("4-0"), HASH_1);
    jedis.xadd(STREAM_KEY_1, new XAddParams().id("5-0"), HASH_2);

    // Read with CLAIM and NOACK
    Response<List<Map.Entry<byte[], List<StreamEntryBinary>>>> resp = pipe.xreadGroupBinary(
        GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(5).claim(50).noAck(),
        offsets(STREAM_KEY_1, XREADGROUP_UNDELIVERED_ENTRY));
    pipe.sync();

    List<Map.Entry<byte[], List<StreamEntryBinary>>> messages = resp.get();
    assertThat(messages, hasSize(1));
    List<StreamEntryBinary> entries = messages.get(0).getValue();
    org.junit.jupiter.api.Assertions.assertEquals(5, entries.size());
    for (int i = 0; i < 3; i++) {
      org.junit.jupiter.api.Assertions.assertNotNull(entries.get(i).getIdleTime());
      org.junit.jupiter.api.Assertions.assertNotNull(entries.get(i).getDeliveredTimes());
    }
    for (int i = 3; i < 5; i++) {
      org.junit.jupiter.api.Assertions.assertNull(entries.get(i).getIdleTime());
      org.junit.jupiter.api.Assertions.assertNull(entries.get(i).getDeliveredTimes());
    }

    long acked = jedis.xack(STREAM_KEY_1, GROUP_NAME, "4-0".getBytes(), "5-0".getBytes());
    org.junit.jupiter.api.Assertions.assertEquals(0L, acked);
  }

}
