package redis.clients.jedis.commands.binary;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.XAddParams;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.resps.StreamEntryBinary;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SinceRedisVersion("6.0.0")
public class BinaryStreamEntryTest {

  private static final byte[] STREAM_KEY = "test-binary-stream".getBytes();
  private static final byte[] GROUP_NAME = "test-group".getBytes();
  private static final byte[] CONSUMER_NAME = "test-consumer".getBytes();
  private static final byte[] FIELD_KEY = "binary-field".getBytes();
  private static final byte[] BINARY_VALUE = new byte[] { 0x00, 0x01, 0x02, 0x03, (byte) 0xFF };

  private static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0");

  @Test
  public void testBinaryStreamEntry() {
    // Add binary data to stream
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder()
            .protocol(RedisProtocol.RESP3).timeoutMillis(5000).password(endpoint.getPassword()).build())) {

      Map<byte[], byte[]> hash = new HashMap<>();
      hash.put(FIELD_KEY, BINARY_VALUE);

      byte[] idBytes = jedis.xadd(STREAM_KEY, new XAddParams(), hash);
      StreamEntryID id = new StreamEntryID(new String(idBytes));
      assertNotNull(id);

      // Read the data back using xreadBinary
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

      // Test xreadBinaryAsMap
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

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testBinaryStreamEntryWithGroup() {
    // Add binary data to stream
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder()
            .protocol(RedisProtocol.RESP3).timeoutMillis(5000).password(endpoint.getPassword()).build())) {

      Map<byte[], byte[]> hash = new HashMap<>();
      hash.put(FIELD_KEY, BINARY_VALUE);

      byte[] idBytes = jedis.xadd(STREAM_KEY, new XAddParams(), hash);
      StreamEntryID id = new StreamEntryID(new String(idBytes));
      assertNotNull(id);

      // Create a consumer group
      try {
        jedis.xgroupCreate(STREAM_KEY, GROUP_NAME, "0-0".getBytes(), true);
      } catch (JedisDataException e) {
        // Ignore BUSYGROUP error
        if (!e.getMessage().contains("BUSYGROUP")) {
          throw e;
        }
      }

      // Read the data back using xreadGroupBinary
      Map.Entry<byte[], byte[]> streamEntry = new AbstractMap.SimpleImmutableEntry<>(STREAM_KEY, ">".getBytes());

      List<Map.Entry<byte[], List<StreamEntryBinary>>> result = jedis.xreadGroupBinary(
              GROUP_NAME, CONSUMER_NAME, XReadGroupParams.xReadGroupParams().count(1), streamEntry);

      assertNotNull(result);
      assertEquals(1, result.size());

      Map.Entry<byte[], List<StreamEntryBinary>> streamData = result.get(0);
      assertArrayEquals(STREAM_KEY, streamData.getKey());

      List<StreamEntryBinary> entries = streamData.getValue();
      assertNotNull(entries);
      assertEquals(1, entries.size());
      StreamEntryBinary entry = entries.get(0);

      verifyBinaryFields(entry.getFields());

    } catch (Exception e) {
      fail("failed to read binary data from stream: " + e.getMessage());
    }
  }

  private void verifyBinaryFields(Map<byte[], byte[]> fields) {
    assertNotNull(fields);

    // Check if the field exists using Arrays.equals since byte[] equality is based on reference
    boolean fieldExists = false;
    for (byte[] key : fields.keySet()) {
      if (Arrays.equals(key, FIELD_KEY)) {
        fieldExists = true;
        break;
      }
    }
    assertTrue(fieldExists);

    // Get the value using the field key
    byte[] value = null;
    for (Map.Entry<byte[], byte[]> field : fields.entrySet()) {
      if (Arrays.equals(field.getKey(), FIELD_KEY)) {
        value = field.getValue();
        break;
      }
    }

    assertNotNull(value);
    assertEquals(BINARY_VALUE.length, value.length);

    // Verify the binary data is preserved correctly
    for (int i = 0; i < BINARY_VALUE.length; i++) {
      assertEquals(BINARY_VALUE[i], value[i]);
    }
  }
}
