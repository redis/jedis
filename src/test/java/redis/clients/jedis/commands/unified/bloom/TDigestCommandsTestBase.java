package redis.clients.jedis.commands.unified.bloom;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.bloom.TDigestMergeParams;
import redis.clients.jedis.commands.unified.UnifiedJedisCommandsTestBase;

/**
 * Base test class for T-Digest commands using the UnifiedJedis pattern.
 */
@Tag("bloom")
public abstract class TDigestCommandsTestBase extends UnifiedJedisCommandsTestBase {

  private static final Random random = new Random();

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("modules-docker");
  }

  public TDigestCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  private void assertMergedUnmergedNodes(String key, int mergedNodes, int unmergedNodes) {
    Map<String, Object> info = jedis.tdigestInfo(key);
    assertEquals(Long.valueOf(mergedNodes), info.get("Merged nodes"));
    assertEquals(Long.valueOf(unmergedNodes), info.get("Unmerged nodes"));
  }

  private void assertTotalWeight(String key, long totalWeight) {
    Map<String, Object> info = jedis.tdigestInfo(key);
    assertEquals(totalWeight,
      (Long) info.get("Merged weight") + (Long) info.get("Unmerged weight"));
  }

  @Test
  public void createSimple() {
    assertEquals("OK", jedis.tdigestCreate("td-simple"));
    Map<String, Object> info = jedis.tdigestInfo("td-simple");
    assertEquals(100L, info.get("Compression"));
  }

  @Test
  public void createAndInfo() {
    for (int i = 100; i < 1000; i += 100) {
      String key = "td-" + i;
      assertEquals("OK", jedis.tdigestCreate(key, i));
      Map<String, Object> info = jedis.tdigestInfo(key);
      assertEquals(Long.valueOf(i), info.get("Compression"));
    }
  }

  @Test
  public void reset() {
    jedis.tdigestCreate("reset", 100);
    assertMergedUnmergedNodes("reset", 0, 0);

    assertEquals("OK", jedis.tdigestReset("reset"));
    assertMergedUnmergedNodes("reset", 0, 0);

    jedis.tdigestAdd("reset", randomValue(), randomValue(), randomValue());
    assertMergedUnmergedNodes("reset", 0, 3);

    assertEquals("OK", jedis.tdigestReset("reset"));
    assertMergedUnmergedNodes("reset", 0, 0);
  }

  @Test
  public void add() {
    jedis.tdigestCreate("tdadd", 100);

    assertEquals("OK", jedis.tdigestAdd("tdadd", randomValue()));
    assertMergedUnmergedNodes("tdadd", 0, 1);

    assertEquals("OK",
      jedis.tdigestAdd("tdadd", randomValue(), randomValue(), randomValue(), randomValue()));
    assertMergedUnmergedNodes("tdadd", 0, 5);
  }

  @Test
  public void merge() {
    jedis.tdigestCreate("{key}td2", 100);
    jedis.tdigestCreate("{key}td4m", 100);

    assertEquals("OK", jedis.tdigestMerge("{key}td2", "{key}td4m"));
    assertMergedUnmergedNodes("{key}td2", 0, 0);

    jedis.tdigestAdd("{key}td2", 1, 1, 1);
    jedis.tdigestAdd("{key}td4m", 1, 1);

    assertEquals("OK", jedis.tdigestMerge("{key}td2", "{key}td4m"));
    assertMergedUnmergedNodes("{key}td2", 3, 2);
  }

  @Test
  public void mergeMultiAndParams() {
    jedis.tdigestCreate("{key}from1", 100);
    jedis.tdigestCreate("{key}from2", 200);

    jedis.tdigestAdd("{key}from1", 1d);
    jedis.tdigestAdd("{key}from2", weightedValue(1d, 10));

    assertEquals("OK", jedis.tdigestMerge("{key}to", "{key}from1", "{key}from2"));
    assertTotalWeight("{key}to", 11L);

    assertEquals("OK",
      jedis.tdigestMerge(TDigestMergeParams.mergeParams().compression(50).override(), "{key}to",
        "{key}from1", "{key}from2"));
    assertEquals(50L, jedis.tdigestInfo("{key}to").get("Compression"));
  }

  @Test
  public void cdf() {
    jedis.tdigestCreate("tdcdf", 100);
    assertEquals(singletonList(Double.NaN), jedis.tdigestCDF("tdcdf", 50));

    jedis.tdigestAdd("tdcdf", 1, 1, 1);
    jedis.tdigestAdd("tdcdf", 100, 100);
    assertEquals(singletonList(0.6), jedis.tdigestCDF("tdcdf", 50));
    jedis.tdigestCDF("tdcdf", 25, 50, 75);
  }

  @Test
  public void quantile() {
    jedis.tdigestCreate("tdqnt", 100);
    assertEquals(singletonList(Double.NaN), jedis.tdigestQuantile("tdqnt", 0.5));

    jedis.tdigestAdd("tdqnt", 1, 1, 1);
    jedis.tdigestAdd("tdqnt", 100, 100);
    assertEquals(singletonList(1.0), jedis.tdigestQuantile("tdqnt", 0.5));
  }

  @Test
  public void minAndMax() {
    final String key = "tdmnmx";
    jedis.tdigestCreate(key, 100);
    assertEquals(Double.NaN, jedis.tdigestMin(key), 0d);
    assertEquals(Double.NaN, jedis.tdigestMax(key), 0d);

    jedis.tdigestAdd(key, 2);
    jedis.tdigestAdd(key, 5);
    assertEquals(2d, jedis.tdigestMin(key), 0.01);
    assertEquals(5d, jedis.tdigestMax(key), 0.01);
  }

  @Test
  public void trimmedMean() {
    final String key = "trimmed_mean";
    jedis.tdigestCreate(key, 500);

    for (int i = 0; i < 20; i++) {
      jedis.tdigestAdd(key, (double) i);
    }

    assertEquals(9.5, jedis.tdigestTrimmedMean(key, 0.1, 0.9), 0.01);
    assertEquals(9.5, jedis.tdigestTrimmedMean(key, 0.0, 1.0), 0.01);
    assertEquals(4.5, jedis.tdigestTrimmedMean(key, 0.0, 0.5), 0.01);
    assertEquals(14.5, jedis.tdigestTrimmedMean(key, 0.5, 1.0), 0.01);
  }

  @Test
  public void rankCommands() {
    final String key = "ranks";
    jedis.tdigestCreate(key);
    jedis.tdigestAdd(key, 2d, 3d, 5d);
    assertEquals(Arrays.asList(0L, 2L), jedis.tdigestRank(key, 2d, 4d));
    assertEquals(Arrays.asList(0L, 1L), jedis.tdigestRevRank(key, 5d, 4d));
    assertEquals(Arrays.asList(2d, 3d), jedis.tdigestByRank(key, 0L, 1L));
    assertEquals(Arrays.asList(5d, 3d), jedis.tdigestByRevRank(key, 0L, 1L));
  }

  private static double randomValue() {
    return random.nextDouble() * 10000;
  }

  private static double[] weightedValue(double value, int weight) {
    double[] values = new double[weight];
    Arrays.fill(values, value);
    return values;
  }
}
