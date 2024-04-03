package redis.clients.jedis.mocked.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;
import redis.clients.jedis.Response;
import redis.clients.jedis.args.ListDirection;
import redis.clients.jedis.args.ListPosition;
import redis.clients.jedis.params.LPosParams;
import redis.clients.jedis.util.KeyValue;

public class PipeliningBaseListCommandsTest extends PipeliningBaseMockedTestBase {

  @Test
  public void testBlmove() {
    ListDirection from = ListDirection.LEFT;
    ListDirection to = ListDirection.RIGHT;
    double timeout = 1.0;

    when(commandObjects.blmove("srcKey", "dstKey", from, to, timeout)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.blmove("srcKey", "dstKey", from, to, timeout);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlmoveBinary() {
    byte[] srcKey = "srcKey".getBytes();
    byte[] dstKey = "dstKey".getBytes();
    ListDirection from = ListDirection.LEFT;
    ListDirection to = ListDirection.RIGHT;
    double timeout = 10.5;

    when(commandObjects.blmove(srcKey, dstKey, from, to, timeout)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.blmove(srcKey, dstKey, from, to, timeout);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlmpop() {
    double timeout = 1.0;
    ListDirection direction = ListDirection.LEFT;

    when(commandObjects.blmpop(timeout, direction, "key1", "key2")).thenReturn(keyValueStringListStringCommandObject);

    Response<KeyValue<String, List<String>>> response = pipeliningBase.blmpop(timeout, direction, "key1", "key2");

    assertThat(commands, contains(keyValueStringListStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlmpopBinary() {
    double timeout = 10.5;
    ListDirection direction = ListDirection.LEFT;
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.blmpop(timeout, direction, key1, key2)).thenReturn(keyValueBytesListBytesCommandObject);

    Response<KeyValue<byte[], List<byte[]>>> response = pipeliningBase.blmpop(timeout, direction, key1, key2);

    assertThat(commands, contains(keyValueBytesListBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlmpopCount() {
    double timeout = 1.0;
    ListDirection direction = ListDirection.LEFT;
    int count = 2;

    when(commandObjects.blmpop(timeout, direction, count, "key1", "key2")).thenReturn(keyValueStringListStringCommandObject);

    Response<KeyValue<String, List<String>>> response = pipeliningBase.blmpop(timeout, direction, count, "key1", "key2");

    assertThat(commands, contains(keyValueStringListStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlmpopCountBinary() {
    double timeout = 10.5;
    ListDirection direction = ListDirection.LEFT;
    int count = 2;
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.blmpop(timeout, direction, count, key1, key2)).thenReturn(keyValueBytesListBytesCommandObject);

    Response<KeyValue<byte[], List<byte[]>>> response = pipeliningBase.blmpop(timeout, direction, count, key1, key2);

    assertThat(commands, contains(keyValueBytesListBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlpop() {
    when(commandObjects.blpop(30, "key")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.blpop(30, "key");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlpopBinary() {
    int timeout = 10;
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.blpop(timeout, key1, key2)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.blpop(timeout, key1, key2);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlpopDoubleTimeout() {
    when(commandObjects.blpop(30.0, "key")).thenReturn(keyValueStringStringCommandObject);

    Response<KeyValue<String, String>> response = pipeliningBase.blpop(30.0, "key");

    assertThat(commands, contains(keyValueStringStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlpopDoubleTimeoutBinary() {
    double timeout = 10.5;
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.blpop(timeout, key1, key2)).thenReturn(keyValueBytesBytesCommandObject);

    Response<KeyValue<byte[], byte[]>> response = pipeliningBase.blpop(timeout, key1, key2);

    assertThat(commands, contains(keyValueBytesBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlpopMultipleKeys() {
    when(commandObjects.blpop(30, "key1", "key2")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.blpop(30, "key1", "key2");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlpopMultipleKeysDoubleTimeout() {
    when(commandObjects.blpop(30.0, "key1", "key2")).thenReturn(keyValueStringStringCommandObject);

    Response<KeyValue<String, String>> response = pipeliningBase.blpop(30.0, "key1", "key2");

    assertThat(commands, contains(keyValueStringStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBrpop() {
    when(commandObjects.brpop(30, "key")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.brpop(30, "key");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBrpopBinary() {
    int timeout = 10;
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.brpop(timeout, key1, key2)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.brpop(timeout, key1, key2);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBrpopDoubleTimeout() {
    when(commandObjects.brpop(30.0, "key")).thenReturn(keyValueStringStringCommandObject);

    Response<KeyValue<String, String>> response = pipeliningBase.brpop(30.0, "key");

    assertThat(commands, contains(keyValueStringStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBrpopDoubleTimeoutBinary() {
    double timeout = 10.5;
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.brpop(timeout, key1, key2)).thenReturn(keyValueBytesBytesCommandObject);

    Response<KeyValue<byte[], byte[]>> response = pipeliningBase.brpop(timeout, key1, key2);

    assertThat(commands, contains(keyValueBytesBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBrpopMultipleKeys() {
    when(commandObjects.brpop(30, "key1", "key2")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.brpop(30, "key1", "key2");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBrpopMultipleKeysDoubleTimeout() {
    when(commandObjects.brpop(30.0, "key1", "key2")).thenReturn(keyValueStringStringCommandObject);

    Response<KeyValue<String, String>> response = pipeliningBase.brpop(30.0, "key1", "key2");

    assertThat(commands, contains(keyValueStringStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBrpoplpush() {
    when(commandObjects.brpoplpush("source", "destination", 30)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.brpoplpush("source", "destination", 30);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBrpoplpushBinary() {
    byte[] source = "source".getBytes();
    byte[] destination = "destination".getBytes();
    int timeout = 10;

    when(commandObjects.brpoplpush(source, destination, timeout)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.brpoplpush(source, destination, timeout);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLindex() {
    when(commandObjects.lindex("key", 1)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.lindex("key", 1);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLindexBinary() {
    byte[] key = "key".getBytes();
    long index = 0;

    when(commandObjects.lindex(key, index)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.lindex(key, index);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLinsert() {
    ListPosition where = ListPosition.BEFORE;

    when(commandObjects.linsert("key", where, "pivot", "value")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.linsert("key", where, "pivot", "value");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLinsertBinary() {
    byte[] key = "key".getBytes();
    ListPosition where = ListPosition.BEFORE;
    byte[] pivot = "pivot".getBytes();
    byte[] value = "value".getBytes();

    when(commandObjects.linsert(key, where, pivot, value)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.linsert(key, where, pivot, value);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLlen() {
    when(commandObjects.llen("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.llen("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLlenBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.llen(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.llen(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLmove() {
    ListDirection from = ListDirection.LEFT;
    ListDirection to = ListDirection.RIGHT;

    when(commandObjects.lmove("srcKey", "dstKey", from, to)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.lmove("srcKey", "dstKey", from, to);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLmoveBinary() {
    byte[] srcKey = "srcKey".getBytes();
    byte[] dstKey = "dstKey".getBytes();
    ListDirection from = ListDirection.LEFT;
    ListDirection to = ListDirection.RIGHT;

    when(commandObjects.lmove(srcKey, dstKey, from, to)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.lmove(srcKey, dstKey, from, to);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLmpop() {
    ListDirection direction = ListDirection.LEFT;

    when(commandObjects.lmpop(direction, "key1", "key2")).thenReturn(keyValueStringListStringCommandObject);

    Response<KeyValue<String, List<String>>> response = pipeliningBase.lmpop(direction, "key1", "key2");

    assertThat(commands, contains(keyValueStringListStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLmpopBinary() {
    ListDirection direction = ListDirection.LEFT;
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.lmpop(direction, key1, key2)).thenReturn(keyValueBytesListBytesCommandObject);

    Response<KeyValue<byte[], List<byte[]>>> response = pipeliningBase.lmpop(direction, key1, key2);

    assertThat(commands, contains(keyValueBytesListBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLmpopCount() {
    ListDirection direction = ListDirection.LEFT;
    int count = 2;

    when(commandObjects.lmpop(direction, count, "key1", "key2")).thenReturn(keyValueStringListStringCommandObject);

    Response<KeyValue<String, List<String>>> response = pipeliningBase.lmpop(direction, count, "key1", "key2");

    assertThat(commands, contains(keyValueStringListStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLmpopCountBinary() {
    ListDirection direction = ListDirection.LEFT;
    int count = 2;
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.lmpop(direction, count, key1, key2)).thenReturn(keyValueBytesListBytesCommandObject);

    Response<KeyValue<byte[], List<byte[]>>> response = pipeliningBase.lmpop(direction, count, key1, key2);

    assertThat(commands, contains(keyValueBytesListBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLpop() {
    when(commandObjects.lpop("key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.lpop("key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLpopBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.lpop(key)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.lpop(key);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLpopCount() {
    when(commandObjects.lpop("key", 2)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.lpop("key", 2);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLpopCountBinary() {
    byte[] key = "key".getBytes();
    int count = 2;

    when(commandObjects.lpop(key, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.lpop(key, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLpos() {
    when(commandObjects.lpos("key", "element")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.lpos("key", "element");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLposBinary() {
    byte[] key = "key".getBytes();
    byte[] element = "element".getBytes();

    when(commandObjects.lpos(key, element)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.lpos(key, element);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLposWithParams() {
    LPosParams params = new LPosParams();

    when(commandObjects.lpos("key", "element", params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.lpos("key", "element", params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLposWithParamsBinary() {
    byte[] key = "key".getBytes();
    byte[] element = "element".getBytes();
    LPosParams params = new LPosParams().rank(1);

    when(commandObjects.lpos(key, element, params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.lpos(key, element, params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLposWithParamsCount() {
    LPosParams params = new LPosParams();

    when(commandObjects.lpos("key", "element", params, 3)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.lpos("key", "element", params, 3);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLposWithParamsCountBinary() {
    byte[] key = "key".getBytes();
    byte[] element = "element".getBytes();
    LPosParams params = new LPosParams().rank(1);
    long count = 2;

    when(commandObjects.lpos(key, element, params, count)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.lpos(key, element, params, count);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLpush() {
    when(commandObjects.lpush("key", "value1", "value2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.lpush("key", "value1", "value2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLpushBinary() {
    byte[] key = "key".getBytes();
    byte[] arg1 = "value1".getBytes();
    byte[] arg2 = "value2".getBytes();

    when(commandObjects.lpush(key, arg1, arg2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.lpush(key, arg1, arg2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLpushx() {
    when(commandObjects.lpushx("key", "value1", "value2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.lpushx("key", "value1", "value2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLpushxBinary() {
    byte[] key = "key".getBytes();
    byte[] arg = "value".getBytes();

    when(commandObjects.lpushx(key, arg)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.lpushx(key, arg);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLrange() {
    when(commandObjects.lrange("key", 0, -1)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.lrange("key", 0, -1);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLrangeBinary() {
    byte[] key = "key".getBytes();
    long start = 0;
    long stop = -1;

    when(commandObjects.lrange(key, start, stop)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.lrange(key, start, stop);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLrem() {
    when(commandObjects.lrem("key", 2, "value")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.lrem("key", 2, "value");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLremBinary() {
    byte[] key = "key".getBytes();
    long count = 1;
    byte[] value = "value".getBytes();

    when(commandObjects.lrem(key, count, value)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.lrem(key, count, value);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLset() {
    when(commandObjects.lset("key", 1, "value")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.lset("key", 1, "value");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLsetBinary() {
    byte[] key = "key".getBytes();
    long index = 0;
    byte[] value = "value".getBytes();

    when(commandObjects.lset(key, index, value)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.lset(key, index, value);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLtrim() {
    when(commandObjects.ltrim("key", 1, -1)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ltrim("key", 1, -1);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLtrimBinary() {
    byte[] key = "key".getBytes();
    long start = 1;
    long stop = -1;

    when(commandObjects.ltrim(key, start, stop)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ltrim(key, start, stop);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRpop() {
    when(commandObjects.rpop("key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.rpop("key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRpopBinary() {
    byte[] key = "key".getBytes();

    when(commandObjects.rpop(key)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.rpop(key);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRpopCount() {
    when(commandObjects.rpop("key", 2)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.rpop("key", 2);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRpopCountBinary() {
    byte[] key = "key".getBytes();
    int count = 2;

    when(commandObjects.rpop(key, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.rpop(key, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRpoplpush() {
    when(commandObjects.rpoplpush("srcKey", "dstKey")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.rpoplpush("srcKey", "dstKey");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRpoplpushBinary() {
    byte[] srckey = "srckey".getBytes();
    byte[] dstkey = "dstkey".getBytes();

    when(commandObjects.rpoplpush(srckey, dstkey)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.rpoplpush(srckey, dstkey);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRpush() {
    when(commandObjects.rpush("key", "value1", "value2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.rpush("key", "value1", "value2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRpushBinary() {
    byte[] key = "key".getBytes();
    byte[] arg1 = "value1".getBytes();
    byte[] arg2 = "value2".getBytes();

    when(commandObjects.rpush(key, arg1, arg2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.rpush(key, arg1, arg2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRpushx() {
    when(commandObjects.rpushx("key", "value1", "value2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.rpushx("key", "value1", "value2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRpushxBinary() {
    byte[] key = "key".getBytes();
    byte[] arg = "value".getBytes();

    when(commandObjects.rpushx(key, arg)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.rpushx(key, arg);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

}
