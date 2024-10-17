package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.awaitility.Awaitility;

import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.commands.jedis.JedisCommandsTestBase;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.util.SafeEncoder;

@RunWith(Parameterized.class)
public class PipeliningTest extends JedisCommandsTestBase {

  private static final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  private static final byte[] bfoo1 = { 0x01, 0x02, 0x03, 0x04, 0x11, 0x12, 0x13, 0x14 };
  private static final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
  private static final byte[] bbaz = { 0x09, 0x0A, 0x0B, 0x0C };

  public PipeliningTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void pipeline() {
    Pipeline p = jedis.pipelined();
    p.set("foo", "bar");
    p.get("foo");
    List<Object> results = p.syncAndReturnAll();

    assertEquals(2, results.size());
    assertEquals("OK", results.get(0));
    assertEquals("bar", results.get(1));
  }

  @Test
  public void pipelineResponse() {
    jedis.set("string", "foo");
    jedis.lpush("list", "foo");
    jedis.hset("hash", "foo", "bar");
    jedis.zadd("zset", 1, "foo");
    jedis.sadd("set", "foo");
    jedis.setrange("setrange", 0, "0123456789");
    byte[] bytesForSetRange = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
    jedis.setrange("setrangebytes".getBytes(), 0, bytesForSetRange);

    Pipeline p = jedis.pipelined();
    Response<String> string = p.get("string");
    Response<String> list = p.lpop("list");
    Response<String> hash = p.hget("hash", "foo");
    Response<List<String>> zset = p.zrange("zset", 0, -1);
    Response<String> set = p.spop("set");
    Response<Boolean> blist = p.exists("list");
    Response<Double> zincrby = p.zincrby("zset", 1, "foo");
    Response<Long> zcard = p.zcard("zset");
    p.lpush("list", "bar");
    Response<List<String>> lrange = p.lrange("list", 0, -1);
    Response<Map<String, String>> hgetAll = p.hgetAll("hash");
    p.sadd("set", "foo");
    Response<Set<String>> smembers = p.smembers("set");
    Response<List<Tuple>> zrangeWithScores = p.zrangeWithScores("zset", 0, -1);
    Response<String> getrange = p.getrange("setrange", 1, 3);
    Response<byte[]> getrangeBytes = p.getrange("setrangebytes".getBytes(), 6, 8);
    p.sync();

    assertEquals("foo", string.get());
    assertEquals("foo", list.get());
    assertEquals("bar", hash.get());
    assertEquals("foo", zset.get().iterator().next());
    assertEquals("foo", set.get());
    assertEquals(false, blist.get());
    assertEquals(Double.valueOf(2), zincrby.get());
    assertEquals(Long.valueOf(1), zcard.get());
    assertEquals(1, lrange.get().size());
    assertNotNull(hgetAll.get().get("foo"));
    assertEquals(1, smembers.get().size());
    assertEquals(1, zrangeWithScores.get().size());
    assertEquals("123", getrange.get());
    byte[] expectedGetRangeBytes = { 6, 7, 8 };
    assertArrayEquals(expectedGetRangeBytes, getrangeBytes.get());
  }

