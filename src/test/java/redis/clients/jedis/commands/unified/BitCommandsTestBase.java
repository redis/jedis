package redis.clients.jedis.commands.unified;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import io.redis.test.annotations.ConditionalOnEnv;
import io.redis.test.annotations.EnabledOnCommand;
import io.redis.test.annotations.SinceRedisVersion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.BitCountOption;
import redis.clients.jedis.args.BitOP;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.BitPosParams;
import redis.clients.jedis.params.BitRangeIntArrayParams;
import redis.clients.jedis.util.SafeEncoder;
import redis.clients.jedis.util.TestEnvUtil;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("integration")
public abstract class BitCommandsTestBase extends UnifiedJedisCommandsTestBase {

  public BitCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void setAndgetbit() {
    assertFalse(jedis.setbit("foo", 0, true));

    assertTrue(jedis.getbit("foo", 0));

    // Binary
    assertFalse(jedis.setbit("bfoo".getBytes(), 0, true));

    assertTrue(jedis.getbit("bfoo".getBytes(), 0));
  }

  @Test
  public void bitpos() {
    String foo = "foo";

    jedis.set(foo, String.valueOf(0));
    //  string "0" with bits: 0011 0000

    jedis.setbit(foo, 3, true);
    jedis.setbit(foo, 7, true);
    jedis.setbit(foo, 13, true);
    jedis.setbit(foo, 39, true);

    /*
     * bit:  00110001 / 00000100 / 00000000 / 00000000 / 00000001
     * byte: 0          1          2          3          4
     */
    long offset = jedis.bitpos(foo, true);
    assertEquals(2, offset);
    offset = jedis.bitpos(foo, false);
    assertEquals(0, offset);

    offset = jedis.bitpos(foo, true, new BitPosParams(1));
    assertEquals(13, offset);
    offset = jedis.bitpos(foo, false, new BitPosParams(1));
    assertEquals(8, offset);

    offset = jedis.bitpos(foo, true, new BitPosParams(2, 3));
    assertEquals(-1, offset);
    offset = jedis.bitpos(foo, false, new BitPosParams(2, 3));
    assertEquals(16, offset);

    offset = jedis.bitpos(foo, true, new BitPosParams(3, 4));
    assertEquals(39, offset);
  }

  @Test
  public void bitposBinary() {
    // binary
    byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };

    jedis.set(bfoo, Protocol.toByteArray(0));
    // bits: 0011 0000

    jedis.setbit(bfoo, 3, true);
    jedis.setbit(bfoo, 7, true);
    jedis.setbit(bfoo, 13, true);
    jedis.setbit(bfoo, 39, true);

    /*
     * bit:  00110001 / 00000100 / 00000000 / 00000000 / 00000001
     * byte: 0          1          2          3          4
     */
    long offset = jedis.bitpos(bfoo, true);
    assertEquals(2, offset);
    offset = jedis.bitpos(bfoo, false);
    assertEquals(0, offset);

    offset = jedis.bitpos(bfoo, true, new BitPosParams(1));
    assertEquals(13, offset);
    offset = jedis.bitpos(bfoo, false, new BitPosParams(1));
    assertEquals(8, offset);

    offset = jedis.bitpos(bfoo, true, new BitPosParams(2, 3));
    assertEquals(-1, offset);
    offset = jedis.bitpos(bfoo, false, new BitPosParams(2, 3));
    assertEquals(16, offset);

