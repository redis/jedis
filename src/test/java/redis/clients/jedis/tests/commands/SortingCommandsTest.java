package redis.clients.jedis.tests.commands;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisException;
import redis.clients.jedis.SortingParams;

public class SortingCommandsTest extends Assert {
    private Jedis jedis;

    @Before
    public void setUp() throws Exception {
	jedis = new Jedis("localhost");
	jedis.connect();
	jedis.flushDB();
    }

    @After
    public void tearDown() throws Exception {
	jedis.flushDB();
	jedis.disconnect();
    }

    @Test
    public void sort() throws JedisException {
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
    public void sortBy() throws JedisException {
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
    public void sortDesc() throws JedisException {
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
    public void sortLimit() throws JedisException {
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
    public void sortAlpha() throws JedisException {
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
}