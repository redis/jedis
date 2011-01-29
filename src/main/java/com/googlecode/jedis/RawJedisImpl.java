package com.googlecode.jedis;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.jedis.PairImpl.newPair;
import static com.googlecode.jedis.Protocol.DEFAULT_CHARSET;
import static com.googlecode.jedis.Protocol.Command.*;
import static com.googlecode.jedis.util.Encoders.asByte;
import static com.googlecode.jedis.util.Encoders.asString;
import static java.lang.System.arraycopy;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

class RawJedisImpl implements RawJedis {

    protected static final Logger log = LoggerFactory.getLogger(Jedis.class);

    protected Connection conn;

    protected RawJedisImpl() {
	conn = new NettyConnection();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#append(byte[], byte[])
     */
    @Override
    public Long append(final byte[] key, final byte[] value) {

	conn.sendCommand(APPEND, value);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#auth(byte[])
     */
    @Override
    public Boolean auth(final byte[] password) {
	conn.sendCommand(AUTH, password);
	return conn.statusCodeReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#bgrewriteaof()
     */
    @Override
    public Boolean bgrewriteaof() {
	conn.sendCommand(BGREWRITEAOF);
	return Arrays.equals("Background append only file rewriting started"
		.getBytes(DEFAULT_CHARSET), conn.statusCodeReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#bgsave()
     */
    @Override
    public Boolean bgsave() {
	conn.sendCommand(BGSAVE);
	return Arrays.equals(
		"Background saving started".getBytes(DEFAULT_CHARSET),
		conn.statusCodeReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#blpop(long, byte[], byte[][])
     */
    @Override
    public List<Pair<byte[], byte[]>> blpopRaw(final long timeout,
	    final byte[] key1, final byte[]... keyN) {
	checkNotNull(key1);

	final byte[][] args = new byte[2 + keyN.length][];
	args[0] = key1;
	args[1 + keyN.length] = asByte(timeout);
	arraycopy(keyN, 0, args, 1, keyN.length);

	conn.sendCommand(BLPOP, args);
	conn.setTimeoutInfinite();

	final List<byte[]> multiBulkReply = conn.multiBulkReply();
	conn.rollbackTimeout();

	final List<Pair<byte[], byte[]>> result = Lists
		.newArrayListWithCapacity(multiBulkReply.size() / 2);
	for (final Iterator<byte[]> it = multiBulkReply.iterator(); it
		.hasNext();) {
	    result.add(newPair(it.next(), it.next()));
	}

	return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#brpop(long, byte[], byte[][])
     */
    @Override
    public List<Pair<byte[], byte[]>> brpopRaw(final long timeout,
	    final byte[] key1, final byte[]... keyN) {
	checkNotNull(key1);

	final byte[][] args = new byte[2 + keyN.length][];
	args[0] = key1;
	args[1 + keyN.length] = asByte(timeout);
	arraycopy(keyN, 0, args, 1, keyN.length);

	conn.sendCommand(BRPOP, args);
	conn.setTimeoutInfinite();
	final List<byte[]> multiBulkReply = conn.multiBulkReply();
	conn.rollbackTimeout();
	final List<Pair<byte[], byte[]>> result = Lists
		.newArrayListWithCapacity(multiBulkReply.size() / 2);
	for (final Iterator<byte[]> it = multiBulkReply.iterator(); it
		.hasNext();) {
	    result.add(newPair(it.next(), it.next()));
	}

	return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#configGet(byte[])
     */
    @Override
    public List<byte[]> configGet(final byte[] pattern) {
	conn.sendCommand(CONFIG, Protocol.Keyword.GET.raw, pattern);
	return conn.multiBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#configSet(byte[], byte[])
     */
    @Override
    public byte[] configSet(final byte[] parameter, final byte[] value) {
	conn.sendCommand(CONFIG, Protocol.Keyword.SET.raw, parameter, value);
	return conn.statusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.Jedis#connect()
     */
    @Override
    public void connect() throws UnknownHostException, IOException {
	if (!conn.isConnected()) {
	    try {
		conn.connect();
	    } catch (final Throwable e) {
		log.error("could not connect: ", e);
	    }
	    if (conn.getJedisConfig().getPassword() != null) {
		auth(conn.getJedisConfig().getPassword().getBytes(UTF_8));
	    }
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#dbSize()
     */
    @Override
    public Long dbSize() {

	conn.sendCommand(DBSIZE);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.googlecode.jedis.RawJedis#debug(com.googlecode.jedis.DebugParams)
     */
    @Override
    public byte[] debugRaw(final DebugParams params) {
	conn.sendCommand(DEBUG, params.getCommand());
	return conn.statusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#decr(byte[])
     */
    @Override
    public Long decr(final byte[] key) {
	checkNotNull(key);

	conn.sendCommand(DECR, key);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#decrBy(byte[], long)
     */
    @Override
    public Long decrBy(final byte[] key, final long value) {
	checkNotNull(key);

	conn.sendCommand(DECRBY, key, asByte(value));
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#del(byte)
     */
    @Override
    public Long del(final byte[] key1, final byte[]... keyN) {
	checkNotNull(key1);

	final byte[][] args = new byte[1 + keyN.length][];
	args[0] = key1;
	System.arraycopy(keyN, 0, args, 1, keyN.length);
	conn.sendCommand(DEL, args);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#disconnect()
     */
    @Override
    public void disconnect() {
	conn.disconnect();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#echo(byte[])
     */
    @Override
    public byte[] echo(final byte[] string) {
	checkNotNull(string);

	conn.sendCommand(ECHO, string);
	return conn.bulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#exists(byte[])
     */
    @Override
    public Boolean exists(final byte[] key) {

	conn.sendCommand(EXISTS, key);
	return conn.integerReply() == 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#expire(byte[], long)
     */
    @Override
    public Boolean expire(final byte[] key, final long seconds) {
	checkNotNull(key);

	conn.sendCommand(EXPIRE, key, asByte(seconds));
	return conn.integerReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#expireAt(byte[], long)
     */
    @Override
    public Boolean expireAt(final byte[] key, final long unixTime) {
	checkNotNull(key);

	conn.sendCommand(EXPIREAT, key, asByte(unixTime));
	return conn.integerReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#flushAll()
     */
    @Override
    public Boolean flushAll() {
	conn.sendCommand(FLUSHALL);
	return conn.statusCodeReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#flushDB()
     */
    @Override
    public Boolean flushDB() {

	conn.sendCommand(FLUSHDB);
	return conn.statusCodeReply().equals("OK");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#get(byte[])
     */
    @Override
    public byte[] get(final byte[] key) {
	checkNotNull(key);
	conn.sendCommand(GET, key);
	return conn.bulkReply();
    }

    private Set<Pair<byte[], Double>> getBinaryPairSet() {
	final List<byte[]> membersWithScores = conn.multiBulkReply();
	final Iterator<byte[]> iterator = membersWithScores.iterator();
	final Set<Pair<byte[], Double>> set = Sets.newLinkedHashSet();

	while (iterator.hasNext()) {
	    set.add(newPair(iterator.next(),
		    Double.valueOf(asString(iterator.next()))));
	}
	return ImmutableSet.copyOf(set);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#getClient()
     */
    public Connection getClient() {
	return conn;
    }

    @Override
    public JedisConfig getJedisConfig() {
	return conn.getJedisConfig();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#getSet(byte[], byte[])
     */
    @Override
    public byte[] getSet(final byte[] key, final byte[] value) {
	if (key == null || value == null) {
	    throw new NullPointerException();
	}

	conn.sendCommand(GETSET, key, value);
	return conn.bulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#hdel(byte[], byte[])
     */
    @Override
    public Long hdel(final byte[] key, final byte[] field) {
	checkNotNull(key);
	checkNotNull(field);

	conn.sendCommand(HDEL, key, field);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#hexists(byte[], byte[])
     */
    @Override
    public Boolean hexists(final byte[] key, final byte[] field) {
	checkNotNull(key);
	checkNotNull(field);

	conn.sendCommand(HEXISTS, key, field);
	return conn.integerReply() == 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#hget(byte[], byte[])
     */
    @Override
    public byte[] hget(final byte[] key, final byte[] field) {
	checkNotNull(key);
	checkNotNull(field);

	conn.sendCommand(HGET, key, field);
	return conn.bulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#hgetAll(byte[])
     */
    @Override
    public Map<byte[], byte[]> hgetAll(final byte[] key) {
	checkNotNull(key);

	conn.sendCommand(HGETALL, key);
	List<byte[]> flatHash = conn.multiBulkReply();
	final Map<byte[], byte[]> result = Maps
		.newHashMapWithExpectedSize(flatHash.size() / 2);
	final Iterator<byte[]> iterator = flatHash.iterator();
	while (iterator.hasNext()) {
	    result.put(iterator.next(), iterator.next());
	}
	return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#hincrBy(byte[], byte[], long)
     */
    @Override
    public Long hincrBy(final byte[] key, final byte[] field, final long value) {
	checkNotNull(key);
	checkNotNull(field);

	conn.sendCommand(HINCRBY, key, field, asByte(value));
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#hkeys(byte[])
     */
    @Override
    public Set<byte[]> hkeys(final byte[] key) {
	checkNotNull(key);

	conn.sendCommand(HKEYS, key);
	final List<byte[]> lresult = conn.multiBulkReply();
	return new LinkedHashSet<byte[]>(lresult);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#hlen(byte[])
     */
    @Override
    public Long hlen(final byte[] key) {
	checkNotNull(key);

	conn.sendCommand(HLEN, key);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#hmget(byte[], byte)
     */
    @Override
    public List<byte[]> hmget(final byte[] key, final byte[]... fields) {
	checkNotNull(key);
	// checkNotNull(field);
	final byte[][] args = new byte[1 + fields.length][];
	args[0] = key;
	arraycopy(fields, 0, args, 1, fields.length);

	conn.sendCommand(HMGET, args);
	return conn.multiBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#hmset(byte[], java.util.Map)
     */
    @Override
    public Boolean hmset(final byte[] key, final Map<byte[], byte[]> hash) {
	checkNotNull(key);
	checkArgument(!(hash.isEmpty()));
	final int hashElements = hash.size() * 2;
	final byte[][] args = new byte[1 + hashElements][];
	int i = 0;
	args[i++] = key;
	for (final Map.Entry<byte[], byte[]> it : hash.entrySet()) {
	    args[i++] = it.getKey();
	    args[i++] = it.getValue();
	}

	conn.sendCommand(HMSET, args);
	return conn.statusCodeReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#hset(byte[], byte[], byte[])
     */
    @Override
    public Long hset(final byte[] key, final byte[] field, final byte[] value) {
	checkNotNull(key);
	checkNotNull(field);
	checkNotNull(value);

	conn.sendCommand(HSET, key, field, value);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#hsetnx(byte[], byte[], byte[])
     */
    @Override
    public Long hsetnx(final byte[] key, final byte[] field, final byte[] value) {
	checkNotNull(key);
	checkNotNull(field);
	checkNotNull(value);

	conn.sendCommand(HSETNX, key, field, value);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#hvals(byte[])
     */
    @Override
    public List<byte[]> hvals(final byte[] key) {
	checkNotNull(key);

	conn.sendCommand(HVALS, key);
	final List<byte[]> lresult = conn.multiBulkReply();
	return lresult;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#incr(byte[])
     */
    @Override
    public Long incr(final byte[] key) {
	checkNotNull(key);

	conn.sendCommand(INCR, key);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#incrBy(byte[], long)
     */
    @Override
    public Long incrBy(final byte[] key, final long value) {
	checkNotNull(key);

	conn.sendCommand(INCRBY, key, asByte(value));
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#info()
     */
    @Override
    public byte[] info() {
	conn.sendCommand(INFO);
	return conn.bulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#isConnected()
     */
    @Override
    public boolean isConnected() {
	return conn.isConnected();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#keys(byte[])
     */
    @Override
    public Set<byte[]> keys(final byte[] pattern) {

	conn.sendCommand(KEYS, pattern);
	return ImmutableSet.copyOf(conn.multiBulkReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#lastsave()
     */
    @Override
    public Long lastsave() {
	conn.sendCommand(LASTSAVE);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#lindex(byte[], int)
     */
    @Override
    public byte[] lindex(final byte[] key, final int index) {
	checkNotNull(key);
	checkNotNull(index);

	conn.sendCommand(LINDEX, key, asByte(index));
	return conn.bulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#linsertAfter(byte[], byte[], byte[])
     */
    @Override
    public Long linsertAfter(final byte[] key, final byte[] element,
	    final byte[] value) {
	checkNotNull(key);
	checkNotNull(element);
	checkNotNull(value);
	conn.sendCommand(LINSERT, key, asByte("AFTER"), element, value);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#linsertBefore(byte[], byte[], byte[])
     */
    @Override
    public Long linsertBefore(final byte[] key, final byte[] element,
	    final byte[] value) {
	checkNotNull(key);
	checkNotNull(element);
	checkNotNull(value);
	conn.sendCommand(LINSERT, key, asByte("BEFORE"), element, value);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#llen(byte[])
     */
    @Override
    public Long llen(final byte[] key) {
	checkNotNull(key);

	conn.sendCommand(LLEN, key);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#lpop(byte[])
     */
    @Override
    public byte[] lpop(final byte[] key) {
	checkNotNull(key);
	conn.sendCommand(LPOP, key);
	return conn.bulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#lpush(byte[], byte[])
     */
    @Override
    public Long lpush(final byte[] key, final byte[] value) {
	checkNotNull(key);
	checkNotNull(value);
	conn.sendCommand(LPUSH, key, value);
	return conn.integerReply();
    }

    @Override
    public Long lpushRaw(final Pair<byte[], byte[]> keyValuePair) {
	return lpush(keyValuePair.getFirst(), keyValuePair.getSecond());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#lpushx(byte[], byte[])
     */
    @Override
    public Long lpushx(final byte[] key, final byte[] value) {
	checkNotNull(key);
	checkNotNull(value);
	conn.sendCommand(LPUSHX, key, value);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#lpushxRaw(com.googlecode.jedis.Pair)
     */
    @Override
    public Long lpushxRaw(final Pair<byte[], byte[]> keyValuePair) {
	checkNotNull(keyValuePair);
	return lpushx(keyValuePair.getFirst(), keyValuePair.getSecond());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#lrange(byte[], int, int)
     */
    @Override
    public List<byte[]> lrange(final byte[] key, final long start,
	    final long end) {
	checkNotNull(key);
	conn.sendCommand(LRANGE, key, asByte(start), asByte(end));
	return conn.multiBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#lrem(byte[], int, byte[])
     */
    @Override
    public Long lrem(final byte[] key, final int count, final byte[] value) {
	checkNotNull(key);
	conn.sendCommand(LREM, key, asByte(count), value);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#lset(byte[], int, byte[])
     */
    @Override
    public Boolean lset(final byte[] key, final int index, final byte[] value) {
	checkNotNull(key);
	checkNotNull(value);
	conn.sendCommand(LSET, key, asByte(index), value);
	return conn.statusCodeReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#ltrim(byte[], int, int)
     */
    @Override
    public Boolean ltrim(final byte[] key, final int start, final int end) {
	checkNotNull(key);
	conn.sendCommand(LTRIM, key, asByte(start), asByte(end));
	return conn.statusCodeReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#mget(byte)
     */
    @Override
    public List<byte[]> mget(final byte[]... keys) {
	conn.sendCommand(MGET, keys);
	return conn.multiBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.googlecode.jedis.RawJedis#monitor(com.googlecode.jedis.JedisMonitor)
     */
    @Override
    public void monitor(final JedisMonitor jedisMonitor) {
	conn.sendCommand(MONITOR);
	jedisMonitor.proceed(conn);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#move(byte[], long)
     */
    @Override
    public Boolean move(final byte[] key, final long index) {
	checkNotNull(key);
	checkArgument(0L <= index && index < 16L);

	conn.sendCommand(MOVE, key, asByte(index));
	return conn.integerReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#msetnx(com.googlecode.jedis.Pair,
     * com.googlecode.jedis.Pair<byte[],byte[]>[])
     */
    @Override
    public Boolean msetnxRaw(final Pair<byte[], byte[]> keyValuePair1,
	    final Pair<byte[], byte[]>... keyValuePairN) {
	checkNotNull(keyValuePair1);
	checkNotNull(keyValuePair1.getFirst());
	checkNotNull(keyValuePair1.getSecond());

	final List<byte[]> keysAndVals = Lists.newArrayList();
	keysAndVals.add(keyValuePair1.getFirst());
	keysAndVals.add(keyValuePair1.getSecond());

	for (final Pair<byte[], byte[]> it : keyValuePairN) {
	    keysAndVals.add(it.getFirst());
	    keysAndVals.add(it.getSecond());
	}

	conn.sendCommand(MSETNX,
		keysAndVals.toArray(new byte[keysAndVals.size()][]));
	return conn.integerReply() == 1L;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#mset(com.googlecode.jedis.Pair,
     * com.googlecode.jedis.Pair<byte[],byte[]>[])
     */
    @Override
    public Boolean msetRaw(final Pair<byte[], byte[]> keyValuePair1,
	    final Pair<byte[], byte[]>... keyValuePairN) {
	checkNotNull(keyValuePair1);
	checkNotNull(keyValuePair1.getFirst());
	checkNotNull(keyValuePair1.getSecond());

	final List<byte[]> keysAndVals = Lists.newArrayList();
	keysAndVals.add(keyValuePair1.getFirst());
	keysAndVals.add(keyValuePair1.getSecond());

	for (final Pair<byte[], byte[]> it : keyValuePairN) {
	    keysAndVals.add(it.getFirst());
	    keysAndVals.add(it.getSecond());
	}

	conn.sendCommand(MSET,
		keysAndVals.toArray(new byte[keysAndVals.size()][]));
	return conn.statusCodeReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#persist(byte[])
     */
    @Override
    public Boolean persist(final byte[] key) {
	checkNotNull(key);
	conn.sendCommand(PERSIST, key);
	return conn.integerReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#ping()
     */
    @Override
    public Boolean ping() {
	conn.sendCommand(PING);
	return Arrays.equals(conn.statusCodeReply(), Protocol.Keyword.PONG.raw);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#quit()
     */
    @Override
    public void quit() {
	conn.sendCommand(QUIT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#randomKeyRaw()
     */
    @Override
    public byte[] randomKeyRaw() {
	conn.sendCommand(RANDOMKEY);
	return conn.bulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#rename(byte[], byte[])
     */
    @Override
    public Boolean rename(final byte[] srcKey, final byte[] dstKey) {
	checkNotNull(srcKey);
	checkNotNull(dstKey);

	conn.sendCommand(RENAME, srcKey, dstKey);
	return conn.statusCodeReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#renamenx(byte[], byte[])
     */
    @Override
    public Boolean renamenx(final byte[] srcKey, final byte[] dstKey) {
	checkNotNull(srcKey);
	checkNotNull(dstKey);

	conn.sendCommand(RENAMENX, srcKey, dstKey);
	return conn.integerReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#rpop(byte[])
     */
    @Override
    public byte[] rpop(final byte[] key) {
	checkNotNull(key);
	conn.sendCommand(RPOP, key);
	return conn.bulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#rpoplpush(byte[], byte[])
     */
    @Override
    public byte[] rpoplpush(final byte[] srckey, final byte[] dstkey) {
	checkNotNull(srckey);
	checkNotNull(dstkey);
	conn.sendCommand(RPOPLPUSH, srckey, dstkey);
	return conn.bulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#rpush(byte[], byte[])
     */
    @Override
    public Long rpush(final byte[] key, final byte[] value) {
	checkNotNull(key);
	checkNotNull(value);
	conn.sendCommand(RPUSH, key, value);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#rpushRaw(com.googlecode.jedis.Pair)
     */
    @Override
    public Long rpushRaw(final Pair<byte[], byte[]> keyValuePair) {
	checkNotNull(keyValuePair);
	return rpush(keyValuePair.getFirst(), keyValuePair.getSecond());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#rpushx(byte[], byte[])
     */
    @Override
    public Long rpushx(final byte[] key, final byte[] value) {
	checkNotNull(key);
	checkNotNull(value);
	conn.sendCommand(RPUSHX, key, value);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#rpushxRaw(com.googlecode.jedis.Pair)
     */
    @Override
    public Long rpushxRaw(final Pair<byte[], byte[]> keyValuePair) {
	checkNotNull(keyValuePair);
	return rpush(keyValuePair.getFirst(), keyValuePair.getSecond());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#sadd(byte[], byte[])
     */
    @Override
    public Boolean sadd(final byte[] key, final byte[] member) {
	checkNotNull(key);
	checkNotNull(member);
	conn.sendCommand(SADD, key, member);
	return conn.integerReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#save()
     */
    @Override
    public Boolean save() {
	conn.sendCommand(SAVE);
	return conn.statusCodeReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#scard(byte[])
     */
    @Override
    public Long scard(final byte[] key) {
	checkNotNull(key);

	conn.sendCommand(SCARD, key);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#sdiff(byte[], byte[][])
     */
    @Override
    public Set<byte[]> sdiff(final byte[] key1, final byte[]... keyN) {
	if (key1 == null) {
	    throw new NullPointerException();
	}

	final byte[][] args = new byte[keyN.length + 1][];
	args[0] = key1;
	arraycopy(keyN, 0, args, 1, keyN.length);
	conn.sendCommand(SDIFF, args);
	return Sets.newLinkedHashSet(conn.multiBulkReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#sdiffstore(byte[], byte[], byte[][])
     */
    @Override
    public Long sdiffstore(final byte[] dstKey, final byte[] key1,
	    final byte[]... keyN) {
	if (dstKey == null || key1 == null) {
	    throw new NullPointerException();
	}

	final byte[][] args = new byte[keyN.length + 2][];
	args[0] = dstKey;
	args[1] = key1;
	arraycopy(keyN, 0, args, 2, keyN.length);
	conn.sendCommand(SDIFFSTORE, args);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#select(int)
     */
    @Override
    public Boolean select(final long index) {
	checkArgument(0L <= index && index < 16L);

	conn.sendCommand(SELECT, asByte(asString(index)));
	return conn.statusCodeReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#set(byte[], byte[])
     */
    @Override
    public Boolean set(final byte[] key, final byte[] value) {
	checkNotNull(key);
	checkNotNull(value);
	conn.sendCommand(SET, key, value);
	return conn.statusCodeReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#setex(byte[], int, byte[])
     */
    @Override
    public Boolean setex(final byte[] key, final byte[] value, final int seconds) {
	checkNotNull(key);
	checkNotNull(value);
	conn.sendCommand(SETEX, key, asByte(seconds), value);
	return conn.statusCodeReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#setex(com.googlecode.jedis.Pair, int)
     */
    @Override
    public Boolean setexRaw(final Pair<byte[], byte[]> keyValuePair,
	    final int seconds) {
	checkNotNull(keyValuePair);
	return setex(keyValuePair.getFirst(), keyValuePair.getSecond(), seconds);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.googlecode.jedis.RawJedis#setJedisConfig(com.googlecode.jedis.JedisConfig
     * )
     */
    @Override
    public void setJedisConfig(final JedisConfig config) {
	conn.setJedisConfig(config);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#setnx(byte[], byte[])
     */
    @Override
    public Boolean setnx(final byte[] key, final byte[] value) {
	checkNotNull(key);
	checkNotNull(value);

	conn.sendCommand(SETNX, key, value);
	return conn.integerReply() == 1L;
    }

    @Override
    public Boolean setnxRaw(final Pair<byte[], byte[]> keyValuePair) {
	checkNotNull(keyValuePair);
	return setnx(keyValuePair.getFirst(), keyValuePair.getSecond());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#setRaw(com.googlecode.jedis.Pair)
     */
    @Override
    public Boolean setRaw(final Pair<byte[], byte[]> keyValuePair) {
	checkNotNull(keyValuePair);
	return set(keyValuePair.getFirst(), keyValuePair.getSecond());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#shutdown()
     */
    @Override
    public Boolean shutdown() {
	conn.sendCommand(SHUTDOWN);
	Boolean status = null;
	try {
	    status = conn.statusCodeReplyAsBoolean();
	} catch (final JedisException ex) {
	}
	return status;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#sinter(byte)
     */
    @Override
    public Set<byte[]> sinter(final byte[] key1, final byte[]... keyN) {
	checkNotNull(key1);
	final byte[][] args = new byte[1 + keyN.length][];
	args[0] = key1;
	arraycopy(keyN, 0, args, 1, keyN.length);
	conn.sendCommand(SINTER, args);
	return ImmutableSet.copyOf(conn.multiBulkReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#sinterstore(byte[], byte)
     */
    @Override
    public Long sinterstore(final byte[] dstKey, final byte[] key1,
	    final byte[]... keyN) {
	checkNotNull(dstKey);
	checkNotNull(key1);
	final byte[][] args = new byte[2 + keyN.length][];
	args[0] = dstKey;
	args[1] = key1;
	arraycopy(keyN, 0, args, 2, keyN.length);
	conn.sendCommand(SINTERSTORE, args);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#sismember(byte[], byte[])
     */
    @Override
    public Boolean sismember(final byte[] key, final byte[] value) {
	checkNotNull(key);
	checkNotNull(value);
	conn.sendCommand(SISMEMBER, key, value);
	return conn.integerReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#slaveof(java.lang.String, int)
     */
    @Override
    public Boolean slaveof(final byte[] host, final int port) {
	checkNotNull(host);
	conn.sendCommand(SLAVEOF, host, asByte(port));
	return conn.statusCodeReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#slaveofNoOne()
     */
    @Override
    public Boolean slaveofNoOne() {
	conn.sendCommand(SLAVEOF, asByte("NO ONE"));
	return conn.statusCodeReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#smembers(byte[])
     */
    @Override
    public Set<byte[]> smembers(final byte[] key) {
	checkNotNull(key);
	conn.sendCommand(SMEMBERS, key);
	return ImmutableSet.copyOf(conn.multiBulkReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#smove(byte[], byte[], byte[])
     */
    @Override
    public Boolean smove(final byte[] srckey, final byte[] dstkey,
	    final byte[] member) {
	checkNotNull(srckey);
	checkNotNull(dstkey);
	checkNotNull(member);
	conn.sendCommand(SMOVE, srckey, dstkey, member);
	return conn.integerReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#sort(byte[])
     */
    @Override
    public List<byte[]> sort(final byte[] key) {
	checkNotNull(key);
	conn.sendCommand(SORT, key);
	return conn.multiBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#sort(byte[], byte[])
     */
    @Override
    public Long sort(final byte[] key, final byte[] dstkey) {
	checkNotNull(key);
	checkNotNull(dstkey);
	conn.sendCommand(SORT, key, asByte("STORE"), dstkey);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#sort(byte[],
     * com.googlecode.jedis.SortParams)
     */
    @Override
    public List<byte[]> sort(final byte[] key,
	    final SortParams sortingParameters) {
	checkNotNull(key);
	final int paramsLenght = sortingParameters.getParams().size();
	final byte[][] args = new byte[1 + paramsLenght][];
	args[0] = key;
	arraycopy(sortingParameters.getParams().toArray(new byte[0][]), 0,
		args, 1, paramsLenght);

	conn.sendCommand(SORT, args);
	return conn.multiBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#sort(byte[],
     * com.googlecode.jedis.SortParams, byte[])
     */
    @Override
    public Long sort(final byte[] key, final SortParams sortingParameters,
	    final byte[] dstkey) {
	checkNotNull(key);
	checkNotNull(dstkey);
	final int paramsLenght = sortingParameters.getParams().size();
	final byte[][] args = new byte[3 + paramsLenght][];
	args[0] = key;
	arraycopy(sortingParameters.getParams().toArray(new byte[0][]), 0,
		args, 1, paramsLenght);
	args[1 + paramsLenght] = asByte("STORE");
	args[2 + paramsLenght] = dstkey;

	conn.sendCommand(SORT, args);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#spop(byte[])
     */
    @Override
    public byte[] spop(final byte[] key) {
	checkNotNull(key);
	conn.sendCommand(SPOP, key);
	return conn.bulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#srandmember(byte[])
     */
    @Override
    public byte[] srandmember(final byte[] key) {
	checkNotNull(key);
	conn.sendCommand(SRANDMEMBER, key);
	return conn.bulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#srem(byte[], byte[])
     */
    @Override
    public Boolean srem(final byte[] key, final byte[] member) {
	checkNotNull(key);
	checkNotNull(member);
	conn.sendCommand(SREM, key, member);
	return conn.integerReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#strlen(byte[])
     */
    @Override
    public Long strlen(final byte[] key) {
	checkNotNull(key);
	conn.sendCommand(STRLEN, key);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#substr(byte[], int, int)
     */
    @Override
    public byte[] substr(final byte[] key, final int start, final int end) {
	checkNotNull(key);
	conn.sendCommand(SUBSTR, key, asByte(start), asByte(end));
	return conn.bulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#sunion(byte[], byte[][])
     */
    @Override
    public Set<byte[]> sunion(final byte[] key1, final byte[]... keyN) {
	checkNotNull(key1);
	final byte[][] args = new byte[1 + keyN.length][];
	args[0] = key1;
	arraycopy(keyN, 0, args, 1, keyN.length);
	conn.sendCommand(SUNION, args);
	return ImmutableSet.copyOf(conn.multiBulkReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#sunionstore(byte[], byte[], byte[][])
     */
    @Override
    public Long sunionstore(final byte[] dstKey, final byte[] key1,
	    final byte[]... keyN) {
	checkNotNull(key1);
	checkNotNull(dstKey);

	final byte[][] args = new byte[keyN.length + 2][];
	args[0] = dstKey;
	args[1] = key1;
	arraycopy(keyN, 0, args, 2, keyN.length);

	conn.sendCommand(SUNIONSTORE, args);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#sync()
     */
    @Override
    public void sync() {
	conn.sendCommand(SYNC);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#ttl(byte[])
     */
    @Override
    public Long ttl(final byte[] key) {
	checkNotNull(key);
	conn.sendCommand(TTL, key);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#type(byte[])
     */
    @Override
    public RedisType type(final byte[] key) {
	checkNotNull(key);
	conn.sendCommand(TYPE, key);
	return RedisType.get(conn.statusCodeReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#unwatch()
     */
    @Override
    public Boolean unwatch() {
	conn.sendCommand(UNWATCH);
	return conn.statusCodeReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#watch(byte[])
     */
    @Override
    public Boolean watch(final byte[] key) {
	conn.sendCommand(WATCH, key);
	return conn.statusCodeReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zadd(byte[], double, byte[])
     */
    @Override
    public Boolean zadd(final byte[] key, final double score, final byte[] value) {
	checkNotNull(key);
	checkNotNull(value);
	conn.sendCommand(ZADD, key, asByte(score), value);
	return conn.integerReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zadd(java.lang.String,
     * com.googlecode.jedis.Pair)
     */
    @Override
    public Boolean zadd(final byte[] key, final Pair<byte[], Double> value) {
	checkNotNull(key);
	checkNotNull(value);
	checkNotNull(value.getFirst());
	conn.sendCommand(ZADD, key, asByte(value.getSecond()), value.getFirst());
	return conn.integerReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zcard(byte[])
     */
    @Override
    public Long zcard(final byte[] key) {
	checkNotNull(key);
	conn.sendCommand(ZCARD, key);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zcount(byte[], byte[], byte[])
     */
    @Override
    public Long zcount(final byte[] key, final byte[] min, final byte[] max) {
	checkNotNull(key);
	checkNotNull(min);
	checkNotNull(max);
	conn.sendCommand(ZCOUNT, key, min, max);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zincrby(byte[], byte[], double)
     */
    @Override
    public Double zincrby(final byte[] key, final byte[] member,
	    final double value) {
	checkNotNull(key);
	checkNotNull(member);
	conn.sendCommand(ZINCRBY, key, asByte(value), member);
	return new Double(new String(conn.bulkReply(), DEFAULT_CHARSET));
    }

    private void zinterstoreHelper(final String mode, final byte[] dstKey,
	    final Pair<byte[], Double> ssetAndWeight1,
	    final Pair<byte[], Double>... ssetAndWeightN) {
	checkNotNull(dstKey);
	checkNotNull(ssetAndWeight1);
	checkNotNull(ssetAndWeight1.getFirst());

	// ZUNIONSTORE destination numkeys key1 key2 ... keyN [WEIGHTS
	// weight1 weight2 ... weightN] [AGGREGATE SUM|MIN|MAX]

	final List<byte[]> params = Lists.newArrayList();
	params.add(dstKey);
	params.add(asByte(String.valueOf(1 + ssetAndWeightN.length)));
	params.add(ssetAndWeight1.getFirst());
	for (final Pair<byte[], Double> it : ssetAndWeightN) {
	    params.add(it.getFirst());
	}
	params.add(asByte("WEIGHTS"));
	params.add(asByte(String.valueOf(ssetAndWeight1.getSecond())));
	for (final Pair<byte[], Double> it : ssetAndWeightN) {
	    params.add(asByte(String.valueOf(it.getSecond())));
	}
	params.add(asByte("AGGREGATE"));
	params.add(asByte(mode));

	conn.sendCommand(ZINTERSTORE, params.toArray(new byte[params.size()][]));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zinterstoreMax(byte[],
     * com.googlecode.jedis.Pair,
     * com.googlecode.jedis.Pair<byte[],java.lang.Double>[])
     */
    @Override
    public Long zinterstoreMax(final byte[] dstKey,
	    final Pair<byte[], Double> ssetAndWeight1,
	    final Pair<byte[], Double>... ssetAndWeightN) {
	zinterstoreHelper("MAX", dstKey, ssetAndWeight1, ssetAndWeightN);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zunionstoreMin(byte[],
     * com.googlecode.jedis.Pair,
     * com.googlecode.jedis.Pair<byte[],java.lang.Double>[])
     */
    @Override
    public Long zinterstoreMin(final byte[] dstKey,
	    final Pair<byte[], Double> ssetAndWeight1,
	    final Pair<byte[], Double>... ssetAndWeightN) {
	zinterstoreHelper("MIN", dstKey, ssetAndWeight1, ssetAndWeightN);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zunionstoreSum(byte[],
     * com.googlecode.jedis.Pair,
     * com.googlecode.jedis.Pair<byte[],java.lang.Double>[])
     */
    @Override
    public Long zinterstoreSum(final byte[] dstKey,
	    final Pair<byte[], Double> ssetAndWeight1,
	    final Pair<byte[], Double>... ssetAndWeightN) {
	zinterstoreHelper("SUM", dstKey, ssetAndWeight1, ssetAndWeightN);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zrange(byte[], long, long)
     */
    @Override
    public Set<byte[]> zrange(final byte[] key, final long start, final long end) {
	checkNotNull(key);
	conn.sendCommand(ZRANGE, key, asByte(start), asByte(end));
	return ImmutableSet.copyOf(conn.multiBulkReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zrangeByScore(byte[], byte[], byte[])
     */
    @Override
    public Set<byte[]> zrangeByScore(final byte[] key, final byte[] min,
	    final byte[] max) {
	checkNotNull(key);
	checkNotNull(min);
	checkNotNull(max);
	conn.sendCommand(ZRANGEBYSCORE, key, min, max);
	return ImmutableSet.copyOf(conn.multiBulkReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zrangeByScore(byte[], byte[], byte[],
     * long, long)
     */
    @Override
    public Set<byte[]> zrangeByScore(final byte[] key, final byte[] min,
	    final byte[] max, final long offset, final long count) {
	checkNotNull(key);
	checkNotNull(min);
	checkNotNull(max);
	conn.sendCommand(ZRANGEBYSCORE, key, min, max,
		Protocol.Keyword.LIMIT.raw, asByte(offset), asByte(count));
	return ImmutableSet.copyOf(conn.multiBulkReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zrangeByScoreWithScores(byte[],
     * byte[], byte[])
     */
    @Override
    public Set<Pair<byte[], Double>> zrangeByScoreWithScores(final byte[] key,
	    final byte[] min, final byte[] max) {
	checkNotNull(key);
	checkNotNull(min);
	checkNotNull(max);
	conn.sendCommand(ZRANGEBYSCORE, key, min, max,
		Protocol.Keyword.WITHSCORES.raw);
	return getBinaryPairSet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zrangeByScoreWithScores(byte[],
     * byte[], byte[], long, long)
     */
    @Override
    public Set<Pair<byte[], Double>> zrangeByScoreWithScores(final byte[] key,
	    final byte[] min, final byte[] max, final long offset,
	    final long count) {
	checkNotNull(key);
	checkNotNull(min);
	checkNotNull(max);
	conn.sendCommand(ZRANGEBYSCORE, key, min, max,
		Protocol.Keyword.WITHSCORES.raw, Protocol.Keyword.LIMIT.raw,
		asByte(offset), asByte(count));
	return getBinaryPairSet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zrangeWithScores(byte[], long, long)
     */
    @Override
    public Set<Pair<byte[], Double>> zrangeWithScores(final byte[] key,
	    final long start, final long end) {
	checkNotNull(key);
	conn.sendCommand(ZRANGE, key, asByte(start), asByte(end),
		asByte("WITHSCORES"));
	return getBinaryPairSet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zrank(byte[], byte[])
     */
    @Override
    public Long zrank(final byte[] key, final byte[] member) {
	checkNotNull(key);
	checkNotNull(member);
	conn.sendCommand(ZRANK, key, member);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zrem(byte[], byte[])
     */
    @Override
    public Boolean zrem(final byte[] key, final byte[] member) {
	checkNotNull(key);
	checkNotNull(member);
	conn.sendCommand(ZREM, key, member);
	return conn.integerReplyAsBoolean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zremrangeByRank(byte[], long, long)
     */
    @Override
    public Long zremrangeByRank(final byte[] key, final long start,
	    final long end) {
	checkNotNull(key);
	conn.sendCommand(ZREMRANGEBYRANK, key, asByte(start), asByte(end));
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zremrangeByScore(byte[], byte[],
     * byte[])
     */
    @Override
    public Long zremrangeByScore(final byte[] key, final byte[] min,
	    final byte[] max) {
	checkNotNull(key);
	checkNotNull(min);
	checkNotNull(max);
	conn.sendCommand(ZREMRANGEBYSCORE, key, min, max);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zrevrange(byte[], long, long)
     */
    @Override
    public Set<byte[]> zrevrange(final byte[] key, final long start,
	    final long end) {
	checkNotNull(key);
	conn.sendCommand(ZREVRANGE, key, asByte(start), asByte(end));
	return ImmutableSet.copyOf(conn.multiBulkReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zrevrangeByScore(byte[], byte[],
     * byte[])
     */
    @Override
    public Set<byte[]> zrevrangeByScore(final byte[] key, final byte[] min,
	    final byte[] max) {
	checkNotNull(key);
	checkNotNull(min);
	checkNotNull(max);
	conn.sendCommand(ZREVRANGEBYSCORE, key, min, max);
	return ImmutableSet.copyOf(conn.multiBulkReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zrevrangeByScore(byte[], byte[],
     * byte[], long, long)
     */
    @Override
    public Set<byte[]> zrevrangeByScore(final byte[] key, final byte[] min,
	    final byte[] max, final long offset, final long count) {
	checkNotNull(key);
	checkNotNull(min);
	checkNotNull(max);
	conn.sendCommand(ZREVRANGEBYSCORE, key, min, max,
		Protocol.Keyword.LIMIT.raw, asByte(offset), asByte(count));
	return ImmutableSet.copyOf(conn.multiBulkReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zrevrangeByScoreWithScores(byte[],
     * byte[], byte[])
     */
    @Override
    public Set<Pair<byte[], Double>> zrevrangeByScoreWithScores(
	    final byte[] key, final byte[] min, final byte[] max) {
	checkNotNull(key);
	checkNotNull(min);
	checkNotNull(max);
	conn.sendCommand(ZREVRANGEBYSCORE, key, min, max,
		Protocol.Keyword.WITHSCORES.raw);
	return getBinaryPairSet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zrangeByScoreWithScores(byte[],
     * byte[], byte[], long, long)
     */
    @Override
    public Set<Pair<byte[], Double>> zrevrangeByScoreWithScores(
	    final byte[] key, final byte[] min, final byte[] max,
	    final long offset, final long count) {
	checkNotNull(key);
	checkNotNull(min);
	checkNotNull(max);
	conn.sendCommand(ZREVRANGEBYSCORE, key, min, max,
		Protocol.Keyword.WITHSCORES.raw, Protocol.Keyword.LIMIT.raw,
		asByte(offset), asByte(count));
	return getBinaryPairSet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zrevrangeWithScores(byte[], long,
     * long)
     */
    @Override
    public Set<Pair<byte[], Double>> zrevrangeWithScores(final byte[] key,
	    final long start, final long end) {
	checkNotNull(key);
	conn.sendCommand(ZREVRANGE, key, asByte(start), asByte(end),
		asByte("WITHSCORES"));
	return getBinaryPairSet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zrevrank(byte[], byte[])
     */
    @Override
    public Long zrevrank(final byte[] key, final byte[] member) {
	checkNotNull(key);
	checkNotNull(member);
	conn.sendCommand(ZREVRANK, key, member);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zscore(byte[], byte[])
     */
    @Override
    public Double zscore(final byte[] key, final byte[] member) {
	checkNotNull(key);
	checkNotNull(member);
	conn.sendCommand(ZSCORE, key, member);
	final byte[] score = conn.bulkReply();
	return (score != null ? new Double(new String(score, DEFAULT_CHARSET))
		: null);
    }

    private void zunionstoreHelper(final String mode, final byte[] dstKey,
	    final Pair<byte[], Double> ssetAndWeight1,
	    final Pair<byte[], Double>... ssetAndWeightN) {
	checkNotNull(dstKey);
	checkNotNull(ssetAndWeight1);

	// ZUNIONSTORE destination numkeys key1 key2 ... keyN [WEIGHTS
	// weight1 weight2 ... weightN] [AGGREGATE SUM|MIN|MAX]

	final List<byte[]> params = Lists.newArrayList();
	params.add(dstKey);
	params.add(asByte(String.valueOf(1 + ssetAndWeightN.length)));
	params.add(ssetAndWeight1.getFirst());
	for (final Pair<byte[], Double> it : ssetAndWeightN) {
	    params.add(it.getFirst());
	}
	params.add(asByte("WEIGHTS"));
	params.add(asByte(String.valueOf(ssetAndWeight1.getSecond())));
	for (final Pair<byte[], Double> it : ssetAndWeightN) {
	    params.add(asByte(String.valueOf(it.getSecond())));
	}
	params.add(asByte("AGGREGATE"));
	params.add(asByte(mode));

	conn.sendCommand(ZUNIONSTORE, params.toArray(new byte[params.size()][]));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zunionstoreMax(byte[],
     * com.googlecode.jedis.Pair,
     * com.googlecode.jedis.Pair<byte[],java.lang.Double>[])
     */
    @Override
    public Long zunionstoreMax(final byte[] dstKey,
	    final Pair<byte[], Double> ssetAndWeight1,
	    final Pair<byte[], Double>... ssetAndWeightN) {
	zunionstoreHelper("MAX", dstKey, ssetAndWeight1, ssetAndWeightN);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zunionstoreMin(byte[],
     * com.googlecode.jedis.Pair,
     * com.googlecode.jedis.Pair<byte[],java.lang.Double>[])
     */
    @Override
    public Long zunionstoreMin(final byte[] dstKey,
	    final Pair<byte[], Double> ssetAndWeight1,
	    final Pair<byte[], Double>... ssetAndWeightN) {
	zunionstoreHelper("MIN", dstKey, ssetAndWeight1, ssetAndWeightN);
	return conn.integerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.googlecode.jedis.RawJedis#zunionstoreSum(byte[],
     * com.googlecode.jedis.Pair,
     * com.googlecode.jedis.Pair<byte[],java.lang.Double>[])
     */
    @Override
    public Long zunionstoreSum(final byte[] dstKey,
	    final Pair<byte[], Double> ssetAndWeight1,
	    final Pair<byte[], Double>... ssetAndWeightN) {
	zunionstoreHelper("SUM", dstKey, ssetAndWeight1, ssetAndWeightN);
	return conn.integerReply();
    }

}