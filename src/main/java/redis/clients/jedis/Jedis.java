package redis.clients.jedis;

import java.util.List;

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

    public void quit() {
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
	return sendCommand("RANDOMKEY").getStatusCodeReply();
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
}
