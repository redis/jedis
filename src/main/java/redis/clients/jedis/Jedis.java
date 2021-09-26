package redis.clients.jedis;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.args.BitPosParams;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.args.ListPosition;
import redis.clients.jedis.commands.AllKeyCommands;
import redis.clients.jedis.params.*;
import redis.clients.jedis.providers.JedisClusterConnectionProvider;
import redis.clients.jedis.providers.JedisConnectionProvider;
import redis.clients.jedis.providers.SimpleJedisConnectionProvider;
import redis.clients.jedis.resps.*;
import redis.clients.jedis.stream.*;

public class Jedis implements AllKeyCommands, AutoCloseable {

  protected final JedisCommandExecutor executor;
  private final RedisCommandObjects commandObjects;

  public Jedis() {
    this(new HostAndPort(Protocol.DEFAULT_HOST, Protocol.DEFAULT_PORT));
  }

  public Jedis(HostAndPort hostAndPort) {
    this(new SimpleJedisConnectionProvider(hostAndPort));
  }

  public Jedis(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
    this(new SimpleJedisConnectionProvider(hostAndPort, clientConfig));
  }

  public Jedis(JedisConnectionProvider provider) {
    this.executor = new SimpleJedisExecutor(provider);
    this.commandObjects = (provider instanceof JedisClusterConnectionProvider)
        ? new RedisClusterCommandObjects() : new RedisCommandObjects();
  }

  public Jedis(JedisClusterConnectionProvider provider, int maxAttempts, Duration maxTotalRetriesDuration) {
    if (provider instanceof JedisClusterConnectionProvider) {
      this.executor = new ClusterCommandExecutor(provider, maxAttempts, maxTotalRetriesDuration);
      this.commandObjects = new RedisClusterCommandObjects();
    } else {
      this.executor = new RetryableCommandExecutor(provider, maxAttempts, maxTotalRetriesDuration);
      this.commandObjects = new RedisCommandObjects();
    }
  }

  @Override
  public void close() throws Exception {
    this.executor.close();
  }

  protected final <T> T executeCommand(CommandObject<T> commandObject) {
    return executor.executeCommand(commandObject);
  }

  @Override
  public boolean exists(String key) {
    return executeCommand(commandObjects.exists(key));
  }

  @Override
  public long persist(String key) {
    return executeCommand(commandObjects.persist(key));
  }

  @Override
  public String type(String key) {
    return executeCommand(commandObjects.type(key));
  }

  @Override
  public byte[] dump(String key) {
    return executeCommand(commandObjects.dump(key));
  }

  @Override
  public String restore(String key, long ttl, byte[] serializedValue) {
    return executeCommand(commandObjects.restore(key, ttl, serializedValue));
  }

  @Override
  public String restore(String key, long ttl, byte[] serializedValue, RestoreParams params) {
    return executeCommand(commandObjects.restore(key, ttl, serializedValue, params));
  }

  @Override
  public long expire(String key, long seconds) {
    return executeCommand(commandObjects.expire(key, seconds));
  }

  @Override
  public long pexpire(String key, long milliseconds) {
    return executeCommand(commandObjects.pexpire(key, milliseconds));
  }

  @Override
  public long expireAt(String key, long unixTime) {
    return executeCommand(commandObjects.expireAt(key, unixTime));
  }

  @Override
  public long pexpireAt(String key, long millisecondsTimestamp) {
    return executeCommand(commandObjects.pexpireAt(key, millisecondsTimestamp));
  }

  @Override
  public long ttl(String key) {
    return executeCommand(commandObjects.ttl(key));
  }

  @Override
  public long pttl(String key) {
    return executeCommand(commandObjects.pttl(key));
  }

  @Override
  public long touch(String key) {
    return executeCommand(commandObjects.touch(key));
  }

  @Override
  public List<String> sort(String key) {
    return executeCommand(commandObjects.sort(key));
  }

