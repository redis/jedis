package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static redis.clients.jedis.Protocol.Command.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import redis.clients.jedis.util.SafeEncoder;

public class ClusterBinaryValuesCommandsTest extends ClusterJedisCommandsTestBase {

  @Test
  public void testBinaryGetAndSet() {
    byte[] byteKey = "foo".getBytes();
    byte[] byteValue = "2".getBytes();
    jedisCluster.set(byteKey, byteValue);
    assertArrayEquals(byteValue, jedisCluster.get(byteKey));
  }

  @Test
  public void testIncr() {
    byte[] byteKey = "foo".getBytes();
    byte[] byteValue = "2".getBytes();
    jedisCluster.set(byteKey, byteValue);
    jedisCluster.incr(byteKey);
    assertArrayEquals("3".getBytes(), jedisCluster.get(byteKey));
  }

  @Test
  public void testSadd() {
    byte[] byteKey = "languages".getBytes();
    byte[] firstLanguage = "java".getBytes();
    byte[] secondLanguage = "python".getBytes();
    byte[][] listLanguages = { firstLanguage, secondLanguage };
    jedisCluster.sadd(byteKey, listLanguages);
    Set<byte[]> setLanguages = jedisCluster.smembers(byteKey);
    List<String> languages = new ArrayList<>();
    for (byte[] language : setLanguages) {
      languages.add(new String(language));
    }
    assertTrue(languages.contains("java"));
    assertTrue(languages.contains("python"));
  }

  @Test
  public void testHmset() {
    byte[] key = "jedis".getBytes();
    byte[] field = "language".getBytes();
    byte[] value = "java".getBytes();
    HashMap<byte[], byte[]> map = new HashMap();
    map.put(field, value);
    jedisCluster.hmset(key, map);
    List<byte[]> listResults = jedisCluster.hmget(key, field);
    for (byte[] result : listResults) {
      assertArrayEquals(value, result);
    }
  }

  @Test
  public void testRpush() {
    byte[] value1 = "value1".getBytes();
    byte[] value2 = "value2".getBytes();
    byte[] key = "key1".getBytes();
    jedisCluster.del(key);
    jedisCluster.rpush(key, value1);
    jedisCluster.rpush(key, value2);
    assertEquals(2, (long) jedisCluster.llen(key));
  }

  @Test
  public void testKeys() {
    assertEquals(0, jedisCluster.keys("{f}o*".getBytes()).size());
    jedisCluster.set("{f}oo1".getBytes(), "bar".getBytes());
    jedisCluster.set("{f}oo2".getBytes(), "bar".getBytes());
    jedisCluster.set("{f}oo3".getBytes(), "bar".getBytes());
    assertEquals(3, jedisCluster.keys("{f}o*".getBytes()).size());
  }

  @Test
  public void testBinaryGeneralCommand(){
    byte[] key = "x".getBytes();
    byte[] value = "1".getBytes();
    jedisCluster.sendCommand("z".getBytes(), SET, key, value);
    jedisCluster.sendCommand("y".getBytes(), INCR, key);
    Object returnObj = jedisCluster.sendCommand("w".getBytes(), GET, key);
    assertEquals("2", SafeEncoder.encode((byte[])returnObj));
  }

  @Test
  public void testGeneralCommand(){
    jedisCluster.sendCommand("z", SET, "x", "1");
    jedisCluster.sendCommand("y", INCR, "x");
    Object returnObj = jedisCluster.sendCommand("w", GET, "x");
    assertEquals("2", SafeEncoder.encode((byte[])returnObj));
  }


  @Test(expected = IllegalArgumentException.class)
  public void failKeys() {
    jedisCluster.keys("*".getBytes());
  }
}
