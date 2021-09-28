package redis.clients.jedis.commands;

import java.util.List;
import redis.clients.jedis.args.FlushMode;

public interface BinaryScriptingCommands {

  Object eval(byte[] script);

  Object eval(byte[] script, int keyCount, byte[]... params);

  Object eval(byte[] script, List<byte[]> keys, List<byte[]> args);

  Object evalsha(byte[] sha1);

  Object evalsha(byte[] sha1, int keyCount, byte[]... params);

  Object evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args);

  // TODO: should be Boolean, add singular version
  List<Long> scriptExists(byte[]... sha1);

  byte[] scriptLoad(byte[] script);

  String scriptFlush();

  String scriptFlush(FlushMode flushMode);

  String scriptKill();
}
