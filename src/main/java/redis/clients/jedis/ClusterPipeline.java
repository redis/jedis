package redis.clients.jedis;

import redis.clients.jedis.args.BitOP;
import redis.clients.jedis.args.BitPosParams;
import redis.clients.jedis.args.ListDirection;
import redis.clients.jedis.args.ListPosition;
import redis.clients.jedis.commands.PipelineCommands;
import redis.clients.jedis.params.*;
import redis.clients.jedis.providers.JedisClusterConnectionProvider;
import redis.clients.jedis.resps.KeyedListElement;
import redis.clients.jedis.resps.LCSMatchResult;
import redis.clients.jedis.resps.ScanResult;

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
}
