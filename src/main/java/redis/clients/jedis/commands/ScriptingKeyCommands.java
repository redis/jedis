package redis.clients.jedis.commands;

import java.util.List;

public interface ScriptingKeyCommands {

  Object eval(String script);

  Object eval(String script, int keyCount, String... params);

  Object eval(String script, List<String> keys, List<String> args);

  Object evalsha(String sha1);

  Object evalsha(String sha1, int keyCount, String... params);

  Object evalsha(String sha1, List<String> keys, List<String> args);
}
