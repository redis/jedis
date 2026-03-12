package redis.clients.jedis.commands.unified.bloom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Tag;

import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.bloom.CFInsertParams;
import redis.clients.jedis.bloom.CFReserveParams;
import redis.clients.jedis.commands.unified.UnifiedJedisCommandsTestBase;
import redis.clients.jedis.exceptions.JedisDataException;

/**
 * Base test class for Cuckoo Filter commands using the UnifiedJedis pattern.
 */
@Tag("integration")
@Tag("bloom")
public abstract class CuckooCommandsTestBase extends UnifiedJedisCommandsTestBase {

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("modules-docker");
  }

  public CuckooCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testReservationCapacityOnly() {
    jedis.cfReserve("cuckoo1", 10);
    Map<String, Object> info = jedis.cfInfo("cuckoo1");
    assertEquals(8L, info.get("Number of buckets"));
    assertEquals(0L, info.get("Number of items inserted"));
    assertEquals(72L, info.get("Size"));
    assertEquals(1L, info.get("Expansion rate"));
    assertEquals(1L, info.get("Number of filters"));
    assertEquals(2L, info.get("Bucket size"));
    assertEquals(20L, info.get("Max iterations"));
    assertEquals(0L, info.get("Number of items deleted"));
  }

  @Test
  public void testReservationCapacityAndBucketSize() {
    jedis.cfReserve("cuckoo2", 200, CFReserveParams.reserveParams().bucketSize(10));
    Map<String, Object> info = jedis.cfInfo("cuckoo2");
    assertEquals(32L, info.get("Number of buckets"));
    assertEquals(0L, info.get("Number of items inserted"));
    assertEquals(376L, info.get("Size"));
    assertEquals(1L, info.get("Expansion rate"));
    assertEquals(1L, info.get("Number of filters"));
    assertEquals(10L, info.get("Bucket size"));
    assertEquals(20L, info.get("Max iterations"));
    assertEquals(0L, info.get("Number of items deleted"));
  }

  @Test
  public void testReservationCapacityAndBucketSizeAndMaxIterations() {
    jedis.cfReserve("cuckoo3", 200, CFReserveParams.reserveParams()
        .bucketSize(10).maxIterations(20));
    Map<String, Object> info = jedis.cfInfo("cuckoo3");
    assertEquals(32L, info.get("Number of buckets"));
    assertEquals(0L, info.get("Number of items inserted"));
    assertEquals(376L, info.get("Size"));
    assertEquals(1L, info.get("Expansion rate"));
    assertEquals(1L, info.get("Number of filters"));
    assertEquals(10L, info.get("Bucket size"));
    assertEquals(20L, info.get("Max iterations"));
    assertEquals(0L, info.get("Number of items deleted"));
  }

  @Test
  public void testReservationAllParams() {
    jedis.cfReserve("cuckoo4", 200, CFReserveParams.reserveParams()
        .bucketSize(10).expansion(4).maxIterations(20));
    Map<String, Object> info = jedis.cfInfo("cuckoo4");
    assertEquals(32L, info.get("Number of buckets"));
    assertEquals(0L, info.get("Number of items inserted"));
    assertEquals(376L, info.get("Size"));
    assertEquals(4L, info.get("Expansion rate"));
    assertEquals(1L, info.get("Number of filters"));
    assertEquals(10L, info.get("Bucket size"));
    assertEquals(20L, info.get("Max iterations"));
    assertEquals(0L, info.get("Number of items deleted"));
  }

  @Test
  public void testAdd() {
    jedis.cfReserve("cuckoo5", 64000);
    assertTrue(jedis.cfAdd("cuckoo5", "test"));
    Map<String, Object> info = jedis.cfInfo("cuckoo5");
    assertEquals(1L, info.get("Number of items inserted"));
  }

  @Test
  public void testAddNxItemDoesExist() {
    jedis.cfReserve("cuckoo6", 64000);
    assertTrue(jedis.cfAddNx("cuckoo6", "filter"));
  }

  @Test
  public void testAddNxItemExists() {
    jedis.cfReserve("cuckoo7", 64000);
    jedis.cfAdd("cuckoo7", "filter");
    assertFalse(jedis.cfAddNx("cuckoo7", "filter"));
  }

  @Test
  public void testInsert() {
    assertEquals(Arrays.asList(true), jedis.cfInsert("cuckoo8", "foo"));
  }

  @Test
  public void testInsertWithCapacity() {
    assertEquals(Arrays.asList(true), jedis.cfInsert("cuckoo9",
        CFInsertParams.insertParams().capacity(1000), "foo"));
  }

  @Test
  public void testInsertNoCreateFilterDoesNotExist() {
    try {
      jedis.cfInsert("cuckoo10", CFInsertParams.insertParams().noCreate(), "foo", "bar");
      fail();
    } catch (JedisDataException e) {
      assertEquals("ERR not found", e.getMessage());
    }
  }

  @Test
  public void testInsertNoCreateFilterExists() {
    jedis.cfInsert("cuckoo11", "bar");
    assertEquals(Arrays.asList(true, true), jedis.cfInsert("cuckoo11",
        CFInsertParams.insertParams().noCreate(), "foo", "bar"));
  }

  @Test
  public void testInsertNx() {
    assertEquals(Arrays.asList(true), jedis.cfInsertNx("cuckoo12", "bar"));
  }

  @Test
  public void testInsertNxWithCapacity() {
    jedis.cfInsertNx("cuckoo13", "bar");
    assertEquals(Arrays.asList(false), jedis.cfInsertNx("cuckoo13",
        CFInsertParams.insertParams().capacity(1000), "bar"));
  }

  @Test
  public void testInsertNxMultiple() {
    jedis.cfInsertNx("cuckoo14", "foo");
    jedis.cfInsertNx("cuckoo14", "bar");
    assertEquals(Arrays.asList(false, false, true),
        jedis.cfInsertNx("cuckoo14", "foo", "bar", "baz"));
  }

  @Test
  public void testInsertNxNoCreate() {
    try {
      jedis.cfInsertNx("cuckoo15", CFInsertParams.insertParams().noCreate(), "foo", "bar");
      fail();
    } catch (JedisDataException e) {
      assertEquals("ERR not found", e.getMessage());
    }
  }

  @Test
  public void testExistsItemDoesntExist() {
    assertFalse(jedis.cfExists("cuckoo16", "foo"));
    assertEquals(Collections.singletonList(false), jedis.cfMExists("cuckoo16", "foo"));
  }

  @Test
  public void testExistsItemExists() {
    jedis.cfInsert("cuckoo17", "foo");
    assertTrue(jedis.cfExists("cuckoo17", "foo"));
    assertEquals(Collections.singletonList(true), jedis.cfMExists("cuckoo17", "foo"));
  }

  @Test
  public void testMExistsMixedItems() {
    jedis.cfInsert("cuckoo27", "foo");
    assertEquals(Arrays.asList(true, false), jedis.cfMExists("cuckoo27", "foo", "bar"));
    assertEquals(Arrays.asList(false, true), jedis.cfMExists("cuckoo27", "bar", "foo"));
  }

  @Test
  public void testDeleteItemDoesntExist() {
    jedis.cfInsert("cuckoo8", "bar");
    assertFalse(jedis.cfDel("cuckoo8", "foo"));
  }

  @Test
  public void testDeleteItemExists() {
    jedis.cfInsert("cuckoo18", "foo");
    assertTrue(jedis.cfDel("cuckoo18", "foo"));
  }

  @Test
  public void testDeleteFilterDoesNotExist() {
    Exception ex = assertThrows(JedisDataException.class, () -> {
      jedis.cfDel("cuckoo19", "foo");
    });
    assertTrue(ex.getMessage().contains("Not found"));
  }

  @Test
  public void testCountFilterDoesNotExist() {
    assertEquals(0L, jedis.cfCount("cuckoo20", "filter"));
  }

  @Test
  public void testCountFilterExist() {
    jedis.cfInsert("cuckoo21", "foo");
    assertEquals(0L, jedis.cfCount("cuckoo21", "filter"));
  }

  @Test
  public void testCountItemExists() {
    jedis.cfInsert("cuckoo22", "foo");
    assertEquals(1L, jedis.cfCount("cuckoo22", "foo"));
  }

  @Test
  public void testInfoFilterDoesNotExists() {
    Exception ex = assertThrows(JedisDataException.class, () -> {
      jedis.cfInfo("cuckoo23");
    });
    assertTrue(ex.getMessage().contains("ERR not found"));
  }

  @Test
  @Timeout(2)
  public void testScanDumpAndLoadChunk() {
    jedis.cfReserve("cuckoo24", 100L, CFReserveParams.reserveParams().bucketSize(50));
    jedis.cfAdd("cuckoo24-dump", "a");

    long iterator = 0;
    while (true) {
      Map.Entry<Long, byte[]> chunkData = jedis.cfScanDump("cuckoo24-dump", iterator);
      iterator = chunkData.getKey();
      if (iterator == 0L) break;
      assertEquals("OK", jedis.cfLoadChunk("cuckoo24-load", iterator, chunkData.getValue()));
    }

    assertEquals(jedis.cfInfo("cuckoo24-dump"), jedis.cfInfo("cuckoo24-load"));
    assertTrue(jedis.cfExists("cuckoo24-load", "a"));
  }
}

