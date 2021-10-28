package redis.clients.jedis.json;

import redis.clients.jedis.Response;

public interface RedisJsonPipelineCommands {

  Response<String> jsonSet(String key, Object object);

  Response<String> jsonSet(String key, Path path, Object object);

  <T> Response<T> jsonGet(String key, Class<T> clazz);

  <T> Response<T> jsonGet(String key, Class<T> clazz, Path... paths);

  Response<Long> jsonDel(String key);

  Response<Long> jsonDel(String key, Path path);
}
