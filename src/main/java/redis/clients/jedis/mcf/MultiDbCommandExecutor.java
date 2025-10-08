package redis.clients.jedis.mcf;

import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.decorators.Decorators.DecorateSupplier;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.mcf.MultiDbConnectionProvider.Database;

/**
 * @author Allen Terleto (aterleto)
 *         <p>
 *         CommandExecutor with built-in retry, circuit-breaker, and failover to another
 *         cluster/database endpoint. With this executor users can seamlessly failover to Disaster
 *         Recovery (DR), Backup, and Active-Active cluster(s) by using simple configuration which
 *         is passed through from Resilience4j - https://resilience4j.readme.io/docs
 *         <p>
 */
@Experimental
public class MultiDbCommandExecutor extends MultiDbFailoverBase implements CommandExecutor {

  public MultiDbCommandExecutor(MultiDbConnectionProvider provider) {
    super(provider);
  }

  @Override
  public <T> T executeCommand(CommandObject<T> commandObject) {
    Database database = provider.getDatabase(); // Pass this by reference for thread safety

    DecorateSupplier<T> supplier = Decorators
        .ofSupplier(() -> this.handleExecuteCommand(commandObject, database));

    supplier.withCircuitBreaker(database.getCircuitBreaker());
    supplier.withRetry(database.getRetry());
    supplier.withFallback(provider.getFallbackExceptionList(),
      e -> this.handleClusterFailover(commandObject, database));
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
  private <T> T handleExecuteCommand(CommandObject<T> commandObject, Database cluster) {
    Connection connection;
    try {
      connection = cluster.getConnection();
    } catch (JedisConnectionException e) {
      provider.assertOperability();
      throw e;
    }
    try {
      return connection.executeCommand(commandObject);
    } catch (Exception e) {
      if (cluster.retryOnFailover() && !isActiveDatabase(cluster)
          && isCircuitBreakerTrackedException(e, cluster)) {
        throw new ConnectionFailoverException(
            "Command failed during failover: " + cluster.getCircuitBreaker().getName(), e);
      }
      throw e;
    } finally {
      connection.close();
    }
  }

  /**
   * Functional interface wrapped in retry and circuit breaker logic to handle open circuit breaker
   * failure scenarios
   */
  private <T> T handleClusterFailover(CommandObject<T> commandObject, Database cluster) {

    databaseFailover(cluster);

    // Recursive call to the initiating method so the operation can be retried on the next cluster
    // connection
    return executeCommand(commandObject);
  }

}