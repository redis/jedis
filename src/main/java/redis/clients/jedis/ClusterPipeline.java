package redis.clients.jedis;

import redis.clients.jedis.args.*;
import redis.clients.jedis.commands.PipelineCommands;
import redis.clients.jedis.params.*;
import redis.clients.jedis.providers.JedisClusterConnectionProvider;
import redis.clients.jedis.resps.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClusterPipeline extends MultiNodePipelineBase implements PipelineCommands {

  private final JedisClusterConnectionProvider provider;
  private final RedisCommandObjects commandObjects;

  public ClusterPipeline(JedisClusterConnectionProvider provider) {
    this.provider = provider;
    this.commandObjects = new RedisCommandObjects();
  }

  @Override
  protected Connection getConnection(HostAndPort nodeKey) {
    return provider.getConnection(nodeKey);
  }

  @Override
  public Response<Boolean> exists(String key) {
    return appendCommand(provider.getNode(key), commandObjects.exists(key));
  }

  @Override
  public Response<Long> exists(String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.exists(keys));
  }

  @Override
  public Response<Long> persist(String key) {
    return appendCommand(provider.getNode(key), commandObjects.persist(key));
  }

  @Override
  public Response<String> type(String key) {
    return appendCommand(provider.getNode(key), commandObjects.type(key));
  }

  @Override
  public Response<byte[]> dump(String key) {
    return appendCommand(provider.getNode(key), commandObjects.dump(key));
  }

  @Override
  public Response<String> restore(String key, long ttl, byte[] serializedValue) {
    return appendCommand(provider.getNode(key), commandObjects.restore(key, ttl, serializedValue));
  }

  @Override
  public Response<String> restore(String key, long ttl, byte[] serializedValue, RestoreParams params) {
    return appendCommand(provider.getNode(key), commandObjects.restore(key, ttl, serializedValue, params));
  }

  @Override
  public Response<Long> expire(String key, long seconds) {
    return appendCommand(provider.getNode(key), commandObjects.expire(key, seconds));
  }

  @Override
  public Response<Long> pexpire(String key, long milliseconds) {
    return appendCommand(provider.getNode(key), commandObjects.pexpire(key, milliseconds));
  }

  @Override
  public Response<Long> expireAt(String key, long unixTime) {
    return appendCommand(provider.getNode(key), commandObjects.expireAt(key, unixTime));
  }

  @Override
  public Response<Long> pexpireAt(String key, long millisecondsTimestamp) {
    return appendCommand(provider.getNode(key), commandObjects.pexpireAt(key, millisecondsTimestamp));
  }

  @Override
  public Response<Long> ttl(String key) {
    return appendCommand(provider.getNode(key), commandObjects.ttl(key));
  }

  @Override
  public Response<Long> pttl(String key) {
    return appendCommand(provider.getNode(key), commandObjects.pttl(key));
  }

  @Override
  public Response<Long> touch(String key) {
    return appendCommand(provider.getNode(key), commandObjects.touch(key));
  }

  @Override
  public Response<Long> touch(String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.touch(keys));
  }

  @Override
  public Response<List<String>> sort(String key) {
    return appendCommand(provider.getNode(key), commandObjects.sort(key));
  }

  @Override
  public Response<Long> sort(String key, String dstKey) {
    return appendCommand(provider.getNode(key), commandObjects.sort(key, dstKey));
  }

  @Override
  public Response<List<String>> sort(String key, SortingParams sortingParameters) {
    return appendCommand(provider.getNode(key), commandObjects.sort(key, sortingParameters));
  }

  @Override
  public Response<Long> sort(String key, SortingParams sortingParameters, String dstKey) {
    return appendCommand(provider.getNode(key), commandObjects.sort(key, sortingParameters, dstKey));
  }

  @Override
  public Response<Long> del(String key) {
    return appendCommand(provider.getNode(key), commandObjects.del(key));
  }

  @Override
  public Response<Long> del(String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.del(keys));
  }

  @Override
  public Response<Long> unlink(String key) {
    return appendCommand(provider.getNode(key), commandObjects.unlink(key));
  }

  @Override
  public Response<Long> unlink(String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.unlink(keys));
  }

  @Override
  public Response<Boolean> copy(String srcKey, String dstKey, boolean replace) {
    return appendCommand(provider.getNode(srcKey), commandObjects.copy(srcKey, dstKey, replace));
  }

  @Override
  public Response<String> rename(String oldKey, String newKey) {
    return appendCommand(provider.getNode(oldKey), commandObjects.rename(oldKey, newKey));
  }

  @Override
  public Response<Long> renamenx(String oldKey, String newKey) {
    return appendCommand(provider.getNode(oldKey), commandObjects.renamenx(oldKey, newKey));
  }

  @Override
  public Response<Long> memoryUsage(String key) {
    return appendCommand(provider.getNode(key), commandObjects.memoryUsage(key));
  }

  @Override
  public Response<Long> memoryUsage(String key, int samples) {
    return appendCommand(provider.getNode(key), commandObjects.memoryUsage(key, samples));
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
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<ScanResult<String>> scan(String cursor) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<ScanResult<String>> scan(String cursor, ScanParams params) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<ScanResult<String>> scan(String cursor, ScanParams params, String type) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<String> randomKey() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<String> get(String key) {
    return appendCommand(provider.getNode(key), commandObjects.get(key));
  }

  @Override
  public Response<String> getDel(String key) {
    return appendCommand(provider.getNode(key), commandObjects.getDel(key));
  }

  @Override
  public Response<String> getEx(String key, GetExParams params) {
    return appendCommand(provider.getNode(key), commandObjects.getEx(key, params));
  }

  @Override
  public Response<Boolean> setbit(String key, long offset, boolean value) {
    return appendCommand(provider.getNode(key), commandObjects.setbit(key, offset, value));
  }

  @Override
  public Response<Boolean> getbit(String key, long offset) {
    return appendCommand(provider.getNode(key), commandObjects.getbit(key, offset));
  }

  @Override
  public Response<Long> setrange(String key, long offset, String value) {
    return appendCommand(provider.getNode(key), commandObjects.setrange(key, offset, value));
  }

  @Override
  public Response<String> getrange(String key, long startOffset, long endOffset) {
    return appendCommand(provider.getNode(key), commandObjects.getrange(key, startOffset, endOffset));
  }

  @Override
  public Response<String> getSet(String key, String value) {
    return appendCommand(provider.getNode(key), commandObjects.getSet(key, value));
  }

  @Override
  public Response<Long> setnx(String key, String value) {
    return appendCommand(provider.getNode(key), commandObjects.setnx(key, value));
  }

  @Override
  public Response<String> setex(String key, long seconds, String value) {
    return appendCommand(provider.getNode(key), commandObjects.setex(key, seconds, value));
  }

  @Override
  public Response<String> psetex(String key, long milliseconds, String value) {
    return appendCommand(provider.getNode(key), commandObjects.psetex(key, milliseconds, value));
  }

  @Override
  public Response<List<String>> mget(String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.mget(keys));
  }

  @Override
  public Response<String> mset(String... keysvalues) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<Long> msetnx(String... keysvalues) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<Long> incr(String key) {
    return appendCommand(provider.getNode(key), commandObjects.incr(key));
  }

  @Override
  public Response<Long> incrBy(String key, long increment) {
    return appendCommand(provider.getNode(key), commandObjects.incrBy(key, increment));
  }

  @Override
  public Response<Double> incrByFloat(String key, double increment) {
    return appendCommand(provider.getNode(key), commandObjects.incrByFloat(key, increment));
  }

  @Override
  public Response<Long> decr(String key) {
    return appendCommand(provider.getNode(key), commandObjects.decr(key));
  }

  @Override
  public Response<Long> decrBy(String key, long decrement) {
    return appendCommand(provider.getNode(key), commandObjects.decrBy(key, decrement));
  }

  @Override
  public Response<Long> append(String key, String value) {
    return appendCommand(provider.getNode(key), commandObjects.append(key, value));
  }

  @Override
  public Response<String> substr(String key, int start, int end) {
    return appendCommand(provider.getNode(key), commandObjects.substr(key, start, end));
  }

  @Override
  public Response<Long> strlen(String key) {
    return appendCommand(provider.getNode(key), commandObjects.strlen(key));
  }

  @Override
  public Response<Long> bitcount(String key) {
    return appendCommand(provider.getNode(key), commandObjects.bitcount(key));
  }

  @Override
  public Response<Long> bitcount(String key, long start, long end) {
    return appendCommand(provider.getNode(key), commandObjects.bitcount(key, start, end));
  }

  @Override
  public Response<Long> bitpos(String key, boolean value) {
    return appendCommand(provider.getNode(key), commandObjects.bitpos(key, value));
  }

  @Override
  public Response<Long> bitpos(String key, boolean value, BitPosParams params) {
    return appendCommand(provider.getNode(key), commandObjects.bitpos(key, value, params));
  }

  @Override
  public Response<List<Long>> bitfield(String key, String... arguments) {
    return appendCommand(provider.getNode(key), commandObjects.bitfield(key, arguments));
  }

  @Override
  public Response<List<Long>> bitfieldReadonly(String key, String... arguments) {
    return appendCommand(provider.getNode(key), commandObjects.bitfieldReadonly(key, arguments));
  }

  @Override
  public Response<Long> bitop(BitOP op, String destKey, String... srcKeys) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Response<LCSMatchResult> strAlgoLCSKeys(String keyA, String keyB, StrAlgoLCSParams params) {
    return appendCommand(provider.getNode(keyA), commandObjects.strAlgoLCSKeys(keyA, keyB, params));
  }

  @Override
  public Response<String> set(String key, String value) {
    return appendCommand(provider.getNode(key), commandObjects.set(key, value));
  }

  @Override
  public Response<String> set(String key, String value, SetParams params) {
    return appendCommand(provider.getNode(key), commandObjects.set(key, value, params));
  }

  @Override
  public Response<Long> rpush(String key, String... string) {
    return appendCommand(provider.getNode(key), commandObjects.rpush(key, string));
  }

  @Override
  public Response<Long> lpush(String key, String... string) {
    return appendCommand(provider.getNode(key), commandObjects.lpush(key, string));
  }

  @Override
  public Response<Long> llen(String key) {
    return appendCommand(provider.getNode(key), commandObjects.llen(key));
  }

  @Override
  public Response<List<String>> lrange(String key, long start, long stop) {
    return appendCommand(provider.getNode(key), commandObjects.lrange(key, start, stop));
  }

  @Override
  public Response<String> ltrim(String key, long start, long stop) {
    return appendCommand(provider.getNode(key), commandObjects.ltrim(key, start, stop));
  }

  @Override
  public Response<String> lindex(String key, long index) {
    return appendCommand(provider.getNode(key), commandObjects.lindex(key, index));
  }

  @Override
  public Response<String> lset(String key, long index, String value) {
    return appendCommand(provider.getNode(key), commandObjects.lset(key, index, value));
  }

  @Override
  public Response<Long> lrem(String key, long count, String value) {
    return appendCommand(provider.getNode(key), commandObjects.lrem(key, count, value));
  }

  @Override
  public Response<String> lpop(String key) {
    return appendCommand(provider.getNode(key), commandObjects.lpop(key));
  }

  @Override
  public Response<List<String>> lpop(String key, int count) {
    return appendCommand(provider.getNode(key), commandObjects.lpop(key, count));
  }

  @Override
  public Response<Long> lpos(String key, String element) {
    return appendCommand(provider.getNode(key), commandObjects.lpos(key, element));
  }

  @Override
  public Response<Long> lpos(String key, String element, LPosParams params) {
    return appendCommand(provider.getNode(key), commandObjects.lpos(key, element, params));
  }

  @Override
  public Response<List<Long>> lpos(String key, String element, LPosParams params, long count) {
    return appendCommand(provider.getNode(key), commandObjects.lpos(key, element, params, count));
  }

  @Override
  public Response<String> rpop(String key) {
    return appendCommand(provider.getNode(key), commandObjects.rpop(key));
  }

  @Override
  public Response<List<String>> rpop(String key, int count) {
    return appendCommand(provider.getNode(key), commandObjects.rpop(key, count));
  }

  @Override
  public Response<Long> linsert(String key, ListPosition where, String pivot, String value) {
    return appendCommand(provider.getNode(key), commandObjects.linsert(key, where, pivot, value));
  }

  @Override
  public Response<Long> lpushx(String key, String... string) {
    return appendCommand(provider.getNode(key), commandObjects.lpushx(key, string));
  }

  @Override
  public Response<Long> rpushx(String key, String... string) {
    return appendCommand(provider.getNode(key), commandObjects.rpushx(key, string));
  }

  @Override
  public Response<List<String>> blpop(int timeout, String key) {
    return appendCommand(provider.getNode(key), commandObjects.blpop(timeout, key));
  }

  @Override
  public Response<KeyedListElement> blpop(double timeout, String key) {
    return appendCommand(provider.getNode(key), commandObjects.blpop(timeout, key));
  }

  @Override
  public Response<List<String>> brpop(int timeout, String key) {
    return appendCommand(provider.getNode(key), commandObjects.brpop(timeout, key));
  }

  @Override
  public Response<KeyedListElement> brpop(double timeout, String key) {
    return appendCommand(provider.getNode(key), commandObjects.brpop(timeout, key));
  }

  @Override
  public Response<List<String>> blpop(int timeout, String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.blpop(timeout, keys));
  }

  @Override
  public Response<KeyedListElement> blpop(double timeout, String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.blpop(timeout, keys));
  }

  @Override
  public Response<List<String>> brpop(int timeout, String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.brpop(timeout, keys));
  }

  @Override
  public Response<KeyedListElement> brpop(double timeout, String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.brpop(timeout, keys));
  }

  @Override
  public Response<String> rpoplpush(String srcKey, String dstKey) {
    return appendCommand(provider.getNode(srcKey), commandObjects.rpoplpush(srcKey, dstKey));
  }

  @Override
  public Response<String> brpoplpush(String source, String destination, int timeout) {
    return appendCommand(provider.getNode(source), commandObjects.brpoplpush(source, destination, timeout));
  }

  @Override
  public Response<String> lmove(String srcKey, String dstKey, ListDirection from, ListDirection to) {
    return appendCommand(provider.getNode(srcKey), commandObjects.lmove(srcKey, dstKey, from, to));
  }

  @Override
  public Response<String> blmove(String srcKey, String dstKey, ListDirection from, ListDirection to, double timeout) {
    return appendCommand(provider.getNode(srcKey), commandObjects.blmove(srcKey, dstKey, from, to, timeout));
  }

  @Override
  public Response<Long> hset(String key, String field, String value) {
    return appendCommand(provider.getNode(key), commandObjects.hset(key, field, value));
  }

  @Override
  public Response<Long> hset(String key, Map<String, String> hash) {
    return appendCommand(provider.getNode(key), commandObjects.hset(key, hash));
  }

  @Override
  public Response<String> hget(String key, String field) {
    return appendCommand(provider.getNode(key), commandObjects.hget(key, field));
  }

  @Override
  public Response<Long> hsetnx(String key, String field, String value) {
    return appendCommand(provider.getNode(key), commandObjects.hsetnx(key, field, value));
  }

  @Override
  public Response<String> hmset(String key, Map<String, String> hash) {
    return appendCommand(provider.getNode(key), commandObjects.hmset(key, hash));
  }

  @Override
  public Response<List<String>> hmget(String key, String... fields) {
    return appendCommand(provider.getNode(key), commandObjects.hmget(key, fields));
  }

  @Override
  public Response<Long> hincrBy(String key, String field, long value) {
    return appendCommand(provider.getNode(key), commandObjects.hincrBy(key, field, value));
  }

  @Override
  public Response<Double> hincrByFloat(String key, String field, double value) {
    return appendCommand(provider.getNode(key), commandObjects.hincrByFloat(key, field, value));
  }

  @Override
  public Response<Boolean> hexists(String key, String field) {
    return appendCommand(provider.getNode(key), commandObjects.hexists(key, field));
  }

  @Override
  public Response<Long> hdel(String key, String... field) {
    return appendCommand(provider.getNode(key), commandObjects.hdel(key, field));
  }

  @Override
  public Response<Long> hlen(String key) {
    return appendCommand(provider.getNode(key), commandObjects.hlen(key));
  }

  @Override
  public Response<Set<String>> hkeys(String key) {
    return appendCommand(provider.getNode(key), commandObjects.hkeys(key));
  }

  @Override
  public Response<List<String>> hvals(String key) {
    return appendCommand(provider.getNode(key), commandObjects.hvals(key));
  }

  @Override
  public Response<Map<String, String>> hgetAll(String key) {
    return appendCommand(provider.getNode(key), commandObjects.hgetAll(key));
  }

  @Override
  public Response<String> hrandfield(String key) {
    return appendCommand(provider.getNode(key), commandObjects.hrandfield(key));
  }

  @Override
  public Response<List<String>> hrandfield(String key, long count) {
    return appendCommand(provider.getNode(key), commandObjects.hrandfield(key, count));
  }

  @Override
  public Response<Map<String, String>> hrandfieldWithValues(String key, long count) {
    return appendCommand(provider.getNode(key), commandObjects.hrandfieldWithValues(key, count));
  }

  @Override
  public Response<ScanResult<Map.Entry<String, String>>> hscan(String key, String cursor, ScanParams params) {
    return appendCommand(provider.getNode(key), commandObjects.hscan(key, cursor, params));
  }

  @Override
  public Response<Long> hstrlen(String key, String field) {
    return appendCommand(provider.getNode(key), commandObjects.hstrlen(key, field));
  }

  @Override
  public Response<Long> sadd(String key, String... member) {
    return appendCommand(provider.getNode(key), commandObjects.sadd(key, member));
  }

  @Override
  public Response<Set<String>> smembers(String key) {
    return appendCommand(provider.getNode(key), commandObjects.smembers(key));
  }

  @Override
  public Response<Long> srem(String key, String... member) {
    return appendCommand(provider.getNode(key), commandObjects.srem(key, member));
  }

  @Override
  public Response<String> spop(String key) {
    return appendCommand(provider.getNode(key), commandObjects.spop(key));
  }

  @Override
  public Response<Set<String>> spop(String key, long count) {
    return appendCommand(provider.getNode(key), commandObjects.spop(key, count));
  }

  @Override
  public Response<Long> scard(String key) {
    return appendCommand(provider.getNode(key), commandObjects.scard(key));
  }

  @Override
  public Response<Boolean> sismember(String key, String member) {
    return appendCommand(provider.getNode(key), commandObjects.sismember(key, member));
  }

  @Override
  public Response<List<Boolean>> smismember(String key, String... members) {
    return appendCommand(provider.getNode(key), commandObjects.smismember(key, members));
  }

  @Override
  public Response<String> srandmember(String key) {
    return appendCommand(provider.getNode(key), commandObjects.srandmember(key));
  }

  @Override
  public Response<List<String>> srandmember(String key, int count) {
    return appendCommand(provider.getNode(key), commandObjects.srandmember(key, count));
  }

  @Override
  public Response<ScanResult<String>> sscan(String key, String cursor, ScanParams params) {
    return appendCommand(provider.getNode(key), commandObjects.sscan(key, cursor, params));
  }

  @Override
  public Response<Set<String>> sdiff(String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.sdiff(keys));
  }

  @Override
  public Response<Long> sdiffstore(String dstKey, String... keys) {
    return appendCommand(provider.getNode(dstKey), commandObjects.sdiffstore(dstKey, keys));
  }

  @Override
  public Response<Set<String>> sinter(String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.sinter(keys));
  }

  @Override
  public Response<Long> sinterstore(String dstKey, String... keys) {
    return appendCommand(provider.getNode(dstKey), commandObjects.sinterstore(dstKey, keys));
  }

  @Override
  public Response<Set<String>> sunion(String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.sunion(keys));
  }

  @Override
  public Response<Long> sunionstore(String dstKey, String... keys) {
    return appendCommand(provider.getNode(dstKey), commandObjects.sunionstore(dstKey, keys));
  }

  @Override
  public Response<Long> smove(String srcKey, String dstKey, String member) {
    return appendCommand(provider.getNode(srcKey), commandObjects.smove(srcKey, dstKey, member));
  }

  @Override
  public Response<Long> zadd(String key, double score, String member) {
    return appendCommand(provider.getNode(key), commandObjects.zadd(key, score, member));
  }

  @Override
  public Response<Long> zadd(String key, double score, String member, ZAddParams params) {
    return appendCommand(provider.getNode(key), commandObjects.zadd(key, score, member, params));
  }

  @Override
  public Response<Long> zadd(String key, Map<String, Double> scoreMembers) {
    return appendCommand(provider.getNode(key), commandObjects.zadd(key, scoreMembers));
  }

  @Override
  public Response<Long> zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
    return appendCommand(provider.getNode(key), commandObjects.zadd(key, scoreMembers, params));
  }

  @Override
  public Response<Double> zaddIncr(String key, double score, String member, ZAddParams params) {
    return appendCommand(provider.getNode(key), commandObjects.zaddIncr(key, score, member, params));
  }

  @Override
  public Response<Long> zrem(String key, String... members) {
    return appendCommand(provider.getNode(key), commandObjects.zrem(key, members));
  }

  @Override
  public Response<Double> zincrby(String key, double increment, String member) {
    return appendCommand(provider.getNode(key), commandObjects.zincrby(key, increment, member));
  }

  @Override
  public Response<Double> zincrby(String key, double increment, String member, ZIncrByParams params) {
    return appendCommand(provider.getNode(key), commandObjects.zincrby(key, increment, member, params));
  }

  @Override
  public Response<Long> zrank(String key, String member) {
    return appendCommand(provider.getNode(key), commandObjects.zrank(key, member));
  }

  @Override
  public Response<Long> zrevrank(String key, String member) {
    return appendCommand(provider.getNode(key), commandObjects.zrevrank(key, member));
  }

  @Override
  public Response<Set<String>> zrange(String key, long start, long stop) {
    return appendCommand(provider.getNode(key), commandObjects.zrange(key, start, stop));
  }

  @Override
  public Response<Set<String>> zrevrange(String key, long start, long stop) {
    return appendCommand(provider.getNode(key), commandObjects.zrevrange(key, start, stop));
  }

  @Override
  public Response<Set<Tuple>> zrangeWithScores(String key, long start, long stop) {
    return appendCommand(provider.getNode(key), commandObjects.zrangeWithScores(key, start, stop));
  }

  @Override
  public Response<Set<Tuple>> zrevrangeWithScores(String key, long start, long stop) {
    return appendCommand(provider.getNode(key), commandObjects.zrevrangeWithScores(key, start, stop));
  }

  @Override
  public Response<String> zrandmember(String key) {
    return appendCommand(provider.getNode(key), commandObjects.zrandmember(key));
  }

  @Override
  public Response<Set<String>> zrandmember(String key, long count) {
    return appendCommand(provider.getNode(key), commandObjects.zrandmember(key, count));
  }

  @Override
  public Response<Set<Tuple>> zrandmemberWithScores(String key, long count) {
    return appendCommand(provider.getNode(key), commandObjects.zrandmemberWithScores(key, count));
  }

  @Override
  public Response<Long> zcard(String key) {
    return appendCommand(provider.getNode(key), commandObjects.zcard(key));
  }

  @Override
  public Response<Double> zscore(String key, String member) {
    return appendCommand(provider.getNode(key), commandObjects.zscore(key, member));
  }

  @Override
  public Response<List<Double>> zmscore(String key, String... members) {    
    return appendCommand(provider.getNode(key), commandObjects.zmscore(key, members));
  }

  @Override
  public Response<Tuple> zpopmax(String key) {
    return appendCommand(provider.getNode(key), commandObjects.zpopmax(key));
  }

  @Override
  public Response<Set<Tuple>> zpopmax(String key, int count) {
    return appendCommand(provider.getNode(key), commandObjects.zpopmax(key, count));
  }

  @Override
  public Response<Tuple> zpopmin(String key) {
    return appendCommand(provider.getNode(key), commandObjects.zpopmin(key));
  }

  @Override
  public Response<Set<Tuple>> zpopmin(String key, int count) {
    return appendCommand(provider.getNode(key), commandObjects.zpopmin(key, count));
  }

  @Override
  public Response<Long> zcount(String key, double min, double max) {
    return appendCommand(provider.getNode(key), commandObjects.zcount(key, min, max));
  }

  @Override
  public Response<Long> zcount(String key, String min, String max) {
    return appendCommand(provider.getNode(key), commandObjects.zcount(key, min, max));
  }

  @Override
  public Response<Set<String>> zrangeByScore(String key, double min, double max) {
    return appendCommand(provider.getNode(key), commandObjects.zrangeByScore(key, min, max));
  }

  @Override
  public Response<Set<String>> zrangeByScore(String key, String min, String max) {
    return appendCommand(provider.getNode(key), commandObjects.zrangeByScore(key, min, max));
  }

  @Override
  public Response<Set<String>> zrevrangeByScore(String key, double max, double min) {
    return appendCommand(provider.getNode(key), commandObjects.zrevrangeByScore(key, max, min));

  }

  @Override
  public Response<Set<String>> zrangeByScore(String key, double min, double max, int offset, int count) {
    return appendCommand(provider.getNode(key), commandObjects.zrangeByScore(key, min, max, offset, count));
  }

  @Override
  public Response<Set<String>> zrevrangeByScore(String key, String max, String min) {
    return appendCommand(provider.getNode(key), commandObjects.zrevrangeByScore(key, max, min));
  }

  @Override
  public Response<Set<String>> zrangeByScore(String key, String min, String max, int offset, int count) {
    return appendCommand(provider.getNode(key), commandObjects.zrangeByScore(key, min, max, offset, count));
  }

  @Override
  public Response<Set<String>> zrevrangeByScore(String key, double max, double min, int offset, int count) {
    return appendCommand(provider.getNode(key), commandObjects.zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max) {
    return appendCommand(provider.getNode(key), commandObjects.zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min) {
    return appendCommand(provider.getNode(key), commandObjects.zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
    return appendCommand(provider.getNode(key), commandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public Response<Set<String>> zrevrangeByScore(String key, String max, String min, int offset, int count) {
    return appendCommand(provider.getNode(key), commandObjects.zrevrangeByScore(key, max, min, offset, count));
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(String key, String min, String max) {
    return appendCommand(provider.getNode(key), commandObjects.zrangeByScoreWithScores(key, min, max));
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min) {
    return appendCommand(provider.getNode(key), commandObjects.zrevrangeByScoreWithScores(key, max, min));
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
    return appendCommand(provider.getNode(key), commandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
    return appendCommand(provider.getNode(key), commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
    return appendCommand(provider.getNode(key), commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
  }

  @Override
  public Response<Long> zremrangeByRank(String key, long start, long stop) {
    return appendCommand(provider.getNode(key), commandObjects.zremrangeByRank(key, start, stop));
  }

  @Override
  public Response<Long> zremrangeByScore(String key, double min, double max) {
    return appendCommand(provider.getNode(key), commandObjects.zremrangeByScore(key, min, max));
  }

  @Override
  public Response<Long> zremrangeByScore(String key, String min, String max) {
    return appendCommand(provider.getNode(key), commandObjects.zremrangeByScore(key, min, max));
  }

  @Override
  public Response<Long> zlexcount(String key, String min, String max) {
    return appendCommand(provider.getNode(key), commandObjects.zlexcount(key, min, max));
  }

  @Override
  public Response<Set<String>> zrangeByLex(String key, String min, String max) {
    return appendCommand(provider.getNode(key), commandObjects.zrangeByLex(key, min, max));
  }

  @Override
  public Response<Set<String>> zrangeByLex(String key, String min, String max, int offset, int count) {
    return appendCommand(provider.getNode(key), commandObjects.zrangeByLex(key, min, max, offset, count));
  }

  @Override
  public Response<Set<String>> zrevrangeByLex(String key, String max, String min) {
    return appendCommand(provider.getNode(key), commandObjects.zrevrangeByLex(key, max, min));
  }

  @Override
  public Response<Set<String>> zrevrangeByLex(String key, String max, String min, int offset, int count) {
    return appendCommand(provider.getNode(key), commandObjects.zrevrangeByLex(key, max, min, offset, count));
  }

  @Override
  public Response<Long> zremrangeByLex(String key, String min, String max) {
    return appendCommand(provider.getNode(key), commandObjects.zremrangeByLex(key, min, max));
  }

  @Override
  public Response<ScanResult<Tuple>> zscan(String key, String cursor, ScanParams params) {
    return appendCommand(provider.getNode(key), commandObjects.zscan(key, cursor, params));
  }

  @Override
  public Response<KeyedZSetElement> bzpopmax(double timeout, String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.bzpopmax(timeout, keys));
  }

  @Override
  public Response<KeyedZSetElement> bzpopmin(double timeout, String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.bzpopmin(timeout, keys));
  }

  @Override
  public Response<Set<String>> zdiff(String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.zdiff(keys));
  }

  @Override
  public Response<Set<Tuple>> zdiffWithScores(String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.zdiffWithScores(keys));
  }

  @Override
  public Response<Long> zdiffStore(String dstKey, String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.zdiffStore(dstKey, keys));
  }

  @Override
  public Response<Long> zinterstore(String dstKey, String... sets) {
    return appendCommand(provider.getNode(dstKey), commandObjects.zinterstore(dstKey, sets));
  }

  @Override
  public Response<Long> zinterstore(String dstKey, ZParams params, String... sets) {
    return appendCommand(provider.getNode(dstKey), commandObjects.zinterstore(dstKey, params, sets));
  }

  @Override
  public Response<Set<String>> zinter(ZParams params, String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.zinter(params, keys));
  }

  @Override
  public Response<Set<Tuple>> zinterWithScores(ZParams params, String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.zinterWithScores(params, keys));
  }

  @Override
  public Response<Set<String>> zunion(ZParams params, String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.zunion(params, keys));
  }

  @Override
  public Response<Set<Tuple>> zunionWithScores(ZParams params, String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.zunionWithScores(params, keys));
  }

  @Override
  public Response<Long> zunionstore(String dstKey, String... sets) {
    return appendCommand(provider.getNode(dstKey), commandObjects.zunionstore(dstKey, sets));
  }

  @Override
  public Response<Long> zunionstore(String dstKey, ZParams params, String... sets) {
    return appendCommand(provider.getNode(dstKey), commandObjects.zunionstore(dstKey, params, sets));
  }

  @Override
  public Response<Long> geoadd(String key, double longitude, double latitude, String member) {
    return appendCommand(provider.getNode(key), commandObjects.geoadd(key, longitude, latitude, member));
  }

  @Override
  public Response<Long> geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
    return appendCommand(provider.getNode(key), commandObjects.geoadd(key, memberCoordinateMap));
  }

  @Override
  public Response<Long> geoadd(String key, GeoAddParams params, Map<String, GeoCoordinate> memberCoordinateMap) {
    return appendCommand(provider.getNode(key), commandObjects.geoadd(key, params, memberCoordinateMap));
  }

  @Override
  public Response<Double> geodist(String key, String member1, String member2) {
    return appendCommand(provider.getNode(key), commandObjects.geodist(key, member1, member2));
  }

  @Override
  public Response<Double> geodist(String key, String member1, String member2, GeoUnit unit) {
    return appendCommand(provider.getNode(key), commandObjects.geodist(key, member1, member2, unit));
  }

  @Override
  public Response<List<String>> geohash(String key, String... members) {
    return appendCommand(provider.getNode(key), commandObjects.geohash(key, members));
  }

  @Override
  public Response<List<GeoCoordinate>> geopos(String key, String... members) {
    return appendCommand(provider.getNode(key), commandObjects.geopos(key, members));
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
    return appendCommand(provider.getNode(key), commandObjects.georadius(key, longitude, latitude, radius, unit));
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusReadonly(String key, double longitude, double latitude, double radius, GeoUnit unit) {
    return appendCommand(provider.getNode(key), commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit));
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
    return appendCommand(provider.getNode(key), commandObjects.georadius(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusReadonly(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
    return appendCommand(provider.getNode(key), commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit, param));
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
    return appendCommand(provider.getNode(key), commandObjects.georadiusByMember(key, member, radius, unit));
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusByMemberReadonly(String key, String member, double radius, GeoUnit unit) {
    return appendCommand(provider.getNode(key), commandObjects.georadiusByMemberReadonly(key, member, radius, unit));
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
    return appendCommand(provider.getNode(key), commandObjects.georadiusByMember(key, member, radius, unit, param));
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusByMemberReadonly(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
    return appendCommand(provider.getNode(key), commandObjects.georadiusByMemberReadonly(key, member, radius, unit, param));
  }

  @Override
  public Response<Long> georadiusStore(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    return appendCommand(provider.getNode(key), commandObjects.georadiusStore(key, longitude, latitude, radius, unit, param, storeParam));
  }

  @Override
  public Response<Long> georadiusByMemberStore(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
    return appendCommand(provider.getNode(key), commandObjects.georadiusByMemberStore(key, member, radius, unit, param, storeParam));
  }

  @Override
  public Response<Long> pfadd(String key, String... elements) {
    return appendCommand(provider.getNode(key), commandObjects.pfadd(key, elements));
  }

  @Override
  public Response<String> pfmerge(String destkey, String... sourcekeys) {
    return appendCommand(provider.getNode(destkey), commandObjects.pfmerge(destkey, sourcekeys));
  }

  @Override
  public Response<Long> pfcount(String key) {
    return appendCommand(provider.getNode(key), commandObjects.pfcount(key));
  }

  @Override
  public Response<Long> pfcount(String... keys) {
    return appendCommand(provider.getNode(keys[0]), commandObjects.pfcount(keys));
  }
}
