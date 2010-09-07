package redis.clients.jedis;

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
	boolean done = false;
	while (!done) {
	    try {
		jedis.connect();
		done = true;
	    } catch (Exception e) {
		try {
		    Thread.sleep(100);
		} catch (InterruptedException e1) {
		}
	    }
	}
	return jedis;
    }

    @Override
    protected void destroyResource(Jedis jedis) {
	if (jedis.isConnected()) {
	    try {
		jedis.quit();
		jedis.disconnect();
	    } catch (Exception e) {

	    }
	}
    }

    @Override
    protected boolean isResourceValid(Jedis jedis) {
	try {
	    return jedis.isConnected() && jedis.ping().equals("PONG");
	} catch (Exception ex) {
	    return false;
	}
    }
}