package redis.clients.jedis;

import org.junit.Test;
import redis.clients.jedis.resps.Tuple;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static junit.framework.TestCase.assertEquals;

public class JedisClusterWithPrefixedKeysTest extends JedisClusterTestBase {
    private static final DefaultJedisClientConfig DEFAULT_CLIENT_CONFIG = DefaultJedisClientConfig.builder().password("cluster").build();
    private static final int DEFAULT_TIMEOUT = 2000;
    private static final int DEFAULT_REDIRECTIONS = 5;
    private static final ConnectionPoolConfig DEFAULT_POOL_CONFIG = new ConnectionPoolConfig();

    @Test
    public void hasPrefixedKeys() {
        HostAndPort hp = new HostAndPort("127.0.0.1", 7379);
        ClusterCommandObjectsWithPrefixedKeys.PREFIX_STRING = "test-prefix:";

        try (JedisClusterWithPrefixedKeys cluster = new JedisClusterWithPrefixedKeys(hp, DEFAULT_CLIENT_CONFIG)) {
            cluster.set("foo1", "bar1");
            cluster.set("foo2".getBytes(StandardCharsets.UTF_8), "bar2".getBytes(StandardCharsets.UTF_8));
            ClusterPipeline pipeline = cluster.pipelined();
            pipeline.incr("foo3");
            pipeline.zadd("foo4", 1234, "bar4");
            pipeline.sync();
        }

        try (JedisCluster cluster = new JedisCluster(hp, DEFAULT_CLIENT_CONFIG, DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
            assertEquals("bar1", cluster.get("test-prefix:foo1"));
            assertEquals("bar2", cluster.get("test-prefix:foo2"));
            assertEquals("1", cluster.get("test-prefix:foo3"));
            assertEquals(new Tuple("bar4", 1234d), cluster.zpopmax("test-prefix:foo4"));
        }
    }
}
