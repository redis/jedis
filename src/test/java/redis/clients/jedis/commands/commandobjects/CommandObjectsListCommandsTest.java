package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.Test;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.ListDirection;
import redis.clients.jedis.args.ListPosition;
import redis.clients.jedis.params.LPosParams;
import redis.clients.jedis.util.KeyValue;

/**
 * Tests related to <a href="https://redis.io/commands/?group=list">List</a> commands.
 */
public class CommandObjectsListCommandsTest extends CommandObjectsStandaloneTestBase {

  public CommandObjectsListCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testPushCommands() {
    String key = "list";

    Long rpush = exec(commandObjects.rpush(key, "hello", "world"));
    assertThat(rpush, equalTo(2L));

    Long lpush = exec(commandObjects.lpush(key, "hello", "world"));
    assertThat(lpush, equalTo(4L));

    List<String> lrange = exec(commandObjects.lrange(key, 0, -1));
    assertThat(lrange, contains("world", "hello", "hello", "world"));
  }

  @Test
  public void testPushCommandsBinary() {
    String keyStr = "list";
    byte[] key = keyStr.getBytes();

    Long rpush = exec(commandObjects.rpush(key, "hello".getBytes(), "world".getBytes()));
    assertThat(rpush, equalTo(2L));

    Long lpush = exec(commandObjects.lpush(key, "hello".getBytes(), "world".getBytes()));
    assertThat(lpush, equalTo(4L));

    List<String> lrange = exec(commandObjects.lrange(keyStr, 0, -1));
    assertThat(lrange, contains("world", "hello", "hello", "world"));
  }

  @Test
  public void testLlen() {
    String key = "list";

    Long initialLength = exec(commandObjects.llen(key));
    assertThat(initialLength, equalTo(0L));

    exec(commandObjects.rpush(key, "value", "value"));

    Long llen = exec(commandObjects.llen(key));
    assertThat(llen, equalTo(2L));

    Long llenBinary = exec(commandObjects.llen(key.getBytes()));
    assertThat(llenBinary, equalTo(2L));
  }

  @Test
  public void testLrange() {
    String key = "list";
    String value1 = "first";
    String value2 = "second";
    String value3 = "third";

    exec(commandObjects.rpush(key, value1, value2, value3));

    List<String> lrange = exec(commandObjects.lrange(key, 0, -1));
    assertThat(lrange, contains(value1, value2, value3));

    List<byte[]> lrangeBinary = exec(commandObjects.lrange(key.getBytes(), 0, -1));
    assertThat(lrangeBinary, contains(value1.getBytes(), value2.getBytes(), value3.getBytes()));

    List<String> partialRange = exec(commandObjects.lrange(key, 1, 2));
    assertThat(partialRange, contains(value2, value3));

    List<String> emptyRange = exec(commandObjects.lrange(key, 4, 5));
    assertThat(emptyRange, empty());
  }

  @Test
  public void testLtrim() {
    String key = "list";

    exec(commandObjects.rpush(key, "one", "two", "three", "four"));

    String trim = exec(commandObjects.ltrim(key, 1, 2));
    assertThat(trim, equalTo("OK"));

    List<String> lrange = exec(commandObjects.lrange(key, 0, -1));
    assertThat(lrange, contains("two", "three"));
  }

  @Test
  public void testLtrimBinary() {
    byte[] key = "list".getBytes();

    exec(commandObjects.rpush(key, "one".getBytes(), "two".getBytes(), "three".getBytes(), "four".getBytes()));

    String trim = exec(commandObjects.ltrim(key, 1, 2));
    assertThat(trim, equalTo("OK"));

    List<byte[]> lrange = exec(commandObjects.lrange(key, 0, -1));
    assertThat(lrange, contains("two".getBytes(), "three".getBytes()));
  }

