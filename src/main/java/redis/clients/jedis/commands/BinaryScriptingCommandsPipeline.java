package redis.clients.jedis.commands;

import java.util.List;
import redis.clients.jedis.Response;

/**
 * WARNING: This interface will be removed in upcoming MINOR release.
 * @deprecated This interface will be removed in upcoming MINOR release.
 */
@Deprecated
public interface BinaryScriptingCommandsPipeline {

  Response<Object> eval(byte[] script, int keyCount, byte[]... params);

  Response<Object> eval(byte[] script, List<byte[]> keys, List<byte[]> args);

  Response<Object> eval(byte[] script);

  Response<Object> evalsha(byte[] sha1);

  Response<Object> evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args);

  Response<Object> evalsha(byte[] sha1, int keyCount, byte[]... params);
}