  @Override
  public List<String> sort(String key, SortingParams sortingParameters) {
    return executeCommand(commandObjects.sort(key, sortingParameters));
  }

  @Override
  public long del(String key) {
    return executeCommand(commandObjects.del(key));
  }

  @Override
  public long unlink(String key) {
    return executeCommand(commandObjects.unlink(key));
  }

  @Override
  public Long memoryUsage(String key) {
    return executeCommand(commandObjects.memoryUsage(key));
  }

  @Override
  public Long memoryUsage(String key, int samples) {
    return executeCommand(commandObjects.memoryUsage(key, samples));
  }

  @Override
  public String set(String key, String value) {
    return executeCommand(commandObjects.set(key, value));
  }

  @Override
  public String set(String key, String value, SetParams params) {
    return executeCommand(commandObjects.set(key, value, params));
  }

  @Override
  public String get(String key) {
    return executeCommand(commandObjects.get(key));
  }

  @Override
  public String getDel(String key) {
    return executeCommand(commandObjects.getDel(key));
  }

  @Override
  public String getEx(String key, GetExParams params) {
    return executeCommand(commandObjects.getEx(key, params));
  }

  @Override
  public boolean setbit(String key, long offset, boolean value) {
    return executeCommand(commandObjects.setbit(key, offset, value));
  }

  @Override
  public boolean getbit(String key, long offset) {
    return executeCommand(commandObjects.getbit(key, offset));
  }

  @Override
  public long setrange(String key, long offset, String value) {
    return executeCommand(commandObjects.setrange(key, offset, value));
  }

  @Override
  public String getrange(String key, long startOffset, long endOffset) {
    return executeCommand(commandObjects.getrange(key, startOffset, endOffset));
  }

  @Override
  public String getSet(String key, String value) {
    return executeCommand(commandObjects.getSet(key, value));
  }

  @Override
  public long setnx(String key, String value) {
    return executeCommand(commandObjects.setnx(key, value));
  }

  @Override
  public String setex(String key, long seconds, String value) {
    return executeCommand(commandObjects.setex(key, seconds, value));
  }

  @Override
  public String psetex(String key, long milliseconds, String value) {
    return executeCommand(commandObjects.psetex(key, milliseconds, value));
  }

  @Override
  public long decrBy(String key, long decrement) {
    return executeCommand(commandObjects.decrBy(key, decrement));
  }

  @Override
  public long decr(String key) {
    return executeCommand(commandObjects.decr(key));
  }

  @Override
  public long incrBy(String key, long increment) {
    return executeCommand(commandObjects.incrBy(key, increment));
  }

  @Override
  public double incrByFloat(String key, double increment) {
    return executeCommand(commandObjects.incrByFloat(key, increment));
  }

  @Override
  public long incr(String key) {
    return executeCommand(commandObjects.incr(key));
  }

  @Override
  public long append(String key, String value) {
    return executeCommand(commandObjects.append(key, value));
  }

  @Override
  public String substr(String key, int start, int end) {
    return executeCommand(commandObjects.substr(key, start, end));
  }

  @Override
  public long strlen(String key) {
    return executeCommand(commandObjects.strlen(key));
  }

  @Override
  public long bitcount(String key) {
    return executeCommand(commandObjects.bitcount(key));
  }

  @Override
  public long bitcount(String key, long start, long end) {
    return executeCommand(commandObjects.bitcount(key, start, end));
  }

  @Override
  public long bitpos(String key, boolean value) {
    return executeCommand(commandObjects.bitpos(key, value));
  }

  @Override
  public long bitpos(String key, boolean value, BitPosParams params) {
    return executeCommand(commandObjects.bitpos(key, value, params));
  }

  @Override
  public List<Long> bitfield(String key, String... arguments) {
    return executeCommand(commandObjects.bitfield(key, arguments));
  }

  @Override
  public List<Long> bitfieldReadonly(String key, String... arguments) {
    return executeCommand(commandObjects.bitfieldReadonly(key, arguments));
  }

