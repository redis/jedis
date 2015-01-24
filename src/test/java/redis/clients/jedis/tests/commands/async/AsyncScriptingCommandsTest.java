package redis.clients.jedis.tests.commands.async;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Test;
import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.tests.commands.JedisCommandTestBase;
import redis.clients.jedis.tests.commands.async.util.CommandWithWaiting;
import redis.clients.util.SafeEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;

public class AsyncScriptingCommandsTest extends AsyncJedisCommandTestBase {

  @SuppressWarnings("unchecked")
  @Test
  public void evalMultiBulk() {
    String script = "return {KEYS[1],KEYS[2],ARGV[1],ARGV[2],ARGV[3]}";
    List<String> keys = new ArrayList<String>();
    keys.add("key1");
    keys.add("key2");

    List<String> args = new ArrayList<String>();
    args.add("first");
    args.add("second");
    args.add("third");

    asyncJedis.eval(OBJECT_CALLBACK.withReset(), script, keys, args);
    List<String> response = (List<String>) OBJECT_CALLBACK.getResponseWithWaiting(1000);

    assertEquals(5, response.size());
    assertEquals("key1", response.get(0));
    assertEquals("key2", response.get(1));
    assertEquals("first", response.get(2));
    assertEquals("second", response.get(3));
    assertEquals("third", response.get(4));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void evalMultiBulkWithBinaryJedis() {
    String script = "return {KEYS[1],KEYS[2],ARGV[1],ARGV[2],ARGV[3]}";
    List<byte[]> keys = new ArrayList<byte[]>();
    keys.add("key1".getBytes());
    keys.add("key2".getBytes());

    List<byte[]> args = new ArrayList<byte[]>();
    args.add("first".getBytes());
    args.add("second".getBytes());
    args.add("third".getBytes());

    asyncJedis.eval(OBJECT_CALLBACK.withReset(), SafeEncoder.encode(script), keys, args);
    List<byte[]> responses = (List<byte[]>) OBJECT_CALLBACK.getResponseWithWaiting(1000);

    assertEquals(5, responses.size());
    assertEquals("key1", new String(responses.get(0)));
    assertEquals("key2", new String(responses.get(1)));
    assertEquals("first", new String(responses.get(2)));
    assertEquals("second", new String(responses.get(3)));
    assertEquals("third", new String(responses.get(4)));
  }

  @Test
  public void evalBulk() {
    String script = "return KEYS[1]";
    List<String> keys = new ArrayList<String>();
    keys.add("key1");

    List<String> args = new ArrayList<String>();
    args.add("first");

    asyncJedis.eval(OBJECT_CALLBACK.withReset(), script, keys, args);
    String response = (String) OBJECT_CALLBACK.getResponseWithWaiting(1000);

    assertEquals("key1", response);
  }

  @Test
  public void evalInt() {
    String script = "return 2";
    List<String> keys = new ArrayList<String>();
    keys.add("key1");

    asyncJedis.eval(OBJECT_CALLBACK.withReset(), script, keys, new ArrayList<String>());
    Long response = (Long) OBJECT_CALLBACK.getResponseWithWaiting(1000);

    assertEquals(new Long(2), response);
  }

  @Test
  public void evalNestedLists() {
    String script = "return { {KEYS[1]} , {2} }";

    asyncJedis.eval(OBJECT_CALLBACK.withReset(), script, 1, "key1");
    List<?> results = (List<?>) OBJECT_CALLBACK.getResponseWithWaiting(1000);

    assertThat((List<String>) results.get(0), listWithItem("key1"));
    assertThat((List<Long>) results.get(1), listWithItem(2L));
  }

  @Test
  public void evalNoArgs() {
    String script = "return KEYS[1]";
    List<String> keys = new ArrayList<String>();
    keys.add("key1");

    asyncJedis.eval(OBJECT_CALLBACK.withReset(), script, keys, new ArrayList<String>());
    String response = (String) OBJECT_CALLBACK.getResponseWithWaiting(1000);

    assertEquals("key1", response);
  }

  @Test
  public void evalsha() {
    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    asyncJedis.eval(OBJECT_CALLBACK.withReset(), "return redis.call('get','foo')");
    asyncJedis.evalsha(OBJECT_CALLBACK.withReset(), "6b1bf486c81ceb7edf3c093f4c48582e38c0e791");
    String result = (String) OBJECT_CALLBACK.getResponseWithWaiting(1000);

    assertEquals("bar", result);
  }

  @Test(expected = JedisDataException.class)
  public void evalshaShaNotFound() {
    asyncJedis.evalsha(OBJECT_CALLBACK.withReset(), "ffffffffffffffffffffffffffffffffffffffff");
    OBJECT_CALLBACK.getResponseWithWaiting(1000);
  }

  @Test
  public void scriptFlush() {
    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    asyncJedis.eval(OBJECT_CALLBACK.withReset(), "return redis.call('get','foo')");
    asyncJedis.scriptFlush(STRING_CALLBACK.withReset());
    asyncJedis.scriptExists(BOOLEAN_LIST_CALLBACK.withReset(),
      "6b1bf486c81ceb7edf3c093f4c48582e38c0e791");
    assertFalse(BOOLEAN_LIST_CALLBACK.getResponseWithWaiting(2000).get(0));
  }

  @Test
  public void scriptExists() {
    asyncJedis.scriptLoad(STRING_CALLBACK.withReset(), "return redis.call('get','foo')");
    asyncJedis.scriptExists(BOOLEAN_LIST_CALLBACK.withReset(),
      "ffffffffffffffffffffffffffffffffffffffff", "6b1bf486c81ceb7edf3c093f4c48582e38c0e791");
    List<Boolean> exists = BOOLEAN_LIST_CALLBACK.getResponseWithWaiting(1000);
    assertFalse(exists.get(0));
    assertTrue(exists.get(1));
  }

  @Test
  public void scriptExistsBinary() {
    asyncJedis.scriptLoad(BYTE_ARRAY_CALLBACK.withReset(),
      SafeEncoder.encode("return redis.call('get','foo')"));
    asyncJedis.scriptExists(BOOLEAN_LIST_CALLBACK.withReset(),
      SafeEncoder.encode("ffffffffffffffffffffffffffffffffffffffff"),
      SafeEncoder.encode("6b1bf486c81ceb7edf3c093f4c48582e38c0e791"));
    List<Boolean> exists = BOOLEAN_LIST_CALLBACK.getResponseWithWaiting(1000);
    assertFalse(exists.get(0));
    assertTrue(exists.get(1));
  }

  @Test
  public void scriptLoad() {
    asyncJedis.scriptLoad(STRING_CALLBACK.withReset(), "return redis.call('get','foo')");
    asyncJedis.scriptExists(BOOLEAN_LIST_CALLBACK.withReset(),
      "6b1bf486c81ceb7edf3c093f4c48582e38c0e791");
    assertTrue(BOOLEAN_LIST_CALLBACK.getResponseWithWaiting(1000).get(0));
  }

  @Test
  public void scriptLoadBinary() {
    asyncJedis.scriptLoad(BYTE_ARRAY_CALLBACK.withReset(),
      SafeEncoder.encode("return redis.call('get','foo')"));
    BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000);
    asyncJedis.scriptExists(BOOLEAN_LIST_CALLBACK.withReset(),
      "6b1bf486c81ceb7edf3c093f4c48582e38c0e791");
    assertTrue(BOOLEAN_LIST_CALLBACK.getResponseWithWaiting(1000).get(0));
  }

