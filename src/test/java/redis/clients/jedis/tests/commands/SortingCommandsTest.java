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
	jedis.lpush("foo", "3");
	jedis.lpush("foo", "2");
	jedis.lpush("foo", "1");

	SortingParams sp = new SortingParams();
	sp.by("bar_*");

	List<String> result = jedis.sort("foo", sp);

	List<String> expected = new ArrayList<String>();
	expected.add("foo_1");
	expected.add("foo_2");
	expected.add("foo_3");

	assertEquals(expected, result);
    }
}