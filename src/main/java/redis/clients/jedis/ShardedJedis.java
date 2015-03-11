package redis.clients.jedis;

import java.io.Closeable;

import redis.clients.jedis.BinaryClient.LIST_POSITION;
import redis.clients.util.Hashing;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import redis.clients.util.Pool;

public class ShardedJedis extends BinaryShardedJedis implements JedisCommands, Closeable {

  protected Pool<ShardedJedis> dataSource = null;

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

  public String set(String key, String value) {
    Jedis j = getShard(key);
    return j.set(key, value);
  }

  @Override
  public String set(String key, String value, String nxxx, String expx, long time) {
    Jedis j = getShard(key);
    return j.set(key, value, nxxx, expx, time);
  }

  public String get(String key) {
    Jedis j = getShard(key);
    return j.get(key);
  }

  public String echo(String string) {
    Jedis j = getShard(string);
    return j.echo(string);
  }

  public Boolean exists(String key) {
    Jedis j = getShard(key);
    return j.exists(key);
  }

  public String type(String key) {
    Jedis j = getShard(key);
    return j.type(key);
  }

  public Long expire(String key, int seconds) {
    Jedis j = getShard(key);
    return j.expire(key, seconds);
  }

  public Long pexpire(final String key, final long milliseconds) {
    Jedis j = getShard(key);
    return j.pexpire(key, milliseconds);
  }

  public Long expireAt(String key, long unixTime) {
    Jedis j = getShard(key);
    return j.expireAt(key, unixTime);
  }

  public Long pexpireAt(String key, long millisecondsTimestamp) {
    Jedis j = getShard(key);
    return j.pexpireAt(key, millisecondsTimestamp);
  }

  public Long ttl(String key) {
    Jedis j = getShard(key);
    return j.ttl(key);
  }

  public Boolean setbit(String key, long offset, boolean value) {
    Jedis j = getShard(key);
    return j.setbit(key, offset, value);
  }

  public Boolean setbit(String key, long offset, String value) {
    Jedis j = getShard(key);
    return j.setbit(key, offset, value);
  }

  public Boolean getbit(String key, long offset) {
    Jedis j = getShard(key);
    return j.getbit(key, offset);
  }

  public Long setrange(String key, long offset, String value) {
    Jedis j = getShard(key);
    return j.setrange(key, offset, value);
  }

  public String getrange(String key, long startOffset, long endOffset) {
    Jedis j = getShard(key);
    return j.getrange(key, startOffset, endOffset);
  }

  public String getSet(String key, String value) {
    Jedis j = getShard(key);
    return j.getSet(key, value);
  }

  public Long setnx(String key, String value) {
    Jedis j = getShard(key);
    return j.setnx(key, value);
  }

  public String setex(String key, int seconds, String value) {
    Jedis j = getShard(key);
    return j.setex(key, seconds, value);
  }

  public List<String> blpop(String arg) {
    Jedis j = getShard(arg);
    return j.blpop(arg);
  }

  public List<String> blpop(int timeout, String key) {
    Jedis j = getShard(key);
    return j.blpop(timeout, key);
  }

  public List<String> brpop(String arg) {
    Jedis j = getShard(arg);
    return j.brpop(arg);
  }

  public List<String> brpop(int timeout, String key) {
    Jedis j = getShard(key);
    return j.brpop(timeout, key);
  }

  public Long decrBy(String key, long integer) {
    Jedis j = getShard(key);
    return j.decrBy(key, integer);
  }

  public Long decr(String key) {
    Jedis j = getShard(key);
    return j.decr(key);
  }

  public Long incrBy(String key, long integer) {
    Jedis j = getShard(key);
    return j.incrBy(key, integer);
  }

  public Double incrByFloat(String key, double integer) {
    Jedis j = getShard(key);
    return j.incrByFloat(key, integer);
  }

  public Long incr(String key) {
    Jedis j = getShard(key);
    return j.incr(key);
  }

  public Long append(String key, String value) {
    Jedis j = getShard(key);
    return j.append(key, value);
  }

  public String substr(String key, int start, int end) {
    Jedis j = getShard(key);
    return j.substr(key, start, end);
  }

  public Long hset(String key, String field, String value) {
    Jedis j = getShard(key);
    return j.hset(key, field, value);
  }

  public String hget(String key, String field) {
    Jedis j = getShard(key);
    return j.hget(key, field);
  }

  public Long hsetnx(String key, String field, String value) {
    Jedis j = getShard(key);
    return j.hsetnx(key, field, value);
  }

  public String hmset(String key, Map<String, String> hash) {
    Jedis j = getShard(key);
    return j.hmset(key, hash);
  }

  public List<String> hmget(String key, String... fields) {
    Jedis j = getShard(key);
    return j.hmget(key, fields);
  }

  public Long hincrBy(String key, String field, long value) {
    Jedis j = getShard(key);
    return j.hincrBy(key, field, value);
  }

  public Double hincrByFloat(String key, String field, double value) {
    Jedis j = getShard(key);
    return j.hincrByFloat(key, field, value);
  }

  public Boolean hexists(String key, String field) {
    Jedis j = getShard(key);
    return j.hexists(key, field);
  }

