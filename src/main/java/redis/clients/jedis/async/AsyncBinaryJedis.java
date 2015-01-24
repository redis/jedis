package redis.clients.jedis.async;

import redis.clients.jedis.*;
import redis.clients.jedis.async.callback.AsyncResponseCallback;
import redis.clients.jedis.async.commands.*;
import redis.clients.jedis.async.process.AsyncDispatcher;
import redis.clients.jedis.async.process.AsyncJedisTask;
import redis.clients.jedis.async.request.RequestBuilder;
import redis.clients.jedis.async.request.RequestParameterBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static redis.clients.jedis.BuilderFactory.*;
import static redis.clients.jedis.Protocol.Command.*;
import static redis.clients.jedis.Protocol.toByteArray;

public class AsyncBinaryJedis implements AsyncBasicCommands, AsyncBinaryJedisCommands,
    AsyncMultiKeyBinaryCommands, AsyncAdvancedBinaryJedisCommands, AsyncBinaryScriptingCommands,
    Closeable {
  public static final int BUFFER_SIZE = 8192;

  protected final AsyncDispatcher processor;

  public AsyncBinaryJedis(String host) throws IOException {
    Connection connection = new Connection(host);
    processor = new AsyncDispatcher(connection, BUFFER_SIZE);
    processor.start();
  }

  public AsyncBinaryJedis(String host, int port) throws IOException {
    Connection connection = new Connection(host, port);
    processor = new AsyncDispatcher(connection, BUFFER_SIZE);
    processor.start();
  }

  public AsyncBinaryJedis(String host, int port, String password) throws IOException {
    Connection connection = new Connection(host, port);
    processor = new AsyncDispatcher(connection, BUFFER_SIZE);
    processor.setPassword(password);
    processor.start();
  }

  @Override
  public void close() throws IOException {
    try {
      stop();
    } catch (InterruptedException e) {
      // pass
    }
  }

  public void stop() throws InterruptedException {
    processor.setShutdown(true);
    processor.join();
  }

  // keys section

  @Override
  public void del(AsyncResponseCallback<Long> callback, byte[] key) {
    byte[] request = RequestBuilder.build(DEL, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void del(AsyncResponseCallback<Long> callback, byte[]... keys) {
    byte[] request = RequestBuilder.build(DEL, keys);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void dump(final AsyncResponseCallback<byte[]> callback, final byte[] key) {
    byte[] request = RequestBuilder.build(DUMP, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void exists(final AsyncResponseCallback<Boolean> callback, final byte[] key) {
    byte[] request = RequestBuilder.build(EXISTS, key);
    processor.registerRequest(new AsyncJedisTask(request, BOOLEAN, callback));
  }

  @Override
  public void expire(AsyncResponseCallback<Long> callback, byte[] key, int seconds) {
    byte[] request = RequestBuilder.build(EXPIRE, key, toByteArray(seconds));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void expireAt(AsyncResponseCallback<Long> callback, byte[] key, long unixTime) {
    byte[] request = RequestBuilder.build(EXPIRE, key, toByteArray(unixTime));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void keys(AsyncResponseCallback<Set<byte[]>> callback, byte[] pattern) {
    byte[] request = RequestBuilder.build(KEYS, pattern);
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_ZSET, callback));
  }

  @Override
  public void migrate(AsyncResponseCallback<String> callback, byte[] host, int port, byte[] key,
      int destinationDb, int timeout) {
    byte[] request = RequestBuilder.build(MIGRATE, host, toByteArray(port), key,
      toByteArray(destinationDb), toByteArray(timeout));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void move(AsyncResponseCallback<Long> callback, byte[] key, int dbIndex) {
    byte[] request = RequestBuilder.build(MOVE, key, toByteArray(dbIndex));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void objectRefcount(AsyncResponseCallback<Long> callback, byte[] key) {
    byte[] request = RequestBuilder.build(OBJECT, Protocol.Keyword.REFCOUNT.raw, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void objectEncoding(AsyncResponseCallback<byte[]> callback, byte[] key) {
    byte[] request = RequestBuilder.build(OBJECT, Protocol.Keyword.ENCODING.raw, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void objectIdletime(AsyncResponseCallback<Long> callback, byte[] key) {
    byte[] request = RequestBuilder.build(OBJECT, Protocol.Keyword.IDLETIME.raw, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void persist(AsyncResponseCallback<Long> callback, byte[] key) {
    byte[] request = RequestBuilder.build(PERSIST, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void pexpire(final AsyncResponseCallback<Long> callback, final byte[] key,
      final long milliseconds) {
    byte[] request = RequestBuilder.build(PEXPIRE, key, toByteArray(milliseconds));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void pexpireAt(final AsyncResponseCallback<Long> callback, final byte[] key,
      final long millisecondsTimestamp) {
    byte[] request = RequestBuilder.build(PEXPIREAT, key, toByteArray(millisecondsTimestamp));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void pttl(final AsyncResponseCallback<Long> callback, final byte[] key) {
    byte[] request = RequestBuilder.build(PTTL, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void randomBinaryKey(AsyncResponseCallback<byte[]> callback) {
    byte[] request = RequestBuilder.build(RANDOMKEY);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void rename(AsyncResponseCallback<String> callback, byte[] oldkey, byte[] newkey) {
    byte[] request = RequestBuilder.build(RENAME, oldkey, newkey);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void renamenx(AsyncResponseCallback<Long> callback, byte[] oldkey, byte[] newkey) {
    byte[] request = RequestBuilder.build(RENAMENX, oldkey, newkey);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void restore(AsyncResponseCallback<String> callback, byte[] key, int ttl,
      byte[] serializedValue) {
    byte[] request = RequestBuilder.build(RESTORE, key, toByteArray(ttl), serializedValue);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void sort(AsyncResponseCallback<List<byte[]>> callback, byte[] key) {
    byte[] request = RequestBuilder.build(SORT, key);
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_LIST, callback));
  }

  @Override
  public void sort(AsyncResponseCallback<List<byte[]>> callback, byte[] key,
      SortingParams sortingParameters) {
    byte[] request = RequestBuilder.build(SORT,
      RequestParameterBuilder.buildSortWithParameter(key, sortingParameters));
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_LIST, callback));
  }

  @Override
  public void sort(AsyncResponseCallback<Long> callback, byte[] key, byte[] dstkey) {
    byte[] request = RequestBuilder.build(SORT, key, Protocol.Keyword.STORE.raw, dstkey);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void sort(AsyncResponseCallback<Long> callback, byte[] key,
      SortingParams sortingParameters, byte[] dstkey) {
    byte[][] params = RequestParameterBuilder.buildSortStoreWithParameter(key, sortingParameters,
      dstkey);
    byte[] request = RequestBuilder.build(SORT, params);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void ttl(AsyncResponseCallback<Long> callback, byte[] key) {
    byte[] request = RequestBuilder.build(TTL, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void type(AsyncResponseCallback<String> callback, byte[] key) {
    byte[] request = RequestBuilder.build(TYPE, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  // string section

  @Override
  public void append(AsyncResponseCallback<Long> callback, byte[] key, byte[] value) {
    byte[] request = RequestBuilder.build(APPEND, key, value);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void bitcount(AsyncResponseCallback<Long> callback, byte[] key) {
    byte[] request = RequestBuilder.build(BITCOUNT, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void bitcount(AsyncResponseCallback<Long> callback, byte[] key, long start, long end) {
    byte[] request = RequestBuilder.build(BITCOUNT, key, toByteArray(start), toByteArray(end));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void bitop(AsyncResponseCallback<Long> callback, BitOP op, byte[] destKey,
      byte[]... srcKeys) {
    byte[] request = RequestBuilder.build(BITOP,
      RequestParameterBuilder.buildBitOpParameter(op, destKey, srcKeys));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void decr(AsyncResponseCallback<Long> callback, byte[] key) {
    byte[] request = RequestBuilder.build(DECR, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void decrBy(AsyncResponseCallback<Long> callback, byte[] key, long integer) {
    byte[] request = RequestBuilder.build(DECRBY, key, toByteArray(integer));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void get(AsyncResponseCallback<byte[]> callback, byte[] key) {
    byte[] request = RequestBuilder.build(GET, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void getbit(AsyncResponseCallback<Boolean> callback, byte[] key, long offset) {
    byte[] request = RequestBuilder.build(GETBIT, key, toByteArray(offset));
    processor.registerRequest(new AsyncJedisTask(request, BOOLEAN, callback));
  }

  @Override
  public void getrange(AsyncResponseCallback<byte[]> callback, byte[] key, long startOffset,
      long endOffset) {
    byte[] request = RequestBuilder.build(GETRANGE, key, toByteArray(startOffset),
      toByteArray(endOffset));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void getSet(AsyncResponseCallback<byte[]> callback, byte[] key, byte[] value) {
    byte[] request = RequestBuilder.build(GETSET, key, value);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void incr(AsyncResponseCallback<Long> callback, byte[] key) {
    byte[] request = RequestBuilder.build(INCR, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void incrBy(AsyncResponseCallback<Long> callback, byte[] key, long integer) {
    byte[] request = RequestBuilder.build(INCRBY, key, toByteArray(integer));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void incrByFloat(AsyncResponseCallback<Double> callback, byte[] key, double increment) {
    byte[] request = RequestBuilder.build(INCRBYFLOAT, key, toByteArray(increment));
    processor.registerRequest(new AsyncJedisTask(request, DOUBLE, callback));
  }

  @Override
  public void mget(AsyncResponseCallback<List<byte[]>> callback, byte[]... keys) {
    byte[] request = RequestBuilder.build(MGET, keys);
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_LIST, callback));
  }

  @Override
  public void mset(AsyncResponseCallback<String> callback, byte[]... keysvalues) {
    byte[] request = RequestBuilder.build(MSET, keysvalues);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void msetnx(AsyncResponseCallback<Long> callback, byte[]... keysvalues) {
    byte[] request = RequestBuilder.build(MSETNX, keysvalues);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void psetex(AsyncResponseCallback<String> callback, byte[] key, int milliseconds,
      byte[] value) {
    byte[] request = RequestBuilder.build(PSETEX, key, toByteArray(milliseconds), value);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void set(AsyncResponseCallback<String> callback, byte[] key, byte[] value) {
    byte[] request = RequestBuilder.build(SET, key, value);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void set(AsyncResponseCallback<String> callback, byte[] key, byte[] value, byte[] nxxx,
      byte[] expx, long time) {
    byte[] request = RequestBuilder.build(Protocol.Command.SET, key, value, nxxx, expx,
      toByteArray(time));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void setbit(AsyncResponseCallback<Boolean> callback, byte[] key, long offset, boolean value) {
    byte[] request = RequestBuilder.build(SETBIT, key, toByteArray(offset), toByteArray(value));
    processor.registerRequest(new AsyncJedisTask(request, BOOLEAN, callback));
  }

  @Override
  public void setbit(AsyncResponseCallback<Boolean> callback, byte[] key, long offset, byte[] value) {
    byte[] request = RequestBuilder.build(SETBIT, key, toByteArray(offset), value);
    processor.registerRequest(new AsyncJedisTask(request, BOOLEAN, callback));
  }

  @Override
  public void setnx(AsyncResponseCallback<Long> callback, byte[] key, byte[] value) {
    byte[] request = RequestBuilder.build(SETNX, key, value);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void setex(AsyncResponseCallback<String> callback, byte[] key, int seconds, byte[] value) {
    byte[] request = RequestBuilder.build(SETEX, key, toByteArray(seconds), value);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void setrange(AsyncResponseCallback<Long> callback, byte[] key, long offset, byte[] value) {
    byte[] request = RequestBuilder.build(SETRANGE, key, toByteArray(offset), value);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void strlen(AsyncResponseCallback<Long> callback, byte[] key) {
    byte[] request = RequestBuilder.build(STRLEN, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void substr(AsyncResponseCallback<byte[]> callback, byte[] key, int start, int end) {
    byte[] request = RequestBuilder.build(SUBSTR, key, toByteArray(start), toByteArray(end));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  // hash section
  @Override
  public void hdel(AsyncResponseCallback<Long> callback, byte[] key, byte[]... field) {
    byte[] request = RequestBuilder.build(HDEL, RequestParameterBuilder.joinParameters(key, field));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void hexists(AsyncResponseCallback<Boolean> callback, byte[] key, byte[] field) {
    byte[] request = RequestBuilder.build(HEXISTS, key, field);
    processor.registerRequest(new AsyncJedisTask(request, BOOLEAN, callback));
  }

  @Override
  public void hget(AsyncResponseCallback<byte[]> callback, byte[] key, byte[] field) {
    byte[] request = RequestBuilder.build(HGET, key, field);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void hgetAll(AsyncResponseCallback<Map<byte[], byte[]>> callback, byte[] key) {
    byte[] request = RequestBuilder.build(HGETALL, key);
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_MAP, callback));
  }

  @Override
  public void hincrBy(AsyncResponseCallback<Long> callback, byte[] key, byte[] field, long value) {
    byte[] request = RequestBuilder.build(HINCRBY, key, field, toByteArray(value));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void hkeys(AsyncResponseCallback<Set<byte[]>> callback, byte[] key) {
    byte[] request = RequestBuilder.build(HKEYS, key);
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_ZSET, callback));
  }

  @Override
  public void hlen(AsyncResponseCallback<Long> callback, byte[] key) {
    byte[] request = RequestBuilder.build(HLEN, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void hmget(AsyncResponseCallback<List<byte[]>> callback, byte[] key, byte[]... fields) {
    byte[] request = RequestBuilder.build(HMGET,
      RequestParameterBuilder.buildMultiArgsParameter(key, fields));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void hmset(AsyncResponseCallback<String> callback, byte[] key, Map<byte[], byte[]> hash) {
    byte[] request = RequestBuilder.build(HMSET,
      RequestParameterBuilder.buildSetMultiAddParameter(key, hash));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void hset(AsyncResponseCallback<Long> callback, byte[] key, byte[] field, byte[] value) {
    byte[] request = RequestBuilder.build(HSET, key, field, value);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void hsetnx(AsyncResponseCallback<Long> callback, byte[] key, byte[] field, byte[] value) {
    byte[] request = RequestBuilder.build(HSETNX, key, field, value);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void hvals(AsyncResponseCallback<List<byte[]>> callback, byte[] key) {
    byte[] request = RequestBuilder.build(HVALS, key);
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_LIST, callback));
  }

  // list section
  @Override
  public void lindex(AsyncResponseCallback<byte[]> callback, byte[] key, long index) {
    byte[] request = RequestBuilder.build(LINDEX, key, toByteArray(index));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void linsert(AsyncResponseCallback<Long> callback, byte[] key, Client.LIST_POSITION where,
      byte[] pivot, byte[] value) {
    byte[] request = RequestBuilder.build(LINSERT, key, where.raw, pivot, value);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void llen(AsyncResponseCallback<Long> callback, byte[] key) {
    byte[] request = RequestBuilder.build(LLEN, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void lpop(AsyncResponseCallback<byte[]> callback, byte[] key) {
    byte[] request = RequestBuilder.build(LPOP, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void lpush(AsyncResponseCallback<Long> callback, byte[] key, byte[]... strings) {
    byte[] request = RequestBuilder.build(LPUSH,
      RequestParameterBuilder.joinParameters(key, strings));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void lpushx(AsyncResponseCallback<Long> callback, byte[] key, byte[]... string) {
    byte[] request = RequestBuilder.build(LPUSHX,
      RequestParameterBuilder.joinParameters(key, string));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void lrange(AsyncResponseCallback<List<byte[]>> callback, byte[] key, long start, long end) {
    byte[] request = RequestBuilder.build(LRANGE, key, toByteArray(start), toByteArray(end));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void lrem(AsyncResponseCallback<Long> callback, byte[] key, long count, byte[] value) {
    byte[] request = RequestBuilder.build(LREM, key, toByteArray(count), value);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void lset(AsyncResponseCallback<String> callback, byte[] key, long index, byte[] value) {
    byte[] request = RequestBuilder.build(LSET, key, toByteArray(index), value);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void ltrim(AsyncResponseCallback<String> callback, byte[] key, long start, long end) {
    byte[] request = RequestBuilder.build(LTRIM, key, toByteArray(start), toByteArray(end));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void rpop(AsyncResponseCallback<byte[]> callback, byte[] key) {
    byte[] request = RequestBuilder.build(RPOP, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void rpoplpush(AsyncResponseCallback<byte[]> callback, byte[] srckey, byte[] dstkey) {
    byte[] request = RequestBuilder.build(RPOPLPUSH, srckey, dstkey);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void rpush(AsyncResponseCallback<Long> callback, byte[] key, byte[]... strings) {
    byte[] request = RequestBuilder.build(RPUSH,
      RequestParameterBuilder.joinParameters(key, strings));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void rpushx(AsyncResponseCallback<Long> callback, byte[] key, byte[]... string) {
    byte[] request = RequestBuilder.build(RPUSHX,
      RequestParameterBuilder.joinParameters(key, string));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  // set section
  @Override
  public void sadd(AsyncResponseCallback<Long> callback, byte[] key, byte[]... members) {
    byte[] request = RequestBuilder.build(SADD,
      RequestParameterBuilder.joinParameters(key, members));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void scard(AsyncResponseCallback<Long> callback, byte[] key) {
    byte[] request = RequestBuilder.build(SCARD, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void sdiff(AsyncResponseCallback<Set<byte[]>> callback, byte[]... keys) {
    byte[] request = RequestBuilder.build(SDIFF, keys);
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_ZSET, callback));
  }

  @Override
  public void sdiffstore(AsyncResponseCallback<Long> callback, byte[] dstkey, byte[]... keys) {
    byte[] request = RequestBuilder.build(SDIFFSTORE,
      RequestParameterBuilder.buildMultiArgsParameter(dstkey, keys));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void sinter(AsyncResponseCallback<Set<byte[]>> callback, byte[]... keys) {
    byte[] request = RequestBuilder.build(SINTER, keys);
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_ZSET, callback));
  }

  @Override
  public void sinterstore(AsyncResponseCallback<Long> callback, byte[] dstkey, byte[]... keys) {
    byte[] request = RequestBuilder.build(SINTERSTORE,
      RequestParameterBuilder.buildMultiArgsParameter(dstkey, keys));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void sismember(AsyncResponseCallback<Boolean> callback, byte[] key, byte[] member) {
    byte[] request = RequestBuilder.build(SISMEMBER, key, member);
    processor.registerRequest(new AsyncJedisTask(request, BOOLEAN, callback));
  }

  @Override
  public void smembers(AsyncResponseCallback<Set<byte[]>> callback, byte[] key) {
    byte[] request = RequestBuilder.build(SMEMBERS, key);
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_ZSET, callback));
  }

  @Override
  public void smove(AsyncResponseCallback<Long> callback, byte[] srckey, byte[] dstkey,
      byte[] member) {
    byte[] request = RequestBuilder.build(SMOVE, srckey, dstkey, member);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void spop(AsyncResponseCallback<byte[]> callback, byte[] key) {
    byte[] request = RequestBuilder.build(SPOP, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void srandmember(AsyncResponseCallback<byte[]> callback, byte[] key) {
    byte[] request = RequestBuilder.build(SRANDMEMBER, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void srem(AsyncResponseCallback<Long> callback, byte[] key, byte[]... members) {
    byte[] request = RequestBuilder.build(SREM,
      RequestParameterBuilder.joinParameters(key, members));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void sunion(AsyncResponseCallback<Set<byte[]>> callback, byte[]... keys) {
    byte[] request = RequestBuilder.build(SUNION, keys);
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_ZSET, callback));
  }

  @Override
  public void sunionstore(AsyncResponseCallback<Long> callback, byte[] dstkey, byte[]... keys) {
    byte[] request = RequestBuilder.build(SUNIONSTORE,
      RequestParameterBuilder.buildMultiArgsParameter(dstkey, keys));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  // sorted set section

  @Override
  public void zadd(AsyncResponseCallback<Long> callback, byte[] key, double score, byte[] member) {
    byte[] request = RequestBuilder.build(ZADD, key, toByteArray(score), member);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void zadd(AsyncResponseCallback<Long> callback, byte[] key,
      Map<byte[], Double> scoreMembers) {
    byte[] request = RequestBuilder.build(ZADD,
      RequestParameterBuilder.buildSortedSetMultiAddParameter(key, scoreMembers));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void zcard(AsyncResponseCallback<Long> callback, byte[] key) {
    byte[] request = RequestBuilder.build(ZCARD, key);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void zcount(AsyncResponseCallback<Long> callback, byte[] key, double min, double max) {
    byte[] request = RequestBuilder.build(ZCOUNT, key, toByteArray(min), toByteArray(max));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void zcount(AsyncResponseCallback<Long> callback, byte[] key, byte[] min, byte[] max) {
    byte[] request = RequestBuilder.build(ZCOUNT, key, min, max);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void zincrby(AsyncResponseCallback<Double> callback, byte[] key, double score,
      byte[] member) {
    byte[] request = RequestBuilder.build(ZINCRBY, key, toByteArray(score), member);
    processor.registerRequest(new AsyncJedisTask(request, DOUBLE, callback));
  }

  @Override
  public void zinterstore(AsyncResponseCallback<Long> callback, byte[] dstkey, byte[]... sets) {
    byte[] request = RequestBuilder.build(ZINTERSTORE,
      RequestParameterBuilder.buildMultiArgsWithArgsLengthParameter(dstkey, sets));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void zinterstore(AsyncResponseCallback<Long> callback, byte[] dstkey, ZParams params,
      byte[]... sets) {
    byte[] request = RequestBuilder.build(ZINTERSTORE,
      RequestParameterBuilder.buildSortedSetStoreWithParameter(dstkey, params, sets));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void zrange(AsyncResponseCallback<Set<byte[]>> callback, byte[] key, long start, long end) {
    byte[] request = RequestBuilder.build(ZRANGE, key, toByteArray(start), toByteArray(end));
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_ZSET, callback));
  }

  @Override
  public void zrangeWithScores(AsyncResponseCallback<Set<Tuple>> callback, byte[] key, long start,
      long end) {
    byte[] request = RequestBuilder.build(ZRANGE, key, toByteArray(start), toByteArray(end),
      Protocol.Keyword.WITHSCORES.raw);
    processor.registerRequest(new AsyncJedisTask(request, TUPLE_ZSET_BINARY, callback));
  }

  @Override
  public void zrangeByScore(AsyncResponseCallback<Set<byte[]>> callback, byte[] key, double min,
      double max) {
    byte byteArrayMin[] = RequestParameterBuilder.convertRangeParameter(min);
    byte byteArrayMax[] = RequestParameterBuilder.convertRangeParameter(max);

    byte[] request = RequestBuilder.build(ZRANGEBYSCORE, key, byteArrayMin, byteArrayMax);
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_ZSET, callback));
  }

  @Override
  public void zrangeByScore(AsyncResponseCallback<Set<byte[]>> callback, byte[] key, byte[] min,
      byte[] max) {
    byte[] request = RequestBuilder.build(ZRANGEBYSCORE, key, min, max);
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_ZSET, callback));
  }

  @Override
  public void zrangeByScore(AsyncResponseCallback<Set<byte[]>> callback, byte[] key, double min,
      double max, int offset, int count) {
    byte byteArrayMin[] = RequestParameterBuilder.convertRangeParameter(min);
    byte byteArrayMax[] = RequestParameterBuilder.convertRangeParameter(max);

    byte[] request = RequestBuilder.build(ZRANGEBYSCORE, key, byteArrayMin, byteArrayMax,
      Protocol.Keyword.LIMIT.raw, toByteArray(offset), toByteArray(count));
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_ZSET, callback));
  }

  @Override
  public void zrangeByScore(AsyncResponseCallback<Set<byte[]>> callback, byte[] key, byte[] min,
      byte[] max, int offset, int count) {
    byte[] request = RequestBuilder.build(ZRANGEBYSCORE, key, min, max, Protocol.Keyword.LIMIT.raw,
      toByteArray(offset), toByteArray(count));
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_ZSET, callback));
  }

  @Override
  public void zrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, byte[] key,
      double min, double max) {
    byte byteArrayMin[] = RequestParameterBuilder.convertRangeParameter(min);
    byte byteArrayMax[] = RequestParameterBuilder.convertRangeParameter(max);

    byte[] request = RequestBuilder.build(ZRANGEBYSCORE, key, byteArrayMin, byteArrayMax,
      Protocol.Keyword.WITHSCORES.raw);
    processor.registerRequest(new AsyncJedisTask(request, TUPLE_ZSET_BINARY, callback));
  }

  @Override
  public void zrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, byte[] key,
      double min, double max, int offset, int count) {
    byte byteArrayMin[] = RequestParameterBuilder.convertRangeParameter(min);
    byte byteArrayMax[] = RequestParameterBuilder.convertRangeParameter(max);

    byte[] request = RequestBuilder.build(ZRANGEBYSCORE, key, byteArrayMin, byteArrayMax,
      Protocol.Keyword.LIMIT.raw, toByteArray(offset), toByteArray(count),
      Protocol.Keyword.WITHSCORES.raw);
    processor.registerRequest(new AsyncJedisTask(request, TUPLE_ZSET_BINARY, callback));
  }

  @Override
  public void zrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, byte[] key,
      byte[] min, byte[] max) {
    byte[] request = RequestBuilder.build(ZRANGEBYSCORE, key, min, max,
      Protocol.Keyword.WITHSCORES.raw);
    processor.registerRequest(new AsyncJedisTask(request, TUPLE_ZSET_BINARY, callback));
  }

  @Override
  public void zrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, byte[] key,
      byte[] min, byte[] max, int offset, int count) {
    byte[] request = RequestBuilder.build(ZRANGEBYSCORE, key, min, max, Protocol.Keyword.LIMIT.raw,
      toByteArray(offset), toByteArray(count), Protocol.Keyword.WITHSCORES.raw);
    processor.registerRequest(new AsyncJedisTask(request, TUPLE_ZSET_BINARY, callback));
  }

  @Override
  public void zrank(AsyncResponseCallback<Long> callback, byte[] key, byte[] member) {
    byte[] request = RequestBuilder.build(ZRANK, key, member);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void zrem(AsyncResponseCallback<Long> callback, byte[] key, byte[]... member) {
    byte[] request = RequestBuilder
        .build(ZREM, RequestParameterBuilder.joinParameters(key, member));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void zremrangeByRank(AsyncResponseCallback<Long> callback, byte[] key, long start, long end) {
    byte[] request = RequestBuilder.build(ZREMRANGEBYRANK, key, toByteArray(start),
      toByteArray(end));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void zremrangeByScore(AsyncResponseCallback<Long> callback, byte[] key, double start,
      double end) {
    byte[] request = RequestBuilder.build(ZREMRANGEBYSCORE, key, toByteArray(start),
      toByteArray(end));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void zremrangeByScore(AsyncResponseCallback<Long> callback, byte[] key, byte[] start,
      byte[] end) {
    byte[] request = RequestBuilder.build(ZREMRANGEBYSCORE, key, start, end);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void zrevrange(AsyncResponseCallback<Set<byte[]>> callback, byte[] key, long start,
      long end) {
    byte[] request = RequestBuilder.build(ZREVRANGE, key, toByteArray(start), toByteArray(end));
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_ZSET, callback));
  }

  @Override
  public void zrevrangeWithScores(AsyncResponseCallback<Set<Tuple>> callback, byte[] key,
      long start, long end) {
    byte[] request = RequestBuilder.build(ZREVRANGE, key, toByteArray(start), toByteArray(end),
      Protocol.Keyword.WITHSCORES.raw);
    processor.registerRequest(new AsyncJedisTask(request, TUPLE_ZSET_BINARY, callback));
  }

  @Override
  public void zrevrangeByScore(AsyncResponseCallback<Set<byte[]>> callback, byte[] key, double max,
      double min) {
    byte byteArrayMin[] = RequestParameterBuilder.convertRangeParameter(min);
    byte byteArrayMax[] = RequestParameterBuilder.convertRangeParameter(max);

    byte[] request = RequestBuilder.build(ZREVRANGEBYSCORE, key, byteArrayMax, byteArrayMin);
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_ZSET, callback));
  }

  @Override
  public void zrevrangeByScore(AsyncResponseCallback<Set<byte[]>> callback, byte[] key, byte[] max,
      byte[] min) {
    byte[] request = RequestBuilder.build(ZREVRANGEBYSCORE, key, max, min);
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_ZSET, callback));
  }

  @Override
  public void zrevrangeByScore(AsyncResponseCallback<Set<byte[]>> callback, byte[] key, double max,
      double min, int offset, int count) {
    byte byteArrayMin[] = RequestParameterBuilder.convertRangeParameter(min);
    byte byteArrayMax[] = RequestParameterBuilder.convertRangeParameter(max);

    byte[] request = RequestBuilder.build(ZREVRANGEBYSCORE, key, byteArrayMax, byteArrayMin,
      Protocol.Keyword.LIMIT.raw, toByteArray(offset), toByteArray(count));
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_ZSET, callback));
  }

  @Override
  public void zrevrangeByScore(AsyncResponseCallback<Set<byte[]>> callback, byte[] key, byte[] max,
      byte[] min, int offset, int count) {
    byte[] request = RequestBuilder.build(ZREVRANGEBYSCORE, key, max, min,
      Protocol.Keyword.LIMIT.raw, toByteArray(offset), toByteArray(count));
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_ZSET, callback));
  }

  @Override
  public void zrevrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, byte[] key,
      double max, double min) {
    byte byteArrayMin[] = RequestParameterBuilder.convertRangeParameter(min);
    byte byteArrayMax[] = RequestParameterBuilder.convertRangeParameter(max);

    byte[] request = RequestBuilder.build(ZREVRANGEBYSCORE, key, byteArrayMax, byteArrayMin,
      Protocol.Keyword.WITHSCORES.raw);
    processor.registerRequest(new AsyncJedisTask(request, TUPLE_ZSET_BINARY, callback));
  }

  @Override
  public void zrevrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, byte[] key,
      double max, double min, int offset, int count) {
    byte byteArrayMin[] = RequestParameterBuilder.convertRangeParameter(min);
    byte byteArrayMax[] = RequestParameterBuilder.convertRangeParameter(max);

    byte[] request = RequestBuilder.build(ZREVRANGEBYSCORE, key, byteArrayMax, byteArrayMin,
      Protocol.Keyword.LIMIT.raw, toByteArray(offset), toByteArray(count),
      Protocol.Keyword.WITHSCORES.raw);
    processor.registerRequest(new AsyncJedisTask(request, TUPLE_ZSET_BINARY, callback));
  }

  @Override
  public void zrevrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, byte[] key,
      byte[] max, byte[] min) {
    byte[] request = RequestBuilder.build(ZREVRANGEBYSCORE, key, max, min,
      Protocol.Keyword.WITHSCORES.raw);
    processor.registerRequest(new AsyncJedisTask(request, TUPLE_ZSET_BINARY, callback));
  }

  @Override
  public void zrevrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, byte[] key,
      byte[] max, byte[] min, int offset, int count) {
    byte[] request = RequestBuilder.build(ZREVRANGEBYSCORE, key, max, min,
      Protocol.Keyword.LIMIT.raw, toByteArray(offset), toByteArray(count),
      Protocol.Keyword.WITHSCORES.raw);
    processor.registerRequest(new AsyncJedisTask(request, TUPLE_ZSET_BINARY, callback));
  }

  @Override
  public void zrevrank(AsyncResponseCallback<Long> callback, byte[] key, byte[] member) {
    byte[] request = RequestBuilder.build(ZREVRANK, key, member);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void zscore(AsyncResponseCallback<Double> callback, byte[] key, byte[] member) {
    byte[] request = RequestBuilder.build(ZSCORE, key, member);
    processor.registerRequest(new AsyncJedisTask(request, DOUBLE, callback));
  }

  @Override
  public void zunionstore(AsyncResponseCallback<Long> callback, byte[] dstkey, byte[]... sets) {
    byte[] request = RequestBuilder.build(ZUNIONSTORE,
      RequestParameterBuilder.buildMultiArgsWithArgsLengthParameter(dstkey, sets));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void zunionstore(AsyncResponseCallback<Long> callback, byte[] dstkey, ZParams params,
      byte[]... sets) {
    byte[] request = RequestBuilder.build(ZUNIONSTORE,
      RequestParameterBuilder.buildSortedSetStoreWithParameter(dstkey, params, sets));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  // script section

  @Override
  public void publish(AsyncResponseCallback<Long> callback, byte[] channel, byte[] message) {
    byte[] request = RequestBuilder.build(PUBLISH, channel, message);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  // connection section

  @Override
  public void echo(AsyncResponseCallback<String> callback, String string) {
    byte[] request = RequestBuilder.build(ECHO, string);
    processor.registerRequest(new AsyncJedisTask(request, STRING, callback));
  }

  @Override
  public void ping(AsyncResponseCallback<String> callback) {
    byte[] request = RequestBuilder.build(PING);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  // server section

  @Override
  public void bgrewriteaof(AsyncResponseCallback<String> callback) {
    byte[] request = RequestBuilder.build(BGREWRITEAOF);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void bgsave(AsyncResponseCallback<String> callback) {
    byte[] request = RequestBuilder.build(BGSAVE);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void configGet(AsyncResponseCallback<List<byte[]>> callback, byte[] pattern) {
    byte[] request = RequestBuilder.build(CONFIG, Protocol.Keyword.GET.raw, pattern);
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY_LIST, callback));
  }

  @Override
  public void configSet(AsyncResponseCallback<String> callback, byte[] parameter, byte[] value) {
    byte[] request = RequestBuilder.build(CONFIG, Protocol.Keyword.SET.raw, parameter, value);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void configResetStat(AsyncResponseCallback<String> callback) {
    byte[] request = RequestBuilder.build(CONFIG, Protocol.Keyword.RESETSTAT.name());
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void dbSize(AsyncResponseCallback<Long> callback) {
    byte[] request = RequestBuilder.build(DBSIZE);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void debug(AsyncResponseCallback<String> callback, DebugParams params) {
    byte[] request = RequestBuilder.build(DEBUG, params.getCommand());
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void flushAll(AsyncResponseCallback<String> callback) {
    byte[] request = RequestBuilder.build(FLUSHALL);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void flushDB(AsyncResponseCallback<String> callback) {
    byte[] request = RequestBuilder.build(FLUSHDB);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void info(AsyncResponseCallback<String> callback) {
    byte[] request = RequestBuilder.build(INFO);
    processor.registerRequest(new AsyncJedisTask(request, STRING, callback));
  }

  @Override
  public void info(AsyncResponseCallback<String> callback, String section) {
    byte[] request = RequestBuilder.build(INFO, section);
    processor.registerRequest(new AsyncJedisTask(request, STRING, callback));
  }

  @Override
  public void lastsave(AsyncResponseCallback<Long> callback) {
    byte[] request = RequestBuilder.build(LASTSAVE);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void save(AsyncResponseCallback<String> callback) {
    byte[] request = RequestBuilder.build(SAVE);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void shutdown(AsyncResponseCallback<String> callback) {
    byte[] request = RequestBuilder.build(SHUTDOWN);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void slaveof(AsyncResponseCallback<String> callback, String host, int port) {
    byte[] request = RequestBuilder.build(SLAVEOF, host, String.valueOf(port));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void slaveofNoOne(AsyncResponseCallback<String> callback) {
    byte[] request = RequestBuilder.build(SLAVEOF, Protocol.Keyword.NO.raw,
      Protocol.Keyword.ONE.raw);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void slowlogGetBinary(AsyncResponseCallback<List<byte[]>> callback) {
    byte[] request = RequestBuilder.build(SLOWLOG, Protocol.Keyword.GET.raw);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void slowlogGetBinary(AsyncResponseCallback<List<byte[]>> callback, long entries) {
    byte[] request = RequestBuilder.build(SLOWLOG, Protocol.Keyword.GET.raw, toByteArray(entries));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void slowlogLen(AsyncResponseCallback<Long> callback) {
    byte[] request = RequestBuilder.build(SLOWLOG, Protocol.Keyword.LEN.raw);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void slowlogReset(AsyncResponseCallback<String> callback) {
    byte[] request = RequestBuilder.build(SLOWLOG, Protocol.Keyword.RESET.raw);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void waitReplicas(AsyncResponseCallback<Long> callback, int replicas, long timeout) {
    byte[] request = RequestBuilder.build(WAIT, toByteArray(replicas), toByteArray(timeout));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void eval(AsyncResponseCallback<Object> callback, byte[] script, byte[] keyCount,
      byte[]... params) {
    byte[] request = RequestBuilder.build(EVAL,
      RequestParameterBuilder.buildEvalParameter(script, keyCount, params));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void eval(AsyncResponseCallback<Object> callback, byte[] script, int keyCount,
      byte[]... params) {
    eval(callback, script, toByteArray(keyCount), params);
  }

  @Override
  public void eval(AsyncResponseCallback<Object> callback, byte[] script, List<byte[]> keys,
      List<byte[]> args) {
    eval(callback, script, keys.size(),
      RequestParameterBuilder.convertEvalBinaryListArgs(keys, args));
  }

  @Override
  public void eval(AsyncResponseCallback<Object> callback, byte[] script) {
    eval(callback, script, 0);
  }

  @Override
  public void evalsha(AsyncResponseCallback<Object> callback, byte[] sha1) {
    evalsha(callback, sha1, 0);
  }

  @Override
  public void evalsha(AsyncResponseCallback<Object> callback, byte[] sha1, List<byte[]> keys,
      List<byte[]> args) {
    evalsha(callback, sha1, keys.size(),
      RequestParameterBuilder.convertEvalBinaryListArgs(keys, args));
  }

  @Override
  public void evalsha(AsyncResponseCallback<Object> callback, byte[] sha1, int keyCount,
      byte[]... params) {
    byte[] request = RequestBuilder.build(EVALSHA,
      RequestParameterBuilder.buildEvalParameter(sha1, toByteArray(keyCount), params));
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void scriptExists(AsyncResponseCallback<List<Boolean>> callback, byte[]... sha1) {
    byte[] request = RequestBuilder.build(SCRIPT,
      RequestParameterBuilder.joinParameters(Protocol.Keyword.EXISTS.raw, sha1));
    processor.registerRequest(new AsyncJedisTask(request, BOOLEAN_LIST, callback));
  }

  @Override
  public void scriptLoad(AsyncResponseCallback<byte[]> callback, byte[] script) {
    byte[] request = RequestBuilder.build(SCRIPT, Protocol.Keyword.LOAD.raw, script);
    processor.registerRequest(new AsyncJedisTask(request, BYTE_ARRAY, callback));
  }

  @Override
  public void scriptFlush(AsyncResponseCallback<String> callback) {
    byte[] request = RequestBuilder.build(SCRIPT, Protocol.Keyword.FLUSH.raw);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }

  @Override
  public void scriptKill(AsyncResponseCallback<String> callback) {
    byte[] request = RequestBuilder.build(SCRIPT, Protocol.Keyword.KILL.raw);
    processor.registerRequest(new AsyncJedisTask(request, callback));
  }
}