  @Test
  public void intermediateSyncs() {
    jedis.set("string", "foo");
    jedis.lpush("list", "foo");
    jedis.hset("hash", "foo", "bar");
    jedis.zadd("zset", 1, "foo");
    jedis.sadd("set", "foo");
    jedis.setrange("setrange", 0, "0123456789");
    byte[] bytesForSetRange = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
    jedis.setrange("setrangebytes".getBytes(), 0, bytesForSetRange);

    Pipeline p = jedis.pipelined();
    Response<String> string = p.get("string");
    Response<String> list = p.lpop("list");
    Response<String> hash = p.hget("hash", "foo");
    Response<List<String>> zset = p.zrange("zset", 0, -1);
    Response<String> set = p.spop("set");
    Response<Boolean> blist = p.exists("list");
    p.sync();

    assertEquals("foo", string.get());
    assertEquals("foo", list.get());
    assertEquals("bar", hash.get());
    assertEquals("foo", zset.get().iterator().next());
    assertEquals("foo", set.get());
    assertEquals(false, blist.get());

    Response<Double> zincrby = p.zincrby("zset", 1, "foo");
    Response<Long> zcard = p.zcard("zset");
    p.lpush("list", "bar");
    Response<List<String>> lrange = p.lrange("list", 0, -1);
    Response<Map<String, String>> hgetAll = p.hgetAll("hash");
    p.sadd("set", "foo");
    p.sync();

    assertEquals(Double.valueOf(2), zincrby.get());
    assertEquals(Long.valueOf(1), zcard.get());
    assertEquals(1, lrange.get().size());
    assertNotNull(hgetAll.get().get("foo"));

    Response<Set<String>> smembers = p.smembers("set");
    Response<List<Tuple>> zrangeWithScores = p.zrangeWithScores("zset", 0, -1);
    Response<String> getrange = p.getrange("setrange", 1, 3);
    Response<byte[]> getrangeBytes = p.getrange("setrangebytes".getBytes(), 6, 8);
    p.sync();

    assertEquals(1, smembers.get().size());
    assertEquals(1, zrangeWithScores.get().size());
    assertEquals("123", getrange.get());
    byte[] expectedGetRangeBytes = { 6, 7, 8 };
    assertArrayEquals(expectedGetRangeBytes, getrangeBytes.get());
  }

  @Test
  public void pipelineResponseWithData() {
    jedis.zadd("zset", 1, "foo");

    Pipeline p = jedis.pipelined();
    Response<Double> score = p.zscore("zset", "foo");
    p.sync();

    assertNotNull(score.get());
  }

  @Test
  public void pipelineBinarySafeHashCommands() {
    jedis.hset("key".getBytes(), "f1".getBytes(), "v111".getBytes());
    jedis.hset("key".getBytes(), "f22".getBytes(), "v2222".getBytes());

    Pipeline p = jedis.pipelined();
    Response<Map<byte[], byte[]>> fmap = p.hgetAll("key".getBytes());
    Response<Set<byte[]>> fkeys = p.hkeys("key".getBytes());
    Response<List<byte[]>> fordered = p.hmget("key".getBytes(), "f22".getBytes(), "f1".getBytes());
    Response<List<byte[]>> fvals = p.hvals("key".getBytes());
    p.sync();

    assertNotNull(fmap.get());
    // we have to do these strange contortions because byte[] is not a very
    // good key
    // for a java Map. It only works with equality (you need the exact key
    // object to retrieve
    // the value) I recommend we switch to using ByteBuffer or something
    // similar:
    // http://stackoverflow.com/questions/1058149/using-a-byte-array-as-hashmap-key-java
    Map<byte[], byte[]> map = fmap.get();
    Set<byte[]> mapKeys = map.keySet();
    Iterator<byte[]> iterMap = mapKeys.iterator();
    byte[] firstMapKey = iterMap.next();
    byte[] secondMapKey = iterMap.next();
    assertFalse(iterMap.hasNext());
    verifyHasBothValues(firstMapKey, secondMapKey, "f1".getBytes(), "f22".getBytes());
    byte[] firstMapValue = map.get(firstMapKey);
    byte[] secondMapValue = map.get(secondMapKey);
    verifyHasBothValues(firstMapValue, secondMapValue, "v111".getBytes(), "v2222".getBytes());

    assertNotNull(fkeys.get());
    Iterator<byte[]> iter = fkeys.get().iterator();
    byte[] firstKey = iter.next();
    byte[] secondKey = iter.next();
    assertFalse(iter.hasNext());
    verifyHasBothValues(firstKey, secondKey, "f1".getBytes(), "f22".getBytes());

    assertNotNull(fordered.get());
    assertArrayEquals("v2222".getBytes(), fordered.get().get(0));
    assertArrayEquals("v111".getBytes(), fordered.get().get(1));

    assertNotNull(fvals.get());
    assertEquals(2, fvals.get().size());
    byte[] firstValue = fvals.get().get(0);
    byte[] secondValue = fvals.get().get(1);
    verifyHasBothValues(firstValue, secondValue, "v111".getBytes(), "v2222".getBytes());
  }