  @Override
  public LCSMatchResult strAlgoLCSStrings(String strA, String strB, StrAlgoLCSParams params) {
    return executeCommand(commandObjects.strAlgoLCSStrings(strA, strB, params));
  }

  @Override
  public long rpush(String key, String... string) {
    return executeCommand(commandObjects.rpush(key, string));
  }

  @Override
  public long lpush(String key, String... string) {
    return executeCommand(commandObjects.lpush(key, string));
  }

  @Override
  public long llen(String key) {
    return executeCommand(commandObjects.llen(key));
  }

  @Override
  public List<String> lrange(String key, long start, long stop) {
    return executeCommand(commandObjects.lrange(key, start, stop));
  }

  @Override
  public String ltrim(String key, long start, long stop) {
    return executeCommand(commandObjects.ltrim(key, start, stop));
  }

  @Override
  public String lindex(String key, long index) {
    return executeCommand(commandObjects.lindex(key, index));
  }

  @Override
  public String lset(String key, long index, String value) {
    return executeCommand(commandObjects.lset(key, index, value));
  }

  @Override
  public long lrem(String key, long count, String value) {
    return executeCommand(commandObjects.lrem(key, count, value));
  }

  @Override
  public String lpop(String key) {
    return executeCommand(commandObjects.lpop(key));
  }

  @Override
  public List<String> lpop(String key, int count) {
    return executeCommand(commandObjects.lpop(key, count));
  }

  @Override
  public Long lpos(String key, String element) {
    return executeCommand(commandObjects.lpos(key, element));
  }

  @Override
  public Long lpos(String key, String element, LPosParams params) {
    return executeCommand(commandObjects.lpos(key, element, params));
  }

  @Override
  public List<Long> lpos(String key, String element, LPosParams params, long count) {
    return executeCommand(commandObjects.lpos(key, element, params, count));
  }

  @Override
  public String rpop(String key) {
    return executeCommand(commandObjects.rpop(key));
  }

  @Override
  public List<String> rpop(String key, int count) {
    return executeCommand(commandObjects.rpop(key, count));
  }

  @Override
  public long linsert(String key, ListPosition where, String pivot, String value) {
    return executeCommand(commandObjects.linsert(key, where, pivot, value));
  }

  @Override
  public long lpushx(String key, String... string) {
    return executeCommand(commandObjects.lpushx(key, string));
  }

  @Override
  public long rpushx(String key, String... string) {
    return executeCommand(commandObjects.rpushx(key, string));
  }

  @Override
  public List<String> blpop(int timeout, String key) {
    return executeCommand(commandObjects.blpop(timeout, key));
  }

  @Override
  public KeyedListElement blpop(double timeout, String key) {
    return executeCommand(commandObjects.blpop(timeout, key));
  }

  @Override
  public List<String> brpop(int timeout, String key) {
    return executeCommand(commandObjects.brpop(timeout, key));
  }

  @Override
  public KeyedListElement brpop(double timeout, String key) {
    return executeCommand(commandObjects.brpop(timeout, key));
  }

  @Override
  public long hset(String key, String field, String value) {
    return executeCommand(commandObjects.hset(key, field, value));
  }

  @Override
  public long hset(String key, Map<String, String> hash) {
    return executeCommand(commandObjects.hset(key, hash));
  }

  @Override
  public String hget(String key, String field) {
    return executeCommand(commandObjects.hget(key, field));
  }

  @Override
  public long hsetnx(String key, String field, String value) {
    return executeCommand(commandObjects.hsetnx(key, field, value));
  }

  @Override
  public String hmset(String key, Map<String, String> hash) {
    return executeCommand(commandObjects.hmset(key, hash));
  }

  @Override
  public List<String> hmget(String key, String... fields) {
    return executeCommand(commandObjects.hmget(key, fields));
  }

  @Override
  public long hincrBy(String key, String field, long value) {
    return executeCommand(commandObjects.hincrBy(key, field, value));
  }

