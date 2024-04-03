package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class UnifiedJedisCountMinSketchCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testCmsIncrBy() {
    String key = "testCMS";
    Map<String, Long> itemIncrements = new HashMap<>();
    itemIncrements.put("item1", 1L);
    itemIncrements.put("item2", 2L);
    List<Long> expectedResponse = Arrays.asList(1L, 2L);

    when(commandObjects.cmsIncrBy(key, itemIncrements)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResponse);

    List<Long> result = jedis.cmsIncrBy(key, itemIncrements);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).cmsIncrBy(key, itemIncrements);
  }

  @Test
  public void testCmsInfo() {
    String key = "testCMS";
    Map<String, Object> expectedResponse = new HashMap<>();
    expectedResponse.put("width", 1000L);
    expectedResponse.put("depth", 5L);
    expectedResponse.put("count", 42L);

    when(commandObjects.cmsInfo(key)).thenReturn(mapStringObjectCommandObject);
    when(commandExecutor.executeCommand(mapStringObjectCommandObject)).thenReturn(expectedResponse);

    Map<String, Object> result = jedis.cmsInfo(key);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(mapStringObjectCommandObject);
    verify(commandObjects).cmsInfo(key);
  }

  @Test
  public void testCmsInitByDim() {
    String key = "testCMS";
    long width = 1000L;
    long depth = 5L;
    String expectedResponse = "OK";

    when(commandObjects.cmsInitByDim(key, width, depth)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.cmsInitByDim(key, width, depth);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).cmsInitByDim(key, width, depth);
  }

  @Test
  public void testCmsInitByProb() {
    String key = "testCMS";
    double error = 0.01;
    double probability = 0.99;
    String expectedResponse = "OK";

    when(commandObjects.cmsInitByProb(key, error, probability)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.cmsInitByProb(key, error, probability);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).cmsInitByProb(key, error, probability);
  }

  @Test
  public void testCmsMerge() {
    String destKey = "destCMS";
    String[] keys = { "cms1", "cms2" };
    String expectedResponse = "OK";

    when(commandObjects.cmsMerge(destKey, keys)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.cmsMerge(destKey, keys);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).cmsMerge(destKey, keys);
  }

  @Test
  public void testCmsMergeWithWeights() {
    String destKey = "destCMS";
    Map<String, Long> keysAndWeights = new HashMap<>();
    keysAndWeights.put("cms1", 1L);
    keysAndWeights.put("cms2", 2L);
    String expectedResponse = "OK";

    when(commandObjects.cmsMerge(destKey, keysAndWeights)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.cmsMerge(destKey, keysAndWeights);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).cmsMerge(destKey, keysAndWeights);
  }

  @Test
  public void testCmsQuery() {
    String key = "testCMS";
    String[] items = { "item1", "item2" };
    List<Long> expectedResponse = Arrays.asList(42L, 27L);

    when(commandObjects.cmsQuery(key, items)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResponse);

    List<Long> result = jedis.cmsQuery(key, items);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).cmsQuery(key, items);
  }

}
