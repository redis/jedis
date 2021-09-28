package redis.clients.jedis.commands;

import java.util.List;
import redis.clients.jedis.args.FlushMode;

public interface ScriptingCommands {

  Object eval(String script);

  Object eval(String script, int keyCount, String... params);

  Object eval(String script, List<String> keys, List<String> args);

  Object evalsha(String sha1);

  Object evalsha(String sha1, int keyCount, String... params);

  Object evalsha(String sha1, List<String> keys, List<String> args);

  Boolean scriptExists(String sha1);

  List<Boolean> scriptExists(String... sha1);

  String scriptLoad(String script);

  String scriptFlush();

  String scriptFlush(FlushMode flushMode);

  String scriptKill();
}
