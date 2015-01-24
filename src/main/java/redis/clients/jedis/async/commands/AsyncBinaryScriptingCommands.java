package redis.clients.jedis.async.commands;

import redis.clients.jedis.async.callback.AsyncResponseCallback;

import java.util.List;

public interface AsyncBinaryScriptingCommands {

  void eval(final AsyncResponseCallback<Object> callback, byte[] script, byte[] keyCount,
      byte[]... params);

  void eval(final AsyncResponseCallback<Object> callback, byte[] script, int keyCount,
      byte[]... params);

  void eval(final AsyncResponseCallback<Object> callback, byte[] script, List<byte[]> keys,
      List<byte[]> args);

  void eval(final AsyncResponseCallback<Object> callback, byte[] script);

  void evalsha(final AsyncResponseCallback<Object> callback, byte[] sha1);

  void evalsha(final AsyncResponseCallback<Object> callback, byte[] sha1, List<byte[]> keys,
      List<byte[]> args);

  void evalsha(final AsyncResponseCallback<Object> callback, byte[] sha1, int keyCount,
      byte[]... params);

  void scriptExists(final AsyncResponseCallback<List<Boolean>> callback, byte[]... sha1);

  void scriptLoad(final AsyncResponseCallback<byte[]> callback, byte[] script);

  void scriptFlush(final AsyncResponseCallback<String> callback);

  void scriptKill(final AsyncResponseCallback<String> callback);
}
