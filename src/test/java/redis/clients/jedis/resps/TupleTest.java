package redis.clients.jedis.resps;

import java.util.HashSet;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import redis.clients.jedis.resps.Tuple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class TupleTest {

  @Test
  public void compareEqual() {
    Tuple t1 = new Tuple("foo", 1d);
    Tuple t2 = new Tuple("foo", 1d);

    assertEquals(0, t1.compareTo(t2));
    assertEquals(0, t2.compareTo(t1));
    assertEquals(t1, t2); // directly calling Tuple.equals()
    assertEquals(t2, t1); // directly calling Tuple.equals()
  }

  @Test
  public void compareSameScore() {
    Tuple t1 = new Tuple("foo", 1d);
    Tuple t2 = new Tuple("bar", 1d);

    assertEquals(1, t1.compareTo(t2));
    assertEquals(-1, t2.compareTo(t1));
    assertNotEquals(t1, t2); // directly calling Tuple.equals()
    assertNotEquals(t2, t1); // directly calling Tuple.equals()
  }

  @Test
  public void compareSameScoreObject() {
    Double score = 1d;
    Tuple t1 = new Tuple("foo", score);
    Tuple t2 = new Tuple("bar", score);

    assertEquals(1, t1.compareTo(t2));
    assertEquals(-1, t2.compareTo(t1));
    assertNotEquals(t1, t2); // directly calling Tuple.equals()
    assertNotEquals(t2, t1); // directly calling Tuple.equals()
  }

  @Test
  public void compareNoMatch() {
    Tuple t1 = new Tuple("foo", 1d);
    Tuple t2 = new Tuple("bar", 2d);

    assertEquals(-1, t1.compareTo(t2));
    assertEquals(1, t2.compareTo(t1));
    assertNotEquals(t1, t2); // directly calling Tuple.equals()
    assertNotEquals(t2, t1); // directly calling Tuple.equals()
  }

  @Test
  public void compareSameObject() {
    Tuple t1 = new Tuple("foo", 1d);
    assertEquals(t1, t1); // directly calling Tuple.equals()
  }

  @Test
  public void compareDifferentType() {
    Tuple t1 = new Tuple("foo", 1d);
    assertNotEquals(t1, new Object()); // directly calling Tuple.equals()
    assertNotEquals(t1, null); // directly calling Tuple.equals()
  }

  @Test
  public void testToString() {
    Tuple t1 = new Tuple("key-name", 1d);
    String toStringResult = t1.toString();

    assertThat(toStringResult, CoreMatchers.containsString("key-name"));
    assertThat(toStringResult, CoreMatchers.containsString("1"));
  }

  @Test
  public void testSameElement() {
    Tuple t1 = new Tuple("user1", 10.0);
    Tuple t2 = new Tuple("user1", 5.0);

    // Intentionally skipping compareTo.
    assertNotEquals(t1, t2); // directly calling Tuple.equals()
    assertNotEquals(t2, t1); // directly calling Tuple.equals()

    HashSet<Tuple> hashSet = new HashSet<Tuple>();
    hashSet.add(t1);
    hashSet.add(t2);
    assertEquals(2, hashSet.size());
  }
}
