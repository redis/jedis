package redis.clients.jedis.executors;

import java.time.Duration;
import java.time.Instant;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.util.JedisAsserts;

public class DefaultCommandExecutor extends ResilientCommandExecutor {

  protected final ConnectionProvider provider;

  public DefaultCommandExecutor(ConnectionProvider provider) {
    this(provider, 1, Duration.ZERO);
  }

  public DefaultCommandExecutor(ConnectionProvider provider, int maxAttempts,
      Duration maxTotalRetriesDuration) {
    super(maxAttempts, maxTotalRetriesDuration);
    JedisAsserts.notNull(provider, "provider must not be null");
    this.provider = provider;
  }

  @Override
  public void close() {
    IOUtils.closeQuietly(this.provider);
  }

  @Override
  public final <T> T executeCommand(CommandObject<T> commandObject) {
    // Fast path: no retries configured — let exceptions propagate directly
    if (maxAttempts <= 1) {
      try (Connection connection = provider.getConnection(commandObject.getArguments())) {
        return connection.executeCommand(commandObject);
      }
    }

    return executeWithRetries(commandObject);
  }

  private <T> T executeWithRetries(CommandObject<T> commandObject) {
    Instant deadline = Instant.now().plus(maxTotalRetriesDuration);
    JedisConnectionException lastException = null;

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try (Connection connection = provider.getConnection(commandObject.getArguments())) {
        return connection.executeCommand(commandObject);
      } catch (JedisConnectionException jce) {
        lastException = jce;
        log.debug("Failed connecting to Redis (attempt {}/{})", attempt, maxAttempts, jce);

        if (attempt < maxAttempts) {
          if (Instant.now().isAfter(deadline)) {
            break;
          }
          sleep(computeBackoffMillis(maxAttempts - attempt, deadline));
        }
      }
    }

    throw buildRetryExhaustedException(lastException, deadline);
  }

  private JedisException buildRetryExhaustedException(JedisConnectionException lastException,
      Instant deadline) {
    String message = Instant.now().isAfter(deadline)
        ? "Retry deadline exceeded."
        : "No more attempts left.";
    JedisException ex = new JedisException(message);
    if (lastException != null) {
      ex.addSuppressed(lastException);
    }
    return ex;
  }
}
