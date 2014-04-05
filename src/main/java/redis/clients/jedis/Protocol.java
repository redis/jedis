package redis.clients.jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisClusterException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.util.RedisInputStream;
import redis.clients.util.RedisOutputStream;
import redis.clients.util.SafeEncoder;

public final class Protocol {

    private static final String ASK_RESPONSE = "ASK";
    private static final String MOVED_RESPONSE = "MOVED";
    private static final String CLUSTERDOWN_RESPONSE = "CLUSTERDOWN";
    public static final int DEFAULT_PORT = 6379;
    public static final int DEFAULT_SENTINEL_PORT = 26379;
    public static final int DEFAULT_TIMEOUT = 2000;
    public static final int DEFAULT_DATABASE = 0;

    public static final String CHARSET = "UTF-8";

    public static final byte DOLLAR_BYTE = '$';
    public static final byte ASTERISK_BYTE = '*';
    public static final byte PLUS_BYTE = '+';
    public static final byte MINUS_BYTE = '-';
    public static final byte COLON_BYTE = ':';

    public static final String SENTINEL_MASTERS = "masters";
    public static final String SENTINEL_GET_MASTER_ADDR_BY_NAME = "get-master-addr-by-name";
    public static final String SENTINEL_RESET = "reset";
    public static final String SENTINEL_SLAVES = "slaves";
    public static final String SENTINEL_FAILOVER = "failover";
    public static final String SENTINEL_MONITOR = "monitor";
    public static final String SENTINEL_REMOVE = "remove";
    public static final String SENTINEL_SET = "set";

    public static final String CLUSTER_NODES = "nodes";
    public static final String CLUSTER_MEET = "meet";
    public static final String CLUSTER_ADDSLOTS = "addslots";
    public static final String CLUSTER_DELSLOTS = "delslots";
    public static final String CLUSTER_INFO = "info";
    public static final String CLUSTER_GETKEYSINSLOT = "getkeysinslot";
    public static final String CLUSTER_SETSLOT = "setslot";
    public static final String CLUSTER_SETSLOT_NODE = "node";
    public static final String CLUSTER_SETSLOT_MIGRATING = "migrating";
    public static final String CLUSTER_SETSLOT_IMPORTING = "importing";
    public static final String PUBSUB_CHANNELS= "channels";
    public static final String PUBSUB_NUMSUB = "numsub";
    public static final String PUBSUB_NUM_PAT = "numpat";

    private Protocol() {
	// this prevent the class from instantiation
    }

    public static void sendCommand(final RedisOutputStream os,
	    final Command command, final byte[]... args) {
	sendCommand(os, command.raw, args);
    }

    private static void sendCommand(final RedisOutputStream os,
	    final byte[] command, final byte[]... args) {
	try {
	    os.write(ASTERISK_BYTE);
	    os.writeIntCrLf(args.length + 1);
	    os.write(DOLLAR_BYTE);
	    os.writeIntCrLf(command.length);
	    os.write(command);
	    os.writeCrLf();

	    for (final byte[] arg : args) {
		os.write(DOLLAR_BYTE);
		os.writeIntCrLf(arg.length);
		os.write(arg);
		os.writeCrLf();
	    }
	} catch (IOException e) {
	    throw new JedisConnectionException(e);
	}
    }

    private static void processError(final RedisInputStream is) {
	String message = is.readLine();
	// TODO: I'm not sure if this is the best way to do this.
	// Maybe Read only first 5 bytes instead?
	if (message.startsWith(MOVED_RESPONSE)) {
	    String[] movedInfo = parseTargetHostAndSlot(message);
	    throw new JedisMovedDataException(message, new HostAndPort(
		    movedInfo[1], Integer.valueOf(movedInfo[2])),
		    Integer.valueOf(movedInfo[0]));
	} else if (message.startsWith(ASK_RESPONSE)) {
	    String[] askInfo = parseTargetHostAndSlot(message);
	    throw new JedisAskDataException(message, new HostAndPort(
		    askInfo[1], Integer.valueOf(askInfo[2])),
		    Integer.valueOf(askInfo[0]));
	} else if (message.startsWith(CLUSTERDOWN_RESPONSE)) {
	    throw new JedisClusterException(message);
	}
	throw new JedisDataException(message);
    }

    private static String[] parseTargetHostAndSlot(
	    String clusterRedirectResponse) {
	String[] response = new String[3];
	String[] messageInfo = clusterRedirectResponse.split(" ");
	String[] targetHostAndPort = messageInfo[2].split(":");
	response[0] = messageInfo[1];
	response[1] = targetHostAndPort[0];
	response[2] = targetHostAndPort[1];
	return response;
    }

    private static Object process(final RedisInputStream is) {
	try {
	    byte b = is.readByte();
	    if (b == MINUS_BYTE) {
		processError(is);
	    } else if (b == ASTERISK_BYTE) {
		return processMultiBulkReply(is);
	    } else if (b == COLON_BYTE) {
		return processInteger(is);
	    } else if (b == DOLLAR_BYTE) {
		return processBulkReply(is);
	    } else if (b == PLUS_BYTE) {
		return processStatusCodeReply(is);
	    } else {
		throw new JedisConnectionException("Unknown reply: " + (char) b);
	    }
	} catch (IOException e) {
	    throw new JedisConnectionException(e);
	}
	return null;
    }

