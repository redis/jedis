package redis.clients.jedis.json.commands;

import java.util.List;
import org.json.JSONArray;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path2;

public interface RedisJsonV2Commands {

  default String jsonSet(String key, Object object) {
    return jsonSet(key, Path2.ROOT_PATH, object);
  }

  default String jsonSetWithEscape(String key, Object object) {
    return jsonSetWithEscape(key, Path2.ROOT_PATH, object);
  }

  default String jsonSet(String key, Object object, JsonSetParams params) {
    return jsonSet(key, Path2.ROOT_PATH, object, params);
  }

  default String jsonSetWithEscape(String key, Object object, JsonSetParams params) {
    return jsonSetWithEscape(key, Path2.ROOT_PATH, object, params);
  }

  String jsonSet(String key, Path2 path, Object object);

  String jsonSetWithEscape(String key, Path2 path, Object object);

  String jsonSet(String key, Path2 path, Object object, JsonSetParams params);

  String jsonSetWithEscape(String key, Path2 path, Object object, JsonSetParams params);

  String jsonMerge(String key, Path2 path, Object object);

  Object jsonGet(String key); // both ver

  Object jsonGet(String key, Path2... paths);

  default List<JSONArray> jsonMGet(String... keys) {
    return jsonMGet(Path2.ROOT_PATH, keys);
  }

  List<JSONArray> jsonMGet(Path2 path, String... keys);

  long jsonDel(String key); // both ver

  long jsonDel(String key, Path2 path);

  long jsonClear(String key); // no test

  long jsonClear(String key, Path2 path);

  List<Boolean> jsonToggle(String key, Path2 path);

  List<Class<?>> jsonType(String key, Path2 path);

  List<Long> jsonStrAppend(String key, Path2 path, Object string);

  List<Long> jsonStrLen(String key, Path2 path);

  Object jsonNumIncrBy(String key, Path2 path, double value);

  List<Long> jsonArrAppend(String key, Path2 path, Object... objects);

  List<Long> jsonArrAppendWithEscape(String key, Path2 path, Object... objects);

  List<Long> jsonArrIndex(String key, Path2 path, Object scalar);

  List<Long> jsonArrIndexWithEscape(String key, Path2 path, Object scalar);

  List<Long> jsonArrInsert(String key, Path2 path, int index, Object... objects);

  List<Long> jsonArrInsertWithEscape(String key, Path2 path, int index, Object... objects);

  List<Object> jsonArrPop(String key, Path2 path);

  List<Object> jsonArrPop(String key, Path2 path, int index);

  List<Long> jsonArrLen(String key, Path2 path);

  List<Long> jsonArrTrim(String key, Path2 path, int start, int stop);

  List<Long> jsonObjLen(String key, Path2 path);

  List<List<String>> jsonObjKeys(String key, Path2 path);

  List<Long> jsonDebugMemory(String key, Path2 path);
}