  private void verifyHasBothValues(byte[] firstKey, byte[] secondKey, byte[] value1, byte[] value2) {
    assertFalse(Arrays.equals(firstKey, secondKey));
    assertTrue(Arrays.equals(firstKey, value1) || Arrays.equals(firstKey, value2));
    assertTrue(Arrays.equals(secondKey, value1) || Arrays.equals(secondKey, value2));
  }

  @Test
  public void pipelineSelect() {
    jedis.set("foo", "bar");
    jedis.swapDB(0, 1);
    Pipeline p = jedis.pipelined();
    p.get("foo");
    p.select(1);
    p.get("foo");
    assertEquals(Arrays.<Object>asList(null, "OK", "bar"), p.syncAndReturnAll());
  }

  @Test
  public void pipelineResponseWithoutData() {
    jedis.zadd("zset", 1, "foo");

    Pipeline p = jedis.pipelined();
    Response<Double> score = p.zscore("zset", "bar");
    p.sync();

    assertNull(score.get());
  }

  @Test(expected = IllegalStateException.class)
  public void pipelineResponseWithinPipeline() {
    jedis.set("string", "foo");

    Pipeline p = jedis.pipelined();
    Response<String> string = p.get("string");
    string.get();
    p.sync();
  }

  @Test
  public void publishInPipeline() {
    Pipeline pipelined = jedis.pipelined();
    Response<Long> p1 = pipelined.publish("foo", "bar");
    Response<Long> p2 = pipelined.publish("foo".getBytes(), "bar".getBytes());
    pipelined.sync();
    assertEquals(0, p1.get().longValue());
    assertEquals(0, p2.get().longValue());
  }

  @Test
  public void canRetrieveUnsetKey() {
    Pipeline p = jedis.pipelined();
    Response<String> shouldNotExist = p.get(UUID.randomUUID().toString());
    p.sync();
    assertNull(shouldNotExist.get());
  }

  @Test
  public void piplineWithError() {
    Pipeline p = jedis.pipelined();
    p.set("foo", "bar");
    Response<Set<String>> error = p.smembers("foo");
    Response<String> r = p.get("foo");
    p.sync();
    try {
      error.get();
      fail();
    } catch (JedisDataException e) {
      // that is fine we should be here
    }
    assertEquals(r.get(), "bar");
  }

  @Test(expected = IllegalStateException.class)
  public void testJedisThrowExceptionWhenInPipeline() {
    Pipeline pipeline = jedis.pipelined();
    pipeline.set("foo", "3");
    jedis.get("somekey");
    fail("Can't use jedis instance when in Pipeline");
  }

  @Test
  public void testReuseJedisWhenPipelineIsEmpty() {
    Pipeline pipeline = jedis.pipelined();
    pipeline.set("foo", "3");
    pipeline.sync();
    String result = jedis.get("foo");
    assertEquals(result, "3");
  }

  @Test
  public void testResetStateWhenInPipeline() {
    Pipeline pipeline = jedis.pipelined();
    pipeline.set("foo", "3");
    jedis.resetState();
    String result = jedis.get("foo");
    assertEquals(result, "3");
  }

  @Test
  public void waitReplicas() {
    Pipeline p = jedis.pipelined();
    p.set("wait", "replicas");
    p.waitReplicas(1, 10);
    p.sync();

    EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone4-replica-of-standalone1");

    try (Jedis j = new Jedis(endpoint.getHostAndPort())) {
      j.auth(endpoint.getPassword());
      assertEquals("replicas", j.get("wait"));
    }
  }

  @Test
  public void waitAof() {
    Pipeline p = jedis.pipelined();
    p.set("wait", "aof");
    p.waitAOF(1L, 0L, 0L);
    p.sync();

    EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone4-replica-of-standalone1");

    try (Jedis j = new Jedis(endpoint.getHostAndPort())) {
      j.auth(endpoint.getPassword());
      Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(50, TimeUnit.MILLISECONDS)
          .untilAsserted(() -> assertEquals("aof", j.get("wait")));
    }
  }

