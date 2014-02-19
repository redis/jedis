package redis.clients.jedis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.Set;

public class JedisRandomConnectionHandler extends JedisClusterConnectionHandler {

    public JedisRandomConnectionHandler(Set<HostAndPort> nodes) {
	super(nodes, new GenericObjectPoolConfig());
    }

    public JedisRandomConnectionHandler(Set<HostAndPort> nodes,
        final GenericObjectPoolConfig poolConfig) {
    super(nodes, poolConfig);
    }

    public Jedis getConnection() {
	return getRandomConnection().getResource();
    }

    @Override
    Jedis getConnectionFromSlot(int slot) {
	return getRandomConnection().getResource();
    }
}
