package redis.clients.jedis.modules.bloom;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.bloom.TDigestMergeParams;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;

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

  private void assertTotalWeight(String key, long totalWeight) {
    Map<String, Object> info = client.tdigestInfo(key);
    assertEquals(totalWeight, (Long) info.get("Merged weight")
        + (Long) info.get("Unmerged weight"));
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

//    client.tdigestAdd("reset", randomValueWeight(), randomValueWeight(), randomValueWeight());
    client.tdigestAdd("reset", randomValue(), randomValue(), randomValue());
    assertMergedUnmergedNodes("reset", 0, 3);

    assertEquals("OK", client.tdigestReset("reset"));
    assertMergedUnmergedNodes("reset", 0, 0);
  }

  @Test
  public void add() {
    client.tdigestCreate("tdadd", 100);

//    assertEquals("OK", client.tdigestAdd("tdadd", randomValueWeight()));
    assertEquals("OK", client.tdigestAdd("tdadd", randomValue()));
    assertMergedUnmergedNodes("tdadd", 0, 1);

//    assertEquals("OK", client.tdigestAdd("tdadd", randomValueWeight(), randomValueWeight(), randomValueWeight(), randomValueWeight()));
    assertEquals("OK", client.tdigestAdd("tdadd", randomValue(), randomValue(), randomValue(), randomValue()));
    assertMergedUnmergedNodes("tdadd", 0, 5);
  }

  @Test
  public void merge() {
    client.tdigestCreate("td2", 100);
    client.tdigestCreate("td4m", 100);

    assertEquals("OK", client.tdigestMerge("td2", "td4m"));
    assertMergedUnmergedNodes("td2", 0, 0);

//    client.tdigestAdd("td2", definedValueWeight(1, 1), definedValueWeight(1, 1), definedValueWeight(1, 1));
//    client.tdigestAdd("td4m", definedValueWeight(1, 100), definedValueWeight(1, 100));
    client.tdigestAdd("td2", 1, 1, 1);
    client.tdigestAdd("td4m", 1, 1);

    assertEquals("OK", client.tdigestMerge("td2", "td4m"));
    assertMergedUnmergedNodes("td2", 3, 2);
  }

  @Test
  public void mergeMultiAndParams() {
    client.tdigestCreate("from1", 100);
    client.tdigestCreate("from2", 200);

//    client.tdigestAdd("from1", KeyValue.of(1d, 1l));
//    client.tdigestAdd("from2", KeyValue.of(1d, 10l));
    client.tdigestAdd("from1", 1d);
    client.tdigestAdd("from2", weightedValue(1d, 10));

    assertEquals("OK", client.tdigestMerge("to", "from1", "from2"));
    assertTotalWeight("to", 11l);

    assertEquals("OK", client.tdigestMerge(TDigestMergeParams.mergeParams()
        .compression(50).override(), "to", "from1", "from2"));
    assertEquals(Long.valueOf(50), client.tdigestInfo("to").get("Compression"));
  }

  @Test
  public void cdf() {
    client.tdigestCreate("tdcdf", 100);
    assertEquals(singletonList(Double.NaN), client.tdigestCDF("tdcdf", 50));

//    client.tdigestAdd("tdcdf", definedValueWeight(1, 1), definedValueWeight(1, 1), definedValueWeight(1, 1));
//    client.tdigestAdd("tdcdf", definedValueWeight(100, 1), definedValueWeight(100, 1));
    client.tdigestAdd("tdcdf", 1, 1, 1);
    client.tdigestAdd("tdcdf", 100, 100);
    assertEquals(singletonList(0.6), client.tdigestCDF("tdcdf", 50));
    client.tdigestCDF("tdcdf", 25, 50, 75);
  }

  @Test
  public void quantile() {
    client.tdigestCreate("tdqnt", 100);
    assertEquals(singletonList(Double.NaN), client.tdigestQuantile("tdqnt", 0.5));

//    client.tdigestAdd("tdqnt", definedValueWeight(1, 1), definedValueWeight(1, 1), definedValueWeight(1, 1));
//    client.tdigestAdd("tdqnt", definedValueWeight(100, 1), definedValueWeight(100, 1));
    client.tdigestAdd("tdqnt", 1, 1, 1);
    client.tdigestAdd("tdqnt", 100, 100);
    assertEquals(singletonList(1.0), client.tdigestQuantile("tdqnt", 0.5));
  }

  @Test
  public void minAndMax() {
    final String key = "tdmnmx";
    client.tdigestCreate(key, 100);
    assertEquals(Double.NaN, client.tdigestMin(key), 0d);
    assertEquals(Double.NaN, client.tdigestMax(key), 0d);

//    client.tdigestAdd(key, definedValueWeight(2, 1));
//    client.tdigestAdd(key, definedValueWeight(5, 1));
    client.tdigestAdd(key, 2);
    client.tdigestAdd(key, 5);
    assertEquals(2d, client.tdigestMin(key), 0.01);
    assertEquals(5d, client.tdigestMax(key), 0.01);
  }

  @Test
  public void trimmedMean() {
    final String key = "trimmed_mean";
    client.tdigestCreate(key, 500);

    for (int i = 0; i < 20; i++) {
//      client.tdigestAdd(key, KeyValue.of(Double.valueOf(i), 1l));
      client.tdigestAdd(key, (double) i);
    }

    assertEquals(9.5, client.tdigestTrimmedMean(key, 0.1, 0.9), 0.01);
    assertEquals(9.5, client.tdigestTrimmedMean(key, 0.0, 1.0), 0.01);
    assertEquals(4.5, client.tdigestTrimmedMean(key, 0.0, 0.5), 0.01);
    assertEquals(14.5, client.tdigestTrimmedMean(key, 0.5, 1.0), 0.01);
  }

  @Test
  public void rankCommands() {
    final String key = "ranks";
    client.tdigestCreate(key);
    client.tdigestAdd(key, 2d, 3d, 5d);
    assertEquals(Arrays.asList(1l, 2l), client.tdigestRank(key, 2d, 4d));
    assertEquals(Arrays.asList(0l, 1l), client.tdigestRevRank(key, 5d, 4d));
    assertEquals(Arrays.asList(2d, 3d), client.tdigestByRank(key, 0l, 1l));
    assertEquals(Arrays.asList(5d, 3d), client.tdigestByRevRank(key, 0l, 1l));
  }
//
//  private static KeyValue<Double, Long> randomValueWeight() {
//    return new KeyValue<>(random.nextDouble() * 10000, Math.abs(random.nextInt()) + 1l);
//  }
//
//  private static KeyValue<Double, Long> definedValueWeight(double value, long weight) {
//    return new KeyValue<>(value, weight);
//  }

  private static double randomValue() {
    return random.nextDouble() * 10000;
  }

  private static double[] weightedValue(double value, int weight) {
    double[] values = new double[weight];
    Arrays.fill(values, value);
    return values;
  }
}
