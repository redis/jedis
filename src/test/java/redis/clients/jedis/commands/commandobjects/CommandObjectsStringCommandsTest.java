package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.Test;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.params.GetExParams;
import redis.clients.jedis.params.LCSParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.resps.LCSMatchResult;

/**
 * Tests related to <a href="https://redis.io/commands/?group=string">String</a> commands.
 */
public class CommandObjectsStringCommandsTest extends CommandObjectsStandaloneTestBase {

  public CommandObjectsStringCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testAppend() {
    String key = "testKey";
    String value = "testValue";

    String initialGet = exec(commandObjects.get(key));
    assertThat(initialGet, nullValue());

    Long append = exec(commandObjects.append(key, value));
    assertThat(append, equalTo((long) value.length()));

    String getAfterAppend = exec(commandObjects.get(key));
    assertThat(getAfterAppend, equalTo(value));

    Long secondAppend = exec(commandObjects.append(key, value));
    assertThat(secondAppend, equalTo((long) value.length() * 2));

    String getAfterSecondAppend = exec(commandObjects.get(key));
    assertThat(getAfterSecondAppend, equalTo(value + value));
  }

  @Test
  public void testAppendBinary() {
    byte[] key = "testKeyBytes".getBytes();
    byte[] value = "testValueBytes".getBytes();

    byte[] initialGet = exec(commandObjects.get(key));
    assertThat(initialGet, nullValue());

    Long append = exec(commandObjects.append(key, value));
    assertThat(append, equalTo((long) value.length));

    byte[] getAfterAppend = exec(commandObjects.get(key));
    assertThat(getAfterAppend, equalTo(value));

    Long secondAppend = exec(commandObjects.append(key, value));
    assertThat(secondAppend, equalTo((long) value.length * 2));

    byte[] getAfterSecondAppend = exec(commandObjects.get(key));

    byte[] expected = new byte[value.length + value.length];
    System.arraycopy(value, 0, expected, 0, value.length);
    System.arraycopy(value, 0, expected, value.length, value.length);

    assertThat(getAfterSecondAppend, equalTo(expected));
  }

  @Test
  public void testDecrementOperations() {
    String key = "testDecr";

    exec(commandObjects.set(key, String.valueOf(10L)));

    String initialGet = exec(commandObjects.get(key));
    assertThat(initialGet, equalTo("10"));

    Long decr = exec(commandObjects.decr(key));
    assertThat(decr, equalTo(9L));

    String getAfterDecr = exec(commandObjects.get(key));
    assertThat(getAfterDecr, equalTo("9"));

    Long decrBy = exec(commandObjects.decrBy(key, 2L));
    assertThat(decrBy, equalTo(7L));

    String getAfterDecrBy = exec(commandObjects.get(key));
    assertThat(getAfterDecrBy, equalTo("7"));
  }

  @Test
  public void testDecrementOperationsBinary() {
    byte[] key = "testDecrBytes".getBytes();

    exec(commandObjects.set(key, String.valueOf(10L).getBytes()));

    byte[] initialGet = exec(commandObjects.get(key));
    assertThat(initialGet, equalTo("10".getBytes()));

    Long decr = exec(commandObjects.decr(key));
    assertThat(decr, equalTo(9L));

    byte[] getAfterDecr = exec(commandObjects.get(key));
    assertThat(getAfterDecr, equalTo("9".getBytes()));

    Long decrBy = exec(commandObjects.decrBy(key, 2L));
    assertThat(decrBy, equalTo(7L));

    byte[] getAfterDecrBy = exec(commandObjects.get(key));
    assertThat(getAfterDecrBy, equalTo("7".getBytes()));
  }

  @Test
  public void testGetOperations() {
    String key = "testGet";
    String value = "value";

    exec(commandObjects.set(key, value));

    String initialGet = exec(commandObjects.get(key));
    assertThat(initialGet, equalTo(value));

    String getDel = exec(commandObjects.getDel(key));
    assertThat(getDel, equalTo(value));

    String getAfterGetDel = exec(commandObjects.get(key));
    assertThat(getAfterGetDel, nullValue());

    // set again
    exec(commandObjects.set(key, value));

    Long initialTtl = exec(commandObjects.ttl(key));
    assertThat(initialTtl, equalTo(-1L));

    GetExParams getExParams = GetExParams.getExParams().ex(10);
    String getEx = exec(commandObjects.getEx(key, getExParams));
    assertThat(getEx, equalTo(value));

    Long ttlAfterGetEx = exec(commandObjects.ttl(key));
    assertThat(ttlAfterGetEx, greaterThan(0L));
  }

