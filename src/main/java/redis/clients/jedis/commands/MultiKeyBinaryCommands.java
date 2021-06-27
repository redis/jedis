package redis.clients.jedis.commands;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.BitOP;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.ZParams;
import redis.clients.jedis.args.*;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.GeoRadiusStoreParam;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.params.XReadParams;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public interface MultiKeyBinaryCommands {

  boolean copy(byte[] srcKey, byte[] dstKey, int db, boolean replace);

  boolean copy(byte[] srcKey, byte[] dstKey, boolean replace);

  long del(byte[]... keys);

  long unlink(byte[]... keys);

  long exists(byte[]... keys);

  byte[] lmove(byte[] srcKey, byte[] dstKey, ListDirection from, ListDirection to);

  byte[] blmove(byte[] srcKey, byte[] dstKey, ListDirection from, ListDirection to, double timeout);

  List<byte[]> blpop(int timeout, byte[]... keys);

  List<byte[]> blpop(double timeout, byte[]... keys);

  List<byte[]> brpop(int timeout, byte[]... keys);

  List<byte[]> brpop(double timeout, byte[]... keys);

  List<byte[]> blpop(byte[]... args);

  List<byte[]> brpop(byte[]... args);

  List<byte[]> bzpopmax(double timeout, byte[]... keys);

  List<byte[]> bzpopmin(double timeout, byte[]... keys);

  Set<byte[]> keys(byte[] pattern);

  List<byte[]> mget(byte[]... keys);

  String mset(byte[]... keysvalues);

  long msetnx(byte[]... keysvalues);

  String rename(byte[] oldkey, byte[] newkey);

  long renamenx(byte[] oldkey, byte[] newkey);

  byte[] rpoplpush(byte[] srckey, byte[] dstkey);

  Set<byte[]> sdiff(byte[]... keys);

  long sdiffstore(byte[] dstkey, byte[]... keys);

  Set<byte[]> sinter(byte[]... keys);

  long sinterstore(byte[] dstkey, byte[]... keys);

  long smove(byte[] srckey, byte[] dstkey, byte[] member);

  long sort(byte[] key, SortingParams sortingParameters, byte[] dstkey);

  long sort(byte[] key, byte[] dstkey);

  Set<byte[]> sunion(byte[]... keys);

  long sunionstore(byte[] dstkey, byte[]... keys);

  String watch(byte[]... keys);

  String unwatch();

  Set<byte[]> zdiff(byte[]... keys);

  Set<Tuple> zdiffWithScores(byte[]... keys);

  long zdiffStore(byte[] dstkey, byte[]... keys);

  Set<byte[]> zinter(ZParams params, byte[]... keys);

  Set<Tuple> zinterWithScores(ZParams params, byte[]... keys);

  long zinterstore(byte[] dstkey, byte[]... sets);

  long zinterstore(byte[] dstkey, ZParams params, byte[]... sets);

  Set<byte[]> zunion(ZParams params, byte[]... keys);

  Set<Tuple> zunionWithScores(ZParams params, byte[]... keys);

  long zunionstore(byte[] dstkey, byte[]... sets);

  long zunionstore(byte[] dstkey, ZParams params, byte[]... sets);

  byte[] brpoplpush(byte[] source, byte[] destination, int timeout);

  Long publish(byte[] channel, byte[] message);

  void subscribe(BinaryJedisPubSub jedisPubSub, byte[]... channels);

  void psubscribe(BinaryJedisPubSub jedisPubSub, byte[]... patterns);

  byte[] randomBinaryKey();

  long bitop(BitOP op, byte[] destKey, byte[]... srcKeys);

  String pfmerge(byte[] destkey, byte[]... sourcekeys);

  long pfcount(byte[]... keys);

  long touch(byte[]... keys);

  ScanResult<byte[]> scan(byte[] cursor);

  ScanResult<byte[]> scan(byte[] cursor, ScanParams params);

  ScanResult<byte[]> scan(byte[] cursor, ScanParams params, byte[] type);

  /**
   * @deprecated This method will be removed due to bug regarding {@code block} param. Use
   * {@link #xread(redis.clients.jedis.params.XReadParams, java.util.Map.Entry...)}.
   */
  @Deprecated
  List<byte[]> xread(int count, long block, Map<byte[], byte[]> streams);

  List<byte[]> xread(XReadParams xReadParams, Entry<byte[], byte[]>... streams);

  /**
   * @deprecated This method will be removed due to bug regarding {@code block} param. Use
   * {@link #xreadGroup(byte..., byte..., redis.clients.jedis.params.XReadGroupParams, java.util.Map.Entry...)}.
   */
  @Deprecated
  List<byte[]> xreadGroup(byte[] groupname, byte[] consumer, int count, long block, boolean noAck,
      Map<byte[], byte[]> streams);

  List<byte[]> xreadGroup(byte[] groupname, byte[] consumer, XReadGroupParams xReadGroupParams,
      Entry<byte[], byte[]>... streams);

  long georadiusStore(byte[] key, double longitude, double latitude, double radius, GeoUnit unit,
      GeoRadiusParam param, GeoRadiusStoreParam storeParam);

  long georadiusByMemberStore(byte[] key, byte[] member, double radius, GeoUnit unit,
      GeoRadiusParam param, GeoRadiusStoreParam storeParam);
}
