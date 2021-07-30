package redis.clients.jedis;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import redis.clients.jedis.commands.JedisCommands;
import redis.clients.jedis.commands.ProtocolCommand;
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
import redis.clients.jedis.util.Hashing;

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
  public String set(final String key, final String value) {
    Jedis j = getShard(key);
    return j.set(key, value);
  }

  @Override
  public String set(final String key, final String value, SetParams params) {
    Jedis j = getShard(key);
    return j.set(key, value, params);
  }

  @Override
  public String get(final String key) {
    Jedis j = getShard(key);
    return j.get(key);
  }

  @Override
  public String getDel(final String key) {
    Jedis j = getShard(key);
    return j.getDel(key);
  }

  @Override
  public String getEx(String key, GetExParams params) {
    Jedis j = getShard(key);
    return j.getEx(key, params);
  }

  @Override
  public String echo(final String string) {
    Jedis j = getShard(string);
    return j.echo(string);
  }

  @Override
  public boolean exists(final String key) {
    Jedis j = getShard(key);
    return j.exists(key);
  }

  @Override
  public String type(final String key) {
    Jedis j = getShard(key);
    return j.type(key);
  }

  @Override
  public byte[] dump(final String key) {
    Jedis j = getShard(key);
    return j.dump(key);
  }

  @Override
  public String restore(final String key, final long ttl, final byte[] serializedValue) {
    Jedis j = getShard(key);
    return j.restore(key, ttl, serializedValue);
  }

  @Override
  public String restoreReplace(final String key, final long ttl, final byte[] serializedValue) {
    Jedis j = getShard(key);
    return j.restoreReplace(key, ttl, serializedValue);
  }

  @Override
  public String restore(final String key, final long ttl, final byte[] serializedValue,
      final RestoreParams params) {
    Jedis j = getShard(key);
    return j.restore(key, ttl, serializedValue, params);
  }

  @Override
  public long expire(final String key, final long seconds) {
    Jedis j = getShard(key);
    return j.expire(key, seconds);
  }

  @Override
  public long pexpire(final String key, final long milliseconds) {
    Jedis j = getShard(key);
    return j.pexpire(key, milliseconds);
  }

  @Override
  public long expireAt(final String key, final long unixTime) {
    Jedis j = getShard(key);
    return j.expireAt(key, unixTime);
  }

  @Override
  public long pexpireAt(final String key, final long millisecondsTimestamp) {
    Jedis j = getShard(key);
    return j.pexpireAt(key, millisecondsTimestamp);
  }

  @Override
  public long ttl(final String key) {
    Jedis j = getShard(key);
    return j.ttl(key);
  }

  @Override
  public long pttl(final String key) {
    Jedis j = getShard(key);
    return j.pttl(key);
  }

  @Override
  public boolean setbit(final String key, final long offset, boolean value) {
    Jedis j = getShard(key);
    return j.setbit(key, offset, value);
  }

  @Override
  public Boolean setbit(final String key, final long offset, final String value) {
    Jedis j = getShard(key);
    return j.setbit(key, offset, value);
  }

  @Override
  public boolean getbit(final String key, final long offset) {
    Jedis j = getShard(key);
    return j.getbit(key, offset);
  }

  @Override
  public long setrange(final String key, final long offset, final String value) {
    Jedis j = getShard(key);
    return j.setrange(key, offset, value);
  }

  @Override
  public String getrange(final String key, final long startOffset, final long endOffset) {
    Jedis j = getShard(key);
    return j.getrange(key, startOffset, endOffset);
  }

  @Override
  public String getSet(final String key, final String value) {
    Jedis j = getShard(key);
    return j.getSet(key, value);
  }

  @Override
  public long setnx(final String key, final String value) {
    Jedis j = getShard(key);
    return j.setnx(key, value);
  }

  @Override
  public String setex(final String key, final long seconds, final String value) {
    Jedis j = getShard(key);
    return j.setex(key, seconds, value);
  }

  @Override
  public String psetex(final String key, final long milliseconds, final String value) {
    Jedis j = getShard(key);
    return j.psetex(key, milliseconds, value);
  }

  @Override
  public List<String> blpop(final int timeout, final String key) {
    Jedis j = getShard(key);
    return j.blpop(timeout, key);
  }

  @Override
  public KeyedListElement blpop(final double timeout, final String key) {
    Jedis j = getShard(key);
    return j.blpop(timeout, key);
  }

  @Override
  public List<String> brpop(final int timeout, final String key) {
    Jedis j = getShard(key);
    return j.brpop(timeout, key);
  }

  @Override
  public KeyedListElement brpop(final double timeout, final String key) {
    Jedis j = getShard(key);
    return j.brpop(timeout, key);
  }

  @Override
  public long decrBy(final String key, final long decrement) {
    Jedis j = getShard(key);
    return j.decrBy(key, decrement);
  }

  @Override
  public long decr(final String key) {
    Jedis j = getShard(key);
    return j.decr(key);
  }

  @Override
  public long incrBy(final String key, final long increment) {
    Jedis j = getShard(key);
    return j.incrBy(key, increment);
  }

  @Override
  public double incrByFloat(final String key, final double increment) {
    Jedis j = getShard(key);
    return j.incrByFloat(key, increment);
  }

  @Override
  public long incr(final String key) {
    Jedis j = getShard(key);
    return j.incr(key);
  }

  @Override
  public long append(final String key, final String value) {
    Jedis j = getShard(key);
    return j.append(key, value);
  }

  @Override
  public String substr(final String key, final int start, final int end) {
    Jedis j = getShard(key);
    return j.substr(key, start, end);
  }

  @Override
  public long hset(final String key, final String field, final String value) {
    Jedis j = getShard(key);
    return j.hset(key, field, value);
  }

  @Override
  public long hset(final String key, final Map<String, String> hash) {
    Jedis j = getShard(key);
    return j.hset(key, hash);
  }

  @Override
  public String hget(final String key, final String field) {
    Jedis j = getShard(key);
    return j.hget(key, field);
  }

  @Override
  public long hsetnx(final String key, final String field, final String value) {
    Jedis j = getShard(key);
    return j.hsetnx(key, field, value);
  }

  @Override
  public String hmset(final String key, final Map<String, String> hash) {
    Jedis j = getShard(key);
    return j.hmset(key, hash);
  }

  @Override
  public List<String> hmget(final String key, String... fields) {
    Jedis j = getShard(key);
    return j.hmget(key, fields);
  }

  @Override
  public long hincrBy(final String key, final String field, final long value) {
    Jedis j = getShard(key);
    return j.hincrBy(key, field, value);
  }

  @Override
  public double hincrByFloat(final String key, final String field, final double value) {
    Jedis j = getShard(key);
    return j.hincrByFloat(key, field, value);
  }

  @Override
  public boolean hexists(final String key, final String field) {
    Jedis j = getShard(key);
    return j.hexists(key, field);
  }

  @Override
  public long del(final String key) {
    Jedis j = getShard(key);
    return j.del(key);
  }

  @Override
  public long unlink(final String key) {
    Jedis j = getShard(key);
    return j.unlink(key);
  }

  @Override
  public long hdel(final String key, String... fields) {
    Jedis j = getShard(key);
    return j.hdel(key, fields);
  }

  @Override
  public long hlen(final String key) {
    Jedis j = getShard(key);
    return j.hlen(key);
  }

  @Override
  public Set<String> hkeys(final String key) {
    Jedis j = getShard(key);
    return j.hkeys(key);
  }

  @Override
  public List<String> hvals(final String key) {
    Jedis j = getShard(key);
    return j.hvals(key);
  }

  @Override
  public Map<String, String> hgetAll(final String key) {
    Jedis j = getShard(key);
    return j.hgetAll(key);
  }

  @Override
  public String hrandfield(final String key) {
    Jedis j = getShard(key);
    return j.hrandfield(key);
  }

  @Override
  public List<String> hrandfield(final String key, final long count) {
    Jedis j = getShard(key);
    return j.hrandfield(key, count);
  }

  @Override
  public Map<String, String> hrandfieldWithValues(final String key, final long count) {
    Jedis j = getShard(key);
    return j.hrandfieldWithValues(key, count);
  }

  @Override
  public long rpush(final String key, String... strings) {
    Jedis j = getShard(key);
    return j.rpush(key, strings);
  }

  @Override
  public long lpush(final String key, String... strings) {
    Jedis j = getShard(key);
    return j.lpush(key, strings);
  }

  @Override
  public long lpushx(final String key, String... string) {
    Jedis j = getShard(key);
    return j.lpushx(key, string);
  }

  @Override
  public long strlen(final String key) {
    Jedis j = getShard(key);
    return j.strlen(key);
  }

  public long move(final String key, final int dbIndex) {
    Jedis j = getShard(key);
    return j.move(key, dbIndex);
  }

  @Override
  public long rpushx(final String key, String... string) {
    Jedis j = getShard(key);
    return j.rpushx(key, string);
  }

  @Override
  public long persist(final String key) {
    Jedis j = getShard(key);
    return j.persist(key);
  }

  @Override
  public long llen(final String key) {
    Jedis j = getShard(key);
    return j.llen(key);
  }

  @Override
  public List<String> lrange(final String key, final long start, final long stop) {
    Jedis j = getShard(key);
    return j.lrange(key, start, stop);
  }

  @Override
  public String ltrim(final String key, final long start, final long stop) {
    Jedis j = getShard(key);
    return j.ltrim(key, start, stop);
  }

  @Override
  public String lindex(final String key, final long index) {
    Jedis j = getShard(key);
    return j.lindex(key, index);
  }

  @Override
  public String lset(final String key, final long index, final String value) {
    Jedis j = getShard(key);
    return j.lset(key, index, value);
  }

  @Override
  public long lrem(final String key, final long count, final String value) {
    Jedis j = getShard(key);
    return j.lrem(key, count, value);
  }

  @Override
  public String lpop(final String key) {
    Jedis j = getShard(key);
    return j.lpop(key);
  }

  @Override
  public List<String> lpop(final String key, final int count) {
    Jedis j = getShard(key);
    return j.lpop(key, count);
  }

  @Override
  public Long lpos(final String key, final String element) {
    Jedis j = getShard(key);
    return j.lpos(key, element);
  }

  @Override
  public Long lpos(final String key, final String element, final LPosParams params) {
    Jedis j = getShard(key);
    return j.lpos(key, element, params);
  }

  @Override
  public List<Long> lpos(final String key, final String element, final LPosParams params,
      final long count) {
    Jedis j = getShard(key);
    return j.lpos(key, element, params, count);
  }

  @Override
  public String rpop(final String key) {
    Jedis j = getShard(key);
    return j.rpop(key);
  }

  @Override
  public List<String> rpop(final String key, final int count) {
    Jedis j = getShard(key);
    return j.rpop(key, count);
  }

  @Override
  public long sadd(final String key, String... members) {
    Jedis j = getShard(key);
    return j.sadd(key, members);
  }

  @Override
  public Set<String> smembers(final String key) {
    Jedis j = getShard(key);
    return j.smembers(key);
  }

  @Override
  public long srem(final String key, String... members) {
    Jedis j = getShard(key);
    return j.srem(key, members);
  }

  @Override
  public String spop(final String key) {
    Jedis j = getShard(key);
    return j.spop(key);
  }

  @Override
  public Set<String> spop(final String key, final long count) {
    Jedis j = getShard(key);
    return j.spop(key, count);
  }

  @Override
  public long scard(final String key) {
    Jedis j = getShard(key);
    return j.scard(key);
  }

  @Override
  public boolean sismember(final String key, final String member) {
    Jedis j = getShard(key);
    return j.sismember(key, member);
  }

  @Override
  public List<Boolean> smismember(final String key, final String... members) {
    Jedis j = getShard(key);
    return j.smismember(key, members);
  }

  @Override
  public String srandmember(final String key) {
    Jedis j = getShard(key);
    return j.srandmember(key);
  }

  @Override
  public List<String> srandmember(final String key, final int count) {
    Jedis j = getShard(key);
    return j.srandmember(key, count);
  }

  @Override
  public long zadd(final String key, final double score, final String member) {
    Jedis j = getShard(key);
    return j.zadd(key, score, member);
  }

  @Override
  public long zadd(final String key, final double score, final String member,
      final ZAddParams params) {
    Jedis j = getShard(key);
    return j.zadd(key, score, member, params);
  }

  @Override
  public long zadd(final String key, final Map<String, Double> scoreMembers) {
    Jedis j = getShard(key);
    return j.zadd(key, scoreMembers);
  }

  @Override
  public long zadd(final String key, final Map<String, Double> scoreMembers, final ZAddParams params) {
    Jedis j = getShard(key);
    return j.zadd(key, scoreMembers, params);
  }

  @Override
  public Double zaddIncr(final String key, final double score, final String member, final ZAddParams params) {
    Jedis j = getShard(key);
    return j.zaddIncr(key, score, member, params);
  }

  @Override
  public Set<String> zrange(final String key, final long start, final long stop) {
    Jedis j = getShard(key);
    return j.zrange(key, start, stop);
  }

  @Override
  public long zrem(final String key, String... members) {
    Jedis j = getShard(key);
    return j.zrem(key, members);
  }

  @Override
  public double zincrby(final String key, final double increment, final String member) {
    Jedis j = getShard(key);
    return j.zincrby(key, increment, member);
  }

  @Override
  public Double zincrby(final String key, final double increment, final String member,
      ZIncrByParams params) {
    Jedis j = getShard(key);
    return j.zincrby(key, increment, member, params);
  }

  @Override
  public Long zrank(final String key, final String member) {
    Jedis j = getShard(key);
    return j.zrank(key, member);
  }

  @Override
  public Long zrevrank(final String key, final String member) {
    Jedis j = getShard(key);
    return j.zrevrank(key, member);
  }

  @Override
  public Set<String> zrevrange(final String key, final long start, final long stop) {
    Jedis j = getShard(key);
    return j.zrevrange(key, start, stop);
  }

  @Override
  public Set<Tuple> zrangeWithScores(final String key, final long start, final long stop) {
    Jedis j = getShard(key);
    return j.zrangeWithScores(key, start, stop);
  }

  @Override
  public Set<Tuple> zrevrangeWithScores(final String key, final long start, final long stop) {
    Jedis j = getShard(key);
    return j.zrevrangeWithScores(key, start, stop);
  }

  @Override
  public String zrandmember(final String key) {
    Jedis j = getShard(key);
    return j.zrandmember(key);
  }

  @Override
  public Set<String> zrandmember(final String key, final long count) {
    Jedis j = getShard(key);
    return j.zrandmember(key, count);
  }

  @Override
  public Set<Tuple> zrandmemberWithScores(final String key, final long count) {
    Jedis j = getShard(key);
    return j.zrandmemberWithScores(key, count);
  }

  @Override
  public long zcard(final String key) {
    Jedis j = getShard(key);
    return j.zcard(key);
  }

  @Override
  public Double zscore(final String key, final String member) {
    Jedis j = getShard(key);
    return j.zscore(key, member);
  }

  @Override
  public List<Double> zmscore(final String key, final String... members) {
    Jedis j = getShard(key);
    return j.zmscore(key, members);
  }

  @Override
  public Tuple zpopmax(final String key) {
    Jedis j = getShard(key);
    return j.zpopmax(key);
  }

  @Override
  public Set<Tuple> zpopmax(final String key, final int count) {
    Jedis j = getShard(key);
    return j.zpopmax(key, count);
  }

  @Override
  public Tuple zpopmin(final String key) {
    Jedis j = getShard(key);
    return j.zpopmin(key);
  }

  @Override
  public Set<Tuple> zpopmin(final String key, final int count) {
    Jedis j = getShard(key);
    return j.zpopmin(key, count);
  }

  @Override
  public List<String> sort(final String key) {
    Jedis j = getShard(key);
    return j.sort(key);
  }

  @Override
  public List<String> sort(final String key, final SortingParams sortingParameters) {
    Jedis j = getShard(key);
    return j.sort(key, sortingParameters);
  }

  @Override
  public long zcount(final String key, final double min, final double max) {
    Jedis j = getShard(key);
    return j.zcount(key, min, max);
  }

  @Override
  public long zcount(final String key, final String min, final String max) {
    Jedis j = getShard(key);
    return j.zcount(key, min, max);
  }

  @Override
  public Set<String> zrangeByScore(final String key, final double min, final double max) {
    Jedis j = getShard(key);
    return j.zrangeByScore(key, min, max);
  }

  @Override
  public Set<String> zrevrangeByScore(final String key, final double max, final double min) {
    Jedis j = getShard(key);
    return j.zrevrangeByScore(key, max, min);
  }

  @Override
  public Set<String> zrangeByScore(final String key, final double min, final double max,
      final int offset, final int count) {
    Jedis j = getShard(key);
    return j.zrangeByScore(key, min, max, offset, count);
  }

  @Override
  public Set<String> zrevrangeByScore(final String key, final double max, final double min,
      final int offset, final int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByScore(key, max, min, offset, count);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final String key, final double min, final double max) {
    Jedis j = getShard(key);
    return j.zrangeByScoreWithScores(key, min, max);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final String key, final double max, final double min) {
    Jedis j = getShard(key);
    return j.zrevrangeByScoreWithScores(key, max, min);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final String key, final double min, final double max,
      final int offset, final int count) {
    Jedis j = getShard(key);
    return j.zrangeByScoreWithScores(key, min, max, offset, count);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final String key, final double max,
      final double min, final int offset, final int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByScoreWithScores(key, max, min, offset, count);
  }

  @Override
  public Set<String> zrangeByScore(final String key, final String min, final String max) {
    Jedis j = getShard(key);
    return j.zrangeByScore(key, min, max);
  }

  @Override
  public Set<String> zrevrangeByScore(final String key, final String max, final String min) {
    Jedis j = getShard(key);
    return j.zrevrangeByScore(key, max, min);
  }

  @Override
  public Set<String> zrangeByScore(final String key, final String min, final String max,
      final int offset, final int count) {
    Jedis j = getShard(key);
    return j.zrangeByScore(key, min, max, offset, count);
  }

  @Override
  public Set<String> zrevrangeByScore(final String key, final String max, final String min,
      final int offset, final int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByScore(key, max, min, offset, count);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final String key, final String min, final String max) {
    Jedis j = getShard(key);
    return j.zrangeByScoreWithScores(key, min, max);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final String key, final String max, final String min) {
    Jedis j = getShard(key);
    return j.zrevrangeByScoreWithScores(key, max, min);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final String key, final String min, final String max,
      final int offset, final int count) {
    Jedis j = getShard(key);
    return j.zrangeByScoreWithScores(key, min, max, offset, count);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final String key, final String max,
      final String min, final int offset, final int count) {
    Jedis j = getShard(key);
    return j.zrevrangeByScoreWithScores(key, max, min, offset, count);
  }

  @Override
  public long zremrangeByRank(final String key, final long start, final long stop) {
    Jedis j = getShard(key);
    return j.zremrangeByRank(key, start, stop);
  }

  @Override
  public long zremrangeByScore(final String key, final double min, final double max) {
    Jedis j = getShard(key);
    return j.zremrangeByScore(key, min, max);
  }

  @Override
  public long zremrangeByScore(final String key, final String min, final String max) {
    Jedis j = getShard(key);
    return j.zremrangeByScore(key, min, max);
  }

  @Override
  public long zlexcount(final String key, final String min, final String max) {
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
  public Set<String> zrevrangeByLex(final String key, final String max, final String min) {
    return getShard(key).zrevrangeByLex(key, max, min);
  }

  @Override
  public Set<String> zrevrangeByLex(final String key, final String max, final String min,
      final int offset, final int count) {
    return getShard(key).zrevrangeByLex(key, max, min, offset, count);
  }

  @Override
  public long zremrangeByLex(final String key, final String min, final String max) {
    return getShard(key).zremrangeByLex(key, min, max);
  }

  @Override
  public long linsert(final String key, final ListPosition where, final String pivot,
      final String value) {
    Jedis j = getShard(key);
    return j.linsert(key, where, pivot, value);
  }

  @Override
  public long bitcount(final String key) {
    Jedis j = getShard(key);
    return j.bitcount(key);
  }

  @Override
  public long bitcount(final String key, final long start, final long end) {
    Jedis j = getShard(key);
    return j.bitcount(key, start, end);
  }

  @Override
  public long bitpos(final String key, final boolean value) {
    Jedis j = getShard(key);
    return j.bitpos(key, value);
  }

  @Override
  public long bitpos(final String key, boolean value, final BitPosParams params) {
    Jedis j = getShard(key);
    return j.bitpos(key, value, params);
  }

  @Override
  public ScanResult<Entry<String, String>> hscan(final String key, final String cursor,
      final ScanParams params) {
    Jedis j = getShard(key);
    return j.hscan(key, cursor, params);
  }

  @Override
  public ScanResult<Tuple> zscan(final String key, final String cursor, final ScanParams params) {
    Jedis j = getShard(key);
    return j.zscan(key, cursor, params);
  }

  @Override
  public ScanResult<String> sscan(final String key, final String cursor, final ScanParams params) {
    Jedis j = getShard(key);
    return j.sscan(key, cursor, params);
  }

  @Override
  public void close() {
    if (dataSource != null) {
      boolean broken = false;

      for (Jedis jedis : getAllShards()) {
        if (jedis.isBroken()) {
          broken = true;
          break;
        }
      }
      ShardedJedisPool pool = this.dataSource;
      this.dataSource = null;
      if (broken) {
        pool.returnBrokenResource(this);
      } else {
        pool.returnResource(this);
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
  public long pfadd(final String key, final String... elements) {
    Jedis j = getShard(key);
    return j.pfadd(key, elements);
  }

  @Override
  public long pfcount(final String key) {
    Jedis j = getShard(key);
    return j.pfcount(key);
  }

  @Override
  public long touch(final String key) {
    Jedis j = getShard(key);
    return j.touch(key);
  }

  @Override
  public long geoadd(final String key, final double longitude, final double latitude,
      final String member) {
    Jedis j = getShard(key);
    return j.geoadd(key, longitude, latitude, member);
  }

  @Override
  public long geoadd(final String key, final Map<String, GeoCoordinate> memberCoordinateMap) {
    Jedis j = getShard(key);
    return j.geoadd(key, memberCoordinateMap);
  }

  @Override
  public long geoadd(String key, GeoAddParams params, Map<String, GeoCoordinate> memberCoordinateMap) {
    Jedis j = getShard(key);
    return j.geoadd(key, params, memberCoordinateMap);
  }

  @Override
  public Double geodist(final String key, final String member1, final String member2) {
    Jedis j = getShard(key);
    return j.geodist(key, member1, member2);
  }

  @Override
  public Double geodist(final String key, final String member1, final String member2,
      final GeoUnit unit) {
    Jedis j = getShard(key);
    return j.geodist(key, member1, member2, unit);
  }

  @Override
  public List<String> geohash(final String key, final String... members) {
    Jedis j = getShard(key);
    return j.geohash(key, members);
  }

  @Override
  public List<GeoCoordinate> geopos(final String key, final String... members) {
    Jedis j = getShard(key);
    return j.geopos(key, members);
  }

  @Override
  public List<GeoRadiusResponse> georadius(final String key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit) {
    Jedis j = getShard(key);
    return j.georadius(key, longitude, latitude, radius, unit);
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(final String key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit) {
    Jedis j = getShard(key);
    return j.georadiusReadonly(key, longitude, latitude, radius, unit);
  }

  @Override
  public List<GeoRadiusResponse> georadius(final String key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    Jedis j = getShard(key);
    return j.georadius(key, longitude, latitude, radius, unit, param);
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(final String key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    Jedis j = getShard(key);
    return j.georadiusReadonly(key, longitude, latitude, radius, unit, param);
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(final String key, final String member,
      final double radius, final GeoUnit unit) {
    Jedis j = getShard(key);
    return j.georadiusByMember(key, member, radius, unit);
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(final String key, final String member,
      final double radius, final GeoUnit unit) {
    Jedis j = getShard(key);
    return j.georadiusByMemberReadonly(key, member, radius, unit);
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(final String key, final String member,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    Jedis j = getShard(key);
    return j.georadiusByMember(key, member, radius, unit, param);
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(final String key, final String member,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    Jedis j = getShard(key);
    return j.georadiusByMemberReadonly(key, member, radius, unit, param);
  }

  @Override
  public List<Long> bitfield(final String key, final String... arguments) {
    Jedis j = getShard(key);
    return j.bitfield(key, arguments);
  }

  @Override
  public List<Long> bitfieldReadonly(String key, final String... arguments) {
    Jedis j = getShard(key);
    return j.bitfieldReadonly(key, arguments);
  }

  @Override
  public long hstrlen(final String key, final String field) {
    Jedis j = getShard(key);
    return j.hstrlen(key, field);
  }

  @Override
  public Long memoryUsage(String key) {
    Jedis j = getShard(key);
    return  j.memoryUsage(key);
  }

  @Override
  public Long memoryUsage(String key, int samples) {
    Jedis j = getShard(key);
    return  j.memoryUsage(key, samples);
  }

  @Override
  public StreamEntryID xadd(String key, StreamEntryID id, Map<String, String> hash) {
    Jedis j = getShard(key);
    return j.xadd(key, id, hash);
  }

  @Override
  public StreamEntryID xadd(String key, StreamEntryID id, Map<String, String> hash, long maxLen,
      boolean approximateLength) {
    Jedis j = getShard(key);
    return j.xadd(key, id, hash, maxLen, approximateLength);
  }

  @Override
  public StreamEntryID xadd(final String key, final Map<String, String> hash, final XAddParams params) {
    Jedis j = getShard(key);
    return j.xadd(key, hash, params);
  }

  @Override
  public long xlen(String key) {
    Jedis j = getShard(key);
    return j.xlen(key);
  }

  @Override
  public List<StreamEntry> xrange(String key, StreamEntryID start, StreamEntryID end) {
    Jedis j = getShard(key);
    return j.xrange(key, start, end);
  }

  @Override
  public List<StreamEntry> xrange(String key, StreamEntryID start, StreamEntryID end, int count) {
    Jedis j = getShard(key);
    return j.xrange(key, start, end, count);
  }

  @Override
  public long xack(String key, String group, StreamEntryID... ids) {
    Jedis j = getShard(key);
    return j.xack(key, group, ids);
  }

  @Override
  public String xgroupCreate(String key, String consumer, StreamEntryID id, boolean makeStream) {
    Jedis j = getShard(key);
    return j.xgroupCreate(key, consumer, id, makeStream);
  }

  @Override
  public String xgroupSetID(String key, String groupname, StreamEntryID id) {
    Jedis j = getShard(key);
    return j.xgroupSetID(key, groupname, id);
  }

  @Override
  public long xgroupDestroy(String key, String groupname) {
    Jedis j = getShard(key);
    return j.xgroupDestroy(key, groupname);
  }

  @Override
  public long xgroupDelConsumer(String key, String groupname, String consumername) {
    Jedis j = getShard(key);
    return j.xgroupDelConsumer(key, groupname, consumername);
  }

  @Override
  public long xdel(String key, StreamEntryID... ids) {
    Jedis j = getShard(key);
    return j.xdel(key, ids);
  }

  @Override
  public long xtrim(String key, long maxLen, boolean approximateLength) {
    Jedis j = getShard(key);
    return j.xtrim(key, maxLen, approximateLength);
  }

  @Override
  public long xtrim(String key, XTrimParams params) {
    Jedis j = getShard(key);
    return j.xtrim(key, params);
  }

  @Override
  public List<StreamEntry> xrevrange(String key, StreamEntryID end, StreamEntryID start) {
    Jedis j = getShard(key);
    return j.xrevrange(key, end, start);
  }

  @Override
  public List<StreamEntry> xrevrange(String key, StreamEntryID end, StreamEntryID start, int count) {
    Jedis j = getShard(key);
    return j.xrevrange(key, end, start, count);
  }

  @Override
  public StreamPendingSummary xpending(String key, String groupname) {
    Jedis j = getShard(key);
    return j.xpending(key, groupname);
  }

  @Override
  public List<StreamPendingEntry> xpending(String key, String groupname, StreamEntryID start,
      StreamEntryID end, int count, String consumername) {
    Jedis j = getShard(key);
    return j.xpending(key, groupname, start, end, count, consumername);
  }

  @Override
  public List<StreamPendingEntry> xpending(String key, String groupname, XPendingParams params) {
    Jedis j = getShard(key);
    return j.xpending(key, groupname, params);
  }

  @Override
  public List<StreamEntry> xclaim(String key, String group, String consumername, long minIdleTime,
      long newIdleTime, int retries, boolean force, StreamEntryID... ids) {
    Jedis j = getShard(key);
    return j.xclaim(key, group, consumername, minIdleTime, newIdleTime, retries, force, ids);
  }

  @Override
  public List<StreamEntry> xclaim(String key, String group, String consumername, long minIdleTime,
      XClaimParams params, StreamEntryID... ids) {
    Jedis j = getShard(key);
    return j.xclaim(key, group, consumername, minIdleTime, params, ids);
  }

  @Override
  public List<StreamEntryID> xclaimJustId(String key, String group, String consumername,
      long minIdleTime, XClaimParams params, StreamEntryID... ids) {
    Jedis j = getShard(key);
    return j.xclaimJustId(key, group, consumername, minIdleTime, params, ids);
  }

  @Override
  public Map.Entry<StreamEntryID, List<StreamEntry>> xautoclaim(String key, String group, String consumerName,
      long minIdleTime, StreamEntryID start, XAutoClaimParams params) {
    Jedis j = getShard(key);
    return j.xautoclaim(key, group, consumerName, minIdleTime, start, params);
  }

  @Override
  public Map.Entry<StreamEntryID, List<StreamEntryID>> xautoclaimJustId(String key, String group, String consumerName,
      long minIdleTime, StreamEntryID start, XAutoClaimParams params) {
    Jedis j = getShard(key);
    return j.xautoclaimJustId(key, group, consumerName, minIdleTime, start, params);
  }

  @Override
  public StreamInfo xinfoStream(String key) {

    Jedis j = getShard(key);
    return j.xinfoStream(key);
  }

  @Override
  public List<StreamGroupInfo> xinfoGroup(String key) {

    Jedis j = getShard(key);
    return j.xinfoGroup(key);
  }

  @Override
  public List<StreamConsumersInfo> xinfoConsumers(String key, String group) {
    Jedis j = getShard(key);
    return j.xinfoConsumers(key, group);
  }

  public Object sendCommand(ProtocolCommand cmd, String... args) {
    // default since no sample key provided in JedisCommands interface
    String sampleKey = args.length > 0 ? args[0] : cmd.toString();
    Jedis j = getShard(sampleKey);
    return j.sendCommand(cmd, args);
  }

  public Object sendBlockingCommand(ProtocolCommand cmd, String... args) {
    // default since no sample key provided in JedisCommands interface
    String sampleKey = args.length > 0 ? args[0] : cmd.toString();
    Jedis j = getShard(sampleKey);
    return j.sendBlockingCommand(cmd, args);
  }

  @Override
  public LCSMatchResult strAlgoLCSStrings(final String strA, final String strB, final StrAlgoLCSParams params) {
    Jedis j = getShard("");
    return j.strAlgoLCSStrings(strA, strB, params);
  }
}