  @Test
  public void testLindexCommands() {
    String key = "list";

    exec(commandObjects.rpush(key, "alpha", "beta", "gamma"));

    String lindex = exec(commandObjects.lindex(key, 1));
    assertThat(lindex, equalTo("beta"));

    byte[] lindexBinary = exec(commandObjects.lindex(key.getBytes(), 2));
    assertThat(lindexBinary, equalTo("gamma".getBytes()));

    String lindexOufOfRange = exec(commandObjects.lindex(key, 5));
    assertThat(lindexOufOfRange, nullValue());

    byte[] lindexLastPositionBinary = exec(commandObjects.lindex(key.getBytes(), -1));
    assertThat(lindexLastPositionBinary, equalTo("gamma".getBytes()));
  }

  @Test
  public void testLset() {
    String key = "list";
    String initialValue = "initial";
    String updatedValue = "updated";

    exec(commandObjects.rpush(key, initialValue));

    String lindexBefore = exec(commandObjects.lindex(key, 0));
    assertThat(lindexBefore, equalTo(initialValue));

    String lset = exec(commandObjects.lset(key, 0, updatedValue));
    assertThat(lset, equalTo("OK"));

    String lindexAfter = exec(commandObjects.lindex(key, 0));
    assertThat(lindexAfter, equalTo(updatedValue));
  }

  @Test
  public void testLsetBinary() {
    byte[] keyBytes = "list".getBytes();
    String initialValue = "initial";
    String updatedValue = "updated";

    exec(commandObjects.rpush(keyBytes, initialValue.getBytes()));

    byte[] lindexBefore = exec(commandObjects.lindex(keyBytes, 0));
    assertThat(lindexBefore, equalTo(initialValue.getBytes()));

    String lset = exec(commandObjects.lset(keyBytes, 0, updatedValue.getBytes()));
    assertThat(lset, equalTo("OK"));

    byte[] lindexAfter = exec(commandObjects.lindex(keyBytes, 0));
    assertThat(lindexAfter, equalTo(updatedValue.getBytes()));
  }

  @Test
  public void testLrem() {
    String key = "remList";

    exec(commandObjects.rpush(key, "duplicate", "duplicate", "unique"));

    List<String> lrangeInitial = exec(commandObjects.lrange(key, 0, -1));
    assertThat(lrangeInitial, contains("duplicate", "duplicate", "unique"));

    Long lrem = exec(commandObjects.lrem(key, 1, "duplicate"));
    assertThat(lrem, equalTo(1L));

    List<String> lrangeAfterLremSingle = exec(commandObjects.lrange(key, 0, -1));
    assertThat(lrangeAfterLremSingle, contains("duplicate", "unique"));

    Long lremNonExistent = exec(commandObjects.lrem(key, 0, "nonexistent"));
    assertThat(lremNonExistent, equalTo(0L));

    List<String> lrangeAfterLremNonExistent = exec(commandObjects.lrange(key, 0, -1));
    assertThat(lrangeAfterLremNonExistent, contains("duplicate", "unique"));
  }

  @Test
  public void testLremBinary() {
    byte[] keyBytes = "remList".getBytes();

    exec(commandObjects.rpush(keyBytes, "duplicate".getBytes(), "duplicate".getBytes(), "unique".getBytes()));

    List<byte[]> lrangeBefore = exec(commandObjects.lrange(keyBytes, 0, -1));
    assertThat(lrangeBefore, contains("duplicate".getBytes(), "duplicate".getBytes(), "unique".getBytes()));

    Long lremMultiple = exec(commandObjects.lrem(keyBytes, 0, "duplicate".getBytes()));
    assertThat(lremMultiple, equalTo(2L));

    List<byte[]> lrangeAfter = exec(commandObjects.lrange(keyBytes, 0, -1));
    assertThat(lrangeAfter, contains("unique".getBytes()));
  }

  @Test
  public void testPopCommands() {
    String key = "popList";

    exec(commandObjects.rpush(key,
        "first", "second", "third", "first", "second", "third"));

    String lpop = exec(commandObjects.lpop(key));
    assertThat(lpop, equalTo("first"));

    String rpop = exec(commandObjects.rpop(key));
    assertThat(rpop, equalTo("third"));

    List<String> lpopMultiple = exec(commandObjects.lpop(key, 2));
    assertThat(lpopMultiple, contains("second", "third"));

    List<String> rpopMultiple = exec(commandObjects.rpop(key, 2));
    assertThat(rpopMultiple, contains("second", "first"));
  }