  @Test
  public void setGet() {
    Pipeline p = jedis.pipelined();
    Response<String> _ok = p.set("hello", "world");
    Response<String> _world = p.setGet("hello", "jedis", SetParams.setParams());
    Response<String> _jedis = p.get("hello");
    Response<String> _null = p.setGet("key", "value", SetParams.setParams());
    p.sync();

    assertEquals("OK", _ok.get());
    assertEquals("world", _world.get());
    assertEquals("jedis", _jedis.get());
    assertNull(_null.get());
  }

  @Test
  public void setGetBinary() {
    Pipeline p = jedis.pipelined();
    Response<String> _ok = p.set("hello".getBytes(), "world".getBytes());
    Response<byte[]> _world = p.setGet("hello".getBytes(), "jedis".getBytes(), SetParams.setParams());
    Response<byte[]> _jedis = p.get("hello".getBytes());
    Response<byte[]> _null = p.setGet("key".getBytes(), "value".getBytes(), SetParams.setParams());
    p.sync();

    assertEquals("OK", _ok.get());
    assertArrayEquals("world".getBytes(), _world.get());
    assertArrayEquals("jedis".getBytes(), _jedis.get());
    assertNull(_null.get());
  }

  @Test
  public void testEval() {
    String script = "return 'success!'";

    Pipeline p = jedis.pipelined();
    Response<Object> result = p.eval(script);
    p.sync();

    assertEquals("success!", result.get());
  }

  @Test
  public void testEvalWithBinary() {
    String script = "return 'success!'";

    Pipeline p = jedis.pipelined();
    Response<Object> result = p.eval(SafeEncoder.encode(script));
    p.sync();

    assertArrayEquals(SafeEncoder.encode("success!"), (byte[]) result.get());
  }

  @Test
  public void testEvalKeyAndArg() {
    String key = "test";
    String arg = "3";
    String script = "redis.call('INCRBY', KEYS[1], ARGV[1]) redis.call('INCRBY', KEYS[1], ARGV[1])";

    Pipeline p = jedis.pipelined();
    p.set(key, "0");
    Response<Object> result0 = p.eval(script, Arrays.asList(key), Arrays.asList(arg));
    p.incr(key);
    Response<Object> result1 = p.eval(script, Arrays.asList(key), Arrays.asList(arg));
    Response<String> result2 = p.get(key);
    p.sync();

    assertNull(result0.get());
    assertNull(result1.get());
    assertEquals("13", result2.get());
  }

  @Test
  public void testEvalKeyAndArgWithBinary() {
    // binary
    byte[] bKey = SafeEncoder.encode("test");
    byte[] bArg = SafeEncoder.encode("3");
    byte[] bScript = SafeEncoder
        .encode("redis.call('INCRBY', KEYS[1], ARGV[1]) redis.call('INCRBY', KEYS[1], ARGV[1])");

    Pipeline bP = jedis.pipelined();
    bP.set(bKey, SafeEncoder.encode("0"));
    Response<Object> bResult0 = bP.eval(bScript, Arrays.asList(bKey), Arrays.asList(bArg));
    bP.incr(bKey);
    Response<Object> bResult1 = bP.eval(bScript, Arrays.asList(bKey), Arrays.asList(bArg));
    Response<byte[]> bResult2 = bP.get(bKey);
    bP.sync();

    assertNull(bResult0.get());
    assertNull(bResult1.get());
    assertArrayEquals(SafeEncoder.encode("13"), bResult2.get());
  }

  @Test
  public void testEvalNestedLists() {
    String script = "return { {KEYS[1]} , {2} }";

    Pipeline p = jedis.pipelined();
    Response<Object> result = p.eval(script, 1, "key1");
    p.sync();

    List<?> results = (List<?>) result.get();
    assertThat((List<String>) results.get(0), Matchers.hasItem("key1"));
    assertThat((List<Long>) results.get(1), Matchers.hasItem(2L));
  }

