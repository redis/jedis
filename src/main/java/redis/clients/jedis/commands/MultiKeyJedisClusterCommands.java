package redis.clients.jedis.commands;

import redis.clients.jedis.BitOP;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.StreamEntry;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.ZParams;
import redis.clients.jedis.args.*;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.GeoRadiusStoreParam;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.resps.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MultiKeyJedisClusterCommands {
  Boolean copy(String srcKey, String dstKey, boolean replace);

  Long del(String... keys);

  Long unlink(String... keys);

  Long exists(String... keys);

  String lmove(String srcKey, String dstKey, ListDirection from, ListDirection to);

  String blmove(String srcKey, String dstKey, ListDirection from, ListDirection to, double timeout);

  List<String> blpop(int timeout, String... keys);

  KeyedListElement blpop(double timeout, String... keys);

  List<String> brpop(int timeout, String... keys);

  KeyedListElement brpop(double timeout, String... keys);

  KeyedZSetElement bzpopmax(double timeout, String... keys);

  KeyedZSetElement bzpopmin(double timeout, String... keys);

  List<String> mget(String... keys);

  String mset(String... keysvalues);

  Long msetnx(String... keysvalues);

  String rename(String oldkey, String newkey);

  Long renamenx(String oldkey, String newkey);

  String rpoplpush(String srckey, String dstkey);

  Set<String> sdiff(String... keys);

  Long sdiffstore(String dstkey, String... keys);

  Set<String> sinter(String... keys);

  Long sinterstore(String dstkey, String... keys);

  Long smove(String srckey, String dstkey, String member);

  Long sort(String key, SortingParams sortingParameters, String dstkey);

  Long sort(String key, String dstkey);

  Set<String> sunion(String... keys);

  Long sunionstore(String dstkey, String... keys);

  Set<String> zdiff(String... keys);

  Set<Tuple> zdiffWithScores(String... keys);

  Long zdiffStore(String dstkey, String... keys);

  Set<String> zinter(ZParams params, String... keys);

  Set<Tuple> zinterWithScores(ZParams params, String... keys);

  Long zinterstore(String dstkey, String... sets);

  Long zinterstore(String dstkey, ZParams params, String... sets);

  Set<String> zunion(ZParams params, String... keys);

  Set<Tuple> zunionWithScores(ZParams params, String... keys);

  Long zunionstore(String dstkey, String... sets);

  Long zunionstore(String dstkey, ZParams params, String... sets);

  String brpoplpush(String source, String destination, int timeout);

  Long publish(String channel, String message);

  void subscribe(JedisPubSub jedisPubSub, String... channels);

  void psubscribe(JedisPubSub jedisPubSub, String... patterns);

  Long bitop(BitOP op, String destKey, String... srcKeys);

  String pfmerge(String destkey, String... sourcekeys);

  long pfcount(String... keys);

  Long touch(String... keys);

  ScanResult<String> scan(String cursor, ScanParams params);

  Set<String> keys(String pattern);

  Long georadiusStore(String key, double longitude, double latitude, double radius, GeoUnit unit,
      GeoRadiusParam param, GeoRadiusStoreParam storeParam);

  Long georadiusByMemberStore(String key, String member, double radius, GeoUnit unit,
      GeoRadiusParam param, GeoRadiusStoreParam storeParam);

  /**
   * XREAD [COUNT count] [BLOCK milliseconds] STREAMS key [key ...] ID [ID ...]
   * @param count
   * @param block
   * @param streams
   * @return
   * @deprecated This method will be removed due to bug regarding {@code block} param. Use
   *             {@link #xread(redis.clients.jedis.params.XReadParams, java.util.Map)}.
   */
  @Deprecated
  List<Map.Entry<String, List<StreamEntry>>> xread(int count, long block,
      Map.Entry<String, StreamEntryID>... streams);

  List<Map.Entry<String, List<StreamEntry>>> xread(XReadParams xReadParams,
      Map<String, StreamEntryID> streams);

  /**
   * XREAD [COUNT count] [BLOCK milliseconds] STREAMS key [key ...] ID [ID ...]
   * @param groupname
   * @param consumer
   * @param count
   * @param block
   * @param noAck
   * @param streams
   * @return
   * @deprecated This method will be removed due to bug regarding {@code block} param. Use
   *             {@link #xreadGroup(java.lang.String, java.lang.String, redis.clients.jedis.params.XReadGroupParams, java.util.Map)}.
   */
  @Deprecated
  List<Map.Entry<String, List<StreamEntry>>> xreadGroup(String groupname, String consumer,
      int count, long block, boolean noAck, Map.Entry<String, StreamEntryID>... streams);

  List<Map.Entry<String, List<StreamEntry>>> xreadGroup(String groupname, String consumer,
      XReadGroupParams xReadGroupParams, Map<String, StreamEntryID> streams);

}
