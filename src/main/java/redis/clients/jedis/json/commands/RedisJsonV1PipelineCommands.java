package redis.clients.jedis.json.commands;

import java.util.List;
import redis.clients.jedis.Response;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path;

/**
 * @deprecated RedisJSON (v1) support is deprecated.
 */
@Deprecated
public interface RedisJsonV1PipelineCommands {

  @Deprecated
  default Response<String> jsonSetLegacy(String key, Object pojo) {
    return jsonSet(key, Path.ROOT_PATH, pojo);
  }

  @Deprecated
  default Response<String> jsonSetLegacy(String key, Object pojo, JsonSetParams params) {
    return jsonSet(key, Path.ROOT_PATH, pojo, params);
  }

  @Deprecated
  Response<String> jsonSet(String key, Path path, Object pojo);

  @Deprecated
  Response<String> jsonSet(String key, Path path, Object pojo, JsonSetParams params);

  @Deprecated
  Response<String> jsonMerge(String key, Path path, Object pojo);

  Response<Object> jsonGet(String key); // both ver

  @Deprecated
  <T> Response<T> jsonGet(String key, Class<T> clazz);

  @Deprecated
  Response<Object> jsonGet(String key, Path... paths);

  @Deprecated
  <T> Response<T> jsonGet(String key, Class<T> clazz, Path... paths);

  @Deprecated
  default <T> Response<List<T>> jsonMGet(Class<T> clazz, String... keys) {
    return jsonMGet(Path.ROOT_PATH, clazz, keys);
  }

  @Deprecated
  <T> Response<List<T>> jsonMGet(Path path, Class<T> clazz, String... keys);

  Response<Long> jsonDel(String key); // both ver

  @Deprecated
  Response<Long> jsonDel(String key, Path path);

  @Deprecated
  Response<Long> jsonClear(String key); // no test

  @Deprecated
  Response<Long> jsonClear(String key, Path path);

  @Deprecated
  Response<String> jsonToggle(String key, Path path);

  @Deprecated
  Response<Class<?>> jsonType(String key);

  @Deprecated
  Response<Class<?>> jsonType(String key, Path path);

  @Deprecated
  Response<Long> jsonStrAppend(String key, Object string);

  @Deprecated
  Response<Long> jsonStrAppend(String key, Path path, Object string);

  @Deprecated
  Response<Long> jsonStrLen(String key);

  @Deprecated
  Response<Long> jsonStrLen(String key, Path path);

  @Deprecated
  Response<Double> jsonNumIncrBy(String key, Path path, double value);

  @Deprecated
  Response<Long> jsonArrAppend(String key, Path path, Object... pojos);

  @Deprecated
  Response<Long> jsonArrIndex(String key, Path path, Object scalar);

  @Deprecated
  Response<Long> jsonArrInsert(String key, Path path, int index, Object... pojos);

  @Deprecated
  Response<Object> jsonArrPop(String key);

  @Deprecated
  <T> Response<T> jsonArrPop(String key, Class<T> clazz);

  @Deprecated
  Response<Object> jsonArrPop(String key, Path path);

  @Deprecated
  <T> Response<T> jsonArrPop(String key, Class<T> clazz, Path path);

  @Deprecated
  Response<Object> jsonArrPop(String key, Path path, int index);

  @Deprecated
  <T> Response<T> jsonArrPop(String key, Class<T> clazz, Path path, int index);

  @Deprecated
  Response<Long> jsonArrLen(String key);

  @Deprecated
  Response<Long> jsonArrLen(String key, Path path);

  @Deprecated
  Response<Long> jsonArrTrim(String key, Path path, int start, int stop);
}
