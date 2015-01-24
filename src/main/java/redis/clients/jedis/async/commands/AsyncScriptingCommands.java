package redis.clients.jedis.async.commands;

import redis.clients.jedis.async.callback.AsyncResponseCallback;

import java.util.List;

public interface AsyncScriptingCommands {
  void eval(final AsyncResponseCallback<Object> callback, String script, int keyCount,
      String... params);

  void eval(final AsyncResponseCallback<Object> callback, String script, List<String> keys,
      List<String> args);

  void eval(final AsyncResponseCallback<Object> callback, String script);

  void evalsha(final AsyncResponseCallback<Object> callback, String sha1);

  void evalsha(final AsyncResponseCallback<Object> callback, String sha1, List<String> keys,
      List<String> args);

  void evalsha(final AsyncResponseCallback<Object> callback, String sha1, int keyCount,
      String... params);

  void scriptExists(final AsyncResponseCallback<List<Boolean>> callback, String... sha1);

  void scriptLoad(final AsyncResponseCallback<String> callback, String script);
}
