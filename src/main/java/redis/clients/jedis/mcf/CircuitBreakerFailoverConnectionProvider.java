package redis.clients.jedis.mcf;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.decorators.Decorators.DecorateSupplier;

import redis.clients.jedis.Connection;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider.Cluster;

/**
 * ConnectionProvider with built-in retry, circuit-breaker, and failover to another cluster/database endpoint.
 * With this executor users can seamlessly failover to Disaster Recovery (DR), Backup, and Active-Active cluster(s)
 * by using simple configuration which is passed through from Resilience4j - https://resilience4j.readme.io/docs
 */
@Experimental
public class CircuitBreakerFailoverConnectionProvider extends CircuitBreakerFailoverBase {

    public CircuitBreakerFailoverConnectionProvider(MultiClusterPooledConnectionProvider provider) {
        super(provider);
    }

    public Connection getConnection() {
        Cluster cluster = provider.getCluster(); // Pass this by reference for thread safety

        DecorateSupplier<Connection> supplier = Decorators.ofSupplier(() -> this.handleGetConnection(cluster));

        supplier.withRetry(cluster.getRetry());
        supplier.withCircuitBreaker(cluster.getCircuitBreaker());
        supplier.withFallback(provider.getFallbackExceptionList(),
                e -> this.handleClusterFailover(cluster.getCircuitBreaker()));

        return supplier.decorate().get();
    }

    /**
     * Functional interface wrapped in retry and circuit breaker logic to handle happy path scenarios
     */
    private Connection handleGetConnection(Cluster cluster) {
        Connection connection = cluster.getConnection();
        connection.ping();
        return connection;
    }

    /**
     * Functional interface wrapped in retry and circuit breaker logic to handle open circuit breaker failure scenarios
     */
    private Connection handleClusterFailover(CircuitBreaker circuitBreaker) {

        clusterFailover(circuitBreaker);

        // Recursive call to the initiating method so the operation can be retried on the next cluster connection
        return getConnection();
    }

}