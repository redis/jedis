package redis.clients.jedis.tests.utils;

import org.junit.Test;
import redis.clients.util.ByteArrayComparator;

import redis.clients.util.SafeEncoder;

import static org.junit.Assert.assertEquals;

public class ByteArrayComparatorTest {

  @Test
  public void test() {
    byte[] foo = SafeEncoder.encode("foo");
    byte[] foo2 = SafeEncoder.encode("foo");
    byte[] bar = SafeEncoder.encode("bar");

    assertEquals(0, ByteArrayComparator.compare(foo, foo2));
    assertEquals(1, ByteArrayComparator.compare(foo, bar));
    assertEquals(-1, ByteArrayComparator.compare(bar, foo));
  }
}