  @Test
  public void testGetOperationsBinary() {
    byte[] key = "testGetBytes".getBytes();
    byte[] value = "value".getBytes();

    exec(commandObjects.set(key, value));

    byte[] initialGet = exec(commandObjects.get(key));
    assertThat(initialGet, equalTo(value));

    byte[] getDel = exec(commandObjects.getDel(key));
    assertThat(getDel, equalTo(value));

    byte[] getAfterGetDel = exec(commandObjects.get(key));
    assertThat(getAfterGetDel, nullValue());

    // set again
    exec(commandObjects.set(key, value));

    Long initialTtl = exec(commandObjects.ttl(key));
    assertThat(initialTtl, equalTo(-1L));

    GetExParams getExParams = GetExParams.getExParams().ex(10);
    byte[] getEx = exec(commandObjects.getEx(key, getExParams));
    assertThat(getEx, equalTo(value));

    Long ttlAfterGetEx = exec(commandObjects.ttl(key));
    assertThat(ttlAfterGetEx, greaterThan(0L));
  }

  @Test
  @Deprecated
  public void testGetSet() {
    String key = "testGetSet";
    String initialValue = "initialValue";
    String newValue = "newValue";

    exec(commandObjects.set(key, initialValue));

    String initialGet = exec(commandObjects.get(key));
    assertThat(initialGet, equalTo(initialValue));

    String getSet = exec(commandObjects.getSet(key, newValue));
    assertThat(getSet, equalTo(initialValue));

    String getAfterGetSet = exec(commandObjects.get(key));
    assertThat(getAfterGetSet, equalTo(newValue));
  }

  @Test
  @Deprecated
  public void testGetSetBinary() {
    byte[] key = "testGetSetBytes".getBytes();
    byte[] initialValue = "initialValue".getBytes();
    byte[] newValue = "newValue".getBytes();

    exec(commandObjects.set(key, initialValue));

    byte[] initialGet = exec(commandObjects.get(key));
    assertThat(initialGet, equalTo(initialValue));

    byte[] getSet = exec(commandObjects.getSet(key, newValue));
    assertThat(getSet, equalTo(initialValue));

    byte[] getAfterGetSet = exec(commandObjects.get(key));
    assertThat(getAfterGetSet, equalTo(newValue));
  }

  @Test
  public void testSetRangeAndGetRange() {
    String key = "testRange";
    String initial = "Hello World";
    String replacement = "Everyone";
    long replacementOffset = 6L;

    exec(commandObjects.set(key, initial));

    Long setRange = exec(commandObjects.setrange(key, replacementOffset, replacement));
    assertThat(setRange, equalTo(14L)); // Length after replacement

    String getRange = exec(commandObjects.getrange(key, 0, -1));
    assertThat(getRange, equalTo("Hello Everyone"));
  }

  @Test
  public void testSetRangeAndGetRangeBinary() {
    byte[] key = "testRangeBytes".getBytes();
    byte[] initialValue = "Hello World".getBytes();
    byte[] replacement = "Everyone".getBytes();
    long replacementOffset = 6L;

    exec(commandObjects.set(key, initialValue));

    Long setRange = exec(commandObjects.setrange(key, replacementOffset, replacement));
    assertThat(setRange, equalTo(14L)); // Length after replacement

    byte[] getRange = exec(commandObjects.getrange(key, 0, -1));
    assertThat(getRange, equalTo("Hello Everyone".getBytes()));
  }

  @Test
  public void testIncrementOperations() {
    String key = "testIncr";

    exec(commandObjects.set(key, "0"));

    Long incr = exec(commandObjects.incr(key));
    assertThat(incr, equalTo(1L));

    String getAfterIncr = exec(commandObjects.get(key));
    assertThat(getAfterIncr, equalTo("1"));

    Long incrBy = exec(commandObjects.incrBy(key, 5L));
    assertThat(incrBy, equalTo(6L));

    String getAfterIncrBy = exec(commandObjects.get(key));
    assertThat(getAfterIncrBy, equalTo("6"));

    Double incrByFloat = exec(commandObjects.incrByFloat(key, 2.5));
    assertThat(incrByFloat, closeTo(8.5, 0.001));

    String getAfterIncrByFloat = exec(commandObjects.get(key));
    assertThat(getAfterIncrByFloat, equalTo("8.5"));
  }

