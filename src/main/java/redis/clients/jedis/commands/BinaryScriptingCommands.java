package redis.clients.jedis.commands;

import java.util.List;
import redis.clients.jedis.util.SafeEncoder;

public interface BinaryScriptingCommands {

  default Object eval(byte[] script) {
    return eval(script, 0);
  }

  /**
   * @deprecated This method will be removed in future. Use {@link #eval(byte..., int, byte[]...)}.
   */
  @Deprecated
  default Object eval(byte[] script, byte[] keyCount, byte[]... params) {
    return eval(script, Integer.parseInt(SafeEncoder.encode(keyCount)), params);
  }

  Object eval(byte[] script, int keyCount, byte[]... params);

  Object eval(byte[] script, List<byte[]> keys, List<byte[]> args);

  default Object evalsha(byte[] sha1) {
    return evalsha(sha1, 0);
  }

  Object evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args);

  Object evalsha(byte[] sha1, int keyCount, byte[]... params);

  // TODO: should be Boolean, add singular version
  List<Long> scriptExists(byte[]... sha1);

  byte[] scriptLoad(byte[] script);

  String scriptFlush();

  String scriptKill();
}
