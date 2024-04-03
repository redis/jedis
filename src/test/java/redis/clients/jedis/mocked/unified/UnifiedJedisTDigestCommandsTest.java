package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import redis.clients.jedis.bloom.TDigestMergeParams;

public class UnifiedJedisTDigestCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testTdigestAdd() {
    String key = "testTDigest";
    double[] values = { 1.0, 2.0, 3.0 };
    String expectedResponse = "OK";

    when(commandObjects.tdigestAdd(key, values)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.tdigestAdd(key, values);

    assertThat(result, sameInstance(expectedResponse));
    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).tdigestAdd(key, values);
  }

  @Test
  public void testTdigestByRank() {
    String key = "testTDigest";
    long[] ranks = { 1, 2 };
    List<Double> expectedResponse = Arrays.asList(0.1, 0.2);

    when(commandObjects.tdigestByRank(key, ranks)).thenReturn(listDoubleCommandObject);
    when(commandExecutor.executeCommand(listDoubleCommandObject)).thenReturn(expectedResponse);

    List<Double> result = jedis.tdigestByRank(key, ranks);

    assertThat(result, sameInstance(expectedResponse));
    verify(commandExecutor).executeCommand(listDoubleCommandObject);
    verify(commandObjects).tdigestByRank(key, ranks);
  }

  @Test
  public void testTdigestByRevRank() {
    String key = "testTDigest";
    long[] ranks = { 1, 2 };
    List<Double> expectedResponse = Arrays.asList(9.9, 9.8);

    when(commandObjects.tdigestByRevRank(key, ranks)).thenReturn(listDoubleCommandObject);
    when(commandExecutor.executeCommand(listDoubleCommandObject)).thenReturn(expectedResponse);

    List<Double> result = jedis.tdigestByRevRank(key, ranks);

    assertThat(result, sameInstance(expectedResponse));
    verify(commandExecutor).executeCommand(listDoubleCommandObject);
    verify(commandObjects).tdigestByRevRank(key, ranks);
  }

  @Test
  public void testTdigestCDF() {
    String key = "testTDigest";
    double[] values = { 0.5, 0.9 };
    List<Double> expectedResponse = Arrays.asList(0.1, 0.95);

    when(commandObjects.tdigestCDF(key, values)).thenReturn(listDoubleCommandObject);
    when(commandExecutor.executeCommand(listDoubleCommandObject)).thenReturn(expectedResponse);

    List<Double> result = jedis.tdigestCDF(key, values);

    assertThat(result, sameInstance(expectedResponse));
    verify(commandExecutor).executeCommand(listDoubleCommandObject);
    verify(commandObjects).tdigestCDF(key, values);
  }

  @Test
  public void testTdigestCreate() {
    String key = "testTDigest";
    String expectedResponse = "OK";

    when(commandObjects.tdigestCreate(key)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.tdigestCreate(key);

    assertThat(result, sameInstance(expectedResponse));
    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).tdigestCreate(key);
  }

  @Test
  public void testTdigestCreateWithCompression() {
    String key = "testTDigest";
    int compression = 100;
    String expectedResponse = "OK";

    when(commandObjects.tdigestCreate(key, compression)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.tdigestCreate(key, compression);

    assertThat(result, sameInstance(expectedResponse));
    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).tdigestCreate(key, compression);
  }

  @Test
  public void testTdigestInfo() {
    String key = "testTDigest";
    Map<String, Object> expectedResponse = new HashMap<>();
    expectedResponse.put("compression", 100);
    expectedResponse.put("capacity", 1000);
    expectedResponse.put("merged_nodes", 500);
    expectedResponse.put("unmerged_nodes", 50);
    expectedResponse.put("total_compressions", 10);

    when(commandObjects.tdigestInfo(key)).thenReturn(mapStringObjectCommandObject);
    when(commandExecutor.executeCommand(mapStringObjectCommandObject)).thenReturn(expectedResponse);

    Map<String, Object> result = jedis.tdigestInfo(key);

    assertThat(result, sameInstance(expectedResponse));
    verify(commandExecutor).executeCommand(mapStringObjectCommandObject);
    verify(commandObjects).tdigestInfo(key);
  }

  @Test
  public void testTdigestMax() {
    String key = "testTDigest";
    double expectedResponse = 10.0;

    when(commandObjects.tdigestMax(key)).thenReturn(doubleCommandObject);
    when(commandExecutor.executeCommand(doubleCommandObject)).thenReturn(expectedResponse);

    double result = jedis.tdigestMax(key);

    assertThat(result, equalTo(expectedResponse));
    verify(commandExecutor).executeCommand(doubleCommandObject);
    verify(commandObjects).tdigestMax(key);
  }

  @Test
  public void testTdigestMerge() {
    String destinationKey = "destTDigest";
    String[] sourceKeys = { "sourceTDigest1", "sourceTDigest2" };
    String expectedResponse = "OK";

    when(commandObjects.tdigestMerge(destinationKey, sourceKeys)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.tdigestMerge(destinationKey, sourceKeys);

    assertThat(result, sameInstance(expectedResponse));
    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).tdigestMerge(destinationKey, sourceKeys);
  }

  @Test
  public void testTdigestMergeWithParams() {
    TDigestMergeParams mergeParams = new TDigestMergeParams().compression(200);
    String destinationKey = "destTDigest";
    String[] sourceKeys = { "sourceTDigest1", "sourceTDigest2" };
    String expectedResponse = "OK";

    when(commandObjects.tdigestMerge(mergeParams, destinationKey, sourceKeys)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.tdigestMerge(mergeParams, destinationKey, sourceKeys);

    assertThat(result, sameInstance(expectedResponse));
    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).tdigestMerge(mergeParams, destinationKey, sourceKeys);
  }

  @Test
  public void testTdigestMin() {
    String key = "testTDigest";
    double expectedResponse = 0.1;

    when(commandObjects.tdigestMin(key)).thenReturn(doubleCommandObject);
    when(commandExecutor.executeCommand(doubleCommandObject)).thenReturn(expectedResponse);

    double result = jedis.tdigestMin(key);

    assertThat(result, equalTo(expectedResponse));
    verify(commandExecutor).executeCommand(doubleCommandObject);
    verify(commandObjects).tdigestMin(key);
  }

  @Test
  public void testTdigestQuantile() {
    String key = "testTDigest";
    double[] quantiles = { 0.1, 0.5, 0.9 };
    List<Double> expectedResponse = Arrays.asList(1.0, 2.0, 3.0);

    when(commandObjects.tdigestQuantile(key, quantiles)).thenReturn(listDoubleCommandObject);
    when(commandExecutor.executeCommand(listDoubleCommandObject)).thenReturn(expectedResponse);

    List<Double> result = jedis.tdigestQuantile(key, quantiles);

    assertThat(result, sameInstance(expectedResponse));
    verify(commandExecutor).executeCommand(listDoubleCommandObject);
    verify(commandObjects).tdigestQuantile(key, quantiles);
  }

  @Test
  public void testTdigestRank() {
    String key = "testTDigest";
    double[] values = { 1.0, 2.0 };
    List<Long> expectedResponse = Arrays.asList(10L, 20L);

    when(commandObjects.tdigestRank(key, values)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResponse);

    List<Long> result = jedis.tdigestRank(key, values);

    assertThat(result, sameInstance(expectedResponse));
    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).tdigestRank(key, values);
  }

  @Test
  public void testTdigestReset() {
    String key = "testTDigest";
    String expectedResponse = "OK";

    when(commandObjects.tdigestReset(key)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.tdigestReset(key);

    assertThat(result, sameInstance(expectedResponse));
    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).tdigestReset(key);
  }

  @Test
  public void testTdigestRevRank() {
    String key = "testTDigest";
    double[] values = { 1.0, 2.0 };
    List<Long> expectedResponse = Arrays.asList(90L, 80L);

    when(commandObjects.tdigestRevRank(key, values)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResponse);

    List<Long> result = jedis.tdigestRevRank(key, values);

    assertThat(result, sameInstance(expectedResponse));
    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).tdigestRevRank(key, values);
  }

  @Test
  public void testTdigestTrimmedMean() {
    String key = "testTDigest";
    double lowCutQuantile = 0.1;
    double highCutQuantile = 0.9;
    double expectedResponse = 5.0;

    when(commandObjects.tdigestTrimmedMean(key, lowCutQuantile, highCutQuantile)).thenReturn(doubleCommandObject);
    when(commandExecutor.executeCommand(doubleCommandObject)).thenReturn(expectedResponse);

    double result = jedis.tdigestTrimmedMean(key, lowCutQuantile, highCutQuantile);

    assertThat(result, equalTo(expectedResponse));
    verify(commandExecutor).executeCommand(doubleCommandObject);
    verify(commandObjects).tdigestTrimmedMean(key, lowCutQuantile, highCutQuantile);
  }

}
