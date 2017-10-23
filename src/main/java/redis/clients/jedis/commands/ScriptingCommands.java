package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.RedisScriptingRoutine;

public interface ScriptingCommands {
  Object eval(String script, int keyCount, String... params);

  Object eval(String script, List<String> keys, List<String> args);

  Object eval(String script);

  Object evalsha(String sha1);

  Object evalsha(String sha1, List<String> keys, List<String> args);

  Object evalsha(String sha1, int keyCount, String... params);

  Object smartEval(RedisScriptingRoutine script);

  Object smartEval(RedisScriptingRoutine script, List<String> keys, List<String> args);

  Object smartEval(RedisScriptingRoutine script, int keyCount, String... params);

  Boolean scriptExists(String sha1);

  List<Boolean> scriptExists(String... sha1);

  String scriptLoad(String script);
}