  public Long del(String key) {
    Jedis j = getShard(key);
    return j.del(key);
  }

  public Long hdel(String key, String... fields) {
    Jedis j = getShard(key);
    return j.hdel(key, fields);
  }

  public Long hlen(String key) {
    Jedis j = getShard(key);
    return j.hlen(key);
  }

  public Set<String> hkeys(String key) {
    Jedis j = getShard(key);
    return j.hkeys(key);
  }

  public List<String> hvals(String key) {
    Jedis j = getShard(key);
    return j.hvals(key);
  }

  public Map<String, String> hgetAll(String key) {
    Jedis j = getShard(key);
    return j.hgetAll(key);
  }

  public Long rpush(String key, String... strings) {
    Jedis j = getShard(key);
    return j.rpush(key, strings);
  }

  public Long lpush(String key, String... strings) {
    Jedis j = getShard(key);
    return j.lpush(key, strings);
  }

  public Long lpushx(String key, String... string) {
    Jedis j = getShard(key);
    return j.lpushx(key, string);
  }

  public Long strlen(final String key) {
    Jedis j = getShard(key);
    return j.strlen(key);
  }

  public Long move(String key, int dbIndex) {
    Jedis j = getShard(key);
    return j.move(key, dbIndex);
  }

  public Long rpushx(String key, String... string) {
    Jedis j = getShard(key);
    return j.rpushx(key, string);
  }

  public Long persist(final String key) {
    Jedis j = getShard(key);
    return j.persist(key);
  }

  public Long llen(String key) {
    Jedis j = getShard(key);
    return j.llen(key);
  }

  public List<String> lrange(String key, long start, long end) {
    Jedis j = getShard(key);
    return j.lrange(key, start, end);
  }

  public String ltrim(String key, long start, long end) {
    Jedis j = getShard(key);
    return j.ltrim(key, start, end);
  }

  public String lindex(String key, long index) {
    Jedis j = getShard(key);
    return j.lindex(key, index);
  }

  public String lset(String key, long index, String value) {
    Jedis j = getShard(key);
    return j.lset(key, index, value);
  }

  public Long lrem(String key, long count, String value) {
    Jedis j = getShard(key);
    return j.lrem(key, count, value);
  }

  public String lpop(String key) {
    Jedis j = getShard(key);
    return j.lpop(key);
  }

  public String rpop(String key) {
    Jedis j = getShard(key);
    return j.rpop(key);
  }

  public Long sadd(String key, String... members) {
    Jedis j = getShard(key);
    return j.sadd(key, members);
  }

  public Set<String> smembers(String key) {
    Jedis j = getShard(key);
    return j.smembers(key);
  }

  public Long srem(String key, String... members) {
    Jedis j = getShard(key);
    return j.srem(key, members);
  }

  public String spop(String key) {
    Jedis j = getShard(key);
    return j.spop(key);
  }

  public Set<String> spop(String key, long count) {
    Jedis j = getShard(key);
    return j.spop(key, count);
  }

  public Long scard(String key) {
    Jedis j = getShard(key);
    return j.scard(key);
  }

  public Boolean sismember(String key, String member) {
    Jedis j = getShard(key);
    return j.sismember(key, member);
  }

  public String srandmember(String key) {
    Jedis j = getShard(key);
    return j.srandmember(key);
  }

  @Override
  public List<String> srandmember(String key, int count) {
    Jedis j = getShard(key);
    return j.srandmember(key, count);
  }

  public Long zadd(String key, double score, String member) {
    Jedis j = getShard(key);
    return j.zadd(key, score, member);
  }

  public Long zadd(String key, Map<String, Double> scoreMembers) {
    Jedis j = getShard(key);
    return j.zadd(key, scoreMembers);
  }

  public Set<String> zrange(String key, long start, long end) {
    Jedis j = getShard(key);
    return j.zrange(key, start, end);
  }

  public Long zrem(String key, String... members) {
    Jedis j = getShard(key);
    return j.zrem(key, members);
  }

  public Double zincrby(String key, double score, String member) {
    Jedis j = getShard(key);
    return j.zincrby(key, score, member);
  }

  public Long zrank(String key, String member) {
    Jedis j = getShard(key);
    return j.zrank(key, member);
  }

  public Long zrevrank(String key, String member) {
    Jedis j = getShard(key);
    return j.zrevrank(key, member);
  }

  public Set<String> zrevrange(String key, long start, long end) {
    Jedis j = getShard(key);
    return j.zrevrange(key, start, end);
  }

  public Set<Tuple> zrangeWithScores(String key, long start, long end) {
    Jedis j = getShard(key);
    return j.zrangeWithScores(key, start, end);
  }

  public Set<Tuple> zrevrangeWithScores(String key, long start, long end) {
    Jedis j = getShard(key);
    return j.zrevrangeWithScores(key, start, end);
  }

  public Long zcard(String key) {
    Jedis j = getShard(key);
    return j.zcard(key);
  }

  public Double zscore(String key, String member) {
    Jedis j = getShard(key);
    return j.zscore(key, member);
  }

