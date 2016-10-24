package redis.clients.jedis;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import redis.clients.jedis.BinaryClient.LIST_POSITION;
import redis.clients.jedis.commands.BinaryJedisCommands;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.set.SetParams;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;
import redis.clients.util.Hashing;
import redis.clients.util.Sharded;

public class BinaryShardedJedis extends Sharded<Jedis, JedisShardInfo> implements
    BinaryJedisCommands {
  public BinaryShardedJedis(List<JedisShardInfo> shards) {
    super(shards);
  }

  public BinaryShardedJedis(List<JedisShardInfo> shards, Hashing algo) {
    super(shards, algo);
  }

  public BinaryShardedJedis(List<JedisShardInfo> shards, Pattern keyTagPattern) {
    super(shards, keyTagPattern);
  }

  public BinaryShardedJedis(List<JedisShardInfo> shards, Hashing algo, Pattern keyTagPattern) {
    super(shards, algo, keyTagPattern);
  }

  public void disconnect() {
    for (Jedis jedis : getAllShards()) {
      try {
        jedis.quit();
      } catch (JedisConnectionException e) {
        // ignore the exception node, so that all other normal nodes can release all connections.
      }
      try {
        jedis.disconnect();
      } catch (JedisConnectionException e) {
        // ignore the exception node, so that all other normal nodes can release all connections.
      }
    }
  }

  protected Jedis create(JedisShardInfo shard) {
    return new Jedis(shard);
  }

  @Override
  public String set(byte[] key, byte[] value) {
    Jedis j = getShard(key);
    return j.set(key, value);
  }

  @Override
  public String set(byte[] key, byte[] value, SetParams params) {
    Jedis j = getShard(key);
    return j.set(key, value, params);
  }

  @Override
  public byte[] get(byte[] key) {
    Jedis j = getShard(key);
    return j.get(key);
  }

  @Override
  public Boolean exists(byte[] key) {
    Jedis j = getShard(key);
    return j.exists(key);
  }

  @Override
  public String type(byte[] key) {
    Jedis j = getShard(key);
    return j.type(key);
  }

  @Override
  public Long expire(byte[] key, int seconds) {
    Jedis j = getShard(key);
    return j.expire(key, seconds);
  }

  @Override
  public Long pexpire(byte[] key, final long milliseconds) {
    Jedis j = getShard(key);
    return j.pexpire(key, milliseconds);
  }

  @Override
  public Long expireAt(byte[] key, long unixTime) {
    Jedis j = getShard(key);
    return j.expireAt(key, unixTime);
  }

  @Override
  public Long pexpireAt(byte[] key, long millisecondsTimestamp) {
    Jedis j = getShard(key);
    return j.pexpireAt(key, millisecondsTimestamp);
  }

  @Override
  public Long ttl(byte[] key) {
    Jedis j = getShard(key);
    return j.ttl(key);
  }

  @Override
  public byte[] getSet(byte[] key, byte[] value) {
    Jedis j = getShard(key);
    return j.getSet(key, value);
  }

  @Override
  public Long setnx(byte[] key, byte[] value) {
    Jedis j = getShard(key);
    return j.setnx(key, value);
  }

  @Override
  public String setex(byte[] key, int seconds, byte[] value) {
    Jedis j = getShard(key);
    return j.setex(key, seconds, value);
  }

  @Override
  public Long decrBy(byte[] key, long integer) {
    Jedis j = getShard(key);
    return j.decrBy(key, integer);
  }

  @Override
  public Long decr(byte[] key) {
    Jedis j = getShard(key);
    return j.decr(key);
  }

  @Override
  public Long del(byte[] key) {
    Jedis j = getShard(key);
    return j.del(key);
  }

  @Override
  public Long incrBy(byte[] key, long integer) {
    Jedis j = getShard(key);
    return j.incrBy(key, integer);
  }

  @Override
  public Double incrByFloat(byte[] key, double integer) {
    Jedis j = getShard(key);
    return j.incrByFloat(key, integer);
  }

  @Override
  public Long incr(byte[] key) {
    Jedis j = getShard(key);
    return j.incr(key);
  }

  @Override
  public Long append(byte[] key, byte[] value) {
    Jedis j = getShard(key);
    return j.append(key, value);
  }

  @Override
  public byte[] substr(byte[] key, int start, int end) {
    Jedis j = getShard(key);
    return j.substr(key, start, end);
  }

  @Override
  public Long hset(byte[] key, byte[] field, byte[] value) {
    Jedis j = getShard(key);
    return j.hset(key, field, value);
  }

  @Override
  public byte[] hget(byte[] key, byte[] field) {
    Jedis j = getShard(key);
    return j.hget(key, field);
  }

  @Override
  public Long hsetnx(byte[] key, byte[] field, byte[] value) {
    Jedis j = getShard(key);
    return j.hsetnx(key, field, value);
  }

  @Override
  public String hmset(byte[] key, Map<byte[], byte[]> hash) {
    Jedis j = getShard(key);
    return j.hmset(key, hash);
  }

  @Override
  public List<byte[]> hmget(byte[] key, byte[]... fields) {
    Jedis j = getShard(key);
    return j.hmget(key, fields);
  }

  @Override
  public Long hincrBy(byte[] key, byte[] field, long value) {
    Jedis j = getShard(key);
    return j.hincrBy(key, field, value);
  }

  @Override
  public Double hincrByFloat(byte[] key, byte[] field, double value) {
    Jedis j = getShard(key);
    return j.hincrByFloat(key, field, value);
  }

  @Override
  public Boolean hexists(byte[] key, byte[] field) {
    Jedis j = getShard(key);
    return j.hexists(key, field);
  }

  @Override
  public Long hdel(byte[] key, byte[]... fields) {
    Jedis j = getShard(key);
    return j.hdel(key, fields);
  }

  @Override
  public Long hlen(byte[] key) {
    Jedis j = getShard(key);
    return j.hlen(key);
  }

  @Override
  public Set<byte[]> hkeys(byte[] key) {
    Jedis j = getShard(key);
    return j.hkeys(key);
  }

  @Override
  public Collection<byte[]> hvals(byte[] key) {
    Jedis j = getShard(key);
    return j.hvals(key);
  }

  @Override
  public Map<byte[], byte[]> hgetAll(byte[] key) {
    Jedis j = getShard(key);
    return j.hgetAll(key);
  }

  @Override
  public Long rpush(byte[] key, byte[]... strings) {
    Jedis j = getShard(key);
    return j.rpush(key, strings);
  }

  @Override
  public Long lpush(byte[] key, byte[]... strings) {
    Jedis j = getShard(key);
    return j.lpush(key, strings);
  }

  @Override
  public Long strlen(final byte[] key) {
    Jedis j = getShard(key);
    return j.strlen(key);
  }

  @Override
  public Long lpushx(byte[] key, byte[]... string) {
    Jedis j = getShard(key);
    return j.lpushx(key, string);
  }

  @Override
  public Long persist(final byte[] key) {
    Jedis j = getShard(key);
    return j.persist(key);
  }

  @Override
  public Long rpushx(byte[] key, byte[]... string) {
    Jedis j = getShard(key);
    return j.rpushx(key, string);
  }

  @Override
  public Long llen(byte[] key) {
    Jedis j = getShard(key);
    return j.llen(key);
  }

  @Override
  public List<byte[]> lrange(byte[] key, long start, long end) {
    Jedis j = getShard(key);
    return j.lrange(key, start, end);
  }

  @Override
  public String ltrim(byte[] key, long start, long end) {
    Jedis j = getShard(key);
    return j.ltrim(key, start, end);
  }

  @Override
  public byte[] lindex(byte[] key, long index) {
    Jedis j = getShard(key);
    return j.lindex(key, index);
  }

  @Override
  public String lset(byte[] key, long index, byte[] value) {
    Jedis j = getShard(key);
    return j.lset(key, index, value);
  }

  @Override
  public Long lrem(byte[] key, long count, byte[] value) {
    Jedis j = getShard(key);
    return j.lrem(key, count, value);
  }

  @Override
  public byte[] lpop(byte[] key) {
    Jedis j = getShard(key);
    return j.lpop(key);
  }

  @Override
  public byte[] rpop(byte[] key) {
    Jedis j = getShard(key);
    return j.rpop(key);
  }

  @Override
  public Long sadd(byte[] key, byte[]... members) {
    Jedis j = getShard(key);
    return j.sadd(key, members);
  }

  @Override
  public Set<byte[]> smembers(byte[] key) {
    Jedis j = getShard(key);
    return j.smembers(key);
  }

  @Override
  public Long srem(byte[] key, byte[]... members) {
    Jedis j = getShard(key);
    return j.srem(key, members);
  }

  @Override
  public byte[] spop(byte[] key) {
    Jedis j = getShard(key);
    return j.spop(key);
  }

  @Override
  public Set<byte[]> spop(byte[] key, long count) {
    Jedis j = getShard(key);
    return j.spop(key, count);
  }

  @Override
  public Long scard(byte[] key) {
    Jedis j = getShard(key);
    return j.scard(key);
  }

  @Override
  public Boolean sismember(byte[] key, byte[] member) {
    Jedis j = getShard(key);
    return j.sismember(key, member);
  }

  @Override
  public byte[] srandmember(byte[] key) {
    Jedis j = getShard(key);
    return j.srandmember(key);
  }

  @Override
  public List srandmember(byte[] key, int count) {
    Jedis j = getShard(key);
    return j.srandmember(key, count);
  }

  @Override
  public Long zadd(byte[] key, double score, byte[] member) {
    Jedis j = getShard(key);
    return j.zadd(key, score, member);
  }

  @Override
  public Long zadd(byte[] key, double score, byte[] member, ZAddParams params) {
    Jedis j = getShard(key);
    return j.zadd(key, score, member, params);
  }

  @Override
  public Long zadd(byte[] key, Map<byte[], Double> scoreMembers) {
    Jedis j = getShard(key);
    return j.zadd(key, scoreMembers);
  }

  @Override
  public Long zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params) {
    Jedis j = getShard(key);
    return j.zadd(key, scoreMembers, params);
  }

  @Override
  public Set<byte[]> zrange(byte[] key, long start, long end) {
    Jedis j = getShard(key);
    return j.zrange(key, start, end);
  }

  @Override
  public Long zrem(byte[] key, byte[]... members) {
    Jedis j = getShard(key);
    return j.zrem(key, members);
  }

  @Override
  public Double zincrby(byte[] key, double score, byte[] member) {
    Jedis j = getShard(key);
    return j.zincrby(key, score, member);
  }

  @Override
  public Double zincrby(byte[] key, double score, byte[] member, ZIncrByParams params) {
    Jedis j = getShard(key);
    return j.zincrby(key, score, member, params);
  }

  @Override
  public Long zrank(byte[] key, byte[] member) {
    Jedis j = getShard(key);
    return j.zrank(key, member);
  }

  @Override
  public Long zrevrank(byte[] key, byte[] member) {
    Jedis j = getShard(key);
    return j.zrevrank(key, member);
  }

  @Override
  public Set<byte[]> zrevrange(byte[] key, long start, long end) {
    Jedis j = getShard(key);
    return j.zrevrange(key, start, end);
  }

  @Override
  public Set<Tuple> zrangeWithScores(byte[] key, long start, long end) {
    Jedis j = getShard(key);
    return j.zrangeWithScores(key, start, end);
  }

  @Override
  public Set<Tuple> zrevrangeWithScores(byte[] key, long start, long end) {
    Jedis j = getShard(key);
    return j.zrevrangeWithScores(key, start, end);
  }

  @Override
  public Long zcard(byte[] key) {
    Jedis j = getShard(key);
    return j.zcard(key);
  }

  @Override
  public Double zscore(byte[] key, byte[] member) {
    Jedis j = getShard(key);
    return j.zscore(key, member);
  }

  @Override
  public List<byte[]> sort(byte[] key) {
    Jedis j = getShard(key);
    return j.sort(key);
  }

  @Override
  public List<byte[]> sort(byte[] key, SortingParams sortingParameters) {
    Jedis j = getShard(key);
    return j.sort(key, sortingParameters);
  }

  @Override
  public Long zcount(byte[] key, double min, double max) {
    Jedis j = getShard(key);
    return j.zcount(key, min, max);
  }

  @Override
  public Long zcount(byte[] key, byte[] min, byte[] max) {
    Jedis j = getShard(key);
    return j.zcount(key, min, max);
  }

  @Override
  public Set<byte[]> zrangeByScore(byte[] key, double min, double max) {
    Jedis j = getShard(key);
    return j.zrangeByScore(key, min, max);
  }

  @Override
  public Set<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
    Jedis j = getShard(key);
    return j.zrangeByScore(key, min, max, offset, count);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
    Jedis j = getShard(key);
    return j.zrangeByScoreWithScores(key, min, max);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset,
      int count) {
    Jedis j = getShard(key);
    return j.zrangeByScoreWithScores(key, min, max, offset, count);
  }

  @Override
  public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max) {
    Jedis j = getShard(key);
    return j.zrangeByScore(key, min, max);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
    Jedis j = getShard(key);
    return j.zrangeByScoreWithScores(key, min, max);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset,
      int count) {
    Jedis j = getShard(key);
    return j.zrangeByScoreWithScores(key, min, max, offset, count);
  }

  @Override
  public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
    Jedis j = getShard(key);
    return j.zrangeByScore(key, min, max, offset, count);
  }

  @Override
  public Set<byte[]> zrevrangeByScore(byte[] key, double max, double min) {
    Jedis j = getShard(key);
    return j.zrevrangeByScore(key, max, min);
  }

  @Override
  public Set<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByScore(key, max, min, offset, count);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
    Jedis j = getShard(key);
    return j.zrevrangeByScoreWithScores(key, max, min);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset,
      int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByScoreWithScores(key, max, min, offset, count);
  }

  @Override
  public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
    Jedis j = getShard(key);
    return j.zrevrangeByScore(key, max, min);
  }

  @Override
  public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByScore(key, max, min, offset, count);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
    Jedis j = getShard(key);
    return j.zrevrangeByScoreWithScores(key, max, min);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset,
      int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByScoreWithScores(key, max, min, offset, count);
  }

  @Override
  public Long zremrangeByRank(byte[] key, long start, long end) {
    Jedis j = getShard(key);
    return j.zremrangeByRank(key, start, end);
  }

  @Override
  public Long zremrangeByScore(byte[] key, double start, double end) {
    Jedis j = getShard(key);
    return j.zremrangeByScore(key, start, end);
  }

  @Override
  public Long zremrangeByScore(byte[] key, byte[] start, byte[] end) {
    Jedis j = getShard(key);
    return j.zremrangeByScore(key, start, end);
  }

  @Override
  public Long zlexcount(final byte[] key, final byte[] min, final byte[] max) {
    Jedis j = getShard(key);
    return j.zlexcount(key, min, max);
  }

  @Override
  public Set<byte[]> zrangeByLex(final byte[] key, final byte[] min, final byte[] max) {
    Jedis j = getShard(key);
    return j.zrangeByLex(key, min, max);
  }

  @Override
  public Set<byte[]> zrangeByLex(final byte[] key, final byte[] min, final byte[] max,
      final int offset, final int count) {
    Jedis j = getShard(key);
    return j.zrangeByLex(key, min, max, offset, count);
  }

  @Override
  public Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
    Jedis j = getShard(key);
    return j.zrevrangeByLex(key, max, min);
  }

  @Override
  public Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByLex(key, max, min, offset, count);
  }

  @Override
  public Long zremrangeByLex(final byte[] key, final byte[] min, final byte[] max) {
    Jedis j = getShard(key);
    return j.zremrangeByLex(key, min, max);
  }

  @Override
  public Long linsert(byte[] key, LIST_POSITION where, byte[] pivot, byte[] value) {
    Jedis j = getShard(key);
    return j.linsert(key, where, pivot, value);
  }

  public ShardedJedisPipeline pipelined() {
    ShardedJedisPipeline pipeline = new ShardedJedisPipeline();
    pipeline.setShardedJedis(this);
    return pipeline;
  }

  public Long objectRefcount(byte[] key) {
    Jedis j = getShard(key);
    return j.objectRefcount(key);
  }

  public byte[] objectEncoding(byte[] key) {
    Jedis j = getShard(key);
    return j.objectEncoding(key);
  }

  public Long objectIdletime(byte[] key) {
    Jedis j = getShard(key);
    return j.objectIdletime(key);
  }

  @Override
  public Boolean setbit(byte[] key, long offset, boolean value) {
    Jedis j = getShard(key);
    return j.setbit(key, offset, value);
  }

  @Override
  public Boolean setbit(byte[] key, long offset, byte[] value) {
    Jedis j = getShard(key);
    return j.setbit(key, offset, value);
  }

  @Override
  public Boolean getbit(byte[] key, long offset) {
    Jedis j = getShard(key);
    return j.getbit(key, offset);
  }

  @Override
  public Long setrange(byte[] key, long offset, byte[] value) {
    Jedis j = getShard(key);
    return j.setrange(key, offset, value);
  }

  @Override
  public byte[] getrange(byte[] key, long startOffset, long endOffset) {
    Jedis j = getShard(key);
    return j.getrange(key, startOffset, endOffset);
  }

  @Override
  public Long move(byte[] key, int dbIndex) {
    Jedis j = getShard(key);
    return j.move(key, dbIndex);
  }

  @Override
  public byte[] echo(byte[] arg) {
    Jedis j = getShard(arg);
    return j.echo(arg);
  }

  public List<byte[]> brpop(byte[] arg) {
    Jedis j = getShard(arg);
    return j.brpop(arg);
  }

  public List<byte[]> blpop(byte[] arg) {
    Jedis j = getShard(arg);
    return j.blpop(arg);
  }

  @Override
  public Long bitcount(byte[] key) {
    Jedis j = getShard(key);
    return j.bitcount(key);
  }

  @Override
  public Long bitcount(byte[] key, long start, long end) {
    Jedis j = getShard(key);
    return j.bitcount(key, start, end);
  }

  @Override
  public Long pfadd(final byte[] key, final byte[]... elements) {
    Jedis j = getShard(key);
    return j.pfadd(key, elements);
  }

  @Override
  public long pfcount(final byte[] key) {
    Jedis j = getShard(key);
    return j.pfcount(key);
  }

  @Override
  public Long geoadd(byte[] key, double longitude, double latitude, byte[] member) {
    Jedis j = getShard(key);
    return j.geoadd(key, longitude, latitude, member);
  }

  @Override
  public Long geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap) {
    Jedis j = getShard(key);
    return j.geoadd(key, memberCoordinateMap);
  }

  @Override
  public Double geodist(byte[] key, byte[] member1, byte[] member2) {
    Jedis j = getShard(key);
    return j.geodist(key, member1, member2);
  }

  @Override
  public Double geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit) {
    Jedis j = getShard(key);
    return j.geodist(key, member1, member2, unit);
  }

  @Override
  public List<byte[]> geohash(byte[] key, byte[]... members) {
    Jedis j = getShard(key);
    return j.geohash(key, members);
  }

  @Override
  public List<GeoCoordinate> geopos(byte[] key, byte[]... members) {
    Jedis j = getShard(key);
    return j.geopos(key, members);
  }

  @Override
  public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude,
      double radius, GeoUnit unit) {
    Jedis j = getShard(key);
    return j.georadius(key, longitude, latitude, radius, unit);
  }

  @Override
  public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude,
      double radius, GeoUnit unit, GeoRadiusParam param) {
    Jedis j = getShard(key);
    return j.georadius(key, longitude, latitude, radius, unit, param);
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius,
      GeoUnit unit) {
    Jedis j = getShard(key);
    return j.georadiusByMember(key, member, radius, unit);
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius,
      GeoUnit unit, GeoRadiusParam param) {
    Jedis j = getShard(key);
    return j.georadiusByMember(key, member, radius, unit, param);
  }

  @Override
  public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor) {
    Jedis j = getShard(key);
    return j.hscan(key, cursor);
  }

  @Override
  public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor, ScanParams params) {
    Jedis j = getShard(key);
    return j.hscan(key, cursor, params);
  }

  @Override
  public ScanResult<byte[]> sscan(byte[] key, byte[] cursor) {
    Jedis j = getShard(key);
    return j.sscan(key, cursor);
  }

  @Override
  public ScanResult<byte[]> sscan(byte[] key, byte[] cursor, ScanParams params) {
    Jedis j = getShard(key);
    return j.sscan(key, cursor, params);
  }

  @Override
  public ScanResult<Tuple> zscan(byte[] key, byte[] cursor) {
    Jedis j = getShard(key);
    return j.zscan(key, cursor);
  }

  @Override
  public ScanResult<Tuple> zscan(byte[] key, byte[] cursor, ScanParams params) {
    Jedis j = getShard(key);
    return j.zscan(key, cursor, params);
  }

  @Override
  public List<Long> bitfield(byte[] key, byte[]... arguments) {
    Jedis j = getShard(key);
    return j.bitfield(key, arguments);
 }

  @Override
  public Long hstrlen(byte[] key, byte[] field) {
    Jedis j = getShard(key);
    return j.hstrlen(key, field);
  }
  
}
