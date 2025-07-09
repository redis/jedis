package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiClusterClientConfig;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;
import redis.clients.jedis.scenario.RecommendedSettings;

public class HealthCheckIntegrationTest {

    private final EndpointConfig endpoint1 = HostAndPorts.getRedisEndpoint("standalone0");
    private final EndpointConfig endpoint2 = HostAndPorts.getRedisEndpoint("standalone1");

    private final JedisClientConfig clientConfig = endpoint1.getClientConfigBuilder()
        .socketTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS)
        .connectionTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS).build();

    @Test
    public void testDisableHealthCheck() {
        FailoverOptions fo1 = FailoverOptions.builder().build();

        MultiClusterPooledConnectionProvider customProvider = getMCCF(fo1);
        try (UnifiedJedis customClient = new UnifiedJedis(customProvider)) {
            // Verify that the client can connect and execute commands
            String result = customClient.ping();
            assertEquals("PONG", result);
        }
    }

    @Test
    public void testDefaultStrategySupplier() {
        FailoverOptions fo1 = FailoverOptions.builder().enableHealthCheck(true).build();

        MultiClusterPooledConnectionProvider customProvider = getMCCF(fo1);
        try (UnifiedJedis customClient = new UnifiedJedis(customProvider)) {
            // Verify that the client can connect and execute commands
            String result = customClient.ping();
            assertEquals("PONG", result);
        }
    }

    @Test
    public void testCustomStrategySupplier() {
        // Create a StrategySupplier that uses the JedisClientConfig when available
        FailoverOptions.StrategySupplier strategySupplier = (hostAndPort, jedisClientConfig) -> {
            UnifiedJedis pinger = new UnifiedJedis(hostAndPort, jedisClientConfig);
            return new HealthCheckStrategy() {

                @Override
                public int getInterval() {
                    return 500;
                }

                @Override
                public int getTimeout() {
                    return 500;
                }

                @Override
                public HealthStatus doHealthCheck(Endpoint endpoint) {
                    return "OK".equals(pinger.ping()) ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
                }

            };
        };

        FailoverOptions fo1 = FailoverOptions.builder().healthCheckStrategySupplier(strategySupplier).build();

        MultiClusterPooledConnectionProvider customProvider = getMCCF(fo1);
        try (UnifiedJedis customClient = new UnifiedJedis(customProvider)) {
            // Verify that the client can connect and execute commands
            String result = customClient.ping();
            assertEquals("PONG", result);
        }
    }

    private MultiClusterPooledConnectionProvider getMCCF(FailoverOptions fo) {
        List<MultiClusterClientConfig.ClusterConfig> clusterConfigs = Arrays.stream(new EndpointConfig[] { endpoint1 })
            .map(e -> MultiClusterClientConfig.ClusterConfig.builder(e.getHostAndPort(), clientConfig)
                .failoverOptions(fo).build())
            .collect(Collectors.toList());

        MultiClusterClientConfig mccf = new MultiClusterClientConfig.Builder(clusterConfigs).retryMaxAttempts(1)
            .retryWaitDuration(1).circuitBreakerSlidingWindowType(SlidingWindowType.COUNT_BASED)
            .circuitBreakerSlidingWindowSize(1).circuitBreakerFailureRateThreshold(100)
            .circuitBreakerSlidingWindowMinCalls(1).build();

        return new MultiClusterPooledConnectionProvider(mccf);
    }
}
