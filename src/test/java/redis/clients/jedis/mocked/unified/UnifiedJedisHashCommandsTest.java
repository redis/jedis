package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public class UnifiedJedisHashCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testHdel() {
    String key = "hashKey";
    String[] fields = { "field1", "field2" };
    long expected = 2L;

    when(commandObjects.hdel(key, fields)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.hdel(key, fields);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).hdel(key, fields);
  }

  @Test
  public void testHdelBinary() {
    byte[] key = "hashKey".getBytes();
    byte[][] fields = { "field1".getBytes(), "field2".getBytes() };
    long expected = 2L; // Assuming both fields were deleted

    when(commandObjects.hdel(key, fields)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.hdel(key, fields);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).hdel(key, fields);
  }

  @Test
  public void testHexists() {
    String key = "hashKey";
    String field = "field1";
    boolean expected = true;

    when(commandObjects.hexists(key, field)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(expected);

    boolean result = jedis.hexists(key, field);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).hexists(key, field);
  }

  @Test
  public void testHexistsBinary() {
    byte[] key = "hashKey".getBytes();
    byte[] field = "field1".getBytes();
    boolean expected = true;

    when(commandObjects.hexists(key, field)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(expected);

    boolean result = jedis.hexists(key, field);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).hexists(key, field);
  }

  @Test
  public void testHget() {
    String key = "hashKey";
    String field = "field1";
    String expectedValue = "value1";

    when(commandObjects.hget(key, field)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedValue);

    String result = jedis.hget(key, field);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).hget(key, field);
  }

  @Test
  public void testHgetBinary() {
    byte[] key = "hashKey".getBytes();
    byte[] field = "field1".getBytes();
    byte[] expectedValue = "value1".getBytes();

    when(commandObjects.hget(key, field)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedValue);

    byte[] result = jedis.hget(key, field);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).hget(key, field);
  }

  @Test
  public void testHgetAll() {
    String key = "hashKey";
    Map<String, String> expectedMap = new HashMap<>();
    expectedMap.put("field1", "value1");
    expectedMap.put("field2", "value2");

    when(commandObjects.hgetAll(key)).thenReturn(mapStringStringCommandObject);
    when(commandExecutor.executeCommand(mapStringStringCommandObject)).thenReturn(expectedMap);

    Map<String, String> result = jedis.hgetAll(key);

    assertThat(result, equalTo(expectedMap));

    verify(commandExecutor).executeCommand(mapStringStringCommandObject);
    verify(commandObjects).hgetAll(key);
  }

  @Test
  public void testHgetAllBinary() {
    byte[] key = "hashKey".getBytes();
    Map<byte[], byte[]> expectedMap = new HashMap<>();
    expectedMap.put("field1".getBytes(), "value1".getBytes());
    expectedMap.put("field2".getBytes(), "value2".getBytes());

    when(commandObjects.hgetAll(key)).thenReturn(mapBytesBytesCommandObject);
    when(commandExecutor.executeCommand(mapBytesBytesCommandObject)).thenReturn(expectedMap);

    Map<byte[], byte[]> result = jedis.hgetAll(key);

    assertThat(result, equalTo(expectedMap));

    verify(commandExecutor).executeCommand(mapBytesBytesCommandObject);
    verify(commandObjects).hgetAll(key);
  }

  @Test
  public void testHincrBy() {
    String key = "hashKey";
    String field = "field1";
    long increment = 2L;
    long expectedValue = 5L; // Assuming the original value was 3

    when(commandObjects.hincrBy(key, field, increment)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedValue);

    long result = jedis.hincrBy(key, field, increment);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).hincrBy(key, field, increment);
  }

  @Test
  public void testHincrByBinary() {
    byte[] key = "hashKey".getBytes();
    byte[] field = "field1".getBytes();
    long increment = 2L;
    long expectedValue = 5L; // Assuming the original value was 3

    when(commandObjects.hincrBy(key, field, increment)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedValue);

    long result = jedis.hincrBy(key, field, increment);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).hincrBy(key, field, increment);
  }

  @Test
  public void testHincrByFloat() {
    String key = "hashKey";
    String field = "field1";
    double increment = 1.5;
    double expectedValue = 4.5; // Assuming the original value was 3.0

    when(commandObjects.hincrByFloat(key, field, increment)).thenReturn(doubleCommandObject);
    when(commandExecutor.executeCommand(doubleCommandObject)).thenReturn(expectedValue);

    double result = jedis.hincrByFloat(key, field, increment);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(doubleCommandObject);
    verify(commandObjects).hincrByFloat(key, field, increment);
  }

  @Test
  public void testHincrByFloatBinary() {
    byte[] key = "hashKey".getBytes();
    byte[] field = "field1".getBytes();
    double increment = 1.5;
    double expectedValue = 4.5; // Assuming the original value was 3.0

    when(commandObjects.hincrByFloat(key, field, increment)).thenReturn(doubleCommandObject);
    when(commandExecutor.executeCommand(doubleCommandObject)).thenReturn(expectedValue);

    double result = jedis.hincrByFloat(key, field, increment);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(doubleCommandObject);
    verify(commandObjects).hincrByFloat(key, field, increment);
  }

  @Test
  public void testHkeys() {
    String key = "hashKey";
    Set<String> expectedKeys = new HashSet<>(Arrays.asList("field1", "field2", "field3"));

    when(commandObjects.hkeys(key)).thenReturn(setStringCommandObject);
    when(commandExecutor.executeCommand(setStringCommandObject)).thenReturn(expectedKeys);

    Set<String> result = jedis.hkeys(key);

    assertThat(result, equalTo(expectedKeys));

    verify(commandExecutor).executeCommand(setStringCommandObject);
    verify(commandObjects).hkeys(key);
  }

  @Test
  public void testHkeysBinary() {
    byte[] key = "hashKey".getBytes();
    Set<byte[]> expectedKeys = new HashSet<>(Arrays.asList("field1".getBytes(), "field2".getBytes(), "field3".getBytes()));

    when(commandObjects.hkeys(key)).thenReturn(setBytesCommandObject);
    when(commandExecutor.executeCommand(setBytesCommandObject)).thenReturn(expectedKeys);

    Set<byte[]> result = jedis.hkeys(key);

    assertThat(result, equalTo(expectedKeys));

    verify(commandExecutor).executeCommand(setBytesCommandObject);
    verify(commandObjects).hkeys(key);
  }

  @Test
  public void testHlen() {
    String key = "hashKey";
    long expected = 3L; // Assuming there are 3 fields in the hash

    when(commandObjects.hlen(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.hlen(key);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).hlen(key);
  }

  @Test
  public void testHlenBinary() {
    byte[] key = "hashKey".getBytes();
    long expected = 3L; // Assuming there are 3 fields in the hash

    when(commandObjects.hlen(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.hlen(key);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).hlen(key);
  }

  @Test
  public void testHmget() {
    String key = "hashKey";
    String[] fields = { "field1", "field2" };
    List<String> expectedValues = Arrays.asList("value1", "value2");

    when(commandObjects.hmget(key, fields)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedValues);

    List<String> result = jedis.hmget(key, fields);

    assertThat(result, equalTo(expectedValues));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).hmget(key, fields);
  }

  @Test
  public void testHmgetBinary() {
    byte[] key = "hashKey".getBytes();
    byte[][] fields = { "field1".getBytes(), "field2".getBytes() };
    List<byte[]> expectedValues = Arrays.asList("value1".getBytes(), "value2".getBytes());

    when(commandObjects.hmget(key, fields)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedValues);

    List<byte[]> result = jedis.hmget(key, fields);

    assertThat(result, equalTo(expectedValues));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).hmget(key, fields);
  }

  @Test
  public void testHmset() {
    String key = "hashKey";
    Map<String, String> hash = new HashMap<>();
    hash.put("field1", "value1");
    hash.put("field2", "value2");
    String expectedStatus = "OK";

    when(commandObjects.hmset(key, hash)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedStatus);

    String result = jedis.hmset(key, hash);

    assertThat(result, equalTo(expectedStatus));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).hmset(key, hash);
  }

  @Test
  public void testHmsetBinary() {
    byte[] key = "hashKey".getBytes();
    Map<byte[], byte[]> hash = new HashMap<>();
    hash.put("field1".getBytes(), "value1".getBytes());
    hash.put("field2".getBytes(), "value2".getBytes());
    String expectedStatus = "OK";

    when(commandObjects.hmset(key, hash)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedStatus);

    String result = jedis.hmset(key, hash);

    assertThat(result, equalTo(expectedStatus));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).hmset(key, hash);
  }

  @Test
  public void testHrandfield() {
    String key = "hashKey";
    String expectedField = "field1";

    when(commandObjects.hrandfield(key)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedField);

    String result = jedis.hrandfield(key);

    assertThat(result, equalTo(expectedField));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).hrandfield(key);
  }

  @Test
  public void testHrandfieldBinary() {
    byte[] key = "hashKey".getBytes();
    byte[] expectedField = "field1".getBytes();

    when(commandObjects.hrandfield(key)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedField);

    byte[] result = jedis.hrandfield(key);

    assertThat(result, equalTo(expectedField));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).hrandfield(key);
  }

  @Test
  public void testHrandfieldCount() {
    String key = "hashKey";
    long count = 2;
    List<String> expectedFields = Arrays.asList("field1", "field2");

    when(commandObjects.hrandfield(key, count)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedFields);

    List<String> result = jedis.hrandfield(key, count);

    assertThat(result, equalTo(expectedFields));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).hrandfield(key, count);
  }

  @Test
  public void testHrandfieldCountBinary() {
    byte[] key = "hashKey".getBytes();
    long count = 2;
    List<byte[]> expectedFields = Arrays.asList("field1".getBytes(), "field2".getBytes());

    when(commandObjects.hrandfield(key, count)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedFields);

    List<byte[]> result = jedis.hrandfield(key, count);

    assertThat(result, equalTo(expectedFields));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).hrandfield(key, count);
  }

  @Test
  public void testHrandfieldWithValues() {
    String key = "hashKey";
    long count = 2;
    List<Map.Entry<String, String>> expectedEntries = new ArrayList<>();
    expectedEntries.add(new AbstractMap.SimpleEntry<>("field1", "value1"));
    expectedEntries.add(new AbstractMap.SimpleEntry<>("field2", "value2"));

    when(commandObjects.hrandfieldWithValues(key, count)).thenReturn(listEntryStringStringCommandObject);
    when(commandExecutor.executeCommand(listEntryStringStringCommandObject)).thenReturn(expectedEntries);

    List<Map.Entry<String, String>> result = jedis.hrandfieldWithValues(key, count);

    assertThat(result, equalTo(expectedEntries));

    verify(commandExecutor).executeCommand(listEntryStringStringCommandObject);
    verify(commandObjects).hrandfieldWithValues(key, count);
  }

  @Test
  public void testHrandfieldWithValuesBinary() {
    byte[] key = "hashKey".getBytes();
    long count = 2;
    List<Map.Entry<byte[], byte[]>> expectedEntries = new ArrayList<>();

    expectedEntries.add(new AbstractMap.SimpleEntry<>("field1".getBytes(), "value1".getBytes()));
    expectedEntries.add(new AbstractMap.SimpleEntry<>("field2".getBytes(), "value2".getBytes()));

    when(commandObjects.hrandfieldWithValues(key, count)).thenReturn(listEntryBytesBytesCommandObject);
    when(commandExecutor.executeCommand(listEntryBytesBytesCommandObject)).thenReturn(expectedEntries);

    List<Map.Entry<byte[], byte[]>> result = jedis.hrandfieldWithValues(key, count);

    assertThat(result, equalTo(expectedEntries));

    verify(commandExecutor).executeCommand(listEntryBytesBytesCommandObject);
    verify(commandObjects).hrandfieldWithValues(key, count);
  }

  @Test
  public void testHscan() {
    String key = "hashKey";
    String cursor = "0";
    ScanParams params = new ScanParams().match("*").count(10);
    List<Map.Entry<String, String>> scanResultData = new ArrayList<>();
    scanResultData.add(new AbstractMap.SimpleEntry<>("field1", "value1"));
    scanResultData.add(new AbstractMap.SimpleEntry<>("field2", "value2"));
    ScanResult<Map.Entry<String, String>> expectedScanResult = new ScanResult<>(cursor, scanResultData);

    when(commandObjects.hscan(key, cursor, params)).thenReturn(scanResultEntryStringStringCommandObject);
    when(commandExecutor.executeCommand(scanResultEntryStringStringCommandObject)).thenReturn(expectedScanResult);

    ScanResult<Map.Entry<String, String>> result = jedis.hscan(key, cursor, params);

    assertThat(result, equalTo(expectedScanResult));

    verify(commandExecutor).executeCommand(scanResultEntryStringStringCommandObject);
    verify(commandObjects).hscan(key, cursor, params);
  }

  @Test
  public void testHscanBinary() {
    byte[] key = "hashKey".getBytes();
    byte[] cursor = ScanParams.SCAN_POINTER_START_BINARY;
    ScanParams params = new ScanParams().match("*".getBytes()).count(10);
    List<Map.Entry<byte[], byte[]>> scanResultData = new ArrayList<>();
    scanResultData.add(new AbstractMap.SimpleEntry<>("field1".getBytes(), "value1".getBytes()));
    scanResultData.add(new AbstractMap.SimpleEntry<>("field2".getBytes(), "value2".getBytes()));
    ScanResult<Map.Entry<byte[], byte[]>> expectedScanResult = new ScanResult<>(cursor, scanResultData);

    when(commandObjects.hscan(key, cursor, params)).thenReturn(scanResultEntryBytesBytesCommandObject);
    when(commandExecutor.executeCommand(scanResultEntryBytesBytesCommandObject)).thenReturn(expectedScanResult);

    ScanResult<Map.Entry<byte[], byte[]>> result = jedis.hscan(key, cursor, params);

    assertThat(result, equalTo(expectedScanResult));

    verify(commandExecutor).executeCommand(scanResultEntryBytesBytesCommandObject);
    verify(commandObjects).hscan(key, cursor, params);
  }

  @Test
  public void testHscanNoValues() {
    String key = "hashKey";
    String cursor = "0";
    ScanParams params = new ScanParams().match("*").count(10);
    List<String> scanResultData = Arrays.asList("field1", "field2");
    ScanResult<String> expectedScanResult = new ScanResult<>(cursor, scanResultData);

    when(commandObjects.hscanNoValues(key, cursor, params)).thenReturn(scanResultStringCommandObject);
    when(commandExecutor.executeCommand(scanResultStringCommandObject)).thenReturn(expectedScanResult);

    ScanResult<String> result = jedis.hscanNoValues(key, cursor, params);

    assertThat(result, equalTo(expectedScanResult));

    verify(commandExecutor).executeCommand(scanResultStringCommandObject);
    verify(commandObjects).hscanNoValues(key, cursor, params);
  }

  @Test
  public void testHscanNoValuesBinary() {
    byte[] key = "hashKey".getBytes();
    byte[] cursor = ScanParams.SCAN_POINTER_START_BINARY;
    ScanParams params = new ScanParams().match("*".getBytes()).count(10);
    List<byte[]> scanResultData = Arrays.asList("field1".getBytes(), "field2".getBytes());
    ScanResult<byte[]> expectedScanResult = new ScanResult<>(cursor, scanResultData);

    when(commandObjects.hscanNoValues(key, cursor, params)).thenReturn(scanResultBytesCommandObject);
    when(commandExecutor.executeCommand(scanResultBytesCommandObject)).thenReturn(expectedScanResult);

    ScanResult<byte[]> result = jedis.hscanNoValues(key, cursor, params);

    assertThat(result, equalTo(expectedScanResult));

    verify(commandExecutor).executeCommand(scanResultBytesCommandObject);
    verify(commandObjects).hscanNoValues(key, cursor, params);
  }

  @Test
  public void testHset() {
    String key = "hashKey";
    String field = "field1";
    String value = "value1";
    long expected = 1L; // Assuming the field was newly set

    when(commandObjects.hset(key, field, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.hset(key, field, value);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).hset(key, field, value);
  }

  @Test
  public void testHsetBinary() {
    byte[] key = "hashKey".getBytes();
    byte[] field = "field1".getBytes();
    byte[] value = "value1".getBytes();
    long expected = 1L; // Assuming the field was newly set

    when(commandObjects.hset(key, field, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.hset(key, field, value);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).hset(key, field, value);
  }

  @Test
  public void testHsetMap() {
    String key = "hashKey";
    Map<String, String> hash = new HashMap<>();
    hash.put("field1", "value1");
    hash.put("field2", "value2");
    long expected = 2L; // Assuming both fields were newly set

    when(commandObjects.hset(key, hash)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.hset(key, hash);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).hset(key, hash);
  }

  @Test
  public void testHsetMapBinary() {
    byte[] key = "hashKey".getBytes();
    Map<byte[], byte[]> hash = new HashMap<>();
    hash.put("field1".getBytes(), "value1".getBytes());
    hash.put("field2".getBytes(), "value2".getBytes());
    long expected = 2L; // Assuming both fields were newly set

    when(commandObjects.hset(key, hash)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.hset(key, hash);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).hset(key, hash);
  }

  @Test
  public void testHsetObject() {
    String key = "myHash";
    String field = "myField";
    Object value = "myValue";
    long expectedResponse = 1L;

    when(commandObjects.hsetObject(key, field, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.hsetObject(key, field, value);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).hsetObject(key, field, value);
  }

  @Test
  public void testHsetObjectMap() {
    String key = "myHash";
    Map<String, Object> hash = new HashMap<>();
    hash.put("field1", "value1");
    hash.put("field2", "value2");
    long expectedResponse = 2L;

    when(commandObjects.hsetObject(key, hash)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.hsetObject(key, hash);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).hsetObject(key, hash);
  }

  @Test
  public void testHsetnx() {
    String key = "hashKey";
    String field = "field1";
    String value = "value1";
    long expected = 1L; // Assuming the field was newly set

    when(commandObjects.hsetnx(key, field, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.hsetnx(key, field, value);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).hsetnx(key, field, value);
  }

  @Test
  public void testHsetnxBinary() {
    byte[] key = "hashKey".getBytes();
    byte[] field = "field1".getBytes();
    byte[] value = "value1".getBytes();
    long expected = 1L; // Assuming the field was newly set

    when(commandObjects.hsetnx(key, field, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.hsetnx(key, field, value);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).hsetnx(key, field, value);
  }

  @Test
  public void testHstrlen() {
    String key = "hashKey";
    String field = "field1";
    long expectedLength = 6L; // Assuming the value of the field is "value1"

    when(commandObjects.hstrlen(key, field)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedLength);

    long result = jedis.hstrlen(key, field);

    assertThat(result, equalTo(expectedLength));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).hstrlen(key, field);
  }

  @Test
  public void testHstrlenBinary() {
    byte[] key = "hashKey".getBytes();
    byte[] field = "field1".getBytes();
    long expectedLength = 6L; // Assuming the value of the field is "value1".getBytes()

    when(commandObjects.hstrlen(key, field)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedLength);

    long result = jedis.hstrlen(key, field);

    assertThat(result, equalTo(expectedLength));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).hstrlen(key, field);
  }

  @Test
  public void testHvals() {
    String key = "hashKey";
    List<String> expectedValues = Arrays.asList("value1", "value2", "value3");

    when(commandObjects.hvals(key)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedValues);

    List<String> result = jedis.hvals(key);

    assertThat(result, equalTo(expectedValues));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).hvals(key);
  }

  @Test
  public void testHvalsBinary() {
    byte[] key = "hashKey".getBytes();
    List<byte[]> expectedValues = Arrays.asList("value1".getBytes(), "value2".getBytes(), "value3".getBytes());

    when(commandObjects.hvals(key)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedValues);

    List<byte[]> result = jedis.hvals(key);

    assertThat(result, equalTo(expectedValues));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).hvals(key);
  }

}
