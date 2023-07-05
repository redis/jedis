package redis.clients.jedis.misc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import redis.clients.jedis.resps.Tuple;

public class TupleTest {

  @Test
  public void compareSameObject() {
    Tuple t1 = new Tuple("foo", 1d);
    assertTrue(t1.equals(t1));
  }

  @Test
  public void compareEqual() {
    Tuple t1 = new Tuple("foo", 1d);
    Tuple t2 = new Tuple("foo", 1d);

    assertEquals(0, t1.compareTo(t2));
    assertEquals(0, t2.compareTo(t1));
    assertTrue(t1.equals(t2));
    assertTrue(t2.equals(t1));
  }

  @Test
  public void compareSameScore() {
    Tuple t1 = new Tuple("foo", 1d);
    Tuple t2 = new Tuple("bar", 1d);

    assertEquals(1, t1.compareTo(t2));
    assertEquals(-1, t2.compareTo(t1));
    assertFalse(t1.equals(t2));
    assertFalse(t2.equals(t1));
  }

  @Test
  public void compareSameScoreObject() {
    Double score = 1d;
    Tuple t1 = new Tuple("foo", score);
    Tuple t2 = new Tuple("bar", score);

    assertEquals(1, t1.compareTo(t2));
    assertEquals(-1, t2.compareTo(t1));
    assertFalse(t1.equals(t2));
    assertFalse(t2.equals(t1));
  }

  @Test
  public void compareNoMatch() {
    Tuple t1 = new Tuple("foo", 1d);
    Tuple t2 = new Tuple("bar", 2d);

    assertEquals(-1, t1.compareTo(t2));
    assertEquals(1, t2.compareTo(t1));
    assertFalse(t1.equals(t2));
    assertFalse(t2.equals(t1));
  }

  @Test
  public void compareDifferentType() {
    Tuple t1 = new Tuple("foo", 1d);

    Object anyObject = new Object();
    assertFalse(t1.equals(anyObject));

    Object nullObject = null;
    assertFalse(t1.equals(nullObject));
  }

  @Test
  public void testToString() {
    Tuple t1 = new Tuple("key-name", 1d);
    String toStringResult = t1.toString();
    MatcherAssert.assertThat(toStringResult, Matchers.containsString("key-name"));
    MatcherAssert.assertThat(toStringResult, Matchers.containsString("1"));
  }

  @Test
  public void testSameElement() {
    Tuple t1 = new Tuple("user1", 10.0);
    Tuple t2 = new Tuple("user1", 5.0);

    // Intentionally skipping compareTo.
    assertFalse(t1.equals(t2));
    assertFalse(t2.equals(t1));

    HashSet<Tuple> hashSet = new HashSet<>();
    hashSet.add(t1);
    hashSet.add(t2);
    assertEquals(2, hashSet.size());
  }
}
