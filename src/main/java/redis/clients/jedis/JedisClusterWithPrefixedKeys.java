package redis.clients.jedis;

import redis.clients.jedis.executors.ClusterCommandExecutor;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.util.JedisClusterCRC16;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

public class JedisClusterWithPrefixedKeys extends UnifiedJedis {

    public JedisClusterWithPrefixedKeys(HostAndPort node, JedisClientConfig clientConfig) {
        this(new ClusterConnectionProvider(Collections.singleton(node), clientConfig), clientConfig);
    }

    public JedisClusterWithPrefixedKeys(ClusterConnectionProvider provider, JedisClientConfig clientConfig) {
        super(new ClusterCommandExecutor(provider, 5, Duration.ofSeconds(100)), provider, new ClusterCommandObjectsWithPrefixedKeys(), clientConfig.getRedisProtocol());

    }

    public Map<String, ConnectionPool> getClusterNodes() {
        return ((ClusterConnectionProvider) provider).getNodes();
    }

    public Connection getConnectionFromSlot(int slot) {
        return ((ClusterConnectionProvider) provider).getConnectionFromSlot(slot);
    }

    // commands
    public long spublish(String channel, String message) {
        return executeCommand(commandObjects.spublish(channel, message));
    }

    public long spublish(byte[] channel, byte[] message) {
        return executeCommand(commandObjects.spublish(channel, message));
    }

    public void ssubscribe(final JedisShardedPubSub jedisPubSub, final String... channels) {
        try (Connection connection = getConnectionFromSlot(JedisClusterCRC16.getSlot(channels[0]))) {
            jedisPubSub.proceed(connection, channels);
        }
    }

    public void ssubscribe(BinaryJedisShardedPubSub jedisPubSub, final byte[]... channels) {
        try (Connection connection = getConnectionFromSlot(JedisClusterCRC16.getSlot(channels[0]))) {
            jedisPubSub.proceed(connection, channels);
        }
    }
    // commands

    @Override
    public ClusterPipeline pipelined() {
        return new ClusterPipeline((ClusterConnectionProvider) provider, (ClusterCommandObjects) commandObjects);
    }

    @Override
    public Transaction multi() {
        throw new UnsupportedOperationException();
    }
}