    offset = jedis.bitpos(bfoo, true, new BitPosParams(3, 4));
    assertEquals(39, offset);
  }

  @Test
  public void bitposWithNoMatchingBitExist() {
    String foo = "foo";

    jedis.set(foo, String.valueOf(0));
    for (int idx = 0; idx < 8; idx++) {
      jedis.setbit(foo, idx, true);
    }

    /*
     * bit:  11111111
     * byte: 0
     */
    long offset = jedis.bitpos(foo, false);
    // offset should be last index + 1
    assertEquals(8, offset);
  }

  @Test
  public void bitposWithNoMatchingBitExistWithinRange() {
    String foo = "foo";

    jedis.set(foo, String.valueOf(0));
    for (int idx = 0; idx < 8 * 5; idx++) {
      jedis.setbit(foo, idx, true);
    }

    /*
     * bit:  11111111 / 11111111 / 11111111 / 11111111 / 11111111
     * byte: 0          1          2          3          4
     */
    long offset = jedis.bitpos(foo, false, new BitPosParams(2, 3));
    // offset should be -1
    assertEquals(-1, offset);
  }

  @Test
  @SinceRedisVersion(value="7.0.0", message="Starting with Redis version 7.0.0: Added the BYTE|BIT option.")
  public void bitposModifier() {
    jedis.set("mykey", "\\x00\\xff\\xf0");
    assertEquals(0, jedis.bitpos("mykey", false));
    assertEquals(1, jedis.bitpos("mykey", true));
    assertEquals(1, jedis.bitpos("mykey", true, BitPosParams.bitPosParams()));
    assertEquals(18, jedis.bitpos("mykey", true, BitPosParams.bitPosParams().start(2)));
    assertEquals(18, jedis.bitpos("mykey", true, BitPosParams.bitPosParams().start(2).end(-1)));
    assertEquals(18, jedis.bitpos("mykey", true, BitPosParams.bitPosParams().start(2).end(-1)
        .modifier(BitCountOption.BYTE)));
    assertEquals(9, jedis.bitpos("mykey", true, BitPosParams.bitPosParams().start(7).end(15)
        .modifier(BitCountOption.BIT)));
  }

  @Test
  public void setAndgetrange() {
    jedis.set("key1", "Hello World");
    assertEquals(11, jedis.setrange("key1", 6, "Jedis"));

    assertEquals("Hello Jedis", jedis.get("key1"));

    assertEquals("Hello", jedis.getrange("key1", 0, 4));
    assertEquals("Jedis", jedis.getrange("key1", 6, 11));
  }

  @Test
  @SinceRedisVersion(value="7.0.0", message="Starting with Redis version 7.0.0: Added the BYTE|BIT option.")
  public void bitCount() {
    jedis.setbit("foo", 16, true);
    jedis.setbit("foo", 24, true);
    jedis.setbit("foo", 40, true);
    jedis.setbit("foo", 56, true);

    assertEquals(4, (long) jedis.bitcount("foo"));
    assertEquals(4, (long) jedis.bitcount("foo".getBytes()));

    assertEquals(3, (long) jedis.bitcount("foo", 2L, 5L));
    assertEquals(3, (long) jedis.bitcount("foo".getBytes(), 2L, 5L));

    assertEquals(3, (long) jedis.bitcount("foo", 2L, 5L, BitCountOption.BYTE));
    assertEquals(3, (long) jedis.bitcount("foo".getBytes(), 2L, 5L, BitCountOption.BYTE));

    assertEquals(0, (long) jedis.bitcount("foo", 2L, 5L, BitCountOption.BIT));
    assertEquals(0, (long) jedis.bitcount("foo".getBytes(), 2L, 5L, BitCountOption.BIT));
  }

  @Test
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
  public void bitOp() {
    jedis.set("key1", "\u0060");
    jedis.set("key2", "\u0044");

    jedis.bitop(BitOP.AND, "resultAnd", "key1", "key2");
    String resultAnd = jedis.get("resultAnd");
    assertEquals("\u0040", resultAnd);

    jedis.bitop(BitOP.OR, "resultOr", "key1", "key2");
    String resultOr = jedis.get("resultOr");
    assertEquals("\u0064", resultOr);

    jedis.bitop(BitOP.XOR, "resultXor", "key1", "key2");
    String resultXor = jedis.get("resultXor");
    assertEquals("\u0024", resultXor);
  }

  @Test
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
  public void bitOpNot() {
    jedis.setbit("key", 0, true);
    jedis.setbit("key", 4, true);

    jedis.bitop(BitOP.NOT, "resultNot", "key");
    String resultNot = jedis.get("resultNot");
    assertEquals("\u0077", resultNot);
  }

  @Test
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
  public void bitOpBinary() {
    byte[] dest = {0x0};
    byte[] key1 = {0x1};
    byte[] key2 = {0x2};

    jedis.set(key1, new byte[]{0x6});
    jedis.set(key2, new byte[]{0x3});

    jedis.bitop(BitOP.AND, dest, key1, key2);
    assertArrayEquals(new byte[]{0x2}, jedis.get(dest));

    jedis.bitop(BitOP.OR, dest, key1, key2);
    assertArrayEquals(new byte[]{0x7}, jedis.get(dest));

    jedis.bitop(BitOP.XOR, dest, key1, key2);
    assertArrayEquals(new byte[]{0x5}, jedis.get(dest));

    jedis.setbit(key1, 0, true);
    jedis.bitop(BitOP.NOT, dest, key1);
    assertArrayEquals(new byte[]{0x79}, jedis.get(dest));
  }

  @Test
  public void bitOpNotMultiSourceShouldFail() {
    assertThrows(JedisDataException.class, () -> jedis.bitop(BitOP.NOT, "dest", "src1", "src2"));
  }

  @Test
  public void testBitfield() {
    List<Long> responses = jedis.bitfield("mykey", "INCRBY", "i5", "100", "1", "GET", "u4", "0");
    assertEquals(1L, responses.get(0).longValue());
    assertEquals(0L, responses.get(1).longValue());
  }

  @Test
  public void testBitfieldReadonly() {
    List<Long> responses = jedis.bitfield("mykey", "INCRBY", "i5", "100", "1", "GET", "u4", "0");
    assertEquals(1L, responses.get(0).longValue());
    assertEquals(0L, responses.get(1).longValue());

    List<Long> responses2 = jedis.bitfieldReadonly("mykey", "GET", "i5", "100");
    assertEquals(1L, responses2.get(0).longValue());

    try {
      jedis.bitfieldReadonly("mykey", "INCRBY", "i5", "100", "1", "GET", "u4", "0");
      fail("Readonly command shouldn't allow INCRBY");
    } catch (JedisDataException e) {
    }
  }

  @Test
  public void testBinaryBitfield() {
    List<Long> responses = jedis.bitfield(SafeEncoder.encode("mykey"),
      SafeEncoder.encode("INCRBY"), SafeEncoder.encode("i5"), SafeEncoder.encode("100"),
      SafeEncoder.encode("1"), SafeEncoder.encode("GET"), SafeEncoder.encode("u4"),
      SafeEncoder.encode("0"));
    assertEquals(1L, responses.get(0).longValue());
    assertEquals(0L, responses.get(1).longValue());
  }

  @Test
  public void testBinaryBitfieldReadonly() {
    List<Long> responses = jedis.bitfield("mykey", "INCRBY", "i5", "100", "1", "GET", "u4", "0");
    assertEquals(1L, responses.get(0).longValue());
    assertEquals(0L, responses.get(1).longValue());

    List<Long> responses2 = jedis.bitfieldReadonly(SafeEncoder.encode("mykey"),
      SafeEncoder.encode("GET"), SafeEncoder.encode("i5"), SafeEncoder.encode("100"));
    assertEquals(1L, responses2.get(0).longValue());
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void bitOpDiff() {
    // Use single-byte values for simplicity
    byte[] key1 = new byte[] { (byte) 0x07 }; // 00000111 - bits 0,1,2 set
    byte[] key2 = new byte[] { (byte) 0x02 }; // 00000010 - bit 1 set
    byte[] key3 = new byte[] { (byte) 0x04 }; // 00000100 - bit 2 set
    String destKey = "{bitOp:key:}resultDiff";

    // Set keys using byte arrays
    jedis.set("{bitOp:key:}1".getBytes(), key1);
    jedis.set("{bitOp:key:}2".getBytes(), key2);
    jedis.set("{bitOp:key:}3".getBytes(), key3);

    // DIFF(key1, key2, key3) = key1 AND NOT(key2 OR key3)
    // key1 = 00000111 (bits 0,1,2 set)
    // key2 = 00000010 (bit 1 set)
    // key3 = 00000100 (bit 2 set)
    // key2 OR key3 = 00000110 (bits 1,2 set)
    // NOT(key2 OR key3) = 11111001 (all bits except 1,2 set)
    // key1 AND NOT(key2 OR key3) = 00000001 (only bit 0 set)
    jedis.bitop(BitOP.DIFF, destKey, "{bitOp:key:}1", "{bitOp:key:}2", "{bitOp:key:}3");

    // Get result as bytes
    byte[] resultBytes = jedis.get(destKey).getBytes();

    // Expected result: 00000001 (only bit 0 set)
    byte[] expectedBytes = new byte[] { (byte) 0x01 };

    // Verify the result
    assertArrayEquals(expectedBytes, resultBytes);
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void bitOpDiff1() {
    // Use single-byte values for simplicity
    byte[] key1 = new byte[] { (byte) 0x07 }; // 00000111 - bits 0,1,2 set
    byte[] key2 = new byte[] { (byte) 0x02 }; // 00000010 - bit 1 set
    byte[] key3 = new byte[] { (byte) 0x04 }; // 00000100 - bit 2 set
    String destKey = "{bitOp:key:}resultDiff1";

    // Set keys using byte arrays
    jedis.set("{bitOp:key:}1".getBytes(), key1);
    jedis.set("{bitOp:key:}2".getBytes(), key2);
    jedis.set("{bitOp:key:}3".getBytes(), key3);

    // DIFF1(key1, key2, key3) = NOT(key1) AND (key2 OR key3)
    // key1 = 00000111 (bits 0,1,2 set)
    // key2 = 00000010 (bit 1 set)
    // key3 = 00000100 (bit 2 set)
    // key2 OR key3 = 00000110 (bits 1,2 set)
    // NOT(key1) = 11111000 (all bits except 0,1,2 set)
    // NOT(key1) AND (key2 OR key3) = 00000000 (no bits set)
    jedis.bitop(BitOP.DIFF1, destKey, "{bitOp:key:}1", "{bitOp:key:}2", "{bitOp:key:}3");

    // Get result as bytes
    byte[] resultBytes = jedis.get(destKey).getBytes();

    // Expected result: 00000000 (no bits set)
    byte[] expectedBytes = new byte[] { (byte) 0x00 };

    // Verify the result
    assertArrayEquals(expectedBytes, resultBytes);
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void bitOpAndor() {
    // Use single-byte values for simplicity
    byte[] key1 = new byte[] { (byte) 0x07 }; // 00000111 - bits 0,1,2 set
    byte[] key2 = new byte[] { (byte) 0x02 }; // 00000010 - bit 1 set
    byte[] key3 = new byte[] { (byte) 0x04 }; // 00000100 - bit 2 set
    String destKey = "{bitOp:key:}resultAndor";

    // Set keys using byte arrays
    jedis.set("{bitOp:key:}1".getBytes(), key1);
    jedis.set("{bitOp:key:}2".getBytes(), key2);
    jedis.set("{bitOp:key:}3".getBytes(), key3);

    // ANDOR(key1, key2, key3) = key1 AND (key2 OR key3)
    // key1 = 00000111 (bits 0,1,2 set)
    // key2 = 00000010 (bit 1 set)
    // key3 = 00000100 (bit 2 set)
    // key2 OR key3 = 00000110 (bits 1,2 set)
    // key1 AND (key2 OR key3) = 00000110 (bits 1,2 set)
    jedis.bitop(BitOP.ANDOR, destKey, "{bitOp:key:}1", "{bitOp:key:}2", "{bitOp:key:}3");

    // Get result as bytes
    byte[] resultBytes = jedis.get(destKey).getBytes();

    // Expected result: 00000110 (bits 1,2 set)
    byte[] expectedBytes = new byte[] { (byte) 0x06 };

    // Verify the result
    assertArrayEquals(expectedBytes, resultBytes);
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void bitOpOne() {
    // Use single-byte values for simplicity
    byte[] key1 = new byte[] { (byte) 0x01 }; // 00000001 - bit 0 set
    byte[] key2 = new byte[] { (byte) 0x02 }; // 00000010 - bit 1 set
    byte[] key3 = new byte[] { (byte) 0x04 }; // 00000100 - bit 2 set
    String destKey = "{bitOp:key:}resultOne";

    // Set keys using byte arrays
    jedis.set("{bitOp:key:}1".getBytes(), key1);
    jedis.set("{bitOp:key:}2".getBytes(), key2);
    jedis.set("{bitOp:key:}3".getBytes(), key3);

    // ONE(key1, key2, key3) = bits set in exactly one of the inputs
    // key1 = 00000001 (bit 0 set)
    // key2 = 00000010 (bit 1 set)
    // key3 = 00000100 (bit 2 set)
    // Result = 00000111 (bits 0,1,2 set - each in exactly one input)
    jedis.bitop(BitOP.ONE, destKey, "{bitOp:key:}1", "{bitOp:key:}2", "{bitOp:key:}3");

    // Get result as bytes
    byte[] resultBytes = jedis.get(destKey).getBytes();

    // Expected result: 00000111 (bits 0,1,2 set)
    byte[] expectedBytes = new byte[] { (byte) 0x07 };

    // Verify the result
    assertArrayEquals(expectedBytes, resultBytes);
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void bitOpDiffBinary() {
    byte[] key1 = { (byte) 0x07 }; // 00000111 - bits 0,1,2 set
    byte[] key2 = { (byte) 0x02 }; // 00000010 - bit 1 set
    byte[] key3 = { (byte) 0x04 }; // 00000100 - bit 2 set
    byte[] dest = "{bitOp:key:}resultDiffBinary".getBytes();

    jedis.set("{bitOp:key:}1".getBytes(), key1);
    jedis.set("{bitOp:key:}2".getBytes(), key2);
    jedis.set("{bitOp:key:}3".getBytes(), key3);

    // DIFF(key1, key2, key3) = key1 AND NOT(key2 OR key3)
    // key1 = 00000111 (bits 0,1,2 set)
    // key2 = 00000010 (bit 1 set)
    // key3 = 00000100 (bit 2 set)
    // key2 OR key3 = 00000110 (bits 1,2 set)
    // NOT(key2 OR key3) = 11111001 (all bits except 1,2 set)
    // key1 AND NOT(key2 OR key3) = 00000001 (only bit 0 set)
    jedis.bitop(BitOP.DIFF, dest, "{bitOp:key:}1".getBytes(), "{bitOp:key:}2".getBytes(),
        "{bitOp:key:}3".getBytes());

    // Expected result: 00000001 (only bit 0 set)
    byte[] expectedBytes = new byte[] { (byte) 0x01 };

    // Verify the result
    assertArrayEquals(expectedBytes, jedis.get(dest));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void bitOpDiff1Binary() {
    byte[] key1 = { (byte) 0x07 }; // 00000111 - bits 0,1,2 set
    byte[] key2 = { (byte) 0x02 }; // 00000010 - bit 1 set
    byte[] key3 = { (byte) 0x04 }; // 00000100 - bit 2 set
    byte[] dest = "{bitOp:key:}resultDiff1Binary".getBytes();

    jedis.set("{bitOp:key:}1".getBytes(), key1);
    jedis.set("{bitOp:key:}2".getBytes(), key2);
    jedis.set("{bitOp:key:}3".getBytes(), key3);

    // DIFF1(key1, key2, key3) = NOT(key1) AND (key2 OR key3)
    // key1 = 00000111 (bits 0,1,2 set)
    // key2 = 00000010 (bit 1 set)
    // key3 = 00000100 (bit 2 set)
    // key2 OR key3 = 00000110 (bits 1,2 set)
    // NOT(key1) = 11111000 (all bits except 0,1,2 set)
    // NOT(key1) AND (key2 OR key3) = 00000000 (no bits set)
    jedis.bitop(BitOP.DIFF1, dest, "{bitOp:key:}1".getBytes(), "{bitOp:key:}2".getBytes(),
        "{bitOp:key:}3".getBytes());

    // Expected result: 00000000 (no bits set)
    byte[] expectedBytes = new byte[] { (byte) 0x00 };

    // Verify the result
    assertArrayEquals(expectedBytes, jedis.get(dest));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void bitOpAndorBinary() {
    byte[] key1 = { (byte) 0x07 }; // 00000111 - bits 0,1,2 set
    byte[] key2 = { (byte) 0x02 }; // 00000010 - bit 1 set
    byte[] key3 = { (byte) 0x04 }; // 00000100 - bit 2 set
    byte[] dest = "{bitOp:key:}resultAndorBinary".getBytes();

    jedis.set("{bitOp:key:}1".getBytes(), key1);
    jedis.set("{bitOp:key:}2".getBytes(), key2);
    jedis.set("{bitOp:key:}3".getBytes(), key3);

    // ANDOR(key1, key2, key3) = key1 AND (key2 OR key3)
    // key1 = 00000111 (bits 0,1,2 set)
    // key2 = 00000010 (bit 1 set)
    // key3 = 00000100 (bit 2 set)
    // key2 OR key3 = 00000110 (bits 1,2 set)
    // key1 AND (key2 OR key3) = 00000110 (bits 1,2 set)
    jedis.bitop(BitOP.ANDOR, dest, "{bitOp:key:}1".getBytes(), "{bitOp:key:}2".getBytes(),
        "{bitOp:key:}3".getBytes());

    // Expected result: 00000110 (bits 1,2 set)
    byte[] expectedBytes = new byte[] { (byte) 0x06 };

    // Verify the result
    assertArrayEquals(expectedBytes, jedis.get(dest));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void bitOpOneBinary() {
    byte[] key1 = { (byte) 0x01 }; // 00000001 - bit 0 set
    byte[] key2 = { (byte) 0x02 }; // 00000010 - bit 1 set
    byte[] key3 = { (byte) 0x04 }; // 00000100 - bit 2 set
    byte[] dest = "{bitOp:key:}resultOneBinary".getBytes();

    jedis.set("{bitOp:key:}1".getBytes(), key1);
    jedis.set("{bitOp:key:}2".getBytes(), key2);
    jedis.set("{bitOp:key:}3".getBytes(), key3);

    // ONE(key1, key2, key3) = bits set in exactly one of the inputs
    // key1 = 00000001 (bit 0 set)
    // key2 = 00000010 (bit 1 set)
    // key3 = 00000100 (bit 2 set)
    // Result = 00000111 (bits 0,1,2 set - each in exactly one input)
    jedis.bitop(BitOP.ONE, dest, "{bitOp:key:}1".getBytes(), "{bitOp:key:}2".getBytes(),
        "{bitOp:key:}3".getBytes());

    // Expected result: 00000111 (bits 0,1,2 set)
    byte[] expectedBytes = new byte[] { (byte) 0x07 };

    // Verify the result
    assertArrayEquals(expectedBytes, jedis.get(dest));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void bitOpDiffSingleSourceShouldFail() {
    assertThrows(JedisDataException.class,
        () -> jedis.bitop(BitOP.DIFF, "{bitOp:key:}dest", "{bitOp:key:}{bitOp:key:}src1"));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void bitOpDiff1SingleSourceShouldFail() {
    assertThrows(JedisDataException.class,
        () -> jedis.bitop(BitOP.DIFF1, "{bitOp:key:}dest", "{bitOp:key:}src1"));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void bitOpAndorSingleSourceShouldFail() {
    assertThrows(JedisDataException.class,
        () -> jedis.bitop(BitOP.ANDOR, "{bitOp:key:}dest", "{bitOp:key:}src1"));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void bitOpDiffBinarySingleSourceShouldFail() {
    byte[] dest = "{bitOp:key:}dest".getBytes();
    byte[] src1 = "{bitOp:key:}src1".getBytes();

    assertThrows(JedisDataException.class, () -> jedis.bitop(BitOP.DIFF, dest, src1));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void bitOpDiff1BinarySingleSourceShouldFail() {
    byte[] dest = "{bitOp:key:}dest".getBytes();
    byte[] src1 = "{bitOp:key:}src1".getBytes();

    assertThrows(JedisDataException.class, () -> jedis.bitop(BitOP.DIFF1, dest, src1));
  }

  @Test
  @SinceRedisVersion("8.1.240")
  public void bitOpAndorBinarySingleSourceShouldFail() {
    byte[] dest = "{bitOp:key:}dest".getBytes();
    byte[] src1 = "{bitOp:key:}src1".getBytes();

    assertThrows(JedisDataException.class, () -> jedis.bitop(BitOP.ANDOR, dest, src1));
  }

  // ---- BITRANGEINTARRAY ----

  private static final List<Long> FA_OFFSETS = Arrays.asList(0L, 7L, 8L, 15L, 16L, 23L, 24L, 31L);

  /** FB: bytes 0, 7 and 8 full, bits 8 and 15, bit 73. */
  private static final List<Long> FB_OFFSETS = concat(range(0, 8), longs(15), range(56, 71),
    longs(73));

  private static List<Long> longs(long... values) {
    List<Long> out = new ArrayList<>();
    for (long v : values) {
      out.add(v);
    }
    return out;
  }

  private static List<Long> range(long from, long to) {
    List<Long> out = new ArrayList<>();
    for (long v = from; v <= to; v++) {
      out.add(v);
    }
    return out;
  }

  @SafeVarargs
  private static List<Long> concat(List<Long>... parts) {
    List<Long> out = new ArrayList<>();
    for (List<Long> p : parts) {
      out.addAll(p);
    }
    return out;
  }

  private static List<Long> flatten(List<List<Long>> pages) {
    List<Long> out = new ArrayList<>();
    for (List<Long> p : pages) {
      out.addAll(p);
    }
    return out;
  }

  private void setBits(String key, List<Long> offsets) {
    for (long o : offsets) {
      jedis.setbit(key, o, true);
    }
  }

  private void writeFA(String key) {
    setBits(key, FA_OFFSETS);
  }

  private void writeFB(String key) {
    setBits(key, FB_OFFSETS);
  }

  private static BitRangeIntArrayParams params() {
    return BitRangeIntArrayParams.bitRangeIntArrayParams();
  }

  /** One row of the FA range table: start, end, unit (null for none) and the expected offsets. */
  private static final class FaRange {
    final long start;
    final long end;
    final BitCountOption unit;
    final List<Long> expected;

    FaRange(long start, long end, BitCountOption unit, List<Long> expected) {
      this.start = start;
      this.end = end;
      this.unit = unit;
      this.expected = expected;
    }

    @Override
    public String toString() {
      return start + " " + end + " " + unit;
    }
  }

  private static List<FaRange> faRanges() {
    List<FaRange> rows = new ArrayList<>();
    for (BitCountOption unit : new BitCountOption[] { null, BitCountOption.BYTE }) {
      rows.add(new FaRange(1, 1, unit, longs(8, 15)));
      rows.add(new FaRange(1, 2, unit, longs(8, 15, 16, 23)));
      rows.add(new FaRange(-1, -1, unit, longs(24, 31)));
      rows.add(new FaRange(-2, -1, unit, longs(16, 23, 24, 31)));
      rows.add(new FaRange(2, 99, unit, longs(16, 23, 24, 31)));
      rows.add(new FaRange(-99, 0, unit, longs(0, 7)));
      rows.add(new FaRange(-99, -98, unit, longs(0, 7)));
      rows.add(new FaRange(3, 1, unit, longs()));
      rows.add(new FaRange(4, 9, unit, longs()));
      rows.add(new FaRange(-1, -2, unit, longs()));
    }
    rows.add(new FaRange(0, 0, BitCountOption.BIT, longs(0)));
    rows.add(new FaRange(7, 8, BitCountOption.BIT, longs(7, 8)));
    rows.add(new FaRange(1, 6, BitCountOption.BIT, longs()));
    rows.add(new FaRange(9, 15, BitCountOption.BIT, longs(15)));
    rows.add(new FaRange(-8, -1, BitCountOption.BIT, longs(24, 31)));
    rows.add(new FaRange(30, 1000, BitCountOption.BIT, longs(31)));
    rows.add(new FaRange(32, 40, BitCountOption.BIT, longs()));
    return rows;
  }

  private List<Long> call(String key, FaRange r) {
    return r.unit == null ? jedis.bitrangeintarray(key, r.start, r.end)
        : jedis.bitrangeintarray(key, r.start, r.end, params().unit(r.unit));
  }

  private void assertFaRanges(String key) {
    for (FaRange r : faRanges()) {
      assertEquals(r.expected, call(key, r), r.toString());
    }
  }

  /** Pages through the whole value: in BIT units the next start is last + 1, in BYTE last / 8 + 1. */
  private List<List<Long>> pages(String key, BitCountOption unit, long count) {
    List<List<Long>> pages = new ArrayList<>();
    long start = 0;
    while (true) {
      List<Long> page = jedis.bitrangeintarray(key, start, -1, params().unit(unit).limit(count));
      pages.add(page);
      if (page.size() < count) {
        return pages;
      }
      long last = page.get(page.size() - 1);
      start = unit == BitCountOption.BIT ? last + 1 : last / 8 + 1;
    }
  }

  private void assertFbLimitCalls(String key) {
    assertEquals(range(0, 7), jedis.bitrangeintarray(key, 0, -1, params().limit(1)));
    assertEquals(concat(range(0, 8), longs(15)),
      jedis.bitrangeintarray(key, 0, -1, params().limit(9)));
    assertEquals(longs(8, 15), jedis.bitrangeintarray(key, 1, -1, params().limit(1)));
    assertEquals(range(0, 8),
      jedis.bitrangeintarray(key, 0, -1, params().unit(BitCountOption.BIT).limit(9)));
    assertEquals(longs(57, 58),
      jedis.bitrangeintarray(key, 57, -1, params().unit(BitCountOption.BIT).limit(2)));
  }

  private void assertFbBitPages(String key) {
    List<List<Long>> pages = pages(key, BitCountOption.BIT, 9);
    assertEquals(Arrays.asList(range(0, 8), concat(longs(15), range(56, 63)),
      concat(range(64, 71), longs(73)), longs()), pages);
    List<Long> joined = flatten(pages);
    assertEquals(FB_OFFSETS, joined);
    assertEquals(27, new HashSet<>(joined).size());
  }

  private void assertFbBytePages(String key) {
    List<List<Long>> pages = pages(key, BitCountOption.BYTE, 9);
    assertEquals(Arrays.asList(concat(range(0, 8), longs(15)), range(56, 71), longs(73)), pages);
    List<Long> joined = flatten(pages);
    assertEquals(FB_OFFSETS, joined);
    assertEquals(27, new HashSet<>(joined).size());
  }

  /** The range cases on FA, in BYTE units (with and without a unit) and BIT. */
  @Test
  @EnabledOnCommand("BITRANGEINTARRAY")
  public void bitrangeintarrayRanges() {
    writeFA("fa");
    assertFaRanges("fa");
  }

  /** For each FA range the reply's size equals BITCOUNT of the same range. */
  @Test
  @EnabledOnCommand("BITRANGEINTARRAY")
  public void bitrangeintarraySizeMatchesBitcount() {
    writeFA("fa");
    for (FaRange r : faRanges()) {
      long count = r.unit == null ? jedis.bitcount("fa", r.start, r.end)
          : jedis.bitcount("fa", r.start, r.end, r.unit);
      assertEquals(count, call("fa", r).size(), r.toString());
    }
  }

  /** A missing key, an empty string and LIMIT 0 reply an empty list, never null. */
  @Test
  @EnabledOnCommand("BITRANGEINTARRAY")
  public void bitrangeintarrayEmptyReplies() {
    assertEquals(longs(), jedis.bitrangeintarray("missing", 0, -1));

    jedis.set("empty", "");
    assertEquals(longs(), jedis.bitrangeintarray("empty", 0, -1));
    assertEquals(longs(),
      jedis.bitrangeintarray("empty", 0, -1, params().unit(BitCountOption.BIT).zeros()));

    writeFA("fa");
    assertEquals(longs(), jedis.bitrangeintarray("fa", 0, -1, params().limit(0)));
    assertEquals(longs(),
      jedis.bitrangeintarray("fa", 0, -1, params().unit(BitCountOption.BYTE).limit(0)));
    assertEquals(longs(),
      jedis.bitrangeintarray("fa", 0, -1, params().unit(BitCountOption.BIT).limit(0)));
  }

  /** ZEROS reports clear bits inside the value only, unlike BITPOS. */
  @Test
  @EnabledOnCommand("BITRANGEINTARRAY")
  public void bitrangeintarrayZerosInsideValue() {
    setBits("fz1", concat(range(0, 7), range(9, 14)));
    assertEquals(longs(8, 15), jedis.bitrangeintarray("fz1", 0, -1, params().zeros()));
    assertEquals(longs(8, 15), jedis.bitrangeintarray("fz1", 0, 99, params().zeros()));

    setBits("fz2", range(0, 15));
    assertEquals(longs(), jedis.bitrangeintarray("fz2", 0, -1, params().zeros()));
    assertEquals(16, jedis.bitpos("fz2", false));
  }

  /** LIMIT on FB in BYTE and BIT units. */
  @Test
  @EnabledOnCommand("BITRANGEINTARRAY")
  public void bitrangeintarrayLimit() {
    writeFB("fb");
    assertFbLimitCalls("fb");
  }

  /** In BYTE units a LIMIT page can hold more offsets than the count. */
  @Test
  @EnabledOnCommand("BITRANGEINTARRAY")
  public void bitrangeintarrayByteLimitCompletesByte() {
    writeFB("fb");
    List<Long> page = jedis.bitrangeintarray("fb", 0, -1, params().limit(9));
    assertEquals(10, page.size());
    assertEquals(concat(range(0, 8), longs(15)), page);
  }

  /** Paging FB in BIT units with count 9 returns every offset once. */
  @Test
  @EnabledOnCommand("BITRANGEINTARRAY")
  public void bitrangeintarrayPagingBit() {
    writeFB("fb");
    assertFbBitPages("fb");
  }

  /** Paging FB in BYTE units with count 9 returns every offset once. */
  @Test
  @EnabledOnCommand("BITRANGEINTARRAY")
  public void bitrangeintarrayPagingByte() {
    writeFB("fb");
    assertFbBytePages("fb");
  }

  /** ZEROS with LIMIT completes the byte in BYTE units and stops in BIT units. */
  @Test
  @EnabledOnCommand("BITRANGEINTARRAY")
  public void bitrangeintarrayZerosLimit() {
    writeFA("fa");
    assertEquals(concat(range(1, 6), range(9, 14)),
      jedis.bitrangeintarray("fa", 0, -1, params().limit(7).zeros()));
    assertEquals(concat(range(1, 6), longs(9)),
      jedis.bitrangeintarray("fa", 0, -1, params().unit(BitCountOption.BIT).limit(7).zeros()));
  }

  /** The widest 64-bit range covers the whole value, in BYTE and BIT units. */
  @Test
  @EnabledOnCommand("BITRANGEINTARRAY")
  public void bitrangeintarrayExtremeRange() {
    writeFA("fa");
    assertEquals(FA_OFFSETS, jedis.bitrangeintarray("fa", Long.MIN_VALUE, Long.MAX_VALUE,
      params().unit(BitCountOption.BYTE)));
    assertEquals(FA_OFFSETS, jedis.bitrangeintarray("fa", Long.MIN_VALUE, Long.MAX_VALUE,
      params().unit(BitCountOption.BIT)));
  }

  /** A count of Long.MAX_VALUE returns every offset, in BYTE and BIT units. */
  @Test
  @EnabledOnCommand("BITRANGEINTARRAY")
  public void bitrangeintarrayMaxLimit() {
    writeFA("fa");
    assertEquals(FA_OFFSETS, jedis.bitrangeintarray("fa", 0, -1,
      params().unit(BitCountOption.BYTE).limit(Long.MAX_VALUE)));
    assertEquals(FA_OFFSETS, jedis.bitrangeintarray("fa", 0, -1,
      params().unit(BitCountOption.BIT).limit(Long.MAX_VALUE)));
  }

  /** A key of the wrong type raises the server's WRONGTYPE error. */
  @Test
  @EnabledOnCommand("BITRANGEINTARRAY")
  public void bitrangeintarrayWrongType() {
    jedis.rpush("fl", "a");
    JedisDataException e = assertThrows(JedisDataException.class,
      () -> jedis.bitrangeintarray("fl", 0, -1));
    assertEquals("WRONGTYPE Operation against a key holding the wrong kind of value",
      e.getMessage());
  }

  /** The byte[] key forms, without and with params. */
  @Test
  @EnabledOnCommand("BITRANGEINTARRAY")
  public void bitrangeintarrayBinary() {
    byte[] key = SafeEncoder.encode("fa");
    for (long o : FA_OFFSETS) {
      jedis.setbit(key, o, true);
    }
    assertEquals(FA_OFFSETS, jedis.bitrangeintarray(key, 0, -1));
    assertEquals(longs(0, 7, 8),
      jedis.bitrangeintarray(key, 0, -1, params().unit(BitCountOption.BIT).limit(3)));
  }

  /** Unit, LIMIT and ZEROS together. */
  @Test
  @EnabledOnCommand("BITRANGEINTARRAY")
  public void bitrangeintarrayAllOptions() {
    writeFA("fa");
    assertEquals(longs(1, 2, 3),
      jedis.bitrangeintarray("fa", 0, -1, params().unit(BitCountOption.BIT).limit(3).zeros()));
  }

  /** The command leaves a string bitmap's type and length unchanged. */
  @Test
  @EnabledOnCommand("BITRANGEINTARRAY")
  public void bitrangeintarrayLeavesStringUnchanged() {
    writeFA("fa");
    jedis.bitrangeintarray("fa", 0, -1);
    assertEquals("string", jedis.type("fa"));
    assertEquals(4, jedis.strlen("fa"));
  }

}
