package redis.clients.jedis.csc;

import org.junit.Test;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.ProtocolCommand;

import java.util.List;

public class CSCTest {
    private static JedisClientConfig clientConfig;
    private static CacheConfig cacheConfig;

    static {
        clientConfig = DefaultJedisClientConfig.builder()
                .resp3()                                      // RESP3 protocol is required for client-side caching
                .trackingConfig(TrackingConfig.BROADCAST)
                .build();

        cacheConfig = getCacheConfig();
    }

    private static CacheConfig getCacheConfig() {

        // This is a simple cacheable implementation that ignores keys starting with "ignore_me"
        Cacheable cacheable = new DefaultCacheable() {
            @Override
            public boolean isCacheable(ProtocolCommand command, List<Object> keys) {
                return isDefaultCacheableCommand(command);
            }
        };

        // Create a cache with a maximum size of 10000 entries
        return CacheConfig.builder()
                .maxSize(10000)
                .cacheable(cacheable)
                .build();
    }

    @Test
    public void testTrackingOnBroadcastMode() {
        HostAndPort node = HostAndPort.from("127.0.0.1:6379");
        try (UnifiedJedis client = new UnifiedJedis(node, clientConfig, CacheFactory.getCache(cacheConfig))) {
            String a1 = client.get("a");
            String a2 = client.get("a");
        }
    }
}
