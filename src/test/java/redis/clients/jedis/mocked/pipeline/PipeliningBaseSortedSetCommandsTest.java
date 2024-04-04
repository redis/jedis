package redis.clients.jedis.mocked.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import redis.clients.jedis.Response;
import redis.clients.jedis.args.SortedSetOption;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;
import redis.clients.jedis.params.ZParams;
import redis.clients.jedis.params.ZRangeParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.util.KeyValue;

public class PipeliningBaseSortedSetCommandsTest extends PipeliningBaseMockedTestBase {

  @Test
  public void testBzmpop() {
    SortedSetOption option = SortedSetOption.MAX;

    when(commandObjects.bzmpop(1.0, option, "key1", "key2")).thenReturn(keyValueStringListTupleCommandObject);

    Response<KeyValue<String, List<Tuple>>> response = pipeliningBase.bzmpop(1.0, option, "key1", "key2");

    assertThat(commands, contains(keyValueStringListTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBzmpopBinary() {
    double timeout = 1.0;
    SortedSetOption option = SortedSetOption.MAX;
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.bzmpop(timeout, option, keys)).thenReturn(keyValueBytesListTupleCommandObject);

    Response<KeyValue<byte[], List<Tuple>>> response = pipeliningBase.bzmpop(timeout, option, keys);

    assertThat(commands, contains(keyValueBytesListTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBzmpopWithCount() {
    SortedSetOption option = SortedSetOption.MAX;
    int count = 2;

    when(commandObjects.bzmpop(1.0, option, count, "key1", "key2")).thenReturn(keyValueStringListTupleCommandObject);

    Response<KeyValue<String, List<Tuple>>> response = pipeliningBase.bzmpop(1.0, option, count, "key1", "key2");

    assertThat(commands, contains(keyValueStringListTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBzmpopWithCountBinary() {
    double timeout = 1.0;
    SortedSetOption option = SortedSetOption.MAX;
    int count = 2;
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.bzmpop(timeout, option, count, keys)).thenReturn(keyValueBytesListTupleCommandObject);

    Response<KeyValue<byte[], List<Tuple>>> response = pipeliningBase.bzmpop(timeout, option, count, keys);

    assertThat(commands, contains(keyValueBytesListTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBzpopmax() {
    when(commandObjects.bzpopmax(1.0, "key1", "key2")).thenReturn(keyValueStringTupleCommandObject);

    Response<KeyValue<String, Tuple>> response = pipeliningBase.bzpopmax(1.0, "key1", "key2");

    assertThat(commands, contains(keyValueStringTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBzpopmaxBinary() {
    double timeout = 1.0;
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.bzpopmax(timeout, keys)).thenReturn(keyValueBytesTupleCommandObject);

    Response<KeyValue<byte[], Tuple>> response = pipeliningBase.bzpopmax(timeout, keys);

    assertThat(commands, contains(keyValueBytesTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBzpopmin() {
    when(commandObjects.bzpopmin(1.0, "key1", "key2")).thenReturn(keyValueStringTupleCommandObject);

    Response<KeyValue<String, Tuple>> response = pipeliningBase.bzpopmin(1.0, "key1", "key2");

    assertThat(commands, contains(keyValueStringTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBzpopminBinary() {
    double timeout = 1.0;
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.bzpopmin(timeout, keys)).thenReturn(keyValueBytesTupleCommandObject);

    Response<KeyValue<byte[], Tuple>> response = pipeliningBase.bzpopmin(timeout, keys);

    assertThat(commands, contains(keyValueBytesTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZadd() {
    when(commandObjects.zadd("key", 1.0, "member")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zadd("key", 1.0, "member");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZaddBinary() {
    byte[] key = "zset".getBytes();
    double score = 1.0;
    byte[] member = "member".getBytes();

    when(commandObjects.zadd(key, score, member)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zadd(key, score, member);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZaddWithParams() {
    ZAddParams params = new ZAddParams();

    when(commandObjects.zadd("key", 1.0, "member", params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zadd("key", 1.0, "member", params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZaddWithParamsBinary() {
    byte[] key = "zset".getBytes();
    double score = 1.0;
    byte[] member = "member".getBytes();
    ZAddParams params = ZAddParams.zAddParams().nx();

    when(commandObjects.zadd(key, score, member, params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zadd(key, score, member, params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZaddMultiple() {
    Map<String, Double> scoreMembers = new HashMap<>();
    scoreMembers.put("member1", 1.0);
    scoreMembers.put("member2", 2.0);

    when(commandObjects.zadd("key", scoreMembers)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zadd("key", scoreMembers);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZaddMultipleBinary() {
    byte[] key = "zset".getBytes();

    Map<byte[], Double> scoreMembers = new HashMap<>();
    scoreMembers.put("member1".getBytes(), 1.0);
    scoreMembers.put("member2".getBytes(), 2.0);

    when(commandObjects.zadd(key, scoreMembers)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zadd(key, scoreMembers);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZaddMultipleWithParams() {
    Map<String, Double> scoreMembers = new HashMap<>();
    scoreMembers.put("member1", 1.0);
    scoreMembers.put("member2", 2.0);

    ZAddParams params = new ZAddParams();

    when(commandObjects.zadd("key", scoreMembers, params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zadd("key", scoreMembers, params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZaddMultipleWithParamsBinary() {
    byte[] key = "zset".getBytes();

    Map<byte[], Double> scoreMembers = new HashMap<>();
    scoreMembers.put("member1".getBytes(), 1.0);
    scoreMembers.put("member2".getBytes(), 2.0);

    ZAddParams params = ZAddParams.zAddParams().nx();

    when(commandObjects.zadd(key, scoreMembers, params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zadd(key, scoreMembers, params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZaddIncr() {
    ZAddParams params = new ZAddParams();

    when(commandObjects.zaddIncr("key", 1.0, "member", params)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.zaddIncr("key", 1.0, "member", params);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZaddIncrBinary() {
    byte[] key = "zset".getBytes();
    double score = 1.0;
    byte[] member = "member".getBytes();
    ZAddParams params = ZAddParams.zAddParams().xx();

    when(commandObjects.zaddIncr(key, score, member, params)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.zaddIncr(key, score, member, params);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZcard() {
    when(commandObjects.zcard("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zcard("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZcardBinary() {
    byte[] key = "zset".getBytes();

    when(commandObjects.zcard(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zcard(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZcount() {
    when(commandObjects.zcount("key", "1", "2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zcount("key", "1", "2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZcountBinary() {
    byte[] key = "zset".getBytes();
    byte[] min = "min".getBytes();
    byte[] max = "max".getBytes();

    when(commandObjects.zcount(key, min, max)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zcount(key, min, max);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZcountDouble() {
    when(commandObjects.zcount("key", 1.0, 2.0)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zcount("key", 1.0, 2.0);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZcountDoubleBinary() {
    byte[] key = "zset".getBytes();
    double min = 1.0;
    double max = 2.0;

    when(commandObjects.zcount(key, min, max)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zcount(key, min, max);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZdiff() {
    when(commandObjects.zdiff("key1", "key2")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zdiff("key1", "key2");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZdiffBinary() {
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zdiff(keys)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zdiff(keys);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZdiffWithScores() {
    when(commandObjects.zdiffWithScores("key1", "key2")).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zdiffWithScores("key1", "key2");

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZdiffWithScoresBinary() {
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zdiffWithScores(keys)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zdiffWithScores(keys);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZdiffStore() {
    when(commandObjects.zdiffStore("dstKey", "key1", "key2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zdiffStore("dstKey", "key1", "key2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZdiffStoreBinary() {
    byte[] dstkey = "destZset".getBytes();
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zdiffStore(dstkey, keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zdiffStore(dstkey, keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZdiffstore() {
    when(commandObjects.zdiffstore("dstKey", "key1", "key2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zdiffstore("dstKey", "key1", "key2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZdiffstoreBinary() {
    byte[] dstkey = "destZset".getBytes();
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zdiffstore(dstkey, keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zdiffstore(dstkey, keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZincrby() {
    when(commandObjects.zincrby("key", 1.0, "member")).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.zincrby("key", 1.0, "member");

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZincrbyBinary() {
    byte[] key = "zset".getBytes();
    double increment = 2.0;
    byte[] member = "member".getBytes();

    when(commandObjects.zincrby(key, increment, member)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.zincrby(key, increment, member);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZincrbyWithParams() {
    ZIncrByParams params = new ZIncrByParams();

    when(commandObjects.zincrby("key", 1.0, "member", params)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.zincrby("key", 1.0, "member", params);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZincrbyWithParamsBinary() {
    byte[] key = "zset".getBytes();
    double increment = 2.0;
    byte[] member = "member".getBytes();
    ZIncrByParams params = ZIncrByParams.zIncrByParams().xx();

    when(commandObjects.zincrby(key, increment, member, params)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.zincrby(key, increment, member, params);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZinter() {
    ZParams params = new ZParams();

    when(commandObjects.zinter(params, "key1", "key2")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zinter(params, "key1", "key2");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZinterBinary() {
    ZParams params = new ZParams();
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zinter(params, keys)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zinter(params, keys);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZinterWithScores() {
    ZParams params = new ZParams();

    when(commandObjects.zinterWithScores(params, "key1", "key2")).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zinterWithScores(params, "key1", "key2");

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZinterWithScoresBinary() {
    ZParams params = new ZParams();
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zinterWithScores(params, keys)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zinterWithScores(params, keys);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZintercard() {
    when(commandObjects.zintercard("key1", "key2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zintercard("key1", "key2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZintercardBinary() {
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zintercard(keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zintercard(keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZintercardWithLimit() {
    long limit = 2;

    when(commandObjects.zintercard(limit, "key1", "key2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zintercard(limit, "key1", "key2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZintercardWithLimitBinary() {
    long limit = 2;
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zintercard(limit, keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zintercard(limit, keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZinterstore() {
    when(commandObjects.zinterstore("dstKey", "set1", "set2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zinterstore("dstKey", "set1", "set2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZinterstoreBinary() {
    byte[] dstkey = "destZset".getBytes();
    byte[][] sets = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zinterstore(dstkey, sets)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zinterstore(dstkey, sets);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZinterstoreWithParams() {
    ZParams params = new ZParams();

    when(commandObjects.zinterstore("dstKey", params, "set1", "set2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zinterstore("dstKey", params, "set1", "set2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZinterstoreWithParamsBinary() {
    byte[] dstkey = "destZset".getBytes();
    ZParams params = new ZParams();
    byte[][] sets = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zinterstore(dstkey, params, sets)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zinterstore(dstkey, params, sets);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZlexcount() {
    when(commandObjects.zlexcount("key", "[a", "[z")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zlexcount("key", "[a", "[z");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZlexcountBinary() {
    byte[] key = "zset".getBytes();
    byte[] min = "[a".getBytes();
    byte[] max = "[z".getBytes();

    when(commandObjects.zlexcount(key, min, max)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zlexcount(key, min, max);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZmpop() {
    SortedSetOption option = SortedSetOption.MAX;

    when(commandObjects.zmpop(option, "key1", "key2")).thenReturn(keyValueStringListTupleCommandObject);

    Response<KeyValue<String, List<Tuple>>> response = pipeliningBase.zmpop(option, "key1", "key2");

    assertThat(commands, contains(keyValueStringListTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZmpopBinary() {
    SortedSetOption option = SortedSetOption.MAX;
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zmpop(option, keys)).thenReturn(keyValueBytesListTupleCommandObject);

    Response<KeyValue<byte[], List<Tuple>>> response = pipeliningBase.zmpop(option, keys);

    assertThat(commands, contains(keyValueBytesListTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZmpopWithCount() {
    SortedSetOption option = SortedSetOption.MAX;
    int count = 2;

    when(commandObjects.zmpop(option, count, "key1", "key2")).thenReturn(keyValueStringListTupleCommandObject);

    Response<KeyValue<String, List<Tuple>>> response = pipeliningBase.zmpop(option, count, "key1", "key2");

    assertThat(commands, contains(keyValueStringListTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZmpopWithCountBinary() {
    SortedSetOption option = SortedSetOption.MAX;
    int count = 2;
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zmpop(option, count, keys)).thenReturn(keyValueBytesListTupleCommandObject);

    Response<KeyValue<byte[], List<Tuple>>> response = pipeliningBase.zmpop(option, count, keys);

    assertThat(commands, contains(keyValueBytesListTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZmscore() {
    when(commandObjects.zmscore("key", "member1", "member2")).thenReturn(listDoubleCommandObject);

    Response<List<Double>> response = pipeliningBase.zmscore("key", "member1", "member2");

    assertThat(commands, contains(listDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZmscoreBinary() {
    byte[] key = "zset".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };

    when(commandObjects.zmscore(key, members)).thenReturn(listDoubleCommandObject);

    Response<List<Double>> response = pipeliningBase.zmscore(key, members);

    assertThat(commands, contains(listDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZpopmax() {
    when(commandObjects.zpopmax("key")).thenReturn(tupleCommandObject);

    Response<Tuple> response = pipeliningBase.zpopmax("key");

    assertThat(commands, contains(tupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZpopmaxBinary() {
    byte[] key = "zset".getBytes();

    when(commandObjects.zpopmax(key)).thenReturn(tupleCommandObject);

    Response<Tuple> response = pipeliningBase.zpopmax(key);

    assertThat(commands, contains(tupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZpopmaxCount() {
    int count = 2;

    when(commandObjects.zpopmax("key", count)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zpopmax("key", count);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZpopmaxCountBinary() {
    byte[] key = "zset".getBytes();
    int count = 2;

    when(commandObjects.zpopmax(key, count)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zpopmax(key, count);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZpopmin() {
    when(commandObjects.zpopmin("key")).thenReturn(tupleCommandObject);

    Response<Tuple> response = pipeliningBase.zpopmin("key");

    assertThat(commands, contains(tupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZpopminBinary() {
    byte[] key = "zset".getBytes();

    when(commandObjects.zpopmin(key)).thenReturn(tupleCommandObject);

    Response<Tuple> response = pipeliningBase.zpopmin(key);

    assertThat(commands, contains(tupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZpopminCount() {
    int count = 2;

    when(commandObjects.zpopmin("key", count)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zpopmin("key", count);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZpopminCountBinary() {
    byte[] key = "zset".getBytes();
    int count = 2;

    when(commandObjects.zpopmin(key, count)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zpopmin(key, count);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrandmember() {
    when(commandObjects.zrandmember("key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.zrandmember("key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrandmemberBinary() {
    byte[] key = "zset".getBytes();

    when(commandObjects.zrandmember(key)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.zrandmember(key);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrandmemberCount() {
    long count = 2;

    when(commandObjects.zrandmember("key", count)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrandmember("key", count);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrandmemberCountBinary() {
    byte[] key = "zset".getBytes();
    long count = 2;

    when(commandObjects.zrandmember(key, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrandmember(key, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrandmemberWithScores() {
    long count = 2;

    when(commandObjects.zrandmemberWithScores("key", count)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrandmemberWithScores("key", count);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrandmemberWithScoresBinary() {
    byte[] key = "zset".getBytes();
    long count = 2;

    when(commandObjects.zrandmemberWithScores(key, count)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrandmemberWithScores(key, count);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrange() {
    when(commandObjects.zrange("key", 0, -1)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrange("key", 0, -1);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeBinary() {
    byte[] key = "zset".getBytes();
    long start = 0;
    long stop = 1;

    when(commandObjects.zrange(key, start, stop)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrange(key, start, stop);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeWithScores() {
    when(commandObjects.zrangeWithScores("key", 0, -1)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeWithScores("key", 0, -1);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeWithScoresBinary() {
    byte[] key = "zset".getBytes();
    long start = 0;
    long stop = 1;

    when(commandObjects.zrangeWithScores(key, start, stop)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeWithScores(key, start, stop);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeWithZRangeParams() {
    ZRangeParams zRangeParams = new ZRangeParams(1, 2);

    when(commandObjects.zrange("key", zRangeParams)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrange("key", zRangeParams);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeWithZRangeParamsBinary() {
    byte[] key = "zset".getBytes();
    ZRangeParams zRangeParams = ZRangeParams.zrangeParams(0, 1);

    when(commandObjects.zrange(key, zRangeParams)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrange(key, zRangeParams);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeWithScoresWithZRangeParams() {
    ZRangeParams zRangeParams = new ZRangeParams(1, 2);

    when(commandObjects.zrangeWithScores("key", zRangeParams)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeWithScores("key", zRangeParams);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeWithScoresWithZRangeParamsBinary() {
    byte[] key = "zset".getBytes();
    ZRangeParams zRangeParams = ZRangeParams.zrangeParams(0, 1);

    when(commandObjects.zrangeWithScores(key, zRangeParams)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeWithScores(key, zRangeParams);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByLex() {
    when(commandObjects.zrangeByLex("key", "[a", "[z")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrangeByLex("key", "[a", "[z");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByLexBinary() {
    byte[] key = "zset".getBytes();
    byte[] min = "[a".getBytes();
    byte[] max = "[z".getBytes();

    when(commandObjects.zrangeByLex(key, min, max)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrangeByLex(key, min, max);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByLexWithOffsetCount() {
    when(commandObjects.zrangeByLex("key", "[a", "[z", 0, 10)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrangeByLex("key", "[a", "[z", 0, 10);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByLexWithOffsetCountBinary() {
    byte[] key = "zset".getBytes();
    byte[] min = "[a".getBytes();
    byte[] max = "[z".getBytes();
    int offset = 0;
    int count = 10;

    when(commandObjects.zrangeByLex(key, min, max, offset, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrangeByLex(key, min, max, offset, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScore() {
    when(commandObjects.zrangeByScore("key", "1", "2")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrangeByScore("key", "1", "2");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreBinary() {
    byte[] key = "zset".getBytes();
    byte[] min = "1".getBytes();
    byte[] max = "2".getBytes();

    when(commandObjects.zrangeByScore(key, min, max)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrangeByScore(key, min, max);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreDouble() {
    when(commandObjects.zrangeByScore("key", 1.0, 2.0)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrangeByScore("key", 1.0, 2.0);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreDoubleBinary() {
    byte[] key = "zset".getBytes();
    double min = 1.0;
    double max = 2.0;

    when(commandObjects.zrangeByScore(key, min, max)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrangeByScore(key, min, max);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithOffsetCount() {
    when(commandObjects.zrangeByScore("key", "1", "2", 0, 1)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrangeByScore("key", "1", "2", 0, 1);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithOffsetCountBinary() {
    byte[] key = "zset".getBytes();
    byte[] min = "1".getBytes();
    byte[] max = "2".getBytes();
    int offset = 0;
    int count = 2;

    when(commandObjects.zrangeByScore(key, min, max, offset, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrangeByScore(key, min, max, offset, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreDoubleWithOffsetCount() {
    when(commandObjects.zrangeByScore("key", 1.0, 2.0, 0, 1)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrangeByScore("key", 1.0, 2.0, 0, 1);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreDoubleWithOffsetCountBinary() {
    byte[] key = "zset".getBytes();
    double min = 1.0;
    double max = 2.0;
    int offset = 0;
    int count = 2;

    when(commandObjects.zrangeByScore(key, min, max, offset, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrangeByScore(key, min, max, offset, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithScores() {
    when(commandObjects.zrangeByScoreWithScores("key", "1", "2")).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeByScoreWithScores("key", "1", "2");

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithScoresBinary() {
    byte[] key = "zset".getBytes();
    byte[] min = "1".getBytes();
    byte[] max = "2".getBytes();

    when(commandObjects.zrangeByScoreWithScores(key, min, max)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeByScoreWithScores(key, min, max);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithScoresDouble() {
    when(commandObjects.zrangeByScoreWithScores("key", 1.0, 2.0)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeByScoreWithScores("key", 1.0, 2.0);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithScoresDoubleBinary() {
    byte[] key = "zset".getBytes();
    double min = 1.0;
    double max = 2.0;

    when(commandObjects.zrangeByScoreWithScores(key, min, max)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeByScoreWithScores(key, min, max);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithScoresWithOffsetCount() {
    when(commandObjects.zrangeByScoreWithScores("key", "1", "2", 0, 1)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeByScoreWithScores("key", "1", "2", 0, 1);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithScoresWithOffsetCountBinary() {
    byte[] key = "zset".getBytes();
    byte[] min = "1".getBytes();
    byte[] max = "2".getBytes();
    int offset = 0;
    int count = 2;

    when(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeByScoreWithScores(key, min, max, offset, count);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithScoresDoubleWithOffsetCount() {
    when(commandObjects.zrangeByScoreWithScores("key", 1.0, 2.0, 0, 1)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeByScoreWithScores("key", 1.0, 2.0, 0, 1);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithScoresDoubleWithOffsetCountBinary() {
    byte[] key = "zset".getBytes();
    double min = 1.0;
    double max = 2.0;
    int offset = 0;
    int count = 2;

    when(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeByScoreWithScores(key, min, max, offset, count);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangestore() {
    ZRangeParams zRangeParams = new ZRangeParams(1, 2);

    when(commandObjects.zrangestore("dest", "src", zRangeParams)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zrangestore("dest", "src", zRangeParams);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangestoreBinary() {
    byte[] dest = "destZset".getBytes();
    byte[] src = "srcZset".getBytes();
    ZRangeParams zRangeParams = ZRangeParams.zrangeParams(0, 1);

    when(commandObjects.zrangestore(dest, src, zRangeParams)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zrangestore(dest, src, zRangeParams);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrank() {
    when(commandObjects.zrank("key", "member")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zrank("key", "member");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrankBinary() {
    byte[] key = "zset".getBytes();
    byte[] member = "member".getBytes();

    when(commandObjects.zrank(key, member)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zrank(key, member);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrankWithScore() {
    when(commandObjects.zrankWithScore("key", "member")).thenReturn(keyValueLongDoubleCommandObject);

    Response<KeyValue<Long, Double>> response = pipeliningBase.zrankWithScore("key", "member");

    assertThat(commands, contains(keyValueLongDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrankWithScoreBinary() {
    byte[] key = "zset".getBytes();
    byte[] member = "member".getBytes();

    when(commandObjects.zrankWithScore(key, member)).thenReturn(keyValueLongDoubleCommandObject);

    Response<KeyValue<Long, Double>> response = pipeliningBase.zrankWithScore(key, member);

    assertThat(commands, contains(keyValueLongDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrem() {
    when(commandObjects.zrem("key", "member1", "member2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zrem("key", "member1", "member2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZremBinary() {
    byte[] key = "zset".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };

    when(commandObjects.zrem(key, members)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zrem(key, members);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZremrangeByLex() {
    when(commandObjects.zremrangeByLex("key", "[a", "[z")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zremrangeByLex("key", "[a", "[z");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZremrangeByLexBinary() {
    byte[] key = "zset".getBytes();
    byte[] min = "[a".getBytes();
    byte[] max = "[z".getBytes();

    when(commandObjects.zremrangeByLex(key, min, max)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zremrangeByLex(key, min, max);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZremrangeByRank() {
    when(commandObjects.zremrangeByRank("key", 0, 1)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zremrangeByRank("key", 0, 1);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZremrangeByRankBinary() {
    byte[] key = "zset".getBytes();
    long start = 0;
    long stop = 1;

    when(commandObjects.zremrangeByRank(key, start, stop)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zremrangeByRank(key, start, stop);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZremrangeByScore() {
    when(commandObjects.zremrangeByScore("key", "1", "2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zremrangeByScore("key", "1", "2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZremrangeByScoreBinary() {
    byte[] key = "zset".getBytes();
    byte[] min = "1".getBytes();
    byte[] max = "2".getBytes();

    when(commandObjects.zremrangeByScore(key, min, max)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zremrangeByScore(key, min, max);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZremrangeByScoreDouble() {
    when(commandObjects.zremrangeByScore("key", 1.0, 2.0)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zremrangeByScore("key", 1.0, 2.0);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZremrangeByScoreDoubleBinary() {
    byte[] key = "zset".getBytes();
    double min = 1.0;
    double max = 2.0;

    when(commandObjects.zremrangeByScore(key, min, max)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zremrangeByScore(key, min, max);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrange() {
    when(commandObjects.zrevrange("key", 0, -1)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrevrange("key", 0, -1);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeBinary() {
    byte[] key = "zset".getBytes();
    long start = 0;
    long stop = 1;

    when(commandObjects.zrevrange(key, start, stop)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrevrange(key, start, stop);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeWithScores() {
    when(commandObjects.zrevrangeWithScores("key", 0, -1)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrevrangeWithScores("key", 0, -1);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeWithScoresBinary() {
    byte[] key = "zset".getBytes();
    long start = 0;
    long stop = 1;

    when(commandObjects.zrevrangeWithScores(key, start, stop)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrevrangeWithScores(key, start, stop);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByLex() {
    when(commandObjects.zrevrangeByLex("key", "[z", "[a")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrevrangeByLex("key", "[z", "[a");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByLexBinary() {
    byte[] key = "zset".getBytes();
    byte[] max = "[z".getBytes();
    byte[] min = "[a".getBytes();

    when(commandObjects.zrevrangeByLex(key, max, min)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrevrangeByLex(key, max, min);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByLexWithLimit() {
    when(commandObjects.zrevrangeByLex("key", "[z", "[a", 0, 10)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrevrangeByLex("key", "[z", "[a", 0, 10);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByLexWithLimitBinary() {
    byte[] key = "zset".getBytes();
    byte[] max = "[z".getBytes();
    byte[] min = "[a".getBytes();
    int offset = 0;
    int count = 10;

    when(commandObjects.zrevrangeByLex(key, max, min, offset, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrevrangeByLex(key, max, min, offset, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScore() {
    when(commandObjects.zrevrangeByScore("key", "2", "1")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrevrangeByScore("key", "2", "1");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreBinary() {
    byte[] key = "zset".getBytes();
    byte[] max = "2".getBytes();
    byte[] min = "1".getBytes();

    when(commandObjects.zrevrangeByScore(key, max, min)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrevrangeByScore(key, max, min);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreDouble() {
    when(commandObjects.zrevrangeByScore("key", 2.0, 1.0)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrevrangeByScore("key", 2.0, 1.0);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreDoubleBinary() {
    byte[] key = "zset".getBytes();
    double max = 2.0;
    double min = 1.0;

    when(commandObjects.zrevrangeByScore(key, max, min)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrevrangeByScore(key, max, min);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreWithLimit() {
    when(commandObjects.zrevrangeByScore("key", "2", "1", 0, 1)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrevrangeByScore("key", "2", "1", 0, 1);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreWithLimitBinary() {
    byte[] key = "zset".getBytes();
    byte[] max = "2".getBytes();
    byte[] min = "1".getBytes();
    int offset = 0;
    int count = 2;

    when(commandObjects.zrevrangeByScore(key, max, min, offset, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrevrangeByScore(key, max, min, offset, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreDoubleWithLimit() {
    when(commandObjects.zrevrangeByScore("key", 2.0, 1.0, 0, 1)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrevrangeByScore("key", 2.0, 1.0, 0, 1);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreDoubleWithLimitBinary() {
    byte[] key = "zset".getBytes();
    double max = 2.0;
    double min = 1.0;
    int offset = 0;
    int count = 2;

    when(commandObjects.zrevrangeByScore(key, max, min, offset, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrevrangeByScore(key, max, min, offset, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreWithScores() {
    when(commandObjects.zrevrangeByScoreWithScores("key", "2", "1")).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrevrangeByScoreWithScores("key", "2", "1");

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreWithScoresBinary() {
    byte[] key = "zset".getBytes();
    byte[] max = "2".getBytes();
    byte[] min = "1".getBytes();

    when(commandObjects.zrevrangeByScoreWithScores(key, max, min)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrevrangeByScoreWithScores(key, max, min);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreWithScoresDouble() {
    when(commandObjects.zrevrangeByScoreWithScores("key", 2.0, 1.0)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrevrangeByScoreWithScores("key", 2.0, 1.0);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreWithScoresDoubleBinary() {
    byte[] key = "zset".getBytes();
    double max = 2.0;
    double min = 1.0;

    when(commandObjects.zrevrangeByScoreWithScores(key, max, min)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrevrangeByScoreWithScores(key, max, min);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreWithScoresWithLimit() {
    when(commandObjects.zrevrangeByScoreWithScores("key", "2", "1", 0, 1)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrevrangeByScoreWithScores("key", "2", "1", 0, 1);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreWithScoresWithLimitBinary() {
    byte[] key = "zset".getBytes();
    byte[] max = "2".getBytes();
    byte[] min = "1".getBytes();
    int offset = 0;
    int count = 2;

    when(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrevrangeByScoreWithScores(key, max, min, offset, count);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreWithScoresDoubleWithLimit() {
    when(commandObjects.zrevrangeByScoreWithScores("key", 2.0, 1.0, 0, 1)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrevrangeByScoreWithScores("key", 2.0, 1.0, 0, 1);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreWithScoresDoubleWithLimitBinary() {
    byte[] key = "zset".getBytes();
    double max = 2.0;
    double min = 1.0;
    int offset = 0;
    int count = 2;

    when(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrevrangeByScoreWithScores(key, max, min, offset, count);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrank() {
    when(commandObjects.zrevrank("key", "member")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zrevrank("key", "member");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrankBinary() {
    byte[] key = "zset".getBytes();
    byte[] member = "member".getBytes();

    when(commandObjects.zrevrank(key, member)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zrevrank(key, member);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrankWithScore() {
    when(commandObjects.zrevrankWithScore("key", "member")).thenReturn(keyValueLongDoubleCommandObject);

    Response<KeyValue<Long, Double>> response = pipeliningBase.zrevrankWithScore("key", "member");

    assertThat(commands, contains(keyValueLongDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrankWithScoreBinary() {
    byte[] key = "zset".getBytes();
    byte[] member = "member".getBytes();

    when(commandObjects.zrevrankWithScore(key, member)).thenReturn(keyValueLongDoubleCommandObject);

    Response<KeyValue<Long, Double>> response = pipeliningBase.zrevrankWithScore(key, member);

    assertThat(commands, contains(keyValueLongDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZscan() {
    ScanParams params = new ScanParams();

    when(commandObjects.zscan("key", "0", params)).thenReturn(scanResultTupleCommandObject);

    Response<ScanResult<Tuple>> response = pipeliningBase.zscan("key", "0", params);

    assertThat(commands, contains(scanResultTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZscanBinary() {
    byte[] key = "zset".getBytes();
    byte[] cursor = "0".getBytes();
    ScanParams params = new ScanParams();

    when(commandObjects.zscan(key, cursor, params)).thenReturn(scanResultTupleCommandObject);

    Response<ScanResult<Tuple>> response = pipeliningBase.zscan(key, cursor, params);

    assertThat(commands, contains(scanResultTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZscore() {
    when(commandObjects.zscore("key", "member")).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.zscore("key", "member");

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZscoreBinary() {
    byte[] key = "zset".getBytes();
    byte[] member = "member".getBytes();

    when(commandObjects.zscore(key, member)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.zscore(key, member);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZunion() {
    ZParams params = new ZParams();

    when(commandObjects.zunion(params, "key1", "key2")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zunion(params, "key1", "key2");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZunionBinary() {
    ZParams params = new ZParams();
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zunion(params, keys)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zunion(params, keys);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZunionWithScores() {
    ZParams params = new ZParams();

    when(commandObjects.zunionWithScores(params, "key1", "key2")).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zunionWithScores(params, "key1", "key2");

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZunionWithScoresBinary() {
    ZParams params = new ZParams();
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zunionWithScores(params, keys)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zunionWithScores(params, keys);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZunionstore() {
    when(commandObjects.zunionstore("dstKey", "set1", "set2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zunionstore("dstKey", "set1", "set2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZunionstoreBinary() {
    byte[] dstkey = "destZset".getBytes();
    byte[][] sets = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zunionstore(dstkey, sets)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zunionstore(dstkey, sets);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZunionstoreWithParams() {
    ZParams params = new ZParams();

    when(commandObjects.zunionstore("dstKey", params, "set1", "set2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zunionstore("dstKey", params, "set1", "set2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZunionstoreWithParamsBinary() {
    byte[] dstkey = "destZset".getBytes();
    ZParams params = new ZParams();
    byte[][] sets = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zunionstore(dstkey, params, sets)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zunionstore(dstkey, params, sets);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

}
