package redis.clients.jedis;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import redis.clients.jedis.BinaryClient.LIST_POSITION;
import redis.clients.jedis.commands.JedisCommands;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.set.SetParams;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;
import redis.clients.util.Hashing;

public class ShardedJedis extends BinaryShardedJedis implements JedisCommands, Closeable {

  protected ShardedJedisPool dataSource = null;

  public ShardedJedis(List<JedisShardInfo> shards) {
    super(shards);
  }

  public ShardedJedis(List<JedisShardInfo> shards, Hashing algo) {
    super(shards, algo);
  }

  public ShardedJedis(List<JedisShardInfo> shards, Pattern keyTagPattern) {
    super(shards, keyTagPattern);
  }

  public ShardedJedis(List<JedisShardInfo> shards, Hashing algo, Pattern keyTagPattern) {
    super(shards, algo, keyTagPattern);
  }

  @Override
  public String set(String key, String value) {
    Jedis j = getShard(key);
    return j.set(key, value);
  }

  @Override
  public String set(String key, String value, SetParams params) {
    Jedis j = getShard(key);
    return j.set(key, value, params);
  }

  @Override
  public String get(String key) {
    Jedis j = getShard(key);
    return j.get(key);
  }

  @Override
  public String echo(String string) {
    Jedis j = getShard(string);
    return j.echo(string);
  }

  @Override
  public Boolean exists(String key) {
    Jedis j = getShard(key);
    return j.exists(key);
  }

  @Override
  public String type(String key) {
    Jedis j = getShard(key);
    return j.type(key);
  }

  @Override
  public Long expire(String key, int seconds) {
    Jedis j = getShard(key);
    return j.expire(key, seconds);
  }

  @Override
  public Long pexpire(final String key, final long milliseconds) {
    Jedis j = getShard(key);
    return j.pexpire(key, milliseconds);
  }

  @Override
  public Long expireAt(String key, long unixTime) {
    Jedis j = getShard(key);
    return j.expireAt(key, unixTime);
  }

  @Override
  public Long pexpireAt(String key, long millisecondsTimestamp) {
    Jedis j = getShard(key);
    return j.pexpireAt(key, millisecondsTimestamp);
  }

  @Override
  public Long ttl(String key) {
    Jedis j = getShard(key);
    return j.ttl(key);
  }

  @Override
  public Long pttl(String key) {
    Jedis j = getShard(key);
    return j.pttl(key);
  }

  @Override
  public Boolean setbit(String key, long offset, boolean value) {
    Jedis j = getShard(key);
    return j.setbit(key, offset, value);
  }

  @Override
  public Boolean setbit(String key, long offset, String value) {
    Jedis j = getShard(key);
    return j.setbit(key, offset, value);
  }

  @Override
  public Boolean getbit(String key, long offset) {
    Jedis j = getShard(key);
    return j.getbit(key, offset);
  }

  @Override
  public Long setrange(String key, long offset, String value) {
    Jedis j = getShard(key);
    return j.setrange(key, offset, value);
  }

  @Override
  public String getrange(String key, long startOffset, long endOffset) {
    Jedis j = getShard(key);
    return j.getrange(key, startOffset, endOffset);
  }

  @Override
  public String getSet(String key, String value) {
    Jedis j = getShard(key);
    return j.getSet(key, value);
  }

  @Override
  public Long setnx(String key, String value) {
    Jedis j = getShard(key);
    return j.setnx(key, value);
  }

  @Override
  public String setex(String key, int seconds, String value) {
    Jedis j = getShard(key);
    return j.setex(key, seconds, value);
  }

  @Override
  public String psetex(String key, long milliseconds, String value) {
    Jedis j = getShard(key);
    return j.psetex(key, milliseconds, value);
  }

  public List<String> blpop(String arg) {
    Jedis j = getShard(arg);
    return j.blpop(arg);
  }

  @Override
  public List<String> blpop(int timeout, String key) {
    Jedis j = getShard(key);
    return j.blpop(timeout, key);
  }

  public List<String> brpop(String arg) {
    Jedis j = getShard(arg);
    return j.brpop(arg);
  }