  @Test
  public void testPopCommandsBinary() {
    byte[] key = "popList".getBytes();

    exec(commandObjects.rpush(key,
        "first".getBytes(), "second".getBytes(), "third".getBytes(),
        "first".getBytes(), "second".getBytes(), "third".getBytes()));

    byte[] lpop = exec(commandObjects.lpop(key));
    assertThat(lpop, equalTo("first".getBytes()));

    byte[] rpop = exec(commandObjects.rpop(key));
    assertThat(rpop, equalTo("third".getBytes()));

    List<byte[]> lpopMultiple = exec(commandObjects.lpop(key, 2));
    assertThat(lpopMultiple, contains("second".getBytes(), "third".getBytes()));

    List<byte[]> rpopMultiple = exec(commandObjects.rpop(key, 2));
    assertThat(rpopMultiple, contains("second".getBytes(), "first".getBytes()));
  }

  @Test
  public void testLpos() {
    String key = "list";
    String value = "target";
    String nonExistentValue = "ghost";

    exec(commandObjects.rpush(key, "start", value, "middle", value, "end"));

    Long lposFirst = exec(commandObjects.lpos(key, value));
    assertThat(lposFirst, equalTo(1L));

    Long lposFirstBinary = exec(commandObjects.lpos(key.getBytes(), value.getBytes()));
    assertThat(lposFirstBinary, equalTo(1L));

    LPosParams params = LPosParams.lPosParams().rank(-1);
    Long lposLast = exec(commandObjects.lpos(key, value, params));
    assertThat(lposLast, equalTo(3L));

    Long lposLastBinary = exec(commandObjects.lpos(key.getBytes(), value.getBytes(), params));
    assertThat(lposLastBinary, equalTo(3L));

    List<Long> lposMultiple = exec(commandObjects.lpos(key, value, params, 2));
    assertThat(lposMultiple, contains(3L, 1L));

    List<Long> lposMultipleBinary = exec(commandObjects.lpos(key.getBytes(), value.getBytes(), params, 2));
    assertThat(lposMultipleBinary, contains(3L, 1L));

    Long lposNonExistent = exec(commandObjects.lpos(key, nonExistentValue));
    assertThat(lposNonExistent, nullValue());

    Long lposNonExistentBinary = exec(commandObjects.lpos(key.getBytes(), nonExistentValue.getBytes()));
    assertThat(lposNonExistentBinary, nullValue());
  }

  @Test
  public void testLinsert() {
    String key = "insertList";
    String pivot = "pivot";
    String valueBefore = "beforePivot";
    String valueAfter = "afterPivot";

    exec(commandObjects.rpush(key, pivot));

    Long linsertBefore = exec(commandObjects.linsert(key, ListPosition.BEFORE, pivot, valueBefore));
    assertThat(linsertBefore, equalTo(2L));

    Long linsertAfter = exec(commandObjects.linsert(key, ListPosition.AFTER, pivot, valueAfter));
    assertThat(linsertAfter, equalTo(3L));

    List<String> lrange = exec(commandObjects.lrange(key, 0, -1));
    assertThat(lrange, contains(valueBefore, pivot, valueAfter));
  }

  @Test
  public void testLinsertBinary() {
    byte[] key = "insertList".getBytes();
    byte[] pivot = "pivot".getBytes();
    byte[] valueBefore = "valueBefore".getBytes();
    byte[] valueAfter = "valueAfter".getBytes();

    exec(commandObjects.rpush(key, pivot));

    Long linsertBefore = exec(commandObjects.linsert(key, ListPosition.BEFORE, pivot, valueBefore));
    assertThat(linsertBefore, equalTo(2L));

    Long linsertAfter = exec(commandObjects.linsert(key, ListPosition.AFTER, pivot, valueAfter));
    assertThat(linsertAfter, equalTo(3L));

    List<byte[]> lrange = exec(commandObjects.lrange(key, 0, -1));
    assertThat(lrange, contains(valueBefore, pivot, valueAfter));
  }

