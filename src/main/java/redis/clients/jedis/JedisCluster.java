package redis.clients.jedis;

import redis.clients.jedis.BinaryClient.LIST_POSITION;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class JedisCluster implements JedisCommands, BasicCommands, Closeable {
  public static final short HASHSLOTS = 16384;
  private static final int DEFAULT_TIMEOUT = 2000;
  private static final int DEFAULT_MAX_REDIRECTIONS = 5;

  public static enum Reset {
    SOFT, HARD
  }

  private int maxRedirections;

  private JedisClusterConnectionHandler connectionHandler;

  public JedisCluster(Set<HostAndPort> nodes, int timeout) {
    this(nodes, timeout, DEFAULT_MAX_REDIRECTIONS);
  }

  public JedisCluster(Set<HostAndPort> nodes) {
    this(nodes, DEFAULT_TIMEOUT);
  }

  public JedisCluster(Set<HostAndPort> nodes, int timeout, int maxRedirections) {
    this(nodes, timeout, maxRedirections, new GenericObjectPoolConfig());
  }

  public JedisCluster(Set<HostAndPort> nodes, final GenericObjectPoolConfig poolConfig) {
    this(nodes, DEFAULT_TIMEOUT, DEFAULT_MAX_REDIRECTIONS, poolConfig);
  }

  public JedisCluster(Set<HostAndPort> nodes, int timeout, final GenericObjectPoolConfig poolConfig) {
    this(nodes, timeout, DEFAULT_MAX_REDIRECTIONS, poolConfig);
  }

  public JedisCluster(Set<HostAndPort> jedisClusterNode, int timeout, int maxRedirections,
      final GenericObjectPoolConfig poolConfig) {
    this.connectionHandler = new JedisSlotBasedConnectionHandler(jedisClusterNode, poolConfig,
        timeout);
    this.maxRedirections = maxRedirections;
  }

  @Override
  public void close() {
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

  @Override
  public String set(final String key, final String value) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.set(key, value);
      }
    }.run(key);
  }

  @Override
  public String set(final String key, final String value, final String nxxx, final String expx,
      final long time) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.set(key, value, nxxx, expx, time);
      }
    }.run(key);
  }

  @Override
  public String get(final String key) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.get(key);
      }
    }.run(key);
  }

  @Override
  public Boolean exists(final String key) {
    return new JedisClusterCommand<Boolean>(connectionHandler, maxRedirections) {
      @Override
      public Boolean execute(Jedis connection) {
        return connection.exists(key);
      }
    }.run(key);
  }

  @Override
  public Long persist(final String key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.persist(key);
      }
    }.run(key);
  }

  @Override
  public String type(final String key) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.type(key);
      }
    }.run(key);
  }

  @Override
  public Long expire(final String key, final int seconds) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.expire(key, seconds);
      }
    }.run(key);
  }

  @Override
  public Long pexpire(final String key, final long milliseconds) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.pexpire(key, milliseconds);
      }
    }.run(key);
  }

  @Override
  public Long expireAt(final String key, final long unixTime) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.expireAt(key, unixTime);
      }
    }.run(key);
  }

  @Override
  public Long pexpireAt(final String key, final long millisecondsTimestamp) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.pexpireAt(key, millisecondsTimestamp);
      }
    }.run(key);
  }

  @Override
  public Long ttl(final String key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.ttl(key);
      }
    }.run(key);
  }

  @Override
  public Boolean setbit(final String key, final long offset, final boolean value) {
    return new JedisClusterCommand<Boolean>(connectionHandler, maxRedirections) {
      @Override
      public Boolean execute(Jedis connection) {
        return connection.setbit(key, offset, value);
      }
    }.run(key);
  }

  @Override
  public Boolean setbit(final String key, final long offset, final String value) {
    return new JedisClusterCommand<Boolean>(connectionHandler, maxRedirections) {
      @Override
      public Boolean execute(Jedis connection) {
        return connection.setbit(key, offset, value);
      }
    }.run(key);
  }

  @Override
  public Boolean getbit(final String key, final long offset) {
    return new JedisClusterCommand<Boolean>(connectionHandler, maxRedirections) {
      @Override
      public Boolean execute(Jedis connection) {
        return connection.getbit(key, offset);
      }
    }.run(key);
  }

  @Override
  public Long setrange(final String key, final long offset, final String value) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.setrange(key, offset, value);
      }
    }.run(key);
  }

  @Override
  public String getrange(final String key, final long startOffset, final long endOffset) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.getrange(key, startOffset, endOffset);
      }
    }.run(key);
  }

  @Override
  public String getSet(final String key, final String value) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.getSet(key, value);
      }
    }.run(key);
  }

  @Override
  public Long setnx(final String key, final String value) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.setnx(key, value);
      }
    }.run(key);
  }

  @Override
  public String setex(final String key, final int seconds, final String value) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.setex(key, seconds, value);
      }
    }.run(key);
  }

  @Override
  public Long decrBy(final String key, final long integer) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.decrBy(key, integer);
      }
    }.run(key);
  }

  @Override
  public Long decr(final String key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.decr(key);
      }
    }.run(key);
  }

  @Override
  public Long incrBy(final String key, final long integer) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.incrBy(key, integer);
      }
    }.run(key);
  }

  @Override
  public Double incrByFloat(final String key, final double value) {
    return new JedisClusterCommand<Double>(connectionHandler, maxRedirections) {
      @Override
      public Double execute(Jedis connection) {
        return connection.incrByFloat(key, value);
      }
    }.run(key);
  }

  @Override
  public Long incr(final String key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.incr(key);
      }
    }.run(key);
  }

  @Override
  public Long append(final String key, final String value) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.append(key, value);
      }
    }.run(key);
  }

  @Override
  public String substr(final String key, final int start, final int end) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.substr(key, start, end);
      }
    }.run(key);
  }

  @Override
  public Long hset(final String key, final String field, final String value) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.hset(key, field, value);
      }
    }.run(key);
  }

  @Override
  public String hget(final String key, final String field) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.hget(key, field);
      }
    }.run(key);
  }

  @Override
  public Long hsetnx(final String key, final String field, final String value) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.hsetnx(key, field, value);
      }
    }.run(key);
  }

  @Override
  public String hmset(final String key, final Map<String, String> hash) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.hmset(key, hash);
      }
    }.run(key);
  }

  @Override
  public List<String> hmget(final String key, final String... fields) {
    return new JedisClusterCommand<List<String>>(connectionHandler, maxRedirections) {
      @Override
      public List<String> execute(Jedis connection) {
        return connection.hmget(key, fields);
      }
    }.run(key);
  }

  @Override
  public Long hincrBy(final String key, final String field, final long value) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.hincrBy(key, field, value);
      }
    }.run(key);
  }

  @Override
  public Boolean hexists(final String key, final String field) {
    return new JedisClusterCommand<Boolean>(connectionHandler, maxRedirections) {
      @Override
      public Boolean execute(Jedis connection) {
        return connection.hexists(key, field);
      }
    }.run(key);
  }

  @Override
  public Long hdel(final String key, final String... field) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.hdel(key, field);
      }
    }.run(key);
  }

  @Override
  public Long hlen(final String key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.hlen(key);
      }
    }.run(key);
  }

  @Override
  public Set<String> hkeys(final String key) {
    return new JedisClusterCommand<Set<String>>(connectionHandler, maxRedirections) {
      @Override
      public Set<String> execute(Jedis connection) {
        return connection.hkeys(key);
      }
    }.run(key);
  }

  @Override
  public List<String> hvals(final String key) {
    return new JedisClusterCommand<List<String>>(connectionHandler, maxRedirections) {
      @Override
      public List<String> execute(Jedis connection) {
        return connection.hvals(key);
      }
    }.run(key);
  }

  @Override
  public Map<String, String> hgetAll(final String key) {
    return new JedisClusterCommand<Map<String, String>>(connectionHandler, maxRedirections) {
      @Override
      public Map<String, String> execute(Jedis connection) {
        return connection.hgetAll(key);
      }
    }.run(key);
  }

  @Override
  public Long rpush(final String key, final String... string) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.rpush(key, string);
      }
    }.run(key);
  }

  @Override
  public Long lpush(final String key, final String... string) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.lpush(key, string);
      }
    }.run(key);
  }

  @Override
  public Long llen(final String key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.llen(key);
      }
    }.run(key);
  }

  @Override
  public List<String> lrange(final String key, final long start, final long end) {
    return new JedisClusterCommand<List<String>>(connectionHandler, maxRedirections) {
      @Override
      public List<String> execute(Jedis connection) {
        return connection.lrange(key, start, end);
      }
    }.run(key);
  }

  @Override
  public String ltrim(final String key, final long start, final long end) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.ltrim(key, start, end);
      }
    }.run(key);
  }

  @Override
  public String lindex(final String key, final long index) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.lindex(key, index);
      }
    }.run(key);
  }

  @Override
  public String lset(final String key, final long index, final String value) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.lset(key, index, value);
      }
    }.run(key);
  }

  @Override
  public Long lrem(final String key, final long count, final String value) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.lrem(key, count, value);
      }
    }.run(key);
  }

  @Override
  public String lpop(final String key) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.lpop(key);
      }
    }.run(key);
  }

  @Override
  public String rpop(final String key) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.rpop(key);
      }
    }.run(key);
  }

  @Override
  public Long sadd(final String key, final String... member) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.sadd(key, member);
      }
    }.run(key);
  }

  @Override
  public Set<String> smembers(final String key) {
    return new JedisClusterCommand<Set<String>>(connectionHandler, maxRedirections) {
      @Override
      public Set<String> execute(Jedis connection) {
        return connection.smembers(key);
      }
    }.run(key);
  }

  @Override
  public Long srem(final String key, final String... member) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.srem(key, member);
      }
    }.run(key);
  }

  @Override
  public String spop(final String key) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.spop(key);
      }
    }.run(key);
  }

  @Override
  public Set<String> spop(final String key, final long count) {
    return new JedisClusterCommand<Set<String>>(connectionHandler, maxRedirections) {
      @Override
      public Set<String> execute(Jedis connection) {
        return connection.spop(key, count);
      }
    }.run(key);
  }

  @Override
  public Long scard(final String key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.scard(key);
      }
    }.run(key);
  }

  @Override
  public Boolean sismember(final String key, final String member) {
    return new JedisClusterCommand<Boolean>(connectionHandler, maxRedirections) {
      @Override
      public Boolean execute(Jedis connection) {
        return connection.sismember(key, member);
      }
    }.run(key);
  }

  @Override
  public String srandmember(final String key) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.srandmember(key);
      }
    }.run(key);
  }

  @Override
  public List<String> srandmember(final String key, final int count) {
    return new JedisClusterCommand<List<String>>(connectionHandler, maxRedirections) {
      @Override
      public List<String> execute(Jedis connection) {
        return connection.srandmember(key, count);
      }
    }.run(key);
  }

  @Override
  public Long strlen(final String key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.strlen(key);
      }
    }.run(key);
  }

  @Override
  public Long zadd(final String key, final double score, final String member) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zadd(key, score, member);
      }
    }.run(key);
  }

  @Override
  public Long zadd(final String key, final Map<String, Double> scoreMembers) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zadd(key, scoreMembers);
      }
    }.run(key);
  }

  @Override
  public Set<String> zrange(final String key, final long start, final long end) {
    return new JedisClusterCommand<Set<String>>(connectionHandler, maxRedirections) {
      @Override
      public Set<String> execute(Jedis connection) {
        return connection.zrange(key, start, end);
      }
    }.run(key);
  }

  @Override
  public Long zrem(final String key, final String... member) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zrem(key, member);
      }
    }.run(key);
  }

  @Override
  public Double zincrby(final String key, final double score, final String member) {
    return new JedisClusterCommand<Double>(connectionHandler, maxRedirections) {
      @Override
      public Double execute(Jedis connection) {
        return connection.zincrby(key, score, member);
      }
    }.run(key);
  }

  @Override
  public Long zrank(final String key, final String member) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zrank(key, member);
      }
    }.run(key);
  }

  @Override
  public Long zrevrank(final String key, final String member) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zrevrank(key, member);
      }
    }.run(key);
  }

  @Override
  public Set<String> zrevrange(final String key, final long start, final long end) {
    return new JedisClusterCommand<Set<String>>(connectionHandler, maxRedirections) {
      @Override
      public Set<String> execute(Jedis connection) {
        return connection.zrevrange(key, start, end);
      }
    }.run(key);
  }

  @Override
  public Set<Tuple> zrangeWithScores(final String key, final long start, final long end) {
    return new JedisClusterCommand<Set<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public Set<Tuple> execute(Jedis connection) {
        return connection.zrangeWithScores(key, start, end);
      }
    }.run(key);
  }

  @Override
  public Set<Tuple> zrevrangeWithScores(final String key, final long start, final long end) {
    return new JedisClusterCommand<Set<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public Set<Tuple> execute(Jedis connection) {
        return connection.zrevrangeWithScores(key, start, end);
      }
    }.run(key);
  }

  @Override
  public Long zcard(final String key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zcard(key);
      }
    }.run(key);
  }

  @Override
  public Double zscore(final String key, final String member) {
    return new JedisClusterCommand<Double>(connectionHandler, maxRedirections) {
      @Override
      public Double execute(Jedis connection) {
        return connection.zscore(key, member);
      }
    }.run(key);
  }

  @Override
  public List<String> sort(final String key) {
    return new JedisClusterCommand<List<String>>(connectionHandler, maxRedirections) {
      @Override
      public List<String> execute(Jedis connection) {
        return connection.sort(key);
      }
    }.run(key);
  }

  @Override
  public List<String> sort(final String key, final SortingParams sortingParameters) {
    return new JedisClusterCommand<List<String>>(connectionHandler, maxRedirections) {
      @Override
      public List<String> execute(Jedis connection) {
        return connection.sort(key, sortingParameters);
      }
    }.run(key);
  }

  @Override
  public Long zcount(final String key, final double min, final double max) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zcount(key, min, max);
      }
    }.run(key);
  }

  @Override
  public Long zcount(final String key, final String min, final String max) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zcount(key, min, max);
      }
    }.run(key);
  }

  @Override
  public Set<String> zrangeByScore(final String key, final double min, final double max) {
    return new JedisClusterCommand<Set<String>>(connectionHandler, maxRedirections) {
      @Override
      public Set<String> execute(Jedis connection) {
        return connection.zrangeByScore(key, min, max);
      }
    }.run(key);
  }

  @Override
  public Set<String> zrangeByScore(final String key, final String min, final String max) {
    return new JedisClusterCommand<Set<String>>(connectionHandler, maxRedirections) {
      @Override
      public Set<String> execute(Jedis connection) {
        return connection.zrangeByScore(key, min, max);
      }
    }.run(key);
  }

  @Override
  public Set<String> zrevrangeByScore(final String key, final double max, final double min) {
    return new JedisClusterCommand<Set<String>>(connectionHandler, maxRedirections) {
      @Override
      public Set<String> execute(Jedis connection) {
        return connection.zrevrangeByScore(key, max, min);
      }
    }.run(key);
  }

  @Override
  public Set<String> zrangeByScore(final String key, final double min, final double max,
      final int offset, final int count) {
    return new JedisClusterCommand<Set<String>>(connectionHandler, maxRedirections) {
      @Override
      public Set<String> execute(Jedis connection) {
        return connection.zrangeByScore(key, min, max, offset, count);
      }
    }.run(key);
  }

  @Override
  public Set<String> zrevrangeByScore(final String key, final String max, final String min) {
    return new JedisClusterCommand<Set<String>>(connectionHandler, maxRedirections) {
      @Override
      public Set<String> execute(Jedis connection) {
        return connection.zrevrangeByScore(key, max, min);
      }
    }.run(key);
  }

  @Override
  public Set<String> zrangeByScore(final String key, final String min, final String max,
      final int offset, final int count) {
    return new JedisClusterCommand<Set<String>>(connectionHandler, maxRedirections) {
      @Override
      public Set<String> execute(Jedis connection) {
        return connection.zrangeByScore(key, min, max, offset, count);
      }
    }.run(key);
  }

  @Override
  public Set<String> zrevrangeByScore(final String key, final double max, final double min,
      final int offset, final int count) {
    return new JedisClusterCommand<Set<String>>(connectionHandler, maxRedirections) {
      @Override
      public Set<String> execute(Jedis connection) {
        return connection.zrevrangeByScore(key, max, min, offset, count);
      }
    }.run(key);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final String key, final double min, final double max) {
    return new JedisClusterCommand<Set<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public Set<Tuple> execute(Jedis connection) {
        return connection.zrangeByScoreWithScores(key, min, max);
      }
    }.run(key);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final String key, final double max, final double min) {
    return new JedisClusterCommand<Set<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public Set<Tuple> execute(Jedis connection) {
        return connection.zrevrangeByScoreWithScores(key, max, min);
      }
    }.run(key);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final String key, final double min, final double max,
      final int offset, final int count) {
    return new JedisClusterCommand<Set<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public Set<Tuple> execute(Jedis connection) {
        return connection.zrangeByScoreWithScores(key, min, max, offset, count);
      }
    }.run(key);
  }

  @Override
  public Set<String> zrevrangeByScore(final String key, final String max, final String min,
      final int offset, final int count) {
    return new JedisClusterCommand<Set<String>>(connectionHandler, maxRedirections) {
      @Override
      public Set<String> execute(Jedis connection) {
        return connection.zrevrangeByScore(key, max, min, offset, count);
      }
    }.run(key);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final String key, final String min, final String max) {
    return new JedisClusterCommand<Set<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public Set<Tuple> execute(Jedis connection) {
        return connection.zrangeByScoreWithScores(key, min, max);
      }
    }.run(key);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final String key, final String max, final String min) {
    return new JedisClusterCommand<Set<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public Set<Tuple> execute(Jedis connection) {
        return connection.zrevrangeByScoreWithScores(key, max, min);
      }
    }.run(key);
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(final String key, final String min, final String max,
      final int offset, final int count) {
    return new JedisClusterCommand<Set<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public Set<Tuple> execute(Jedis connection) {
        return connection.zrangeByScoreWithScores(key, min, max, offset, count);
      }
    }.run(key);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final String key, final double max,
      final double min, final int offset, final int count) {
    return new JedisClusterCommand<Set<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public Set<Tuple> execute(Jedis connection) {
        return connection.zrevrangeByScoreWithScores(key, max, min, offset, count);
      }
    }.run(key);
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(final String key, final String max,
      final String min, final int offset, final int count) {
    return new JedisClusterCommand<Set<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public Set<Tuple> execute(Jedis connection) {
        return connection.zrevrangeByScoreWithScores(key, max, min, offset, count);
      }
    }.run(key);
  }

  @Override
  public Long zremrangeByRank(final String key, final long start, final long end) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zremrangeByRank(key, start, end);
      }
    }.run(key);
  }

  @Override
  public Long zremrangeByScore(final String key, final double start, final double end) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zremrangeByScore(key, start, end);
      }
    }.run(key);
  }

  @Override
  public Long zremrangeByScore(final String key, final String start, final String end) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zremrangeByScore(key, start, end);
      }
    }.run(key);
  }

  @Override
  public Long zlexcount(final String key, final String min, final String max) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zlexcount(key, min, max);
      }
    }.run(key);
  }

  @Override
  public Set<String> zrangeByLex(final String key, final String min, final String max) {
    return new JedisClusterCommand<Set<String>>(connectionHandler, maxRedirections) {
      @Override
      public Set<String> execute(Jedis connection) {
        return connection.zrangeByLex(key, min, max);
      }
    }.run(key);
  }

  @Override
  public Set<String> zrangeByLex(final String key, final String min, final String max,
      final int offset, final int count) {
    return new JedisClusterCommand<Set<String>>(connectionHandler, maxRedirections) {
      @Override
      public Set<String> execute(Jedis connection) {
        return connection.zrangeByLex(key, min, max, offset, count);
      }
    }.run(key);
  }

  @Override
  public Set<String> zrevrangeByLex(final String key, final String max, final String min) {
    return new JedisClusterCommand<Set<String>>(connectionHandler, maxRedirections) {
      @Override
      public Set<String> execute(Jedis connection) {
        return connection.zrevrangeByLex(key, max, min);
      }
    }.run(key);
  }

  @Override
  public Set<String> zrevrangeByLex(final String key, final String max, final String min,
      final int offset, final int count) {
    return new JedisClusterCommand<Set<String>>(connectionHandler, maxRedirections) {
      @Override
      public Set<String> execute(Jedis connection) {
        return connection.zrevrangeByLex(key, max, min, offset, count);
      }
    }.run(key);
  }

  @Override
  public Long zremrangeByLex(final String key, final String min, final String max) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.zremrangeByLex(key, min, max);
      }
    }.run(key);
  }

  @Override
  public Long linsert(final String key, final LIST_POSITION where, final String pivot,
      final String value) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.linsert(key, where, pivot, value);
      }
    }.run(key);
  }

  @Override
  public Long lpushx(final String key, final String... string) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.lpushx(key, string);
      }
    }.run(key);
  }

  @Override
  public Long rpushx(final String key, final String... string) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.rpushx(key, string);
      }
    }.run(key);
  }

  /**
   * @deprecated unusable command, this command will be removed in 3.0.0.
   */
  @Override
  @Deprecated
  public List<String> blpop(final String arg) {
    return new JedisClusterCommand<List<String>>(connectionHandler, maxRedirections) {
      @Override
      public List<String> execute(Jedis connection) {
        return connection.blpop(arg);
      }
    }.run(arg);
  }

  /**
   * @deprecated unusable command, this command will be removed in 3.0.0.
   */
  @Override
  @Deprecated
  public List<String> brpop(final String arg) {
    return new JedisClusterCommand<List<String>>(connectionHandler, maxRedirections) {
      @Override
      public List<String> execute(Jedis connection) {
        return connection.brpop(arg);
      }
    }.run(arg);
  }

  @Override
  public Long del(final String key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.del(key);
      }
    }.run(key);
  }

  @Override
  public String echo(final String string) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.echo(string);
      }
    }.run(null);
  }

  @Override
  public Long move(final String key, final int dbIndex) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.move(key, dbIndex);
      }
    }.run(key);
  }

  @Override
  public Long bitcount(final String key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.bitcount(key);
      }
    }.run(key);
  }

  @Override
  public Long bitcount(final String key, final long start, final long end) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.bitcount(key, start, end);
      }
    }.run(key);
  }

  /**
   * Deprecated, BasicCommands is not fit to JedisCluster, so it'll be removed
   */
  @Deprecated
  @Override
  public String ping() {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.ping();
      }
    }.run(null);
  }

  /**
   * Deprecated, BasicCommands is not fit to JedisCluster, so it'll be removed
   */
  @Deprecated
  @Override
  public String quit() {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.quit();
      }
    }.run(null);
  }

  /**
   * Deprecated, BasicCommands is not fit to JedisCluster, so it'll be removed
   */
  @Deprecated
  @Override
  public String flushDB() {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.flushDB();
      }
    }.run(null);
  }

  /**
   * Deprecated, BasicCommands is not fit to JedisCluster, so it'll be removed
   */
  @Deprecated
  @Override
  public Long dbSize() {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.dbSize();
      }
    }.run(null);
  }

  /**
   * Deprecated, BasicCommands is not fit to JedisCluster, so it'll be removed
   */
  @Deprecated
  @Override
  public String select(final int index) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.select(index);
      }
    }.run(null);
  }

  /**
   * Deprecated, BasicCommands is not fit to JedisCluster, so it'll be removed
   */
  @Deprecated
  @Override
  public String flushAll() {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.flushAll();
      }
    }.run(null);
  }

  /**
   * Deprecated, BasicCommands is not fit to JedisCluster, so it'll be removed
   */
  @Deprecated
  @Override
  public String auth(final String password) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.auth(password);
      }
    }.run(null);
  }

  /**
   * Deprecated, BasicCommands is not fit to JedisCluster, so it'll be removed
   */
  @Deprecated
  @Override
  public String save() {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.save();
      }
    }.run(null);
  }

  /**
   * Deprecated, BasicCommands is not fit to JedisCluster, so it'll be removed
   */
  @Deprecated
  @Override
  public String bgsave() {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.bgsave();
      }
    }.run(null);
  }

  /**
   * Deprecated, BasicCommands is not fit to JedisCluster, so it'll be removed
   */
  @Deprecated
  @Override
  public String bgrewriteaof() {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.bgrewriteaof();
      }
    }.run(null);
  }

  /**
   * Deprecated, BasicCommands is not fit to JedisCluster, so it'll be removed
   */
  @Deprecated
  @Override
  public Long lastsave() {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.lastsave();
      }
    }.run(null);
  }

  /**
   * Deprecated, BasicCommands is not fit to JedisCluster, so it'll be removed
   */
  @Deprecated
  @Override
  public String shutdown() {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.shutdown();
      }
    }.run(null);
  }

  /**
   * Deprecated, BasicCommands is not fit to JedisCluster, so it'll be removed
   */
  @Deprecated
  @Override
  public String info() {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.info();
      }
    }.run(null);
  }

  /**
   * Deprecated, BasicCommands is not fit to JedisCluster, so it'll be removed
   */
  @Deprecated
  @Override
  public String info(final String section) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.info(section);
      }
    }.run(null);
  }

  /**
   * Deprecated, BasicCommands is not fit to JedisCluster, so it'll be removed
   */
  @Deprecated
  @Override
  public String slaveof(final String host, final int port) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.slaveof(host, port);
      }
    }.run(null);
  }

  /**
   * Deprecated, BasicCommands is not fit to JedisCluster, so it'll be removed
   */
  @Deprecated
  @Override
  public String slaveofNoOne() {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.slaveofNoOne();
      }
    }.run(null);
  }

  /**
   * Deprecated, BasicCommands is not fit to JedisCluster, so it'll be removed
   */
  @Deprecated
  @Override
  public Long getDB() {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.getDB();
      }
    }.run(null);
  }

  /**
   * Deprecated, BasicCommands is not fit to JedisCluster, so it'll be removed
   */
  @Deprecated
  @Override
  public String debug(final DebugParams params) {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.debug(params);
      }
    }.run(null);
  }

  /**
   * Deprecated, BasicCommands is not fit to JedisCluster, so it'll be removed
   */
  @Deprecated
  @Override
  public String configResetStat() {
    return new JedisClusterCommand<String>(connectionHandler, maxRedirections) {
      @Override
      public String execute(Jedis connection) {
        return connection.configResetStat();
      }
    }.run(null);
  }

  public Map<String, JedisPool> getClusterNodes() {
    return connectionHandler.getNodes();
  }

  /**
   * Deprecated, BasicCommands is not fit to JedisCluster, so it'll be removed
   */
  @Deprecated
  @Override
  public Long waitReplicas(int replicas, long timeout) {
    // TODO Auto-generated method stub
    return null;
  }

  @Deprecated
  /**
   * This method is deprecated due to bug (scan cursor should be unsigned long)
   * And will be removed on next major release
   * @see https://github.com/xetorthio/jedis/issues/531 
   */
  @Override
  public ScanResult<Entry<String, String>> hscan(final String key, final int cursor) {
    return new JedisClusterCommand<ScanResult<Entry<String, String>>>(connectionHandler,
        maxRedirections) {
      @Override
      public ScanResult<Entry<String, String>> execute(Jedis connection) {
        return connection.hscan(key, cursor);
      }
    }.run(null);
  }

  @Deprecated
  /**
   * This method is deprecated due to bug (scan cursor should be unsigned long)
   * And will be removed on next major release
   * @see https://github.com/xetorthio/jedis/issues/531 
   */
  @Override
  public ScanResult<String> sscan(final String key, final int cursor) {
    return new JedisClusterCommand<ScanResult<String>>(connectionHandler, maxRedirections) {
      @Override
      public ScanResult<String> execute(Jedis connection) {
        return connection.sscan(key, cursor);
      }
    }.run(null);
  }

  @Deprecated
  /**
   * This method is deprecated due to bug (scan cursor should be unsigned long)
   * And will be removed on next major release
   * @see https://github.com/xetorthio/jedis/issues/531 
   */
  @Override
  public ScanResult<Tuple> zscan(final String key, final int cursor) {
    return new JedisClusterCommand<ScanResult<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public ScanResult<Tuple> execute(Jedis connection) {
        return connection.zscan(key, cursor);
      }
    }.run(null);
  }

  @Override
  public ScanResult<Entry<String, String>> hscan(final String key, final String cursor) {
    return new JedisClusterCommand<ScanResult<Entry<String, String>>>(connectionHandler,
        maxRedirections) {
      @Override
      public ScanResult<Entry<String, String>> execute(Jedis connection) {
        return connection.hscan(key, cursor);
      }
    }.run(key);
  }

  @Override
  public ScanResult<String> sscan(final String key, final String cursor) {
    return new JedisClusterCommand<ScanResult<String>>(connectionHandler, maxRedirections) {
      @Override
      public ScanResult<String> execute(Jedis connection) {
        return connection.sscan(key, cursor);
      }
    }.run(key);
  }

  @Override
  public ScanResult<Tuple> zscan(final String key, final String cursor) {
    return new JedisClusterCommand<ScanResult<Tuple>>(connectionHandler, maxRedirections) {
      @Override
      public ScanResult<Tuple> execute(Jedis connection) {
        return connection.zscan(key, cursor);
      }
    }.run(key);
  }

  @Override
  public Long pfadd(final String key, final String... elements) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.pfadd(key, elements);
      }
    }.run(key);
  }

  @Override
  public long pfcount(final String key) {
    return new JedisClusterCommand<Long>(connectionHandler, maxRedirections) {
      @Override
      public Long execute(Jedis connection) {
        return connection.pfcount(key);
      }
    }.run(key);
  }

  @Override
  public List<String> blpop(final int timeout, final String key) {
    return new JedisClusterCommand<List<String>>(connectionHandler, maxRedirections) {
      @Override
      public List<String> execute(Jedis connection) {
        return connection.blpop(timeout, key);
      }
    }.run(key);
  }

  @Override
  public List<String> brpop(final int timeout, final String key) {
    return new JedisClusterCommand<List<String>>(connectionHandler, maxRedirections) {
      @Override
      public List<String> execute(Jedis connection) {
        return connection.brpop(timeout, key);
      }
    }.run(key);
  }

}