  @Override
  public List<String> brpop(int timeout, String key) {
    Jedis j = getShard(key);
    return j.brpop(timeout, key);
  }

  @Override
  public Long decrBy(String key, long integer) {
    Jedis j = getShard(key);
    return j.decrBy(key, integer);
  }

  @Override
  public Long decr(String key) {
    Jedis j = getShard(key);
    return j.decr(key);
  }

  @Override
  public Long incrBy(String key, long integer) {
    Jedis j = getShard(key);
    return j.incrBy(key, integer);
  }

  @Override
  public Double incrByFloat(String key, double integer) {
    Jedis j = getShard(key);
    return j.incrByFloat(key, integer);
  }

  @Override
  public Long incr(String key) {
    Jedis j = getShard(key);
    return j.incr(key);
  }

  @Override
  public Long append(String key, String value) {
    Jedis j = getShard(key);
    return j.append(key, value);
  }

  @Override
  public String substr(String key, int start, int end) {
    Jedis j = getShard(key);
    return j.substr(key, start, end);
  }

  @Override
  public Long hset(String key, String field, String value) {
    Jedis j = getShard(key);
    return j.hset(key, field, value);
  }

  @Override
  public String hget(String key, String field) {
    Jedis j = getShard(key);
    return j.hget(key, field);
  }

  @Override
  public Long hsetnx(String key, String field, String value) {
    Jedis j = getShard(key);
    return j.hsetnx(key, field, value);
  }

  @Override
  public String hmset(String key, Map<String, String> hash) {
    Jedis j = getShard(key);
    return j.hmset(key, hash);
  }

  @Override
  public List<String> hmget(String key, String... fields) {
    Jedis j = getShard(key);
    return j.hmget(key, fields);
  }

  @Override
  public Long hincrBy(String key, String field, long value) {
    Jedis j = getShard(key);
    return j.hincrBy(key, field, value);
  }

  @Override
  public Double hincrByFloat(String key, String field, double value) {
    Jedis j = getShard(key);
    return j.hincrByFloat(key, field, value);
  }

  @Override
  public Boolean hexists(String key, String field) {
    Jedis j = getShard(key);
    return j.hexists(key, field);
  }

  @Override
  public Long del(String key) {
    Jedis j = getShard(key);
    return j.del(key);
  }

  @Override
  public Long hdel(String key, String... fields) {
    Jedis j = getShard(key);
    return j.hdel(key, fields);
  }

  @Override
  public Long hlen(String key) {
    Jedis j = getShard(key);
    return j.hlen(key);
  }

  @Override
  public Set<String> hkeys(String key) {
    Jedis j = getShard(key);
    return j.hkeys(key);
  }

  @Override
  public List<String> hvals(String key) {
    Jedis j = getShard(key);
    return j.hvals(key);
  }

  @Override
  public Map<String, String> hgetAll(String key) {
    Jedis j = getShard(key);
    return j.hgetAll(key);
  }

  @Override
  public Long rpush(String key, String... strings) {
    Jedis j = getShard(key);
    return j.rpush(key, strings);
  }

  @Override
  public Long lpush(String key, String... strings) {
    Jedis j = getShard(key);
    return j.lpush(key, strings);
  }

  @Override
  public Long lpushx(String key, String... string) {
    Jedis j = getShard(key);
    return j.lpushx(key, string);
  }

  @Override
  public Long strlen(final String key) {
    Jedis j = getShard(key);
    return j.strlen(key);
  }

  @Override
  public Long move(String key, int dbIndex) {
    Jedis j = getShard(key);
    return j.move(key, dbIndex);
  }

  @Override
  public Long rpushx(String key, String... string) {
    Jedis j = getShard(key);
    return j.rpushx(key, string);
  }

  @Override
  public Long persist(final String key) {
    Jedis j = getShard(key);
    return j.persist(key);
  }

  @Override
  public Long llen(String key) {
    Jedis j = getShard(key);
    return j.llen(key);
  }

  @Override
  public List<String> lrange(String key, long start, long end) {
    Jedis j = getShard(key);
    return j.lrange(key, start, end);
  }

