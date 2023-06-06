package redis.clients.jedis.json.commands;

import java.util.List;
import redis.clients.jedis.Response;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path;

public interface RedisJsonV1PipelineCommands extends RedisJsonV2PipelineCommands {

  default Response<String> jsonSetLegacy(String key, Object pojo) {
    return jsonSet(key, Path.ROOT_PATH, pojo);
  }

  default Response<String> jsonSetLegacy(String key, Object pojo, JsonSetParams params) {
    return jsonSet(key, Path.ROOT_PATH, pojo, params);
  }

  Response<String> jsonSet(String key, Path path, Object pojo);

  Response<String> jsonSet(String key, Path path, Object pojo, JsonSetParams params);

  Response<String> jsonMerge(String key, Path path, Object pojo);

  Response<Object> jsonGet(String key); // both ver

  <T> Response<T> jsonGet(String key, Class<T> clazz);

  Response<Object> jsonGet(String key, Path... paths);

  <T> Response<T> jsonGet(String key, Class<T> clazz, Path... paths);

  default <T> Response<List<T>> jsonMGet(Class<T> clazz, String... keys) {
    return jsonMGet(Path.ROOT_PATH, clazz, keys);
  }

  <T> Response<List<T>> jsonMGet(Path path, Class<T> clazz, String... keys);

  Response<Long> jsonDel(String key); // both ver

  Response<Long> jsonDel(String key, Path path);

  Response<Long> jsonClear(String key); // no test

  Response<Long> jsonClear(String key, Path path);

  Response<String> jsonToggle(String key, Path path);

  Response<Class<?>> jsonType(String key);

  Response<Class<?>> jsonType(String key, Path path);

  Response<Long> jsonStrAppend(String key, Object string);

  Response<Long> jsonStrAppend(String key, Path path, Object string);

  Response<Long> jsonStrLen(String key);

  Response<Long> jsonStrLen(String key, Path path);

  Response<Double> jsonNumIncrBy(String key, Path path, double value);

  Response<Long> jsonArrAppend(String key, Path path, Object... pojos);

  Response<Long> jsonArrIndex(String key, Path path, Object scalar);

  Response<Long> jsonArrInsert(String key, Path path, int index, Object... pojos);

  Response<Object> jsonArrPop(String key);

  <T> Response<T> jsonArrPop(String key, Class<T> clazz);

  Response<Object> jsonArrPop(String key, Path path);

  <T> Response<T> jsonArrPop(String key, Class<T> clazz, Path path);

  Response<Object> jsonArrPop(String key, Path path, int index);

  <T> Response<T> jsonArrPop(String key, Class<T> clazz, Path path, int index);

  Response<Long> jsonArrLen(String key);

  Response<Long> jsonArrLen(String key, Path path);

  Response<Long> jsonArrTrim(String key, Path path, int start, int stop);
}
