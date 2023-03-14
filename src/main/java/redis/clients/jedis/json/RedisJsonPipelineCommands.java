package redis.clients.jedis.json;

import org.json.JSONArray;
import redis.clients.jedis.Response;

import java.util.List;

public interface RedisJsonPipelineCommands {

  default Response<String> jsonSet(String key, Object object) {
    return RedisJsonPipelineCommands.this.jsonSet(key, Path2.ROOT_PATH, object);
  }

  default Response<String> jsonSetWithEscape(String key, Object object) {
    return jsonSetWithEscape(key, Path2.ROOT_PATH, object);
  }

  default Response<String> jsonSetLegacy(String key, Object pojo) {
    return jsonSet(key, Path.ROOT_PATH, pojo);
  }

  default Response<String> jsonSet(String key, Object object, JsonSetParams params) {
    return jsonSet(key, Path2.ROOT_PATH, object, params);
  }

  default Response<String> jsonSetWithEscape(String key, Object object, JsonSetParams params) {
    return jsonSetWithEscape(key, Path2.ROOT_PATH, object, params);
  }

  default Response<String> jsonSetLegacy(String key, Object pojo, JsonSetParams params) {
    return jsonSet(key, Path.ROOT_PATH, pojo, params);
  }

  //<T> Response<T> jsonGet(String key);

  //<T> Response<T> jsonGet(String key, Path... paths);

  Response<String> jsonSet(String key, Path2 path, Object object);

  Response<String> jsonSetWithEscape(String key, Path2 path, Object object);

  Response<String> jsonSet(String key, Path path, Object pojo);

  Response<String> jsonSet(String key, Path2 path, Object object, JsonSetParams params);

  Response<String> jsonSetWithEscape(String key, Path2 path, Object object, JsonSetParams params);

  Response<String> jsonSet(String key, Path path, Object pojo, JsonSetParams params);

  Response<Object> jsonGet(String key);

  <T> Response<T> jsonGet(String key, Class<T> clazz);

  Response<Object> jsonGet(String key, Path2... paths);

  Response<Object> jsonGet(String key, Path... paths);

  <T> Response<T> jsonGet(String key, Class<T> clazz, Path... paths);

  default Response<List<JSONArray>> jsonMGet(String... keys) {
    return jsonMGet(Path2.ROOT_PATH, keys);
  }

  default <T> Response<List<T>> jsonMGet(Class<T> clazz, String... keys) {
    return jsonMGet(Path.ROOT_PATH, clazz, keys);
  }

  Response<List<JSONArray>> jsonMGet(Path2 path, String... keys);

  <T> Response<List<T>> jsonMGet(Path path, Class<T> clazz, String... keys);

  Response<Long> jsonDel(String key);

  Response<Long> jsonDel(String key, Path2 path);

  Response<Long> jsonDel(String key, Path path);

  Response<Long> jsonClear(String key);

  Response<Long> jsonClear(String key, Path2 path);

  Response<Long> jsonClear(String key, Path path);

  Response<List<Boolean>> jsonToggle(String key, Path2 path);

  Response<String> jsonToggle(String key, Path path);

  Response<Class<?>> jsonType(String key);

  Response<List<Class<?>>> jsonType(String key, Path2 path);

  Response<Class<?>> jsonType(String key, Path path);

  Response<Long> jsonStrAppend(String key, Object string);

  Response<List<Long>> jsonStrAppend(String key, Path2 path, Object string);

  Response<Long> jsonStrAppend(String key, Path path, Object string);

  Response<Long> jsonStrLen(String key);

  Response<List<Long>> jsonStrLen(String key, Path2 path);

  Response<Long> jsonStrLen(String key, Path path);

  Response<JSONArray> jsonNumIncrBy(String key, Path2 path, double value);

  Response<Double> jsonNumIncrBy(String key, Path path, double value);

  Response<List<Long>> jsonArrAppend(String key, Path2 path, Object... objects);

  Response<List<Long>> jsonArrAppendWithEscape(String key, Path2 path, Object... objects);

  Response<Long> jsonArrAppend(String key, Path path, Object... pojos);

  Response<List<Long>> jsonArrIndex(String key, Path2 path, Object scalar);

  Response<List<Long>> jsonArrIndexWithEscape(String key, Path2 path, Object scalar);

  Response<Long> jsonArrIndex(String key, Path path, Object scalar);

  Response<List<Long>> jsonArrInsert(String key, Path2 path, int index, Object... objects);

  Response<List<Long>> jsonArrInsertWithEscape(String key, Path2 path, int index, Object... objects);

  Response<Long> jsonArrInsert(String key, Path path, int index, Object... pojos);

  Response<Object> jsonArrPop(String key);

  <T> Response<T> jsonArrPop(String key, Class<T> clazz);

  Response<List<Object>> jsonArrPop(String key, Path2 path);

  Response<Object> jsonArrPop(String key, Path path);

  <T> Response<T> jsonArrPop(String key, Class<T> clazz, Path path);

  Response<List<Object>> jsonArrPop(String key, Path2 path, int index);

  Response<Object> jsonArrPop(String key, Path path, int index);

  <T> Response<T> jsonArrPop(String key, Class<T> clazz, Path path, int index);

  Response<Long> jsonArrLen(String key);

  Response<List<Long>> jsonArrLen(String key, Path2 path);

  Response<Long> jsonArrLen(String key, Path path);

  Response<List<Long>> jsonArrTrim(String key, Path2 path, int start, int stop);

  Response<Long> jsonArrTrim(String key, Path path, int start, int stop);
}
