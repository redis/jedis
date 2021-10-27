package redis.clients.jedis.json;

public interface RedisJsonCommands {

  String jsonSet(String key, Object object);

  String jsonSet(String key, Path path, Object object);

  <T> T jsonGet(String key, Class<T> clazz);

  <T> T jsonGet(String key, Class<T> clazz, Path... paths);

  Long jsonDel(String key);

  Long jsonDel(String key, Path path);
}
