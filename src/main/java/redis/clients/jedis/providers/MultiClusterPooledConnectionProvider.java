package redis.clients.jedis.providers;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.*;
import redis.clients.jedis.MultiClusterClientConfig.ClusterConfig;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisValidationException;
import redis.clients.jedis.util.Pool;


/**
 * @author Allen Terleto (aterleto)
 * <p>
 * ConnectionProvider which supports multiple cluster/database endpoints each with their own isolated connection pool.
 * With this ConnectionProvider users can seamlessly failover to Disaster Recovery (DR), Backup, and Active-Active cluster(s)
 * by using simple configuration which is passed through from Resilience4j - https://resilience4j.readme.io/docs
 * <p>
 * Support for manual failback is provided by way of {@link #setActiveMultiClusterIndex(int)}
 * <p>
 */
// TODO: move?
@Experimental
public class MultiClusterPooledConnectionProvider implements ConnectionProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Ordered map of cluster/database endpoints which were provided at startup via the MultiClusterClientConfig.
     * Users can move down (failover) or (up) failback the map depending on their availability and order.
     */
    private final Map<Integer, Cluster> multiClusterMap = new ConcurrentHashMap<>();

    /**
     * Indicates the actively used cluster/database endpoint (connection pool) amongst the pre-configured list which were
     * provided at startup via the MultiClusterClientConfig. All traffic will be routed according to this index.
     */
    private volatile Integer activeMultiClusterIndex = 1;

    /**
     * Indicates the final cluster/database endpoint (connection pool), according to the pre-configured list
     * provided at startup via the MultiClusterClientConfig, is unavailable and therefore no further failover is possible.
     * Users can manually failback to an available cluster which would reset this flag via {@link #setActiveMultiClusterIndex(int)}
     */
    private volatile boolean lastClusterCircuitBreakerForcedOpen = false;

    /**
     * Functional interface typically used for activeMultiClusterIndex persistence or custom logging after a successful
     * failover of a cluster/database endpoint (connection pool). Cluster/database endpoint info is passed as the sole parameter
     *
     * Example: cluster:2:redis-smart-cache.demo.com:12000
     */
    private Consumer<String> clusterFailoverPostProcessor;

    private List<Class<? extends Throwable>> fallbackExceptionList;

    public MultiClusterPooledConnectionProvider(MultiClusterClientConfig multiClusterClientConfig) {

        if (multiClusterClientConfig == null)
            throw new JedisValidationException("MultiClusterClientConfig must not be NULL for MultiClusterPooledConnectionProvider");

        ////////////// Configure Retry ////////////////////

        RetryConfig.Builder retryConfigBuilder = RetryConfig.custom();
        retryConfigBuilder.maxAttempts(multiClusterClientConfig.getRetryMaxAttempts());
        retryConfigBuilder.intervalFunction(IntervalFunction.ofExponentialBackoff(multiClusterClientConfig.getRetryWaitDuration(),
                multiClusterClientConfig.getRetryWaitDurationExponentialBackoffMultiplier()));
        retryConfigBuilder.failAfterMaxAttempts(false); // JedisConnectionException will be thrown
        retryConfigBuilder.retryExceptions(multiClusterClientConfig.getRetryIncludedExceptionList().stream().toArray(Class[]::new));

        List<Class> retryIgnoreExceptionList = multiClusterClientConfig.getRetryIgnoreExceptionList();
        if (retryIgnoreExceptionList != null)
            retryConfigBuilder.ignoreExceptions(retryIgnoreExceptionList.stream().toArray(Class[]::new));

        RetryConfig retryConfig = retryConfigBuilder.build();

        ////////////// Configure Circuit Breaker ////////////////////

        CircuitBreakerConfig.Builder circuitBreakerConfigBuilder = CircuitBreakerConfig.custom();
        circuitBreakerConfigBuilder.failureRateThreshold(multiClusterClientConfig.getCircuitBreakerFailureRateThreshold());
        circuitBreakerConfigBuilder.slowCallRateThreshold(multiClusterClientConfig.getCircuitBreakerSlowCallRateThreshold());
        circuitBreakerConfigBuilder.slowCallDurationThreshold(multiClusterClientConfig.getCircuitBreakerSlowCallDurationThreshold());
        circuitBreakerConfigBuilder.minimumNumberOfCalls(multiClusterClientConfig.getCircuitBreakerSlidingWindowMinCalls());
        circuitBreakerConfigBuilder.slidingWindowType(multiClusterClientConfig.getCircuitBreakerSlidingWindowType());
        circuitBreakerConfigBuilder.slidingWindowSize(multiClusterClientConfig.getCircuitBreakerSlidingWindowSize());
        circuitBreakerConfigBuilder.recordExceptions(multiClusterClientConfig.getCircuitBreakerIncludedExceptionList().stream().toArray(Class[]::new));
        circuitBreakerConfigBuilder.automaticTransitionFromOpenToHalfOpenEnabled(false); // State transitions are forced. No half open states are used

        List<Class> circuitBreakerIgnoreExceptionList = multiClusterClientConfig.getCircuitBreakerIgnoreExceptionList();
        if (circuitBreakerIgnoreExceptionList != null)
            circuitBreakerConfigBuilder.ignoreExceptions(circuitBreakerIgnoreExceptionList.stream().toArray(Class[]::new));

        CircuitBreakerConfig circuitBreakerConfig = circuitBreakerConfigBuilder.build();

        ////////////// Configure Cluster Map ////////////////////

        ClusterConfig[] clusterConfigs = multiClusterClientConfig.getClusterConfigs();
        for (ClusterConfig config : clusterConfigs) {
            GenericObjectPoolConfig<Connection> poolConfig = config.getConnectionPoolConfig();

            String clusterId = "cluster:" + config.getPriority() + ":" + config.getHostAndPort();

            Retry retry = RetryRegistry.of(retryConfig).retry(clusterId);

            Retry.EventPublisher retryPublisher = retry.getEventPublisher();
            retryPublisher.onRetry(event -> log.warn(String.valueOf(event)));
            retryPublisher.onError(event -> log.error(String.valueOf(event)));

            CircuitBreaker circuitBreaker = CircuitBreakerRegistry.of(circuitBreakerConfig).circuitBreaker(clusterId);

            CircuitBreaker.EventPublisher circuitBreakerEventPublisher = circuitBreaker.getEventPublisher();
            circuitBreakerEventPublisher.onCallNotPermitted(event -> log.error(String.valueOf(event)));
            circuitBreakerEventPublisher.onError(event -> log.error(String.valueOf(event)));
            circuitBreakerEventPublisher.onFailureRateExceeded(event -> log.error(String.valueOf(event)));
            circuitBreakerEventPublisher.onSlowCallRateExceeded(event -> log.error(String.valueOf(event)));
            circuitBreakerEventPublisher.onStateTransition(event -> log.warn(String.valueOf(event)));

            if (poolConfig != null) {
                multiClusterMap.put(config.getPriority(),
                        new Cluster(new ConnectionPool(config.getHostAndPort(),
                                config.getJedisClientConfig(), poolConfig), retry, circuitBreaker));
            } else {
                multiClusterMap.put(config.getPriority(),
                        new Cluster(new ConnectionPool(config.getHostAndPort(),
                                config.getJedisClientConfig()), retry, circuitBreaker));
            }
        }

        /// --- ///

        this.fallbackExceptionList = multiClusterClientConfig.getFallbackExceptionList();
    }

    /**
     * Increments the actively used cluster/database endpoint (connection pool) amongst the pre-configured list which were
     * provided at startup via the MultiClusterClientConfig. All traffic will be routed according to this index.
     *
     * Only indexes within the pre-configured range (static) are supported otherwise an exception will be thrown.
     *
     * In the event that the next prioritized connection has a forced open state,
     * the method will recursively increment the index in order to avoid a failed command.
     */
    public int incrementActiveMultiClusterIndex() {

        // Field-level synchronization is used to avoid the edge case in which
        // setActiveMultiClusterIndex(int multiClusterIndex) is called at the same time
        synchronized (activeMultiClusterIndex) {

            String originalClusterName = getClusterCircuitBreaker().getName();

            // Only increment if it can pass this validation otherwise we will need to check for NULL in the data path
            if (activeMultiClusterIndex + 1 > multiClusterMap.size()) {

                lastClusterCircuitBreakerForcedOpen = true;

                throw new JedisConnectionException("Cluster/database endpoint could not failover since the MultiClusterClientConfig was not " +
                                                   "provided with an additional cluster/database endpoint according to its prioritized sequence. " +
                                                   "If applicable, consider failing back OR restarting with an available cluster/database endpoint");
            }
            else activeMultiClusterIndex++;

            CircuitBreaker circuitBreaker = getClusterCircuitBreaker();

            // Handles edge-case in which the user resets the activeMultiClusterIndex to a higher priority prematurely
            // which forces a failover to the next prioritized cluster that has potentially not yet recovered
            if (CircuitBreaker.State.FORCED_OPEN.equals(circuitBreaker.getState()))
                incrementActiveMultiClusterIndex();

            else log.warn("Cluster/database endpoint successfully updated from '{}' to '{}'", originalClusterName, circuitBreaker.getName());
        }

        return activeMultiClusterIndex;
    }

    /**
     * Design decision was made to defer responsibility for cross-replication validation to the user.
     *
     * Alternatively there was discussion to handle cross-cluster replication validation by
     * setting a key/value pair per hashslot in the active connection (with a TTL) and
     * subsequently reading it from the target connection.
     */
    public void validateTargetConnection(int multiClusterIndex) {

        CircuitBreaker circuitBreaker = getClusterCircuitBreaker(multiClusterIndex);

        State originalState = circuitBreaker.getState();
        try {
            // Transitions the state machine to a CLOSED state, allowing state transition, metrics and event publishing
            // Safe since the activeMultiClusterIndex has not yet been changed and therefore no traffic will be routed yet
            circuitBreaker.transitionToClosedState();

            try (Connection targetConnection = getConnection(multiClusterIndex)) {
                targetConnection.ping();
            }
        }
        catch (Exception e) {

            // If the original state was FORCED_OPEN, then transition it back which stops state transition, metrics and event publishing
            if (CircuitBreaker.State.FORCED_OPEN.equals(originalState))
                circuitBreaker.transitionToForcedOpenState();

            throw new JedisValidationException(circuitBreaker.getName() + " failed to connect. Please check configuration and try again.", e);
        }
    }

    /**
     * Manually overrides the actively used cluster/database endpoint (connection pool) amongst the
     * pre-configured list which were provided at startup via the MultiClusterClientConfig.
     * All traffic will be routed according to the provided new index.
     *
     * Special care should be taken to confirm cluster/database availability AND
     * potentially cross-cluster replication BEFORE using this capability.
     */
    public synchronized void setActiveMultiClusterIndex(int multiClusterIndex) {

        // Field-level synchronization is used to avoid the edge case in which
        // incrementActiveMultiClusterIndex() is called at the same time
        synchronized (activeMultiClusterIndex) {

            // Allows an attempt to reset the current cluster from a FORCED_OPEN to CLOSED state in the event that no failover is possible
            if (activeMultiClusterIndex == multiClusterIndex &&
                !CircuitBreaker.State.FORCED_OPEN.equals(getClusterCircuitBreaker(multiClusterIndex).getState()))
                    return;

            if (multiClusterIndex < 1 || multiClusterIndex > multiClusterMap.size())
                throw new JedisValidationException("MultiClusterIndex: " + multiClusterIndex + " is not within " +
                          "the configured range. Please choose an index between 1 and " + multiClusterMap.size());

            validateTargetConnection(multiClusterIndex);

            String originalClusterName = getClusterCircuitBreaker().getName();

            if (activeMultiClusterIndex == multiClusterIndex)
                log.warn("Cluster/database endpoint '{}' successfully closed its circuit breaker", originalClusterName);
            else
                log.warn("Cluster/database endpoint successfully updated from '{}' to '{}'",
                         originalClusterName, getClusterCircuitBreaker(multiClusterIndex).getName());

            activeMultiClusterIndex = multiClusterIndex;
            lastClusterCircuitBreakerForcedOpen = false;
        }
    }

    @Override
    public void close() {
        multiClusterMap.get(activeMultiClusterIndex).getConnectionPool().close();
    }

    @Override
    public Connection getConnection() {
        return multiClusterMap.get(activeMultiClusterIndex).getConnection();
    }

    public Connection getConnection(int multiClusterIndex) {
        return multiClusterMap.get(multiClusterIndex).getConnection();
    }

    @Override
    public Connection getConnection(CommandArguments args) {
        return multiClusterMap.get(activeMultiClusterIndex).getConnection();
    }

    @Override
    public Map<?, Pool<Connection>> getConnectionMap() {
        ConnectionPool connectionPool = multiClusterMap.get(activeMultiClusterIndex).getConnectionPool();
        return Collections.singletonMap(connectionPool.getFactory(), connectionPool);
    }

    public Cluster getCluster() {
        return multiClusterMap.get(activeMultiClusterIndex);
    }

    public CircuitBreaker getClusterCircuitBreaker() {
        return multiClusterMap.get(activeMultiClusterIndex).getCircuitBreaker();
    }

    public CircuitBreaker getClusterCircuitBreaker(int multiClusterIndex) {
        return multiClusterMap.get(multiClusterIndex).getCircuitBreaker();
    }

    public boolean isLastClusterCircuitBreakerForcedOpen() {
        return lastClusterCircuitBreakerForcedOpen;
    }

    public void runClusterFailoverPostProcessor(Integer multiClusterIndex) {
        if (clusterFailoverPostProcessor != null)
            clusterFailoverPostProcessor.accept(getClusterCircuitBreaker(multiClusterIndex).getName());
    }

    public void setClusterFailoverPostProcessor(Consumer<String> clusterFailoverPostProcessor) {
        this.clusterFailoverPostProcessor = clusterFailoverPostProcessor;
    }

    public List<Class<? extends Throwable>> getFallbackExceptionList() {
        return fallbackExceptionList;
    }

    public static class Cluster {

        private final ConnectionPool connectionPool;
        private final Retry retry;
        private final CircuitBreaker circuitBreaker;

        public Cluster(ConnectionPool connectionPool, Retry retry, CircuitBreaker circuitBreaker) {
            this.connectionPool = connectionPool;
            this.retry = retry;
            this.circuitBreaker = circuitBreaker;
        }

        public Connection getConnection() {
            return connectionPool.getResource();
        }

        public ConnectionPool getConnectionPool() {
            return connectionPool;
        }

        public Retry getRetry() {
            return retry;
        }

        public CircuitBreaker getCircuitBreaker() {
            return circuitBreaker;
        }
    }

}