package com.googlecode.jedis;

import static com.googlecode.jedis.SortParams.newSortParams;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SortTest extends JedisTestBase {

    private final String lNumbers = "lNumbers";

    @BeforeMethod
    public void insertTestData() {
	final Random rand = new Random(23453L);

	for (int i = 0; i < 50; i++) {
	    jedis.lpush(lNumbers, String.valueOf(rand.nextDouble() * 100000.));
	}
    }

    @Test
    public void sort() {

	final List<String> sorted = jedis.sort(lNumbers);
	for (final Iterator<String> it = sorted.iterator(); it.hasNext();) {
	    assertThat(new Double(it.next()),
		    lessThanOrEqualTo(new Double(it.next())));
	}
    }

    @Test
    public void sortAlpha() {
	final Random rand = new Random(23453L);
	for (int i = 0; i < 50; i++) {
	    jedis.lpush("foo", Long.toString(rand.nextLong() * 100000L, 36));
	}

	final List<String> sorted = jedis.sort("foo", newSortParams().alpha());

	for (final Iterator<String> it = sorted.iterator(); it.hasNext();) {
	    assertThat(it.next(), lessThanOrEqualTo(it.next()));
	}
    }

    @Test
    public void sortBy() {
	jedis.lpush("foo", "2");
	jedis.lpush("foo", "3");
	jedis.lpush("foo", "1");

	jedis.set("bar1", "3");
	jedis.set("bar2", "2");
	jedis.set("bar3", "1");

	assertThat(jedis.sort("foo", newSortParams().by("bar*")),
		contains("3", "2", "1"));
    }

    @Test
    public void sortDesc() {
	final List<String> sorted = jedis
		.sort(lNumbers, newSortParams().desc());
	for (final Iterator<String> it = sorted.iterator(); it.hasNext();) {
	    assertThat(new Double(it.next()), greaterThanOrEqualTo(new Double(
		    it.next())));
	}
    }

    @Test
    public void sortGet() {
	jedis.lpush("foo", "8");
	jedis.lpush("foo", "3");
	jedis.lpush("foo", "4");

	jedis.set("bar3", "a");
	jedis.set("bar4", "b");
	jedis.set("bar8", "c");

	jedis.hset("car3", "x", "1");
	jedis.hset("car4", "x", "2");
	jedis.hset("car8", "x", "3");

	assertThat(jedis.sort("foo", newSortParams().get("bar*", "car*->x")),
		contains("a", "1", "b", "2", "c", "3"));

    }

    @Test
    public void sortLimit() {
	final List<String> sorted = jedis.sort(lNumbers,
		newSortParams().limit(0, 10));

	assertThat(sorted.size(), is(10));

	for (final Iterator<String> it = sorted.iterator(); it.hasNext();) {
	    assertThat(new Double(it.next()),
		    lessThanOrEqualTo(new Double(it.next())));
	}
    }

    @Test
    public void sortStore() {

	assertThat(jedis.sort(lNumbers, "dst"), is(50L));
	List<String> sorted = jedis.sort("dst");
	for (final Iterator<String> it = sorted.iterator(); it.hasNext();) {
	    assertThat(new Double(it.next()),
		    lessThanOrEqualTo(new Double(it.next())));
	}

	assertThat(jedis.sort(lNumbers, newSortParams().desc(), "dst"), is(50L));
	sorted = jedis.lrange("dst", 0, -1);
	for (final Iterator<String> it = sorted.iterator(); it.hasNext();) {
	    assertThat(new Double(it.next()), greaterThanOrEqualTo(new Double(
		    it.next())));
	}
    }

}