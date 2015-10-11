package redis.clients.jedis;

import static redis.clients.jedis.Protocol.toByteArray;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.BinaryClient.LIST_POSITION;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.commands.PipelineBinaryJedisCommands;
import redis.clients.jedis.commands.PipelineJedisCommands;
import redis.clients.jedis.params.set.SetParams;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;

public abstract class PipelineBase extends Queable implements PipelineBinaryJedisCommands,
    PipelineJedisCommands {

  protected abstract Client getClient();

  @Override
  public Response<Long> append(String key, String value) {
    getClient().append(key, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> append(byte[] key, byte[] value) {
    getClient().append(key, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<List<String>> blpop(String key) {
    String[] temp = new String[1];
    temp[0] = key;
    getClient().blpop(temp);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<List<String>> brpop(String key) {
    String[] temp = new String[1];
    temp[0] = key;
    getClient().brpop(temp);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<List<byte[]>> blpop(byte[] key) {
    byte[][] temp = new byte[1][];
    temp[0] = key;
    getClient().blpop(temp);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  @Override
  public Response<List<byte[]>> brpop(byte[] key) {
    byte[][] temp = new byte[1][];
    temp[0] = key;
    getClient().brpop(temp);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  @Override
  public Response<Long> decr(String key) {
    getClient().decr(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> decr(byte[] key) {
    getClient().decr(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> decrBy(String key, long integer) {
    getClient().decrBy(key, integer);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> decrBy(byte[] key, long integer) {
    getClient().decrBy(key, integer);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> del(String key) {
    getClient().del(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> del(byte[] key) {
    getClient().del(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> echo(String string) {
    getClient().echo(string);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<byte[]> echo(byte[] string) {
    getClient().echo(string);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<Boolean> exists(String key) {
    getClient().exists(key);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  @Override
  public Response<Boolean> exists(byte[] key) {
    getClient().exists(key);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  @Override
  public Response<Long> expire(String key, int seconds) {
    getClient().expire(key, seconds);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> expire(byte[] key, int seconds) {
    getClient().expire(key, seconds);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> expireAt(String key, long unixTime) {
    getClient().expireAt(key, unixTime);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> expireAt(byte[] key, long unixTime) {
    getClient().expireAt(key, unixTime);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> get(String key) {
    getClient().get(key);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<byte[]> get(byte[] key) {
    getClient().get(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<Boolean> getbit(String key, long offset) {
    getClient().getbit(key, offset);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  @Override
  public Response<Boolean> getbit(byte[] key, long offset) {
    getClient().getbit(key, offset);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  public Response<Long> bitpos(final String key, final boolean value) {
    return bitpos(key, value, new BitPosParams());
  }

  public Response<Long> bitpos(final String key, final boolean value, final BitPosParams params) {
    getClient().bitpos(key, value, params);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> bitpos(final byte[] key, final boolean value) {
    return bitpos(key, value, new BitPosParams());
  }

  public Response<Long> bitpos(final byte[] key, final boolean value, final BitPosParams params) {
    getClient().bitpos(key, value, params);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> getrange(String key, long startOffset, long endOffset) {
    getClient().getrange(key, startOffset, endOffset);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> getSet(String key, String value) {
    getClient().getSet(key, value);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<byte[]> getSet(byte[] key, byte[] value) {
    getClient().getSet(key, value);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<byte[]> getrange(byte[] key, long startOffset, long endOffset) {
    getClient().getrange(key, startOffset, endOffset);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<Long> hdel(String key, String... field) {
    getClient().hdel(key, field);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> hdel(byte[] key, byte[]... field) {
    getClient().hdel(key, field);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Boolean> hexists(String key, String field) {
    getClient().hexists(key, field);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  @Override
  public Response<Boolean> hexists(byte[] key, byte[] field) {
    getClient().hexists(key, field);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  @Override
  public Response<String> hget(String key, String field) {
    getClient().hget(key, field);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<byte[]> hget(byte[] key, byte[] field) {
    getClient().hget(key, field);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<Map<String, String>> hgetAll(String key) {
    getClient().hgetAll(key);
    return getResponse(BuilderFactory.STRING_MAP);
  }

  @Override
  public Response<Map<byte[], byte[]>> hgetAll(byte[] key) {
    getClient().hgetAll(key);
    return getResponse(BuilderFactory.BYTE_ARRAY_MAP);
  }

  @Override
  public Response<Long> hincrBy(String key, String field, long value) {
    getClient().hincrBy(key, field, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> hincrBy(byte[] key, byte[] field, long value) {
    getClient().hincrBy(key, field, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Set<String>> hkeys(String key) {
    getClient().hkeys(key);
    return getResponse(BuilderFactory.STRING_SET);
  }

  @Override
  public Response<Set<byte[]>> hkeys(byte[] key) {
    getClient().hkeys(key);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Long> hlen(String key) {
    getClient().hlen(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> hlen(byte[] key) {
    getClient().hlen(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<List<String>> hmget(String key, String... fields) {
    getClient().hmget(key, fields);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<List<byte[]>> hmget(byte[] key, byte[]... fields) {
    getClient().hmget(key, fields);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  @Override
  public Response<String> hmset(String key, Map<String, String> hash) {
    getClient().hmset(key, hash);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> hmset(byte[] key, Map<byte[], byte[]> hash) {
    getClient().hmset(key, hash);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Long> hset(String key, String field, String value) {
    getClient().hset(key, field, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> hset(byte[] key, byte[] field, byte[] value) {
    getClient().hset(key, field, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> hsetnx(String key, String field, String value) {
    getClient().hsetnx(key, field, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> hsetnx(byte[] key, byte[] field, byte[] value) {
    getClient().hsetnx(key, field, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<List<String>> hvals(String key) {
    getClient().hvals(key);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<List<byte[]>> hvals(byte[] key) {
    getClient().hvals(key);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  @Override
  public Response<Long> incr(String key) {
    getClient().incr(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> incr(byte[] key) {
    getClient().incr(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> incrBy(String key, long integer) {
    getClient().incrBy(key, integer);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> incrBy(byte[] key, long integer) {
    getClient().incrBy(key, integer);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> lindex(String key, long index) {
    getClient().lindex(key, index);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<byte[]> lindex(byte[] key, long index) {
    getClient().lindex(key, index);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<Long> linsert(String key, LIST_POSITION where, String pivot, String value) {
    getClient().linsert(key, where, pivot, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> linsert(byte[] key, LIST_POSITION where, byte[] pivot, byte[] value) {
    getClient().linsert(key, where, pivot, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> llen(String key) {
    getClient().llen(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> llen(byte[] key) {
    getClient().llen(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> lpop(String key) {
    getClient().lpop(key);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<byte[]> lpop(byte[] key) {
    getClient().lpop(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<Long> lpush(String key, String... string) {
    getClient().lpush(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> lpush(byte[] key, byte[]... string) {
    getClient().lpush(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> lpushx(String key, String... string) {
    getClient().lpushx(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> lpushx(byte[] key, byte[]... bytes) {
    getClient().lpushx(key, bytes);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<List<String>> lrange(String key, long start, long end) {
    getClient().lrange(key, start, end);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<List<byte[]>> lrange(byte[] key, long start, long end) {
    getClient().lrange(key, start, end);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  @Override
  public Response<Long> lrem(String key, long count, String value) {
    getClient().lrem(key, count, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> lrem(byte[] key, long count, byte[] value) {
    getClient().lrem(key, count, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> lset(String key, long index, String value) {
    getClient().lset(key, index, value);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> lset(byte[] key, long index, byte[] value) {
    getClient().lset(key, index, value);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> ltrim(String key, long start, long end) {
    getClient().ltrim(key, start, end);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> ltrim(byte[] key, long start, long end) {
    getClient().ltrim(key, start, end);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Long> move(String key, int dbIndex) {
    getClient().move(key, dbIndex);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> move(byte[] key, int dbIndex) {
    getClient().move(key, dbIndex);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> persist(String key) {
    getClient().persist(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> persist(byte[] key) {
    getClient().persist(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> rpop(String key) {
    getClient().rpop(key);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<byte[]> rpop(byte[] key) {
    getClient().rpop(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<Long> rpush(String key, String... string) {
    getClient().rpush(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> rpush(byte[] key, byte[]... string) {
    getClient().rpush(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> rpushx(String key, String... string) {
    getClient().rpushx(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> rpushx(byte[] key, byte[]... string) {
    getClient().rpushx(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> sadd(String key, String... member) {
    getClient().sadd(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> sadd(byte[] key, byte[]... member) {
    getClient().sadd(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> scard(String key) {
    getClient().scard(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> scard(byte[] key) {
    getClient().scard(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> set(String key, String value) {
    getClient().set(key, value);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> set(byte[] key, byte[] value) {
    getClient().set(key, value);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> set(String key, String value, SetParams params) {
    getClient().set(key, value, params);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> set(byte[] key, byte[] value, SetParams params) {
    getClient().set(key, value, params);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Boolean> setbit(String key, long offset, boolean value) {
    getClient().setbit(key, offset, value);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  @Override
  public Response<Boolean> setbit(byte[] key, long offset, byte[] value) {
    getClient().setbit(key, offset, value);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  @Override
  public Response<String> setex(String key, int seconds, String value) {
    getClient().setex(key, seconds, value);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> setex(byte[] key, int seconds, byte[] value) {
    getClient().setex(key, seconds, value);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Long> setnx(String key, String value) {
    getClient().setnx(key, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> setnx(byte[] key, byte[] value) {
    getClient().setnx(key, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> setrange(String key, long offset, String value) {
    getClient().setrange(key, offset, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> setrange(byte[] key, long offset, byte[] value) {
    getClient().setrange(key, offset, value);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Boolean> sismember(String key, String member) {
    getClient().sismember(key, member);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  @Override
  public Response<Boolean> sismember(byte[] key, byte[] member) {
    getClient().sismember(key, member);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  @Override
  public Response<Set<String>> smembers(String key) {
    getClient().smembers(key);
    return getResponse(BuilderFactory.STRING_SET);
  }

  @Override
  public Response<Set<byte[]>> smembers(byte[] key) {
    getClient().smembers(key);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<List<String>> sort(String key) {
    getClient().sort(key);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<List<byte[]>> sort(byte[] key) {
    getClient().sort(key);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  @Override
  public Response<List<String>> sort(String key, SortingParams sortingParameters) {
    getClient().sort(key, sortingParameters);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<List<byte[]>> sort(byte[] key, SortingParams sortingParameters) {
    getClient().sort(key, sortingParameters);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  @Override
  public Response<String> spop(String key) {
    getClient().spop(key);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Set<String>> spop(String key, long count) {
    getClient().spop(key, count);
    return getResponse(BuilderFactory.STRING_SET);
  }

  @Override
  public Response<byte[]> spop(byte[] key) {
    getClient().spop(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  @Override
  public Response<Set<byte[]>> spop(byte[] key, long count) {
    getClient().spop(key, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<String> srandmember(String key) {
    getClient().srandmember(key);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<List<String>> srandmember(String key, int count) {
    getClient().srandmember(key, count);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<byte[]> srandmember(byte[] key) {
    getClient().srandmember(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  public Response<List<byte[]>> srandmember(byte[] key, int count) {
    getClient().srandmember(key, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  @Override
  public Response<Long> srem(String key, String... member) {
    getClient().srem(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> srem(byte[] key, byte[]... member) {
    getClient().srem(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> strlen(String key) {
    getClient().strlen(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> strlen(byte[] key) {
    getClient().strlen(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> substr(String key, int start, int end) {
    getClient().substr(key, start, end);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> substr(byte[] key, int start, int end) {
    getClient().substr(key, start, end);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Long> ttl(String key) {
    getClient().ttl(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> ttl(byte[] key) {
    getClient().ttl(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> type(String key) {
    getClient().type(key);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> type(byte[] key) {
    getClient().type(key);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Long> zadd(String key, double score, String member) {
    getClient().zadd(key, score, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zadd(String key, double score, String member, ZAddParams params) {
    getClient().zadd(key, score, member, params);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zadd(String key, Map<String, Double> scoreMembers) {
    getClient().zadd(key, scoreMembers);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
    getClient().zadd(key, scoreMembers, params);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zadd(byte[] key, double score, byte[] member) {
    getClient().zadd(key, score, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zadd(byte[] key, double score, byte[] member, ZAddParams params) {
    getClient().zadd(key, score, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zadd(byte[] key, Map<byte[], Double> scoreMembers) {
    getClient().zadd(key, scoreMembers);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params) {
    getClient().zadd(key, scoreMembers, params);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zcard(String key) {
    getClient().zcard(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zcard(byte[] key) {
    getClient().zcard(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zcount(String key, double min, double max) {
    getClient().zcount(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zcount(String key, String min, String max) {
    getClient().zcount(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zcount(byte[] key, double min, double max) {
    getClient().zcount(key, toByteArray(min), toByteArray(max));
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zcount(byte[] key, byte[] min, byte[] max) {
    getClient().zcount(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Double> zincrby(String key, double score, String member) {
    getClient().zincrby(key, score, member);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Double> zincrby(String key, double score, String member, ZIncrByParams params) {
    getClient().zincrby(key, score, member, params);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Double> zincrby(byte[] key, double score, byte[] member) {
    getClient().zincrby(key, score, member);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Double> zincrby(byte[] key, double score, byte[] member, ZIncrByParams params) {
    getClient().zincrby(key, score, member);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Set<String>> zrange(String key, long start, long end) {
    getClient().zrange(key, start, end);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrange(byte[] key, long start, long end) {
    getClient().zrange(key, start, end);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<String>> zrangeByScore(String key, double min, double max) {
    getClient().zrangeByScore(key, min, max);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrangeByScore(byte[] key, double min, double max) {
    return zrangeByScore(key, toByteArray(min), toByteArray(max));
  }

  @Override
  public Response<Set<String>> zrangeByScore(String key, String min, String max) {
    getClient().zrangeByScore(key, min, max);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrangeByScore(byte[] key, byte[] min, byte[] max) {
    getClient().zrangeByScore(key, min, max);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<String>> zrangeByScore(String key, double min, double max, int offset,
      int count) {
    getClient().zrangeByScore(key, min, max, offset, count);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  public Response<Set<String>> zrangeByScore(String key, String min, String max, int offset,
      int count) {
    getClient().zrangeByScore(key, min, max, offset, count);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrangeByScore(byte[] key, double min, double max, int offset,
      int count) {
    return zrangeByScore(key, toByteArray(min), toByteArray(max), offset, count);
  }

  @Override
  public Response<Set<byte[]>> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset,
      int count) {
    getClient().zrangeByScore(key, min, max, offset, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max) {
    getClient().zrangeByScoreWithScores(key, min, max);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  public Response<Set<Tuple>> zrangeByScoreWithScores(String key, String min, String max) {
    getClient().zrangeByScoreWithScores(key, min, max);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(byte[] key, double min, double max) {
    return zrangeByScoreWithScores(key, toByteArray(min), toByteArray(max));
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
    getClient().zrangeByScoreWithScores(key, min, max);
    return getResponse(BuilderFactory.TUPLE_ZSET_BINARY);
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max,
      int offset, int count) {
    getClient().zrangeByScoreWithScores(key, min, max, offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  public Response<Set<Tuple>> zrangeByScoreWithScores(String key, String min, String max,
      int offset, int count) {
    getClient().zrangeByScoreWithScores(key, min, max, offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(byte[] key, double min, double max,
      int offset, int count) {
    getClient().zrangeByScoreWithScores(key, toByteArray(min), toByteArray(max), offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET_BINARY);
  }

  @Override
  public Response<Set<Tuple>> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max,
      int offset, int count) {
    getClient().zrangeByScoreWithScores(key, min, max, offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET_BINARY);
  }

  @Override
  public Response<Set<String>> zrevrangeByScore(String key, double max, double min) {
    getClient().zrevrangeByScore(key, max, min);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrevrangeByScore(byte[] key, double max, double min) {
    getClient().zrevrangeByScore(key, toByteArray(max), toByteArray(min));
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<String>> zrevrangeByScore(String key, String max, String min) {
    getClient().zrevrangeByScore(key, max, min);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
    getClient().zrevrangeByScore(key, max, min);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<String>> zrevrangeByScore(String key, double max, double min, int offset,
      int count) {
    getClient().zrevrangeByScore(key, max, min, offset, count);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  public Response<Set<String>> zrevrangeByScore(String key, String max, String min, int offset,
      int count) {
    getClient().zrevrangeByScore(key, max, min, offset, count);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrevrangeByScore(byte[] key, double max, double min, int offset,
      int count) {
    getClient().zrevrangeByScore(key, toByteArray(max), toByteArray(min), offset, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset,
      int count) {
    getClient().zrevrangeByScore(key, max, min, offset, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min) {
    getClient().zrevrangeByScoreWithScores(key, max, min);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min) {
    getClient().zrevrangeByScoreWithScores(key, max, min);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
    getClient().zrevrangeByScoreWithScores(key, toByteArray(max), toByteArray(min));
    return getResponse(BuilderFactory.TUPLE_ZSET_BINARY);
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
    getClient().zrevrangeByScoreWithScores(key, max, min);
    return getResponse(BuilderFactory.TUPLE_ZSET_BINARY);
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min,
      int offset, int count) {
    getClient().zrevrangeByScoreWithScores(key, max, min, offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min,
      int offset, int count) {
    getClient().zrevrangeByScoreWithScores(key, max, min, offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, double max, double min,
      int offset, int count) {
    getClient().zrevrangeByScoreWithScores(key, toByteArray(max), toByteArray(min), offset,
      count);
    return getResponse(BuilderFactory.TUPLE_ZSET_BINARY);
  }

  @Override
  public Response<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min,
      int offset, int count) {
    getClient().zrevrangeByScoreWithScores(key, max, min, offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET_BINARY);
  }

  @Override
  public Response<Set<Tuple>> zrangeWithScores(String key, long start, long end) {
    getClient().zrangeWithScores(key, start, end);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrangeWithScores(byte[] key, long start, long end) {
    getClient().zrangeWithScores(key, start, end);
    return getResponse(BuilderFactory.TUPLE_ZSET_BINARY);
  }

  @Override
  public Response<Long> zrank(String key, String member) {
    getClient().zrank(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zrank(byte[] key, byte[] member) {
    getClient().zrank(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zrem(String key, String... member) {
    getClient().zrem(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zrem(byte[] key, byte[]... member) {
    getClient().zrem(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zremrangeByRank(String key, long start, long end) {
    getClient().zremrangeByRank(key, start, end);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zremrangeByRank(byte[] key, long start, long end) {
    getClient().zremrangeByRank(key, start, end);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zremrangeByScore(String key, double start, double end) {
    getClient().zremrangeByScore(key, start, end);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zremrangeByScore(String key, String start, String end) {
    getClient().zremrangeByScore(key, start, end);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zremrangeByScore(byte[] key, double start, double end) {
    getClient().zremrangeByScore(key, toByteArray(start), toByteArray(end));
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zremrangeByScore(byte[] key, byte[] start, byte[] end) {
    getClient().zremrangeByScore(key, start, end);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Set<String>> zrevrange(String key, long start, long end) {
    getClient().zrevrange(key, start, end);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrevrange(byte[] key, long start, long end) {
    getClient().zrevrange(key, start, end);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrevrangeWithScores(String key, long start, long end) {
    getClient().zrevrangeWithScores(key, start, end);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Set<Tuple>> zrevrangeWithScores(byte[] key, long start, long end) {
    getClient().zrevrangeWithScores(key, start, end);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  @Override
  public Response<Long> zrevrank(String key, String member) {
    getClient().zrevrank(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zrevrank(byte[] key, byte[] member) {
    getClient().zrevrank(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Double> zscore(String key, String member) {
    getClient().zscore(key, member);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Double> zscore(byte[] key, byte[] member) {
    getClient().zscore(key, member);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Long> zlexcount(final byte[] key, final byte[] min, final byte[] max) {
    getClient().zlexcount(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zlexcount(final String key, final String min, final String max) {
    getClient().zlexcount(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Set<byte[]>> zrangeByLex(final byte[] key, final byte[] min, final byte[] max) {
    getClient().zrangeByLex(key, min, max);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<String>> zrangeByLex(final String key, final String min, final String max) {
    getClient().zrangeByLex(key, min, max);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrangeByLex(final byte[] key, final byte[] min, final byte[] max,
      final int offset, final int count) {
    getClient().zrangeByLex(key, min, max, offset, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<String>> zrangeByLex(final String key, final String min, final String max,
      final int offset, final int count) {
    getClient().zrangeByLex(key, min, max, offset, count);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrevrangeByLex(final byte[] key, final byte[] max, final byte[] min) {
    getClient().zrevrangeByLex(key, max, min);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<String>> zrevrangeByLex(final String key, final String max, final String min) {
    getClient().zrevrangeByLex(key, max, min);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Set<byte[]>> zrevrangeByLex(final byte[] key, final byte[] max, final byte[] min,
      final int offset, final int count) {
    getClient().zrevrangeByLex(key, max, min, offset, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  @Override
  public Response<Set<String>> zrevrangeByLex(final String key, final String max, final String min,
      final int offset, final int count) {
    getClient().zrevrangeByLex(key, max, min, offset, count);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  @Override
  public Response<Long> zremrangeByLex(final byte[] key, final byte[] min, final byte[] max) {
    getClient().zremrangeByLex(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> zremrangeByLex(final String key, final String min, final String max) {
    getClient().zremrangeByLex(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> bitcount(String key) {
    getClient().bitcount(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> bitcount(String key, long start, long end) {
    getClient().bitcount(key, start, end);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> bitcount(byte[] key) {
    getClient().bitcount(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> bitcount(byte[] key, long start, long end) {
    getClient().bitcount(key, start, end);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<byte[]> dump(String key) {
    getClient().dump(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  public Response<byte[]> dump(byte[] key) {
    getClient().dump(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  public Response<String> migrate(String host, int port, String key, int destinationDb, int timeout) {
    getClient().migrate(host, port, key, destinationDb, timeout);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> migrate(byte[] host, int port, byte[] key, int destinationDb, int timeout) {
    getClient().migrate(host, port, key, destinationDb, timeout);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<Long> objectRefcount(String key) {
    getClient().objectRefcount(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> objectRefcount(byte[] key) {
    getClient().objectRefcount(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> objectEncoding(String key) {
    getClient().objectEncoding(key);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<byte[]> objectEncoding(byte[] key) {
    getClient().objectEncoding(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  public Response<Long> objectIdletime(String key) {
    getClient().objectIdletime(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> objectIdletime(byte[] key) {
    getClient().objectIdletime(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pexpire(String key, long milliseconds) {
    getClient().pexpire(key, milliseconds);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pexpire(byte[] key, long milliseconds) {
    getClient().pexpire(key, milliseconds);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pexpireAt(String key, long millisecondsTimestamp) {
    getClient().pexpireAt(key, millisecondsTimestamp);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pexpireAt(byte[] key, long millisecondsTimestamp) {
    getClient().pexpireAt(key, millisecondsTimestamp);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> pttl(String key) {
    getClient().pttl(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> pttl(byte[] key) {
    getClient().pttl(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> restore(String key, int ttl, byte[] serializedValue) {
    getClient().restore(key, ttl, serializedValue);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> restore(byte[] key, int ttl, byte[] serializedValue) {
    getClient().restore(key, ttl, serializedValue);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<Double> incrByFloat(String key, double increment) {
    getClient().incrByFloat(key, increment);
    return getResponse(BuilderFactory.DOUBLE);
  }

  public Response<Double> incrByFloat(byte[] key, double increment) {
    getClient().incrByFloat(key, increment);
    return getResponse(BuilderFactory.DOUBLE);
  }

  public Response<String> psetex(String key, long milliseconds, String value) {
    getClient().psetex(key, milliseconds, value);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> psetex(byte[] key, long milliseconds, byte[] value) {
    getClient().psetex(key, milliseconds, value);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<Double> hincrByFloat(String key, String field, double increment) {
    getClient().hincrByFloat(key, field, increment);
    return getResponse(BuilderFactory.DOUBLE);
  }

  public Response<Double> hincrByFloat(byte[] key, byte[] field, double increment) {
    getClient().hincrByFloat(key, field, increment);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Long> pfadd(byte[] key, byte[]... elements) {
    getClient().pfadd(key, elements);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pfcount(byte[] key) {
    getClient().pfcount(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pfadd(String key, String... elements) {
    getClient().pfadd(key, elements);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pfcount(String key) {
    getClient().pfcount(key);
    return getResponse(BuilderFactory.LONG);
  }

  // multi key operations

  public Response<List<String>> brpop(String... args) {
    getClient().brpop(args);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<List<String>> brpop(int timeout, String... keys) {
    getClient().brpop(timeout, keys);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<List<String>> blpop(String... args) {
    getClient().blpop(args);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<List<String>> blpop(int timeout, String... keys) {
    getClient().blpop(timeout, keys);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<Map<String, String>> blpopMap(int timeout, String... keys) {
    getClient().blpop(timeout, keys);
    return getResponse(BuilderFactory.STRING_MAP);
  }

  public Response<List<byte[]>> brpop(byte[]... args) {
    getClient().brpop(args);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  public Response<List<String>> brpop(int timeout, byte[]... keys) {
    getClient().brpop(timeout, keys);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<Map<String, String>> brpopMap(int timeout, String... keys) {
    getClient().blpop(timeout, keys);
    return getResponse(BuilderFactory.STRING_MAP);
  }

  public Response<List<byte[]>> blpop(byte[]... args) {
    getClient().blpop(args);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  public Response<List<String>> blpop(int timeout, byte[]... keys) {
    getClient().blpop(timeout, keys);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<Long> del(String... keys) {
    getClient().del(keys);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> del(byte[]... keys) {
    getClient().del(keys);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> exists(String... keys) {
    getClient().exists(keys);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> exists(byte[]... keys) {
    getClient().exists(keys);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Set<String>> keys(String pattern) {
    getClient().keys(pattern);
    return getResponse(BuilderFactory.STRING_SET);
  }

  public Response<Set<byte[]>> keys(byte[] pattern) {
    getClient().keys(pattern);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  public Response<List<String>> mget(String... keys) {
    getClient().mget(keys);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<List<byte[]>> mget(byte[]... keys) {
    getClient().mget(keys);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  public Response<String> mset(String... keysvalues) {
    getClient().mset(keysvalues);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> mset(byte[]... keysvalues) {
    getClient().mset(keysvalues);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<Long> msetnx(String... keysvalues) {
    getClient().msetnx(keysvalues);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> msetnx(byte[]... keysvalues) {
    getClient().msetnx(keysvalues);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> rename(String oldkey, String newkey) {
    getClient().rename(oldkey, newkey);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> rename(byte[] oldkey, byte[] newkey) {
    getClient().rename(oldkey, newkey);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<Long> renamenx(String oldkey, String newkey) {
    getClient().renamenx(oldkey, newkey);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> renamenx(byte[] oldkey, byte[] newkey) {
    getClient().renamenx(oldkey, newkey);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> rpoplpush(String srckey, String dstkey) {
    getClient().rpoplpush(srckey, dstkey);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<byte[]> rpoplpush(byte[] srckey, byte[] dstkey) {
    getClient().rpoplpush(srckey, dstkey);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  public Response<Set<String>> sdiff(String... keys) {
    getClient().sdiff(keys);
    return getResponse(BuilderFactory.STRING_SET);
  }

  public Response<Set<byte[]>> sdiff(byte[]... keys) {
    getClient().sdiff(keys);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  public Response<Long> sdiffstore(String dstkey, String... keys) {
    getClient().sdiffstore(dstkey, keys);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> sdiffstore(byte[] dstkey, byte[]... keys) {
    getClient().sdiffstore(dstkey, keys);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Set<String>> sinter(String... keys) {
    getClient().sinter(keys);
    return getResponse(BuilderFactory.STRING_SET);
  }

  public Response<Set<byte[]>> sinter(byte[]... keys) {
    getClient().sinter(keys);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  public Response<Long> sinterstore(String dstkey, String... keys) {
    getClient().sinterstore(dstkey, keys);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> sinterstore(byte[] dstkey, byte[]... keys) {
    getClient().sinterstore(dstkey, keys);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> smove(String srckey, String dstkey, String member) {
    getClient().smove(srckey, dstkey, member);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> smove(byte[] srckey, byte[] dstkey, byte[] member) {
    getClient().smove(srckey, dstkey, member);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> sort(String key, SortingParams sortingParameters, String dstkey) {
    getClient().sort(key, sortingParameters, dstkey);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> sort(byte[] key, SortingParams sortingParameters, byte[] dstkey) {
    getClient().sort(key, sortingParameters, dstkey);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> sort(String key, String dstkey) {
    getClient().sort(key, dstkey);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> sort(byte[] key, byte[] dstkey) {
    getClient().sort(key, dstkey);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Set<String>> sunion(String... keys) {
    getClient().sunion(keys);
    return getResponse(BuilderFactory.STRING_SET);
  }

  public Response<Set<byte[]>> sunion(byte[]... keys) {
    getClient().sunion(keys);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  public Response<Long> sunionstore(String dstkey, String... keys) {
    getClient().sunionstore(dstkey, keys);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> sunionstore(byte[] dstkey, byte[]... keys) {
    getClient().sunionstore(dstkey, keys);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> watch(String... keys) {
    getClient().watch(keys);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> watch(byte[]... keys) {
    getClient().watch(keys);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<Long> zinterstore(String dstkey, String... sets) {
    getClient().zinterstore(dstkey, sets);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zinterstore(byte[] dstkey, byte[]... sets) {
    getClient().zinterstore(dstkey, sets);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zinterstore(String dstkey, ZParams params, String... sets) {
    getClient().zinterstore(dstkey, params, sets);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zinterstore(byte[] dstkey, ZParams params, byte[]... sets) {
    getClient().zinterstore(dstkey, params, sets);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zunionstore(String dstkey, String... sets) {
    getClient().zunionstore(dstkey, sets);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zunionstore(byte[] dstkey, byte[]... sets) {
    getClient().zunionstore(dstkey, sets);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zunionstore(String dstkey, ZParams params, String... sets) {
    getClient().zunionstore(dstkey, params, sets);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zunionstore(byte[] dstkey, ZParams params, byte[]... sets) {
    getClient().zunionstore(dstkey, params, sets);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> bgrewriteaof() {
    getClient().bgrewriteaof();
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> bgsave() {
    getClient().bgsave();
    return getResponse(BuilderFactory.STRING);
  }

  public Response<List<String>> configGet(String pattern) {
    getClient().configGet(pattern);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<String> configSet(String parameter, String value) {
    getClient().configSet(parameter, value);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> brpoplpush(String source, String destination, int timeout) {
    getClient().brpoplpush(source, destination, timeout);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<byte[]> brpoplpush(byte[] source, byte[] destination, int timeout) {
    getClient().brpoplpush(source, destination, timeout);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  public Response<String> configResetStat() {
    getClient().configResetStat();
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> save() {
    getClient().save();
    return getResponse(BuilderFactory.STRING);
  }

  public Response<Long> lastsave() {
    getClient().lastsave();
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> publish(String channel, String message) {
    getClient().publish(channel, message);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> publish(byte[] channel, byte[] message) {
    getClient().publish(channel, message);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> randomKey() {
    getClient().randomKey();
    return getResponse(BuilderFactory.STRING);
  }

  public Response<byte[]> randomKeyBinary() {
    getClient().randomKey();
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  public Response<String> flushDB() {
    getClient().flushDB();
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> flushAll() {
    getClient().flushAll();
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> info() {
    getClient().info();
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> info(final String section) {
    getClient().info(section);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<Long> dbSize() {
    getClient().dbSize();
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> shutdown() {
    getClient().shutdown();
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> ping() {
    getClient().ping();
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> select(int index) {
    getClient().select(index);
    Response<String> response = getResponse(BuilderFactory.STRING);
    getClient().setDb(index);

    return response;
  }

  public Response<Long> bitop(BitOP op, byte[] destKey, byte[]... srcKeys) {
    getClient().bitop(op, destKey, srcKeys);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> geoadd(byte[] key, double longitude, double latitude, byte[] member) {
    getClient().geoadd(key, longitude, latitude, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap) {
    getClient().geoadd(key, memberCoordinateMap);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> geoadd(String key, double longitude, double latitude, String member) {
    getClient().geoadd(key, longitude, latitude, member);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
    getClient().geoadd(key, memberCoordinateMap);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Double> geodist(byte[] key, byte[] member1, byte[] member2) {
    getClient().geodist(key, member1, member2);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Double> geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit) {
    getClient().geodist(key, member1, member2, unit);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Double> geodist(String key, String member1, String member2) {
    getClient().geodist(key, member1, member2);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<Double> geodist(String key, String member1, String member2, GeoUnit unit) {
    getClient().geodist(key, member1, member2);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Override
  public Response<List<byte[]>> geohash(byte[] key, byte[]... members) {
    getClient().geohash(key, members);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  @Override
  public Response<List<String>> geohash(String key, String... members) {
    getClient().geohash(key, members);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  @Override
  public Response<List<GeoCoordinate>> geopos(byte[] key, byte[]... members) {
    getClient().geopos(key, members);
    return getResponse(BuilderFactory.GEO_COORDINATE_LIST);
  }

  @Override
  public Response<List<GeoCoordinate>> geopos(String key, String... members) {
    getClient().geopos(key, members);
    return getResponse(BuilderFactory.GEO_COORDINATE_LIST);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadius(byte[] key, double longitude, double latitude,
      double radius, GeoUnit unit) {
    getClient().georadius(key, longitude, latitude, radius, unit);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadius(byte[] key, double longitude, double latitude,
      double radius, GeoUnit unit, GeoRadiusParam param) {
    getClient().georadius(key, longitude, latitude, radius, unit, param);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude,
      double radius, GeoUnit unit) {
    getClient().georadius(key, longitude, latitude, radius, unit);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude,
      double radius, GeoUnit unit, GeoRadiusParam param) {
    getClient().georadius(key, longitude, latitude, radius, unit, param);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusByMember(byte[] key, byte[] member,
      double radius, GeoUnit unit) {
    getClient().georadiusByMember(key, member, radius, unit);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusByMember(byte[] key, byte[] member,
      double radius, GeoUnit unit, GeoRadiusParam param) {
    getClient().georadiusByMember(key, member, radius, unit, param);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusByMember(String key, String member,
      double radius, GeoUnit unit) {
    getClient().georadiusByMember(key, member, radius, unit);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  @Override
  public Response<List<GeoRadiusResponse>> georadiusByMember(String key, String member,
      double radius, GeoUnit unit, GeoRadiusParam param) {
    getClient().georadiusByMember(key, member, radius, unit, param);
    return getResponse(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
  }

  public Response<Long> bitop(BitOP op, String destKey, String... srcKeys) {
    getClient().bitop(op, destKey, srcKeys);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> clusterNodes() {
    getClient().clusterNodes();
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> clusterMeet(final String ip, final int port) {
    getClient().clusterMeet(ip, port);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> clusterAddSlots(final int... slots) {
    getClient().clusterAddSlots(slots);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> clusterDelSlots(final int... slots) {
    getClient().clusterDelSlots(slots);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> clusterInfo() {
    getClient().clusterInfo();
    return getResponse(BuilderFactory.STRING);
  }

  public Response<List<String>> clusterGetKeysInSlot(final int slot, final int count) {
    getClient().clusterGetKeysInSlot(slot, count);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<String> clusterSetSlotNode(final int slot, final String nodeId) {
    getClient().clusterSetSlotNode(slot, nodeId);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> clusterSetSlotMigrating(final int slot, final String nodeId) {
    getClient().clusterSetSlotMigrating(slot, nodeId);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> clusterSetSlotImporting(final int slot, final String nodeId) {
    getClient().clusterSetSlotImporting(slot, nodeId);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<Object> eval(String script) {
    return this.eval(script, 0, new String[0]);
  }

  public Response<Object> eval(String script, List<String> keys, List<String> args) {
    String[] argv = Jedis.getParams(keys, args);
    return this.eval(script, keys.size(), argv);
  }

  public Response<Object> eval(String script, int keyCount, String... params) {
    getClient().eval(script, keyCount, params);
    return getResponse(BuilderFactory.EVAL_RESULT);
  }

  public Response<Object> evalsha(String script) {
    return this.evalsha(script, 0, new String[0]);
  }

  public Response<Object> evalsha(String sha1, List<String> keys, List<String> args) {
    String[] argv = Jedis.getParams(keys, args);
    return this.evalsha(sha1, keys.size(), argv);
  }

  public Response<Object> evalsha(String sha1, int keyCount, String... params) {
    getClient().evalsha(sha1, keyCount, params);
    return getResponse(BuilderFactory.EVAL_RESULT);
  }

  public Response<Object> eval(byte[] script) {
    return this.eval(script, 0);
  }

  public Response<Object> eval(byte[] script, byte[] keyCount, byte[]... params) {
    getClient().eval(script, keyCount, params);
    return getResponse(BuilderFactory.EVAL_BINARY_RESULT);
  }

  public Response<Object> eval(byte[] script, List<byte[]> keys, List<byte[]> args) {
    byte[][] argv = BinaryJedis.getParamsWithBinary(keys, args);
    return this.eval(script, keys.size(), argv);
  }

  public Response<Object> eval(byte[] script, int keyCount, byte[]... params) {
    getClient().eval(script, keyCount, params);
    return getResponse(BuilderFactory.EVAL_BINARY_RESULT);
  }

  public Response<Object> evalsha(byte[] sha1) {
    return this.evalsha(sha1, 0);
  }

  public Response<Object> evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args) {
    byte[][] argv = BinaryJedis.getParamsWithBinary(keys, args);
    return this.evalsha(sha1, keys.size(), argv);
  }

  public Response<Object> evalsha(byte[] sha1, int keyCount, byte[]... params) {
    getClient().evalsha(sha1, keyCount, params);
    return getResponse(BuilderFactory.EVAL_BINARY_RESULT);
  }

  @Override
  public Response<Long> pfcount(String... keys) {
    getClient().pfcount(keys);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pfcount(final byte[]... keys) {
    getClient().pfcount(keys);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<String> pfmerge(byte[] destkey, byte[]... sourcekeys) {
    getClient().pfmerge(destkey, sourcekeys);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<String> pfmerge(String destkey, String... sourcekeys) {
    getClient().pfmerge(destkey, sourcekeys);
    return getResponse(BuilderFactory.STRING);
  }

}
