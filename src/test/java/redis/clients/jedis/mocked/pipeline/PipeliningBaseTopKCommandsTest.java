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

public class PipeliningBaseTopKCommandsTest extends PipeliningBaseMockedTestBase {

  @Test
  public void testTopkAdd() {
    when(commandObjects.topkAdd("myTopK", "item1", "item2")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.topkAdd("myTopK", "item1", "item2");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTopkIncrBy() {
    Map<String, Long> itemIncrements = new HashMap<>();
    itemIncrements.put("item1", 1L);
    itemIncrements.put("item2", 2L);

    when(commandObjects.topkIncrBy("myTopK", itemIncrements)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.topkIncrBy("myTopK", itemIncrements);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTopkInfo() {
    when(commandObjects.topkInfo("myTopK")).thenReturn(mapStringObjectCommandObject);

    Response<Map<String, Object>> response = pipeliningBase.topkInfo("myTopK");

    assertThat(commands, contains(mapStringObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTopkList() {
    when(commandObjects.topkList("myTopK")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.topkList("myTopK");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTopkListWithCount() {
    when(commandObjects.topkListWithCount("myTopK")).thenReturn(mapStringLongCommandObject);

    Response<Map<String, Long>> response = pipeliningBase.topkListWithCount("myTopK");

    assertThat(commands, contains(mapStringLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTopkQuery() {
    when(commandObjects.topkQuery("myTopK", "item1", "item2")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.topkQuery("myTopK", "item1", "item2");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTopkReserve() {
    when(commandObjects.topkReserve("myTopK", 3L)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.topkReserve("myTopK", 3L);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTopkReserveWithParams() {
    long width = 50L;
    long depth = 5L;
    double decay = 0.9;

    when(commandObjects.topkReserve("myTopK", 3L, width, depth, decay)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.topkReserve("myTopK", 3L, width, depth, decay);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

}
