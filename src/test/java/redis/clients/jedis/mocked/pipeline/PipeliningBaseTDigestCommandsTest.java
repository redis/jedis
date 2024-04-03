package redis.clients.jedis.mocked.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import redis.clients.jedis.Response;
import redis.clients.jedis.bloom.TDigestMergeParams;

public class PipeliningBaseTDigestCommandsTest extends PipeliningBaseMockedTestBase {

  @Test
  public void testTdigestAdd() {
    when(commandObjects.tdigestAdd("myTDigest", 1.0, 2.0, 3.0)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.tdigestAdd("myTDigest", 1.0, 2.0, 3.0);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestByRank() {
    when(commandObjects.tdigestByRank("myTDigest", 1, 2)).thenReturn(listDoubleCommandObject);

    Response<List<Double>> response = pipeliningBase.tdigestByRank("myTDigest", 1, 2);

    assertThat(commands, contains(listDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestByRevRank() {
    when(commandObjects.tdigestByRevRank("myTDigest", 1, 2)).thenReturn(listDoubleCommandObject);

    Response<List<Double>> response = pipeliningBase.tdigestByRevRank("myTDigest", 1, 2);

    assertThat(commands, contains(listDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestCDF() {
    when(commandObjects.tdigestCDF("myTDigest", 1.0, 2.0)).thenReturn(listDoubleCommandObject);

    Response<List<Double>> response = pipeliningBase.tdigestCDF("myTDigest", 1.0, 2.0);

    assertThat(commands, contains(listDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestCreate() {
    when(commandObjects.tdigestCreate("myTDigest")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.tdigestCreate("myTDigest");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestCreateWithCompression() {
    when(commandObjects.tdigestCreate("myTDigest", 100)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.tdigestCreate("myTDigest", 100);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestInfo() {
    when(commandObjects.tdigestInfo("myTDigest")).thenReturn(mapStringObjectCommandObject);

    Response<Map<String, Object>> response = pipeliningBase.tdigestInfo("myTDigest");

    assertThat(commands, contains(mapStringObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestMax() {
    when(commandObjects.tdigestMax("myTDigest")).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.tdigestMax("myTDigest");

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestMerge() {
    when(commandObjects.tdigestMerge("destinationTDigest", "sourceTDigest1", "sourceTDigest2"))
        .thenReturn(stringCommandObject);

    Response<String> response =
        pipeliningBase.tdigestMerge("destinationTDigest", "sourceTDigest1", "sourceTDigest2");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestMergeWithParams() {
    TDigestMergeParams mergeParams = new TDigestMergeParams().compression(100);

    when(commandObjects.tdigestMerge(mergeParams, "destinationTDigest", "sourceTDigest1", "sourceTDigest2"))
        .thenReturn(stringCommandObject);

    Response<String> response =
        pipeliningBase.tdigestMerge(mergeParams, "destinationTDigest", "sourceTDigest1", "sourceTDigest2");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestMin() {
    when(commandObjects.tdigestMin("myTDigest")).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.tdigestMin("myTDigest");

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestQuantile() {
    when(commandObjects.tdigestQuantile("myTDigest", 0.5, 0.9)).thenReturn(listDoubleCommandObject);

    Response<List<Double>> response = pipeliningBase.tdigestQuantile("myTDigest", 0.5, 0.9);

    assertThat(commands, contains(listDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestRank() {
    when(commandObjects.tdigestRank("myTDigest", 1.0, 2.0)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.tdigestRank("myTDigest", 1.0, 2.0);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestReset() {
    when(commandObjects.tdigestReset("myTDigest")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.tdigestReset("myTDigest");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestRevRank() {
    when(commandObjects.tdigestRevRank("myTDigest", 1.0, 2.0)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.tdigestRevRank("myTDigest", 1.0, 2.0);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestTrimmedMean() {
    when(commandObjects.tdigestTrimmedMean("myTDigest", 0.1, 0.9)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.tdigestTrimmedMean("myTDigest", 0.1, 0.9);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

}