  @Test
  public void testPushxCommands() {
    String key = "pushxList";
    String value1 = "first";
    String value2 = "second";

    Long lpushxInitial = exec(commandObjects.lpushx(key, value1));
    assertThat(lpushxInitial, equalTo(0L));

    Long rpushxInitial = exec(commandObjects.rpushx(key, value1));
    assertThat(rpushxInitial, equalTo(0L));

    Boolean exists = exec(commandObjects.exists(key));
    assertThat(exists, equalTo(false));

    exec(commandObjects.lpush(key, "init"));

    Long lpushx = exec(commandObjects.lpushx(key, value1, value2));
    assertThat(lpushx, equalTo(3L)); // new size returned

    Long rpushx = exec(commandObjects.rpushx(key, value1, value2));
    assertThat(rpushx, equalTo(5L));

    List<String> lrange = exec(commandObjects.lrange(key, 0, -1));
    assertThat(lrange, contains(value2, value1, "init", value1, value2));
  }

  @Test
  public void testPushxCommandsBinary() {
    byte[] key = "pushxList".getBytes();
    byte[] value1 = "first".getBytes();
    byte[] value2 = "second".getBytes();

    Long lpushxInitial = exec(commandObjects.lpushx(key, value1));
    assertThat(lpushxInitial, equalTo(0L));

    Long rpushxInitial = exec(commandObjects.rpushx(key, value1));
    assertThat(rpushxInitial, equalTo(0L));

    Boolean exists = exec(commandObjects.exists(key));
    assertThat(exists, equalTo(false));

    exec(commandObjects.lpush(key, "init".getBytes()));

    Long lpushx = exec(commandObjects.lpushx(key, value1, value2));
    assertThat(lpushx, equalTo(3L));

    Long rpushx = exec(commandObjects.rpushx(key, value1, value2));
    assertThat(rpushx, equalTo(5L));

    List<byte[]> lrange = exec(commandObjects.lrange(key, 0, -1));
    assertThat(lrange, contains(value2, value1, "init".getBytes(), value1, value2));
  }

  @Test
  public void testBlpop() {
    String key1 = "list1";
    String key2 = "list2";
    String value1 = "value1";
    String value2 = "value2";

    exec(commandObjects.lpush(key1, value1));

    List<String> blpop = exec(commandObjects.blpop(1, key1));
    assertThat(blpop, contains(key1, value1));

    exec(commandObjects.lpush(key1, value1));
    exec(commandObjects.lpush(key2, value2));

    List<String> blpopMultiple = exec(commandObjects.blpop(1, key1, key2));
    assertThat(blpopMultiple, anyOf(contains(key1, value1), contains(key2, value2)));

    exec(commandObjects.lpush(key1, value1));

    KeyValue<String, String> blpopDoubleTimeout = exec(commandObjects.blpop(1.0, key1));
    assertThat(blpopDoubleTimeout.getKey(), equalTo(key1));
    assertThat(blpopDoubleTimeout.getValue(), equalTo(value1));

    exec(commandObjects.lpush(key1, value1));
    exec(commandObjects.lpush(key2, value2));

    KeyValue<String, String> blpopDoubleTimeoutMultiple = exec(commandObjects.blpop(1.0, key1, key2));
    assertThat(blpopDoubleTimeoutMultiple.getKey(), anyOf(equalTo(key1), equalTo(key2)));
    assertThat(blpopDoubleTimeoutMultiple.getValue(), anyOf(equalTo(value1), equalTo(value2)));
  }

