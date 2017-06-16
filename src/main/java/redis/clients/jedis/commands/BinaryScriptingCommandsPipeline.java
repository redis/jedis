package redis.clients.jedis.commands;

import redis.clients.jedis.Response;

import java.util.List;

public interface BinaryScriptingCommandsPipeline {

  Response<Object> eval(byte[] script, byte[] keyCount, byte[]... params);

  Response<Object> eval(byte[] script, int keyCount, byte[]... params);

  Response<Object> eval(byte[] script, List<byte[]> keys, List<byte[]> args);

  Response<Object> eval(byte[] script);

  Response<Object> evalsha(byte[] sha1);

  Response<Object> evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args);

  Response<Object> evalsha(byte[] sha1, int keyCount, byte[]... params);
}
