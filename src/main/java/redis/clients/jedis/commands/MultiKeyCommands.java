package redis.clients.jedis.commands;

import redis.clients.jedis.params.StrAlgoLCSParams;
import redis.clients.jedis.resps.KeyedZSetElement;
import redis.clients.jedis.args.*;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.GeoRadiusStoreParam;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.resps.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.SortingParams;
import redis.clients.jedis.params.ZParams;
import redis.clients.jedis.stream.StreamEntry;
import redis.clients.jedis.stream.StreamEntryID;

//Legacy
public interface MultiKeyCommands {

  boolean copy(String srcKey, String dstKey, int db, boolean replace);

  boolean copy(String srcKey, String dstKey, boolean replace);

  long del(String... keys);

  long unlink(String... keys);

  long exists(String... keys);

  String lmove(String srcKey, String dstKey, ListDirection from, ListDirection to);

  String blmove(String srcKey, String dstKey, ListDirection from, ListDirection to, double timeout);

  List<String> blpop(int timeout, String... keys);

  KeyedListElement blpop(double timeout, String... keys);

  List<String> brpop(int timeout, String... keys);

  KeyedListElement brpop(double timeout, String... keys);

  List<String> blpop(String... args);

  List<String> brpop(String... args);

  KeyedZSetElement bzpopmax(double timeout, String... keys);

  KeyedZSetElement bzpopmin(double timeout, String... keys);

  Set<String> keys(String pattern);

  List<String> mget(String... keys);

  String mset(String... keysvalues);

  long msetnx(String... keysvalues);

  String rename(String oldkey, String newkey);

  long renamenx(String oldkey, String newkey);

  String rpoplpush(String srckey, String dstkey);

  Set<String> sdiff(String... keys);

  long sdiffstore(String dstkey, String... keys);

  Set<String> sinter(String... keys);

  long sinterstore(String dstkey, String... keys);

  long smove(String srckey, String dstkey, String member);

  long sort(String key, SortingParams sortingParameters, String dstkey);

  long sort(String key, String dstkey);

  Set<String> sunion(String... keys);

  long sunionstore(String dstkey, String... keys);

  String watch(String... keys);

  String unwatch();

  Set<String> zdiff(String... keys);

  Set<Tuple> zdiffWithScores(String... keys);

  long zdiffStore(String dstkey, String... keys);

  long zinterstore(String dstkey, String... sets);

  long zinterstore(String dstkey, ZParams params, String... sets);

  Set<String> zinter(ZParams params, String... keys);

  Set<Tuple> zinterWithScores(ZParams params, String... keys);

  Set<String> zunion(ZParams params, String... keys);

  Set<Tuple> zunionWithScores(ZParams params, String... keys);

  long zunionstore(String dstkey, String... sets);

  long zunionstore(String dstkey, ZParams params, String... sets);

  String brpoplpush(String source, String destination, int timeout);

  Long publish(String channel, String message);

  String randomKey();

  long bitop(BitOP op, String destKey, String... srcKeys);

  ScanResult<String> scan(String cursor);

  ScanResult<String> scan(String cursor, ScanParams params);

  ScanResult<String> scan(String cursor, ScanParams params, String type);

  String pfmerge(String destkey, String... sourcekeys);

  long pfcount(String... keys);

  long touch(String... keys);

  /**
   * XREAD [COUNT count] [BLOCK milliseconds] STREAMS key [key ...] ID [ID ...]
   *
   * @param count
   * @param block
   * @param streams
   * @return
   * @deprecated This method will be removed due to bug regarding {@code block} param. Use
   * {@link MultiKeyCommands#xread(redis.clients.jedis.params.XReadParams, java.util.Map)}.
   */
  @Deprecated
  List<Map.Entry<String, List<StreamEntry>>> xread(int count, long block,
      Map.Entry<String, StreamEntryID>... streams);

  List<Map.Entry<String, List<StreamEntry>>> xread(XReadParams xReadParams,
      Map<String, StreamEntryID> streams);

  /**
   * XREAD [COUNT count] [BLOCK milliseconds] STREAMS key [key ...] ID [ID ...]
   *
   * @param groupname
   * @param consumer
   * @param count
   * @param block
   * @param noAck
   * @param streams
   * @return
   * @deprecated This method will be removed due to bug regarding {@code block} param. Use
   * {@link #xreadGroup(java.lang.String, java.lang.String, redis.clients.jedis.params.XReadGroupParams, java.util.Map)}.
   */
  @Deprecated
  List<Map.Entry<String, List<StreamEntry>>> xreadGroup(String groupname, String consumer,
      int count, long block, boolean noAck, Map.Entry<String, StreamEntryID>... streams);

  List<Map.Entry<String, List<StreamEntry>>> xreadGroup(String groupname, String consumer,
      XReadGroupParams xReadGroupParams, Map<String, StreamEntryID> streams);

  long georadiusStore(String key, double longitude, double latitude, double radius, GeoUnit unit,
      GeoRadiusParam param, GeoRadiusStoreParam storeParam);

  long georadiusByMemberStore(String key, String member, double radius, GeoUnit unit,
      GeoRadiusParam param, GeoRadiusStoreParam storeParam);

  LCSMatchResult strAlgoLCSKeys(final String keyA, final String keyB, final StrAlgoLCSParams params);
}
