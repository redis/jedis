package redis.clients.jedis;

import java.util.List;

public interface JedisClusterBinaryScriptingCommands {
  Object eval(byte[] script, byte[] keyCount, byte[]... params);

  Object eval(byte[] script, int keyCount, byte[]... params);

  Object eval(byte[] script, List<byte[]> keys, List<byte[]> args);

  Object eval(byte[] script, byte[] key);

  Object evalsha(byte[] script, byte[] key);

  Object evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args);

  Object evalsha(byte[] sha1, int keyCount, byte[]... params);

  List<Long> scriptExists(byte[] key, byte[][] sha1);

  byte[] scriptLoad(byte[] script, byte[] key);

  String scriptFlush(byte[] key);

  String scriptKill(byte[] key);
}