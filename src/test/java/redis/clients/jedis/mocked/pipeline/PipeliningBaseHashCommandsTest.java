package redis.clients.jedis.mocked.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import redis.clients.jedis.Response;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public class PipeliningBaseHashCommandsTest extends PipeliningBaseMockedTestBase {

  @Test
  public void testHdel() {
    when(commandObjects.hdel("key", "field1", "field2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hdel("key", "field1", "field2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHdelBinary() {
    byte[] key = "hash".getBytes();
    byte[] field1 = "field1".getBytes();
    byte[] field2 = "field2".getBytes();

    when(commandObjects.hdel(key, field1, field2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hdel(key, field1, field2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHexists() {
    when(commandObjects.hexists("key", "field")).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.hexists("key", "field");

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHexistsBinary() {
    byte[] key = "hash".getBytes();
    byte[] field = "field1".getBytes();

    when(commandObjects.hexists(key, field)).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.hexists(key, field);

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHget() {
    when(commandObjects.hget("key", "field")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.hget("key", "field");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHgetBinary() {
    byte[] key = "hash".getBytes();
    byte[] field = "field1".getBytes();

    when(commandObjects.hget(key, field)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.hget(key, field);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHgetAll() {
    when(commandObjects.hgetAll("key")).thenReturn(mapStringStringCommandObject);

    Response<Map<String, String>> response = pipeliningBase.hgetAll("key");

    assertThat(commands, contains(mapStringStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHgetAllBinary() {
    byte[] key = "hash".getBytes();

    when(commandObjects.hgetAll(key)).thenReturn(mapBytesBytesCommandObject);

    Response<Map<byte[], byte[]>> response = pipeliningBase.hgetAll(key);

    assertThat(commands, contains(mapBytesBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHincrBy() {
    when(commandObjects.hincrBy("key", "field", 1L)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hincrBy("key", "field", 1L);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHincrByBinary() {
    byte[] key = "hash".getBytes();
    byte[] field = "field1".getBytes();
    long increment = 2L;

    when(commandObjects.hincrBy(key, field, increment)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hincrBy(key, field, increment);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHincrByFloat() {
    when(commandObjects.hincrByFloat("key", "field", 1.0)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.hincrByFloat("key", "field", 1.0);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHincrByFloatBinary() {
    byte[] key = "hash".getBytes();
    byte[] field = "field1".getBytes();
    double increment = 2.5;

    when(commandObjects.hincrByFloat(key, field, increment)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.hincrByFloat(key, field, increment);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHkeys() {
    when(commandObjects.hkeys("key")).thenReturn(setStringCommandObject);

    Response<Set<String>> response = pipeliningBase.hkeys("key");

    assertThat(commands, contains(setStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHkeysBinary() {
    byte[] key = "hash".getBytes();

    when(commandObjects.hkeys(key)).thenReturn(setBytesCommandObject);

    Response<Set<byte[]>> response = pipeliningBase.hkeys(key);

    assertThat(commands, contains(setBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHlen() {
    when(commandObjects.hlen("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hlen("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHlenBinary() {
    byte[] key = "hash".getBytes();

    when(commandObjects.hlen(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hlen(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHmget() {
    when(commandObjects.hmget("key", "field1", "field2")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.hmget("key", "field1", "field2");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHmgetBinary() {
    byte[] key = "hash".getBytes();
    byte[] field1 = "field1".getBytes();
    byte[] field2 = "field2".getBytes();

    when(commandObjects.hmget(key, field1, field2)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.hmget(key, field1, field2);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHmset() {
    Map<String, String> hash = new HashMap<>();
    hash.put("field1", "value1");
    hash.put("field2", "value2");

    when(commandObjects.hmset("key", hash)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.hmset("key", hash);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHmsetBinary() {
    byte[] key = "hash".getBytes();

    Map<byte[], byte[]> hash = new HashMap<>();
    hash.put("field1".getBytes(), "value1".getBytes());

    when(commandObjects.hmset(key, hash)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.hmset(key, hash);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHrandfield() {
    when(commandObjects.hrandfield("key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.hrandfield("key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHrandfieldBinary() {
    byte[] key = "hash".getBytes();

    when(commandObjects.hrandfield(key)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.hrandfield(key);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHrandfieldCount() {
    long count = 2;

    when(commandObjects.hrandfield("key", count)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.hrandfield("key", count);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHrandfieldCountBinary() {
    byte[] key = "hash".getBytes();
    long count = 2;

    when(commandObjects.hrandfield(key, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.hrandfield(key, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHrandfieldWithValues() {
    long count = 2;

    when(commandObjects.hrandfieldWithValues("key", count)).thenReturn(listEntryStringStringCommandObject);

    Response<List<Map.Entry<String, String>>> response = pipeliningBase.hrandfieldWithValues("key", count);

    assertThat(commands, contains(listEntryStringStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHrandfieldWithValuesBinary() {
    byte[] key = "hash".getBytes();
    long count = 2;

    when(commandObjects.hrandfieldWithValues(key, count)).thenReturn(listEntryBytesBytesCommandObject);

    Response<List<Map.Entry<byte[], byte[]>>> response = pipeliningBase.hrandfieldWithValues(key, count);

    assertThat(commands, contains(listEntryBytesBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHscan() {
    String cursor = "0";
    ScanParams params = new ScanParams();

    when(commandObjects.hscan("key", cursor, params)).thenReturn(scanResultEntryStringStringCommandObject);

    Response<ScanResult<Map.Entry<String, String>>> response = pipeliningBase.hscan("key", cursor, params);

    assertThat(commands, contains(scanResultEntryStringStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHscanBinary() {
    byte[] key = "hash".getBytes();
    byte[] cursor = "0".getBytes();
    ScanParams params = new ScanParams().match("*").count(10);

    when(commandObjects.hscan(key, cursor, params)).thenReturn(scanResultEntryBytesBytesCommandObject);

    Response<ScanResult<Map.Entry<byte[], byte[]>>> response = pipeliningBase.hscan(key, cursor, params);

    assertThat(commands, contains(scanResultEntryBytesBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHscanNoValues() {
    String cursor = "0";
    ScanParams params = new ScanParams();

    when(commandObjects.hscanNoValues("key", cursor, params)).thenReturn(scanResultStringCommandObject);

    Response<ScanResult<String>> response = pipeliningBase.hscanNoValues("key", cursor, params);

    assertThat(commands, contains(scanResultStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHscanNoValuesBinary() {
    byte[] key = "hash".getBytes();
    byte[] cursor = "0".getBytes();
    ScanParams params = new ScanParams().match("*").count(10);

    when(commandObjects.hscanNoValues(key, cursor, params)).thenReturn(scanResultBytesCommandObject);

    Response<ScanResult<byte[]>> response = pipeliningBase.hscanNoValues(key, cursor, params);

    assertThat(commands, contains(scanResultBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHset() {
    when(commandObjects.hset("key", "field", "value")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hset("key", "field", "value");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHsetBinary() {
    byte[] key = "hash".getBytes();
    byte[] field = "field1".getBytes();
    byte[] value = "value1".getBytes();

    when(commandObjects.hset(key, field, value)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hset(key, field, value);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHsetMap() {
    Map<String, String> hash = new HashMap<>();
    hash.put("field1", "value1");
    hash.put("field2", "value2");

    when(commandObjects.hset("key", hash)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hset("key", hash);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHsetMapBinary() {
    byte[] key = "hash".getBytes();

    Map<byte[], byte[]> hash = new HashMap<>();
    hash.put("field1".getBytes(), "value1".getBytes());

    when(commandObjects.hset(key, hash)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hset(key, hash);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHsetnx() {
    when(commandObjects.hsetnx("key", "field", "value")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hsetnx("key", "field", "value");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHsetnxBinary() {
    byte[] key = "hash".getBytes();
    byte[] field = "field1".getBytes();
    byte[] value = "value1".getBytes();

    when(commandObjects.hsetnx(key, field, value)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hsetnx(key, field, value);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHstrlen() {
    when(commandObjects.hstrlen("key", "field")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hstrlen("key", "field");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHstrlenBinary() {
    byte[] key = "hash".getBytes();
    byte[] field = "field1".getBytes();

    when(commandObjects.hstrlen(key, field)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hstrlen(key, field);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHvals() {
    when(commandObjects.hvals("key")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.hvals("key");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHvalsBinary() {
    byte[] key = "hash".getBytes();

    when(commandObjects.hvals(key)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.hvals(key);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

}