    private static byte[] processStatusCodeReply(final RedisInputStream is) {
	return SafeEncoder.encode(is.readLine());
    }

    private static byte[] processBulkReply(final RedisInputStream is) {
	int len = Integer.parseInt(is.readLine());
	if (len == -1) {
	    return null;
	}
	byte[] read = new byte[len];
	int offset = 0;
	try {
	    while (offset < len) {
		int size = is.read(read, offset, (len - offset));
		if (size == -1)
		    throw new JedisConnectionException(
			    "It seems like server has closed the connection.");
		offset += size;
	    }
	    // read 2 more bytes for the command delimiter
	    is.readByte();
	    is.readByte();
	} catch (IOException e) {
	    throw new JedisConnectionException(e);
	}

	return read;
    }

    private static Long processInteger(final RedisInputStream is) {
	String num = is.readLine();
	return Long.valueOf(num);
    }

    private static List<Object> processMultiBulkReply(final RedisInputStream is) {
	int num = Integer.parseInt(is.readLine());
	if (num == -1) {
	    return null;
	}
	List<Object> ret = new ArrayList<Object>(num);
	for (int i = 0; i < num; i++) {
	    try {
		ret.add(process(is));
	    } catch (JedisDataException e) {
		ret.add(e);
	    }
	}
	return ret;
    }

    public static Object read(final RedisInputStream is) {
	return process(is);
    }

    public static final byte[] toByteArray(final boolean value) {
    return toByteArray(value ? 1 : 0);
    }

    public static final byte[] toByteArray(final int value) {
	return SafeEncoder.encode(String.valueOf(value));
    }

    public static final byte[] toByteArray(final long value) {
	return SafeEncoder.encode(String.valueOf(value));
    }

    public static final byte[] toByteArray(final double value) {
	return SafeEncoder.encode(String.valueOf(value));
    }

    public static enum Command {
	PING, SET, GET, QUIT, EXISTS, DEL, TYPE, FLUSHDB, KEYS, RANDOMKEY, RENAME, RENAMENX, RENAMEX, DBSIZE, EXPIRE, EXPIREAT, TTL, SELECT, MOVE, FLUSHALL, GETSET, MGET, SETNX, SETEX, MSET, MSETNX, DECRBY, DECR, INCRBY, INCR, APPEND, SUBSTR, HSET, HGET, HSETNX, HMSET, HMGET, HINCRBY, HEXISTS, HDEL, HLEN, HKEYS, HVALS, HGETALL, RPUSH, LPUSH, LLEN, LRANGE, LTRIM, LINDEX, LSET, LREM, LPOP, RPOP, RPOPLPUSH, SADD, SMEMBERS, SREM, SPOP, SMOVE, SCARD, SISMEMBER, SINTER, SINTERSTORE, SUNION, SUNIONSTORE, SDIFF, SDIFFSTORE, SRANDMEMBER, ZADD, ZRANGE, ZREM, ZINCRBY, ZRANK, ZREVRANK, ZREVRANGE, ZCARD, ZSCORE, MULTI, DISCARD, EXEC, WATCH, UNWATCH, SORT, BLPOP, BRPOP, AUTH, SUBSCRIBE, PUBLISH, UNSUBSCRIBE, PSUBSCRIBE, PUNSUBSCRIBE, PUBSUB, ZCOUNT, ZRANGEBYSCORE, ZREVRANGEBYSCORE, ZREMRANGEBYRANK, ZREMRANGEBYSCORE, ZUNIONSTORE, ZINTERSTORE, SAVE, BGSAVE, BGREWRITEAOF, LASTSAVE, SHUTDOWN, INFO, MONITOR, SLAVEOF, CONFIG, STRLEN, SYNC, LPUSHX, PERSIST, RPUSHX, ECHO, LINSERT, DEBUG, BRPOPLPUSH, SETBIT, GETBIT, SETRANGE, GETRANGE, EVAL, EVALSHA, SCRIPT, SLOWLOG, OBJECT, BITCOUNT, BITOP, SENTINEL, DUMP, RESTORE, PEXPIRE, PEXPIREAT, PTTL, INCRBYFLOAT, PSETEX, CLIENT, TIME, MIGRATE, HINCRBYFLOAT, SCAN, HSCAN, SSCAN, ZSCAN, WAIT, CLUSTER, ASKING;

	public final byte[] raw;

	Command() {
	    raw = SafeEncoder.encode(this.name());
	}
    }

    public static enum Keyword {
	AGGREGATE, ALPHA, ASC, BY, DESC, GET, LIMIT, MESSAGE, NO, NOSORT, PMESSAGE, PSUBSCRIBE, PUNSUBSCRIBE, OK, ONE, QUEUED, SET, STORE, SUBSCRIBE, UNSUBSCRIBE, WEIGHTS, WITHSCORES, RESETSTAT, RESET, FLUSH, EXISTS, LOAD, KILL, LEN, REFCOUNT, ENCODING, IDLETIME, AND, OR, XOR, NOT, GETNAME, SETNAME, LIST, MATCH, COUNT;
	public final byte[] raw;

	Keyword() {
	    raw = SafeEncoder.encode(this.name().toLowerCase());
	}
    }
}
