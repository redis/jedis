package redis.clients.jedis.commands;

import redis.clients.jedis.BitOP;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.Response;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.StreamEntry;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.ZParams;
import redis.clients.jedis.args.*;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Multikey related commands (these are split out because they are non-shardable)
 */
public interface MultiKeyCommandsPipeline {
  Response<Boolean> copy(String srcKey, String dstKey, int db, boolean replace);

  Response<Boolean> copy(String srcKey, String dstKey, boolean replace);

  Response<Long> del(String... keys);

  Response<Long> unlink(String... keys);

  Response<Long> exists(String... keys);

  Response<String> lmove(String srcKey, String dstKey, ListDirection from, ListDirection to);

  Response<String> blmove(String srcKey, String dstKey, ListDirection from, ListDirection to,
      double timeout);

  Response<List<String>> blpop(String... args);

  Response<List<String>> blpop(int timeout, String... args);

  Response<KeyedListElement> blpop(double timeout, String... args);

  Response<List<String>> brpop(String... args);

  Response<List<String>> brpop(int timeout, String... args);

  Response<KeyedListElement> brpop(double timeout, String... args);

  Response<KeyedZSetElement> bzpopmax(double timeout, String... keys);

  Response<KeyedZSetElement> bzpopmin(double timeout, String... keys);

  Response<Set<String>> keys(String pattern);

  Response<List<String>> mget(String... keys);

  Response<String> mset(String... keysvalues);

  Response<Long> msetnx(String... keysvalues);

  Response<String> rename(String oldkey, String newkey);

  Response<Long> renamenx(String oldkey, String newkey);

  Response<String> rpoplpush(String srckey, String dstkey);

  Response<Set<String>> sdiff(String... keys);

  Response<Long> sdiffstore(String dstkey, String... keys);

  Response<Set<String>> sinter(String... keys);

  Response<Long> sinterstore(String dstkey, String... keys);

  Response<Long> smove(String srckey, String dstkey, String member);

  Response<Long> sort(String key, SortingParams sortingParameters, String dstkey);

  Response<Long> sort(String key, String dstkey);

  Response<Set<String>> sunion(String... keys);

  Response<Long> sunionstore(String dstkey, String... keys);

  Response<String> watch(String... keys);

  Response<String> unwatch();

  Response<Set<String>> zdiff(String... keys);

  Response<Set<Tuple>> zdiffWithScores(String... keys);

  Response<Long> zdiffStore(String dstkey, String... keys);

  Response<Set<String>> zinter(ZParams params, String... keys);

  Response<Set<Tuple>> zinterWithScores(ZParams params, String... keys);

  Response<Long> zinterstore(String dstkey, String... sets);

  Response<Long> zinterstore(String dstkey, ZParams params, String... sets);

  Response<Set<String>> zunion(ZParams params, String... keys);

  Response<Set<Tuple>> zunionWithScores(ZParams params, String... keys);

  Response<Long> zunionstore(String dstkey, String... sets);

  Response<Long> zunionstore(String dstkey, ZParams params, String... sets);

  Response<String> brpoplpush(String source, String destination, int timeout);

  Response<Long> publish(String channel, String message);

  Response<String> randomKey();

  Response<Long> bitop(BitOP op, String destKey, String... srcKeys);

  Response<String> pfmerge(String destkey, String... sourcekeys);

  Response<Long> pfcount(String... keys);

  Response<Long> touch(String... keys);

  Response<String> migrate(String host, int port, int destinationDB, int timeout,
      MigrateParams params, String... keys);

  Response<Long> georadiusStore(String key, double longitude, double latitude, double radius,
      GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam);

  Response<Long> georadiusByMemberStore(String key, String member, double radius, GeoUnit unit,
      GeoRadiusParam param, GeoRadiusStoreParam storeParam);

  /**
   * @deprecated Use {@link #xread(redis.clients.jedis.params.XReadParams, java.util.Map)}.
   */
  @Deprecated
  Response<List<Map.Entry<String, List<StreamEntry>>>> xread(int count, long block,
      Map.Entry<String, StreamEntryID>... streams);

  Response<List<Map.Entry<String, List<StreamEntry>>>> xread(XReadParams xReadParams,
      Map<String, StreamEntryID> streams);

  /**
   * @deprecated Use
   *             {@link #xreadGroup(java.lang.String, java.lang.String, redis.clients.jedis.params.XReadGroupParams, java.util.Map)}.
   */
  @Deprecated
  Response<List<Map.Entry<String, List<StreamEntry>>>> xreadGroup(String groupname, String consumer,
      int count, long block, boolean noAck, Map.Entry<String, StreamEntryID>... streams);

  Response<List<Map.Entry<String, List<StreamEntry>>>> xreadGroup(String groupname, String consumer,
      XReadGroupParams xReadGroupParams, Map<String, StreamEntryID> streams);
}
