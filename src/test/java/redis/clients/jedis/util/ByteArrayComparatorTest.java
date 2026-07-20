package redis.clients.jedis.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ByteArrayComparatorTest {

  @Test
  public void test() {
    byte[] foo = SafeEncoder.encode("foo");
    byte[] foo2 = SafeEncoder.encode("foo");
    byte[] bar = SafeEncoder.encode("bar");

    assertTrue(ByteArrayComparator.compare(foo, foo2) == 0);
    assertTrue(ByteArrayComparator.compare(foo, bar) > 0);
    assertTrue(ByteArrayComparator.compare(bar, foo) < 0);
  }

  @Test
  public void testPrefix() {
    byte[] foo = SafeEncoder.encode("foo");
    byte[] fooo = SafeEncoder.encode("fooo");
    assertTrue(ByteArrayComparator.compare(foo, fooo) < 0);
    assertTrue(ByteArrayComparator.compare(fooo, foo) > 0);
  }

  @Test
  public void testHighBitBytesOrderUnsigned() {
    // Bytes must order as unsigned (0..255) to match Redis' own byte ordering.
    byte[] low = { (byte) 0x7F }; // 127
    byte[] high = { (byte) 0x80 }; // 128 unsigned
    assertTrue(ByteArrayComparator.compare(high, low) > 0);
    assertTrue(ByteArrayComparator.compare(low, high) < 0);

    // ASCII 'e' (0x65) sorts before the UTF-8 bytes of U+00E9 (0xC3 0xA9).
    byte[] e = { (byte) 0x65 };
    byte[] eAcute = { (byte) 0xC3, (byte) 0xA9 };
    assertTrue(ByteArrayComparator.compare(e, eAcute) < 0);
    assertTrue(ByteArrayComparator.compare(eAcute, e) > 0);
  }
}
