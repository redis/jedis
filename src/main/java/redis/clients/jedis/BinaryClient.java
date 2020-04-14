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
import static redis.clients.jedis.Protocol.Keyword.FREQ;
import static redis.clients.jedis.Protocol.Keyword.HELP;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.params.ClientKillParams;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.MigrateParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;
import redis.clients.jedis.util.SafeEncoder;

public class BinaryClient extends Connection {

  private boolean isInMulti;

  private String user;
  private String password;

  private int db;

  private boolean isInWatch;

  public BinaryClient() {
    super();
  }

  public BinaryClient(final String host) {
    super(host);
  }

  public BinaryClient(final String host, final int port) {
    super(host, port);
  }

  public BinaryClient(final String host, final int port, final boolean ssl) {
    super(host, port, ssl);
  }

  public BinaryClient(final String host, final int port, final boolean ssl,
      final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
      final HostnameVerifier hostnameVerifier) {
    super(host, port, ssl, sslSocketFactory, sslParameters, hostnameVerifier);
  }

  public BinaryClient(final JedisSocketFactory jedisSocketFactory) {
    super(jedisSocketFactory);
  }

  public boolean isInMulti() {
    return isInMulti;
  }

  public boolean isInWatch() {
    return isInWatch;
  }

  private byte[][] joinParameters(byte[] first, byte[][] rest) {
    byte[][] result = new byte[rest.length + 1][];
    result[0] = first;
    System.arraycopy(rest, 0, result, 1, rest.length);
    return result;
  }

  private byte[][] joinParameters(byte[] first, byte[] second, byte[][] rest) {
    byte[][] result = new byte[rest.length + 2][];
    result[0] = first;
    result[1] = second;
    System.arraycopy(rest, 0, result, 2, rest.length);
    return result;
  }

  public void setUser(final String user) { this.user = user; }

  public void setPassword(final String password) {
    this.password = password;
  }

  public void setDb(int db) {
    this.db = db;
  }

  @Override
  public void connect() {
    if (!isConnected()) {
      super.connect();
      if (user != null) {
        auth(user, password);
        getStatusCodeReply();
      } else if (password != null) {
        auth(password);
        getStatusCodeReply();
      }
      if (db > 0) {
        select(db);
        getStatusCodeReply();
      }
    }
  }

  public void ping() {
    sendCommand(PING);
  }

  public void ping(final byte[] message) {
    sendCommand(PING, message);
  }

  public void set(final byte[] key, final byte[] value) {
    sendCommand(SET, key, value);
  }

  public void set(final byte[] key, final byte[] value, final SetParams params) {
    sendCommand(SET, params.getByteParams(key, value));
  }

  public void get(final byte[] key) {
    sendCommand(GET, key);
  }

  public void quit() {
    db = 0;
    sendCommand(QUIT);
  }

  public void exists(final byte[]... keys) {
    sendCommand(EXISTS, keys);
  }

  public void del(final byte[]... keys) {
    sendCommand(DEL, keys);
  }

