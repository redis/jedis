package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.BitPosParams;
import redis.clients.jedis.StreamConsumersInfo;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.GeoRadiusResponse;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.ListPosition;
import redis.clients.jedis.StreamGroupInfo;
import redis.clients.jedis.StreamInfo;
import redis.clients.jedis.StreamPendingEntry;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.StreamEntry;
import redis.clients.jedis.StreamPendingSummary;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.params.GeoAddParams;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.GetExParams;
import redis.clients.jedis.params.RestoreParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.StrAlgoLCSParams;
import redis.clients.jedis.params.XAddParams;
import redis.clients.jedis.params.XAutoClaimParams;
import redis.clients.jedis.params.XClaimParams;
import redis.clients.jedis.params.XPendingParams;
import redis.clients.jedis.params.XTrimParams;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;
import redis.clients.jedis.params.LPosParams;
import redis.clients.jedis.resps.KeyedListElement;
import redis.clients.jedis.resps.LCSMatchResult;

/**
 * Common interface for sharded and non-sharded Jedis
 */
public interface JedisCommands {
  String set(String key, String value);

  String set(String key, String value, SetParams params);

  String get(String key);

  String getDel(String key);

  String getEx(String key, GetExParams params);

  boolean exists(String key);

  long persist(String key);

  String type(String key);

  byte[] dump(String key);

  /**
   * @deprecated Use {@link #restore(java.lang.String, long, byte[])}.
   */
  @Deprecated
  default String restore(String key, int ttl, byte[] serializedValue) {
    return restore(key, (long) ttl, serializedValue);
  }

  String restore(String key, long ttl, byte[] serializedValue);

  /**
   * @deprecated Use {@link #restore(java.lang.String, long, byte[], redis.clients.jedis.params.RestoreParams)}.
   */
  @Deprecated
  default String restoreReplace(String key, int ttl, byte[] serializedValue) {
    return restoreReplace(key, (long) ttl, serializedValue);
  }

  /**
   * @deprecated Use {@link #restore(java.lang.String, long, byte[], redis.clients.jedis.params.RestoreParams)}.
   */
  @Deprecated
  String restoreReplace(String key, long ttl, byte[] serializedValue);

  String restore(String key, long ttl, byte[] serializedValue, RestoreParams params);

  /**
   * @deprecated Use {@link #expire(java.lang.String, long)}.
   */
  @Deprecated
  default Long expire(String key, int seconds) {
    return expire(key, (long) seconds);
  }

  long expire(String key, long seconds);

  long pexpire(String key, long milliseconds);

  long expireAt(String key, long unixTime);

  long pexpireAt(String key, long millisecondsTimestamp);

  long ttl(String key);

  long pttl(String key);

  long touch(String key);

  boolean setbit(String key, long offset, boolean value);

  /**
   * @deprecated Use {@link #setbit(java.lang.String, long, boolean)}.
   */
  @Deprecated
  Boolean setbit(String key, long offset, String value);

  boolean getbit(String key, long offset);

  long setrange(String key, long offset, String value);

  String getrange(String key, long startOffset, long endOffset);

  String getSet(String key, String value);

  long setnx(String key, String value);

  /**
   * @deprecated Use {@link #setex(java.lang.String, long, java.lang.String)}.
   */
  @Deprecated
  default String setex(String key, int seconds, String value) {
    return setex(key, (long) seconds, value);
  }

  String setex(String key, long seconds, String value);

  String psetex(String key, long milliseconds, String value);

  long decrBy(String key, long decrement);

  long decr(String key);

  long incrBy(String key, long increment);

  double incrByFloat(String key, double increment);

  long incr(String key);

  long append(String key, String value);

  String substr(String key, int start, int end);

  long hset(String key, String field, String value);

  long hset(String key, Map<String, String> hash);

  String hget(String key, String field);

  long hsetnx(String key, String field, String value);

  String hmset(String key, Map<String, String> hash);