  @Test
  public void testEvalNestedListsWithBinary() {
    byte[] bScript = SafeEncoder.encode("return { {KEYS[1]} , {2} }");
    byte[] bKey = SafeEncoder.encode("key1");

    Pipeline p = jedis.pipelined();
    Response<Object> result = p.eval(bScript, 1, bKey);
    p.sync();

    List<?> results = (List<?>) result.get();
    assertThat((List<byte[]>) results.get(0), Matchers.hasItem(bKey));
    assertThat((List<Long>) results.get(1), Matchers.hasItem(2L));
  }

  @Test
  public void testEvalsha() {
    String script = "return 'success!'";
    String sha1 = jedis.scriptLoad(script);

    assertTrue(jedis.scriptExists(sha1));

    Pipeline p = jedis.pipelined();
    Response<Object> result = p.evalsha(sha1);
    p.sync();

    assertEquals("success!", result.get());
  }

  @Test
  public void testEvalshaKeyAndArg() {
    String key = "test";
    String arg = "3";
    String script = "redis.call('INCRBY', KEYS[1], ARGV[1]) redis.call('INCRBY', KEYS[1], ARGV[1])";
    String sha1 = jedis.scriptLoad(script);

    assertTrue(jedis.scriptExists(sha1));

    Pipeline p = jedis.pipelined();
    p.set(key, "0");
    Response<Object> result0 = p.evalsha(sha1, Arrays.asList(key), Arrays.asList(arg));
    p.incr(key);
    Response<Object> result1 = p.evalsha(sha1, Arrays.asList(key), Arrays.asList(arg));
    Response<String> result2 = p.get(key);
    p.sync();

    assertNull(result0.get());
    assertNull(result1.get());
    assertEquals("13", result2.get());
  }

  @Test
  public void testEvalshaKeyAndArgWithBinary() {
    byte[] bKey = SafeEncoder.encode("test");
    byte[] bArg = SafeEncoder.encode("3");
    String script = "redis.call('INCRBY', KEYS[1], ARGV[1]) redis.call('INCRBY', KEYS[1], ARGV[1])";
    byte[] bScript = SafeEncoder.encode(script);
    byte[] bSha1 = jedis.scriptLoad(bScript);

    assertTrue(jedis.scriptExists(bSha1));

    Pipeline p = jedis.pipelined();
    p.set(bKey, SafeEncoder.encode("0"));
    Response<Object> result0 = p.evalsha(bSha1, Arrays.asList(bKey), Arrays.asList(bArg));
    p.incr(bKey);
    Response<Object> result1 = p.evalsha(bSha1, Arrays.asList(bKey), Arrays.asList(bArg));
    Response<byte[]> result2 = p.get(bKey);
    p.sync();

    assertNull(result0.get());
    assertNull(result1.get());
    assertArrayEquals(SafeEncoder.encode("13"), result2.get());
  }

  @Test
  public void testSyncWithNoCommandQueued() {
    // we need to test with fresh instance of Jedis
    Jedis jedis2 = new Jedis(endpoint.getHost(), endpoint.getPort(), 500);

    Pipeline pipeline = jedis2.pipelined();
    pipeline.sync();

    jedis2.close();

    jedis2 = new Jedis(endpoint.getHost(), endpoint.getPort(), 500);

    pipeline = jedis2.pipelined();
    List<Object> resp = pipeline.syncAndReturnAll();
    assertTrue(resp.isEmpty());

    jedis2.close();
  }

  @Test
  public void testCloseable() throws IOException {
    // we need to test with fresh instance of Jedis
    Jedis jedis2 = new Jedis(endpoint.getHost(), endpoint.getPort(), 500);
    jedis2.auth(endpoint.getPassword());

    Pipeline pipeline = jedis2.pipelined();
    Response<String> retFuture1 = pipeline.set("a", "1");
    Response<String> retFuture2 = pipeline.set("b", "2");

    pipeline.close();

    // it shouldn't meet any exception
    retFuture1.get();
    retFuture2.get();
    jedis2.close();
  }

