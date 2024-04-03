package redis.clients.jedis.mocked.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import redis.clients.jedis.Response;

public class PipeliningBaseCountMinSketchCommandsTest extends PipeliningBaseMockedTestBase {

  @Test
  public void testCmsIncrBy() {
    Map<String, Long> itemIncrements = new HashMap<>();
    itemIncrements.put("item1", 1L);
    itemIncrements.put("item2", 2L);

    when(commandObjects.cmsIncrBy("myCountMinSketch", itemIncrements)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.cmsIncrBy("myCountMinSketch", itemIncrements);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCmsInfo() {
    when(commandObjects.cmsInfo("myCountMinSketch")).thenReturn(mapStringObjectCommandObject);

    Response<Map<String, Object>> response = pipeliningBase.cmsInfo("myCountMinSketch");

    assertThat(commands, contains(mapStringObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCmsInitByDim() {
    when(commandObjects.cmsInitByDim("myCountMinSketch", 1000L, 5L)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.cmsInitByDim("myCountMinSketch", 1000L, 5L);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCmsInitByProb() {
    double error = 0.01;
    double probability = 0.99;

    when(commandObjects.cmsInitByProb("myCountMinSketch", error, probability)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.cmsInitByProb("myCountMinSketch", error, probability);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCmsMerge() {
    when(commandObjects.cmsMerge("mergedCountMinSketch", "cms1", "cms2")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.cmsMerge("mergedCountMinSketch", "cms1", "cms2");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCmsMergeWithWeights() {
    Map<String, Long> keysAndWeights = new HashMap<>();
    keysAndWeights.put("cms1", 1L);
    keysAndWeights.put("cms2", 2L);

    when(commandObjects.cmsMerge("mergedCountMinSketch", keysAndWeights)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.cmsMerge("mergedCountMinSketch", keysAndWeights);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCmsQuery() {
    when(commandObjects.cmsQuery("myCountMinSketch", "item1", "item2")).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.cmsQuery("myCountMinSketch", "item1", "item2");

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

}
