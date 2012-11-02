package redis.clients.jedis;

import redis.clients.jedis.BinaryClient.LIST_POSITION;

import java.util.*;

public class ShardedJedisPipeline extends Queable {
    private BinaryShardedJedis jedis;
    private List<FutureResult> results = new ArrayList<FutureResult>();
    private Queue<Client> clients = new LinkedList<Client>();

    private static class FutureResult {
        private Client client;

        public FutureResult(Client client) {
            this.client = client;
        }

        public Object get() {
            return client.getOne();
        }
    }

    public void setShardedJedis(BinaryShardedJedis jedis) {
        this.jedis = jedis;
    }

    public Response<String> set(String key, String value) {
        Client c = getClient(key);
        c.set(key, value);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING);
    }

    public Response<String> get(String key) {
        Client c = getClient(key);
        c.get(key);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Long> del(String key) {
        Client c = getClient(key);
        c.del(key);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Boolean> exists(String key) {
        Client c = getClient(key);
        c.exists(key);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.BOOLEAN);
    }

    public Response<Boolean> type(String key) {
        Client c = getClient(key);
        c.type(key);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.BOOLEAN);
    }

    public Response<Long> expire(String key, int seconds) {
        Client c = getClient(key);
        c.expire(key, seconds);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> expireAt(String key, long unixTime) {
        Client c = getClient(key);
        c.expireAt(key, unixTime);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> ttl(String key) {
        Client c = getClient(key);
        c.ttl(key);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<String> getSet(String key, String value) {
        Client c = getClient(key);
        c.getSet(key, value);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Long> setnx(String key, String value) {
        Client c = getClient(key);
        c.setnx(key, value);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> setex(String key, int seconds, String value) {
        Client c = getClient(key);
        c.setex(key, seconds, value);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);        
    }

    public Response<Long> decrBy(String key, long integer) {
        Client c = getClient(key);
        c.decrBy(key, integer);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> decr(String key) {
        Client c = getClient(key);
        c.decr(key);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> incrBy(String key, int integer) {
        Client c = getClient(key);
        c.incrBy(key, integer);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> incr(String key) {
        Client c = getClient(key);
        c.incr(key);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> append(String key, String value) {
        Client c = getClient(key);
        c.append(key, value);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<String> substr(String key, int start, int end) {
        Client c = getClient(key);
        c.substr(key, start, end);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Long> hset(String key, String field, String value) {
        Client c = getClient(key);
        c.hset(key, field, value);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<String> hget(String key, String field) {
        Client c = getClient(key);
        c.hget(key, field);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Long> hsetnx(String key, String field, String value) {
        Client c = getClient(key);
        c.hsetnx(key, field, value);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<String> hmset(String key, Map<String, String> hash) {
        Client c = getClient(key);
        c.hmset(key, hash);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING);
    }

    public Response<List<String>> hmget(String key, String... fields) {
        Client c = getClient(key);
        c.hmget(key, fields);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING_LIST);
    }

    public Response<Long> hincrBy(String key, String field, int value) {
        Client c = getClient(key);
        c.hincrBy(key, field, value);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Boolean> hexists(String key, String field) {
        Client c = getClient(key);
        c.hexists(key, field);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.BOOLEAN);
    }

    public Response<Long> hdel(String key, String field) {
        Client c = getClient(key);
        c.hdel(key, field);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> hlen(String key) {
        Client c = getClient(key);
        c.hlen(key);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Set<String>> hkeys(String key) {
        Client c = getClient(key);
        c.hkeys(key);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING_SET);
    }

    public Response<Set<String>> hvals(String key) {
        Client c = getClient(key);
        c.hvals(key);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING_SET);
    }

    public Response<Map<String, String>> hgetAll(String key) {
        Client c = getClient(key);
        c.hgetAll(key);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING_MAP);
    }

    public Response<Long> rpush(String key, String string) {
        Client c = getClient(key);
        c.rpush(key, string);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> lpush(String key, String string) {
        Client c = getClient(key);
        c.lpush(key, string);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> llen(String key) {
        Client c = getClient(key);
        c.llen(key);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<List<String>> lrange(String key, int start, int end) {
        Client c = getClient(key);
        c.lrange(key, start, end);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING_LIST);
    }

    public Response<String> ltrim(String key, int start, int end) {
        Client c = getClient(key);
        c.ltrim(key, start, end);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING);
    }

    public Response<String> lindex(String key, int index) {
        Client c = getClient(key);
        c.lindex(key, index);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING);
    }

    public Response<String> lset(String key, int index, String value) {
        Client c = getClient(key);
        c.lset(key, index, value);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Long> lrem(String key, int count, String value) {
        Client c = getClient(key);
        c.lrem(key, count, value);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<String> lpop(String key) {
        Client c = getClient(key);
        c.lpop(key);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING);
    }

    public Response<String> rpop(String key) {
        Client c = getClient(key);
        c.rpop(key);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Long> sadd(String key, String member) {
        Client c = getClient(key);
        c.sadd(key, member);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Set<String>> smembers(String key) {
        Client c = getClient(key);
        c.smembers(key);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING_SET);
    }

    public Response<Long> srem(String key, String member) {
        Client c = getClient(key);
        c.srem(key, member);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<String> spop(String key) {
        Client c = getClient(key);
        c.spop(key);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Long> scard(String key) {
        Client c = getClient(key);
        c.scard(key);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Boolean> sismember(String key, String member) {
        Client c = getClient(key);
        c.sismember(key, member);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.BOOLEAN);
    }

    public Response<String> srandmember(String key) {
        Client c = getClient(key);
        c.srandmember(key);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Long> zadd(String key, double score, String member) {
        Client c = getClient(key);
        c.zadd(key, score, member);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Set<String>> zrange(String key, int start, int end) {
        Client c = getClient(key);
        c.zrange(key, start, end);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING_ZSET);
    }

    public Response<Long> zrem(String key, String member) {
        Client c = getClient(key);
        c.zrem(key, member);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Double> zincrby(String key, double score, String member) {
        Client c = getClient(key);
        c.zincrby(key, score, member);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.DOUBLE);
    }

    public Response<Long> zrank(String key, String member) {
        Client c = getClient(key);
        c.zrank(key, member);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> zrevrank(String key, String member) {
        Client c = getClient(key);
        c.zrevrank(key, member);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Set<String>> zrevrange(String key, int start, int end) {
        Client c = getClient(key);
        c.zrevrange(key, start, end);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING_ZSET);
    }

    public Response<Set<Tuple>> zrangeWithScores(String key, int start, int end) {
        Client c = getClient(key);
        c.zrangeWithScores(key, start, end);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.TUPLE_ZSET);
    }

    public Response<Set<Tuple>> zrevrangeWithScores(String key, int start, int end) {
        Client c = getClient(key);
        c.zrevrangeWithScores(key, start, end);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.TUPLE_ZSET);
    }

    public Response<Long> zcard(String key) {
        Client c = getClient(key);
        c.zcard(key);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Double> zscore(String key, String member) {
        Client c = getClient(key);
        c.zscore(key, member);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.DOUBLE);
    }

    public Response<Double> sort(String key) {
        Client c = getClient(key);
        c.sort(key);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.DOUBLE);
    }

    public Response<List<String>> sort(String key, SortingParams sortingParameters) {
        Client c = getClient(key);
        c.sort(key, sortingParameters);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING_LIST);
    }

    public Response<Long> zcount(String key, double min, double max) {
        Client c = getClient(key);
        c.zcount(key, min, max);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Set<String>> zrangeByScore(String key, double min, double max) {
        Client c = getClient(key);
        c.zrangeByScore(key, min, max);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING_ZSET);
    }

    public Response<Set<String>> zrangeByScore(String key, double min, double max,
                                               int offset, int count) {
        Client c = getClient(key);
        c.zrangeByScore(key, min, max, offset, count);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.STRING_ZSET);
    }

    public Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max) {
        Client c = getClient(key);
        c.zrangeByScoreWithScores(key, min, max);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.TUPLE_ZSET);
    }

    public Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max,
            int offset, int count) {
        Client c = getClient(key);
        c.zrangeByScoreWithScores(key, min, max, offset, count);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.TUPLE_ZSET);
    }

    public Response<Long> zremrangeByRank(String key, int start, int end) {
        Client c = getClient(key);
        c.zremrangeByRank(key, start, end);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> zremrangeByScore(String key, double start, double end) {
        Client c = getClient(key);
        c.zremrangeByScore(key, start, end);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> linsert(String key, LIST_POSITION where, String pivot,
                                  String value) {
        Client c = getClient(key);
        c.linsert(key, where, pivot, value);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Boolean> getbit(String key, long offset) {
        Client c = getClient(key);
        c.getbit(key, offset);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.BOOLEAN);
    }

    public Response<Boolean> setbit(String key, long offset, boolean value) {
        Client c = getClient(key);
        c.setbit(key, offset, value);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.BOOLEAN);
    }

    public Response<Long> setrange(String key, long offset, String value) {
        Client c = getClient(key);
        c.setrange(key, offset, value);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);

    }

    public Response<Long> getrange(String key, long startOffset, long endOffset) {
        Client c = getClient(key);
        c.getrange(key, startOffset, endOffset);
        results.add(new FutureResult(c));
        return getResponse(BuilderFactory.LONG);
    }

    public List<Object> getResults() {
        List<Object> r = new ArrayList<Object>();
        for (FutureResult fr : results) {
            r.add(fr.get());
        }
        return r;
    }

    /**
     * Syncronize pipeline by reading all responses. This operation closes the
     * pipeline. In order to get return values from pipelined commands, capture
     * the different Response&lt;?&gt; of the commands you execute.
     */
    public void sync() {
        for (Client client : clients) {
            generateResponse(client.getOne());
        }
    }

    /**
     * Syncronize pipeline by reading all responses. This operation closes the
     * pipeline. Whenever possible try to avoid using this version and use
     * ShardedJedisPipeline.sync() as it won't go through all the responses and generate the
     * right response type (usually it is a waste of time).
     *
     * @return A list of all the responses in the order you executed them.
     */
    public List<Object> syncAndReturnAll() {
        List<Object> formatted = new ArrayList<Object>();
        for (Client client : clients) {
            formatted.add(generateResponse(client.getOne()).get());
        }
        return formatted;
    }

    /**
     * This method will be removed in Jedis 3.0. Use the methods that return Response's and call
     * sync().
     */
    @Deprecated
    public void execute() {
    }

    private Client getClient(String key) {
        Client client = jedis.getShard(key).getClient();
        clients.add(client);
        return client;
    }
}