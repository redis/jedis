package redis.clients.jedis.mcf;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.mcf.MultiDbConnectionProvider.Database;
import redis.clients.jedis.util.IOUtils;

/**
 * @author Allen Terleto (aterleto)
 *         <p>
 *         Base class for CommandExecutor with built-in retry, circuit-breaker, and failover to
 *         another cluster/database endpoint. With this executor users can seamlessly failover to
 *         Disaster Recovery (DR), Backup, and Active-Active cluster(s) by using simple
 *         configuration which is passed through from Resilience4j -
 *         https://resilience4j.readme.io/docs
 *         <p>
 */
@Experimental
public class MultiDbFailoverBase implements AutoCloseable {
  private final Lock lock = new ReentrantLock(true);

  protected final MultiDbConnectionProvider provider;

  public MultiDbFailoverBase(MultiDbConnectionProvider provider) {
    this.provider = provider;
  }

  @Override
  public void close() {
    IOUtils.closeQuietly(this.provider);
  }

  /**
   * Functional interface wrapped in retry and circuit breaker logic to handle open circuit breaker
   * failure scenarios
   */
  protected void clusterFailover(Database database) {
    lock.lock();

    CircuitBreaker circuitBreaker = database.getCircuitBreaker();
    try {
      // Check state to handle race conditions since () is
      // non-idempotent
      if (!CircuitBreaker.State.FORCED_OPEN.equals(circuitBreaker.getState())) {

        // Transitions state machine to a FORCED_OPEN state, stopping state transition, metrics and
        // event publishing.
        // To recover/transition from this forced state the user will need to manually failback

        Database activeDatabase = provider.getDatabase();
        // This should be possible only if active database is switched from by other reasons than
        // circuit breaker, just before circuit breaker triggers
        if (activeDatabase != database) {
          return;
        }

        database.setGracePeriod();
        circuitBreaker.transitionToForcedOpenState();

        // Iterating the active database will allow subsequent calls to the executeCommand() to use
        // the next
        // database's connection pool - according to the configuration's prioritization/order/weight
        provider.switchToHealthyDatabase(SwitchReason.CIRCUIT_BREAKER, database);
      }
      // this check relies on the fact that many failover attempts can hit with the same CB,
      // only the first one will trigger a failover, and make the CB FORCED_OPEN.
      // when the rest reaches here, the active database is already the next one, and should be
      // different than
      // active CB. If its the same one and there are no more clusters to failover to, then throw an
      // exception
      else if (database == provider.getDatabase()) {
        provider.switchToHealthyDatabase(SwitchReason.CIRCUIT_BREAKER, database);
      }
      // Ignore exceptions since we are already in a failure state
    } finally {
      lock.unlock();
    }
  }

  boolean isActiveDatabase(Database database) {
    Database activeDatabase = provider.getDatabase();
    return activeDatabase != null && activeDatabase.equals(database);
  }

  static boolean isCircuitBreakerTrackedException(Exception e, Database database) {
    return database.getCircuitBreaker().getCircuitBreakerConfig().getRecordExceptionPredicate()
        .test(e);
  }
}
