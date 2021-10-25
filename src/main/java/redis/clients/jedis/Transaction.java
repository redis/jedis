package redis.clients.jedis;

import redis.clients.jedis.args.*;
import redis.clients.jedis.commands.PipelineCommands;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.*;
import redis.clients.jedis.stream.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Transaction extends PipelinedTransactionBase implements PipelineCommands {

  private final RedisCommandObjects commandObjects;

  public Transaction(Connection connection) {
    super(connection);
    this.commandObjects = new RedisCommandObjects();
  }

  @Override
  public Response<Boolean> exists(String key) {
    return appendCommand(commandObjects.exists(key));
  }

  @Override
  public Response<Long> exists(String... keys) {
    return appendCommand(commandObjects.exists(keys));
  }

  @Override
  public Response<Long> persist(String key) {
    return appendCommand(commandObjects.persist(key));
  }

  @Override
  public Response<String> type(String key) {
    return appendCommand(commandObjects.type(key));
  }

  @Override
  public Response<byte[]> dump(String key) {
    return appendCommand(commandObjects.dump(key));
  }

  @Override
  public Response<String> restore(String key, long ttl, byte[] serializedValue) {
    return appendCommand(commandObjects.restore(key, ttl, serializedValue));
  }

  @Override
  public Response<String> restore(String key, long ttl, byte[] serializedValue, RestoreParams params) {
    return appendCommand(commandObjects.restore(key, ttl, serializedValue, params));
  }

  @Override
  public Response<Long> expire(String key, long seconds) {
    return appendCommand(commandObjects.expire(key, seconds));
  }

  @Override
  public Response<Long> pexpire(String key, long milliseconds) {
    return appendCommand(commandObjects.pexpire(key, milliseconds));
  }

  @Override
  public Response<Long> expireAt(String key, long unixTime) {
    return appendCommand(commandObjects.expireAt(key, unixTime));
  }

  @Override
  public Response<Long> pexpireAt(String key, long millisecondsTimestamp) {
    return appendCommand(commandObjects.pexpireAt(key, millisecondsTimestamp));
  }

  @Override
  public Response<Long> ttl(String key) {
    return appendCommand(commandObjects.ttl(key));
  }

  @Override
  public Response<Long> pttl(String key) {
    return appendCommand(commandObjects.pttl(key));
  }

  @Override
  public Response<Long> touch(String key) {
    return appendCommand(commandObjects.touch(key));
  }

  @Override
  public Response<Long> touch(String... keys) {
    return appendCommand(commandObjects.touch(keys));
  }

  @Override
  public Response<List<String>> sort(String key) {
    return appendCommand(commandObjects.sort(key));
  }

  @Override
  public Response<Long> sort(String key, String dstKey) {
    return appendCommand(commandObjects.sort(key, dstKey));
  }

  @Override
  public Response<List<String>> sort(String key, SortingParams sortingParameters) {
    return appendCommand(commandObjects.sort(key, sortingParameters));
  }

  @Override
  public Response<Long> sort(String key, SortingParams sortingParameters, String dstKey) {
    return appendCommand(commandObjects.sort(key, sortingParameters, dstKey));
  }

  @Override
  public Response<Long> del(String key) {
    return appendCommand(commandObjects.del(key));
  }

  @Override
  public Response<Long> del(String... keys) {
    return appendCommand(commandObjects.del(keys));
  }

  @Override
  public Response<Long> unlink(String key) {
    return appendCommand(commandObjects.unlink(key));
  }

  @Override
  public Response<Long> unlink(String... keys) {
    return appendCommand(commandObjects.unlink(keys));
  }

  @Override
  public Response<Boolean> copy(String srcKey, String dstKey, boolean replace) {
    return appendCommand(commandObjects.copy(srcKey, dstKey, replace));
  }

  @Override
  public Response<String> rename(String oldkey, String newkey) {
    return appendCommand(commandObjects.rename(oldkey, newkey));
  }

  @Override
  public Response<Long> renamenx(String oldkey, String newkey) {
    return appendCommand(commandObjects.renamenx(oldkey, newkey));
  }

  @Override
  public Response<Long> memoryUsage(String key) {
    return appendCommand(commandObjects.memoryUsage(key));
  }

  @Override
  public Response<Long> memoryUsage(String key, int samples) {
    return appendCommand(commandObjects.memoryUsage(key, samples));
  }

  @Override
  public Response<Long> objectRefcount(String key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<String> objectEncoding(String key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<Long> objectIdletime(String key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<Long> objectFreq(String key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<Set<String>> keys(String pattern) {
    return appendCommand(commandObjects.keys(pattern));
  }

  @Override
  public Response<ScanResult<String>> scan(String cursor) {
    return appendCommand(commandObjects.scan(cursor));
  }

  @Override
  public Response<ScanResult<String>> scan(String cursor, ScanParams params) {
    return appendCommand(commandObjects.scan(cursor, params));
  }

  @Override
  public Response<ScanResult<String>> scan(String cursor, ScanParams params, String type) {
    return appendCommand(commandObjects.scan(cursor, params, type));
  }

  @Override
  public Response<String> randomKey() {
    return appendCommand(commandObjects.randomKey());
  }

  @Override
  public Response<String> get(String key) {
    return appendCommand(commandObjects.get(key));
  }

  @Override
  public Response<String> getDel(String key) {
    return appendCommand(commandObjects.getDel(key));
  }

  @Override
  public Response<String> getEx(String key, GetExParams params) {
    return appendCommand(commandObjects.getEx(key, params));
  }

  @Override
  public Response<Boolean> setbit(String key, long offset, boolean value) {
    return appendCommand(commandObjects.setbit(key, offset, value));
  }

  @Override
  public Response<Boolean> getbit(String key, long offset) {
    return appendCommand(commandObjects.getbit(key, offset));
  }

  @Override
  public Response<Long> setrange(String key, long offset, String value) {
    return appendCommand(commandObjects.setrange(key, offset, value));
  }

  @Override
  public Response<String> getrange(String key, long startOffset, long endOffset) {
    return appendCommand(commandObjects.getrange(key, startOffset, endOffset));
  }

  @Override
  public Response<String> getSet(String key, String value) {
    return appendCommand(commandObjects.getSet(key, value));
  }

  @Override
  public Response<Long> setnx(String key, String value) {
    return appendCommand(commandObjects.setnx(key, value));
  }

  @Override
  public Response<String> setex(String key, long seconds, String value) {
    return appendCommand(commandObjects.setex(key, seconds, value));
  }

  @Override
  public Response<String> psetex(String key, long milliseconds, String value) {
    return appendCommand(commandObjects.psetex(key, milliseconds, value));
  }

  @Override
  public Response<List<String>> mget(String... keys) {
    return appendCommand(commandObjects.mget(keys));
  }

  @Override
  public Response<String> mset(String... keysvalues) {
    return appendCommand(commandObjects.mset(keysvalues));
  }

  @Override
  public Response<Long> msetnx(String... keysvalues) {
    return appendCommand(commandObjects.msetnx(keysvalues));
  }

  @Override
  public Response<Long> incr(String key) {
    return appendCommand(commandObjects.incr(key));
  }

  @Override
  public Response<Long> incrBy(String key, long increment) {
    return appendCommand(commandObjects.incrBy(key, increment));
  }

  @Override
  public Response<Double> incrByFloat(String key, double increment) {
    return appendCommand(commandObjects.incrByFloat(key, increment));
  }

  @Override
  public Response<Long> decr(String key) {
    return appendCommand(commandObjects.decr(key));
  }

  @Override
  public Response<Long> decrBy(String key, long decrement) {
    return appendCommand(commandObjects.decrBy(key, decrement));
  }

  @Override
  public Response<Long> append(String key, String value) {
    return appendCommand(commandObjects.append(key, value));
  }

  @Override
  public Response<String> substr(String key, int start, int end) {
    return appendCommand(commandObjects.substr(key, start, end));
  }

  @Override
  public Response<Long> strlen(String key) {
    return appendCommand(commandObjects.strlen(key));
  }

  @Override
  public Response<Long> bitcount(String key) {
    return appendCommand(commandObjects.bitcount(key));
  }

  @Override
  public Response<Long> bitcount(String key, long start, long end) {
    return appendCommand(commandObjects.bitcount(key, start, end));
  }

  @Override
  public Response<Long> bitpos(String key, boolean value) {
    return appendCommand(commandObjects.bitpos(key, value));
  }

  @Override
  public Response<Long> bitpos(String key, boolean value, BitPosParams params) {
    return appendCommand(commandObjects.bitpos(key, value, params));
  }

  @Override
  public Response<List<Long>> bitfield(String key, String... arguments) {
    return appendCommand(commandObjects.bitfield(key, arguments));
  }

  @Override
  public Response<List<Long>> bitfieldReadonly(String key, String... arguments) {
    return appendCommand(commandObjects.bitfieldReadonly(key, arguments));
  }

  @Override
  public Response<Long> bitop(BitOP op, String destKey, String... srcKeys) {
    return appendCommand(commandObjects.bitop(op, destKey, srcKeys));
  }

  @Override
  public Response<LCSMatchResult> strAlgoLCSKeys(String keyA, String keyB, StrAlgoLCSParams params) {
    return appendCommand(commandObjects.strAlgoLCSKeys(keyA, keyB, params));
  }

  @Override
  public Response<String> set(String key, String value) {
    return appendCommand(commandObjects.set(key, value));
  }

  @Override
  public Response<String> set(String key, String value, SetParams params) {
    return appendCommand(commandObjects.set(key, value, params));
  }

  @Override
  public Response<Long> rpush(String key, String... string) {
    return appendCommand(commandObjects.rpush(key, string));

  }

  @Override
  public Response<Long> lpush(String key, String... string) {
    return appendCommand(commandObjects.lpush(key, string));
  }

  @Override
  public Response<Long> llen(String key) {
    return appendCommand(commandObjects.llen(key));
  }

  @Override
  public Response<List<String>> lrange(String key, long start, long stop) {
    return appendCommand(commandObjects.lrange(key, start, stop));
  }

  @Override
  public Response<String> ltrim(String key, long start, long stop) {
    return appendCommand(commandObjects.ltrim(key, start, stop));
  }

  @Override
  public Response<String> lindex(String key, long index) {
    return appendCommand(commandObjects.lindex(key, index));
  }

  @Override
  public Response<String> lset(String key, long index, String value) {
    return appendCommand(commandObjects.lset(key, index, value));
  }

  @Override
  public Response<Long> lrem(String key, long count, String value) {
    return appendCommand(commandObjects.lrem(key, count, value));
  }

  @Override
  public Response<String> lpop(String key) {
    return appendCommand(commandObjects.lpop(key));
  }

  @Override
  public Response<List<String>> lpop(String key, int count) {
    return appendCommand(commandObjects.lpop(key, count));
  }

  @Override
  public Response<Long> lpos(String key, String element) {
    return appendCommand(commandObjects.lpos(key, element));
  }

  @Override
  public Response<Long> lpos(String key, String element, LPosParams params) {
    return appendCommand(commandObjects.lpos(key, element, params));
  }

  @Override
  public Response<List<Long>> lpos(String key, String element, LPosParams params, long count) {
    return appendCommand(commandObjects.lpos(key, element, params, count));
  }

  @Override
  public Response<String> rpop(String key) {
    return appendCommand(commandObjects.rpop(key));
  }

  @Override
  public Response<List<String>> rpop(String key, int count) {
    return appendCommand(commandObjects.rpop(key, count));
  }

  @Override
  public Response<Long> linsert(String key, ListPosition where, String pivot, String value) {
    return appendCommand(commandObjects.linsert(key, where, pivot, value));
  }

  @Override
  public Response<Long> lpushx(String key, String... string) {
    return appendCommand(commandObjects.lpushx(key, string));
  }

  @Override
  public Response<Long> rpushx(String key, String... string) {
    return appendCommand(commandObjects.rpushx(key, string));
  }

  @Override
  public Response<List<String>> blpop(int timeout, String key) {
    return appendCommand(commandObjects.blpop(timeout, key));
  }

  @Override
  public Response<KeyedListElement> blpop(double timeout, String key) {
    return appendCommand(commandObjects.blpop(timeout, key));
  }

  @Override
  public Response<List<String>> brpop(int timeout, String key) {
    return appendCommand(commandObjects.brpop(timeout, key));
  }

  @Override
  public Response<KeyedListElement> brpop(double timeout, String key) {
    return appendCommand(commandObjects.brpop(timeout, key));
  }

  @Override
  public Response<List<String>> blpop(int timeout, String... keys) {
    return appendCommand(commandObjects.blpop(timeout, keys));
  }

  @Override
  public Response<KeyedListElement> blpop(double timeout, String... keys) {
    return appendCommand(commandObjects.blpop(timeout, keys));
  }

  @Override
  public Response<List<String>> brpop(int timeout, String... keys) {
    return appendCommand(commandObjects.brpop(timeout, keys));
  }

  @Override
  public Response<KeyedListElement> brpop(double timeout, String... keys) {
    return appendCommand(commandObjects.brpop(timeout, keys));
  }

  @Override
  public Response<String> rpoplpush(String srcKey, String dstKey) {
    return appendCommand(commandObjects.rpoplpush(srcKey, dstKey));
  }

  @Override
  public Response<String> brpoplpush(String source, String destination, int timeout) {
    return appendCommand(commandObjects.brpoplpush(source, destination, timeout));
  }

  @Override
  public Response<String> lmove(String srcKey, String dstKey, ListDirection from, ListDirection to) {
    return appendCommand(commandObjects.lmove(srcKey, dstKey, from, to));
  }

  @Override
  public Response<String> blmove(String srcKey, String dstKey, ListDirection from, ListDirection to, double timeout) {
    return appendCommand(commandObjects.blmove(srcKey, dstKey, from, to, timeout));
  }

  @Override
  public Response<Long> hset(String key, String field, String value) {
    return appendCommand(commandObjects.hset(key, field, value));
  }

  @Override
  public Response<Long> hset(String key, Map<String, String> hash) {
    return appendCommand(commandObjects.hset(key, hash));
  }

  @Override
  public Response<String> hget(String key, String field) {
    return appendCommand(commandObjects.hget(key, field));
  }

  @Override
  public Response<Long> hsetnx(String key, String field, String value) {
    return appendCommand(commandObjects.hsetnx(key, field, value));
  }

  @Override
  public Response<String> hmset(String key, Map<String, String> hash) {
    return appendCommand(commandObjects.hmset(key, hash));
  }

  @Override
  public Response<List<String>> hmget(String key, String... fields) {
    return appendCommand(commandObjects.hmget(key, fields));
  }

  @Override
  public Response<Long> hincrBy(String key, String field, long value) {
    return appendCommand(commandObjects.hincrBy(key, field, value));
  }

  @Override
  public Response<Double> hincrByFloat(String key, String field, double value) {
    return appendCommand(commandObjects.hincrByFloat(key, field, value));
  }

  @Override
  public Response<Boolean> hexists(String key, String field) {
    return appendCommand(commandObjects.hexists(key, field));
  }

  @Override
  public Response<Long> hdel(String key, String... field) {
    return appendCommand(commandObjects.hdel(key, field));
  }

  @Override
  public Response<Long> hlen(String key) {
    return appendCommand(commandObjects.hlen(key));
  }

  @Override
  public Response<Set<String>> hkeys(String key) {
    return appendCommand(commandObjects.hkeys(key));
  }

  @Override
  public Response<List<String>> hvals(String key) {
    return appendCommand(commandObjects.hvals(key));
  }

  @Override
  public Response<Map<String, String>> hgetAll(String key) {
    return appendCommand(commandObjects.hgetAll(key));
  }

  @Override
  public Response<String> hrandfield(String key) {
    return appendCommand(commandObjects.hrandfield(key));
  }

  @Override
  public Response<List<String>> hrandfield(String key, long count) {
    return appendCommand(commandObjects.hrandfield(key, count));
  }

  @Override
  public Response<Map<String, String>> hrandfieldWithValues(String key, long count) {
    return appendCommand(commandObjects.hrandfieldWithValues(key, count));
  }

  @Override
  public Response<ScanResult<Map.Entry<String, String>>> hscan(String key, String cursor, ScanParams params) {
    return appendCommand(commandObjects.hscan(key, cursor, params));
  }

  @Override
  public Response<Long> hstrlen(String key, String field) {
    return appendCommand(commandObjects.hstrlen(key, field));
  }

  @Override
  public Response<Long> sadd(String key, String... member) {
    return appendCommand(commandObjects.sadd(key, member));
  }

  @Override
  public Response<Set<String>> smembers(String key) {
    return appendCommand(commandObjects.smembers(key));
  }

  @Override
  public Response<Long> srem(String key, String... member) {
    return appendCommand(commandObjects.srem(key, member));
  }

  @Override
  public Response<String> spop(String key) {
    return appendCommand(commandObjects.spop(key));
  }

  @Override
  public Response<Set<String>> spop(String key, long count) {
    return appendCommand(commandObjects.spop(key, count));
  }

  @Override
  public Response<Long> scard(String key) {
    return appendCommand(commandObjects.scard(key));
  }

  @Override
  public Response<Boolean> sismember(String key, String member) {
    return appendCommand(commandObjects.sismember(key, member));
  }

  @Override
  public Response<List<Boolean>> smismember(String key, String... members) {
    return appendCommand(commandObjects.smismember(key, members));
  }

  @Override
  public Response<String> srandmember(String key) {
    return appendCommand(commandObjects.srandmember(key));
  }

  @Override
  public Response<List<String>> srandmember(String key, int count) {
    return appendCommand(commandObjects.srandmember(key, count));
  }

  @Override
  public Response<ScanResult<String>> sscan(String key, String cursor, ScanParams params) {
    return appendCommand(commandObjects.sscan(key, cursor, params));
  }

  @Override
  public Response<Set<String>> sdiff(String... keys) {
    return appendCommand(commandObjects.sdiff(keys));
  }

  @Override
  public Response<Long> sdiffstore(String dstKey, String... keys) {
    return appendCommand(commandObjects.sdiffstore(dstKey, keys));
  }

  @Override
  public Response<Set<String>> sinter(String... keys) {
    return appendCommand(commandObjects.sinter(keys));
  }

  @Override
  public Response<Long> sinterstore(String dstKey, String... keys) {
    return appendCommand(commandObjects.sinterstore(dstKey, keys));
  }

  @Override
  public Response<Set<String>> sunion(String... keys) {
    return appendCommand(commandObjects.sunion(keys));
  }

  @Override
  public Response<Long> sunionstore(String dstKey, String... keys) {
    return appendCommand(commandObjects.sunionstore(dstKey, keys));
  }

  @Override
  public Response<Long> smove(String srcKey, String dstKey, String member) {
    return appendCommand(commandObjects.smove(srcKey, dstKey, member));
  }

  @Override
  public Response<Long> zadd(String key, double score, String member) {
    return appendCommand(commandObjects.zadd(key, score, member));
  }

  @Override
  public Response<Long> zadd(String key, double score, String member, ZAddParams params) {
    return appendCommand(commandObjects.zadd(key, score, member, params));
  }

  @Override
  public Response<Long> zadd(String key, Map<String, Double> scoreMembers) {
    return appendCommand(commandObjects.zadd(key, scoreMembers));
  }

  @Override
  public Response<Long> zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
    return appendCommand(commandObjects.zadd(key, scoreMembers, params));
  }

  @Override
  public Response<Double> zaddIncr(String key, double score, String member, ZAddParams params) {
    return appendCommand(commandObjects.zaddIncr(key, score, member, params));
  }

  @Override
  public Response<Long> zrem(String key, String... members) {
    return appendCommand(commandObjects.zrem(key, members));
  }

  @Override
  public Response<Double> zincrby(String key, double increment, String member) {
    return appendCommand(commandObjects.zincrby(key, increment, member));
  }

  @Override
  public Response<Double> zincrby(String key, double increment, String member, ZIncrByParams params) {
    return appendCommand(commandObjects.zincrby(key, increment, member, params));
  }

  @Override
  public Response<Long> zrank(String key, String member) {
    return appendCommand(commandObjects.zrank(key, member));
  }

  @Override
  public Response<Long> zrevrank(String key, String member) {
    return appendCommand(commandObjects.zrevrank(key, member));
  }

  @Override
  public Response<Set<String>> zrange(String key, long start, long stop) {
    return appendCommand(commandObjects.zrange(key, start, stop));
  }

  @Override
  public Response<Set<String>> zrevrange(String key, long start, long stop) {
    return appendCommand(commandObjects.zrevrange(key, start, stop));
  }

  @Override
  public Response<Set<Tuple>> zrangeWithScores(String key, long start, long stop) {
    return appendCommand(commandObjects.zrangeWithScores(key, start, stop));
  }

  @Override
  public Response<Set<Tuple>> zrevrangeWithScores(String key, long start, long stop) {
    return appendCommand(commandObjects.zrevrangeWithScores(key, start, stop));
  }

  @Override
  public Response<String> zrandmember(String key) {
    return appendCommand(commandObjects.zrandmember(key));
  }

  @Override
  public Response<Set<String>> zrandmember(String key, long count) {
    return appendCommand(commandObjects.zrandmember(key, count));
  }

  @Override
  public Response<Set<Tuple>> zrandmemberWithScores(String key, long count) {
    return appendCommand(commandObjects.zrandmemberWithScores(key, count));
  }

  @Override
  public Response<Long> zcard(String key) {
    return appendCommand(commandObjects.zcard(key));
  }

  @Override
  public Response<Double> zscore(String key, String member) {
    return appendCommand(commandObjects.zscore(key, member));
  }

  @Override
  public Response<List<Double>> zmscore(String key, String... members) {
    return appendCommand(commandObjects.zmscore(key, members));
  }

  @Override
  public Response<Tuple> zpopmax(String key) {
    return appendCommand(commandObjects.zpopmax(key));
  }

  @Override
  public Response<Set<Tuple>> zpopmax(String key, int count) {
    return appendCommand(commandObjects.zpopmax(key, count));
  }

  @Override
  public Response<Tuple> zpopmin(String key) {
    return appendCommand(commandObjects.zpopmin(key));
  }

  @Override
  public Response<Set<Tuple>> zpopmin(String key, int count) {
    return appendCommand(commandObjects.zpopmin(key, count));
  }

  @Override
  public Response<Long> zcount(String key, double min, double max) {
    return appendCommand(commandObjects.zcount(key, min, max));
  }

  @Override
  public Response<Long> zcount(String key, String min, String max) {
    return appendCommand(commandObjects.zcount(key, min, max));
  }

  @Override
  public Response<Set<String>> zrangeByScore(String key, double min, double max) {
    return appendCommand(commandObjects.zrangeByScore(key, min, max));
  }

  @Override
  public Response<Set<String>> zrangeByScore(String key, String min, String max) {
    return appendCommand(commandObjects.zrangeByScore(key, min, max));
  }

  @Override
  public Response<Set<String>> zrevrangeByScore(String key, double max, double min) {
    return appendCommand(commandObjects.zrevrangeByScore(key, max, min));

  }

  @Override
  public Response<Set<String>> zrangeByScore(String key, double min, double max, int offset, int count) {
    return appendCommand(commandObjects.zrangeByScore(key, min, max, offset, count));
  }

  @Override
  public Response<Set<String>> zrevrangeByScore(String key, String max, String min) {
    return appendCommand(commandObjects.zrevrangeByScore(key, max, min));
  }

  @Override
  public Response<Set<String>> zrangeByScore(String key, String min, String max, int offset, int count) {
    return appendCommand(commandObjects.zrangeByScore(key, min, max, offset, count));
  }

  @Override
  public Response<Set<String>> zrevrangeByScore(String key, double max, double min, int offset, int count) {
    return appendCommand(commandObjects.zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max) {
    return appendCommand(commandObjects.zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min) {
    return appendCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
    return appendCommand(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public Response<Set<String>> zrevrangeByScore(String key, String max, String min, int offset, int count) {
    return appendCommand(commandObjects.zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(String key, String min, String max) {
    return appendCommand(commandObjects.zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min) {
    return appendCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
    return appendCommand(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
    return appendCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
    return appendCommand(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public Response<Long> zremrangeByRank(String key, long start, long stop) {
    return appendCommand(commandObjects.zremrangeByRank(key, start, stop));
  }

  @Override
  public Response<Long> zremrangeByScore(String key, double min, double max) {
    return appendCommand(commandObjects.zremrangeByScore(key, min, max));
  }

  @Override
  public Response<Long> zremrangeByScore(String key, String min, String max) {
    return appendCommand(commandObjects.zremrangeByScore(key, min, max));
  }

  @Override
  public Response<Long> zlexcount(String key, String min, String max) {
    return appendCommand(commandObjects.zlexcount(key, min, max));
  }

  @Override
  public Response<Set<String>> zrangeByLex(String key, String min, String max) {
    return appendCommand(commandObjects.zrangeByLex(key, min, max));
  }

  @Override
  public Response<Set<String>> zrangeByLex(String key, String min, String max, int offset, int count) {
    return appendCommand(commandObjects.zrangeByLex(key, min, max, offset, count));
  }

  @Override
  public Response<Set<String>> zrevrangeByLex(String key, String max, String min) {
    return appendCommand(commandObjects.zrevrangeByLex(key, max, min));
  }

  @Override
  public Response<Set<String>> zrevrangeByLex(String key, String max, String min, int offset, int count) {
    return appendCommand(commandObjects.zrevrangeByLex(key, max, min, offset, count));
  }

  @Override
  public Response<Long> zremrangeByLex(String key, String min, String max) {
    return appendCommand(commandObjects.zremrangeByLex(key, min, max));
  }

  @Override
  public Response<ScanResult<Tuple>> zscan(String key, String cursor, ScanParams params) {
    return appendCommand(commandObjects.zscan(key, cursor, params));
  }

  @Override
  public Response<KeyedZSetElement> bzpopmax(double timeout, String... keys) {
    return appendCommand(commandObjects.bzpopmax(timeout, keys));
  }

  @Override
  public Response<KeyedZSetElement> bzpopmin(double timeout, String... keys) {
    return appendCommand(commandObjects.bzpopmin(timeout, keys));
  }

  @Override
  public Response<Set<String>> zdiff(String... keys) {
    return appendCommand(commandObjects.zdiff(keys));
  }

  @Override
  public Response<Set<Tuple>> zdiffWithScores(String... keys) {
    return appendCommand(commandObjects.zdiffWithScores(keys));
  }

  @Override
  public Response<Long> zdiffStore(String dstKey, String... keys) {
    return appendCommand(commandObjects.zdiffStore(dstKey, keys));
  }

  @Override
  public Response<Long> zinterstore(String dstKey, String... sets) {
    return appendCommand(commandObjects.zinterstore(dstKey, sets));
  }

  @Override
  public Response<Long> zinterstore(String dstKey, ZParams params, String... sets) {
    return appendCommand(commandObjects.zinterstore(dstKey, params, sets));
  }

  @Override
  public Response<Set<String>> zinter(ZParams params, String... keys) {
    return appendCommand(commandObjects.zinter(params, keys));
  }

  @Override
  public Response<Set<Tuple>> zinterWithScores(ZParams params, String... keys) {
    return appendCommand(commandObjects.zinterWithScores(params, keys));
  }

  @Override
  public Response<Set<String>> zunion(ZParams params, String... keys) {
    return appendCommand(commandObjects.zunion(params, keys));
  }

  @Override
  public Response<Set<Tuple>> zunionWithScores(ZParams params, String... keys) {
    return appendCommand(commandObjects.zunionWithScores(params, keys));
  }

  @Override
  public Response<Long> zunionstore(String dstKey, String... sets) {
    return appendCommand(commandObjects.zunionstore(dstKey, sets));
  }

  @Override
  public Response<Long> zunionstore(String dstKey, ZParams params, String... sets) {
    return appendCommand(commandObjects.zunionstore(dstKey, params, sets));
  }

  @Override
  public Response<Long> geoadd(String key, double longitude, double latitude, String member) {
    return appendCommand(commandObjects.geoadd(key, longitude, latitude, member));
  }

  @Override
  public Response<Long> geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
    return appendCommand(commandObjects.geoadd(key, memberCoordinateMap));
  }

  @Override
  public Response<Long> geoadd(String key, GeoAddParams params, Map<String, GeoCoordinate> memberCoordinateMap) {
    return appendCommand(commandObjects.geoadd(key, params, memberCoordinateMap));
  }

  @Override
  public Response<Double> geodist(String key, String member1, String member2) {
    return appendCommand(commandObjects.geodist(key, member1, member2));
  }

  @Override
  public Response<Double> geodist(String key, String member1, String member2, GeoUnit unit) {
    return appendCommand(commandObjects.geodist(key, member1, member2, unit));
  }

  @Override
  public Response<List<String>> geohash(String key, String... members) {
    return appendCommand(commandObjects.geohash(key, members));
  }

  @Override
  public Response<List<GeoCoordinate>> geopos(String key, String... members) {
    return appendCommand(commandObjects.geopos(key, members));
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
    return appendCommand(commandObjects.georadius(key, longitude, latitude, radius, unit));
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusReadonly(String key, double longitude, double latitude, double radius, GeoUnit unit) {
    return appendCommand(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit));
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
    return appendCommand(commandObjects.georadius(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusReadonly(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
    return appendCommand(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
    return appendCommand(commandObjects.georadiusByMember(key, member, radius, unit));
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusByMemberReadonly(String key, String member, double radius, GeoUnit unit) {
    return appendCommand(commandObjects.georadiusByMemberReadonly(key, member, radius, unit));
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
    return appendCommand(commandObjects.georadiusByMember(key, member, radius, unit, param));
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusByMemberReadonly(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
    return appendCommand(commandObjects.georadiusByMemberReadonly(key, member, radius, unit, param));
  }

  @Override
  public Response<Long> georadiusStore(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    return appendCommand(commandObjects.georadiusStore(key, longitude, latitude, radius, unit, param, storeParam));
  }

  @Override
  public Response<Long> georadiusByMemberStore(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    return appendCommand(commandObjects.georadiusByMemberStore(key, member, radius, unit, param, storeParam));
  }

  @Override
  public Response<Long> pfadd(String key, String... elements) {
    return appendCommand(commandObjects.pfadd(key, elements));
  }

  @Override
  public Response<String> pfmerge(String destkey, String... sourcekeys) {
    return appendCommand(commandObjects.pfmerge(destkey, sourcekeys));
  }

  @Override
  public Response<Long> pfcount(String key) {
    return appendCommand(commandObjects.pfcount(key));
  }

  @Override
  public Response<Long> pfcount(String... keys) {
    return appendCommand(commandObjects.pfcount(keys));
  }

  @Override
  public Response<StreamEntryID> xadd(String key, StreamEntryID id, Map<String, String> hash) {
    return appendCommand(commandObjects.xadd(key, id, hash));
  }

  @Override
  public Response<StreamEntryID> xadd_v2(String key, XAddParams params, Map<String, String> hash) {
    return appendCommand(commandObjects.xadd(key, params, hash));
  }

  @Override
  public Response<Long> xlen(String key) {
    return appendCommand(commandObjects.xlen(key));
  }

  @Override
  public Response<List<StreamEntry>> xrange(String key, StreamEntryID start, StreamEntryID end) {
    return appendCommand(commandObjects.xrange(key, start, end));
  }

  @Override
  public Response<List<StreamEntry>> xrange(String key, StreamEntryID start, StreamEntryID end, int count) {
    return appendCommand(commandObjects.xrange(key, start, end, count));
  }

  @Override
  public Response<List<StreamEntry>> xrevrange(String key, StreamEntryID end, StreamEntryID start) {
    return appendCommand(commandObjects.xrevrange(key, start, end));
  }

  @Override
  public Response<List<StreamEntry>> xrevrange(String key, StreamEntryID end, StreamEntryID start, int count) {
    return appendCommand(commandObjects.xrevrange(key, start, end, count));
  }

  @Override
  public Response<Long> xack(String key, String group, StreamEntryID... ids) {
    return appendCommand(commandObjects.xack(key, group, ids));
  }

  @Override
  public Response<String> xgroupCreate(String key, String groupname, StreamEntryID id, boolean makeStream) {
    return appendCommand(commandObjects.xgroupCreate(key, groupname, id, makeStream));
  }

  @Override
  public Response<String> xgroupSetID(String key, String groupname, StreamEntryID id) {
    return appendCommand(commandObjects.xgroupSetID(key, groupname, id));
  }

  @Override
  public Response<Long> xgroupDestroy(String key, String groupname) {
    return appendCommand(commandObjects.xgroupDestroy(key, groupname));
  }

  @Override
  public Response<Long> xgroupDelConsumer(String key, String groupname, String consumername) {
    return appendCommand(commandObjects.xgroupDelConsumer(key, groupname, consumername));
  }

  @Override
  public Response<StreamPendingSummary> xpending(String key, String groupname) {
    return appendCommand(commandObjects.xpending(key, groupname));
  }

  @Override
  public Response<List<StreamPendingEntry>> xpending(String key, String groupname, StreamEntryID start, StreamEntryID end, int count, String consumername) {
    return appendCommand(commandObjects.xpending(key, groupname, start, end, count, consumername));
  }

  @Override
  public Response<List<StreamPendingEntry>> xpending(String key, String groupname, XPendingParams params) {
    return appendCommand(commandObjects.xpending(key, groupname, params));
  }

  @Override
  public Response<Long> xdel(String key, StreamEntryID... ids) {
    return appendCommand(commandObjects.xdel(key, ids));
  }

  @Override
  public Response<Long> xtrim(String key, long maxLen, boolean approximate) {
    return appendCommand(commandObjects.xtrim(key, maxLen, approximate));
  }

  @Override
  public Response<Long> xtrim(String key, XTrimParams params) {
    return appendCommand(commandObjects.xtrim(key, params));
  }

  @Override
  public Response<List<StreamEntry>> xclaim(String key, String group, String consumername, long minIdleTime, XClaimParams params, StreamEntryID... ids) {
    return appendCommand(commandObjects.xclaim(key, group, consumername, minIdleTime, params, ids));
  }

  @Override
  public Response<List<StreamEntryID>> xclaimJustId(String key, String group, String consumername, long minIdleTime, XClaimParams params, StreamEntryID... ids) {
    return appendCommand(commandObjects.xclaimJustId(key, group, consumername, minIdleTime, params, ids));
  }

  @Override
  public Response<Map.Entry<StreamEntryID, List<StreamEntry>>> xautoclaim(String key, String group, String consumername, long minIdleTime, StreamEntryID start, XAutoClaimParams params) {
    return appendCommand(commandObjects.xautoclaim(key, group, consumername, minIdleTime, start, params));
  }

  @Override
  public Response<Map.Entry<StreamEntryID, List<StreamEntryID>>> xautoclaimJustId(String key, String group, String consumername, long minIdleTime, StreamEntryID start, XAutoClaimParams params) {
    return appendCommand(commandObjects.xautoclaimJustId(key, group, consumername, minIdleTime, start, params));
  }

  @Override
  public Response<StreamInfo> xinfoStream(String key) {
    return appendCommand(commandObjects.xinfoStream(key));
  }

  @Override
  public Response<List<StreamGroupInfo>> xinfoGroup(String key) {
    return appendCommand(commandObjects.xinfoGroup(key));
  }

  @Override
  public Response<List<StreamConsumersInfo>> xinfoConsumers(String key, String group) {
    return appendCommand(commandObjects.xinfoConsumers(key, group));
  }

  @Override
  public Response<List<Map.Entry<String, List<StreamEntry>>>> xread(XReadParams xReadParams, Map<String, StreamEntryID> streams) {
    return appendCommand(commandObjects.xread(xReadParams, streams));
  }

  @Override
  public Response<List<Map.Entry<String, List<StreamEntry>>>> xreadGroup(String groupname, String consumer, XReadGroupParams xReadGroupParams, Map<String, StreamEntryID> streams) {
    return appendCommand(commandObjects.xreadGroup(groupname, consumer, xReadGroupParams, streams));
  }

  @Override
  public Response<Object> eval(String script) {
    return appendCommand(commandObjects.eval(script));
  }

  @Override
  public Response<Object> eval(String script, int keyCount, String... params) {
    return appendCommand(commandObjects.eval(script, keyCount, params));
  }

  @Override
  public Response<Object> eval(String script, List<String> keys, List<String> args) {
    return appendCommand(commandObjects.eval(script, keys, args));
  }

  @Override
  public Response<Object> evalsha(String sha1) {
    return appendCommand(commandObjects.evalsha(sha1));
  }

  @Override
  public Response<Object> evalsha(String sha1, int keyCount, String... params) {
    return appendCommand(commandObjects.evalsha(sha1, keyCount, params));
  }

  @Override
  public Response<Object> evalsha(String sha1, List<String> keys, List<String> args) {
    return appendCommand(commandObjects.evalsha(sha1, keys, args));
  }

  @Override
  public Response<Long> waitReplicas(String sampleKey, int replicas, long timeout) {
    throw new UnsupportedOperationException("Not supported yet.");
    //return appendCommand(commandObjects.waitReplicas(sampleKey, replicas, timeout));
  }

  @Override
  public Response<Object> eval(String script, String sampleKey) {
    throw new UnsupportedOperationException("Not supported yet.");
    //return appendCommand(commandObjects.eval(script, sampleKey));
  }

  @Override
  public Response<Object> evalsha(String sha1, String sampleKey) {
    throw new UnsupportedOperationException("Not supported yet.");
    //return appendCommand(commandObjects.evalsha(sha1, sampleKey));
  }

  @Override
  public Response<Boolean> scriptExists(String sha1, String sampleKey) {
    throw new UnsupportedOperationException("Not supported yet.");
    //return appendCommand(commandObjects.scriptExists(sha1, sampleKey));
  }

  @Override
  public Response<List<Boolean>> scriptExists(String sampleKey, String... sha1) {
    throw new UnsupportedOperationException("Not supported yet.");
    //return appendCommand(commandObjects.scriptExists(sampleKey, sha1));
  }

  @Override
  public Response<String> scriptLoad(String script, String sampleKey) {
    throw new UnsupportedOperationException("Not supported yet.");
    //return appendCommand(commandObjects.scriptLoad(script, sampleKey));
  }

  @Override
  public Response<String> scriptFlush(String sampleKey) {
    throw new UnsupportedOperationException("Not supported yet.");
    //return appendCommand(commandObjects.scriptFlush(sampleKey));
  }

  @Override
  public Response<String> scriptKill(String sampleKey) {
    throw new UnsupportedOperationException("Not supported yet.");
    //return appendCommand(commandObjects.scriptKill(sampleKey));
  }
}
