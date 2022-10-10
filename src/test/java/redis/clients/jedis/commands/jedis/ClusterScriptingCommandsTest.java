package redis.clients.jedis.commands.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import redis.clients.jedis.args.FlushMode;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.exceptions.JedisDataException;

public class ClusterScriptingCommandsTest extends ClusterJedisCommandsTestBase {

  @Test(expected = JedisClusterOperationException.class)
  public void testJedisClusterException() {
    String script = "return {KEYS[1],KEYS[2],ARGV[1],ARGV[2],ARGV[3]}";
    List<String> keys = new ArrayList<>();
    keys.add("key1");
    keys.add("key2");
    List<String> args = new ArrayList<>();
    args.add("first");
    args.add("second");
    args.add("third");
    cluster.eval(script, keys, args);
  }

  @Test
  public void testEval2() {
    String script = "return redis.call('set',KEYS[1],'bar')";
    int numKeys = 1;
    String[] args = { "foo" };
    cluster.eval(script, numKeys, args);
    assertEquals("bar", cluster.get("foo"));
  }

  @Test
  public void testScriptLoadAndScriptExists() {
    String sha1 = cluster.scriptLoad("return redis.call('get','foo')", "key1");
    assertTrue(cluster.scriptExists(sha1, "key1"));
  }

  @Test
  public void testEvalsha() {
    String sha1 = cluster.scriptLoad("return 10", "key1");
    Object o = cluster.evalsha(sha1, 1, "key1");
    assertEquals("10", o.toString());
  }

  @Test(expected = JedisClusterOperationException.class)
  public void testJedisClusterException2() {
    byte[] script = "return {KEYS[1],KEYS[2],ARGV[1],ARGV[2],ARGV[3]}".getBytes();
    List<byte[]> keys = new ArrayList<byte[]>();
    keys.add("key1".getBytes());
    keys.add("key2".getBytes());
    List<byte[]> args = new ArrayList<byte[]>();
    args.add("first".getBytes());
    args.add("second".getBytes());
    args.add("third".getBytes());
    cluster.eval(script, keys, args);
  }

  @Test
  public void testBinaryEval() {
    byte[] script = "return redis.call('set',KEYS[1],'bar')".getBytes();
    byte[] args = "foo".getBytes();
    cluster.eval(script, 1, args);
    assertEquals("bar", cluster.get("foo"));
  }

  @Test
  public void testBinaryScriptFlush() {
    byte[] byteKey = "key1".getBytes();
    cluster.scriptLoad("return redis.call('get','foo')".getBytes(), byteKey);
    assertEquals("OK", cluster.scriptFlush(byteKey));
    assertEquals("OK", cluster.scriptFlush(byteKey, FlushMode.SYNC));
  }

  @Test(expected = JedisDataException.class)
  public void testBinaryScriptKill() {
    byte[] byteKey = "key1".getBytes();
    cluster.scriptKill(byteKey);
  }

  @Test
  public void testBinaryScriptExists() {
    byte[] byteKey = "key1".getBytes();
    byte[] sha1 = cluster.scriptLoad("return redis.call('get','foo')".getBytes(), byteKey);
    byte[][] arraySha1 = { sha1 };
    assertEquals(Collections.singletonList(Boolean.TRUE), cluster.scriptExists(byteKey, arraySha1));
  }
}
