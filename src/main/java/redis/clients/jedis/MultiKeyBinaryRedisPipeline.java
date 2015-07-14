package redis.clients.jedis;

import java.util.List;
import java.util.Set;

/**
 * Multikey related commands (these are split out because they are non-shardable)
 */
public interface MultiKeyBinaryRedisPipeline {

  Response<Long> del(byte[]... keys);

  Response<Long> exists(byte[]... keys);

  Response<List<byte[]>> blpop(byte[]... args);

  Response<List<byte[]>> brpop(byte[]... args);

  Response<Set<byte[]>> keys(byte[] pattern);

  Response<List<byte[]>> mget(byte[]... keys);

  Response<String> mset(byte[]... keysvalues);

  Response<Long> msetnx(byte[]... keysvalues);

  Response<String> rename(byte[] oldkey, byte[] newkey);

  Response<Long> renamenx(byte[] oldkey, byte[] newkey);

  Response<byte[]> rpoplpush(byte[] srckey, byte[] dstkey);

  Response<Set<byte[]>> sdiff(byte[]... keys);

  Response<Long> sdiffstore(byte[] dstkey, byte[]... keys);

  Response<Set<byte[]>> sinter(byte[]... keys);

  Response<Long> sinterstore(byte[] dstkey, byte[]... keys);

  Response<Long> smove(byte[] srckey, byte[] dstkey, byte[] member);

  Response<Long> sort(byte[] key, SortingParams sortingParameters, byte[] dstkey);

  Response<Long> sort(byte[] key, byte[] dstkey);

  Response<Set<byte[]>> sunion(byte[]... keys);

  Response<Long> sunionstore(byte[] dstkey, byte[]... keys);

  Response<String> watch(byte[]... keys);

  Response<Long> zinterstore(byte[] dstkey, byte[]... sets);

  Response<Long> zinterstore(byte[] dstkey, ZParams params, byte[]... sets);

  Response<Long> zunionstore(byte[] dstkey, byte[]... sets);

  Response<Long> zunionstore(byte[] dstkey, ZParams params, byte[]... sets);

  Response<byte[]> brpoplpush(byte[] source, byte[] destination, int timeout);

  Response<Long> publish(byte[] channel, byte[] message);

  Response<byte[]> randomKeyBinary();

  Response<Long> bitop(BitOP op, final byte[] destKey, byte[]... srcKeys);

  Response<String> pfmerge(final byte[] destkey, final byte[]... sourcekeys);

  Response<Long> pfcount(final byte[]... keys);
}
