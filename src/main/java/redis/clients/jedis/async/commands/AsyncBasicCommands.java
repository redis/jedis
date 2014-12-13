package redis.clients.jedis.async.commands;

import redis.clients.jedis.DebugParams;
import redis.clients.jedis.async.callback.AsyncResponseCallback;

public interface AsyncBasicCommands {
  // FIXME : how we can apply db select with reconnecting?
  // connection section

  void echo(AsyncResponseCallback<String> callback, String string);

  void ping(final AsyncResponseCallback<String> callback);

  // server section

  void bgrewriteaof(final AsyncResponseCallback<String> callback);

  void bgsave(final AsyncResponseCallback<String> callback);

  void configResetStat(final AsyncResponseCallback<String> callback);

  void dbSize(final AsyncResponseCallback<Long> callback);

  void debug(final AsyncResponseCallback<String> callback, final DebugParams params);

  void flushAll(final AsyncResponseCallback<String> callback);

  void flushDB(final AsyncResponseCallback<String> callback);

  void info(final AsyncResponseCallback<String> callback);

  void info(final AsyncResponseCallback<String> callback, final String section);

  void lastsave(final AsyncResponseCallback<Long> callback);

  void save(final AsyncResponseCallback<String> callback);

  void shutdown(final AsyncResponseCallback<String> callback);

  void slaveof(final AsyncResponseCallback<String> callback, final String host, final int port);

  void slaveofNoOne(final AsyncResponseCallback<String> callback);

  // undocumented

  void waitReplicas(final AsyncResponseCallback<Long> callback, final int replicas,
      final long timeout);

}
