package redis.clients.jedis.mocked.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.google.gson.JsonObject;
import org.json.JSONArray;
import org.junit.Test;
import redis.clients.jedis.Response;
import redis.clients.jedis.json.JsonObjectMapper;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.json.Path2;

public class PipeliningBaseJsonCommandsTest extends PipeliningBaseMockedTestBase {

  @Test
  public void testJsonArrAppendWithPath() {
    Path path = new Path("$.array");
    Object[] objects = { "one", "two", "three" };

    when(commandObjects.jsonArrAppend("myJson", path, objects)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonArrAppend("myJson", path, objects);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrAppendWithPath2() {
    Path2 path = Path2.of("$.array");
    Object[] objects = { "one", "two", "three" };

    when(commandObjects.jsonArrAppend("myJson", path, objects)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.jsonArrAppend("myJson", path, objects);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrAppendWithPath2WithEscape() {
    Path2 path = Path2.of("$.array");
    Object[] objects = { "one", "two", "three" };

    when(commandObjects.jsonArrAppendWithEscape("myJson", path, objects)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.jsonArrAppendWithEscape("myJson", path, objects);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrIndexWithPath() {
    Path path = new Path("$.array");
    Object scalar = "two";

    when(commandObjects.jsonArrIndex("myJson", path, scalar)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonArrIndex("myJson", path, scalar);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrIndexWithPath2() {
    Path2 path = Path2.of("$.array");
    Object scalar = "two";

    when(commandObjects.jsonArrIndex("myJson", path, scalar)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.jsonArrIndex("myJson", path, scalar);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrIndexWithPath2WithEscape() {
    Path2 path = Path2.of("$.array");
    Object scalar = "two";

    when(commandObjects.jsonArrIndexWithEscape("myJson", path, scalar)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.jsonArrIndexWithEscape("myJson", path, scalar);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrInsertWithPath() {
    Path path = new Path("$.array");
    Object[] pojos = { "one", "two", "three" };

    when(commandObjects.jsonArrInsert("myJson", path, 1, pojos)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonArrInsert("myJson", path, 1, pojos);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrInsertWithPath2() {
    Path2 path = Path2.of("$.array");
    Object[] objects = { "one", "two", "three" };

    when(commandObjects.jsonArrInsert("myJson", path, 1, objects)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.jsonArrInsert("myJson", path, 1, objects);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrInsertWithPath2WithEscape() {
    Path2 path = Path2.of("$.array");
    Object[] objects = { "one", "two", "three" };

    when(commandObjects.jsonArrInsertWithEscape("myJson", path, 1, objects)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.jsonArrInsertWithEscape("myJson", path, 1, objects);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrLen() {
    when(commandObjects.jsonArrLen("myJson")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonArrLen("myJson");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrLenWithPath() {
    Path path = new Path("$.array");

    when(commandObjects.jsonArrLen("myJson", path)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonArrLen("myJson", path);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrLenWithPath2() {
    Path2 path = Path2.of("$.array");

    when(commandObjects.jsonArrLen("myJson", path)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.jsonArrLen("myJson", path);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrPop() {
    when(commandObjects.jsonArrPop("myJson")).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.jsonArrPop("myJson");

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrPopWithPath() {
    Path path = new Path("$.array");

    when(commandObjects.jsonArrPop("myJson", path)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.jsonArrPop("myJson", path);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrPopWithPathAndIndex() {
    Path path = new Path("$.array");

    when(commandObjects.jsonArrPop("myJson", path, 1)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.jsonArrPop("myJson", path, 1);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrPopWithClassAndPath() {
    Path path = new Path("$.array");

    when(commandObjects.jsonArrPop("myJson", MyBean.class, path)).thenReturn(myBeanCommandObject);

    Response<MyBean> response = pipeliningBase.jsonArrPop("myJson", MyBean.class, path);

    assertThat(commands, contains(myBeanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrPopWithClassPathAndIndex() {
    Path path = new Path("$.array");

    when(commandObjects.jsonArrPop("myJson", MyBean.class, path, 1)).thenReturn(myBeanCommandObject);

    Response<MyBean> response = pipeliningBase.jsonArrPop("myJson", MyBean.class, path, 1);

    assertThat(commands, contains(myBeanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrPopWithPath2() {
    Path2 path = Path2.of("$.array");

    when(commandObjects.jsonArrPop("myJson", path)).thenReturn(listObjectCommandObject);

    Response<List<Object>> response = pipeliningBase.jsonArrPop("myJson", path);

    assertThat(commands, contains(listObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrPopWithPath2AndIndex() {
    Path2 path = Path2.of("$.array");

    when(commandObjects.jsonArrPop("myJson", path, 1)).thenReturn(listObjectCommandObject);

    Response<List<Object>> response = pipeliningBase.jsonArrPop("myJson", path, 1);

    assertThat(commands, contains(listObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrPopWithClass() {
    when(commandObjects.jsonArrPop("myJson", MyBean.class)).thenReturn(myBeanCommandObject);

    Response<MyBean> response = pipeliningBase.jsonArrPop("myJson", MyBean.class);

    assertThat(commands, contains(myBeanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrTrimWithPath() {
    Path path = new Path("$.array");

    when(commandObjects.jsonArrTrim("myJson", path, 1, 2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonArrTrim("myJson", path, 1, 2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrTrimWithPath2() {
    Path2 path = Path2.of("$.array");

    when(commandObjects.jsonArrTrim("myJson", path, 1, 2)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.jsonArrTrim("myJson", path, 1, 2);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonClear() {
    when(commandObjects.jsonClear("myJson")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonClear("myJson");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonClearWithPath() {
    Path path = new Path("$.field");

    when(commandObjects.jsonClear("myJson", path)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonClear("myJson", path);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonClearWithPath2() {
    Path2 path = Path2.of("$.field");

    when(commandObjects.jsonClear("myJson", path)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonClear("myJson", path);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonDel() {
    when(commandObjects.jsonDel("myJson")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonDel("myJson");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonDelWithPath() {
    Path path = new Path("$.field");

    when(commandObjects.jsonDel("myJson", path)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonDel("myJson", path);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonDelWithPath2() {
    Path2 path = Path2.of("$.field");

    when(commandObjects.jsonDel("myJson", path)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonDel("myJson", path);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonGet() {
    when(commandObjects.jsonGet("myJson")).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.jsonGet("myJson");

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonGetWithClass() {
    when(commandObjects.jsonGet("myJson", MyBean.class)).thenReturn(myBeanCommandObject);

    Response<MyBean> response = pipeliningBase.jsonGet("myJson", MyBean.class);

    assertThat(commands, contains(myBeanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonGetWithPath() {
    Path[] paths = { new Path("$.field1"), new Path("$.field2") };

    when(commandObjects.jsonGet("myJson", paths)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.jsonGet("myJson", paths);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonGetWithPath2() {
    Path2[] paths = { Path2.of("$.field1"), Path2.of("$.field2") };

    when(commandObjects.jsonGet("myJson", paths)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.jsonGet("myJson", paths);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonGetWithClassAndPath() {
    Path[] paths = { new Path("$.field1"), new Path("$.field2") };

    when(commandObjects.jsonGet("myJson", MyBean.class, paths)).thenReturn(myBeanCommandObject);

    Response<MyBean> response = pipeliningBase.jsonGet("myJson", MyBean.class, paths);

    assertThat(commands, contains(myBeanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonMergeWithPath() {
    Path path = new Path("$.field");
    Object object = new JsonObject();

    when(commandObjects.jsonMerge("myJson", path, object)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.jsonMerge("myJson", path, object);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonMergeWithPath2() {
    Path2 path = Path2.of("$.field");
    Object object = new JsonObject();

    when(commandObjects.jsonMerge("myJson", path, object)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.jsonMerge("myJson", path, object);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonMGetWithPathAndClass() {
    Path path = new Path("$.field");

    when(commandObjects.jsonMGet(path, MyBean.class, "key1", "key2")).thenReturn(listMyBeanCommandObject);

    Response<List<MyBean>> response = pipeliningBase.jsonMGet(path, MyBean.class, "key1", "key2");

    assertThat(commands, contains(listMyBeanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonMGetWithPath2() {
    Path2 path = Path2.of("$.field");

    when(commandObjects.jsonMGet(path, "key1", "key2")).thenReturn(listJsonArrayCommandObject);

    Response<List<JSONArray>> response = pipeliningBase.jsonMGet(path, "key1", "key2");

    assertThat(commands, contains(listJsonArrayCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonNumIncrByWithPath() {
    Path path = new Path("$.number");

    when(commandObjects.jsonNumIncrBy("myJson", path, 42.0)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.jsonNumIncrBy("myJson", path, 42.0);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonNumIncrByWithPath2() {
    Path2 path = Path2.of("$.number");

    when(commandObjects.jsonNumIncrBy("myJson", path, 42.0)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.jsonNumIncrBy("myJson", path, 42.0);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonSetWithPath() {
    Path path = Path.of("$.field");
    Object object = new JsonObject();

    when(commandObjects.jsonSet("myJson", path, object)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.jsonSet("myJson", path, object);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonSetWithPathAndParams() {
    Path path = new Path("$.field");
    Object object = new JsonObject();
    JsonSetParams params = new JsonSetParams().nx();

    when(commandObjects.jsonSet("myJson", path, object, params)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.jsonSet("myJson", path, object, params);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonSetWithPath2() {
    Path2 path = Path2.of("$.field");
    Object object = new JsonObject();

    when(commandObjects.jsonSet("myJson", path, object)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.jsonSet("myJson", path, object);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonSetWithPath2WithEscape() {
    Path2 path = Path2.of("$.field");
    Object object = new JsonObject();

    when(commandObjects.jsonSetWithEscape("myJson", path, object)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.jsonSetWithEscape("myJson", path, object);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonSetWithPath2AndParams() {
    Path2 path = Path2.of("$.field");
    Object object = new JsonObject();
    JsonSetParams params = new JsonSetParams().nx();

    when(commandObjects.jsonSet("myJson", path, object, params)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.jsonSet("myJson", path, object, params);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonSetWithPath2EscapeAndParams() {
    Path2 path = Path2.of("$.field");
    Object object = new JsonObject();
    JsonSetParams params = new JsonSetParams().nx();

    when(commandObjects.jsonSetWithEscape("myJson", path, object, params)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.jsonSetWithEscape("myJson", path, object, params);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonStrAppend() {
    when(commandObjects.jsonStrAppend("myJson", "append")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonStrAppend("myJson", "append");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonStrAppendWithPath() {
    Path path = new Path("$.field");

    when(commandObjects.jsonStrAppend("myJson", path, "append")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonStrAppend("myJson", path, "append");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonStrAppendWithPath2() {
    Path2 path = Path2.of("$.field");

    when(commandObjects.jsonStrAppend("myJson", path, "append")).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.jsonStrAppend("myJson", path, "append");

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonStrLen() {
    when(commandObjects.jsonStrLen("myJson")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonStrLen("myJson");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonStrLenWithPath() {
    Path path = new Path("$.field");

    when(commandObjects.jsonStrLen("myJson", path)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonStrLen("myJson", path);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonStrLenWithPath2() {
    Path2 path = Path2.of("$.field");

    when(commandObjects.jsonStrLen("myJson", path)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.jsonStrLen("myJson", path);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonToggleWithPath() {
    Path path = new Path("$.field");

    when(commandObjects.jsonToggle("myJson", path)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.jsonToggle("myJson", path);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonToggleWithPath2() {
    Path2 path = Path2.of("$.field");

    when(commandObjects.jsonToggle("myJson", path)).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.jsonToggle("myJson", path);

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonType() {
    when(commandObjects.jsonType("myJson")).thenReturn(classCommandObject);

    Response<Class<?>> response = pipeliningBase.jsonType("myJson");

    assertThat(commands, contains(classCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonTypeWithPath() {
    Path path = new Path("$.field");

    when(commandObjects.jsonType("myJson", path)).thenReturn(classCommandObject);

    Response<Class<?>> response = pipeliningBase.jsonType("myJson", path);

    assertThat(commands, contains(classCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonTypeWithPath2() {
    Path2 path = Path2.of("$.field");

    when(commandObjects.jsonType("myJson", path)).thenReturn(listClassCommandObject);

    Response<List<Class<?>>> response = pipeliningBase.jsonType("myJson", path);

    assertThat(commands, contains(listClassCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSetJsonObjectMapper() {
    JsonObjectMapper jsonObjectMapper = mock(JsonObjectMapper.class);
    doNothing().when(commandObjects).setJsonObjectMapper(jsonObjectMapper);

    pipeliningBase.setJsonObjectMapper(jsonObjectMapper);

    verify(commandObjects).setJsonObjectMapper(jsonObjectMapper);
  }

}
