package redis.clients.jedis.commands.unified.pipeline;

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
    client.del(STREAM_KEY_1);
    client.del(STREAM_KEY_2);
    try {
      client.xgroupCreate(STREAM_KEY_1, GROUP_NAME,
          StreamEntryID.XGROUP_LAST_ENTRY.toString().getBytes(), true);
    } catch (JedisDataException e) {
      if (!e.getMessage().contains("BUSYGROUP")) {
        throw e;
      }
    }
    try {
      client.xgroupCreate(STREAM_KEY_2, GROUP_NAME,
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
        entry -> client.xadd(STREAM_KEY_1, new XAddParams().id(entry.getID()), entry.getFields()));

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
        entry -> client.xadd(STREAM_KEY_1, new XAddParams().id(entry.getID()), entry.getFields()));

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
        entry -> client.xadd(STREAM_KEY_1, new XAddParams().id(entry.getID()), entry.getFields()));
    stream2Entries.forEach(
        entry -> client.xadd(STREAM_KEY_2, new XAddParams().id(entry.getID()), entry.getFields()));

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
        entry -> client.xadd(STREAM_KEY_1, new XAddParams().id(entry.getID()), entry.getFields()));

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
        entry -> client.xadd(STREAM_KEY_1, new XAddParams().id(entry.getID()), entry.getFields()));

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
        entry -> client.xadd(STREAM_KEY_1, new XAddParams().id(entry.getID()), entry.getFields()));
    stream2Entries.forEach(
        entry -> client.xadd(STREAM_KEY_2, new XAddParams().id(entry.getID()), entry.getFields()));

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

}
