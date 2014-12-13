package redis.clients.jedis.async.commands;

import redis.clients.jedis.async.callback.AsyncResponseCallback;

import java.util.List;

public interface AsyncAdvancedBinaryJedisCommands {
  // keys section

  void objectRefcount(final AsyncResponseCallback<Long> callback, final byte[] key);

  void objectEncoding(final AsyncResponseCallback<byte[]> callback, final byte[] key);

  void objectIdletime(final AsyncResponseCallback<Long> callback, final byte[] key);

  // server section

  void configGet(final AsyncResponseCallback<List<byte[]>> callback, final byte[] pattern);

  void configSet(final AsyncResponseCallback<String> callback, final byte[] parameter,
      final byte[] value);

  void slowlogReset(final AsyncResponseCallback<String> callback);

  void slowlogLen(final AsyncResponseCallback<Long> callback);

  void slowlogGetBinary(final AsyncResponseCallback<List<byte[]>> callback);

  void slowlogGetBinary(final AsyncResponseCallback<List<byte[]>> callback, final long entries);

}
