package redis.clients.jedis.json.commands;

import java.util.List;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path;

public interface RedisJsonV1Commands {

  default String jsonSetLegacy(String key, Object pojo) {
    return jsonSet(key, Path.ROOT_PATH, pojo);
  }

  default String jsonSetLegacy(String key, Object pojo, JsonSetParams params) {
    return jsonSet(key, Path.ROOT_PATH, pojo, params);
  }

  String jsonSet(String key, Path path, Object pojo);

  String jsonSetWithPlainString(String key, Path path, String string);

  String jsonSet(String key, Path path, Object pojo, JsonSetParams params);

  String jsonMerge(String key, Path path, Object pojo);

  Object jsonGet(String key); // both ver

  <T> T jsonGet(String key, Class<T> clazz);

  Object jsonGet(String key, Path... paths);

  String jsonGetAsPlainString(String key, Path path);

  <T> T jsonGet(String key, Class<T> clazz, Path... paths);

  default <T> List<T> jsonMGet(Class<T> clazz, String... keys) {
    return jsonMGet(Path.ROOT_PATH, clazz, keys);
  }

  <T> List<T> jsonMGet(Path path, Class<T> clazz, String... keys);

  long jsonDel(String key); // both ver

  long jsonDel(String key, Path path);

  long jsonClear(String key); // no test

  long jsonClear(String key, Path path);

  String jsonToggle(String key, Path path);

  Class<?> jsonType(String key);

  Class<?> jsonType(String key, Path path);

  long jsonStrAppend(String key, Object string);

  long jsonStrAppend(String key, Path path, Object string);

  Long jsonStrLen(String key);

  Long jsonStrLen(String key, Path path);

  double jsonNumIncrBy(String key, Path path, double value);

  Long jsonArrAppend(String key, Path path, Object... pojos);

  long jsonArrIndex(String key, Path path, Object scalar);

  long jsonArrInsert(String key, Path path, int index, Object... pojos);

  Object jsonArrPop(String key);

  <T> T jsonArrPop(String key, Class<T> clazz);

  Object jsonArrPop(String key, Path path);

  <T> T jsonArrPop(String key, Class<T> clazz, Path path);

  Object jsonArrPop(String key, Path path, int index);

  <T> T jsonArrPop(String key, Class<T> clazz, Path path, int index);

  Long jsonArrLen(String key);

  Long jsonArrLen(String key, Path path);

  Long jsonArrTrim(String key, Path path, int start, int stop);

  Long jsonObjLen(String key);

  Long jsonObjLen(String key, Path path);

  List<String> jsonObjKeys(String key);

  List<String> jsonObjKeys(String key, Path path);

  long jsonDebugMemory(String key);

  long jsonDebugMemory(String key, Path path);

  List<Object> jsonResp(String key);

  List<Object> jsonResp(String key, Path path);
}
