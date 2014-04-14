package redis.clients.jedis;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BinaryJedisCluster implements BinaryJedisCommands, BasicCommands,
        JedisClusterBinaryScriptingCommands {
    
    public static final short HASHSLOTS = 16384;
    protected static final int DEFAULT_TIMEOUT = 1;
    protected static final int DEFAULT_MAX_REDIRECTIONS = 5;

    protected int timeout;
    protected int maxRedirections;

    protected JedisClusterConnectionHandler connectionHandler;
    
    public BinaryJedisCluster(Set<HostAndPort> nodes, int timeout) {
        this(nodes, timeout, DEFAULT_MAX_REDIRECTIONS);
        }

    public BinaryJedisCluster(Set<HostAndPort> nodes) {
    this(nodes, DEFAULT_TIMEOUT);
    }

    public BinaryJedisCluster(Set<HostAndPort> jedisClusterNode, int timeout,
        int maxRedirections) {
    this.connectionHandler = new JedisSlotBasedConnectionHandler(
        jedisClusterNode);
    this.timeout = timeout;
    this.maxRedirections = maxRedirections;
    }
    
    @Override
    public String set(final byte[] key, final byte[] value) {
        return new JedisClusterCommand<String>(connectionHandler, timeout,
        maxRedirections) {
            @Override
            public String execute(Jedis connection) {
            return connection.set(key, value);
            }
        }.runBinary(key);
    }
 
    @Override
    public byte[] get(final byte[] key) {
        return new JedisClusterCommand<byte[]>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public byte[] execute(Jedis connection) {
                return connection.get(key);
            }
        }.runBinary(key);
    }
    
    @Override
    public Boolean exists(final byte[] key) {
        return new JedisClusterCommand<Boolean>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Boolean execute(Jedis connection) {
                return connection.exists(key);
            }
        }.runBinary(key);
    }

    @Override
    public Long persist(final byte[] key) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.persist(key);
            }
        }.runBinary(key);
    }

    @Override
    public String type(final byte[] key) {
        return new JedisClusterCommand<String>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public String execute(Jedis connection) {
                return connection.type(key);
            }
        }.runBinary(key);
    }

    @Override
    public Long expire(final byte[] key, final int seconds) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.expire(key, seconds);
            }
        }.runBinary(key);
    }

    @Override
    public Long expireAt(final byte[] key, final long unixTime) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.expireAt(key, unixTime);
            }
        }.runBinary(key);
    }

    @Override
    public Long ttl(final byte[] key) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.ttl(key);
            }
        }.runBinary(key);
    }

    @Override
    public Boolean setbit(final byte[] key, final long offset, final boolean value) {
        return new JedisClusterCommand<Boolean>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Boolean execute(Jedis connection) {
                return connection.setbit(key, offset, value);
            }
        }.runBinary(key);
    }
    
    @Override
    public Boolean setbit(final byte[] key, final long offset, final byte[] value) {
        return new JedisClusterCommand<Boolean>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Boolean execute(Jedis connection) {
                return connection.setbit(key, offset, value);
            }
        }.runBinary(key);
    }

    @Override
    public Boolean getbit(final byte[] key, final long offset) {
        return new JedisClusterCommand<Boolean>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Boolean execute(Jedis connection) {
                return connection.getbit(key, offset);
            }
        }.runBinary(key);
    }

    @Override
    public Long setrange(final byte[] key, final long offset, final byte[] value) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.setrange(key, offset, value);
            }
        }.runBinary(key);
    }

    @Override
    public byte[] getrange(final byte[] key, final long startOffset, final long endOffset) {
        return new JedisClusterCommand<byte[]>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public byte[] execute(Jedis connection) {
                return connection.getrange(key, startOffset, endOffset);
            }
        }.runBinary(key);
    }

    @Override
    public byte[] getSet(final byte[] key, final byte[] value) {
        return new JedisClusterCommand<byte[]>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public byte[] execute(Jedis connection) {
                return connection.getSet(key, value);
            }
        }.runBinary(key);
    }

    @Override
    public Long setnx(final byte[] key, final byte[] value) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.setnx(key, value);
            }
        }.runBinary(key);
    }

    @Override
    public String setex(final byte[] key, final int seconds, final byte[] value) {
        return new JedisClusterCommand<String>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public String execute(Jedis connection) {
                return connection.setex(key, seconds, value);
            }
        }.runBinary(key);
    }

    @Override
    public Long decrBy(final byte[] key, final long integer) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.decrBy(key, integer);
            }
        }.runBinary(key);
    }

    @Override
    public Long decr(final byte[] key) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.decr(key);
            }
        }.runBinary(key);
    }

    @Override
    public Long incrBy(final byte[] key, final long integer) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.incrBy(key, integer);
            }
        }.runBinary(key);
    }

    @Override
    public Long incr(final byte[] key) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.incr(key);
            }
        }.runBinary(key);
    }

    @Override
    public Long append(final byte[] key, final byte[] value) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.append(key, value);
            }
        }.runBinary(key);
    }

    @Override
    public byte[] substr(final byte[] key, final int start, final int end) {
        return new JedisClusterCommand<byte[]>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public byte[] execute(Jedis connection) {
                return connection.substr(key, start, end);
            }
        }.runBinary(key);
    }

    @Override
    public Long hset(final byte[] key, final byte[] field, final byte[] value) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.hset(key, field, value);
            }
        }.runBinary(key);
    }

    @Override
    public byte[] hget(final byte[] key, final byte[] field) {
        return new JedisClusterCommand<byte[]>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public byte[] execute(Jedis connection) {
                return connection.hget(key, field);
            }
        }.runBinary(key);
    }

    @Override
    public Long hsetnx(final byte[] key, final byte[] field, final byte[] value) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.hsetnx(key, field, value);
            }
        }.runBinary(key);
    }

    @Override
    public String hmset(final byte[] key, final Map<byte[], byte[]> hash) {
        return new JedisClusterCommand<String>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public String execute(Jedis connection) {
                return connection.hmset(key, hash);
            }
        }.runBinary(key);
    }

    @Override
    public List<byte[]> hmget(final byte[] key, final byte[]... fields) {
        return new JedisClusterCommand<List<byte[]>>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public List<byte[]> execute(Jedis connection) {
                return connection.hmget(key, fields);
            }
        }.runBinary(key);
    }

    @Override
    public Long hincrBy(final byte[] key, final byte[] field, final long value) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.hincrBy(key, field, value);
            }
        }.runBinary(key);
    }

    @Override
    public Boolean hexists(final byte[] key, final byte[] field) {
        return new JedisClusterCommand<Boolean>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Boolean execute(Jedis connection) {
                return connection.hexists(key, field);
            }
        }.runBinary(key);
    }

    @Override
    public Long hdel(final byte[] key, final byte[]... field) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.hdel(key, field);
            }
        }.runBinary(key);
    }

    @Override
    public Long hlen(final byte[] key) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.hlen(key);
            }
        }.runBinary(key);
    }

    @Override
    public Set<byte[]> hkeys(final byte[] key) {
        return new JedisClusterCommand<Set<byte[]>>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Set<byte[]> execute(Jedis connection) {
                return connection.hkeys(key);
            }
        }.runBinary(key);
    }

    @Override
    public Collection<byte[]> hvals(final byte[] key) {
        return new JedisClusterCommand<Collection<byte[]>>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Collection<byte[]> execute(Jedis connection) {
                return connection.hvals(key);
            }
        }.runBinary(key);
    }

    @Override
    public Map<byte[], byte[]> hgetAll(final byte[] key) {
        return new JedisClusterCommand<Map<byte[], byte[]>>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Map<byte[], byte[]> execute(Jedis connection) {
                return connection.hgetAll(key);
            }
        }.runBinary(key);
    }

    @Override
    public Long rpush(final byte[] key, final byte[]... args) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.rpush(key, args);
            }
        }.runBinary(key);
    }

    @Override
    public Long lpush(final byte[] key, final byte[]... args) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.lpush(key, args);
            }
        }.runBinary(key);
    }

    @Override
    public Long llen(final byte[] key) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.llen(key);
            }
        }.runBinary(key);
    }

    @Override
    public List<byte[]> lrange(final byte[] key, final long start, final long end) {
        return new JedisClusterCommand<List<byte[]>>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public List<byte[]> execute(Jedis connection) {
                return connection.lrange(key, start, end);
            }
        }.runBinary(key);
    }

    @Override
    public String ltrim(final byte[] key, final long start, final long end) {
        return new JedisClusterCommand<String>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public String execute(Jedis connection) {
                return connection.ltrim(key, start, end);
            }
        }.runBinary(key);
    }

    @Override
    public byte[] lindex(final byte[] key, final long index) {
        return new JedisClusterCommand<byte[]>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public byte[] execute(Jedis connection) {
                return connection.lindex(key, index);
            }
        }.runBinary(key);
    }

    @Override
    public String lset(final byte[] key, final long index, final byte[] value) {
        return new JedisClusterCommand<String>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public String execute(Jedis connection) {
                return connection.lset(key, index, value);
            }
        }.runBinary(key);
    }

    @Override
    public Long lrem(final byte[] key, final long count, final byte[] value) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.lrem(key, count, value);
            }
        }.runBinary(key);
    }

    @Override
    public byte[] lpop(final byte[] key) {
        return new JedisClusterCommand<byte[]>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public byte[] execute(Jedis connection) {
                return connection.lpop(key);
            }
        }.runBinary(key);
    }

    @Override
    public byte[] rpop(final byte[] key) {
        return new JedisClusterCommand<byte[]>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public byte[] execute(Jedis connection) {
                return connection.rpop(key);
            }
        }.runBinary(key);
    }

    @Override
    public Long sadd(final byte[] key, final byte[]... member) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.sadd(key, member);
            }
        }.runBinary(key);
    }

    @Override
    public Set<byte[]> smembers(final byte[] key) {
        return new JedisClusterCommand<Set<byte[]>>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Set<byte[]> execute(Jedis connection) {
                return connection.smembers(key);
            }
        }.runBinary(key);
    }

    @Override
    public Long srem(final byte[] key, final byte[]... member) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.srem(key, member);
            }
        }.runBinary(key);
    }

    @Override
    public byte[] spop(final byte[] key) {
        return new JedisClusterCommand<byte[]>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public byte[] execute(Jedis connection) {
                return connection.spop(key);
            }
        }.runBinary(key);
    }

    @Override
    public Long scard(final byte[] key) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.scard(key);
            }
        }.runBinary(key);
    }

    @Override
    public Boolean sismember(final byte[] key, final byte[] member) {
        return new JedisClusterCommand<Boolean>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Boolean execute(Jedis connection) {
                return connection.sismember(key, member);
            }
        }.runBinary(key);
    }

    @Override
    public byte[] srandmember(final byte[] key) {
        return new JedisClusterCommand<byte[]>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public byte[] execute(Jedis connection) {
                return connection.srandmember(key);
            }
        }.runBinary(key);
    }

    @Override
    public Long strlen(final byte[] key) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.strlen(key);
            }
        }.runBinary(key);
    }

    @Override
    public Long zadd(final byte[] key, final double score, final byte[] member) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.zadd(key, score, member);
            }
        }.runBinary(key);
    }

    @Override
    public Long zadd(final byte[] key, final Map<byte[], Double> scoreMembers) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.zadd(key, scoreMembers);
            }
        }.runBinary(key);
    }

    @Override
    public Set<byte[]> zrange(final byte[] key, final long start, final long end) {
        return new JedisClusterCommand<Set<byte[]>>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Set<byte[]> execute(Jedis connection) {
                return connection.zrange(key, start, end);
            }
        }.runBinary(key);
    }

    @Override
    public Long zrem(final byte[] key, final byte[]... member) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.zrem(key, member);
            }
        }.runBinary(key);
    }

    @Override
    public Double zincrby(final byte[] key, final double score, final byte[] member) {
        return new JedisClusterCommand<Double>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Double execute(Jedis connection) {
                return connection.zincrby(key, score, member);
            }
        }.runBinary(key);
    }

    @Override
    public Long zrank(final byte[] key, final byte[] member) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.zrank(key, member);
            }
        }.runBinary(key);
    }

    @Override
    public Long zrevrank(final byte[] key, final byte[] member) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.zrevrank(key, member);
            }
        }.runBinary(key);
    }

    @Override
    public Set<byte[]> zrevrange(final byte[] key, final long start, final long end) {
        return new JedisClusterCommand<Set<byte[]>>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Set<byte[]> execute(Jedis connection) {
                return connection.zrevrange(key, start, end);
            }
        }.runBinary(key);
    }

    @Override
    public Set<Tuple> zrangeWithScores(final byte[] key, final long start, final long end) {
        return new JedisClusterCommand<Set<Tuple>>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Set<Tuple> execute(Jedis connection) {
                return connection.zrangeWithScores(key, start, end);
            }
        }.runBinary(key);
    }

    @Override
    public Set<Tuple> zrevrangeWithScores(final byte[] key, final long start, final long end) {
        return new JedisClusterCommand<Set<Tuple>>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Set<Tuple> execute(Jedis connection) {
                return connection.zrevrangeWithScores(key, start, end);
            }
        }.runBinary(key);
    }

    @Override
    public Long zcard(final byte[] key) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.zcard(key);
            }
        }.runBinary(key);
    }

    @Override
    public Double zscore(final byte[] key, final byte[] member) {
        return new JedisClusterCommand<Double>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Double execute(Jedis connection) {
                return connection.zscore(key, member);
            }
        }.runBinary(key);
    }

    @Override
    public List<byte[]> sort(final byte[] key) {
        return new JedisClusterCommand<List<byte[]>>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public List<byte[]> execute(Jedis connection) {
                return connection.sort(key);
            }
        }.runBinary(key);
    }

    
    @Override
    public List<byte[]> sort(final byte[] key, final SortingParams sortingParameters) {
        return new JedisClusterCommand<List<byte[]>>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public List<byte[]> execute(Jedis connection) {
                return connection.sort(key, sortingParameters);
            }
        }.runBinary(key);
    }

    @Override
    public Long zcount(final byte[] key, final double min, final double max) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.zcount(key, min, max);
            }
        }.runBinary(key);
    }

    @Override
    public Long zcount(final byte[] key, final byte[] min, final byte[] max) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Long execute(Jedis connection) {
                return connection.zcount(key, min, max);
            }
        }.runBinary(key);
    }

    @Override
    public Set<byte[]> zrangeByScore(final byte[] key, final double min, final double max) {
        return new JedisClusterCommand<Set<byte[]>>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Set<byte[]> execute(Jedis connection) {
                return connection.zrangeByScore(key, min, max);
            }
        }.runBinary(key);
    }

    @Override
    public Set<byte[]> zrangeByScore(final byte[] key, final byte[] min, final byte[] max) {
        return new JedisClusterCommand<Set<byte[]>>(connectionHandler, timeout,
                maxRedirections) {
            @Override
            public Set<byte[]> execute(Jedis connection) {
                return connection.zrangeByScore(key, min, max);
            }
        }.runBinary(key);
    }

    @Override
    public Set<byte[]> zrevrangeByScore(final byte[] key, final double max, final double min) {
        return new JedisClusterCommand<Set<byte[]>>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Set<byte[]> execute(Jedis connection) {
             return connection.zrevrangeByScore(key, max,min);
             }
         }.runBinary(key);
    }

    @Override
    public Set<byte[]> zrangeByScore(final byte[] key, final double min, final double max, final int offset,
	    final int count) {
        return new JedisClusterCommand<Set<byte[]>>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Set<byte[]> execute(Jedis connection) {
             return connection.zrangeByScore(key, min, max, offset, count);
             }
         }.runBinary(key);
    }

    @Override
    public Set<byte[]> zrevrangeByScore(final byte[] key, final byte[] max, final byte[] min) {
        return new JedisClusterCommand<Set<byte[]>>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Set<byte[]> execute(Jedis connection) {
             return connection.zrevrangeByScore(key, max, min);
             }
         }.runBinary(key);
    }

    @Override
    public Set<byte[]> zrangeByScore(final byte[] key, final byte[] min, final byte[] max, final int offset,
	    final int count) {
        return new JedisClusterCommand<Set<byte[]>>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Set<byte[]> execute(Jedis connection) {
             return connection.zrangeByScore(key, min, max, offset, count);
             }
         }.runBinary(key);
    }

    @Override
    public Set<byte[]> zrevrangeByScore(final byte[] key, final double max, final double min,
	    final int offset, final int count) {
        return new JedisClusterCommand<Set<byte[]>>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Set<byte[]> execute(Jedis connection) {
             return connection.zrevrangeByScore(key, max, min, offset, count);
             }
         }.runBinary(key);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final double min, final double max) {
        return new JedisClusterCommand<Set<Tuple>>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Set<Tuple> execute(Jedis connection) {
             return connection.zrangeByScoreWithScores(key, min, max);
             }
         }.runBinary(key);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(final byte[] key, final double max, final double min) {
        return new JedisClusterCommand<Set<Tuple>>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Set<Tuple> execute(Jedis connection) {
             return connection.zrevrangeByScoreWithScores(key, max, min);
             }
         }.runBinary(key);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final double min, final double max,
	    final int offset, final int count) {
        return new JedisClusterCommand<Set<Tuple>>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Set<Tuple> execute(Jedis connection) {
             return connection.zrangeByScoreWithScores(key, min, max, offset, count);
             }
         }.runBinary(key);
    }

    @Override
    public Set<byte[]> zrevrangeByScore(final byte[] key, final byte[] max, final byte[] min,
	    final int offset, final int count) {
        return new JedisClusterCommand<Set<byte[]>>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Set<byte[]> execute(Jedis connection) {
             return connection.zrevrangeByScore(key, max, min, offset, count);
             }
         }.runBinary(key);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final byte[] min, final byte[] max) {
        return new JedisClusterCommand<Set<Tuple>>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Set<Tuple> execute(Jedis connection) {
             return connection.zrangeByScoreWithScores(key, min, max);
             }
         }.runBinary(key);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(final byte[] key, final byte[] max, final byte[] min) {
        return new JedisClusterCommand<Set<Tuple>>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Set<Tuple> execute(Jedis connection) {
             return connection.zrevrangeByScoreWithScores(key, max, min);
             }
         }.runBinary(key);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(final byte[] key, final byte[] min, final byte[] max,
	    final int offset, final int count) {
        return new JedisClusterCommand<Set<Tuple>>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Set<Tuple> execute(Jedis connection) {
             return connection.zrangeByScoreWithScores(key, min, max, offset, count);
             }
         }.runBinary(key);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(final byte[] key, final double max, final double min,
	    final int offset, final int count) {
        return new JedisClusterCommand<Set<Tuple>>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Set<Tuple> execute(Jedis connection) {
             return connection.zrevrangeByScoreWithScores(key, max, min, offset, count);
             }
         }.runBinary(key);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(final byte[] key, final byte[] max, final byte[] min,
	    final int offset, final int count) {
        return new JedisClusterCommand<Set<Tuple>>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Set<Tuple> execute(Jedis connection) {
             return connection.zrevrangeByScoreWithScores(key, max, min, offset, count);
             }
         }.runBinary(key);
    }
	    
    @Override
    public Long zremrangeByRank(final byte[] key, final long start, final long end) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Long execute(Jedis connection) {
             return connection.zremrangeByRank(key, start, end);
             }
         }.runBinary(key);
    }

    @Override
    public Long zremrangeByScore(final byte[] key, final double start, final double end) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Long execute(Jedis connection) {
             return connection.zremrangeByScore(key, start, end);
             }
         }.runBinary(key);
    }

    @Override
    public Long zremrangeByScore(final byte[] key, final byte[] start, final byte[] end) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Long execute(Jedis connection) {
             return connection.zremrangeByScore(key, start, end);
             }
         }.runBinary(key);
    }

    @Override
    public Long linsert(final byte[] key, final Client.LIST_POSITION where, final byte[] pivot,
	    final byte[] value) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Long execute(Jedis connection) {
             return connection.linsert(key, where, pivot, value);
             }
         }.runBinary(key);
    }

    @Override
    public Long lpushx(final byte[] key, final byte[]... arg) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Long execute(Jedis connection) {
             return connection.lpushx(key, arg);
             }
         }.runBinary(key);
    }

    @Override
    public Long rpushx(final byte[] key, final byte[]... arg) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Long execute(Jedis connection) {
             return connection.rpushx(key, arg);
             }
         }.runBinary(key);
    }

    @Override
    public List<byte[]> blpop(final byte[] arg) {
        return new JedisClusterCommand<List<byte[]>>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public List<byte[]> execute(Jedis connection) {
             return connection.blpop(arg);
             }
         }.runBinary(null);
    }

    @Override
    public List<byte[]> brpop(final byte[] arg) {
        return new JedisClusterCommand<List<byte[]>>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public List<byte[]> execute(Jedis connection) {
             return connection.brpop(arg);
             }
         }.runBinary(null);
    }

    @Override
    public Long del(final byte[] key) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Long execute(Jedis connection) {
             return connection.del(key);
             }
         }.runBinary(key);
    }

    @Override
    public byte[] echo(final byte[] arg) {
        return new JedisClusterCommand<byte[]>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public byte[] execute(Jedis connection) {
             return connection.echo(arg);
             }
         }.runBinary(null);
    }

    @Override 
    public Long move(final byte[] key, final int dbIndex) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Long execute(Jedis connection) {
             return connection.move(key, dbIndex);
             }
         }.runBinary(key);
    }

    @Override
    public Long bitcount(final byte[] key) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Long execute(Jedis connection) {
             return connection.bitcount(key);
             }
         }.runBinary(key);
    }

    @Override
    public Long bitcount(final byte[] key, final long start, final long end) {
        return new JedisClusterCommand<Long>(connectionHandler, timeout,
                maxRedirections) {
             @Override
             public Long execute(Jedis connection) {
             return connection.bitcount(key, start, end);
             }
         }.runBinary(key);
    }
    
    @Override
    public String ping() {
    return new JedisClusterCommand<String>(connectionHandler, timeout,
        maxRedirections) {
        @Override
        public String execute(Jedis connection) {
        return connection.ping();
        }
    }.run(null);
    }

    @Override
    public String quit() {
    return new JedisClusterCommand<String>(connectionHandler, timeout,
        maxRedirections) {
        @Override
        public String execute(Jedis connection) {
        return connection.quit();
        }
    }.run(null);
    }

    @Override
    public String flushDB() {
    return new JedisClusterCommand<String>(connectionHandler, timeout,
        maxRedirections) {
        @Override
        public String execute(Jedis connection) {
        return connection.flushDB();
        }
    }.run(null);
    }

    @Override
    public Long dbSize() {
    return new JedisClusterCommand<Long>(connectionHandler, timeout,
        maxRedirections) {
        @Override
        public Long execute(Jedis connection) {
        return connection.dbSize();
        }
    }.run(null);
    }

    @Override
    public String select(final int index) {
    return new JedisClusterCommand<String>(connectionHandler, timeout,
        maxRedirections) {
        @Override
        public String execute(Jedis connection) {
        return connection.select(index);
        }
    }.run(null);
    }

    @Override
    public String flushAll() {
    return new JedisClusterCommand<String>(connectionHandler, timeout,
        maxRedirections) {
        @Override
        public String execute(Jedis connection) {
        return connection.flushAll();
        }
    }.run(null);
    }

    @Override
    public String auth(final String password) {
    return new JedisClusterCommand<String>(connectionHandler, timeout,
        maxRedirections) {
        @Override
        public String execute(Jedis connection) {
        return connection.auth(password);
        }
    }.run(null);
    }

    @Override
    public String save() {
    return new JedisClusterCommand<String>(connectionHandler, timeout,
        maxRedirections) {
        @Override
        public String execute(Jedis connection) {
        return connection.save();
        }
    }.run(null);
    }

    @Override
    public String bgsave() {
    return new JedisClusterCommand<String>(connectionHandler, timeout,
        maxRedirections) {
        @Override
        public String execute(Jedis connection) {
        return connection.bgsave();
        }
    }.run(null);
    }

    @Override
    public String bgrewriteaof() {
    return new JedisClusterCommand<String>(connectionHandler, timeout,
        maxRedirections) {
        @Override
        public String execute(Jedis connection) {
        return connection.bgrewriteaof();
        }
    }.run(null);
    }

    @Override
    public Long lastsave() {
    return new JedisClusterCommand<Long>(connectionHandler, timeout,
        maxRedirections) {
        @Override
        public Long execute(Jedis connection) {
        return connection.lastsave();
        }
    }.run(null);
    }

    @Override
    public String shutdown() {
    return new JedisClusterCommand<String>(connectionHandler, timeout,
        maxRedirections) {
        @Override
        public String execute(Jedis connection) {
        return connection.shutdown();
        }
    }.run(null);
    }

    @Override
    public String info() {
    return new JedisClusterCommand<String>(connectionHandler, timeout,
        maxRedirections) {
        @Override
        public String execute(Jedis connection) {
        return connection.info();
        }
    }.run(null);
    }

    @Override
    public String info(final String section) {
    return new JedisClusterCommand<String>(connectionHandler, timeout,
        maxRedirections) {
        @Override
        public String execute(Jedis connection) {
        return connection.info(section);
        }
    }.run(null);
    }

    @Override
    public String slaveof(final String host, final int port) {
    return new JedisClusterCommand<String>(connectionHandler, timeout,
        maxRedirections) {
        @Override
        public String execute(Jedis connection) {
        return connection.slaveof(host, port);
        }
    }.run(null);
    }

    @Override
    public String slaveofNoOne() {
    return new JedisClusterCommand<String>(connectionHandler, timeout,
        maxRedirections) {
        @Override
        public String execute(Jedis connection) {
        return connection.slaveofNoOne();
        }
    }.run(null);
    }

    @Override
    public Long getDB() {
    return new JedisClusterCommand<Long>(connectionHandler, timeout,
        maxRedirections) {
        @Override
        public Long execute(Jedis connection) {
        return connection.getDB();
        }
    }.run(null);
    }

    @Override
    public String debug(final DebugParams params) {
    return new JedisClusterCommand<String>(connectionHandler, timeout,
        maxRedirections) {
        @Override
        public String execute(Jedis connection) {
        return connection.debug(params);
        }
    }.run(null);
    }

    @Override
    public String configResetStat() {
    return new JedisClusterCommand<String>(connectionHandler, timeout,
        maxRedirections) {
        @Override
        public String execute(Jedis connection) {
        return connection.configResetStat();
        }
    }.run(null);
    }

    public Map<String, JedisPool> getClusterNodes() {
    return connectionHandler.getNodes();
    }

    @Override
    public Long waitReplicas(int replicas, long timeout) {
    // TODO Auto-generated method stub
    return null;
    }
    
    @Override
    public Object eval(final byte[] script, final byte[] keyCount, final byte[]... params) {
        return new JedisClusterCommand<Object>(connectionHandler, timeout,
                maxRedirections) {
                @Override
                public Object execute(Jedis connection) {
                return connection.eval(script, keyCount, params);
                }
            }.runBinary(ByteBuffer.wrap(keyCount).getInt(), params);
    }

    @Override
    public Object eval(final byte[] script, final int keyCount, final byte[]... params) {
        return new JedisClusterCommand<Object>(connectionHandler, timeout,
                maxRedirections) {
                @Override
                public Object execute(Jedis connection) {
                return connection.eval(script, keyCount, params);
                }
            }.runBinary(keyCount, params);
    }

    @Override
    public Object eval(final byte[] script, final List<byte[]> keys, final List<byte[]> args) {
        return new JedisClusterCommand<Object>(connectionHandler, timeout,
                maxRedirections) {
                @Override
                public Object execute(Jedis connection) {
                return connection.eval(script, keys, args);
                }
            }.runBinary(keys.size(), keys.toArray(new byte[keys.size()][]));
    }

    @Override
    public Object eval(final byte[] script, byte[] key) {
        return new JedisClusterCommand<Object>(connectionHandler, timeout,
                maxRedirections) {
                @Override
                public Object execute(Jedis connection) {
                return connection.eval(script);
                }
            }.runBinary(key);
    }

    @Override
    public Object evalsha(final byte[] script, byte[] key) {
        return new JedisClusterCommand<Object>(connectionHandler, timeout,
                maxRedirections) {
                @Override
                public Object execute(Jedis connection) {
                return connection.evalsha(script);
                }
            }.runBinary(key);
    }

    @Override
    public Object evalsha(final byte[] sha1, final List<byte[]> keys, final List<byte[]> args) {
        return new JedisClusterCommand<Object>(connectionHandler, timeout,
                maxRedirections) {
                @Override
                public Object execute(Jedis connection) {
                return connection.evalsha(sha1, keys, args);
                }
            }.runBinary(keys.size(), keys.toArray(new byte[keys.size()][]));
    }

    @Override
    public Object evalsha(final byte[] sha1, final int keyCount, final byte[]... params) {
        return new JedisClusterCommand<Object>(connectionHandler, timeout,
                maxRedirections) {
                @Override
                public Object execute(Jedis connection) {
                return connection.evalsha(sha1, keyCount, params);
                }
            }.runBinary(keyCount, params);
    }
    
    @Override
    public List<Long> scriptExists(final byte[] key, final byte[][] sha1) {
        return new JedisClusterCommand<List<Long>>(connectionHandler, timeout,
                maxRedirections) {
                @Override
                public List<Long> execute(Jedis connection) {
                return connection.scriptExists(sha1);
                }
            }.runBinary(key);
    }
    
    @Override
    public byte[] scriptLoad(final byte[] script, final byte[] key) {
        return new JedisClusterCommand<byte[]>(connectionHandler, timeout,
                maxRedirections) {
                @Override
                public byte[] execute(Jedis connection) {
                return connection.scriptLoad(script);
                }
            }.runBinary(key);
    }
    
    @Override
    public String scriptFlush(final byte[] key) {
        return new JedisClusterCommand<String>(connectionHandler, timeout,
                maxRedirections) {
                @Override
                public String execute(Jedis connection) {
                return connection.scriptFlush();
                }
            }.runBinary(key);
    }
    
    @Override
    public String scriptKill(byte[] key) {
        return new JedisClusterCommand<String>(connectionHandler, timeout,
                maxRedirections) {
                @Override
                public String execute(Jedis connection) {
                return connection.scriptKill();
                }
            }.runBinary(key);
    }
}
