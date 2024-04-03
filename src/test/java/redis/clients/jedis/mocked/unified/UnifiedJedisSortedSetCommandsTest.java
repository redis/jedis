package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import redis.clients.jedis.args.SortedSetOption;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;
import redis.clients.jedis.params.ZParams;
import redis.clients.jedis.params.ZRangeParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.util.KeyValue;

public class UnifiedJedisSortedSetCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testBzmpop() {
    double timeout = 2.0;
    SortedSetOption option = SortedSetOption.MAX;
    String[] keys = { "zset1", "zset2" };
    KeyValue<String, List<Tuple>> expectedPopResult = new KeyValue<>("zset1", Collections.singletonList(new Tuple("member1", 1.0)));

    when(commandObjects.bzmpop(timeout, option, keys)).thenReturn(keyValueStringListTupleCommandObject);
    when(commandExecutor.executeCommand(keyValueStringListTupleCommandObject)).thenReturn(expectedPopResult);

    KeyValue<String, List<Tuple>> result = jedis.bzmpop(timeout, option, keys);

    assertThat(result, equalTo(expectedPopResult));

    verify(commandExecutor).executeCommand(keyValueStringListTupleCommandObject);
    verify(commandObjects).bzmpop(timeout, option, keys);
  }

  @Test
  public void testBzmpopBinary() {
    double timeout = 2.0;
    SortedSetOption option = SortedSetOption.MAX;
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };
    KeyValue<byte[], List<Tuple>> expectedPopResult = new KeyValue<>("zset1".getBytes(), Collections.singletonList(new Tuple("member1", 1.0)));

    when(commandObjects.bzmpop(timeout, option, keys)).thenReturn(keyValueBytesListTupleCommandObject);
    when(commandExecutor.executeCommand(keyValueBytesListTupleCommandObject)).thenReturn(expectedPopResult);

    KeyValue<byte[], List<Tuple>> result = jedis.bzmpop(timeout, option, keys);

    assertThat(result, equalTo(expectedPopResult));

    verify(commandExecutor).executeCommand(keyValueBytesListTupleCommandObject);
    verify(commandObjects).bzmpop(timeout, option, keys);
  }

  @Test
  public void testBzmpopWithCount() {
    double timeout = 2.0;
    SortedSetOption option = SortedSetOption.MAX;
    int count = 2;
    String[] keys = { "zset1", "zset2" };
    KeyValue<String, List<Tuple>> expectedPopResult = new KeyValue<>("zset1", Arrays.asList(new Tuple("member1", 1.0), new Tuple("member2", 2.0)));

    when(commandObjects.bzmpop(timeout, option, count, keys)).thenReturn(keyValueStringListTupleCommandObject);
    when(commandExecutor.executeCommand(keyValueStringListTupleCommandObject)).thenReturn(expectedPopResult);

    KeyValue<String, List<Tuple>> result = jedis.bzmpop(timeout, option, count, keys);

    assertThat(result, equalTo(expectedPopResult));

    verify(commandExecutor).executeCommand(keyValueStringListTupleCommandObject);
    verify(commandObjects).bzmpop(timeout, option, count, keys);
  }

  @Test
  public void testBzmpopWithCountBinary() {
    double timeout = 2.0;
    SortedSetOption option = SortedSetOption.MAX;
    int count = 2;
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };
    KeyValue<byte[], List<Tuple>> expectedPopResult = new KeyValue<>("zset1".getBytes(), Arrays.asList(new Tuple("member1", 1.0), new Tuple("member2", 2.0)));

    when(commandObjects.bzmpop(timeout, option, count, keys)).thenReturn(keyValueBytesListTupleCommandObject);
    when(commandExecutor.executeCommand(keyValueBytesListTupleCommandObject)).thenReturn(expectedPopResult);

    KeyValue<byte[], List<Tuple>> result = jedis.bzmpop(timeout, option, count, keys);

    assertThat(result, equalTo(expectedPopResult));

    verify(commandExecutor).executeCommand(keyValueBytesListTupleCommandObject);
    verify(commandObjects).bzmpop(timeout, option, count, keys);
  }

  @Test
  public void testBzpopmax() {
    double timeout = 2.0;
    String[] keys = { "zset1", "zset2" };
    Tuple expectedTuple = new Tuple("member1", 1.0);
    KeyValue<String, Tuple> expectedKeyValue = new KeyValue<>("zset1", expectedTuple);

    when(commandObjects.bzpopmax(timeout, keys)).thenReturn(keyValueStringTupleCommandObject);
    when(commandExecutor.executeCommand(keyValueStringTupleCommandObject)).thenReturn(expectedKeyValue);

    KeyValue<String, Tuple> result = jedis.bzpopmax(timeout, keys);

    assertThat(result, equalTo(expectedKeyValue));

    verify(commandExecutor).executeCommand(keyValueStringTupleCommandObject);
    verify(commandObjects).bzpopmax(timeout, keys);
  }

  @Test
  public void testBzpopmaxBinary() {
    double timeout = 2.0;
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };
    Tuple expectedTuple = new Tuple("member1".getBytes(), 1.0);
    KeyValue<byte[], Tuple> expectedKeyValue = new KeyValue<>("zset1".getBytes(), expectedTuple);

    when(commandObjects.bzpopmax(timeout, keys)).thenReturn(keyValueBytesTupleCommandObject);
    when(commandExecutor.executeCommand(keyValueBytesTupleCommandObject)).thenReturn(expectedKeyValue);

    KeyValue<byte[], Tuple> result = jedis.bzpopmax(timeout, keys);

    assertThat(result, equalTo(expectedKeyValue));

    verify(commandExecutor).executeCommand(keyValueBytesTupleCommandObject);
    verify(commandObjects).bzpopmax(timeout, keys);
  }

  @Test
  public void testBzpopmin() {
    double timeout = 2.0;
    String[] keys = { "zset1", "zset2" };
    Tuple expectedTuple = new Tuple("member1", 1.0);
    KeyValue<String, Tuple> expectedKeyValue = new KeyValue<>("zset1", expectedTuple);

    when(commandObjects.bzpopmin(timeout, keys)).thenReturn(keyValueStringTupleCommandObject);
    when(commandExecutor.executeCommand(keyValueStringTupleCommandObject)).thenReturn(expectedKeyValue);

    KeyValue<String, Tuple> result = jedis.bzpopmin(timeout, keys);

    assertThat(result, equalTo(expectedKeyValue));

    verify(commandExecutor).executeCommand(keyValueStringTupleCommandObject);
    verify(commandObjects).bzpopmin(timeout, keys);
  }

  @Test
  public void testBzpopminBinary() {
    double timeout = 2.0;
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };
    Tuple expectedTuple = new Tuple("member1".getBytes(), 1.0);
    KeyValue<byte[], Tuple> expectedKeyValue = new KeyValue<>("zset1".getBytes(), expectedTuple);

    when(commandObjects.bzpopmin(timeout, keys)).thenReturn(keyValueBytesTupleCommandObject);
    when(commandExecutor.executeCommand(keyValueBytesTupleCommandObject)).thenReturn(expectedKeyValue);

    KeyValue<byte[], Tuple> result = jedis.bzpopmin(timeout, keys);

    assertThat(result, equalTo(expectedKeyValue));

    verify(commandExecutor).executeCommand(keyValueBytesTupleCommandObject);
    verify(commandObjects).bzpopmin(timeout, keys);
  }

  @Test
  public void testZadd() {
    String key = "zsetKey";
    double score = 1.0;
    String member = "member1";
    long expectedAdded = 1L; // Assuming the member was successfully added

    when(commandObjects.zadd(key, score, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedAdded);

    long result = jedis.zadd(key, score, member);

    assertThat(result, equalTo(expectedAdded));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zadd(key, score, member);
  }

  @Test
  public void testZaddBinary() {
    byte[] key = "zsetKey".getBytes();
    double score = 1.0;
    byte[] member = "member1".getBytes();
    long expectedAdded = 1L; // Assuming the member was successfully added

    when(commandObjects.zadd(key, score, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedAdded);

    long result = jedis.zadd(key, score, member);

    assertThat(result, equalTo(expectedAdded));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zadd(key, score, member);
  }

  @Test
  public void testZaddWithParams() {
    String key = "zsetKey";
    double score = 1.0;
    String member = "member1";
    ZAddParams params = ZAddParams.zAddParams().nx();
    long expectedAdded = 1L; // Assuming the member was successfully added with NX flag

    when(commandObjects.zadd(key, score, member, params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedAdded);

    long result = jedis.zadd(key, score, member, params);

    assertThat(result, equalTo(expectedAdded));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zadd(key, score, member, params);
  }

  @Test
  public void testZaddWithParamsBinary() {
    byte[] key = "zsetKey".getBytes();
    double score = 1.0;
    byte[] member = "member1".getBytes();
    ZAddParams params = ZAddParams.zAddParams().nx();
    long expectedAdded = 1L; // Assuming the member was successfully added with NX flag

    when(commandObjects.zadd(key, score, member, params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedAdded);

    long result = jedis.zadd(key, score, member, params);

    assertThat(result, equalTo(expectedAdded));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zadd(key, score, member, params);
  }

  @Test
  public void testZaddMultiple() {
    String key = "zsetKey";
    Map<String, Double> scoreMembers = new HashMap<>();
    scoreMembers.put("member1", 1.0);
    scoreMembers.put("member2", 2.0);
    long expectedAdded = 2L; // Assuming both members were successfully added

    when(commandObjects.zadd(key, scoreMembers)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedAdded);

    long result = jedis.zadd(key, scoreMembers);

    assertThat(result, equalTo(expectedAdded));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zadd(key, scoreMembers);
  }

  @Test
  public void testZaddMultipleBinary() {
    byte[] key = "zsetKey".getBytes();
    Map<byte[], Double> scoreMembers = new HashMap<>();
    scoreMembers.put("member1".getBytes(), 1.0);
    scoreMembers.put("member2".getBytes(), 2.0);
    long expectedAdded = 2L; // Assuming both members were successfully added

    when(commandObjects.zadd(key, scoreMembers)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedAdded);

    long result = jedis.zadd(key, scoreMembers);

    assertThat(result, equalTo(expectedAdded));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zadd(key, scoreMembers);
  }

  @Test
  public void testZaddMultipleWithParams() {
    String key = "zsetKey";
    Map<String, Double> scoreMembers = new HashMap<>();
    scoreMembers.put("member1", 1.0);
    scoreMembers.put("member2", 2.0);
    ZAddParams params = ZAddParams.zAddParams().xx();
    long expectedAdded = 2L; // Assuming both members were successfully added with XX flag

    when(commandObjects.zadd(key, scoreMembers, params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedAdded);

    long result = jedis.zadd(key, scoreMembers, params);

    assertThat(result, equalTo(expectedAdded));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zadd(key, scoreMembers, params);
  }

  @Test
  public void testZaddMultipleWithParamsBinary() {
    byte[] key = "zsetKey".getBytes();
    Map<byte[], Double> scoreMembers = new HashMap<>();
    scoreMembers.put("member1".getBytes(), 1.0);
    scoreMembers.put("member2".getBytes(), 2.0);
    ZAddParams params = ZAddParams.zAddParams().xx();
    long expectedAdded = 2L; // Assuming both members were successfully added with XX flag

    when(commandObjects.zadd(key, scoreMembers, params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedAdded);

    long result = jedis.zadd(key, scoreMembers, params);

    assertThat(result, equalTo(expectedAdded));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zadd(key, scoreMembers, params);
  }

  @Test
  public void testZaddIncr() {
    String key = "zsetKey";
    double score = 1.0;
    String member = "member1";
    ZAddParams params = ZAddParams.zAddParams().ch();
    Double expectedNewScore = 2.0; // Assuming the member's score was incremented to 2.0

    when(commandObjects.zaddIncr(key, score, member, params)).thenReturn(doubleCommandObject);
    when(commandExecutor.executeCommand(doubleCommandObject)).thenReturn(expectedNewScore);

    Double result = jedis.zaddIncr(key, score, member, params);

    assertThat(result, equalTo(expectedNewScore));

    verify(commandExecutor).executeCommand(doubleCommandObject);
    verify(commandObjects).zaddIncr(key, score, member, params);
  }

  @Test
  public void testZaddIncrBinary() {
    byte[] key = "zsetKey".getBytes();
    double score = 1.0;
    byte[] member = "member1".getBytes();
    ZAddParams params = ZAddParams.zAddParams().ch();
    Double expectedNewScore = 2.0; // Assuming the member's score was incremented to 2.0

    when(commandObjects.zaddIncr(key, score, member, params)).thenReturn(doubleCommandObject);
    when(commandExecutor.executeCommand(doubleCommandObject)).thenReturn(expectedNewScore);

    Double result = jedis.zaddIncr(key, score, member, params);

    assertThat(result, equalTo(expectedNewScore));

    verify(commandExecutor).executeCommand(doubleCommandObject);
    verify(commandObjects).zaddIncr(key, score, member, params);
  }

  @Test
  public void testZcard() {
    String key = "zsetKey";
    long expectedCardinality = 5L; // Assuming the sorted set has 5 members

    when(commandObjects.zcard(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.zcard(key);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zcard(key);
  }

  @Test
  public void testZcardBinary() {
    byte[] key = "zsetKey".getBytes();
    long expectedCardinality = 5L; // Assuming the sorted set has 5 members

    when(commandObjects.zcard(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.zcard(key);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zcard(key);
  }

  @Test
  public void testZcount() {
    String key = "zsetKey";
    String min = "1";
    String max = "2";
    long expectedCount = 3L; // Assuming there are 3 members within the score range

    when(commandObjects.zcount(key, min, max)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCount);

    long result = jedis.zcount(key, min, max);

    assertThat(result, equalTo(expectedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zcount(key, min, max);
  }

  @Test
  public void testZcountBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] min = "1".getBytes();
    byte[] max = "2".getBytes();
    long expectedCount = 3L; // Assuming there are 3 members within the score range

    when(commandObjects.zcount(key, min, max)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCount);

    long result = jedis.zcount(key, min, max);

    assertThat(result, equalTo(expectedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zcount(key, min, max);
  }

  @Test
  public void testZcountDouble() {
    String key = "zsetKey";
    double min = 1.0;
    double max = 2.0;
    long expectedCount = 3L; // Assuming there are 3 members within the score range

    when(commandObjects.zcount(key, min, max)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCount);

    long result = jedis.zcount(key, min, max);

    assertThat(result, equalTo(expectedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zcount(key, min, max);
  }

  @Test
  public void testZcountDoubleBinary() {
    byte[] key = "zsetKey".getBytes();
    double min = 1.0;
    double max = 2.0;
    long expectedCount = 3L; // Assuming there are 3 members within the score range

    when(commandObjects.zcount(key, min, max)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCount);

    long result = jedis.zcount(key, min, max);

    assertThat(result, equalTo(expectedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zcount(key, min, max);
  }

  @Test
  public void testZdiff() {
    String[] keys = { "zset1", "zset2", "zset3" };
    List<String> expectedDifference = Arrays.asList("member1", "member3");

    when(commandObjects.zdiff(keys)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedDifference);

    List<String> result = jedis.zdiff(keys);

    assertThat(result, equalTo(expectedDifference));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).zdiff(keys);
  }

  @Test
  public void testZdiffBinary() {
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes(), "zset3".getBytes() };
    List<byte[]> expectedDifference = Arrays.asList("member1".getBytes(), "member3".getBytes());

    when(commandObjects.zdiff(keys)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedDifference);

    List<byte[]> result = jedis.zdiff(keys);

    assertThat(result, equalTo(expectedDifference));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).zdiff(keys);
  }

  @Test
  public void testZdiffWithScores() {
    String[] keys = { "zset1", "zset2", "zset3" };
    List<Tuple> expectedDifferenceWithScores = Arrays.asList(
        new Tuple("member1", 1.0),
        new Tuple("member3", 3.0)
    );

    when(commandObjects.zdiffWithScores(keys)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedDifferenceWithScores);

    List<Tuple> result = jedis.zdiffWithScores(keys);

    assertThat(result, equalTo(expectedDifferenceWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zdiffWithScores(keys);
  }

  @Test
  public void testZdiffWithScoresBinary() {
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes(), "zset3".getBytes() };
    List<Tuple> expectedDifferenceWithScores = Arrays.asList(
        new Tuple("member1".getBytes(), 1.0),
        new Tuple("member3".getBytes(), 3.0)
    );

    when(commandObjects.zdiffWithScores(keys)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedDifferenceWithScores);

    List<Tuple> result = jedis.zdiffWithScores(keys);

    assertThat(result, equalTo(expectedDifferenceWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zdiffWithScores(keys);
  }

  @Test
  public void testZdiffStore() {
    String dstkey = "zsetDiff";
    String[] keys = { "zset1", "zset2", "zset3" };
    long expectedStoredCount = 2L; // Assuming 2 elements were stored

    when(commandObjects.zdiffStore(dstkey, keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStoredCount);

    long result = jedis.zdiffStore(dstkey, keys);

    assertThat(result, equalTo(expectedStoredCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zdiffStore(dstkey, keys);
  }

  @Test
  public void testZdiffStoreBinary() {
    byte[] dstkey = "zsetDiff".getBytes();
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes(), "zset3".getBytes() };
    long expectedStoredCount = 2L; // Assuming 2 elements were stored

    when(commandObjects.zdiffStore(dstkey, keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStoredCount);

    long result = jedis.zdiffStore(dstkey, keys);

    assertThat(result, equalTo(expectedStoredCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zdiffStore(dstkey, keys);
  }

  @Test
  public void testZdiffstore() {
    String dstkey = "zsetDiff";
    String[] keys = { "zset1", "zset2", "zset3" };
    long expectedStoredCount = 2L; // Assuming 2 elements were stored

    when(commandObjects.zdiffstore(dstkey, keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStoredCount);

    long result = jedis.zdiffstore(dstkey, keys);

    assertThat(result, equalTo(expectedStoredCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zdiffstore(dstkey, keys);
  }

  @Test
  public void testZdiffstoreBinary() {
    byte[] dstkey = "zsetDiff".getBytes();
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes(), "zset3".getBytes() };
    long expectedStoredCount = 2L; // Assuming 2 elements were stored

    when(commandObjects.zdiffstore(dstkey, keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStoredCount);

    long result = jedis.zdiffstore(dstkey, keys);

    assertThat(result, equalTo(expectedStoredCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zdiffstore(dstkey, keys);
  }

  @Test
  public void testZincrby() {
    String key = "zsetKey";
    double increment = 2.0;
    String member = "member1";
    double expectedScore = 3.0; // Assuming the member's score was incremented to 3.0

    when(commandObjects.zincrby(key, increment, member)).thenReturn(doubleCommandObject);
    when(commandExecutor.executeCommand(doubleCommandObject)).thenReturn(expectedScore);

    double result = jedis.zincrby(key, increment, member);

    assertThat(result, equalTo(expectedScore));

    verify(commandExecutor).executeCommand(doubleCommandObject);
    verify(commandObjects).zincrby(key, increment, member);
  }

  @Test
  public void testZincrbyBinary() {
    byte[] key = "zsetKey".getBytes();
    double increment = 2.0;
    byte[] member = "member1".getBytes();
    double expectedScore = 3.0; // Assuming the member's score was incremented to 3.0

    when(commandObjects.zincrby(key, increment, member)).thenReturn(doubleCommandObject);
    when(commandExecutor.executeCommand(doubleCommandObject)).thenReturn(expectedScore);

    double result = jedis.zincrby(key, increment, member);

    assertThat(result, equalTo(expectedScore));

    verify(commandExecutor).executeCommand(doubleCommandObject);
    verify(commandObjects).zincrby(key, increment, member);
  }

  @Test
  public void testZincrbyWithParams() {
    String key = "zsetKey";
    double increment = 1.5;
    String member = "member1";
    ZIncrByParams params = ZIncrByParams.zIncrByParams().xx();
    Double expectedNewScore = 4.5; // Assuming the member's score was incremented to 4.5 with XX flag

    when(commandObjects.zincrby(key, increment, member, params)).thenReturn(doubleCommandObject);
    when(commandExecutor.executeCommand(doubleCommandObject)).thenReturn(expectedNewScore);

    Double result = jedis.zincrby(key, increment, member, params);

    assertThat(result, equalTo(expectedNewScore));

    verify(commandExecutor).executeCommand(doubleCommandObject);
    verify(commandObjects).zincrby(key, increment, member, params);
  }

  @Test
  public void testZincrbyWithParamsBinary() {
    byte[] key = "zsetKey".getBytes();
    double increment = 1.5;
    byte[] member = "member1".getBytes();
    ZIncrByParams params = ZIncrByParams.zIncrByParams().xx();
    Double expectedNewScore = 4.5; // Assuming the member's score was incremented to 4.5 with XX flag

    when(commandObjects.zincrby(key, increment, member, params)).thenReturn(doubleCommandObject);
    when(commandExecutor.executeCommand(doubleCommandObject)).thenReturn(expectedNewScore);

    Double result = jedis.zincrby(key, increment, member, params);

    assertThat(result, equalTo(expectedNewScore));

    verify(commandExecutor).executeCommand(doubleCommandObject);
    verify(commandObjects).zincrby(key, increment, member, params);
  }

  @Test
  public void testZinter() {
    ZParams params = new ZParams().weights(2, 3).aggregate(ZParams.Aggregate.SUM);
    String[] keys = { "zset1", "zset2" };
    List<String> expectedIntersection = Arrays.asList("member1", "member2");

    when(commandObjects.zinter(params, keys)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedIntersection);

    List<String> result = jedis.zinter(params, keys);

    assertThat(result, equalTo(expectedIntersection));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).zinter(params, keys);
  }

  @Test
  public void testZinterBinary() {
    ZParams params = new ZParams().weights(1, 2).aggregate(ZParams.Aggregate.MAX);
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };
    List<byte[]> expectedIntersection = Arrays.asList("member1".getBytes(), "member2".getBytes());

    when(commandObjects.zinter(params, keys)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedIntersection);

    List<byte[]> result = jedis.zinter(params, keys);

    assertThat(result, equalTo(expectedIntersection));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).zinter(params, keys);
  }

  @Test
  public void testZinterWithScores() {
    ZParams params = new ZParams().weights(2, 3).aggregate(ZParams.Aggregate.SUM);
    String[] keys = { "zset1", "zset2" };
    List<Tuple> expectedIntersectionWithScores = Arrays.asList(
        new Tuple("member1", 5.0),
        new Tuple("member2", 9.0)
    );

    when(commandObjects.zinterWithScores(params, keys)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedIntersectionWithScores);

    List<Tuple> result = jedis.zinterWithScores(params, keys);

    assertThat(result, equalTo(expectedIntersectionWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zinterWithScores(params, keys);
  }

  @Test
  public void testZinterWithScoresBinary() {
    ZParams params = new ZParams().weights(1, 2).aggregate(ZParams.Aggregate.MAX);
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };
    List<Tuple> expectedIntersectionWithScores = Arrays.asList(new Tuple("member1", 1.0), new Tuple("member2", 2.0));

    when(commandObjects.zinterWithScores(params, keys)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedIntersectionWithScores);

    List<Tuple> result = jedis.zinterWithScores(params, keys);

    assertThat(result, equalTo(expectedIntersectionWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zinterWithScores(params, keys);
  }

  @Test
  public void testZintercard() {
    String[] keys = { "zset1", "zset2" };
    long expectedCardinality = 2L;

    when(commandObjects.zintercard(keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.zintercard(keys);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zintercard(keys);
  }

  @Test
  public void testZintercardBinary() {
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };
    long expectedCardinality = 2L; // Assuming the cardinality of the intersection is 2

    when(commandObjects.zintercard(keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.zintercard(keys);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zintercard(keys);
  }

  @Test
  public void testZintercardWithLimit() {
    String[] keys = { "zset1", "zset2" };
    long limit = 1000L;
    long expectedCardinality = 2L;

    when(commandObjects.zintercard(limit, keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.zintercard(limit, keys);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zintercard(limit, keys);
  }

  @Test
  public void testZintercardWithLimitBinary() {
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };
    long limit = 1000L;
    long expectedIntersectionCardinality = 5L;

    when(commandObjects.zintercard(limit, keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedIntersectionCardinality);

    long result = jedis.zintercard(limit, keys);

    assertThat(result, equalTo(expectedIntersectionCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zintercard(limit, keys);
  }

  @Test
  public void testZinterstore() {
    String dstkey = "zsetInter";
    String[] sets = { "zset1", "zset2" };
    long expectedStoredCount = 3L; // Assuming 3 elements were stored

    when(commandObjects.zinterstore(dstkey, sets)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStoredCount);

    long result = jedis.zinterstore(dstkey, sets);

    assertThat(result, equalTo(expectedStoredCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zinterstore(dstkey, sets);
  }

  @Test
  public void testZinterstoreBinary() {
    byte[] dstkey = "zsetInter".getBytes();
    byte[][] sets = { "zset1".getBytes(), "zset2".getBytes() };
    long expectedStoredCount = 3L; // Assuming 3 elements were stored

    when(commandObjects.zinterstore(dstkey, sets)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStoredCount);

    long result = jedis.zinterstore(dstkey, sets);

    assertThat(result, equalTo(expectedStoredCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zinterstore(dstkey, sets);
  }

  @Test
  public void testZinterstoreWithParams() {
    String dstkey = "zsetInter";
    ZParams params = new ZParams().weights(2, 3).aggregate(ZParams.Aggregate.SUM);
    String[] sets = { "zset1", "zset2" };
    long expectedStoredCount = 3L; // Assuming 3 elements were stored with the specified weights and aggregation

    when(commandObjects.zinterstore(dstkey, params, sets)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStoredCount);

    long result = jedis.zinterstore(dstkey, params, sets);

    assertThat(result, equalTo(expectedStoredCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zinterstore(dstkey, params, sets);
  }

  @Test
  public void testZinterstoreWithParamsBinary() {
    byte[] dstkey = "zsetInter".getBytes();
    ZParams params = new ZParams().weights(2, 3).aggregate(ZParams.Aggregate.SUM);
    byte[][] sets = { "zset1".getBytes(), "zset2".getBytes() };
    long expectedStoredCount = 3L; // Assuming 3 elements were stored with the specified weights and aggregation

    when(commandObjects.zinterstore(dstkey, params, sets)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStoredCount);

    long result = jedis.zinterstore(dstkey, params, sets);

    assertThat(result, equalTo(expectedStoredCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zinterstore(dstkey, params, sets);
  }

  @Test
  public void testZlexcount() {
    String key = "zsetKey";
    String min = "[a";
    String max = "(b";
    long expectedCount = 5L; // Assuming there are 5 elements in the lex range

    when(commandObjects.zlexcount(key, min, max)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCount);

    long result = jedis.zlexcount(key, min, max);

    assertThat(result, equalTo(expectedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zlexcount(key, min, max);
  }

  @Test
  public void testZlexcountBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] min = "[a".getBytes();
    byte[] max = "[b".getBytes();
    long expectedCount = 5L; // Assuming there are 5 elements in the lex range

    when(commandObjects.zlexcount(key, min, max)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCount);

    long result = jedis.zlexcount(key, min, max);

    assertThat(result, equalTo(expectedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zlexcount(key, min, max);
  }

  @Test
  public void testZmpop() {
    SortedSetOption option = SortedSetOption.MAX;
    String[] keys = { "zset1", "zset2" };
    KeyValue<String, List<Tuple>> expectedPopResult = new KeyValue<>("zset1", Collections.singletonList(new Tuple("member1", 1.0)));

    when(commandObjects.zmpop(option, keys)).thenReturn(keyValueStringListTupleCommandObject);
    when(commandExecutor.executeCommand(keyValueStringListTupleCommandObject)).thenReturn(expectedPopResult);

    KeyValue<String, List<Tuple>> result = jedis.zmpop(option, keys);

    assertThat(result, equalTo(expectedPopResult));

    verify(commandExecutor).executeCommand(keyValueStringListTupleCommandObject);
    verify(commandObjects).zmpop(option, keys);
  }

  @Test
  public void testZmpopBinary() {
    SortedSetOption option = SortedSetOption.MAX;
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };
    KeyValue<byte[], List<Tuple>> expectedPopResult = new KeyValue<>("zset1".getBytes(), Collections.singletonList(new Tuple("member1", 1.0)));

    when(commandObjects.zmpop(option, keys)).thenReturn(keyValueBytesListTupleCommandObject);
    when(commandExecutor.executeCommand(keyValueBytesListTupleCommandObject)).thenReturn(expectedPopResult);

    KeyValue<byte[], List<Tuple>> result = jedis.zmpop(option, keys);

    assertThat(result, equalTo(expectedPopResult));

    verify(commandExecutor).executeCommand(keyValueBytesListTupleCommandObject);
    verify(commandObjects).zmpop(option, keys);
  }

  @Test
  public void testZmpopWithCount() {
    SortedSetOption option = SortedSetOption.MAX;
    int count = 2;
    String[] keys = { "zset1", "zset2" };
    KeyValue<String, List<Tuple>> expectedPopResult = new KeyValue<>("zset1", Arrays.asList(new Tuple("member1", 1.0), new Tuple("member2", 2.0)));

    when(commandObjects.zmpop(option, count, keys)).thenReturn(keyValueStringListTupleCommandObject);
    when(commandExecutor.executeCommand(keyValueStringListTupleCommandObject)).thenReturn(expectedPopResult);

    KeyValue<String, List<Tuple>> result = jedis.zmpop(option, count, keys);

    assertThat(result, equalTo(expectedPopResult));

    verify(commandExecutor).executeCommand(keyValueStringListTupleCommandObject);
    verify(commandObjects).zmpop(option, count, keys);
  }

  @Test
  public void testZmpopWithCountBinary() {
    SortedSetOption option = SortedSetOption.MAX;
    int count = 2;
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };
    KeyValue<byte[], List<Tuple>> expectedPopResult = new KeyValue<>("zset1".getBytes(), Arrays.asList(new Tuple("member1", 1.0), new Tuple("member2", 2.0)));

    when(commandObjects.zmpop(option, count, keys)).thenReturn(keyValueBytesListTupleCommandObject);
    when(commandExecutor.executeCommand(keyValueBytesListTupleCommandObject)).thenReturn(expectedPopResult);

    KeyValue<byte[], List<Tuple>> result = jedis.zmpop(option, count, keys);

    assertThat(result, equalTo(expectedPopResult));

    verify(commandExecutor).executeCommand(keyValueBytesListTupleCommandObject);
    verify(commandObjects).zmpop(option, count, keys);
  }

  @Test
  public void testZmscore() {
    String key = "zsetKey";
    String[] members = { "member1", "member2" };
    List<Double> expectedScores = Arrays.asList(1.0, 2.0); // Assuming the members have scores of 1.0 and 2.0 respectively

    when(commandObjects.zmscore(key, members)).thenReturn(listDoubleCommandObject);
    when(commandExecutor.executeCommand(listDoubleCommandObject)).thenReturn(expectedScores);

    List<Double> result = jedis.zmscore(key, members);

    assertThat(result, equalTo(expectedScores));

    verify(commandExecutor).executeCommand(listDoubleCommandObject);
    verify(commandObjects).zmscore(key, members);
  }

  @Test
  public void testZmscoreBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };
    List<Double> expectedScores = Arrays.asList(1.0, 2.0); // Assuming the members have scores of 1.0 and 2.0 respectively

    when(commandObjects.zmscore(key, members)).thenReturn(listDoubleCommandObject);
    when(commandExecutor.executeCommand(listDoubleCommandObject)).thenReturn(expectedScores);

    List<Double> result = jedis.zmscore(key, members);

    assertThat(result, equalTo(expectedScores));

    verify(commandExecutor).executeCommand(listDoubleCommandObject);
    verify(commandObjects).zmscore(key, members);
  }

  @Test
  public void testZpopmax() {
    String key = "zsetKey";
    Tuple expectedTuple = new Tuple("member1", 2.0); // Assuming this member has the highest score

    when(commandObjects.zpopmax(key)).thenReturn(tupleCommandObject);
    when(commandExecutor.executeCommand(tupleCommandObject)).thenReturn(expectedTuple);

    Tuple result = jedis.zpopmax(key);

    assertThat(result, equalTo(expectedTuple));

    verify(commandExecutor).executeCommand(tupleCommandObject);
    verify(commandObjects).zpopmax(key);
  }

  @Test
  public void testZpopmaxBinary() {
    byte[] key = "zsetKey".getBytes();
    Tuple expectedTuple = new Tuple("member1".getBytes(), 2.0); // Assuming this member has the highest score

    when(commandObjects.zpopmax(key)).thenReturn(tupleCommandObject);
    when(commandExecutor.executeCommand(tupleCommandObject)).thenReturn(expectedTuple);

    Tuple result = jedis.zpopmax(key);

    assertThat(result, equalTo(expectedTuple));

    verify(commandExecutor).executeCommand(tupleCommandObject);
    verify(commandObjects).zpopmax(key);
  }

  @Test
  public void testZpopmaxWithCount() {
    String key = "zsetKey";
    int count = 2;
    List<Tuple> expectedTuples = Arrays.asList(
        new Tuple("member1", 2.0),
        new Tuple("member2", 1.5)
    ); // Assuming these members have the highest scores

    when(commandObjects.zpopmax(key, count)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedTuples);

    List<Tuple> result = jedis.zpopmax(key, count);

    assertThat(result, equalTo(expectedTuples));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zpopmax(key, count);
  }

  @Test
  public void testZpopmaxWithCountBinary() {
    byte[] key = "zsetKey".getBytes();
    int count = 2;
    List<Tuple> expectedTuples = Arrays.asList(
        new Tuple("member1".getBytes(), 2.0),
        new Tuple("member2".getBytes(), 1.5)
    ); // Assuming these members have the highest scores

    when(commandObjects.zpopmax(key, count)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedTuples);

    List<Tuple> result = jedis.zpopmax(key, count);

    assertThat(result, equalTo(expectedTuples));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zpopmax(key, count);
  }

  @Test
  public void testZpopmin() {
    String key = "zsetKey";
    Tuple expectedTuple = new Tuple("member1", 1.0); // Assuming this member has the lowest score

    when(commandObjects.zpopmin(key)).thenReturn(tupleCommandObject);
    when(commandExecutor.executeCommand(tupleCommandObject)).thenReturn(expectedTuple);

    Tuple result = jedis.zpopmin(key);

    assertThat(result, equalTo(expectedTuple));

    verify(commandExecutor).executeCommand(tupleCommandObject);
    verify(commandObjects).zpopmin(key);
  }

  @Test
  public void testZpopminBinary() {
    byte[] key = "zsetKey".getBytes();
    Tuple expectedTuple = new Tuple("member1".getBytes(), 1.0); // Assuming this member has the lowest score

    when(commandObjects.zpopmin(key)).thenReturn(tupleCommandObject);
    when(commandExecutor.executeCommand(tupleCommandObject)).thenReturn(expectedTuple);

    Tuple result = jedis.zpopmin(key);

    assertThat(result, equalTo(expectedTuple));

    verify(commandExecutor).executeCommand(tupleCommandObject);
    verify(commandObjects).zpopmin(key);
  }

  @Test
  public void testZpopminWithCount() {
    String key = "zsetKey";
    int count = 2;
    List<Tuple> expectedTuples = Arrays.asList(
        new Tuple("member1", 1.0),
        new Tuple("member2", 1.5)
    ); // Assuming these members have the lowest scores

    when(commandObjects.zpopmin(key, count)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedTuples);

    List<Tuple> result = jedis.zpopmin(key, count);

    assertThat(result, equalTo(expectedTuples));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zpopmin(key, count);
  }

  @Test
  public void testZpopminWithCountBinary() {
    byte[] key = "zsetKey".getBytes();
    int count = 2;
    List<Tuple> expectedTuples = Arrays.asList(
        new Tuple("member1".getBytes(), 1.0),
        new Tuple("member2".getBytes(), 1.5)
    ); // Assuming these members have the lowest scores

    when(commandObjects.zpopmin(key, count)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedTuples);

    List<Tuple> result = jedis.zpopmin(key, count);

    assertThat(result, equalTo(expectedTuples));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zpopmin(key, count);
  }

  @Test
  public void testZrandmember() {
    String key = "zsetKey";
    String expectedMember = "member1"; // Assuming this member is randomly selected

    when(commandObjects.zrandmember(key)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedMember);

    String result = jedis.zrandmember(key);

    assertThat(result, equalTo(expectedMember));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).zrandmember(key);
  }

  @Test
  public void testZrandmemberBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] expectedMember = "member1".getBytes(); // Assuming this member is randomly selected

    when(commandObjects.zrandmember(key)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedMember);

    byte[] result = jedis.zrandmember(key);

    assertArrayEquals(expectedMember, result);

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).zrandmember(key);
  }

  @Test
  public void testZrandmemberWithCount() {
    String key = "zsetKey";
    long count = 2;
    List<String> expectedMembers = Arrays.asList("member1", "member2"); // Assuming these members are randomly selected

    when(commandObjects.zrandmember(key, count)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedMembers);

    List<String> result = jedis.zrandmember(key, count);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).zrandmember(key, count);
  }

  @Test
  public void testZrandmemberBytesWithCount() {
    byte[] key = "zsetKey".getBytes();
    long count = 2;
    List<byte[]> expectedMembers = Arrays.asList("member1".getBytes(), "member2".getBytes()); // Assuming these members are randomly selected

    when(commandObjects.zrandmember(key, count)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedMembers);

    List<byte[]> result = jedis.zrandmember(key, count);

    for (int i = 0; i < expectedMembers.size(); i++) {
      assertArrayEquals(expectedMembers.get(i), result.get(i));
    }

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).zrandmember(key, count);
  }

  @Test
  public void testZrandmemberWithScores() {
    String key = "zsetKey";
    long count = 2;
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member1", 1.0),
        new Tuple("member2", 2.0)
    ); // Assuming these members with scores are randomly selected

    when(commandObjects.zrandmemberWithScores(key, count)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrandmemberWithScores(key, count);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrandmemberWithScores(key, count);
  }

  @Test
  public void testZrandmemberWithScoresBinary() {
    byte[] key = "zsetKey".getBytes();
    long count = 2;
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member1".getBytes(), 1.0),
        new Tuple("member2".getBytes(), 2.0)
    ); // Assuming these members with scores are randomly selected

    when(commandObjects.zrandmemberWithScores(key, count)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrandmemberWithScores(key, count);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrandmemberWithScores(key, count);
  }

  @Test
  public void testZrange() {
    String key = "zsetKey";
    long start = 0;
    long stop = -1; // This typically means all elements in the sorted set
    List<String> expectedMembers = Arrays.asList("member1", "member2", "member3");

    when(commandObjects.zrange(key, start, stop)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedMembers);

    List<String> result = jedis.zrange(key, start, stop);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).zrange(key, start, stop);
  }

  @Test
  public void testZrangeBinary() {
    byte[] key = "zsetKey".getBytes();
    long start = 0;
    long stop = -1; // This typically means all elements in the sorted set
    List<byte[]> expectedMembers = Arrays.asList("member1".getBytes(), "member2".getBytes(), "member3".getBytes());

    when(commandObjects.zrange(key, start, stop)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedMembers);

    List<byte[]> result = jedis.zrange(key, start, stop);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).zrange(key, start, stop);
  }

  @Test
  public void testZrangeWithScores() {
    String key = "zsetKey";
    long start = 0;
    long stop = -1; // This typically means all elements in the sorted set with their scores
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member1", 1.0),
        new Tuple("member2", 2.0),
        new Tuple("member3", 3.0)
    );

    when(commandObjects.zrangeWithScores(key, start, stop)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrangeWithScores(key, start, stop);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrangeWithScores(key, start, stop);
  }

  @Test
  public void testZrangeWithScoresBinary() {
    byte[] key = "zsetKey".getBytes();
    long start = 0;
    long stop = -1; // This typically means all elements in the sorted set with their scores
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member1".getBytes(), 1.0),
        new Tuple("member2".getBytes(), 2.0),
        new Tuple("member3".getBytes(), 3.0)
    );

    when(commandObjects.zrangeWithScores(key, start, stop)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrangeWithScores(key, start, stop);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrangeWithScores(key, start, stop);
  }

  @Test
  public void testZrangeWithZRangeParams() {
    String key = "zsetKey";
    ZRangeParams zRangeParams = ZRangeParams.zrangeParams(1, 3);
    List<String> expectedMembers = Arrays.asList("member1", "member2");

    when(commandObjects.zrange(key, zRangeParams)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedMembers);

    List<String> result = jedis.zrange(key, zRangeParams);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).zrange(key, zRangeParams);
  }

  @Test
  public void testZrangeWithZRangeParamsBinary() {
    byte[] key = "zsetKey".getBytes();
    ZRangeParams zRangeParams = ZRangeParams.zrangeParams(1, 3);
    List<byte[]> expectedMembers = Arrays.asList("member1".getBytes(), "member2".getBytes());

    when(commandObjects.zrange(key, zRangeParams)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedMembers);

    List<byte[]> result = jedis.zrange(key, zRangeParams);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).zrange(key, zRangeParams);
  }

  @Test
  public void testZrangeWithScoresWithZRangeParams() {
    String key = "zsetKey";
    ZRangeParams zRangeParams = ZRangeParams.zrangeParams(1, 3);
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member1", 1.0),
        new Tuple("member2", 2.0)
    );

    when(commandObjects.zrangeWithScores(key, zRangeParams)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrangeWithScores(key, zRangeParams);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrangeWithScores(key, zRangeParams);
  }

  @Test
  public void testZrangeWithScoresWithZRangeParamsBinary() {
    byte[] key = "zsetKey".getBytes();
    ZRangeParams zRangeParams = ZRangeParams.zrangeParams(1, 3);
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member1".getBytes(), 1.0),
        new Tuple("member2".getBytes(), 2.0)
    );

    when(commandObjects.zrangeWithScores(key, zRangeParams)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrangeWithScores(key, zRangeParams);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrangeWithScores(key, zRangeParams);
  }

  @Test
  public void testZrangeByLex() {
    String key = "zsetKey";
    String min = "[a";
    String max = "(b";
    List<String> expectedMembers = Arrays.asList("alpha", "beta");

    when(commandObjects.zrangeByLex(key, min, max)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedMembers);

    List<String> result = jedis.zrangeByLex(key, min, max);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).zrangeByLex(key, min, max);
  }

  @Test
  public void testZrangeByLexBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] min = "[a".getBytes();
    byte[] max = "[b".getBytes();
    List<byte[]> expectedMembers = Arrays.asList("alpha".getBytes(), "beta".getBytes());

    when(commandObjects.zrangeByLex(key, min, max)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedMembers);

    List<byte[]> result = jedis.zrangeByLex(key, min, max);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).zrangeByLex(key, min, max);
  }

  @Test
  public void testZrangeByLexWithOffsetCount() {
    String key = "zsetKey";
    String min = "[a";
    String max = "(b";
    int offset = 1;
    int count = 2;
    List<String> expectedMembers = Arrays.asList("beta", "gamma");

    when(commandObjects.zrangeByLex(key, min, max, offset, count)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedMembers);

    List<String> result = jedis.zrangeByLex(key, min, max, offset, count);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).zrangeByLex(key, min, max, offset, count);
  }

  @Test
  public void testZrangeByLexWithOffsetCountBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] min = "[a".getBytes();
    byte[] max = "[b".getBytes();
    int offset = 1;
    int count = 2;
    List<byte[]> expectedMembers = Arrays.asList("beta".getBytes(), "gamma".getBytes());

    when(commandObjects.zrangeByLex(key, min, max, offset, count)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedMembers);

    List<byte[]> result = jedis.zrangeByLex(key, min, max, offset, count);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).zrangeByLex(key, min, max, offset, count);
  }

  @Test
  public void testZrangeByScore() {
    String key = "zsetKey";
    String min = "1";
    String max = "3";
    List<String> expectedMembers = Arrays.asList("member1", "member2");

    when(commandObjects.zrangeByScore(key, min, max)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedMembers);

    List<String> result = jedis.zrangeByScore(key, min, max);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).zrangeByScore(key, min, max);
  }

  @Test
  public void testZrangeByScoreBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] min = "1".getBytes();
    byte[] max = "3".getBytes();
    List<byte[]> expectedMembers = Arrays.asList("member1".getBytes(), "member2".getBytes());

    when(commandObjects.zrangeByScore(key, min, max)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedMembers);

    List<byte[]> result = jedis.zrangeByScore(key, min, max);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).zrangeByScore(key, min, max);
  }

  @Test
  public void testZrangeByScoreDouble() {
    String key = "zsetKey";
    double min = 1.0;
    double max = 3.0;
    List<String> expectedMembers = Arrays.asList("member1", "member2");

    when(commandObjects.zrangeByScore(key, min, max)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedMembers);

    List<String> result = jedis.zrangeByScore(key, min, max);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).zrangeByScore(key, min, max);
  }

  @Test
  public void testZrangeByScoreDoubleBinary() {
    byte[] key = "zsetKey".getBytes();
    double min = 1.0;
    double max = 3.0;
    List<byte[]> expectedMembers = Arrays.asList("member1".getBytes(), "member2".getBytes());

    when(commandObjects.zrangeByScore(key, min, max)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedMembers);

    List<byte[]> result = jedis.zrangeByScore(key, min, max);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).zrangeByScore(key, min, max);
  }

  @Test
  public void testZrangeByScoreWithOffsetCount() {
    String key = "zsetKey";
    String min = "1";
    String max = "3";
    int offset = 1;
    int count = 2;
    List<String> expectedMembers = Arrays.asList("member2", "member3");

    when(commandObjects.zrangeByScore(key, min, max, offset, count)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedMembers);

    List<String> result = jedis.zrangeByScore(key, min, max, offset, count);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).zrangeByScore(key, min, max, offset, count);
  }

  @Test
  public void testZrangeByScoreWithOffsetCountBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] min = "1".getBytes();
    byte[] max = "3".getBytes();
    int offset = 1;
    int count = 2;
    List<byte[]> expectedMembers = Arrays.asList("member2".getBytes(), "member3".getBytes());

    when(commandObjects.zrangeByScore(key, min, max, offset, count)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedMembers);

    List<byte[]> result = jedis.zrangeByScore(key, min, max, offset, count);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).zrangeByScore(key, min, max, offset, count);
  }

  @Test
  public void testZrangeByScoreDoubleWithOffsetCount() {
    String key = "zsetKey";
    double min = 1.0;
    double max = 3.0;
    int offset = 1;
    int count = 2;
    List<String> expectedMembers = Arrays.asList("member2", "member3");

    when(commandObjects.zrangeByScore(key, min, max, offset, count)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedMembers);

    List<String> result = jedis.zrangeByScore(key, min, max, offset, count);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).zrangeByScore(key, min, max, offset, count);
  }

  @Test
  public void testZrangeByScoreDoubleWithOffsetCountBinary() {
    byte[] key = "zsetKey".getBytes();
    double min = 1.0;
    double max = 3.0;
    int offset = 1;
    int count = 2;
    List<byte[]> expectedMembers = Arrays.asList("member2".getBytes(), "member3".getBytes());

    when(commandObjects.zrangeByScore(key, min, max, offset, count)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedMembers);

    List<byte[]> result = jedis.zrangeByScore(key, min, max, offset, count);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).zrangeByScore(key, min, max, offset, count);
  }

  @Test
  public void testZrangeByScoreWithScores() {
    String key = "zsetKey";
    String min = "1";
    String max = "3";
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member1", 1.0),
        new Tuple("member2", 2.0)
    );

    when(commandObjects.zrangeByScoreWithScores(key, min, max)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrangeByScoreWithScores(key, min, max);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrangeByScoreWithScores(key, min, max);
  }

  @Test
  public void testZrangeByScoreWithScoresBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] min = "1".getBytes();
    byte[] max = "3".getBytes();
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member1".getBytes(), 1.0),
        new Tuple("member2".getBytes(), 2.0)
    );

    when(commandObjects.zrangeByScoreWithScores(key, min, max)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrangeByScoreWithScores(key, min, max);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrangeByScoreWithScores(key, min, max);
  }

  @Test
  public void testZrangeByScoreWithScoresDouble() {
    String key = "zsetKey";
    double min = 1.0;
    double max = 3.0;
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member1", 1.0),
        new Tuple("member2", 2.0)
    );

    when(commandObjects.zrangeByScoreWithScores(key, min, max)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrangeByScoreWithScores(key, min, max);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrangeByScoreWithScores(key, min, max);
  }

  @Test
  public void testZrangeByScoreWithScoresDoubleBinary() {
    byte[] key = "zsetKey".getBytes();
    double min = 1.0;
    double max = 3.0;
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member1".getBytes(), 1.0),
        new Tuple("member2".getBytes(), 2.0)
    );

    when(commandObjects.zrangeByScoreWithScores(key, min, max)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrangeByScoreWithScores(key, min, max);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrangeByScoreWithScores(key, min, max);
  }

  @Test
  public void testZrangeByScoreWithScoresWithOffsetCount() {
    String key = "zsetKey";
    String min = "1";
    String max = "3";
    int offset = 1;
    int count = 2;
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member2", 2.0),
        new Tuple("member3", 3.0)
    );

    when(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrangeByScoreWithScores(key, min, max, offset, count);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrangeByScoreWithScores(key, min, max, offset, count);
  }

  @Test
  public void testZrangeByScoreWithScoresWithOffsetCountBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] min = "1".getBytes();
    byte[] max = "3".getBytes();
    int offset = 1;
    int count = 2;
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member2".getBytes(), 2.0),
        new Tuple("member3".getBytes(), 3.0)
    );

    when(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrangeByScoreWithScores(key, min, max, offset, count);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrangeByScoreWithScores(key, min, max, offset, count);
  }

  @Test
  public void testZrangeByScoreWithScoresDoubleWithOffsetCount() {
    String key = "zsetKey";
    double min = 1.0;
    double max = 3.0;
    int offset = 1;
    int count = 2;
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member2", 2.0),
        new Tuple("member3", 3.0)
    );

    when(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrangeByScoreWithScores(key, min, max, offset, count);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrangeByScoreWithScores(key, min, max, offset, count);
  }

  @Test
  public void testZrangeByScoreWithScoresDoubleWithOffsetCountBinary() {
    byte[] key = "zsetKey".getBytes();
    double min = 1.0;
    double max = 3.0;
    int offset = 1;
    int count = 2;
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member2".getBytes(), 2.0),
        new Tuple("member3".getBytes(), 3.0)
    );

    when(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrangeByScoreWithScores(key, min, max, offset, count);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrangeByScoreWithScores(key, min, max, offset, count);
  }

  @Test
  public void testZrangestore() {
    String dest = "destinationKey";
    String src = "sourceKey";
    ZRangeParams zRangeParams = ZRangeParams.zrangeParams(1, 3);
    long expectedStoredCount = 2L; // Assuming 2 members were within the range and stored

    when(commandObjects.zrangestore(dest, src, zRangeParams)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStoredCount);

    long result = jedis.zrangestore(dest, src, zRangeParams);

    assertThat(result, equalTo(expectedStoredCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zrangestore(dest, src, zRangeParams);
  }

  @Test
  public void testZrangestoreBinary() {
    byte[] dest = "destinationKey".getBytes();
    byte[] src = "sourceKey".getBytes();
    ZRangeParams zRangeParams = ZRangeParams.zrangeParams(1, 3);
    long expectedStoredCount = 2L; // Assuming 2 members were within the range and stored

    when(commandObjects.zrangestore(dest, src, zRangeParams)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStoredCount);

    long result = jedis.zrangestore(dest, src, zRangeParams);

    assertThat(result, equalTo(expectedStoredCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zrangestore(dest, src, zRangeParams);
  }

  @Test
  public void testZrank() {
    String key = "zsetKey";
    String member = "member1";
    Long expectedRank = 0L; // Assuming the member is the first in the sorted set

    when(commandObjects.zrank(key, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedRank);

    Long result = jedis.zrank(key, member);

    assertThat(result, equalTo(expectedRank));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zrank(key, member);
  }

  @Test
  public void testZrankBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] member = "member1".getBytes();
    Long expectedRank = 0L; // Assuming the member is the first in the sorted set

    when(commandObjects.zrank(key, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedRank);

    Long result = jedis.zrank(key, member);

    assertThat(result, equalTo(expectedRank));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zrank(key, member);
  }

  @Test
  public void testZrankWithScore() {
    String key = "zsetKey";
    String member = "member1";
    KeyValue<Long, Double> expectedRankWithScore = new KeyValue<>(0L, 1.0); // Assuming the member is the first with a score of 1.0

    when(commandObjects.zrankWithScore(key, member)).thenReturn(keyValueLongDoubleCommandObject);
    when(commandExecutor.executeCommand(keyValueLongDoubleCommandObject)).thenReturn(expectedRankWithScore);

    KeyValue<Long, Double> result = jedis.zrankWithScore(key, member);

    assertThat(result, equalTo(expectedRankWithScore));

    verify(commandExecutor).executeCommand(keyValueLongDoubleCommandObject);
    verify(commandObjects).zrankWithScore(key, member);
  }

  @Test
  public void testZrankWithScoreBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] member = "member1".getBytes();
    KeyValue<Long, Double> expectedRankWithScore = new KeyValue<>(0L, 1.0); // Assuming the member is the first with a score of 1.0

    when(commandObjects.zrankWithScore(key, member)).thenReturn(keyValueLongDoubleCommandObject);
    when(commandExecutor.executeCommand(keyValueLongDoubleCommandObject)).thenReturn(expectedRankWithScore);

    KeyValue<Long, Double> result = jedis.zrankWithScore(key, member);

    assertThat(result, equalTo(expectedRankWithScore));

    verify(commandExecutor).executeCommand(keyValueLongDoubleCommandObject);
    verify(commandObjects).zrankWithScore(key, member);
  }

  @Test
  public void testZrem() {
    String key = "zsetKey";
    String[] members = { "member1", "member2" };
    long expectedRemoved = 2L; // Assuming both members were successfully removed

    when(commandObjects.zrem(key, members)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedRemoved);

    long result = jedis.zrem(key, members);

    assertThat(result, equalTo(expectedRemoved));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zrem(key, members);
  }

  @Test
  public void testZremBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };
    long expectedRemoved = 2L; // Assuming both members were successfully removed

    when(commandObjects.zrem(key, members)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedRemoved);

    long result = jedis.zrem(key, members);

    assertThat(result, equalTo(expectedRemoved));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zrem(key, members);
  }

  @Test
  public void testZremrangeByLex() {
    String key = "zsetKey";
    String min = "[a";
    String max = "[b";
    long expectedRemovals = 2L; // Assuming 2 elements were removed

    when(commandObjects.zremrangeByLex(key, min, max)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedRemovals);

    long result = jedis.zremrangeByLex(key, min, max);

    assertThat(result, equalTo(expectedRemovals));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zremrangeByLex(key, min, max);
  }

  @Test
  public void testZremrangeByLexBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] min = "[a".getBytes();
    byte[] max = "[b".getBytes();
    long expectedRemovals = 2L; // Assuming 2 elements were removed

    when(commandObjects.zremrangeByLex(key, min, max)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedRemovals);

    long result = jedis.zremrangeByLex(key, min, max);

    assertThat(result, equalTo(expectedRemovals));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zremrangeByLex(key, min, max);
  }

  @Test
  public void testZremrangeByRank() {
    String key = "zsetKey";
    long start = 0;
    long stop = 2;
    long expectedRemovals = 3L; // Assuming 3 elements were removed

    when(commandObjects.zremrangeByRank(key, start, stop)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedRemovals);

    long result = jedis.zremrangeByRank(key, start, stop);

    assertThat(result, equalTo(expectedRemovals));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zremrangeByRank(key, start, stop);
  }

  @Test
  public void testZremrangeByRankBinary() {
    byte[] key = "zsetKey".getBytes();
    long start = 0;
    long stop = 2;
    long expectedRemovals = 3L; // Assuming 3 elements were removed

    when(commandObjects.zremrangeByRank(key, start, stop)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedRemovals);

    long result = jedis.zremrangeByRank(key, start, stop);

    assertThat(result, equalTo(expectedRemovals));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zremrangeByRank(key, start, stop);
  }

  @Test
  public void testZremrangeByScore() {
    String key = "zsetKey";
    String min = "1";
    String max = "3";
    long expectedRemovals = 2L; // Assuming 2 elements were removed

    when(commandObjects.zremrangeByScore(key, min, max)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedRemovals);

    long result = jedis.zremrangeByScore(key, min, max);

    assertThat(result, equalTo(expectedRemovals));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zremrangeByScore(key, min, max);
  }

  @Test
  public void testZremrangeByScoreBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] min = "1".getBytes();
    byte[] max = "3".getBytes();
    long expectedRemovals = 2L; // Assuming 2 elements were removed

    when(commandObjects.zremrangeByScore(key, min, max)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedRemovals);

    long result = jedis.zremrangeByScore(key, min, max);

    assertThat(result, equalTo(expectedRemovals));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zremrangeByScore(key, min, max);
  }

  @Test
  public void testZremrangeByScoreDouble() {
    String key = "zsetKey";
    double min = 1.0;
    double max = 3.0;
    long expectedRemovals = 2L; // Assuming 2 elements were removed

    when(commandObjects.zremrangeByScore(key, min, max)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedRemovals);

    long result = jedis.zremrangeByScore(key, min, max);

    assertThat(result, equalTo(expectedRemovals));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zremrangeByScore(key, min, max);
  }

  @Test
  public void testZremrangeByScoreDoubleBinary() {
    byte[] key = "zsetKey".getBytes();
    double min = 1.0;
    double max = 3.0;
    long expectedRemovals = 2L; // Assuming 2 elements were removed

    when(commandObjects.zremrangeByScore(key, min, max)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedRemovals);

    long result = jedis.zremrangeByScore(key, min, max);

    assertThat(result, equalTo(expectedRemovals));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zremrangeByScore(key, min, max);
  }

  @Test
  public void testZrevrange() {
    String key = "zsetKey";
    long start = 0;
    long stop = -1; // This typically means all elements in the sorted set, in reverse order
    List<String> expectedMembers = Arrays.asList("member3", "member2", "member1");

    when(commandObjects.zrevrange(key, start, stop)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedMembers);

    List<String> result = jedis.zrevrange(key, start, stop);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).zrevrange(key, start, stop);
  }

  @Test
  public void testZrevrangeBinary() {
    byte[] key = "zsetKey".getBytes();
    long start = 0;
    long stop = -1; // This typically means all elements in the sorted set, in reverse order
    List<byte[]> expectedMembers = Arrays.asList("member3".getBytes(), "member2".getBytes(), "member1".getBytes());

    when(commandObjects.zrevrange(key, start, stop)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedMembers);

    List<byte[]> result = jedis.zrevrange(key, start, stop);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).zrevrange(key, start, stop);
  }

  @Test
  public void testZrevrangeWithScores() {
    String key = "zsetKey";
    long start = 0;
    long stop = -1; // This typically means all elements in the sorted set with their scores, in reverse order
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member3", 3.0),
        new Tuple("member2", 2.0),
        new Tuple("member1", 1.0)
    );

    when(commandObjects.zrevrangeWithScores(key, start, stop)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrevrangeWithScores(key, start, stop);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrevrangeWithScores(key, start, stop);
  }

  @Test
  public void testZrevrangeWithScoresBinary() {
    byte[] key = "zsetKey".getBytes();
    long start = 0;
    long stop = -1; // This typically means all elements in the sorted set with their scores, in reverse order
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member3".getBytes(), 3.0),
        new Tuple("member2".getBytes(), 2.0),
        new Tuple("member1".getBytes(), 1.0)
    );
    when(commandObjects.zrevrangeWithScores(key, start, stop)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrevrangeWithScores(key, start, stop);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrevrangeWithScores(key, start, stop);
  }

  @Test
  public void testZrevrangeByLex() {
    String key = "zsetKey";
    String max = "[z";
    String min = "[a";
    List<String> expectedMembers = Arrays.asList("omega", "mu", "alpha");

    when(commandObjects.zrevrangeByLex(key, max, min)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedMembers);

    List<String> result = jedis.zrevrangeByLex(key, max, min);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).zrevrangeByLex(key, max, min);
  }

  @Test
  public void testZrevrangeByLexBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] max = "[z".getBytes();
    byte[] min = "[a".getBytes();
    List<byte[]> expectedMembers = Arrays.asList("omega".getBytes(), "mu".getBytes(), "alpha".getBytes());

    when(commandObjects.zrevrangeByLex(key, max, min)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedMembers);

    List<byte[]> result = jedis.zrevrangeByLex(key, max, min);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).zrevrangeByLex(key, max, min);
  }

  @Test
  public void testZrevrangeByLexWithOffsetCount() {
    String key = "zsetKey";
    String max = "[z";
    String min = "[a";
    int offset = 1;
    int count = 2;
    List<String> expectedMembers = Arrays.asList("mu", "alpha");

    when(commandObjects.zrevrangeByLex(key, max, min, offset, count)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedMembers);

    List<String> result = jedis.zrevrangeByLex(key, max, min, offset, count);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).zrevrangeByLex(key, max, min, offset, count);
  }

  @Test
  public void testZrevrangeByLexWithOffsetCountBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] max = "[z".getBytes();
    byte[] min = "[a".getBytes();
    int offset = 1;
    int count = 2;
    List<byte[]> expectedMembers = Arrays.asList("mu".getBytes(), "alpha".getBytes());

    when(commandObjects.zrevrangeByLex(key, max, min, offset, count)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedMembers);

    List<byte[]> result = jedis.zrevrangeByLex(key, max, min, offset, count);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).zrevrangeByLex(key, max, min, offset, count);
  }

  @Test
  public void testZrevrangeByScore() {
    String key = "zsetKey";
    String max = "3";
    String min = "1";
    List<String> expectedMembers = Arrays.asList("member2", "member1");

    when(commandObjects.zrevrangeByScore(key, max, min)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedMembers);

    List<String> result = jedis.zrevrangeByScore(key, max, min);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).zrevrangeByScore(key, max, min);
  }

  @Test
  public void testZrevrangeByScoreBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] max = "3".getBytes();
    byte[] min = "1".getBytes();
    List<byte[]> expectedMembers = Arrays.asList("member2".getBytes(), "member1".getBytes());

    when(commandObjects.zrevrangeByScore(key, max, min)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedMembers);

    List<byte[]> result = jedis.zrevrangeByScore(key, max, min);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).zrevrangeByScore(key, max, min);
  }

  @Test
  public void testZrevrangeByScoreDouble() {
    String key = "zsetKey";
    double max = 3.0;
    double min = 1.0;
    List<String> expectedMembers = Arrays.asList("member2", "member1");

    when(commandObjects.zrevrangeByScore(key, max, min)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedMembers);

    List<String> result = jedis.zrevrangeByScore(key, max, min);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).zrevrangeByScore(key, max, min);
  }

  @Test
  public void testZrevrangeByScoreDoubleBinary() {
    byte[] key = "zsetKey".getBytes();
    double max = 3.0;
    double min = 1.0;
    List<byte[]> expectedMembers = Arrays.asList("member2".getBytes(), "member1".getBytes());

    when(commandObjects.zrevrangeByScore(key, max, min)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedMembers);

    List<byte[]> result = jedis.zrevrangeByScore(key, max, min);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).zrevrangeByScore(key, max, min);
  }

  @Test
  public void testZrevrangeByScoreDoubleWithLimit() {
    String key = "zsetKey";
    double max = 3.0;
    double min = 1.0;
    int offset = 1;
    int count = 2;
    List<String> expectedMembers = Arrays.asList("member2", "member1");

    when(commandObjects.zrevrangeByScore(key, max, min, offset, count)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedMembers);

    List<String> result = jedis.zrevrangeByScore(key, max, min, offset, count);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).zrevrangeByScore(key, max, min, offset, count);
  }

  @Test
  public void testZrevrangeByScoreDoubleWithLimitBinary() {
    byte[] key = "zsetKey".getBytes();
    double max = 3.0;
    double min = 1.0;
    int offset = 1;
    int count = 2;
    List<byte[]> expectedMembers = Arrays.asList("member2".getBytes(), "member1".getBytes());

    when(commandObjects.zrevrangeByScore(key, max, min, offset, count)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedMembers);

    List<byte[]> result = jedis.zrevrangeByScore(key, max, min, offset, count);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).zrevrangeByScore(key, max, min, offset, count);
  }

  @Test
  public void testZrevrangeByScoreWithScoresDouble() {
    String key = "zsetKey";
    double max = 3.0;
    double min = 1.0;
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member2", 2.0),
        new Tuple("member1", 1.0)
    );

    when(commandObjects.zrevrangeByScoreWithScores(key, max, min)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrevrangeByScoreWithScores(key, max, min);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrevrangeByScoreWithScores(key, max, min);
  }

  @Test
  public void testZrevrangeByScoreWithScoresDoubleBinary() {
    byte[] key = "zsetKey".getBytes();
    double max = 3.0;
    double min = 1.0;
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member2".getBytes(), 2.0),
        new Tuple("member1".getBytes(), 1.0)
    );

    when(commandObjects.zrevrangeByScoreWithScores(key, max, min)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrevrangeByScoreWithScores(key, max, min);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrevrangeByScoreWithScores(key, max, min);
  }

  @Test
  public void testZrevrangeByScoreWithLimit() {
    String key = "zsetKey";
    String max = "3";
    String min = "1";
    int offset = 1;
    int count = 2;
    List<String> expectedMembers = Arrays.asList("member2", "member1");

    when(commandObjects.zrevrangeByScore(key, max, min, offset, count)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedMembers);

    List<String> result = jedis.zrevrangeByScore(key, max, min, offset, count);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).zrevrangeByScore(key, max, min, offset, count);
  }

  @Test
  public void testZrevrangeByScoreWithLimitBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] max = "3".getBytes();
    byte[] min = "1".getBytes();
    int offset = 1;
    int count = 2;
    List<byte[]> expectedMembers = Arrays.asList("member2".getBytes(), "member1".getBytes());

    when(commandObjects.zrevrangeByScore(key, max, min, offset, count)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedMembers);

    List<byte[]> result = jedis.zrevrangeByScore(key, max, min, offset, count);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).zrevrangeByScore(key, max, min, offset, count);
  }

  @Test
  public void testZrevrangeByScoreWithScores() {
    String key = "zsetKey";
    String max = "3";
    String min = "1";
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member2", 2.0),
        new Tuple("member1", 1.0)
    );

    when(commandObjects.zrevrangeByScoreWithScores(key, max, min)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrevrangeByScoreWithScores(key, max, min);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrevrangeByScoreWithScores(key, max, min);
  }

  @Test
  public void testZrevrangeByScoreWithScoresBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] max = "3".getBytes();
    byte[] min = "1".getBytes();
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member2".getBytes(), 2.0),
        new Tuple("member1".getBytes(), 1.0)
    );

    when(commandObjects.zrevrangeByScoreWithScores(key, max, min)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrevrangeByScoreWithScores(key, max, min);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrevrangeByScoreWithScores(key, max, min);
  }

  @Test
  public void testZrevrangeByScoreWithScoresWithLimit() {
    String key = "zsetKey";
    String max = "3";
    String min = "1";
    int offset = 1;
    int count = 2;
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member2", 2.0),
        new Tuple("member1", 1.0)
    );

    when(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrevrangeByScoreWithScores(key, max, min, offset, count);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrevrangeByScoreWithScores(key, max, min, offset, count);
  }

  @Test
  public void testZrevrangeByScoreWithScoresWithLimitBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] max = "3".getBytes();
    byte[] min = "1".getBytes();
    int offset = 1;
    int count = 2;
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member2".getBytes(), 2.0),
        new Tuple("member1".getBytes(), 1.0)
    );

    when(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrevrangeByScoreWithScores(key, max, min, offset, count);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrevrangeByScoreWithScores(key, max, min, offset, count);
  }

  @Test
  public void testZrevrangeByScoreWithScoresDoubleWithLimit() {
    String key = "zsetKey";
    double max = 3.0;
    double min = 1.0;
    int offset = 1;
    int count = 2;
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member2", 2.0),
        new Tuple("member1", 1.0)
    );

    when(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrevrangeByScoreWithScores(key, max, min, offset, count);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrevrangeByScoreWithScores(key, max, min, offset, count);
  }

  @Test
  public void testZrevrangeByScoreWithScoresDoubleWithLimitBinary() {
    byte[] key = "zsetKey".getBytes();
    double max = 3.0;
    double min = 1.0;
    int offset = 1;
    int count = 2;
    List<Tuple> expectedMembersWithScores = Arrays.asList(
        new Tuple("member2".getBytes(), 2.0),
        new Tuple("member1".getBytes(), 1.0)
    );

    when(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedMembersWithScores);

    List<Tuple> result = jedis.zrevrangeByScoreWithScores(key, max, min, offset, count);

    assertThat(result, equalTo(expectedMembersWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zrevrangeByScoreWithScores(key, max, min, offset, count);
  }

  @Test
  public void testZrevrank() {
    String key = "zsetKey";
    String member = "member1";
    Long expectedRevRank = 10L; // Assuming the member is the eleventh from the end in the sorted set

    when(commandObjects.zrevrank(key, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedRevRank);

    Long result = jedis.zrevrank(key, member);

    assertThat(result, equalTo(expectedRevRank));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zrevrank(key, member);
  }

  @Test
  public void testZrevrankBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] member = "member1".getBytes();
    Long expectedRevRank = 10L; // Assuming the member is the eleventh from the end in the sorted set

    when(commandObjects.zrevrank(key, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedRevRank);

    Long result = jedis.zrevrank(key, member);

    assertThat(result, equalTo(expectedRevRank));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zrevrank(key, member);
  }

  @Test
  public void testZrevrankWithScore() {
    String key = "zsetKey";
    String member = "member1";
    KeyValue<Long, Double> expectedRevRankWithScore = new KeyValue<>(10L, 1.0); // Assuming the member is the eleventh from the end with a score of 1.0

    when(commandObjects.zrevrankWithScore(key, member)).thenReturn(keyValueLongDoubleCommandObject);
    when(commandExecutor.executeCommand(keyValueLongDoubleCommandObject)).thenReturn(expectedRevRankWithScore);

    KeyValue<Long, Double> result = jedis.zrevrankWithScore(key, member);

    assertThat(result, equalTo(expectedRevRankWithScore));

    verify(commandExecutor).executeCommand(keyValueLongDoubleCommandObject);
    verify(commandObjects).zrevrankWithScore(key, member);
  }

  @Test
  public void testZrevrankWithScoreBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] member = "member1".getBytes();
    KeyValue<Long, Double> expectedRevRankWithScore = new KeyValue<>(10L, 1.0); // Assuming the member is the eleventh from the end with a score of 1.0

    when(commandObjects.zrevrankWithScore(key, member)).thenReturn(keyValueLongDoubleCommandObject);
    when(commandExecutor.executeCommand(keyValueLongDoubleCommandObject)).thenReturn(expectedRevRankWithScore);

    KeyValue<Long, Double> result = jedis.zrevrankWithScore(key, member);

    assertThat(result, equalTo(expectedRevRankWithScore));

    verify(commandExecutor).executeCommand(keyValueLongDoubleCommandObject);
    verify(commandObjects).zrevrankWithScore(key, member);
  }

  @Test
  public void testZscan() {
    String key = "zsetKey";
    String cursor = "0";
    ScanParams params = new ScanParams().match("*").count(10);
    List<Tuple> expectedTuples = Arrays.asList(
        new Tuple("member1", 1.0),
        new Tuple("member2", 2.0)
    );
    ScanResult<Tuple> expectedScanResult = new ScanResult<>(cursor, expectedTuples);

    when(commandObjects.zscan(key, cursor, params)).thenReturn(scanResultTupleCommandObject);
    when(commandExecutor.executeCommand(scanResultTupleCommandObject)).thenReturn(expectedScanResult);

    ScanResult<Tuple> result = jedis.zscan(key, cursor, params);

    assertThat(result, equalTo(expectedScanResult));

    verify(commandExecutor).executeCommand(scanResultTupleCommandObject);
    verify(commandObjects).zscan(key, cursor, params);
  }

  @Test
  public void testZscanBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] cursor = "0".getBytes();
    ScanParams params = new ScanParams().match("*").count(10);
    List<Tuple> expectedTuples = Arrays.asList(
        new Tuple("member1".getBytes(), 1.0),
        new Tuple("member2".getBytes(), 2.0)
    );
    ScanResult<Tuple> expectedScanResult = new ScanResult<>(cursor, expectedTuples);

    when(commandObjects.zscan(key, cursor, params)).thenReturn(scanResultTupleCommandObject);
    when(commandExecutor.executeCommand(scanResultTupleCommandObject)).thenReturn(expectedScanResult);

    ScanResult<Tuple> result = jedis.zscan(key, cursor, params);

    assertThat(result, equalTo(expectedScanResult));

    verify(commandExecutor).executeCommand(scanResultTupleCommandObject);
    verify(commandObjects).zscan(key, cursor, params);
  }

  @Test
  public void testZscore() {
    String key = "zsetKey";
    String member = "member1";
    Double expectedScore = 1.0; // Assuming the member has a score of 1.0

    when(commandObjects.zscore(key, member)).thenReturn(doubleCommandObject);
    when(commandExecutor.executeCommand(doubleCommandObject)).thenReturn(expectedScore);

    Double result = jedis.zscore(key, member);

    assertThat(result, equalTo(expectedScore));

    verify(commandExecutor).executeCommand(doubleCommandObject);
    verify(commandObjects).zscore(key, member);
  }

  @Test
  public void testZscoreBinary() {
    byte[] key = "zsetKey".getBytes();
    byte[] member = "member1".getBytes();
    Double expectedScore = 1.0; // Assuming the member has a score of 1.0

    when(commandObjects.zscore(key, member)).thenReturn(doubleCommandObject);
    when(commandExecutor.executeCommand(doubleCommandObject)).thenReturn(expectedScore);

    Double result = jedis.zscore(key, member);

    assertThat(result, equalTo(expectedScore));

    verify(commandExecutor).executeCommand(doubleCommandObject);
    verify(commandObjects).zscore(key, member);
  }

  @Test
  public void testZunion() {
    ZParams params = new ZParams().weights(1, 2).aggregate(ZParams.Aggregate.MIN);
    String[] keys = { "zset1", "zset2" };
    List<String> expectedUnion = Arrays.asList("member1", "member2");

    when(commandObjects.zunion(params, keys)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedUnion);

    List<String> result = jedis.zunion(params, keys);

    assertThat(result, equalTo(expectedUnion));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).zunion(params, keys);
  }

  @Test
  public void testZunionBinary() {
    ZParams params = new ZParams().weights(1, 2).aggregate(ZParams.Aggregate.MAX);
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };
    List<byte[]> expectedUnion = Arrays.asList("member1".getBytes(), "member2".getBytes());

    when(commandObjects.zunion(params, keys)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedUnion);

    List<byte[]> result = jedis.zunion(params, keys);

    assertThat(result, equalTo(expectedUnion));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).zunion(params, keys);
  }

  @Test
  public void testZunionWithScores() {
    ZParams params = new ZParams().weights(1, 2).aggregate(ZParams.Aggregate.MAX);
    String[] keys = { "zset1", "zset2" };
    List<Tuple> expectedUnionWithScores = Arrays.asList(new Tuple("member1", 1.0), new Tuple("member2", 2.0));

    when(commandObjects.zunionWithScores(params, keys)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedUnionWithScores);

    List<Tuple> result = jedis.zunionWithScores(params, keys);

    assertThat(result, equalTo(expectedUnionWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zunionWithScores(params, keys);
  }

  @Test
  public void testZunionWithScoresBinary() {
    ZParams params = new ZParams().weights(1, 2).aggregate(ZParams.Aggregate.MAX);
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };
    List<Tuple> expectedUnionWithScores = Arrays.asList(new Tuple("member1", 1.0), new Tuple("member2", 2.0));

    when(commandObjects.zunionWithScores(params, keys)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedUnionWithScores);

    List<Tuple> result = jedis.zunionWithScores(params, keys);

    assertThat(result, equalTo(expectedUnionWithScores));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).zunionWithScores(params, keys);
  }

  @Test
  public void testZunionstore() {
    String dstkey = "zsetUnion";
    String[] sets = { "zset1", "zset2" };
    long expectedStoredCount = 3L;

    when(commandObjects.zunionstore(dstkey, sets)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStoredCount);

    long result = jedis.zunionstore(dstkey, sets);

    assertThat(result, equalTo(expectedStoredCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zunionstore(dstkey, sets);
  }

  @Test
  public void testZunionstoreBinary() {
    byte[] dstkey = "zsetUnion".getBytes();
    byte[][] sets = { "zset1".getBytes(), "zset2".getBytes() };
    long expectedStoredCount = 3L;

    when(commandObjects.zunionstore(dstkey, sets)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStoredCount);

    long result = jedis.zunionstore(dstkey, sets);

    assertThat(result, equalTo(expectedStoredCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zunionstore(dstkey, sets);
  }

  @Test
  public void testZunionstoreWithParams() {
    String dstkey = "zsetUnion";
    ZParams params = new ZParams().weights(1, 2).aggregate(ZParams.Aggregate.MAX);
    String[] sets = { "zset1", "zset2" };
    long expectedStoredCount = 3L;

    when(commandObjects.zunionstore(dstkey, params, sets)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStoredCount);

    long result = jedis.zunionstore(dstkey, params, sets);

    assertThat(result, equalTo(expectedStoredCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zunionstore(dstkey, params, sets);
  }

  @Test
  public void testZunionstoreWithParamsBinary() {
    byte[] dstkey = "zsetUnion".getBytes();
    ZParams params = new ZParams().weights(1, 2).aggregate(ZParams.Aggregate.MAX);
    byte[][] sets = { "zset1".getBytes(), "zset2".getBytes() };
    long expectedStoredCount = 3L;

    when(commandObjects.zunionstore(dstkey, params, sets)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStoredCount);

    long result = jedis.zunionstore(dstkey, params, sets);

    assertThat(result, equalTo(expectedStoredCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).zunionstore(dstkey, params, sets);
  }

}