  @Test
  public void time() {
    Pipeline p = jedis.pipelined();

    p.time();

    // we get back one result, with two components: the seconds, and the microseconds, but encoded as strings
    Matcher timeResponseMatcher = hasItems(matchesPattern("\\d+"), matchesPattern("\\d+"));
    assertThat(p.syncAndReturnAll(),
        hasItems(timeResponseMatcher));
  }

  @Test
  public void dbSize() {
    Pipeline p = jedis.pipelined();

    p.dbSize();
    p.set("foo", "bar");
    p.dbSize();

    assertThat(p.syncAndReturnAll(),
        hasItems(0L, "OK", 1L));
  }

  @Test
  public void move() {
    Pipeline p = jedis.pipelined();

    p.move("foo", 1);
    p.set("foo", "bar");
    p.move("foo", 1);
    p.get("foo");
    p.select(1);
    p.get("foo");

    assertThat(p.syncAndReturnAll(),
        hasItems(0L, "OK", 1L, null, "OK", "bar"));
  }

  @Test
  public void moveBinary() {
    Pipeline p = jedis.pipelined();

    p.move(bfoo, 1);
    p.set(bfoo, bbar);
    p.move(bfoo, 1);
    p.get(bfoo);
    p.select(1);
    p.get(bfoo);

    assertThat(p.syncAndReturnAll(),
        hasItems(0L, "OK", 1L, null, "OK", bbar));
  }

  @Test
  public void swapDb() {
    Pipeline p = jedis.pipelined();

    p.set("foo", "bar");
    p.get("foo");
    p.select(1);
    p.get("foo");
    p.swapDB(0, 1);
    p.select(0);
    p.get("foo");
    p.select(1);
    p.get("foo");

    assertThat(p.syncAndReturnAll(),
        hasItems("OK", "bar", "OK", null, "OK", "OK", null, "OK", "bar"));
  }

  @Test
  public void copyToAnotherDb() {
    Pipeline p = jedis.pipelined();

    p.copy("foo", "foo-copy", 1, false);
    p.set("foo", "bar");
    p.copy("foo", "foo-copy", 1, false);
    p.get("foo");
    p.select(1);
    p.get("foo-copy");
    p.select(0);
    p.set("foo", "baz");
    p.copy("foo", "foo-copy", 1, false);
    p.get("foo");
    p.select(1);
    p.get("foo-copy");

    assertThat(p.syncAndReturnAll(),
        hasItems(false, "OK", true, "bar", "OK", "bar", "OK", "OK", false, "baz", "bar"));
  }

  @Test
  public void copyToAnotherDbBinary() {
    Pipeline p = jedis.pipelined();


    p.copy(bfoo, bfoo1, 1, false);
    p.set(bfoo, bbar);
    p.copy(bfoo, bfoo1, 1, false);
    p.get(bfoo);
    p.select(1);
    p.get(bfoo1);
    p.select(0);
    p.set(bfoo, bbaz);
    p.copy(bfoo, bfoo1, 1, false);
    p.get(bfoo);
    p.select(1);
    p.get(bfoo1);

    assertThat(p.syncAndReturnAll(),
        hasItems(false, "OK", true, bbar, "OK", bbar, "OK", "OK", false, bbaz, bbar));
  }

  enum Foo implements ProtocolCommand {
    FOO;

    @Override
    public byte[] getRaw() {
      return SafeEncoder.encode(name());
    }
  }

  @Test
  public void errorInTheMiddle() {
    CommandObject<String> invalidCommand =
        new CommandObject<>(new CommandObjects().commandArguments(Foo.FOO), BuilderFactory.STRING);

    Pipeline p = jedis.pipelined();

    p.set("foo", "bar");
    p.appendCommand(invalidCommand);
    p.get("foo");

    assertThat(p.syncAndReturnAll(),
        hasItems(
            equalTo("OK"),
            instanceOf(JedisDataException.class),
            equalTo("bar")
        ));
  }

}
