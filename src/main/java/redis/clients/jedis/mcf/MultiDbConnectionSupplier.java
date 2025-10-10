package redis.clients.jedis.mcf;

import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.decorators.Decorators.DecorateSupplier;

import redis.clients.jedis.Connection;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.mcf.MultiDbConnectionProvider.Database;

/**
 * ConnectionProvider with built-in retry, circuit-breaker, and failover to another /database
 * endpoint. With this executor users can seamlessly failover to Disaster Recovery (DR), Backup, and
 * Active-Active cluster(s) by using simple configuration
 */
@Experimental
public class MultiDbConnectionSupplier extends MultiDbFailoverBase {

  public MultiDbConnectionSupplier(MultiDbConnectionProvider provider) {
    super(provider);
  }

  public Connection getConnection() {
    Database database = provider.getDatabase(); // Pass this by reference for thread safety

    DecorateSupplier<Connection> supplier = Decorators
        .ofSupplier(() -> this.handleGetConnection(database));

    supplier.withRetry(database.getRetry());
    supplier.withCircuitBreaker(database.getCircuitBreaker());
    supplier.withFallback(provider.getFallbackExceptionList(),
      e -> this.handleDatabaseFailover(database));

    try {
      return supplier.decorate().get();
    } catch (Exception e) {
      if (database.getCircuitBreaker().getState() == State.OPEN && isActiveDatabase(database)) {
        databaseFailover(database);
      }
      throw e;
    }
  }

  /**
   * Functional interface wrapped in retry and circuit breaker logic to handle happy path scenarios
   */
  private Connection handleGetConnection(Database database) {
    Connection connection = database.getConnection();
    connection.ping();
    return connection;
  }

  /**
   * Functional interface wrapped in retry and circuit breaker logic to handle open circuit breaker
   * failure scenarios
   */
  private Connection handleDatabaseFailover(Database database) {

    databaseFailover(database);

    // Recursive call to the initiating method so the operation can be retried on the next database
    // connection
    return getConnection();
  }

}