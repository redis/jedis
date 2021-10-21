package redis.clients.jedis;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.providers.JedisConnectionProvider;
import redis.clients.jedis.util.IOUtils;

public class RetryableCommandExecutor implements JedisCommandExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(RetryableCommandExecutor.class);

  private final JedisConnectionProvider provider;
  private final int maxAttempts;
  private final Duration maxTotalRetriesDuration;

  public RetryableCommandExecutor(JedisConnectionProvider provider, int maxAttempts,
      Duration maxTotalRetriesDuration) {
    this.provider = provider;
    this.maxAttempts = maxAttempts;
    this.maxTotalRetriesDuration = maxTotalRetriesDuration;
  }

  @Override
  public void close() {
    IOUtils.closeQuietly(this.provider);
  }

  @Override
  public final <T> T executeCommand(CommandObject<T> commandObject) {

    Instant deadline = Instant.now().plus(maxTotalRetriesDuration);

    int consecutiveConnectionFailures = 0;
    JedisException lastException = null;
    for (int attemptsLeft = this.maxAttempts; attemptsLeft > 0; attemptsLeft--) {
      Connection connection = null;
      try {
        connection = provider.getConnection(commandObject.getArguments());

        return connection.executeCommand(commandObject);

      } catch (JedisConnectionException jce) {
        lastException = jce;
        ++consecutiveConnectionFailures;
        LOG.debug("Failed connecting to Redis: {}", connection, jce);
        // "- 1" because we just did one, but the attemptsLeft counter hasn't been decremented yet
        boolean reset = handleConnectionProblem(attemptsLeft - 1, consecutiveConnectionFailures, deadline);
        if (reset) {
          consecutiveConnectionFailures = 0;
        }
      } finally {
        if (connection != null) {
          connection.close();
        }
      }
      if (Instant.now().isAfter(deadline)) {
        throw new JedisException("Cluster retry deadline exceeded.");
      }
    }

    JedisException maxAttemptsException = new JedisException("No more cluster attempts left.");
    maxAttemptsException.addSuppressed(lastException);
    throw maxAttemptsException;
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
      throw new JedisException("Cluster retry deadline exceeded.");
    }

    return millisLeft / (attemptsLeft * (attemptsLeft + 1));
  }

  protected void sleep(long sleepMillis) {
    try {
      TimeUnit.MILLISECONDS.sleep(sleepMillis);
    } catch (InterruptedException e) {
      throw new JedisException(e);
    }
  }
}
