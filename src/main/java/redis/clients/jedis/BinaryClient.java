package redis.clients.jedis;

import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.Protocol.Command.*;
import static redis.clients.jedis.Protocol.Keyword.ENCODING;
import static redis.clients.jedis.Protocol.Keyword.IDLETIME;
import static redis.clients.jedis.Protocol.Keyword.LEN;
import static redis.clients.jedis.Protocol.Keyword.LIMIT;
import static redis.clients.jedis.Protocol.Keyword.NO;
import static redis.clients.jedis.Protocol.Keyword.ONE;
import static redis.clients.jedis.Protocol.Keyword.REFCOUNT;
import static redis.clients.jedis.Protocol.Keyword.RESET;
import static redis.clients.jedis.Protocol.Keyword.STORE;
import static redis.clients.jedis.Protocol.Keyword.WITHSCORES;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.util.SafeEncoder;

public class BinaryClient extends Connection {
  public enum LIST_POSITION {
    BEFORE, AFTER;
    public final byte[] raw;

    private LIST_POSITION() {
      raw = SafeEncoder.encode(name());
    }
  }

  private boolean isInMulti;

  private String password;

  private long db;

  private boolean isInWatch;

  public boolean isInMulti() {
    return isInMulti;
  }

  public boolean isInWatch() {
    return isInWatch;
  }

  public BinaryClient() {
    super();
  }

  public BinaryClient(final String host) {
    super(host);
  }

  public BinaryClient(final String host, final int port) {
    super(host, port);
  }

