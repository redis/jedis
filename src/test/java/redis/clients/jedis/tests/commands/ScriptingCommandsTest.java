package redis.clients.jedis.tests.commands;

import org.hamcrest.CoreMatchers;
import org.hamcrest.core.CombinableMatcher;
import org.junit.Test;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.util.SafeEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;

public class ScriptingCommandsTest extends JedisCommandTestBase {

    @SuppressWarnings("unchecked")
    @Test
    public void evalMultiBulk() {
	String script = "return {KEYS[1],KEYS[2],ARGV[1],ARGV[2]}";
	List<String> keys = new ArrayList<String>();
	keys.add("key1");
	keys.add("key2");

	List<String> args = new ArrayList<String>();
	args.add("first");
	args.add("second");

	List<String> response = (List<String>) jedis.eval(script, keys, args);

	assertEquals(4, response.size());
	assertEquals("key1", response.get(0));
	assertEquals("key2", response.get(1));
	assertEquals("first", response.get(2));
	assertEquals("second", response.get(3));
    }

    @Test
    public void evalBulk() {
	String script = "return KEYS[1]";
	List<String> keys = new ArrayList<String>();
	keys.add("key1");

	List<String> args = new ArrayList<String>();
	args.add("first");

	String response = (String) jedis.eval(script, keys, args);

	assertEquals("key1", response);
    }

    @Test
    public void evalInt() {
	String script = "return 2";
	List<String> keys = new ArrayList<String>();
	keys.add("key1");

	Long response = (Long) jedis
		.eval(script, keys, new ArrayList<String>());

	assertEquals(new Long(2), response);
    }

    @Test
    public void evalNestedLists() {
	String script = "return { {KEYS[1]} , {2} }";
	List<?> results = (List<?>) jedis.eval(script, 1, "key1");

	assertThat((List<String>) results.get(0), listWithItem("key1"));
	assertThat((List<Long>) results.get(1), listWithItem(2L));
    }

    @Test
    public void evalNoArgs() {
	String script = "return KEYS[1]";
	List<String> keys = new ArrayList<String>();
	keys.add("key1");
	String response = (String) jedis.eval(script, keys,
		new ArrayList<String>());

	assertEquals("key1", response);
    }

    @Test
    public void evalsha() {
	jedis.set("foo", "bar");
	jedis.eval("return redis.call('get','foo')");
	String result = (String) jedis
		.evalsha("6b1bf486c81ceb7edf3c093f4c48582e38c0e791");

	assertEquals("bar", result);
    }

    @Test(expected = JedisDataException.class)
    public void evalshaShaNotFound() {
	jedis.evalsha("ffffffffffffffffffffffffffffffffffffffff");
    }

    @Test
    public void scriptFlush() {
	jedis.set("foo", "bar");
	jedis.eval("return redis.call('get','foo')");
	jedis.scriptFlush();
	assertFalse(jedis
		.scriptExists("6b1bf486c81ceb7edf3c093f4c48582e38c0e791"));
    }

    @Test
    public void scriptExists() {
	jedis.scriptLoad("return redis.call('get','foo')");
	List<Boolean> exists = jedis.scriptExists(
		"ffffffffffffffffffffffffffffffffffffffff",
		"6b1bf486c81ceb7edf3c093f4c48582e38c0e791");
	assertFalse(exists.get(0));
	assertTrue(exists.get(1));
    }

    @Test
    public void scriptExistsBinary() {
	jedis.scriptLoad(SafeEncoder.encode("return redis.call('get','foo')"));
	List<Long> exists = jedis.scriptExists(
		SafeEncoder.encode("ffffffffffffffffffffffffffffffffffffffff"),
		SafeEncoder.encode("6b1bf486c81ceb7edf3c093f4c48582e38c0e791"));
	assertEquals(new Long(0), exists.get(0));
	assertEquals(new Long(1), exists.get(1));
    }

    @Test
    public void scriptLoad() {
	jedis.scriptLoad("return redis.call('get','foo')");
	assertTrue(jedis
		.scriptExists("6b1bf486c81ceb7edf3c093f4c48582e38c0e791"));
    }

    @Test
    public void scriptLoadBinary() {
	jedis.scriptLoad(SafeEncoder.encode("return redis.call('get','foo')"));
	List<Long> exists = jedis.scriptExists(SafeEncoder
		.encode("6b1bf486c81ceb7edf3c093f4c48582e38c0e791"));
	assertEquals(new Long(1), exists.get(0));
    }

    @Test
    public void scriptKill() {
	try {
	    jedis.scriptKill();
	} catch (JedisDataException e) {
	    assertTrue(e.getMessage().contains(
		    "No scripts in execution right now."));
	}
    }

    @Test
    public void scriptEvalReturnNullValues() {
	String script = "return {KEYS[1],KEYS[2],ARGV[1],ARGV[2]}";
	List<String> results = (List<String>) jedis.eval(script, 2, "key1", "key2", "1", "2");
	assertEquals("key1", results.get(0));
	assertEquals("key2", results.get(1));
	assertEquals("1", results.get(2));
	assertEquals("2", results.get(3));
    }

    @Test
    public void scriptEvalShaReturnNullValues() {
	String script = "return {KEYS[1],KEYS[2],ARGV[1],ARGV[2]}";
	String sha = jedis.scriptLoad(script);
	List<String> results = (List<String>) jedis.evalsha(sha, 2, "key1", "key2", "1", "2");
	assertEquals("key1", results.get(0));
	assertEquals("key2", results.get(1));
	assertEquals("1", results.get(2));
	assertEquals("2", results.get(3));
    }

    private <T> CombinableMatcher<List<T>> listWithItem(T expected) {
	return both(CoreMatchers.<List<T>>instanceOf(List.class)).and(hasItem(equalTo(expected)));
    }
}

