package redis.clients.jedis;

import java.util.function.Function;
import redis.clients.jedis.commands.BinaryJedisClusterCommands;
import redis.clients.jedis.commands.JedisClusterBinaryScriptingCommands;
import redis.clients.jedis.commands.MultiKeyBinaryJedisClusterCommands;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.GeoRadiusStoreParam;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;
import redis.clients.jedis.params.LPosParams;
import redis.clients.jedis.util.JedisClusterCRC16;
import redis.clients.jedis.util.JedisClusterHashTagUtil;
import redis.clients.jedis.util.KeyMergeUtil;
import redis.clients.jedis.util.SafeEncoder;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * Interface to a Jedis cluster.
 * <p/>
 * Uses {@link DefaultRetryer}, or you can inject your own using
 * {@link BinaryJedisCluster#BinaryJedisCluster(JedisClusterConnectionHandler, Retryer)}.
 *
 * @see JedisCluster
 */
public class BinaryJedisCluster implements BinaryJedisClusterCommands,
    MultiKeyBinaryJedisClusterCommands, JedisClusterBinaryScriptingCommands, Closeable {

  public static final int HASHSLOTS = 16384;
  protected static final int DEFAULT_TIMEOUT = 2000;
  protected static final int DEFAULT_MAX_ATTEMPTS = 5;

  protected final Retryer retryer;
  protected final JedisClusterConnectionHandler connectionHandler;

  public BinaryJedisCluster(Set<HostAndPort> nodes) {
    this(nodes, DEFAULT_TIMEOUT);
  }

  public BinaryJedisCluster(Set<HostAndPort> nodes, int timeout) {
    this(nodes, timeout, DEFAULT_MAX_ATTEMPTS, new GenericObjectPoolConfig());
  }

  public BinaryJedisCluster(Set<HostAndPort> jedisClusterNode, int timeout, int maxAttempts,
      final GenericObjectPoolConfig poolConfig) {
    this(jedisClusterNode, timeout, timeout, maxAttempts, poolConfig);
  }

  public BinaryJedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout,
                            int soTimeout, int maxAttempts, final GenericObjectPoolConfig poolConfig) {
    this(jedisClusterNode, connectionTimeout, soTimeout, maxAttempts, null, poolConfig);
  }

  public BinaryJedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout, int maxAttempts, String password, GenericObjectPoolConfig poolConfig) {
    this(jedisClusterNode, connectionTimeout, soTimeout, maxAttempts, password, null, poolConfig);
  }

  public BinaryJedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout, int maxAttempts, String password, String clientName, GenericObjectPoolConfig poolConfig) {
    this(jedisClusterNode, connectionTimeout, soTimeout, maxAttempts, null, password, clientName, poolConfig);
  }

  public BinaryJedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout, int maxAttempts, String user, String password, String clientName, GenericObjectPoolConfig poolConfig) {
    JedisClusterConnectionHandler connectionHandler = new JedisSlotBasedConnectionHandler(jedisClusterNode, poolConfig,
        connectionTimeout, soTimeout, user, password, clientName);
    this.connectionHandler = connectionHandler;
    this.retryer = new DefaultRetryer(maxAttempts);
  }

  public BinaryJedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout,
      int infiniteSoTimeout, int maxAttempts, String user, String password, String clientName, GenericObjectPoolConfig poolConfig) {
    JedisClusterConnectionHandler connectionHandler = new JedisSlotBasedConnectionHandler(jedisClusterNode, poolConfig,
        connectionTimeout, soTimeout, infiniteSoTimeout, user, password, clientName);
    this.connectionHandler = connectionHandler;
    this.retryer = new DefaultRetryer(maxAttempts);
  }

  public BinaryJedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout, int maxAttempts, String password, String clientName, GenericObjectPoolConfig poolConfig,
      boolean ssl) {
    this(jedisClusterNode, connectionTimeout, soTimeout, maxAttempts, password, clientName, poolConfig, ssl, null, null, null, null);
  }

  public BinaryJedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout, int maxAttempts,
      String user, String password, String clientName, GenericObjectPoolConfig poolConfig, boolean ssl) {
    this(jedisClusterNode, connectionTimeout, soTimeout, maxAttempts, user, password, clientName, poolConfig, ssl, null, null, null, null);
  }

  public BinaryJedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout, int maxAttempts, String password, String clientName, GenericObjectPoolConfig poolConfig,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters, HostnameVerifier hostnameVerifier, JedisClusterHostAndPortMap hostAndPortMap) {
    this(jedisClusterNode, connectionTimeout, soTimeout, maxAttempts, null, password, clientName,
        poolConfig, ssl, sslSocketFactory, sslParameters, hostnameVerifier, hostAndPortMap);
  }

  public BinaryJedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout,
      int maxAttempts, String user, String password, String clientName, GenericObjectPoolConfig poolConfig,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier, JedisClusterHostAndPortMap hostAndPortMap) {
    JedisClusterConnectionHandler connectionHandler = new JedisSlotBasedConnectionHandler(jedisClusterNode, poolConfig,
        connectionTimeout, soTimeout, user, password, clientName, ssl, sslSocketFactory, sslParameters, hostnameVerifier, hostAndPortMap);
    this.connectionHandler = connectionHandler;
    this.retryer = new DefaultRetryer(maxAttempts);
  }

  public BinaryJedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout,
      int infiniteSoTimeout, int maxAttempts, String user, String password, String clientName, GenericObjectPoolConfig poolConfig,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters, HostnameVerifier hostnameVerifier, JedisClusterHostAndPortMap hostAndPortMap) {
    JedisClusterConnectionHandler connectionHandler = new JedisSlotBasedConnectionHandler(jedisClusterNode, poolConfig,
        connectionTimeout, soTimeout, infiniteSoTimeout, user, password, clientName, ssl, sslSocketFactory, sslParameters, hostnameVerifier, hostAndPortMap);
    this.connectionHandler = connectionHandler;
    this.retryer = new DefaultRetryer(maxAttempts);
  }

  public BinaryJedisCluster(JedisClusterConnectionHandler connectionHandler, Retryer retryer) {
    this.connectionHandler = connectionHandler;
    this.retryer = retryer;
  }

  @Override
  public void close() {
    if (connectionHandler != null) {
      connectionHandler.close();
    }
  }

  public Map<String, JedisPool> getClusterNodes() {
    return connectionHandler.getNodes();
  }

  public Jedis getConnectionFromSlot(int slot) {
    return this.connectionHandler.getConnectionFromSlot(slot);
  }

  private <R> R runBinary(Function<Jedis, R> command, byte[] key) {
    return retryer.runWithRetries(connectionHandler, JedisClusterCRC16.getSlot(key), command);
  }

  private <R> R runBinary(Function<Jedis, R> command, int keyCount, byte[]... keys) {
    if (keys == null || keys.length == 0) {
      throw new JedisClusterOperationException("No way to dispatch this command to Redis Cluster.");
    }

    // For multiple keys, only execute if they all share the same connection slot.
    int slot = JedisClusterCRC16.getSlot(keys[0]);
    if (keys.length > 1) {
      for (int i = 1; i < keyCount; i++) {
        int nextSlot = JedisClusterCRC16.getSlot(keys[i]);
        if (slot != nextSlot) {
          throw new JedisClusterOperationException("No way to dispatch this command to Redis "
              + "Cluster because keys have different slots.");
        }
      }
    }

    return retryer.runWithRetries(connectionHandler, slot, command);
  }

  @Override
  public String set(final byte[] key, final byte[] value) {
    return runBinary((connection) -> connection.set(key, value), key);
  }

  @Override
  public String set(final byte[] key, final byte[] value, final SetParams params) {
    return runBinary((connection) -> connection.set(key, value, params), key);
  }

  @Override
  public byte[] get(final byte[] key) {
    return runBinary((connection) -> connection.get(key), key);
  }

  @Override
  public Long exists(final byte[]... keys) {
    return runBinary((connection) -> connection.exists(keys), keys.length, keys);
  }

  @Override
  public Boolean exists(final byte[] key) {
    return runBinary((connection) -> connection.exists(key), key);
  }

  @Override
  public Long persist(final byte[] key) {
    return runBinary((connection) -> connection.persist(key), key);
  }

  @Override
  public String type(final byte[] key) {
    return runBinary((connection) -> connection.type(key), key);
  }

  @Override
  public byte[] dump(final byte[] key) {
    return runBinary((connection) -> connection.dump(key), key);
  }

  @Override
  public String restore(final byte[] key, final int ttl, final byte[] serializedValue) {
    return runBinary((connection) -> connection.restore(key, ttl, serializedValue), key);
  }

  @Override
  public Long expire(final byte[] key, final int seconds) {
    return runBinary((connection) -> connection.expire(key, seconds), key);
  }

  @Override
  public Long pexpire(final byte[] key, final long milliseconds) {
    return runBinary((connection) -> connection.pexpire(key, milliseconds), key);
  }

  @Override
  public Long expireAt(final byte[] key, final long unixTime) {
    return runBinary((connection) -> connection.expireAt(key, unixTime), key);
  }

  @Override
  public Long pexpireAt(final byte[] key, final long millisecondsTimestamp) {
    return runBinary((connection) -> connection.pexpireAt(key, millisecondsTimestamp), key);
  }

  @Override
  public Long ttl(final byte[] key) {
    return runBinary((connection) -> connection.ttl(key), key);
  }

  @Override
  public Long pttl(final byte[] key) {
    return runBinary((connection) -> connection.pttl(key), key);
  }

  @Override
  public Long touch(final byte[] key) {
    return runBinary((connection) -> connection.touch(key), key);
  }

  @Override
  public Long touch(final byte[]... keys) {
    return runBinary((connection) -> connection.touch(keys), keys.length, keys);
  }

  @Override
  public Boolean setbit(final byte[] key, final long offset, final boolean value) {
    return runBinary((connection) -> connection.setbit(key, offset, value), key);
  }

  @Override
  public Boolean setbit(final byte[] key, final long offset, final byte[] value) {
    return runBinary((connection) -> connection.setbit(key, offset, value), key);
  }

  @Override
  public Boolean getbit(final byte[] key, final long offset) {
    return runBinary((connection) -> connection.getbit(key, offset), key);
  }

  @Override
  public Long setrange(final byte[] key, final long offset, final byte[] value) {
    return runBinary((connection) -> connection.setrange(key, offset, value), key);
  }

  @Override
  public byte[] getrange(final byte[] key, final long startOffset, final long endOffset) {
    return runBinary((connection) -> connection.getrange(key, startOffset, endOffset), key);
  }

  @Override
  public byte[] getSet(final byte[] key, final byte[] value) {
    return runBinary((connection) -> connection.getSet(key, value), key);
  }

  @Override
  public Long setnx(final byte[] key, final byte[] value) {
    return runBinary((connection) -> connection.setnx(key, value), key);
  }

  @Override
  public String psetex(final byte[] key, final long milliseconds, final byte[] value) {
    return runBinary((connection) -> connection.psetex(key, milliseconds, value), key);
  }

  @Override
  public String setex(final byte[] key, final int seconds, final byte[] value) {
    return runBinary((connection) -> connection.setex(key, seconds, value), key);
  }

  @Override
  public Long decrBy(final byte[] key, final long decrement) {
    return runBinary((connection) -> connection.decrBy(key, decrement), key);
  }

  @Override
  public Long decr(final byte[] key) {
    return runBinary((connection) -> connection.decr(key), key);
  }

  @Override
  public Long incrBy(final byte[] key, final long increment) {
    return runBinary((connection) -> connection.incrBy(key, increment), key);
  }

  @Override
  public Double incrByFloat(final byte[] key, final double increment) {
    return runBinary((connection) -> connection.incrByFloat(key, increment), key);
  }

  @Override
  public Long incr(final byte[] key) {
    return runBinary((connection) -> connection.incr(key), key);
  }

  @Override
  public Long append(final byte[] key, final byte[] value) {
    return runBinary((connection) -> connection.append(key, value), key);
  }

  @Override
  public byte[] substr(final byte[] key, final int start, final int end) {
    return runBinary((connection) -> connection.substr(key, start, end), key);
  }

  @Override
  public Long hset(final byte[] key, final byte[] field, final byte[] value) {
    return runBinary((connection) -> connection.hset(key, field, value), key);
  }

  @Override
  public Long hset(final byte[] key, final Map<byte[], byte[]> hash) {
    return runBinary((connection) -> connection.hset(key, hash), key);
  }

  @Override
  public byte[] hget(final byte[] key, final byte[] field) {
    return runBinary((connection) -> connection.hget(key, field), key);
  }

  @Override
  public Long hsetnx(final byte[] key, final byte[] field, final byte[] value) {
    return runBinary((connection) -> connection.hsetnx(key, field, value), key);
  }

  @Override
  public String hmset(final byte[] key, final Map<byte[], byte[]> hash) {
    return runBinary((connection) -> connection.hmset(key, hash), key);
  }

  @Override
  public List<byte[]> hmget(final byte[] key, final byte[]... fields) {
    return runBinary((connection) -> connection.hmget(key, fields), key);
  }

  @Override
  public Long hincrBy(final byte[] key, final byte[] field, final long value) {
    return runBinary((connection) -> connection.hincrBy(key, field, value), key);
  }

  @Override
  public Double hincrByFloat(final byte[] key, final byte[] field, final double value) {
    return runBinary((connection) -> connection.hincrByFloat(key, field, value), key);
  }

  @Override
  public Boolean hexists(final byte[] key, final byte[] field) {
    return runBinary((connection) -> connection.hexists(key, field), key);
  }

  @Override
  public Long hdel(final byte[] key, final byte[]... field) {
    return runBinary((connection) -> connection.hdel(key, field), key);
  }

  @Override
  public Long hlen(final byte[] key) {
    return runBinary((connection) -> connection.hlen(key), key);
  }

  @Override
  public Set<byte[]> hkeys(final byte[] key) {
    return runBinary((connection) -> connection.hkeys(key), key);
  }

  @Override
  public List<byte[]> hvals(final byte[] key) {
    return runBinary((connection) -> connection.hvals(key), key);
  }

  @Override
  public Map<byte[], byte[]> hgetAll(final byte[] key) {
    return runBinary((connection) -> connection.hgetAll(key), key);
  }

  @Override
  public Long rpush(final byte[] key, final byte[]... args) {
    return runBinary((connection) -> connection.rpush(key, args), key);
  }

  @Override
  public Long lpush(final byte[] key, final byte[]... args) {
    return runBinary((connection) -> connection.lpush(key, args), key);
  }

  @Override
  public Long llen(final byte[] key) {
    return runBinary((connection) -> connection.llen(key), key);
  }

  @Override
  public List<byte[]> lrange(final byte[] key, final long start, final long stop) {
    return runBinary((connection) -> connection.lrange(key, start, stop), key);
  }

  @Override
  public String ltrim(final byte[] key, final long start, final long stop) {
    return runBinary((connection) -> connection.ltrim(key, start, stop), key);
  }

  @Override
  public byte[] lindex(final byte[] key, final long index) {
    return runBinary((connection) -> connection.lindex(key, index), key);
  }

  @Override
  public String lset(final byte[] key, final long index, final byte[] value) {
    return runBinary((connection) -> connection.lset(key, index, value), key);
  }

  @Override
  public Long lrem(final byte[] key, final long count, final byte[] value) {
    return runBinary((connection) -> connection.lrem(key, count, value), key);
  }

  @Override
  public byte[] lpop(final byte[] key) {
    return runBinary((connection) -> connection.lpop(key), key);
  }

  @Override
  public List<byte[]> lpop(final byte[] key, final int count) {
    return runBinary((connection) -> connection.lpop(key, count), key);
  }

  @Override
  public Long lpos(final byte[] key, final byte[] element) {
    return runBinary((connection) -> connection.lpos(key, element), key);
  }

  @Override
  public Long lpos(final byte[] key, final byte[] element, final LPosParams params) {
    return runBinary((connection) -> connection.lpos(key, element, params), key);
  }

  @Override
  public List<Long> lpos(final byte[] key, final byte[] element, final LPosParams params, final long count) {
    return runBinary((connection) -> connection.lpos(key, element, params, count), key);
  }

  @Override
  public byte[] rpop(final byte[] key) {
    return runBinary((connection) -> connection.rpop(key), key);
  }

  @Override
  public List<byte[]> rpop(final byte[] key, final int count) {
    return runBinary((connection) -> connection.rpop(key, count), key);
  }

  @Override
  public Long sadd(final byte[] key, final byte[]... member) {
    return runBinary((connection) -> connection.sadd(key, member), key);
  }

  @Override
  public Set<byte[]> smembers(final byte[] key) {
    return runBinary((connection) -> connection.smembers(key), key);
  }

  @Override
  public Long srem(final byte[] key, final byte[]... member) {
    return runBinary((connection) -> connection.srem(key, member), key);
  }

  @Override
  public byte[] spop(final byte[] key) {
    return runBinary((connection) -> connection.spop(key), key);
  }

  @Override
  public Set<byte[]> spop(final byte[] key, final long count) {
    return runBinary((connection) -> connection.spop(key, count), key);
  }

  @Override
  public Long scard(final byte[] key) {
    return runBinary((connection) -> connection.scard(key), key);
  }

  @Override
  public Boolean sismember(final byte[] key, final byte[] member) {
    return runBinary((connection) -> connection.sismember(key, member), key);
  }

  @Override
  public List<Boolean> smismember(final byte[] key, final byte[]... members) {
    return runBinary((connection) -> connection.smismember(key, members), key);
  }

  @Override
  public byte[] srandmember(final byte[] key) {
    return runBinary((connection) -> connection.srandmember(key), key);
  }

  @Override
  public Long strlen(final byte[] key) {
    return runBinary((connection) -> connection.strlen(key), key);
  }

  @Override
  public Long zadd(final byte[] key, final double score, final byte[] member) {
    return runBinary((connection) -> connection.zadd(key, score, member), key);
  }

  @Override
  public Long zadd(final byte[] key, final double score, final byte[] member,
      final ZAddParams params) {
    return runBinary((connection) -> connection.zadd(key, score, member, params), key);
  }

  @Override
  public Long zadd(final byte[] key, final Map<byte[], Double> scoreMembers) {
    return runBinary((connection) -> connection.zadd(key, scoreMembers), key);
  }

  @Override
  public Long zadd(final byte[] key, final Map<byte[], Double> scoreMembers, final ZAddParams params) {
    return runBinary((connection) -> connection.zadd(key, scoreMembers, params), key);
  }

  @Override
  public Set<byte[]> zrange(final byte[] key, final long start, final long stop) {
    return runBinary((connection) -> connection.zrange(key, start, stop), key);
  }

  @Override
  public Long zrem(final byte[] key, final byte[]... members) {
    return runBinary((connection) -> connection.zrem(key, members), key);
  }

  @Override
  public Double zincrby(final byte[] key, final double increment, final byte[] member) {
    return runBinary((connection) -> connection.zincrby(key, increment, member), key);
  }

  @Override
  public Double zincrby(final byte[] key, final double increment, final byte[] member,
      final ZIncrByParams params) {
    return runBinary((connection) -> connection.zincrby(key, increment, member, params), key);
  }

  @Override
  public Long zrank(final byte[] key, final byte[] member) {
    return runBinary((connection) -> connection.zrank(key, member), key);
  }

  @Override
  public Long zrevrank(final byte[] key, final byte[] member) {
    return runBinary((connection) -> connection.zrevrank(key, member), key);
  }

  @Override
  public Set<byte[]> zrevrange(final byte[] key, final long start, final long stop) {
    return runBinary((connection) -> connection.zrevrange(key, start, stop), key);
  }

  @Override
  public Set<Tuple> zrangeWithScores(final byte[] key, final long start, final long stop) {
    return runBinary((connection) -> connection.zrangeWithScores(key, start, stop), key);
  }

  @Override
  public Set<Tuple> zrevrangeWithScores(final byte[] key, final long start, final long stop) {
    return runBinary((connection) -> connection.zrevrangeWithScores(key, start, stop), key);
  }

  @Override
  public Long zcard(final byte[] key) {
    return runBinary((connection) -> connection.zcard(key), key);
  }

  @Override
  public Double zscore(final byte[] key, final byte[] member) {
    return runBinary((connection) -> connection.zscore(key, member), key);
  }

  @Override
  public List<Double> zmscore(final byte[] key, final byte[]... members) {
    return runBinary((connection) -> connection.zmscore(key, members), key);
  }

  @Override
  public Tuple zpopmax(final byte[] key) {
    return runBinary((connection) -> connection.zpopmax(key), key);
  }

  @Override
  public Set<Tuple> zpopmax(final byte[] key, final int count) {
    return runBinary((connection) -> connection.zpopmax(key, count), key);
  }

  @Override
  public Tuple zpopmin(final byte[] key) {
    return runBinary((connection) -> connection.zpopmin(key), key);
  }

  @Override
  public Set<Tuple> zpopmin(final byte[] key, final int count) {
    return runBinary((connection) -> connection.zpopmin(key, count), key);
  }

  @Override
  public List<byte[]> sort(final byte[] key) {
    return runBinary((connection) -> connection.sort(key), key);
  }

  @Override
  public List<byte[]> sort(final byte[] key, final SortingParams sortingParameters) {
    return runBinary((connection) -> connection.sort(key, sortingParameters), key);
  }

  @Override
  public Long zcount(final byte[] key, final double min, final double max) {
    return runBinary((connection) -> connection.zcount(key, min, max), key);
  }

  @Override
  public Long zcount(final byte[] key, final byte[] min, final byte[] max) {
    return runBinary((connection) -> connection.zcount(key, min, max), key);
  }

  @Override
  public Set<byte[]> zrangeByScore(final byte[] key, final double min, final double max) {
    return runBinary((connection) -> connection.zrangeByScore(key, min, max), key);
  }

  @Override
  public Set<byte[]> zrangeByScore(final byte[] key, final byte[] min, final byte[] max) {
    return runBinary((connection) -> connection.zrangeByScore(key, min, max), key);
  }

  @Override
  public Set<byte[]> zrevrangeByScore(final byte[] key, final double max, final double min) {
    return runBinary((connection) -> connection.zrevrangeByScore(key, max, min), key);
  }

  @Override
  public Set<byte[]> zrangeByScore(final byte[] key, final double min, final double max,
      final int offset, final int count) {
    return runBinary((connection) -> connection.zrangeByScore(key, min, max, offset, count), key);
  }

  @Override
  public Set<byte[]> zrevrangeByScore(final byte[] key, final byte[] max, final byte[] min) {
    return runBinary((connection) -> connection.zrevrangeByScore(key, max, min), key);
  }

  @Override
  public Set<byte[]> zrangeByScore(final byte[] key, final byte[] min, final byte[] max,
      final int offset, final int count) {
    return runBinary((connection) -> connection.zrangeByScore(key, min, max, offset, count), key);
  }

  @Override
  public Set<byte[]> zrevrangeByScore(final byte[] key, final double max, final double min,
      final int offset, final int count) {
    return runBinary((connection) -> connection.zrevrangeByScore(key, max, min, offset, count), key);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final double min, final double max) {
    return runBinary((connection) -> connection.zrangeByScoreWithScores(key, min, max), key);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final byte[] key, final double max, final double min) {
    return runBinary((connection) -> connection.zrevrangeByScoreWithScores(key, max, min), key);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final double min, final double max,
      final int offset, final int count) {
    return runBinary((connection) -> connection.zrangeByScoreWithScores(key, min, max, offset, count), key);
  }

  @Override
  public Set<byte[]> zrevrangeByScore(final byte[] key, final byte[] max, final byte[] min,
      final int offset, final int count) {
    return runBinary((connection) -> connection.zrevrangeByScore(key, max, min, offset, count), key);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final byte[] min, final byte[] max) {
    return runBinary((connection) -> connection.zrangeByScoreWithScores(key, min, max), key);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final byte[] key, final byte[] max, final byte[] min) {
    return runBinary((connection) -> connection.zrevrangeByScoreWithScores(key, max, min), key);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final byte[] min, final byte[] max,
      final int offset, final int count) {
    return runBinary((connection) -> connection.zrangeByScoreWithScores(key, min, max, offset, count), key);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final byte[] key, final double max,
      final double min, final int offset, final int count) {
    return runBinary((connection) -> connection.zrevrangeByScoreWithScores(key, max, min, offset, count), key);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final byte[] key, final byte[] max,
      final byte[] min, final int offset, final int count) {
    return runBinary((connection) -> connection.zrevrangeByScoreWithScores(key, max, min, offset, count), key);
  }

  @Override
  public Long zremrangeByRank(final byte[] key, final long start, final long stop) {
    return runBinary((connection) -> connection.zremrangeByRank(key, start, stop), key);
  }

  @Override
  public Long zremrangeByScore(final byte[] key, final double min, final double max) {
    return runBinary((connection) -> connection.zremrangeByScore(key, min, max), key);
  }

  @Override
  public Long zremrangeByScore(final byte[] key, final byte[] min, final byte[] max) {
    return runBinary((connection) -> connection.zremrangeByScore(key, min, max), key);
  }

  @Override
  public Long linsert(final byte[] key, final ListPosition where, final byte[] pivot,
      final byte[] value) {
    return runBinary((connection) -> connection.linsert(key, where, pivot, value), key);
  }

  @Override
  public Long lpushx(final byte[] key, final byte[]... arg) {
    return runBinary((connection) -> connection.lpushx(key, arg), key);
  }

  @Override
  public Long rpushx(final byte[] key, final byte[]... arg) {
    return runBinary((connection) -> connection.rpushx(key, arg), key);
  }

  @Override
  public Long del(final byte[] key) {
    return runBinary((connection) -> connection.del(key), key);
  }

  @Override
  public Long unlink(final byte[] key) {
    return runBinary((connection) -> connection.unlink(key), key);
  }

  @Override
  public Long unlink(final byte[]... keys) {
    return runBinary((connection) -> connection.unlink(keys), keys.length, keys);
  }

  @Override
  public byte[] echo(final byte[] arg) {
    // note that it'll be run from arbitary node
    return runBinary((connection) -> connection.echo(arg), arg);
  }

  @Override
  public Long bitcount(final byte[] key) {
    return runBinary((connection) -> connection.bitcount(key), key);
  }

  @Override
  public Long bitcount(final byte[] key, final long start, final long end) {
    return runBinary((connection) -> connection.bitcount(key, start, end), key);
  }

  @Override
  public Long pfadd(final byte[] key, final byte[]... elements) {
    return runBinary((connection) -> connection.pfadd(key, elements), key);
  }

  @Override
  public long pfcount(final byte[] key) {
    return runBinary((connection) -> connection.pfcount(key), key);
  }

  @Override
  public List<byte[]> srandmember(final byte[] key, final int count) {
    return runBinary((connection) -> connection.srandmember(key, count), key);
  }

  @Override
  public Long zlexcount(final byte[] key, final byte[] min, final byte[] max) {
    return runBinary((connection) -> connection.zlexcount(key, min, max), key);
  }

  @Override
  public Set<byte[]> zrangeByLex(final byte[] key, final byte[] min, final byte[] max) {
    return runBinary((connection) -> connection.zrangeByLex(key, min, max), key);
  }

  @Override
  public Set<byte[]> zrangeByLex(final byte[] key, final byte[] min, final byte[] max,
      final int offset, final int count) {
    return runBinary((connection) -> connection.zrangeByLex(key, min, max, offset, count), key);
  }

  @Override
  public Set<byte[]> zrevrangeByLex(final byte[] key, final byte[] max, final byte[] min) {
    return runBinary((connection) -> connection.zrevrangeByLex(key, max, min), key);
  }

  @Override
  public Set<byte[]> zrevrangeByLex(final byte[] key, final byte[] max, final byte[] min,
      final int offset, final int count) {
    return runBinary((connection) -> connection.zrevrangeByLex(key, max, min, offset, count), key);
  }

  @Override
  public Long zremrangeByLex(final byte[] key, final byte[] min, final byte[] max) {
    return runBinary((connection) -> connection.zremrangeByLex(key, min, max), key);
  }

  @Override
  public Object eval(final byte[] script, final byte[] keyCount, final byte[]... params) {
    return runBinary((connection) -> connection.eval(script, keyCount, params), Integer.parseInt(SafeEncoder.encode(keyCount)), params);
  }

  @Override
  public Object eval(final byte[] script, final int keyCount, final byte[]... params) {
    return runBinary((connection) -> connection.eval(script, keyCount, params), keyCount, params);
  }

  @Override
  public Object eval(final byte[] script, final List<byte[]> keys, final List<byte[]> args) {
    return runBinary((connection) -> connection.eval(script, keys, args), keys.size(), keys.toArray(new byte[keys.size()][]));
  }

  @Override
  public Object eval(final byte[] script, final byte[] sampleKey) {
    return runBinary((connection) -> connection.eval(script), sampleKey);
  }

  @Override
  public Object evalsha(final byte[] sha1, final byte[] sampleKey) {
    return runBinary((connection) -> connection.evalsha(sha1), sampleKey);
  }

  @Override
  public Object evalsha(final byte[] sha1, final List<byte[]> keys, final List<byte[]> args) {
    return runBinary((connection) -> connection.evalsha(sha1, keys, args), keys.size(), keys.toArray(new byte[keys.size()][]));
  }

  @Override
  public Object evalsha(final byte[] sha1, final int keyCount, final byte[]... params) {
    return runBinary((connection) -> connection.evalsha(sha1, keyCount, params), keyCount, params);
  }

  @Override
  public List<Long> scriptExists(final byte[] sampleKey, final byte[]... sha1) {
    return runBinary((connection) -> connection.scriptExists(sha1), sampleKey);
  }

  @Override
  public byte[] scriptLoad(final byte[] script, final byte[] sampleKey) {
    return runBinary((connection) -> connection.scriptLoad(script), sampleKey);
  }

  @Override
  public String scriptFlush(final byte[] sampleKey) {
    return runBinary(BinaryJedis::scriptFlush, sampleKey);
  }

  @Override
  public String scriptKill(final byte[] sampleKey) {
    return runBinary(BinaryJedis::scriptKill, sampleKey);
  }

  @Override
  public Long del(final byte[]... keys) {
    return runBinary((connection) -> connection.del(keys), keys.length, keys);
  }

  @Override
  public List<byte[]> blpop(final int timeout, final byte[]... keys) {
    return runBinary((connection) -> connection.blpop(timeout, keys), keys.length, keys);
  }

  @Override
  public List<byte[]> brpop(final int timeout, final byte[]... keys) {
    return runBinary((connection) -> connection.brpop(timeout, keys), keys.length, keys);
  }

  @Override
  public List<byte[]> mget(final byte[]... keys) {
    return runBinary((connection) -> connection.mget(keys), keys.length, keys);
  }

  @Override
  public String mset(final byte[]... keysvalues) {
    byte[][] keys = new byte[keysvalues.length / 2][];

    for (int keyIdx = 0; keyIdx < keys.length; keyIdx++) {
      keys[keyIdx] = keysvalues[keyIdx * 2];
    }

    return runBinary((connection) -> connection.mset(keysvalues), keys.length, keys);
  }

  @Override
  public Long msetnx(final byte[]... keysvalues) {
    byte[][] keys = new byte[keysvalues.length / 2][];

    for (int keyIdx = 0; keyIdx < keys.length; keyIdx++) {
      keys[keyIdx] = keysvalues[keyIdx * 2];
    }

    return runBinary((connection) -> connection.msetnx(keysvalues), keys.length, keys);
  }

  @Override
  public String rename(final byte[] oldkey, final byte[] newkey) {
    return runBinary((connection) -> connection.rename(oldkey, newkey), 2, oldkey, newkey);
  }

  @Override
  public Long renamenx(final byte[] oldkey, final byte[] newkey) {
    return runBinary((connection) -> connection.renamenx(oldkey, newkey), 2, oldkey, newkey);
  }

  @Override
  public byte[] rpoplpush(final byte[] srckey, final byte[] dstkey) {
    return runBinary((connection) -> connection.rpoplpush(srckey, dstkey), 2, srckey, dstkey);
  }

  @Override
  public Set<byte[]> sdiff(final byte[]... keys) {
    return runBinary((connection) -> connection.sdiff(keys), keys.length, keys);
  }

  @Override
  public Long sdiffstore(final byte[] dstkey, final byte[]... keys) {
    byte[][] wholeKeys = KeyMergeUtil.merge(dstkey, keys);

    return runBinary((connection) -> connection.sdiffstore(dstkey, keys), wholeKeys.length, wholeKeys);
  }

  @Override
  public Set<byte[]> sinter(final byte[]... keys) {
    return runBinary((connection) -> connection.sinter(keys), keys.length, keys);
  }

  @Override
  public Long sinterstore(final byte[] dstkey, final byte[]... keys) {
    byte[][] wholeKeys = KeyMergeUtil.merge(dstkey, keys);

    return runBinary((connection) -> connection.sinterstore(dstkey, keys), wholeKeys.length, wholeKeys);
  }

  @Override
  public Long smove(final byte[] srckey, final byte[] dstkey, final byte[] member) {
    return runBinary((connection) -> connection.smove(srckey, dstkey, member), 2, srckey, dstkey);
  }

  @Override
  public Long sort(final byte[] key, final SortingParams sortingParameters, final byte[] dstkey) {
    return runBinary((connection) -> connection.sort(key, sortingParameters, dstkey), 2, key, dstkey);
  }

  @Override
  public Long sort(final byte[] key, final byte[] dstkey) {
    return runBinary((connection) -> connection.sort(key, dstkey), 2, key, dstkey);
  }

  @Override
  public Set<byte[]> sunion(final byte[]... keys) {
    return runBinary((connection) -> connection.sunion(keys), keys.length, keys);
  }

  @Override
  public Long sunionstore(final byte[] dstkey, final byte[]... keys) {
    byte[][] wholeKeys = KeyMergeUtil.merge(dstkey, keys);

    return runBinary((connection) -> connection.sunionstore(dstkey, keys), wholeKeys.length, wholeKeys);
  }

  @Override
  public Long zinterstore(final byte[] dstkey, final byte[]... sets) {
    byte[][] wholeKeys = KeyMergeUtil.merge(dstkey, sets);

    return runBinary((connection) -> connection.zinterstore(dstkey, sets), wholeKeys.length, wholeKeys);
  }

  @Override
  public Long zinterstore(final byte[] dstkey, final ZParams params, final byte[]... sets) {
    byte[][] wholeKeys = KeyMergeUtil.merge(dstkey, sets);

    return runBinary((connection) -> connection.zinterstore(dstkey, params, sets), wholeKeys.length, wholeKeys);
  }

  @Override
  public Long zunionstore(final byte[] dstkey, final byte[]... sets) {
    byte[][] wholeKeys = KeyMergeUtil.merge(dstkey, sets);

    return runBinary((connection) -> connection.zunionstore(dstkey, sets), wholeKeys.length, wholeKeys);
  }

  @Override
  public Long zunionstore(final byte[] dstkey, final ZParams params, final byte[]... sets) {
    byte[][] wholeKeys = KeyMergeUtil.merge(dstkey, sets);

    return runBinary((connection) -> connection.zunionstore(dstkey, params, sets), wholeKeys.length, wholeKeys);
  }

  @Override
  public byte[] brpoplpush(final byte[] source, final byte[] destination, final int timeout) {
    return runBinary((connection) -> connection.brpoplpush(source, destination, timeout), 2, source, destination);
  }

  @Override
  public Long publish(final byte[] channel, final byte[] message) {
    return retryer.runWithRetries(connectionHandler, (connection) -> connection.publish(channel, message));
  }

  @Override
  public void subscribe(final BinaryJedisPubSub jedisPubSub, final byte[]... channels) {
    retryer.runWithRetries(connectionHandler, (connection) -> {
      connection.subscribe(jedisPubSub, channels);
      return null;
    });
  }

  @Override
  public void psubscribe(final BinaryJedisPubSub jedisPubSub, final byte[]... patterns) {
    retryer.runWithRetries(connectionHandler, (connection) -> {
      connection.psubscribe(jedisPubSub, patterns);
      return null;
    });
  }

  @Override
  public Long bitop(final BitOP op, final byte[] destKey, final byte[]... srcKeys) {
    byte[][] wholeKeys = KeyMergeUtil.merge(destKey, srcKeys);

    return runBinary((connection) -> connection.bitop(op, destKey, srcKeys), wholeKeys.length, wholeKeys);
  }

  @Override
  public String pfmerge(final byte[] destkey, final byte[]... sourcekeys) {
    byte[][] wholeKeys = KeyMergeUtil.merge(destkey, sourcekeys);

    return runBinary((connection) -> connection.pfmerge(destkey, sourcekeys), wholeKeys.length, wholeKeys);
  }

  @Override
  public Long pfcount(final byte[]... keys) {
    return runBinary((connection) -> connection.pfcount(keys), keys.length, keys);
  }

  @Override
  public Long geoadd(final byte[] key, final double longitude, final double latitude,
      final byte[] member) {
    return runBinary((connection) -> connection.geoadd(key, longitude, latitude, member), key);
  }

  @Override
  public Long geoadd(final byte[] key, final Map<byte[], GeoCoordinate> memberCoordinateMap) {
    return runBinary((connection) -> connection.geoadd(key, memberCoordinateMap), key);
  }

  @Override
  public Double geodist(final byte[] key, final byte[] member1, final byte[] member2) {
    return runBinary((connection) -> connection.geodist(key, member1, member2), key);
  }

  @Override
  public Double geodist(final byte[] key, final byte[] member1, final byte[] member2,
      final GeoUnit unit) {
    return runBinary((connection) -> connection.geodist(key, member1, member2, unit), key);
  }

  @Override
  public List<byte[]> geohash(final byte[] key, final byte[]... members) {
    return runBinary((connection) -> connection.geohash(key, members), key);
  }

  @Override
  public List<GeoCoordinate> geopos(final byte[] key, final byte[]... members) {
    return runBinary((connection) -> connection.geopos(key, members), key);
  }

  @Override
  public List<GeoRadiusResponse> georadius(final byte[] key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit) {
    return runBinary((connection) -> connection.georadius(key, longitude, latitude, radius, unit), key);
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(final byte[] key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit) {
    return runBinary((connection) -> connection.georadiusReadonly(key, longitude, latitude, radius, unit), key);
  }

  @Override
  public List<GeoRadiusResponse> georadius(final byte[] key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    return runBinary((connection) -> connection.georadius(key, longitude, latitude, radius, unit, param), key);
  }

  @Override
  public Long georadiusStore(final byte[] key, final double longitude, final double latitude, final double radius,
      final GeoUnit unit, final GeoRadiusParam param, final GeoRadiusStoreParam storeParam) {
    byte[][] keys = storeParam.getByteKeys(key);
    return runBinary((connection) -> connection.georadiusStore(key, longitude, latitude, radius, unit, param, storeParam), keys.length, keys);
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(final byte[] key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    return runBinary((connection) -> connection.georadiusReadonly(key, longitude, latitude, radius, unit, param), key);
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(final byte[] key, final byte[] member,
      final double radius, final GeoUnit unit) {
    return runBinary((connection) -> connection.georadiusByMember(key, member, radius, unit), key);
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(final byte[] key, final byte[] member,
      final double radius, final GeoUnit unit) {
    return runBinary((connection) -> connection.georadiusByMemberReadonly(key, member, radius, unit), key);
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(final byte[] key, final byte[] member,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    return runBinary((connection) -> connection.georadiusByMember(key, member, radius, unit, param), key);
  }

  @Override
  public Long georadiusByMemberStore(final byte[] key, final byte[] member, final double radius, final GeoUnit unit,
      final GeoRadiusParam param, final GeoRadiusStoreParam storeParam) {
    byte[][] keys = storeParam.getByteKeys(key);
    return runBinary((connection) -> connection.georadiusByMemberStore(key, member, radius, unit, param, storeParam), keys.length, keys);
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(final byte[] key, final byte[] member,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    return runBinary((connection) -> connection.georadiusByMemberReadonly(key, member, radius, unit, param), key);
  }

  @Override
  public Set<byte[]> keys(final byte[] pattern) {
    if (pattern == null || pattern.length == 0) {
      throw new IllegalArgumentException(this.getClass().getSimpleName()
          + " only supports KEYS commands with non-empty patterns");
    }
    if (!JedisClusterHashTagUtil.isClusterCompliantMatchPattern(pattern)) {
      throw new IllegalArgumentException(this.getClass().getSimpleName()
          + " only supports KEYS commands with patterns containing hash-tags ( curly-brackets enclosed strings )");
    }
    return runBinary((connection) -> connection.keys(pattern), pattern);
  }

  @Override
  public ScanResult<byte[]> scan(final byte[] cursor, final ScanParams params) {

    byte[] matchPattern;

    if (params == null || (matchPattern = params.binaryMatch()) == null || matchPattern.length == 0) {
      throw new IllegalArgumentException(BinaryJedisCluster.class.getSimpleName()
          + " only supports SCAN commands with non-empty MATCH patterns");
    }

    if (!JedisClusterHashTagUtil.isClusterCompliantMatchPattern(matchPattern)) {
      throw new IllegalArgumentException(BinaryJedisCluster.class.getSimpleName()
          + " only supports SCAN commands with MATCH patterns containing hash-tags ( curly-brackets enclosed strings )");
    }

    return runBinary((connection) -> connection.scan(cursor, params), matchPattern);
  }

  @Override
  public ScanResult<Map.Entry<byte[], byte[]>> hscan(final byte[] key, final byte[] cursor) {
    return runBinary((connection) -> connection.hscan(key, cursor), key);
  }

  @Override
  public ScanResult<Map.Entry<byte[], byte[]>> hscan(final byte[] key, final byte[] cursor,
      final ScanParams params) {
    return runBinary((connection) -> connection.hscan(key, cursor, params), key);
  }

  @Override
  public ScanResult<byte[]> sscan(final byte[] key, final byte[] cursor) {
    return runBinary((connection) -> connection.sscan(key, cursor), key);
  }

  @Override
  public ScanResult<byte[]> sscan(final byte[] key, final byte[] cursor, final ScanParams params) {
    return runBinary((connection) -> connection.sscan(key, cursor, params), key);
  }

  @Override
  public ScanResult<Tuple> zscan(final byte[] key, final byte[] cursor) {
    return runBinary((connection) -> connection.zscan(key, cursor), key);
  }

  @Override
  public ScanResult<Tuple> zscan(final byte[] key, final byte[] cursor, final ScanParams params) {
    return runBinary((connection) -> connection.zscan(key, cursor, params), key);
  }

  @Override
  public List<Long> bitfield(final byte[] key, final byte[]... arguments) {
    return runBinary((connection) -> connection.bitfield(key, arguments), key);
  }

  @Override
  public List<Long> bitfieldReadonly(final byte[] key, final byte[]... arguments) {
    return runBinary((connection) -> connection.bitfieldReadonly(key, arguments), key);
  }

  @Override
  public Long hstrlen(final byte[] key, final byte[] field) {
    return runBinary((connection) -> connection.hstrlen(key, field), key);
  }

  @Override
  public Long memoryUsage(final byte[] key) {
    return runBinary((connection) -> connection.memoryUsage(key), key);
  }

  @Override
  public Long memoryUsage(final byte[] key, final int samples) {
    return runBinary((connection) -> connection.memoryUsage(key, samples), key);
  }

  @Override
  public byte[] xadd(final byte[] key, final byte[] id, final Map<byte[], byte[]> hash, final long maxLen, final boolean approximateLength){
    return runBinary((connection) -> connection.xadd(key, id, hash, maxLen, approximateLength), key);
  }

  @Override
  public Long xlen(final byte[] key) {
    return runBinary((connection) -> connection.xlen(key), key);
  }

  @Override
  public List<byte[]> xrange(final byte[] key, final byte[] start, final byte[] end, final long count) {
    return runBinary((connection) -> connection.xrange(key, start, end, count), key);
  }

  @Override
  public List<byte[]> xrevrange(final byte[] key, final byte[] end, final byte[] start, final int count) {
    return runBinary((connection) -> connection.xrevrange(key, end, start, count), key);
  }

  @Override
  public List<byte[]> xread(final int count, final long block, final Map<byte[], byte[]> streams) {
    byte[][] keys = streams.keySet().toArray(new byte[streams.size()][]);

    return runBinary((connection) -> connection.xread(count, block, streams), keys.length, keys);
  }

  @Override
  public Long xack(final byte[] key, final byte[] group, final byte[]... ids) {
    return runBinary((connection) -> connection.xack(key, group, ids), key);
  }

  @Override
  public String xgroupCreate(final byte[] key, final byte[] consumer, final byte[] id, final boolean makeStream) {
    return runBinary((connection) -> connection.xgroupCreate(key, consumer, id, makeStream), key);
  }

  @Override
  public String xgroupSetID(final byte[] key, final byte[] consumer, final byte[] id) {
    return runBinary((connection) -> connection.xgroupSetID(key, consumer, id), key);
  }

  @Override
  public Long xgroupDestroy(final byte[] key, final byte[] consumer) {
    return runBinary((connection) -> connection.xgroupDestroy(key, consumer), key);
  }

  @Override
  public Long xgroupDelConsumer(final byte[] key, final byte[] consumer, final byte[] consumerName) {
    return runBinary((connection) -> connection.xgroupDelConsumer(key, consumer, consumerName), key);
  }

  @Override
  public   List<byte[]> xreadGroup(final byte[] groupname, final byte[] consumer, final int count, final long block,
      final boolean noAck, final Map<byte[], byte[]> streams){

    byte[][] keys = streams.keySet().toArray(new byte[streams.size()][]);

    return runBinary((connection) -> connection.xreadGroup(groupname, consumer, count, block, noAck, streams), keys.length, keys);
  }

  @Override
  public Long xdel(final byte[] key, final byte[]... ids) {
    return runBinary((connection) -> connection.xdel(key, ids), key);
  }

  @Override
  public Long xtrim(final byte[] key, final long maxLen, final boolean approximateLength) {
    return runBinary((connection) -> connection.xtrim(key, maxLen, approximateLength), key);
  }

  @Override
  public List<byte[]> xpending(final byte[] key, final byte[] groupname, final byte[] start, final byte[] end,
      final int count, final byte[] consumername) {
    return runBinary((connection) -> connection.xpending(key, groupname, start, end, count, consumername), key);
  }

  @Override
  public List<byte[]> xclaim(final byte[] key, final byte[] groupname, final byte[] consumername,
      final long minIdleTime, final long newIdleTime, final int retries, final boolean force, final byte[][] ids) {
    return runBinary((connection) -> connection.xclaim(key, groupname, consumername, minIdleTime, newIdleTime, retries, force, ids), key);
  }

  @Override
  public Long waitReplicas(final byte[] key, final int replicas, final long timeout) {
    return runBinary((connection) -> connection.waitReplicas(replicas, timeout), key);
  }

  public Object sendCommand(final byte[] sampleKey, final ProtocolCommand cmd, final byte[]... args) {
    return runBinary((connection) -> connection.sendCommand(cmd, args), sampleKey);
  }

  public Object sendBlockingCommand(final byte[] sampleKey, final ProtocolCommand cmd, final byte[]... args) {
    return runBinary((connection) -> connection.sendBlockingCommand(cmd, args), sampleKey);
  }
}
