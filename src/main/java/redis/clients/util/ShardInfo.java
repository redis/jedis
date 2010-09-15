package redis.clients.util;

import redis.clients.jedis.Protocol;

public class ShardInfo {
    @Override
    public String toString() {
	return "ShardInfo [host=" + host + ", port=" + port + ", weight="
		+ weight + "]";
    }

    private String host;
    private int port;
    private int timeout;
    private int weight;
    private String password = null;

    public String getHost() {
	return host;
    }

    public int getPort() {
	return port;
    }

    public int getTimeout() {
	return timeout;
    }

    public ShardInfo(String host) {
	this(host, Protocol.DEFAULT_PORT);
    }

    public ShardInfo(String host, int port) {
	this(host, port, 2000);
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((host == null) ? 0 : host.hashCode());
	result = prime * result + port;
	result = prime * result + timeout;
	result = prime * result + weight;
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	ShardInfo other = (ShardInfo) obj;
	if (host == null) {
	    if (other.host != null)
		return false;
	} else if (!host.equals(other.host))
	    return false;
	if (port != other.port)
	    return false;
	if (timeout != other.timeout)
	    return false;
	if (weight != other.weight)
	    return false;
	return true;
    }

    public ShardInfo(String host, int port, int timeout) {
	this(host, port, timeout, Sharded.DEFAULT_WEIGHT);
    }

    public ShardInfo(String host, int port, int timeout, int weight) {
	this.host = host;
	this.port = port;
	this.timeout = timeout;
	this.weight = weight;
    }

    public String getPassword() {
	return password;
    }

    public void setPassword(String auth) {
	this.password = auth;
    }

    public void setTimeout(int timeout) {
	this.timeout = timeout;
    }

    public int getWeight() {
	return this.weight;
    }
}
