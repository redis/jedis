package redis.clients.jedis.json;

import java.util.List;

public interface RedisJsonCommands {

  String jsonSet(String key, Object object);

  String jsonSet(String key, Object object, JsonSetParams params);

  String jsonSet(String key, Path path, Object object);

  String jsonSet(String key, Path path, Object object, JsonSetParams params);

  //<T> T jsonGet(String key);

  //<T> T jsonGet(String key, Path... paths);

  <T> T jsonGet(String key, Class<T> clazz);

  <T> T jsonGet(String key, Class<T> clazz, Path... paths);

  <T> List<T> jsonMGet(Class<T> clazz, String... keys);

  <T> List<T> jsonMGet(Path path, Class<T> clazz, String... keys);

  Long jsonDel(String key);

  Long jsonDel(String key, Path path);

  Long jsonClear(String key, Path path);

  String jsonToggle(String key, Path path);

  Class<?> jsonType(String key);

  Class<?> jsonType(String key, Path path);

  Long jsonStrAppend(String key, Path path, Object... objects);

  Long jsonStrLen(String key, Path path);

  Long jsonArrAppend(String key, Path path, Object... objects);

  Long jsonArrIndex(String key, Path path, Object scalar);

  Long jsonArrInsert(String key, Path path, Long index, Object... objects);

  Long jsonArrLen(String key, Path path);

  <T> T jsonArrPop(String key, Class<T> clazz, Path path, Long index);

  <T> T jsonArrPop(String key, Class<T> clazz, Path path);

  <T> T jsonArrPop(String key, Class<T> clazz);

  Long jsonArrTrim(String key, Path path, Long start, Long stop);
}
