package redis.clients.jedis.executors;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.annots.VisibleForTesting;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.exceptions.JedisRedirectionException;
import redis.clients.jedis.providers.RedirectConnectionProvider;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.util.JedisAsserts;

public class RedirectCommandExecutor implements CommandExecutor {

  private final Logger log = LoggerFactory.getLogger(getClass());

  public final RedirectConnectionProvider provider;
  protected final int maxAttempts;
  protected final Duration maxTotalRetriesDuration;

  public RedirectCommandExecutor(RedirectConnectionProvider provider, int maxAttempts,
      Duration maxTotalRetriesDuration) {
    JedisAsserts.notNull(provider, "provider must not be null");
    JedisAsserts.isTrue(maxAttempts > 0, "maxAttempts must be greater than 0");
    JedisAsserts.notNull(maxTotalRetriesDuration, "maxTotalRetriesDuration must not be null");

    this.provider = provider;
    this.maxAttempts = maxAttempts;
    this.maxTotalRetriesDuration = maxTotalRetriesDuration;
  }

  @Override
  public void close() {
    this.provider.close();
  }

  @Override
  public final <T> T executeCommand(CommandObject<T> commandObject) {
    Instant deadline = Instant.now().plus(maxTotalRetriesDuration);

    int consecutiveConnectionFailures = 0;
    Exception lastException = null;
    for (int attemptsLeft = this.maxAttempts; attemptsLeft > 0; attemptsLeft--) {
      Connection connection = null;
      try {
        connection = provider.getConnection();
        return execute(connection, commandObject);

      } catch (JedisConnectionException jce) {
        lastException = jce;
        ++consecutiveConnectionFailures;
        log.debug("Failed connecting to Redis: {}", connection, jce);
        boolean reset = handleConnectionProblem(attemptsLeft - 1, consecutiveConnectionFailures,
          deadline);
        if (reset) {
          consecutiveConnectionFailures = 0;
        }
      } catch (JedisRedirectionException jre) {
        if (lastException == null || lastException instanceof JedisRedirectionException) {
          lastException = jre;
        }
        log.debug("Redirected by server to {}", jre.getTargetNode());
        consecutiveConnectionFailures = 0;
        provider.renewPool(connection, jre.getTargetNode());
      } finally {
        IOUtils.closeQuietly(connection);
      }
      if (Instant.now().isAfter(deadline)) {
        throw new JedisException("Redirect retry deadline exceeded.");
      }
    }

    JedisException maxRedirectException = new JedisException("No more redirect attempts left.");
    if (lastException != null) {
      maxRedirectException.addSuppressed(lastException);
    }
    throw maxRedirectException;
  }

  @VisibleForTesting
  protected <T> T execute(Connection connection, CommandObject<T> commandObject) {
    return connection.executeCommand(commandObject);
  }

  private boolean handleConnectionProblem(int attemptsLeft, int consecutiveConnectionFailures,
      Instant doneDeadline) {
    if (this.maxAttempts < 3) {
      if (attemptsLeft == 0) {
        provider.renewPool(null, null);
        return true;
      }
      return false;
    }

    if (consecutiveConnectionFailures < 2) {
      return false;
    }

    sleep(getBackoffSleepMillis(attemptsLeft, doneDeadline));
    provider.renewPool(null, null);
    return true;
  }

  private static long getBackoffSleepMillis(int attemptsLeft, Instant deadline) {
    if (attemptsLeft <= 0) {
      return 0;
    }

    long millisLeft = Duration.between(Instant.now(), deadline).toMillis();
    if (millisLeft < 0) {
      throw new JedisException("Redirect retry deadline exceeded.");
    }

    long maxBackOff = millisLeft / (attemptsLeft * attemptsLeft);
    return ThreadLocalRandom.current().nextLong(maxBackOff + 1);
  }

  @VisibleForTesting
  protected void sleep(long sleepMillis) {
    try {
      TimeUnit.MILLISECONDS.sleep(sleepMillis);
    } catch (InterruptedException e) {
      throw new JedisException(e);
    }
  }
}