  @Test
  public void testIncrementOperationsBinary() {
    byte[] key = "testIncrBytes".getBytes();

    exec(commandObjects.set(key, "0".getBytes()));

    Long incr = exec(commandObjects.incr(key));
    assertThat(incr, equalTo(1L));

    byte[] getAfterIncr = exec(commandObjects.get(key));
    assertThat(getAfterIncr, equalTo("1".getBytes()));

    Long incrBy = exec(commandObjects.incrBy(key, 5L));
    assertThat(incrBy, equalTo(6L));

    byte[] getAfterIncrBy = exec(commandObjects.get(key));
    assertThat(getAfterIncrBy, equalTo("6".getBytes()));

    Double incrByFloat = exec(commandObjects.incrByFloat(key, 2.5));
    assertThat(incrByFloat, closeTo(8.5, 0.001));

    byte[] getAfterIncrByFloat = exec(commandObjects.get(key));
    assertThat(getAfterIncrByFloat, equalTo("8.5".getBytes()));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testLcs() {
    String keyA = "keyA";
    String keyB = "keyB";

    // "abcdfg" is the common substring
    String valueA = "abcdfgh";
    String valueB = "abcdefg";

    exec(commandObjects.set(keyA, valueA));
    exec(commandObjects.set(keyB, valueB));

    LCSMatchResult lcsLen = exec(commandObjects.lcs(keyA, keyB, new LCSParams().len()));
    assertThat(lcsLen.getLen(), equalTo(6L));
    assertThat(lcsLen.getMatchString(), nullValue());

    LCSMatchResult lcs = exec(commandObjects.lcs(keyA, keyB, new LCSParams()));
    assertThat(lcs.getLen(), equalTo(0L));
    assertThat(lcs.getMatchString(), equalTo("abcdfg"));

    LCSMatchResult lcsMatches = exec(commandObjects.lcs(keyA, keyB, new LCSParams().idx().withMatchLen()));
    assertThat(lcsMatches.getLen(), equalTo(6L));
    assertThat(lcsMatches.getMatchString(), nullValue());
    assertThat(lcsMatches.getMatches(), hasSize(2));

    LCSMatchResult.MatchedPosition match1 = lcsMatches.getMatches().get(0);
    assertThat(match1.getMatchLen(), equalTo(2L));
    assertThat(match1.getA().getStart(), equalTo(4L));
    assertThat(match1.getA().getEnd(), equalTo(5L));
    assertThat(match1.getB().getStart(), equalTo(5L));
    assertThat(match1.getB().getEnd(), equalTo(6L));

    LCSMatchResult.MatchedPosition match2 = lcsMatches.getMatches().get(1);
    assertThat(match2.getMatchLen(), equalTo(4L));
    assertThat(match2.getA().getStart(), equalTo(0L));
    assertThat(match2.getA().getEnd(), equalTo(3L));
    assertThat(match2.getB().getStart(), equalTo(0L));
    assertThat(match2.getB().getEnd(), equalTo(3L));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testLcsBinary() {
    byte[] keyA = "keyA".getBytes();
    byte[] keyB = "keyB".getBytes();

    // "abcdfg" is the common substring
    String valueA = "abcdfgh";
    String valueB = "abcdefg";

    exec(commandObjects.set(keyA, valueA.getBytes()));
    exec(commandObjects.set(keyB, valueB.getBytes()));

    LCSMatchResult lcsLen = exec(commandObjects.lcs(keyA, keyB, new LCSParams().len()));
    assertThat(lcsLen.getLen(), equalTo(6L));
    assertThat(lcsLen.getMatchString(), nullValue());

    LCSMatchResult lcs = exec(commandObjects.lcs(keyA, keyB, new LCSParams()));
    assertThat(lcs.getLen(), equalTo(0L));
    assertThat(lcs.getMatchString(), equalTo("abcdfg"));

    LCSMatchResult lcsMatches = exec(commandObjects.lcs(keyA, keyB, new LCSParams().idx().withMatchLen()));
    assertThat(lcsMatches.getLen(), equalTo(6L));
    assertThat(lcsMatches.getMatchString(), nullValue());
    assertThat(lcsMatches.getMatches(), hasSize(2));

    LCSMatchResult.MatchedPosition match1 = lcsMatches.getMatches().get(0);
    assertThat(match1.getMatchLen(), equalTo(2L));
    assertThat(match1.getA().getStart(), equalTo(4L));
    assertThat(match1.getA().getEnd(), equalTo(5L));
    assertThat(match1.getB().getStart(), equalTo(5L));
    assertThat(match1.getB().getEnd(), equalTo(6L));

    LCSMatchResult.MatchedPosition match2 = lcsMatches.getMatches().get(1);
    assertThat(match2.getMatchLen(), equalTo(4L));
    assertThat(match2.getA().getStart(), equalTo(0L));
    assertThat(match2.getA().getEnd(), equalTo(3L));
    assertThat(match2.getB().getStart(), equalTo(0L));
    assertThat(match2.getB().getEnd(), equalTo(3L));
  }

  @Test
  public void testMgetMsetAndMsetnx() {
    String key1 = "key1";
    String key2 = "key2";

    String mset = exec(commandObjects.mset(key1, "value1", key2, "value2"));
    assertThat(mset, equalTo("OK"));

    List<String> mget = exec(commandObjects.mget(key1, key2));
    assertThat(mget, contains("value1", "value2"));

    Long msetNx = exec(commandObjects.msetnx(key1, "new1", key2, "new2"));
    assertThat(msetNx, equalTo(0L));

    List<String> mgetAfterMsetNx = exec(commandObjects.mget(key1, key2));
    assertThat(mgetAfterMsetNx, contains("value1", "value2"));

    Long del = exec(commandObjects.del(key1, key2));
    assertThat(del, equalTo(2L));

    List<String> mgetAfterDel = exec(commandObjects.mget(key1, key2));
    assertThat(mgetAfterDel, contains(nullValue(), nullValue()));

    Long msetNxAfterDel = exec(commandObjects.msetnx(key1, "new1", key2, "new2"));
    assertThat(msetNxAfterDel, equalTo(1L));

    List<String> mgetAfterMsetNxAfterDel = exec(commandObjects.mget(key1, key2));
    assertThat(mgetAfterMsetNxAfterDel, contains("new1", "new2"));
  }

  @Test
  public void testMgetMsetAndMsetnxBinary() {
    byte[] key1 = "key1Bytes".getBytes();
    byte[] key2 = "key2Bytes".getBytes();

    String mset = exec(commandObjects.mset(key1, "value1".getBytes(), key2, "value2".getBytes()));
    assertThat(mset, equalTo("OK"));

    List<byte[]> mget = exec(commandObjects.mget(key1, key2));
    assertThat(mget, contains("value1".getBytes(), "value2".getBytes()));

    Long msetNx = exec(commandObjects.msetnx(key1, "new1".getBytes(), key2, "new2".getBytes()));
    assertThat(msetNx, equalTo(0L));

    List<byte[]> mgetAfterMsetNx = exec(commandObjects.mget(key1, key2));
    assertThat(mgetAfterMsetNx, contains("value1".getBytes(), "value2".getBytes()));

    Long del = exec(commandObjects.del(key1, key2));
    assertThat(del, equalTo(2L));

    List<byte[]> mgetAfterDel = exec(commandObjects.mget(key1, key2));
    assertThat(mgetAfterDel, contains(nullValue(), nullValue()));

    Long msetNxAfterDel = exec(commandObjects.msetnx(key1, "new1".getBytes(), key2, "new2".getBytes()));
    assertThat(msetNxAfterDel, equalTo(1L));

    List<byte[]> mgetAfterMsetNxAfterDel = exec(commandObjects.mget(key1, key2));
    assertThat(mgetAfterMsetNxAfterDel, contains("new1".getBytes(), "new2".getBytes()));
  }

  @Test
  public void testPsetexPttl() {
    String key = "tempKey";
    long milliseconds = 1000L;

    String psetEx = exec(commandObjects.psetex(key, milliseconds, "tempValue"));
    assertThat(psetEx, equalTo("OK"));

    Long pttl = exec(commandObjects.pttl(key));
    assertThat(pttl, greaterThan(0L));
  }

  @Test
  public void testPsetexPttlBinary() {
    byte[] key = "tempKey".getBytes();
    long milliseconds = 1000L;

    String psetEx = exec(commandObjects.psetex(key, milliseconds, "tempValue".getBytes()));
    assertThat(psetEx, equalTo("OK"));

    Long pttl = exec(commandObjects.pttl(key));
    assertThat(pttl, greaterThan(0L));
  }

  @Test
  public void testSetAndSetGet() {
    String key = "myKey";

    String set = exec(commandObjects.set(key, "firstValue"));
    assertThat(set, equalTo("OK"));

    String initialGet = exec(commandObjects.get(key));
    assertThat(initialGet, equalTo("firstValue"));

    SetParams setParams = new SetParams().ex(10);
    String setWithParams = exec(commandObjects.set(key, "secondValue", setParams));
    assertThat(setWithParams, equalTo("OK"));

    String getAfterSetWithParams = exec(commandObjects.get(key));
    assertThat(getAfterSetWithParams, equalTo("secondValue"));

    String setGet = exec(commandObjects.setGet(key, "thirdValue"));
    assertThat(setGet, equalTo("secondValue"));

    String getAfterSetGet = exec(commandObjects.get(key));
    assertThat(getAfterSetGet, equalTo("thirdValue"));

    String setGetWithParams = exec(commandObjects.setGet(key, "finalValue", setParams));
    assertThat(setGetWithParams, equalTo("thirdValue"));

    String finalGet = exec(commandObjects.get(key));
    assertThat(finalGet, equalTo("finalValue"));
  }

  @Test
  public void testSetAndSetGetBinary() {
    byte[] key = "myKeyBytes".getBytes();

    String set = exec(commandObjects.set(key, "firstValue".getBytes()));
    assertThat(set, equalTo("OK"));

    byte[] getAfterSet = exec(commandObjects.get(key));
    assertThat(getAfterSet, equalTo("firstValue".getBytes()));

    SetParams setParams = new SetParams().ex(10);
    String setWithParams = exec(commandObjects.set(key, "secondValue".getBytes(), setParams));
    assertThat(setWithParams, equalTo("OK"));

    byte[] getAfterSetWithParams = exec(commandObjects.get(key));
    assertThat(getAfterSetWithParams, equalTo("secondValue".getBytes()));

    byte[] setGet = exec(commandObjects.setGet(key, "thirdValue".getBytes()));
    assertThat(setGet, equalTo("secondValue".getBytes()));

    byte[] getAfterSetGet = exec(commandObjects.get(key));
    assertThat(getAfterSetGet, equalTo("thirdValue".getBytes()));

    byte[] setGetWithParams = exec(commandObjects.setGet(key, "finalValue".getBytes(), setParams));
    assertThat(setGetWithParams, equalTo("thirdValue".getBytes()));

    byte[] getAfterSetGetWithParams = exec(commandObjects.get(key));
    assertThat(getAfterSetGetWithParams, equalTo("finalValue".getBytes()));
  }

  @Test
  public void testSetnxAndSetexWithGets() {
    String key = "uniqueKey";

    Long setNx = exec(commandObjects.setnx(key, "helloWorld"));
    assertThat(setNx, equalTo(1L));

    String getAfterSetNx = exec(commandObjects.get(key));
    assertThat(getAfterSetNx, equalTo("helloWorld"));

    String setEx = exec(commandObjects.setex(key, 10L, "newValue"));
    assertThat(setEx, equalTo("OK"));

    String getAfterSetEx = exec(commandObjects.get(key));
    assertThat(getAfterSetEx, equalTo("newValue"));

    Long setNxAgain = exec(commandObjects.setnx(key, "anotherNewValue"));
    assertThat(setNxAgain, equalTo(0L));

    String getAfterSetNxAgain = exec(commandObjects.get(key));
    assertThat(getAfterSetNxAgain, equalTo("newValue"));
  }

  @Test
  public void testSetnxAndSetexWithGetsBinary() {
    byte[] key = "uniqueKeyBytes".getBytes();

    Long setNx = exec(commandObjects.setnx(key, "helloWorld".getBytes()));
    assertThat(setNx, equalTo(1L));

    byte[] getAfterSetNx = exec(commandObjects.get(key));
    assertThat(getAfterSetNx, equalTo("helloWorld".getBytes()));

    String setEx = exec(commandObjects.setex(key, 10L, "newValue".getBytes()));
    assertThat(setEx, equalTo("OK"));

    byte[] getAfterSetEx = exec(commandObjects.get(key));
    assertThat(getAfterSetEx, equalTo("newValue".getBytes()));

    Long setNxAgain = exec(commandObjects.setnx(key, "anotherNewValueBytes".getBytes()));
    assertThat(setNxAgain, equalTo(0L));

    byte[] getAfterSetNxAgain = exec(commandObjects.get(key));
    assertThat(getAfterSetNxAgain, equalTo("newValue".getBytes()));
  }

  @Test
  public void testSubstrAndStrlen() {
    String key = "testKey";
    String value = "HelloWorld";

    int start = 1;
    int end = 5; // end is inclusive
    String fragment = "elloW";

    exec(commandObjects.set(key, value));

    String substr = exec(commandObjects.substr(key, start, end));
    assertThat(substr, equalTo(fragment));

    byte[] substrBinary = exec(commandObjects.substr(key.getBytes(), start, end));
    assertThat(substrBinary, equalTo(fragment.getBytes()));

    Long strlen = exec(commandObjects.strlen(key));
    assertThat(strlen, equalTo((long) value.length()));

    Long strlenBinary = exec(commandObjects.strlen(key.getBytes()));
    assertThat(strlenBinary, equalTo((long) value.length()));
  }
}