  public List<String> sort(String key) {
    Jedis j = getShard(key);
    return j.sort(key);
  }

  public List<String> sort(String key, SortingParams sortingParameters) {
    Jedis j = getShard(key);
    return j.sort(key, sortingParameters);
  }

  public Long zcount(String key, double min, double max) {
    Jedis j = getShard(key);
    return j.zcount(key, min, max);
  }

  public Long zcount(String key, String min, String max) {
    Jedis j = getShard(key);
    return j.zcount(key, min, max);
  }

  public Set<String> zrangeByScore(String key, double min, double max) {
    Jedis j = getShard(key);
    return j.zrangeByScore(key, min, max);
  }

  public Set<String> zrevrangeByScore(String key, double max, double min) {
    Jedis j = getShard(key);
    return j.zrevrangeByScore(key, max, min);
  }

  public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
    Jedis j = getShard(key);
    return j.zrangeByScore(key, min, max, offset, count);
  }

  public Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByScore(key, max, min, offset, count);
  }

  public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
    Jedis j = getShard(key);
    return j.zrangeByScoreWithScores(key, min, max);
  }

  public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
    Jedis j = getShard(key);
    return j.zrevrangeByScoreWithScores(key, max, min);
  }

  public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset,
      int count) {
    Jedis j = getShard(key);
    return j.zrangeByScoreWithScores(key, min, max, offset, count);
  }

  public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset,
      int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByScoreWithScores(key, max, min, offset, count);
  }

  public Set<String> zrangeByScore(String key, String min, String max) {
    Jedis j = getShard(key);
    return j.zrangeByScore(key, min, max);
  }

  public Set<String> zrevrangeByScore(String key, String max, String min) {
    Jedis j = getShard(key);
    return j.zrevrangeByScore(key, max, min);
  }

  public Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
    Jedis j = getShard(key);
    return j.zrangeByScore(key, min, max, offset, count);
  }

  public Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByScore(key, max, min, offset, count);
  }

  public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
    Jedis j = getShard(key);
    return j.zrangeByScoreWithScores(key, min, max);
  }

  public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
    Jedis j = getShard(key);
    return j.zrevrangeByScoreWithScores(key, max, min);
  }

  public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset,
      int count) {
    Jedis j = getShard(key);
    return j.zrangeByScoreWithScores(key, min, max, offset, count);
  }

  public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset,
      int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByScoreWithScores(key, max, min, offset, count);
  }

  public Long zremrangeByRank(String key, long start, long end) {
    Jedis j = getShard(key);
    return j.zremrangeByRank(key, start, end);
  }

  public Long zremrangeByScore(String key, double start, double end) {
    Jedis j = getShard(key);
    return j.zremrangeByScore(key, start, end);
  }

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

  public Long linsert(String key, LIST_POSITION where, String pivot, String value) {
    Jedis j = getShard(key);
    return j.linsert(key, where, pivot, value);
  }

  public Long bitcount(final String key) {
    Jedis j = getShard(key);
    return j.bitcount(key);
  }

  public Long bitcount(final String key, long start, long end) {
    Jedis j = getShard(key);
    return j.bitcount(key, start, end);
  }

  @Deprecated
  /**
   * This method is deprecated due to bug (scan cursor should be unsigned long)
   * And will be removed on next major release
   * @see https://github.com/xetorthio/jedis/issues/531 
   */
  public ScanResult<Entry<String, String>> hscan(String key, int cursor) {
    Jedis j = getShard(key);
    return j.hscan(key, cursor);
  }

  @Deprecated
  /**
   * This method is deprecated due to bug (scan cursor should be unsigned long)
   * And will be removed on next major release
   * @see https://github.com/xetorthio/jedis/issues/531 
   */
  public ScanResult<String> sscan(String key, int cursor) {
    Jedis j = getShard(key);
    return j.sscan(key, cursor);
  }

  @Deprecated
  /**
   * This method is deprecated due to bug (scan cursor should be unsigned long)
   * And will be removed on next major release
   * @see https://github.com/xetorthio/jedis/issues/531 
   */
  public ScanResult<Tuple> zscan(String key, int cursor) {
    Jedis j = getShard(key);
    return j.zscan(key, cursor);
  }

  public ScanResult<Entry<String, String>> hscan(String key, final String cursor) {
    Jedis j = getShard(key);
    return j.hscan(key, cursor);
  }

  public ScanResult<String> sscan(String key, final String cursor) {
    Jedis j = getShard(key);
    return j.sscan(key, cursor);
  }

  public ScanResult<Tuple> zscan(String key, final String cursor) {
    Jedis j = getShard(key);
    return j.zscan(key, cursor);
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

  public void setDataSource(Pool<ShardedJedis> shardedJedisPool) {
    this.dataSource = shardedJedisPool;
  }

  public void resetState() {
    for (Jedis jedis : getAllShards()) {
      jedis.resetState();
    }
  }

  public Long pfadd(String key, String... elements) {
    Jedis j = getShard(key);
    return j.pfadd(key, elements);
  }

  @Override
  public long pfcount(String key) {
    Jedis j = getShard(key);
    return j.pfcount(key);
  }
}
