package redis.clients.jedis;

import java.io.IOException;
import java.net.UnknownHostException;

import redis.clients.util.FixedResourcePool;

public class JedisPool extends FixedResourcePool<Jedis> {
    private String host;
    private int port;
    private int timeout;

    public JedisPool(String host) {
	this.host = host;
	this.port = Protocol.DEFAULT_PORT;
    }

    public JedisPool(String host, int port) {
	this.host = host;
	this.port = port;
    }

    public JedisPool(String host, int port, int timeout) {
	this.host = host;
	this.port = port;
	this.timeout = timeout;
    }

    @Override
    protected Jedis createResource() {
	Jedis jedis = new Jedis(this.host, this.port, this.timeout);
	try {
	    jedis.connect();
	} catch (UnknownHostException e) {
	    throw new JedisException(e);
	} catch (IOException e) {
	    throw new JedisException(e);
	}
	return jedis;
    }

    @Override
    protected void destroyResource(Jedis jedis) {
	jedis.quit();
	try {
	    jedis.disconnect();
	} catch (IOException e) {
	    throw new JedisException(e);
	}
    }

    @Override
    protected boolean isResourceValid(Jedis jedis) {
	return jedis.ping().equals("OK");
    }
}