  @Test
  public void testBlpopBinary() {
    byte[] key1 = "list1".getBytes();
    byte[] key2 = "list2".getBytes();
    byte[] value1 = "value1".getBytes();
    byte[] value2 = "value2".getBytes();

    exec(commandObjects.lpush(key1, value1));

    List<byte[]> blpop = exec(commandObjects.blpop(1, key1));
    assertThat(blpop.get(0), equalTo(key1));
    assertThat(blpop.get(1), equalTo(value1));

    exec(commandObjects.lpush(key1, value1));
    exec(commandObjects.lpush(key2, value2));

    List<byte[]> blpopMultiple = exec(commandObjects.blpop(1, key1, key2));
    assertThat(blpopMultiple, anyOf(contains(key1, value1), contains(key2, value2)));

    exec(commandObjects.lpush(key1, value1));

    KeyValue<byte[], byte[]> blpopDoubleTimeout = exec(commandObjects.blpop(1.0, key1));
    assertThat(blpopDoubleTimeout.getKey(), equalTo(key1));
    assertThat(blpopDoubleTimeout.getValue(), equalTo(value1));

    exec(commandObjects.lpush(key1, value1));
    exec(commandObjects.lpush(key2, value2));

    KeyValue<byte[], byte[]> blpopDoubleTimeoutMultiple = exec(commandObjects.blpop(1.0, key1, key2));
    assertThat(blpopDoubleTimeoutMultiple.getKey(), anyOf(equalTo(key1), equalTo(key2)));
    assertThat(blpopDoubleTimeoutMultiple.getValue(), anyOf(equalTo(value1), equalTo(value2)));
  }

  @Test
  public void testBrpop() {
    String key1 = "list1";
    String key2 = "list2";
    String value1 = "value1";
    String value2 = "value2";

    exec(commandObjects.lpush(key1, value1));

    List<String> brpop = exec(commandObjects.brpop(1, key1));
    assertThat(brpop, contains(key1, value1));

    exec(commandObjects.lpush(key1, value1));
    exec(commandObjects.lpush(key2, value2));

    List<String> brpopMultiple = exec(commandObjects.brpop(1, key1, key2));
    assertThat(brpopMultiple, anyOf(contains(key1, value1), contains(key2, value2)));

    exec(commandObjects.lpush(key1, value1));

    KeyValue<String, String> brpopDoubleTimeout = exec(commandObjects.brpop(1.0, key1));
    assertThat(brpopDoubleTimeout.getKey(), equalTo(key1));
    assertThat(brpopDoubleTimeout.getValue(), equalTo(value1));

    exec(commandObjects.lpush(key1, value1));
    exec(commandObjects.lpush(key2, value2));

    KeyValue<String, String> brpopDoubleTimeoutMultiple = exec(commandObjects.brpop(1.0, key1, key2));
    assertThat(brpopDoubleTimeoutMultiple.getKey(), anyOf(equalTo(key1), equalTo(key2)));
    assertThat(brpopDoubleTimeoutMultiple.getValue(), anyOf(equalTo(value1), equalTo(value2)));
  }

  @Test
  public void testBrpopBinary() {
    byte[] key1 = "list1".getBytes();
    byte[] key2 = "list2".getBytes();
    byte[] value1 = "value1".getBytes();
    byte[] value2 = "value2".getBytes();

    exec(commandObjects.lpush(key1, value1));

    List<byte[]> brpop = exec(commandObjects.brpop(1, key1));
    assertThat(brpop.get(0), equalTo(key1));
    assertThat(brpop.get(1), equalTo(value1));

    exec(commandObjects.lpush(key1, value1));
    exec(commandObjects.lpush(key2, value2));

    List<byte[]> brpopMultiple = exec(commandObjects.brpop(1, key1, key2));
    assertThat(brpopMultiple, anyOf(contains(key1, value1), contains(key2, value2)));

    exec(commandObjects.lpush(key1, value1));

    KeyValue<byte[], byte[]> brpopDoubleTimeout = exec(commandObjects.brpop(1.0, key1));
    assertThat(brpopDoubleTimeout.getKey(), equalTo(key1));
    assertThat(brpopDoubleTimeout.getValue(), equalTo(value1));

    exec(commandObjects.lpush(key1, value1));
    exec(commandObjects.lpush(key2, value2));

    KeyValue<byte[], byte[]> brpopDoubleTimeoutMultiple = exec(commandObjects.brpop(1.0, key1, key2));
    assertThat(brpopDoubleTimeoutMultiple.getKey(), anyOf(equalTo(key1), equalTo(key2)));
    assertThat(brpopDoubleTimeoutMultiple.getValue(), anyOf(equalTo(value1), equalTo(value2)));
  }

