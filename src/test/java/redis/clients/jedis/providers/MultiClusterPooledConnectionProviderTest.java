package redis.clients.jedis.providers;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.*;
import redis.clients.jedis.MultiClusterJedisClientConfig.ClusterJedisClientConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisValidationException;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * @see MultiClusterPooledConnectionProvider
 */
public class MultiClusterPooledConnectionProviderTest {

    private static final HostAndPort hostAndPort1 = HostAndPorts.getRedisServers().get(0);
    private static final HostAndPort hostAndPort2 = HostAndPorts.getRedisServers().get(0);

    private MultiClusterPooledConnectionProvider provider;

    @Before
    public void setUp() {

        ClusterJedisClientConfig[] clusterJedisClientConfigs = new ClusterJedisClientConfig[2];
        clusterJedisClientConfigs[0] = new ClusterJedisClientConfig(hostAndPort1, DefaultJedisClientConfig.builder().build());
        clusterJedisClientConfigs[1] = new ClusterJedisClientConfig(hostAndPort2, DefaultJedisClientConfig.builder().build());

        MultiClusterJedisClientConfig.Builder builder = new MultiClusterJedisClientConfig.Builder(clusterJedisClientConfigs);

        // Configures a single failed command to trigger an open circuit on the next subsequent failure
        builder.circuitBreakerSlidingWindowSize(1);
        builder.circuitBreakerSlidingWindowMinCalls(1);

        provider = new MultiClusterPooledConnectionProvider(builder.build());
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
        provider.setActiveMultiClusterIndex(1);

        AtomicBoolean isValidTest = new AtomicBoolean(false);

        provider.setClusterFailoverPostProcessor(a -> { isValidTest.set(true); });

        try (UnifiedJedis jedis = new UnifiedJedis(provider)) {

            // This should fail after 3 retries and meet the requirements to open the circuit on the next iteration
            try {
                jedis.set("foo", "bar");
                jedis.incr("foo");
            } catch (Exception e) {}

            // This should fail after 3 retries and open the circuit which will trigger the post processor
            try {
                jedis.set("foo", "bar");
                jedis.incr("foo");
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

}