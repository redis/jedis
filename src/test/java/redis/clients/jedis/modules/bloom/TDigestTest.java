package redis.clients.jedis.modules.bloom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Map;
import java.util.Random;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;
import redis.clients.jedis.util.KeyValue;

public class TDigestTest extends RedisModuleCommandsTestBase {

  private static final Random random = new Random();

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }
//
//  @AfterClass
//  public static void tearDown() {
////    RedisModuleCommandsTestBase.tearDown();
//  }

  private void assertMergedUnmergedNodes(String key, int mergedNodes, int unmergedNodes) {
    Map<String, Object> info = client.tdigestInfo(key);
    assertEquals(Long.valueOf(mergedNodes), info.get("Merged nodes"));
    assertEquals(Long.valueOf(unmergedNodes), info.get("Unmerged nodes"));
  }

  @Test
  public void createSimple() {
    assertEquals("OK", client.tdigestCreate("td-simple"));

    Map<String, Object> info = client.tdigestInfo("td-simple");
    assertEquals(Long.valueOf(100), info.get("Compression"));
  }

  @Test
  public void createAndInfo() {
    for (int i = 100; i < 1000; i += 100) {
      String key = "td-" + i;
      assertEquals("OK", client.tdigestCreate(key, i));

      Map<String, Object> info = client.tdigestInfo(key);
      assertEquals(Long.valueOf(i), info.get("Compression"));
    }
  }

  @Test
  public void reset() {
    client.tdigestCreate("reset", 100);
    assertMergedUnmergedNodes("reset", 0, 0);

    // on empty
    assertEquals("OK", client.tdigestReset("reset"));
    assertMergedUnmergedNodes("reset", 0, 0);

    client.tdigestAdd("reset", randomValueWeight(), randomValueWeight(), randomValueWeight());
    assertMergedUnmergedNodes("reset", 0, 3);

    assertEquals("OK", client.tdigestReset("reset"));
    assertMergedUnmergedNodes("reset", 0, 0);
  }

  @Test
  public void add() {
    client.tdigestCreate("tdadd", 100);

    assertEquals("OK", client.tdigestAdd("tdadd", randomValueWeight()));
    assertMergedUnmergedNodes("tdadd", 0, 1);

    assertEquals("OK", client.tdigestAdd("tdadd", randomValueWeight(), randomValueWeight(), randomValueWeight(), randomValueWeight()));
    assertMergedUnmergedNodes("tdadd", 0, 5);
  }

  @Test
  public void merge() {
    client.tdigestCreate("td2", 100);
    client.tdigestCreate("td4m", 100);

    assertEquals("OK", client.tdigestMerge("td2", "td4m"));
    assertMergedUnmergedNodes("td2", 0, 0);

    client.tdigestAdd("td2", definedValueWeight(1, 1), definedValueWeight(1, 1), definedValueWeight(1, 1));
    client.tdigestAdd("td4m", definedValueWeight(1, 100), definedValueWeight(1, 100));

    assertEquals("OK", client.tdigestMerge("td2", "td4m"));
    assertMergedUnmergedNodes("td2", 3, 2);
  }

  @Test
  public void cdf() {
    try {
      client.tdigestCDF("tdcdf", 50);
      fail("key does not exist");
    } catch (JedisDataException jde) {
      assertEquals("ERR T-Digest: key does not exist", jde.getMessage());
    }

    client.tdigestCreate("tdcdf", 100);
    assertEquals(Double.NaN, client.tdigestCDF("tdcdf", 50), 0d);

    client.tdigestAdd("tdcdf", definedValueWeight(1, 1), definedValueWeight(1, 1), definedValueWeight(1, 1));
    client.tdigestAdd("tdcdf", definedValueWeight(100, 1), definedValueWeight(100, 1));
    assertEquals(0.6, client.tdigestCDF("tdcdf", 50), 0.01);
  }

  @Test
  public void quantile() {
    try {
      client.tdigestQuantile("tdqnt", 0.5);
      fail("key does not exist");
    } catch (JedisDataException jde) {
      assertEquals("ERR T-Digest: key does not exist", jde.getMessage());
    }

    client.tdigestCreate("tdqnt", 100);
    assertEquals(Collections.singletonMap(0.5, Double.NaN), client.tdigestQuantile("tdqnt", 0.5));

    client.tdigestAdd("tdqnt", definedValueWeight(1, 1), definedValueWeight(1, 1), definedValueWeight(1, 1));
    client.tdigestAdd("tdqnt", definedValueWeight(100, 1), definedValueWeight(100, 1));
    assertEquals(Collections.singletonMap(0.5, 1.0), client.tdigestQuantile("tdqnt", 0.5));
  }

  @Test
  public void minAndMax() {
    final String key = "tdmnmx";
    try {
      client.tdigestMin(key);
      fail("key does not exist");
    } catch (JedisDataException jde) {
      assertEquals("ERR T-Digest: key does not exist", jde.getMessage());
    }
    try {
      client.tdigestMax(key);
      fail("key does not exist");
    } catch (JedisDataException jde) {
      assertEquals("ERR T-Digest: key does not exist", jde.getMessage());
    }

    client.tdigestCreate(key, 100);
    assertEquals(Double.MAX_VALUE, client.tdigestMin(key), 0d);
    assertEquals(-Double.MAX_VALUE, client.tdigestMax(key), 0d);

    client.tdigestAdd(key, definedValueWeight(2, 1));
    client.tdigestAdd(key, definedValueWeight(5, 1));
    assertEquals(2d, client.tdigestMin(key), 0.01);
    assertEquals(5d, client.tdigestMax(key), 0.01);
  }

  private static KeyValue<Double, Double> randomValueWeight() {
    return new KeyValue<>(random.nextDouble() * 10000, random.nextDouble() * 500 + 1);
  }

  private static KeyValue<Double, Double> definedValueWeight(double value, double weight) {
    return new KeyValue<>(value, weight);
  }
}