  List<String> hmget(String key, String... fields);

  long hincrBy(String key, String field, long value);

  double hincrByFloat(String key, String field, double value);

  boolean hexists(String key, String field);

  long hdel(String key, String... field);

  long hlen(String key);

  Set<String> hkeys(String key);

  List<String> hvals(String key);

  Map<String, String> hgetAll(String key);

  String hrandfield(String key);

  List<String> hrandfield(String key, long count);

  Map<String, String> hrandfieldWithValues(String key, long count);

  long rpush(String key, String... string);

  long lpush(String key, String... string);

  long llen(String key);

  List<String> lrange(String key, long start, long stop);

  String ltrim(String key, long start, long stop);

  String lindex(String key, long index);

  String lset(String key, long index, String value);

  long lrem(String key, long count, String value);

  String lpop(String key);

  List<String> lpop(String key, int count);

  Long lpos(String key, String element);

  Long lpos(String key, String element, LPosParams params);

  List<Long> lpos(String key, String element, LPosParams params, long count);

  String rpop(String key);

  List<String> rpop(String key, int count);

  long sadd(String key, String... member);

  Set<String> smembers(String key);

  long srem(String key, String... member);

  String spop(String key);

  Set<String> spop(String key, long count);

  long scard(String key);

  boolean sismember(String key, String member);

  List<Boolean> smismember(String key, String... members);

  String srandmember(String key);

  List<String> srandmember(String key, int count);

  long strlen(String key);

  long zadd(String key, double score, String member);

  long zadd(String key, double score, String member, ZAddParams params);

  long zadd(String key, Map<String, Double> scoreMembers);