  public void unlink(final byte[]... keys) {
    sendCommand(UNLINK, keys);
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

  public void touch(final byte[]... keys) {
    sendCommand(TOUCH, keys);
  }

  public void select(final int index) {
    sendCommand(SELECT, toByteArray(index));
  }

  public void swapDB(final int index1, final int index2) {
    sendCommand(SWAPDB, toByteArray(index1), toByteArray(index2));
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

  public void decrBy(final byte[] key, final long decrement) {
    sendCommand(DECRBY, key, toByteArray(decrement));
  }

  public void decr(final byte[] key) {
    sendCommand(DECR, key);
  }

  public void incrBy(final byte[] key, final long increment) {
    sendCommand(INCRBY, key, toByteArray(increment));
  }

  public void incrByFloat(final byte[] key, final double increment) {
    sendCommand(INCRBYFLOAT, key, toByteArray(increment));
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

  public void hset(final byte[] key, final Map<byte[], byte[]> hash) {
    final byte[][] params = new byte[1 + hash.size() * 2][];

    int index = 0;
    params[index++] = key;
    for (final Entry<byte[], byte[]> entry : hash.entrySet()) {
      params[index++] = entry.getKey();
      params[index++] = entry.getValue();
    }
    sendCommand(HSET, params);
  }

  public void hget(final byte[] key, final byte[] field) {
    sendCommand(HGET, key, field);
  }

  public void hsetnx(final byte[] key, final byte[] field, final byte[] value) {
    sendCommand(HSETNX, key, field, value);
  }

  public void hmset(final byte[] key, final Map<byte[], byte[]> hash) {
    final List<byte[]> params = new ArrayList<>();
    params.add(key);

    for (final Entry<byte[], byte[]> entry : hash.entrySet()) {
      params.add(entry.getKey());
      params.add(entry.getValue());
    }
    sendCommand(HMSET, params.toArray(new byte[params.size()][]));
  }

  public void hmget(final byte[] key, final byte[]... fields) {
    sendCommand(HMGET, joinParameters(key, fields));
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

  public void lrange(final byte[] key, final long start, final long stop) {
    sendCommand(LRANGE, key, toByteArray(start), toByteArray(stop));
  }

  public void ltrim(final byte[] key, final long start, final long stop) {
    sendCommand(LTRIM, key, toByteArray(start), toByteArray(stop));
  }

  public void lindex(final byte[] key, final long index) {
    sendCommand(LINDEX, key, toByteArray(index));
  }

  public void lset(final byte[] key, final long index, final byte[] value) {
    sendCommand(LSET, key, toByteArray(index), value);
  }

  public void lrem(final byte[] key, final long count, final byte[] value) {
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
    sendCommand(SINTERSTORE, joinParameters(dstkey, keys));
  }

  public void sunion(final byte[]... keys) {
    sendCommand(SUNION, keys);
  }

  public void sunionstore(final byte[] dstkey, final byte[]... keys) {
    sendCommand(SUNIONSTORE, joinParameters(dstkey, keys));
  }

  public void sdiff(final byte[]... keys) {
    sendCommand(SDIFF, keys);
  }

  public void sdiffstore(final byte[] dstkey, final byte[]... keys) {
    sendCommand(SDIFFSTORE, joinParameters(dstkey, keys));
  }

  public void srandmember(final byte[] key) {
    sendCommand(SRANDMEMBER, key);
  }

  public void zadd(final byte[] key, final double score, final byte[] member) {
    sendCommand(ZADD, key, toByteArray(score), member);
  }

  public void zadd(final byte[] key, final double score, final byte[] member,
      final ZAddParams params) {
    sendCommand(ZADD, params.getByteParams(key, toByteArray(score), member));
  }

  public void zadd(final byte[] key, final Map<byte[], Double> scoreMembers) {
    ArrayList<byte[]> args = new ArrayList<>(scoreMembers.size() * 2 + 1);
    args.add(key);
    args.addAll(convertScoreMembersToByteArrays(scoreMembers));

    byte[][] argsArray = new byte[args.size()][];
    args.toArray(argsArray);

    sendCommand(ZADD, argsArray);
  }

  public void zadd(final byte[] key, final Map<byte[], Double> scoreMembers, final ZAddParams params) {
    ArrayList<byte[]> args = convertScoreMembersToByteArrays(scoreMembers);
    byte[][] argsArray = new byte[args.size()][];
    args.toArray(argsArray);

    sendCommand(ZADD, params.getByteParams(key, argsArray));
  }

  public void zrange(final byte[] key, final long start, final long stop) {
    sendCommand(ZRANGE, key, toByteArray(start), toByteArray(stop));
  }

  public void zrem(final byte[] key, final byte[]... members) {
    sendCommand(ZREM, joinParameters(key, members));
  }

  public void zincrby(final byte[] key, final double increment, final byte[] member) {
    sendCommand(ZINCRBY, key, toByteArray(increment), member);
  }

  public void zincrby(final byte[] key, final double increment, final byte[] member,
      final ZIncrByParams params) {
    // Note that it actually calls ZADD with INCR option, so it requires Redis 3.0.2 or upper.
    sendCommand(ZADD, params.getByteParams(key, toByteArray(increment), member));
  }

  public void zrank(final byte[] key, final byte[] member) {
    sendCommand(ZRANK, key, member);
  }

  public void zrevrank(final byte[] key, final byte[] member) {
    sendCommand(ZREVRANK, key, member);
  }

  public void zrevrange(final byte[] key, final long start, final long stop) {
    sendCommand(ZREVRANGE, key, toByteArray(start), toByteArray(stop));
  }

  public void zrangeWithScores(final byte[] key, final long start, final long stop) {
    sendCommand(ZRANGE, key, toByteArray(start), toByteArray(stop), WITHSCORES.raw);
  }

  public void zrevrangeWithScores(final byte[] key, final long start, final long stop) {
    sendCommand(ZREVRANGE, key, toByteArray(start), toByteArray(stop), WITHSCORES.raw);
  }

  public void zcard(final byte[] key) {
    sendCommand(ZCARD, key);
  }

  public void zscore(final byte[] key, final byte[] member) {
    sendCommand(ZSCORE, key, member);
  }

  public void zpopmax(final byte[] key) {
    sendCommand(ZPOPMAX, key);
  }

  public void zpopmax(final byte[] key, final int count) {
    sendCommand(ZPOPMAX, key, toByteArray(count));
  }

  public void zpopmin(final byte[] key) {
    sendCommand(ZPOPMIN, key);
  }

  public void zpopmin(final byte[] key, final long count) {
    sendCommand(ZPOPMIN, key, toByteArray(count));
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
    final List<byte[]> args = new ArrayList<>();
    args.add(key);
    args.addAll(sortingParameters.getParams());
    sendCommand(SORT, args.toArray(new byte[args.size()][]));
  }

  public void blpop(final byte[][] args) {
    sendCommand(BLPOP, args);
  }

  public void blpop(final int timeout, final byte[]... keys) {
    final List<byte[]> args = new ArrayList<>();
    for (final byte[] arg : keys) {
      args.add(arg);
    }
    args.add(Protocol.toByteArray(timeout));
    blpop(args.toArray(new byte[args.size()][]));
  }

  public void sort(final byte[] key, final SortingParams sortingParameters, final byte[] dstkey) {
    final List<byte[]> args = new ArrayList<>();
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
    final List<byte[]> args = new ArrayList<>();
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

  public void auth(final String user, final String password) {
    setUser(user);
    setPassword(password);
    sendCommand(AUTH, user, password);
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
    sendCommand(ZCOUNT, key, toByteArray(min), toByteArray(max));
  }

  public void zcount(final byte[] key, final byte[] min, final byte[] max) {
    sendCommand(ZCOUNT, key, min, max);
  }

  public void zrangeByScore(final byte[] key, final double min, final double max) {
    sendCommand(ZRANGEBYSCORE, key, toByteArray(min), toByteArray(max));
  }

  public void zrangeByScore(final byte[] key, final byte[] min, final byte[] max) {
    sendCommand(ZRANGEBYSCORE, key, min, max);
  }

  public void zrevrangeByScore(final byte[] key, final double max, final double min) {
    sendCommand(ZREVRANGEBYSCORE, key, toByteArray(max), toByteArray(min));
  }

  public void zrevrangeByScore(final byte[] key, final byte[] max, final byte[] min) {
    sendCommand(ZREVRANGEBYSCORE, key, max, min);
  }

  public void zrangeByScore(final byte[] key, final double min, final double max, final int offset,
      final int count) {
    sendCommand(ZRANGEBYSCORE, key, toByteArray(min), toByteArray(max), LIMIT.raw, toByteArray(offset),
      toByteArray(count));
  }

  public void zrevrangeByScore(final byte[] key, final double max, final double min,
      final int offset, final int count) {
    sendCommand(ZREVRANGEBYSCORE, key, toByteArray(max), toByteArray(min), LIMIT.raw, toByteArray(offset),
      toByteArray(count));
  }

  public void zrangeByScoreWithScores(final byte[] key, final double min, final double max) {
    sendCommand(ZRANGEBYSCORE, key, toByteArray(min), toByteArray(max), WITHSCORES.raw);
  }

  public void zrevrangeByScoreWithScores(final byte[] key, final double max, final double min) {
    sendCommand(ZREVRANGEBYSCORE, key, toByteArray(max), toByteArray(min), WITHSCORES.raw);
  }

  public void zrangeByScoreWithScores(final byte[] key, final double min, final double max,
      final int offset, final int count) {
    sendCommand(ZRANGEBYSCORE, key, toByteArray(min), toByteArray(max), LIMIT.raw, toByteArray(offset),
      toByteArray(count), WITHSCORES.raw);
  }

  public void zrevrangeByScoreWithScores(final byte[] key, final double max, final double min,
      final int offset, final int count) {
    sendCommand(ZREVRANGEBYSCORE, key, toByteArray(max), toByteArray(min), LIMIT.raw, toByteArray(offset),
      toByteArray(count), WITHSCORES.raw);
  }

  public void zrangeByScore(final byte[] key, final byte[] min, final byte[] max, final int offset,
      final int count) {
    sendCommand(ZRANGEBYSCORE, key, min, max, LIMIT.raw, toByteArray(offset), toByteArray(count));
  }

  public void zrevrangeByScore(final byte[] key, final byte[] max, final byte[] min,
      final int offset, final int count) {
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

  public void zremrangeByRank(final byte[] key, final long start, final long stop) {
    sendCommand(ZREMRANGEBYRANK, key, toByteArray(start), toByteArray(stop));
  }

  public void zremrangeByScore(final byte[] key, final double min, final double max) {
    sendCommand(ZREMRANGEBYSCORE, key, toByteArray(min), toByteArray(max));
  }

  public void zremrangeByScore(final byte[] key, final byte[] min, final byte[] max) {
    sendCommand(ZREMRANGEBYSCORE, key, min, max);
  }

  public void zunionstore(final byte[] dstkey, final byte[]... sets) {
    sendCommand(ZUNIONSTORE, joinParameters(dstkey, toByteArray(sets.length), sets));
  }

  public void zunionstore(final byte[] dstkey, final ZParams params, final byte[]... sets) {
    final List<byte[]> args = new ArrayList<>();
    args.add(dstkey);
    args.add(Protocol.toByteArray(sets.length));
    for (final byte[] set : sets) {
      args.add(set);
    }
    args.addAll(params.getParams());
    sendCommand(ZUNIONSTORE, args.toArray(new byte[args.size()][]));
  }

  public void zinterstore(final byte[] dstkey, final byte[]... sets) {
    sendCommand(ZINTERSTORE, joinParameters(dstkey, Protocol.toByteArray(sets.length), sets));
  }

  public void zinterstore(final byte[] dstkey, final ZParams params, final byte[]... sets) {
    final List<byte[]> args = new ArrayList<>();
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

  public void zremrangeByLex(final byte[] key, final byte[] min, final byte[] max) {
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

  public void linsert(final byte[] key, final ListPosition where, final byte[] pivot,
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
    sendCommand(CONFIG, Keyword.RESETSTAT.raw);
  }

  public void configRewrite() {
    sendCommand(CONFIG, Keyword.REWRITE.raw);
  }

  public void setbit(final byte[] key, final long offset, final byte[] value) {
    sendCommand(SETBIT, key, toByteArray(offset), value);
  }

  public void setbit(final byte[] key, final long offset, final boolean value) {
    sendCommand(SETBIT, key, toByteArray(offset), toByteArray(value));
  }

  public void getbit(final byte[] key, final long offset) {
    sendCommand(GETBIT, key, toByteArray(offset));
  }

  public void bitpos(final byte[] key, final boolean value, final BitPosParams params) {
    final List<byte[]> args = new ArrayList<>();
    args.add(key);
    args.add(toByteArray(value));
    args.addAll(params.getParams());
    sendCommand(BITPOS, args.toArray(new byte[args.size()][]));
  }

  public void setrange(final byte[] key, final long offset, final byte[] value) {
    sendCommand(SETRANGE, key, toByteArray(offset), value);
  }

  public void getrange(final byte[] key, final long startOffset, final long endOffset) {
    sendCommand(GETRANGE, key, toByteArray(startOffset), toByteArray(endOffset));
  }

  public int getDB() {
    return db;
  }

  @Override
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
    if (isInWatch()) {
      unwatch();
      getStatusCodeReply();
    }
  }

  public void eval(final byte[] script, final byte[] keyCount, final byte[][] params) {
    sendCommand(EVAL, joinParameters(script, keyCount, params));
  }

  public void eval(final byte[] script, final int keyCount, final byte[]... params) {
    sendCommand(EVAL, joinParameters(script, toByteArray(keyCount), params));
  }

  public void evalsha(final byte[] sha1, final byte[] keyCount, final byte[]... params) {
    sendCommand(EVALSHA, joinParameters(sha1, keyCount, params));
  }

  public void evalsha(final byte[] sha1, final int keyCount, final byte[]... params) {
    sendCommand(EVALSHA, joinParameters(sha1, toByteArray(keyCount), params));
  }

  public void scriptFlush() {
    sendCommand(SCRIPT, Keyword.FLUSH.raw);
  }

  public void scriptExists(final byte[]... sha1) {
    sendCommand(SCRIPT, joinParameters(Keyword.EXISTS.raw, sha1));
  }

  public void scriptLoad(final byte[] script) {
    sendCommand(SCRIPT, Keyword.LOAD.raw, script);
  }

  public void scriptKill() {
    sendCommand(SCRIPT, Keyword.KILL.raw);
  }

  public void slowlogGet() {
    sendCommand(SLOWLOG, Keyword.GET.raw);
  }

  public void slowlogGet(final long entries) {
    sendCommand(SLOWLOG, Keyword.GET.raw, toByteArray(entries));
  }

  public void slowlogReset() {
    sendCommand(SLOWLOG, RESET.raw);
  }

  public void slowlogLen() {
    sendCommand(SLOWLOG, LEN.raw);
  }

  public void objectRefcount(final byte[] key) {
    sendCommand(OBJECT, REFCOUNT.raw, key);
  }

  public void objectIdletime(final byte[] key) {
    sendCommand(OBJECT, IDLETIME.raw, key);
  }

  public void objectEncoding(final byte[] key) {
    sendCommand(OBJECT, ENCODING.raw, key);
  }

  public void objectHelp() {
    sendCommand(OBJECT, HELP.raw);
  }

  public void objectFreq(final byte[] key) {
    sendCommand(OBJECT, FREQ.raw, key);
  }

  public void bitcount(final byte[] key) {
    sendCommand(BITCOUNT, key);
  }

  public void bitcount(final byte[] key, final long start, final long end) {
    sendCommand(BITCOUNT, key, toByteArray(start), toByteArray(end));
  }

  public void bitop(final BitOP op, final byte[] destKey, final byte[]... srcKeys) {
    sendCommand(BITOP, joinParameters(op.raw, destKey, srcKeys));
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

  public void restoreReplace(final byte[] key, final int ttl, final byte[] serializedValue) {
    sendCommand(RESTORE, key, toByteArray(ttl), serializedValue, Keyword.REPLACE.raw);
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

  public void psetex(final byte[] key, final long milliseconds, final byte[] value) {
    sendCommand(PSETEX, key, toByteArray(milliseconds), value);
  }

  public void srandmember(final byte[] key, final int count) {
    sendCommand(SRANDMEMBER, key, toByteArray(count));
  }

  public void memoryDoctor() {
    sendCommand(MEMORY, Keyword.DOCTOR.raw);
  }

  public void clientKill(final byte[] ipPort) {
    sendCommand(CLIENT, Keyword.KILL.raw, ipPort);
  }

  public void clientKill(final String ip, final int port) {
    sendCommand(CLIENT, Keyword.KILL.name(), ip + ':' + port);
  }

  public void clientKill(ClientKillParams params) {
    sendCommand(CLIENT, joinParameters(Keyword.KILL.raw, params.getByteParams()));
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

  public void clientPause(final long timeout) {
    sendCommand(CLIENT, Keyword.PAUSE.raw, toByteArray(timeout));
  }

  public void time() {
    sendCommand(TIME);
  }

  public void migrate(final String host, final int port, final byte[] key, final int destinationDb,
      final int timeout) {
    sendCommand(MIGRATE, SafeEncoder.encode(host), toByteArray(port), key,
        toByteArray(destinationDb), toByteArray(timeout));
  }

  public void migrate(final String host, final int port, final int destinationDB,
      final int timeout, final MigrateParams params, final byte[]... keys) {
    byte[][] bparams = params.getByteParams();
    int len = 5 + bparams.length + 1 + keys.length;
    byte[][] args = new byte[len][];
    int i = 0;
    args[i++] = SafeEncoder.encode(host);
    args[i++] = toByteArray(port);
    args[i++] = new byte[0];
    args[i++] = toByteArray(destinationDB);
    args[i++] = toByteArray(timeout);
    System.arraycopy(bparams, 0, args, i, bparams.length);
    i += bparams.length;
    args[i++] = Keyword.KEYS.raw;
    System.arraycopy(keys, 0, args, i, keys.length);
    sendCommand(MIGRATE, args);
  }

  public void hincrByFloat(final byte[] key, final byte[] field, final double increment) {
    sendCommand(HINCRBYFLOAT, key, field, toByteArray(increment));
  }

  public void scan(final byte[] cursor, final ScanParams params) {
    final List<byte[]> args = new ArrayList<>();
    args.add(cursor);
    args.addAll(params.getParams());
    sendCommand(SCAN, args.toArray(new byte[args.size()][]));
  }

  public void hscan(final byte[] key, final byte[] cursor, final ScanParams params) {
    final List<byte[]> args = new ArrayList<>();
    args.add(key);
    args.add(cursor);
    args.addAll(params.getParams());
    sendCommand(HSCAN, args.toArray(new byte[args.size()][]));
  }

  public void sscan(final byte[] key, final byte[] cursor, final ScanParams params) {
    final List<byte[]> args = new ArrayList<>();
    args.add(key);
    args.add(cursor);
    args.addAll(params.getParams());
    sendCommand(SSCAN, args.toArray(new byte[args.size()][]));
  }

  public void zscan(final byte[] key, final byte[] cursor, final ScanParams params) {
    final List<byte[]> args = new ArrayList<>();
    args.add(key);
    args.add(cursor);
    args.addAll(params.getParams());
    sendCommand(ZSCAN, args.toArray(new byte[args.size()][]));
  }

  public void waitReplicas(final int replicas, final long timeout) {
    sendCommand(WAIT, toByteArray(replicas), toByteArray(timeout));
  }

  public void cluster(final byte[]... args) {
    sendCommand(CLUSTER, args);
  }

  public void asking() {
    sendCommand(ASKING);
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

  public void readonly() {
    sendCommand(READONLY);
  }

  public void geoadd(final byte[] key, final double longitude, final double latitude, final byte[] member) {
    sendCommand(GEOADD, key, toByteArray(longitude), toByteArray(latitude), member);
  }

  public void geoadd(final byte[] key, final Map<byte[], GeoCoordinate> memberCoordinateMap) {
    List<byte[]> args = new ArrayList<>(memberCoordinateMap.size() * 3 + 1);
    args.add(key);
    args.addAll(convertGeoCoordinateMapToByteArrays(memberCoordinateMap));

    byte[][] argsArray = new byte[args.size()][];
    args.toArray(argsArray);

    sendCommand(GEOADD, argsArray);
  }

  public void geodist(final byte[] key, final byte[] member1, final byte[] member2) {
    sendCommand(GEODIST, key, member1, member2);
  }

  public void geodist(final byte[] key, final byte[] member1, final byte[] member2, final GeoUnit unit) {
    sendCommand(GEODIST, key, member1, member2, unit.raw);
  }

  public void geohash(final byte[] key, final byte[]... members) {
    sendCommand(GEOHASH, joinParameters(key, members));
  }

  public void geopos(final byte[] key, final byte[][] members) {
    sendCommand(GEOPOS, joinParameters(key, members));
  }

  public void georadius(final byte[] key, final double longitude, final double latitude, final double radius, final GeoUnit unit) {
    sendCommand(GEORADIUS, key, toByteArray(longitude), toByteArray(latitude), toByteArray(radius),
      unit.raw);
  }

  public void georadiusReadonly(final byte[] key, final double longitude, final double latitude, final double radius, final GeoUnit unit) {
    sendCommand(GEORADIUS_RO, key, toByteArray(longitude), toByteArray(latitude), toByteArray(radius),
      unit.raw);
  }

  public void georadius(final byte[] key, final double longitude, final double latitude, final double radius, final GeoUnit unit,
      final GeoRadiusParam param) {
    sendCommand(GEORADIUS, param.getByteParams(key, toByteArray(longitude), toByteArray(latitude),
      toByteArray(radius), unit.raw));
  }

  public void georadiusReadonly(final byte[] key, final double longitude, final double latitude, final double radius, final GeoUnit unit,
      final GeoRadiusParam param) {
    sendCommand(GEORADIUS_RO, param.getByteParams(key, toByteArray(longitude), toByteArray(latitude),
      toByteArray(radius), unit.raw));
  }

  public void georadiusByMember(final byte[] key, final byte[] member, final double radius, final GeoUnit unit) {
    sendCommand(GEORADIUSBYMEMBER, key, member, toByteArray(radius), unit.raw);
  }

  public void georadiusByMemberReadonly(final byte[] key, final byte[] member, final double radius, final GeoUnit unit) {
    sendCommand(GEORADIUSBYMEMBER_RO, key, member, toByteArray(radius), unit.raw);
  }

  public void georadiusByMember(final byte[] key, final byte[] member, final double radius, final GeoUnit unit,
      final GeoRadiusParam param) {
    sendCommand(GEORADIUSBYMEMBER, param.getByteParams(key, member, toByteArray(radius), unit.raw));
  }

  public void georadiusByMemberReadonly(final byte[] key, final byte[] member, final double radius, final GeoUnit unit,
      final GeoRadiusParam param) {
    sendCommand(GEORADIUSBYMEMBER_RO, param.getByteParams(key, member, toByteArray(radius), unit.raw));
  }

  public void moduleLoad(final byte[] path) {
    sendCommand(MODULE, Keyword.LOAD.raw, path);
  }

  public void moduleList() {
    sendCommand(MODULE, Keyword.LIST.raw);
  }

  public void moduleUnload(final byte[] name) {
    sendCommand(MODULE, Keyword.UNLOAD.raw, name);
  }

  private ArrayList<byte[]> convertScoreMembersToByteArrays(final Map<byte[], Double> scoreMembers) {
    ArrayList<byte[]> args = new ArrayList<>(scoreMembers.size() * 2);

    for (Map.Entry<byte[], Double> entry : scoreMembers.entrySet()) {
      args.add(toByteArray(entry.getValue()));
      args.add(entry.getKey());
    }

    return args;
  }

  public void aclWhoAmI() { sendCommand(ACL, Keyword.WHOAMI.raw); }

  public void aclGenPass() { sendCommand(ACL, Keyword.GENPASS.raw); }

  public void aclList() { sendCommand(ACL, Keyword.LIST.raw); }

  public void aclUsers() { sendCommand(ACL, Keyword.USERS.raw); }

  public void aclCat() { sendCommand(ACL, Keyword.CAT.raw); }

  public void aclCat(final byte[] category) {
    sendCommand(ACL, Keyword.CAT.raw, category);
  }

  public void aclSetUser(final byte[] name) {
    sendCommand(ACL, Keyword.SETUSER.raw, name);
  }

  public void aclGetUser(final byte[] name) {
    sendCommand(ACL, Keyword.GETUSER.raw, name);
  }

  public void aclSetUser(final byte[] name, byte[][] parameters) {
    sendCommand(ACL, joinParameters(Keyword.SETUSER.raw,name, parameters));
  }

  public void aclDelUser(final byte[] name) {
    sendCommand(ACL, Keyword.DELUSER.raw, name);
  }

  private List<byte[]> convertGeoCoordinateMapToByteArrays(
      final Map<byte[], GeoCoordinate> memberCoordinateMap) {
    List<byte[]> args = new ArrayList<>(memberCoordinateMap.size() * 3);

    for (Entry<byte[], GeoCoordinate> entry : memberCoordinateMap.entrySet()) {
      GeoCoordinate coordinate = entry.getValue();
      args.add(toByteArray(coordinate.getLongitude()));
      args.add(toByteArray(coordinate.getLatitude()));
      args.add(entry.getKey());
    }

    return args;
  }

  public void bitfield(final byte[] key, final byte[]... value) {
    sendCommand(BITFIELD, joinParameters(key, value));
  }

  public void bitfieldReadonly(final byte[] key, final byte[]... arguments) {
    sendCommand(BITFIELD_RO, joinParameters(key, arguments));
  }

  public void hstrlen(final byte[] key, final byte[] field) {
    sendCommand(HSTRLEN, key, field);
  }
  
  public void xadd(final byte[] key, final byte[] id, final Map<byte[], byte[]> hash, long maxLen, boolean approximateLength) {
      int maxLexArgs = 0;
      if(maxLen < Long.MAX_VALUE) { // optional arguments
        if(approximateLength) {
          maxLexArgs = 3; // e.g. MAXLEN ~ 1000 
        } else {
          maxLexArgs = 2; // e.g. MAXLEN 1000
        }
      }
    
	  final byte[][] params = new byte[2 + maxLexArgs + hash.size() * 2][];
	  int index = 0;
	  params[index++] = key;
	  if(maxLen < Long.MAX_VALUE) {
	    params[index++] = Keyword.MAXLEN.raw;
	    if(approximateLength) {
	      params[index++] = Protocol.BYTES_TILDE;
	    }
	    params[index++] = toByteArray(maxLen);
	  }
	  
	  params[index++] = id;
	  for (final Entry<byte[], byte[]> entry : hash.entrySet()) {
	    params[index++] = entry.getKey();
	    params[index++] = entry.getValue();
	  }
	  sendCommand(XADD, params);
  }
  
  public void xlen(final byte[] key) {
     sendCommand(XLEN, key);
  }
  
  public void xrange(final byte[] key, final byte[] start, final byte[] end, final long count) { 
     sendCommand(XRANGE, key, start, end, Keyword.COUNT.raw, toByteArray(count));
  }
  
  public void xrevrange(final byte[] key, final byte[] end, final byte[] start, final int count) {
    sendCommand(XREVRANGE, key, end, start, Keyword.COUNT.raw, toByteArray(count));
  }

  public void xread(final int count, final long block, final Map<byte[], byte[]> streams) {
    final byte[][] params = new byte[3 + streams.size() * 2 + (block > 0 ? 2 : 0)][];

    int streamsIndex = 0;
    params[streamsIndex++] = Keyword.COUNT.raw;
    params[streamsIndex++] = toByteArray(count);
    if(block > 0) {
      params[streamsIndex++] = Keyword.BLOCK.raw;
      params[streamsIndex++] = toByteArray(block);
    }
    
    params[streamsIndex++] = Keyword.STREAMS.raw;
    int idsIndex = streamsIndex + streams.size();

    for (final Entry<byte[], byte[]> entry : streams.entrySet()) {
      params[streamsIndex++] = entry.getKey();
      params[idsIndex++] = entry.getValue();
    }
    
    sendCommand(XREAD, params);
 }
  
  public void xack(final byte[] key, final byte[] group, final byte[]... ids) {
    final byte[][] params = new byte[2 + ids.length][];
    int index = 0;
    params[index++] = key;
    params[index++] = group;
    for (final byte[] id : ids) {
      params[index++] = id;
    }
    sendCommand(XACK, params);
  }
   
  public void xgroupCreate(final byte[] key, final byte[] groupname, final byte[] id, boolean makeStream) {
    if(makeStream) {
      sendCommand(XGROUP, Keyword.CREATE.raw, key, groupname, id, Keyword.MKSTREAM.raw);  
    } else {
      sendCommand(XGROUP, Keyword.CREATE.raw, key, groupname, id);  
    }
  }

  public void xgroupSetID(final byte[] key, final byte[] groupname, final byte[] id) {
    sendCommand(XGROUP, Keyword.SETID.raw, key, groupname, id);    
  }

  public void xgroupDestroy(final byte[] key, final byte[] groupname) {
    sendCommand(XGROUP, Keyword.DESTROY.raw, key, groupname);    
  }

  public void xgroupDelConsumer(final byte[] key, final byte[] groupname, final byte[] consumerName) {
    sendCommand(XGROUP, Keyword.DELCONSUMER.raw, key, groupname, consumerName);    
  }
   
  public void xdel(final byte[] key, final byte[]... ids) {
    final byte[][] params = new byte[1 + ids.length][];
    int index = 0;
    params[index++] = key;
    for (final byte[] id : ids) {
      params[index++] = id;
    }
    sendCommand(XDEL, params);
  }
  
  public void xtrim(byte[] key, long maxLen, boolean approximateLength) {
    if(approximateLength) {
      sendCommand(XTRIM, key, Keyword.MAXLEN.raw, Protocol.BYTES_TILDE ,toByteArray(maxLen));
    } else {
      sendCommand(XTRIM, key, Keyword.MAXLEN.raw, toByteArray(maxLen));
    }
  }
  
  public void xreadGroup(byte[] groupname, byte[] consumer, int count, long block, boolean noAck, Map<byte[], byte[]> streams) {
    
    int optional = 0;
    if(count>0) {
      optional += 2;
    }
    if(block > 0) {
      optional += 2;
    }
    if(noAck) {
      optional += 1;
    }
    
    
    final byte[][] params = new byte[4 + optional + streams.size() * 2][];

    int streamsIndex = 0;
    params[streamsIndex++] = Keyword.GROUP.raw;
    params[streamsIndex++] = groupname;
    params[streamsIndex++] = consumer;
    if(count>0) {
      params[streamsIndex++] = Keyword.COUNT.raw;
      params[streamsIndex++] = toByteArray(count);
    }
    if(block > 0) {
      params[streamsIndex++] = Keyword.BLOCK.raw;
      params[streamsIndex++] = toByteArray(block);
    }
    if(noAck) {
      params[streamsIndex++] = Keyword.NOACK.raw;
    }
    params[streamsIndex++] = Keyword.STREAMS.raw;
    
    int idsIndex = streamsIndex + streams.size();
    for (final Entry<byte[], byte[]> entry : streams.entrySet()) {
      params[streamsIndex++] = entry.getKey();
      params[idsIndex++] = entry.getValue();
    }
    
    sendCommand(XREADGROUP, params);
  }

  
  public void xpending(byte[] key, byte[] groupname, byte[] start, byte[] end, int count, byte[] consumername) {
    if(consumername == null) {
      sendCommand(XPENDING, key, groupname, start, end, toByteArray(count));
    } else {
      sendCommand(XPENDING, key, groupname, start, end, toByteArray(count), consumername);
    }
  }

  public void xclaim(byte[] key, byte[] groupname, byte[] consumername, long minIdleTime, long newIdleTime, int retries, boolean force, byte[][] ids) {
      
      ArrayList<byte[]> arguments = new ArrayList<>(10 + ids.length);

      arguments.add(key);
      arguments.add(groupname);
      arguments.add(consumername);
      arguments.add(toByteArray(minIdleTime));
      
      for(byte[] id : ids) {
        arguments.add(id);  
      }
      if(newIdleTime > 0) {
        arguments.add(Keyword.IDLE.raw);
        arguments.add(toByteArray(newIdleTime));
      }
      if(retries > 0) {
        arguments.add(Keyword.RETRYCOUNT.raw);
        arguments.add(toByteArray(retries));        
      }
      if(force) {
        arguments.add(Keyword.FORCE.raw);        
      }
      sendCommand(XCLAIM, arguments.toArray(new byte[arguments.size()][]));
  }

  public void xinfoStream(byte[] key) {

    sendCommand(XINFO,Keyword.STREAM.raw,key);

  }

  public void xinfoGroup(byte[] key) {

    sendCommand(XINFO,Keyword.GROUPS.raw,key);

  }

  public void xinfoConsumers (byte[] key, byte[] group) {

    sendCommand(XINFO,Keyword.CONSUMERS.raw,key,group);
  }

}
