package redis.clients.jedis.mocked.unified;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonObject;
import org.json.JSONArray;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import redis.clients.jedis.json.JsonObjectMapper;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.json.Path2;

public class UnifiedJedisJsonCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testJsonArrAppendWithPath() {
    String key = "testKey";
    Path path = Path.of(".path.to.array");
    Object[] pojos = new Object[]{ "value1", "value2" };
    Long expectedResponse = 4L;

    when(commandObjects.jsonArrAppend(key, path, pojos)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    Long result = jedis.jsonArrAppend(key, path, pojos);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).jsonArrAppend(key, path, pojos);
  }

  @Test
  public void testJsonArrAppendWithPath2() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.array");
    Object[] objects = new Object[]{ "value1", "value2" };
    List<Long> expectedResponse = Arrays.asList(3L, 4L);

    when(commandObjects.jsonArrAppend(key, path, objects)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResponse);

    List<Long> result = jedis.jsonArrAppend(key, path, objects);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).jsonArrAppend(key, path, objects);
  }

  @Test
  public void testJsonArrAppendWithPath2WithEscape() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.array");
    Object[] objects = new Object[]{ "value1", "value2" };
    List<Long> expectedResponse = Arrays.asList(3L, 4L);

    when(commandObjects.jsonArrAppendWithEscape(key, path, objects)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResponse);

    List<Long> result = jedis.jsonArrAppendWithEscape(key, path, objects);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).jsonArrAppendWithEscape(key, path, objects);
  }

  @Test
  public void testJsonArrIndexWithPath() {
    String key = "testKey";
    Path path = Path.of(".path.to.array");
    Object scalar = "value";
    long expectedResponse = 2L;

    when(commandObjects.jsonArrIndex(key, path, scalar)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.jsonArrIndex(key, path, scalar);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).jsonArrIndex(key, path, scalar);
  }

  @Test
  public void testJsonArrIndexWithPath2() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.array");
    Object scalar = "value";
    List<Long> expectedResponse = Collections.singletonList(2L);

    when(commandObjects.jsonArrIndex(key, path, scalar)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResponse);

    List<Long> result = jedis.jsonArrIndex(key, path, scalar);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).jsonArrIndex(key, path, scalar);
  }

  @Test
  public void testJsonArrIndexWithPath2WithEscape() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.array");
    Object scalar = "value";
    List<Long> expectedResponse = Collections.singletonList(2L);

    when(commandObjects.jsonArrIndexWithEscape(key, path, scalar)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResponse);

    List<Long> result = jedis.jsonArrIndexWithEscape(key, path, scalar);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).jsonArrIndexWithEscape(key, path, scalar);
  }

  @Test
  public void testJsonArrInsertWithPath() {
    String key = "testKey";
    Path path = Path.of(".path.to.array");
    int index = 1;
    Object[] pojos = new Object[]{ "value1", "value2" };
    long expectedResponse = 5L;

    when(commandObjects.jsonArrInsert(key, path, index, pojos)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.jsonArrInsert(key, path, index, pojos);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).jsonArrInsert(key, path, index, pojos);
  }

  @Test
  public void testJsonArrInsertWithPath2() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.array");
    int index = 1;
    Object[] objects = new Object[]{ "value1", "value2" };
    List<Long> expectedResponse = Collections.singletonList(5L);

    when(commandObjects.jsonArrInsert(key, path, index, objects)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResponse);

    List<Long> result = jedis.jsonArrInsert(key, path, index, objects);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).jsonArrInsert(key, path, index, objects);
  }

  @Test
  public void testJsonArrInsertWithPath2WithEscape() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.array");
    int index = 1;
    Object[] objects = new Object[]{ "value1", "value2" };
    List<Long> expectedResponse = Collections.singletonList(5L);

    when(commandObjects.jsonArrInsertWithEscape(key, path, index, objects)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResponse);

    List<Long> result = jedis.jsonArrInsertWithEscape(key, path, index, objects);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).jsonArrInsertWithEscape(key, path, index, objects);
  }

  @Test
  public void testJsonArrLen() {
    String key = "testKey";
    Long expectedResponse = 10L;

    when(commandObjects.jsonArrLen(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    Long result = jedis.jsonArrLen(key);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).jsonArrLen(key);
  }

  @Test
  public void testJsonArrLenWithPath() {
    String key = "testKey";
    Path path = Path.of(".path.to.array");
    Long expectedResponse = 10L;

    when(commandObjects.jsonArrLen(key, path)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    Long result = jedis.jsonArrLen(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).jsonArrLen(key, path);
  }

  @Test
  public void testJsonArrLenWithPath2() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.array");
    List<Long> expectedResponse = Collections.singletonList(10L);

    when(commandObjects.jsonArrLen(key, path)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResponse);

    List<Long> result = jedis.jsonArrLen(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).jsonArrLen(key, path);
  }

  @Test
  public void testJsonArrPop() {
    String key = "testKey";
    Object expectedResponse = "poppedValue";

    when(commandObjects.jsonArrPop(key)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedResponse);

    Object result = jedis.jsonArrPop(key);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).jsonArrPop(key);
  }

  @Test
  public void testJsonArrPopWithPath() {
    String key = "testKey";
    Path path = Path.of(".path.to.array");
    Object expectedResponse = "poppedValue";

    when(commandObjects.jsonArrPop(key, path)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedResponse);

    Object result = jedis.jsonArrPop(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).jsonArrPop(key, path);
  }

  @Test
  public void testJsonArrPopWithPathAndIndex() {
    String key = "testKey";
    Path path = Path.of(".path.to.array");
    int index = 1;
    Object expectedResponse = "poppedValueAtIndex";

    when(commandObjects.jsonArrPop(key, path, index)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedResponse);

    Object result = jedis.jsonArrPop(key, path, index);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).jsonArrPop(key, path, index);
  }

  @Test
  public void testJsonArrPopWithClassAndPath() {
    String key = "testKey";
    Class<String> clazz = String.class;
    Path path = Path.of(".path.to.array");
    String expectedResponse = "poppedValue";

    when(commandObjects.jsonArrPop(key, clazz, path)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.jsonArrPop(key, clazz, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).jsonArrPop(key, clazz, path);
  }

  @Test
  public void testJsonArrPopWithClassPathAndIndex() {
    String key = "testKey";
    Class<String> clazz = String.class;
    Path path = Path.of(".path.to.array");
    int index = 1;
    String expectedResponse = "poppedValueAtIndex";

    when(commandObjects.jsonArrPop(key, clazz, path, index)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.jsonArrPop(key, clazz, path, index);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).jsonArrPop(key, clazz, path, index);
  }

  @Test
  public void testJsonArrPopWithPath2() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.array");
    List<Object> expectedResponse = Collections.singletonList("poppedValue");

    when(commandObjects.jsonArrPop(key, path)).thenReturn(listObjectCommandObject);
    when(commandExecutor.executeCommand(listObjectCommandObject)).thenReturn(expectedResponse);

    List<Object> result = jedis.jsonArrPop(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listObjectCommandObject);
    verify(commandObjects).jsonArrPop(key, path);
  }

  @Test
  public void testJsonArrPopWithPath2AndIndex() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.array");
    int index = 1;
    List<Object> expectedResponse = Collections.singletonList("poppedValueAtIndex");

    when(commandObjects.jsonArrPop(key, path, index)).thenReturn(listObjectCommandObject);
    when(commandExecutor.executeCommand(listObjectCommandObject)).thenReturn(expectedResponse);

    List<Object> result = jedis.jsonArrPop(key, path, index);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listObjectCommandObject);
    verify(commandObjects).jsonArrPop(key, path, index);
  }

  @Test
  public void testJsonArrPopWithClass() {
    String key = "testKey";
    Class<String> clazz = String.class;
    String expectedResponse = "poppedValue";

    when(commandObjects.jsonArrPop(key, clazz)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.jsonArrPop(key, clazz);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).jsonArrPop(key, clazz);
  }

  @Test
  public void testJsonArrTrimWithPath() {
    String key = "testKey";
    Path path = Path.of(".path.to.array");
    int start = 1;
    int stop = 3;
    Long expectedResponse = 3L;

    when(commandObjects.jsonArrTrim(key, path, start, stop)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    Long result = jedis.jsonArrTrim(key, path, start, stop);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).jsonArrTrim(key, path, start, stop);
  }

  @Test
  public void testJsonArrTrimWithPath2() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.array");
    int start = 1;
    int stop = 3;
    List<Long> expectedResponse = Collections.singletonList(3L);

    when(commandObjects.jsonArrTrim(key, path, start, stop)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResponse);

    List<Long> result = jedis.jsonArrTrim(key, path, start, stop);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).jsonArrTrim(key, path, start, stop);
  }

  @Test
  public void testJsonClear() {
    String key = "testKey";
    long expectedResponse = 1L;

    when(commandObjects.jsonClear(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.jsonClear(key);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).jsonClear(key);
  }

  @Test
  public void testJsonClearWithPath() {
    String key = "testKey";
    Path path = Path.of(".path.to.element");
    long expectedResponse = 1L;

    when(commandObjects.jsonClear(key, path)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.jsonClear(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).jsonClear(key, path);
  }

  @Test
  public void testJsonClearWithPath2() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.element");
    long expectedResponse = 1L;

    when(commandObjects.jsonClear(key, path)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.jsonClear(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).jsonClear(key, path);
  }

  @Test
  public void testJsonDebugMemory() {
    String key = "testKey";
    long expectedResponse = 1024L;

    when(commandObjects.jsonDebugMemory(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.jsonDebugMemory(key);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).jsonDebugMemory(key);
  }

  @Test
  public void testJsonDebugMemoryWithPath() {
    String key = "testKey";
    Path path = Path.of(".path.to.element");
    long expectedResponse = 512L;

    when(commandObjects.jsonDebugMemory(key, path)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.jsonDebugMemory(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).jsonDebugMemory(key, path);
  }

  @Test
  public void testJsonDebugMemoryWithPath2() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.element");
    List<Long> expectedResponse = Collections.singletonList(512L);

    when(commandObjects.jsonDebugMemory(key, path)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResponse);

    List<Long> result = jedis.jsonDebugMemory(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).jsonDebugMemory(key, path);
  }

  @Test
  public void testJsonDel() {
    String key = "testKey";
    long expectedResponse = 1L;

    when(commandObjects.jsonDel(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.jsonDel(key);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).jsonDel(key);
  }

  @Test
  public void testJsonDelWithPath() {
    String key = "testKey";
    Path path = Path.of(".path.to.element");
    long expectedResponse = 1L;

    when(commandObjects.jsonDel(key, path)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.jsonDel(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).jsonDel(key, path);
  }

  @Test
  public void testJsonDelWithPath2() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.element");
    long expectedResponse = 1L;

    when(commandObjects.jsonDel(key, path)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.jsonDel(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).jsonDel(key, path);
  }

  @Test
  public void testJsonGet() {
    String key = "testKey";
    Object expectedResponse = new JsonObject();

    when(commandObjects.jsonGet(key)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedResponse);

    Object result = jedis.jsonGet(key);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).jsonGet(key);
  }

  @Test
  public void testJsonGetWithClass() {
    String key = "testKey";
    Class<MyBean> clazz = MyBean.class;
    MyBean expectedResponse = new MyBean();

    when(commandObjects.jsonGet(key, clazz)).thenReturn(myBeanCommandObject);
    when(commandExecutor.executeCommand(myBeanCommandObject)).thenReturn(expectedResponse);

    MyBean result = jedis.jsonGet(key, clazz);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(myBeanCommandObject);
    verify(commandObjects).jsonGet(key, clazz);
  }

  @Test
  public void testJsonGetWithPath() {
    String key = "testKey";
    Path[] paths = new Path[]{ Path.of(".path.to.element1"), Path.of(".path.to.element2") };
    Object expectedResponse = new JsonObject();

    when(commandObjects.jsonGet(key, paths)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedResponse);

    Object result = jedis.jsonGet(key, paths);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).jsonGet(key, paths);
  }

  @Test
  public void testJsonGetWithPath2() {
    String key = "testKey";
    Path2[] paths = new Path2[]{ Path2.of(".path.to.element1"), Path2.of(".path.to.element2") };
    Object expectedResponse = new JsonObject();

    when(commandObjects.jsonGet(key, paths)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedResponse);

    Object result = jedis.jsonGet(key, paths);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).jsonGet(key, paths);
  }

  @Test
  public void testJsonGetAsPlainString() {
    String key = "testKey";
    Path path = Path.of(".path.to.element");
    String expectedResponse = "{\"field\":\"value\"}";

    when(commandObjects.jsonGetAsPlainString(key, path)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.jsonGetAsPlainString(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).jsonGetAsPlainString(key, path);
  }

  @Test
  public void testJsonGetWithClassAndPath() {
    String key = "testKey";
    Class<MyBean> clazz = MyBean.class;
    Path[] paths = new Path[]{ Path.of(".path.to.element1"), Path.of(".path.to.element2") };
    MyBean expectedResponse = new MyBean();

    when(commandObjects.jsonGet(key, clazz, paths)).thenReturn(myBeanCommandObject);
    when(commandExecutor.executeCommand(myBeanCommandObject)).thenReturn(expectedResponse);

    MyBean result = jedis.jsonGet(key, clazz, paths);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(myBeanCommandObject);
    verify(commandObjects).jsonGet(key, clazz, paths);
  }

  @Test
  public void testJsonMergeWithPath() {
    String key = "testKey";
    Path path = Path.of(".path.to.element");
    Object pojo = new MyBean();
    String expectedResponse = "OK";

    when(commandObjects.jsonMerge(key, path, pojo)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.jsonMerge(key, path, pojo);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).jsonMerge(key, path, pojo);
  }

  @Test
  public void testJsonMergeWithPath2() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.element");
    Object object = new JsonObject();
    String expectedResponse = "OK";

    when(commandObjects.jsonMerge(key, path, object)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.jsonMerge(key, path, object);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).jsonMerge(key, path, object);
  }

  @Test
  public void testJsonMGetWithPathAndClass() {
    Path path = Path.of(".path.to.element");
    Class<MyBean> clazz = MyBean.class;
    String[] keys = { "testKey1", "testKey2" };
    List<MyBean> expectedResponse = Arrays.asList(new MyBean(), new MyBean());

    when(commandObjects.jsonMGet(path, clazz, keys)).thenReturn(listMyBeanCommandObject);
    when(commandExecutor.executeCommand(listMyBeanCommandObject)).thenReturn(expectedResponse);

    List<MyBean> result = jedis.jsonMGet(path, clazz, keys);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listMyBeanCommandObject);
    verify(commandObjects).jsonMGet(path, clazz, keys);
  }

  @Test
  public void testJsonMGetWithPath2() {
    Path2 path = Path2.of(".path.to.element");
    String[] keys = { "testKey1", "testKey2" };
    List<JSONArray> expectedResponse = Arrays.asList(new JSONArray(), new JSONArray());

    when(commandObjects.jsonMGet(path, keys)).thenReturn(listJsonArrayCommandObject);
    when(commandExecutor.executeCommand(listJsonArrayCommandObject)).thenReturn(expectedResponse);

    List<JSONArray> result = jedis.jsonMGet(path, keys);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listJsonArrayCommandObject);
    verify(commandObjects).jsonMGet(path, keys);
  }

  @Test
  public void testJsonNumIncrByWithPath() {
    String key = "testKey";
    Path path = Path.of(".path.to.element");
    double value = 10.5;
    double expectedResponse = 20.5;

    when(commandObjects.jsonNumIncrBy(key, path, value)).thenReturn(doubleCommandObject);
    when(commandExecutor.executeCommand(doubleCommandObject)).thenReturn(expectedResponse);

    double result = jedis.jsonNumIncrBy(key, path, value);

    assertEquals(expectedResponse, result, 0.0);

    verify(commandExecutor).executeCommand(doubleCommandObject);
    verify(commandObjects).jsonNumIncrBy(key, path, value);
  }

  @Test
  public void testJsonNumIncrByWithPath2() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.element");
    double value = 10.5;
    Object expectedResponse = 20.5;

    when(commandObjects.jsonNumIncrBy(key, path, value)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedResponse);

    Object result = jedis.jsonNumIncrBy(key, path, value);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).jsonNumIncrBy(key, path, value);
  }

  @Test
  public void testJsonSetWithPath() {
    String key = "testKey";
    Path path = Path.of(".path.to.element");
    Object pojo = new MyBean();
    String expectedResponse = "OK";

    when(commandObjects.jsonSet(key, path, pojo)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.jsonSet(key, path, pojo);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).jsonSet(key, path, pojo);
  }

  @Test
  public void testJsonSetWithPathAndParams() {
    String key = "testKey";
    Path path = Path.of(".path.to.element");
    Object pojo = new MyBean();
    JsonSetParams params = new JsonSetParams().nx();
    String expectedResponse = "OK";

    when(commandObjects.jsonSet(key, path, pojo, params)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.jsonSet(key, path, pojo, params);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).jsonSet(key, path, pojo, params);
  }

  @Test
  public void testJsonSetWithPath2() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.element");
    Object object = new JsonObject();
    String expectedResponse = "OK";

    when(commandObjects.jsonSet(key, path, object)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.jsonSet(key, path, object);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).jsonSet(key, path, object);
  }

  @Test
  public void testJsonSetWithPath2WithEscape() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.element");
    Object object = new JsonObject();
    String expectedResponse = "OK";

    when(commandObjects.jsonSetWithEscape(key, path, object)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.jsonSetWithEscape(key, path, object);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).jsonSetWithEscape(key, path, object);
  }

  @Test
  public void testJsonSetWithPath2AndParams() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.element");
    Object object = new JsonObject();
    JsonSetParams params = new JsonSetParams().nx();
    String expectedResponse = "OK";

    when(commandObjects.jsonSet(key, path, object, params)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.jsonSet(key, path, object, params);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).jsonSet(key, path, object, params);
  }

  @Test
  public void testJsonSetWithPath2EscapeAndParams() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.element");
    Object object = new JsonObject();
    JsonSetParams params = new JsonSetParams().nx();
    String expectedResponse = "OK";

    when(commandObjects.jsonSetWithEscape(key, path, object, params)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.jsonSetWithEscape(key, path, object, params);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).jsonSetWithEscape(key, path, object, params);
  }

  @Test
  public void testJsonSetWithPlainString() {
    String key = "testKey";
    Path path = Path.of(".path.to.element");
    String jsonString = "{\"field\":\"value\"}";
    String expectedResponse = "OK";

    when(commandObjects.jsonSetWithPlainString(key, path, jsonString)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.jsonSetWithPlainString(key, path, jsonString);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).jsonSetWithPlainString(key, path, jsonString);
  }

  @Test
  public void testJsonStrAppend() {
    String key = "testKey";
    Object string = "additional string";
    long expectedResponse = 20L;

    when(commandObjects.jsonStrAppend(key, string)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.jsonStrAppend(key, string);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).jsonStrAppend(key, string);
  }

  @Test
  public void testJsonStrAppendWithPath() {
    String key = "testKey";
    Path path = Path.of(".path.to.element");
    Object string = "additional string";
    long expectedResponse = 20L;

    when(commandObjects.jsonStrAppend(key, path, string)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.jsonStrAppend(key, path, string);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).jsonStrAppend(key, path, string);
  }

  @Test
  public void testJsonStrAppendWithPath2() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.element");
    Object string = "additional string";
    List<Long> expectedResponse = Collections.singletonList(20L);

    when(commandObjects.jsonStrAppend(key, path, string)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResponse);

    List<Long> result = jedis.jsonStrAppend(key, path, string);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).jsonStrAppend(key, path, string);
  }

  @Test
  public void testJsonStrLen() {
    String key = "testKey";
    Long expectedResponse = 15L;

    when(commandObjects.jsonStrLen(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    Long result = jedis.jsonStrLen(key);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).jsonStrLen(key);
  }

  @Test
  public void testJsonStrLenWithPath() {
    String key = "testKey";
    Path path = Path.of(".path.to.element");
    Long expectedResponse = 15L;

    when(commandObjects.jsonStrLen(key, path)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    Long result = jedis.jsonStrLen(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).jsonStrLen(key, path);
  }

  @Test
  public void testJsonStrLenWithPath2() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.element");
    List<Long> expectedResponse = Collections.singletonList(15L);

    when(commandObjects.jsonStrLen(key, path)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResponse);

    List<Long> result = jedis.jsonStrLen(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).jsonStrLen(key, path);
  }

  @Test
  public void testJsonToggleWithPath() {
    String key = "testKey";
    Path path = Path.of(".path.to.element");
    String expectedResponse = "OK";

    when(commandObjects.jsonToggle(key, path)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.jsonToggle(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).jsonToggle(key, path);
  }

  @Test
  public void testJsonToggleWithPath2() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.element");
    List<Boolean> expectedResponse = Arrays.asList(true, false);

    when(commandObjects.jsonToggle(key, path)).thenReturn(listBooleanCommandObject);
    when(commandExecutor.executeCommand(listBooleanCommandObject)).thenReturn(expectedResponse);

    List<Boolean> result = jedis.jsonToggle(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listBooleanCommandObject);
    verify(commandObjects).jsonToggle(key, path);
  }

  @Test
  public void testJsonType() {
    String key = "testKey";
    Class<?> expectedResponse = String.class;

    when(commandObjects.jsonType(key)).thenReturn(classCommandObject);
    when(commandExecutor.executeCommand(classCommandObject)).thenAnswer(new Answer<Class<?>>() {
      @Override
      public Class<?> answer(InvocationOnMock invocationOnMock) {
        return expectedResponse;
      }
    });

    Class<?> result = jedis.jsonType(key);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(classCommandObject);
    verify(commandObjects).jsonType(key);
  }

  @Test
  public void testJsonTypeWithPath() {
    String key = "testKey";
    Path path = Path.of(".path.to.element");
    Class<?> expectedResponse = String.class;

    when(commandObjects.jsonType(key, path)).thenReturn(classCommandObject);
    when(commandExecutor.executeCommand(classCommandObject)).thenAnswer(new Answer<Class<?>>() {
      @Override
      public Class<?> answer(InvocationOnMock invocationOnMock) {
        return expectedResponse;
      }
    });

    Class<?> result = jedis.jsonType(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(classCommandObject);
    verify(commandObjects).jsonType(key, path);
  }

  @Test
  public void testJsonTypeWithPath2() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.element");
    List<Class<?>> expectedResponse = Collections.singletonList(String.class);

    when(commandObjects.jsonType(key, path)).thenReturn(listClassCommandObject);
    when(commandExecutor.executeCommand(listClassCommandObject)).thenReturn(expectedResponse);

    List<Class<?>> result = jedis.jsonType(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listClassCommandObject);
    verify(commandObjects).jsonType(key, path);
  }

  @Test
  public void testJsonObjKeys() {
    String key = "testKey";
    List<String> expectedResponse = Arrays.asList("key1", "key2", "key3");

    when(commandObjects.jsonObjKeys(key)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedResponse);

    List<String> result = jedis.jsonObjKeys(key);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).jsonObjKeys(key);
  }

  @Test
  public void testJsonObjKeysWithPath() {
    String key = "testKey";
    Path path = Path.of(".path.to.object");
    List<String> expectedResponse = Arrays.asList("key1", "key2");

    when(commandObjects.jsonObjKeys(key, path)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedResponse);

    List<String> result = jedis.jsonObjKeys(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).jsonObjKeys(key, path);
  }

  @Test
  public void testJsonObjKeysWithPath2() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.object");
    List<List<String>> expectedResponse = Collections.singletonList(Arrays.asList("key1", "key2"));

    when(commandObjects.jsonObjKeys(key, path)).thenReturn(listListStringCommandObject);
    when(commandExecutor.executeCommand(listListStringCommandObject)).thenReturn(expectedResponse);

    List<List<String>> result = jedis.jsonObjKeys(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listListStringCommandObject);
    verify(commandObjects).jsonObjKeys(key, path);
  }

  @Test
  public void testJsonObjLen() {
    String key = "testKey";
    Long expectedResponse = 5L;

    when(commandObjects.jsonObjLen(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    Long result = jedis.jsonObjLen(key);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).jsonObjLen(key);
  }

  @Test
  public void testJsonObjLenWithPath() {
    String key = "testKey";
    Path path = Path.of(".path.to.object");
    Long expectedResponse = 3L;

    when(commandObjects.jsonObjLen(key, path)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    Long result = jedis.jsonObjLen(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).jsonObjLen(key, path);
  }

  @Test
  public void testJsonObjLenWithPath2() {
    String key = "testKey";
    Path2 path = Path2.of(".path.to.object");
    List<Long> expectedResponse = Collections.singletonList(3L);

    when(commandObjects.jsonObjLen(key, path)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expectedResponse);

    List<Long> result = jedis.jsonObjLen(key, path);

    assertEquals(expectedResponse, result);

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).jsonObjLen(key, path);
  }

  @Test
  public void testSetJsonObjectMapper() {
    JsonObjectMapper jsonObjectMapper = mock(JsonObjectMapper.class);

    jedis.setJsonObjectMapper(jsonObjectMapper);

    verify(commandObjects).setJsonObjectMapper(jsonObjectMapper);
  }

}