  @Test
  public void testRpoplpushAndBrpoplpush() {
    String srcKey = "sourceList";
    String dstKey = "destinationList";
    String value1 = "value1";
    String value2 = "value2";

    String noResult = exec(commandObjects.rpoplpush(srcKey, dstKey));
    assertThat(noResult, nullValue());

    exec(commandObjects.lpush(srcKey, value1));

    String result = exec(commandObjects.rpoplpush(srcKey, dstKey));
    assertThat(result, equalTo(value1));

    List<String> dstList = exec(commandObjects.lrange(dstKey, 0, -1));
    assertThat(dstList, contains(value1));

    exec(commandObjects.lpush(srcKey, value2));

    String bResult = exec(commandObjects.brpoplpush(srcKey, dstKey, 1));
    assertThat(bResult, equalTo(value2));

    dstList = exec(commandObjects.lrange(dstKey, 0, -1));
    assertThat(dstList, contains(value2, value1));
  }

  @Test
  public void testRpoplpushAndBrpoplpushBinary() {
    byte[] srcKey = "sourceList".getBytes();
    byte[] dstKey = "destinationList".getBytes();
    byte[] value1 = "value1".getBytes();
    byte[] value2 = "value2".getBytes();

    exec(commandObjects.lpush(srcKey, value1));

    byte[] result = exec(commandObjects.rpoplpush(srcKey, dstKey));
    assertThat(result, equalTo(value1));

    List<byte[]> dstList = exec(commandObjects.lrange(dstKey, 0, -1));
    assertThat(dstList, contains(equalTo(value1)));

    exec(commandObjects.lpush(srcKey, value2));

    byte[] bResult = exec(commandObjects.brpoplpush(srcKey, dstKey, 1));
    assertThat(bResult, equalTo(value2));

    dstList = exec(commandObjects.lrange(dstKey, 0, -1));
    assertThat(dstList, contains(equalTo(value2), equalTo(value1)));
  }

  @Test
  public void testLmoveAndBlmove() {
    String srcKey = "sourceList";
    String dstKey = "destinationList";
    String value1 = "value1";
    String value2 = "value2";

    exec(commandObjects.lpush(srcKey, value1));

    String result = exec(commandObjects.lmove(srcKey, dstKey, ListDirection.LEFT, ListDirection.RIGHT));
    assertThat(result, equalTo(value1));

    List<String> dstList = exec(commandObjects.lrange(dstKey, 0, -1));
    assertThat(dstList, contains(value1));

    exec(commandObjects.lpush(srcKey, value2));

    String bResult = exec(commandObjects.blmove(srcKey, dstKey, ListDirection.LEFT, ListDirection.LEFT, 1.0));
    assertThat(bResult, equalTo(value2));

    dstList = exec(commandObjects.lrange(dstKey, 0, -1));
    assertThat(dstList, contains(value2, value1));
  }

