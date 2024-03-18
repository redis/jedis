package redis.clients.jedis.prefix;

import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.util.prefix.ClusterCommandObjectsWithPrefixedKeys;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.providers.ClusterConnectionProvider;

import java.time.Duration;
import java.util.Collections;

public class JedisClusterPrefixedKeysTest extends PrefixedKeysTest {
    private static final DefaultJedisClientConfig DEFAULT_CLIENT_CONFIG = DefaultJedisClientConfig.builder().password("cluster").build();
    private static final int DEFAULT_TIMEOUT = 2000;
    private static final int DEFAULT_REDIRECTIONS = 5;
    private static final ConnectionPoolConfig DEFAULT_POOL_CONFIG = new ConnectionPoolConfig();
    private static final HostAndPort HOST_AND_PORT = HostAndPorts.getStableClusterServers().get(0);

    @Override
    protected void flush() {
        try (Jedis jedis = new Jedis(HOST_AND_PORT)) {
                jedis.auth("cluster");
                jedis.flushAll();
        }
    }

    @Override
    public void prefixesKeysInTransaction() {
        // Transactions are not supported by JedisCluster, so override this test to no-op
    }

    @Override
    public UnifiedJedis prefixingJedis() {
        ClusterConnectionProvider connectionProvider = new ClusterConnectionProvider(Collections.singleton(HOST_AND_PORT), DEFAULT_CLIENT_CONFIG);
        int maxAttempts = 5;
        Duration maxTotalRetriesDuration = Duration.ofSeconds(5 * DEFAULT_TIMEOUT);
        ClusterCommandObjectsWithPrefixedKeys commandObjects = new ClusterCommandObjectsWithPrefixedKeys("test-prefix:");
        return new JedisCluster(connectionProvider, maxAttempts, maxTotalRetriesDuration, commandObjects, DEFAULT_CLIENT_CONFIG.getRedisProtocol());
    }

    @Override
    public UnifiedJedis nonPrefixingJedis() {
        return new JedisCluster(HOST_AND_PORT, DEFAULT_CLIENT_CONFIG, DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG);
    }
}
