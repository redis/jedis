package redis.clients.jedis;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.System.arraycopy;
import static redis.clients.jedis.PairImpl.newPair;
import static redis.clients.jedis.Protocol.DEFAULT_CHARSET;
import static redis.clients.jedis.Protocol.Command.APPEND;
import static redis.clients.jedis.Protocol.Command.AUTH;
import static redis.clients.jedis.Protocol.Command.BGREWRITEAOF;
import static redis.clients.jedis.Protocol.Command.BGSAVE;
import static redis.clients.jedis.Protocol.Command.GETSET;
import static redis.clients.jedis.Protocol.Command.SDIFFSTORE;
import static redis.clients.jedis.Protocol.Command.SRANDMEMBER;
import static redis.clients.jedis.Protocol.Command.SUNION;
import static redis.clients.jedis.Protocol.Command.SUNIONSTORE;
import static redis.clients.jedis.Protocol.Command.ZADD;
import static redis.clients.jedis.Protocol.Command.ZCARD;
import static redis.clients.jedis.Protocol.Command.ZCOUNT;
import static redis.clients.jedis.Protocol.Command.ZINCRBY;
import static redis.clients.jedis.Protocol.Command.ZINTERSTORE;
import static redis.clients.jedis.Protocol.Command.ZRANGE;
import static redis.clients.jedis.Protocol.Command.ZRANGEBYSCORE;
import static redis.clients.jedis.Protocol.Command.ZREMRANGEBYRANK;
import static redis.clients.jedis.Protocol.Command.ZREMRANGEBYSCORE;
import static redis.clients.jedis.Protocol.Command.ZREVRANGE;
import static redis.clients.jedis.Protocol.Command.ZREVRANGEBYSCORE;
import static redis.clients.jedis.Protocol.Command.ZREVRANK;
import static redis.clients.jedis.Protocol.Command.ZSCORE;
import static redis.clients.jedis.Protocol.Command.ZUNIONSTORE;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Protocol.Command;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class RawJedisImpl implements RawJedis {

    static protected byte[] asByte(final double value) {
	return asByte(String.valueOf(value));
    }

    static protected byte[] asByte(final long value) {
	return asByte(String.valueOf(value));
    }

    static protected byte[] asByte(String value) {
	return Strings.nullToEmpty(value).getBytes(Protocol.DEFAULT_CHARSET);
    }

    static protected byte[][] asByte(String[] value) {
	byte[][] ret = new byte[value.length][];
	for (int i = 0; i < value.length; i++) {
	    ret[i] = asByte(value[i]);
	}
	return ret;
    }

    static protected String asString(byte[] value) {
	return (value != null) ? new String(value, Protocol.DEFAULT_CHARSET)
		: null;
    }

    protected Client client;

    protected RawJedisImpl() {
	client = new Client();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#append(byte[], byte[])
     */
    @Override
    public Long append(final byte[] key, final byte[] value) {
	runChecks();
	client.sendCommand(APPEND, value);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#auth(byte[])
     */
    @Override
    public String auth(final byte[] password) {
	runChecks();
	client.sendCommand(AUTH, password);
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#bgrewriteaof()
     */
    @Override
    public String bgrewriteaof() {
	client.bgrewriteaof();
	client.sendCommand(BGREWRITEAOF);
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#bgsave()
     */
    @Override
    public String bgsave() {
	client.sendCommand(BGSAVE);
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#blpop(long, byte[], byte[][])
     */
    @Override
    public List<byte[]> blpop(final long timeout, final byte[] key1,
	    byte[]... keyN) {
	runChecks();
	if (key1 == null) {
	    throw new NullPointerException();
	}

	final List<byte[]> args = Lists.newArrayList();
	args.add(key1);
	for (final byte[] key : keyN) {
	    args.add(key);
	}
	args.add(Protocol.toByteArray(timeout));

	client.blpop(args.toArray(new byte[args.size()][]));

	client.setTimeoutInfinite();
	final List<byte[]> multiBulkReply = client.getBinaryMultiBulkReply();
	client.rollbackTimeout();
	return multiBulkReply;
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#brpop(long, byte[], byte[][])
     */
    @Override
    public List<byte[]> brpop(long timeout, byte[] key1, byte[]... keyN) {
	runChecks();
	final List<byte[]> args = Lists.newArrayList();
	args.add(key1);
	for (final byte[] key : keyN) {
	    args.add(key);
	}
	args.add(Protocol.toByteArray(timeout));

	client.brpop(args.toArray(new byte[args.size()][]));
	client.setTimeoutInfinite();
	final List<byte[]> multiBulkReply = client.getBinaryMultiBulkReply();
	client.rollbackTimeout();

	return multiBulkReply;
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#configGet(java.lang.String)
     */
    @Override
    public List<String> configGet(final String pattern) {
	client.configGet(pattern);
	return client.getMultiBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#configSet(java.lang.String,
     * java.lang.String)
     */
    @Override
    public String configSet(final String parameter, final String value) {
	client.configSet(parameter, value);
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.Jedis#connect()
     */
    @Override
    public void connect() throws UnknownHostException, IOException {
	if (!client.isConnected()) {
	    client.connect();
	    if (client.getConfig().getPassword() != null) {
		this.auth(client.getConfig().getPassword()
			.getBytes(DEFAULT_CHARSET));
	    }
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#dbSize()
     */
    @Override
    public Long dbSize() {
	runChecks();
	client.dbSize();
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#debug(redis.clients.jedis.DebugParams)
     */
    @Override
    public String debug(final DebugParams params) {
	client.debug(params);
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#decr(byte[])
     */
    @Override
    public Long decr(final byte[] key) {
	runChecks();
	client.decr(key);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#decrBy(byte[], long)
     */
    @Override
    public Long decrBy(final byte[] key, final long integer) {
	runChecks();
	client.decrBy(key, integer);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#del(byte)
     */
    @Override
    public Long del(final byte[] key1, final byte[]... keyN) {
	runChecks();
	client.del(key1, keyN);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#disconnect()
     */
    @Override
    public void disconnect() throws IOException {
	client.disconnect();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#echo(byte[])
     */
    @Override
    public byte[] echo(final byte[] string) {
	client.echo(string);
	return client.getBinaryBulkReply();
    }

    @Override
    public List<byte[]> executeRaw() {
	Preconditions.checkState(client.isPipelineMode(),
		"Not in pipelined mode!");
	client.setPipelineMode(false);
	return client.getAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#exists(byte[])
     */
    @Override
    public Boolean exists(final byte[] key) {
	runChecks();
	client.exists(key);
	return client.getIntegerReply() == 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#expire(byte[], int)
     */
    @Override
    public Long expire(final byte[] key, final int seconds) {
	runChecks();
	client.expire(key, seconds);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#expireAt(byte[], long)
     */
    @Override
    public Long expireAt(final byte[] key, final long unixTime) {
	runChecks();
	client.expireAt(key, unixTime);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#flushAll()
     */
    @Override
    public Boolean flushAll() {
	runChecks();
	client.flushAll();
	return client.getStatusCodeReply().equals("OK");
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#flushDB()
     */
    @Override
    public Boolean flushDB() {
	runChecks();
	client.flushDB();
	return client.getStatusCodeReply().equals("OK");
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#get(byte[])
     */
    @Override
    public byte[] get(final byte[] key) {
	runChecks();
	client.get(key);
	return client.getBinaryBulkReply();
    }

    private Set<Pair<byte[], Double>> getBinaryPairSet() {
	List<byte[]> membersWithScores = client.getBinaryMultiBulkReply();
	Iterator<byte[]> iterator = membersWithScores.iterator();
	Set<Pair<byte[], Double>> set = Sets.newLinkedHashSet();

	while (iterator.hasNext()) {
	    set.add(newPair(iterator.next(),
		    Double.valueOf(SafeEncoder.encode(iterator.next()))));
	}
	return set;
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#getClient()
     */
    public Client getClient() {
	return client;
    }

    @Override
    public JedisConfig getJedisConfig() {
	return client.getConfig();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#getSet(byte[], byte[])
     */
    @Override
    public byte[] getSet(final byte[] key, final byte[] value) {
	if (key == null || value == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(GETSET, key, value);
	return client.getBinaryBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#hdel(byte[], byte[])
     */
    @Override
    public Long hdel(final byte[] key, final byte[] field) {
	runChecks();
	client.hdel(key, field);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#hexists(byte[], byte[])
     */
    @Override
    public Boolean hexists(final byte[] key, final byte[] field) {
	runChecks();
	client.hexists(key, field);
	return client.getIntegerReply() == 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#hget(byte[], byte[])
     */
    @Override
    public byte[] hget(final byte[] key, final byte[] field) {
	runChecks();
	client.hget(key, field);
	return client.getBinaryBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#hgetAll(byte[])
     */
    @Override
    public Map<byte[], byte[]> hgetAll(final byte[] key) {
	runChecks();
	client.hgetAll(key);
	final List<byte[]> flatHash = client.getBinaryMultiBulkReply();
	final Map<byte[], byte[]> hash = new JedisByteHashMap();
	final Iterator<byte[]> iterator = flatHash.iterator();
	while (iterator.hasNext()) {
	    hash.put(iterator.next(), iterator.next());
	}

	return hash;
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#hincrBy(byte[], byte[], long)
     */
    @Override
    public Long hincrBy(final byte[] key, final byte[] field, final long value) {
	runChecks();
	client.hincrBy(key, field, value);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#hkeys(byte[])
     */
    @Override
    public Set<byte[]> hkeys(final byte[] key) {
	runChecks();
	client.hkeys(key);
	final List<byte[]> lresult = client.getBinaryMultiBulkReply();
	return new LinkedHashSet<byte[]>(lresult);
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#hlen(byte[])
     */
    @Override
    public Long hlen(final byte[] key) {
	runChecks();
	client.hlen(key);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#hmget(byte[], byte)
     */
    @Override
    public List<byte[]> hmget(final byte[] key, final byte[]... fields) {
	runChecks();
	client.hmget(key, fields);
	return client.getBinaryMultiBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#hmset(byte[], java.util.Map)
     */
    @Override
    public String hmset(final byte[] key, final Map<byte[], byte[]> hash) {
	runChecks();
	client.hmset(key, hash);
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#hset(byte[], byte[], byte[])
     */
    @Override
    public Long hset(final byte[] key, final byte[] field, final byte[] value) {
	runChecks();
	client.hset(key, field, value);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#hsetnx(byte[], byte[], byte[])
     */
    @Override
    public Long hsetnx(final byte[] key, final byte[] field, final byte[] value) {
	runChecks();
	client.hsetnx(key, field, value);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#hvals(byte[])
     */
    @Override
    public List<byte[]> hvals(final byte[] key) {
	runChecks();
	client.hvals(key);
	final List<byte[]> lresult = client.getBinaryMultiBulkReply();
	return lresult;
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#incr(byte[])
     */
    @Override
    public Long incr(final byte[] key) {
	runChecks();
	client.incr(key);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#incrBy(byte[], long)
     */
    @Override
    public Long incrBy(final byte[] key, final long integer) {
	runChecks();
	client.incrBy(key, integer);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#info()
     */
    @Override
    public String info() {
	client.info();
	return client.getBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#isConnected()
     */
    @Override
    public boolean isConnected() {
	return client.isConnected();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#keys(byte[])
     */
    @Override
    public Set<byte[]> keys(final byte[] pattern) {
	runChecks();
	client.keys(pattern);
	final HashSet<byte[]> keySet = new LinkedHashSet<byte[]>(
		client.getBinaryMultiBulkReply());
	return keySet;
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#lastsave()
     */
    @Override
    public Long lastsave() {
	client.lastsave();
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#lindex(byte[], int)
     */
    @Override
    public byte[] lindex(final byte[] key, final int index) {
	runChecks();
	client.lindex(key, index);
	return client.getBinaryBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#linsertAfter(byte[], byte[], byte[])
     */
    @Override
    public Long linsertAfter(byte[] key, byte[] element, byte[] value) {
	client.linsertAfter(key, element, value);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#linsertBefore(byte[], byte[], byte[])
     */
    @Override
    public Long linsertBefore(byte[] key, byte[] element, byte[] value) {
	client.linsertBefore(key, element, value);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#llen(byte[])
     */
    @Override
    public Long llen(final byte[] key) {
	runChecks();
	client.llen(key);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#lpop(byte[])
     */
    @Override
    public byte[] lpop(final byte[] key) {
	runChecks();
	client.lpop(key);
	return client.getBinaryBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#lpush(byte[], byte[])
     */
    @Override
    public Long lpush(final byte[] key, final byte[] string) {
	runChecks();
	client.lpush(key, string);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#lpushx(byte[], byte[])
     */
    @Override
    public Long lpushx(final byte[] key, final byte[] string) {
	client.lpushx(key, string);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#lrange(byte[], int, int)
     */
    @Override
    public List<byte[]> lrange(final byte[] key, final long start,
	    final long end) {
	runChecks();
	client.lrange(key, start, end);
	return client.getBinaryMultiBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#lrem(byte[], int, byte[])
     */
    @Override
    public Long lrem(final byte[] key, final int count, final byte[] value) {
	runChecks();
	client.lrem(key, count, value);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#lset(byte[], int, byte[])
     */
    @Override
    public String lset(final byte[] key, final int index, final byte[] value) {
	runChecks();
	client.lset(key, index, value);
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#ltrim(byte[], int, int)
     */
    @Override
    public String ltrim(final byte[] key, final int start, final int end) {
	runChecks();
	client.ltrim(key, start, end);
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#mget(byte)
     */
    @Override
    public List<byte[]> mget(final byte[]... keys) {
	runChecks();
	client.mget(keys);
	return client.getBinaryMultiBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * redis.clients.jedis.RawJedis#monitor(redis.clients.jedis.JedisMonitor)
     */
    @Override
    public void monitor(final JedisMonitor jedisMonitor) {
	client.monitor();
	jedisMonitor.proceed(client);
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#move(byte[], int)
     */
    @Override
    public Long move(final byte[] key, final int dbIndex) {
	runChecks();
	client.move(key, dbIndex);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#mset(byte)
     */
    @Override
    public String mset(final byte[]... keysvalues) {
	runChecks();
	client.mset(keysvalues);
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#msetnx(byte)
     */
    @Override
    public Long msetnx(final byte[]... keysvalues) {
	runChecks();
	client.msetnx(keysvalues);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#persist(byte[])
     */
    @Override
    public Long persist(final byte[] key) {
	client.persist(key);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#ping()
     */
    @Override
    public Boolean ping() {
	runChecks();
	client.sendCommand(Command.PING);
	return client.getStatusCodeReply().equals("PONG");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * redis.clients.jedis.RawJedis#psubscribe(redis.clients.jedis.JedisPubSub,
     * java.lang.String)
     */
    @Override
    public void psubscribe(final JedisPubSub jedisPubSub,
	    final String pattern1, final String... patternN) {
	client.setTimeoutInfinite();
	jedisPubSub.proceedWithPatterns(client, pattern1, patternN);
	client.rollbackTimeout();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#publish(java.lang.String,
     * java.lang.String)
     */
    @Override
    public Long publish(final String channel, final String message) {
	client.publish(channel, message);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#quit()
     */
    @Override
    public void quit() {
	runChecks();
	client.quit();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#randomBinaryKey()
     */
    @Override
    public byte[] randomBinaryKey() {
	runChecks();
	client.randomKey();
	return client.getBinaryBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#rename(byte[], byte[])
     */
    @Override
    public String rename(final byte[] oldkey, final byte[] newkey) {
	runChecks();
	client.rename(oldkey, newkey);
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#renamenx(byte[], byte[])
     */
    @Override
    public Long renamenx(final byte[] oldkey, final byte[] newkey) {
	runChecks();
	client.renamenx(oldkey, newkey);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#rpop(byte[])
     */
    @Override
    public byte[] rpop(final byte[] key) {
	runChecks();
	client.rpop(key);
	return client.getBinaryBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#rpoplpush(byte[], byte[])
     */
    @Override
    public byte[] rpoplpush(final byte[] srckey, final byte[] dstkey) {
	runChecks();
	client.rpoplpush(srckey, dstkey);
	return client.getBinaryBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#rpush(byte[], byte[])
     */
    @Override
    public Long rpush(final byte[] key, final byte[] string) {
	runChecks();
	client.rpush(key, string);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#rpushx(byte[], byte[])
     */
    @Override
    public Long rpushx(final byte[] key, final byte[] string) {
	client.rpushx(key, string);
	return client.getIntegerReply();
    }

    protected void runChecks() {
	if (client.isInMulti()) {
	    throw new JedisException(
		    "Cannot use Jedis when in Multi. Please use JedisTransaction instead.");
	}
	try {
	    this.connect();
	} catch (UnknownHostException e) {
	    throw new JedisException(e);
	} catch (IOException e) {
	    throw new JedisException(e);
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#sadd(byte[], byte[])
     */
    @Override
    public Boolean sadd(final byte[] key, final byte[] member) {
	runChecks();
	client.sadd(key, member);
	return client.getIntegerReply().equals(1L);
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#save()
     */
    @Override
    public String save() {
	client.save();
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#scard(byte[])
     */
    @Override
    public Long scard(final byte[] key) {
	runChecks();
	client.scard(key);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#sdiff(byte[], byte[][])
     */
    @Override
    public Set<byte[]> sdiff(byte[] key1, final byte[]... keyN) {
	if (key1 == null) {
	    throw new NullPointerException();
	}
	runChecks();
	byte[][] args = new byte[keyN.length + 1][];
	args[0] = key1;
	arraycopy(keyN, 0, args, 1, keyN.length);
	client.sendCommand(Command.SDIFF, args);
	return Sets.newLinkedHashSet(client.getBinaryMultiBulkReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#sdiffstore(byte[], byte[], byte[][])
     */
    @Override
    public Long sdiffstore(final byte[] dstKey, byte[] key1,
	    final byte[]... keyN) {
	if (dstKey == null || key1 == null) {
	    throw new NullPointerException();
	}
	runChecks();
	byte[][] args = new byte[keyN.length + 2][];
	args[0] = dstKey;
	args[1] = key1;
	arraycopy(keyN, 0, args, 2, keyN.length);
	client.sendCommand(SDIFFSTORE, args);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#select(int)
     */
    @Override
    public String select(final int index) {
	runChecks();
	client.select(index);
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#set(byte[], byte[])
     */
    @Override
    public Boolean set(final byte[] key, final byte[] value) {
	checkNotNull(key);
	checkNotNull(value);

	runChecks();
	client.sendCommand(Command.SET, key, value);

	String reply = client.getStatusCodeReply();
	return (reply != null) ? reply.equals("OK") : false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#setClient(redis.clients.jedis.Client)
     */
    public void setClient(Client client) {
	this.client = client;
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#setex(byte[], int, byte[])
     */
    @Override
    public String setex(final byte[] key, final int seconds, final byte[] value) {
	runChecks();
	client.setex(key, seconds, value);
	return client.getStatusCodeReply();
    }

    @Override
    public void setJedisConfig(JedisConfig config) {
	client.setConfig(config);
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#setnx(byte[], byte[])
     */
    @Override
    public Long setnx(final byte[] key, final byte[] value) {
	runChecks();
	client.setnx(key, value);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#shutdown()
     */
    @Override
    public String shutdown() {
	client.shutdown();
	String status = null;
	try {
	    status = client.getStatusCodeReply();
	} catch (JedisException ex) {
	    status = null;
	}
	return status;
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#sinter(byte)
     */
    @Override
    public Set<byte[]> sinter(byte[] key1, final byte[]... keyN) {
	runChecks();
	client.sinter(key1, keyN);
	final List<byte[]> members = client.getBinaryMultiBulkReply();
	return new LinkedHashSet<byte[]>(members);
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#sinterstore(byte[], byte)
     */
    @Override
    public Long sinterstore(final byte[] dstKey, byte[] key1,
	    final byte[]... keyN) {
	runChecks();
	client.sinterstore(dstKey, key1, keyN);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#sismember(byte[], byte[])
     */
    @Override
    public Boolean sismember(final byte[] key, final byte[] member) {
	runChecks();
	client.sismember(key, member);
	return client.getIntegerReply().equals(1L);
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#slaveof(java.lang.String, int)
     */
    @Override
    public String slaveof(final String host, final int port) {
	client.slaveof(host, port);
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#slaveofNoOne()
     */
    @Override
    public String slaveofNoOne() {
	client.slaveofNoOne();
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#smembers(byte[])
     */
    @Override
    public Set<byte[]> smembers(final byte[] key) {
	runChecks();
	client.smembers(key);
	final List<byte[]> members = client.getBinaryMultiBulkReply();
	return new LinkedHashSet<byte[]>(members);
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#smove(byte[], byte[], byte[])
     */
    @Override
    public Boolean smove(final byte[] srckey, final byte[] dstkey,
	    final byte[] member) {
	runChecks();
	client.smove(srckey, dstkey, member);
	return client.getIntegerReply().equals(1L);
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#sort(byte[])
     */
    @Override
    public List<byte[]> sort(final byte[] key) {
	runChecks();
	client.sort(key);
	return client.getBinaryMultiBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#sort(byte[], byte[])
     */
    @Override
    public Long sort(final byte[] key, final byte[] dstkey) {
	runChecks();
	client.sort(key, dstkey);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#sort(byte[],
     * redis.clients.jedis.SortParams)
     */
    @Override
    public List<byte[]> sort(final byte[] key,
	    final SortParams sortingParameters) {
	runChecks();
	client.sort(key, sortingParameters);
	return client.getBinaryMultiBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#sort(byte[],
     * redis.clients.jedis.SortParams, byte[])
     */
    @Override
    public Long sort(final byte[] key, final SortParams sortingParameters,
	    final byte[] dstkey) {
	runChecks();
	client.sort(key, sortingParameters, dstkey);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#spop(byte[])
     */
    @Override
    public byte[] spop(final byte[] key) {
	runChecks();
	client.spop(key);
	return client.getBinaryBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#srandmember(byte[])
     */
    @Override
    public byte[] srandmember(final byte[] key) {
	if (key == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(SRANDMEMBER, key);
	return client.getBinaryBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#srem(byte[], byte[])
     */
    @Override
    public Boolean srem(final byte[] key, final byte[] member) {
	runChecks();
	client.srem(key, member);
	return client.getIntegerReply().equals(1L);
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#strlen(byte[])
     */
    @Override
    public Long strlen(final byte[] key) {
	client.strlen(key);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * redis.clients.jedis.RawJedis#subscribe(redis.clients.jedis.JedisPubSub,
     * java.lang.String)
     */
    @Override
    public void subscribe(final JedisPubSub jedisPubSub,
	    final String... channels) {
	client.setTimeoutInfinite();
	jedisPubSub.proceed(client, channels);
	client.rollbackTimeout();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#substr(byte[], int, int)
     */
    @Override
    public byte[] substr(final byte[] key, final int start, final int end) {
	runChecks();
	client.substr(key, start, end);
	return client.getBinaryBulkReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#sunion(byte[], byte[][])
     */
    @Override
    public Set<byte[]> sunion(byte[] key1, final byte[]... keyN) {
	if (key1 == null) {
	    throw new NullPointerException();
	}
	runChecks();

	byte[][] args = new byte[1 + keyN.length][];
	args[0] = key1;
	arraycopy(keyN, 0, args, 1, keyN.length);

	client.sendCommand(SUNION, args);
	return Sets.newLinkedHashSet(client.getBinaryMultiBulkReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#sunionstore(byte[], byte[], byte[][])
     */
    @Override
    public Long sunionstore(final byte[] dstKey, byte[] key1,
	    final byte[]... keyN) {
	if (dstKey == null || key1 == null) {
	    throw new NullPointerException();
	}
	runChecks();

	byte[][] args = new byte[keyN.length + 2][];
	args[0] = dstKey;
	args[1] = key1;
	arraycopy(keyN, 0, args, 2, keyN.length);

	client.sendCommand(SUNIONSTORE, args);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#sync()
     */
    @Override
    public void sync() {
	client.sync();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#ttl(byte[])
     */
    @Override
    public Long ttl(final byte[] key) {
	runChecks();
	client.ttl(key);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#type(byte[])
     */
    @Override
    public String type(final byte[] key) {
	runChecks();
	client.type(key);
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#unwatch()
     */
    @Override
    public String unwatch() {
	client.unwatch();
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#watch(byte[])
     */
    @Override
    public String watch(final byte[] key) {
	client.watch(key);
	return client.getStatusCodeReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zadd(byte[], double, byte[])
     */
    @Override
    public Boolean zadd(final byte[] key, final double score, final byte[] value) {
	if (key == null || value == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZADD, key, asByte(score), value);
	return (client.getIntegerReply() == 1L);
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zadd(java.lang.String,
     * redis.clients.jedis.Pair)
     */
    @Override
    public Boolean zadd(final byte[] key, final Pair<byte[], Double> value) {
	if (key == null || value == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZADD, key, asByte(value.getSecond()),
		value.getFirst());

	return (client.getIntegerReply() == 1L);
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zcard(byte[])
     */
    @Override
    public Long zcard(final byte[] key) {
	if (key == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZCARD, key);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zcount(byte[], byte[], byte[])
     */
    @Override
    public Long zcount(byte[] key, byte[] min, byte[] max) {
	if (key == null || min == null || max == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZCOUNT, key, min, max);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zincrby(byte[], byte[], double)
     */
    @Override
    public Double zincrby(byte[] key, byte[] member, double value) {
	if (key == null || member == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZINCRBY, key, asByte(value), member);
	return new Double(client.getBulkReply());
    }

    private void zinterstoreHelper(String mode, byte[] dstKey,
	    Pair<byte[], Double> ssetAndWeight1,
	    Pair<byte[], Double>... ssetAndWeightN) {
	checkNotNull(dstKey);
	checkNotNull(ssetAndWeight1);
	runChecks();

	// ZUNIONSTORE destination numkeys key1 key2 ... keyN [WEIGHTS
	// weight1 weight2 ... weightN] [AGGREGATE SUM|MIN|MAX]

	List<byte[]> params = Lists.newArrayList();
	params.add(dstKey);
	params.add(asByte(String.valueOf(1 + ssetAndWeightN.length)));
	params.add(ssetAndWeight1.getFirst());
	for (Pair<byte[], Double> it : ssetAndWeightN) {
	    params.add(it.getFirst());
	}
	params.add(asByte("WEIGHTS"));
	params.add(asByte(String.valueOf(ssetAndWeight1.getSecond())));
	for (Pair<byte[], Double> it : ssetAndWeightN) {
	    params.add(asByte(String.valueOf(it.getSecond())));
	}
	params.add(asByte("AGGREGATE"));
	params.add(asByte(mode));

	client.sendCommand(ZINTERSTORE,
		params.toArray(new byte[params.size()][]));
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zinterstoreMax(byte[],
     * redis.clients.jedis.Pair,
     * redis.clients.jedis.Pair<byte[],java.lang.Double>[])
     */
    @Override
    public Long zinterstoreMax(byte[] dstKey,
	    Pair<byte[], Double> ssetAndWeight1,
	    Pair<byte[], Double>... ssetAndWeightN) {
	zinterstoreHelper("MAX", dstKey, ssetAndWeight1, ssetAndWeightN);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zunionstoreMin(byte[],
     * redis.clients.jedis.Pair,
     * redis.clients.jedis.Pair<byte[],java.lang.Double>[])
     */
    @Override
    public Long zinterstoreMin(byte[] dstKey,
	    Pair<byte[], Double> ssetAndWeight1,
	    Pair<byte[], Double>... ssetAndWeightN) {
	zinterstoreHelper("MIN", dstKey, ssetAndWeight1, ssetAndWeightN);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zunionstoreSum(byte[],
     * redis.clients.jedis.Pair,
     * redis.clients.jedis.Pair<byte[],java.lang.Double>[])
     */
    @Override
    public Long zinterstoreSum(byte[] dstKey,
	    Pair<byte[], Double> ssetAndWeight1,
	    Pair<byte[], Double>... ssetAndWeightN) {
	zinterstoreHelper("SUM", dstKey, ssetAndWeight1, ssetAndWeightN);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zrange(byte[], long, long)
     */
    @Override
    public Set<byte[]> zrange(final byte[] key, final long start, final long end) {
	if (key == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZRANGE, key, asByte(start), asByte(end));
	return Sets.newLinkedHashSet(client.getBinaryMultiBulkReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zrangeByScore(byte[], byte[], byte[])
     */
    @Override
    public Set<byte[]> zrangeByScore(final byte[] key, final byte[] min,
	    final byte[] max) {
	if (key == null || min == null || max == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZRANGEBYSCORE, key, min, max);
	return Sets.newLinkedHashSet(client.getBinaryMultiBulkReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zrangeByScore(byte[], byte[], byte[],
     * long, long)
     */
    @Override
    public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max,
	    long offset, long count) {
	if (key == null || min == null || max == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZRANGEBYSCORE, key, min, max,
		Protocol.Keyword.LIMIT.raw, asByte(offset), asByte(count));
	return Sets.newLinkedHashSet(client.getBinaryMultiBulkReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zrangeByScoreWithScores(byte[], byte[],
     * byte[])
     */
    @Override
    public Set<Pair<byte[], Double>> zrangeByScoreWithScores(byte[] key,
	    byte[] min, byte[] max) {
	if (key == null || min == null || max == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZRANGEBYSCORE, key, min, max,
		Protocol.Keyword.WITHSCORES.raw);
	return getBinaryPairSet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zrangeByScoreWithScores(byte[], byte[],
     * byte[], long, long)
     */
    @Override
    public Set<Pair<byte[], Double>> zrangeByScoreWithScores(byte[] key,
	    byte[] min, byte[] max, long offset, long count) {
	if (key == null || min == null || max == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZRANGEBYSCORE, key, min, max,
		Protocol.Keyword.WITHSCORES.raw, Protocol.Keyword.LIMIT.raw,
		asByte(offset), asByte(count));
	return getBinaryPairSet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zrangeWithScores(byte[], long, long)
     */
    @Override
    public Set<Pair<byte[], Double>> zrangeWithScores(byte[] key, long start,
	    long end) {
	if (key == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZRANGE, key, asByte(start), asByte(end),
		asByte("WITHSCORES"));
	return getBinaryPairSet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zrank(byte[], byte[])
     */
    @Override
    public Long zrank(final byte[] key, final byte[] member) {
	if (key == null || member == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(Command.ZRANK, key, member);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zrem(byte[], byte[])
     */
    @Override
    public Boolean zrem(final byte[] key, final byte[] member) {
	if (key == null || member == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.zrem(key, member);
	return (client.getIntegerReply() == 1L);
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zremrangeByRank(byte[], long, long)
     */
    @Override
    public Long zremrangeByRank(final byte[] key, final long start,
	    final long end) {
	if (key == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZREMRANGEBYRANK, key, asByte(start), asByte(end));
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zremrangeByScore(byte[], byte[],
     * byte[])
     */
    @Override
    public Long zremrangeByScore(final byte[] key, final byte[] min,
	    final byte[] max) {
	if (key == null || min == null || max == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZREMRANGEBYSCORE, key, min, max);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zrevrange(byte[], long, long)
     */
    @Override
    public Set<byte[]> zrevrange(final byte[] key, final long start,
	    final long end) {
	if (key == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZREVRANGE, key, asByte(start), asByte(end));
	return Sets.newLinkedHashSet(client.getBinaryMultiBulkReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zrevrangeByScore(byte[], byte[],
     * byte[])
     */
    @Override
    public Set<byte[]> zrevrangeByScore(final byte[] key, final byte[] min,
	    final byte[] max) {
	if (key == null || min == null || max == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZREVRANGEBYSCORE, key, min, max);
	return Sets.newLinkedHashSet(client.getBinaryMultiBulkReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zrevrangeByScore(byte[], byte[],
     * byte[], long, long)
     */
    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, byte[] min, byte[] max,
	    long offset, long count) {
	if (key == null || min == null || max == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZREVRANGEBYSCORE, key, min, max,
		Protocol.Keyword.LIMIT.raw, asByte(offset), asByte(count));
	return Sets.newLinkedHashSet(client.getBinaryMultiBulkReply());
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zrevrangeByScoreWithScores(byte[],
     * byte[], byte[])
     */
    @Override
    public Set<Pair<byte[], Double>> zrevrangeByScoreWithScores(byte[] key,
	    byte[] min, byte[] max) {
	if (key == null || min == null || max == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZREVRANGEBYSCORE, key, min, max,
		Protocol.Keyword.WITHSCORES.raw);
	return getBinaryPairSet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zrangeByScoreWithScores(byte[], byte[],
     * byte[], long, long)
     */
    @Override
    public Set<Pair<byte[], Double>> zrevrangeByScoreWithScores(byte[] key,
	    byte[] min, byte[] max, long offset, long count) {
	if (key == null || min == null || max == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZREVRANGEBYSCORE, key, min, max,
		Protocol.Keyword.WITHSCORES.raw, Protocol.Keyword.LIMIT.raw,
		asByte(offset), asByte(count));
	return getBinaryPairSet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zrevrangeWithScores(byte[], long, long)
     */
    @Override
    public Set<Pair<byte[], Double>> zrevrangeWithScores(byte[] key,
	    long start, long end) {
	if (key == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZREVRANGE, key, asByte(start), asByte(end),
		asByte("WITHSCORES"));
	return getBinaryPairSet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zrevrank(byte[], byte[])
     */
    @Override
    public Long zrevrank(final byte[] key, final byte[] member) {
	if (key == null || member == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZREVRANK, key, member);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zscore(byte[], byte[])
     */
    @Override
    public Double zscore(final byte[] key, final byte[] member) {
	if (key == null || member == null) {
	    throw new NullPointerException();
	}
	runChecks();
	client.sendCommand(ZSCORE, key, member);
	final String score = client.getBulkReply();
	return (score != null ? new Double(score) : null);
    }

    private void zunionstoreHelper(String mode, byte[] dstKey,
	    Pair<byte[], Double> ssetAndWeight1,
	    Pair<byte[], Double>... ssetAndWeightN) {
	checkNotNull(dstKey);
	checkNotNull(ssetAndWeight1);
	runChecks();

	// ZUNIONSTORE destination numkeys key1 key2 ... keyN [WEIGHTS
	// weight1 weight2 ... weightN] [AGGREGATE SUM|MIN|MAX]

	List<byte[]> params = Lists.newArrayList();
	params.add(dstKey);
	params.add(asByte(String.valueOf(1 + ssetAndWeightN.length)));
	params.add(ssetAndWeight1.getFirst());
	for (Pair<byte[], Double> it : ssetAndWeightN) {
	    params.add(it.getFirst());
	}
	params.add(asByte("WEIGHTS"));
	params.add(asByte(String.valueOf(ssetAndWeight1.getSecond())));
	for (Pair<byte[], Double> it : ssetAndWeightN) {
	    params.add(asByte(String.valueOf(it.getSecond())));
	}
	params.add(asByte("AGGREGATE"));
	params.add(asByte(mode));

	client.sendCommand(ZUNIONSTORE,
		params.toArray(new byte[params.size()][]));
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zunionstoreMax(byte[],
     * redis.clients.jedis.Pair,
     * redis.clients.jedis.Pair<byte[],java.lang.Double>[])
     */
    @Override
    public Long zunionstoreMax(byte[] dstKey,
	    Pair<byte[], Double> ssetAndWeight1,
	    Pair<byte[], Double>... ssetAndWeightN) {
	zunionstoreHelper("MAX", dstKey, ssetAndWeight1, ssetAndWeightN);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zunionstoreMin(byte[],
     * redis.clients.jedis.Pair,
     * redis.clients.jedis.Pair<byte[],java.lang.Double>[])
     */
    @Override
    public Long zunionstoreMin(byte[] dstKey,
	    Pair<byte[], Double> ssetAndWeight1,
	    Pair<byte[], Double>... ssetAndWeightN) {
	zunionstoreHelper("MIN", dstKey, ssetAndWeight1, ssetAndWeightN);
	return client.getIntegerReply();
    }

    /*
     * (non-Javadoc)
     * 
     * @see redis.clients.jedis.RawJedis#zunionstoreSum(byte[],
     * redis.clients.jedis.Pair,
     * redis.clients.jedis.Pair<byte[],java.lang.Double>[])
     */
    @Override
    public Long zunionstoreSum(byte[] dstKey,
	    Pair<byte[], Double> ssetAndWeight1,
	    Pair<byte[], Double>... ssetAndWeightN) {
	zunionstoreHelper("SUM", dstKey, ssetAndWeight1, ssetAndWeightN);
	return client.getIntegerReply();
    }

}