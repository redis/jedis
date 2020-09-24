package redis.clients.jedis.commands;

import redis.clients.jedis.Response;

import java.util.List;

public interface BinaryScriptingCommandsPipeline {

  Response<Object> eval(byte[] targetKey, byte[] script, byte[] keyCount, byte[]... params);

  Response<Object> eval(byte[] targetKey, byte[] script, int keyCount, byte[]... params);

  Response<Object> eval(byte[] targetKey, byte[] script, List<byte[]> keys, List<byte[]> args);

  Response<Object> eval(byte[] targetKey, byte[] script);

  Response<Object> evalsha(byte[] targetKey, byte[] sha1);

  Response<Object> evalsha(byte[] targetKey, byte[] sha1, List<byte[]> keys, List<byte[]> args);

  Response<Object> evalsha(byte[] targetKey, byte[] sha1, int keyCount, byte[]... params);
}
