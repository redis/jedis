package redis.clients.jedis.commands.unified;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.XAddParams;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.resps.StreamEntryBinary;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public abstract class BinaryStreamsCommandsTestBase extends UnifiedJedisCommandsTestBase {

  protected static final byte[] STREAM_KEY = "test-binary-stream".getBytes();
  protected static final byte[] FIELD_KEY = "binary-field".getBytes();
  protected static final byte[] BINARY_VALUE = new byte[] { 0x00, 0x01, 0x02, 0x03, (byte) 0xFF };

  public BinaryStreamsCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testBinaryStreamEntry() {
    Map<byte[], byte[]> hash = new HashMap<>();
    hash.put(FIELD_KEY, BINARY_VALUE);

    byte[] idBytes = jedis.xadd(STREAM_KEY, new XAddParams(), hash);
    StreamEntryID id = new StreamEntryID(new String(idBytes));
    assertNotNull(id);

    Map.Entry<byte[], byte[]> streamEntry = new AbstractMap.SimpleImmutableEntry<>(STREAM_KEY, "0-0".getBytes());
    List<Map.Entry<byte[], List<StreamEntryBinary>>> result = jedis.xreadBinary(
        XReadParams.xReadParams().count(1), streamEntry);

    assertNotNull(result);
    assertEquals(1, result.size());

    Map.Entry<byte[], List<StreamEntryBinary>> streamData = result.get(0);
    assertArrayEquals(STREAM_KEY, streamData.getKey());

    List<StreamEntryBinary> entries = streamData.getValue();
    assertNotNull(entries);
    assertEquals(1, entries.size());

    StreamEntryBinary entry = entries.get(0);
    verifyBinaryFields(entry.getFields());

    Map<byte[], List<StreamEntryBinary>> mapResult = jedis.xreadBinaryAsMap(
        XReadParams.xReadParams().count(1), streamEntry);

    assertNotNull(mapResult);
    assertEquals(1, mapResult.size());

    boolean keyFound = false;
    for (byte[] key : mapResult.keySet()) {
      if (Arrays.equals(key, STREAM_KEY)) {
        keyFound = true;
        List<StreamEntryBinary> mapEntries = mapResult.get(key);
        assertNotNull(mapEntries);
        assertEquals(1, mapEntries.size());
        verifyBinaryFields(mapEntries.get(0).getFields());
        break;
      }
    }
    assertTrue(keyFound);
  }

  @Test
  public void testBinaryStreamEntryWithGroup() {
    byte[] streamKey = UUID.randomUUID().toString().getBytes();
    byte[] groupName = UUID.randomUUID().toString().getBytes();
    byte[] consumerName = UUID.randomUUID().toString().getBytes();

    Map<byte[], byte[]> hash = new HashMap<>();
    hash.put(FIELD_KEY, BINARY_VALUE);

    byte[] idBytes = jedis.xadd(streamKey, new XAddParams(), hash);
    StreamEntryID id = new StreamEntryID(new String(idBytes));
    assertNotNull(id);

    try {
      jedis.xgroupCreate(streamKey, groupName, "0-0".getBytes(), true);
    } catch (JedisDataException e) {
      if (!e.getMessage().contains("BUSYGROUP")) {
        throw e;
      }
    }

    Map.Entry<byte[], byte[]> streamEntry = new AbstractMap.SimpleImmutableEntry<>(streamKey, ">".getBytes());

    List<Map.Entry<byte[], List<StreamEntryBinary>>> result = jedis.xreadGroupBinary(
            groupName, consumerName, XReadGroupParams.xReadGroupParams().count(1), streamEntry);

    assertNotNull(result);
    assertEquals(1, result.size());

    Map.Entry<byte[], List<StreamEntryBinary>> streamData = result.get(0);
    assertArrayEquals(streamKey, streamData.getKey());

    List<StreamEntryBinary> entries = streamData.getValue();
    assertNotNull(entries);
    assertEquals(1, entries.size());
    StreamEntryBinary entry = entries.get(0);

    verifyBinaryFields(entry.getFields());
  }

  @Test
  public void testMultipleBinaryStreamEntries() {
    byte[] streamKey = UUID.randomUUID().toString().getBytes();

    Map<byte[], byte[]> hash1 = new HashMap<>();
    hash1.put(FIELD_KEY, BINARY_VALUE);
    hash1.put("field2".getBytes(), "value2".getBytes());

    Map<byte[], byte[]> hash2 = new HashMap<>();
    hash2.put("field3".getBytes(), "different".getBytes());
    hash2.put("field4".getBytes(), new byte[] { 0x10, 0x20, 0x30 });

    jedis.xadd(streamKey, new XAddParams(), hash1);
    jedis.xadd(streamKey, new XAddParams(), hash2);

    Map.Entry<byte[], byte[]> streamEntry = new AbstractMap.SimpleImmutableEntry<>(streamKey, "0-0".getBytes());
    List<Map.Entry<byte[], List<StreamEntryBinary>>> result = jedis.xreadBinary(
            XReadParams.xReadParams().count(2), streamEntry);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(2, result.get(0).getValue().size());
  }

  protected void verifyBinaryFields(Map<byte[], byte[]> fields) {
    assertNotNull(fields);

    boolean fieldExists = false;
    for (byte[] key : fields.keySet()) {
      if (Arrays.equals(key, FIELD_KEY)) {
        fieldExists = true;
        break;
      }
    }
    assertTrue(fieldExists);

    byte[] value = null;
    for (Map.Entry<byte[], byte[]> field : fields.entrySet()) {
      if (Arrays.equals(field.getKey(), FIELD_KEY)) {
        value = field.getValue();
        break;
      }
    }

    assertNotNull(value);
    assertEquals(BINARY_VALUE.length, value.length);

    for (int i = 0; i < BINARY_VALUE.length; i++) {
      assertEquals(BINARY_VALUE[i], value[i]);
    }
  }
}
