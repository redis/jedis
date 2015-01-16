package redis.clients.jedis.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisDataException;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class PipeliningTest extends Assert {
  private static HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);

  private Jedis jedis;

  @Before
  public void setUp() throws Exception {
    jedis = new Jedis(hnp.getHost(), hnp.getPort(), 500);
    jedis.connect();
    jedis.auth("foobared");
    jedis.flushAll();
  }

  @Test
  public void pipeline() throws UnsupportedEncodingException {
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

    Pipeline p = jedis.pipelined();
    Response<String> string = p.get("string");
    Response<String> list = p.lpop("list");
    Response<String> hash = p.hget("hash", "foo");
    Response<Set<String>> zset = p.zrange("zset", 0, -1);
    Response<String> set = p.spop("set");
    Response<Boolean> blist = p.exists("list");
    Response<Double> zincrby = p.zincrby("zset", 1, "foo");
    Response<Long> zcard = p.zcard("zset");
    p.lpush("list", "bar");
    Response<List<String>> lrange = p.lrange("list", 0, -1);
    Response<Map<String, String>> hgetAll = p.hgetAll("hash");
    p.sadd("set", "foo");
    Response<Set<String>> smembers = p.smembers("set");
    Response<Set<Tuple>> zrangeWithScores = p.zrangeWithScores("zset", 0, -1);
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
    Pipeline p = jedis.pipelined();
    p.select(1);
    p.sync();
  }

  @Test
  public void pipelineResponseWithoutData() {
    jedis.zadd("zset", 1, "foo");

    Pipeline p = jedis.pipelined();
    Response<Double> score = p.zscore("zset", "bar");
    p.sync();

    assertNull(score.get());
  }

  @Test(expected = JedisDataException.class)
  public void pipelineResponseWithinPipeline() {
    jedis.set("string", "foo");

    Pipeline p = jedis.pipelined();
    Response<String> string = p.get("string");
    string.get();
    p.sync();
  }

  @Test
  public void pipelineWithPubSub() {
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

  @Test
  public void multi() {
    Pipeline p = jedis.pipelined();
    p.multi();
    Response<Long> r1 = p.hincrBy("a", "f1", -1);
    Response<Long> r2 = p.hincrBy("a", "f1", -2);
    Response<List<Object>> r3 = p.exec();
    List<Object> result = p.syncAndReturnAll();

    assertEquals(new Long(-1), r1.get());
    assertEquals(new Long(-3), r2.get());

    assertEquals(4, result.size());

    assertEquals("OK", result.get(0));
    assertEquals("QUEUED", result.get(1));
    assertEquals("QUEUED", result.get(2));

    // 4th result is a list with the results from the multi
    @SuppressWarnings("unchecked")
    List<Object> multiResult = (List<Object>) result.get(3);
    assertEquals(new Long(-1), multiResult.get(0));
    assertEquals(new Long(-3), multiResult.get(1));

    assertEquals(new Long(-1), r3.get().get(0));
    assertEquals(new Long(-3), r3.get().get(1));

  }

  @Test
  public void multiWithMassiveRequests() {
    Pipeline p = jedis.pipelined();
    p.multi();

    List<Response<?>> responseList = new ArrayList<Response<?>>();
    for (int i = 0; i < 100000; i++) {
      // any operation should be ok, but shouldn't forget about timeout
      responseList.add(p.setbit("test", 1, true));
    }

    Response<List<Object>> exec = p.exec();
    p.sync();

    // we don't need to check return value
    // if below codes run without throwing Exception, we're ok
    exec.get();

    for (Response<?> resp : responseList) {
      resp.get();
    }
  }

  @Test
  public void multiWithSync() {
    jedis.set("foo", "314");
    jedis.set("bar", "foo");
    jedis.set("hello", "world");
    Pipeline p = jedis.pipelined();
    Response<String> r1 = p.get("bar");
    p.multi();
    Response<String> r2 = p.get("foo");
    p.exec();
    Response<String> r3 = p.get("hello");
    p.sync();

    // before multi
    assertEquals("foo", r1.get());
    // It should be readable whether exec's response was built or not
    assertEquals("314", r2.get());
    // after multi
    assertEquals("world", r3.get());
  }

  @Test(expected = JedisDataException.class)
  public void pipelineExecShoudThrowJedisDataExceptionWhenNotInMulti() {
    Pipeline pipeline = jedis.pipelined();
    pipeline.exec();
  }

  @Test(expected = JedisDataException.class)
  public void pipelineDiscardShoudThrowJedisDataExceptionWhenNotInMulti() {
    Pipeline pipeline = jedis.pipelined();
    pipeline.discard();
  }

  @Test(expected = JedisDataException.class)
  public void pipelineMultiShoudThrowJedisDataExceptionWhenAlreadyInMulti() {
    Pipeline pipeline = jedis.pipelined();
    pipeline.multi();
    pipeline.set("foo", "3");
    pipeline.multi();
  }

  @Test
  public void testDiscardInPipeline() {
    Pipeline pipeline = jedis.pipelined();
    pipeline.multi();
    pipeline.set("foo", "bar");
    Response<String> discard = pipeline.discard();
    Response<String> get = pipeline.get("foo");
    pipeline.sync();
    discard.get();
    get.get();
  }

  @Test
  public void testEval() {
    String script = "return 'success!'";

    Pipeline p = jedis.pipelined();
    Response<String> result = p.eval(script);
    p.sync();

    assertEquals("success!", result.get());
  }

  @Test
  public void testEvalKeyAndArg() {
    String key = "test";
    String arg = "3";
    String script = "redis.call('INCRBY', KEYS[1], ARGV[1]) redis.call('INCRBY', KEYS[1], ARGV[1])";

    Pipeline p = jedis.pipelined();
    p.set(key, "0");
    Response<String> result0 = p.eval(script, Arrays.asList(key), Arrays.asList(arg));
    p.incr(key);
    Response<String> result1 = p.eval(script, Arrays.asList(key), Arrays.asList(arg));
    Response<String> result2 = p.get(key);
    p.sync();

    assertNull(result0.get());
    assertNull(result1.get());
    assertEquals("13", result2.get());
  }

  @Test
  public void testEvalsha() {
    String script = "return 'success!'";
    String sha1 = jedis.scriptLoad(script);

    assertTrue(jedis.scriptExists(sha1));

    Pipeline p = jedis.pipelined();
    Response<String> result = p.evalsha(sha1);
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
    Response<String> result0 = p.evalsha(sha1, Arrays.asList(key), Arrays.asList(arg));
    p.incr(key);
    Response<String> result1 = p.evalsha(sha1, Arrays.asList(key), Arrays.asList(arg));
    Response<String> result2 = p.get(key);
    p.sync();

    assertNull(result0.get());
    assertNull(result1.get());
    assertEquals("13", result2.get());
  }

  @Test
  public void testPipelinedTransactionResponse() {

    String key1 = "key1";
    String val1 = "val1";

    String key2 = "key2";
    String val2 = "val2";

    String key3 = "key3";
    String field1 = "field1";
    String field2 = "field2";
    String field3 = "field3";
    String field4 = "field4";

    String value1 = "value1";
    String value2 = "value2";
    String value3 = "value3";
    String value4 = "value4";

    Map<String, String> hashMap = new HashMap<String, String>();
    hashMap.put(field1, value1);
    hashMap.put(field2, value2);

    String key4 = "key4";
    Map<String, String> hashMap1 = new HashMap<String, String>();
    hashMap1.put(field3, value3);
    hashMap1.put(field4, value4);

    jedis.set(key1, val1);
    jedis.set(key2, val2);
    jedis.hmset(key3, hashMap);
    jedis.hmset(key4, hashMap1);

    Pipeline pipeline = jedis.pipelined();
    pipeline.multi();

    pipeline.get(key1);
    pipeline.hgetAll(key2);
    pipeline.hgetAll(key3);
    pipeline.get(key4);

    Response<List<Object>> response = pipeline.exec();
    pipeline.sync();

    List<Object> result = response.get();

    assertEquals(4, result.size());

    assertEquals("val1", result.get(0));

    assertTrue(result.get(1) instanceof JedisDataException);

    Map<String, String> hashMapReceived = (Map<String, String>) result.get(2);
    Iterator<String> iterator = hashMapReceived.keySet().iterator();
    String mapKey1 = iterator.next();
    String mapKey2 = iterator.next();
    assertFalse(iterator.hasNext());
    verifyHasBothValues(mapKey1, mapKey2, field1, field2);
    String mapValue1 = hashMapReceived.get(mapKey1);
    String mapValue2 = hashMapReceived.get(mapKey2);
    verifyHasBothValues(mapValue1, mapValue2, value1, value2);

    assertTrue(result.get(3) instanceof JedisDataException);
  }

  @Test
  public void testSyncWithNoCommandQueued() {
    // we need to test with fresh instance of Jedis
    Jedis jedis2 = new Jedis(hnp.getHost(), hnp.getPort(), 500);

    Pipeline pipeline = jedis2.pipelined();
    pipeline.sync();

    jedis2.close();

    jedis2 = new Jedis(hnp.getHost(), hnp.getPort(), 500);

    pipeline = jedis2.pipelined();
    List<Object> resp = pipeline.syncAndReturnAll();
    assertTrue(resp.isEmpty());

    jedis2.close();
  }

  private void verifyHasBothValues(String firstKey, String secondKey, String value1, String value2) {
    assertFalse(firstKey.equals(secondKey));
    assertTrue(firstKey.equals(value1) || firstKey.equals(value2));
    assertTrue(secondKey.equals(value1) || secondKey.equals(value2));
  }
}
