package redis.clients.jedis.async.commands;

import redis.clients.jedis.async.callback.AsyncResponseCallback;
import redis.clients.util.Slowlog;

import java.util.List;

public interface AsyncAdvancedJedisCommands {
  // keys section

  void objectRefcount(AsyncResponseCallback<Long> callback, String string);

  void objectEncoding(AsyncResponseCallback<String> callback, String string);

  void objectIdletime(AsyncResponseCallback<Long> callback, String string);

  // server section

  void configGet(AsyncResponseCallback<List<String>> callback, String pattern);

  void configSet(AsyncResponseCallback<String> callback, String parameter, String value);

  void slowlogReset(AsyncResponseCallback<String> callback);

  void slowlogLen(AsyncResponseCallback<Long> callback);

  void slowlogGet(AsyncResponseCallback<List<Slowlog>> callback);

  void slowlogGet(AsyncResponseCallback<List<Slowlog>> callback, long entries);

}
