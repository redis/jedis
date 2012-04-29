package redis.clients.jedis;

import redis.clients.util.ConnectionInfo;
import redis.clients.util.ShardInfo;
import redis.clients.util.Sharded;

public class JedisShardInfo extends ConnectionInfo implements ShardInfo<Jedis> {
    
    private int timeout;
    private int weight;
    private String name = null;
    
    @Override
    public String toString() {
        return super.toString() + "*" + getWeight();
    }

    public JedisShardInfo(String host) {
        this(host, Protocol.DEFAULT_PORT);
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
    	super(host, port);
        this.weight = weight;
        this.timeout = timeout;
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

    public Jedis createResource() {
        return new Jedis(this);
    }

	public int getWeight() {
		return weight;
	}
}
