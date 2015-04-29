package redis.clients.jedis;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import redis.clients.jedis.BinaryClient.LIST_POSITION;
import redis.clients.jedis.exceptions.JedisConnectionException;
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

  public String set(byte[] key, byte[] value) {
    Jedis j = getShard(key);
    return j.set(key, value);
  }

  public String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time) {
    Jedis j = getShard(key);
    return j.set(key, value, nxxx, expx, time);
  }

  public byte[] get(byte[] key) {
    Jedis j = getShard(key);
    return j.get(key);
  }

  public Boolean exists(byte[] key) {
    Jedis j = getShard(key);
    return j.exists(key);
  }

  public String type(byte[] key) {
    Jedis j = getShard(key);
    return j.type(key);
  }

  public Long expire(byte[] key, int seconds) {
    Jedis j = getShard(key);
    return j.expire(key, seconds);
  }

  public Long pexpire(byte[] key, final long milliseconds) {
    Jedis j = getShard(key);
    return j.pexpire(key, milliseconds);
  }

  @Deprecated
  public Long pexpire(String key, final long milliseconds) {
    Jedis j = getShard(key);
    return j.pexpire(key, milliseconds);
  }

  public Long expireAt(byte[] key, long unixTime) {
    Jedis j = getShard(key);
    return j.expireAt(key, unixTime);
  }

  public Long pexpireAt(byte[] key, long millisecondsTimestamp) {
    Jedis j = getShard(key);
    return j.pexpireAt(key, millisecondsTimestamp);
  }

  public Long ttl(byte[] key) {
    Jedis j = getShard(key);
    return j.ttl(key);
  }

  public byte[] getSet(byte[] key, byte[] value) {
    Jedis j = getShard(key);
    return j.getSet(key, value);
  }

  public Long setnx(byte[] key, byte[] value) {
    Jedis j = getShard(key);
    return j.setnx(key, value);
  }

  public String setex(byte[] key, int seconds, byte[] value) {
    Jedis j = getShard(key);
    return j.setex(key, seconds, value);
  }

  public Long decrBy(byte[] key, long integer) {
    Jedis j = getShard(key);
    return j.decrBy(key, integer);
  }

  public Long decr(byte[] key) {
    Jedis j = getShard(key);
    return j.decr(key);
  }

  public Long del(byte[] key) {
    Jedis j = getShard(key);
    return j.del(key);
  }

  public Long incrBy(byte[] key, long integer) {
    Jedis j = getShard(key);
    return j.incrBy(key, integer);
  }

  public Double incrByFloat(byte[] key, double integer) {
    Jedis j = getShard(key);
    return j.incrByFloat(key, integer);
  }

  public Long incr(byte[] key) {
    Jedis j = getShard(key);
    return j.incr(key);
  }

  public Long append(byte[] key, byte[] value) {
    Jedis j = getShard(key);
    return j.append(key, value);
  }

  public byte[] substr(byte[] key, int start, int end) {
    Jedis j = getShard(key);
    return j.substr(key, start, end);
  }

  public Long hset(byte[] key, byte[] field, byte[] value) {
    Jedis j = getShard(key);
    return j.hset(key, field, value);
  }

  public byte[] hget(byte[] key, byte[] field) {
    Jedis j = getShard(key);
    return j.hget(key, field);
  }

  public Long hsetnx(byte[] key, byte[] field, byte[] value) {
    Jedis j = getShard(key);
    return j.hsetnx(key, field, value);
  }

  public String hmset(byte[] key, Map<byte[], byte[]> hash) {
    Jedis j = getShard(key);
    return j.hmset(key, hash);
  }

  public List<byte[]> hmget(byte[] key, byte[]... fields) {
    Jedis j = getShard(key);
    return j.hmget(key, fields);
  }

  public Long hincrBy(byte[] key, byte[] field, long value) {
    Jedis j = getShard(key);
    return j.hincrBy(key, field, value);
  }

  public Double hincrByFloat(byte[] key, byte[] field, double value) {
    Jedis j = getShard(key);
    return j.hincrByFloat(key, field, value);
  }

  public Boolean hexists(byte[] key, byte[] field) {
    Jedis j = getShard(key);
    return j.hexists(key, field);
  }

  public Long hdel(byte[] key, byte[]... fields) {
    Jedis j = getShard(key);
    return j.hdel(key, fields);
  }

  public Long hlen(byte[] key) {
    Jedis j = getShard(key);
    return j.hlen(key);
  }

  public Set<byte[]> hkeys(byte[] key) {
    Jedis j = getShard(key);
    return j.hkeys(key);
  }

  public Collection<byte[]> hvals(byte[] key) {
    Jedis j = getShard(key);
    return j.hvals(key);
  }

  public Map<byte[], byte[]> hgetAll(byte[] key) {
    Jedis j = getShard(key);
    return j.hgetAll(key);
  }

  public Long rpush(byte[] key, byte[]... strings) {
    Jedis j = getShard(key);
    return j.rpush(key, strings);
  }

  public Long lpush(byte[] key, byte[]... strings) {
    Jedis j = getShard(key);
    return j.lpush(key, strings);
  }

  public Long strlen(final byte[] key) {
    Jedis j = getShard(key);
    return j.strlen(key);
  }

  public Long lpushx(byte[] key, byte[]... string) {
    Jedis j = getShard(key);
    return j.lpushx(key, string);
  }

  public Long persist(final byte[] key) {
    Jedis j = getShard(key);
    return j.persist(key);
  }

  public Long rpushx(byte[] key, byte[]... string) {
    Jedis j = getShard(key);
    return j.rpushx(key, string);
  }

  public Long llen(byte[] key) {
    Jedis j = getShard(key);
    return j.llen(key);
  }

  public List<byte[]> lrange(byte[] key, long start, long end) {
    Jedis j = getShard(key);
    return j.lrange(key, start, end);
  }

  public String ltrim(byte[] key, long start, long end) {
    Jedis j = getShard(key);
    return j.ltrim(key, start, end);
  }

  public byte[] lindex(byte[] key, long index) {
    Jedis j = getShard(key);
    return j.lindex(key, index);
  }

  public String lset(byte[] key, long index, byte[] value) {
    Jedis j = getShard(key);
    return j.lset(key, index, value);
  }

  public Long lrem(byte[] key, long count, byte[] value) {
    Jedis j = getShard(key);
    return j.lrem(key, count, value);
  }

  public byte[] lpop(byte[] key) {
    Jedis j = getShard(key);
    return j.lpop(key);
  }

  public byte[] rpop(byte[] key) {
    Jedis j = getShard(key);
    return j.rpop(key);
  }

  public Long sadd(byte[] key, byte[]... members) {
    Jedis j = getShard(key);
    return j.sadd(key, members);
  }

  public Set<byte[]> smembers(byte[] key) {
    Jedis j = getShard(key);
    return j.smembers(key);
  }

  public Long srem(byte[] key, byte[]... members) {
    Jedis j = getShard(key);
    return j.srem(key, members);
  }

  public byte[] spop(byte[] key) {
    Jedis j = getShard(key);
    return j.spop(key);
  }

  public Set<byte[]> spop(byte[] key, long count) {
    Jedis j = getShard(key);
    return j.spop(key, count);
  }

  public Long scard(byte[] key) {
    Jedis j = getShard(key);
    return j.scard(key);
  }

  public Boolean sismember(byte[] key, byte[] member) {
    Jedis j = getShard(key);
    return j.sismember(key, member);
  }

  public byte[] srandmember(byte[] key) {
    Jedis j = getShard(key);
    return j.srandmember(key);
  }

  @Override
  public List srandmember(byte[] key, int count) {
    Jedis j = getShard(key);
    return j.srandmember(key, count);
  }

  public Long zadd(byte[] key, double score, byte[] member) {
    Jedis j = getShard(key);
    return j.zadd(key, score, member);
  }

  public Long zadd(byte[] key, Map<byte[], Double> scoreMembers) {
    Jedis j = getShard(key);
    return j.zadd(key, scoreMembers);
  }

  public Set<byte[]> zrange(byte[] key, long start, long end) {
    Jedis j = getShard(key);
    return j.zrange(key, start, end);
  }

  public Long zrem(byte[] key, byte[]... members) {
    Jedis j = getShard(key);
    return j.zrem(key, members);
  }

  public Double zincrby(byte[] key, double score, byte[] member) {
    Jedis j = getShard(key);
    return j.zincrby(key, score, member);
  }

  public Long zrank(byte[] key, byte[] member) {
    Jedis j = getShard(key);
    return j.zrank(key, member);
  }

  public Long zrevrank(byte[] key, byte[] member) {
    Jedis j = getShard(key);
    return j.zrevrank(key, member);
  }

  public Set<byte[]> zrevrange(byte[] key, long start, long end) {
    Jedis j = getShard(key);
    return j.zrevrange(key, start, end);
  }

  public Set<Tuple> zrangeWithScores(byte[] key, long start, long end) {
    Jedis j = getShard(key);
    return j.zrangeWithScores(key, start, end);
  }

  public Set<Tuple> zrevrangeWithScores(byte[] key, long start, long end) {
    Jedis j = getShard(key);
    return j.zrevrangeWithScores(key, start, end);
  }

  public Long zcard(byte[] key) {
    Jedis j = getShard(key);
    return j.zcard(key);
  }

  public Double zscore(byte[] key, byte[] member) {
    Jedis j = getShard(key);
    return j.zscore(key, member);
  }

  public List<byte[]> sort(byte[] key) {
    Jedis j = getShard(key);
    return j.sort(key);
  }

  public List<byte[]> sort(byte[] key, SortingParams sortingParameters) {
    Jedis j = getShard(key);
    return j.sort(key, sortingParameters);
  }

  public Long zcount(byte[] key, double min, double max) {
    Jedis j = getShard(key);
    return j.zcount(key, min, max);
  }

  public Long zcount(byte[] key, byte[] min, byte[] max) {
    Jedis j = getShard(key);
    return j.zcount(key, min, max);
  }

  public Set<byte[]> zrangeByScore(byte[] key, double min, double max) {
    Jedis j = getShard(key);
    return j.zrangeByScore(key, min, max);
  }

  public Set<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
    Jedis j = getShard(key);
    return j.zrangeByScore(key, min, max, offset, count);
  }

  public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
    Jedis j = getShard(key);
    return j.zrangeByScoreWithScores(key, min, max);
  }

  public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset,
      int count) {
    Jedis j = getShard(key);
    return j.zrangeByScoreWithScores(key, min, max, offset, count);
  }

  public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max) {
    Jedis j = getShard(key);
    return j.zrangeByScore(key, min, max);
  }

  public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
    Jedis j = getShard(key);
    return j.zrangeByScoreWithScores(key, min, max);
  }

  public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset,
      int count) {
    Jedis j = getShard(key);
    return j.zrangeByScoreWithScores(key, min, max, offset, count);
  }

  public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
    Jedis j = getShard(key);
    return j.zrangeByScore(key, min, max, offset, count);
  }

  public Set<byte[]> zrevrangeByScore(byte[] key, double max, double min) {
    Jedis j = getShard(key);
    return j.zrevrangeByScore(key, max, min);
  }

  public Set<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByScore(key, max, min, offset, count);
  }

  public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
    Jedis j = getShard(key);
    return j.zrevrangeByScoreWithScores(key, max, min);
  }

  public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset,
      int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByScoreWithScores(key, max, min, offset, count);
  }

  public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
    Jedis j = getShard(key);
    return j.zrevrangeByScore(key, max, min);
  }

  public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByScore(key, max, min, offset, count);
  }

  public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
    Jedis j = getShard(key);
    return j.zrevrangeByScoreWithScores(key, max, min);
  }

  public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset,
      int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByScoreWithScores(key, max, min, offset, count);
  }

  public Long zremrangeByRank(byte[] key, long start, long end) {
    Jedis j = getShard(key);
    return j.zremrangeByRank(key, start, end);
  }

  public Long zremrangeByScore(byte[] key, double start, double end) {
    Jedis j = getShard(key);
    return j.zremrangeByScore(key, start, end);
  }

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

  public Long linsert(byte[] key, LIST_POSITION where, byte[] pivot, byte[] value) {
    Jedis j = getShard(key);
    return j.linsert(key, where, pivot, value);
  }

  @Deprecated
  /**
   * This method is deprecated due to its error prone with multi
   * and will be removed on next major release
   * You can use pipelined() instead
   * @see https://github.com/xetorthio/jedis/pull/498
   */
  public List<Object> pipelined(ShardedJedisPipeline shardedJedisPipeline) {
    shardedJedisPipeline.setShardedJedis(this);
    shardedJedisPipeline.execute();
    return shardedJedisPipeline.getResults();
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

  public Boolean setbit(byte[] key, long offset, boolean value) {
    Jedis j = getShard(key);
    return j.setbit(key, offset, value);
  }

  public Boolean setbit(byte[] key, long offset, byte[] value) {
    Jedis j = getShard(key);
    return j.setbit(key, offset, value);
  }

  public Boolean getbit(byte[] key, long offset) {
    Jedis j = getShard(key);
    return j.getbit(key, offset);
  }

  public Long setrange(byte[] key, long offset, byte[] value) {
    Jedis j = getShard(key);
    return j.setrange(key, offset, value);
  }

  public byte[] getrange(byte[] key, long startOffset, long endOffset) {
    Jedis j = getShard(key);
    return j.getrange(key, startOffset, endOffset);
  }

  public Long move(byte[] key, int dbIndex) {
    Jedis j = getShard(key);
    return j.move(key, dbIndex);
  }

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

  public Long bitcount(byte[] key) {
    Jedis j = getShard(key);
    return j.bitcount(key);
  }

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

}
