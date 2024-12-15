package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.Test;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.BitCountOption;
import redis.clients.jedis.args.BitOP;
import redis.clients.jedis.params.BitPosParams;

/**
 * Tests related to <a href="https://redis.io/commands/?group=bitmap">Bitmap</a> commands.
 */
public class CommandObjectsBitmapCommandsTest extends CommandObjectsStandaloneTestBase {

  public CommandObjectsBitmapCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testSetbitAndGetbit() {
    String key = "bitKey";
    long offset = 10;

    Boolean initialValue = exec(commandObjects.getbit(key, offset));
    assertThat(initialValue, equalTo(false));

    Boolean setbit = exec(commandObjects.setbit(key, offset, true));
    assertThat(setbit, equalTo(false)); // original value returned

    Boolean finalValue = exec(commandObjects.getbit(key, offset));
    assertThat(finalValue, equalTo(true));
  }

  @Test
  public void testSetbitAndGetbitBinary() {
    byte[] key = "bitKeyBytes".getBytes();
    long offset = 10;

    Boolean initialValue = exec(commandObjects.getbit(key, offset));
    assertThat(initialValue, equalTo(false));

    Boolean setbit = exec(commandObjects.setbit(key, offset, true));
    assertThat(setbit, equalTo(false)); // original value returned

    Boolean finalValue = exec(commandObjects.getbit(key, offset));
    assertThat(finalValue, equalTo(true));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0", message = "Starting with Redis version 7.0.0: Added the BYTE|BIT option.")
  public void testBitcount() {
    String key = "bitcountKey";
    byte[] keyBytes = key.getBytes();

    // Set some bits
    exec(commandObjects.setbit(key, 1, true));
    exec(commandObjects.setbit(key, 2, true));
    exec(commandObjects.setbit(key, 7, true)); // This makes 1 byte with 3 bits set
    exec(commandObjects.setbit(key, 8, true)); // Next byte, first bit set

    Long bitcountFullString = exec(commandObjects.bitcount(key));
    assertThat(bitcountFullString, equalTo(4L));

    Long bitcountFirstByte = exec(commandObjects.bitcount(key, 0, 0));
    assertThat(bitcountFirstByte, equalTo(3L));

    Long bitcountFullStringBinary = exec(commandObjects.bitcount(keyBytes));
    assertThat(bitcountFullStringBinary, equalTo(4L));

    Long bitcountFirstByteBinary = exec(commandObjects.bitcount(keyBytes, 0, 0));
    assertThat(bitcountFirstByteBinary, equalTo(3L));

    Long bitcountFirstSixBits = exec(commandObjects.bitcount(key, 0, 5, BitCountOption.BIT));
    assertThat(bitcountFirstSixBits, equalTo(2L));

    Long bitcountFirstSixBitsBinary = exec(commandObjects.bitcount(keyBytes, 0, 5, BitCountOption.BIT));
    assertThat(bitcountFirstSixBitsBinary, equalTo(2L));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0", message="Starting with Redis version 7.0.0: Added the BYTE|BIT option.")
  public void testBitpos() {
    String key = "bitposKey";
    byte[] keyBytes = key.getBytes();

    // Set some bits
    exec(commandObjects.setbit(key, 10, true));
    exec(commandObjects.setbit(key, 22, true));
    exec(commandObjects.setbit(key, 30, true));

    Long firstSetBit = exec(commandObjects.bitpos(key, true));
    assertThat(firstSetBit, equalTo(10L));

    Long firstUnsetBit = exec(commandObjects.bitpos(key, false));
    assertThat(firstUnsetBit, equalTo(0L));

    BitPosParams params = new BitPosParams(15, 25).modifier(BitCountOption.BIT);

    Long firstSetBitInRange = exec(commandObjects.bitpos(key, true, params));
    assertThat(firstSetBitInRange, equalTo(22L));

    Long firstUnsetBitInRange = exec(commandObjects.bitpos(key, false, params));
    assertThat(firstUnsetBitInRange, equalTo(15L));

    Long firstSetBitBinary = exec(commandObjects.bitpos(keyBytes, true));
    assertThat(firstSetBitBinary, equalTo(10L));

    Long firstUnsetBitBinary = exec(commandObjects.bitpos(keyBytes, false));
    assertThat(firstUnsetBitBinary, equalTo(0L));

    Long firstSetBitInRangeBinary = exec(commandObjects.bitpos(keyBytes, true, params));
    assertThat(firstSetBitInRangeBinary, equalTo(22L));

    Long firstUnsetBitInRangeBinary = exec(commandObjects.bitpos(keyBytes, false, params));
    assertThat(firstUnsetBitInRangeBinary, equalTo(15L));
  }

  @Test
  public void testBitfield() {
    String key = "bitfieldKey";

    List<Long> bitfieldResult = exec(commandObjects.bitfield(
        key, "INCRBY", "i5", "100", "7", "GET", "i5", "100"));

    // Contains the result of the INCRBY operation, and the result of the GET operation.
    assertThat(bitfieldResult, contains(7L, 7L));

    List<Long> bitfieldRoResult = exec(commandObjects.bitfieldReadonly(
        key, "GET", "i4", "100"));
    assertThat(bitfieldRoResult, contains(3L));
  }

  @Test
  public void testBitfieldBinary() {
    byte[] key = "bitfieldKeyBytes".getBytes();

    List<Long> bitfieldResult = exec(commandObjects.bitfield(key,
        "INCRBY".getBytes(), "i5".getBytes(), "100".getBytes(), "7".getBytes(),
        "GET".getBytes(), "i5".getBytes(), "100".getBytes()));

    // Contains the result of the INCRBY operation, and the result of the GET operation.
    assertThat(bitfieldResult, contains(7L, 7L));

    List<Long> bitfieldRoResult = exec(commandObjects.bitfieldReadonly(key,
        "GET".getBytes(), "i4".getBytes(), "100".getBytes()));
    assertThat(bitfieldRoResult, contains(3L));
  }

  @Test
  public void testBitop() {
    String srcKey1 = "srcKey1";
    String srcKey2 = "srcKey2";
    String destKey = "destKey";

    // Set some bits
    exec(commandObjects.setbit(srcKey1, 1, true));
    exec(commandObjects.setbit(srcKey1, 2, true));
    exec(commandObjects.setbit(srcKey1, 3, true));

    exec(commandObjects.setbit(srcKey2, 1, true));
    exec(commandObjects.setbit(srcKey2, 3, true));

    Long bitopResult = exec(commandObjects.bitop(BitOP.AND, destKey, srcKey1, srcKey2));
    assertThat(bitopResult, equalTo(1L)); // 1 byte stored

    assertThat(exec(commandObjects.getbit(destKey, 1)), equalTo(true));
    assertThat(exec(commandObjects.getbit(destKey, 2)), equalTo(false));
    assertThat(exec(commandObjects.getbit(destKey, 3)), equalTo(true));
  }

  @Test
  public void testBitopBinary() {
    byte[] srcKey1 = "srcKey1".getBytes();
    byte[] srcKey2 = "srcKey2".getBytes();
    byte[] destKey = "destKey".getBytes();

    // Set some bits
    exec(commandObjects.setbit(srcKey1, 1, true));
    exec(commandObjects.setbit(srcKey1, 2, true));
    exec(commandObjects.setbit(srcKey1, 3, true));

    exec(commandObjects.setbit(srcKey2, 1, true));
    exec(commandObjects.setbit(srcKey2, 3, true));

    Long bitopResult = exec(commandObjects.bitop(BitOP.XOR, destKey, srcKey1, srcKey2));
    assertThat(bitopResult, equalTo(1L)); // 1 byte stored

    assertThat(exec(commandObjects.getbit(new String(destKey), 1)), equalTo(false));
    assertThat(exec(commandObjects.getbit(new String(destKey), 2)), equalTo(true));
    assertThat(exec(commandObjects.getbit(new String(destKey), 3)), equalTo(false));
  }
}
