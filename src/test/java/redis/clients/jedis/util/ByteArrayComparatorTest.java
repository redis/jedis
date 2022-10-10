package redis.clients.jedis.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
}