  @Override
  public String ltrim(String key, long start, long end) {
    Jedis j = getShard(key);
    return j.ltrim(key, start, end);
  }

  @Override
  public String lindex(String key, long index) {
    Jedis j = getShard(key);
    return j.lindex(key, index);
  }

  @Override
  public String lset(String key, long index, String value) {
    Jedis j = getShard(key);
    return j.lset(key, index, value);
  }

  @Override
  public Long lrem(String key, long count, String value) {
    Jedis j = getShard(key);
    return j.lrem(key, count, value);
  }

  @Override
  public String lpop(String key) {
    Jedis j = getShard(key);
    return j.lpop(key);
  }

  @Override
  public String rpop(String key) {
    Jedis j = getShard(key);
    return j.rpop(key);
  }

  @Override
  public Long sadd(String key, String... members) {
    Jedis j = getShard(key);
    return j.sadd(key, members);
  }

  @Override
  public Set<String> smembers(String key) {
    Jedis j = getShard(key);
    return j.smembers(key);
  }

  @Override
  public Long srem(String key, String... members) {
    Jedis j = getShard(key);
    return j.srem(key, members);
  }

  @Override
  public String spop(String key) {
    Jedis j = getShard(key);
    return j.spop(key);
  }

  @Override
  public Set<String> spop(String key, long count) {
    Jedis j = getShard(key);
    return j.spop(key, count);
  }

  @Override
  public Long scard(String key) {
    Jedis j = getShard(key);
    return j.scard(key);
  }

  @Override
  public Boolean sismember(String key, String member) {
    Jedis j = getShard(key);
    return j.sismember(key, member);
  }

  @Override
  public String srandmember(String key) {
    Jedis j = getShard(key);
    return j.srandmember(key);
  }

  @Override
  public List<String> srandmember(String key, int count) {
    Jedis j = getShard(key);
    return j.srandmember(key, count);
  }

  @Override
  public Long zadd(String key, double score, String member) {
    Jedis j = getShard(key);
    return j.zadd(key, score, member);
  }

  @Override
  public Long zadd(String key, double score, String member, ZAddParams params) {
    Jedis j = getShard(key);
    return j.zadd(key, score, member, params);
  }

  @Override
  public Long zadd(String key, Map<String, Double> scoreMembers) {
    Jedis j = getShard(key);
    return j.zadd(key, scoreMembers);
  }

