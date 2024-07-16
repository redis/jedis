package redis.clients.jedis.mcf;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;
import redis.clients.jedis.util.IOUtils;

/**
 * @author Allen Terleto (aterleto)
 * <p>
 * Base class for CommandExecutor with built-in retry, circuit-breaker, and failover to another cluster/database
 * endpoint. With this executor users can seamlessly failover to Disaster Recovery (DR), Backup, and Active-Active
 * cluster(s) by using simple configuration which is passed through from
 * Resilience4j - https://resilience4j.readme.io/docs
 * <p>
 */
@Experimental
public class CircuitBreakerFailoverBase implements AutoCloseable {
    private final Lock lock = new ReentrantLock(true);

    protected final MultiClusterPooledConnectionProvider provider;

    public CircuitBreakerFailoverBase(MultiClusterPooledConnectionProvider provider) {
        this.provider = provider;
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(this.provider);
    }

    /**
     * Functional interface wrapped in retry and circuit breaker logic to handle open circuit breaker failure scenarios
     */
    protected void clusterFailover(CircuitBreaker circuitBreaker) {
        lock.lock();
        
        try {
            // Check state to handle race conditions since incrementActiveMultiClusterIndex() is non-idempotent
            if (!CircuitBreaker.State.FORCED_OPEN.equals(circuitBreaker.getState())) {

                // Transitions state machine to a FORCED_OPEN state, stopping state transition, metrics and event publishing.
                // To recover/transition from this forced state the user will need to manually failback
                circuitBreaker.transitionToForcedOpenState();

                // Incrementing the activeMultiClusterIndex will allow subsequent calls to the executeCommand()
                // to use the next cluster's connection pool - according to the configuration's prioritization/order
                int activeMultiClusterIndex = provider.incrementActiveMultiClusterIndex();

                // Implementation is optionally provided during configuration. Typically, used for activeMultiClusterIndex persistence or custom logging
                provider.runClusterFailoverPostProcessor(activeMultiClusterIndex);
            }

            // Once the priority list is exhausted only a manual failback can open the circuit breaker so all subsequent operations will fail
            else if (provider.isLastClusterCircuitBreakerForcedOpen()) {
                throw new JedisConnectionException("Cluster/database endpoint could not failover since the MultiClusterClientConfig was not " +
                                                   "provided with an additional cluster/database endpoint according to its prioritized sequence. " +
                                                   "If applicable, consider failing back OR restarting with an available cluster/database endpoint");
            }
        } finally {
            lock.unlock();
        }
    }

}
