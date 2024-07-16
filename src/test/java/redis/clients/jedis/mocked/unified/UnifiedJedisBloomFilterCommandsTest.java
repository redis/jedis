package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import redis.clients.jedis.bloom.BFInsertParams;
import redis.clients.jedis.bloom.BFReserveParams;

public class UnifiedJedisBloomFilterCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testBfAdd() {
    String key = "testBloom";
    String item = "item1";
    boolean expectedResponse = true;

    when(commandObjects.bfAdd(key, item)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(expectedResponse);

    boolean result = jedis.bfAdd(key, item);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).bfAdd(key, item);
  }

  @Test
  public void testBfCard() {
    String key = "testBloom";
    long expectedResponse = 42L;

    when(commandObjects.bfCard(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.bfCard(key);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).bfCard(key);
  }

  @Test
  public void testBfExists() {
    String key = "testBloom";
    String item = "item1";
    boolean expectedResponse = true;

    when(commandObjects.bfExists(key, item)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(expectedResponse);

    boolean result = jedis.bfExists(key, item);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).bfExists(key, item);
  }

  @Test
  public void testBfInfo() {
    String key = "testBloom";
    Map<String, Object> expectedResponse = new HashMap<>();
    expectedResponse.put("size", 42L);
    expectedResponse.put("numberOfFilters", 3L);
    expectedResponse.put("insertedItems", 1000L);

    when(commandObjects.bfInfo(key)).thenReturn(mapStringObjectCommandObject);
    when(commandExecutor.executeCommand(mapStringObjectCommandObject)).thenReturn(expectedResponse);

    Map<String, Object> result = jedis.bfInfo(key);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(mapStringObjectCommandObject);
    verify(commandObjects).bfInfo(key);
  }

  @Test
  public void testBfInsert() {
    String key = "testBloom";
    String[] items = { "item1", "item2" };
    List<Boolean> expectedResponse = Arrays.asList(true, false);

    when(commandObjects.bfInsert(key, items)).thenReturn(listBooleanCommandObject);
    when(commandExecutor.executeCommand(listBooleanCommandObject)).thenReturn(expectedResponse);

    List<Boolean> result = jedis.bfInsert(key, items);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(listBooleanCommandObject);
    verify(commandObjects).bfInsert(key, items);
  }

  @Test
  public void testBfInsertWithParams() {
    String key = "testBloom";
    BFInsertParams insertParams = new BFInsertParams().noCreate();
    String[] items = { "item1", "item2" };
    List<Boolean> expectedResponse = Arrays.asList(true, false);

    when(commandObjects.bfInsert(key, insertParams, items)).thenReturn(listBooleanCommandObject);
    when(commandExecutor.executeCommand(listBooleanCommandObject)).thenReturn(expectedResponse);

    List<Boolean> result = jedis.bfInsert(key, insertParams, items);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(listBooleanCommandObject);
    verify(commandObjects).bfInsert(key, insertParams, items);
  }

  @Test
  public void testBfLoadChunk() {
    String key = "testBloom";
    long iterator = 1L;
    byte[] data = new byte[]{ 1, 2, 3 };
    String expectedResponse = "OK";

    when(commandObjects.bfLoadChunk(key, iterator, data)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.bfLoadChunk(key, iterator, data);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).bfLoadChunk(key, iterator, data);
  }

  @Test
  public void testBfMAdd() {
    String key = "testBloom";
    String[] items = { "item1", "item2", "item3" };
    List<Boolean> expectedResponse = Arrays.asList(true, false, true);

    when(commandObjects.bfMAdd(key, items)).thenReturn(listBooleanCommandObject);
    when(commandExecutor.executeCommand(listBooleanCommandObject)).thenReturn(expectedResponse);

    List<Boolean> result = jedis.bfMAdd(key, items);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(listBooleanCommandObject);
    verify(commandObjects).bfMAdd(key, items);
  }

  @Test
  public void testBfMExists() {
    String key = "testBloom";
    String[] items = { "item1", "item2" };
    List<Boolean> expectedResponse = Arrays.asList(true, false);

    when(commandObjects.bfMExists(key, items)).thenReturn(listBooleanCommandObject);
    when(commandExecutor.executeCommand(listBooleanCommandObject)).thenReturn(expectedResponse);

    List<Boolean> result = jedis.bfMExists(key, items);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(listBooleanCommandObject);
    verify(commandObjects).bfMExists(key, items);
  }

  @Test
  public void testBfReserve() {
    String key = "testBloom";
    double errorRate = 0.01;
    long capacity = 10000L;
    String expectedResponse = "OK";

    when(commandObjects.bfReserve(key, errorRate, capacity)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.bfReserve(key, errorRate, capacity);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).bfReserve(key, errorRate, capacity);
  }

  @Test
  public void testBfReserveWithParams() {
    String key = "testBloom";
    double errorRate = 0.01;
    long capacity = 10000L;
    BFReserveParams reserveParams = new BFReserveParams().expansion(2);
    String expectedResponse = "OK";

    when(commandObjects.bfReserve(key, errorRate, capacity, reserveParams)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.bfReserve(key, errorRate, capacity, reserveParams);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).bfReserve(key, errorRate, capacity, reserveParams);
  }

  @Test
  public void testBfScanDump() {
    String key = "testBloom";
    long iterator = 0L;
    Map.Entry<Long, byte[]> expectedResponse = new AbstractMap.SimpleEntry<>(1L, new byte[]{ 1, 2, 3 });

    when(commandObjects.bfScanDump(key, iterator)).thenReturn(entryLongBytesCommandObject);
    when(commandExecutor.executeCommand(entryLongBytesCommandObject)).thenReturn(expectedResponse);

    Map.Entry<Long, byte[]> result = jedis.bfScanDump(key, iterator);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(entryLongBytesCommandObject);
    verify(commandObjects).bfScanDump(key, iterator);
  }

}