  @Override
  public Long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
    Jedis j = getShard(key);
    return j.zadd(key, scoreMembers, params);
  }

  @Override
  public Set<String> zrange(String key, long start, long end) {
    Jedis j = getShard(key);
    return j.zrange(key, start, end);
  }

  @Override
  public Long zrem(String key, String... members) {
    Jedis j = getShard(key);
    return j.zrem(key, members);
  }

  @Override
  public Double zincrby(String key, double score, String member) {
    Jedis j = getShard(key);
    return j.zincrby(key, score, member);
  }

  @Override
  public Double zincrby(String key, double score, String member, ZIncrByParams params) {
    Jedis j = getShard(key);
    return j.zincrby(key, score, member, params);
  }

  @Override
  public Long zrank(String key, String member) {
    Jedis j = getShard(key);
    return j.zrank(key, member);
  }

  @Override
  public Long zrevrank(String key, String member) {
    Jedis j = getShard(key);
    return j.zrevrank(key, member);
  }

  @Override
  public Set<String> zrevrange(String key, long start, long end) {
    Jedis j = getShard(key);
    return j.zrevrange(key, start, end);
  }

  @Override
  public Set<Tuple> zrangeWithScores(String key, long start, long end) {
    Jedis j = getShard(key);
    return j.zrangeWithScores(key, start, end);
  }

  @Override
  public Set<Tuple> zrevrangeWithScores(String key, long start, long end) {
    Jedis j = getShard(key);
    return j.zrevrangeWithScores(key, start, end);
  }

  @Override
  public Long zcard(String key) {
    Jedis j = getShard(key);
    return j.zcard(key);
  }

  @Override
  public Double zscore(String key, String member) {
    Jedis j = getShard(key);
    return j.zscore(key, member);
  }

  @Override
  public List<String> sort(String key) {
    Jedis j = getShard(key);
    return j.sort(key);
  }

  @Override
  public List<String> sort(String key, SortingParams sortingParameters) {
    Jedis j = getShard(key);
    return j.sort(key, sortingParameters);
  }

  @Override
  public Long zcount(String key, double min, double max) {
    Jedis j = getShard(key);
    return j.zcount(key, min, max);
  }

  @Override
  public Long zcount(String key, String min, String max) {
    Jedis j = getShard(key);
    return j.zcount(key, min, max);
  }

  @Override
  public Set<String> zrangeByScore(String key, double min, double max) {
    Jedis j = getShard(key);
    return j.zrangeByScore(key, min, max);
  }

  @Override
  public Set<String> zrevrangeByScore(String key, double max, double min) {
    Jedis j = getShard(key);
    return j.zrevrangeByScore(key, max, min);
  }

  @Override
  public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
    Jedis j = getShard(key);
    return j.zrangeByScore(key, min, max, offset, count);
  }

  @Override
  public Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByScore(key, max, min, offset, count);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
    Jedis j = getShard(key);
    return j.zrangeByScoreWithScores(key, min, max);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
    Jedis j = getShard(key);
    return j.zrevrangeByScoreWithScores(key, max, min);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset,
      int count) {
    Jedis j = getShard(key);
    return j.zrangeByScoreWithScores(key, min, max, offset, count);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset,
      int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByScoreWithScores(key, max, min, offset, count);
  }

  @Override
  public Set<String> zrangeByScore(String key, String min, String max) {
    Jedis j = getShard(key);
    return j.zrangeByScore(key, min, max);
  }

  @Override
  public Set<String> zrevrangeByScore(String key, String max, String min) {
    Jedis j = getShard(key);
    return j.zrevrangeByScore(key, max, min);
  }

  @Override
  public Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
    Jedis j = getShard(key);
    return j.zrangeByScore(key, min, max, offset, count);
  }

  @Override
  public Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByScore(key, max, min, offset, count);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
    Jedis j = getShard(key);
    return j.zrangeByScoreWithScores(key, min, max);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
    Jedis j = getShard(key);
    return j.zrevrangeByScoreWithScores(key, max, min);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset,
      int count) {
    Jedis j = getShard(key);
    return j.zrangeByScoreWithScores(key, min, max, offset, count);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset,
      int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByScoreWithScores(key, max, min, offset, count);
  }

  @Override
  public Long zremrangeByRank(String key, long start, long end) {
    Jedis j = getShard(key);
    return j.zremrangeByRank(key, start, end);
  }

  @Override
  public Long zremrangeByScore(String key, double start, double end) {
    Jedis j = getShard(key);
    return j.zremrangeByScore(key, start, end);
  }

  @Override
  public Long zremrangeByScore(String key, String start, String end) {
    Jedis j = getShard(key);
    return j.zremrangeByScore(key, start, end);
  }

  @Override
  public Long zlexcount(final String key, final String min, final String max) {
    return getShard(key).zlexcount(key, min, max);
  }

  @Override
  public Set<String> zrangeByLex(final String key, final String min, final String max) {
    return getShard(key).zrangeByLex(key, min, max);
  }

  @Override
  public Set<String> zrangeByLex(final String key, final String min, final String max,
      final int offset, final int count) {
    return getShard(key).zrangeByLex(key, min, max, offset, count);
  }

  @Override
  public Set<String> zrevrangeByLex(String key, String max, String min) {
    return getShard(key).zrevrangeByLex(key, max, min);
  }

  @Override
  public Set<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
    return getShard(key).zrevrangeByLex(key, max, min, offset, count);
  }

  @Override
  public Long zremrangeByLex(final String key, final String min, final String max) {
    return getShard(key).zremrangeByLex(key, min, max);
  }

  @Override
  public Long linsert(String key, LIST_POSITION where, String pivot, String value) {
    Jedis j = getShard(key);
    return j.linsert(key, where, pivot, value);
  }

  @Override
  public Long bitcount(final String key) {
    Jedis j = getShard(key);
    return j.bitcount(key);
  }

  @Override
  public Long bitcount(final String key, long start, long end) {
    Jedis j = getShard(key);
    return j.bitcount(key, start, end);
  }

  @Override
  public Long bitpos(String key, boolean value) {
    Jedis j = getShard(key);
    return j.bitpos(key, value);
  }

  @Override
  public Long bitpos(String key, boolean value, BitPosParams params) {
    Jedis j = getShard(key);
    return j.bitpos(key, value, params);
  }

  @Override
  public ScanResult<Entry<String, String>> hscan(String key, final String cursor) {
    Jedis j = getShard(key);
    return j.hscan(key, cursor);
  }

  @Override
  public ScanResult<Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
    Jedis j = getShard(key);
    return j.hscan(key, cursor, params);
  }

  @Override
  public ScanResult<String> sscan(String key, final String cursor) {
    Jedis j = getShard(key);
    return j.sscan(key, cursor);
  }

  @Override
  public ScanResult<Tuple> zscan(String key, final String cursor) {
    Jedis j = getShard(key);
    return j.zscan(key, cursor);
  }

  @Override
  public ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
    Jedis j = getShard(key);
    return j.zscan(key, cursor, params);
  }

  @Override
  public ScanResult<String> sscan(String key, String cursor, ScanParams params) {
    Jedis j = getShard(key);
    return j.sscan(key, cursor, params);
  }

  @Override
  public void close() {
    if (dataSource != null) {
      boolean broken = false;

      for (Jedis jedis : getAllShards()) {
        if (jedis.getClient().isBroken()) {
          broken = true;
          break;
        }
      }

      if (broken) {
        dataSource.returnBrokenResource(this);
      } else {
        dataSource.returnResource(this);
      }

    } else {
      disconnect();
    }
  }

  public void setDataSource(ShardedJedisPool shardedJedisPool) {
    this.dataSource = shardedJedisPool;
  }

  public void resetState() {
    for (Jedis jedis : getAllShards()) {
      jedis.resetState();
    }
  }

  @Override
  public Long pfadd(String key, String... elements) {
    Jedis j = getShard(key);
    return j.pfadd(key, elements);
  }

  @Override
  public long pfcount(String key) {
    Jedis j = getShard(key);
    return j.pfcount(key);
  }

  @Override
  public Long geoadd(String key, double longitude, double latitude, String member) {
    Jedis j = getShard(key);
    return j.geoadd(key, longitude, latitude, member);
  }

  @Override
  public Long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
    Jedis j = getShard(key);
    return j.geoadd(key, memberCoordinateMap);
  }

  @Override
  public Double geodist(String key, String member1, String member2) {
    Jedis j = getShard(key);
    return j.geodist(key, member1, member2);
  }

  @Override
  public Double geodist(String key, String member1, String member2, GeoUnit unit) {
    Jedis j = getShard(key);
    return j.geodist(key, member1, member2, unit);
  }

  @Override
  public List<String> geohash(String key, String... members) {
    Jedis j = getShard(key);
    return j.geohash(key, members);
  }

  @Override
  public List<GeoCoordinate> geopos(String key, String... members) {
    Jedis j = getShard(key);
    return j.geopos(key, members);
  }

  @Override
  public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude,
      double radius, GeoUnit unit) {
    Jedis j = getShard(key);
    return j.georadius(key, longitude, latitude, radius, unit);
  }

  @Override
  public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude,
      double radius, GeoUnit unit, GeoRadiusParam param) {
    Jedis j = getShard(key);
    return j.georadius(key, longitude, latitude, radius, unit, param);
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius,
      GeoUnit unit) {
    Jedis j = getShard(key);
    return j.georadiusByMember(key, member, radius, unit);
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius,
      GeoUnit unit, GeoRadiusParam param) {
    Jedis j = getShard(key);
    return j.georadiusByMember(key, member, radius, unit, param);
  }
}
