package redis.clients.jedis;

import java.util.List;

public interface JedisClusterScriptingCommands {
  Object eval(String script, int keyCount, String... params);

  Object eval(String script, List<String> keys, List<String> args);

  Object eval(String script, String key);

  Object evalsha(String script, String key);

  Object evalsha(String sha1, List<String> keys, List<String> args);

  Object evalsha(String sha1, int keyCount, String... params);

  Boolean scriptExists(String sha1, String key);

  List<Boolean> scriptExists(String key, String... sha1);

  String scriptLoad(String script, String key);
}