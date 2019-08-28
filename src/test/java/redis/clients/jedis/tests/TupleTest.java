package redis.clients.jedis.tests;

import org.junit.Test;
import redis.clients.jedis.Tuple;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TupleTest {

    @Test
    public void compareEqual() {
        Tuple t1 = new Tuple("foo", 1d);
        Tuple t2 = new Tuple("foo", 1d);

        assertEquals(0, t1.compareTo(t2));
        assertEquals(0, t2.compareTo(t1));
        assertEquals(t1, t2);
        assertEquals(t2, t1);
    }

    @Test
    public void compareSameScore() {
        Tuple t1 = new Tuple("foo", 1d);
        Tuple t2 = new Tuple("bar", 1d);

        assertEquals(1, t1.compareTo(t2));
        assertEquals(-1, t2.compareTo(t1));
        assertNotEquals(t1, t2);
        assertNotEquals(t2, t1);
    }

    @Test
    public void compareSameScoreObject() {
        Double score = 1d;
        Tuple t1 = new Tuple("foo", score);
        Tuple t2 = new Tuple("bar", score);

        assertEquals(1, t1.compareTo(t2));
        assertEquals(-1, t2.compareTo(t1));
        assertNotEquals(t1, t2);
        assertNotEquals(t2, t1);
    }

    @Test
    public void compareNoMatch() {
        Tuple t1 = new Tuple("foo", 1d);
        Tuple t2 = new Tuple("bar", 2d);

        assertEquals(-1, t1.compareTo(t2));
        assertEquals(1, t2.compareTo(t1));
        assertNotEquals(t1, t2);
        assertNotEquals(t2, t1);
    }

    @Test
    public void testSameElement() {
        Tuple t1 = new Tuple("user1", 10.0);
        Tuple t2 = new Tuple("user1", 5.0);

        // Intentionally skipping compareTo.
        assertNotEquals(t1, t2);
        assertNotEquals(t2, t1);

        Set<Tuple> hashSet = new HashSet<>();
        hashSet.add(t1);
        hashSet.add(t2);
        assertEquals(2, hashSet.size());
    }
}
