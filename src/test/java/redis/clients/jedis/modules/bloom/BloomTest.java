package redis.clients.jedis.modules.bloom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.bloom.BFInsertParams;
import redis.clients.jedis.bloom.BFReserveParams;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;

public class BloomTest extends RedisModuleCommandsTestBase {

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }
//
//  @AfterClass
//  public static void tearDown() {
////    RedisModuleCommandsTestBase.tearDown();
//  }

  @Test
  public void reserveBasic() {
    client.bfReserve("myBloom", 0.001, 100L);
    assertTrue(client.bfAdd("myBloom", "val1"));
    assertTrue(client.bfExists("myBloom", "val1"));
    assertFalse(client.bfExists("myBloom", "val2"));
  }

  @Test(expected = JedisDataException.class)
  public void reserveValidateZeroCapacity() {
    client.bfReserve("myBloom", 0.001, 0L);
  }

  @Test(expected = JedisDataException.class)
  public void reserveValidateZeroError() {
    client.bfReserve("myBloom", 0d, 100L);
  }

  @Test(expected = JedisDataException.class)
  public void reserveAlreadyExists() {
    client.bfReserve("myBloom", 0.1, 100);
    client.bfReserve("myBloom", 0.1, 100);
  }

  @Test
  public void reserveV2() {
    client.bfReserve("reserve-basic", 0.001, 2);
    assertEquals(Arrays.asList(true), client.bfInsert("reserve-basic", "a"));
    assertEquals(Arrays.asList(true), client.bfInsert("reserve-basic", "b"));
    assertEquals(Arrays.asList(true), client.bfInsert("reserve-basic", "c"));
  }

  @Test
  public void reserveEmptyParams() {
    client.bfReserve("empty-param", 0.001, 2, BFReserveParams.reserveParams());
    assertEquals(Arrays.asList(true), client.bfInsert("empty-param", "a"));
    assertEquals(Arrays.asList(true), client.bfInsert("empty-param", "b"));
    assertEquals(Arrays.asList(true), client.bfInsert("empty-param", "c"));
  }

  @Test
  public void reserveNonScaling() {
    client.bfReserve("nonscaling", 0.001, 2, BFReserveParams.reserveParams().nonScaling());
    assertEquals(Arrays.asList(true), client.bfInsert("nonscaling", "a"));
    assertEquals(Arrays.asList(true), client.bfInsert("nonscaling", "b"));
    assertEquals(Arrays.asList((Boolean) null), client.bfInsert("nonscaling", "c"));
  }

  @Test
  public void reserveExpansion() {
    // bf.reserve bfexpansion 0.001 1000 expansion 4
    client.bfReserve("bfexpansion", 0.001, 1000, BFReserveParams.reserveParams().expansion(4));
    assertEquals(Arrays.asList(true), client.bfInsert("bfexpansion", "a"));
    assertEquals(Arrays.asList(true), client.bfInsert("bfexpansion",
        BFInsertParams.insertParams().noCreate(), "b"));
  }

  @Test
  public void addExistsString() {
    assertTrue(client.bfAdd("newFilter", "foo"));
    assertTrue(client.bfExists("newFilter", "foo"));
    assertFalse(client.bfExists("newFilter", "bar"));
    assertFalse(client.bfAdd("newFilter", "foo"));
  }
