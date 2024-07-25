package redis.clients.jedis.executors;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.annots.VisibleForTesting;
import redis.clients.jedis.csc.ClientSideCache;
import redis.clients.jedis.csc.ClientSideCacheCommandExecutorHelper;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.providers.ConnectionProvider;

public class RetryableCommandExecutor implements CommandExecutor {

  private final Logger log = LoggerFactory.getLogger(getClass());

  protected final ConnectionProvider provider;
  protected final int maxAttempts;
  protected final Duration maxTotalRetriesDuration;

  private final ClientSideCacheCommandExecutorHelper cache;

  public RetryableCommandExecutor(ConnectionProvider provider, int maxAttempts,
      Duration maxTotalRetriesDuration) {
    this(provider, maxAttempts, maxTotalRetriesDuration, (ClientSideCache) null);
  }

  public RetryableCommandExecutor(ConnectionProvider provider, int maxAttempts,
      Duration maxTotalRetriesDuration, ClientSideCache cache) {
    this.provider = provider;
    this.maxAttempts = maxAttempts;
    this.maxTotalRetriesDuration = maxTotalRetriesDuration;
    this.cache = new ClientSideCacheCommandExecutorHelper(cache);
  }

  @Override
  public void close() {
    IOUtils.closeQuietly(this.provider);
  }

  @Override
  public final <T> T executeCommand(CommandObject<T> commandObject) {
    return executeCommand(commandObject, null);
  }

  @Override
  public final <T> T executeCommand(CommandObject<T> commandObject, Supplier<Object[]> keys) {

    Instant deadline = Instant.now().plus(maxTotalRetriesDuration);

    int consecutiveConnectionFailures = 0;
    JedisException lastException = null;
    for (int attemptsLeft = this.maxAttempts; attemptsLeft > 0; attemptsLeft--) {
      Connection connection = null;
      try {
        connection = provider.getConnection(commandObject.getArguments());

        return execute(connection, commandObject, keys);

      } catch (JedisConnectionException jce) {
        lastException = jce;
        ++consecutiveConnectionFailures;
        log.debug("Failed connecting to Redis: {}", connection, jce);
        // "- 1" because we just did one, but the attemptsLeft counter hasn't been decremented yet
        boolean reset = handleConnectionProblem(attemptsLeft - 1, consecutiveConnectionFailures, deadline);
        if (reset) {
          consecutiveConnectionFailures = 0;
        }
      } finally {
        IOUtils.closeQuietly(connection);
      }
      if (Instant.now().isAfter(deadline)) {
        throw new JedisException("Retry deadline exceeded.");
      }
    }

    JedisException maxAttemptsException = new JedisException("No more attempts left.");
    maxAttemptsException.addSuppressed(lastException);
    throw maxAttemptsException;
  }

  /**
   * WARNING: This method is accessible for the purpose of testing.
   * This should not be used or overriden.
   */
  @VisibleForTesting
  protected <T> T execute(Connection connection, CommandObject<T> commandObject, Supplier<Object[]> keys) {
    if (this.cache != null && keys != null) {
      return this.cache.get(connection, commandObject, (Object[]) keys.get());
    } else {
      return connection.executeCommand(commandObject);
    }
  }

  /**
   * Related values should be reset if <code>TRUE</code> is returned.
   *
   * @param attemptsLeft
   * @param consecutiveConnectionFailures
   * @param doneDeadline
   * @return true - if some actions are taken
   * <br /> false - if no actions are taken
   */
  private boolean handleConnectionProblem(int attemptsLeft, int consecutiveConnectionFailures, Instant doneDeadline) {

    if (consecutiveConnectionFailures < 2) {
      return false;
    }

    sleep(getBackoffSleepMillis(attemptsLeft, doneDeadline));
    return true;
  }

  private static long getBackoffSleepMillis(int attemptsLeft, Instant deadline) {
    if (attemptsLeft <= 0) {
      return 0;
    }

    long millisLeft = Duration.between(Instant.now(), deadline).toMillis();
    if (millisLeft < 0) {
      throw new JedisException("Retry deadline exceeded.");
    }

    return millisLeft / (attemptsLeft * (attemptsLeft + 1));
  }

  /**
   * WARNING: This method is accessible for the purpose of testing.
   * This should not be used or overriden.
   */
  @VisibleForTesting
  protected void sleep(long sleepMillis) {
    try {
      TimeUnit.MILLISECONDS.sleep(sleepMillis);
    } catch (InterruptedException e) {
      throw new JedisException(e);
    }
  }
}
