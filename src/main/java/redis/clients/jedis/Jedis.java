package redis.clients.jedis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Jedis extends Client {
    public Jedis(String host) {
	super(host);
    }

    public String ping() throws JedisException {
	return sendCommand("PING").getStatusCodeReply();
    }

    public String set(String key, String value) throws JedisException {
	return sendCommand("SET", key, value).getStatusCodeReply();
    }

    public String get(String key) throws JedisException {
	return sendCommand("GET", key).getBulkReply();
    }

    public void quit() throws JedisException {
	sendCommand("QUIT");
    }

    public int exists(String key) throws JedisException {
	return sendCommand("EXISTS", key).getIntegerReply();
    }

    public int del(String... keys) throws JedisException {
	return sendCommand("DEL", keys).getIntegerReply();
    }

    public String type(String key) throws JedisException {
	return sendCommand("TYPE", key).getStatusCodeReply();
    }

    public String flushDB() throws JedisException {
	return sendCommand("FLUSHDB").getStatusCodeReply();
    }

    public List<String> keys(String pattern) throws JedisException {
	return sendCommand("KEYS", pattern).getMultiBulkReply();
    }

    public String randomKey() throws JedisException {
	return sendCommand("RANDOMKEY").getBulkReply();
    }

    public String rename(String oldkey, String newkey) throws JedisException {
	return sendCommand("RENAME", oldkey, newkey).getStatusCodeReply();
    }

    public int renamenx(String oldkey, String newkey) throws JedisException {
	return sendCommand("RENAMENX", oldkey, newkey).getIntegerReply();
    }

    public int dbSize() throws JedisException {
	return sendCommand("DBSIZE").getIntegerReply();
    }

    public int expire(String key, int seconds) throws JedisException {
	return sendCommand("EXPIRE", key, String.valueOf(seconds))
		.getIntegerReply();
    }

    public int expireAt(String key, long unixTime) throws JedisException {
	return sendCommand("EXPIREAT", key, String.valueOf(unixTime))
		.getIntegerReply();
    }

    public int ttl(String key) throws JedisException {
	return sendCommand("TTL", key).getIntegerReply();
    }

    public String select(int index) throws JedisException {
	return sendCommand("SELECT", String.valueOf(index))
		.getStatusCodeReply();
    }

    public int move(String key, int dbIndex) throws JedisException {
	return sendCommand("MOVE", key, String.valueOf(dbIndex))
		.getIntegerReply();
    }

    public String flushAll() throws JedisException {
	return sendCommand("FLUSHALL").getStatusCodeReply();
    }

    public String getSet(String key, String value) throws JedisException {
	return sendCommand("GETSET", key, value).getBulkReply();
    }

    public List<String> mget(String... keys) throws JedisException {
	return sendCommand("MGET", keys).getMultiBulkReply();
    }

    public int setnx(String key, String value) throws JedisException {
	return sendCommand("SETNX", key, value).getIntegerReply();
    }

    public String setex(String key, int seconds, String value)
	    throws JedisException {
	return sendCommand("SETEX", key, String.valueOf(seconds), value)
		.getStatusCodeReply();
    }

    public String mset(String... keysvalues) throws JedisException {
	return sendCommand("MSET", keysvalues).getStatusCodeReply();
    }

    public int msetnx(String... keysvalues) throws JedisException {
	return sendCommand("MSETNX", keysvalues).getIntegerReply();
    }

    public int decrBy(String key, int integer) throws JedisException {
	return sendCommand("DECRBY", key, String.valueOf(integer))
		.getIntegerReply();
    }

    public int decr(String key) throws JedisException {
	return sendCommand("DECR", key).getIntegerReply();
    }

    public int incrBy(String key, int integer) throws JedisException {
	return sendCommand("INCRBY", key, String.valueOf(integer))
		.getIntegerReply();
    }

    public int incr(String key) throws JedisException {
	return sendCommand("INCR", key).getIntegerReply();
    }

    public int append(String key, String value) throws JedisException {
	return sendCommand("APPEND", key, value).getIntegerReply();
    }

    public String substr(String key, int start, int end) throws JedisException {
	return sendCommand("SUBSTR", key, String.valueOf(start),
		String.valueOf(end)).getBulkReply();
    }

    public int hset(String key, String field, String value)
	    throws JedisException {
	return sendCommand("HSET", key, field, value).getIntegerReply();
    }

    public String hget(String key, String field) throws JedisException {
	return sendCommand("HGET", key, field).getBulkReply();
    }

    public int hsetnx(String key, String field, String value)
	    throws JedisException {
	return sendCommand("HSETNX", key, field, value).getIntegerReply();
    }

    public String hmset(String key, Map<String, String> hash)
	    throws JedisException {
	String[] params = new String[(hash.size() * 2) + 1];
	params[0] = key;
	Iterator<Entry<String, String>> iterator = hash.entrySet().iterator();
	int i = 1;
	while (iterator.hasNext()) {
	    Entry<String, String> entry = iterator.next();
	    params[i++] = entry.getKey();
	    params[i++] = entry.getValue();
	}
	return sendCommand("HMSET", params).getStatusCodeReply();
    }

    public List<String> hmget(String key, String... fields)
	    throws JedisException {
	String[] params = new String[fields.length + 1];
	params[0] = key;
	System.arraycopy(fields, 0, params, 1, fields.length);
	return sendCommand("HMGET", params).getMultiBulkReply();
    }

    public int hincrBy(String key, String field, int value)
	    throws JedisException {
	return sendCommand("HINCRBY", key, field, String.valueOf(value))
		.getIntegerReply();
    }

    public int hexists(String key, String field) throws JedisException {
	return sendCommand("HEXISTS", key, field).getIntegerReply();
    }

    public int hdel(String key, String field) throws JedisException {
	return sendCommand("HDEL", key, field).getIntegerReply();
    }

    public int hlen(String key) throws JedisException {
	return sendCommand("HLEN", key).getIntegerReply();
    }

    public List<String> hkeys(String key) throws JedisException {
	return sendCommand("HKEYS", key).getMultiBulkReply();
    }

    public List<String> hvals(String key) throws JedisException {
	return sendCommand("HVALS", key).getMultiBulkReply();
    }

    public Map<String, String> hgetAll(String key) throws JedisException {
	List<String> flatHash = sendCommand("HGETALL", key).getMultiBulkReply();
	Map<String, String> hash = new HashMap<String, String>();
	Iterator<String> iterator = flatHash.iterator();
	while (iterator.hasNext()) {
	    hash.put(iterator.next(), iterator.next());
	}

	return hash;
    }
}