//
//  @Test
//  public void addExistsByte() {
//    assertTrue(client.bfAdd("newFilter", "foo".getBytes()));
//    assertFalse(client.bfAdd("newFilter", "foo".getBytes()));
//    assertTrue(client.bfExists("newFilter", "foo".getBytes()));
//    assertFalse(client.bfExists("newFilter", "bar".getBytes()));
//  }

  @Test
  public void testExistsNonExist() {
    assertFalse(client.bfExists("nonExist", "foo"));
  }

  @Test
  public void addExistsMulti() {
    List<Boolean> rv = client.bfMAdd("newFilter", "foo", "bar", "baz");
    assertEquals(Arrays.asList(true, true, true), rv);

    rv = client.bfMAdd("newFilter", "newElem", "bar", "baz");
    assertEquals(Arrays.asList(true, false, false), rv);

//    // Try with bytes
//    rv = client.bfMAdd("newFilter", new byte[]{1}, new byte[]{2}, new byte[]{3});
//    assertEquals(3, rv.length);
//    for (boolean i : rv) {
//      assertTrue(i);
//    }
//
//    rv = client.bfMAdd("newFilter", new byte[]{0}, new byte[]{3});
//    assertEquals(2, rv.length);
//    assertTrue(rv[0]);
//    assertFalse(rv[1]);
//
//    rv = client.existsMulti("newFilter", new byte[]{0}, new byte[]{1}, new byte[]{2}, new byte[]{3}, new byte[]{5});
//    assertEquals(5, rv.length);
//    assertTrue(rv[0]);
//    assertTrue(rv[1]);
//    assertTrue(rv[2]);
//    assertTrue(rv[3]);
//    assertFalse(rv[4]);
  }

  @Test
  public void testExample() {
    // Simple bloom filter using default module settings
    client.bfAdd("simpleBloom", "Mark");
    // Does "Mark" now exist?
    client.bfExists("simpleBloom", "Mark"); // true
    client.bfExists("simpleBloom", "Farnsworth"); // False

    // If you have a long list of items to check/add, you can use the
    // "multi" methods
    client.bfMAdd("simpleBloom", "foo", "bar", "baz", "bat", "bag");

    // Check if they exist:
    List<Boolean> rv = client.bfMExists("simpleBloom", "foo", "bar", "baz", "bat", "Mark", "nonexist");
    // All items except the last one will be 'true'
    assertEquals(Arrays.asList(true, true, true, true, true, false), rv);

    // Reserve a "customized" bloom filter
    client.bfReserve("specialBloom", 0.0001, 10000);
    client.bfAdd("specialBloom", "foo");
  }

  @Test
  public void testInsert() {
    client.bfInsert("b1", new BFInsertParams().capacity(1L), "1");
    assertTrue(client.bfExists("b1", "1"));

    // returning an error if the filter does not already exist
    try {
      client.bfInsert("b2", new BFInsertParams().noCreate(), "1");
      fail("Should error if the filter does not already exist.");
    } catch (JedisDataException e) {
      assertEquals("ERR not found", e.getMessage());
    }

    client.bfInsert("b3", new BFInsertParams().capacity(1L).error(0.0001), "2");
    assertTrue(client.bfExists("b3", "2"));
  }

  @Test
  public void issue49() {
    BFInsertParams insertOptions = new BFInsertParams();
    List<Boolean> insert = client.bfInsert("mykey", insertOptions, "a", "b", "c");
    assertEquals(3, insert.size());
  }

  @Test
  public void testInfo() {
    client.bfInsert("test_info", new BFInsertParams().capacity(1L), "1");
    Map<String, Object> info = client.bfInfo("test_info");
    assertEquals(Long.valueOf(1), info.get("Number of items inserted"));

    // returning an error if the filter does not already exist
    try {
      client.bfInfo("not_exist");
      fail("Should error if the filter does not already exist.");
    } catch (JedisDataException e) {
      assertEquals("ERR not found", e.getMessage());
    }
  }

  @Test
  public void insertNonScaling() {
    List<Boolean> insert = client.bfInsert("nonscaling_err", BFInsertParams.insertParams()
        .capacity(4).nonScaling(), "a", "b", "c");
    assertEquals(Arrays.asList(true, true, true), insert);

    insert = client.bfInsert("nonscaling_err", "d", "e");
    assertEquals(Arrays.asList(true, null), insert);
  }

  @Test
  public void insertExpansion() {
    // BF.INSERT bfexpansion CAPACITY 3 expansion 3 ITEMS a b c d e f g h j k l o i u y t r e w q
    List<Boolean> insert = client.bfInsert("bfexpansion", BFInsertParams.insertParams()
        .capacity(3).expansion(3), "a", "b", "c", "d", "e", "f", "g", "h", "j", "k", "l",
        "o", "i", "u", "y", "t", "r", "e", "w", "q");
    assertEquals(20, insert.size());
  }

  @Test(timeout = 2000L)
  public void testScanDumpAndLoadChunk() {
    client.bfAdd("bloom-dump", "a");

    long iterator = 0;
    while (true) {
      Map.Entry<Long, byte[]> chunkData = client.bfScanDump("bloom-dump", iterator);
      iterator = chunkData.getKey();
      if (iterator == 0L) break;
      assertEquals("OK", client.bfLoadChunk("bloom-load", iterator, chunkData.getValue()));
    }

    // check for properties
    assertEquals(client.bfInfo("bloom-dump"), client.bfInfo("bloom-load"));
    // check for existing items
    assertTrue(client.bfExists("bloom-load", "a"));
  }
}
