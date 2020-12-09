package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import redis.clients.jedis.BitOP;
import redis.clients.jedis.BitPosParams;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.SafeEncoder;

import java.util.List;

public class BitCommandsTest extends JedisCommandTestBase {
  @Test
  public void setAndgetbit() {
    boolean bit = jedis.setbit("foo", 0, true);
    assertEquals(false, bit);

    bit = jedis.getbit("foo", 0);
    assertEquals(true, bit);

    boolean bbit = jedis.setbit("bfoo".getBytes(), 0, "1".getBytes());
    assertFalse(bbit);

    bbit = jedis.getbit("bfoo".getBytes(), 0);
    assertTrue(bbit);
  }

  @Test
  public void bitpos() {
    String foo = "foo";

    jedis.set(foo, String.valueOf(0));

    jedis.setbit(foo, 3, true);
    jedis.setbit(foo, 7, true);
    jedis.setbit(foo, 13, true);
    jedis.setbit(foo, 39, true);

    /*
     * byte: 0 1 2 3 4 bit: 00010001 / 00000100 / 00000000 / 00000000 / 00000001
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

    jedis.setbit(bfoo, 3, true);
    jedis.setbit(bfoo, 7, true);
    jedis.setbit(bfoo, 13, true);
    jedis.setbit(bfoo, 39, true);

    /*
     * byte: 0 1 2 3 4 bit: 00010001 / 00000100 / 00000000 / 00000000 / 00000001
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
     * byte: 0 bit: 11111111
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
     * byte: 0 1 2 3 4 bit: 11111111 / 11111111 / 11111111 / 11111111 / 11111111
     */
    long offset = jedis.bitpos(foo, false, new BitPosParams(2, 3));
    // offset should be -1
    assertEquals(-1, offset);
  }

  @Test
  public void setAndgetrange() {
    jedis.set("key1", "Hello World");
    long reply = jedis.setrange("key1", 6, "Jedis");
    assertEquals(11, reply);

    assertEquals("Hello Jedis", jedis.get("key1"));

    assertEquals("Hello", jedis.getrange("key1", 0, 4));
    assertEquals("Jedis", jedis.getrange("key1", 6, 11));
  }

  @Test
  public void bitCount() {
    jedis.setbit("foo", 16, true);
    jedis.setbit("foo", 24, true);
    jedis.setbit("foo", 40, true);
    jedis.setbit("foo", 56, true);

    long c4 = jedis.bitcount("foo");
    assertEquals(4, c4);

    long c3 = jedis.bitcount("foo", 2L, 5L);
    assertEquals(3, c3);
  }

  @Test
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
  public void bitOpNot() {
    jedis.setbit("key", 0, true);
    jedis.setbit("key", 4, true);

    jedis.bitop(BitOP.NOT, "resultNot", "key");

    String resultNot = jedis.get("resultNot");
    assertEquals("\u0077", resultNot);
  }

  @Test(expected = redis.clients.jedis.exceptions.JedisDataException.class)
  public void bitOpNotMultiSourceShouldFail() {
    jedis.bitop(BitOP.NOT, "dest", "src1", "src2");
  }

  @Test
  public void testBitfield() {
    List<Long> responses = jedis.bitfield("mykey", "INCRBY","i5","100","1", "GET", "u4", "0");
    assertEquals(1L, responses.get(0).longValue());
    assertEquals(0L, responses.get(1).longValue());
  }

  @Test
  public void testBitfieldReadonly() {
    List<Long> responses = jedis.bitfield("mykey", "INCRBY","i5","100","1", "GET", "u4", "0");
    assertEquals(1L, responses.get(0).longValue());
    assertEquals(0L, responses.get(1).longValue());

    List<Long> responses2 = jedis.bitfieldReadonly("mykey", "GET", "i5", "100");
    assertEquals(1L, responses2.get(0).longValue());
    
    try {
      jedis.bitfieldReadonly("mykey", "INCRBY","i5","100","1", "GET", "u4", "0");
      fail("Readonly command shouldn't allow INCRBY");
    }catch(JedisDataException e) {}
  }

  @Test
  public void testBinaryBitfield() {
    List<Long> responses = jedis.bitfield(SafeEncoder.encode("mykey"), SafeEncoder.encode("INCRBY"),
            SafeEncoder.encode("i5"), SafeEncoder.encode("100"), SafeEncoder.encode("1"),
            SafeEncoder.encode("GET"), SafeEncoder.encode("u4"), SafeEncoder.encode("0")
    );
    assertEquals(1L, responses.get(0).longValue());
    assertEquals(0L, responses.get(1).longValue());
  }

  @Test
  public void testBinaryBitfieldReadonly() {
    List<Long> responses = jedis.bitfield("mykey", "INCRBY","i5","100","1", "GET", "u4", "0");
    assertEquals(1L, responses.get(0).longValue());
    assertEquals(0L, responses.get(1).longValue());

    List<Long> responses2 = jedis.bitfieldReadonly(SafeEncoder.encode("mykey"), SafeEncoder.encode("GET"),
        SafeEncoder.encode("i5"), SafeEncoder.encode("100"));
    assertEquals(1L, responses2.get(0).longValue());
  }

}