  long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params);

  Double zaddIncr(String key, double score, String member, ZAddParams params);

  Set<String> zrange(String key, long start, long stop);

  long zrem(String key, String... members);

  double zincrby(String key, double increment, String member);

  Double zincrby(String key, double increment, String member, ZIncrByParams params);

  Long zrank(String key, String member);

  Long zrevrank(String key, String member);

  Set<String> zrevrange(String key, long start, long stop);

  Set<Tuple> zrangeWithScores(String key, long start, long stop);

  Set<Tuple> zrevrangeWithScores(String key, long start, long stop);

  String zrandmember(String key);

  Set<String> zrandmember(String key, long count);

  Set<Tuple> zrandmemberWithScores(String key, long count);

  long zcard(String key);

  Double zscore(String key, String member);

  List<Double> zmscore(String key, String... members);

  Tuple zpopmax(String key);

  Set<Tuple> zpopmax(String key, int count);

  Tuple zpopmin(String key);

  Set<Tuple> zpopmin(String key, int count);

  List<String> sort(String key);

  List<String> sort(String key, SortingParams sortingParameters);

  long zcount(String key, double min, double max);

  long zcount(String key, String min, String max);

  Set<String> zrangeByScore(String key, double min, double max);

  Set<String> zrangeByScore(String key, String min, String max);

  Set<String> zrevrangeByScore(String key, double max, double min);

  Set<String> zrangeByScore(String key, double min, double max, int offset, int count);

  Set<String> zrevrangeByScore(String key, String max, String min);

  Set<String> zrangeByScore(String key, String min, String max, int offset, int count);

  Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count);

  Set<Tuple> zrangeByScoreWithScores(String key, double min, double max);

  Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min);

  Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count);

  Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count);

  Set<Tuple> zrangeByScoreWithScores(String key, String min, String max);

  Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min);

  Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count);

  Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count);

  Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count);

  long zremrangeByRank(String key, long start, long stop);

  long zremrangeByScore(String key, double min, double max);

  long zremrangeByScore(String key, String min, String max);

  long zlexcount(String key, String min, String max);

  Set<String> zrangeByLex(String key, String min, String max);

  Set<String> zrangeByLex(String key, String min, String max, int offset, int count);

  Set<String> zrevrangeByLex(String key, String max, String min);

  Set<String> zrevrangeByLex(String key, String max, String min, int offset, int count);

  long zremrangeByLex(String key, String min, String max);

  long linsert(String key, ListPosition where, String pivot, String value);

  long lpushx(String key, String... string);

  long rpushx(String key, String... string);

  List<String> blpop(int timeout, String key);

  KeyedListElement blpop(double timeout, String key);

  List<String> brpop(int timeout, String key);

  KeyedListElement brpop(double timeout, String key);

  long del(String key);

  long unlink(String key);

  String echo(String string);

  long bitcount(String key);

  long bitcount(String key, long start, long end);

  long bitpos(String key, boolean value);

  long bitpos(String key, boolean value, BitPosParams params);

  default ScanResult<Map.Entry<String, String>> hscan(String key, String cursor) {
    return hscan(key, cursor, new ScanParams());
  }

  ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params);

  default ScanResult<String> sscan(String key, String cursor) {
    return sscan(key, cursor, new ScanParams());
  }

  ScanResult<String> sscan(String key, String cursor, ScanParams params);

  default ScanResult<Tuple> zscan(String key, String cursor) {
    return zscan(key, cursor, new ScanParams());
  }

  ScanResult<Tuple> zscan(String key, String cursor, ScanParams params);

  long pfadd(String key, String... elements);

  long pfcount(String key);

  // Geo Commands

  long geoadd(String key, double longitude, double latitude, String member);

  long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap);

  long geoadd(String key, GeoAddParams params, Map<String, GeoCoordinate> memberCoordinateMap);

  Double geodist(String key, String member1, String member2);

  Double geodist(String key, String member1, String member2, GeoUnit unit);

  List<String> geohash(String key, String... members);

  List<GeoCoordinate> geopos(String key, String... members);

  List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius,
      GeoUnit unit);

  List<GeoRadiusResponse> georadiusReadonly(String key, double longitude, double latitude,
      double radius, GeoUnit unit);

  List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius,
      GeoUnit unit, GeoRadiusParam param);

  List<GeoRadiusResponse> georadiusReadonly(String key, double longitude, double latitude,
      double radius, GeoUnit unit, GeoRadiusParam param);

  List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit);

  List<GeoRadiusResponse> georadiusByMemberReadonly(String key, String member, double radius, GeoUnit unit);

  List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit,
      GeoRadiusParam param);

  List<GeoRadiusResponse> georadiusByMemberReadonly(String key, String member, double radius,
      GeoUnit unit, GeoRadiusParam param);

  /**
   * Executes BITFIELD Redis command
   * @param key
   * @param arguments
   */
  List<Long> bitfield(String key, String...arguments);

  List<Long> bitfieldReadonly(String key, String...arguments);

  /**
   * Used for HSTRLEN Redis command
   * @param key
   * @param field
   * @return length of the value for key
   */
  long hstrlen(String key, String field);

  /**
   * XADD key ID field string [field string ...]
   *
   * @param key
   * @param id
   * @param hash
   * @return the ID of the added entry
   */
  StreamEntryID xadd(String key, StreamEntryID id, Map<String, String> hash);

  /**
   * XADD key MAXLEN ~ LEN ID field string [field string ...]
   *
   * @param key
   * @param id
   * @param hash
   * @param maxLen
   * @param approximateLength
   */
  StreamEntryID xadd(String key, StreamEntryID id, Map<String, String> hash, long maxLen, boolean approximateLength);

  /**
   * XADD key [NOMKSTREAM] [MAXLEN|MINID [=|~] threshold [LIMIT count]] *|ID field value [field value ...]
   *
   * @param key
   * @param hash
   * @param params
   */
  StreamEntryID xadd(String key, Map<String, String> hash, XAddParams params);

  /**
   * XLEN key
   *
   * @param key
   */
  long xlen(String key);

  /**
   * XRANGE key start end
   *
   * @param key
   * @param start minimum {@link StreamEntryID} for the retrieved range, passing <code>null</code> will indicate minimum ID possible in the stream
   * @param end maximum {@link StreamEntryID} for the retrieved range, passing <code>null</code> will indicate maximum ID possible in the stream
   * @return The entries with IDs matching the specified range.
   */
  List<StreamEntry> xrange(String key, StreamEntryID start, StreamEntryID end);

  /**
   * XRANGE key start end COUNT count
   *
   * @param key
   * @param start minimum {@link StreamEntryID} for the retrieved range, passing <code>null</code> will indicate minimum ID possible in the stream
   * @param end maximum {@link StreamEntryID} for the retrieved range, passing <code>null</code> will indicate maximum ID possible in the stream
   * @param count maximum number of entries returned
   * @return The entries with IDs matching the specified range.
   */
  List<StreamEntry> xrange(String key, StreamEntryID start, StreamEntryID end, int count);

  /**
   * XREVRANGE key end start
   *
   * @param key
   * @param start minimum {@link StreamEntryID} for the retrieved range, passing <code>null</code> will indicate minimum ID possible in the stream
   * @param end maximum {@link StreamEntryID} for the retrieved range, passing <code>null</code> will indicate maximum ID possible in the stream
   * @return the entries with IDs matching the specified range, from the higher ID to the lower ID matching.
   */
  List<StreamEntry> xrevrange(String key, StreamEntryID end, StreamEntryID start);

  /**
   * XREVRANGE key end start COUNT count
   *
   * @param key
   * @param start minimum {@link StreamEntryID} for the retrieved range, passing <code>null</code> will indicate minimum ID possible in the stream
   * @param end maximum {@link StreamEntryID} for the retrieved range, passing <code>null</code> will indicate maximum ID possible in the stream
   * @param count The entries with IDs matching the specified range.
   * @return the entries with IDs matching the specified range, from the higher ID to the lower ID matching.
   */
  List<StreamEntry> xrevrange(String key, StreamEntryID end, StreamEntryID start, int count);

  /**
   * XACK key group ID [ID ...]
   *
   * @param key
   * @param group
   * @param ids
   */
  long xack(String key, String group, StreamEntryID... ids);

  /**
   * XGROUP CREATE <key> <groupname> <id or $>
   *
   * @param key
   * @param groupname
   * @param id
   * @param makeStream
   */
  String xgroupCreate( String key, String groupname, StreamEntryID id, boolean makeStream);

  /**
   * XGROUP SETID <key> <groupname> <id or $>
   *
   * @param key
   * @param groupname
   * @param id
   */
  String xgroupSetID( String key, String groupname, StreamEntryID id);

  /**
   * XGROUP DESTROY <key> <groupname>
   *
   * @param key
   * @param groupname
   */
  long xgroupDestroy(String key, String groupname);

  /**
   * XGROUP DELCONSUMER <key> <groupname> <consumername>
   * @param key
   * @param groupname
   * @param consumername
   */
  long xgroupDelConsumer( String key, String groupname, String consumername);

  /**
   * XPENDING key group
   *
   * @param key
   * @param groupname
   */
  StreamPendingSummary xpending(String key, String groupname);

  /**
   * XPENDING key group [start end count] [consumer]
   *
   * @param key
   * @param groupname
   * @param start
   * @param end
   * @param count
   * @param consumername
   */
  List<StreamPendingEntry> xpending(String key, String groupname, StreamEntryID start,
      StreamEntryID end, int count, String consumername);

  /**
   * XPENDING key group [[IDLE min-idle-time] start end count [consumer]]
   *
   * @param key
   * @param groupname
   * @param params
   */
  List<StreamPendingEntry> xpending(String key, String groupname, XPendingParams params);

  /**
   * XDEL key ID [ID ...]
   * @param key
   * @param ids
   */
  long xdel(String key, StreamEntryID... ids);

  /**
   * XTRIM key MAXLEN [~] count
   * @param key
   * @param maxLen
   * @param approximate
   */
  long xtrim(String key, long maxLen, boolean approximate);

  /**
   * XTRIM key MAXLEN|MINID [=|~] threshold [LIMIT count]
   * @param key
   * @param params
   */
  long xtrim(String key, XTrimParams params);

  /**
   *  XCLAIM <key> <group> <consumer> <min-idle-time> <ID-1> <ID-2>
   *        [IDLE <milliseconds>] [TIME <mstime>] [RETRYCOUNT <count>]
   *        [FORCE] [JUSTID]
   */
  List<StreamEntry> xclaim( String key, String group, String consumername, long minIdleTime,
      long newIdleTime, int retries, boolean force, StreamEntryID... ids);

  /**
   *  XCLAIM <key> <group> <consumer> <min-idle-time> <ID-1> ... <ID-N>
   *        [IDLE <milliseconds>] [TIME <mstime>] [RETRYCOUNT <count>]
   *        [FORCE]
   */
  List<StreamEntry> xclaim(String key, String group, String consumername, long minIdleTime,
      XClaimParams params, StreamEntryID... ids);

  /**
   *  XCLAIM <key> <group> <consumer> <min-idle-time> <ID-1> ... <ID-N>
   *        [IDLE <milliseconds>] [TIME <mstime>] [RETRYCOUNT <count>]
   *        [FORCE] JUSTID
   */
  List<StreamEntryID> xclaimJustId(String key, String group, String consumername, long minIdleTime,
      XClaimParams params, StreamEntryID... ids);

  /**
   * XAUTOCLAIM key group consumer min-idle-time start [COUNT count]
   *
   * @param key Stream Key
   * @param group Consumer Group
   * @param consumerName Consumer name to transfer the auto claimed entries
   * @param minIdleTime Entries pending more than minIdleTime will be transferred ownership
   * @param start {@link StreamEntryID} - Entries >= start will be transferred ownership, passing <code>null</code> will indicate '-'
   * @param params {@link XAutoClaimParams}
   */
  Map.Entry<StreamEntryID, List<StreamEntry>> xautoclaim(String key, String group, String consumerName,
      long minIdleTime, StreamEntryID start, XAutoClaimParams params);

  /**
   * XAUTOCLAIM key group consumer min-idle-time start [COUNT count] JUSTID
   *
   * @param key Stream Key
   * @param group Consumer Group
   * @param consumerName Consumer name to transfer the auto claimed entries
   * @param minIdleTime Entries pending more than minIdleTime will be transferred ownership
   * @param start {@link StreamEntryID} - Entries >= start will be transferred ownership, passing <code>null</code> will indicate '-'
   * @param params {@link XAutoClaimParams}
   */
  Map.Entry<StreamEntryID, List<StreamEntryID>> xautoclaimJustId(String key, String group, String consumerName,
      long minIdleTime, StreamEntryID start, XAutoClaimParams params);

  /**
   * Introspection command used in order to retrieve different information about the stream
   * @param key Stream name
   * @return {@link StreamInfo} that contains information about the stream
   */
  StreamInfo xinfoStream (String key);

  /**
   * Introspection command used in order to retrieve different information about groups in the stream
   * @param key Stream name
   * @return List of {@link StreamGroupInfo} containing information about groups
   */
  List<StreamGroupInfo> xinfoGroup (String key);

  /**
   * Introspection command used in order to retrieve different information about consumers in the group
   * @param key Stream name
   * @param group Group name
   * @return List of {@link StreamConsumersInfo} containing information about consumers that belong
   * to the the group
   */
  List<StreamConsumersInfo> xinfoConsumers (String key, String group);

  Long memoryUsage(String key);

  Long memoryUsage(String key, int samples);

  LCSMatchResult strAlgoLCSStrings(final String strA, final String strB, final StrAlgoLCSParams params);
}
