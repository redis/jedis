package redis.clients.jedis;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.commands.BinaryRedisPipeline;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.commands.RedisPipeline;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;

public abstract class PipelineBase extends Queable implements BinaryRedisPipeline, RedisPipeline {

  protected abstract Client getClient(String key);

  protected abstract Client getClient(byte[] key);

  @Override
  public Response<Long> append(final String key, final String value) {
    getClient(key).append(key, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> append(final byte[] key, final byte[] value) {
    getClient(key).append(key, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<List<String>> blpop(final String key) {
    String[] temp = new String[1];
    temp[0] = key;
    getClient(key).blpop(temp);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<List<String>> brpop(final String key) {
    String[] temp = new String[1];
    temp[0] = key;
    getClient(key).brpop(temp);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<List<byte[]>> blpop(final byte[] key) {
    byte[][] temp = new byte[1][];
    temp[0] = key;
    getClient(key).blpop(temp);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  @Override
  public Response<List<byte[]>> brpop(final byte[] key) {
    byte[][] temp = new byte[1][];
    temp[0] = key;
    getClient(key).brpop(temp);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  @Override
  public Response<Long> decr(final String key) {
    getClient(key).decr(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> decr(final byte[] key) {
    getClient(key).decr(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> decrBy(final String key, final long decrement) {
    getClient(key).decrBy(key, decrement);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> decrBy(final byte[] key, final long decrement) {
    getClient(key).decrBy(key, decrement);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> del(final String key) {
    getClient(key).del(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> del(final byte[] key) {
    getClient(key).del(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> unlink(final String key) {
    getClient(key).unlink(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> unlink(final byte[] key) {
    getClient(key).unlink(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> echo(final String string) {
    getClient(string).echo(string);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<byte[]> echo(final byte[] string) {
    getClient(string).echo(string);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<Boolean> exists(final String key) {
    getClient(key).exists(key);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  @Override
  public Response<Boolean> exists(final byte[] key) {
    getClient(key).exists(key);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  @Override
  public Response<Long> expire(final String key, final int seconds) {
    getClient(key).expire(key, seconds);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> expire(final byte[] key, final int seconds) {
    getClient(key).expire(key, seconds);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> expireAt(final String key, final long unixTime) {
    getClient(key).expireAt(key, unixTime);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> expireAt(final byte[] key, final long unixTime) {
    getClient(key).expireAt(key, unixTime);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> get(final String key) {
    getClient(key).get(key);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<byte[]> get(final byte[] key) {
    getClient(key).get(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<Boolean> getbit(final String key, final long offset) {
    getClient(key).getbit(key, offset);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  @Override
  public Response<Boolean> getbit(final byte[] key, final long offset) {
    getClient(key).getbit(key, offset);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  @Override
  public Response<Long> bitpos(final String key, final boolean value) {
    return bitpos(key, value, new BitPosParams());
  }

  @Override
  public Response<Long> bitpos(final String key, final boolean value, final BitPosParams params) {
    getClient(key).bitpos(key, value, params);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> bitpos(final byte[] key, final boolean value) {
    return bitpos(key, value, new BitPosParams());
  }

  @Override
  public Response<Long> bitpos(final byte[] key, final boolean value, final BitPosParams params) {
    getClient(key).bitpos(key, value, params);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> getrange(final String key, final long startOffset, final long endOffset) {
    getClient(key).getrange(key, startOffset, endOffset);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> getSet(final String key, final String value) {
    getClient(key).getSet(key, value);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<byte[]> getSet(final byte[] key, final byte[] value) {
    getClient(key).getSet(key, value);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<byte[]> getrange(final byte[] key, final long startOffset, final long endOffset) {
    getClient(key).getrange(key, startOffset, endOffset);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<Long> hdel(final String key, final String... field) {
    getClient(key).hdel(key, field);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> hdel(final byte[] key, final byte[]... field) {
    getClient(key).hdel(key, field);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Boolean> hexists(final String key, final String field) {
    getClient(key).hexists(key, field);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  @Override
  public Response<Boolean> hexists(final byte[] key, final byte[] field) {
    getClient(key).hexists(key, field);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  @Override
  public Response<String> hget(final String key, final String field) {
    getClient(key).hget(key, field);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<byte[]> hget(final byte[] key, final byte[] field) {
    getClient(key).hget(key, field);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<Map<String, String>> hgetAll(final String key) {
    getClient(key).hgetAll(key);
    return getResponse(BuilderFactory.STRING_MAP);
  }

  @Override
  public Response<Map<byte[], byte[]>> hgetAll(final byte[] key) {
    getClient(key).hgetAll(key);
    return getResponse(BuilderFactory.BYTE_ARRAY_MAP);
  }

  @Override
  public Response<Long> hincrBy(final String key, final String field, final long value) {
    getClient(key).hincrBy(key, field, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> hincrBy(final byte[] key, final byte[] field, final long value) {
    getClient(key).hincrBy(key, field, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Set<String>> hkeys(final String key) {
    getClient(key).hkeys(key);
    return getResponse(BuilderFactory.STRING_SET);
  }

  @Override
  public Response<Set<byte[]>> hkeys(final byte[] key) {
    getClient(key).hkeys(key);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Long> hlen(final String key) {
    getClient(key).hlen(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> hlen(final byte[] key) {
    getClient(key).hlen(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<List<String>> hmget(final String key, final String... fields) {
    getClient(key).hmget(key, fields);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<List<byte[]>> hmget(final byte[] key, final byte[]... fields) {
    getClient(key).hmget(key, fields);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  @Override
  public Response<String> hmset(final String key, final Map<String, String> hash) {
    getClient(key).hmset(key, hash);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> hmset(final byte[] key, final Map<byte[], byte[]> hash) {
    getClient(key).hmset(key, hash);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Long> hset(final String key, final String field, final String value) {
    getClient(key).hset(key, field, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> hset(final byte[] key, final byte[] field, final byte[] value) {
    getClient(key).hset(key, field, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> hset(final String key, final Map<String, String> hash) {
    getClient(key).hset(key, hash);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> hset(final byte[] key, final Map<byte[], byte[]> hash) {
    getClient(key).hset(key, hash);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> hsetnx(final String key, final String field, final String value) {
    getClient(key).hsetnx(key, field, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> hsetnx(final byte[] key, final byte[] field, final byte[] value) {
    getClient(key).hsetnx(key, field, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<List<String>> hvals(final String key) {
    getClient(key).hvals(key);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<List<byte[]>> hvals(final byte[] key) {
    getClient(key).hvals(key);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  @Override
  public Response<Long> incr(final String key) {
    getClient(key).incr(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> incr(final byte[] key) {
    getClient(key).incr(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> incrBy(final String key, final long increment) {
    getClient(key).incrBy(key, increment);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> incrBy(final byte[] key, final long increment) {
    getClient(key).incrBy(key, increment);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> lindex(final String key, final long index) {
    getClient(key).lindex(key, index);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<byte[]> lindex(final byte[] key, final long index) {
    getClient(key).lindex(key, index);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<Long> linsert(final String key, final ListPosition where, final String pivot, final String value) {
    getClient(key).linsert(key, where, pivot, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> linsert(final byte[] key, final ListPosition where, final byte[] pivot, final byte[] value) {
    getClient(key).linsert(key, where, pivot, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> llen(final String key) {
    getClient(key).llen(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> llen(final byte[] key) {
    getClient(key).llen(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> lpop(final String key) {
    getClient(key).lpop(key);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<byte[]> lpop(final byte[] key) {
    getClient(key).lpop(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<Long> lpush(final String key, final String... string) {
    getClient(key).lpush(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> lpush(final byte[] key, final byte[]... string) {
    getClient(key).lpush(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> lpushx(final String key, final String... string) {
    getClient(key).lpushx(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> lpushx(final byte[] key, final byte[]... bytes) {
    getClient(key).lpushx(key, bytes);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<List<String>> lrange(final String key, final long start, final long stop) {
    getClient(key).lrange(key, start, stop);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<List<byte[]>> lrange(final byte[] key, final long start, final long stop) {
    getClient(key).lrange(key, start, stop);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  @Override
  public Response<Long> lrem(final String key, final long count, final String value) {
    getClient(key).lrem(key, count, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> lrem(final byte[] key, final long count, final byte[] value) {
    getClient(key).lrem(key, count, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> lset(final String key, final long index, final String value) {
    getClient(key).lset(key, index, value);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> lset(final byte[] key, final long index, final byte[] value) {
    getClient(key).lset(key, index, value);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> ltrim(final String key, final long start, final long stop) {
    getClient(key).ltrim(key, start, stop);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> ltrim(final byte[] key, final long start, final long stop) {
    getClient(key).ltrim(key, start, stop);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Long> move(final String key, final int dbIndex) {
    getClient(key).move(key, dbIndex);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> move(final byte[] key, final int dbIndex) {
    getClient(key).move(key, dbIndex);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> persist(final String key) {
    getClient(key).persist(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> persist(final byte[] key) {
    getClient(key).persist(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> rpop(final String key) {
    getClient(key).rpop(key);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<byte[]> rpop(final byte[] key) {
    getClient(key).rpop(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<Long> rpush(final String key, final String... string) {
    getClient(key).rpush(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> rpush(final byte[] key, final byte[]... string) {
    getClient(key).rpush(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> rpushx(final String key, final String... string) {
    getClient(key).rpushx(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> rpushx(final byte[] key, final byte[]... string) {
    getClient(key).rpushx(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> sadd(final String key, final String... member) {
    getClient(key).sadd(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> sadd(final byte[] key, final byte[]... member) {
    getClient(key).sadd(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> scard(final String key) {
    getClient(key).scard(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> scard(final byte[] key) {
    getClient(key).scard(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> set(final String key, final String value) {
    getClient(key).set(key, value);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> set(final byte[] key, final byte[] value) {
    getClient(key).set(key, value);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> set(final String key, final String value, SetParams params) {
    getClient(key).set(key, value, params);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> set(final byte[] key, final byte[] value, SetParams params) {
    getClient(key).set(key, value, params);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Boolean> setbit(final String key, final long offset, boolean value) {
    getClient(key).setbit(key, offset, value);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  @Override
  public Response<Boolean> setbit(final byte[] key, final long offset, final byte[] value) {
    getClient(key).setbit(key, offset, value);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  @Override
  public Response<String> setex(final String key, final int seconds, final String value) {
    getClient(key).setex(key, seconds, value);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> setex(final byte[] key, final int seconds, final byte[] value) {
    getClient(key).setex(key, seconds, value);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Long> setnx(final String key, final String value) {
    getClient(key).setnx(key, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> setnx(final byte[] key, final byte[] value) {
    getClient(key).setnx(key, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> setrange(final String key, final long offset, final String value) {
    getClient(key).setrange(key, offset, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> setrange(final byte[] key, final long offset, final byte[] value) {
    getClient(key).setrange(key, offset, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Boolean> sismember(final String key, final String member) {
    getClient(key).sismember(key, member);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  @Override
  public Response<Boolean> sismember(final byte[] key, final byte[] member) {
    getClient(key).sismember(key, member);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  @Override
  public Response<Set<String>> smembers(final String key) {
    getClient(key).smembers(key);
    return getResponse(BuilderFactory.STRING_SET);
  }

  @Override
  public Response<Set<byte[]>> smembers(final byte[] key) {
    getClient(key).smembers(key);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<List<String>> sort(final String key) {
    getClient(key).sort(key);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<List<byte[]>> sort(final byte[] key) {
    getClient(key).sort(key);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  @Override
  public Response<List<String>> sort(final String key, final SortingParams sortingParameters) {
    getClient(key).sort(key, sortingParameters);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<List<byte[]>> sort(final byte[] key, final SortingParams sortingParameters) {
    getClient(key).sort(key, sortingParameters);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  @Override
  public Response<String> spop(final String key) {
    getClient(key).spop(key);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Set<String>> spop(final String key, final long count) {
    getClient(key).spop(key, count);
    return getResponse(BuilderFactory.STRING_SET);
  }

  @Override
  public Response<byte[]> spop(final byte[] key) {
    getClient(key).spop(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<Set<byte[]>> spop(final byte[] key, final long count) {
    getClient(key).spop(key, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<String> srandmember(final String key) {
    getClient(key).srandmember(key);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<List<String>> srandmember(final String key, final int count) {
    getClient(key).srandmember(key, count);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<byte[]> srandmember(final byte[] key) {
    getClient(key).srandmember(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<List<byte[]>> srandmember(final byte[] key, final int count) {
    getClient(key).srandmember(key, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  @Override
  public Response<Long> srem(final String key, final String... member) {
    getClient(key).srem(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> srem(final byte[] key, final byte[]... member) {
    getClient(key).srem(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> strlen(final String key) {
    getClient(key).strlen(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> strlen(final byte[] key) {
    getClient(key).strlen(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> substr(final String key, final int start, final int end) {
    getClient(key).substr(key, start, end);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> substr(final byte[] key, final int start, final int end) {
    getClient(key).substr(key, start, end);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Long> touch(final String key) {
    getClient(key).touch(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> touch(final byte[] key) {
    getClient(key).touch(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> ttl(final String key) {
    getClient(key).ttl(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> ttl(final byte[] key) {
    getClient(key).ttl(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> type(final String key) {
    getClient(key).type(key);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> type(final byte[] key) {
    getClient(key).type(key);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Long> zadd(final String key, final double score, final String member) {
    getClient(key).zadd(key, score, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zadd(final String key, final double score, final String member, final ZAddParams params) {
    getClient(key).zadd(key, score, member, params);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zadd(final String key, final Map<String, Double> scoreMembers) {
    getClient(key).zadd(key, scoreMembers);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zadd(final String key, final Map<String, Double> scoreMembers, final ZAddParams params) {
    getClient(key).zadd(key, scoreMembers, params);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zadd(final byte[] key, final double score, final byte[] member) {
    getClient(key).zadd(key, score, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zadd(final byte[] key, final double score, final byte[] member, final ZAddParams params) {
    getClient(key).zadd(key, score, member, params);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zadd(final byte[] key, final Map<byte[], Double> scoreMembers) {
    getClient(key).zadd(key, scoreMembers);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zadd(final byte[] key, final Map<byte[], Double> scoreMembers, final ZAddParams params) {
    getClient(key).zadd(key, scoreMembers, params);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zcard(final String key) {
    getClient(key).zcard(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zcard(final byte[] key) {
    getClient(key).zcard(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zcount(final String key, final double min, final double max) {
    getClient(key).zcount(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zcount(final String key, final String min, final String max) {
    getClient(key).zcount(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zcount(final byte[] key, final double min, final double max) {
    getClient(key).zcount(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zcount(final byte[] key, final byte[] min, final byte[] max) {
    getClient(key).zcount(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Double> zincrby(final String key, final double increment, final String member) {
    getClient(key).zincrby(key, increment, member);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Double> zincrby(final String key, final double increment, final String member, ZIncrByParams params) {
    getClient(key).zincrby(key, increment, member, params);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Double> zincrby(final byte[] key, final double increment, final byte[] member) {
    getClient(key).zincrby(key, increment, member);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Double> zincrby(final byte[] key, final double increment, final byte[] member, ZIncrByParams params) {
    getClient(key).zincrby(key, increment, member);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Set<String>> zrange(final String key, final long start, final long stop) {
    getClient(key).zrange(key, start, stop);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrange(final byte[] key, final long start, final long stop) {
    getClient(key).zrange(key, start, stop);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<String>> zrangeByScore(final String key, final double min, final double max) {
    getClient(key).zrangeByScore(key, min, max);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrangeByScore(final byte[] key, final double min, final double max) {
    getClient(key).zrangeByScore(key, min, max);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<String>> zrangeByScore(final String key, final String min, final String max) {
    getClient(key).zrangeByScore(key, min, max);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrangeByScore(final byte[] key, final byte[] min, final byte[] max) {
    getClient(key).zrangeByScore(key, min, max);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<String>> zrangeByScore(final String key, final double min, final double max, final int offset,
      final int count) {
    getClient(key).zrangeByScore(key, min, max, offset, count);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<String>> zrangeByScore(final String key, final String min, final String max, final int offset,
      final int count) {
    getClient(key).zrangeByScore(key, min, max, offset, count);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrangeByScore(final byte[] key, final double min, final double max, final int offset,
      final int count) {
    getClient(key).zrangeByScore(key, min, max, offset, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrangeByScore(final byte[] key, final byte[] min, final byte[] max, final int offset,
      final int count) {
    getClient(key).zrangeByScore(key, min, max, offset, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(final String key, final double min, final double max) {
    getClient(key).zrangeByScoreWithScores(key, min, max);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(final String key, final String min, final String max) {
    getClient(key).zrangeByScoreWithScores(key, min, max);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(final byte[] key, final double min, final double max) {
    getClient(key).zrangeByScoreWithScores(key, min, max);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(final byte[] key, final byte[] min, final byte[] max) {
    getClient(key).zrangeByScoreWithScores(key, min, max);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(final String key, final double min, final double max,
      final int offset, final int count) {
    getClient(key).zrangeByScoreWithScores(key, min, max, offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(final String key, final String min, final String max,
      final int offset, final int count) {
    getClient(key).zrangeByScoreWithScores(key, min, max, offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(final byte[] key, final double min, final double max,
      final int offset, final int count) {
    getClient(key).zrangeByScoreWithScores(key, min, max, offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(final byte[] key, final byte[] min, final byte[] max,
      final int offset, final int count) {
    getClient(key).zrangeByScoreWithScores(key, min, max, offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<String>> zrevrangeByScore(final String key, final double max, final double min) {
    getClient(key).zrevrangeByScore(key, max, min);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrevrangeByScore(final byte[] key, final double max, final double min) {
    getClient(key).zrevrangeByScore(key, max, min);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<String>> zrevrangeByScore(final String key, final String max, final String min) {
    getClient(key).zrevrangeByScore(key, max, min);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrevrangeByScore(final byte[] key, final byte[] max, final byte[] min) {
    getClient(key).zrevrangeByScore(key, max, min);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<String>> zrevrangeByScore(final String key, final double max, final double min, final int offset,
      final int count) {
    getClient(key).zrevrangeByScore(key, max, min, offset, count);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<String>> zrevrangeByScore(final String key, final String max, final String min, final int offset,
      final int count) {
    getClient(key).zrevrangeByScore(key, max, min, offset, count);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrevrangeByScore(final byte[] key, final double max, final double min, final int offset,
      final int count) {
    getClient(key).zrevrangeByScore(key, max, min, offset, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrevrangeByScore(final byte[] key, final byte[] max, final byte[] min, final int offset,
      final int count) {
    getClient(key).zrevrangeByScore(key, max, min, offset, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(final String key, final double max, final double min) {
    getClient(key).zrevrangeByScoreWithScores(key, max, min);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(final String key, final String max, final String min) {
    getClient(key).zrevrangeByScoreWithScores(key, max, min);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(final byte[] key, final double max, final double min) {
    getClient(key).zrevrangeByScoreWithScores(key, max, min);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(final byte[] key, final byte[] max, final byte[] min) {
    getClient(key).zrevrangeByScoreWithScores(key, max, min);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(final String key, final double max, final double min,
      final int offset, final int count) {
    getClient(key).zrevrangeByScoreWithScores(key, max, min, offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(final String key, final String max, final String min,
      final int offset, final int count) {
    getClient(key).zrevrangeByScoreWithScores(key, max, min, offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(final byte[] key, final double max, final double min,
      final int offset, final int count) {
    getClient(key).zrevrangeByScoreWithScores(key, max, min, offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(final byte[] key, final byte[] max, final byte[] min,
      final int offset, final int count) {
    getClient(key).zrevrangeByScoreWithScores(key, max, min, offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrangeWithScores(final String key, final long start, final long stop) {
    getClient(key).zrangeWithScores(key, start, stop);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrangeWithScores(final byte[] key, final long start, final long stop) {
    getClient(key).zrangeWithScores(key, start, stop);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Long> zrank(final String key, final String member) {
    getClient(key).zrank(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zrank(final byte[] key, final byte[] member) {
    getClient(key).zrank(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zrem(final String key, final String... members) {
    getClient(key).zrem(key, members);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zrem(final byte[] key, final byte[]... members) {
    getClient(key).zrem(key, members);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zremrangeByRank(final String key, final long start, final long stop) {
    getClient(key).zremrangeByRank(key, start, stop);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zremrangeByRank(final byte[] key, final long start, final long stop) {
    getClient(key).zremrangeByRank(key, start, stop);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zremrangeByScore(final String key, final double min, final double max) {
    getClient(key).zremrangeByScore(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zremrangeByScore(final String key, final String min, final String max) {
    getClient(key).zremrangeByScore(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zremrangeByScore(final byte[] key, final double min, final double max) {
    getClient(key).zremrangeByScore(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zremrangeByScore(final byte[] key, final byte[] min, final byte[] max) {
    getClient(key).zremrangeByScore(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Set<String>> zrevrange(final String key, final long start, final long stop) {
    getClient(key).zrevrange(key, start, stop);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrevrange(final byte[] key, final long start, final long stop) {
    getClient(key).zrevrange(key, start, stop);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrevrangeWithScores(final String key, final long start, final long stop) {
    getClient(key).zrevrangeWithScores(key, start, stop);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrevrangeWithScores(final byte[] key, final long start, final long stop) {
    getClient(key).zrevrangeWithScores(key, start, stop);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Long> zrevrank(final String key, final String member) {
    getClient(key).zrevrank(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zrevrank(final byte[] key, final byte[] member) {
    getClient(key).zrevrank(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Double> zscore(final String key, final String member) {
    getClient(key).zscore(key, member);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Double> zscore(final byte[] key, final byte[] member) {
    getClient(key).zscore(key, member);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Tuple> zpopmax(final String key) {
    getClient(key).zpopmax(key);
    return getResponse(BuilderFactory.TUPLE);
  }

  @Override
  public Response<Tuple> zpopmax(final byte[] key) {
    getClient(key).zpopmax(key);
    return getResponse(BuilderFactory.TUPLE);
  }

  @Override
  public Response<Set<Tuple>> zpopmax(final String key, final int count) {
    getClient(key).zpopmax(key, count);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zpopmax(final byte[] key, final int count) {
    getClient(key).zpopmax(key, count);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Tuple> zpopmin(final String key) {
    getClient(key).zpopmin(key);
    return getResponse(BuilderFactory.TUPLE);
  }

  @Override
  public Response<Tuple> zpopmin(final byte[] key) {
    getClient(key).zpopmin(key);
    return getResponse(BuilderFactory.TUPLE);
  }

  @Override
  public Response<Set<Tuple>> zpopmin(final byte[] key, final int count) {
    getClient(key).zpopmin(key, count);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zpopmin(final String key, final int count) {
    getClient(key).zpopmin(key, count);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Long> zlexcount(final byte[] key, final byte[] min, final byte[] max) {
    getClient(key).zlexcount(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zlexcount(final String key, final String min, final String max) {
    getClient(key).zlexcount(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Set<byte[]>> zrangeByLex(final byte[] key, final byte[] min, final byte[] max) {
    getClient(key).zrangeByLex(key, min, max);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<String>> zrangeByLex(final String key, final String min, final String max) {
    getClient(key).zrangeByLex(key, min, max);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrangeByLex(final byte[] key, final byte[] min, final byte[] max,
      final int offset, final int count) {
    getClient(key).zrangeByLex(key, min, max, offset, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<String>> zrangeByLex(final String key, final String min, final String max,
      final int offset, final int count) {
    getClient(key).zrangeByLex(key, min, max, offset, count);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrevrangeByLex(final byte[] key, final byte[] max, final byte[] min) {
    getClient(key).zrevrangeByLex(key, max, min);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<String>> zrevrangeByLex(final String key, final String max, final String min) {
    getClient(key).zrevrangeByLex(key, max, min);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrevrangeByLex(final byte[] key, final byte[] max, final byte[] min,
      final int offset, final int count) {
    getClient(key).zrevrangeByLex(key, max, min, offset, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<String>> zrevrangeByLex(final String key, final String max, final String min,
      final int offset, final int count) {
    getClient(key).zrevrangeByLex(key, max, min, offset, count);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Long> zremrangeByLex(final byte[] key, final byte[] min, final byte[] max) {
    getClient(key).zremrangeByLex(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zremrangeByLex(final String key, final String min, final String max) {
    getClient(key).zremrangeByLex(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> bitcount(final String key) {
    getClient(key).bitcount(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> bitcount(final String key, final long start, final long end) {
    getClient(key).bitcount(key, start, end);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> bitcount(final byte[] key) {
    getClient(key).bitcount(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> bitcount(final byte[] key, final long start, final long end) {
    getClient(key).bitcount(key, start, end);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<byte[]> dump(final String key) {
    getClient(key).dump(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<byte[]> dump(final byte[] key) {
    getClient(key).dump(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<String> migrate(final String host, final int port,
      final String key, final int destinationDb, final int timeout) {
    getClient(key).migrate(host, port, key, destinationDb, timeout);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> migrate(final String host, final int port,
      final byte[] key, final int destinationDb, final int timeout) {
    getClient(key).migrate(host, port, key, destinationDb, timeout);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Long> objectRefcount(final String key) {
    getClient(key).objectRefcount(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> objectRefcount(final byte[] key) {
    getClient(key).objectRefcount(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> objectEncoding(final String key) {
    getClient(key).objectEncoding(key);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<byte[]> objectEncoding(final byte[] key) {
    getClient(key).objectEncoding(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<Long> objectIdletime(final String key) {
    getClient(key).objectIdletime(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> objectIdletime(final byte[] key) {
    getClient(key).objectIdletime(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> objectFreq(byte[] key) {
    getClient(key).objectFreq(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> objectFreq(String key) {
    getClient(key).objectFreq(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pexpire(final String key, final long milliseconds) {
    getClient(key).pexpire(key, milliseconds);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pexpire(final byte[] key, final long milliseconds) {
    getClient(key).pexpire(key, milliseconds);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pexpireAt(final String key, final long millisecondsTimestamp) {
    getClient(key).pexpireAt(key, millisecondsTimestamp);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pexpireAt(final byte[] key, final long millisecondsTimestamp) {
    getClient(key).pexpireAt(key, millisecondsTimestamp);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pttl(final String key) {
    getClient(key).pttl(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pttl(final byte[] key) {
    getClient(key).pttl(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> restore(final String key, final int ttl, final byte[] serializedValue) {
    getClient(key).restore(key, ttl, serializedValue);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> restore(final byte[] key, final int ttl, final byte[] serializedValue) {
    getClient(key).restore(key, ttl, serializedValue);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> restoreReplace(final String key, final int ttl, final byte[] serializedValue) {
    getClient(key).restoreReplace(key, ttl, serializedValue);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> restoreReplace(final byte[] key, final int ttl, final byte[] serializedValue) {
    getClient(key).restoreReplace(key, ttl, serializedValue);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Double> incrByFloat(final String key, final double increment) {
    getClient(key).incrByFloat(key, increment);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Double> incrByFloat(final byte[] key, final double increment) {
    getClient(key).incrByFloat(key, increment);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<String> psetex(final String key, final long milliseconds, final String value) {
    getClient(key).psetex(key, milliseconds, value);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> psetex(final byte[] key, final long milliseconds, final byte[] value) {
    getClient(key).psetex(key, milliseconds, value);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Double> hincrByFloat(final String key, final String field, final double increment) {
    getClient(key).hincrByFloat(key, field, increment);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Double> hincrByFloat(final byte[] key, final byte[] field, final double increment) {
    getClient(key).hincrByFloat(key, field, increment);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Long> pfadd(final byte[] key, final byte[]... elements) {
    getClient(key).pfadd(key, elements);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pfcount(final byte[] key) {
    getClient(key).pfcount(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pfadd(final String key, final String... elements) {
    getClient(key).pfadd(key, elements);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pfcount(final String key) {
    getClient(key).pfcount(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> geoadd(final byte[] key, final double longitude, final double latitude, final byte[] member) {
    getClient(key).geoadd(key, longitude, latitude, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> geoadd(final byte[] key, final Map<byte[], GeoCoordinate> memberCoordinateMap) {
    getClient(key).geoadd(key, memberCoordinateMap);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> geoadd(final String key, final double longitude, final double latitude, final String member) {
    getClient(key).geoadd(key, longitude, latitude, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> geoadd(final String key, final Map<String, GeoCoordinate> memberCoordinateMap) {
    getClient(key).geoadd(key, memberCoordinateMap);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Double> geodist(final byte[] key, final byte[] member1, final byte[] member2) {
    getClient(key).geodist(key, member1, member2);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Double> geodist(final byte[] key, final byte[] member1, final byte[] member2, final GeoUnit unit) {
    getClient(key).geodist(key, member1, member2, unit);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Double> geodist(final String key, final String member1, final String member2) {
    getClient(key).geodist(key, member1, member2);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Double> geodist(final String key, final String member1, final String member2, final GeoUnit unit) {
    getClient(key).geodist(key, member1, member2);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<List<byte[]>> geohash(final byte[] key, final byte[]... members) {
    getClient(key).geohash(key, members);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  @Override
  public Response<List<String>> geohash(final String key, final String... members) {
    getClient(key).geohash(key, members);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<List<GeoCoordinate>> geopos(final byte[] key, final byte[]... members) {
    getClient(key).geopos(key, members);
    return getResponse(BuilderFactory.GEO_COORDINATE_LIST);
  }

  @Override
  public Response<List<GeoCoordinate>> geopos(final String key, final String... members) {
    getClient(key).geopos(key, members);
    return getResponse(BuilderFactory.GEO_COORDINATE_LIST);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadius(final byte[] key, final double longitude, final double latitude,
      final double radius, final GeoUnit unit) {
    getClient(key).georadius(key, longitude, latitude, radius, unit);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusReadonly(final byte[] key, final double longitude, final double latitude,
      final double radius, final GeoUnit unit) {
    getClient(key).georadiusReadonly(key, longitude, latitude, radius, unit);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadius(final byte[] key, final double longitude, final double latitude,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    getClient(key).georadius(key, longitude, latitude, radius, unit, param);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusReadonly(final byte[] key, final double longitude, final double latitude,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    getClient(key).georadiusReadonly(key, longitude, latitude, radius, unit, param);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadius(final String key, final double longitude, final double latitude,
      final double radius, final GeoUnit unit) {
    getClient(key).georadius(key, longitude, latitude, radius, unit);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusReadonly(final String key, final double longitude, final double latitude,
      final double radius, final GeoUnit unit) {
    getClient(key).georadiusReadonly(key, longitude, latitude, radius, unit);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadius(final String key, final double longitude, final double latitude,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    getClient(key).georadius(key, longitude, latitude, radius, unit, param);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusReadonly(final String key, final double longitude, final double latitude,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    getClient(key).georadiusReadonly(key, longitude, latitude, radius, unit, param);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusByMember(final byte[] key, final byte[] member,
      final double radius, final GeoUnit unit) {
    getClient(key).georadiusByMember(key, member, radius, unit);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusByMemberReadonly(final byte[] key, final byte[] member,
      final double radius, final GeoUnit unit) {
    getClient(key).georadiusByMemberReadonly(key, member, radius, unit);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusByMember(final byte[] key, final byte[] member,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    getClient(key).georadiusByMember(key, member, radius, unit, param);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusByMemberReadonly(final byte[] key, final byte[] member,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    getClient(key).georadiusByMemberReadonly(key, member, radius, unit, param);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusByMember(final String key, final String member,
      final double radius, final GeoUnit unit) {
    getClient(key).georadiusByMember(key, member, radius, unit);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusByMemberReadonly(final String key, final String member,
      final double radius, final GeoUnit unit) {
    getClient(key).georadiusByMemberReadonly(key, member, radius, unit);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusByMember(final String key, final String member,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    getClient(key).georadiusByMember(key, member, radius, unit, param);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusByMemberReadonly(final String key, final String member,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    getClient(key).georadiusByMemberReadonly(key, member, radius, unit, param);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<Long>> bitfield(final String key, final String... elements) {
    getClient(key).bitfield(key, elements);
    return getResponse(BuilderFactory.LONG_LIST);
  }

  @Override
  public Response<List<Long>> bitfield(final byte[] key, final byte[]... elements) {
    getClient(key).bitfield(key, elements);
    return getResponse(BuilderFactory.LONG_LIST);
  }

  @Override
  public Response<List<Long>> bitfieldReadonly(byte[] key, final byte[]... arguments) {
    getClient(key).bitfieldReadonly(key, arguments);
    return getResponse(BuilderFactory.LONG_LIST);
  }

  @Override
  public Response<List<Long>> bitfieldReadonly(String key, final String... arguments) {
    getClient(key).bitfieldReadonly(key, arguments);
    return getResponse(BuilderFactory.LONG_LIST);
  }

  @Override
  public Response<Long> hstrlen(final String key, final String field) {
    getClient(key).hstrlen(key, field);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> hstrlen(final byte[] key, final byte[] field) {
    getClient(key).hstrlen(key, field);
    return getResponse(BuilderFactory.LONG);
  }
  
  @Override
  public Response<StreamEntryID> xadd(String key, StreamEntryID id, Map<String, String> hash){
    return xadd(key, id, hash, Long.MAX_VALUE, true);    
  }
  
  @Override
  public Response<byte[]> xadd(byte[] key, byte[] id, Map<byte[], byte[]> hash){
    return xadd(key, id, hash, Long.MAX_VALUE, true);
  }


  @Override
  public Response<StreamEntryID> xadd(String key, StreamEntryID id, Map<String, String> hash, long maxLen, boolean approximateLength){
    getClient(key).xadd(key, id, hash, maxLen, approximateLength);
    return getResponse(BuilderFactory.STREAM_ENTRY_ID);    
  }
  

  @Override
  public Response<byte[]> xadd(byte[] key, byte[] id, Map<byte[], byte[]> hash, long maxLen, boolean approximateLength){
    getClient(key).xadd(key, id, hash, maxLen, approximateLength);
    return getResponse(BuilderFactory.BYTE_ARRAY);        
  }

  
  @Override
  public Response<Long> xlen(String key){
    getClient(key).xlen(key);
    return getResponse(BuilderFactory.LONG);
  }
  
  @Override
  public Response<Long> xlen(byte[] key){
    getClient(key).xlen(key);
    return getResponse(BuilderFactory.LONG);    
  }

  @Override
  public Response<List<StreamEntry>> xrange(String key, StreamEntryID start, StreamEntryID end, int count){
    getClient(key).xrange(key, start, end, count);
    return getResponse(BuilderFactory.STREAM_ENTRY_LIST);        
  }

  @Override
  public Response<List<byte[]>> xrange(byte[] key, byte[] start, byte[] end, int count){
    getClient(key).xrange(key, start, end, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);            
  }

  @Override
  public Response<List<StreamEntry>> xrevrange(String key, StreamEntryID end, StreamEntryID start, int count){
    getClient(key).xrevrange(key, start, end, count);
    return getResponse(BuilderFactory.STREAM_ENTRY_LIST);            
  }

  @Override
  public Response<List<byte[]>> xrevrange(byte[] key, byte[] end, byte[] start, int count){
    getClient(key).xrevrange(key, start, end, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);                
  }

   
  @Override
  public Response<Long> xack(String key, String group,  StreamEntryID... ids){
    getClient(key).xack(key, group, ids);
    return getResponse(BuilderFactory.LONG);                
  }
  
  @Override
  public Response<Long> xack(byte[] key, byte[] group,  byte[]... ids){
    getClient(key).xack(key, group, ids);
    return getResponse(BuilderFactory.LONG);                    
  }
  
  @Override
  public Response<String> xgroupCreate( String key, String groupname, StreamEntryID id, boolean makeStream){
    getClient(key).xgroupCreate(key, groupname, id, makeStream);
    return getResponse(BuilderFactory.STRING);
  }
  
  @Override
  public Response<String> xgroupCreate(byte[] key, byte[] groupname, byte[] id, boolean makeStream){
    getClient(key).xgroupCreate(key, groupname, id, makeStream);
    return getResponse(BuilderFactory.STRING);    
  }
  
  @Override
  public Response<String> xgroupSetID( String key, String groupname, StreamEntryID id){
    getClient(key).xgroupSetID(key, groupname, id);
    return getResponse(BuilderFactory.STRING);
  }
  
  @Override
  public Response<String> xgroupSetID(byte[] key, byte[] groupname, byte[] id){
    getClient(key).xgroupSetID(key, groupname, id);
    return getResponse(BuilderFactory.STRING);    
  }
  
  @Override
  public Response<Long> xgroupDestroy( String key, String groupname){
    getClient(key).xgroupDestroy(key, groupname);
    return getResponse(BuilderFactory.LONG);
  }
  
  @Override
  public Response<Long> xgroupDestroy(byte[] key, byte[] groupname){
    getClient(key).xgroupDestroy(key, groupname);
    return getResponse(BuilderFactory.LONG);
  }
  
  @Override
  public Response<Long> xgroupDelConsumer( String key, String groupname, String consumername){
    getClient(key).xgroupDelConsumer(key, groupname, consumername);
    return getResponse(BuilderFactory.LONG);
  }
  
  @Override
  public Response<Long> xgroupDelConsumer(byte[] key, byte[] groupname, byte[] consumername){
    getClient(key).xgroupDelConsumer(key, groupname, consumername);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<List<StreamPendingEntry>> xpending(String key, String groupname, StreamEntryID start, StreamEntryID end, int count, String consumername){
    getClient(key).xpending(key, groupname, start, end, count, consumername);
    return getResponse(BuilderFactory.STREAM_PENDING_ENTRY_LIST);        
  }
  
  @Override
  public Response<List<StreamPendingEntry>> xpending(byte[] key, byte[] groupname, byte[] start, byte[] end, int count, byte[] consumername){
    getClient(key).xpending(key, groupname, start, end, count, consumername);
    return getResponse(BuilderFactory.STREAM_PENDING_ENTRY_LIST);            
  }

  
  @Override
  public Response<Long> xdel( String key, StreamEntryID... ids){
    getClient(key).xdel(key, ids);
    return getResponse(BuilderFactory.LONG);        
  }

  @Override
  public Response<Long> xdel(byte[] key, byte[]... ids){
    getClient(key).xdel(key, ids);
    return getResponse(BuilderFactory.LONG);            
  }
  
  @Override
  public Response<Long> xtrim( String key, long maxLen, boolean approximateLength){
    getClient(key).xtrim(key, maxLen, approximateLength);
    return getResponse(BuilderFactory.LONG);        
  }
  
  @Override
  public Response<Long> xtrim(byte[] key, long maxLen, boolean approximateLength){
    getClient(key).xtrim(key, maxLen, approximateLength);
    return getResponse(BuilderFactory.LONG);            
  }
 
  @Override
  public Response<List<StreamEntry>> xclaim( String key, String group, String consumername, long minIdleTime, 
      long newIdleTime, int retries, boolean force, StreamEntryID... ids){
    getClient(key).xclaim(key, group, consumername, minIdleTime, newIdleTime, retries, force, ids);
    return getResponse(BuilderFactory.STREAM_ENTRY_LIST);        
  }
 
  @Override
  public Response<List<byte[]>> xclaim(byte[] key, byte[] group, byte[] consumername, long minIdleTime, 
      long newIdleTime, int retries, boolean force, byte[]... ids){
    getClient(key).xclaim(key, group, consumername, minIdleTime, newIdleTime, retries, force, ids);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);            
  }

  public Response<Object> sendCommand(final String sampleKey, final ProtocolCommand cmd, final String... args) {
    getClient(sampleKey).sendCommand(cmd, args);
    return getResponse(BuilderFactory.OBJECT);
  }

  public Response<Object> sendCommand(final byte[] sampleKey, final ProtocolCommand cmd, final byte[]... args) {
    getClient(sampleKey).sendCommand(cmd, args);
    return getResponse(BuilderFactory.OBJECT);
  }
}
