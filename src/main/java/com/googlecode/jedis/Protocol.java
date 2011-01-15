package com.googlecode.jedis;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

final class Protocol {

    protected static enum Command {
	PING, SET, GET, QUIT, EXISTS, DEL, TYPE, FLUSHDB, KEYS, RANDOMKEY, RENAME, RENAMENX, RENAMEX, DBSIZE, EXPIRE, EXPIREAT, TTL, SELECT, MOVE, FLUSHALL, GETSET, MGET, SETNX, SETEX, MSET, MSETNX, DECRBY, DECR, INCRBY, INCR, APPEND, SUBSTR, HSET, HGET, HSETNX, HMSET, HMGET, HINCRBY, HEXISTS, HDEL, HLEN, HKEYS, HVALS, HGETALL, RPUSH, LPUSH, LLEN, LRANGE, LTRIM, LINDEX, LSET, LREM, LPOP, RPOP, RPOPLPUSH, SADD, SMEMBERS, SREM, SPOP, SMOVE, SCARD, SISMEMBER, SINTER, SINTERSTORE, SUNION, SUNIONSTORE, SDIFF, SDIFFSTORE, SRANDMEMBER, ZADD, ZRANGE, ZREM, ZINCRBY, ZRANK, ZREVRANK, ZREVRANGE, ZCARD, ZSCORE, MULTI, DISCARD, EXEC, WATCH, UNWATCH, SORT, BLPOP, BRPOP, AUTH, SUBSCRIBE, PUBLISH, UNSUBSCRIBE, PSUBSCRIBE, PUNSUBSCRIBE, ZCOUNT, ZRANGEBYSCORE, ZREMRANGEBYRANK, ZREMRANGEBYSCORE, ZUNIONSTORE, ZINTERSTORE, SAVE, BGSAVE, BGREWRITEAOF, LASTSAVE, SHUTDOWN, INFO, MONITOR, SLAVEOF, CONFIG, STRLEN, SYNC, LPUSHX, PERSIST, RPUSHX, ECHO, LINSERT, DEBUG, ZREVRANGEBYSCORE;

	protected final byte[] raw;

	Command() {
	    raw = name().getBytes(DEFAULT_CHARSET);
	}
    }

    protected static enum Keyword {
	AGGREGATE, ALPHA, ASC, BY, DESC, GET, LIMIT, MESSAGE, NO, NOSORT, PMESSAGE, PSUBSCRIBE, PUNSUBSCRIBE, OK, ONE, QUEUED, SET, STORE, SUBSCRIBE, UNSUBSCRIBE, WEIGHTS, WITHSCORES;
	protected final byte[] raw;

	Keyword() {
	    raw = name().getBytes(DEFAULT_CHARSET);
	}

    }

    Logger log = LoggerFactory.getLogger(Protocol.class);

    protected static final String DEFAULT_HOST = "localhost";

    protected static final int DEFAULT_PORT = 6379;

    protected static final int DEFAULT_TIMEOUT = 2000;
    protected static final String CHARSET = "UTF-8";
    protected static final byte DOLLAR_BYTE = '$';
    protected static final byte ASTERISK_BYTE = '*';
    protected static final byte PLUS_BYTE = '+';

    protected static final byte MINUS_BYTE = '-';

    protected static final byte COLON_BYTE = ':';

    protected static final Charset DEFAULT_CHARSET = Charsets.UTF_8;

    protected static final String DEFAULT_PASSWORD = null;

    protected static final byte[] toByteArray(final double value) {
	return String.valueOf(value).getBytes(DEFAULT_CHARSET);
    }

    protected static final byte[] toByteArray(final int value) {
	return String.valueOf(value).getBytes(DEFAULT_CHARSET);
    }

    protected static final byte[] toByteArray(final long value) {
	return String.valueOf(value).getBytes(DEFAULT_CHARSET);
    }

    private Object process(final RedisInputStream is) {
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
		throw new JedisException("Unknown reply: " + (char) b);
	    }
	} catch (IOException e) {
	    log.error("Some error", e);
	    throw new JedisException(e);
	}
	return null;
    }

    private byte[] processBulkReply(final RedisInputStream is) {
	int size = Integer.parseInt(is.readLine());
	if (size == -1) {
	    return null;
	}
	byte[] read = new byte[size];
	int offset = 0;
	try {
	    while (offset < size) {
		offset += is.read(read, offset, (size - offset));
	    }
	    // read 2 more bytes for the command delimiter
	    is.readByte();
	    is.readByte();
	} catch (IOException e) {
	    throw new JedisException(e);
	}

	return read;
    }

    protected void processError(final RedisInputStream is) {
	String message = is.readLine();
	throw new JedisException(message);
    }

    protected Long processInteger(final RedisInputStream is) {
	String num = is.readLine();
	return Long.valueOf(num);
    }

    protected List<Object> processMultiBulkReply(final RedisInputStream is) {
	int size = Integer.parseInt(is.readLine());
	if (size == -1) {
	    return null;
	}
	List<Object> ret = new ArrayList<Object>(size);
	for (int i = 0; i < size; i++) {
	    ret.add(process(is));
	}
	return ret;
    }

    protected byte[] processStatusCodeReply(final RedisInputStream is) {
	return SafeEncoder.encode(is.readLine());
    }

    protected Object read(final RedisInputStream is) {
	return process(is);
    }

    // see: Effective Java 2: Item 42, performance
    private void sendCommand(final RedisOutputStream os, final byte[] command,
	    final byte[]... args) {
	try {
	    os.write(ASTERISK_BYTE);
	    os.writeIntCrLf(1 + args.length);
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

	    os.flush();
	} catch (IOException e) {
	    throw new JedisException(e);
	}
    }

    // see: Effective Java 2: Item 42, performance
    protected void sendCommand(final RedisOutputStream os,
	    final Command command, final byte[]... args) {
	sendCommand(os, command.raw, args);
    }

}