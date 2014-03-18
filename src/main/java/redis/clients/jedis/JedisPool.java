package redis.clients.jedis;

import java.net.URI;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.util.Pool;

public class JedisPool extends Pool<Jedis> {

    public JedisPool(final GenericObjectPoolConfig poolConfig, final String host) {
	this(poolConfig, host, Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT,
		null, Protocol.DEFAULT_DATABASE, null);
    }

    public JedisPool(String host, int port) {
	this(new GenericObjectPoolConfig(), host, port,
		Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE, null);
    }

    public JedisPool(final String host) {
	URI uri = URI.create(host);
	if (uri.getScheme() != null && uri.getScheme().equals("redis")) {
	    String h = uri.getHost();
	    int port = uri.getPort();
	    String password = uri.getUserInfo().split(":", 2)[1];
	    int database = Integer.parseInt(uri.getPath().split("/", 2)[1]);
	    this.internalPool = new GenericObjectPool<Jedis>(
		    new JedisFactory(h, port, Protocol.DEFAULT_TIMEOUT,
			    password, database, null),
		    new GenericObjectPoolConfig());
	} else {
	    this.internalPool = new GenericObjectPool<Jedis>(new JedisFactory(
		    host, Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT,
		    null, Protocol.DEFAULT_DATABASE, null),
		    new GenericObjectPoolConfig());
	}
    }

    public JedisPool(final URI uri) {
	String h = uri.getHost();
	int port = uri.getPort();
	String password = uri.getUserInfo().split(":", 2)[1];
	int database = Integer.parseInt(uri.getPath().split("/", 2)[1]);
	this.internalPool = new GenericObjectPool<Jedis>(new JedisFactory(h,
		port, Protocol.DEFAULT_TIMEOUT, password, database, null),
		new GenericObjectPoolConfig());
    }

    public JedisPool(final GenericObjectPoolConfig poolConfig,
	    final String host, int port, int timeout, final String password) {
	this(poolConfig, host, port, timeout, password,
		Protocol.DEFAULT_DATABASE, null);
    }

    public JedisPool(final GenericObjectPoolConfig poolConfig,
	    final String host, final int port) {
	this(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT, null,
		Protocol.DEFAULT_DATABASE, null);
    }

    public JedisPool(final GenericObjectPoolConfig poolConfig,
	    final String host, final int port, final int timeout) {
	this(poolConfig, host, port, timeout, null, Protocol.DEFAULT_DATABASE,
		null);
    }

    public JedisPool(final GenericObjectPoolConfig poolConfig,
	    final String host, int port, int timeout, final String password,
	    final int database) {
	this(poolConfig, host, port, timeout, password, database, null);
    }

    public JedisPool(final GenericObjectPoolConfig poolConfig,
	    final String host, int port, int timeout, final String password,
	    final int database, final String clientName) {
	super(poolConfig, new JedisFactory(host, port, timeout, password,
		database, clientName));
    }

    public void returnBrokenResource(final Jedis resource) {
	if (resource != null) {
	    returnBrokenResourceObject(resource);
	}
    }

    public void returnResource(final Jedis resource) {
	if (resource != null) {
	    resource.resetState();
	    returnResourceObject(resource);
	}
    }
}
