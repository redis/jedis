package redis.clients.jedis.executors;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.decorators.Decorators.DecorateSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;
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

    private final Logger log = LoggerFactory.getLogger(getClass());

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
        DecorateSupplier<T> supplier = Decorators.ofSupplier(() -> this.handleExecuteCommand(commandObject));

        supplier.withRetry(provider.getClusterRetry());
        supplier.withCircuitBreaker(provider.getClusterCircuitBreaker());
        supplier.withFallback(circuitBreakerFallbackException, e -> this.handleClusterFailover(commandObject));

        return supplier.decorate().get();
    }

    /**
     * Functional interface wrapped in retry and circuit breaker logic to handle happy path scenarios
     */
    private <T> T handleExecuteCommand(CommandObject<T> commandObject) {
        Connection connection = null;
        try {
            connection = provider.getConnection(commandObject.getArguments());

            if (log.isDebugEnabled())
                log.debug("{} processed handleExecuteCommand(commandObject)", provider.getClusterRetry().getName());

            return connection.executeCommand(commandObject);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    /**
     * Functional interface wrapped in retry and circuit breaker logic to handle open circuit breaker failure scenarios
     */
    private synchronized <T> T handleClusterFailover(CommandObject<T> commandObject) {

        // Check state to handle race conditions since incrementActiveMultiClusterIndex() is non-idempotent
        if (!CircuitBreaker.State.FORCED_OPEN.equals(provider.getClusterCircuitBreaker().getState())) {

            // Transitions state machine to a FORCED_OPEN state, stopping state transition, metrics and event publishing.
            // To recover/transition from this forced state the user will need to manually failback
            provider.getClusterCircuitBreaker().transitionToForcedOpenState();

            String originalCluster = provider.getClusterCircuitBreaker().getName();

            // Incrementing the activeMultiClusterIndex will allow subsequent calls to executeCommand
            // use the next cluster connection according to the configuration's prioritization/order
            provider.incrementActiveMultiClusterIndex();

            log.warn("CircuitBreaker failed over connection pool from '{}' to '{}'",
                     originalCluster, provider.getClusterCircuitBreaker().getName());
        }

        // Recursive call to the initiating method so the operation can be retried on the next cluster connection
        return executeCommand(commandObject);
    }

}