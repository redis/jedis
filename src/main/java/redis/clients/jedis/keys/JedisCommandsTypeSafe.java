package redis.clients.jedis.keys;

import redis.clients.jedis.*;
import redis.clients.jedis.commands.JedisCommands;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.set.SetParams;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface JedisCommandsTypeSafe extends JedisCommands {

    default String set(SetKey key, String value) { return set(key.getKey(), value); }

    default String set(SetKey key, String value, SetParams params) { return set(key.getKey(), value, params); }

    String get(String key);

    Boolean exists(String key);

    Long persist(String key);

    String type(String key);

    byte[] dump(String key);

    String restore(String key, int ttl, byte[] serializedValue);

    default Long expire(TypeSafeKey key, int seconds) { return expire(key.getKey(), seconds); }

    Long pexpire(String key, long milliseconds);

    Long expireAt(String key, long unixTime);

    Long pexpireAt(String key, long millisecondsTimestamp);

    Long ttl(String key);

    Long pttl(String key);

    default Long touch(TypeSafeKey key) { return touch(key.getKey()); }

    default Boolean setbit(SetKey key, long offset, boolean value) { return setbit(key.getKey(), offset, value); }

    default Boolean setbit(SetKey key, long offset, String value) { return setbit(key.getKey(), offset, value); }

    Boolean getbit(String key, long offset);

    default Long setrange(SetKey key, long offset, String value) { return setrange(key.getKey(), offset, value); }

    String getrange(String key, long startOffset, long endOffset);

    String getSet(String key, String value);

    default Long setnx(SetKey key, String value) { return setnx(key.getKey(), value); }

    default String setex(SetKey key, int seconds, String value) { return setex(key.getKey(), seconds, value); }

    String psetex(String key, long milliseconds, String value);

    default Long decrBy(NumberKey key, long decrement) {  return decrBy(key.getKey(), decrement); }

    default Long decr(NumberKey key) {  return decr(key.getKey()); }

    default Long incrBy(NumberKey key, long increment) {  return incrBy(key.getKey(), increment); }

    default Double incrByFloat(NumberKey key, double increment) {  return incrByFloat(key.getKey(), increment); }

    default Long incr(NumberKey key) {  return incr(key.getKey()); }

    default Long append(StringKey key, String value) { return append(key.getKey(), value); }

    // Hashes

    default Long hset(HashKey key, String field, String value) { return hset(key.getKey(), field, value); }

    default Long hset(HashKey key, Map<String, String> hash) { return hset(key.getKey(), hash); }

    default String hget(HashKey key, String field) { return hget(key.getKey(), field); }

    default Long hsetnx(HashKey key, String field, String value) { return hsetnx(key.getKey(), field, value); }

    default String hmset(HashKey key, Map<String, String> hash) { return hmset(key.getKey(), hash); }

    default List<String> hmget(HashKey key, String... fields) { return hmget(key.getKey(), fields); }

    default Long hincrBy(HashKey key, String field, long value) { return hincrBy(key.getKey(), field, value); }

    default Double hincrByFloat(HashKey key, String field, double value) { return hincrByFloat(key.getKey(), field, value); }

    default Boolean hexists(HashKey key, String field) { return hexists(key.getKey(), field); }

    default Long hdel(HashKey key, String... field) { return hdel(key.getKey(), field); }

    default Long hlen(HashKey key) { return hlen(key.getKey()); }

    default Set<String> hkeys(HashKey key) { return hkeys(key.getKey()); }

    default List<String> hvals(HashKey key) { return hvals(key.getKey()); }

    default Map<String, String> hgetAll(HashKey key) { return hgetAll(key.getKey()); }

    // Lists

    default Long rpush(ListKey key, String... string) { return rpush(key.getKey(), string); }

    default Long lpush(ListKey key, String... string) { return lpush(key.getKey(), string); }

    default Long llen(ListKey key) { return llen(key.getKey()); }

    default List<String> lrange(ListKey key, long start, long stop) { return lrange(key.getKey(), start, stop); }

    default String ltrim(ListKey key, long start, long stop) { return ltrim(key.getKey(), start, stop); }

    default String lindex(ListKey key, long index) { return lindex(key.getKey(), index); }

    default String lset(ListKey key, long index, String value) { return lset(key.getKey(), index, value); }

    default Long lrem(ListKey key, long count, String value) { return lrem(key.getKey(), count, value); }

    default String lpop(ListKey key) { return lpop(key.getKey()); }

    default String rpop(ListKey key) { return rpop(key.getKey()); }

    // Sets

    default Long sadd(SetKey key, String... member) { return sadd(key.getKey(), member); }

    default Set<String> smembers(SetKey key) { return smembers(key.getKey()); }

    default Long srem(SetKey key, String... member) { return srem(key.getKey(), member); }

    default String spop(SetKey key) { return spop(key.getKey()); }

    default Set<String> spop(SetKey key, long count) { return spop(key.getKey(), count); }

    default Long scard(SetKey key) { return scard(key.getKey()); }

    default Boolean sismember(SetKey key, String member) { return sismember(key.getKey(), member); }

    default String srandmember(SetKey key) { return srandmember(key.getKey()); }

    default List<String> srandmember(SetKey key, int count) { return srandmember(key.getKey(), count); }

    default Long strlen(SetKey key) { return strlen(key.getKey()); }

    // Sorted sets

    default Long zadd(SortedSetKey key, double score, String member) { return zadd(key.getKey(), score, member); }

    default Long zadd(SortedSetKey key, double score, String member, ZAddParams params) { return zadd(key.getKey(), score, member, params); }

    default Long zadd(SortedSetKey key, Map<String, Double> scoreMembers) { return zadd(key.getKey(), scoreMembers); }

    default Long zadd(SortedSetKey key, Map<String, Double> scoreMembers, ZAddParams params) { return zadd(key.getKey(), scoreMembers, params); }

    default Set<String> zrange(SortedSetKey key, long start, long stop) { return zrange(key.getKey(), start, stop); }

    default Long zrem(SortedSetKey key, String... members) { return zrem(key.getKey(), members); }

    default Double zincrby(SortedSetKey key, double increment, String member) { return zincrby(key.getKey(), increment, member); }

    default Double zincrby(SortedSetKey key, double increment, String member, ZIncrByParams params) { return zincrby(key.getKey(), increment, member, params); }

    default Long zrank(SortedSetKey key, String member) { return zrank(key.getKey(), member); }

    default Long zrevrank(SortedSetKey key, String member) { return zrevrank(key.getKey(), member); }

    default Set<String> zrevrange(SortedSetKey key, long start, long stop) { return zrevrange(key.getKey(), start, stop); }

    default Set<Tuple> zrangeWithScores(SortedSetKey key, long start, long stop) { return zrangeWithScores(key.getKey(), start, stop); }

    default Set<Tuple> zrevrangeWithScores(SortedSetKey key, long start, long stop) { return zrevrangeWithScores(key.getKey(), start, stop); }

    default Long zcard(SortedSetKey key) { return zcard(key.getKey()); }

    default Double zscore(SortedSetKey key, String member) { return zscore(key.getKey(), member); }

    default Long zcount(SortedSetKey key, double min, double max) { return zcount(key.getKey(), min, max); }

    default Long zcount(SortedSetKey key, String min, String max) { return zcount(key.getKey(), min, max); }

    default Set<String> zrangeByScore(SortedSetKey key, double min, double max) { return zrangeByScore(key.getKey(), min, max); }

    default Set<String> zrangeByScore(SortedSetKey key, String min, String max) { return zrangeByScore(key.getKey(), min, max); }

    default Set<String> zrevrangeByScore(SortedSetKey key, double max, double min) { return zrevrangeByScore(key.getKey(), max, min); }

    default Set<String> zrangeByScore(SortedSetKey key, double min, double max, int offset, int count) { return zrangeByScore(key.getKey(), min, max, offset, count); }

    default Set<String> zrevrangeByScore(SortedSetKey key, String max, String min) { return zrevrangeByScore(key.getKey(), max, min); }

    default Set<String> zrangeByScore(SortedSetKey key, String min, String max, int offset, int count) { return zrangeByScore(key.getKey(), min, max, offset, count); }

    default Set<String> zrevrangeByScore(SortedSetKey key, double max, double min, int offset, int count) { return zrevrangeByScore(key.getKey(), max, min, offset, count); }

    default Set<Tuple> zrangeByScoreWithScores(SortedSetKey key, double min, double max) { return zrangeByScoreWithScores(key.getKey(), min, max); }

    default Set<Tuple> zrevrangeByScoreWithScores(SortedSetKey key, double max, double min) { return zrevrangeByScoreWithScores(key.getKey(), max, min); }

    default Set<Tuple> zrangeByScoreWithScores(SortedSetKey key, double min, double max, int offset, int count) { return zrangeByScoreWithScores(key.getKey(), min, max, offset, count); }

    default Set<String> zrevrangeByScore(SortedSetKey key, String max, String min, int offset, int count) { return zrevrangeByScore(key.getKey(), max, min, offset, count); }

    default Set<Tuple> zrangeByScoreWithScores(SortedSetKey key, String min, String max) { return zrangeByScoreWithScores(key.getKey(), min, max); }

    default Set<Tuple> zrevrangeByScoreWithScores(SortedSetKey key, String max, String min) { return zrevrangeByScoreWithScores(key.getKey(), max, min); }

    default Set<Tuple> zrangeByScoreWithScores(SortedSetKey key, String min, String max, int offset, int count) { return zrangeByScoreWithScores(key.getKey(), min, max, offset, count); }

    default Set<Tuple> zrevrangeByScoreWithScores(SortedSetKey key, double max, double min, int offset, int count) { return zrevrangeByScoreWithScores(key.getKey(), max, min, offset, count); }

    default Set<Tuple> zrevrangeByScoreWithScores(SortedSetKey key, String max, String min, int offset, int count) { return zrevrangeByScoreWithScores(key.getKey(), max, min, offset, count); }

    default Long zremrangeByRank(SortedSetKey key, long start, long stop) { return zremrangeByRank(key.getKey(), start, stop); }

    default Long zremrangeByScore(SortedSetKey key, double min, double max) { return zremrangeByScore(key.getKey(), min, max); }

    default Long zremrangeByScore(SortedSetKey key, String min, String max) { return zremrangeByScore(key.getKey(), min, max); }

    default Long zlexcount(SortedSetKey key, String min, String max) { return zlexcount(key.getKey(), min, max); }

    default Set<String> zrangeByLex(SortedSetKey key, String min, String max) { return zrangeByLex(key.getKey(), min, max); }

    default Set<String> zrangeByLex(SortedSetKey key, String min, String max, int offset, int count) { return zrangeByLex(key.getKey(), min, max, offset, count); }

    default Set<String> zrevrangeByLex(SortedSetKey key, String max, String min) { return zrevrangeByLex(key.getKey(), max, min); }

    default Set<String> zrevrangeByLex(SortedSetKey key, String max, String min, int offset, int count) { return zrevrangeByLex(key.getKey(), max, min, offset, count); }

    default Long zremrangeByLex(SortedSetKey key, String min, String max) { return zremrangeByLex(key.getKey(), min, max); }
    
    // Lists

    default Long linsert(ListKey key, ListPosition where, String pivot, String value) { return linsert(key.getKey(), where, pivot, value); }

    default Long lpushx(ListKey key, String... string) { return lpushx(key.getKey(), string); }

    default Long rpushx(ListKey key, String... string) { return rpushx(key.getKey(), string); }

    default List<String> blpop(int timeout, ListKey key) { return blpop(timeout, key.getKey()); }

    default List<String> brpop(int timeout, ListKey key) { return brpop(timeout, key.getKey()); }

    Long del(String key);

    Long unlink(String key);

    String echo(String string);

    Long move(String key, int dbIndex);

    Long bitcount(String key);

    Long bitcount(String key, long start, long end);

    Long bitpos(String key, boolean value);

    Long bitpos(String key, boolean value, BitPosParams params);

    default ScanResult<Map.Entry<String, String>> hscan(HashKey key, String cursor) { return hscan(key.getKey(), cursor); }

    default ScanResult<Map.Entry<String, String>> hscan(HashKey key, String cursor, ScanParams params) { return hscan(key.getKey(), cursor, params); }

    default ScanResult<String> sscan(SetKey key, String cursor) { return sscan(key.getKey(), cursor); }

    default ScanResult<Tuple> zscan(SortedSetKey key, String cursor) { return zscan(key.getKey(), cursor); }

    default ScanResult<Tuple> zscan(SortedSetKey key, String cursor, ScanParams params) { return zscan(key.getKey(), cursor, params); }

    default ScanResult<String> sscan(SetKey key, String cursor, ScanParams params) { return sscan(key.getKey(), cursor, params); }

    Long pfadd(String key, String... elements);

    long pfcount(String key);

    Long geoadd(String key, double longitude, double latitude, String member);

    Long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap);

    Double geodist(String key, String member1, String member2);

    Double geodist(String key, String member1, String member2, GeoUnit unit);

    List<String> geohash(String key, String... members);

    List<GeoCoordinate> geopos(String key, String... members);

    List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit);

    List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param);

    List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit);

    List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param);

    List<Long> bitfield(String key, String... arguments);

    default Long hstrlen(HashKey key, String field) { return hstrlen(key.getKey(), field); }
}
