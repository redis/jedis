package redis.clients.jedis.providers;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.*;
import redis.clients.jedis.MultiClusterClientConfig.ClusterConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisValidationException;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * @see MultiClusterPooledConnectionProvider
 */
public class MultiClusterPooledConnectionProviderTest {

    private final EndpointConfig endpointStandalone0 = HostAndPorts.getRedisEndpoint("standalone0");
    private final EndpointConfig endpointStandalone1 = HostAndPorts.getRedisEndpoint("standalone1");

    private MultiClusterPooledConnectionProvider provider;

    @Before
    public void setUp() {

        ClusterConfig[] clusterConfigs = new ClusterConfig[2];
        clusterConfigs[0] = new ClusterConfig(endpointStandalone0.getHostAndPort(), endpointStandalone0.getClientConfigBuilder().build());
        clusterConfigs[1] = new ClusterConfig(endpointStandalone1.getHostAndPort(), endpointStandalone0.getClientConfigBuilder().build());

        provider = new MultiClusterPooledConnectionProvider(new MultiClusterClientConfig.Builder(clusterConfigs).build());
    }

    @Test
    public void testCircuitBreakerForcedTransitions() {

        CircuitBreaker circuitBreaker = provider.getClusterCircuitBreaker(1);
        circuitBreaker.getState();

        if (CircuitBreaker.State.FORCED_OPEN.equals(circuitBreaker.getState()))
            circuitBreaker.transitionToClosedState();

        circuitBreaker.transitionToForcedOpenState();
        assertEquals(CircuitBreaker.State.FORCED_OPEN, circuitBreaker.getState());

        circuitBreaker.transitionToClosedState();
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    public void testIncrementActiveMultiClusterIndex() {
        int index = provider.incrementActiveMultiClusterIndex();
        assertEquals(2, index);
    }

    @Test(expected = JedisConnectionException.class)
    public void testIncrementActiveMultiClusterIndexOutOfRange() {
        provider.setActiveMultiClusterIndex(1);

        int index = provider.incrementActiveMultiClusterIndex();
        assertEquals(2, index);

        provider.incrementActiveMultiClusterIndex(); // Should throw an exception
    }

    @Test
    public void testIsLastClusterCircuitBreakerForcedOpen() {
        provider.setActiveMultiClusterIndex(1);

        try {
            provider.incrementActiveMultiClusterIndex();
        } catch (Exception e) {}

        // This should set the isLastClusterCircuitBreakerForcedOpen to true
        try {
            provider.incrementActiveMultiClusterIndex();
        } catch (Exception e) {}

        assertEquals(true, provider.isLastClusterCircuitBreakerForcedOpen());
    }

    @Test
    public void testRunClusterFailoverPostProcessor() {
        ClusterConfig[] clusterConfigs = new ClusterConfig[2];
        clusterConfigs[0] = new ClusterConfig(new HostAndPort("purposefully-incorrect", 0000),
                                                                    DefaultJedisClientConfig.builder().build());
        clusterConfigs[1] = new ClusterConfig(new HostAndPort("purposefully-incorrect", 0001),
                                                                    DefaultJedisClientConfig.builder().build());

        MultiClusterClientConfig.Builder builder = new MultiClusterClientConfig.Builder(clusterConfigs);

        // Configures a single failed command to trigger an open circuit on the next subsequent failure
        builder.circuitBreakerSlidingWindowSize(1);
        builder.circuitBreakerSlidingWindowMinCalls(1);

        AtomicBoolean isValidTest = new AtomicBoolean(false);

        MultiClusterPooledConnectionProvider localProvider = new MultiClusterPooledConnectionProvider(builder.build());
        localProvider.setClusterFailoverPostProcessor(a -> { isValidTest.set(true); });

        try (UnifiedJedis jedis = new UnifiedJedis(localProvider)) {

            // This should fail after 3 retries and meet the requirements to open the circuit on the next iteration
            try {
                jedis.get("foo");
            } catch (Exception e) {}

            // This should fail after 3 retries and open the circuit which will trigger the post processor
            try {
                jedis.get("foo");
            } catch (Exception e) {}

        }

        assertEquals(true, isValidTest.get());
    }

    @Test(expected = JedisValidationException.class)
    public void testSetActiveMultiClusterIndexEqualsZero() {
        provider.setActiveMultiClusterIndex(0); // Should throw an exception
    }

    @Test(expected = JedisValidationException.class)
    public void testSetActiveMultiClusterIndexLessThanZero() {
        provider.setActiveMultiClusterIndex(-1); // Should throw an exception
    }

    @Test(expected = JedisValidationException.class)
    public void testSetActiveMultiClusterIndexOutOfRange() {
        provider.setActiveMultiClusterIndex(3); // Should throw an exception
    }

    @Test
    public void testConnectionPoolConfigApplied() {
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxTotal(8);
        poolConfig.setMaxIdle(4);
        poolConfig.setMinIdle(1);
        ClusterConfig[] clusterConfigs = new ClusterConfig[2];
        clusterConfigs[0] = new ClusterConfig(endpointStandalone0.getHostAndPort(), endpointStandalone0.getClientConfigBuilder().build(), poolConfig);
        clusterConfigs[1] = new ClusterConfig(endpointStandalone1.getHostAndPort(), endpointStandalone0.getClientConfigBuilder().build(), poolConfig);
        try (MultiClusterPooledConnectionProvider customProvider = new MultiClusterPooledConnectionProvider(
                new MultiClusterClientConfig.Builder(clusterConfigs).build())) {
            MultiClusterPooledConnectionProvider.Cluster activeCluster = customProvider.getCluster();
            ConnectionPool connectionPool = activeCluster.getConnectionPool();
            assertEquals(8, connectionPool.getMaxTotal());
            assertEquals(4, connectionPool.getMaxIdle());
            assertEquals(1, connectionPool.getMinIdle());
        }
    }
}
