package redis.clients.jedis.mcf;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider.Cluster;
import redis.clients.jedis.util.IOUtils;

/**
 * @author Allen Terleto (aterleto)
 *         <p>
 *         Base class for CommandExecutor with built-in retry, circuit-breaker, and failover to another cluster/database
 *         endpoint. With this executor users can seamlessly failover to Disaster Recovery (DR), Backup, and
 *         Active-Active cluster(s) by using simple configuration which is passed through from Resilience4j -
 *         https://resilience4j.readme.io/docs
 *         <p>
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
            // Check state to handle race conditions since iterateActiveCluster() is
            // non-idempotent
            if (!CircuitBreaker.State.FORCED_OPEN.equals(circuitBreaker.getState())) {

                // Transitions state machine to a FORCED_OPEN state, stopping state transition, metrics and
                // event publishing.
                // To recover/transition from this forced state the user will need to manually failback

                Cluster activeCluster = provider.getCluster();
                // This should be possible only if active cluster is switched from by other reasons than circuit
                // breaker, just before circuit breaker triggers
                if (activeCluster.getCircuitBreaker() != circuitBreaker) {
                    return;
                }

                activeCluster.setGracePeriod();
                circuitBreaker.transitionToForcedOpenState();

                // Iterating the active cluster will allow subsequent calls to the executeCommand() to use the next
                // cluster's connection pool - according to the configuration's prioritization/order/weight
                // int activeMultiClusterIndex = provider.incrementActiveMultiClusterIndex1();
                provider.iterateActiveCluster(SwitchReason.CIRCUIT_BREAKER);
            }
            // this check relies on the fact that many failover attempts can hit with the same CB,
            // only the first one will trigger a failover, and make the CB FORCED_OPEN.
            // when the rest reaches here, the active cluster is already the next one, and should be different than
            // active CB. If its the same one and there are no more clusters to failover to, then throw an exception
            else if (circuitBreaker == provider.getCluster().getCircuitBreaker() && !provider.canIterateOnceMore()) {
                throw new JedisConnectionException(
                    "Cluster/database endpoint could not failover since the MultiClusterClientConfig was not "
                        + "provided with an additional cluster/database endpoint according to its prioritized sequence. "
                        + "If applicable, consider failing back OR restarting with an available cluster/database endpoint");
            }
            // Ignore exceptions since we are already in a failure state
        } finally {
            lock.unlock();
        }
    }

}
