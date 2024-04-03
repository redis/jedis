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

public class UnifiedJedisTopKCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testTopkAdd() {
    String key = "testTopK";
    String[] items = { "item1", "item2" };
    List<String> expectedResponse = Arrays.asList("item3", "item4");

    when(commandObjects.topkAdd(key, items)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedResponse);

    List<String> result = jedis.topkAdd(key, items);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).topkAdd(key, items);
  }

  @Test
  public void testTopkIncrBy() {
    String key = "testTopK";
    Map<String, Long> itemIncrements = new HashMap<>();
    itemIncrements.put("item1", 1L);
    itemIncrements.put("item2", 2L);
    List<String> expectedResponse = Arrays.asList("item3", "item4");

    when(commandObjects.topkIncrBy(key, itemIncrements)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedResponse);

    List<String> result = jedis.topkIncrBy(key, itemIncrements);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).topkIncrBy(key, itemIncrements);
  }

  @Test
  public void testTopkInfo() {
    String key = "testTopK";
    Map<String, Object> expectedResponse = new HashMap<>();
    expectedResponse.put("k", 10L);
    expectedResponse.put("width", 50L);
    expectedResponse.put("depth", 5L);
    expectedResponse.put("decay", 0.9);

    when(commandObjects.topkInfo(key)).thenReturn(mapStringObjectCommandObject);
    when(commandExecutor.executeCommand(mapStringObjectCommandObject)).thenReturn(expectedResponse);

    Map<String, Object> result = jedis.topkInfo(key);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(mapStringObjectCommandObject);
    verify(commandObjects).topkInfo(key);
  }

  @Test
  public void testTopkList() {
    String key = "testTopK";
    List<String> expectedResponse = Arrays.asList("item1", "item2", "item3");

    when(commandObjects.topkList(key)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedResponse);

    List<String> result = jedis.topkList(key);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).topkList(key);
  }

  @Test
  public void testTopkListWithCount() {
    String key = "testTopK";
    Map<String, Long> expectedResponse = new HashMap<>();
    expectedResponse.put("item1", 1L);
    expectedResponse.put("item2", 2L);

    when(commandObjects.topkListWithCount(key)).thenReturn(mapStringLongCommandObject);
    when(commandExecutor.executeCommand(mapStringLongCommandObject)).thenReturn(expectedResponse);

    Map<String, Long> result = jedis.topkListWithCount(key);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(mapStringLongCommandObject);
    verify(commandObjects).topkListWithCount(key);
  }

  @Test
  public void testTopkQuery() {
    String key = "testTopK";
    String[] items = { "item1", "item2" };
    List<Boolean> expectedResponse = Arrays.asList(true, false);

    when(commandObjects.topkQuery(key, items)).thenReturn(listBooleanCommandObject);
    when(commandExecutor.executeCommand(listBooleanCommandObject)).thenReturn(expectedResponse);

    List<Boolean> result = jedis.topkQuery(key, items);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(listBooleanCommandObject);
    verify(commandObjects).topkQuery(key, items);
  }

  @Test
  public void testTopkReserve() {
    String key = "testTopK";
    long topk = 10L;
    String expectedResponse = "OK";

    when(commandObjects.topkReserve(key, topk)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.topkReserve(key, topk);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).topkReserve(key, topk);
  }

  @Test
  public void testTopkReserveWidth() {
    String key = "testTopK";
    long topk = 10L;
    long width = 50L;
    long depth = 5L;
    double decay = 0.9;
    String expectedResponse = "OK";

    when(commandObjects.topkReserve(key, topk, width, depth, decay)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.topkReserve(key, topk, width, depth, decay);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).topkReserve(key, topk, width, depth, decay);
  }

}
