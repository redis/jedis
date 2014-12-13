package redis.clients.jedis.async.commands;

import redis.clients.jedis.*;
import redis.clients.jedis.async.callback.AsyncResponseCallback;

import java.util.List;
import java.util.Set;

public interface AsyncMultiKeyCommands {
  // keys section
  void del(AsyncResponseCallback<Long> callback, String... keys);

  void keys(AsyncResponseCallback<Set<String>> callback, String pattern);

  void randomKey(AsyncResponseCallback<String> callback);

  void rename(AsyncResponseCallback<String> callback, String oldkey, String newkey);

  void renamenx(AsyncResponseCallback<Long> callback, String oldkey, String newkey);

  void sort(AsyncResponseCallback<Long> callback, String key, String dstkey);

  void sort(AsyncResponseCallback<Long> callback, String key, SortingParams sortingParameters,
      String dstkey);

  // string section

  void bitop(AsyncResponseCallback<Long> callback, BitOP op, final String destKey,
      String... srcKeys);

  void mget(AsyncResponseCallback<List<String>> callback, String... keys);

  void mset(AsyncResponseCallback<String> callback, String... keysvalues);

  void msetnx(AsyncResponseCallback<Long> callback, String... keysvalues);

  // list section

  void rpoplpush(AsyncResponseCallback<String> callback, String srckey, String dstkey);

  // set section

  void sdiff(AsyncResponseCallback<Set<String>> callback, String... keys);

  void sdiffstore(AsyncResponseCallback<Long> callback, String dstkey, String... keys);

  void sinter(AsyncResponseCallback<Set<String>> callback, String... keys);

  void sinterstore(AsyncResponseCallback<Long> callback, String dstkey, String... keys);

  void smove(AsyncResponseCallback<Long> callback, String srckey, String dstkey, String member);

  void sunion(AsyncResponseCallback<Set<String>> callback, String... keys);

  void sunionstore(AsyncResponseCallback<Long> callback, String dstkey, String... keys);

  // sorted set section

  void zinterstore(AsyncResponseCallback<Long> callback, String dstkey, String... sets);

  void zinterstore(AsyncResponseCallback<Long> callback, String dstkey, ZParams params,
      String... sets);

  void zunionstore(AsyncResponseCallback<Long> callback, String dstkey, String... sets);

  void zunionstore(AsyncResponseCallback<Long> callback, String dstkey, ZParams params,
      String... sets);

  // pub/sub section

  void publish(AsyncResponseCallback<Long> callback, String channel, String message);

}
