package redis.clients.jedis.commands.unified.bloom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Tag;

import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.bloom.BFInsertParams;
import redis.clients.jedis.bloom.BFReserveParams;
import redis.clients.jedis.commands.unified.UnifiedJedisCommandsTestBase;
import redis.clients.jedis.exceptions.JedisDataException;

/**
 * Base test class for Bloom Filter commands using the UnifiedJedis pattern.
 */
@Tag("bloom")
public abstract class BloomCommandsTestBase extends UnifiedJedisCommandsTestBase {

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("modules-docker");
  }

  public BloomCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void reserveBasic() {
    jedis.bfReserve("myBloom", 0.001, 100L);
    assertTrue(jedis.bfAdd("myBloom", "val1"));
    assertTrue(jedis.bfExists("myBloom", "val1"));
    assertFalse(jedis.bfExists("myBloom", "val2"));
  }

  @Test
  public void reserveValidateZeroCapacity() {
    assertThrows(JedisDataException.class, () -> jedis.bfReserve("myBloom", 0.001, 0));
  }

  @Test
  public void reserveValidateZeroError() {
    assertThrows(JedisDataException.class, () -> jedis.bfReserve("myBloom", 0d, 100L));
  }

  @Test
  public void reserveAlreadyExists() {
    jedis.bfReserve("myBloom", 0.1, 100);
    assertThrows(JedisDataException.class, () -> jedis.bfReserve("myBloom", 0.1, 100));
  }

  @Test
  public void reserveV2() {
    jedis.bfReserve("reserve-basic", 0.001, 2);
    assertEquals(Arrays.asList(true), jedis.bfInsert("reserve-basic", "a"));
    assertEquals(Arrays.asList(true), jedis.bfInsert("reserve-basic", "b"));
    assertEquals(Arrays.asList(true), jedis.bfInsert("reserve-basic", "c"));
  }

  @Test
  public void reserveEmptyParams() {
    jedis.bfReserve("empty-param", 0.001, 2, BFReserveParams.reserveParams());
    assertEquals(Arrays.asList(true), jedis.bfInsert("empty-param", "a"));
    assertEquals(Arrays.asList(true), jedis.bfInsert("empty-param", "b"));
    assertEquals(Arrays.asList(true), jedis.bfInsert("empty-param", "c"));
  }

  @Test
  public void reserveNonScaling() {
    jedis.bfReserve("nonscaling", 0.001, 2, BFReserveParams.reserveParams().nonScaling());
    assertEquals(Arrays.asList(true), jedis.bfInsert("nonscaling", "a"));
    assertEquals(Arrays.asList(true), jedis.bfInsert("nonscaling", "b"));
    assertEquals(Arrays.asList((Boolean) null), jedis.bfInsert("nonscaling", "c"));
  }

  @Test
  public void reserveExpansion() {
    jedis.bfReserve("bfexpansion", 0.001, 1000, BFReserveParams.reserveParams().expansion(4));
    assertEquals(Arrays.asList(true), jedis.bfInsert("bfexpansion", "a"));
    assertEquals(Arrays.asList(true),
      jedis.bfInsert("bfexpansion", BFInsertParams.insertParams().noCreate(), "b"));
  }

  @Test
  public void addExistsString() {
    assertTrue(jedis.bfAdd("newFilter", "foo"));
    assertTrue(jedis.bfExists("newFilter", "foo"));
    assertFalse(jedis.bfExists("newFilter", "bar"));
    assertFalse(jedis.bfAdd("newFilter", "foo"));
  }

  @Test
  public void testExistsNonExist() {
    assertFalse(jedis.bfExists("nonExist", "foo"));
  }

  @Test
  public void addExistsMulti() {
    List<Boolean> rv = jedis.bfMAdd("newFilter", "foo", "bar", "baz");
    assertEquals(Arrays.asList(true, true, true), rv);

    rv = jedis.bfMAdd("newFilter", "newElem", "bar", "baz");
    assertEquals(Arrays.asList(true, false, false), rv);
  }

  @Test
  public void testExample() {
    jedis.bfAdd("simpleBloom", "Mark");
    jedis.bfExists("simpleBloom", "Mark");
    jedis.bfExists("simpleBloom", "Farnsworth");

    jedis.bfMAdd("simpleBloom", "foo", "bar", "baz", "bat", "bag");

    List<Boolean> rv = jedis.bfMExists("simpleBloom", "foo", "bar", "baz", "bat", "Mark",
      "nonexist");
    assertEquals(Arrays.asList(true, true, true, true, true, false), rv);

    jedis.bfReserve("specialBloom", 0.0001, 10000);
    jedis.bfAdd("specialBloom", "foo");
  }

  @Test
  public void testInsert() {
    jedis.bfInsert("b1", new BFInsertParams().capacity(1L), "1");
    assertTrue(jedis.bfExists("b1", "1"));

    JedisDataException jde = assertThrows(JedisDataException.class,
      () -> jedis.bfInsert("b2", new BFInsertParams().noCreate(), "1"),
      "Should error if the filter does not already exist.");
    assertEquals("ERR not found", jde.getMessage());

    jedis.bfInsert("b3", new BFInsertParams().capacity(1L).error(0.0001), "2");
    assertTrue(jedis.bfExists("b3", "2"));
  }

  @Test
  public void issue49() {
    BFInsertParams insertOptions = new BFInsertParams();
    List<Boolean> insert = jedis.bfInsert("mykey", insertOptions, "a", "b", "c");
    assertEquals(3, insert.size());
  }

  @Test
  public void card() {
    jedis.bfInsert("test_card", new BFInsertParams().capacity(1L), "1");
    assertEquals(1L, jedis.bfCard("test_card"));

    assertEquals(0L, jedis.bfCard("not_exist"));

    jedis.set("foo", "bar");
    assertThrows(JedisDataException.class, () -> jedis.bfCard("foo"),
      "Should error if the filter is not a bloom filter");
  }

  @Test
  public void info() {
    jedis.bfInsert("test_info", new BFInsertParams().capacity(1L), "1");
    Map<String, Object> info = jedis.bfInfo("test_info");
    assertEquals(1L, info.get("Number of items inserted"));

    JedisDataException jde = assertThrows(JedisDataException.class, () -> jedis.bfInfo("not_exist"),
      "Should error if the filter does not already exist.");
    assertEquals("ERR not found", jde.getMessage());
  }

  @Test
  public void insertNonScaling() {
    List<Boolean> insert = jedis.bfInsert("nonscaling_err",
      BFInsertParams.insertParams().capacity(4).nonScaling(), "a", "b", "c");
    assertEquals(Arrays.asList(true, true, true), insert);

    insert = jedis.bfInsert("nonscaling_err", "d", "e");
    assertEquals(Arrays.asList(true, null), insert);
  }

  @Test
  public void insertExpansion() {
    List<Boolean> insert = jedis.bfInsert("bfexpansion2",
      BFInsertParams.insertParams().capacity(3).expansion(3), "a", "b", "c", "d", "e", "f", "g",
      "h", "j", "k", "l", "o", "i", "u", "y", "t", "r", "e", "w", "q");
    assertEquals(20, insert.size());
  }

  @Test
  @Timeout(2)
  public void testScanDumpAndLoadChunk() {
    jedis.bfAdd("bloom-dump", "a");

    long iterator = 0;
    while (true) {
      Map.Entry<Long, byte[]> chunkData = jedis.bfScanDump("bloom-dump", iterator);
      iterator = chunkData.getKey();
      if (iterator == 0L) break;
      assertEquals("OK", jedis.bfLoadChunk("bloom-load", iterator, chunkData.getValue()));
    }

    assertEquals(jedis.bfInfo("bloom-dump"), jedis.bfInfo("bloom-load"));
    assertTrue(jedis.bfExists("bloom-load", "a"));
  }
}
