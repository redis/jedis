package redis.clients.jedis;

import static redis.clients.jedis.Protocol.toByteArray;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.BinaryClient.LIST_POSITION;

public abstract class PipelineBase extends Queable implements BinaryRedisPipeline, RedisPipeline {

  protected abstract Client getClient(String key);

  protected abstract Client getClient(byte[] key);

  public Response<Long> append(String key, String value) {
    getClient(key).append(key, value);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> append(byte[] key, byte[] value) {
    getClient(key).append(key, value);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<List<String>> blpop(String key) {
    String[] temp = new String[1];
    temp[0] = key;
    getClient(key).blpop(temp);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<List<String>> brpop(String key) {
    String[] temp = new String[1];
    temp[0] = key;
    getClient(key).brpop(temp);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<List<byte[]>> blpop(byte[] key) {
    byte[][] temp = new byte[1][];
    temp[0] = key;
    getClient(key).blpop(temp);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  public Response<List<byte[]>> brpop(byte[] key) {
    byte[][] temp = new byte[1][];
    temp[0] = key;
    getClient(key).brpop(temp);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  public Response<Long> decr(String key) {
    getClient(key).decr(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> decr(byte[] key) {
    getClient(key).decr(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> decrBy(String key, long integer) {
    getClient(key).decrBy(key, integer);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> decrBy(byte[] key, long integer) {
    getClient(key).decrBy(key, integer);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> del(String key) {
    getClient(key).del(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> del(byte[] key) {
    getClient(key).del(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> echo(String string) {
    getClient(string).echo(string);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<byte[]> echo(byte[] string) {
    getClient(string).echo(string);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  public Response<Boolean> exists(String key) {
    getClient(key).exists(key);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  public Response<Boolean> exists(byte[] key) {
    getClient(key).exists(key);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  public Response<Long> expire(String key, int seconds) {
    getClient(key).expire(key, seconds);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> expire(byte[] key, int seconds) {
    getClient(key).expire(key, seconds);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> expireAt(String key, long unixTime) {
    getClient(key).expireAt(key, unixTime);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> expireAt(byte[] key, long unixTime) {
    getClient(key).expireAt(key, unixTime);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> get(String key) {
    getClient(key).get(key);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<byte[]> get(byte[] key) {
    getClient(key).get(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  public Response<Boolean> getbit(String key, long offset) {
    getClient(key).getbit(key, offset);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  public Response<Boolean> getbit(byte[] key, long offset) {
    getClient(key).getbit(key, offset);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  public Response<Long> bitpos(final String key, final boolean value) {
    return bitpos(key, value, new BitPosParams());
  }

  public Response<Long> bitpos(final String key, final boolean value, final BitPosParams params) {
    getClient(key).bitpos(key, value, params);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> bitpos(final byte[] key, final boolean value) {
    return bitpos(key, value, new BitPosParams());
  }

  public Response<Long> bitpos(final byte[] key, final boolean value, final BitPosParams params) {
    getClient(key).bitpos(key, value, params);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> getrange(String key, long startOffset, long endOffset) {
    getClient(key).getrange(key, startOffset, endOffset);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> getSet(String key, String value) {
    getClient(key).getSet(key, value);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<byte[]> getSet(byte[] key, byte[] value) {
    getClient(key).getSet(key, value);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  public Response<Long> getrange(byte[] key, long startOffset, long endOffset) {
    getClient(key).getrange(key, startOffset, endOffset);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> hdel(String key, String... field) {
    getClient(key).hdel(key, field);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> hdel(byte[] key, byte[]... field) {
    getClient(key).hdel(key, field);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Boolean> hexists(String key, String field) {
    getClient(key).hexists(key, field);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  public Response<Boolean> hexists(byte[] key, byte[] field) {
    getClient(key).hexists(key, field);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  public Response<String> hget(String key, String field) {
    getClient(key).hget(key, field);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<byte[]> hget(byte[] key, byte[] field) {
    getClient(key).hget(key, field);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  public Response<Map<String, String>> hgetAll(String key) {
    getClient(key).hgetAll(key);
    return getResponse(BuilderFactory.STRING_MAP);
  }

  public Response<Map<byte[], byte[]>> hgetAll(byte[] key) {
    getClient(key).hgetAll(key);
    return getResponse(BuilderFactory.BYTE_ARRAY_MAP);
  }

  public Response<Long> hincrBy(String key, String field, long value) {
    getClient(key).hincrBy(key, field, value);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> hincrBy(byte[] key, byte[] field, long value) {
    getClient(key).hincrBy(key, field, value);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Set<String>> hkeys(String key) {
    getClient(key).hkeys(key);
    return getResponse(BuilderFactory.STRING_SET);
  }

  public Response<Set<byte[]>> hkeys(byte[] key) {
    getClient(key).hkeys(key);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  public Response<Long> hlen(String key) {
    getClient(key).hlen(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> hlen(byte[] key) {
    getClient(key).hlen(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<List<String>> hmget(String key, String... fields) {
    getClient(key).hmget(key, fields);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<List<byte[]>> hmget(byte[] key, byte[]... fields) {
    getClient(key).hmget(key, fields);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  public Response<String> hmset(String key, Map<String, String> hash) {
    getClient(key).hmset(key, hash);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> hmset(byte[] key, Map<byte[], byte[]> hash) {
    getClient(key).hmset(key, hash);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<Long> hset(String key, String field, String value) {
    getClient(key).hset(key, field, value);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> hset(byte[] key, byte[] field, byte[] value) {
    getClient(key).hset(key, field, value);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> hsetnx(String key, String field, String value) {
    getClient(key).hsetnx(key, field, value);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> hsetnx(byte[] key, byte[] field, byte[] value) {
    getClient(key).hsetnx(key, field, value);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<List<String>> hvals(String key) {
    getClient(key).hvals(key);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<List<byte[]>> hvals(byte[] key) {
    getClient(key).hvals(key);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  public Response<Long> incr(String key) {
    getClient(key).incr(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> incr(byte[] key) {
    getClient(key).incr(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> incrBy(String key, long integer) {
    getClient(key).incrBy(key, integer);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> incrBy(byte[] key, long integer) {
    getClient(key).incrBy(key, integer);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> lindex(String key, long index) {
    getClient(key).lindex(key, index);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<byte[]> lindex(byte[] key, long index) {
    getClient(key).lindex(key, index);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  public Response<Long> linsert(String key, LIST_POSITION where, String pivot, String value) {
    getClient(key).linsert(key, where, pivot, value);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> linsert(byte[] key, LIST_POSITION where, byte[] pivot, byte[] value) {
    getClient(key).linsert(key, where, pivot, value);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> llen(String key) {
    getClient(key).llen(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> llen(byte[] key) {
    getClient(key).llen(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> lpop(String key) {
    getClient(key).lpop(key);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<byte[]> lpop(byte[] key) {
    getClient(key).lpop(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  public Response<Long> lpush(String key, String... string) {
    getClient(key).lpush(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> lpush(byte[] key, byte[]... string) {
    getClient(key).lpush(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> lpushx(String key, String... string) {
    getClient(key).lpushx(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> lpushx(byte[] key, byte[]... bytes) {
    getClient(key).lpushx(key, bytes);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<List<String>> lrange(String key, long start, long end) {
    getClient(key).lrange(key, start, end);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<List<byte[]>> lrange(byte[] key, long start, long end) {
    getClient(key).lrange(key, start, end);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  public Response<Long> lrem(String key, long count, String value) {
    getClient(key).lrem(key, count, value);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> lrem(byte[] key, long count, byte[] value) {
    getClient(key).lrem(key, count, value);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> lset(String key, long index, String value) {
    getClient(key).lset(key, index, value);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> lset(byte[] key, long index, byte[] value) {
    getClient(key).lset(key, index, value);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> ltrim(String key, long start, long end) {
    getClient(key).ltrim(key, start, end);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> ltrim(byte[] key, long start, long end) {
    getClient(key).ltrim(key, start, end);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<Long> move(String key, int dbIndex) {
    getClient(key).move(key, dbIndex);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> move(byte[] key, int dbIndex) {
    getClient(key).move(key, dbIndex);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> persist(String key) {
    getClient(key).persist(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> persist(byte[] key) {
    getClient(key).persist(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> rpop(String key) {
    getClient(key).rpop(key);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<byte[]> rpop(byte[] key) {
    getClient(key).rpop(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  public Response<Long> rpush(String key, String... string) {
    getClient(key).rpush(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> rpush(byte[] key, byte[]... string) {
    getClient(key).rpush(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> rpushx(String key, String... string) {
    getClient(key).rpushx(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> rpushx(byte[] key, byte[]... string) {
    getClient(key).rpushx(key, string);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> sadd(String key, String... member) {
    getClient(key).sadd(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> sadd(byte[] key, byte[]... member) {
    getClient(key).sadd(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> scard(String key) {
    getClient(key).scard(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> scard(byte[] key) {
    getClient(key).scard(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> set(String key, String value) {
    getClient(key).set(key, value);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> set(byte[] key, byte[] value) {
    getClient(key).set(key, value);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<Boolean> setbit(String key, long offset, boolean value) {
    getClient(key).setbit(key, offset, value);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  public Response<Boolean> setbit(byte[] key, long offset, byte[] value) {
    getClient(key).setbit(key, offset, value);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  public Response<String> setex(String key, int seconds, String value) {
    getClient(key).setex(key, seconds, value);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> setex(byte[] key, int seconds, byte[] value) {
    getClient(key).setex(key, seconds, value);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<Long> setnx(String key, String value) {
    getClient(key).setnx(key, value);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> setnx(byte[] key, byte[] value) {
    getClient(key).setnx(key, value);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> setrange(String key, long offset, String value) {
    getClient(key).setrange(key, offset, value);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> setrange(byte[] key, long offset, byte[] value) {
    getClient(key).setrange(key, offset, value);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Boolean> sismember(String key, String member) {
    getClient(key).sismember(key, member);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  public Response<Boolean> sismember(byte[] key, byte[] member) {
    getClient(key).sismember(key, member);
    return getResponse(BuilderFactory.BOOLEAN);
  }

  public Response<Set<String>> smembers(String key) {
    getClient(key).smembers(key);
    return getResponse(BuilderFactory.STRING_SET);
  }

  public Response<Set<byte[]>> smembers(byte[] key) {
    getClient(key).smembers(key);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  public Response<List<String>> sort(String key) {
    getClient(key).sort(key);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<List<byte[]>> sort(byte[] key) {
    getClient(key).sort(key);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  public Response<List<String>> sort(String key, SortingParams sortingParameters) {
    getClient(key).sort(key, sortingParameters);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<List<byte[]>> sort(byte[] key, SortingParams sortingParameters) {
    getClient(key).sort(key, sortingParameters);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  public Response<String> spop(String key) {
    getClient(key).spop(key);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<Set<String>> spop(String key, long count) {
    getClient(key).spop(key, count);
    return getResponse(BuilderFactory.STRING_SET);
  }

  public Response<byte[]> spop(byte[] key) {
    getClient(key).spop(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  public Response<Set<byte[]>> spop(byte[] key, long count) {
    getClient(key).spop(key, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  public Response<String> srandmember(String key) {
    getClient(key).srandmember(key);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<List<String>> srandmember(String key, int count) {
    getClient(key).srandmember(key, count);
    return getResponse(BuilderFactory.STRING_LIST);
  }

  public Response<byte[]> srandmember(byte[] key) {
    getClient(key).srandmember(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  public Response<List<byte[]>> srandmember(byte[] key, int count) {
    getClient(key).srandmember(key, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_LIST);
  }

  public Response<Long> srem(String key, String... member) {
    getClient(key).srem(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> srem(byte[] key, byte[]... member) {
    getClient(key).srem(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> strlen(String key) {
    getClient(key).strlen(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> strlen(byte[] key) {
    getClient(key).strlen(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> substr(String key, int start, int end) {
    getClient(key).substr(key, start, end);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> substr(byte[] key, int start, int end) {
    getClient(key).substr(key, start, end);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<Long> ttl(String key) {
    getClient(key).ttl(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> ttl(byte[] key) {
    getClient(key).ttl(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> type(String key) {
    getClient(key).type(key);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> type(byte[] key) {
    getClient(key).type(key);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<Long> zadd(String key, double score, String member) {
    getClient(key).zadd(key, score, member);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zadd(String key, Map<String, Double> scoreMembers) {
    getClient(key).zadd(key, scoreMembers);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zadd(byte[] key, double score, byte[] member) {
    getClient(key).zadd(key, score, member);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zcard(String key) {
    getClient(key).zcard(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zcard(byte[] key) {
    getClient(key).zcard(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zcount(String key, double min, double max) {
    getClient(key).zcount(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zcount(String key, String min, String max) {
    getClient(key).zcount(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zcount(byte[] key, double min, double max) {
    getClient(key).zcount(key, toByteArray(min), toByteArray(max));
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zcount(byte[] key, byte[] min, byte[] max) {
    getClient(key).zcount(key, min, max);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Double> zincrby(String key, double score, String member) {
    getClient(key).zincrby(key, score, member);
    return getResponse(BuilderFactory.DOUBLE);
  }

  public Response<Double> zincrby(byte[] key, double score, byte[] member) {
    getClient(key).zincrby(key, score, member);
    return getResponse(BuilderFactory.DOUBLE);
  }

  public Response<Set<String>> zrange(String key, long start, long end) {
    getClient(key).zrange(key, start, end);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  public Response<Set<byte[]>> zrange(byte[] key, long start, long end) {
    getClient(key).zrange(key, start, end);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  public Response<Set<String>> zrangeByScore(String key, double min, double max) {
    getClient(key).zrangeByScore(key, min, max);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  public Response<Set<byte[]>> zrangeByScore(byte[] key, double min, double max) {
    return zrangeByScore(key, toByteArray(min), toByteArray(max));
  }

  public Response<Set<String>> zrangeByScore(String key, String min, String max) {
    getClient(key).zrangeByScore(key, min, max);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  public Response<Set<byte[]>> zrangeByScore(byte[] key, byte[] min, byte[] max) {
    getClient(key).zrangeByScore(key, min, max);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  public Response<Set<String>> zrangeByScore(String key, double min, double max, int offset,
      int count) {
    getClient(key).zrangeByScore(key, min, max, offset, count);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  public Response<Set<String>> zrangeByScore(String key, String min, String max, int offset,
      int count) {
    getClient(key).zrangeByScore(key, min, max, offset, count);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  public Response<Set<byte[]>> zrangeByScore(byte[] key, double min, double max, int offset,
      int count) {
    return zrangeByScore(key, toByteArray(min), toByteArray(max), offset, count);
  }

  public Response<Set<byte[]>> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset,
      int count) {
    getClient(key).zrangeByScore(key, min, max, offset, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  public Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max) {
    getClient(key).zrangeByScoreWithScores(key, min, max);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  public Response<Set<Tuple>> zrangeByScoreWithScores(String key, String min, String max) {
    getClient(key).zrangeByScoreWithScores(key, min, max);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  public Response<Set<Tuple>> zrangeByScoreWithScores(byte[] key, double min, double max) {
    return zrangeByScoreWithScores(key, toByteArray(min), toByteArray(max));
  }

  public Response<Set<Tuple>> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
    getClient(key).zrangeByScoreWithScores(key, min, max);
    return getResponse(BuilderFactory.TUPLE_ZSET_BINARY);
  }

  public Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max,
      int offset, int count) {
    getClient(key).zrangeByScoreWithScores(key, min, max, offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  public Response<Set<Tuple>> zrangeByScoreWithScores(String key, String min, String max,
      int offset, int count) {
    getClient(key).zrangeByScoreWithScores(key, min, max, offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  public Response<Set<Tuple>> zrangeByScoreWithScores(byte[] key, double min, double max,
      int offset, int count) {
    getClient(key).zrangeByScoreWithScores(key, toByteArray(min), toByteArray(max), offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET_BINARY);
  }

  public Response<Set<Tuple>> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max,
      int offset, int count) {
    getClient(key).zrangeByScoreWithScores(key, min, max, offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET_BINARY);
  }

  public Response<Set<String>> zrevrangeByScore(String key, double max, double min) {
    getClient(key).zrevrangeByScore(key, max, min);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  public Response<Set<byte[]>> zrevrangeByScore(byte[] key, double max, double min) {
    getClient(key).zrevrangeByScore(key, toByteArray(max), toByteArray(min));
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  public Response<Set<String>> zrevrangeByScore(String key, String max, String min) {
    getClient(key).zrevrangeByScore(key, max, min);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  public Response<Set<byte[]>> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
    getClient(key).zrevrangeByScore(key, max, min);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  public Response<Set<String>> zrevrangeByScore(String key, double max, double min, int offset,
      int count) {
    getClient(key).zrevrangeByScore(key, max, min, offset, count);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  public Response<Set<String>> zrevrangeByScore(String key, String max, String min, int offset,
      int count) {
    getClient(key).zrevrangeByScore(key, max, min, offset, count);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  public Response<Set<byte[]>> zrevrangeByScore(byte[] key, double max, double min, int offset,
      int count) {
    getClient(key).zrevrangeByScore(key, toByteArray(max), toByteArray(min), offset, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  public Response<Set<byte[]>> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset,
      int count) {
    getClient(key).zrevrangeByScore(key, max, min, offset, count);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min) {
    getClient(key).zrevrangeByScoreWithScores(key, max, min);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min) {
    getClient(key).zrevrangeByScoreWithScores(key, max, min);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  public Response<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
    getClient(key).zrevrangeByScoreWithScores(key, toByteArray(max), toByteArray(min));
    return getResponse(BuilderFactory.TUPLE_ZSET_BINARY);
  }

  public Response<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
    getClient(key).zrevrangeByScoreWithScores(key, max, min);
    return getResponse(BuilderFactory.TUPLE_ZSET_BINARY);
  }

  public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min,
      int offset, int count) {
    getClient(key).zrevrangeByScoreWithScores(key, max, min, offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min,
      int offset, int count) {
    getClient(key).zrevrangeByScoreWithScores(key, max, min, offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  public Response<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, double max, double min,
      int offset, int count) {
    getClient(key).zrevrangeByScoreWithScores(key, toByteArray(max), toByteArray(min), offset,
      count);
    return getResponse(BuilderFactory.TUPLE_ZSET_BINARY);
  }

  public Response<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min,
      int offset, int count) {
    getClient(key).zrevrangeByScoreWithScores(key, max, min, offset, count);
    return getResponse(BuilderFactory.TUPLE_ZSET_BINARY);
  }

  public Response<Set<Tuple>> zrangeWithScores(String key, long start, long end) {
    getClient(key).zrangeWithScores(key, start, end);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  public Response<Set<Tuple>> zrangeWithScores(byte[] key, long start, long end) {
    getClient(key).zrangeWithScores(key, start, end);
    return getResponse(BuilderFactory.TUPLE_ZSET_BINARY);
  }

  public Response<Long> zrank(String key, String member) {
    getClient(key).zrank(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zrank(byte[] key, byte[] member) {
    getClient(key).zrank(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zrem(String key, String... member) {
    getClient(key).zrem(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zrem(byte[] key, byte[]... member) {
    getClient(key).zrem(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zremrangeByRank(String key, long start, long end) {
    getClient(key).zremrangeByRank(key, start, end);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zremrangeByRank(byte[] key, long start, long end) {
    getClient(key).zremrangeByRank(key, start, end);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zremrangeByScore(String key, double start, double end) {
    getClient(key).zremrangeByScore(key, start, end);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zremrangeByScore(String key, String start, String end) {
    getClient(key).zremrangeByScore(key, start, end);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zremrangeByScore(byte[] key, double start, double end) {
    getClient(key).zremrangeByScore(key, toByteArray(start), toByteArray(end));
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zremrangeByScore(byte[] key, byte[] start, byte[] end) {
    getClient(key).zremrangeByScore(key, start, end);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Set<String>> zrevrange(String key, long start, long end) {
    getClient(key).zrevrange(key, start, end);
    return getResponse(BuilderFactory.STRING_ZSET);
  }

  public Response<Set<byte[]>> zrevrange(byte[] key, long start, long end) {
    getClient(key).zrevrange(key, start, end);
    return getResponse(BuilderFactory.BYTE_ARRAY_ZSET);
  }

  public Response<Set<Tuple>> zrevrangeWithScores(String key, long start, long end) {
    getClient(key).zrevrangeWithScores(key, start, end);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  public Response<Set<Tuple>> zrevrangeWithScores(byte[] key, long start, long end) {
    getClient(key).zrevrangeWithScores(key, start, end);
    return getResponse(BuilderFactory.TUPLE_ZSET);
  }

  public Response<Long> zrevrank(String key, String member) {
    getClient(key).zrevrank(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> zrevrank(byte[] key, byte[] member) {
    getClient(key).zrevrank(key, member);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Double> zscore(String key, String member) {
    getClient(key).zscore(key, member);
    return getResponse(BuilderFactory.DOUBLE);
  }

  public Response<Double> zscore(byte[] key, byte[] member) {
    getClient(key).zscore(key, member);
    return getResponse(BuilderFactory.DOUBLE);
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

  public Response<Long> bitcount(String key) {
    getClient(key).bitcount(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> bitcount(String key, long start, long end) {
    getClient(key).bitcount(key, start, end);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> bitcount(byte[] key) {
    getClient(key).bitcount(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> bitcount(byte[] key, long start, long end) {
    getClient(key).bitcount(key, start, end);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<byte[]> dump(String key) {
    getClient(key).dump(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  public Response<byte[]> dump(byte[] key) {
    getClient(key).dump(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  public Response<String> migrate(String host, int port, String key, int destinationDb, int timeout) {
    getClient(key).migrate(host, port, key, destinationDb, timeout);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> migrate(byte[] host, int port, byte[] key, int destinationDb, int timeout) {
    getClient(key).migrate(host, port, key, destinationDb, timeout);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<Long> objectRefcount(String key) {
    getClient(key).objectRefcount(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> objectRefcount(byte[] key) {
    getClient(key).objectRefcount(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> objectEncoding(String key) {
    getClient(key).objectEncoding(key);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<byte[]> objectEncoding(byte[] key) {
    getClient(key).objectEncoding(key);
    return getResponse(BuilderFactory.BYTE_ARRAY);
  }

  public Response<Long> objectIdletime(String key) {
    getClient(key).objectIdletime(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> objectIdletime(byte[] key) {
    getClient(key).objectIdletime(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Deprecated
  public Response<Long> pexpire(String key, int milliseconds) {
    return pexpire(key, (long) milliseconds);
  }

  @Deprecated
  public Response<Long> pexpire(byte[] key, int milliseconds) {
    return pexpire(key, (long) milliseconds);
  }

  public Response<Long> pexpire(String key, long milliseconds) {
    getClient(key).pexpire(key, milliseconds);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> pexpire(byte[] key, long milliseconds) {
    getClient(key).pexpire(key, milliseconds);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> pexpireAt(String key, long millisecondsTimestamp) {
    getClient(key).pexpireAt(key, millisecondsTimestamp);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> pexpireAt(byte[] key, long millisecondsTimestamp) {
    getClient(key).pexpireAt(key, millisecondsTimestamp);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> pttl(String key) {
    getClient(key).pttl(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<Long> pttl(byte[] key) {
    getClient(key).pttl(key);
    return getResponse(BuilderFactory.LONG);
  }

  public Response<String> restore(String key, int ttl, byte[] serializedValue) {
    getClient(key).restore(key, ttl, serializedValue);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> restore(byte[] key, int ttl, byte[] serializedValue) {
    getClient(key).restore(key, ttl, serializedValue);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<Double> incrByFloat(String key, double increment) {
    getClient(key).incrByFloat(key, increment);
    return getResponse(BuilderFactory.DOUBLE);
  }

  public Response<Double> incrByFloat(byte[] key, double increment) {
    getClient(key).incrByFloat(key, increment);
    return getResponse(BuilderFactory.DOUBLE);
  }

  @Deprecated
  public Response<String> psetex(String key, int milliseconds, String value) {
    return psetex(key, (long) milliseconds, value);
  }

  @Deprecated
  public Response<String> psetex(byte[] key, int milliseconds, byte[] value) {
    return psetex(key, (long) milliseconds, value);
  }

  public Response<String> psetex(String key, long milliseconds, String value) {
    getClient(key).psetex(key, milliseconds, value);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> psetex(byte[] key, long milliseconds, byte[] value) {
    getClient(key).psetex(key, milliseconds, value);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> set(String key, String value, String nxxx) {
    getClient(key).set(key, value, nxxx);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> set(byte[] key, byte[] value, byte[] nxxx) {
    getClient(key).set(key, value, nxxx);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> set(String key, String value, String nxxx, String expx, int time) {
    getClient(key).set(key, value, nxxx, expx, time);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, int time) {
    getClient(key).set(key, value, nxxx, expx, time);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<Double> hincrByFloat(String key, String field, double increment) {
    getClient(key).hincrByFloat(key, field, increment);
    return getResponse(BuilderFactory.DOUBLE);
  }

  public Response<Double> hincrByFloat(byte[] key, byte[] field, double increment) {
    getClient(key).hincrByFloat(key, field, increment);
    return getResponse(BuilderFactory.DOUBLE);
  }

  public Response<String> eval(String script) {
    return this.eval(script, 0);
  }

  public Response<String> eval(String script, List<String> keys, List<String> args) {
    String[] argv = Jedis.getParams(keys, args);
    return this.eval(script, keys.size(), argv);
  }

  public Response<String> eval(String script, int numKeys, String... args) {
    getClient(script).eval(script, numKeys, args);
    return getResponse(BuilderFactory.STRING);
  }

  public Response<String> evalsha(String script) {
    return this.evalsha(script, 0);
  }

  public Response<String> evalsha(String sha1, List<String> keys, List<String> args) {
    String[] argv = Jedis.getParams(keys, args);
    return this.evalsha(sha1, keys.size(), argv);
  }

  public Response<String> evalsha(String sha1, int numKeys, String... args) {
    getClient(sha1).evalsha(sha1, numKeys, args);
    return getResponse(BuilderFactory.STRING);
  }

  @Override
  public Response<Long> pfadd(byte[] key, byte[]... elements) {
    getClient(key).pfadd(key, elements);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pfcount(byte[] key) {
    getClient(key).pfcount(key);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pfadd(String key, String... elements) {
    getClient(key).pfadd(key, elements);
    return getResponse(BuilderFactory.LONG);
  }

  @Override
  public Response<Long> pfcount(String key) {
    getClient(key).pfcount(key);
    return getResponse(BuilderFactory.LONG);
  }

}