  @Test
  public void scriptKill() {
    try {
      asyncJedis.scriptKill(STRING_CALLBACK.withReset());
    } catch (JedisDataException e) {
      assertTrue(e.getMessage().contains("No scripts in execution right now."));
    }
  }

  @Test
  public void scriptEvalReturnNullValues() {
    String script = "return {KEYS[1],KEYS[2],ARGV[1],ARGV[2]}";
    asyncJedis.eval(OBJECT_CALLBACK.withReset(), script, 2, "key1", "key2", "1", "2");
    List<String> results = (List<String>) OBJECT_CALLBACK.getResponseWithWaiting(1000);
    assertEquals("key1", results.get(0));
    assertEquals("key2", results.get(1));
    assertEquals("1", results.get(2));
    assertEquals("2", results.get(3));
  }

  @Test
  public void scriptEvalShaReturnNullValues() {
    String script = "return {KEYS[1],KEYS[2],ARGV[1],ARGV[2]}";
    asyncJedis.scriptLoad(STRING_CALLBACK.withReset(), script);
    String sha = STRING_CALLBACK.getResponseWithWaiting(1000);
    asyncJedis.evalsha(OBJECT_CALLBACK.withReset(), sha, 2, "key1", "key2", "1", "2");
    List<String> results = (List<String>) OBJECT_CALLBACK.getResponseWithWaiting(1000);
    assertEquals("key1", results.get(0));
    assertEquals("key2", results.get(1));
    assertEquals("1", results.get(2));
    assertEquals("2", results.get(3));
  }

  private <T> Matcher<Iterable<? super T>> listWithItem(T expected) {
    return CoreMatchers.<T> hasItem(equalTo(expected));
  }
}
