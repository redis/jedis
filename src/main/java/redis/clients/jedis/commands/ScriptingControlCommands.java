package redis.clients.jedis.commands;

import java.util.List;
import redis.clients.jedis.args.FlushMode;

public interface ScriptingControlCommands {

  Boolean scriptExists(String sha1);

  List<Boolean> scriptExists(String... sha1);

  Boolean scriptExists(byte[] sha1);

  List<Boolean> scriptExists(byte[]... sha1);

  String scriptLoad(String script);

  byte[] scriptLoad(byte[] script);

  String scriptFlush();

  String scriptFlush(FlushMode flushMode);

  String scriptKill();

}
