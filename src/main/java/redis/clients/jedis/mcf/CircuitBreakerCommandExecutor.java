package redis.clients.jedis.mcf;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.decorators.Decorators.DecorateSupplier;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider.Cluster;

/**
 * @author Allen Terleto (aterleto)
 * <p>
 * CommandExecutor with built-in retry, circuit-breaker, and failover to another cluster/database endpoint.
 * With this executor users can seamlessly failover to Disaster Recovery (DR), Backup, and Active-Active cluster(s)
 * by using simple configuration which is passed through from Resilience4j - https://resilience4j.readme.io/docs
 * <p>
 */
@Experimental
public class CircuitBreakerCommandExecutor extends CircuitBreakerFailoverBase implements CommandExecutor {

    public CircuitBreakerCommandExecutor(MultiClusterPooledConnectionProvider provider) {
        super(provider);
    }

    @Override
    public <T> T executeCommand(CommandObject<T> commandObject) {
        Cluster cluster = provider.getCluster(); // Pass this by reference for thread safety

        DecorateSupplier<T> supplier = Decorators.ofSupplier(() -> this.handleExecuteCommand(commandObject, cluster));

        supplier.withRetry(cluster.getRetry());
        supplier.withCircuitBreaker(cluster.getCircuitBreaker());
        supplier.withFallback(provider.getFallbackExceptionList(),
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
    private <T> T handleClusterFailover(CommandObject<T> commandObject, CircuitBreaker circuitBreaker) {

        clusterFailover(circuitBreaker);

        // Recursive call to the initiating method so the operation can be retried on the next cluster connection
        return executeCommand(commandObject);
    }

}