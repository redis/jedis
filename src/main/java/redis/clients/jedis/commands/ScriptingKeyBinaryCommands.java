package redis.clients.jedis.commands;

import java.util.List;

public interface ScriptingKeyBinaryCommands {

  Object eval(byte[] script);

  Object eval(byte[] script, int keyCount, byte[]... params);

  Object eval(byte[] script, List<byte[]> keys, List<byte[]> args);

  Object evalReadonly(byte[] script, List<byte[]> keys, List<byte[]> args);

  Object evalsha(byte[] sha1);

  Object evalsha(byte[] sha1, int keyCount, byte[]... params);

  Object evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args);

  Object evalshaReadonly(byte[] sha1, List<byte[]> keys, List<byte[]> args);
}
