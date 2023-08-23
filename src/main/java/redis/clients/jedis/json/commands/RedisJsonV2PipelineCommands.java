package redis.clients.jedis.json.commands;

import java.util.List;
import org.json.JSONArray;
import redis.clients.jedis.Response;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path2;

public interface RedisJsonV2PipelineCommands {

  default Response<String> jsonSet(String key, Object object) {
    return jsonSet(key, Path2.ROOT_PATH, object);
  }

  default Response<String> jsonSetWithEscape(String key, Object object) {
    return jsonSetWithEscape(key, Path2.ROOT_PATH, object);
  }

  default Response<String> jsonSet(String key, Object object, JsonSetParams params) {
    return jsonSet(key, Path2.ROOT_PATH, object, params);
  }

  default Response<String> jsonSetWithEscape(String key, Object object, JsonSetParams params) {
    return jsonSetWithEscape(key, Path2.ROOT_PATH, object, params);
  }

  Response<String> jsonSet(String key, Path2 path, Object object);

  Response<String> jsonSetWithEscape(String key, Path2 path, Object object);

  Response<String> jsonSet(String key, Path2 path, Object object, JsonSetParams params);

  Response<String> jsonSetWithEscape(String key, Path2 path, Object object, JsonSetParams params);

  Response<String> jsonMerge(String key, Path2 path, Object object);

  Response<Object> jsonGet(String key); // both ver

  Response<Object> jsonGet(String key, Path2... paths);

  default Response<List<JSONArray>> jsonMGet(String... keys) {
    return jsonMGet(Path2.ROOT_PATH, keys);
  }

  Response<List<JSONArray>> jsonMGet(Path2 path, String... keys);

  Response<Long> jsonDel(String key); // both ver

  Response<Long> jsonDel(String key, Path2 path);

  Response<Long> jsonClear(String key); // no test

  Response<Long> jsonClear(String key, Path2 path);

  Response<List<Boolean>> jsonToggle(String key, Path2 path);

  Response<List<Class<?>>> jsonType(String key, Path2 path);

  Response<List<Long>> jsonStrAppend(String key, Path2 path, Object string);

  Response<List<Long>> jsonStrLen(String key, Path2 path);

  Response<Object> jsonNumIncrBy(String key, Path2 path, double value);

  Response<List<Long>> jsonArrAppend(String key, Path2 path, Object... objects);

  Response<List<Long>> jsonArrAppendWithEscape(String key, Path2 path, Object... objects);

  Response<List<Long>> jsonArrIndex(String key, Path2 path, Object scalar);

  Response<List<Long>> jsonArrIndexWithEscape(String key, Path2 path, Object scalar);

  Response<List<Long>> jsonArrInsert(String key, Path2 path, int index, Object... objects);

  Response<List<Long>> jsonArrInsertWithEscape(String key, Path2 path, int index, Object... objects);

  Response<List<Object>> jsonArrPop(String key, Path2 path);

  Response<List<Object>> jsonArrPop(String key, Path2 path, int index);

  Response<List<Long>> jsonArrLen(String key, Path2 path);

  Response<List<Long>> jsonArrTrim(String key, Path2 path, int start, int stop);
}
