package redis.clients.jedis.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import org.junit.Test;

import redis.clients.jedis.Tuple;

public class TupleTest {

  @Test
  public void compareEqual() {
    Tuple t1 = new Tuple("foo", 1d);
    Tuple t2 = new Tuple("foo", 1d);

    assertEquals(0, t1.compareTo(t2));
    assertEquals(0, t2.compareTo(t1));
    assertTrue(t1.equals(t2)); // directly calling Tuple.equals()
    assertTrue(t2.equals(t1)); // directly calling Tuple.equals()
  }

  @Test
  public void compareSameScore() {
    Tuple t1 = new Tuple("foo", 1d);
    Tuple t2 = new Tuple("bar", 1d);

    assertEquals(1, t1.compareTo(t2));
    assertEquals(-1, t2.compareTo(t1));
    assertFalse(t1.equals(t2)); // directly calling Tuple.equals()
    assertFalse(t2.equals(t1)); // directly calling Tuple.equals()
  }

  @Test
  public void compareSameScoreObject() {
    Double score = 1d;
    Tuple t1 = new Tuple("foo", score);
    Tuple t2 = new Tuple("bar", score);

    assertEquals(1, t1.compareTo(t2));
    assertEquals(-1, t2.compareTo(t1));
    assertFalse(t1.equals(t2)); // directly calling Tuple.equals()
    assertFalse(t2.equals(t1)); // directly calling Tuple.equals()
  }

  @Test
  public void compareNoMatch() {
    Tuple t1 = new Tuple("foo", 1d);
    Tuple t2 = new Tuple("bar", 2d);

    assertEquals(-1, t1.compareTo(t2));
    assertEquals(1, t2.compareTo(t1));
    assertFalse(t1.equals(t2)); // directly calling Tuple.equals()
    assertFalse(t2.equals(t1)); // directly calling Tuple.equals()
  }

  @Test
  public void testSameElement() {
    Tuple t1 = new Tuple("user1", 10.0);
    Tuple t2 = new Tuple("user1", 5.0);

    // Intentionally skipping compareTo.
    assertFalse(t1.equals(t2)); // directly calling Tuple.equals()
    assertFalse(t2.equals(t1)); // directly calling Tuple.equals()

    HashSet<Tuple> hashSet = new HashSet<Tuple>();
    hashSet.add(t1);
    hashSet.add(t2);
    assertEquals(2, hashSet.size());
  }
}
