package redis.clients.jedis.mcf;

import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.decorators.Decorators.DecorateSupplier;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.mcf.MultiDbConnectionProvider.Database;

/**
 * @author Allen Terleto (aterleto)
 *         <p>
 *         CommandExecutor with built-in retry, circuit-breaker, and failover to another database
 *         endpoint. With this executor users can seamlessly failover to Disaster Recovery (DR),
 *         Backup, and Active-Active cluster(s) by using simple configuration which is passed
 *         through from Resilience4j - https://resilience4j.readme.io/docs
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
      e -> this.handleDatabaseFailover(commandObject, database));
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
   * Executes a command with retry and circuit breaker logic for happy path scenarios.
   */
  private <T> T handleExecuteCommand(CommandObject<T> commandObject, Database database) {
    Connection connection = acquireConnection(database);
    Exception commandException = null;

    try {
      return connection.executeCommand(commandObject);
    } catch (Exception e) {
      commandException = wrapIfFailover(e, database);
      // this throw below does not propogate, it is just a placeholder for the compiler.
      // commandException will be rethrown in finally block.
      // see closeConnectionAndRethrow() for more details
      throw e;
    } finally {
      closeConnectionAndRethrow(connection, commandException);
    }
  }

  private Connection acquireConnection(Database database) {
    try {
      return database.getConnection();
    } catch (JedisConnectionException e) {
      provider.assertOperability();
      throw e;
    }
  }

  /**
   * Returns a {@link ConnectionFailoverException} if the exception occurred during an active
   * failover, otherwise returns the original exception unchanged.
   */
  private Exception wrapIfFailover(Exception e, Database database) {
    if (isFailDuringFailover(e, database)) {
      return new ConnectionFailoverException(
          "Command failed during failover: " + database.getCircuitBreaker().getName(), e);
    }
    return e;
  }

  private boolean isFailDuringFailover(Exception e, Database database) {
    return database.retryOnFailover() && !isActiveDatabase(database)
        && isCircuitBreakerTrackedException(e, database);
  }

  /**
   * Closes the connection, suppressing any close exception onto the command exception if present.
   * IMPORTANTNOTE: We capture and rethrow {@code commandException} here rather than letting the
   * original throw propagate, because {@code connection.close()} (via commons-pool 2.13.1+) may
   * itself throw when attempting to replace an invalidated connection. Suppressing the close
   * exception onto the command exception preserves the root cause. See:
   * https://github.com/apache/commons-pool/commit/32fd7010d9cf9e789cbba8a51c57b58edc46bcd3
   * https://issues.apache.org/jira/projects/POOL/issues/POOL-424
   */
  private void closeConnectionAndRethrow(Connection connection, Exception commandException) {
    try {
      connection.close();
    } catch (Exception closeException) {
      if (commandException != null) {
        commandException.addSuppressed(closeException);
      } else {
        throw closeException;
      }
    }

    if (commandException instanceof RuntimeException) {
      throw (RuntimeException) commandException;
    }
    if (commandException != null) {
      throw new JedisException(commandException);
    }
  }

  /**
   * Functional interface wrapped in retry and circuit breaker logic to handle open circuit breaker
   * failure scenarios
   */
  private <T> T handleDatabaseFailover(CommandObject<T> commandObject, Database database) {

    databaseFailover(database);

    // Recursive call to the initiating method so the operation can be retried on the next database
    // connection
    return executeCommand(commandObject);
  }

}