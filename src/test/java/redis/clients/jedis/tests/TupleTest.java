package redis.clients.jedis.tests;

import org.junit.Test;

import redis.clients.jedis.Tuple;

import static org.junit.Assert.assertEquals;

/**
 * @author Antonio Tomac <antonio.tomac@mediatoolkit.com>
 */
public class TupleTest {

  @Test
  public void tupleCompare() {
    Tuple t1 = new Tuple("foo", 1d);
    Tuple t2 = new Tuple("bar", 2d);

    assertEquals(-1, t1.compareTo(t2));
    assertEquals(1, t2.compareTo(t1));
    assertEquals(0, t2.compareTo(t2));
  }

  @Test
  public void testCompareTo() {
    Tuple t1 = new Tuple("foo", 1.0);
    Tuple t2 = new Tuple("bar", 1.0);
    Tuple t3 = new Tuple("foo", 10.0);

    assertEquals(0, t1.compareTo(t2));
    assertEquals(0, t2.compareTo(t1));

    assertEquals(0, t1.compareTo(t3));
    assertEquals(0, t3.compareTo(t1));
  }

}
