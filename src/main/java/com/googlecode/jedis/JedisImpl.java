package com.googlecode.jedis;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static com.googlecode.jedis.PairImpl.newPair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

class JedisImpl extends RawJedisImpl implements Jedis {

    static private class BytePairToStringPair implements
	    Function<Pair<byte[], Double>, Pair<String, Double>> {
	@Override
	public Pair<String, Double> apply(Pair<byte[], Double> input) {
	    return newPair(asString(input.getFirst()), input.getSecond());
	}
    }

    private static class ByteToStringFunction implements
	    Function<byte[], String> {
	@Override
	public String apply(byte[] input) {
	    return asString(input);
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
	runChecks();
	client.append(key, value);
	return client.getIntegerReply();
    }

    private String asStringOrNull(byte[] value) {
	return (value != null) ? asString(value) : (String) null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#auth(java.lang.String)
     */
    @Override
    public String auth(final String password) {
	if (password == null) {
	    throw new NullPointerException();
	}
	return auth(asByte(password));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#blpop(long, java.lang.String,
     * java.lang.String[])
     */
    @Override
    public List<String> blpop(final long timeout, final String key1,
	    final String... keyN) {
	runChecks();
	List<String> args = Lists.newArrayList();
	args.add(key1);
	for (final String key : keyN) {
	    args.add(key);
	}
	args.add(String.valueOf(timeout));

	client.blpop(args.toArray(new String[args.size()]));
	client.setTimeoutInfinite();
	final List<String> multiBulkReply = client.getMultiBulkReply();
	client.rollbackTimeout();
	return multiBulkReply;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#brpop(long, java.lang.String,
     * java.lang.String[])
     */
    @Override
    public List<String> brpop(final long timeout, final String key1,
	    final String... keyN) {
	runChecks();
	List<String> args = Lists.newArrayList();
	args.add(key1);
	for (String arg : keyN) {
	    args.add(arg);
	}
	args.add(String.valueOf(timeout));

	client.brpop(args.toArray(new String[args.size()]));
	client.setTimeoutInfinite();
	List<String> multiBulkReply = client.getMultiBulkReply();
	client.rollbackTimeout();

	return multiBulkReply;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#decr(java.lang.String)
     */
    @Override
    public Long decr(final String key) {
	checkNotNull(key);
	return decr(asByte(key));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#decrBy(java.lang.String, long)
     */
    @Override
    public Long decrBy(final String key, final long value) {
	checkNotNull(key);
	return decrBy(asByte(key), value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#del(java.lang.String)
     */
    @Override
    public Long del(String key1, final String... keyN) {
	checkNotNull(key1);
	return del(asByte(key1), asByte(keyN));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#echo(java.lang.String)
     */
    @Override
    public String echo(final String string) {
	client.echo(string);
	return client.getBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#exists(java.lang.String)
     */
    @Override
    public Boolean exists(final String key) {
	runChecks();
	client.exists(key);
	return client.getIntegerReply() == 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#expire(java.lang.String, long)
     */
    @Override
    public Boolean expire(final String key, final long seconds) {
	checkNotNull(key);
	return expire(asByte(key), seconds);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#expireAt(java.lang.String, long)
     */
    @Override
    public Boolean expireAt(final String key, final long unixTime) {
	checkNotNull(key);
	return expireAt(asByte(key), unixTime);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#get(java.lang.String)
     */
    @Override
    public String get(final String key) {
	checkNotNull(key);
	return asStringOrNull(get(asByte(key)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#getSet(java.lang.String,
     * java.lang.String)
     */
    @Override
    public String getSet(final String key, final String value) {
	if (key == null || value == null) {
	    throw new NullPointerException();
	}
	return asString(getSet(asByte(key), asByte(value)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hdel(java.lang.String, java.lang.String)
     */
    @Override
    public Long hdel(final String key, final String field) {
	runChecks();
	client.hdel(key, field);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hexists(java.lang.String,
     * java.lang.String)
     */
    @Override
    public Boolean hexists(final String key, final String field) {
	runChecks();
	client.hexists(key, field);
	return client.getIntegerReply() == 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hget(java.lang.String, java.lang.String)
     */
    @Override
    public String hget(final String key, final String field) {
	runChecks();
	client.hget(key, field);
	return client.getBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hgetAll(java.lang.String)
     */
    @Override
    public Map<String, String> hgetAll(final String key) {
	runChecks();
	client.hgetAll(key);
	final List<String> flatHash = client.getMultiBulkReply();
	final Map<String, String> hash = new HashMap<String, String>();
	final Iterator<String> iterator = flatHash.iterator();
	while (iterator.hasNext()) {
	    hash.put(iterator.next(), iterator.next());
	}

	return hash;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hincrBy(java.lang.String,
     * java.lang.String, long)
     */
    @Override
    public Long hincrBy(final String key, final String field, final long value) {
	runChecks();
	client.hincrBy(key, field, value);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hkeys(java.lang.String)
     */
    @Override
    public Set<String> hkeys(final String key) {
	runChecks();
	client.hkeys(key);
	final List<String> lresult = client.getMultiBulkReply();
	return new HashSet<String>(lresult);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hlen(java.lang.String)
     */
    @Override
    public Long hlen(final String key) {
	runChecks();
	client.hlen(key);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hmget(java.lang.String, java.lang.String)
     */
    @Override
    public List<String> hmget(final String key, final String... fields) {
	runChecks();
	client.hmget(key, fields);
	return client.getMultiBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hmset(java.lang.String, java.util.Map)
     */
    @Override
    public Boolean hmset(final String key, final Map<String, String> hash) {
	runChecks();
	client.hmset(key, hash);
	return client.getStatusCodeReply().equals("OK");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hset(java.lang.String, java.lang.String,
     * java.lang.String)
     */
    @Override
    public Long hset(final String key, final String field, final String value) {
	runChecks();
	client.hset(key, field, value);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hsetnx(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public Long hsetnx(final String key, final String field, final String value) {
	runChecks();
	client.hsetnx(key, field, value);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#hvals(java.lang.String)
     */
    @Override
    public List<String> hvals(final String key) {
	runChecks();
	client.hvals(key);
	final List<String> lresult = client.getMultiBulkReply();
	return lresult;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#incr(java.lang.String)
     */
    @Override
    public Long incr(final String key) {
	checkNotNull(key);
	return incr(asByte(key));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#incrBy(java.lang.String, long)
     */
    @Override
    public Long incrBy(final String key, final long value) {
	checkNotNull(key);
	return incrBy(asByte(key), value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#keys(java.lang.String)
     */
    @Override
    public Set<String> keys(final String pattern) {
	runChecks();
	client.keys(pattern);
	final HashSet<String> keySet = new HashSet<String>(
		client.getMultiBulkReply());
	return keySet;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#lindex(java.lang.String, int)
     */
    @Override
    public String lindex(final String key, final int index) {
	runChecks();
	client.lindex(key, index);
	return client.getBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#linsertAfter(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public Long linsertAfter(String key, String element, String value) {
	client.linsertAfter(key, element, value);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#linsertBefore(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public Long linsertBefore(String key, String element, String value) {
	client.linsertBefore(key, element, value);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#llen(java.lang.String)
     */
    @Override
    public Long llen(final String key) {
	runChecks();
	client.llen(key);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#lpop(java.lang.String)
     */
    @Override
    public String lpop(final String key) {
	runChecks();
	client.lpop(key);
	return client.getBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#lpush(java.lang.String, java.lang.String)
     */
    @Override
    public Long lpush(final String key, final String value) {
	runChecks();
	client.lpush(key, value);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#lpushx(java.lang.String,
     * java.lang.String)
     */
    @Override
    public Long lpushx(final String key, final String value) {
	client.lpushx(key, value);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#lrange(java.lang.String, int, int)
     */
    @Override
    public List<String> lrange(final String key, final long start,
	    final long end) {
	runChecks();
	client.lrange(key, start, end);
	return client.getMultiBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#lrem(java.lang.String, int,
     * java.lang.String)
     */
    @Override
    public Long lrem(final String key, final int count, final String value) {
	runChecks();
	client.lrem(key, count, value);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#lset(java.lang.String, int,
     * java.lang.String)
     */
    @Override
    public String lset(final String key, final int index, final String value) {
	runChecks();
	client.lset(key, index, value);
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#ltrim(java.lang.String, int, int)
     */
    @Override
    public String ltrim(final String key, final int start, final int end) {
	runChecks();
	client.ltrim(key, start, end);
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#mget(java.lang.String)
     */
    @Override
    public List<String> mget(final String... keys) {
	runChecks();
	client.mget(keys);
	return client.getMultiBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#move(java.lang.String, long)
     */
    @Override
    public Boolean move(final String key, final long index) {
	checkNotNull(key);
	return move(asByte(key), index);
    }

    @Override
    public Boolean mset(Pair<String, String> keyValuePair1,
	    Pair<String, String>... keyValuePairN) {
	checkNotNull(keyValuePair1);
	checkNotNull(keyValuePair1.getFirst());
	checkNotNull(keyValuePair1.getSecond());

	@SuppressWarnings("unchecked")
	Pair<byte[], byte[]>[] args = new Pair[keyValuePairN.length];

	for (int i = 0; i < args.length; i++) {
	    args[i] = asByte(keyValuePairN[i]);
	}

	return msetRaw(asByte(keyValuePair1), args);
    }

    @Override
    public Boolean msetnx(Pair<String, String> keyValuePair1,
	    Pair<String, String>... keyValuePairN) {
	checkNotNull(keyValuePair1);
	checkNotNull(keyValuePair1.getFirst());
	checkNotNull(keyValuePair1.getSecond());

	@SuppressWarnings("unchecked")
	Pair<byte[], byte[]>[] args = new Pair[keyValuePairN.length];

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
	checkNotNull(key);
	return persist(asByte(key));
    }

    @Override
    public void pipelined() {
	client.setPipelineMode(true);
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
	checkNotNull(srcKey);
	checkNotNull(dstKey);
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
	checkNotNull(srcKey);
	checkNotNull(dstKey);
	return renamenx(asByte(srcKey), asByte(dstKey));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#select(int)
     */

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#rpop(java.lang.String)
     */
    @Override
    public String rpop(final String key) {
	runChecks();
	client.rpop(key);
	return client.getBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#rpoplpush(java.lang.String,
     * java.lang.String)
     */
    @Override
    public String rpoplpush(final String srckey, final String dstkey) {
	runChecks();
	client.rpoplpush(srckey, dstkey);
	return client.getBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#rpush(java.lang.String, java.lang.String)
     */
    @Override
    public Long rpush(final String key, final String string) {
	runChecks();
	client.rpush(key, string);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#rpushx(java.lang.String,
     * java.lang.String)
     */
    @Override
    public Long rpushx(final String key, final String string) {
	client.rpushx(key, string);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#sadd(java.lang.String, java.lang.String)
     */
    @Override
    public Boolean sadd(final String key, final String member) {
	runChecks();
	client.sadd(key, member);
	return client.getIntegerReply().equals(1L);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#scard(java.lang.String)
     */
    @Override
    public Long scard(final String key) {
	runChecks();
	client.scard(key);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#sdiff(java.lang.String,
     * java.lang.String[])
     */
    @Override
    public Set<String> sdiff(final String key1, final String... keyN) {
	if (key1 == null) {
	    throw new NullPointerException();
	}
	runChecks();
	return Sets.newLinkedHashSet(transform(
		sdiff(asByte(key1), asByte(keyN)), new ByteToStringFunction()));
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
	runChecks();
	return sdiffstore(asByte(dstKey), asByte(key1), asByte(keyN));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#set(java.lang.String, java.lang.String)
     */
    @Override
    public Boolean set(final String key, final String value) {
	checkNotNull(key);
	checkNotNull(value);

	return set(asByte(key), asByte(value));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#setex(java.lang.String, int,
     * java.lang.String)
     */
    @Override
    public String setex(final String key, final int seconds, final String value) {
	runChecks();
	client.setex(key, seconds, value);
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#setnx(java.lang.String, java.lang.String)
     */
    @Override
    public Boolean setnx(final String key, final String value) {
	checkNotNull(key);
	checkNotNull(value);
	return setnx(asByte(key), asByte(value));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#sinter(java.lang.String)
     */
    @Override
    public Set<String> sinter(String key1, final String... keyN) {
	runChecks();
	client.sinter(key1, keyN);
	final List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#sinterstore(java.lang.String,
     * java.lang.String, java.lang.String[])
     */
    @Override
    public Long sinterstore(final String dstkey, String srcKey1,
	    final String... scrKeyN) {
	if (dstkey == null || srcKey1 == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sinterstore(dstkey, srcKey1, scrKeyN);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#sismember(java.lang.String,
     * java.lang.String)
     */
    @Override
    public Boolean sismember(final String key, final String member) {
	if (key == null || member == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sismember(key, member);
	return client.getIntegerReply().equals(1L);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#smembers(java.lang.String)
     */
    @Override
    public Set<String> smembers(final String key) {
	runChecks();
	client.smembers(key);
	final List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
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
	runChecks();
	client.smove(srckey, dstkey, member);
	return client.getIntegerReply().equals(1L);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#sort(java.lang.String)
     */
    @Override
    public List<String> sort(final String key) {
	runChecks();
	client.sort(key);
	return client.getMultiBulkReply();
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
	runChecks();
	client.sort(key, sortingParameters);
	return client.getMultiBulkReply();
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
	runChecks();
	client.sort(key, sortingParameters, dstkey);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#sort(java.lang.String, java.lang.String)
     */
    @Override
    public Long sort(final String key, final String dstkey) {
	runChecks();
	client.sort(key, dstkey);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#spop(java.lang.String)
     */
    @Override
    public String spop(final String key) {
	runChecks();
	client.spop(key);
	return client.getBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#srandmember(java.lang.String)
     */
    @Override
    public String srandmember(final String key) {
	if (key == null) {
	    throw new NullPointerException();
	}
	runChecks();
	byte[] ret = srandmember(asByte(key));
	return (ret != null) ? asString(ret) : null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#srem(java.lang.String, java.lang.String)
     */
    @Override
    public Boolean srem(final String key, final String member) {
	runChecks();
	client.srem(key, member);
	return client.getIntegerReply().equals(1L);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#strlen(java.lang.String)
     */
    @Override
    public Long strlen(final String key) {
	client.strlen(key);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.googlecode.jedis.Jedis#subscribe(com.googlecode.jedis.JedisPubSub,
     * java.lang.String)
     */
    @Override
    public void subscribe(JedisPubSub jedisPubSub, String... channels) {
	client.setTimeoutInfinite();
	jedisPubSub.proceed(client, channels);
	client.rollbackTimeout();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#sunion(java.lang.String)
     */
    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#substr(java.lang.String, int, int)
     */
    @Override
    public String substr(final String key, final int start, final int end) {
	runChecks();
	client.substr(key, start, end);
	return client.getBulkReply();
    }

    @Override
    public Set<String> sunion(String key1, final String... keyN) {
	if (key1 == null) {
	    throw new NullPointerException();
	}
	return Sets
		.newLinkedHashSet(transform(sunion(asByte(key1), asByte(keyN)),
			new ByteToStringFunction()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#sunionstore(java.lang.String,
     * java.lang.String, java.lang.String[])
     */
    @Override
    public Long sunionstore(final String dstKey, String key1,
	    final String... keyN) {
	if (dstKey == null || key1 == null) {
	    throw new NullPointerException();
	}
	return sunionstore(asByte(dstKey), asByte(key1), asByte(keyN));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#ttl(java.lang.String)
     */
    @Override
    public Long ttl(final String key) {
	runChecks();
	client.ttl(key);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#type(java.lang.String)
     */
    @Override
    public String type(final String key) {
	runChecks();
	client.type(key);
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#watch(java.lang.String)
     */
    @Override
    public String watch(final String... keys) {
	client.watch(keys);
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zadd(java.lang.String, double,
     * java.lang.String)
     */
    @Override
    public Boolean zadd(final String key, final double score, final String value) {
	checkNotNull(key);
	checkNotNull(value);
	return zadd(asByte(key), score, asByte(value));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zadd(java.lang.String,
     * com.googlecode.jedis.Pair)
     */
    @Override
    public Boolean zadd(String key, Pair<String, Double> value) {
	checkNotNull(key);
	checkNotNull(value);
	return zadd(asByte(key), value.getSecond(), asByte(value.getFirst()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zcard(java.lang.String)
     */
    @Override
    public Long zcard(final String key) {
	checkNotNull(key);
	return zcard(asByte(key));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zcount(java.lang.String, double, double)
     */
    @Override
    public Long zcount(final String key, final String min, final String max) {
	checkNotNull(key);
	checkNotNull(min);
	checkNotNull(max);
	return zcount(asByte(key), asByte(min), asByte(max));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zincrby(java.lang.String,
     * java.lang.String, double)
     */
    @Override
    public Double zincrby(String key, String member, double value) {
	checkNotNull(key);
	checkNotNull(member);
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
	    Pair<String, Double> ssetAndWeight1,
	    Pair<String, Double>... ssetAndWeightN) {
	checkNotNull(dstKey);
	checkNotNull(ssetAndWeight1);

	@SuppressWarnings("unchecked")
	Pair<byte[], Double>[] args = new Pair[ssetAndWeightN.length];

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
	    Pair<String, Double> ssetAndWeight1,
	    Pair<String, Double>... ssetAndWeightN) {
	checkNotNull(dstKey);
	checkNotNull(ssetAndWeight1);

	@SuppressWarnings("unchecked")
	Pair<byte[], Double>[] args = new Pair[ssetAndWeightN.length];

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
	    Pair<String, Double> ssetAndWeight1,
	    Pair<String, Double>... ssetAndWeightN) {
	checkNotNull(dstKey);
	checkNotNull(ssetAndWeight1);

	@SuppressWarnings("unchecked")
	Pair<byte[], Double>[] args = new Pair[ssetAndWeightN.length];

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

	return newLinkedHashSet(transform(zrange(asByte(key), start, end),
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
	checkNotNull(key);
	checkNotNull(min);
	checkNotNull(max);

	return Sets.newLinkedHashSet(transform(
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
	checkNotNull(key);
	checkNotNull(min);
	checkNotNull(max);

	return Sets.newLinkedHashSet(transform(
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
    public Set<Pair<String, Double>> zrangeByScoreWithScores(String key,
	    String min, String max) {
	checkNotNull(key);
	checkNotNull(min);
	checkNotNull(max);

	return Sets.newLinkedHashSet(transform(
		zrangeByScoreWithScores(asByte(key), asByte(min), asByte(max)),
		new BytePairToStringPair()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zrangeByScoreWithScores(java.lang.String,
     * java.lang.String, java.lang.String, long, long)
     */
    @Override
    public Set<Pair<String, Double>> zrangeByScoreWithScores(String key,
	    String min, String max, long offset, long count) {
	checkNotNull(key);
	checkNotNull(min);
	checkNotNull(max);
	return Sets.newLinkedHashSet(transform(
		zrangeByScoreWithScores(asByte(key), asByte(min), asByte(max),
			offset, count), new BytePairToStringPair()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zrangeWithScores(java.lang.String, long,
     * long)
     */
    @Override
    public Set<Pair<String, Double>> zrangeWithScores(String key, long start,
	    long end) {
	checkNotNull(key);
	return newLinkedHashSet(transform(
		zrangeWithScores(asByte(key), start, end),
		new BytePairToStringPair()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zrank(java.lang.String, java.lang.String)
     */
    @Override
    public Long zrank(final String key, final String member) {
	checkNotNull(key);
	checkNotNull(member);
	return zrank(asByte(key), asByte(member));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zrem(java.lang.String, java.lang.String)
     */
    @Override
    public Boolean zrem(final String key, final String member) {
	checkNotNull(key);
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
	checkNotNull(key);
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
	checkNotNull(key);
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
	checkNotNull(key);
	return newLinkedHashSet(transform(zrevrange(asByte(key), start, end),
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
	checkNotNull(key);
	checkNotNull(min);
	checkNotNull(max);

	return Sets.newLinkedHashSet(transform(
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
	checkNotNull(key);
	checkNotNull(min);
	checkNotNull(max);

	return Sets.newLinkedHashSet(transform(
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
    public Set<Pair<String, Double>> zrevrangeByScoreWithScores(String key,
	    String min, String max) {
	checkNotNull(key);
	checkNotNull(min);
	checkNotNull(max);

	return Sets.newLinkedHashSet(transform(
		zrevrangeByScoreWithScores(asByte(key), asByte(min),
			asByte(max)), new BytePairToStringPair()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.googlecode.jedis.Jedis#zrevrangeByScoreWithScores(java.lang.String,
     * java.lang.String, java.lang.String, long, long)
     */
    @Override
    public Set<Pair<String, Double>> zrevrangeByScoreWithScores(String key,
	    String min, String max, long offset, long count) {
	checkNotNull(key);
	checkNotNull(min);
	checkNotNull(max);

	return Sets
		.newLinkedHashSet(transform(
			zrevrangeByScoreWithScores(asByte(key), asByte(min),
				asByte(max), offset, count),
			new BytePairToStringPair()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zrevrangeWithScores(java.lang.String,
     * long, long)
     */
    @Override
    public Set<Pair<String, Double>> zrevrangeWithScores(String key,
	    long start, long end) {
	checkNotNull(key);
	return newLinkedHashSet(transform(
		zrevrangeWithScores(asByte(key), start, end),
		new BytePairToStringPair()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#zrevrank(java.lang.String,
     * java.lang.String)
     */
    @Override
    public Long zrevrank(final String key, final String member) {
	checkNotNull(key);
	checkNotNull(member);
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
	checkNotNull(key);
	checkNotNull(member);
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
	    Pair<String, Double> ssetAndWeight1,
	    Pair<String, Double>... ssetAndWeightN) {
	checkNotNull(dstKey);
	checkNotNull(ssetAndWeight1);

	@SuppressWarnings("unchecked")
	Pair<byte[], Double>[] args = new Pair[ssetAndWeightN.length];

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
	    Pair<String, Double> ssetAndWeight1,
	    Pair<String, Double>... ssetAndWeightN) {
	checkNotNull(dstKey);
	checkNotNull(ssetAndWeight1);

	@SuppressWarnings("unchecked")
	Pair<byte[], Double>[] args = new Pair[ssetAndWeightN.length];

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
	    Pair<String, Double> ssetAndWeight1,
	    Pair<String, Double>... ssetAndWeightN) {
	checkNotNull(dstKey);
	checkNotNull(ssetAndWeight1);

	@SuppressWarnings("unchecked")
	Pair<byte[], Double>[] args = new Pair[ssetAndWeightN.length];

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