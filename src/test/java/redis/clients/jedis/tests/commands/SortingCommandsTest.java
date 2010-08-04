package redis.clients.jedis.tests.commands;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import redis.clients.jedis.SortingParams;

public class SortingCommandsTest extends JedisCommandTestBase {
    @Test
    public void sort() {
	jedis.lpush("foo", "3");
	jedis.lpush("foo", "2");
	jedis.lpush("foo", "1");

	List<String> result = jedis.sort("foo");

	List<String> expected = new ArrayList<String>();
	expected.add("1");
	expected.add("2");
	expected.add("3");

	assertEquals(expected, result);
    }

    @Test
    public void sortBy() {
	jedis.lpush("foo", "2");
	jedis.lpush("foo", "3");
	jedis.lpush("foo", "1");

	jedis.set("bar1", "3");
	jedis.set("bar2", "2");
	jedis.set("bar3", "1");

	SortingParams sp = new SortingParams();
	sp.by("bar*");

	List<String> result = jedis.sort("foo", sp);

	List<String> expected = new ArrayList<String>();
	expected.add("3");
	expected.add("2");
	expected.add("1");

	assertEquals(expected, result);
    }

    @Test
    public void sortDesc() {
	jedis.lpush("foo", "3");
	jedis.lpush("foo", "2");
	jedis.lpush("foo", "1");

	SortingParams sp = new SortingParams();
	sp.desc();

	List<String> result = jedis.sort("foo", sp);

	List<String> expected = new ArrayList<String>();
	expected.add("3");
	expected.add("2");
	expected.add("1");

	assertEquals(expected, result);
    }

    @Test
    public void sortLimit() {
	for (int n = 10; n > 0; n--) {
	    jedis.lpush("foo", String.valueOf(n));
	}

	SortingParams sp = new SortingParams();
	sp.limit(0, 3);

	List<String> result = jedis.sort("foo", sp);

	List<String> expected = new ArrayList<String>();
	expected.add("1");
	expected.add("2");
	expected.add("3");

	assertEquals(expected, result);
    }

    @Test
    public void sortAlpha() {
	jedis.lpush("foo", "1");
	jedis.lpush("foo", "2");
	jedis.lpush("foo", "10");

	SortingParams sp = new SortingParams();
	sp.alpha();

	List<String> result = jedis.sort("foo", sp);

	List<String> expected = new ArrayList<String>();
	expected.add("1");
	expected.add("10");
	expected.add("2");

	assertEquals(expected, result);
    }

    @Test
    public void sortGet() {
	jedis.lpush("foo", "1");
	jedis.lpush("foo", "2");
	jedis.lpush("foo", "10");

	jedis.set("bar1", "bar1");
	jedis.set("bar2", "bar2");
	jedis.set("bar10", "bar10");

	jedis.set("car1", "car1");
	jedis.set("car2", "car2");
	jedis.set("car10", "car10");

	SortingParams sp = new SortingParams();
	sp.get("car*", "bar*");

	List<String> result = jedis.sort("foo", sp);

	List<String> expected = new ArrayList<String>();
	expected.add("car1");
	expected.add("bar1");
	expected.add("car2");
	expected.add("bar2");
	expected.add("car10");
	expected.add("bar10");

	assertEquals(expected, result);
    }

    @Test
    public void sortStore() {
	jedis.lpush("foo", "1");
	jedis.lpush("foo", "2");
	jedis.lpush("foo", "10");

	int result = jedis.sort("foo", "result");

	List<String> expected = new ArrayList<String>();
	expected.add("1");
	expected.add("2");
	expected.add("10");

	assertEquals(3, result);
	assertEquals(expected, jedis.lrange("result", 0, 1000));
    }

}