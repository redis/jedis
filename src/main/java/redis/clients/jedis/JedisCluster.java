package redis.clients.jedis;

import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.GeoRadiusStoreParam;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;
import redis.clients.jedis.params.LPosParams;
import redis.clients.jedis.commands.JedisClusterCommands;
import redis.clients.jedis.commands.JedisClusterScriptingCommands;
import redis.clients.jedis.commands.MultiKeyJedisClusterCommands;
import redis.clients.jedis.util.JedisClusterHashTagUtil;
import redis.clients.jedis.util.KeyMergeUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * Interface to a Jedis cluster.
 * <p/>
 * Uses {@link DefaultRetryer}, or you can inject your own using
 * {@link JedisCluster#JedisCluster(Retryer)}.
 *
 * @see BinaryJedisCluster
 */
public class JedisCluster extends BinaryJedisCluster implements JedisClusterCommands,
    MultiKeyJedisClusterCommands, JedisClusterScriptingCommands {

  public JedisCluster(HostAndPort node) {
    this(Collections.singleton(node));
  }

  public JedisCluster(HostAndPort node, int timeout) {
    this(Collections.singleton(node), timeout);
  }

  public JedisCluster(HostAndPort node, int timeout, int maxAttempts) {
    this(Collections.singleton(node), timeout, maxAttempts);
  }

  public JedisCluster(HostAndPort node, final GenericObjectPoolConfig poolConfig) {
    this(Collections.singleton(node), poolConfig);
  }

  public JedisCluster(HostAndPort node, int timeout, final GenericObjectPoolConfig poolConfig) {
    this(Collections.singleton(node), timeout, poolConfig);
  }

  public JedisCluster(HostAndPort node, int timeout, int maxAttempts,
      final GenericObjectPoolConfig poolConfig) {
    this(Collections.singleton(node), timeout, maxAttempts, poolConfig);
  }

  public JedisCluster(HostAndPort node, int connectionTimeout, int soTimeout,
      int maxAttempts, final GenericObjectPoolConfig poolConfig) {
    this(Collections.singleton(node), connectionTimeout, soTimeout, maxAttempts, poolConfig);
  }

  public JedisCluster(HostAndPort node, int connectionTimeout, int soTimeout,
      int maxAttempts, String password, final GenericObjectPoolConfig poolConfig) {
    this(Collections.singleton(node), connectionTimeout, soTimeout, maxAttempts, password, poolConfig);
  }

  public JedisCluster(HostAndPort node, int connectionTimeout, int soTimeout,
      int maxAttempts, String password, String clientName, final GenericObjectPoolConfig poolConfig) {
    this(Collections.singleton(node), connectionTimeout, soTimeout, maxAttempts, password, clientName, poolConfig);
  }

  public JedisCluster(HostAndPort node, int connectionTimeout, int soTimeout,
      int maxAttempts, String user, String password, String clientName, final GenericObjectPoolConfig poolConfig) {
    this(Collections.singleton(node), connectionTimeout, soTimeout, maxAttempts, user, password, clientName, poolConfig);
  }

  public JedisCluster(HostAndPort node, int connectionTimeout, int soTimeout,
      int maxAttempts, String password, String clientName, final GenericObjectPoolConfig poolConfig,
      boolean ssl) {
    this(Collections.singleton(node), connectionTimeout, soTimeout, maxAttempts, password, clientName, poolConfig, ssl);
  }

  public JedisCluster(HostAndPort node, int connectionTimeout, int soTimeout, int maxAttempts,
      String user, String password, String clientName, final GenericObjectPoolConfig poolConfig, boolean ssl) {
    this(Collections.singleton(node), connectionTimeout, soTimeout, maxAttempts, user, password, clientName, poolConfig, ssl);
  }

  public JedisCluster(HostAndPort node, int connectionTimeout, int soTimeout,
      int maxAttempts, String password, String clientName, final GenericObjectPoolConfig poolConfig,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier, JedisClusterHostAndPortMap hostAndPortMap) {
    this(Collections.singleton(node), connectionTimeout, soTimeout, maxAttempts, password, clientName, poolConfig,
        ssl, sslSocketFactory, sslParameters, hostnameVerifier, hostAndPortMap);
  }

  public JedisCluster(HostAndPort node, int connectionTimeout, int soTimeout,
      int maxAttempts, String user, String password, String clientName, final GenericObjectPoolConfig poolConfig,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier, JedisClusterHostAndPortMap hostAndPortMap) {
    this(Collections.singleton(node), connectionTimeout, soTimeout, maxAttempts, user, password, clientName, poolConfig,
            ssl, sslSocketFactory, sslParameters, hostnameVerifier, hostAndPortMap);
  }

  public JedisCluster(Set<HostAndPort> nodes) {
    this(nodes, DEFAULT_TIMEOUT);
  }

  public JedisCluster(Set<HostAndPort> nodes, int timeout) {
    this(nodes, timeout, DEFAULT_MAX_ATTEMPTS);
  }

  public JedisCluster(Set<HostAndPort> nodes, int timeout, int maxAttempts) {
    this(nodes, timeout, maxAttempts, new GenericObjectPoolConfig());
  }

  public JedisCluster(Set<HostAndPort> nodes, final GenericObjectPoolConfig poolConfig) {
    this(nodes, DEFAULT_TIMEOUT, DEFAULT_MAX_ATTEMPTS, poolConfig);
  }

  public JedisCluster(Set<HostAndPort> nodes, int timeout, final GenericObjectPoolConfig poolConfig) {
    this(nodes, timeout, DEFAULT_MAX_ATTEMPTS, poolConfig);
  }

  public JedisCluster(Set<HostAndPort> jedisClusterNode, int timeout, int maxAttempts,
      final GenericObjectPoolConfig poolConfig) {
    super(jedisClusterNode, timeout, maxAttempts, poolConfig);
  }

  public JedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout,
      int maxAttempts, final GenericObjectPoolConfig poolConfig) {
    super(jedisClusterNode, connectionTimeout, soTimeout, maxAttempts, poolConfig);
  }

  public JedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout,
                      int maxAttempts, String password, final GenericObjectPoolConfig poolConfig) {
    super(jedisClusterNode, connectionTimeout, soTimeout, maxAttempts, password, poolConfig);
  }

  public JedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout,
          int maxAttempts, String password, String clientName, final GenericObjectPoolConfig poolConfig) {
    super(jedisClusterNode, connectionTimeout, soTimeout, maxAttempts, password, clientName, poolConfig);
  }

  public JedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout,
          int maxAttempts, String user, String password, String clientName, final GenericObjectPoolConfig poolConfig) {
    super(jedisClusterNode, connectionTimeout, soTimeout, maxAttempts, user, password, clientName, poolConfig);
  }

  public JedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout,
      int infiniteSoTimeout, int maxAttempts, String user, String password, String clientName, final GenericObjectPoolConfig poolConfig) {
    super(jedisClusterNode, connectionTimeout, soTimeout, infiniteSoTimeout, maxAttempts, user, password, clientName, poolConfig);
  }

  public JedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout,
      int maxAttempts, String password, String clientName, final GenericObjectPoolConfig poolConfig,
      boolean ssl) {
    super(jedisClusterNode, connectionTimeout, soTimeout, maxAttempts, password, clientName, poolConfig, ssl);
  }

  public JedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout,
      int maxAttempts, String user, String password, String clientName,
      final GenericObjectPoolConfig poolConfig, boolean ssl) {
    super(jedisClusterNode, connectionTimeout, soTimeout, maxAttempts, user, password, clientName, poolConfig, ssl);
  }

  public JedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout,
      int maxAttempts, String password, String clientName, final GenericObjectPoolConfig poolConfig,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier, JedisClusterHostAndPortMap hostAndPortMap) {
    super(jedisClusterNode, connectionTimeout, soTimeout, maxAttempts, password, clientName, poolConfig,
        ssl, sslSocketFactory, sslParameters, hostnameVerifier, hostAndPortMap);
  }

  public JedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout, int infiniteSoTimeout,
      int maxAttempts, String password, String clientName, final GenericObjectPoolConfig poolConfig,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier, JedisClusterHostAndPortMap hostAndPortMap) {
    this(jedisClusterNode, connectionTimeout, soTimeout, infiniteSoTimeout, maxAttempts, null, password,
        clientName, poolConfig, ssl, sslSocketFactory, sslParameters, hostnameVerifier, hostAndPortMap);
  }

  public JedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout,
      int maxAttempts, String user, String password, String clientName, final GenericObjectPoolConfig poolConfig,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier, JedisClusterHostAndPortMap hostAndPortMap) {
    super(jedisClusterNode, connectionTimeout, soTimeout, maxAttempts, user,password, clientName, poolConfig,
            ssl, sslSocketFactory, sslParameters, hostnameVerifier, hostAndPortMap);
  }

  public JedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout, int infiniteSoTimeout,
      int maxAttempts, String user, String password, String clientName, final GenericObjectPoolConfig poolConfig,
      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
      HostnameVerifier hostnameVerifier, JedisClusterHostAndPortMap hostAndPortMap) {
    super(jedisClusterNode, connectionTimeout, soTimeout, infiniteSoTimeout, maxAttempts, user, password,
        clientName, poolConfig, ssl, sslSocketFactory, sslParameters, hostnameVerifier, hostAndPortMap);
  }

  public JedisCluster(Retryer retryer) {
    super(retryer);
  }

  @Override
  public String set(final String key, final String value) {
    return retryer.run((connection) -> connection.set(key, value), key);
  }

  @Override
  public String set(final String key, final String value, final SetParams params) {
    return retryer.run((connection) -> connection.set(key, value, params), key);
  }

  @Override
  public String get(final String key) {
    return retryer.run((connection) -> connection.get(key), key);
  }

  @Override
  public Boolean exists(final String key) {
    return retryer.run((connection) -> connection.exists(key), key);
  }

  @Override
  public Long exists(final String... keys) {
    return retryer.run((connection) -> connection.exists(keys), keys.length, keys);
  }

  @Override
  public Long persist(final String key) {
    return retryer.run((connection) -> connection.persist(key), key);
  }

  @Override
  public String type(final String key) {
    return retryer.run((connection) -> connection.type(key), key);
  }

  @Override
  public byte[] dump(final String key) {
    return retryer.run((connection) -> connection.dump(key), key);
  }

  @Override
  public String restore(final String key, final int ttl, final byte[] serializedValue) {
    return retryer.run((connection) -> connection.restore(key, ttl, serializedValue), key);
  }

  @Override
  public Long expire(final String key, final int seconds) {
    return retryer.run((connection) -> connection.expire(key, seconds), key);
  }

  @Override
  public Long pexpire(final String key, final long milliseconds) {
    return retryer.run((connection) -> connection.pexpire(key, milliseconds), key);
  }

  @Override
  public Long expireAt(final String key, final long unixTime) {
    return retryer.run((connection) -> connection.expireAt(key, unixTime), key);
  }

  @Override
  public Long pexpireAt(final String key, final long millisecondsTimestamp) {
    return retryer.run((connection) -> connection.pexpireAt(key, millisecondsTimestamp), key);
  }

  @Override
  public Long ttl(final String key) {
    return retryer.run((connection) -> connection.ttl(key), key);
  }

  @Override
  public Long pttl(final String key) {
    return retryer.run((connection) -> connection.pttl(key), key);
  }

  @Override
  public Long touch(final String key) {
    return retryer.run((connection) -> connection.touch(key), key);
  }

  @Override
  public Long touch(final String... keys) {
    return retryer.run((connection) -> connection.touch(keys), keys.length, keys);
  }

  @Override
  public Boolean setbit(final String key, final long offset, final boolean value) {
    return retryer.run((connection) -> connection.setbit(key, offset, value), key);
  }

  @Override
  public Boolean setbit(final String key, final long offset, final String value) {
    return retryer.run((connection) -> connection.setbit(key, offset, value), key);
  }

  @Override
  public Boolean getbit(final String key, final long offset) {
    return retryer.run((connection) -> connection.getbit(key, offset), key);
  }

  @Override
  public Long setrange(final String key, final long offset, final String value) {
    return retryer.run((connection) -> connection.setrange(key, offset, value), key);
  }

  @Override
  public String getrange(final String key, final long startOffset, final long endOffset) {
    return retryer.run((connection) -> connection.getrange(key, startOffset, endOffset), key);
  }

  @Override
  public String getSet(final String key, final String value) {
    return retryer.run((connection) -> connection.getSet(key, value), key);
  }

  @Override
  public Long setnx(final String key, final String value) {
    return retryer.run((connection) -> connection.setnx(key, value), key);
  }

  @Override
  public String setex(final String key, final int seconds, final String value) {
    return retryer.run((connection) -> connection.setex(key, seconds, value), key);
  }

  @Override
  public String psetex(final String key, final long milliseconds, final String value) {
    return retryer.run((connection) -> connection.psetex(key, milliseconds, value), key);
  }

  @Override
  public Long decrBy(final String key, final long decrement) {
    return retryer.run((connection) -> connection.decrBy(key, decrement), key);
  }

  @Override
  public Long decr(final String key) {
    return retryer.run((connection) -> connection.decr(key), key);
  }

  @Override
  public Long incrBy(final String key, final long increment) {
    return retryer.run((connection) -> connection.incrBy(key, increment), key);
  }

  @Override
  public Double incrByFloat(final String key, final double increment) {
    return retryer.run((connection) -> connection.incrByFloat(key, increment), key);
  }

  @Override
  public Long incr(final String key) {
    return retryer.run((connection) -> connection.incr(key), key);
  }

  @Override
  public Long append(final String key, final String value) {
    return retryer.run((connection) -> connection.append(key, value), key);
  }

  @Override
  public String substr(final String key, final int start, final int end) {
    return retryer.run((connection) -> connection.substr(key, start, end), key);
  }

  @Override
  public Long hset(final String key, final String field, final String value) {
    return retryer.run((connection) -> connection.hset(key, field, value), key);
  }

  @Override
  public Long hset(final String key, final Map<String, String> hash) {
    return retryer.run((connection) -> connection.hset(key, hash), key);
  }

  @Override
  public String hget(final String key, final String field) {
    return retryer.run((connection) -> connection.hget(key, field), key);
  }

  @Override
  public Long hsetnx(final String key, final String field, final String value) {
    return retryer.run((connection) -> connection.hsetnx(key, field, value), key);
  }

  @Override
  public String hmset(final String key, final Map<String, String> hash) {
    return retryer.run((connection) -> connection.hmset(key, hash), key);
  }

  @Override
  public List<String> hmget(final String key, final String... fields) {
    return retryer.run((connection) -> connection.hmget(key, fields), key);
  }

  @Override
  public Long hincrBy(final String key, final String field, final long value) {
    return retryer.run((connection) -> connection.hincrBy(key, field, value), key);
  }

  @Override
  public Double hincrByFloat(final String key, final String field, final double value) {
    return retryer.run((connection) -> connection.hincrByFloat(key, field, value), key);
  }

  @Override
  public Boolean hexists(final String key, final String field) {
    return retryer.run((connection) -> connection.hexists(key, field), key);
  }

  @Override
  public Long hdel(final String key, final String... field) {
    return retryer.run((connection) -> connection.hdel(key, field), key);
  }

  @Override
  public Long hlen(final String key) {
    return retryer.run((connection) -> connection.hlen(key), key);
  }

  @Override
  public Set<String> hkeys(final String key) {
    return retryer.run((connection) -> connection.hkeys(key), key);
  }

  @Override
  public List<String> hvals(final String key) {
    return retryer.run((connection) -> connection.hvals(key), key);
  }

  @Override
  public Map<String, String> hgetAll(final String key) {
    return retryer.run((connection) -> connection.hgetAll(key), key);
  }

  @Override
  public Long rpush(final String key, final String... string) {
    return retryer.run((connection) -> connection.rpush(key, string), key);
  }

  @Override
  public Long lpush(final String key, final String... string) {
    return retryer.run((connection) -> connection.lpush(key, string), key);
  }

  @Override
  public Long llen(final String key) {
    return retryer.run((connection) -> connection.llen(key), key);
  }

  @Override
  public List<String> lrange(final String key, final long start, final long stop) {
    return retryer.run((connection) -> connection.lrange(key, start, stop), key);
  }

  @Override
  public String ltrim(final String key, final long start, final long stop) {
    return retryer.run((connection) -> connection.ltrim(key, start, stop), key);
  }

  @Override
  public String lindex(final String key, final long index) {
    return retryer.run((connection) -> connection.lindex(key, index), key);
  }

  @Override
  public String lset(final String key, final long index, final String value) {
    return retryer.run((connection) -> connection.lset(key, index, value), key);
  }

  @Override
  public Long lrem(final String key, final long count, final String value) {
    return retryer.run((connection) -> connection.lrem(key, count, value), key);
  }

  @Override
  public String lpop(final String key) {
    return retryer.run((connection) -> connection.lpop(key), key);
  }

  @Override
  public List<String> lpop(final String key, final int count) {
    return retryer.run((connection) -> connection.lpop(key, count), key);
  }

  @Override
  public Long lpos(final String key, final String element) {
    return retryer.run((connection) -> connection.lpos(key, element), key);
  }

  @Override
  public Long lpos(final String key, final String element, final LPosParams params) {
    return retryer.run((connection) -> connection.lpos(key, element, params), key);
  }

  @Override
  public List<Long> lpos(final String key, final String element, final LPosParams params, final long count) {
    return retryer.run((connection) -> connection.lpos(key, element, params, count), key);
  }

  @Override
  public String rpop(final String key) {
    return retryer.run((connection) -> connection.rpop(key), key);
  }

  @Override
  public List<String> rpop(final String key, final int count) {
    return retryer.run((connection) -> connection.rpop(key, count), key);
  }

  @Override
  public Long sadd(final String key, final String... member) {
    return retryer.run((connection) -> connection.sadd(key, member), key);
  }

  @Override
  public Set<String> smembers(final String key) {
    return retryer.run((connection) -> connection.smembers(key), key);
  }

  @Override
  public Long srem(final String key, final String... member) {
    return retryer.run((connection) -> connection.srem(key, member), key);
  }

  @Override
  public String spop(final String key) {
    return retryer.run((connection) -> connection.spop(key), key);
  }

  @Override
  public Set<String> spop(final String key, final long count) {
    return retryer.run((connection) -> connection.spop(key, count), key);
  }

  @Override
  public Long scard(final String key) {
    return retryer.run((connection) -> connection.scard(key), key);
  }

  @Override
  public Boolean sismember(final String key, final String member) {
    return retryer.run((connection) -> connection.sismember(key, member), key);
  }

  @Override
  public List<Boolean> smismember(final String key, final String... members) {
    return retryer.run((connection) -> connection.smismember(key, members), key);
  }

  @Override
  public String srandmember(final String key) {
    return retryer.run((connection) -> connection.srandmember(key), key);
  }

  @Override
  public List<String> srandmember(final String key, final int count) {
    return retryer.run((connection) -> connection.srandmember(key, count), key);
  }

  @Override
  public Long strlen(final String key) {
    return retryer.run((connection) -> connection.strlen(key), key);
  }

  @Override
  public Long zadd(final String key, final double score, final String member) {
    return retryer.run((connection) -> connection.zadd(key, score, member), key);
  }

  @Override
  public Long zadd(final String key, final double score, final String member,
      final ZAddParams params) {
    return retryer.run((connection) -> connection.zadd(key, score, member, params), key);
  }

  @Override
  public Long zadd(final String key, final Map<String, Double> scoreMembers) {
    return retryer.run((connection) -> connection.zadd(key, scoreMembers), key);
  }

  @Override
  public Long zadd(final String key, final Map<String, Double> scoreMembers, final ZAddParams params) {
    return retryer.run((connection) -> connection.zadd(key, scoreMembers, params), key);
  }

  @Override
  public Set<String> zrange(final String key, final long start, final long stop) {
    return retryer.run((connection) -> connection.zrange(key, start, stop), key);
  }

  @Override
  public Long zrem(final String key, final String... members) {
    return retryer.run((connection) -> connection.zrem(key, members), key);
  }

  @Override
  public Double zincrby(final String key, final double increment, final String member) {
    return retryer.run((connection) -> connection.zincrby(key, increment, member), key);
  }

  @Override
  public Double zincrby(final String key, final double increment, final String member,
      final ZIncrByParams params) {
    return retryer.run((connection) -> connection.zincrby(key, increment, member, params), key);
  }

  @Override
  public Long zrank(final String key, final String member) {
    return retryer.run((connection) -> connection.zrank(key, member), key);
  }

  @Override
  public Long zrevrank(final String key, final String member) {
    return retryer.run((connection) -> connection.zrevrank(key, member), key);
  }

  @Override
  public Set<String> zrevrange(final String key, final long start, final long stop) {
    return retryer.run((connection) -> connection.zrevrange(key, start, stop), key);
  }

  @Override
  public Set<Tuple> zrangeWithScores(final String key, final long start, final long stop) {
    return retryer.run((connection) -> connection.zrangeWithScores(key, start, stop), key);
  }

  @Override
  public Set<Tuple> zrevrangeWithScores(final String key, final long start, final long stop) {
    return retryer.run((connection) -> connection.zrevrangeWithScores(key, start, stop), key);
  }

  @Override
  public Long zcard(final String key) {
    return retryer.run((connection) -> connection.zcard(key), key);
  }

  @Override
  public Double zscore(final String key, final String member) {
    return retryer.run((connection) -> connection.zscore(key, member), key);
  }

  @Override
  public List<Double> zmscore(final String key, final String... members) {
    return retryer.run((connection) -> connection.zmscore(key, members), key);
  }

  @Override
  public Tuple zpopmax(final String key) {
    return retryer.run((connection) -> connection.zpopmax(key), key);
  }

  @Override
  public Set<Tuple> zpopmax(final String key, final int count) {
    return retryer.run((connection) -> connection.zpopmax(key, count), key);
  }

  @Override
  public Tuple zpopmin(final String key) {
    return retryer.run((connection) -> connection.zpopmin(key), key);
  }

  @Override
  public Set<Tuple> zpopmin(final String key, final int count) {
    return retryer.run((connection) -> connection.zpopmin(key, count), key);
  }

  @Override
  public List<String> sort(final String key) {
    return retryer.run((connection) -> connection.sort(key), key);
  }

  @Override
  public List<String> sort(final String key, final SortingParams sortingParameters) {
    return retryer.run((connection) -> connection.sort(key, sortingParameters), key);
  }

  @Override
  public Long zcount(final String key, final double min, final double max) {
    return retryer.run((connection) -> connection.zcount(key, min, max), key);
  }

  @Override
  public Long zcount(final String key, final String min, final String max) {
    return retryer.run((connection) -> connection.zcount(key, min, max), key);
  }

  @Override
  public Set<String> zrangeByScore(final String key, final double min, final double max) {
    return retryer.run((connection) -> connection.zrangeByScore(key, min, max), key);
  }

  @Override
  public Set<String> zrangeByScore(final String key, final String min, final String max) {
    return retryer.run((connection) -> connection.zrangeByScore(key, min, max), key);
  }

  @Override
  public Set<String> zrevrangeByScore(final String key, final double max, final double min) {
    return retryer.run((connection) -> connection.zrevrangeByScore(key, max, min), key);
  }

  @Override
  public Set<String> zrangeByScore(final String key, final double min, final double max,
      final int offset, final int count) {
    return retryer.run((connection) -> connection.zrangeByScore(key, min, max, offset, count), key);
  }

  @Override
  public Set<String> zrevrangeByScore(final String key, final String max, final String min) {
    return retryer.run((connection) -> connection.zrevrangeByScore(key, max, min), key);
  }

  @Override
  public Set<String> zrangeByScore(final String key, final String min, final String max,
      final int offset, final int count) {
    return retryer.run((connection) -> connection.zrangeByScore(key, min, max, offset, count), key);
  }

  @Override
  public Set<String> zrevrangeByScore(final String key, final double max, final double min,
      final int offset, final int count) {
    return retryer.run((connection) -> connection.zrevrangeByScore(key, max, min, offset, count), key);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final String key, final double min, final double max) {
    return retryer.run((connection) -> connection.zrangeByScoreWithScores(key, min, max), key);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final String key, final double max, final double min) {
    return retryer.run((connection) -> connection.zrevrangeByScoreWithScores(key, max, min), key);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final String key, final double min, final double max,
      final int offset, final int count) {
    return retryer.run((connection) -> connection.zrangeByScoreWithScores(key, min, max, offset, count), key);
  }

  @Override
  public Set<String> zrevrangeByScore(final String key, final String max, final String min,
      final int offset, final int count) {
    return retryer.run((connection) -> connection.zrevrangeByScore(key, max, min, offset, count), key);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final String key, final String min, final String max) {
    return retryer.run((connection) -> connection.zrangeByScoreWithScores(key, min, max), key);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final String key, final String max, final String min) {
    return retryer.run((connection) -> connection.zrevrangeByScoreWithScores(key, max, min), key);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final String key, final String min, final String max,
      final int offset, final int count) {
    return retryer.run((connection) -> connection.zrangeByScoreWithScores(key, min, max, offset, count), key);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final String key, final double max,
      final double min, final int offset, final int count) {
    return retryer.run((connection) -> connection.zrevrangeByScoreWithScores(key, max, min, offset, count), key);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final String key, final String max,
      final String min, final int offset, final int count) {
    return retryer.run((connection) -> connection.zrevrangeByScoreWithScores(key, max, min, offset, count), key);
  }

  @Override
  public Long zremrangeByRank(final String key, final long start, final long stop) {
    return retryer.run((connection) -> connection.zremrangeByRank(key, start, stop), key);
  }

  @Override
  public Long zremrangeByScore(final String key, final double min, final double max) {
    return retryer.run((connection) -> connection.zremrangeByScore(key, min, max), key);
  }

  @Override
  public Long zremrangeByScore(final String key, final String min, final String max) {
    return retryer.run((connection) -> connection.zremrangeByScore(key, min, max), key);
  }

  @Override
  public Long zlexcount(final String key, final String min, final String max) {
    return retryer.run((connection) -> connection.zlexcount(key, min, max), key);
  }

  @Override
  public Set<String> zrangeByLex(final String key, final String min, final String max) {
    return retryer.run((connection) -> connection.zrangeByLex(key, min, max), key);
  }

  @Override
  public Set<String> zrangeByLex(final String key, final String min, final String max,
      final int offset, final int count) {
    return retryer.run((connection) -> connection.zrangeByLex(key, min, max, offset, count), key);
  }

  @Override
  public Set<String> zrevrangeByLex(final String key, final String max, final String min) {
    return retryer.run((connection) -> connection.zrevrangeByLex(key, max, min), key);
  }

  @Override
  public Set<String> zrevrangeByLex(final String key, final String max, final String min,
      final int offset, final int count) {
    return retryer.run((connection) -> connection.zrevrangeByLex(key, max, min, offset, count), key);
  }

  @Override
  public Long zremrangeByLex(final String key, final String min, final String max) {
    return retryer.run((connection) -> connection.zremrangeByLex(key, min, max), key);
  }

  @Override
  public Long linsert(final String key, final ListPosition where, final String pivot,
      final String value) {
    return retryer.run((connection) -> connection.linsert(key, where, pivot, value), key);
  }

  @Override
  public Long lpushx(final String key, final String... string) {
    return retryer.run((connection) -> connection.lpushx(key, string), key);
  }

  @Override
  public Long rpushx(final String key, final String... string) {
    return retryer.run((connection) -> connection.rpushx(key, string), key);
  }

  @Override
  public Long del(final String key) {
    return retryer.run((connection) -> connection.del(key), key);
  }

  @Override
  public Long unlink(final String key) {
    return retryer.run((connection) -> connection.unlink(key), key);
  }

  @Override
  public Long unlink(final String... keys) {
    return retryer.run((connection) -> connection.unlink(keys), keys.length, keys);
  }

  @Override
  public String echo(final String string) {
    // note that it'll be run from arbitrary node
    return retryer.run((connection) -> connection.echo(string), string);
  }

  @Override
  public Long bitcount(final String key) {
    return retryer.run((connection) -> connection.bitcount(key), key);
  }

  @Override
  public Long bitcount(final String key, final long start, final long end) {
    return retryer.run((connection) -> connection.bitcount(key, start, end), key);
  }

  @Override
  public Set<String> keys(final String pattern) {
    if (pattern == null || pattern.isEmpty()) {
      throw new IllegalArgumentException(this.getClass().getSimpleName()
          + " only supports KEYS commands with non-empty patterns");
    }
    if (!JedisClusterHashTagUtil.isClusterCompliantMatchPattern(pattern)) {
      throw new IllegalArgumentException(this.getClass().getSimpleName()
          + " only supports KEYS commands with patterns containing hash-tags ( curly-brackets enclosed strings )");
    }
    return retryer.run((connection) -> connection.keys(pattern), pattern);
  }

  @Override
  public ScanResult<String> scan(final String cursor, final ScanParams params) {

    String matchPattern;

    if (params == null || (matchPattern = params.match()) == null || matchPattern.isEmpty()) {
      throw new IllegalArgumentException(JedisCluster.class.getSimpleName()
          + " only supports SCAN commands with non-empty MATCH patterns");
    }

    if (!JedisClusterHashTagUtil.isClusterCompliantMatchPattern(matchPattern)) {
      throw new IllegalArgumentException(JedisCluster.class.getSimpleName()
          + " only supports SCAN commands with MATCH patterns containing hash-tags ( curly-brackets enclosed strings )");
    }

    return retryer.run((connection) -> connection.scan(cursor, params), matchPattern);
  }

  @Override
  public ScanResult<Entry<String, String>> hscan(final String key, final String cursor) {
    return retryer.run((connection) -> connection.hscan(key, cursor), key);
  }

  @Override
  public ScanResult<String> sscan(final String key, final String cursor) {
    return retryer.run((connection) -> connection.sscan(key, cursor), key);
  }

  @Override
  public ScanResult<Tuple> zscan(final String key, final String cursor) {
    return retryer.run((connection) -> connection.zscan(key, cursor), key);
  }

  @Override
  public Long pfadd(final String key, final String... elements) {
    return retryer.run((connection) -> connection.pfadd(key, elements), key);
  }

  @Override
  public long pfcount(final String key) {
    return retryer.run((connection) -> connection.pfcount(key), key);
  }

  @Override
  public List<String> blpop(final int timeout, final String key) {
    return retryer.run((connection) -> connection.blpop(timeout, key), key);
  }

  @Override
  public List<String> brpop(final int timeout, final String key) {
    return retryer.run((connection) -> connection.brpop(timeout, key), key);
  }

  @Override
  public Long del(final String... keys) {
    return retryer.run((connection) -> connection.del(keys), keys.length, keys);
  }

  @Override
  public List<String> blpop(final int timeout, final String... keys) {
    return retryer.run((connection) -> connection.blpop(timeout, keys), keys.length, keys);

  }

  @Override
  public List<String> brpop(final int timeout, final String... keys) {
    return retryer.run((connection) -> connection.brpop(timeout, keys), keys.length, keys);
  }

  @Override
  public List<String> mget(final String... keys) {
    return retryer.run((connection) -> connection.mget(keys), keys.length, keys);
  }

  @Override
  public String mset(final String... keysvalues) {
    String[] keys = new String[keysvalues.length / 2];

    for (int keyIdx = 0; keyIdx < keys.length; keyIdx++) {
      keys[keyIdx] = keysvalues[keyIdx * 2];
    }

    return retryer.run((connection) -> connection.mset(keysvalues), keys.length, keys);
  }

  @Override
  public Long msetnx(final String... keysvalues) {
    String[] keys = new String[keysvalues.length / 2];

    for (int keyIdx = 0; keyIdx < keys.length; keyIdx++) {
      keys[keyIdx] = keysvalues[keyIdx * 2];
    }

    return retryer.run((connection) -> connection.msetnx(keysvalues), keys.length, keys);
  }

  @Override
  public String rename(final String oldkey, final String newkey) {
    return retryer.run((connection) -> connection.rename(oldkey, newkey), 2, oldkey, newkey);
  }

  @Override
  public Long renamenx(final String oldkey, final String newkey) {
    return retryer.run((connection) -> connection.renamenx(oldkey, newkey), 2, oldkey, newkey);
  }

  @Override
  public String rpoplpush(final String srckey, final String dstkey) {
    return retryer.run((connection) -> connection.rpoplpush(srckey, dstkey), 2, srckey, dstkey);
  }

  @Override
  public Set<String> sdiff(final String... keys) {
    return retryer.run((connection) -> connection.sdiff(keys), keys.length, keys);
  }

  @Override
  public Long sdiffstore(final String dstkey, final String... keys) {
    String[] mergedKeys = KeyMergeUtil.merge(dstkey, keys);

    return retryer.run((connection) -> connection.sdiffstore(dstkey, keys), mergedKeys.length, mergedKeys);
  }

  @Override
  public Set<String> sinter(final String... keys) {
    return retryer.run((connection) -> connection.sinter(keys), keys.length, keys);
  }

  @Override
  public Long sinterstore(final String dstkey, final String... keys) {
    String[] mergedKeys = KeyMergeUtil.merge(dstkey, keys);

    return retryer.run((connection) -> connection.sinterstore(dstkey, keys), mergedKeys.length, mergedKeys);
  }

  @Override
  public Long smove(final String srckey, final String dstkey, final String member) {
    return retryer.run((connection) -> connection.smove(srckey, dstkey, member), 2, srckey, dstkey);
  }

  @Override
  public Long sort(final String key, final SortingParams sortingParameters, final String dstkey) {
    return retryer.run((connection) -> connection.sort(key, sortingParameters, dstkey), 2, key, dstkey);
  }

  @Override
  public Long sort(final String key, final String dstkey) {
    return retryer.run((connection) -> connection.sort(key, dstkey), 2, key, dstkey);
  }

  @Override
  public Set<String> sunion(final String... keys) {
    return retryer.run((connection) -> connection.sunion(keys), keys.length, keys);
  }

  @Override
  public Long sunionstore(final String dstkey, final String... keys) {
    String[] wholeKeys = KeyMergeUtil.merge(dstkey, keys);

    return retryer.run((connection) -> connection.sunionstore(dstkey, keys), wholeKeys.length, wholeKeys);
  }

  @Override
  public Long zinterstore(final String dstkey, final String... sets) {
    String[] wholeKeys = KeyMergeUtil.merge(dstkey, sets);

    return retryer.run((connection) -> connection.zinterstore(dstkey, sets), wholeKeys.length, wholeKeys);
  }

  @Override
  public Long zinterstore(final String dstkey, final ZParams params, final String... sets) {
    String[] mergedKeys = KeyMergeUtil.merge(dstkey, sets);

    return retryer.run((connection) -> connection.zinterstore(dstkey, params, sets), mergedKeys.length, mergedKeys);
  }

  @Override
  public Long zunionstore(final String dstkey, final String... sets) {
    String[] mergedKeys = KeyMergeUtil.merge(dstkey, sets);

    return retryer.run((connection) -> connection.zunionstore(dstkey, sets), mergedKeys.length, mergedKeys);
  }

  @Override
  public Long zunionstore(final String dstkey, final ZParams params, final String... sets) {
    String[] mergedKeys = KeyMergeUtil.merge(dstkey, sets);

    return retryer.run((connection) -> connection.zunionstore(dstkey, params, sets), mergedKeys.length, mergedKeys);
  }

  @Override
  public String brpoplpush(final String source, final String destination, final int timeout) {
    return retryer.run((connection) -> connection.brpoplpush(source, destination, timeout), 2, source, destination);
  }

  @Override
  public Long publish(final String channel, final String message) {
    return retryer.runWithRetries((connection) -> connection.publish(channel, message));
  }

  @Override
  public void subscribe(final JedisPubSub jedisPubSub, final String... channels) {
    retryer.runWithRetries((connection) -> {
      connection.subscribe(jedisPubSub, channels);
      return null;
    });
  }

  @Override
  public void psubscribe(final JedisPubSub jedisPubSub, final String... patterns) {
    retryer.runWithRetries((connection) -> {
      connection.psubscribe(jedisPubSub, patterns);
      return null;
    });
  }

  @Override
  public Long bitop(final BitOP op, final String destKey, final String... srcKeys) {
    String[] mergedKeys = KeyMergeUtil.merge(destKey, srcKeys);

    return retryer.run((connection) -> connection.bitop(op, destKey, srcKeys), mergedKeys.length, mergedKeys);
  }

  @Override
  public String pfmerge(final String destkey, final String... sourcekeys) {
    String[] mergedKeys = KeyMergeUtil.merge(destkey, sourcekeys);

    return retryer.run((connection) -> connection.pfmerge(destkey, sourcekeys), mergedKeys.length, mergedKeys);
  }

  @Override
  public long pfcount(final String... keys) {
    return retryer.run((connection) -> connection.pfcount(keys), keys.length, keys);
  }

  @Override
  public Object eval(final String script, final int keyCount, final String... params) {
    return retryer.run((connection) -> connection.eval(script, keyCount, params), keyCount, params);
  }

  @Override
  public Object eval(final String script, final String sampleKey) {
    return retryer.run((connection) -> connection.eval(script), sampleKey);
  }

  @Override
  public Object eval(final String script, final List<String> keys, final List<String> args) {
    return retryer.run((connection) -> connection.eval(script, keys, args), keys.size(), keys.toArray(new String[keys.size()]));
  }

  @Override
  public Object evalsha(final String sha1, final int keyCount, final String... params) {
    return retryer.run((connection) -> connection.evalsha(sha1, keyCount, params), keyCount, params);
  }

  @Override
  public Object evalsha(final String sha1, final List<String> keys, final List<String> args) {
    return retryer.run((connection) -> connection.evalsha(sha1, keys, args), keys.size(), keys.toArray(new String[keys.size()]));
  }

  @Override
  public Object evalsha(final String sha1, final String sampleKey) {
    return retryer.run((connection) -> connection.evalsha(sha1), sampleKey);
  }

  @Override
  public Boolean scriptExists(final String sha1, final String sampleKey) {
    return retryer.run((connection) -> connection.scriptExists(sha1), sampleKey);
  }

  @Override
  public List<Boolean> scriptExists(final String sampleKey, final String... sha1) {
    return retryer.run((connection) -> connection.scriptExists(sha1), sampleKey);
  }

  @Override
  public String scriptLoad(final String script, final String sampleKey) {
    return retryer.run((connection) -> connection.scriptLoad(script), sampleKey);
  }

  @Override
  public String scriptFlush(final String sampleKey) {
    return retryer.run(BinaryJedis::scriptFlush, sampleKey);
  }

  @Override
  public String scriptKill(final String sampleKey) {
    return retryer.run(BinaryJedis::scriptKill, sampleKey);
  }

  @Override
  public Long geoadd(final String key, final double longitude, final double latitude,
      final String member) {
    return retryer.run((connection) -> connection.geoadd(key, longitude, latitude, member), key);
  }

  @Override
  public Long geoadd(final String key, final Map<String, GeoCoordinate> memberCoordinateMap) {
    return retryer.run((connection) -> connection.geoadd(key, memberCoordinateMap), key);
  }

  @Override
  public Double geodist(final String key, final String member1, final String member2) {
    return retryer.run((connection) -> connection.geodist(key, member1, member2), key);
  }

  @Override
  public Double geodist(final String key, final String member1, final String member2,
      final GeoUnit unit) {
    return retryer.run((connection) -> connection.geodist(key, member1, member2, unit), key);
  }

  @Override
  public List<String> geohash(final String key, final String... members) {
    return retryer.run((connection) -> connection.geohash(key, members), key);
  }

  @Override
  public List<GeoCoordinate> geopos(final String key, final String... members) {
    return retryer.run((connection) -> connection.geopos(key, members), key);
  }

  @Override
  public List<GeoRadiusResponse> georadius(final String key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit) {
    return retryer.run((connection) -> connection.georadius(key, longitude, latitude, radius, unit), key);
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(final String key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit) {
    return retryer.run((connection) -> connection.georadiusReadonly(key, longitude, latitude, radius, unit), key);
  }

  @Override
  public List<GeoRadiusResponse> georadius(final String key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    return retryer.run((connection) -> connection.georadius(key, longitude, latitude, radius, unit, param), key);
  }

  @Override
  public Long georadiusStore(final String key, final double longitude, final double latitude,
      final double radius, final GeoUnit unit, final GeoRadiusParam param, final GeoRadiusStoreParam storeParam) {
    String[] keys = storeParam.getStringKeys(key);
    return retryer.run((connection) -> connection.georadiusStore(key, longitude, latitude, radius, unit, param, storeParam), keys.length, keys);
  }

  @Override
  public List<GeoRadiusResponse> georadiusReadonly(final String key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    return retryer.run((connection) -> connection.georadiusReadonly(key, longitude, latitude, radius, unit, param), key);
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(final String key, final String member,
      final double radius, final GeoUnit unit) {
    return retryer.run((connection) -> connection.georadiusByMember(key, member, radius, unit), key);
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(final String key, final String member,
      final double radius, final GeoUnit unit) {
    return retryer.run((connection) -> connection.georadiusByMemberReadonly(key, member, radius, unit), key);
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(final String key, final String member,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    return retryer.run((connection) -> connection.georadiusByMember(key, member, radius, unit, param), key);
  }

  @Override
  public Long georadiusByMemberStore(final String key, final String member, final double radius, final GeoUnit unit,
      final GeoRadiusParam param, final GeoRadiusStoreParam storeParam) {
    String[] keys = storeParam.getStringKeys(key);
    return retryer.run((connection) -> connection.georadiusByMemberStore(key, member, radius, unit, param, storeParam), keys.length, keys);
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMemberReadonly(final String key, final String member,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    return retryer.run((connection) -> connection.georadiusByMemberReadonly(key, member, radius, unit, param), key);
  }

  @Override
  public List<Long> bitfield(final String key, final String... arguments) {
    return retryer.run((connection) -> connection.bitfield(key, arguments), key);
  }

  @Override
  public List<Long> bitfieldReadonly(final String key, final String... arguments) {
    return retryer.run((connection) -> connection.bitfieldReadonly(key, arguments), key);
  }

  @Override
  public Long hstrlen(final String key, final String field) {
    return retryer.run((connection) -> connection.hstrlen(key, field), key);
  }


  @Override
  public Long memoryUsage(final String key) {
    return retryer.run((connection) -> connection.memoryUsage(key), key);
  }

  @Override
  public Long memoryUsage(final String key, final int samples) {
    return retryer.run((connection) -> connection.memoryUsage(key, samples), key);
  }

  @Override
  public StreamEntryID xadd(final String key, final StreamEntryID id, final Map<String, String> hash) {
    return retryer.run((connection) -> connection.xadd(key, id, hash), key);
  }

  @Override
  public StreamEntryID xadd(final String key, final StreamEntryID id, final Map<String, String> hash, final long maxLen, final boolean approximateLength) {
    return retryer.run((connection) -> connection.xadd(key, id, hash, maxLen, approximateLength), key);
  }

  @Override
  public Long xlen(final String key) {
    return retryer.run((connection) -> connection.xlen(key), key);
  }

  @Override
  public List<StreamEntry> xrange(final String key, final StreamEntryID start, final StreamEntryID end, final int count) {
    return retryer.run((connection) -> connection.xrange(key, start, end, count), key);
  }

  @Override
  public List<StreamEntry> xrevrange(final String key, final StreamEntryID end, final StreamEntryID start, final int count) {
    return retryer.run((connection) -> connection.xrevrange(key, end, start, count), key);
  }

  @Override
  public List<Entry<String, List<StreamEntry>>> xread(final int count, final long block, final Entry<String, StreamEntryID>... streams) {
    String[] keys = new String[streams.length];
    for(int i=0; i<streams.length; ++i) {
      keys[i] = streams[i].getKey();
    }

    return retryer.run((connection) -> connection.xread(count, block, streams), keys.length, keys);
  }

  @Override
  public Long xack(final String key, final String group, final StreamEntryID... ids) {
    return retryer.run((connection) -> connection.xack(key, group, ids), key);
  }

  @Override
  public String xgroupCreate(final String key, final String groupname, final StreamEntryID id, final boolean makeStream) {
    return retryer.run((connection) -> connection.xgroupCreate(key, groupname, id, makeStream), key);
  }

  @Override
  public String xgroupSetID(final String key, final String groupname, final StreamEntryID id) {
    return retryer.run((connection) -> connection.xgroupSetID(key, groupname, id), key);
  }

  @Override
  public Long xgroupDestroy(final String key, final String groupname) {
    return retryer.run((connection) -> connection.xgroupDestroy(key, groupname), key);
  }

  @Override
  public Long xgroupDelConsumer(final String key, final String groupname, final String consumername) {
    return retryer.run((connection) -> connection.xgroupDelConsumer(key, groupname, consumername), key);
  }

  @Override
  public List<Entry<String, List<StreamEntry>>> xreadGroup(final String groupname, final String consumer, final int count, final long block,
      final boolean noAck, final Entry<String, StreamEntryID>... streams) {

    String[] keys = new String[streams.length];
    for(int i=0; i<streams.length; ++i) {
      keys[i] = streams[i].getKey();
    }

    return retryer.run((connection) -> connection.xreadGroup(groupname, consumer, count, block, noAck, streams), keys.length, keys);
  }

  @Override
  public List<StreamPendingEntry> xpending(final String key, final String groupname, final StreamEntryID start, final StreamEntryID end, final int count,
      final String consumername) {
    return retryer.run((connection) -> connection.xpending(key, groupname, start, end, count, consumername), key);
  }

  @Override
  public Long xdel(final String key, final StreamEntryID... ids) {
    return retryer.run((connection) -> connection.xdel(key, ids), key);
  }

  @Override
  public Long xtrim(final  String key, final long maxLen, final boolean approximateLength) {
    return retryer.run((connection) -> connection.xtrim(key, maxLen, approximateLength), key);
  }

  @Override
  public List<StreamEntry> xclaim(final String key, final String group, final String consumername, final long minIdleTime, final long newIdleTime,
      final int retries, final boolean force, final StreamEntryID... ids) {
    return retryer.run((connection) -> connection.xclaim(key, group, consumername, minIdleTime, newIdleTime, retries, force, ids), key);
  }

  public Long waitReplicas(final String key, final int replicas, final long timeout) {
    return retryer.run((connection) -> connection.waitReplicas(replicas, timeout), key);
  }

  public Object sendCommand(final String sampleKey, final ProtocolCommand cmd, final String... args) {
    return retryer.run((connection) -> connection.sendCommand(cmd, args), sampleKey);
  }

  public Object sendBlockingCommand(final String sampleKey, final ProtocolCommand cmd, final String... args) {
    return retryer.run((connection) -> connection.sendBlockingCommand(cmd, args), sampleKey);
  }

}
