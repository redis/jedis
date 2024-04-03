package redis.clients.jedis.mocked.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import redis.clients.jedis.Response;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public class PipeliningBaseSetCommandsTest extends PipeliningBaseMockedTestBase {

  @Test
  public void testSadd() {
    when(commandObjects.sadd("key", "member1", "member2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sadd("key", "member1", "member2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSaddBinary() {
    byte[] key = "key".getBytes();
    byte[] member1 = "member1".getBytes();
    byte[] member2 = "member2".getBytes();

    when(commandObjects.sadd(key, member1, member2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sadd(key, member1, member2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScard() {
    when(commandObjects.scard("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.scard("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScardBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.scard(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.scard(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSdiff() {
    when(commandObjects.sdiff("key1", "key2")).thenReturn(setStringCommandObject);

    Response<Set<String>> response = pipeliningBase.sdiff("key1", "key2");

    assertThat(commands, contains(setStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSdiffBinary() {
    byte[][] keys = { "key1".getBytes(), "key2".getBytes(), "key3".getBytes() };

    when(commandObjects.sdiff(keys)).thenReturn(setBytesCommandObject);

    Response<Set<byte[]>> response = pipeliningBase.sdiff(keys);

    assertThat(commands, contains(setBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSdiffstore() {
    when(commandObjects.sdiffstore("dstKey", "key1", "key2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sdiffstore("dstKey", "key1", "key2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSdiffstoreBinary() {
    byte[] dstkey = "destination".getBytes();
    byte[][] keys = { "key1".getBytes(), "key2".getBytes() };

    when(commandObjects.sdiffstore(dstkey, keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sdiffstore(dstkey, keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSinter() {
    when(commandObjects.sinter("key1", "key2")).thenReturn(setStringCommandObject);

    Response<Set<String>> response = pipeliningBase.sinter("key1", "key2");

    assertThat(commands, contains(setStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSinterBinary() {
    byte[][] keys = { "key1".getBytes(), "key2".getBytes() };

    when(commandObjects.sinter(keys)).thenReturn(setBytesCommandObject);

    Response<Set<byte[]>> response = pipeliningBase.sinter(keys);

    assertThat(commands, contains(setBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSintercard() {
    when(commandObjects.sintercard("key1", "key2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sintercard("key1", "key2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSintercardBinary() {
    byte[][] keys = { "key1".getBytes(), "key2".getBytes() };

    when(commandObjects.sintercard(keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sintercard(keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSintercardWithLimit() {
    int limit = 1;

    when(commandObjects.sintercard(limit, "key1", "key2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sintercard(limit, "key1", "key2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSintercardWithLimitBinary() {
    int limit = 2;
    byte[][] keys = { "key1".getBytes(), "key2".getBytes() };

    when(commandObjects.sintercard(limit, keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sintercard(limit, keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSinterstore() {
    when(commandObjects.sinterstore("dstKey", "key1", "key2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sinterstore("dstKey", "key1", "key2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSinterstoreBinary() {
    byte[] dstkey = "destination".getBytes();
    byte[][] keys = { "key1".getBytes(), "key2".getBytes() };

    when(commandObjects.sinterstore(dstkey, keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sinterstore(dstkey, keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSismember() {
    when(commandObjects.sismember("key", "member")).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.sismember("key", "member");

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSismemberBinary() {
    byte[] key = "key".getBytes();
    byte[] member = "member".getBytes();

    when(commandObjects.sismember(key, member)).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.sismember(key, member);

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSmembers() {
    when(commandObjects.smembers("key")).thenReturn(setStringCommandObject);

    Response<Set<String>> response = pipeliningBase.smembers("key");

    assertThat(commands, contains(setStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSmembersBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.smembers(key)).thenReturn(setBytesCommandObject);

    Response<Set<byte[]>> response = pipeliningBase.smembers(key);

    assertThat(commands, contains(setBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSmismember() {
    when(commandObjects.smismember("key", "member1", "member2")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.smismember("key", "member1", "member2");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSmismemberBinary() {
    byte[] key = "key".getBytes();
    byte[] member1 = "member1".getBytes();
    byte[] member2 = "member2".getBytes();

    when(commandObjects.smismember(key, member1, member2)).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.smismember(key, member1, member2);

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSmove() {
    when(commandObjects.smove("srcKey", "dstKey", "member")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.smove("srcKey", "dstKey", "member");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSmoveBinary() {
    byte[] srckey = "source".getBytes();
    byte[] dstkey = "destination".getBytes();
    byte[] member = "member".getBytes();

    when(commandObjects.smove(srckey, dstkey, member)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.smove(srckey, dstkey, member);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSpop() {
    when(commandObjects.spop("key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.spop("key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSpopBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.spop(key)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.spop(key);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSpopCount() {
    long count = 2;

    when(commandObjects.spop("key", count)).thenReturn(setStringCommandObject);

    Response<Set<String>> response = pipeliningBase.spop("key", count);

    assertThat(commands, contains(setStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSpopCountBinary() {
    byte[] key = "key".getBytes();
    long count = 2;

    when(commandObjects.spop(key, count)).thenReturn(setBytesCommandObject);

    Response<Set<byte[]>> response = pipeliningBase.spop(key, count);

    assertThat(commands, contains(setBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSrandmember() {
    when(commandObjects.srandmember("key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.srandmember("key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSrandmemberBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.srandmember(key)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.srandmember(key);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSrandmemberCount() {
    int count = 2;

    when(commandObjects.srandmember("key", count)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.srandmember("key", count);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSrandmemberCountBinary() {
    byte[] key = "key".getBytes();
    int count = 2;

    when(commandObjects.srandmember(key, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.srandmember(key, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSrem() {
    when(commandObjects.srem("key", "member1", "member2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.srem("key", "member1", "member2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSremBinary() {
    byte[] key = "key".getBytes();
    byte[] member1 = "member1".getBytes();
    byte[] member2 = "member2".getBytes();

    when(commandObjects.srem(key, member1, member2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.srem(key, member1, member2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSscan() {
    String cursor = "0";
    ScanParams params = new ScanParams();

    when(commandObjects.sscan("key", cursor, params)).thenReturn(scanResultStringCommandObject);

    Response<ScanResult<String>> response = pipeliningBase.sscan("key", cursor, params);

    assertThat(commands, contains(scanResultStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSscanBinary() {
    byte[] key = "key".getBytes();
    byte[] cursor = "0".getBytes();
    ScanParams params = new ScanParams().match("pattern*").count(10);

    when(commandObjects.sscan(key, cursor, params)).thenReturn(scanResultBytesCommandObject);

    Response<ScanResult<byte[]>> response = pipeliningBase.sscan(key, cursor, params);

    assertThat(commands, contains(scanResultBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSunion() {
    when(commandObjects.sunion("key1", "key2")).thenReturn(setStringCommandObject);

    Response<Set<String>> response = pipeliningBase.sunion("key1", "key2");

    assertThat(commands, contains(setStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSunionBinary() {
    byte[][] keys = { "key1".getBytes(), "key2".getBytes() };

    when(commandObjects.sunion(keys)).thenReturn(setBytesCommandObject);

    Response<Set<byte[]>> response = pipeliningBase.sunion(keys);

    assertThat(commands, contains(setBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSunionstore() {
    when(commandObjects.sunionstore("dstKey", "key1", "key2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sunionstore("dstKey", "key1", "key2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSunionstoreBinary() {
    byte[] dstkey = "destination".getBytes();
    byte[][] keys = { "key1".getBytes(), "key2".getBytes() };

    when(commandObjects.sunionstore(dstkey, keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sunionstore(dstkey, keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

}