  @Override
  public double hincrByFloat(String key, String field, double value) {
    return executeCommand(commandObjects.hincrByFloat(key, field, value));
  }

  @Override
  public boolean hexists(String key, String field) {
    return executeCommand(commandObjects.hexists(key, field));
  }

  @Override
  public long hdel(String key, String... field) {
    return executeCommand(commandObjects.hdel(key, field));
  }

  @Override
  public long hlen(String key) {
    return executeCommand(commandObjects.hlen(key));
  }

  @Override
  public Set<String> hkeys(String key) {
    return executeCommand(commandObjects.hkeys(key));
  }

  @Override
  public List<String> hvals(String key) {
    return executeCommand(commandObjects.hvals(key));
  }

  @Override
  public Map<String, String> hgetAll(String key) {
    return executeCommand(commandObjects.hgetAll(key));
  }

  @Override
  public String hrandfield(String key) {
    return executeCommand(commandObjects.hrandfield(key));
  }

  @Override
  public List<String> hrandfield(String key, long count) {
    return executeCommand(commandObjects.hrandfield(key, count));
  }

  @Override
  public Map<String, String> hrandfieldWithValues(String key, long count) {
    return executeCommand(commandObjects.hrandfieldWithValues(key, count));
  }

  @Override
  public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
    return executeCommand(commandObjects.hscan(key, cursor, params));
  }

  @Override
  public long hstrlen(String key, String field) {
    return executeCommand(commandObjects.hstrlen(key, field));
  }

  @Override
  public long sadd(String key, String... member) {
    return executeCommand(commandObjects.sadd(key, member));
  }

  @Override
  public Set<String> smembers(String key) {
    return executeCommand(commandObjects.smembers(key));
  }

  @Override
  public long srem(String key, String... member) {
    return executeCommand(commandObjects.srem(key, member));
  }

  @Override
  public String spop(String key) {
    return executeCommand(commandObjects.spop(key));
  }

  @Override
  public Set<String> spop(String key, long count) {
    return executeCommand(commandObjects.spop(key, count));
  }

  @Override
  public long scard(String key) {
    return executeCommand(commandObjects.scard(key));
  }

  @Override
  public boolean sismember(String key, String member) {
    return executeCommand(commandObjects.sismember(key, member));
  }

  @Override
  public List<Boolean> smismember(String key, String... members) {
    return executeCommand(commandObjects.smismember(key, members));
  }

  @Override
  public String srandmember(String key) {
    return executeCommand(commandObjects.srandmember(key));
  }

  @Override
  public List<String> srandmember(String key, int count) {
    return executeCommand(commandObjects.srandmember(key, count));
  }

  @Override
  public ScanResult<String> sscan(String key, String cursor, ScanParams params) {
    return executeCommand(commandObjects.sscan(key, cursor, params));
  }

  @Override
  public long zadd(String key, double score, String member) {
    return executeCommand(commandObjects.zadd(key, score, member));
  }

  @Override
  public long zadd(String key, double score, String member, ZAddParams params) {
    return executeCommand(commandObjects.zadd(key, score, member, params));
  }

  @Override
  public long zadd(String key, Map<String, Double> scoreMembers) {
    return executeCommand(commandObjects.zadd(key, scoreMembers));
  }

  @Override
  public long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
    return executeCommand(commandObjects.zadd(key, scoreMembers, params));
  }

  @Override
  public Double zaddIncr(String key, double score, String member, ZAddParams params) {
    return executeCommand(commandObjects.zaddIncr(key, score, member, params));
  }

  @Override
  public Set<String> zrange(String key, long start, long stop) {
    return executeCommand(commandObjects.zrange(key, start, stop));
  }

  @Override
  public long zrem(String key, String... members) {
    return executeCommand(commandObjects.zrem(key, members));
  }

  @Override
  public double zincrby(String key, double increment, String member) {
    return executeCommand(commandObjects.zincrby(key, increment, member));
  }

  @Override
  public Double zincrby(String key, double increment, String member, ZIncrByParams params) {
    return executeCommand(commandObjects.zincrby(key, increment, member, params));
  }

  @Override
  public Long zrank(String key, String member) {
    return executeCommand(commandObjects.zrank(key, member));
  }

  @Override
  public Long zrevrank(String key, String member) {
    return executeCommand(commandObjects.zrevrank(key, member));
  }

  @Override
  public Set<String> zrevrange(String key, long start, long stop) {
    return executeCommand(commandObjects.zrevrange(key, start, stop));
  }

  @Override
  public Set<Tuple> zrangeWithScores(String key, long start, long stop) {
    return executeCommand(commandObjects.zrangeWithScores(key, start, stop));
  }

  @Override
  public Set<Tuple> zrevrangeWithScores(String key, long start, long stop) {
    return executeCommand(commandObjects.zrevrangeWithScores(key, start, stop));
  }

  @Override
  public String zrandmember(String key) {
    return executeCommand(commandObjects.zrandmember(key));
  }

  @Override
  public Set<String> zrandmember(String key, long count) {
    return executeCommand(commandObjects.zrandmember(key, count));
  }

  @Override
  public Set<Tuple> zrandmemberWithScores(String key, long count) {
    return executeCommand(commandObjects.zrandmemberWithScores(key, count));
  }

  @Override
  public long zcard(String key) {
    return executeCommand(commandObjects.zcard(key));
  }

  @Override
  public Double zscore(String key, String member) {
    return executeCommand(commandObjects.zscore(key, member));
  }

  @Override
  public List<Double> zmscore(String key, String... members) {
    return executeCommand(commandObjects.zmscore(key, members));
  }

  @Override
  public Tuple zpopmax(String key) {
    return executeCommand(commandObjects.zpopmax(key));
  }

  @Override
  public Set<Tuple> zpopmax(String key, int count) {
    return executeCommand(commandObjects.zpopmax(key, count));
  }

  @Override
  public Tuple zpopmin(String key) {
    return executeCommand(commandObjects.zpopmin(key));
  }

  @Override
  public Set<Tuple> zpopmin(String key, int count) {
    return executeCommand(commandObjects.zpopmin(key, count));
  }

  @Override
  public long zcount(String key, double min, double max) {
    return executeCommand(commandObjects.zcount(key, min, max));
  }

  @Override
  public long zcount(String key, String min, String max) {
    return executeCommand(commandObjects.zcount(key, min, max));
  }

  @Override
  public Set<String> zrangeByScore(String key, double min, double max) {
    return executeCommand(commandObjects.zrangeByScore(key, min, max));
  }

  @Override
  public Set<String> zrangeByScore(String key, String min, String max) {
    return executeCommand(commandObjects.zrangeByScore(key, min, max));
  }

  @Override
  public Set<String> zrevrangeByScore(String key, double max, double min) {
    return executeCommand(commandObjects.zrevrangeByScore(key, max, min));
  }

  @Override
  public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByScore(key, min, max, offset, count));
  }

  @Override
  public Set<String> zrevrangeByScore(String key, String max, String min) {
    return executeCommand(commandObjects.zrevrangeByScore(key, max, min));
  }

  @Override
  public Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByScore(key, min, max, offset, count));
  }

  @Override
  public Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
    return executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
    return executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
    return executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
    return executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public long zremrangeByRank(String key, long start, long stop) {
    return executeCommand(commandObjects.zremrangeByRank(key, start, stop));
  }

  @Override
  public long zremrangeByScore(String key, double min, double max) {
    return executeCommand(commandObjects.zremrangeByScore(key, min, max));
  }

  @Override
  public long zremrangeByScore(String key, String min, String max) {
    return executeCommand(commandObjects.zremrangeByScore(key, min, max));
  }

  @Override
  public long zlexcount(String key, String min, String max) {
    return executeCommand(commandObjects.zlexcount(key, min, max));
  }

  @Override
  public Set<String> zrangeByLex(String key, String min, String max) {
    return executeCommand(commandObjects.zrangeByLex(key, min, max));
  }

  @Override
  public Set<String> zrangeByLex(String key, String min, String max, int offset, int count) {
    return executeCommand(commandObjects.zrangeByLex(key, min, max, offset, count));
  }

  @Override
  public Set<String> zrevrangeByLex(String key, String max, String min) {
    return executeCommand(commandObjects.zrevrangeByLex(key, max, min));
  }

  @Override
  public Set<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
    return executeCommand(commandObjects.zrevrangeByLex(key, max, min, offset, count));
  }

  @Override
  public long zremrangeByLex(String key, String min, String max) {
    return executeCommand(commandObjects.zremrangeByLex(key, min, max));
  }

  @Override
  public ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
    return executeCommand(commandObjects.zscan(key, cursor, params));
  }

  @Override
  public StreamEntryID xadd(String key, StreamEntryID id, Map<String, String> hash) {
    return executeCommand(commandObjects.xadd(key, id, hash));
  }

  @Override
  public StreamEntryID xadd(String key, StreamEntryID id, Map<String, String> hash, long maxLen, boolean approximateLength) {
    return executeCommand(commandObjects.xadd(key, id, hash, maxLen, approximateLength));
  }

  @Override
  public StreamEntryID xadd(String key, Map<String, String> hash, XAddParams params) {
    return executeCommand(commandObjects.xadd(key, hash, params));
  }

  @Override
  public long xlen(String key) {
    return executeCommand(commandObjects.xlen(key));
  }

  @Override
  public List<StreamEntry> xrange(String key, StreamEntryID start, StreamEntryID end) {
    return executeCommand(commandObjects.xrange(key, start, end));
  }

  @Override
  public List<StreamEntry> xrange(String key, StreamEntryID start, StreamEntryID end, int count) {
    return executeCommand(commandObjects.xrange(key, start, end, count));
  }

  @Override
  public List<StreamEntry> xrevrange(String key, StreamEntryID end, StreamEntryID start) {
    return executeCommand(commandObjects.xrevrange(key, end, start));
  }

  @Override
  public List<StreamEntry> xrevrange(String key, StreamEntryID end, StreamEntryID start, int count) {
    return executeCommand(commandObjects.xrevrange(key, end, start, count));
  }

  @Override
  public long xack(String key, String group, StreamEntryID... ids) {
    return executeCommand(commandObjects.xack(key, group, ids));
  }

  @Override
  public String xgroupCreate(String key, String groupname, StreamEntryID id, boolean makeStream) {
    return executeCommand(commandObjects.xgroupCreate(key, groupname, id, makeStream));
  }

  @Override
  public String xgroupSetID(String key, String groupname, StreamEntryID id) {
    return executeCommand(commandObjects.xgroupSetID(key, groupname, id));
  }

  @Override
  public long xgroupDestroy(String key, String groupname) {
    return executeCommand(commandObjects.xgroupDestroy(key, groupname));
  }

  @Override
  public long xgroupDelConsumer(String key, String groupname, String consumername) {
    return executeCommand(commandObjects.xgroupDelConsumer(key, groupname, consumername));
  }

  @Override
  public StreamPendingSummary xpending(String key, String groupname) {
    return executeCommand(commandObjects.xpending(key, groupname));
  }

  @Override
  public List<StreamPendingEntry> xpending(String key, String groupname, StreamEntryID start, StreamEntryID end, int count, String consumername) {
    return executeCommand(commandObjects.xpending(key, groupname, start, end, count, consumername));
  }

  @Override
  public List<StreamPendingEntry> xpending(String key, String groupname, XPendingParams params) {
    return executeCommand(commandObjects.xpending(key, groupname, params));
  }

  @Override
  public long xdel(String key, StreamEntryID... ids) {
    return executeCommand(commandObjects.xdel(key, ids));
  }

  @Override
  public long xtrim(String key, long maxLen, boolean approximate) {
    return executeCommand(commandObjects.xtrim(key, maxLen, approximate));
  }

  @Override
  public long xtrim(String key, XTrimParams params) {
    return executeCommand(commandObjects.xtrim(key, params));
  }

  @Override
  public List<StreamEntry> xclaim(String key, String group, String consumername, long minIdleTime, long newIdleTime, int retries, boolean force, StreamEntryID... ids) {
    return executeCommand(commandObjects.xclaim(key, group, consumername, minIdleTime, newIdleTime, retries, force, ids));
  }

  @Override
  public List<StreamEntry> xclaim(String key, String group, String consumername, long minIdleTime, XClaimParams params, StreamEntryID... ids) {
    return executeCommand(commandObjects.xclaim(key, group, consumername, minIdleTime, params, ids));
  }

  @Override
  public List<StreamEntryID> xclaimJustId(String key, String group, String consumername, long minIdleTime, XClaimParams params, StreamEntryID... ids) {
    return executeCommand(commandObjects.xclaimJustId(key, group, consumername, minIdleTime, params, ids));
  }

  @Override
  public Map.Entry<StreamEntryID, List<StreamEntry>> xautoclaim(String key, String group, String consumerName, long minIdleTime, StreamEntryID start, XAutoClaimParams params) {
    return executeCommand(commandObjects.xautoclaim(key, group, consumerName, minIdleTime, start, params));
  }

  @Override
  public Map.Entry<StreamEntryID, List<StreamEntryID>> xautoclaimJustId(String key, String group, String consumerName, long minIdleTime, StreamEntryID start, XAutoClaimParams params) {
    return executeCommand(commandObjects.xautoclaimJustId(key, group, consumerName, minIdleTime, start, params));
  }

  @Override
  public StreamInfo xinfoStream(String key) {
    return executeCommand(commandObjects.xinfoStream(key));
  }

  @Override
  public List<StreamGroupInfo> xinfoGroup(String key) {
    return executeCommand(commandObjects.xinfoGroup(key));
  }

  @Override
  public List<StreamConsumersInfo> xinfoConsumers(String key, String group) {
    return executeCommand(commandObjects.xinfoConsumers(key, group));
  }

  @Override
  public long geoadd(String key, double longitude, double latitude, String member) {
    return executeCommand(commandObjects.geoadd(key, longitude, latitude, member));
  }

  @Override
  public long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
    return executeCommand(commandObjects.geoadd(key, memberCoordinateMap));
  }

  @Override
  public long geoadd(String key, GeoAddParams params, Map<String, GeoCoordinate> memberCoordinateMap) {
    return executeCommand(commandObjects.geoadd(key, params, memberCoordinateMap));
  }

  @Override
  public Double geodist(String key, String member1, String member2) {
    return executeCommand(commandObjects.geodist(key, member1, member2));
  }

  @Override
  public Double geodist(String key, String member1, String member2, GeoUnit unit) {
    return executeCommand(commandObjects.geodist(key, member1, member2, unit));
  }

  @Override
  public List<String> geohash(String key, String... members) {
    return executeCommand(commandObjects.geohash(key, members));
  }

  @Override
  public List<GeoCoordinate> geopos(String key, String... members) {
    return executeCommand(commandObjects.geopos(key, members));
  }

  @Override
  public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.georadius(key, longitude, latitude, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(String key, double longitude, double latitude, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(commandObjects.georadius(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.georadiusByMember(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(String key, String member, double radius, GeoUnit unit) {
    return executeCommand(commandObjects.georadiusByMemberReadonly(key, member, radius, unit));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(commandObjects.georadiusByMember(key, member, radius, unit, param));
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
    return executeCommand(commandObjects.georadiusByMemberReadonly(key, member, radius, unit, param));
  }

  @Override
  public long pfadd(String key, String... elements) {
    return executeCommand(commandObjects.pfadd(key, elements));
  }

  @Override
  public long pfcount(String key) {
    return executeCommand(commandObjects.pfcount(key));
  }

}
