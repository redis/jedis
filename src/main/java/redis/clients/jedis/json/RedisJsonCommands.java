package redis.clients.jedis.json;

import java.util.List;
import org.json.JSONArray;

public interface RedisJsonCommands {

  default String jsonSet(String key, Object object) {
    return RedisJsonCommands.this.jsonSet(key, Path2.ROOT_PATH, object);
  }

  default String jsonSetWithEscape(String key, Object object) {
    return jsonSetWithEscape(key, Path2.ROOT_PATH, object);
  }

  default String jsonSetLegacy(String key, Object pojo) {
    return jsonSet(key, Path.ROOT_PATH, pojo);
  }

  default String jsonSet(String key, Object object, JsonSetParams params) {
    return jsonSet(key, Path2.ROOT_PATH, object, params);
  }

  default String jsonSetWithEscape(String key, Object object, JsonSetParams params) {
    return jsonSetWithEscape(key, Path2.ROOT_PATH, object, params);
  }

  default String jsonSetLegacy(String key, Object pojo, JsonSetParams params) {
    return jsonSet(key, Path.ROOT_PATH, pojo, params);
  }

  String jsonSet(String key, Path2 path, Object object);

  String jsonSetWithEscape(String key, Path2 path, Object object);

  String jsonSet(String key, Path path, Object pojo);

  String jsonSetWithPlainString(String key, Path path, String string);

  String jsonSet(String key, Path2 path, Object object, JsonSetParams params);

  String jsonSetWithEscape(String key, Path2 path, Object object, JsonSetParams params);

  String jsonSet(String key, Path path, Object pojo, JsonSetParams params);

  Object jsonGet(String key);

  <T> T jsonGet(String key, Class<T> clazz);

  Object jsonGet(String key, Path2... paths);

  Object jsonGet(String key, Path... paths);

  String jsonGetAsPlainString(String key, Path path);

  <T> T jsonGet(String key, Class<T> clazz, Path... paths);

  default List<JSONArray> jsonMGet(String... keys) {
    return jsonMGet(Path2.ROOT_PATH, keys);
  }

  default <T> List<T> jsonMGet(Class<T> clazz, String... keys) {
    return jsonMGet(Path.ROOT_PATH, clazz, keys);
  }

  List<JSONArray> jsonMGet(Path2 path, String... keys);

  <T> List<T> jsonMGet(Path path, Class<T> clazz, String... keys);

  long jsonDel(String key);

  long jsonDel(String key, Path2 path);

  long jsonDel(String key, Path path);

  long jsonClear(String key);

  long jsonClear(String key, Path2 path);

  long jsonClear(String key, Path path);

  List<Boolean> jsonToggle(String key, Path2 path);

  String jsonToggle(String key, Path path);

  Class<?> jsonType(String key);

  List<Class<?>> jsonType(String key, Path2 path);

  Class<?> jsonType(String key, Path path);

  long jsonStrAppend(String key, Object string);

  List<Long> jsonStrAppend(String key, Path2 path, Object string);

  long jsonStrAppend(String key, Path path, Object string);

  Long jsonStrLen(String key);

  List<Long> jsonStrLen(String key, Path2 path);

  Long jsonStrLen(String key, Path path);

  JSONArray jsonNumIncrBy(String key, Path2 path, double value);

  double jsonNumIncrBy(String key, Path path, double value);

  List<Long> jsonArrAppend(String key, Path2 path, Object... objects);

  List<Long> jsonArrAppendWithEscape(String key, Path2 path, Object... objects);

  Long jsonArrAppend(String key, Path path, Object... pojos);

  List<Long> jsonArrIndex(String key, Path2 path, Object scalar);

  List<Long> jsonArrIndexWithEscape(String key, Path2 path, Object scalar);

  long jsonArrIndex(String key, Path path, Object scalar);

  List<Long> jsonArrInsert(String key, Path2 path, int index, Object... objects);

  List<Long> jsonArrInsertWithEscape(String key, Path2 path, int index, Object... objects);

  long jsonArrInsert(String key, Path path, int index, Object... pojos);

  Object jsonArrPop(String key);

  <T> T jsonArrPop(String key, Class<T> clazz);

  List<Object> jsonArrPop(String key, Path2 path);

  Object jsonArrPop(String key, Path path);

  <T> T jsonArrPop(String key, Class<T> clazz, Path path);

  List<Object> jsonArrPop(String key, Path2 path, int index);

  Object jsonArrPop(String key, Path path, int index);

  <T> T jsonArrPop(String key, Class<T> clazz, Path path, int index);

  Long jsonArrLen(String key);

  List<Long> jsonArrLen(String key, Path2 path);

  Long jsonArrLen(String key, Path path);

  List<Long> jsonArrTrim(String key, Path2 path, int start, int stop);

  Long jsonArrTrim(String key, Path path, int start, int stop);

  Long jsonObjLen(String key);

  Long jsonObjLen(String key, Path path);

  List<Long> jsonObjLen(String key, Path2 path);

  List<String> jsonObjKeys(String key);

  List<String> jsonObjKeys(String key, Path path);

  List<List<String>> jsonObjKeys(String key, Path2 path);

  long jsonDebugMemory(String key);

  long jsonDebugMemory(String key, Path path);

  List<Long> jsonDebugMemory(String key, Path2 path);

  List<Object> jsonResp(String key);

  List<Object> jsonResp(String key, Path path);

  List<List<Object>> jsonResp(String key, Path2 path);
}
