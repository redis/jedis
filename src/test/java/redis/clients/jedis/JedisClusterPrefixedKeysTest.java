package redis.clients.jedis;

import org.junit.Before;
import redis.clients.jedis.args.ClusterResetType;
import redis.clients.jedis.executors.ClusterCommandExecutor;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.util.JedisClusterTestUtil;

import java.time.Duration;
import java.util.Collections;

public class JedisClusterPrefixedKeysTest extends PrefixedKeysTest {
    private static final DefaultJedisClientConfig DEFAULT_CLIENT_CONFIG = DefaultJedisClientConfig.builder().password("cluster").build();
    private static final int DEFAULT_TIMEOUT = 2000;
    private static final int DEFAULT_REDIRECTIONS = 5;
    private static final ConnectionPoolConfig DEFAULT_POOL_CONFIG = new ConnectionPoolConfig();
    private static final HostAndPort HOST_AND_PORT = new HostAndPort("127.0.0.1", 7379);

    @Before
    public void setUp() throws InterruptedException {
        Jedis jedis = new Jedis(HOST_AND_PORT);
        jedis.auth("cluster");
        jedis.clusterReset(ClusterResetType.HARD);
        jedis.flushAll();

        int[] slots = new int[Protocol.CLUSTER_HASHSLOTS];

        for (int i = 0; i < Protocol.CLUSTER_HASHSLOTS; ++i) {
            slots[i] = i;
        }

        jedis.clusterAddSlots(slots);
        JedisClusterTestUtil.waitForClusterReady(jedis);
    }

    @Before
    public void tearDown() throws InterruptedException {
        Jedis jedis = new Jedis(HOST_AND_PORT);
        jedis.auth("cluster");
        jedis.clusterReset(ClusterResetType.HARD);
        jedis.flushAll();
    }

    @Override
    public UnifiedJedis prefixingJedis() {
        ClusterConnectionProvider connectionProvider = new ClusterConnectionProvider(Collections.singleton(HOST_AND_PORT), DEFAULT_CLIENT_CONFIG);
        ClusterCommandExecutor executor = new ClusterCommandExecutor(connectionProvider, 5, Duration.ofSeconds(5 * DEFAULT_TIMEOUT));
        ClusterCommandObjectsWithPrefixedKeys commandObjects = new ClusterCommandObjectsWithPrefixedKeys("test-prefix:");
        return new JedisCluster(executor, connectionProvider, commandObjects, DEFAULT_CLIENT_CONFIG.getRedisProtocol());
    }

    @Override
    public UnifiedJedis nonPrefixingJedis() {
        return new JedisCluster(HOST_AND_PORT, DEFAULT_CLIENT_CONFIG, DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG);
    }
}
