package redis.clients.jedis.tests;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.tests.commands.JedisCommandTestBase;
import static redis.clients.jedis.tests.utils.AssertUtil.assertByteArraySetEquals;
import redis.clients.util.SafeEncoder;
import redis.clients.util.ScanCursor;

/**
 * @author Antonio Tomac <antonio.tomac@mediatoolkit.com>
 */
public class ScanCursorTest extends JedisCommandTestBase {

  final byte[] keyb = SafeEncoder.encode("key");
  final byte[] key1b = SafeEncoder.encode("key_1");
  final byte[] key2b = SafeEncoder.encode("key_2");
  final byte[] key3b = SafeEncoder.encode("key_3");
  final byte[] key4b = SafeEncoder.encode("key_4");
  final byte[] key5b = SafeEncoder.encode("key_5");
  final byte[] key6b = SafeEncoder.encode("key_6");
  final byte[] key7b = SafeEncoder.encode("key_7");
  final byte[] key8b = SafeEncoder.encode("key_8");
  final byte[] key9b = SafeEncoder.encode("key_9");
  final byte[] a1b = SafeEncoder.encode("a1");
  final byte[] a2b = SafeEncoder.encode("a2");
  final byte[] a3b = SafeEncoder.encode("a3");
  final byte[] elem1b = SafeEncoder.encode("elem_1");
  final byte[] elem2b = SafeEncoder.encode("elem_2");
  final byte[] elem3b = SafeEncoder.encode("elem_3");

  @Test
  public void testOneBatch() {
    jedis.mset("key_1", "1", "key_2", "1", "key_3", "1");

    Set<String> set = ScanCursor.builder(jedis).count(10).scan().toSet();
    Set<String> expected = new HashSet<>(Arrays.asList("key_1", "key_2", "key_3"));
    assertEquals(expected, set);

    Set<byte[]> setBin = ScanCursor.builder(jedis).count(10).scanBin().toSet();
    Set<byte[]> expectedBin = new HashSet<>(Arrays.asList(key1b, key2b, key3b));
    assertByteArraySetEquals(expectedBin, setBin);
  }

  @Test
  public void testOneZSetBatch() {
    jedis.zadd("zset", 1., "key_1");
    jedis.zadd("zset", 1., "key_2");
    jedis.zadd("zset", 1., "key_3");
    Set<Tuple> zset = ScanCursor.builder(jedis).zScan("zset").toSet();
    Set<Tuple> expected = new HashSet<>(Arrays.asList(new Tuple("key_1", 1.),
      new Tuple("key_2", 1.), new Tuple("key_3", 1.)));
    assertEquals(expected, zset);
  }

  @Test
  public void testOneHSetBatch() {
    jedis.hset("hset", "key_1", "val_1");
    jedis.hset("hset", "key_2", "val_2");
    jedis.hset("hset", "key_3", "val_3");

    Map<String, String> expected = new HashMap<>();
    expected.put("key_1", "val_1");
    expected.put("key_2", "val_2");
    expected.put("key_3", "val_3");
    Map<String, String> result = new HashMap<>();
    ScanCursor<Entry<String, String>> hset = ScanCursor.builder(jedis).hScan("hset");
    for (Entry<String, String> entry : hset) {
      result.put(entry.getKey(), entry.getValue());
    }
    assertEquals(expected, result);

    Map<String, String> resultBin = new HashMap<>();
    ScanCursor<Entry<byte[], byte[]>> hsetBin = ScanCursor.builder(jedis).hScanBin(
      SafeEncoder.encode("hset"));
    for (Entry<byte[], byte[]> entry : hsetBin) {
      resultBin.put(new String(entry.getKey()), new String(entry.getValue()));
    }
    assertEquals(expected, resultBin);
  }

  @Test
  public void testOneSSetBatch() {
    jedis.sadd("sset", "elem_1", "elem_2", "elem_3");

    Set<String> sset = ScanCursor.builder(jedis).sScan("sset").toSet();
    Set<String> expected = new HashSet<>(Arrays.asList("elem_1", "elem_2", "elem_3"));
    assertEquals(expected, sset);

    Set<byte[]> ssetBin = ScanCursor.builder(jedis).sScanBin(SafeEncoder.encode("sset")).toSet();
    Set<byte[]> expectedBin = new HashSet<>(Arrays.asList(elem1b, elem2b, elem3b));
    assertByteArraySetEquals(expectedBin, ssetBin);
  }

  @Test
  public void testEmpty() {
    Set<String> set = ScanCursor.builder(jedis).pattern("missing*").count(10).scan().toSet();
    assertTrue(set.isEmpty());

    Set<byte[]> setBin = ScanCursor.builder(jedis).pattern("missing*").count(10).scanBin().toSet();
    assertTrue(setBin.isEmpty());
  }

  @Test
  public void testMoreBatches() {
    jedis.mset("key_1", "1", "key_2", "1", "key_3", "1", "key_4", "1", "key_5", "1", "key_6", "1",
      "key_7", "1", "key_8", "1", "key_9", "1");

    Set<String> set = ScanCursor.builder(jedis).count(2).scan().toSet();
    Set<String> expected = new HashSet<>(Arrays.asList("key_1", "key_2", "key_3", "key_4", "key_5",
      "key_6", "key_7", "key_8", "key_9"));
    assertEquals(expected, set);

    Set<byte[]> setBin = ScanCursor.builder(jedis).count(2).scanBin().toSet();
    Set<byte[]> expectedBin = new HashSet<>(Arrays.asList(key1b, key2b, key3b, key4b, key5b, key6b,
      key7b, key8b, key9b));
    assertByteArraySetEquals(expectedBin, setBin);
  }

  @Test
  public void testPattern() {
    jedis.mset("key", "1", "val", "1");

    Set<String> set = ScanCursor.builder(jedis).pattern("ke*").scan().toSet();
    Set<String> expected = new HashSet<>(Arrays.asList("key"));
    assertEquals(expected, set);

    Set<byte[]> setBin = ScanCursor.builder(jedis).pattern("ke*").scanBin().toSet();
    Set<byte[]> expectedBin = new HashSet<>(Arrays.asList(keyb));
    assertByteArraySetEquals(expectedBin, setBin);
  }

  @Test
  public void testPatternSparse() {
    jedis.mset("a1", "1", "b1", "1", "a2", "1", "c1", "1", "b2", "1", "b3", "1", "d1", "1", "c3",
      "1", "b4", "1", "b5", "1", "b6", "1", "a3", "1");

    Set<String> set = ScanCursor.builder(jedis).pattern("a*").count(2).scan().toSet();
    Set<String> expected = new HashSet<>(Arrays.asList("a1", "a2", "a3"));
    assertEquals(expected, set);

    Set<byte[]> setBin = ScanCursor.builder(jedis).pattern("a*").count(2).scanBin().toSet();
    Set<byte[]> expectedBin = new HashSet<>(Arrays.asList(a1b, a2b, a3b));
    assertByteArraySetEquals(expectedBin, setBin);
  }

}