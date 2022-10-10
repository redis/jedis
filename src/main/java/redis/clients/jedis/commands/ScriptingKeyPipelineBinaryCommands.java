package redis.clients.jedis.commands;

import java.util.List;
import redis.clients.jedis.Response;

public interface ScriptingKeyPipelineBinaryCommands {

  Response<Object> eval(byte[] script);

  Response<Object> eval(byte[] script, int keyCount, byte[]... params);

  Response<Object> eval(byte[] script, List<byte[]> keys, List<byte[]> args);

  Response<Object> evalReadonly(byte[] script, List<byte[]> keys, List<byte[]> args);

  Response<Object> evalsha(byte[] sha1);

  Response<Object> evalsha(byte[] sha1, int keyCount, byte[]... params);

  Response<Object> evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args);

  Response<Object> evalshaReadonly(byte[] sha1, List<byte[]> keys, List<byte[]> args);
}
