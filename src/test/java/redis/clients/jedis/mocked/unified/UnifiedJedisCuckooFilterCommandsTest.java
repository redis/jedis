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
import redis.clients.jedis.bloom.CFInsertParams;
import redis.clients.jedis.bloom.CFReserveParams;

public class UnifiedJedisCuckooFilterCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testCfAdd() {
    String key = "testCuckooFilter";
    String item = "item1";
    boolean expectedResponse = true;

    when(commandObjects.cfAdd(key, item)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(expectedResponse);

    boolean result = jedis.cfAdd(key, item);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).cfAdd(key, item);
  }

  @Test
  public void testCfAddNx() {
    String key = "testCuckooFilter";
    String item = "item1";
    boolean expectedResponse = true;

    when(commandObjects.cfAddNx(key, item)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(expectedResponse);

    boolean result = jedis.cfAddNx(key, item);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).cfAddNx(key, item);
  }

  @Test
  public void testCfCount() {
    String key = "testCuckooFilter";
    String item = "item1";
    long expectedResponse = 42L;

    when(commandObjects.cfCount(key, item)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.cfCount(key, item);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).cfCount(key, item);
  }

  @Test
  public void testCfDel() {
    String key = "testCuckooFilter";
    String item = "item1";
    boolean expectedResponse = true;

    when(commandObjects.cfDel(key, item)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(expectedResponse);

    boolean result = jedis.cfDel(key, item);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).cfDel(key, item);
  }

  @Test
  public void testCfExists() {
    String key = "testCuckooFilter";
    String item = "item1";
    boolean expectedResponse = true;

    when(commandObjects.cfExists(key, item)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(expectedResponse);

    boolean result = jedis.cfExists(key, item);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).cfExists(key, item);
  }

  @Test
  public void testCfInfo() {
    String key = "testCuckooFilter";
    Map<String, Object> expectedResponse = new HashMap<>();
    expectedResponse.put("size", 42L);
    expectedResponse.put("bucketSize", 2L);
    expectedResponse.put("maxIterations", 500L);

    when(commandObjects.cfInfo(key)).thenReturn(mapStringObjectCommandObject);
    when(commandExecutor.executeCommand(mapStringObjectCommandObject)).thenReturn(expectedResponse);

    Map<String, Object> result = jedis.cfInfo(key);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(mapStringObjectCommandObject);
    verify(commandObjects).cfInfo(key);
  }

  @Test
  public void testCfInsert() {
    String key = "testCuckooFilter";
    String[] items = { "item1", "item2" };
    List<Boolean> expectedResponse = Arrays.asList(true, false);

    when(commandObjects.cfInsert(key, items)).thenReturn(listBooleanCommandObject);
    when(commandExecutor.executeCommand(listBooleanCommandObject)).thenReturn(expectedResponse);

    List<Boolean> result = jedis.cfInsert(key, items);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(listBooleanCommandObject);
    verify(commandObjects).cfInsert(key, items);
  }

  @Test
  public void testCfInsertWithParams() {
    String key = "testCuckooFilter";
    CFInsertParams insertParams = new CFInsertParams().noCreate();
    String[] items = { "item1", "item2" };
    List<Boolean> expectedResponse = Arrays.asList(true, false);

    when(commandObjects.cfInsert(key, insertParams, items)).thenReturn(listBooleanCommandObject);
    when(commandExecutor.executeCommand(listBooleanCommandObject)).thenReturn(expectedResponse);

    List<Boolean> result = jedis.cfInsert(key, insertParams, items);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(listBooleanCommandObject);
    verify(commandObjects).cfInsert(key, insertParams, items);
  }

  @Test
  public void testCfInsertNx() {
    String key = "testCuckooFilter";
    String[] items = { "item1", "item2" };
    List<Boolean> expectedResponse = Arrays.asList(true, false);

    when(commandObjects.cfInsertNx(key, items)).thenReturn(listBooleanCommandObject);
    when(commandExecutor.executeCommand(listBooleanCommandObject)).thenReturn(expectedResponse);

    List<Boolean> result = jedis.cfInsertNx(key, items);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(listBooleanCommandObject);
    verify(commandObjects).cfInsertNx(key, items);
  }

  @Test
  public void testCfInsertNxWithParams() {
    String key = "testCuckooFilter";
    CFInsertParams insertParams = new CFInsertParams().noCreate();
    String[] items = { "item1", "item2" };
    List<Boolean> expectedResponse = Arrays.asList(true, false);

    when(commandObjects.cfInsertNx(key, insertParams, items)).thenReturn(listBooleanCommandObject);
    when(commandExecutor.executeCommand(listBooleanCommandObject)).thenReturn(expectedResponse);

    List<Boolean> result = jedis.cfInsertNx(key, insertParams, items);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(listBooleanCommandObject);
    verify(commandObjects).cfInsertNx(key, insertParams, items);
  }

  @Test
  public void testCfLoadChunk() {
    String key = "testCuckooFilter";
    long iterator = 1L;
    byte[] data = new byte[]{ 1, 2, 3 };
    String expectedResponse = "OK";

    when(commandObjects.cfLoadChunk(key, iterator, data)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.cfLoadChunk(key, iterator, data);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).cfLoadChunk(key, iterator, data);
  }

  @Test
  public void testCfMExists() {
    String key = "testCuckooFilter";
    String[] items = { "item1", "item2", "item3" };
    List<Boolean> expectedResponse = Arrays.asList(true, false, true);

    when(commandObjects.cfMExists(key, items)).thenReturn(listBooleanCommandObject);
    when(commandExecutor.executeCommand(listBooleanCommandObject)).thenReturn(expectedResponse);

    List<Boolean> result = jedis.cfMExists(key, items);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(listBooleanCommandObject);
    verify(commandObjects).cfMExists(key, items);
  }

  @Test
  public void testCfReserve() {
    String key = "testCuckooFilter";
    long capacity = 10000L;
    String expectedResponse = "OK";

    when(commandObjects.cfReserve(key, capacity)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.cfReserve(key, capacity);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).cfReserve(key, capacity);
  }

  @Test
  public void testCfReserveWithParams() {
    String key = "testCuckooFilter";
    long capacity = 10000L;
    CFReserveParams reserveParams = new CFReserveParams().expansion(2);
    String expectedResponse = "OK";

    when(commandObjects.cfReserve(key, capacity, reserveParams)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.cfReserve(key, capacity, reserveParams);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).cfReserve(key, capacity, reserveParams);
  }

  @Test
  public void testCfScanDump() {
    String key = "testCuckooFilter";
    long iterator = 0L;
    Map.Entry<Long, byte[]> expectedResponse = new AbstractMap.SimpleEntry<>(1L, new byte[]{ 1, 2, 3 });

    when(commandObjects.cfScanDump(key, iterator)).thenReturn(entryLongBytesCommandObject);
    when(commandExecutor.executeCommand(entryLongBytesCommandObject)).thenReturn(expectedResponse);

    Map.Entry<Long, byte[]> result = jedis.cfScanDump(key, iterator);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(entryLongBytesCommandObject);
    verify(commandObjects).cfScanDump(key, iterator);
  }

}
