package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import redis.clients.jedis.args.ListDirection;
import redis.clients.jedis.args.ListPosition;
import redis.clients.jedis.params.LPosParams;
import redis.clients.jedis.util.KeyValue;

public class UnifiedJedisListCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testBlmove() {
    String srcKey = "sourceList";
    String dstKey = "destinationList";
    ListDirection from = ListDirection.LEFT;
    ListDirection to = ListDirection.RIGHT;
    double timeout = 10.5; // Timeout in seconds
    String expectedMovedValue = "value";

    when(commandObjects.blmove(srcKey, dstKey, from, to, timeout)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedMovedValue);

    String result = jedis.blmove(srcKey, dstKey, from, to, timeout);

    assertThat(result, equalTo(expectedMovedValue));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).blmove(srcKey, dstKey, from, to, timeout);
  }

  @Test
  public void testBlmoveBinary() {
    byte[] srcKey = "sourceList".getBytes();
    byte[] dstKey = "destinationList".getBytes();
    ListDirection from = ListDirection.LEFT;
    ListDirection to = ListDirection.RIGHT;
    double timeout = 10.5; // Timeout in seconds
    byte[] expectedMovedValue = "value".getBytes();

    when(commandObjects.blmove(srcKey, dstKey, from, to, timeout)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedMovedValue);

    byte[] result = jedis.blmove(srcKey, dstKey, from, to, timeout);

    assertThat(result, equalTo(expectedMovedValue));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).blmove(srcKey, dstKey, from, to, timeout);
  }

  @Test
  public void testBlmpop() {
    double timeout = 10.5; // Timeout in seconds
    ListDirection direction = ListDirection.LEFT;
    String[] keys = { "listKey1", "listKey2" };
    KeyValue<String, List<String>> expectedKeyValue = new KeyValue<>("listKey1", Arrays.asList("value1", "value2"));

    when(commandObjects.blmpop(timeout, direction, keys)).thenReturn(keyValueStringListStringCommandObject);
    when(commandExecutor.executeCommand(keyValueStringListStringCommandObject)).thenReturn(expectedKeyValue);

    KeyValue<String, List<String>> result = jedis.blmpop(timeout, direction, keys);

    assertThat(result, equalTo(expectedKeyValue));

    verify(commandExecutor).executeCommand(keyValueStringListStringCommandObject);
    verify(commandObjects).blmpop(timeout, direction, keys);
  }

  @Test
  public void testBlmpopBinary() {
    double timeout = 10.5; // Timeout in seconds
    ListDirection direction = ListDirection.LEFT;
    byte[][] keys = { "listKey1".getBytes(), "listKey2".getBytes() };
    KeyValue<byte[], List<byte[]>> expectedKeyValue = new KeyValue<>("listKey1".getBytes(), Arrays.asList("value1".getBytes(), "value2".getBytes()));

    when(commandObjects.blmpop(timeout, direction, keys)).thenReturn(keyValueBytesListBytesCommandObject);
    when(commandExecutor.executeCommand(keyValueBytesListBytesCommandObject)).thenReturn(expectedKeyValue);

    KeyValue<byte[], List<byte[]>> result = jedis.blmpop(timeout, direction, keys);

    assertThat(result, equalTo(expectedKeyValue));

    verify(commandExecutor).executeCommand(keyValueBytesListBytesCommandObject);
    verify(commandObjects).blmpop(timeout, direction, keys);
  }

  @Test
  public void testBlmpopCount() {
    double timeout = 10.5; // Timeout in seconds
    ListDirection direction = ListDirection.RIGHT;
    int count = 2;
    String[] keys = { "listKey1", "listKey2" };
    KeyValue<String, List<String>> expectedKeyValue = new KeyValue<>("listKey2", Arrays.asList("value3", "value4"));

    when(commandObjects.blmpop(timeout, direction, count, keys)).thenReturn(keyValueStringListStringCommandObject);
    when(commandExecutor.executeCommand(keyValueStringListStringCommandObject)).thenReturn(expectedKeyValue);

    KeyValue<String, List<String>> result = jedis.blmpop(timeout, direction, count, keys);

    assertThat(result, equalTo(expectedKeyValue));

    verify(commandExecutor).executeCommand(keyValueStringListStringCommandObject);
    verify(commandObjects).blmpop(timeout, direction, count, keys);
  }

  @Test
  public void testBlmpopCountBinary() {
    double timeout = 10.5; // Timeout in seconds
    ListDirection direction = ListDirection.RIGHT;
    int count = 2;
    byte[][] keys = { "listKey1".getBytes(), "listKey2".getBytes() };
    KeyValue<byte[], List<byte[]>> expectedKeyValue = new KeyValue<>("listKey2".getBytes(), Arrays.asList("value3".getBytes(), "value4".getBytes()));

    when(commandObjects.blmpop(timeout, direction, count, keys)).thenReturn(keyValueBytesListBytesCommandObject);
    when(commandExecutor.executeCommand(keyValueBytesListBytesCommandObject)).thenReturn(expectedKeyValue);

    KeyValue<byte[], List<byte[]>> result = jedis.blmpop(timeout, direction, count, keys);

    assertThat(result, equalTo(expectedKeyValue));

    verify(commandExecutor).executeCommand(keyValueBytesListBytesCommandObject);
    verify(commandObjects).blmpop(timeout, direction, count, keys);
  }

  @Test
  public void testBlpop() {
    int timeout = 10; // Timeout in seconds
    String key = "listKey";
    List<String> expectedValues = Arrays.asList(key, "value1");

    when(commandObjects.blpop(timeout, key)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedValues);

    List<String> result = jedis.blpop(timeout, key);

    assertThat(result, equalTo(expectedValues));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).blpop(timeout, key);
  }

  @Test
  public void testBlpopBinary() {
    int timeout = 10; // Timeout in seconds
    byte[][] keys = { "listKey1".getBytes(), "listKey2".getBytes() };
    List<byte[]> expectedValues = Arrays.asList("listKey1".getBytes(), "value1".getBytes());

    when(commandObjects.blpop(timeout, keys)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedValues);

    List<byte[]> result = jedis.blpop(timeout, keys);

    assertThat(result, equalTo(expectedValues));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).blpop(timeout, keys);
  }

  @Test
  public void testBlpopDoubleTimeout() {
    double timeout = 10.5; // Timeout in seconds
    String key = "listKey";
    KeyValue<String, String> expectedKeyValue = new KeyValue<>(key, "value1");

    when(commandObjects.blpop(timeout, key)).thenReturn(keyValueStringStringCommandObject);
    when(commandExecutor.executeCommand(keyValueStringStringCommandObject)).thenReturn(expectedKeyValue);

    KeyValue<String, String> result = jedis.blpop(timeout, key);

    assertThat(result, equalTo(expectedKeyValue));

    verify(commandExecutor).executeCommand(keyValueStringStringCommandObject);
    verify(commandObjects).blpop(timeout, key);
  }

  @Test
  public void testBlpopDoubleTimeoutBinary() {
    double timeout = 10.5; // Timeout in seconds
    byte[][] keys = { "listKey1".getBytes(), "listKey2".getBytes() };
    KeyValue<byte[], byte[]> expectedKeyValue = new KeyValue<>("listKey1".getBytes(), "value1".getBytes());

    when(commandObjects.blpop(timeout, keys)).thenReturn(keyValueBytesBytesCommandObject);
    when(commandExecutor.executeCommand(keyValueBytesBytesCommandObject)).thenReturn(expectedKeyValue);

    KeyValue<byte[], byte[]> result = jedis.blpop(timeout, keys);

    assertThat(result, equalTo(expectedKeyValue));

    verify(commandExecutor).executeCommand(keyValueBytesBytesCommandObject);
    verify(commandObjects).blpop(timeout, keys);
  }

  @Test
  public void testBlpopMultipleKeys() {
    int timeout = 10; // Timeout in seconds
    String[] keys = { "listKey1", "listKey2" };
    List<String> expectedValues = Arrays.asList("listKey1", "value1");

    when(commandObjects.blpop(timeout, keys)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedValues);

    List<String> result = jedis.blpop(timeout, keys);

    assertThat(result, equalTo(expectedValues));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).blpop(timeout, keys);
  }

  @Test
  public void testBlpopMultipleKeysDoubleTimeout() {
    double timeout = 10.5; // Timeout in seconds
    String[] keys = { "listKey1", "listKey2" };
    KeyValue<String, String> expectedKeyValue = new KeyValue<>("listKey1", "value1");

    when(commandObjects.blpop(timeout, keys)).thenReturn(keyValueStringStringCommandObject);
    when(commandExecutor.executeCommand(keyValueStringStringCommandObject)).thenReturn(expectedKeyValue);

    KeyValue<String, String> result = jedis.blpop(timeout, keys);

    assertThat(result, equalTo(expectedKeyValue));

    verify(commandExecutor).executeCommand(keyValueStringStringCommandObject);
    verify(commandObjects).blpop(timeout, keys);
  }

  @Test
  public void testBrpop() {
    int timeout = 10; // Timeout in seconds
    String key = "listKey";
    List<String> expectedValues = Arrays.asList(key, "value1");

    when(commandObjects.brpop(timeout, key)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedValues);

    List<String> result = jedis.brpop(timeout, key);

    assertThat(result, equalTo(expectedValues));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).brpop(timeout, key);
  }

  @Test
  public void testBrpopBinary() {
    int timeout = 10; // Timeout in seconds
    byte[][] keys = { "listKey1".getBytes(), "listKey2".getBytes() };
    List<byte[]> expectedValues = Arrays.asList("listKey1".getBytes(), "value1".getBytes());

    when(commandObjects.brpop(timeout, keys)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedValues);

    List<byte[]> result = jedis.brpop(timeout, keys);

    assertThat(result, equalTo(expectedValues));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).brpop(timeout, keys);
  }

  @Test
  public void testBrpopDoubleTimeout() {
    double timeout = 10.5; // Timeout in seconds
    String key = "listKey";
    KeyValue<String, String> expectedKeyValue = new KeyValue<>(key, "value1");

    when(commandObjects.brpop(timeout, key)).thenReturn(keyValueStringStringCommandObject);
    when(commandExecutor.executeCommand(keyValueStringStringCommandObject)).thenReturn(expectedKeyValue);

    KeyValue<String, String> result = jedis.brpop(timeout, key);

    assertThat(result, equalTo(expectedKeyValue));

    verify(commandExecutor).executeCommand(keyValueStringStringCommandObject);
    verify(commandObjects).brpop(timeout, key);
  }

  @Test
  public void testBrpopDoubleTimeoutBinary() {
    double timeout = 10.5; // Timeout in seconds
    byte[][] keys = { "listKey1".getBytes(), "listKey2".getBytes() };
    KeyValue<byte[], byte[]> expectedKeyValue = new KeyValue<>("listKey1".getBytes(), "value1".getBytes());

    when(commandObjects.brpop(timeout, keys)).thenReturn(keyValueBytesBytesCommandObject);
    when(commandExecutor.executeCommand(keyValueBytesBytesCommandObject)).thenReturn(expectedKeyValue);

    KeyValue<byte[], byte[]> result = jedis.brpop(timeout, keys);

    assertThat(result, equalTo(expectedKeyValue));

    verify(commandExecutor).executeCommand(keyValueBytesBytesCommandObject);
    verify(commandObjects).brpop(timeout, keys);
  }

  @Test
  public void testBrpopMultipleKeys() {
    int timeout = 10; // Timeout in seconds
    String[] keys = { "listKey1", "listKey2" };
    List<String> expectedValues = Arrays.asList("listKey1", "value1");

    when(commandObjects.brpop(timeout, keys)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedValues);

    List<String> result = jedis.brpop(timeout, keys);

    assertThat(result, equalTo(expectedValues));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).brpop(timeout, keys);
  }

  @Test
  public void testBrpopMultipleKeysDoubleTimeout() {
    double timeout = 10.5; // Timeout in seconds
    String[] keys = { "listKey1", "listKey2" };
    KeyValue<String, String> expectedKeyValue = new KeyValue<>("listKey1", "value1");

    when(commandObjects.brpop(timeout, keys)).thenReturn(keyValueStringStringCommandObject);
    when(commandExecutor.executeCommand(keyValueStringStringCommandObject)).thenReturn(expectedKeyValue);

    KeyValue<String, String> result = jedis.brpop(timeout, keys);

    assertThat(result, equalTo(expectedKeyValue));

    verify(commandExecutor).executeCommand(keyValueStringStringCommandObject);
    verify(commandObjects).brpop(timeout, keys);
  }

  @Test
  public void testBrpoplpush() {
    String source = "sourceList";
    String destination = "destinationList";
    int timeout = 10; // Timeout in seconds
    String expectedPoppedAndPushedValue = "value";

    when(commandObjects.brpoplpush(source, destination, timeout)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedPoppedAndPushedValue);

    String result = jedis.brpoplpush(source, destination, timeout);

    assertThat(result, equalTo(expectedPoppedAndPushedValue));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).brpoplpush(source, destination, timeout);
  }

  @Test
  public void testBrpoplpushBinary() {
    byte[] source = "sourceList".getBytes();
    byte[] destination = "destinationList".getBytes();
    int timeout = 10; // Timeout in seconds
    byte[] expectedPoppedAndPushedValue = "value".getBytes();

    when(commandObjects.brpoplpush(source, destination, timeout)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedPoppedAndPushedValue);

    byte[] result = jedis.brpoplpush(source, destination, timeout);

    assertThat(result, equalTo(expectedPoppedAndPushedValue));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).brpoplpush(source, destination, timeout);
  }

  @Test
  public void testLindex() {
    String key = "listKey";
    long index = 1; // Get the element at index 1
    String expectedValue = "value2";

    when(commandObjects.lindex(key, index)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedValue);

    String result = jedis.lindex(key, index);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).lindex(key, index);
  }

  @Test
  public void testLindexBinary() {
    byte[] key = "listKey".getBytes();
    long index = 1; // Get the element at index 1
    byte[] expectedValue = "value2".getBytes();

    when(commandObjects.lindex(key, index)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedValue);

    byte[] result = jedis.lindex(key, index);

    assertThat(result, equalTo(expectedValue));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).lindex(key, index);
  }

  @Test
  public void testLinsert() {
    String key = "listKey";
    ListPosition where = ListPosition.BEFORE;
    String pivot = "pivotValue";
    String value = "newValue";
    long expectedInsertions = 1L; // Assuming one element was inserted

    when(commandObjects.linsert(key, where, pivot, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedInsertions);

    long result = jedis.linsert(key, where, pivot, value);

    assertThat(result, equalTo(expectedInsertions));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).linsert(key, where, pivot, value);
  }

  @Test
  public void testLinsertBinary() {
    byte[] key = "listKey".getBytes();
    ListPosition where = ListPosition.AFTER;
    byte[] pivot = "pivotValue".getBytes();
    byte[] value = "newValue".getBytes();
    long expectedInsertions = 1L; // Assuming one element was inserted

    when(commandObjects.linsert(key, where, pivot, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedInsertions);

    long result = jedis.linsert(key, where, pivot, value);

    assertThat(result, equalTo(expectedInsertions));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).linsert(key, where, pivot, value);
  }

  @Test
  public void testLlen() {
    String key = "listKey";
    long expectedLength = 5L; // Assuming the length of the list is 5

    when(commandObjects.llen(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedLength);

    long result = jedis.llen(key);

    assertThat(result, equalTo(expectedLength));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).llen(key);
  }

  @Test
  public void testLlenBinary() {
    byte[] key = "listKey".getBytes();
    long expectedLength = 5L; // Assuming the length of the list is 5

    when(commandObjects.llen(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedLength);

    long result = jedis.llen(key);

    assertThat(result, equalTo(expectedLength));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).llen(key);
  }

  @Test
  public void testLmove() {
    String srcKey = "sourceList";
    String dstKey = "destinationList";
    ListDirection from = ListDirection.LEFT;
    ListDirection to = ListDirection.RIGHT;
    String expectedMovedValue = "value";
    when(commandObjects.lmove(srcKey, dstKey, from, to)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedMovedValue);

    String result = jedis.lmove(srcKey, dstKey, from, to);

    assertThat(result, equalTo(expectedMovedValue));
    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).lmove(srcKey, dstKey, from, to);
  }

  @Test
  public void testLmoveBinary() {
    byte[] srcKey = "sourceList".getBytes();
    byte[] dstKey = "destinationList".getBytes();
    ListDirection from = ListDirection.LEFT;
    ListDirection to = ListDirection.RIGHT;
    byte[] expectedMovedValue = "value".getBytes();

    when(commandObjects.lmove(srcKey, dstKey, from, to)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedMovedValue);

    byte[] result = jedis.lmove(srcKey, dstKey, from, to);

    assertThat(result, equalTo(expectedMovedValue));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).lmove(srcKey, dstKey, from, to);
  }

  @Test
  public void testLmpop() {
    ListDirection direction = ListDirection.LEFT;
    String[] keys = { "listKey1", "listKey2" };
    KeyValue<String, List<String>> expectedKeyValue = new KeyValue<>("listKey1", Arrays.asList("value1", "value2"));

    when(commandObjects.lmpop(direction, keys)).thenReturn(keyValueStringListStringCommandObject);
    when(commandExecutor.executeCommand(keyValueStringListStringCommandObject)).thenReturn(expectedKeyValue);

    KeyValue<String, List<String>> result = jedis.lmpop(direction, keys);

    assertThat(result, equalTo(expectedKeyValue));

    verify(commandExecutor).executeCommand(keyValueStringListStringCommandObject);
    verify(commandObjects).lmpop(direction, keys);
  }

  @Test
  public void testLmpopBinary() {
    ListDirection direction = ListDirection.LEFT;
    byte[][] keys = { "listKey1".getBytes(), "listKey2".getBytes() };
    KeyValue<byte[], List<byte[]>> expectedKeyValue = new KeyValue<>("listKey1".getBytes(), Arrays.asList("value1".getBytes(), "value2".getBytes()));

    when(commandObjects.lmpop(direction, keys)).thenReturn(keyValueBytesListBytesCommandObject);
    when(commandExecutor.executeCommand(keyValueBytesListBytesCommandObject)).thenReturn(expectedKeyValue);

    KeyValue<byte[], List<byte[]>> result = jedis.lmpop(direction, keys);

    assertThat(result, equalTo(expectedKeyValue));

    verify(commandExecutor).executeCommand(keyValueBytesListBytesCommandObject);
    verify(commandObjects).lmpop(direction, keys);
  }

  @Test
  public void testLmpopCount() {
    ListDirection direction = ListDirection.RIGHT;
    int count = 2;
    String[] keys = { "listKey1", "listKey2" };
    KeyValue<String, List<String>> expectedKeyValue = new KeyValue<>("listKey2", Arrays.asList("value3", "value4"));

    when(commandObjects.lmpop(direction, count, keys)).thenReturn(keyValueStringListStringCommandObject);
    when(commandExecutor.executeCommand(keyValueStringListStringCommandObject)).thenReturn(expectedKeyValue);

    KeyValue<String, List<String>> result = jedis.lmpop(direction, count, keys);

    assertThat(result, equalTo(expectedKeyValue));

    verify(commandExecutor).executeCommand(keyValueStringListStringCommandObject);
    verify(commandObjects).lmpop(direction, count, keys);
  }

  @Test
  public void testLmpopCountBinary() {
    ListDirection direction = ListDirection.RIGHT;
    int count = 2;
    byte[][] keys = { "listKey1".getBytes(), "listKey2".getBytes() };
    KeyValue<byte[], List<byte[]>> expectedKeyValue = new KeyValue<>("listKey2".getBytes(), Arrays.asList("value3".getBytes(), "value4".getBytes()));

    when(commandObjects.lmpop(direction, count, keys)).thenReturn(keyValueBytesListBytesCommandObject);
    when(commandExecutor.executeCommand(keyValueBytesListBytesCommandObject)).thenReturn(expectedKeyValue);

    KeyValue<byte[], List<byte[]>> result = jedis.lmpop(direction, count, keys);

    assertThat(result, equalTo(expectedKeyValue));

    verify(commandExecutor).executeCommand(keyValueBytesListBytesCommandObject);
    verify(commandObjects).lmpop(direction, count, keys);
  }

  @Test
  public void testLpop() {
    String key = "listKey";
    String expectedPoppedValue = "poppedValue";

    when(commandObjects.lpop(key)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedPoppedValue);

    String result = jedis.lpop(key);

    assertThat(result, equalTo(expectedPoppedValue));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).lpop(key);
  }

  @Test
  public void testLpopBinary() {
    byte[] key = "listKey".getBytes();
    byte[] expectedPoppedValue = "poppedValue".getBytes();

    when(commandObjects.lpop(key)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedPoppedValue);

    byte[] result = jedis.lpop(key);

    assertThat(result, equalTo(expectedPoppedValue));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).lpop(key);
  }

  @Test
  public void testLpopCount() {
    String key = "listKey";
    int count = 2; // Pop two elements
    List<String> expectedPoppedValues = Arrays.asList("value1", "value2");

    when(commandObjects.lpop(key, count)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedPoppedValues);

    List<String> result = jedis.lpop(key, count);

    assertThat(result, equalTo(expectedPoppedValues));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).lpop(key, count);
  }

  @Test
  public void testLpopCountBinary() {
    byte[] key = "listKey".getBytes();
    int count = 2; // Pop two elements
    List<byte[]> expectedPoppedValues = Arrays.asList("value1".getBytes(), "value2".getBytes());

    when(commandObjects.lpop(key, count)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedPoppedValues);

    List<byte[]> result = jedis.lpop(key, count);

    assertThat(result, equalTo(expectedPoppedValues));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).lpop(key, count);
  }

  @Test
  public void testLpos() {
    String key = "listKey";
    String element = "valueToFind";
    Long expectedPosition = 1L; // Assuming the element is at index 1

    when(commandObjects.lpos(key, element)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedPosition);

    Long result = jedis.lpos(key, element);

    assertThat(result, equalTo(expectedPosition));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).lpos(key, element);
  }

  @Test
  public void testLposBinary() {
    byte[] key = "listKey".getBytes();
    byte[] element = "valueToFind".getBytes();
    Long expectedPosition = 1L; // Assuming the element is at index 1

    when(commandObjects.lpos(key, element)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedPosition);

    Long result = jedis.lpos(key, element);

    assertThat(result, equalTo(expectedPosition));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).lpos(key, element);
  }

  @Test
  public void testLposWithParams() {
    String key = "listKey";
    String element = "valueToFind";
    LPosParams params = new LPosParams().rank(2); // Find the second occurrence
    Long expectedPosition = 3L; // Assuming the second occurrence is at index 3

    when(commandObjects.lpos(key, element, params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedPosition);

    Long result = jedis.lpos(key, element, params);

    assertThat(result, equalTo(expectedPosition));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).lpos(key, element, params);
  }

  @Test
  public void testLposWithParamsBinary() {
    byte[] key = "listKey".getBytes();
    byte[] element = "valueToFind".getBytes();
    LPosParams params = new LPosParams().rank(2); // Find the second occurrence
    Long expectedPosition = 3L; // Assuming the second occurrence is at index 3

    when(commandObjects.lpos(key, element, params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedPosition);

    Long result = jedis.lpos(key, element, params);

    assertThat(result, equalTo(expectedPosition));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).lpos(key, element, params);
  }

  @Test
  public void testLposWithParamsCount() {
    String key = "listKey";
    String element = "valueToFind";
    LPosParams params = new LPosParams().rank(1); // Find the first occurrence
    long count = 2; // Find up to two positions
    List<Long> expectedPositions = Arrays.asList(1L, 4L); // Assuming occurrences at indexes 1 and 4

    when(commandObjects.lpos(key, element, params, count)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedPositions);

    List<Long> result = jedis.lpos(key, element, params, count);

    assertThat(result, equalTo(expectedPositions));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).lpos(key, element, params, count);
  }

  @Test
  public void testLposWithParamsCountBinary() {
    byte[] key = "listKey".getBytes();
    byte[] element = "valueToFind".getBytes();
    LPosParams params = new LPosParams().rank(1); // Find the first occurrence
    long count = 2; // Find up to two positions
    List<Long> expectedPositions = Arrays.asList(1L, 4L); // Assuming occurrences at indexes 1 and 4

    when(commandObjects.lpos(key, element, params, count)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedPositions);

    List<Long> result = jedis.lpos(key, element, params, count);

    assertThat(result, equalTo(expectedPositions));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).lpos(key, element, params, count);
  }

  @Test
  public void testLpush() {
    String key = "listKey";
    String[] strings = { "value1", "value2", "value3" };
    long expectedLength = 3L; // Assuming the new length of the list is 3 after LPUSH

    when(commandObjects.lpush(key, strings)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedLength);

    long result = jedis.lpush(key, strings);

    assertThat(result, equalTo(expectedLength));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).lpush(key, strings);
  }

  @Test
  public void testLpushBinary() {
    byte[] key = "listKey".getBytes();
    byte[][] args = { "value1".getBytes(), "value2".getBytes(), "value3".getBytes() };
    long expectedLength = 3L; // Assuming the new length of the list is 3 after LPUSH

    when(commandObjects.lpush(key, args)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedLength);

    long result = jedis.lpush(key, args);

    assertThat(result, equalTo(expectedLength));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).lpush(key, args);
  }

  @Test
  public void testLpushx() {
    String key = "listKey";
    String[] strings = { "value1", "value2" };
    long expectedLength = 5L; // Assuming the new length of the list is 5 after LPUSHX

    when(commandObjects.lpushx(key, strings)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedLength);

    long result = jedis.lpushx(key, strings);

    assertThat(result, equalTo(expectedLength));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).lpushx(key, strings);
  }

  @Test
  public void testLpushxBinary() {
    byte[] key = "listKey".getBytes();
    byte[][] args = { "value1".getBytes(), "value2".getBytes() };
    long expectedLength = 5L; // Assuming the new length of the list is 5 after LPUSHX

    when(commandObjects.lpushx(key, args)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedLength);

    long result = jedis.lpushx(key, args);

    assertThat(result, equalTo(expectedLength));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).lpushx(key, args);
  }

  @Test
  public void testLrange() {
    String key = "listKey";
    long start = 0;
    long stop = -1; // Get all elements in the list
    List<String> expectedValues = Arrays.asList("value1", "value2", "value3");

    when(commandObjects.lrange(key, start, stop)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedValues);

    List<String> result = jedis.lrange(key, start, stop);

    assertThat(result, equalTo(expectedValues));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).lrange(key, start, stop);
  }

  @Test
  public void testLrangeBinary() {
    byte[] key = "listKey".getBytes();
    long start = 0;
    long stop = -1; // Get all elements in the list
    List<byte[]> expectedValues = Arrays.asList("value1".getBytes(), "value2".getBytes(), "value3".getBytes());

    when(commandObjects.lrange(key, start, stop)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedValues);

    List<byte[]> result = jedis.lrange(key, start, stop);

    assertThat(result, equalTo(expectedValues));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).lrange(key, start, stop);
  }

  @Test
  public void testLrem() {
    String key = "listKey";
    long count = 1; // Remove the first occurrence
    String value = "valueToRemove";
    long expectedRemovals = 1L; // Assuming one element was removed

    when(commandObjects.lrem(key, count, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedRemovals);

    long result = jedis.lrem(key, count, value);

    assertThat(result, equalTo(expectedRemovals));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).lrem(key, count, value);
  }

  @Test
  public void testLremBinary() {
    byte[] key = "listKey".getBytes();
    long count = 1; // Remove the first occurrence
    byte[] value = "valueToRemove".getBytes();
    long expectedRemovals = 1L; // Assuming one element was removed

    when(commandObjects.lrem(key, count, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedRemovals);

    long result = jedis.lrem(key, count, value);

    assertThat(result, equalTo(expectedRemovals));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).lrem(key, count, value);
  }

  @Test
  public void testLtrim() {
    String key = "listKey";
    long start = 1;
    long stop = -1; // Trim the list to keep elements from index 1 to the end
    String expectedResponse = "OK";

    when(commandObjects.ltrim(key, start, stop)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.ltrim(key, start, stop);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).ltrim(key, start, stop);
  }

  @Test
  public void testLtrimBinary() {
    byte[] key = "listKey".getBytes();
    long start = 1;
    long stop = -1; // Trim the list to keep elements from index 1 to the end
    String expectedResponse = "OK";

    when(commandObjects.ltrim(key, start, stop)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.ltrim(key, start, stop);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).ltrim(key, start, stop);
  }

  @Test
  public void testLset() {
    String key = "listKey";
    long index = 1; // Set the element at index 1
    String value = "newValue";
    String expectedResponse = "OK";

    when(commandObjects.lset(key, index, value)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.lset(key, index, value);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).lset(key, index, value);
  }

  @Test
  public void testLsetBinary() {
    byte[] key = "listKey".getBytes();
    long index = 1; // Set the element at index 1
    byte[] value = "newValue".getBytes();
    String expectedResponse = "OK";

    when(commandObjects.lset(key, index, value)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.lset(key, index, value);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).lset(key, index, value);
  }

  @Test
  public void testRpop() {
    String key = "listKey";
    String expectedPoppedValue = "poppedValue";

    when(commandObjects.rpop(key)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedPoppedValue);

    String result = jedis.rpop(key);

    assertThat(result, equalTo(expectedPoppedValue));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).rpop(key);
  }

  @Test
  public void testRpopBinary() {
    byte[] key = "listKey".getBytes();
    byte[] expectedPoppedValue = "poppedValue".getBytes();

    when(commandObjects.rpop(key)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedPoppedValue);

    byte[] result = jedis.rpop(key);

    assertThat(result, equalTo(expectedPoppedValue));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).rpop(key);
  }

  @Test
  public void testRpopCount() {
    String key = "listKey";
    int count = 2; // Pop two elements
    List<String> expectedPoppedValues = Arrays.asList("value1", "value2");

    when(commandObjects.rpop(key, count)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedPoppedValues);

    List<String> result = jedis.rpop(key, count);

    assertThat(result, equalTo(expectedPoppedValues));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).rpop(key, count);
  }

  @Test
  public void testRpopCountBinary() {
    byte[] key = "listKey".getBytes();
    int count = 2; // Pop two elements
    List<byte[]> expectedPoppedValues = Arrays.asList("value1".getBytes(), "value2".getBytes());

    when(commandObjects.rpop(key, count)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedPoppedValues);

    List<byte[]> result = jedis.rpop(key, count);

    assertThat(result, equalTo(expectedPoppedValues));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).rpop(key, count);
  }

  @Test
  public void testRpoplpush() {
    String srckey = "sourceList";
    String dstkey = "destinationList";
    String expectedPoppedAndPushedValue = "value";

    when(commandObjects.rpoplpush(srckey, dstkey)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedPoppedAndPushedValue);

    String result = jedis.rpoplpush(srckey, dstkey);

    assertThat(result, equalTo(expectedPoppedAndPushedValue));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).rpoplpush(srckey, dstkey);
  }

  @Test
  public void testRpoplpushBinary() {
    byte[] srckey = "sourceList".getBytes();
    byte[] dstkey = "destinationList".getBytes();
    byte[] expectedPoppedAndPushedValue = "value".getBytes();

    when(commandObjects.rpoplpush(srckey, dstkey)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedPoppedAndPushedValue);

    byte[] result = jedis.rpoplpush(srckey, dstkey);

    assertThat(result, equalTo(expectedPoppedAndPushedValue));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).rpoplpush(srckey, dstkey);
  }

  @Test
  public void testRpush() {
    String key = "listKey";
    String[] strings = { "value1", "value2", "value3" };
    long expectedLength = 3L; // Assuming the new length of the list is 3 after RPUSH

    when(commandObjects.rpush(key, strings)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedLength);

    long result = jedis.rpush(key, strings);

    assertThat(result, equalTo(expectedLength));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).rpush(key, strings);
  }

  @Test
  public void testRpushBinary() {
    byte[] key = "listKey".getBytes();
    byte[][] args = { "value1".getBytes(), "value2".getBytes(), "value3".getBytes() };
    long expectedLength = 3L; // Assuming the new length of the list is 3 after RPUSH
    when(commandObjects.rpush(key, args)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedLength);

    long result = jedis.rpush(key, args);

    assertThat(result, equalTo(expectedLength));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).rpush(key, args);
  }

  @Test
  public void testRpushx() {
    String key = "listKey";
    String[] strings = { "value1", "value2" };
    long expectedLength = 7L; // Assuming the new length of the list is 7 after RPUSHX

    when(commandObjects.rpushx(key, strings)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedLength);

    long result = jedis.rpushx(key, strings);

    assertThat(result, equalTo(expectedLength));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).rpushx(key, strings);
  }

  @Test
  public void testRpushxBinary() {
    byte[] key = "listKey".getBytes();
    byte[][] args = { "value1".getBytes(), "value2".getBytes() };
    long expectedLength = 7L; // Assuming the new length of the list is 7 after RPUSHX

    when(commandObjects.rpushx(key, args)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedLength);

    long result = jedis.rpushx(key, args);

    assertThat(result, equalTo(expectedLength));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).rpushx(key, args);
  }

}
