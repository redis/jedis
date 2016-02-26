package redis.clients.jedis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.exceptions.JedisClusterException;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;
import redis.clients.util.KeyMergeUtil;
import redis.clients.util.SafeEncoder;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BinaryJedisCluster implements BasicCommands, BinaryJedisClusterCommands,
    MultiKeyBinaryJedisClusterCommands, JedisClusterBinaryScriptingCommands, Closeable {

  public static final short HASHSLOTS = 16384;
  protected static final int DEFAULT_TIMEOUT = 2000;
  protected static final int DEFAULT_MAX_REDIRECTIONS = 5;

  protected int maxRedirections;

  protected JedisClusterConnectionHandler connectionHandler;

  public BinaryJedisCluster(Set<HostAndPort> nodes, int timeout) {
    this(nodes, timeout, DEFAULT_MAX_REDIRECTIONS, new GenericObjectPoolConfig());
  }

  public BinaryJedisCluster(Set<HostAndPort> nodes) {
    this(nodes, DEFAULT_TIMEOUT);
  }

  public BinaryJedisCluster(Set<HostAndPort> jedisClusterNode, int timeout, int maxRedirections,
      final GenericObjectPoolConfig poolConfig) {
    this.connectionHandler = new JedisSlotBasedConnectionHandler(jedisClusterNode, poolConfig,
        timeout);
    this.maxRedirections = maxRedirections;
  }

  public BinaryJedisCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout,
      int soTimeout, int maxRedirections, final GenericObjectPoolConfig poolConfig) {
    this.connectionHandler = new JedisSlotBasedConnectionHandler(jedisClusterNode, poolConfig,
        connectionTimeout, soTimeout);
    this.maxRedirections = maxRedirections;
  }

  @Override
  public void close() throws IOException {
    if (connectionHandler != null) {
      for (JedisPool pool : connectionHandler.getNodes().values()) {
        try {
          if (pool != null) {
            pool.destroy();
          }
        } catch (Exception e) {
          // pass
        }
      }
    }
  }

  public Map<String, JedisPool> getClusterNodes() {
    return connectionHandler.getNodes();
  }

  @Override
  public String set(final byte[] key, final byte[] value) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.set(key, value);
      }
    }.runBinary(key);
  }

  @Override
  public String set(final byte[] key, final byte[] value, final byte[] nxxx, final byte[] expx,
      final long time) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.set(key, value, nxxx, expx, time);
      }
    }.runBinary(key);
  }

  @Override
  public byte[] get(final byte[] key) {
    return new JedisClusterCommand<byte[]>(connectionHandler, maxRedirections) {
      @Override
      public byte[] execute(Jedis connection) {
        return connection.get(key);
      }
    }.runBinary(key);
  }

  @Override
  public Boolean exists(final byte[] key) {
    return new JedisClusterCommand<Boolean>(connectionHandler, maxRedirections) {
      @Override
      public Boolean execute(Jedis connection) {
        return connection.exists(key);
      }
    }.runBinary(key);
  }

  @Override
  public Long exists(final byte[]... keys) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.exists(keys);
      }
    }.runBinary(keys.length, keys);
  }

  @Override
  public Long persist(final byte[] key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.persist(key);
      }
    }.runBinary(key);
  }

  @Override
  public String type(final byte[] key) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.type(key);
      }
    }.runBinary(key);
  }

  @Override
  public Long expire(final byte[] key, final int seconds) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.expire(key, seconds);
      }
    }.runBinary(key);
  }

  @Override
  public Long pexpire(final byte[] key, final long milliseconds) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.pexpire(key, milliseconds);
      }
    }.runBinary(key);
  }

  @Override
  public Long expireAt(final byte[] key, final long unixTime) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.expireAt(key, unixTime);
      }
    }.runBinary(key);
  }

  @Override
  public Long pexpireAt(final byte[] key, final long millisecondsTimestamp) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.pexpire(key, millisecondsTimestamp);
      }
    }.runBinary(key);
  }

  @Override
  public Long ttl(final byte[] key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.ttl(key);
      }
    }.runBinary(key);
  }

  @Override
  public Boolean setbit(final byte[] key, final long offset, final boolean value) {
    return new JedisClusterCommand<Boolean>(connectionHandler, maxRedirections) {
      @Override
      public Boolean execute(Jedis connection) {
        return connection.setbit(key, offset, value);
      }
    }.runBinary(key);
  }

  @Override
  public Boolean setbit(final byte[] key, final long offset, final byte[] value) {
    return new JedisClusterCommand<Boolean>(connectionHandler, maxRedirections) {
      @Override
      public Boolean execute(Jedis connection) {
        return connection.setbit(key, offset, value);
      }
    }.runBinary(key);
  }

  @Override
  public Boolean getbit(final byte[] key, final long offset) {
    return new JedisClusterCommand<Boolean>(connectionHandler, maxRedirections) {
      @Override
      public Boolean execute(Jedis connection) {
        return connection.getbit(key, offset);
      }
    }.runBinary(key);
  }

  @Override
  public Long setrange(final byte[] key, final long offset, final byte[] value) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.setrange(key, offset, value);
      }
    }.runBinary(key);
  }

  @Override
  public byte[] getrange(final byte[] key, final long startOffset, final long endOffset) {
    return new JedisClusterCommand<byte[]>(connectionHandler, maxRedirections) {
      @Override
      public byte[] execute(Jedis connection) {
        return connection.getrange(key, startOffset, endOffset);
      }
    }.runBinary(key);
  }

  @Override
  public byte[] getSet(final byte[] key, final byte[] value) {
    return new JedisClusterCommand<byte[]>(connectionHandler, maxRedirections) {
      @Override
      public byte[] execute(Jedis connection) {
        return connection.getSet(key, value);
      }
    }.runBinary(key);
  }

  @Override
  public Long setnx(final byte[] key, final byte[] value) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.setnx(key, value);
      }
    }.runBinary(key);
  }

  @Override
  public String setex(final byte[] key, final int seconds, final byte[] value) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.setex(key, seconds, value);
      }
    }.runBinary(key);
  }

  @Override
  public Long decrBy(final byte[] key, final long integer) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.decrBy(key, integer);
      }
    }.runBinary(key);
  }

  @Override
  public Long decr(final byte[] key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.decr(key);
      }
    }.runBinary(key);
  }

  @Override
  public Long incrBy(final byte[] key, final long integer) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.incrBy(key, integer);
      }
    }.runBinary(key);
  }

  @Override
  public Double incrByFloat(final byte[] key, final double value) {
    return new JedisClusterCommand<Double>(connectionHandler, maxRedirections) {
      @Override
      public Double execute(Jedis connection) {
        return connection.incrByFloat(key, value);
      }
    }.runBinary(key);
  }

  @Override
  public Long incr(final byte[] key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.incr(key);
      }
    }.runBinary(key);
  }

  @Override
  public Long append(final byte[] key, final byte[] value) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.append(key, value);
      }
    }.runBinary(key);
  }

  @Override
  public byte[] substr(final byte[] key, final int start, final int end) {
    return new JedisClusterCommand<byte[]>(connectionHandler, maxRedirections) {
      @Override
      public byte[] execute(Jedis connection) {
        return connection.substr(key, start, end);
      }
    }.runBinary(key);
  }

  @Override
  public Long hset(final byte[] key, final byte[] field, final byte[] value) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.hset(key, field, value);
      }
    }.runBinary(key);
  }

  @Override
  public byte[] hget(final byte[] key, final byte[] field) {
    return new JedisClusterCommand<byte[]>(connectionHandler, maxRedirections) {
      @Override
      public byte[] execute(Jedis connection) {
        return connection.hget(key, field);
      }
    }.runBinary(key);
  }

  @Override
  public Long hsetnx(final byte[] key, final byte[] field, final byte[] value) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.hsetnx(key, field, value);
      }
    }.runBinary(key);
  }

  @Override
  public String hmset(final byte[] key, final Map<byte[], byte[]> hash) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.hmset(key, hash);
      }
    }.runBinary(key);
  }

  @Override
  public List<byte[]> hmget(final byte[] key, final byte[]... fields) {
    return new JedisClusterCommand<List<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public List<byte[]> execute(Jedis connection) {
        return connection.hmget(key, fields);
      }
    }.runBinary(key);
  }

  @Override
  public Long hincrBy(final byte[] key, final byte[] field, final long value) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.hincrBy(key, field, value);
      }
    }.runBinary(key);
  }

  @Override
  public Double hincrByFloat(final byte[] key, final byte[] field, final double value) {
    return new JedisClusterCommand<Double>(connectionHandler, maxRedirections) {
      @Override
      public Double execute(Jedis connection) {
        return connection.hincrByFloat(key, field, value);
      }
    }.runBinary(key);
  }

  @Override
  public Boolean hexists(final byte[] key, final byte[] field) {
    return new JedisClusterCommand<Boolean>(connectionHandler, maxRedirections) {
      @Override
      public Boolean execute(Jedis connection) {
        return connection.hexists(key, field);
      }
    }.runBinary(key);
  }

  @Override
  public Long hdel(final byte[] key, final byte[]... field) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.hdel(key, field);
      }
    }.runBinary(key);
  }

  @Override
  public Long hlen(final byte[] key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.hlen(key);
      }
    }.runBinary(key);
  }

  @Override
  public Set<byte[]> hkeys(final byte[] key) {
    return new JedisClusterCommand<Set<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Set<byte[]> execute(Jedis connection) {
        return connection.hkeys(key);
      }
    }.runBinary(key);
  }

  @Override
  public Collection<byte[]> hvals(final byte[] key) {
    return new JedisClusterCommand<Collection<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Collection<byte[]> execute(Jedis connection) {
        return connection.hvals(key);
      }
    }.runBinary(key);
  }

  @Override
  public Map<byte[], byte[]> hgetAll(final byte[] key) {
    return new JedisClusterCommand<Map<byte[], byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Map<byte[], byte[]> execute(Jedis connection) {
        return connection.hgetAll(key);
      }
    }.runBinary(key);
  }

  @Override
  public Long rpush(final byte[] key, final byte[]... args) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.rpush(key, args);
      }
    }.runBinary(key);
  }

  @Override
  public Long lpush(final byte[] key, final byte[]... args) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.lpush(key, args);
      }
    }.runBinary(key);
  }

  @Override
  public Long llen(final byte[] key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.llen(key);
      }
    }.runBinary(key);
  }

  @Override
  public List<byte[]> lrange(final byte[] key, final long start, final long end) {
    return new JedisClusterCommand<List<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public List<byte[]> execute(Jedis connection) {
        return connection.lrange(key, start, end);
      }
    }.runBinary(key);
  }

  @Override
  public String ltrim(final byte[] key, final long start, final long end) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.ltrim(key, start, end);
      }
    }.runBinary(key);
  }

  @Override
  public byte[] lindex(final byte[] key, final long index) {
    return new JedisClusterCommand<byte[]>(connectionHandler, maxRedirections) {
      @Override
      public byte[] execute(Jedis connection) {
        return connection.lindex(key, index);
      }
    }.runBinary(key);
  }

  @Override
  public String lset(final byte[] key, final long index, final byte[] value) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.lset(key, index, value);
      }
    }.runBinary(key);
  }

  @Override
  public Long lrem(final byte[] key, final long count, final byte[] value) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.lrem(key, count, value);
      }
    }.runBinary(key);
  }

  @Override
  public byte[] lpop(final byte[] key) {
    return new JedisClusterCommand<byte[]>(connectionHandler, maxRedirections) {
      @Override
      public byte[] execute(Jedis connection) {
        return connection.lpop(key);
      }
    }.runBinary(key);
  }

  @Override
  public byte[] rpop(final byte[] key) {
    return new JedisClusterCommand<byte[]>(connectionHandler, maxRedirections) {
      @Override
      public byte[] execute(Jedis connection) {
        return connection.rpop(key);
      }
    }.runBinary(key);
  }

  @Override
  public Long sadd(final byte[] key, final byte[]... member) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.sadd(key, member);
      }
    }.runBinary(key);
  }

  @Override
  public Set<byte[]> smembers(final byte[] key) {
    return new JedisClusterCommand<Set<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Set<byte[]> execute(Jedis connection) {
        return connection.smembers(key);
      }
    }.runBinary(key);
  }

  @Override
  public Long srem(final byte[] key, final byte[]... member) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.srem(key, member);
      }
    }.runBinary(key);
  }

  @Override
  public byte[] spop(final byte[] key) {
    return new JedisClusterCommand<byte[]>(connectionHandler, maxRedirections) {
      @Override
      public byte[] execute(Jedis connection) {
        return connection.spop(key);
      }
    }.runBinary(key);
  }

  @Override
  public Set<byte[]> spop(final byte[] key, final long count) {
    return new JedisClusterCommand<Set<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Set<byte[]> execute(Jedis connection) {
        return connection.spop(key, count);
      }
    }.runBinary(key);
  }

  @Override
  public Long scard(final byte[] key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.scard(key);
      }
    }.runBinary(key);
  }

  @Override
  public Boolean sismember(final byte[] key, final byte[] member) {
    return new JedisClusterCommand<Boolean>(connectionHandler, maxRedirections) {
      @Override
      public Boolean execute(Jedis connection) {
        return connection.sismember(key, member);
      }
    }.runBinary(key);
  }

  @Override
  public byte[] srandmember(final byte[] key) {
    return new JedisClusterCommand<byte[]>(connectionHandler, maxRedirections) {
      @Override
      public byte[] execute(Jedis connection) {
        return connection.srandmember(key);
      }
    }.runBinary(key);
  }

  @Override
  public Long strlen(final byte[] key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.strlen(key);
      }
    }.runBinary(key);
  }

  @Override
  public Long zadd(final byte[] key, final double score, final byte[] member) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zadd(key, score, member);
      }
    }.runBinary(key);
  }

  @Override
  public Long zadd(final byte[] key, final Map<byte[], Double> scoreMembers) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zadd(key, scoreMembers);
      }
    }.runBinary(key);
  }

  @Override
  public Long zadd(final byte[] key, final double score, final byte[] member,
      final ZAddParams params) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zadd(key, score, member, params);
      }
    }.runBinary(key);
  }

  @Override
  public Long zadd(final byte[] key, final Map<byte[], Double> scoreMembers, final ZAddParams params) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zadd(key, scoreMembers, params);
      }
    }.runBinary(key);
  }

  @Override
  public Set<byte[]> zrange(final byte[] key, final long start, final long end) {
    return new JedisClusterCommand<Set<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Set<byte[]> execute(Jedis connection) {
        return connection.zrange(key, start, end);
      }
    }.runBinary(key);
  }

  @Override
  public Long zrem(final byte[] key, final byte[]... member) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zrem(key, member);
      }
    }.runBinary(key);
  }

  @Override
  public Double zincrby(final byte[] key, final double score, final byte[] member) {
    return new JedisClusterCommand<Double>(connectionHandler, maxRedirections) {
      @Override
      public Double execute(Jedis connection) {
        return connection.zincrby(key, score, member);
      }
    }.runBinary(key);
  }

  @Override
  public Double zincrby(final byte[] key, final double score, final byte[] member,
      final ZIncrByParams params) {
    return new JedisClusterCommand<Double>(connectionHandler, maxRedirections) {
      @Override
      public Double execute(Jedis connection) {
        return connection.zincrby(key, score, member, params);
      }
    }.runBinary(key);
  }

  @Override
  public Long zrank(final byte[] key, final byte[] member) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zrank(key, member);
      }
    }.runBinary(key);
  }

  @Override
  public Long zrevrank(final byte[] key, final byte[] member) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zrevrank(key, member);
      }
    }.runBinary(key);
  }

  @Override
  public Set<byte[]> zrevrange(final byte[] key, final long start, final long end) {
    return new JedisClusterCommand<Set<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Set<byte[]> execute(Jedis connection) {
        return connection.zrevrange(key, start, end);
      }
    }.runBinary(key);
  }

  @Override
  public Set<Tuple> zrangeWithScores(final byte[] key, final long start, final long end) {
    return new JedisClusterCommand<Set<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public Set<Tuple> execute(Jedis connection) {
        return connection.zrangeWithScores(key, start, end);
      }
    }.runBinary(key);
  }

  @Override
  public Set<Tuple> zrevrangeWithScores(final byte[] key, final long start, final long end) {
    return new JedisClusterCommand<Set<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public Set<Tuple> execute(Jedis connection) {
        return connection.zrevrangeWithScores(key, start, end);
      }
    }.runBinary(key);
  }

  @Override
  public Long zcard(final byte[] key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zcard(key);
      }
    }.runBinary(key);
  }

  @Override
  public Double zscore(final byte[] key, final byte[] member) {
    return new JedisClusterCommand<Double>(connectionHandler, maxRedirections) {
      @Override
      public Double execute(Jedis connection) {
        return connection.zscore(key, member);
      }
    }.runBinary(key);
  }

  @Override
  public List<byte[]> sort(final byte[] key) {
    return new JedisClusterCommand<List<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public List<byte[]> execute(Jedis connection) {
        return connection.sort(key);
      }
    }.runBinary(key);
  }

  @Override
  public List<byte[]> sort(final byte[] key, final SortingParams sortingParameters) {
    return new JedisClusterCommand<List<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public List<byte[]> execute(Jedis connection) {
        return connection.sort(key, sortingParameters);
      }
    }.runBinary(key);
  }

  @Override
  public Long zcount(final byte[] key, final double min, final double max) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zcount(key, min, max);
      }
    }.runBinary(key);
  }

  @Override
  public Long zcount(final byte[] key, final byte[] min, final byte[] max) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zcount(key, min, max);
      }
    }.runBinary(key);
  }

  @Override
  public Set<byte[]> zrangeByScore(final byte[] key, final double min, final double max) {
    return new JedisClusterCommand<Set<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Set<byte[]> execute(Jedis connection) {
        return connection.zrangeByScore(key, min, max);
      }
    }.runBinary(key);
  }

  @Override
  public Set<byte[]> zrangeByScore(final byte[] key, final byte[] min, final byte[] max) {
    return new JedisClusterCommand<Set<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Set<byte[]> execute(Jedis connection) {
        return connection.zrangeByScore(key, min, max);
      }
    }.runBinary(key);
  }

  @Override
  public Set<byte[]> zrevrangeByScore(final byte[] key, final double max, final double min) {
    return new JedisClusterCommand<Set<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Set<byte[]> execute(Jedis connection) {
        return connection.zrevrangeByScore(key, max, min);
      }
    }.runBinary(key);
  }

  @Override
  public Set<byte[]> zrangeByScore(final byte[] key, final double min, final double max,
      final int offset, final int count) {
    return new JedisClusterCommand<Set<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Set<byte[]> execute(Jedis connection) {
        return connection.zrangeByScore(key, min, max, offset, count);
      }
    }.runBinary(key);
  }

  @Override
  public Set<byte[]> zrevrangeByScore(final byte[] key, final byte[] max, final byte[] min) {
    return new JedisClusterCommand<Set<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Set<byte[]> execute(Jedis connection) {
        return connection.zrevrangeByScore(key, max, min);
      }
    }.runBinary(key);
  }

  @Override
  public Set<byte[]> zrangeByScore(final byte[] key, final byte[] min, final byte[] max,
      final int offset, final int count) {
    return new JedisClusterCommand<Set<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Set<byte[]> execute(Jedis connection) {
        return connection.zrangeByScore(key, min, max, offset, count);
      }
    }.runBinary(key);
  }

  @Override
  public Set<byte[]> zrevrangeByScore(final byte[] key, final double max, final double min,
      final int offset, final int count) {
    return new JedisClusterCommand<Set<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Set<byte[]> execute(Jedis connection) {
        return connection.zrevrangeByScore(key, max, min, offset, count);
      }
    }.runBinary(key);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final double min, final double max) {
    return new JedisClusterCommand<Set<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public Set<Tuple> execute(Jedis connection) {
        return connection.zrangeByScoreWithScores(key, min, max);
      }
    }.runBinary(key);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final byte[] key, final double max, final double min) {
    return new JedisClusterCommand<Set<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public Set<Tuple> execute(Jedis connection) {
        return connection.zrevrangeByScoreWithScores(key, max, min);
      }
    }.runBinary(key);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final double min, final double max,
      final int offset, final int count) {
    return new JedisClusterCommand<Set<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public Set<Tuple> execute(Jedis connection) {
        return connection.zrangeByScoreWithScores(key, min, max, offset, count);
      }
    }.runBinary(key);
  }

  @Override
  public Set<byte[]> zrevrangeByScore(final byte[] key, final byte[] max, final byte[] min,
      final int offset, final int count) {
    return new JedisClusterCommand<Set<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Set<byte[]> execute(Jedis connection) {
        return connection.zrevrangeByScore(key, max, min, offset, count);
      }
    }.runBinary(key);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final byte[] min, final byte[] max) {
    return new JedisClusterCommand<Set<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public Set<Tuple> execute(Jedis connection) {
        return connection.zrangeByScoreWithScores(key, min, max);
      }
    }.runBinary(key);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final byte[] key, final byte[] max, final byte[] min) {
    return new JedisClusterCommand<Set<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public Set<Tuple> execute(Jedis connection) {
        return connection.zrevrangeByScoreWithScores(key, max, min);
      }
    }.runBinary(key);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final byte[] min, final byte[] max,
      final int offset, final int count) {
    return new JedisClusterCommand<Set<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public Set<Tuple> execute(Jedis connection) {
        return connection.zrangeByScoreWithScores(key, min, max, offset, count);
      }
    }.runBinary(key);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final byte[] key, final double max,
      final double min, final int offset, final int count) {
    return new JedisClusterCommand<Set<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public Set<Tuple> execute(Jedis connection) {
        return connection.zrevrangeByScoreWithScores(key, max, min, offset, count);
      }
    }.runBinary(key);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final byte[] key, final byte[] max,
      final byte[] min, final int offset, final int count) {
    return new JedisClusterCommand<Set<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public Set<Tuple> execute(Jedis connection) {
        return connection.zrevrangeByScoreWithScores(key, max, min, offset, count);
      }
    }.runBinary(key);
  }

  @Override
  public Long zremrangeByRank(final byte[] key, final long start, final long end) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zremrangeByRank(key, start, end);
      }
    }.runBinary(key);
  }

  @Override
  public Long zremrangeByScore(final byte[] key, final double start, final double end) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zremrangeByScore(key, start, end);
      }
    }.runBinary(key);
  }

  @Override
  public Long zremrangeByScore(final byte[] key, final byte[] start, final byte[] end) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zremrangeByScore(key, start, end);
      }
    }.runBinary(key);
  }

  @Override
  public Long linsert(final byte[] key, final Client.LIST_POSITION where, final byte[] pivot,
      final byte[] value) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.linsert(key, where, pivot, value);
      }
    }.runBinary(key);
  }

  @Override
  public Long lpushx(final byte[] key, final byte[]... arg) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.lpushx(key, arg);
      }
    }.runBinary(key);
  }

  @Override
  public Long rpushx(final byte[] key, final byte[]... arg) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.rpushx(key, arg);
      }
    }.runBinary(key);
  }

  @Override
  public Long del(final byte[] key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.del(key);
      }
    }.runBinary(key);
  }

  @Override
  public byte[] echo(final byte[] arg) {
    // note that it'll be run from arbitary node
    return new JedisClusterCommand<byte[]>(connectionHandler, maxRedirections) {
      @Override
      public byte[] execute(Jedis connection) {
        return connection.echo(arg);
      }
    }.runBinary(arg);
  }

  @Override
  public Long bitcount(final byte[] key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.bitcount(key);
      }
    }.runBinary(key);
  }

  @Override
  public Long bitcount(final byte[] key, final long start, final long end) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.bitcount(key, start, end);
      }
    }.runBinary(key);
  }

  @Override
  public Long pfadd(final byte[] key, final byte[]... elements) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.pfadd(key, elements);
      }
    }.runBinary(key);
  }

  @Override
  public long pfcount(final byte[] key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.pfcount(key);
      }
    }.runBinary(key);
  }

  @Override
  public List<byte[]> srandmember(final byte[] key, final int count) {
    return new JedisClusterCommand<List<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public List<byte[]> execute(Jedis connection) {
        return connection.srandmember(key, count);
      }
    }.runBinary(key);
  }

  @Override
  public Long zlexcount(final byte[] key, final byte[] min, final byte[] max) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zlexcount(key, min, max);
      }
    }.runBinary(key);
  }

  @Override
  public Set<byte[]> zrangeByLex(final byte[] key, final byte[] min, final byte[] max) {
    return new JedisClusterCommand<Set<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Set<byte[]> execute(Jedis connection) {
        return connection.zrangeByLex(key, min, max);
      }
    }.runBinary(key);
  }

  @Override
  public Set<byte[]> zrangeByLex(final byte[] key, final byte[] min, final byte[] max,
      final int offset, final int count) {
    return new JedisClusterCommand<Set<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Set<byte[]> execute(Jedis connection) {
        return connection.zrangeByLex(key, min, max, offset, count);
      }
    }.runBinary(key);
  }

  @Override
  public Set<byte[]> zrevrangeByLex(final byte[] key, final byte[] max, final byte[] min) {
    return new JedisClusterCommand<Set<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Set<byte[]> execute(Jedis connection) {
        return connection.zrevrangeByLex(key, max, min);
      }
    }.runBinary(key);
  }

  @Override
  public Set<byte[]> zrevrangeByLex(final byte[] key, final byte[] max, final byte[] min,
      final int offset, final int count) {
    return new JedisClusterCommand<Set<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Set<byte[]> execute(Jedis connection) {
        return connection.zrevrangeByLex(key, max, min, offset, count);
      }
    }.runBinary(key);
  }

  @Override
  public Long zremrangeByLex(final byte[] key, final byte[] min, final byte[] max) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zremrangeByLex(key, min, max);
      }
    }.runBinary(key);
  }

  @Override
  public Object eval(final byte[] script, final byte[] keyCount, final byte[]... params) {
    return new JedisClusterCommand<Object>(connectionHandler, maxRedirections) {
      @Override
      public Object execute(Jedis connection) {
        return connection.eval(script, keyCount, params);
      }
    }.runBinary(Integer.parseInt(SafeEncoder.encode(keyCount)), params);
  }

  @Override
  public Object eval(final byte[] script, final int keyCount, final byte[]... params) {
    return new JedisClusterCommand<Object>(connectionHandler, maxRedirections) {
      @Override
      public Object execute(Jedis connection) {
        return connection.eval(script, keyCount, params);
      }
    }.runBinary(keyCount, params);
  }

  @Override
  public Object eval(final byte[] script, final List<byte[]> keys, final List<byte[]> args) {
    return new JedisClusterCommand<Object>(connectionHandler, maxRedirections) {
      @Override
      public Object execute(Jedis connection) {
        return connection.eval(script, keys, args);
      }
    }.runBinary(keys.size(), keys.toArray(new byte[keys.size()][]));
  }

  @Override
  public Object eval(final byte[] script, byte[] key) {
    return new JedisClusterCommand<Object>(connectionHandler, maxRedirections) {
      @Override
      public Object execute(Jedis connection) {
        return connection.eval(script);
      }
    }.runBinary(key);
  }

  @Override
  public Object evalsha(final byte[] script, byte[] key) {
    return new JedisClusterCommand<Object>(connectionHandler, maxRedirections) {
      @Override
      public Object execute(Jedis connection) {
        return connection.evalsha(script);
      }
    }.runBinary(key);
  }

  @Override
  public Object evalsha(final byte[] sha1, final List<byte[]> keys, final List<byte[]> args) {
    return new JedisClusterCommand<Object>(connectionHandler, maxRedirections) {
      @Override
      public Object execute(Jedis connection) {
        return connection.evalsha(sha1, keys, args);
      }
    }.runBinary(keys.size(), keys.toArray(new byte[keys.size()][]));
  }

  @Override
  public Object evalsha(final byte[] sha1, final int keyCount, final byte[]... params) {
    return new JedisClusterCommand<Object>(connectionHandler, maxRedirections) {
      @Override
      public Object execute(Jedis connection) {
        return connection.evalsha(sha1, keyCount, params);
      }
    }.runBinary(keyCount, params);
  }

  @Override
  public List<Long> scriptExists(final byte[] key, final byte[][] sha1) {
    return new JedisClusterCommand<List<Long>>(connectionHandler, maxRedirections) {
      @Override
      public List<Long> execute(Jedis connection) {
        return connection.scriptExists(sha1);
      }
    }.runBinary(key);
  }

  @Override
  public byte[] scriptLoad(final byte[] script, final byte[] key) {
    return new JedisClusterCommand<byte[]>(connectionHandler, maxRedirections) {
      @Override
      public byte[] execute(Jedis connection) {
        return connection.scriptLoad(script);
      }
    }.runBinary(key);
  }

  @Override
  public String scriptFlush(final byte[] key) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.scriptFlush();
      }
    }.runBinary(key);
  }

  @Override
  public String scriptKill(byte[] key) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.scriptKill();
      }
    }.runBinary(key);
  }

  @Override
  public Long del(final byte[]... keys) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.del(keys);
      }
    }.runBinary(keys.length, keys);
  }

  @Override
  public List<byte[]> blpop(final int timeout, final byte[]... keys) {
    return new JedisClusterCommand<List<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public List<byte[]> execute(Jedis connection) {
        return connection.blpop(timeout, keys);
      }
    }.runBinary(keys.length, keys);
  }

  @Override
  public List<byte[]> brpop(final int timeout, final byte[]... keys) {
    return new JedisClusterCommand<List<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public List<byte[]> execute(Jedis connection) {
        return connection.brpop(timeout, keys);
      }
    }.runBinary(keys.length, keys);
  }

  @Override
  public List<byte[]> mget(final byte[]... keys) {
    return new JedisClusterCommand<List<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public List<byte[]> execute(Jedis connection) {
        return connection.mget(keys);
      }
    }.runBinary(keys.length, keys);
  }

  @Override
  public String mset(final byte[]... keysvalues) {
    byte[][] keys = new byte[keysvalues.length / 2][];

    for (int keyIdx = 0; keyIdx < keys.length; keyIdx++) {
      keys[keyIdx] = keysvalues[keyIdx * 2];
    }

    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.mset(keysvalues);
      }
    }.runBinary(keys.length, keys);
  }

  @Override
  public Long msetnx(final byte[]... keysvalues) {
    byte[][] keys = new byte[keysvalues.length / 2][];

    for (int keyIdx = 0; keyIdx < keys.length; keyIdx++) {
      keys[keyIdx] = keysvalues[keyIdx * 2];
    }

    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.msetnx(keysvalues);
      }
    }.runBinary(keys.length, keys);
  }

  @Override
  public String rename(final byte[] oldkey, final byte[] newkey) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.rename(oldkey, newkey);
      }
    }.runBinary(2, oldkey, newkey);
  }

  @Override
  public Long renamenx(final byte[] oldkey, final byte[] newkey) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.renamenx(oldkey, newkey);
      }
    }.runBinary(2, oldkey, newkey);
  }

  @Override
  public byte[] rpoplpush(final byte[] srckey, final byte[] dstkey) {
    return new JedisClusterCommand<byte[]>(connectionHandler, maxRedirections) {
      @Override
      public byte[] execute(Jedis connection) {
        return connection.rpoplpush(srckey, dstkey);
      }
    }.runBinary(2, srckey, dstkey);
  }

  @Override
  public Set<byte[]> sdiff(final byte[]... keys) {
    return new JedisClusterCommand<Set<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Set<byte[]> execute(Jedis connection) {
        return connection.sdiff(keys);
      }
    }.runBinary(keys.length, keys);
  }

  @Override
  public Long sdiffstore(final byte[] dstkey, final byte[]... keys) {
    byte[][] wholeKeys = KeyMergeUtil.merge(dstkey, keys);

    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.sdiffstore(dstkey, keys);
      }
    }.runBinary(wholeKeys.length, wholeKeys);
  }

  @Override
  public Set<byte[]> sinter(final byte[]... keys) {
    return new JedisClusterCommand<Set<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Set<byte[]> execute(Jedis connection) {
        return connection.sinter(keys);
      }
    }.runBinary(keys.length, keys);
  }

  @Override
  public Long sinterstore(final byte[] dstkey, final byte[]... keys) {
    byte[][] wholeKeys = KeyMergeUtil.merge(dstkey, keys);

    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.sinterstore(dstkey, keys);
      }
    }.runBinary(wholeKeys.length, wholeKeys);
  }

  @Override
  public Long smove(final byte[] srckey, final byte[] dstkey, final byte[] member) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.smove(srckey, dstkey, member);
      }
    }.runBinary(2, srckey, dstkey);
  }

  @Override
  public Long sort(final byte[] key, final SortingParams sortingParameters, final byte[] dstkey) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.sort(key, sortingParameters, dstkey);
      }
    }.runBinary(2, key, dstkey);
  }

  @Override
  public Long sort(final byte[] key, final byte[] dstkey) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.sort(key, dstkey);
      }
    }.runBinary(2, key, dstkey);
  }

  @Override
  public Set<byte[]> sunion(final byte[]... keys) {
    return new JedisClusterCommand<Set<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public Set<byte[]> execute(Jedis connection) {
        return connection.sunion(keys);
      }
    }.runBinary(keys.length, keys);
  }

  @Override
  public Long sunionstore(final byte[] dstkey, final byte[]... keys) {
    byte[][] wholeKeys = KeyMergeUtil.merge(dstkey, keys);

    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.sunionstore(dstkey, keys);
      }
    }.runBinary(wholeKeys.length, wholeKeys);
  }

  @Override
  public Long zinterstore(final byte[] dstkey, final byte[]... sets) {
    byte[][] wholeKeys = KeyMergeUtil.merge(dstkey, sets);

    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zinterstore(dstkey, sets);
      }
    }.runBinary(wholeKeys.length, wholeKeys);
  }

  @Override
  public Long zinterstore(final byte[] dstkey, final ZParams params, final byte[]... sets) {
    byte[][] wholeKeys = KeyMergeUtil.merge(dstkey, sets);

    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zinterstore(dstkey, params, sets);
      }
    }.runBinary(wholeKeys.length, wholeKeys);
  }

  @Override
  public Long zunionstore(final byte[] dstkey, final byte[]... sets) {
    byte[][] wholeKeys = KeyMergeUtil.merge(dstkey, sets);

    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zunionstore(dstkey, sets);
      }
    }.runBinary(wholeKeys.length, wholeKeys);
  }

  @Override
  public Long zunionstore(final byte[] dstkey, final ZParams params, final byte[]... sets) {
    byte[][] wholeKeys = KeyMergeUtil.merge(dstkey, sets);

    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zunionstore(dstkey, params, sets);
      }
    }.runBinary(wholeKeys.length, wholeKeys);
  }

  @Override
  public byte[] brpoplpush(final byte[] source, final byte[] destination, final int timeout) {
    return new JedisClusterCommand<byte[]>(connectionHandler, maxRedirections) {
      @Override
      public byte[] execute(Jedis connection) {
        return connection.brpoplpush(source, destination, timeout);
      }
    }.runBinary(2, source, destination);
  }

  @Override
  public Long publish(final byte[] channel, final byte[] message) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.publish(channel, message);
      }
    }.runWithAnyNode();
  }

  @Override
  public void subscribe(final BinaryJedisPubSub jedisPubSub, final byte[]... channels) {
    new JedisClusterCommand<Integer>(connectionHandler, maxRedirections) {
      @Override
      public Integer execute(Jedis connection) {
        connection.subscribe(jedisPubSub, channels);
        return 0;
      }
    }.runWithAnyNode();
  }

  @Override
  public void psubscribe(final BinaryJedisPubSub jedisPubSub, final byte[]... patterns) {
    new JedisClusterCommand<Integer>(connectionHandler, maxRedirections) {
      @Override
      public Integer execute(Jedis connection) {
        connection.subscribe(jedisPubSub, patterns);
        return 0;
      }
    }.runWithAnyNode();
  }

  @Override
  public Long bitop(final BitOP op, final byte[] destKey, final byte[]... srcKeys) {
    byte[][] wholeKeys = KeyMergeUtil.merge(destKey, srcKeys);

    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.bitop(op, destKey, srcKeys);
      }
    }.runBinary(wholeKeys.length, wholeKeys);
  }

  @Override
  public String pfmerge(final byte[] destkey, final byte[]... sourcekeys) {
    byte[][] wholeKeys = KeyMergeUtil.merge(destkey, sourcekeys);

    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.pfmerge(destkey, sourcekeys);
      }
    }.runBinary(wholeKeys.length, wholeKeys);
  }

  @Override
  public Long pfcount(final byte[]... keys) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.pfcount(keys);
      }
    }.runBinary(keys.length, keys);
  }

  /*
   * below methods will be removed at 3.0
   */

  /**
   * @deprecated No key operation doesn't make sense for Redis Cluster scheduled to be removed on
   *             next major release
   */
  @Deprecated
  @Override
  public String ping() {
    throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
  }

  /**
   * @deprecated No key operation doesn't make sense for Redis Cluster scheduled to be removed on
   *             next major release
   */
  @Deprecated
  @Override
  public String quit() {
    throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
  }

  /**
   * @deprecated No key operation doesn't make sense for Redis Cluster scheduled to be removed on
   *             next major release
   */
  @Deprecated
  @Override
  public String flushDB() {
    throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
  }

  /**
   * @deprecated No key operation doesn't make sense for Redis Cluster and Redis Cluster only uses
   *             db index 0 scheduled to be removed on next major release
   */
  @Deprecated
  @Override
  public Long dbSize() {
    throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
  }

  /**
   * @deprecated No key operation doesn't make sense for Redis Cluster and Redis Cluster only uses
   *             db index 0 scheduled to be removed on next major release
   */
  @Deprecated
  @Override
  public String select(int index) {
    throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
  }

  /**
   * @deprecated No key operation doesn't make sense for Redis Cluster scheduled to be removed on
   *             next major release
   */
  @Deprecated
  @Override
  public String flushAll() {
    throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
  }

  /**
   * @deprecated No key operation doesn't make sense for Redis Cluster and Redis Cluster doesn't
   *             support authorization scheduled to be removed on next major release
   */
  @Deprecated
  @Override
  public String auth(String password) {
    throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
  }

  /**
   * @deprecated No key operation doesn't make sense for Redis Cluster scheduled to be removed on
   *             next major release
   */
  @Deprecated
  @Override
  public String save() {
    throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
  }

  /**
   * @deprecated No key operation doesn't make sense for Redis Cluster scheduled to be removed on
   *             next major release
   */
  @Deprecated
  @Override
  public String bgsave() {
    throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
  }

  /**
   * @deprecated No key operation doesn't make sense for Redis Cluster scheduled to be removed on
   *             next major release
   */
  @Deprecated
  @Override
  public String bgrewriteaof() {
    throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
  }

  /**
   * @deprecated No key operation doesn't make sense for Redis Cluster scheduled to be removed on
   *             next major release
   */
  @Deprecated
  @Override
  public Long lastsave() {
    throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
  }

  /**
   * @deprecated No key operation doesn't make sense for Redis Cluster scheduled to be removed on
   *             next major release
   */
  @Deprecated
  @Override
  public String shutdown() {
    throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
  }

  /**
   * @deprecated No key operation doesn't make sense for Redis Cluster scheduled to be removed on
   *             next major release
   */
  @Deprecated
  @Override
  public String info() {
    throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
  }

  /**
   * @deprecated No key operation doesn't make sense for Redis Cluster scheduled to be removed on
   *             next major release
   */
  @Deprecated
  @Override
  public String info(String section) {
    throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
  }

  /**
   * @deprecated No key operation doesn't make sense for Redis Cluster scheduled to be removed on
   *             next major release
   */
  @Deprecated
  @Override
  public String slaveof(String host, int port) {
    throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
  }

  /**
   * @deprecated No key operation doesn't make sense for Redis Cluster scheduled to be removed on
   *             next major release
   */
  @Deprecated
  @Override
  public String slaveofNoOne() {
    throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
  }

  /**
   * @deprecated No key operation doesn't make sense for Redis Cluster and Redis Cluster only uses
   *             db index 0 scheduled to be removed on next major release
   */
  @Deprecated
  @Override
  public Long getDB() {
    throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
  }

  /**
   * @deprecated No key operation doesn't make sense for Redis Cluster scheduled to be removed on
   *             next major release
   */
  @Deprecated
  @Override
  public String debug(DebugParams params) {
    throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
  }

  /**
   * @deprecated No key operation doesn't make sense for Redis Cluster scheduled to be removed on
   *             next major release
   */
  @Deprecated
  @Override
  public String configResetStat() {
    throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
  }

  /**
   * @deprecated No key operation doesn't make sense for Redis Cluster scheduled to be removed on
   *             next major release
   */
  @Deprecated
  @Override
  public Long waitReplicas(int replicas, long timeout) {
    throw new JedisClusterException("No way to dispatch this command to Redis Cluster.");
  }

  @Override
  public Long geoadd(final byte[] key, final double longitude, final double latitude,
      final byte[] member) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.geoadd(key, longitude, latitude, member);
      }
    }.runBinary(key);
  }

  @Override
  public Long geoadd(final byte[] key, final Map<byte[], GeoCoordinate> memberCoordinateMap) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.geoadd(key, memberCoordinateMap);
      }
    }.runBinary(key);
  }

  @Override
  public Double geodist(final byte[] key, final byte[] member1, final byte[] member2) {
    return new JedisClusterCommand<Double>(connectionHandler, maxRedirections) {
      @Override
      public Double execute(Jedis connection) {
        return connection.geodist(key, member1, member2);
      }
    }.runBinary(key);
  }

  @Override
  public Double geodist(final byte[] key, final byte[] member1, final byte[] member2,
      final GeoUnit unit) {
    return new JedisClusterCommand<Double>(connectionHandler, maxRedirections) {
      @Override
      public Double execute(Jedis connection) {
        return connection.geodist(key, member1, member2, unit);
      }
    }.runBinary(key);
  }

  @Override
  public List<byte[]> geohash(final byte[] key, final byte[]... members) {
    return new JedisClusterCommand<List<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public List<byte[]> execute(Jedis connection) {
        return connection.geohash(key, members);
      }
    }.runBinary(key);
  }

  @Override
  public List<GeoCoordinate> geopos(final byte[] key, final byte[]... members) {
    return new JedisClusterCommand<List<GeoCoordinate>>(connectionHandler, maxRedirections) {
      @Override
      public List<GeoCoordinate> execute(Jedis connection) {
        return connection.geopos(key, members);
      }
    }.runBinary(key);
  }

  @Override
  public List<GeoRadiusResponse> georadius(final byte[] key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit) {
    return new JedisClusterCommand<List<GeoRadiusResponse>>(connectionHandler, maxRedirections) {
      @Override
      public List<GeoRadiusResponse> execute(Jedis connection) {
        return connection.georadius(key, longitude, latitude, radius, unit);
      }
    }.runBinary(key);
  }

  @Override
  public List<GeoRadiusResponse> georadius(final byte[] key, final double longitude,
      final double latitude, final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    return new JedisClusterCommand<List<GeoRadiusResponse>>(connectionHandler, maxRedirections) {
      @Override
      public List<GeoRadiusResponse> execute(Jedis connection) {
        return connection.georadius(key, longitude, latitude, radius, unit, param);
      }
    }.runBinary(key);
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(final byte[] key, final byte[] member,
      final double radius, final GeoUnit unit) {
    return new JedisClusterCommand<List<GeoRadiusResponse>>(connectionHandler, maxRedirections) {
      @Override
      public List<GeoRadiusResponse> execute(Jedis connection) {
        return connection.georadiusByMember(key, member, radius, unit);
      }
    }.runBinary(key);
  }

  @Override
  public List<GeoRadiusResponse> georadiusByMember(final byte[] key, final byte[] member,
      final double radius, final GeoUnit unit, final GeoRadiusParam param) {
    return new JedisClusterCommand<List<GeoRadiusResponse>>(connectionHandler, maxRedirections) {
      @Override
      public List<GeoRadiusResponse> execute(Jedis connection) {
        return connection.georadiusByMember(key, member, radius, unit, param);
      }
    }.runBinary(key);
  }

  @Override
  public ScanResult<Map.Entry<byte[], byte[]>> hscan(final byte[] key, final byte[] cursor) {
    return new JedisClusterCommand<ScanResult<Map.Entry<byte[], byte[]>>>(connectionHandler,
        maxRedirections) {
      @Override
      public ScanResult<Map.Entry<byte[], byte[]>> execute(Jedis connection) {
        return connection.hscan(key, cursor);
      }
    }.runBinary(key);
  }

  @Override
  public ScanResult<Map.Entry<byte[], byte[]>> hscan(final byte[] key, final byte[] cursor,
      final ScanParams params) {
    return new JedisClusterCommand<ScanResult<Map.Entry<byte[], byte[]>>>(connectionHandler,
        maxRedirections) {
      @Override
      public ScanResult<Map.Entry<byte[], byte[]>> execute(Jedis connection) {
        return connection.hscan(key, cursor, params);
      }
    }.runBinary(key);
  }

  @Override
  public ScanResult<byte[]> sscan(final byte[] key, final byte[] cursor) {
    return new JedisClusterCommand<ScanResult<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public ScanResult<byte[]> execute(Jedis connection) {
        return connection.sscan(key, cursor);
      }
    }.runBinary(key);
  }

  @Override
  public ScanResult<byte[]> sscan(final byte[] key, final byte[] cursor, final ScanParams params) {
    return new JedisClusterCommand<ScanResult<byte[]>>(connectionHandler, maxRedirections) {
      @Override
      public ScanResult<byte[]> execute(Jedis connection) {
        return connection.sscan(key, cursor, params);
      }
    }.runBinary(key);
  }

  @Override
  public ScanResult<Tuple> zscan(final byte[] key, final byte[] cursor) {
    return new JedisClusterCommand<ScanResult<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public ScanResult<Tuple> execute(Jedis connection) {
        return connection.zscan(key, cursor);
      }
    }.runBinary(key);
  }

  @Override
  public ScanResult<Tuple> zscan(final byte[] key, final byte[] cursor, final ScanParams params) {
    return new JedisClusterCommand<ScanResult<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public ScanResult<Tuple> execute(Jedis connection) {
        return connection.zscan(key, cursor, params);
      }
    }.runBinary(key);
  }
}