package redis.clients.jedis.tests.commands;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.util.SafeEncoder;

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
	String script = "return {redis.call('hget',KEYS[1],ARGV[1]),redis.call('hget',KEYS[2],ARGV[2])}";
	jedis.eval(script, 2, "key1", "key2", "1", "1");
    }

    @Test
    public void scriptEvalShaReturnNullValues() {
	String script = "return {redis.call('hget',KEYS[1],ARGV[1]),redis.call('hget',KEYS[2],ARGV[2])}";
	String sha = jedis.scriptLoad(script);
	jedis.evalsha(sha, 2, "key1", "key2", "1", "1");
    }
}