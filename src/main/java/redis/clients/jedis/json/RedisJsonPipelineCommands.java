package redis.clients.jedis.json;

import redis.clients.jedis.Response;

import java.util.List;

public interface RedisJsonPipelineCommands {

  Response<String> jsonSet(String key, Object object);

  Response<String> jsonSet(String key, Object object, ExistenceModifier flag);

  Response<String> jsonSet(String key, Path path, Object object);

  Response<String> jsonSet(String key, Path path, Object object, ExistenceModifier flag);

  //<T> Response<T> jsonGet(String key);

  //<T> Response<T> jsonGet(String key, Path... paths);

  <T> Response<T> jsonGet(String key, Class<T> clazz);

  <T> Response<T> jsonGet(String key, Class<T> clazz, Path... paths);

  <T> Response<List<T>> jsonMGet(Class<T> clazz, String... keys);

  <T> Response<List<T>> jsonMGet(Path path, Class<T> clazz, String... keys);

  Response<Long> jsonDel(String key);

  Response<Long> jsonDel(String key, Path path);

  Response<Long> jsonClear(String key, Path path);

  Response<String> jsonToggle(String key, Path path);

  Response<Class<?>> jsonType(String key);

  Response<Class<?>> jsonType(String key, Path path);

  Response<Long> jsonStrAppend(String key, Path path, Object... objects);

  Response<Long> jsonStrLen(String key, Path path);

  Response<Long> jsonArrAppend(String key, Path path, Object... objects);

  Response<Long> jsonArrIndex(String key, Path path, Object scalar);

  Response<Long> jsonArrInsert(String key, Path path, Long index, Object... objects);

  Response<Long> jsonArrLen(String key, Path path);

  <T> Response<T> jsonArrPop(String key, Class<T> clazz, Path path, Long index);

  <T> Response<T> jsonArrPop(String key, Class<T> clazz, Path path);

  <T> Response<T> jsonArrPop(String key, Class<T> clazz);

  Response<Long> jsonArrTrim(String key, Path path, Long start, Long stop);
}
