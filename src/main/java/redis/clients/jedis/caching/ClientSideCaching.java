package redis.clients.jedis.caching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.commands.JedisCommands;
import redis.clients.jedis.commands.JedisCommands;
import redis.clients.jedis.params.ClientTrackingParams;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.ZIncrByParams;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientSideCaching implements JedisCommands {

    protected static Logger log = LoggerFactory.getLogger(ClientSideCaching.class.getName());



    private Jedis dataJedis = null;
    private RedisClientSideCache redisClientSideCache = null;
    private ClientTrackingParams params = null;

    public ClientSideCaching(boolean enabled, Jedis dataJedis, JedisPool jedisPool, ClientTrackingParams params) {
        this(enabled, dataJedis, jedisPool.getResource(), params);
    }

    public ClientSideCaching(boolean enabled, Jedis dataJedis, Jedis invalidationJedis, ClientTrackingParams params) {
        this.dataJedis = dataJedis;
        redisClientSideCache = new RedisClientSideCache(invalidationJedis);

        if (params == null) {
            this.params = ClientTrackingParams.clientTrackingParams();
        } else {
            this.params = params;
        }

        // Jedis only support RESP2 for now so we must add a REDIRECT
        if (this.params.getParam(Protocol.Keyword.REDIRECT.name()) == null) {
            log.info("Tracking redirect client id {}",redisClientSideCache.getClientId() );
            this.params.redirect(redisClientSideCache.getClientId());
        }

        this.dataJedis.clientTracking(true, this.params);
    }

    public ClientSideCaching(boolean enabled, Jedis dataJedis, Jedis invalidationJedis) {
        this( enabled, dataJedis, invalidationJedis, null );
    }

    public ClientSideCaching(boolean enabled, Jedis dataJedis, JedisPool jedisPool) {
        this( enabled, dataJedis, jedisPool, null );
    }

    @Override
    public String set(String key, String value) {
        // if no loop set the value in cache from application not from the invalidation
        if (params.isNoLoop()) {
            redisClientSideCache.putValueInCache(key, value);
        }
        return dataJedis.set(key, value);
    }

    @Override
    public String set(String key, String value, SetParams params) {
        return dataJedis.set(key, value, params);
    }

    @Override
    public String get(String key) {
        String value = (String)redisClientSideCache.getValueFromCache(key);
        if (value == null) {
            value = dataJedis.get(key);
            if (value != null) {
                redisClientSideCache.putValueInCache(key, value);
                log.info("Got value for {} from Redis (and set in cache)", key);
        }
        } else {
                log.info("Got value for {} from cache", key);
        }
        return value;
    }


    @Override
    public Map<String, String> hgetAll(String key) {
        Map<String, String> value = (Map<String, String>)redisClientSideCache.getValueFromCache(key);
        if (value == null) {
            value = dataJedis.hgetAll(key);
            if (value != null) {
                redisClientSideCache.putValueInCache(key, value);
                log.info("Got value for {} from Redis (and set in cache)", key);
            }
        } else {
            log.info("Got value for {} from cache", key);
        }
        return value;
    }

    @Override
    public Long hset(String key, Map<String, String> hash) {
        //invalidate the cache all the time to avoid checking all fields
        // noloop is not supported
        redisClientSideCache.removeKeyFromCache(key);
        return dataJedis.hset(key, hash);
    }

    @Override
    public Boolean exists(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long persist(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call"); 
    }

    @Override
    public String type(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public byte[] dump(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public String restore(String key, int ttl, byte[] serializedValue) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public String restoreReplace(String key, int ttl, byte[] serializedValue) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long expire(String key, int seconds) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long pexpire(String key, long milliseconds) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long expireAt(String key, long unixTime) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long pexpireAt(String key, long millisecondsTimestamp) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long ttl(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long pttl(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long touch(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Boolean setbit(String key, long offset, boolean value) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Boolean setbit(String key, long offset, String value) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Boolean getbit(String key, long offset) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long setrange(String key, long offset, String value) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public String getrange(String key, long startOffset, long endOffset) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public String getSet(String key, String value) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long setnx(String key, String value) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public String setex(String key, int seconds, String value) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public String psetex(String key, long milliseconds, String value) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long decrBy(String key, long decrement) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long decr(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long incrBy(String key, long increment) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Double incrByFloat(String key, double increment) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long incr(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long append(String key, String value) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public String substr(String key, int start, int end) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long hset(String key, String field, String value) { 
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public String hget(String key, String field) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long hsetnx(String key, String field, String value) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public String hmset(String key, Map<String, String> hash) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<String> hmget(String key, String... fields) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long hincrBy(String key, String field, long value) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Double hincrByFloat(String key, String field, double value) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Boolean hexists(String key, String field) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long hdel(String key, String... field) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long hlen(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<String> hkeys(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<String> hvals(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long rpush(String key, String... string) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long lpush(String key, String... string) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long llen(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<String> lrange(String key, long start, long stop) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public String ltrim(String key, long start, long stop) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public String lindex(String key, long index) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public String lset(String key, long index, String value) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long lrem(String key, long count, String value) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public String lpop(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public String rpop(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long sadd(String key, String... member) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<String> smembers(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long srem(String key, String... member) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public String spop(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<String> spop(String key, long count) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long scard(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Boolean sismember(String key, String member) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public String srandmember(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<String> srandmember(String key, int count) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long strlen(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long zadd(String key, double score, String member) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long zadd(String key, double score, String member, ZAddParams params) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long zadd(String key, Map<String, Double> scoreMembers) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<String> zrange(String key, long start, long stop) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long zrem(String key, String... members) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Double zincrby(String key, double increment, String member) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Double zincrby(String key, double increment, String member, ZIncrByParams params) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long zrank(String key, String member) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long zrevrank(String key, String member) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<String> zrevrange(String key, long start, long stop) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<Tuple> zrangeWithScores(String key, long start, long stop) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<Tuple> zrevrangeWithScores(String key, long start, long stop) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long zcard(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Double zscore(String key, String member) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Tuple zpopmax(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<Tuple> zpopmax(String key, int count) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Tuple zpopmin(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<Tuple> zpopmin(String key, int count) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<String> sort(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<String> sort(String key, SortingParams sortingParameters) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long zcount(String key, double min, double max) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long zcount(String key, String min, String max) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<String> zrangeByScore(String key, double min, double max) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<String> zrangeByScore(String key, String min, String max) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<String> zrevrangeByScore(String key, double max, double min) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<String> zrevrangeByScore(String key, String max, String min) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long zremrangeByRank(String key, long start, long stop) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long zremrangeByScore(String key, double min, double max) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long zremrangeByScore(String key, String min, String max) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long zlexcount(String key, String min, String max) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<String> zrangeByLex(String key, String min, String max) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<String> zrangeByLex(String key, String min, String max, int offset, int count) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<String> zrevrangeByLex(String key, String max, String min) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Set<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long zremrangeByLex(String key, String min, String max) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long linsert(String key, ListPosition where, String pivot, String value) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long lpushx(String key, String... string) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long rpushx(String key, String... string) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<String> blpop(int timeout, String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<String> brpop(int timeout, String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long del(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long unlink(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public String echo(String string) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long move(String key, int dbIndex) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long bitcount(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long bitcount(String key, long start, long end) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long bitpos(String key, boolean value) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long bitpos(String key, boolean value, BitPosParams params) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public ScanResult<String> sscan(String key, String cursor) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public ScanResult<Tuple> zscan(String key, String cursor) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public ScanResult<String> sscan(String key, String cursor, ScanParams params) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long pfadd(String key, String... elements) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public long pfcount(String key) {
        return 0;
    }

    @Override
    public Long geoadd(String key, double longitude, double latitude, String member) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Double geodist(String key, String member1, String member2) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Double geodist(String key, String member1, String member2, GeoUnit unit) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<String> geohash(String key, String... members) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<GeoCoordinate> geopos(String key, String... members) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius,
            GeoUnit unit) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<GeoRadiusResponse> georadiusReadonly(String key, double longitude, double latitude, double radius,
            GeoUnit unit) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius,
            GeoUnit unit, GeoRadiusParam param) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<GeoRadiusResponse> georadiusReadonly(String key, double longitude, double latitude, double radius,
            GeoUnit unit, GeoRadiusParam param) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMemberReadonly(String key, String member, double radius, GeoUnit unit) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit,
            GeoRadiusParam param) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMemberReadonly(String key, String member, double radius, GeoUnit unit,
            GeoRadiusParam param) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<Long> bitfield(String key, String... arguments) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<Long> bitfieldReadonly(String key, String... arguments) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long hstrlen(String key, String field) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public StreamEntryID xadd(String key, StreamEntryID id, Map<String, String> hash) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public StreamEntryID xadd(String key, StreamEntryID id, Map<String, String> hash, long maxLen,
            boolean approximateLength) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public Long xlen(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<StreamEntry> xrange(String key, StreamEntryID start, StreamEntryID end, int count) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<StreamEntry> xrevrange(String key, StreamEntryID end, StreamEntryID start, int count) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public long xack(String key, String group, StreamEntryID... ids) {
        return 0;
    }

    @Override
    public String xgroupCreate(String key, String groupname, StreamEntryID id, boolean makeStream) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public String xgroupSetID(String key, String groupname, StreamEntryID id) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public long xgroupDestroy(String key, String groupname) {
        return 0;
    }

    @Override
    public Long xgroupDelConsumer(String key, String groupname, String consumername) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<StreamPendingEntry> xpending(String key, String groupname, StreamEntryID start, StreamEntryID end,
            int count, String consumername) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public long xdel(String key, StreamEntryID... ids) {
        return 0;
    }

    @Override
    public long xtrim(String key, long maxLen, boolean approximate) {
        return 0;
    }

    @Override
    public List<StreamEntry> xclaim(String key, String group, String consumername, long minIdleTime, long newIdleTime,
            int retries, boolean force, StreamEntryID... ids) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public StreamInfo xinfoStream(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<StreamGroupInfo> xinfoGroup(String key) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }

    @Override
    public List<StreamConsumersInfo> xinfoConsumers(String key, String group) {
        throw new UnsupportedOperationException("Use the native Jedis call");
    }
    }
