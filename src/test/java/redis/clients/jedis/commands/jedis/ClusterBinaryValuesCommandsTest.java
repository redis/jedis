package redis.clients.jedis.commands.jedis;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static redis.clients.jedis.Protocol.Command.*;
import static redis.clients.jedis.util.AssertUtil.assertByteArrayListEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.GeoRadiusStoreParam;
import redis.clients.jedis.util.SafeEncoder;

public class ClusterBinaryValuesCommandsTest extends ClusterJedisCommandsTestBase {

  @Test
  public void nullKeys() {
    String foo = "foo";

    try {
      cluster.exists((String) null);
      fail();
    } catch (NullPointerException e) {
      // expected
    }

    try {
      cluster.exists(foo, null);
      fail();
    } catch (NullPointerException e) {
      // expected
    }

    try {
      cluster.exists(null, foo);
      fail();
    } catch (NullPointerException e) {
      // expected
    }
  }

  @Test
  public void testBinaryGetAndSet() {
    byte[] byteKey = "foo".getBytes();
    byte[] byteValue = "2".getBytes();
    cluster.set(byteKey, byteValue);
    assertArrayEquals(byteValue, cluster.get(byteKey));
  }

  @Test
  public void testIncr() {
    byte[] byteKey = "foo".getBytes();
    byte[] byteValue = "2".getBytes();
    cluster.set(byteKey, byteValue);
    cluster.incr(byteKey);
    assertArrayEquals("3".getBytes(), cluster.get(byteKey));
  }

  @Test
  public void testSadd() {
    byte[] byteKey = "languages".getBytes();
    byte[] firstLanguage = "java".getBytes();
    byte[] secondLanguage = "python".getBytes();
    byte[][] listLanguages = { firstLanguage, secondLanguage };
    cluster.sadd(byteKey, listLanguages);
    Set<byte[]> setLanguages = cluster.smembers(byteKey);
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
    cluster.hmset(key, map);
    List<byte[]> listResults = cluster.hmget(key, field);
    for (byte[] result : listResults) {
      assertArrayEquals(value, result);
    }
  }

  @Test
  public void testRpush() {
    byte[] value1 = "value1".getBytes();
    byte[] value2 = "value2".getBytes();
    byte[] key = "key1".getBytes();
    cluster.del(key);
    cluster.rpush(key, value1);
    cluster.rpush(key, value2);
    assertEquals(2, (long) cluster.llen(key));
  }

  @Test
  public void georadiusStoreBinary() {
      // prepare datas
      Map<byte[], GeoCoordinate> bcoordinateMap = new HashMap<byte[], GeoCoordinate>();
      bcoordinateMap.put("Palermo".getBytes(), new GeoCoordinate(13.361389, 38.115556));
      bcoordinateMap.put("Catania".getBytes(), new GeoCoordinate(15.087269, 37.502669));
      cluster.geoadd("{Sicily}".getBytes(), bcoordinateMap);

      long size = cluster.georadiusStore("{Sicily}".getBytes(), 15, 37, 200, GeoUnit.KM,
        GeoRadiusParam.geoRadiusParam(),
        GeoRadiusStoreParam.geoRadiusStoreParam().store("{Sicily}Store"));
      assertEquals(2, size);
      List<byte[]> bexpected = new ArrayList<byte[]>();
      bexpected.add("Palermo".getBytes());
      bexpected.add("Catania".getBytes());
      assertByteArrayListEquals(bexpected, cluster.zrange("{Sicily}Store".getBytes(), 0, -1));
  }

  @Test
  public void testKeys() {
    assertEquals(0, cluster.keys("{f}o*".getBytes()).size());
    cluster.set("{f}oo1".getBytes(), "bar".getBytes());
    cluster.set("{f}oo2".getBytes(), "bar".getBytes());
    cluster.set("{f}oo3".getBytes(), "bar".getBytes());
    assertEquals(3, cluster.keys("{f}o*".getBytes()).size());
  }

  @Test
  public void testBinaryGeneralCommand() {
    byte[] key = "x".getBytes();
    byte[] value = "1".getBytes();
    cluster.sendCommand("z".getBytes(), SET, key, value);
    cluster.sendCommand("y".getBytes(), INCR, key);
    Object returnObj = cluster.sendCommand("w".getBytes(), GET, key);
    assertEquals("2", SafeEncoder.encode((byte[]) returnObj));
  }

  @Test
  public void testGeneralCommand() {
    cluster.sendCommand("z", SET, "x", "1");
    cluster.sendCommand("y", INCR, "x");
    Object returnObj = cluster.sendCommand("w", GET, "x");
    assertEquals("2", SafeEncoder.encode((byte[]) returnObj));
  }

  @Test(expected = IllegalArgumentException.class)
  public void failKeys() {
    cluster.keys("*".getBytes());
  }
}
