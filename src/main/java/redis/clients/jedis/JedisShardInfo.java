package redis.clients.jedis;

import java.net.URI;

import redis.clients.util.ShardInfo;
import redis.clients.util.Sharded;

public class JedisShardInfo extends ShardInfo<Jedis> {
    public String toString() {
	return host + ":" + port + "*" + getWeight();
    }

    private int timeout;
    private String host;
    private int port;
    private String password = null;
    private String name = null;

    public String getHost() {
	return host;
    }

    public int getPort() {
	return port;
    }

    public JedisShardInfo(String host) {
	super(Sharded.DEFAULT_WEIGHT);
	URI uri = URI.create(host);
	if (uri.getScheme() != null && uri.getScheme().equals("redis")) {
	    this.host = uri.getHost();
	    this.port = uri.getPort();
	    this.password = uri.getUserInfo().split(":", 2)[1];
	} else {
	    this.host = host;
	    this.port = Protocol.DEFAULT_PORT;
	}
    }

    public JedisShardInfo(String host, String name) {
	this(host, Protocol.DEFAULT_PORT, name);
    }

    public JedisShardInfo(String host, int port) {
	this(host, port, 2000);
    }

    public JedisShardInfo(String host, int port, String name) {
	this(host, port, 2000, name);
    }

    public JedisShardInfo(String host, int port, int timeout) {
	this(host, port, timeout, Sharded.DEFAULT_WEIGHT);
    }

    public JedisShardInfo(String host, int port, int timeout, String name) {
	this(host, port, timeout, Sharded.DEFAULT_WEIGHT);
	this.name = name;
    }

    public JedisShardInfo(String host, int port, int timeout, int weight) {
	super(weight);
	this.host = host;
	this.port = port;
	this.timeout = timeout;
    }

    public JedisShardInfo(URI uri) {
	super(Sharded.DEFAULT_WEIGHT);
	this.host = uri.getHost();
	this.port = uri.getPort();
	this.password = uri.getUserInfo().split(":", 2)[1];
    }

    public String getPassword() {
	return password;
    }

    public void setPassword(String auth) {
	this.password = auth;
    }

    public int getTimeout() {
	return timeout;
    }

    public void setTimeout(int timeout) {
	this.timeout = timeout;
    }

    public String getName() {
	return name;
    }

    @Override
    public Jedis createResource() {
	return new Jedis(this);
    }
}
