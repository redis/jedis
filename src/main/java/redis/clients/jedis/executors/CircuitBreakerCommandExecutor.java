package redis.clients.jedis.executors;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.decorators.Decorators.DecorateSupplier;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider.Cluster;
import redis.clients.jedis.util.IOUtils;

import java.util.Arrays;
import java.util.List;


/**
 * @author Allen Terleto (aterleto)
 * <p>
 * CommandExecutor with built-in retry, circuit-breaker, and failover to another cluster/database endpoint.
 * With this executor users can seamlessly failover to Disaster Recovery (DR), Backup, and Active-Active cluster(s)
 * by using simple configuration which is passed through from Resilience4j - https://resilience4j.readme.io/docs
 * <p>
 */
public class CircuitBreakerCommandExecutor implements CommandExecutor {

    private final static List<Class<? extends Throwable>> circuitBreakerFallbackException =
                                                          Arrays.asList(CallNotPermittedException.class);

    private final MultiClusterPooledConnectionProvider provider;

    public CircuitBreakerCommandExecutor(MultiClusterPooledConnectionProvider provider) {
        this.provider = provider;
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(this.provider);
    }

    @Override
    public <T> T executeCommand(CommandObject<T> commandObject) {
        Cluster cluster = provider.getCluster(); // Pass this by reference for thread safety

        DecorateSupplier<T> supplier = Decorators.ofSupplier(() -> this.handleExecuteCommand(commandObject, cluster));

        supplier.withRetry(cluster.getRetry());
        supplier.withCircuitBreaker(cluster.getCircuitBreaker());
        supplier.withFallback(circuitBreakerFallbackException,
                              e -> this.handleClusterFailover(commandObject, cluster.getCircuitBreaker()));

        return supplier.decorate().get();
    }

    /**
     * Functional interface wrapped in retry and circuit breaker logic to handle happy path scenarios
     */
    private <T> T handleExecuteCommand(CommandObject<T> commandObject, Cluster cluster) {
        try (Connection connection = cluster.getConnection()) {
            return connection.executeCommand(commandObject);
        }
    }

    /**
     * Functional interface wrapped in retry and circuit breaker logic to handle open circuit breaker failure scenarios
     */
    private synchronized <T> T handleClusterFailover(CommandObject<T> commandObject, CircuitBreaker circuitBreaker) {

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

        // Recursive call to the initiating method so the operation can be retried on the next cluster connection
        return executeCommand(commandObject);
    }

}