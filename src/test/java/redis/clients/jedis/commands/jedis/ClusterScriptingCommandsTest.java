package redis.clients.jedis.commands.jedis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.redis.test.annotations.SinceRedisVersion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.function.Executable;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.args.FlushMode;
import redis.clients.jedis.exceptions.JedisBroadcastException;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.SafeEncoder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Tag("integration")
public class ClusterScriptingCommandsTest extends ClusterJedisCommandsTestBase {

  @Test
  public void testJedisClusterException() {
    String script = "return {KEYS[1],KEYS[2],ARGV[1],ARGV[2],ARGV[3]}";
    List<String> keys = new ArrayList<>();
    keys.add("key1");
    keys.add("key2");
    List<String> args = new ArrayList<>();
    args.add("first");
    args.add("second");
    args.add("third");

    assertThrows(JedisClusterOperationException.class, () -> {
      cluster.eval(script, keys, args);
    });
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

  @Test
  public void testJedisClusterException2() {
    byte[] script = "return {KEYS[1],KEYS[2],ARGV[1],ARGV[2],ARGV[3]}".getBytes();
    List<byte[]> keys = new ArrayList<byte[]>();
    keys.add("key1".getBytes());
    keys.add("key2".getBytes());
    List<byte[]> args = new ArrayList<byte[]>();
    args.add("first".getBytes());
    args.add("second".getBytes());
    args.add("third".getBytes());

    assertThrows( JedisClusterOperationException.class, ()-> cluster.eval(script, keys, args));

  }

  @Test
  public void testBinaryEval() {
    byte[] script = "return redis.call('set',KEYS[1],'bar')".getBytes();
    byte[] args = "foo".getBytes();
    cluster.eval(script, 1, args);
    assertEquals("bar", cluster.get("foo"));
  }

  @Test
  public void evalVarargsCrossSlotKeys() {
    // "key1" and "key2" hash to different slots; rejected client-side because the first keyCount
    // varargs are registered for slot computation
    String script = "return {KEYS[1],KEYS[2]}";
    String sha1 = cluster.scriptLoad(script, "key1");
    byte[] bscript = SafeEncoder.encode(script);
    byte[] bsha1 = SafeEncoder.encode(sha1);
    byte[] bkey1 = SafeEncoder.encode("key1");
    byte[] bkey2 = SafeEncoder.encode("key2");

    assertCrossSlotRejected(() -> cluster.eval(script, 2, "key1", "key2"));
    assertCrossSlotRejected(() -> cluster.eval(bscript, 2, bkey1, bkey2));
    assertCrossSlotRejected(() -> cluster.evalsha(sha1, 2, "key1", "key2"));
    assertCrossSlotRejected(() -> cluster.evalsha(bsha1, 2, bkey1, bkey2));
  }

  private static void assertCrossSlotRejected(Executable command) {
    JedisClusterOperationException e = assertThrows(JedisClusterOperationException.class, command);
    assertThat(e.getMessage(), containsString("multiple hash slots"));
  }

  @Test
  public void testBinaryScriptFlush() {
    byte[] byteKey = "key1".getBytes();
    cluster.scriptLoad("return redis.call('get','foo')".getBytes(), byteKey);
    assertEquals("OK", cluster.scriptFlush(byteKey));
    assertEquals("OK", cluster.scriptFlush(byteKey, FlushMode.SYNC));
  }

  @Test
  public void testBinaryScriptKill() {
    byte[] byteKey = "key1".getBytes();
    assertThrows(JedisDataException.class, ()-> cluster.scriptKill(byteKey));
  }

  @Test
  public void testBinaryScriptExists() {
    byte[] byteKey = "key1".getBytes();
    byte[] sha1 = cluster.scriptLoad("return redis.call('get','foo')".getBytes(), byteKey);
    byte[][] arraySha1 = { sha1 };
    assertEquals(Collections.singletonList(Boolean.TRUE), cluster.scriptExists(byteKey, arraySha1));
  }

  @Test
  public void broadcast() {

    String script_1 = "return 'jedis'";
    String sha1_1 = cluster.scriptLoad(script_1);

    String script_2 = "return 79";
    String sha1_2 = cluster.scriptLoad(script_2);

    assertEquals(Arrays.asList(true, true), cluster.scriptExists(Arrays.asList(sha1_1, sha1_2)));

    cluster.scriptFlush();

    assertEquals(Arrays.asList(false, false), cluster.scriptExists(Arrays.asList(sha1_1, sha1_2)));
  }

  @Test
  @SinceRedisVersion("7.0.0")
  public void broadcastWithError() {

    JedisBroadcastException error = assertThrows(JedisBroadcastException.class, () -> cluster.functionDelete("xyz"));

    Map<HostAndPort, Object> replies = error.getReplies();
    assertEquals(3, replies.size());
    replies.values().forEach(r -> {
      assertSame(JedisDataException.class, r.getClass());
      assertEquals("ERR Library not found", ((JedisDataException) r).getMessage());
    });
  }
}