  private byte[][] joinParameters(byte[] first, byte[][] rest) {
    byte[][] result = new byte[rest.length + 1][];
    result[0] = first;
    for (int i = 0; i < rest.length; i++) {
      result[i + 1] = rest[i];
    }
    return result;
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  public void setDb(long db) {
    this.db = db;
  }

  @Override
  public void connect() {
    if (!isConnected()) {
      super.connect();
      if (password != null) {
        auth(password);
        getStatusCodeReply();
      }
      if (db > 0) {
        select(Long.valueOf(db).intValue());
        getStatusCodeReply();
      }
    }
  }

  public void ping() {
    sendCommand(Command.PING);
  }

  public void set(final byte[] key, final byte[] value) {
    sendCommand(Command.SET, key, value);
  }

  public void set(final byte[] key, final byte[] value, final byte[] nxxx, final byte[] expx,
      final long time) {
    sendCommand(Command.SET, key, value, nxxx, expx, toByteArray(time));
  }

  public void get(final byte[] key) {
    sendCommand(Command.GET, key);
  }

  public void quit() {
    db = 0;
    sendCommand(QUIT);
  }

  public void exists(final byte[] key) {
    sendCommand(EXISTS, key);
  }

  public void del(final byte[]... keys) {
    sendCommand(DEL, keys);
  }

  public void type(final byte[] key) {
    sendCommand(TYPE, key);
  }

  public void flushDB() {
    sendCommand(FLUSHDB);
  }

  public void keys(final byte[] pattern) {
    sendCommand(KEYS, pattern);
  }

  public void randomKey() {
    sendCommand(RANDOMKEY);
  }

  public void rename(final byte[] oldkey, final byte[] newkey) {
    sendCommand(RENAME, oldkey, newkey);
  }

  public void renamenx(final byte[] oldkey, final byte[] newkey) {
    sendCommand(RENAMENX, oldkey, newkey);
  }

  public void dbSize() {
    sendCommand(DBSIZE);
  }

  public void expire(final byte[] key, final int seconds) {
    sendCommand(EXPIRE, key, toByteArray(seconds));
  }

  public void expireAt(final byte[] key, final long unixTime) {
    sendCommand(EXPIREAT, key, toByteArray(unixTime));
  }

  public void ttl(final byte[] key) {
    sendCommand(TTL, key);
  }

  public void select(final int index) {
    sendCommand(SELECT, toByteArray(index));
  }

  public void move(final byte[] key, final int dbIndex) {
    sendCommand(MOVE, key, toByteArray(dbIndex));
  }

  public void flushAll() {
    sendCommand(FLUSHALL);
  }

  public void getSet(final byte[] key, final byte[] value) {
    sendCommand(GETSET, key, value);
  }

  public void mget(final byte[]... keys) {
    sendCommand(MGET, keys);
  }

  public void setnx(final byte[] key, final byte[] value) {
    sendCommand(SETNX, key, value);
  }

  public void setex(final byte[] key, final int seconds, final byte[] value) {
    sendCommand(SETEX, key, toByteArray(seconds), value);
  }

  public void mset(final byte[]... keysvalues) {
    sendCommand(MSET, keysvalues);
  }

  public void msetnx(final byte[]... keysvalues) {
    sendCommand(MSETNX, keysvalues);
  }

  public void decrBy(final byte[] key, final long integer) {
    sendCommand(DECRBY, key, toByteArray(integer));
  }

  public void decr(final byte[] key) {
    sendCommand(DECR, key);
  }

  public void incrBy(final byte[] key, final long integer) {
    sendCommand(INCRBY, key, toByteArray(integer));
  }

  public void incrByFloat(final byte[] key, final double value) {
    sendCommand(INCRBYFLOAT, key, toByteArray(value));
  }

  public void incr(final byte[] key) {
    sendCommand(INCR, key);
  }

  public void append(final byte[] key, final byte[] value) {
    sendCommand(APPEND, key, value);
  }

  public void substr(final byte[] key, final int start, final int end) {
    sendCommand(SUBSTR, key, toByteArray(start), toByteArray(end));
  }

  public void hset(final byte[] key, final byte[] field, final byte[] value) {
    sendCommand(HSET, key, field, value);
  }

  public void hget(final byte[] key, final byte[] field) {
    sendCommand(HGET, key, field);
  }

  public void hsetnx(final byte[] key, final byte[] field, final byte[] value) {
    sendCommand(HSETNX, key, field, value);
  }

  public void hmset(final byte[] key, final Map<byte[], byte[]> hash) {
    final List<byte[]> params = new ArrayList<byte[]>();
    params.add(key);

    for (final Entry<byte[], byte[]> entry : hash.entrySet()) {
      params.add(entry.getKey());
      params.add(entry.getValue());
    }
    sendCommand(HMSET, params.toArray(new byte[params.size()][]));
  }

  public void hmget(final byte[] key, final byte[]... fields) {
    final byte[][] params = new byte[fields.length + 1][];
    params[0] = key;
    System.arraycopy(fields, 0, params, 1, fields.length);
    sendCommand(HMGET, params);
  }

  public void hincrBy(final byte[] key, final byte[] field, final long value) {
    sendCommand(HINCRBY, key, field, toByteArray(value));
  }

  public void hexists(final byte[] key, final byte[] field) {
    sendCommand(HEXISTS, key, field);
  }

  public void hdel(final byte[] key, final byte[]... fields) {
    sendCommand(HDEL, joinParameters(key, fields));
  }

  public void hlen(final byte[] key) {
    sendCommand(HLEN, key);
  }

  public void hkeys(final byte[] key) {
    sendCommand(HKEYS, key);
  }

  public void hvals(final byte[] key) {
    sendCommand(HVALS, key);
  }

  public void hgetAll(final byte[] key) {
    sendCommand(HGETALL, key);
  }

  public void rpush(final byte[] key, final byte[]... strings) {
    sendCommand(RPUSH, joinParameters(key, strings));
  }

  public void lpush(final byte[] key, final byte[]... strings) {
    sendCommand(LPUSH, joinParameters(key, strings));
  }

  public void llen(final byte[] key) {
    sendCommand(LLEN, key);
  }

  public void lrange(final byte[] key, final long start, final long end) {
    sendCommand(LRANGE, key, toByteArray(start), toByteArray(end));
  }

  public void ltrim(final byte[] key, final long start, final long end) {
    sendCommand(LTRIM, key, toByteArray(start), toByteArray(end));
  }

  public void lindex(final byte[] key, final long index) {
    sendCommand(LINDEX, key, toByteArray(index));
  }

  public void lset(final byte[] key, final long index, final byte[] value) {
    sendCommand(LSET, key, toByteArray(index), value);
  }

  public void lrem(final byte[] key, long count, final byte[] value) {
    sendCommand(LREM, key, toByteArray(count), value);
  }

  public void lpop(final byte[] key) {
    sendCommand(LPOP, key);
  }

  public void rpop(final byte[] key) {
    sendCommand(RPOP, key);
  }

  public void rpoplpush(final byte[] srckey, final byte[] dstkey) {
    sendCommand(RPOPLPUSH, srckey, dstkey);
  }

  public void sadd(final byte[] key, final byte[]... members) {
    sendCommand(SADD, joinParameters(key, members));
  }

  public void smembers(final byte[] key) {
    sendCommand(SMEMBERS, key);
  }

  public void srem(final byte[] key, final byte[]... members) {
    sendCommand(SREM, joinParameters(key, members));
  }

  public void spop(final byte[] key) {
    sendCommand(SPOP, key);
  }

  public void spop(final byte[] key, final long count) {
    sendCommand(SPOP, key, toByteArray(count));
  }

  public void smove(final byte[] srckey, final byte[] dstkey, final byte[] member) {
    sendCommand(SMOVE, srckey, dstkey, member);
  }

  public void scard(final byte[] key) {
    sendCommand(SCARD, key);
  }

  public void sismember(final byte[] key, final byte[] member) {
    sendCommand(SISMEMBER, key, member);
  }

  public void sinter(final byte[]... keys) {
    sendCommand(SINTER, keys);
  }

  public void sinterstore(final byte[] dstkey, final byte[]... keys) {
    final byte[][] params = new byte[keys.length + 1][];
    params[0] = dstkey;
    System.arraycopy(keys, 0, params, 1, keys.length);
    sendCommand(SINTERSTORE, params);
  }

  public void sunion(final byte[]... keys) {
    sendCommand(SUNION, keys);
  }

  public void sunionstore(final byte[] dstkey, final byte[]... keys) {
    byte[][] params = new byte[keys.length + 1][];
    params[0] = dstkey;
    System.arraycopy(keys, 0, params, 1, keys.length);
    sendCommand(SUNIONSTORE, params);
  }

  public void sdiff(final byte[]... keys) {
    sendCommand(SDIFF, keys);
  }

  public void sdiffstore(final byte[] dstkey, final byte[]... keys) {
    byte[][] params = new byte[keys.length + 1][];
    params[0] = dstkey;
    System.arraycopy(keys, 0, params, 1, keys.length);
    sendCommand(SDIFFSTORE, params);
  }

  public void srandmember(final byte[] key) {
    sendCommand(SRANDMEMBER, key);
  }

  public void zadd(final byte[] key, final double score, final byte[] member) {
    sendCommand(ZADD, key, toByteArray(score), member);
  }

  public void zaddBinary(final byte[] key, final Map<byte[], Double> scoreMembers) {

    ArrayList<byte[]> args = new ArrayList<byte[]>(scoreMembers.size() * 2 + 1);
    args.add(key);

    for (Map.Entry<byte[], Double> entry : scoreMembers.entrySet()) {
      args.add(toByteArray(entry.getValue()));
      args.add(entry.getKey());
    }

    byte[][] argsArray = new byte[args.size()][];
    args.toArray(argsArray);

    sendCommand(ZADD, argsArray);
  }

  public void zrange(final byte[] key, final long start, final long end) {
    sendCommand(ZRANGE, key, toByteArray(start), toByteArray(end));
  }

  public void zrem(final byte[] key, final byte[]... members) {
    sendCommand(ZREM, joinParameters(key, members));
  }

  public void zincrby(final byte[] key, final double score, final byte[] member) {
    sendCommand(ZINCRBY, key, toByteArray(score), member);
  }

  public void zrank(final byte[] key, final byte[] member) {
    sendCommand(ZRANK, key, member);
  }

  public void zrevrank(final byte[] key, final byte[] member) {
    sendCommand(ZREVRANK, key, member);
  }

  public void zrevrange(final byte[] key, final long start, final long end) {
    sendCommand(ZREVRANGE, key, toByteArray(start), toByteArray(end));
  }

  public void zrangeWithScores(final byte[] key, final long start, final long end) {
    sendCommand(ZRANGE, key, toByteArray(start), toByteArray(end), WITHSCORES.raw);
  }

  public void zrevrangeWithScores(final byte[] key, final long start, final long end) {
    sendCommand(ZREVRANGE, key, toByteArray(start), toByteArray(end), WITHSCORES.raw);
  }

  public void zcard(final byte[] key) {
    sendCommand(ZCARD, key);
  }

  public void zscore(final byte[] key, final byte[] member) {
    sendCommand(ZSCORE, key, member);
  }

  public void multi() {
    sendCommand(MULTI);
    isInMulti = true;
  }

  public void discard() {
    sendCommand(DISCARD);
    isInMulti = false;
    isInWatch = false;
  }

  public void exec() {
    sendCommand(EXEC);
    isInMulti = false;
    isInWatch = false;
  }

  public void watch(final byte[]... keys) {
    sendCommand(WATCH, keys);
    isInWatch = true;
  }

  public void unwatch() {
    sendCommand(UNWATCH);
    isInWatch = false;
  }

  public void sort(final byte[] key) {
    sendCommand(SORT, key);
  }

  public void sort(final byte[] key, final SortingParams sortingParameters) {
    final List<byte[]> args = new ArrayList<byte[]>();
    args.add(key);
    args.addAll(sortingParameters.getParams());
    sendCommand(SORT, args.toArray(new byte[args.size()][]));
  }

  public void blpop(final byte[][] args) {
    sendCommand(BLPOP, args);
  }

  public void blpop(final int timeout, final byte[]... keys) {
    final List<byte[]> args = new ArrayList<byte[]>();
    for (final byte[] arg : keys) {
      args.add(arg);
    }
    args.add(Protocol.toByteArray(timeout));
    blpop(args.toArray(new byte[args.size()][]));
  }

  public void sort(final byte[] key, final SortingParams sortingParameters, final byte[] dstkey) {
    final List<byte[]> args = new ArrayList<byte[]>();
    args.add(key);
    args.addAll(sortingParameters.getParams());
    args.add(STORE.raw);
    args.add(dstkey);
    sendCommand(SORT, args.toArray(new byte[args.size()][]));
  }

  public void sort(final byte[] key, final byte[] dstkey) {
    sendCommand(SORT, key, STORE.raw, dstkey);
  }

  public void brpop(final byte[][] args) {
    sendCommand(BRPOP, args);
  }

  public void brpop(final int timeout, final byte[]... keys) {
    final List<byte[]> args = new ArrayList<byte[]>();
    for (final byte[] arg : keys) {
      args.add(arg);
    }
    args.add(Protocol.toByteArray(timeout));
    brpop(args.toArray(new byte[args.size()][]));
  }

  public void auth(final String password) {
    setPassword(password);
    sendCommand(AUTH, password);
  }

  public void subscribe(final byte[]... channels) {
    sendCommand(SUBSCRIBE, channels);
  }

  public void publish(final byte[] channel, final byte[] message) {
    sendCommand(PUBLISH, channel, message);
  }

  public void unsubscribe() {
    sendCommand(UNSUBSCRIBE);
  }

  public void unsubscribe(final byte[]... channels) {
    sendCommand(UNSUBSCRIBE, channels);
  }

  public void psubscribe(final byte[]... patterns) {
    sendCommand(PSUBSCRIBE, patterns);
  }

  public void punsubscribe() {
    sendCommand(PUNSUBSCRIBE);
  }

  public void punsubscribe(final byte[]... patterns) {
    sendCommand(PUNSUBSCRIBE, patterns);
  }

  public void pubsub(final byte[]... args) {
    sendCommand(PUBSUB, args);
  }

  public void zcount(final byte[] key, final double min, final double max) {

    byte byteArrayMin[] = (min == Double.NEGATIVE_INFINITY) ? "-inf".getBytes() : toByteArray(min);
    byte byteArrayMax[] = (max == Double.POSITIVE_INFINITY) ? "+inf".getBytes() : toByteArray(max);

    sendCommand(ZCOUNT, key, byteArrayMin, byteArrayMax);
  }

  public void zcount(final byte[] key, final byte min[], final byte max[]) {
    sendCommand(ZCOUNT, key, min, max);
  }

  public void zcount(final byte[] key, final String min, final String max) {
    sendCommand(ZCOUNT, key, min.getBytes(), max.getBytes());
  }

  public void zrangeByScore(final byte[] key, final double min, final double max) {

    byte byteArrayMin[] = (min == Double.NEGATIVE_INFINITY) ? "-inf".getBytes() : toByteArray(min);
    byte byteArrayMax[] = (max == Double.POSITIVE_INFINITY) ? "+inf".getBytes() : toByteArray(max);

    sendCommand(ZRANGEBYSCORE, key, byteArrayMin, byteArrayMax);
  }

  public void zrangeByScore(final byte[] key, final byte[] min, final byte[] max) {
    sendCommand(ZRANGEBYSCORE, key, min, max);
  }

  public void zrangeByScore(final byte[] key, final String min, final String max) {
    sendCommand(ZRANGEBYSCORE, key, min.getBytes(), max.getBytes());
  }

  public void zrevrangeByScore(final byte[] key, final double max, final double min) {

    byte byteArrayMin[] = (min == Double.NEGATIVE_INFINITY) ? "-inf".getBytes() : toByteArray(min);
    byte byteArrayMax[] = (max == Double.POSITIVE_INFINITY) ? "+inf".getBytes() : toByteArray(max);

    sendCommand(ZREVRANGEBYSCORE, key, byteArrayMax, byteArrayMin);
  }

  public void zrevrangeByScore(final byte[] key, final byte[] max, final byte[] min) {
    sendCommand(ZREVRANGEBYSCORE, key, max, min);
  }

  public void zrevrangeByScore(final byte[] key, final String max, final String min) {
    sendCommand(ZREVRANGEBYSCORE, key, max.getBytes(), min.getBytes());
  }

  public void zrangeByScore(final byte[] key, final double min, final double max, final int offset,
      int count) {

    byte byteArrayMin[] = (min == Double.NEGATIVE_INFINITY) ? "-inf".getBytes() : toByteArray(min);
    byte byteArrayMax[] = (max == Double.POSITIVE_INFINITY) ? "+inf".getBytes() : toByteArray(max);

    sendCommand(ZRANGEBYSCORE, key, byteArrayMin, byteArrayMax, LIMIT.raw, toByteArray(offset),
      toByteArray(count));
  }

  public void zrangeByScore(final byte[] key, final String min, final String max, final int offset,
      int count) {

    sendCommand(ZRANGEBYSCORE, key, min.getBytes(), max.getBytes(), LIMIT.raw, toByteArray(offset),
      toByteArray(count));
  }

  public void zrevrangeByScore(final byte[] key, final double max, final double min,
      final int offset, int count) {

    byte byteArrayMin[] = (min == Double.NEGATIVE_INFINITY) ? "-inf".getBytes() : toByteArray(min);
    byte byteArrayMax[] = (max == Double.POSITIVE_INFINITY) ? "+inf".getBytes() : toByteArray(max);

    sendCommand(ZREVRANGEBYSCORE, key, byteArrayMax, byteArrayMin, LIMIT.raw, toByteArray(offset),
      toByteArray(count));
  }

  public void zrevrangeByScore(final byte[] key, final String max, final String min,
      final int offset, int count) {

    sendCommand(ZREVRANGEBYSCORE, key, max.getBytes(), min.getBytes(), LIMIT.raw,
      toByteArray(offset), toByteArray(count));
  }

  public void zrangeByScoreWithScores(final byte[] key, final double min, final double max) {

    byte byteArrayMin[] = (min == Double.NEGATIVE_INFINITY) ? "-inf".getBytes() : toByteArray(min);
    byte byteArrayMax[] = (max == Double.POSITIVE_INFINITY) ? "+inf".getBytes() : toByteArray(max);

    sendCommand(ZRANGEBYSCORE, key, byteArrayMin, byteArrayMax, WITHSCORES.raw);
  }

  public void zrangeByScoreWithScores(final byte[] key, final String min, final String max) {

    sendCommand(ZRANGEBYSCORE, key, min.getBytes(), max.getBytes(), WITHSCORES.raw);
  }

  public void zrevrangeByScoreWithScores(final byte[] key, final double max, final double min) {

    byte byteArrayMin[] = (min == Double.NEGATIVE_INFINITY) ? "-inf".getBytes() : toByteArray(min);
    byte byteArrayMax[] = (max == Double.POSITIVE_INFINITY) ? "+inf".getBytes() : toByteArray(max);

    sendCommand(ZREVRANGEBYSCORE, key, byteArrayMax, byteArrayMin, WITHSCORES.raw);
  }

  public void zrevrangeByScoreWithScores(final byte[] key, final String max, final String min) {
    sendCommand(ZREVRANGEBYSCORE, key, max.getBytes(), min.getBytes(), WITHSCORES.raw);
  }

  public void zrangeByScoreWithScores(final byte[] key, final double min, final double max,
      final int offset, final int count) {

    byte byteArrayMin[] = (min == Double.NEGATIVE_INFINITY) ? "-inf".getBytes() : toByteArray(min);
    byte byteArrayMax[] = (max == Double.POSITIVE_INFINITY) ? "+inf".getBytes() : toByteArray(max);

    sendCommand(ZRANGEBYSCORE, key, byteArrayMin, byteArrayMax, LIMIT.raw, toByteArray(offset),
      toByteArray(count), WITHSCORES.raw);
  }

  public void zrangeByScoreWithScores(final byte[] key, final String min, final String max,
      final int offset, final int count) {
    sendCommand(ZRANGEBYSCORE, key, min.getBytes(), max.getBytes(), LIMIT.raw, toByteArray(offset),
      toByteArray(count), WITHSCORES.raw);
  }

  public void zrevrangeByScoreWithScores(final byte[] key, final double max, final double min,
      final int offset, final int count) {

    byte byteArrayMin[] = (min == Double.NEGATIVE_INFINITY) ? "-inf".getBytes() : toByteArray(min);
    byte byteArrayMax[] = (max == Double.POSITIVE_INFINITY) ? "+inf".getBytes() : toByteArray(max);

    sendCommand(ZREVRANGEBYSCORE, key, byteArrayMax, byteArrayMin, LIMIT.raw, toByteArray(offset),
      toByteArray(count), WITHSCORES.raw);
  }

  public void zrevrangeByScoreWithScores(final byte[] key, final String max, final String min,
      final int offset, final int count) {

    sendCommand(ZREVRANGEBYSCORE, key, max.getBytes(), min.getBytes(), LIMIT.raw,
      toByteArray(offset), toByteArray(count), WITHSCORES.raw);
  }

  public void zrangeByScore(final byte[] key, final byte[] min, final byte[] max, final int offset,
      int count) {
    sendCommand(ZRANGEBYSCORE, key, min, max, LIMIT.raw, toByteArray(offset), toByteArray(count));
  }

  public void zrevrangeByScore(final byte[] key, final byte[] max, final byte[] min,
      final int offset, int count) {
    sendCommand(ZREVRANGEBYSCORE, key, max, min, LIMIT.raw, toByteArray(offset), toByteArray(count));
  }

  public void zrangeByScoreWithScores(final byte[] key, final byte[] min, final byte[] max) {
    sendCommand(ZRANGEBYSCORE, key, min, max, WITHSCORES.raw);
  }

  public void zrevrangeByScoreWithScores(final byte[] key, final byte[] max, final byte[] min) {
    sendCommand(ZREVRANGEBYSCORE, key, max, min, WITHSCORES.raw);
  }

  public void zrangeByScoreWithScores(final byte[] key, final byte[] min, final byte[] max,
      final int offset, final int count) {
    sendCommand(ZRANGEBYSCORE, key, min, max, LIMIT.raw, toByteArray(offset), toByteArray(count),
      WITHSCORES.raw);
  }

  public void zrevrangeByScoreWithScores(final byte[] key, final byte[] max, final byte[] min,
      final int offset, final int count) {
    sendCommand(ZREVRANGEBYSCORE, key, max, min, LIMIT.raw, toByteArray(offset),
      toByteArray(count), WITHSCORES.raw);
  }

  public void zremrangeByRank(final byte[] key, final long start, final long end) {
    sendCommand(ZREMRANGEBYRANK, key, toByteArray(start), toByteArray(end));
  }

  public void zremrangeByScore(final byte[] key, final byte[] start, final byte[] end) {
    sendCommand(ZREMRANGEBYSCORE, key, start, end);
  }

  public void zremrangeByScore(final byte[] key, final String start, final String end) {
    sendCommand(ZREMRANGEBYSCORE, key, start.getBytes(), end.getBytes());
  }

  public void zunionstore(final byte[] dstkey, final byte[]... sets) {
    final byte[][] params = new byte[sets.length + 2][];
    params[0] = dstkey;
    params[1] = toByteArray(sets.length);
    System.arraycopy(sets, 0, params, 2, sets.length);
    sendCommand(ZUNIONSTORE, params);
  }

  public void zunionstore(final byte[] dstkey, final ZParams params, final byte[]... sets) {
    final List<byte[]> args = new ArrayList<byte[]>();
    args.add(dstkey);
    args.add(Protocol.toByteArray(sets.length));
    for (final byte[] set : sets) {
      args.add(set);
    }
    args.addAll(params.getParams());
    sendCommand(ZUNIONSTORE, args.toArray(new byte[args.size()][]));
  }

  public void zinterstore(final byte[] dstkey, final byte[]... sets) {
    final byte[][] params = new byte[sets.length + 2][];
    params[0] = dstkey;
    params[1] = Protocol.toByteArray(sets.length);
    System.arraycopy(sets, 0, params, 2, sets.length);
    sendCommand(ZINTERSTORE, params);
  }

  public void zinterstore(final byte[] dstkey, final ZParams params, final byte[]... sets) {
    final List<byte[]> args = new ArrayList<byte[]>();
    args.add(dstkey);
    args.add(Protocol.toByteArray(sets.length));
    for (final byte[] set : sets) {
      args.add(set);
    }
    args.addAll(params.getParams());
    sendCommand(ZINTERSTORE, args.toArray(new byte[args.size()][]));
  }

  public void zlexcount(final byte[] key, final byte[] min, final byte[] max) {
    sendCommand(ZLEXCOUNT, key, min, max);
  }

  public void zrangeByLex(final byte[] key, final byte[] min, final byte[] max) {
    sendCommand(ZRANGEBYLEX, key, min, max);
  }

  public void zrangeByLex(final byte[] key, final byte[] min, final byte[] max, final int offset,
      final int count) {
    sendCommand(ZRANGEBYLEX, key, min, max, LIMIT.raw, toByteArray(offset), toByteArray(count));
  }

  public void zrevrangeByLex(final byte[] key, final byte[] max, final byte[] min) {
    sendCommand(ZREVRANGEBYLEX, key, max, min);
  }

  public void zrevrangeByLex(final byte[] key, final byte[] max, final byte[] min,
      final int offset, final int count) {
    sendCommand(ZREVRANGEBYLEX, key, max, min, LIMIT.raw, toByteArray(offset), toByteArray(count));
  }

  public void zremrangeByLex(byte[] key, byte[] min, byte[] max) {
    sendCommand(ZREMRANGEBYLEX, key, min, max);
  }

  public void save() {
    sendCommand(SAVE);
  }

  public void bgsave() {
    sendCommand(BGSAVE);
  }

  public void bgrewriteaof() {
    sendCommand(BGREWRITEAOF);
  }

  public void lastsave() {
    sendCommand(LASTSAVE);
  }

  public void shutdown() {
    sendCommand(SHUTDOWN);
  }

  public void info() {
    sendCommand(INFO);
  }

  public void info(final String section) {
    sendCommand(INFO, section);
  }

  public void monitor() {
    sendCommand(MONITOR);
  }

  public void slaveof(final String host, final int port) {
    sendCommand(SLAVEOF, host, String.valueOf(port));
  }

  public void slaveofNoOne() {
    sendCommand(SLAVEOF, NO.raw, ONE.raw);
  }

  public void configGet(final byte[] pattern) {
    sendCommand(CONFIG, Keyword.GET.raw, pattern);
  }

  public void configSet(final byte[] parameter, final byte[] value) {
    sendCommand(CONFIG, Keyword.SET.raw, parameter, value);
  }

  public void strlen(final byte[] key) {
    sendCommand(STRLEN, key);
  }

  public void sync() {
    sendCommand(SYNC);
  }

  public void lpushx(final byte[] key, final byte[]... string) {
    sendCommand(LPUSHX, joinParameters(key, string));
  }

  public void persist(final byte[] key) {
    sendCommand(PERSIST, key);
  }

  public void rpushx(final byte[] key, final byte[]... string) {
    sendCommand(RPUSHX, joinParameters(key, string));
  }

  public void echo(final byte[] string) {
    sendCommand(ECHO, string);
  }

  public void linsert(final byte[] key, final LIST_POSITION where, final byte[] pivot,
      final byte[] value) {
    sendCommand(LINSERT, key, where.raw, pivot, value);
  }

  public void debug(final DebugParams params) {
    sendCommand(DEBUG, params.getCommand());
  }

  public void brpoplpush(final byte[] source, final byte[] destination, final int timeout) {
    sendCommand(BRPOPLPUSH, source, destination, toByteArray(timeout));
  }

  public void configResetStat() {
    sendCommand(CONFIG, Keyword.RESETSTAT.name());
  }

  public void setbit(byte[] key, long offset, byte[] value) {
    sendCommand(SETBIT, key, toByteArray(offset), value);
  }

  public void setbit(byte[] key, long offset, boolean value) {
    sendCommand(SETBIT, key, toByteArray(offset), toByteArray(value));
  }

  public void getbit(byte[] key, long offset) {
    sendCommand(GETBIT, key, toByteArray(offset));
  }

  public void bitpos(final byte[] key, final boolean value, final BitPosParams params) {
    final List<byte[]> args = new ArrayList<byte[]>();
    args.add(key);
    args.add(toByteArray(value));
    args.addAll(params.getParams());
    sendCommand(BITPOS, args.toArray(new byte[args.size()][]));
  }

  public void setrange(byte[] key, long offset, byte[] value) {
    sendCommand(SETRANGE, key, toByteArray(offset), value);
  }

  public void getrange(byte[] key, long startOffset, long endOffset) {
    sendCommand(GETRANGE, key, toByteArray(startOffset), toByteArray(endOffset));
  }

  public Long getDB() {
    return db;
  }

  public void disconnect() {
    db = 0;
    super.disconnect();
  }

  @Override
  public void close() {
    db = 0;
    super.close();
  }

  public void resetState() {
    if (isInMulti()) discard();

    if (isInWatch()) unwatch();
  }

  private void sendEvalCommand(Command command, byte[] script, byte[] keyCount, byte[][] params) {

    final byte[][] allArgs = new byte[params.length + 2][];

    allArgs[0] = script;
    allArgs[1] = keyCount;

    for (int i = 0; i < params.length; i++)
      allArgs[i + 2] = params[i];

    sendCommand(command, allArgs);
  }

  public void eval(byte[] script, byte[] keyCount, byte[][] params) {
    sendEvalCommand(EVAL, script, keyCount, params);
  }

  public void eval(byte[] script, int keyCount, byte[]... params) {
    eval(script, toByteArray(keyCount), params);
  }

  public void evalsha(byte[] sha1, byte[] keyCount, byte[]... params) {
    sendEvalCommand(EVALSHA, sha1, keyCount, params);
  }

  public void evalsha(byte[] sha1, int keyCount, byte[]... params) {
    sendEvalCommand(EVALSHA, sha1, toByteArray(keyCount), params);
  }

  public void scriptFlush() {
    sendCommand(SCRIPT, Keyword.FLUSH.raw);
  }

  public void scriptExists(byte[]... sha1) {
    byte[][] args = new byte[sha1.length + 1][];
    args[0] = Keyword.EXISTS.raw;
    for (int i = 0; i < sha1.length; i++)
      args[i + 1] = sha1[i];

    sendCommand(SCRIPT, args);
  }

  public void scriptLoad(byte[] script) {
    sendCommand(SCRIPT, Keyword.LOAD.raw, script);
  }

  public void scriptKill() {
    sendCommand(SCRIPT, Keyword.KILL.raw);
  }

  public void slowlogGet() {
    sendCommand(SLOWLOG, Keyword.GET.raw);
  }

  public void slowlogGet(long entries) {
    sendCommand(SLOWLOG, Keyword.GET.raw, toByteArray(entries));
  }

  public void slowlogReset() {
    sendCommand(SLOWLOG, RESET.raw);
  }

  public void slowlogLen() {
    sendCommand(SLOWLOG, LEN.raw);
  }

  public void objectRefcount(byte[] key) {
    sendCommand(OBJECT, REFCOUNT.raw, key);
  }

  public void objectIdletime(byte[] key) {
    sendCommand(OBJECT, IDLETIME.raw, key);
  }

  public void objectEncoding(byte[] key) {
    sendCommand(OBJECT, ENCODING.raw, key);
  }

  public void bitcount(byte[] key) {
    sendCommand(BITCOUNT, key);
  }

  public void bitcount(byte[] key, long start, long end) {
    sendCommand(BITCOUNT, key, toByteArray(start), toByteArray(end));
  }

  public void bitop(BitOP op, byte[] destKey, byte[]... srcKeys) {
    Keyword kw = Keyword.AND;
    int len = srcKeys.length;
    switch (op) {
    case AND:
      kw = Keyword.AND;
      break;
    case OR:
      kw = Keyword.OR;
      break;
    case XOR:
      kw = Keyword.XOR;
      break;
    case NOT:
      kw = Keyword.NOT;
      len = Math.min(1, len);
      break;
    }

    byte[][] bargs = new byte[len + 2][];
    bargs[0] = kw.raw;
    bargs[1] = destKey;
    for (int i = 0; i < len; ++i) {
      bargs[i + 2] = srcKeys[i];
    }

    sendCommand(BITOP, bargs);
  }

  public void sentinel(final byte[]... args) {
    sendCommand(SENTINEL, args);
  }

  public void dump(final byte[] key) {
    sendCommand(DUMP, key);
  }

  public void restore(final byte[] key, final int ttl, final byte[] serializedValue) {
    sendCommand(RESTORE, key, toByteArray(ttl), serializedValue);
  }

  @Deprecated
  public void pexpire(final byte[] key, final int milliseconds) {
    pexpire(key, (long) milliseconds);
  }

  public void pexpire(final byte[] key, final long milliseconds) {
    sendCommand(PEXPIRE, key, toByteArray(milliseconds));
  }

  public void pexpireAt(final byte[] key, final long millisecondsTimestamp) {
    sendCommand(PEXPIREAT, key, toByteArray(millisecondsTimestamp));
  }

  public void pttl(final byte[] key) {
    sendCommand(PTTL, key);
  }

  @Deprecated
  public void psetex(final byte[] key, final int milliseconds, final byte[] value) {
    psetex(key, (long) milliseconds, value);
  }

  public void psetex(final byte[] key, final long milliseconds, final byte[] value) {
    sendCommand(PSETEX, key, toByteArray(milliseconds), value);
  }

  public void set(final byte[] key, final byte[] value, final byte[] nxxx) {
    sendCommand(Command.SET, key, value, nxxx);
  }

  public void set(final byte[] key, final byte[] value, final byte[] nxxx, final byte[] expx,
      final int time) {
    sendCommand(Command.SET, key, value, nxxx, expx, toByteArray(time));
  }

  public void srandmember(final byte[] key, final int count) {
    sendCommand(SRANDMEMBER, key, toByteArray(count));
  }

  public void clientKill(final byte[] client) {
    sendCommand(CLIENT, Keyword.KILL.raw, client);
  }

  public void clientGetname() {
    sendCommand(CLIENT, Keyword.GETNAME.raw);
  }

  public void clientList() {
    sendCommand(CLIENT, Keyword.LIST.raw);
  }

  public void clientSetname(final byte[] name) {
    sendCommand(CLIENT, Keyword.SETNAME.raw, name);
  }

  public void time() {
    sendCommand(TIME);
  }

  public void migrate(final byte[] host, final int port, final byte[] key, final int destinationDb,
      final int timeout) {
    sendCommand(MIGRATE, host, toByteArray(port), key, toByteArray(destinationDb),
      toByteArray(timeout));
  }

  public void hincrByFloat(final byte[] key, final byte[] field, double increment) {
    sendCommand(HINCRBYFLOAT, key, field, toByteArray(increment));
  }

  @Deprecated
  /**
   * This method is deprecated due to bug (scan cursor should be unsigned long)
   * And will be removed on next major release
   * @see https://github.com/xetorthio/jedis/issues/531
   */
  public void scan(int cursor, final ScanParams params) {
    final List<byte[]> args = new ArrayList<byte[]>();
    args.add(toByteArray(cursor));
    args.addAll(params.getParams());
    sendCommand(SCAN, args.toArray(new byte[args.size()][]));
  }

  @Deprecated
  /**
   * This method is deprecated due to bug (scan cursor should be unsigned long)
   * And will be removed on next major release
   * @see https://github.com/xetorthio/jedis/issues/531 
   */
  public void hscan(final byte[] key, int cursor, final ScanParams params) {
    final List<byte[]> args = new ArrayList<byte[]>();
    args.add(key);
    args.add(toByteArray(cursor));
    args.addAll(params.getParams());
    sendCommand(HSCAN, args.toArray(new byte[args.size()][]));
  }

  @Deprecated
  /**
   * This method is deprecated due to bug (scan cursor should be unsigned long)
   * And will be removed on next major release
   * @see https://github.com/xetorthio/jedis/issues/531 
   */
  public void sscan(final byte[] key, int cursor, final ScanParams params) {
    final List<byte[]> args = new ArrayList<byte[]>();
    args.add(key);
    args.add(toByteArray(cursor));
    args.addAll(params.getParams());
    sendCommand(SSCAN, args.toArray(new byte[args.size()][]));
  }

  @Deprecated
  /**
   * This method is deprecated due to bug (scan cursor should be unsigned long)
   * And will be removed on next major release
   * @see https://github.com/xetorthio/jedis/issues/531 
   */
  public void zscan(final byte[] key, int cursor, final ScanParams params) {
    final List<byte[]> args = new ArrayList<byte[]>();
    args.add(key);
    args.add(toByteArray(cursor));
    args.addAll(params.getParams());
    sendCommand(ZSCAN, args.toArray(new byte[args.size()][]));
  }

  public void scan(final byte[] cursor, final ScanParams params) {
    final List<byte[]> args = new ArrayList<byte[]>();
    args.add(cursor);
    args.addAll(params.getParams());
    sendCommand(SCAN, args.toArray(new byte[args.size()][]));
  }

  public void hscan(final byte[] key, final byte[] cursor, final ScanParams params) {
    final List<byte[]> args = new ArrayList<byte[]>();
    args.add(key);
    args.add(cursor);
    args.addAll(params.getParams());
    sendCommand(HSCAN, args.toArray(new byte[args.size()][]));
  }

  public void sscan(final byte[] key, final byte[] cursor, final ScanParams params) {
    final List<byte[]> args = new ArrayList<byte[]>();
    args.add(key);
    args.add(cursor);
    args.addAll(params.getParams());
    sendCommand(SSCAN, args.toArray(new byte[args.size()][]));
  }

  public void zscan(final byte[] key, final byte[] cursor, final ScanParams params) {
    final List<byte[]> args = new ArrayList<byte[]>();
    args.add(key);
    args.add(cursor);
    args.addAll(params.getParams());
    sendCommand(ZSCAN, args.toArray(new byte[args.size()][]));
  }

  public void waitReplicas(int replicas, long timeout) {
    sendCommand(WAIT, toByteArray(replicas), toByteArray(timeout));
  }

  public void cluster(final byte[]... args) {
    sendCommand(CLUSTER, args);
  }

  public void asking() {
    sendCommand(Command.ASKING);
  }

  public void pfadd(final byte[] key, final byte[]... elements) {
    sendCommand(PFADD, joinParameters(key, elements));
  }

  public void pfcount(final byte[] key) {
    sendCommand(PFCOUNT, key);
  }

  public void pfcount(final byte[]... keys) {
    sendCommand(PFCOUNT, keys);
  }

  public void pfmerge(final byte[] destkey, final byte[]... sourcekeys) {
    sendCommand(PFMERGE, joinParameters(destkey, sourcekeys));
  }
}
