package redis.clients.jedis.json.commands;

import java.util.List;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path;

/**
 * @deprecated RedisJSON (v1) support is deprecated.
 */
@Deprecated
public interface RedisJsonV1Commands {

  @Deprecated
  default String jsonSetLegacy(String key, Object pojo) {
    return jsonSet(key, Path.ROOT_PATH, pojo);
  }

  @Deprecated
  default String jsonSetLegacy(String key, Object pojo, JsonSetParams params) {
    return jsonSet(key, Path.ROOT_PATH, pojo, params);
  }

  @Deprecated
  String jsonSet(String key, Path path, Object pojo);

  @Deprecated
  String jsonSetWithPlainString(String key, Path path, String string);

  @Deprecated
  String jsonSet(String key, Path path, Object pojo, JsonSetParams params);

  @Deprecated
  String jsonMerge(String key, Path path, Object pojo);

  Object jsonGet(String key); // both ver

  @Deprecated
  <T> T jsonGet(String key, Class<T> clazz);

  @Deprecated
  Object jsonGet(String key, Path... paths);

  @Deprecated
  String jsonGetAsPlainString(String key, Path path);

  @Deprecated
  <T> T jsonGet(String key, Class<T> clazz, Path... paths);

  @Deprecated
  default <T> List<T> jsonMGet(Class<T> clazz, String... keys) {
    return jsonMGet(Path.ROOT_PATH, clazz, keys);
  }

  @Deprecated
  <T> List<T> jsonMGet(Path path, Class<T> clazz, String... keys);

  long jsonDel(String key); // both ver

  @Deprecated
  long jsonDel(String key, Path path);

  long jsonClear(String key); // no test

  @Deprecated
  long jsonClear(String key, Path path);

  @Deprecated
  String jsonToggle(String key, Path path);

  @Deprecated
  Class<?> jsonType(String key);

  @Deprecated
  Class<?> jsonType(String key, Path path);

  @Deprecated
  long jsonStrAppend(String key, Object string);

  @Deprecated
  long jsonStrAppend(String key, Path path, Object string);

  @Deprecated
  Long jsonStrLen(String key);

  @Deprecated
  Long jsonStrLen(String key, Path path);

  @Deprecated
  double jsonNumIncrBy(String key, Path path, double value);

  @Deprecated
  Long jsonArrAppend(String key, Path path, Object... pojos);

  @Deprecated
  long jsonArrIndex(String key, Path path, Object scalar);

  @Deprecated
  long jsonArrInsert(String key, Path path, int index, Object... pojos);

  @Deprecated
  Object jsonArrPop(String key);

  @Deprecated
  <T> T jsonArrPop(String key, Class<T> clazz);

  @Deprecated
  Object jsonArrPop(String key, Path path);

  @Deprecated
  <T> T jsonArrPop(String key, Class<T> clazz, Path path);

  @Deprecated
  Object jsonArrPop(String key, Path path, int index);

  @Deprecated
  <T> T jsonArrPop(String key, Class<T> clazz, Path path, int index);

  @Deprecated
  Long jsonArrLen(String key);

  @Deprecated
  Long jsonArrLen(String key, Path path);

  @Deprecated
  Long jsonArrTrim(String key, Path path, int start, int stop);

  @Deprecated
  Long jsonObjLen(String key);

  @Deprecated
  Long jsonObjLen(String key, Path path);

  @Deprecated
  List<String> jsonObjKeys(String key);

  @Deprecated
  List<String> jsonObjKeys(String key, Path path);

  @Deprecated
  long jsonDebugMemory(String key);

  @Deprecated
  long jsonDebugMemory(String key, Path path);
}
