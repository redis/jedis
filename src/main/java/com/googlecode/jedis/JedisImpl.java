package com.googlecode.jedis;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Collections2.transform;
import static com.googlecode.jedis.PairImpl.newPair;
import static com.googlecode.jedis.util.Encoders.asByte;
import static com.googlecode.jedis.util.Encoders.asString;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

class JedisImpl extends RawJedisImpl implements Jedis {

    private static class ByteToStringFunction implements
	    Function<byte[], String> {
	@Override
	public String apply(final byte[] input) {
	    return asString(input);
	}
    }

    private static class PairByteByteToPairStringString implements
	    Function<Pair<byte[], byte[]>, Pair<String, String>> {
	@Override
	public Pair<String, String> apply(final Pair<byte[], byte[]> input) {
	    return newPair(asString(input.getFirst()),
		    asString(input.getSecond()));
	}
    }

    private static class PairByteDoubleToPairStringDouble implements
	    Function<Pair<byte[], Double>, Pair<String, Double>> {
	@Override
	public Pair<String, Double> apply(final Pair<byte[], Double> input) {
	    return newPair(asString(input.getFirst()), input.getSecond());
	}
    }

    /**
     * Create a {@link Jedis} instance via
     * {@link JedisFactory#newJedisInstance(JedisConfig)}
     */
    protected JedisImpl() {
	super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#append(java.lang.String,
     * java.lang.String)
     */
    @Override
    public Long append(final String key, final String value) {
	return append(asByte(key), asByte(value));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#blpop(long, java.lang.String,
     * java.lang.String[])
     */
    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#auth(java.lang.String)
     */
    @Override
    public Boolean auth(final String password) {
	return auth(asByte(password));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#blpop(long, java.lang.String,
     * java.lang.String[])
     */
    @Override
    public List<Pair<String, String>> blpop(final long timeout,
	    final String key1, final String... keyN) {
	return ImmutableList.copyOf(transform(
		blpopRaw(timeout, asByte(key1), asByte(keyN)),
		new PairByteByteToPairStringString()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#brpop(long, java.lang.String,
     * java.lang.String[])
     */
    @Override
    public List<Pair<String, String>> brpop(final long timeout,
	    final String key1, final String... keyN) {
	return ImmutableList.copyOf(transform(
		brpopRaw(timeout, asByte(key1), asByte(keyN)),
		new PairByteByteToPairStringString()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#configGet(java.lang.String)
     */
    @Override
    public List<String> configGet(final String pattern) {
	return Lists.newArrayList(transform(configGet(asByte(pattern)),
		new ByteToStringFunction()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#configSet(java.lang.String,
     * java.lang.String)
     */
    @Override
    public String configSet(final String parameter, final String value) {
	return asString(configSet(asByte(parameter), asByte(value)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#debug(com.googlecode.jedis.DebugParams)
     */
    @Override
    public String debug(final DebugParams params) {
	return asString(debugRaw(params));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#decr(java.lang.String)
     */
    @Override
    public Long decr(final String key) {
	return decr(asByte(key));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#decrBy(java.lang.String, long)
     */
    @Override
    public Long decrBy(final String key, final long value) {
	return decrBy(asByte(key), value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#del(java.lang.String)
     */
    @Override
    public Long del(final String key1, final String... keyN) {
	return del(asByte(key1), asByte(keyN));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#echo(java.lang.String)
     */
    @Override
    public String echo(final String string) {
	return asString(echo(asByte(string)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#exists(java.lang.String)
     */
    @Override
    public Boolean exists(final String key) {
	return exists(asByte(key));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#expire(java.lang.String, long)
     */
    @Override
    public Boolean expire(final String key, final long seconds) {
	return expire(asByte(key), seconds);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#expireAt(java.lang.String, long)
     */
    @Override
    public Boolean expireAt(final String key, final long unixTime) {
	return expireAt(asByte(key), unixTime);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#get(java.lang.String)
     */
    @Override
    public String get(final String key) {
	return asString(get(asByte(key)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#getSet(java.lang.String,
     * java.lang.String)
     */
    @Override
    public String getSet(final String key, final String value) {
	return asString(getSet(asByte(key), asByte(value)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hdel(java.lang.String, java.lang.String)
     */
    @Override
    public Long hdel(final String key, final String field) {
	return hdel(asByte(key), asByte(field));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hexists(java.lang.String,
     * java.lang.String)
     */
    @Override
    public Boolean hexists(final String key, final String field) {
	return hexists(asByte(key), asByte(field));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hget(java.lang.String, java.lang.String)
     */
    @Override
    public String hget(final String key, final String field) {
	return asString(hget(asByte(key), asByte(field)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hgetAll(java.lang.String)
     */
    @Override
    public Map<String, String> hgetAll(final String key) {
	final Map<String, String> result = Maps.newHashMap();
	for (final Map.Entry<byte[], byte[]> it : hgetAll(asByte(key))
		.entrySet()) {
	    result.put(asString(it.getKey()), asString(it.getValue()));
	}
	return ImmutableMap.copyOf(result);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hincrBy(java.lang.String,
     * java.lang.String, long)
     */
    @Override
    public Long hincrBy(final String key, final String field, final long value) {
	return hincrBy(asByte(key), asByte(field), value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hkeys(java.lang.String)
     */
    @Override
    public Set<String> hkeys(final String key) {
	return ImmutableSet.copyOf(Collections2.transform(hkeys(asByte(key)),
		new ByteToStringFunction()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hlen(java.lang.String)
     */
    @Override
    public Long hlen(final String key) {
	return hlen(asByte(key));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hmget(java.lang.String, java.lang.String)
     */
    @Override
    public List<String> hmget(final String key, final String... fields) {
	return Lists.newArrayList(transform(hmget(asByte(key), asByte(fields)),
		new ByteToStringFunction()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hmset(java.lang.String, java.util.Map)
     */
    @Override
    public Boolean hmset(final String key, final Map<String, String> hash) {
	final Map<byte[], byte[]> bhash = Maps.newHashMap();
	for (final Map.Entry<String, String> it : hash.entrySet()) {
	    bhash.put(asByte(it.getKey()), asByte(it.getValue()));
	}
	return hmset(asByte(key), bhash);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hset(java.lang.String, java.lang.String,
     * java.lang.String)
     */
    @Override
    public Long hset(final String key, final String field, final String value) {
	return hset(asByte(key), asByte(field), asByte(value));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hsetnx(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public Long hsetnx(final String key, final String field, final String value) {
	return hsetnx(asByte(key), asByte(field), asByte(value));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hvals(java.lang.String)
     */
    @Override
    public List<String> hvals(final String key) {
	return ImmutableList.copyOf(transform(hvals(asByte(key)),
		new ByteToStringFunction()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#incr(java.lang.String)
     */
    @Override
    public Long incr(final String key) {
	return incr(asByte(key));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#incrBy(java.lang.String, long)
     */
    @Override
    public Long incrBy(final String key, final long value) {
	return incrBy(asByte(key), value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#keys(java.lang.String)
     */
    @Override
    public Set<String> keys(final String pattern) {
	return ImmutableSet.copyOf(transform(keys(asByte(pattern)),
		new ByteToStringFunction()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#lindex(java.lang.String, int)
     */
    @Override
    public String lindex(final String key, final int index) {
	return asString(lindex(asByte(key), index));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#linsertAfter(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public Long linsertAfter(final String key, final String element,
	    final String value) {
	return linsertAfter(asByte(key), asByte(element), asByte(value));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#linsertBefore(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public Long linsertBefore(final String key, final String element,
	    final String value) {
	return linsertBefore(asByte(key), asByte(element), asByte(value));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#llen(java.lang.String)
     */
    @Override
    public Long llen(final String key) {
	return llen(asByte(key));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#lpop(java.lang.String)
     */
    @Override
    public String lpop(final String key) {
	return asString(lpop(asByte(key)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#lpush(com.googlecode.jedis.Pair)
     */
    @Override
    public Long lpush(final Pair<String, String> keyValuePair) {
	return lpush(keyValuePair.getFirst(), keyValuePair.getSecond());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#lpush(java.lang.String, java.lang.String)
     */
    @Override
    public Long lpush(final String key, final String value) {
	return lpush(asByte(key), asByte(value));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#lpushx(java.lang.String,
     * java.lang.String)
     */
    @Override
    public Long lpushx(final String key, final String value) {
	return lpushx(asByte(key), asByte(value));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#lrange(java.lang.String, int, int)
     */
    @Override
    public List<String> lrange(final String key, final long start,
	    final long end) {
	return ImmutableList.copyOf(transform(lrange(asByte(key), start, end),
		new ByteToStringFunction()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#lrem(java.lang.String, int,
     * java.lang.String)
     */
    @Override
    public Long lrem(final String key, final int count, final String value) {
	return lrem(asByte(key), count, asByte(value));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#lset(java.lang.String, int,
     * java.lang.String)
     */
    @Override
    public Boolean lset(final String key, final int index, final String value) {
	return lset(asByte(key), index, asByte(value));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#ltrim(java.lang.String, int, int)
     */
    @Override
    public Boolean ltrim(final String key, final int start, final int end) {
	return ltrim(asByte(key), start, end);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#mget(java.lang.String)
     */
    @Override
    public List<String> mget(final String... keys) {
	return ImmutableList.copyOf(transform(mget(asByte(keys)),
		new ByteToStringFunction()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#move(java.lang.String, long)
     */
    @Override
    public Boolean move(final String key, final long index) {
	return move(asByte(key), index);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#mset(com.googlecode.jedis.Pair,
     * com.googlecode.jedis.Pair<java.lang.String,java.lang.String>[])
     */
    @Override
    public Boolean mset(final Pair<String, String> keyValuePair1,
	    final Pair<String, String>... keyValuePairN) {
	checkNotNull(keyValuePair1);
	checkNotNull(keyValuePair1.getFirst());
	checkNotNull(keyValuePair1.getSecond());

	@SuppressWarnings("unchecked")
	final Pair<byte[], byte[]>[] args = new Pair[keyValuePairN.length];

	for (int i = 0; i < args.length; i++) {
	    args[i] = asByte(keyValuePairN[i]);
	}

	return msetRaw(asByte(keyValuePair1), args);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#msetnx(com.googlecode.jedis.Pair,
     * com.googlecode.jedis.Pair<java.lang.String,java.lang.String>[])
     */
    @Override
    public Boolean msetnx(final Pair<String, String> keyValuePair1,
	    final Pair<String, String>... keyValuePairN) {
	checkNotNull(keyValuePair1);
	checkNotNull(keyValuePair1.getFirst());
	checkNotNull(keyValuePair1.getSecond());

	@SuppressWarnings("unchecked")
	final Pair<byte[], byte[]>[] args = new Pair[keyValuePairN.length];

	for (int i = 0; i < args.length; i++) {
	    args[i] = asByte(keyValuePairN[i]);
	}

	return msetnxRaw(asByte(keyValuePair1), args);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#persist(java.lang.String)
     */
    @Override
    public Boolean persist(final String key) {
	return persist(asByte(key));
    }

    @Override
    public void pipelined() {
	// TODO
	// conn.setPipelineMode(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#randomKey()
     */
    @Override
    public String randomKey() {
	return asString(randomKeyRaw());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#rename(java.lang.String,
     * java.lang.String)
     */
    @Override
    public Boolean rename(final String srcKey, final String dstKey) {
	return rename(asByte(srcKey), asByte(dstKey));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#renamenx(java.lang.String,
     * java.lang.String)
     */
    @Override
    public Boolean renamenx(final String srcKey, final String dstKey) {
	return renamenx(asByte(srcKey), asByte(dstKey));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#rpop(java.lang.String)
     */
    @Override
    public String rpop(final String key) {
	return asString(rpop(asByte(key)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#rpoplpush(java.lang.String,
     * java.lang.String)
     */
    @Override
    public String rpoplpush(final String srckey, final String dstkey) {
	return asString(rpoplpush(asByte(srckey), asByte(dstkey)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#rpush(com.googlecode.jedis.Pair)
     */
    @Override
    public Long rpush(final Pair<String, String> keyValuePair) {
	checkNotNull(keyValuePair);
	return rpush(keyValuePair.getFirst(), keyValuePair.getSecond());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#rpush(java.lang.String, java.lang.String)
     */
    @Override
    public Long rpush(final String key, final String value) {
	return rpush(asByte(key), asByte(value));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#rpushx(java.lang.String,
     * java.lang.String)
     */
    @Override
    public Long rpushx(final String key, final String value) {
	return rpushx(asByte(key), asByte(value));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#sadd(java.lang.String, java.lang.String)
     */
    @Override
    public Boolean sadd(final String key, final String member) {
	return sadd(asByte(key), asByte(member));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#scard(java.lang.String)
     */
    @Override
    public Long scard(final String key) {
	return scard(asByte(key));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#sdiff(java.lang.String,
     * java.lang.String[])
     */
    @Override
    public Set<String> sdiff(final String key1, final String... keyN) {
	return ImmutableSet.copyOf(transform(sdiff(asByte(key1), asByte(keyN)),
		new ByteToStringFunction()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#sdiffstore(java.lang.String,
     * java.lang.String, java.lang.String[])
     */
    @Override
    public Long sdiffstore(final String dstKey, final String key1,
	    final String... keyN) {
	return sdiffstore(asByte(dstKey), asByte(key1), asByte(keyN));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#set(com.googlecode.jedis.Pair)
     */
    @Override
    public Boolean set(final Pair<String, String> keyValuePair) {
	checkNotNull(keyValuePair);
	return set(keyValuePair.getFirst(), keyValuePair.getSecond());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#set(java.lang.String, java.lang.String)
     */
    @Override
    public Boolean set(final String key, final String value) {
	return set(asByte(key), asByte(value));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedisImpl#setex(com.googlecode.jedis.Pair,
     * int)
     */
    @Override
    public Boolean setex(final Pair<String, String> keyValuePair,
	    final int seconds) {
	checkNotNull(keyValuePair);
	return setex(keyValuePair.getFirst(), keyValuePair.getSecond(), seconds);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#setex(java.lang.String, int,
     * java.lang.String)
     */
    @Override
    public Boolean setex(final String key, final String value, final int seconds) {
	return setex(asByte(key), asByte(value), seconds);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#setnx(com.googlecode.jedis.Pair)
     */
    @Override
    public Boolean setnx(final Pair<String, String> keyValuePair) {
	return setnx(keyValuePair.getFirst(), keyValuePair.getSecond());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#setnx(java.lang.String, java.lang.String)
     */
    @Override
    public Boolean setnx(final String key, final String value) {
	return setnx(asByte(key), asByte(value));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#sinter(java.lang.String)
     */
    @Override
    public Set<String> sinter(final String key1, final String... keyN) {
	return ImmutableSet
		.copyOf(transform(sinter(asByte(key1), asByte(keyN)),
			new ByteToStringFunction()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#sinterstore(java.lang.String,
     * java.lang.String, java.lang.String[])
     */
    @Override
    public Long sinterstore(final String dstkey, final String srcKey1,
	    final String... scrKeyN) {
	return sinterstore(asByte(dstkey), asByte(srcKey1), asByte(scrKeyN));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#sismember(java.lang.String,
     * java.lang.String)
     */
    @Override
    public Boolean sismember(final String key, final String member) {
	return sismember(asByte(key), asByte(member));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#smembers(java.lang.String)
     */
    @Override
    public Set<String> smembers(final String key) {
	return ImmutableSet.copyOf(transform(smembers(asByte(key)),
		new ByteToStringFunction()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#smove(java.lang.String, java.lang.String,
     * java.lang.String)
     */
    @Override
    public Boolean smove(final String srckey, final String dstkey,
	    final String member) {
	return smove(asByte(srckey), asByte(dstkey), asByte(member));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#sort(java.lang.String)
     */
    @Override
    public List<String> sort(final String key) {
	return ImmutableList.copyOf(transform(sort(asByte(key)),
		new ByteToStringFunction()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#sort(java.lang.String,
     * com.googlecode.jedis.SortParams)
     */
    @Override
    public List<String> sort(final String key,
	    final SortParams sortingParameters) {
	return ImmutableList.copyOf(transform(
		sort(asByte(key), sortingParameters),
		new ByteToStringFunction()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#sort(java.lang.String,
     * com.googlecode.jedis.SortParams, java.lang.String)
     */
    @Override
    public Long sort(final String key, final SortParams sortingParameters,
	    final String dstkey) {
	return sort(asByte(key), sortingParameters, asByte(dstkey));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#sort(java.lang.String, java.lang.String)
     */
    @Override
    public Long sort(final String key, final String dstkey) {
	return sort(asByte(key), asByte(dstkey));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#spop(java.lang.String)
     */
    @Override
    public String spop(final String key) {
	return asString(spop(asByte(key)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#srandmember(java.lang.String)
     */
    @Override
    public String srandmember(final String key) {
	return asString(srandmember(asByte(key)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#srem(java.lang.String, java.lang.String)
     */
    @Override
    public Boolean srem(final String key, final String member) {
	return srem(asByte(key), asByte(member));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#strlen(java.lang.String)
     */
    @Override
    public Long strlen(final String key) {
	return strlen(asByte(key));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#substr(java.lang.String, int, int)
     */
    @Override
    public String substr(final String key, final int start, final int end) {
	return asString(substr(asByte(key), start, end));
    }

    @Override
    public Set<String> sunion(final String key1, final String... keyN) {
	return ImmutableSet
		.copyOf(transform(sunion(asByte(key1), asByte(keyN)),
			new ByteToStringFunction()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#sunionstore(java.lang.String,
     * java.lang.String, java.lang.String[])
     */
    @Override
    public Long sunionstore(final String dstKey, final String key1,
	    final String... keyN) {
	return sunionstore(asByte(dstKey), asByte(key1), asByte(keyN));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#ttl(java.lang.String)
     */
    @Override
    public Long ttl(final String key) {
	return ttl(asByte(key));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#type(java.lang.String)
     */
    @Override
    public RedisType type(final String key) {
	return type(asByte(key));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#watch(java.lang.String)
     */
    @Override
    public Boolean watch(final String key) {
	return watch(asByte(key));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#watch(java.lang.String)
     */
    @Override
    public String watch(final String... keys) {
	throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zadd(java.lang.String, double,
     * java.lang.String)
     */
    @Override
    public Boolean zadd(final String key, final double score, final String value) {
	return zadd(asByte(key), score, asByte(value));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zadd(java.lang.String,
     * com.googlecode.jedis.Pair)
     */
    @Override
    public Boolean zadd(final String key, final Pair<String, Double> value) {
	return zadd(asByte(key), value.getSecond(), asByte(value.getFirst()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zcard(java.lang.String)
     */
    @Override
    public Long zcard(final String key) {
	return zcard(asByte(key));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zcount(java.lang.String, double, double)
     */
    @Override
    public Long zcount(final String key, final String min, final String max) {
	return zcount(asByte(key), asByte(min), asByte(max));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zincrby(java.lang.String,
     * java.lang.String, double)
     */
    @Override
    public Double zincrby(final String key, final String member,
	    final double value) {
	return zincrby(asByte(key), asByte(member), value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zinterstoreMax(java.lang.String,
     * com.googlecode.jedis.Pair,
     * com.googlecode.jedis.Pair<java.lang.String,java.lang.Double>[])
     */
    @Override
    public Long zinterstoreMax(final String dstKey,
	    final Pair<String, Double> ssetAndWeight1,
	    final Pair<String, Double>... ssetAndWeightN) {

	@SuppressWarnings("unchecked")
	final Pair<byte[], Double>[] args = new Pair[ssetAndWeightN.length];

	for (int i = 0; i < args.length; i++) {
	    args[i] = newPair(asByte(ssetAndWeightN[i].getFirst()),
		    ssetAndWeightN[i].getSecond());
	}

	return zinterstoreMax(
		asByte(dstKey),
		newPair(asByte(ssetAndWeight1.getFirst()),
			ssetAndWeight1.getSecond()), args);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zinterstoreMin(java.lang.String,
     * com.googlecode.jedis.Pair,
     * com.googlecode.jedis.Pair<java.lang.String,java.lang.Double>[])
     */
    @Override
    public Long zinterstoreMin(final String dstKey,
	    final Pair<String, Double> ssetAndWeight1,
	    final Pair<String, Double>... ssetAndWeightN) {

	@SuppressWarnings("unchecked")
	final Pair<byte[], Double>[] args = new Pair[ssetAndWeightN.length];

	for (int i = 0; i < args.length; i++) {
	    args[i] = newPair(asByte(ssetAndWeightN[i].getFirst()),
		    ssetAndWeightN[i].getSecond());
	}

	return zinterstoreMin(
		asByte(dstKey),
		newPair(asByte(ssetAndWeight1.getFirst()),
			ssetAndWeight1.getSecond()), args);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zinterstoreSum(java.lang.String,
     * com.googlecode.jedis.Pair,
     * com.googlecode.jedis.Pair<java.lang.String,java.lang.Double>[])
     */
    @Override
    public Long zinterstoreSum(final String dstKey,
	    final Pair<String, Double> ssetAndWeight1,
	    final Pair<String, Double>... ssetAndWeightN) {

	@SuppressWarnings("unchecked")
	final Pair<byte[], Double>[] args = new Pair[ssetAndWeightN.length];

	for (int i = 0; i < args.length; i++) {
	    args[i] = newPair(asByte(ssetAndWeightN[i].getFirst()),
		    ssetAndWeightN[i].getSecond());
	}

	return zinterstoreSum(
		asByte(dstKey),
		newPair(asByte(ssetAndWeight1.getFirst()),
			ssetAndWeight1.getSecond()), args);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zrange(java.lang.String, long, long)
     */
    @Override
    public Set<String> zrange(final String key, final long start, final long end) {
	checkNotNull(key);
	return ImmutableSet.copyOf(transform(zrange(asByte(key), start, end),
		new ByteToStringFunction()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zrangeByScore(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public Set<String> zrangeByScore(final String key, final String min,
	    final String max) {
	return ImmutableSet.copyOf(transform(
		zrangeByScore(asByte(key), asByte(min), asByte(max)),
		new ByteToStringFunction()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zrangeByScore(java.lang.String,
     * java.lang.String, java.lang.String, long, long)
     */
    @Override
    public Set<String> zrangeByScore(final String key, final String min,
	    final String max, final long offset, final long count) {
	return ImmutableSet.copyOf(transform(
		zrangeByScore(asByte(key), asByte(min), asByte(max), offset,
			count), new ByteToStringFunction()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zrangeByScoreWithScores(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public Set<Pair<String, Double>> zrangeByScoreWithScores(final String key,
	    final String min, final String max) {
	return ImmutableSet.copyOf(transform(
		zrangeByScoreWithScores(asByte(key), asByte(min), asByte(max)),
		new PairByteDoubleToPairStringDouble()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zrangeByScoreWithScores(java.lang.String,
     * java.lang.String, java.lang.String, long, long)
     */
    @Override
    public Set<Pair<String, Double>> zrangeByScoreWithScores(final String key,
	    final String min, final String max, final long offset,
	    final long count) {
	return ImmutableSet
		.copyOf(transform(
			zrangeByScoreWithScores(asByte(key), asByte(min),
				asByte(max), offset, count),
			new PairByteDoubleToPairStringDouble()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zrangeWithScores(java.lang.String, long,
     * long)
     */
    @Override
    public Set<Pair<String, Double>> zrangeWithScores(final String key,
	    final long start, final long end) {
	return ImmutableSet.copyOf(transform(
		zrangeWithScores(asByte(key), start, end),
		new PairByteDoubleToPairStringDouble()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zrank(java.lang.String, java.lang.String)
     */
    @Override
    public Long zrank(final String key, final String member) {
	return zrank(asByte(key), asByte(member));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zrem(java.lang.String, java.lang.String)
     */
    @Override
    public Boolean zrem(final String key, final String member) {
	return zrem(asByte(key), asByte(member));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zremrangeByRank(java.lang.String, int,
     * int)
     */
    @Override
    public Long zremrangeByRank(final String key, final long start,
	    final long end) {
	return zremrangeByRank(asByte(key), start, end);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zremrangeByScore(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public Long zremrangeByScore(final String key, final String min,
	    final String max) {
	return zremrangeByScore(asByte(key), asByte(min), asByte(max));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zrevrange(java.lang.String, long, long)
     */
    @Override
    public Set<String> zrevrange(final String key, final long start,
	    final long end) {
	return ImmutableSet
		.copyOf(transform(zrevrange(asByte(key), start, end),
			new ByteToStringFunction()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zrevrangeByScore(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public Set<String> zrevrangeByScore(final String key, final String min,
	    final String max) {
	return ImmutableSet.copyOf(transform(
		zrevrangeByScore(asByte(key), asByte(min), asByte(max)),
		new ByteToStringFunction()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zrevrangeByScore(java.lang.String,
     * java.lang.String, java.lang.String, long, long)
     */
    @Override
    public Set<String> zrevrangeByScore(final String key, final String min,
	    final String max, final long offset, final long count) {
	return ImmutableSet.copyOf(transform(
		zrevrangeByScore(asByte(key), asByte(min), asByte(max), offset,
			count), new ByteToStringFunction()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.googlecode.jedis.Jedis#zrevrangeByScoreWithScores(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public Set<Pair<String, Double>> zrevrangeByScoreWithScores(
	    final String key, final String min, final String max) {
	return ImmutableSet.copyOf(transform(
		zrevrangeByScoreWithScores(asByte(key), asByte(min),
			asByte(max)), new PairByteDoubleToPairStringDouble()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.googlecode.jedis.Jedis#zrevrangeByScoreWithScores(java.lang.String,
     * java.lang.String, java.lang.String, long, long)
     */
    @Override
    public Set<Pair<String, Double>> zrevrangeByScoreWithScores(
	    final String key, final String min, final String max,
	    final long offset, final long count) {
	return ImmutableSet.copyOf(transform(
		zrevrangeByScoreWithScores(asByte(key), asByte(min),
			asByte(max), offset, count),
		new PairByteDoubleToPairStringDouble()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zrevrangeWithScores(java.lang.String,
     * long, long)
     */
    @Override
    public Set<Pair<String, Double>> zrevrangeWithScores(final String key,
	    final long start, final long end) {
	return ImmutableSet.copyOf(transform(
		zrevrangeWithScores(asByte(key), start, end),
		new PairByteDoubleToPairStringDouble()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zrevrank(java.lang.String,
     * java.lang.String)
     */
    @Override
    public Long zrevrank(final String key, final String member) {
	return zrevrank(asByte(key), asByte(member));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zscore(java.lang.String,
     * java.lang.String)
     */
    @Override
    public Double zscore(final String key, final String member) {
	return zscore(asByte(key), asByte(member));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zunionstoreMax(java.lang.String,
     * com.googlecode.jedis.Pair,
     * com.googlecode.jedis.Pair<java.lang.String,java.lang.Double>[])
     */
    @Override
    public Long zunionstoreMax(final String dstKey,
	    final Pair<String, Double> ssetAndWeight1,
	    final Pair<String, Double>... ssetAndWeightN) {
	@SuppressWarnings("unchecked")
	final Pair<byte[], Double>[] args = new Pair[ssetAndWeightN.length];

	for (int i = 0; i < args.length; i++) {
	    args[i] = newPair(asByte(ssetAndWeightN[i].getFirst()),
		    ssetAndWeightN[i].getSecond());
	}

	return zunionstoreMax(
		asByte(dstKey),
		newPair(asByte(ssetAndWeight1.getFirst()),
			ssetAndWeight1.getSecond()), args);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zunionstoreMin(java.lang.String,
     * com.googlecode.jedis.Pair,
     * com.googlecode.jedis.Pair<java.lang.String,java.lang.Double>[])
     */
    @Override
    public Long zunionstoreMin(final String dstKey,
	    final Pair<String, Double> ssetAndWeight1,
	    final Pair<String, Double>... ssetAndWeightN) {
	@SuppressWarnings("unchecked")
	final Pair<byte[], Double>[] args = new Pair[ssetAndWeightN.length];

	for (int i = 0; i < args.length; i++) {
	    args[i] = newPair(asByte(ssetAndWeightN[i].getFirst()),
		    ssetAndWeightN[i].getSecond());
	}

	return zunionstoreMin(
		asByte(dstKey),
		newPair(asByte(ssetAndWeight1.getFirst()),
			ssetAndWeight1.getSecond()), args);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zunionstoreSum(java.lang.String,
     * com.googlecode.jedis.Pair,
     * com.googlecode.jedis.Pair<java.lang.String,java.lang.Double>[])
     */
    @Override
    public Long zunionstoreSum(final String dstKey,
	    final Pair<String, Double> ssetAndWeight1,
	    final Pair<String, Double>... ssetAndWeightN) {
	@SuppressWarnings("unchecked")
	final Pair<byte[], Double>[] args = new Pair[ssetAndWeightN.length];

	for (int i = 0; i < args.length; i++) {
	    args[i] = newPair(asByte(ssetAndWeightN[i].getFirst()),
		    ssetAndWeightN[i].getSecond());
	}

	return zunionstoreSum(
		asByte(dstKey),
		newPair(asByte(ssetAndWeight1.getFirst()),
			ssetAndWeight1.getSecond()), args);
    }

}