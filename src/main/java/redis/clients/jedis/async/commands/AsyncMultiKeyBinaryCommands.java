package redis.clients.jedis.async.commands;

import redis.clients.jedis.BitOP;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.ZParams;
import redis.clients.jedis.async.callback.AsyncResponseCallback;

import java.util.List;
import java.util.Set;

public interface AsyncMultiKeyBinaryCommands {
  // Keys section
  void del(final AsyncResponseCallback<Long> callback, final byte[]... keys);

  void keys(final AsyncResponseCallback<Set<byte[]>> callback, final byte[] pattern);

  void randomBinaryKey(final AsyncResponseCallback<byte[]> callback);

  void rename(final AsyncResponseCallback<String> callback, final byte[] oldkey, final byte[] newkey);

  void renamenx(final AsyncResponseCallback<Long> callback, final byte[] oldkey, final byte[] newkey);

  void sort(final AsyncResponseCallback<Long> callback, final byte[] key,
      final SortingParams sortingParameters, final byte[] dstkey);

  void sort(final AsyncResponseCallback<Long> callback, final byte[] key, final byte[] dstkey);

  // string section

  void bitop(final AsyncResponseCallback<Long> callback, final BitOP op, final byte[] destKey,
      final byte[]... srcKeys);

  void mget(final AsyncResponseCallback<List<byte[]>> callback, final byte[]... keys);

  void mset(final AsyncResponseCallback<String> callback, final byte[]... keysvalues);

  void msetnx(final AsyncResponseCallback<Long> callback, final byte[]... keysvalues);

  // list section

  void rpoplpush(final AsyncResponseCallback<byte[]> callback, final byte[] srckey,
      final byte[] dstkey);

  // set section

  void sdiff(final AsyncResponseCallback<Set<byte[]>> callback, final byte[]... keys);

  void sdiffstore(final AsyncResponseCallback<Long> callback, final byte[] dstkey,
      final byte[]... keys);

  void sinter(final AsyncResponseCallback<Set<byte[]>> callback, final byte[]... keys);

  void sinterstore(final AsyncResponseCallback<Long> callback, final byte[] dstkey,
      final byte[]... keys);

  void smove(final AsyncResponseCallback<Long> callback, final byte[] srckey, final byte[] dstkey,
      final byte[] member);

  void sunion(final AsyncResponseCallback<Set<byte[]>> callback, final byte[]... keys);

  void sunionstore(final AsyncResponseCallback<Long> callback, final byte[] dstkey,
      final byte[]... keys);

  // sorted set section

  void zinterstore(final AsyncResponseCallback<Long> callback, final byte[] dstkey,
      final byte[]... sets);

  void zinterstore(final AsyncResponseCallback<Long> callback, final byte[] dstkey,
      final ZParams params, final byte[]... sets);

  void zunionstore(final AsyncResponseCallback<Long> callback, final byte[] dstkey,
      final byte[]... sets);

  void zunionstore(final AsyncResponseCallback<Long> callback, final byte[] dstkey,
      final ZParams params, final byte[]... sets);

  // pub/sub section

  void publish(final AsyncResponseCallback<Long> callback, final byte[] channel,
      final byte[] message);

}
