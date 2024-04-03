package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import redis.clients.jedis.params.GetExParams;
import redis.clients.jedis.params.LCSParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.resps.LCSMatchResult;

public class UnifiedJedisStringCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testAppend() {
    String key = "key";
    String value = "value";
    long expectedLength = 10L; // Assuming the new length of the string is 10 after append

    when(commandObjects.append(key, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedLength);

    long result = jedis.append(key, value);

    assertThat(result, equalTo(expectedLength));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).append(key, value);
  }

  @Test
  public void testAppendBinary() {
    byte[] key = "key".getBytes();
    byte[] value = "value".getBytes();
    long expectedLength = 10L; // Assuming the new length of the string is 10 after append

    when(commandObjects.append(key, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedLength);

    long result = jedis.append(key, value);

    assertThat(result, equalTo(expectedLength));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).append(key, value);
  }

  @Test
  public void testDecr() {
    String key = "key";
    long expectedValue = -1L; // Assuming the key was decremented successfully

    when(commandObjects.decr(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedValue);

    long result = jedis.decr(key);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).decr(key);
  }

  @Test
  public void testDecrBinary() {
    byte[] key = "key".getBytes();
    long expectedValue = -1L; // Assuming the key was decremented successfully

    when(commandObjects.decr(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedValue);

    long result = jedis.decr(key);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).decr(key);
  }

  @Test
  public void testDecrBy() {
    String key = "key";
    long decrement = 2L;
    long expectedValue = -2L; // Assuming the key was decremented by 2 successfully

    when(commandObjects.decrBy(key, decrement)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedValue);

    long result = jedis.decrBy(key, decrement);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).decrBy(key, decrement);
  }

  @Test
  public void testDecrByBinary() {
    byte[] key = "key".getBytes();
    long decrement = 2L;
    long expectedValue = -2L; // Assuming the key was decremented by 2 successfully

    when(commandObjects.decrBy(key, decrement)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedValue);

    long result = jedis.decrBy(key, decrement);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).decrBy(key, decrement);
  }

  @Test
  public void testGet() {
    String key = "key";
    String expectedValue = "value";

    when(commandObjects.get(key)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedValue);

    String result = jedis.get(key);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).get(key);
  }

  @Test
  public void testGetBinary() {
    byte[] key = "key".getBytes();
    byte[] expectedValue = "value".getBytes();

    when(commandObjects.get(key)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedValue);

    byte[] result = jedis.get(key);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).get(key);
  }

  @Test
  public void testGetDel() {
    String key = "key";
    String expectedValue = "value";

    when(commandObjects.getDel(key)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedValue);

    String result = jedis.getDel(key);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).getDel(key);
  }

  @Test
  public void testGetDelBinary() {
    byte[] key = "key".getBytes();
    byte[] expectedValue = "value".getBytes();

    when(commandObjects.getDel(key)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedValue);

    byte[] result = jedis.getDel(key);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).getDel(key);
  }

  @Test
  public void testGetEx() {
    String key = "key";
    GetExParams params = new GetExParams().ex(10);
    String expectedValue = "value";

    when(commandObjects.getEx(key, params)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedValue);

    String result = jedis.getEx(key, params);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).getEx(key, params);
  }

  @Test
  public void testGetExBinary() {
    byte[] key = "key".getBytes();
    GetExParams params = new GetExParams().ex(10);
    byte[] expectedValue = "value".getBytes();

    when(commandObjects.getEx(key, params)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedValue);

    byte[] result = jedis.getEx(key, params);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).getEx(key, params);
  }

  @Test
  public void testGetrange() {
    String key = "key";
    long startOffset = 0L;
    long endOffset = 10L;
    String expectedResponse = "value";

    when(commandObjects.getrange(key, startOffset, endOffset)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.getrange(key, startOffset, endOffset);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).getrange(key, startOffset, endOffset);
  }

  @Test
  public void testGetrangeBinary() {
    byte[] key = "key".getBytes();
    long startOffset = 0L;
    long endOffset = 10L;
    byte[] expectedResponse = "value".getBytes();

    when(commandObjects.getrange(key, startOffset, endOffset)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedResponse);

    byte[] result = jedis.getrange(key, startOffset, endOffset);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).getrange(key, startOffset, endOffset);
  }

  @Test
  public void testGetSet() {
    String key = "key";
    String value = "newValue";
    String expectedPreviousValue = "oldValue";

    when(commandObjects.getSet(key, value)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedPreviousValue);

    String result = jedis.getSet(key, value);

    assertThat(result, equalTo(expectedPreviousValue));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).getSet(key, value);
  }

  @Test
  public void testGetSetBinary() {
    byte[] key = "key".getBytes();
    byte[] value = "newValue".getBytes();
    byte[] expectedPreviousValue = "oldValue".getBytes();

    when(commandObjects.getSet(key, value)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedPreviousValue);

    byte[] result = jedis.getSet(key, value);

    assertThat(result, equalTo(expectedPreviousValue));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).getSet(key, value);
  }

  @Test
  public void testIncr() {
    String key = "key";
    long expectedValue = 1L;

    when(commandObjects.incr(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedValue);

    long result = jedis.incr(key);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).incr(key);
  }

  @Test
  public void testIncrBinary() {
    byte[] key = "key".getBytes();
    long expectedValue = 1L; // Assuming the key was incremented successfully

    when(commandObjects.incr(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedValue);

    long result = jedis.incr(key);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).incr(key);
  }

  @Test
  public void testIncrBy() {
    String key = "key";
    long increment = 2L;
    long expectedValue = 3L; // Assuming the key was incremented by 2 successfully

    when(commandObjects.incrBy(key, increment)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedValue);

    long result = jedis.incrBy(key, increment);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).incrBy(key, increment);
  }

  @Test
  public void testIncrByBinary() {
    byte[] key = "key".getBytes();
    long increment = 2L;
    long expectedValue = 3L; // Assuming the key was incremented by 2 successfully

    when(commandObjects.incrBy(key, increment)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedValue);

    long result = jedis.incrBy(key, increment);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).incrBy(key, increment);
  }

  @Test
  public void testIncrByFloat() {
    String key = "key";
    double increment = 2.5;
    double expectedValue = 3.5; // Assuming the key was incremented by 2.5 successfully

    when(commandObjects.incrByFloat(key, increment)).thenReturn(doubleCommandObject);
    when(commandExecutor.executeCommand(doubleCommandObject)).thenReturn(expectedValue);

    double result = jedis.incrByFloat(key, increment);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(doubleCommandObject);
    verify(commandObjects).incrByFloat(key, increment);
  }

  @Test
  public void testIncrByFloatBinary() {
    byte[] key = "key".getBytes();
    double increment = 2.5;
    double expectedValue = 3.5; // Assuming the key was incremented by 2.5 successfully

    when(commandObjects.incrByFloat(key, increment)).thenReturn(doubleCommandObject);
    when(commandExecutor.executeCommand(doubleCommandObject)).thenReturn(expectedValue);

    double result = jedis.incrByFloat(key, increment);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(doubleCommandObject);
    verify(commandObjects).incrByFloat(key, increment);
  }

  @Test
  public void testLcs() {
    String keyA = "keyA";
    String keyB = "keyB";
    LCSParams params = new LCSParams().withMatchLen();
    LCSMatchResult expectedResult = new LCSMatchResult(5); // Assuming the LCS length is 5

    when(commandObjects.lcs(keyA, keyB, params)).thenReturn(lcsMatchResultCommandObject);
    when(commandExecutor.executeCommand(lcsMatchResultCommandObject)).thenReturn(expectedResult);

    LCSMatchResult result = jedis.lcs(keyA, keyB, params);

    assertThat(result, equalTo(expectedResult));

    verify(commandExecutor).executeCommand(lcsMatchResultCommandObject);
    verify(commandObjects).lcs(keyA, keyB, params);
  }

  @Test
  public void testLcsBinary() {
    byte[] keyA = "keyA".getBytes();
    byte[] keyB = "keyB".getBytes();
    LCSParams params = new LCSParams().withMatchLen();
    LCSMatchResult expectedResult = new LCSMatchResult(5); // Assuming the LCS length is 5

    when(commandObjects.lcs(keyA, keyB, params)).thenReturn(lcsMatchResultCommandObject);
    when(commandExecutor.executeCommand(lcsMatchResultCommandObject)).thenReturn(expectedResult);

    LCSMatchResult result = jedis.lcs(keyA, keyB, params);

    assertThat(result, equalTo(expectedResult));

    verify(commandExecutor).executeCommand(lcsMatchResultCommandObject);
    verify(commandObjects).lcs(keyA, keyB, params);
  }

  @Test
  public void testMget() {
    String[] keys = { "key1", "key2", "key3" };
    List<String> expectedValues = Arrays.asList("value1", "value2", "value3");

    when(commandObjects.mget(keys)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedValues);

    List<String> result = jedis.mget(keys);

    assertThat(result, equalTo(expectedValues));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).mget(keys);
  }

  @Test
  public void testMgetBinary() {
    byte[][] keys = { "key1".getBytes(), "key2".getBytes(), "key3".getBytes() };
    List<byte[]> expectedValues = Arrays.asList("value1".getBytes(), "value2".getBytes(), "value3".getBytes());

    when(commandObjects.mget(keys)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedValues);

    List<byte[]> result = jedis.mget(keys);

    assertThat(result, equalTo(expectedValues));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).mget(keys);
  }

  @Test
  public void testMset() {
    String[] keysvalues = { "key1", "value1", "key2", "value2" };
    String expectedResponse = "OK";

    when(commandObjects.mset(keysvalues)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.mset(keysvalues);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).mset(keysvalues);
  }

  @Test
  public void testMsetBinary() {
    byte[][] keysvalues = { "key1".getBytes(), "value1".getBytes(), "key2".getBytes(), "value2".getBytes() };
    String expectedResponse = "OK";

    when(commandObjects.mset(keysvalues)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.mset(keysvalues);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).mset(keysvalues);
  }

  @Test
  public void testMsetnx() {
    String[] keysvalues = { "key1", "value1", "key2", "value2" };
    long expectedResponse = 1L; // Assuming the keys were set successfully

    when(commandObjects.msetnx(keysvalues)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.msetnx(keysvalues);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).msetnx(keysvalues);
  }

  @Test
  public void testMsetnxBinary() {
    byte[][] keysvalues = { "key1".getBytes(), "value1".getBytes(), "key2".getBytes(), "value2".getBytes() };
    long expectedResponse = 1L; // Assuming the keys were set successfully

    when(commandObjects.msetnx(keysvalues)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.msetnx(keysvalues);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).msetnx(keysvalues);
  }

  @Test
  public void testPsetex() {
    String key = "key";
    long milliseconds = 1000L;
    String value = "value";
    String expectedResponse = "OK";

    when(commandObjects.psetex(key, milliseconds, value)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.psetex(key, milliseconds, value);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).psetex(key, milliseconds, value);
  }

  @Test
  public void testPsetexBinary() {
    byte[] key = "key".getBytes();
    long milliseconds = 1000L;
    byte[] value = "value".getBytes();
    String expectedResponse = "OK";

    when(commandObjects.psetex(key, milliseconds, value)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.psetex(key, milliseconds, value);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).psetex(key, milliseconds, value);
  }

  @Test
  public void testSet() {
    String key = "key";
    String value = "value";
    String expectedResponse = "OK";

    when(commandObjects.set(key, value)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.set(key, value);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).set(key, value);
  }

  @Test
  public void testSetBinary() {
    byte[] key = "key".getBytes();
    byte[] value = "value".getBytes();
    String expectedResponse = "OK";

    when(commandObjects.set(key, value)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.set(key, value);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).set(key, value);
  }

  @Test
  public void testSetWithParams() {
    String key = "key";
    String value = "value";
    SetParams params = new SetParams().nx().ex(10);
    String expectedResponse = "OK";

    when(commandObjects.set(key, value, params)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.set(key, value, params);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).set(key, value, params);
  }

  @Test
  public void testSetWithParamsBinary() {
    byte[] key = "key".getBytes();
    byte[] value = "value".getBytes();
    SetParams params = new SetParams().nx().ex(10);
    String expectedResponse = "OK";

    when(commandObjects.set(key, value, params)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.set(key, value, params);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).set(key, value, params);
  }

  @Test
  public void testSetGet() {
    String key = "key";
    String value = "value";
    String expectedPreviousValue = "previousValue";

    when(commandObjects.setGet(key, value)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedPreviousValue);

    String result = jedis.setGet(key, value);

    assertThat(result, equalTo(expectedPreviousValue));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).setGet(key, value);
  }

  @Test
  public void testSetGetBinary() {
    byte[] key = "key".getBytes();
    byte[] value = "value".getBytes();
    byte[] expectedPreviousValue = "previousValue".getBytes();

    when(commandObjects.setGet(key, value)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedPreviousValue);

    byte[] result = jedis.setGet(key, value);

    assertThat(result, equalTo(expectedPreviousValue));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).setGet(key, value);
  }

  @Test
  public void testSetGetWithParams() {
    String key = "key";
    String value = "value";
    SetParams params = new SetParams().nx().ex(10);
    String expectedPreviousValue = "previousValue";

    when(commandObjects.setGet(key, value, params)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedPreviousValue);

    String result = jedis.setGet(key, value, params);

    assertThat(result, equalTo(expectedPreviousValue));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).setGet(key, value, params);
  }

  @Test
  public void testSetGetWithParamsBinary() {
    byte[] key = "key".getBytes();
    byte[] value = "value".getBytes();
    SetParams params = new SetParams().nx().ex(10);
    byte[] expectedPreviousValue = "previousValue".getBytes();

    when(commandObjects.setGet(key, value, params)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedPreviousValue);

    byte[] result = jedis.setGet(key, value, params);

    assertThat(result, equalTo(expectedPreviousValue));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).setGet(key, value, params);
  }

  @Test
  public void testSetex() {
    String key = "key";
    long seconds = 60L;
    String value = "value";
    String expectedResponse = "OK";

    when(commandObjects.setex(key, seconds, value)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.setex(key, seconds, value);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).setex(key, seconds, value);
  }

  @Test
  public void testSetexBinary() {
    byte[] key = "key".getBytes();
    long seconds = 60L;
    byte[] value = "value".getBytes();
    String expectedResponse = "OK";

    when(commandObjects.setex(key, seconds, value)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.setex(key, seconds, value);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).setex(key, seconds, value);
  }

  @Test
  public void testSetnx() {
    String key = "key";
    String value = "value";
    long expectedResponse = 1L; // Assuming the key was set successfully

    when(commandObjects.setnx(key, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.setnx(key, value);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).setnx(key, value);
  }

  @Test
  public void testSetnxBinary() {
    byte[] key = "key".getBytes();
    byte[] value = "value".getBytes();
    long expectedResponse = 1L; // Assuming the key was set successfully

    when(commandObjects.setnx(key, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.setnx(key, value);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).setnx(key, value);
  }

  @Test
  public void testSetrange() {
    String key = "key";
    long offset = 10L;
    String value = "value";
    long expectedResponse = value.length();

    when(commandObjects.setrange(key, offset, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.setrange(key, offset, value);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).setrange(key, offset, value);
  }

  @Test
  public void testSetrangeBinary() {
    byte[] key = "key".getBytes();
    long offset = 10L;
    byte[] value = "value".getBytes();
    long expectedResponse = value.length;

    when(commandObjects.setrange(key, offset, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.setrange(key, offset, value);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).setrange(key, offset, value);
  }

  @Test
  public void testStrlen() {
    String key = "key";
    long expectedLength = 5L; // Assuming the length of the string value is 5

    when(commandObjects.strlen(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedLength);

    long result = jedis.strlen(key);

    assertThat(result, equalTo(expectedLength));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).strlen(key);
  }

  @Test
  public void testStrlenBinary() {
    byte[] key = "key".getBytes();
    long expectedLength = 5L; // Assuming the length of the string value is 5

    when(commandObjects.strlen(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedLength);

    long result = jedis.strlen(key);

    assertThat(result, equalTo(expectedLength));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).strlen(key);
  }

  @Test
  public void testSubstr() {
    String key = "key";
    int start = 0;
    int end = 3;
    String expectedSubstring = "valu";

    when(commandObjects.substr(key, start, end)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedSubstring);

    String result = jedis.substr(key, start, end);

    assertThat(result, equalTo(expectedSubstring));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).substr(key, start, end);
  }

  @Test
  public void testSubstrBinary() {
    byte[] key = "key".getBytes();
    int start = 0;
    int end = 3;
    byte[] expectedSubstring = "valu".getBytes();

    when(commandObjects.substr(key, start, end)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedSubstring);

    byte[] result = jedis.substr(key, start, end);

    assertThat(result, equalTo(expectedSubstring));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).substr(key, start, end);
  }

}
