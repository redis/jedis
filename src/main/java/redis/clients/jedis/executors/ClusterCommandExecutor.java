package redis.clients.jedis.executors;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.*;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.util.IOUtils;

public class ClusterCommandExecutor implements CommandExecutor {

  private final Logger log = LoggerFactory.getLogger(getClass());

  public final ClusterConnectionProvider provider;
  protected final int maxAttempts;
  protected final Duration maxTotalRetriesDuration;

  public ClusterCommandExecutor(ClusterConnectionProvider provider, int maxAttempts,
      Duration maxTotalRetriesDuration) {
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

    JedisRedirectionException redirect = null;
    int consecutiveConnectionFailures = 0;
    Exception lastException = null;
    for (int attemptsLeft = this.maxAttempts; attemptsLeft > 0; attemptsLeft--) {
      Connection connection = null;
      try {
        if (redirect != null) {
          connection = provider.getConnection(redirect.getTargetNode());
          if (redirect instanceof JedisAskDataException) {
            // TODO: Pipeline asking with the original command to make it faster....
            connection.executeCommand(Protocol.Command.ASKING);
          }
        } else {
          connection = provider.getConnection(commandObject.getArguments());
        }

        return execute(connection, commandObject);

      } catch (JedisClusterOperationException jnrcne) {
        throw jnrcne;
      } catch (JedisConnectionException jce) {
        lastException = jce;
        ++consecutiveConnectionFailures;
        log.debug("Failed connecting to Redis: {}", connection, jce);
        // "- 1" because we just did one, but the attemptsLeft counter hasn't been decremented yet
        boolean reset = handleConnectionProblem(attemptsLeft - 1, consecutiveConnectionFailures, deadline);
        if (reset) {
          consecutiveConnectionFailures = 0;
          redirect = null;
        }
      } catch (JedisRedirectionException jre) {
        // avoid updating lastException if it is a connection exception
        if (lastException == null || lastException instanceof JedisRedirectionException) {
          lastException = jre;
        }
        log.debug("Redirected by server to {}", jre.getTargetNode());
        consecutiveConnectionFailures = 0;
        redirect = jre;
        // if MOVED redirection occurred,
        if (jre instanceof JedisMovedDataException) {
          // it rebuilds cluster's slot cache recommended by Redis cluster specification
          provider.renewSlotCache(connection);
        }
      } finally {
        IOUtils.closeQuietly(connection);
      }
      if (Instant.now().isAfter(deadline)) {
        throw new JedisClusterOperationException("Cluster retry deadline exceeded.");
      }
    }

    JedisClusterOperationException maxAttemptsException
        = new JedisClusterOperationException("No more cluster attempts left.");
    maxAttemptsException.addSuppressed(lastException);
    throw maxAttemptsException;
  }

  /**
   * WARNING: This method is accessible for the purpose of testing.
   * This should not be used or overriden.
   */
  protected <T> T execute(Connection connection, CommandObject<T> commandObject) {
    return connection.executeCommand(commandObject);
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
    if (this.maxAttempts < 3) {
      // Since we only renew the slots cache after two consecutive connection
      // failures (see consecutiveConnectionFailures above), we need to special
      // case the situation where we max out after two or fewer attempts.
      // Otherwise, on two or fewer max attempts, the slots cache would never be
      // renewed.
      if (attemptsLeft == 0) {
        provider.renewSlotCache();
        return true;
      }
      return false;
    }

    if (consecutiveConnectionFailures < 2) {
      return false;
    }

    sleep(getBackoffSleepMillis(attemptsLeft, doneDeadline));
    //We need this because if node is not reachable anymore - we need to finally initiate slots
    //renewing, or we can stuck with cluster state without one node in opposite case.
    //TODO make tracking of successful/unsuccessful operations for node - do renewing only
    //if there were no successful responses from this node last few seconds
    provider.renewSlotCache();
    return true;
  }

  private static long getBackoffSleepMillis(int attemptsLeft, Instant deadline) {
    if (attemptsLeft <= 0) {
      return 0;
    }

    long millisLeft = Duration.between(Instant.now(), deadline).toMillis();
    if (millisLeft < 0) {
      throw new JedisClusterOperationException("Cluster retry deadline exceeded.");
    }

    long maxBackOff = millisLeft / (attemptsLeft * attemptsLeft);
    return ThreadLocalRandom.current().nextLong(maxBackOff + 1);
  }

  /**
   * WARNING: This method is accessible for the purpose of testing.
   * This should not be used or overriden.
   */
  protected void sleep(long sleepMillis) {
    try {
      TimeUnit.MILLISECONDS.sleep(sleepMillis);
    } catch (InterruptedException e) {
      throw new JedisClusterOperationException(e);
    }
  }
}
