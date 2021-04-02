package redis.clients.jedis.commands;

import redis.clients.jedis.BitOP;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.Response;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.ZParams;
import redis.clients.jedis.args.*;
import redis.clients.jedis.params.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Multikey related commands (these are split out because they are non-shardable)
 */
public interface MultiKeyBinaryRedisPipeline {
  Response<Boolean> copy(byte[] srcKey, byte[] dstKey, int db, boolean replace);

  Response<Boolean> copy(byte[] srcKey, byte[] dstKey, boolean replace);

  Response<Long> del(byte[]... keys);

  Response<Long> unlink(byte[]... keys);

  Response<Long> exists(byte[]... keys);

  Response<byte[]> lmove(byte[] srcKey, byte[] dstKey, ListDirection from, ListDirection to);

  Response<byte[]> blmove(byte[] srcKey, byte[] dstKey, ListDirection from, ListDirection to,
      double timeout);

  Response<List<byte[]>> blpop(byte[]... args);

  Response<List<byte[]>> blpop(double timeout, byte[]... args);

  Response<List<byte[]>> brpop(byte[]... args);

  Response<List<byte[]>> brpop(double timeout, byte[]... args);

  Response<List<byte[]>> bzpopmax(double timeout, byte[]... keys);

  Response<List<byte[]>> bzpopmin(double timeout, byte[]... keys);

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

  Response<String> unwatch();

  Response<Set<byte[]>> zdiff(byte[]... keys);

  Response<Set<Tuple>> zdiffWithScores(byte[]... keys);

  Response<Long> zdiffStore(byte[] dstkey, byte[]... keys);

  Response<Set<byte[]>> zinter(ZParams params, byte[]... keys);

  Response<Set<Tuple>> zinterWithScores(ZParams params, byte[]... keys);

  Response<Long> zinterstore(byte[] dstkey, byte[]... sets);

  Response<Long> zinterstore(byte[] dstkey, ZParams params, byte[]... sets);

  Response<Set<byte[]>> zunion(ZParams params, byte[]... keys);

  Response<Set<Tuple>> zunionWithScores(ZParams params, byte[]... keys);

  Response<Long> zunionstore(byte[] dstkey, byte[]... sets);

  Response<Long> zunionstore(byte[] dstkey, ZParams params, byte[]... sets);

  Response<byte[]> brpoplpush(byte[] source, byte[] destination, int timeout);

  Response<Long> publish(byte[] channel, byte[] message);

  Response<byte[]> randomKeyBinary();

  Response<Long> bitop(BitOP op, byte[] destKey, byte[]... srcKeys);

  Response<String> pfmerge(byte[] destkey, byte[]... sourcekeys);

  Response<Long> pfcount(byte[]... keys);

  Response<Long> touch(byte[]... keys);

  Response<String> migrate(String host, int port, int destinationDB, int timeout,
      MigrateParams params, byte[]... keys);

  Response<Long> georadiusStore(byte[] key, double longitude, double latitude, double radius,
      GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam);

  Response<Long> georadiusByMemberStore(byte[] key, byte[] member, double radius, GeoUnit unit,
      GeoRadiusParam param, GeoRadiusStoreParam storeParam);

  /**
   * @deprecated Use {@link #xread(redis.clients.jedis.params.XReadParams, java.util.Map.Entry...)}.
   */
  @Deprecated
  Response<List<byte[]>> xread(int count, long block, Map<byte[], byte[]> streams);

  Response<List<byte[]>> xread(XReadParams xReadParams, Map.Entry<byte[], byte[]>... streams);

  /**
   * @deprecated Use
   *             {@link #xreadGroup(byte..., byte..., redis.clients.jedis.params.XReadGroupParams, java.util.Map.Entry...)}.
   */
  @Deprecated
  Response<List<byte[]>> xreadGroup(byte[] groupname, byte[] consumer, int count, long block,
      boolean noAck, Map<byte[], byte[]> streams);

  Response<List<byte[]>> xreadGroup(byte[] groupname, byte[] consumer,
      XReadGroupParams xReadGroupParams, Map.Entry<byte[], byte[]>... streams);
}
