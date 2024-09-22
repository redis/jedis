package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import redis.clients.jedis.args.BitCountOption;
import redis.clients.jedis.args.BitOP;
import redis.clients.jedis.params.BitPosParams;

public class UnifiedJedisBitmapCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testBitcount() {
    String key = "key";
    long expectedCount = 4L;

    when(commandObjects.bitcount(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCount);

    long result = jedis.bitcount(key);

    assertThat(result, sameInstance(expectedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).bitcount(key);
  }

  @Test
  public void testBitcountBinary() {
    byte[] key = "key".getBytes();
    long expectedCount = 4L;

    when(commandObjects.bitcount(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCount);

    long result = jedis.bitcount(key);

    assertThat(result, sameInstance(expectedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).bitcount(key);
  }

  @Test
  public void testBitcountRange() {
    String key = "key";
    long start = 1L;
    long end = 2L;
    long expectedCount = 2L;

    when(commandObjects.bitcount(key, start, end)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCount);

    long result = jedis.bitcount(key, start, end);

    assertThat(result, sameInstance(expectedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).bitcount(key, start, end);
  }

  @Test
  public void testBitcountRangeBinary() {
    byte[] key = "key".getBytes();
    long start = 1L;
    long end = 2L;
    long expectedCount = 2L;

    when(commandObjects.bitcount(key, start, end)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCount);

    long result = jedis.bitcount(key, start, end);

    assertThat(result, sameInstance(expectedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).bitcount(key, start, end);
  }

  @Test
  public void testBitcountRangeOption() {
    String key = "key";
    long start = 1L;
    long end = 2L;
    BitCountOption option = BitCountOption.BYTE;
    long expectedCount = 2L;

    when(commandObjects.bitcount(key, start, end, option)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCount);

    long result = jedis.bitcount(key, start, end, option);

    assertThat(result, sameInstance(expectedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).bitcount(key, start, end, option);
  }

  @Test
  public void testBitcountRangeOptionBinary() {
    byte[] key = "key".getBytes();
    long start = 1L;
    long end = 2L;
    BitCountOption option = BitCountOption.BYTE;
    long expectedCount = 2L;

    when(commandObjects.bitcount(key, start, end, option)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCount);

    long result = jedis.bitcount(key, start, end, option);

    assertThat(result, sameInstance(expectedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).bitcount(key, start, end, option);
  }

  @Test
  public void testBitfield() {
    String key = "key";
    String[] arguments = { "INCRBY", "mykey", "1", "1000" };
    List<Long> expectedResults = Arrays.asList(1000L, 2000L);

    when(commandObjects.bitfield(key, arguments)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResults);

    List<Long> results = jedis.bitfield(key, arguments);

    assertThat(results, sameInstance(expectedResults));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).bitfield(key, arguments);
  }

  @Test
  public void testBitfieldBinary() {
    byte[] key = "key".getBytes();
    byte[][] arguments = { "INCRBY".getBytes(), "mykey".getBytes(), "1".getBytes(), "1000".getBytes() };
    List<Long> expectedResults = Arrays.asList(1000L, 2000L);

    when(commandObjects.bitfield(key, arguments)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResults);

    List<Long> results = jedis.bitfield(key, arguments);

    assertThat(results, sameInstance(expectedResults));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).bitfield(key, arguments);
  }

  @Test
  public void testBitfieldReadonly() {
    String key = "key";
    String[] arguments = { "GET", "u4", "0" };
    List<Long> expectedResults = Collections.singletonList(15L);

    when(commandObjects.bitfieldReadonly(key, arguments)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResults);

    List<Long> results = jedis.bitfieldReadonly(key, arguments);

    assertThat(results, sameInstance(expectedResults));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).bitfieldReadonly(key, arguments);
  }

  @Test
  public void testBitfieldReadonlyBinary() {
    byte[] key = "key".getBytes();
    byte[][] arguments = { "GET".getBytes(), "u4".getBytes(), "0".getBytes() };
    List<Long> expectedResults = Collections.singletonList(15L);

    when(commandObjects.bitfieldReadonly(key, arguments)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResults);

    List<Long> results = jedis.bitfieldReadonly(key, arguments);

    assertThat(results, sameInstance(expectedResults));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).bitfieldReadonly(key, arguments);
  }

  @Test
  public void testBitop() {
    BitOP op = BitOP.OR;
    String destKey = "destKey";
    String[] srcKeys = { "srcKey1", "srcKey2" };
    long expectedResponse = 3L;

    when(commandObjects.bitop(op, destKey, srcKeys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.bitop(op, destKey, srcKeys);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).bitop(op, destKey, srcKeys);
  }

  @Test
  public void testBitopBinary() {
    BitOP op = BitOP.XOR;
    byte[] destKey = "destKey".getBytes();
    byte[][] srcKeys = { "srcKey1".getBytes(), "srcKey2".getBytes() };
    long expectedResponse = 4L;

    when(commandObjects.bitop(op, destKey, srcKeys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.bitop(op, destKey, srcKeys);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).bitop(op, destKey, srcKeys);
  }

  @Test
  public void testBitpos() {
    String key = "key";
    boolean value = true; // Looking for the first bit set to 1
    long expectedPosition = 2L; // Assuming the first bit set to 1 is at position 2

    when(commandObjects.bitpos(key, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedPosition);

    long result = jedis.bitpos(key, value);

    assertThat(result, sameInstance(expectedPosition));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).bitpos(key, value);
  }

  @Test
  public void testBitposBinary() {
    byte[] key = "key".getBytes();
    boolean value = true; // Looking for the first bit set to 1
    long expectedPosition = 2L; // Assuming the first bit set to 1 is at position 2

    when(commandObjects.bitpos(key, value)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedPosition);

    long result = jedis.bitpos(key, value);

    assertThat(result, sameInstance(expectedPosition));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).bitpos(key, value);
  }

  @Test
  public void testBitposParams() {
    String key = "key";
    boolean value = false; // Looking for the first bit set to 0
    BitPosParams params = new BitPosParams(1); // Starting the search from byte offset 1
    long expectedPosition = 8L; // Assuming the first bit set to 0 from offset 1 is at position 8

    when(commandObjects.bitpos(key, value, params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedPosition);

    long result = jedis.bitpos(key, value, params);

    assertThat(result, sameInstance(expectedPosition));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).bitpos(key, value, params);
  }

  @Test
  public void testBitposParamsBinary() {
    byte[] key = "key".getBytes();
    boolean value = false; // Looking for the first bit set to 0
    BitPosParams params = new BitPosParams(1); // Starting the search from byte offset 1
    long expectedPosition = 8L; // Assuming the first bit set to 0 from offset 1 is at position 8

    when(commandObjects.bitpos(key, value, params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedPosition);

    long result = jedis.bitpos(key, value, params);

    assertThat(result, sameInstance(expectedPosition));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).bitpos(key, value, params);
  }

  @Test
  public void testGetbit() {
    String key = "key";
    long offset = 10L;
    boolean expectedResponse = true;

    when(commandObjects.getbit(key, offset)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(expectedResponse);

    boolean result = jedis.getbit(key, offset);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).getbit(key, offset);
  }

  @Test
  public void testGetbitBinary() {
    byte[] key = "key".getBytes();
    long offset = 10L;
    boolean expectedResponse = true;

    when(commandObjects.getbit(key, offset)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(expectedResponse);

    boolean result = jedis.getbit(key, offset);

    assertThat(result, sameInstance(expectedResponse));
    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).getbit(key, offset);
  }

  @Test
  public void testSetbit() {
    String key = "key";
    long offset = 10L;
    boolean value = true;
    boolean expectedResponse = true;

    when(commandObjects.setbit(key, offset, value)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(expectedResponse);

    boolean result = jedis.setbit(key, offset, value);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).setbit(key, offset, value);
  }

  @Test
  public void testSetbitBinary() {
    byte[] key = "key".getBytes();
    long offset = 10L;
    boolean value = true;
    boolean expectedResponse = true;

    when(commandObjects.setbit(key, offset, value)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(expectedResponse);

    boolean result = jedis.setbit(key, offset, value);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).setbit(key, offset, value);
  }

}
