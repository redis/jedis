package redis.clients.jedis.async;

import redis.clients.jedis.*;
import redis.clients.jedis.async.callback.AsyncResponseCallback;
import redis.clients.jedis.async.commands.*;
import redis.clients.jedis.async.process.AsyncJedisTask;
import redis.clients.jedis.async.request.RequestBuilder;
import redis.clients.jedis.async.request.RequestParameterBuilder;
import redis.clients.util.SafeEncoder;
import redis.clients.util.Slowlog;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static redis.clients.jedis.BuilderFactory.*;
import static redis.clients.jedis.Protocol.Command.*;
import static redis.clients.jedis.Protocol.Keyword.LIMIT;
import static redis.clients.jedis.Protocol.toByteArray;

public class AsyncJedis extends AsyncBinaryJedis implements AsyncBasicCommands, AsyncJedisCommands,
    AsyncMultiKeyCommands, AsyncAdvancedJedisCommands, AsyncScriptingCommands, Closeable {
  public AsyncJedis(String host) throws IOException {
    super(host);
  }

  public AsyncJedis(String host, int port) throws IOException {
    super(host, port);
  }

  public AsyncJedis(String host, int port, String password) throws IOException {
    super(host, port, password);
  }

  // keys section

  @Override
  public void del(AsyncResponseCallback<Long> callback, String key) {
    del(callback, SafeEncoder.encode(key));
  }

  @Override
  public void del(AsyncResponseCallback<Long> callback, String... keys) {
    del(callback, SafeEncoder.encodeMany(keys));
  }

  @Override
  public void dump(AsyncResponseCallback<byte[]> callback, String key) {
    dump(callback, SafeEncoder.encode(key));
  }

  @Override
  public void exists(AsyncResponseCallback<Boolean> callback, String key) {
    exists(callback, SafeEncoder.encode(key));
  }

  @Override
  public void expire(AsyncResponseCallback<Long> callback, String key, int seconds) {
    expire(callback, SafeEncoder.encode(key), seconds);
  }

  @Override
  public void expireAt(AsyncResponseCallback<Long> callback, String key, long unixTime) {
    expireAt(callback, SafeEncoder.encode(key), unixTime);
  }

  @Override
  public void keys(AsyncResponseCallback<Set<String>> callback, String pattern) {
    byte[] request = RequestBuilder.build(KEYS, pattern);
    processor.registerRequest(new AsyncJedisTask(request, STRING_ZSET, callback));
  }

  @Override
  public void migrate(AsyncResponseCallback<String> callback, String host, int port, String key,
      int destinationDb, int timeout) {
    migrate(callback, SafeEncoder.encode(host), port, SafeEncoder.encode(key), destinationDb,
      timeout);
  }

  @Override
  public void move(AsyncResponseCallback<Long> callback, String key, int dbIndex) {
    move(callback, SafeEncoder.encode(key), dbIndex);
  }

  @Override
  public void objectRefcount(AsyncResponseCallback<Long> callback, String string) {
    objectRefcount(callback, SafeEncoder.encode(string));
  }

  @Override
  public void objectEncoding(AsyncResponseCallback<String> callback, String string) {
    byte[] request = RequestBuilder.build(OBJECT, Protocol.Keyword.ENCODING.raw,
      SafeEncoder.encode(string));
    processor.registerRequest(new AsyncJedisTask(request, STRING, callback));
  }

  @Override
  public void objectIdletime(AsyncResponseCallback<Long> callback, String string) {
    objectIdletime(callback, SafeEncoder.encode(string));
  }

  @Override
  public void persist(AsyncResponseCallback<Long> callback, String key) {
    persist(callback, SafeEncoder.encode(key));
  }

  @Override
  public void pexpire(AsyncResponseCallback<Long> callback, String key, long milliseconds) {
    pexpire(callback, SafeEncoder.encode(key), milliseconds);
  }

  @Override
  public void pexpireAt(AsyncResponseCallback<Long> callback, String key, long millisecondsTimestamp) {
    pexpireAt(callback, SafeEncoder.encode(key), millisecondsTimestamp);
  }

  @Override
  public void pttl(AsyncResponseCallback<Long> callback, String key) {
    pttl(callback, SafeEncoder.encode(key));
  }

  @Override
  public void randomKey(AsyncResponseCallback<String> callback) {
    byte[] request = RequestBuilder.build(RANDOMKEY);
    processor.registerRequest(new AsyncJedisTask(request, STRING, callback));
  }

  @Override
  public void rename(AsyncResponseCallback<String> callback, String oldkey, String newkey) {
    rename(callback, SafeEncoder.encode(oldkey), SafeEncoder.encode(newkey));
  }

  @Override
  public void renamenx(AsyncResponseCallback<Long> callback, String oldkey, String newkey) {
    renamenx(callback, SafeEncoder.encode(oldkey), SafeEncoder.encode(newkey));
  }

  @Override
  public void restore(AsyncResponseCallback<String> callback, String key, int ttl,
      byte[] serializedValue) {
    restore(callback, SafeEncoder.encode(key), ttl, serializedValue);
  }

  @Override
  public void sort(AsyncResponseCallback<List<String>> callback, String key) {
    byte[] request = RequestBuilder.build(SORT, key);
    processor.registerRequest(new AsyncJedisTask(request, STRING_LIST, callback));
  }

  @Override
  public void sort(AsyncResponseCallback<List<String>> callback, String key,
      SortingParams sortingParameters) {
    byte[] request = RequestBuilder.build(SORT,
      RequestParameterBuilder.buildSortWithParameter(key, sortingParameters));
    processor.registerRequest(new AsyncJedisTask(request, STRING_LIST, callback));
  }

  @Override
  public void sort(AsyncResponseCallback<Long> callback, String key, String dstkey) {
    sort(callback, SafeEncoder.encode(key), SafeEncoder.encode(dstkey));
  }

  @Override
  public void sort(AsyncResponseCallback<Long> callback, String key,
      SortingParams sortingParameters, String dstkey) {
    sort(callback, SafeEncoder.encode(key), sortingParameters, SafeEncoder.encode(dstkey));
  }

  @Override
  public void ttl(AsyncResponseCallback<Long> callback, String key) {
    ttl(callback, SafeEncoder.encode(key));
  }

  @Override
  public void type(AsyncResponseCallback<String> callback, String key) {
    type(callback, SafeEncoder.encode(key));
  }

  // string section

  @Override
  public void append(AsyncResponseCallback<Long> callback, String key, String value) {
    append(callback, SafeEncoder.encode(key), SafeEncoder.encode(value));
  }

  @Override
  public void bitcount(AsyncResponseCallback<Long> callback, String key) {
    bitcount(callback, SafeEncoder.encode(key));
  }

  @Override
  public void bitcount(AsyncResponseCallback<Long> callback, String key, long start, long end) {
    bitcount(callback, SafeEncoder.encode(key), start, end);
  }

  @Override
  public void bitop(AsyncResponseCallback<Long> callback, BitOP op, String destKey,
      String... srcKeys) {
    bitop(callback, op, SafeEncoder.encode(destKey), SafeEncoder.encodeMany(srcKeys));
  }

  @Override
  public void decr(AsyncResponseCallback<Long> callback, String key) {
    decr(callback, SafeEncoder.encode(key));
  }

  @Override
  public void decrBy(AsyncResponseCallback<Long> callback, String key, long integer) {
    decrBy(callback, SafeEncoder.encode(key), integer);
  }

  @Override
  public void get(AsyncResponseCallback<String> callback, String key) {
    byte[] request = RequestBuilder.build(GET, SafeEncoder.encode(key));
    processor.registerRequest(new AsyncJedisTask(request, STRING, callback));
  }

  @Override
  public void getbit(AsyncResponseCallback<Boolean> callback, String key, long offset) {
    getbit(callback, SafeEncoder.encode(key), offset);
  }

  @Override
  public void getrange(AsyncResponseCallback<String> callback, String key, long startOffset,
      long endOffset) {
    byte[] request = RequestBuilder.build(GETRANGE, SafeEncoder.encode(key),
      toByteArray(startOffset), toByteArray(endOffset));
    processor.registerRequest(new AsyncJedisTask(request, STRING, callback));
  }

  @Override
  public void getSet(AsyncResponseCallback<String> callback, String key, String value) {
    byte[] request = RequestBuilder.build(GETSET, SafeEncoder.encode(key),
      SafeEncoder.encode(value));
    processor.registerRequest(new AsyncJedisTask(request, STRING, callback));
  }

  @Override
  public void incr(AsyncResponseCallback<Long> callback, String key) {
    incr(callback, SafeEncoder.encode(key));
  }

  @Override
  public void incrBy(AsyncResponseCallback<Long> callback, String key, long integer) {
    incrBy(callback, SafeEncoder.encode(key), integer);
  }

  @Override
  public void incrByFloat(AsyncResponseCallback<Double> callback, String key, double increment) {
    incrByFloat(callback, SafeEncoder.encode(key), increment);
  }

  @Override
  public void mget(AsyncResponseCallback<List<String>> callback, String... keys) {
    byte[] request = RequestBuilder.build(MGET, keys);
    processor.registerRequest(new AsyncJedisTask(request, STRING_LIST, callback));
  }

  @Override
  public void mset(AsyncResponseCallback<String> callback, String... keysvalues) {
    mset(callback, SafeEncoder.encodeMany(keysvalues));
  }

  @Override
  public void msetnx(AsyncResponseCallback<Long> callback, String... keysvalues) {
    msetnx(callback, SafeEncoder.encodeMany(keysvalues));
  }

  @Override
  public void psetex(AsyncResponseCallback<String> callback, String key, int milliseconds,
      String value) {
    psetex(callback, SafeEncoder.encode(key), milliseconds, SafeEncoder.encode(value));
  }

  @Override
  public void set(AsyncResponseCallback<String> callback, String key, String value) {
    set(callback, SafeEncoder.encode(key), SafeEncoder.encode(value));
  }

  @Override
  public void set(AsyncResponseCallback<String> callback, String key, String value, String nxxx,
      String expx, long time) {
    set(callback, SafeEncoder.encode(key), SafeEncoder.encode(value), SafeEncoder.encode(nxxx),
      SafeEncoder.encode(expx), time);
  }

  @Override
  public void setbit(AsyncResponseCallback<Boolean> callback, String key, long offset, boolean value) {
    setbit(callback, SafeEncoder.encode(key), offset, value);
  }

  @Override
  public void setbit(AsyncResponseCallback<Boolean> callback, String key, long offset, String value) {
    setbit(callback, SafeEncoder.encode(key), offset, SafeEncoder.encode(value));
  }

  @Override
  public void setnx(AsyncResponseCallback<Long> callback, String key, String value) {
    setnx(callback, SafeEncoder.encode(key), SafeEncoder.encode(value));
  }

  @Override
  public void setex(AsyncResponseCallback<String> callback, String key, int seconds, String value) {
    setex(callback, SafeEncoder.encode(key), seconds, SafeEncoder.encode(value));
  }

  @Override
  public void setrange(AsyncResponseCallback<Long> callback, String key, long offset, String value) {
    setrange(callback, SafeEncoder.encode(key), offset, SafeEncoder.encode(value));
  }

  @Override
  public void strlen(AsyncResponseCallback<Long> callback, String key) {
    strlen(callback, SafeEncoder.encode(key));
  }

  @Override
  public void substr(AsyncResponseCallback<String> callback, String key, int start, int end) {
    byte[] request = RequestBuilder.build(SUBSTR, SafeEncoder.encode(key), toByteArray(start),
      toByteArray(end));
    processor.registerRequest(new AsyncJedisTask(request, STRING, callback));
  }

  // hash section

  @Override
  public void hdel(AsyncResponseCallback<Long> callback, String key, String... field) {
    hdel(callback, SafeEncoder.encode(key), SafeEncoder.encodeMany(field));
  }

  @Override
  public void hexists(AsyncResponseCallback<Boolean> callback, String key, String field) {
    hexists(callback, SafeEncoder.encode(key), SafeEncoder.encode(field));
  }

  @Override
  public void hget(AsyncResponseCallback<String> callback, String key, String field) {
    byte[] request = RequestBuilder.build(HGET, key, field);
    processor.registerRequest(new AsyncJedisTask(request, STRING, callback));
  }

  @Override
  public void hgetAll(AsyncResponseCallback<Map<String, String>> callback, String key) {
    byte[] request = RequestBuilder.build(HGETALL, key);
    processor.registerRequest(new AsyncJedisTask(request, STRING_MAP, callback));
  }

  @Override
  public void hincrBy(AsyncResponseCallback<Long> callback, String key, String field, long value) {
    hincrBy(callback, SafeEncoder.encode(key), SafeEncoder.encode(field), value);
  }

  @Override
  public void hkeys(AsyncResponseCallback<Set<String>> callback, String key) {
    byte[] request = RequestBuilder.build(HKEYS, key);
    processor.registerRequest(new AsyncJedisTask(request, STRING_SET, callback));
  }

  @Override
  public void hlen(AsyncResponseCallback<Long> callback, String key) {
    hlen(callback, SafeEncoder.encode(key));
  }

  @Override
  public void hmget(AsyncResponseCallback<List<String>> callback, String key, String... fields) {
    byte[] request = RequestBuilder.build(HMGET,
      RequestParameterBuilder.buildMultiArgsParameter(key, fields));
    processor.registerRequest(new AsyncJedisTask(request, STRING_LIST, callback));
  }

  @Override
  public void hmset(AsyncResponseCallback<String> callback, String key, Map<String, String> hash) {
    byte[] request = RequestBuilder.build(HMSET,
      RequestParameterBuilder.buildSetMultiAddParameter(key, hash));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void hset(AsyncResponseCallback<Long> callback, String key, String field, String value) {
    hset(callback, SafeEncoder.encode(key), SafeEncoder.encode(field), SafeEncoder.encode(value));
  }

  @Override
  public void hsetnx(AsyncResponseCallback<Long> callback, String key, String field, String value) {
    hsetnx(callback, SafeEncoder.encode(key), SafeEncoder.encode(field), SafeEncoder.encode(value));
  }

  @Override
  public void hvals(AsyncResponseCallback<List<String>> callback, String key) {
    byte[] request = RequestBuilder.build(HVALS, key);
    processor.registerRequest(new AsyncJedisTask(request, STRING_LIST, callback));
  }

  // list section

  @Override
  public void lindex(AsyncResponseCallback<String> callback, String key, long index) {
    byte[] request = RequestBuilder.build(LINDEX, SafeEncoder.encode(key), toByteArray(index));
    processor.registerRequest(new AsyncJedisTask(request, STRING, callback));
  }

  @Override
  public void linsert(AsyncResponseCallback<Long> callback, String key, Client.LIST_POSITION where,
      String pivot, String value) {
    linsert(callback, SafeEncoder.encode(key), where, SafeEncoder.encode(pivot),
      SafeEncoder.encode(value));
  }

  @Override
  public void llen(AsyncResponseCallback<Long> callback, String key) {
    llen(callback, SafeEncoder.encode(key));
  }

  @Override
  public void lpop(AsyncResponseCallback<String> callback, String key) {
    byte[] request = RequestBuilder.build(LPOP, key);
    processor.registerRequest(new AsyncJedisTask(request, STRING, callback));
  }

  @Override
  public void lpush(AsyncResponseCallback<Long> callback, String key, String... strings) {
    lpush(callback, SafeEncoder.encode(key), SafeEncoder.encodeMany(strings));
  }

  @Override
  public void lpushx(AsyncResponseCallback<Long> callback, String key, String... strings) {
    lpushx(callback, SafeEncoder.encode(key), SafeEncoder.encodeMany(strings));
  }

  @Override
  public void lrange(AsyncResponseCallback<List<String>> callback, String key, long start, long end) {
    byte[] request = RequestBuilder.build(LRANGE, SafeEncoder.encode(key), toByteArray(start),
      toByteArray(end));
    processor.registerRequest(new AsyncJedisTask(request, STRING_LIST, callback));
  }

  @Override
  public void lrem(AsyncResponseCallback<Long> callback, String key, long count, String value) {
    lrem(callback, SafeEncoder.encode(key), count, SafeEncoder.encode(value));
  }

  @Override
  public void lset(AsyncResponseCallback<String> callback, String key, long index, String value) {
    lset(callback, SafeEncoder.encode(key), index, SafeEncoder.encode(value));
  }

  @Override
  public void ltrim(AsyncResponseCallback<String> callback, String key, long start, long end) {
    ltrim(callback, SafeEncoder.encode(key), start, end);
  }

  @Override
  public void rpop(AsyncResponseCallback<String> callback, String key) {
    byte[] request = RequestBuilder.build(RPOP, key);
    processor.registerRequest(new AsyncJedisTask(request, STRING, callback));
  }

  @Override
  public void rpoplpush(AsyncResponseCallback<String> callback, String srckey, String dstkey) {
    byte[] request = RequestBuilder.build(RPOPLPUSH, srckey, dstkey);
    processor.registerRequest(new AsyncJedisTask(request, STRING, callback));
  }

  @Override
  public void rpush(AsyncResponseCallback<Long> callback, String key, String... strings) {
    rpush(callback, SafeEncoder.encode(key), SafeEncoder.encodeMany(strings));
  }

  @Override
  public void rpushx(AsyncResponseCallback<Long> callback, String key, String... strings) {
    rpushx(callback, SafeEncoder.encode(key), SafeEncoder.encodeMany(strings));
  }

  // set section

  @Override
  public void sadd(AsyncResponseCallback<Long> callback, String key, String... members) {
    sadd(callback, SafeEncoder.encode(key), SafeEncoder.encodeMany(members));
  }

  @Override
  public void scard(AsyncResponseCallback<Long> callback, String key) {
    scard(callback, SafeEncoder.encode(key));
  }

  @Override
  public void sdiff(AsyncResponseCallback<Set<String>> callback, String... keys) {
    byte[] request = RequestBuilder.build(SDIFF, keys);
    processor.registerRequest(new AsyncJedisTask(request, STRING_ZSET, callback));
  }

  @Override
  public void sdiffstore(AsyncResponseCallback<Long> callback, String dstkey, String... keys) {
    sdiffstore(callback, SafeEncoder.encode(dstkey), SafeEncoder.encodeMany(keys));
  }

  @Override
  public void sinter(AsyncResponseCallback<Set<String>> callback, String... keys) {
    byte[] request = RequestBuilder.build(SINTER, keys);
    processor.registerRequest(new AsyncJedisTask(request, STRING_ZSET, callback));
  }

  @Override
  public void sinterstore(AsyncResponseCallback<Long> callback, String dstkey, String... keys) {
    sinterstore(callback, SafeEncoder.encode(dstkey), SafeEncoder.encodeMany(keys));
  }

  @Override
  public void sismember(AsyncResponseCallback<Boolean> callback, String key, String member) {
    sismember(callback, SafeEncoder.encode(key), SafeEncoder.encode(member));
  }

  @Override
  public void smembers(AsyncResponseCallback<Set<String>> callback, String key) {
    byte[] request = RequestBuilder.build(SMEMBERS, key);
    processor.registerRequest(new AsyncJedisTask(request, STRING_ZSET, callback));
  }

  @Override
  public void smove(AsyncResponseCallback<Long> callback, String srckey, String dstkey,
      String member) {
    smove(callback, SafeEncoder.encode(srckey), SafeEncoder.encode(dstkey),
      SafeEncoder.encode(member));
  }

  @Override
  public void spop(AsyncResponseCallback<String> callback, String key) {
    byte[] request = RequestBuilder.build(SPOP, key);
    processor.registerRequest(new AsyncJedisTask(request, STRING, callback));
  }

  @Override
  public void srandmember(AsyncResponseCallback<String> callback, String key) {
    byte[] request = RequestBuilder.build(SRANDMEMBER, key);
    processor.registerRequest(new AsyncJedisTask(request, STRING, callback));
  }

  @Override
  public void srem(AsyncResponseCallback<Long> callback, String key, String... members) {
    srem(callback, SafeEncoder.encode(key), SafeEncoder.encodeMany(members));
  }

  @Override
  public void sunion(AsyncResponseCallback<Set<String>> callback, String... keys) {
    byte[] request = RequestBuilder.build(SUNION, keys);
    processor.registerRequest(new AsyncJedisTask(request, STRING_ZSET, callback));
  }

  @Override
  public void sunionstore(AsyncResponseCallback<Long> callback, String dstkey, String... keys) {
    sunionstore(callback, SafeEncoder.encode(dstkey), SafeEncoder.encodeMany(keys));
  }

  // sorted set section

  @Override
  public void zadd(AsyncResponseCallback<Long> callback, String key, double score, String member) {
    zadd(callback, SafeEncoder.encode(key), score, SafeEncoder.encode(member));
  }

  @Override
  public void zadd(AsyncResponseCallback<Long> callback, String key,
      Map<String, Double> scoreMembers) {
    byte[] request = RequestBuilder.build(ZADD,
      RequestParameterBuilder.buildSortedSetMultiAddParameter(key, scoreMembers));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void zcard(AsyncResponseCallback<Long> callback, String key) {
    zcard(callback, SafeEncoder.encode(key));
  }

  @Override
  public void zcount(AsyncResponseCallback<Long> callback, String key, double min, double max) {
    zcount(callback, SafeEncoder.encode(key), min, max);
  }

  @Override
  public void zcount(AsyncResponseCallback<Long> callback, String key, String min, String max) {
    zcount(callback, SafeEncoder.encode(key), SafeEncoder.encode(min), SafeEncoder.encode(max));
  }

  @Override
  public void zincrby(AsyncResponseCallback<Double> callback, String key, double score,
      String member) {
    zincrby(callback, SafeEncoder.encode(key), score, SafeEncoder.encode(member));
  }

  @Override
  public void zinterstore(AsyncResponseCallback<Long> callback, String dstkey, String... sets) {
    zinterstore(callback, SafeEncoder.encode(dstkey), SafeEncoder.encodeMany(sets));
  }

  @Override
  public void zinterstore(AsyncResponseCallback<Long> callback, String dstkey, ZParams params,
      String... sets) {
    byte[] request = RequestBuilder.build(ZINTERSTORE,
      RequestParameterBuilder.buildSortedSetStoreWithParameter(dstkey, params, sets));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void zrange(AsyncResponseCallback<Set<String>> callback, String key, long start, long end) {
    byte[] request = RequestBuilder.build(ZRANGE, SafeEncoder.encode(key), toByteArray(start),
      toByteArray(end));
    processor.registerRequest(new AsyncJedisTask(request, STRING_ZSET, callback));
  }

  @Override
  public void zrangeWithScores(AsyncResponseCallback<Set<Tuple>> callback, String key, long start,
      long end) {
    byte[] request = RequestBuilder.build(ZRANGE, SafeEncoder.encode(key), toByteArray(start),
      toByteArray(end), Protocol.Keyword.WITHSCORES.raw);
    processor.registerRequest(new AsyncJedisTask(request, TUPLE_ZSET, callback));
  }

  @Override
  public void zrangeByScore(AsyncResponseCallback<Set<String>> callback, String key, double min,
      double max) {
    byte byteArrayMin[] = RequestParameterBuilder.convertRangeParameter(min);
    byte byteArrayMax[] = RequestParameterBuilder.convertRangeParameter(max);

    byte[] request = RequestBuilder.build(ZRANGEBYSCORE, SafeEncoder.encode(key), byteArrayMin,
      byteArrayMax);
    processor.registerRequest(new AsyncJedisTask(request, STRING_ZSET, callback));
  }

  @Override
  public void zrangeByScore(AsyncResponseCallback<Set<String>> callback, String key, String min,
      String max) {
    byte[] request = RequestBuilder.build(ZRANGEBYSCORE, key, min, max);
    processor.registerRequest(new AsyncJedisTask(request, STRING_ZSET, callback));
  }

  @Override
  public void zrangeByScore(AsyncResponseCallback<Set<String>> callback, String key, double min,
      double max, int offset, int count) {
    byte byteArrayMin[] = RequestParameterBuilder.convertRangeParameter(min);
    byte byteArrayMax[] = RequestParameterBuilder.convertRangeParameter(max);

    byte[] request = RequestBuilder.build(ZRANGEBYSCORE, SafeEncoder.encode(key), byteArrayMin,
      byteArrayMax, Protocol.Keyword.LIMIT.raw, toByteArray(offset), toByteArray(count));
    processor.registerRequest(new AsyncJedisTask(request, STRING_ZSET, callback));
  }

  @Override
  public void zrangeByScore(AsyncResponseCallback<Set<String>> callback, String key, String min,
      String max, int offset, int count) {
    byte[] request = RequestBuilder.build(ZRANGEBYSCORE, SafeEncoder.encode(key),
      SafeEncoder.encode(min), SafeEncoder.encode(max), Protocol.Keyword.LIMIT.raw,
      toByteArray(offset), toByteArray(count));
    processor.registerRequest(new AsyncJedisTask(request, STRING_ZSET, callback));
  }

  @Override
  public void zrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, String key,
      String min, String max) {
    byte[] request = RequestBuilder.build(ZRANGEBYSCORE, SafeEncoder.encode(key),
      SafeEncoder.encode(min), SafeEncoder.encode(max), Protocol.Keyword.WITHSCORES.raw);
    processor.registerRequest(new AsyncJedisTask(request, TUPLE_ZSET, callback));
  }

  @Override
  public void zrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, String key,
      double min, double max) {
    byte byteArrayMin[] = RequestParameterBuilder.convertRangeParameter(min);
    byte byteArrayMax[] = RequestParameterBuilder.convertRangeParameter(max);

    byte[] request = RequestBuilder.build(ZRANGEBYSCORE, SafeEncoder.encode(key), byteArrayMin,
      byteArrayMax, Protocol.Keyword.WITHSCORES.raw);
    processor.registerRequest(new AsyncJedisTask(request, TUPLE_ZSET, callback));
  }

  @Override
  public void zrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, String key,
      double min, double max, int offset, int count) {
    byte byteArrayMin[] = RequestParameterBuilder.convertRangeParameter(min);
    byte byteArrayMax[] = RequestParameterBuilder.convertRangeParameter(max);

    byte[] request = RequestBuilder.build(ZRANGEBYSCORE, SafeEncoder.encode(key), byteArrayMin,
      byteArrayMax, Protocol.Keyword.LIMIT.raw, toByteArray(offset), toByteArray(count),
      Protocol.Keyword.WITHSCORES.raw);
    processor.registerRequest(new AsyncJedisTask(request, TUPLE_ZSET, callback));
  }

  @Override
  public void zrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, String key,
      String min, String max, int offset, int count) {
    byte[] request = RequestBuilder.build(ZRANGEBYSCORE, SafeEncoder.encode(key),
      SafeEncoder.encode(min), SafeEncoder.encode(max), Protocol.Keyword.LIMIT.raw,
      toByteArray(offset), toByteArray(count), Protocol.Keyword.WITHSCORES.raw);

    processor.registerRequest(new AsyncJedisTask(request, TUPLE_ZSET, callback));
  }

  @Override
  public void zrank(AsyncResponseCallback<Long> callback, String key, String member) {
    zrank(callback, SafeEncoder.encode(key), SafeEncoder.encode(member));
  }

  @Override
  public void zrem(AsyncResponseCallback<Long> callback, String key, String... members) {
    zrem(callback, SafeEncoder.encode(key), SafeEncoder.encodeMany(members));
  }

  @Override
  public void zremrangeByRank(AsyncResponseCallback<Long> callback, String key, long start, long end) {
    zremrangeByRank(callback, SafeEncoder.encode(key), start, end);
  }

  @Override
  public void zremrangeByScore(AsyncResponseCallback<Long> callback, String key, double start,
      double end) {
    zremrangeByScore(callback, SafeEncoder.encode(key), start, end);
  }

  @Override
  public void zremrangeByScore(AsyncResponseCallback<Long> callback, String key, String start,
      String end) {
    zremrangeByScore(callback, SafeEncoder.encode(key), SafeEncoder.encode(start),
      SafeEncoder.encode(end));
  }

  @Override
  public void zrevrange(AsyncResponseCallback<Set<String>> callback, String key, long start,
      long end) {
    byte[] request = RequestBuilder.build(ZREVRANGE, SafeEncoder.encode(key), toByteArray(start),
      toByteArray(end));
    processor.registerRequest(new AsyncJedisTask(request, STRING_ZSET, callback));
  }

  @Override
  public void zrevrangeWithScores(AsyncResponseCallback<Set<Tuple>> callback, String key,
      long start, long end) {
    byte[] request = RequestBuilder.build(ZREVRANGE, SafeEncoder.encode(key), toByteArray(start),
      toByteArray(end), Protocol.Keyword.WITHSCORES.raw);
    processor.registerRequest(new AsyncJedisTask(request, TUPLE_ZSET, callback));
  }

  @Override
  public void zrevrangeByScore(AsyncResponseCallback<Set<String>> callback, String key, double max,
      double min) {
    byte byteArrayMin[] = RequestParameterBuilder.convertRangeParameter(min);
    byte byteArrayMax[] = RequestParameterBuilder.convertRangeParameter(max);

    byte[] request = RequestBuilder.build(ZREVRANGEBYSCORE, SafeEncoder.encode(key), byteArrayMax,
      byteArrayMin);
    processor.registerRequest(new AsyncJedisTask(request, STRING_ZSET, callback));
  }

  @Override
  public void zrevrangeByScore(AsyncResponseCallback<Set<String>> callback, String key, String max,
      String min) {
    byte[] request = RequestBuilder.build(ZREVRANGEBYSCORE, key, max, min);
    processor.registerRequest(new AsyncJedisTask(request, STRING_ZSET, callback));
  }

  @Override
  public void zrevrangeByScore(AsyncResponseCallback<Set<String>> callback, String key, double max,
      double min, int offset, int count) {
    byte byteArrayMin[] = RequestParameterBuilder.convertRangeParameter(min);
    byte byteArrayMax[] = RequestParameterBuilder.convertRangeParameter(max);

    byte[] request = RequestBuilder.build(ZREVRANGEBYSCORE, SafeEncoder.encode(key), byteArrayMax,
      byteArrayMin, Protocol.Keyword.LIMIT.raw, toByteArray(offset), toByteArray(count));
    processor.registerRequest(new AsyncJedisTask(request, STRING_ZSET, callback));
  }

  @Override
  public void zrevrangeByScore(AsyncResponseCallback<Set<String>> callback, String key, String max,
      String min, int offset, int count) {
    byte[] request = RequestBuilder.build(ZREVRANGEBYSCORE, SafeEncoder.encode(key),
      SafeEncoder.encode(max), SafeEncoder.encode(min), Protocol.Keyword.LIMIT.raw,
      toByteArray(offset), toByteArray(count));
    processor.registerRequest(new AsyncJedisTask(request, STRING_ZSET, callback));
  }

  @Override
  public void zrevrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, String key,
      double max, double min) {
    byte byteArrayMin[] = RequestParameterBuilder.convertRangeParameter(min);
    byte byteArrayMax[] = RequestParameterBuilder.convertRangeParameter(max);

    byte[] request = RequestBuilder.build(ZREVRANGEBYSCORE, SafeEncoder.encode(key), byteArrayMax,
      byteArrayMin, Protocol.Keyword.WITHSCORES.raw);
    processor.registerRequest(new AsyncJedisTask(request, TUPLE_ZSET, callback));
  }

  @Override
  public void zrevrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, String key,
      String max, String min) {
    byte[] request = RequestBuilder.build(ZREVRANGEBYSCORE, SafeEncoder.encode(key),
      SafeEncoder.encode(max), SafeEncoder.encode(min), Protocol.Keyword.WITHSCORES.raw);
    processor.registerRequest(new AsyncJedisTask(request, TUPLE_ZSET, callback));
  }

  @Override
  public void zrevrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, String key,
      double max, double min, int offset, int count) {
    byte byteArrayMin[] = RequestParameterBuilder.convertRangeParameter(min);
    byte byteArrayMax[] = RequestParameterBuilder.convertRangeParameter(max);

    byte[] request = RequestBuilder.build(ZREVRANGEBYSCORE, SafeEncoder.encode(key), byteArrayMax,
      byteArrayMin, Protocol.Keyword.LIMIT.raw, toByteArray(offset), toByteArray(count),
      Protocol.Keyword.WITHSCORES.raw);
    processor.registerRequest(new AsyncJedisTask(request, TUPLE_ZSET, callback));
  }

  @Override
  public void zrevrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, String key,
      String max, String min, int offset, int count) {
    byte[] request = RequestBuilder.build(ZREVRANGEBYSCORE, SafeEncoder.encode(key),
      SafeEncoder.encode(max), SafeEncoder.encode(min), Protocol.Keyword.LIMIT.raw,
      toByteArray(offset), toByteArray(count), Protocol.Keyword.WITHSCORES.raw);
    processor.registerRequest(new AsyncJedisTask(request, TUPLE_ZSET, callback));
  }

  @Override
  public void zrevrank(AsyncResponseCallback<Long> callback, String key, String member) {
    zrevrank(callback, SafeEncoder.encode(key), SafeEncoder.encode(member));
  }

  @Override
  public void zscore(AsyncResponseCallback<Double> callback, String key, String member) {
    zscore(callback, SafeEncoder.encode(key), SafeEncoder.encode(member));
  }

  @Override
  public void zunionstore(AsyncResponseCallback<Long> callback, String dstkey, String... sets) {
    zunionstore(callback, SafeEncoder.encode(dstkey), SafeEncoder.encodeMany(sets));
  }

  @Override
  public void zunionstore(AsyncResponseCallback<Long> callback, String dstkey, ZParams params,
      String... sets) {
    byte[] request = RequestBuilder.build(ZUNIONSTORE,
      RequestParameterBuilder.buildSortedSetStoreWithParameter(dstkey, params, sets));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  // script section

  @Override
  public void publish(AsyncResponseCallback<Long> callback, String channel, String message) {
    publish(callback, SafeEncoder.encode(channel), SafeEncoder.encode(message));
  }

  // server section

  @Override
  public void configGet(AsyncResponseCallback<List<String>> callback, String pattern) {
    byte[] request = RequestBuilder.build(CONFIG, Protocol.Keyword.GET.raw,
      SafeEncoder.encode(pattern));
    processor.registerRequest(new AsyncJedisTask(request, STRING_LIST, callback));
  }

  public void configSet(AsyncResponseCallback<String> callback, String parameter, String value) {
    byte[] request = RequestBuilder.build(CONFIG, Protocol.Keyword.SET.raw,
      SafeEncoder.encode(parameter), SafeEncoder.encode(value));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void slowlogGet(AsyncResponseCallback<List<Slowlog>> callback) {
    byte[] request = RequestBuilder.build(SLOWLOG, Protocol.Keyword.GET.raw);
    processor.registerRequest(new AsyncJedisTask(request, SLOWLOG_LIST, callback));
  }

  @Override
  public void slowlogGet(AsyncResponseCallback<List<Slowlog>> callback, long entries) {
    byte[] request = RequestBuilder.build(SLOWLOG, Protocol.Keyword.GET.raw, toByteArray(entries));
    processor.registerRequest(new AsyncJedisTask(request, SLOWLOG_LIST, callback));
  }

  @Override
  public void eval(AsyncResponseCallback<Object> callback, String script, int keyCount,
      String... params) {
    byte[] request = RequestBuilder.build(EVAL,
      RequestParameterBuilder.buildEvalParameter(script, keyCount, params));
    processor.registerRequest(new AsyncJedisTask(request, EVAL_STRING, callback));
  }

  @Override
  public void eval(AsyncResponseCallback<Object> callback, String script, List<String> keys,
      List<String> args) {
    eval(callback, script, keys.size(), RequestParameterBuilder.convertEvalListArgs(keys, args));
  }

  @Override
  public void eval(AsyncResponseCallback<Object> callback, String script) {
    eval(callback, script, 0);
  }

  @Override
  public void evalsha(AsyncResponseCallback<Object> callback, String sha1) {
    evalsha(callback, sha1, 0);
  }

  @Override
  public void evalsha(AsyncResponseCallback<Object> callback, String sha1, List<String> keys,
      List<String> args) {
    evalsha(callback, sha1, keys.size(), RequestParameterBuilder.convertEvalListArgs(keys, args));
  }

  @Override
  public void evalsha(AsyncResponseCallback<Object> callback, String sha1, int keyCount,
      String... params) {
    byte[] request = RequestBuilder.build(EVALSHA,
      RequestParameterBuilder.buildEvalParameter(sha1, keyCount, params));
    processor.registerRequest(new AsyncJedisTask(request, EVAL_STRING, callback));
  }

  @Override
  public void scriptExists(AsyncResponseCallback<List<Boolean>> callback, String... sha1) {
    byte[] request = RequestBuilder.build(SCRIPT,
      RequestParameterBuilder.joinParameters(Protocol.Keyword.EXISTS.raw, sha1));
    processor.registerRequest(new AsyncJedisTask(request, BOOLEAN_LIST, callback));
  }

  @Override
  public void scriptLoad(AsyncResponseCallback<String> callback, String script) {
    byte[] request = RequestBuilder.build(SCRIPT, Protocol.Keyword.LOAD.raw,
      SafeEncoder.encode(script));
    processor.registerRequest(new AsyncJedisTask(request, STRING, callback));
  }
}