  @Test
  public void testLmoveAndBlmoveBinary() {
    byte[] srcKey = "sourceList".getBytes();
    byte[] dstKey = "destinationList".getBytes();
    byte[] value1 = "value1".getBytes();
    byte[] value2 = "value2".getBytes();

    exec(commandObjects.lpush(srcKey, value1));

    byte[] result = exec(commandObjects.lmove(srcKey, dstKey, ListDirection.LEFT, ListDirection.RIGHT));
    assertThat(result, equalTo(value1));

    List<byte[]> dstList = exec(commandObjects.lrange(dstKey, 0, -1));
    assertThat(dstList.get(0), equalTo(value1));

    exec(commandObjects.lpush(srcKey, value2));

    byte[] bResult = exec(commandObjects.blmove(srcKey, dstKey, ListDirection.LEFT, ListDirection.LEFT, 1.0));
    assertThat(bResult, equalTo(value2));

    dstList = exec(commandObjects.lrange(dstKey, 0, -1));
    assertThat(dstList, contains(equalTo(value2), equalTo(value1)));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testLmpopAndBlmpop() {
    String key1 = "list1";
    String key2 = "list2";
    String value1 = "value1";
    String value2 = "value2";

    exec(commandObjects.lpush(key1, value1, value1, value1, value1, value1, value1));
    exec(commandObjects.lpush(key2, value2, value2, value2, value2, value2, value2));

    KeyValue<String, List<String>> lmpop = exec(commandObjects.lmpop(ListDirection.LEFT, key1, key2));
    assertThat(lmpop.getKey(), anyOf(equalTo(key1), equalTo(key2)));
    assertThat(lmpop.getValue(), anyOf(contains(value1), contains(value2)));

    KeyValue<String, List<String>> lmpopMultiple = exec(commandObjects.lmpop(ListDirection.LEFT, 2, key1, key2));
    assertThat(lmpopMultiple.getKey(), anyOf(equalTo(key1), equalTo(key2)));
    assertThat(lmpopMultiple.getValue(), anyOf(contains(value1, value1), contains(value2, value2)));

    KeyValue<String, List<String>> blmpop = exec(commandObjects.blmpop(1.0, ListDirection.LEFT, key1, key2));
    assertThat(blmpop.getKey(), anyOf(equalTo(key1), equalTo(key2)));
    assertThat(blmpop.getValue(), anyOf(contains(value1), contains(value2)));

    KeyValue<String, List<String>> blmpopMultiple = exec(commandObjects.blmpop(1.0, ListDirection.LEFT, 2, key1, key2));
    assertThat(blmpopMultiple.getKey(), anyOf(equalTo(key1), equalTo(key2)));
    assertThat(blmpopMultiple.getValue(), anyOf(contains(value1, value1), contains(value2, value2)));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testLmpopAndBlmpopBinary() {
    byte[] key1 = "list1".getBytes();
    byte[] key2 = "list2".getBytes();
    byte[] value1 = "value1".getBytes();
    byte[] value2 = "value2".getBytes();

    exec(commandObjects.lpush(key1, value1, value1, value1, value1, value1, value1));
    exec(commandObjects.lpush(key2, value2, value2, value2, value2, value2, value2));

    KeyValue<byte[], List<byte[]>> lmpop = exec(commandObjects.lmpop(ListDirection.LEFT, key1, key2));
    assertThat(lmpop.getKey(), anyOf(equalTo(key1), equalTo(key2)));
    assertThat(lmpop.getValue(), anyOf(contains(equalTo(value1)), contains(equalTo(value2))));

    KeyValue<byte[], List<byte[]>> lmpopMultiple = exec(commandObjects.lmpop(ListDirection.LEFT, 2, key1, key2));
    assertThat(lmpopMultiple.getKey(), anyOf(equalTo(key1), equalTo(key2)));
    assertThat(lmpopMultiple.getValue(), anyOf(contains(equalTo(value1), equalTo(value1)), contains(equalTo(value2), equalTo(value2))));

    KeyValue<byte[], List<byte[]>> blmpop = exec(commandObjects.blmpop(1.0, ListDirection.LEFT, key1, key2));
    assertThat(blmpop.getKey(), anyOf(equalTo(key1), equalTo(key2)));
    assertThat(blmpop.getValue(), anyOf(contains(equalTo(value1)), contains(equalTo(value2))));

    KeyValue<byte[], List<byte[]>> blmpopMultiple = exec(commandObjects.blmpop(1.0, ListDirection.LEFT, 2, key1, key2));
    assertThat(blmpopMultiple.getKey(), anyOf(equalTo(key1), equalTo(key2)));
    assertThat(blmpopMultiple.getValue(), anyOf(contains(equalTo(value1), equalTo(value1)), contains(equalTo(value2), equalTo(value2))));
  }
}
