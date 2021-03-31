package redis.clients.jedis;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.jedis.exceptions.JedisRedirectionException;
import redis.clients.jedis.util.JedisClusterCRC16;

public abstract class JedisClusterCommand<T> {

  private static final Logger LOG = LoggerFactory.getLogger(JedisClusterCommand.class);

  private final JedisClusterConnectionHandler connectionHandler;
  private final int maxAttempts;
  private final Duration maxTotalRetriesDuration;

  public JedisClusterCommand(JedisClusterConnectionHandler connectionHandler, int maxAttempts) {
    this(connectionHandler, maxAttempts, Duration.ofMillis((long) BinaryJedisCluster.DEFAULT_TIMEOUT * maxAttempts));
  }

  /**
   * @param connectionHandler
   * @param maxAttempts
   * @param maxTotalRetriesDuration No more attempts after we have been trying for this long.
   */
  public JedisClusterCommand(JedisClusterConnectionHandler connectionHandler, int maxAttempts,
      Duration maxTotalRetriesDuration) {
    this.connectionHandler = connectionHandler;
    this.maxAttempts = maxAttempts;
    this.maxTotalRetriesDuration = maxTotalRetriesDuration;
  }

  public abstract T execute(Jedis connection);

  public T run(String key) {
    return runWithRetries(JedisClusterCRC16.getSlot(key));
  }

  public T run(int keyCount, String... keys) {
    if (keys == null || keys.length == 0) {
      throw new JedisClusterOperationException("No way to dispatch this command to Redis Cluster.");
    }

    // For multiple keys, only execute if they all share the same connection slot.
    int slot = JedisClusterCRC16.getSlot(keys[0]);
    if (keys.length > 1) {
      for (int i = 1; i < keyCount; i++) {
        int nextSlot = JedisClusterCRC16.getSlot(keys[i]);
        if (slot != nextSlot) {
          throw new JedisClusterOperationException("No way to dispatch this command to Redis "
              + "Cluster because keys have different slots.");
        }
      }
    }

    return runWithRetries(slot);
  }

  public T runBinary(byte[] key) {
    return runWithRetries(JedisClusterCRC16.getSlot(key));
  }

  public T runBinary(int keyCount, byte[]... keys) {
    if (keys == null || keys.length == 0) {
      throw new JedisClusterOperationException("No way to dispatch this command to Redis Cluster.");
    }

    // For multiple keys, only execute if they all share the same connection slot.
    int slot = JedisClusterCRC16.getSlot(keys[0]);
    if (keys.length > 1) {
      for (int i = 1; i < keyCount; i++) {
        int nextSlot = JedisClusterCRC16.getSlot(keys[i]);
        if (slot != nextSlot) {
          throw new JedisClusterOperationException("No way to dispatch this command to Redis "
              + "Cluster because keys have different slots.");
        }
      }
    }

    return runWithRetries(slot);
  }

  public T runWithAnyNode() {
    Jedis connection = null;
    try {
      connection = connectionHandler.getConnection();
      return execute(connection);
    } finally {
      releaseConnection(connection);
    }
  }

  private T runWithRetries(final int slot) {
    Instant deadline = Instant.now().plus(maxTotalRetriesDuration);

    JedisRedirectionException redirect = null;
    int consecutiveConnectionFailures = 0;
    Exception lastException = null;
    for (int attemptsLeft = this.maxAttempts; attemptsLeft > 0; attemptsLeft--) {
      Jedis connection = null;
      try {
        if (redirect != null) {
          connection = connectionHandler.getConnectionFromNode(redirect.getTargetNode());
          if (redirect instanceof JedisAskDataException) {
            // TODO: Pipeline asking with the original command to make it faster....
            connection.asking();
          }
        } else {
          connection = connectionHandler.getConnectionFromSlot(slot);
        }

        return execute(connection);

      } catch (JedisConnectionException jce) {
        lastException = jce;
        ++consecutiveConnectionFailures;
        LOG.debug("Failed connecting to Redis: {}", connection, jce);
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
        LOG.debug("Redirected by server to {}", jre.getTargetNode());
        consecutiveConnectionFailures = 0;
        redirect = jre;
        // if MOVED redirection occurred,
        if (jre instanceof JedisMovedDataException) {
          // it rebuilds cluster's slot cache recommended by Redis cluster specification
          this.connectionHandler.renewSlotCache(connection);
        }
      } finally {
        releaseConnection(connection);
      }
      if (Instant.now().isAfter(deadline)) {
        throw new JedisClusterOperationException("Cluster retry deadline exceeded.");
      }
    }

    JedisClusterOperationException maxAttemptsException = new JedisClusterOperationException("No more cluster attempts left.");
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
    if (this.maxAttempts < 3) {
      // Since we only renew the slots cache after two consecutive connection
      // failures (see consecutiveConnectionFailures above), we need to special
      // case the situation where we max out after two or fewer attempts.
      // Otherwise, on two or fewer max attempts, the slots cache would never be
      // renewed.
      if (attemptsLeft == 0) {
        this.connectionHandler.renewSlotCache();
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
    this.connectionHandler.renewSlotCache();
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

    return millisLeft / (attemptsLeft * (attemptsLeft + 1));
  }

  protected void sleep(long sleepMillis) {
    try {
      TimeUnit.MILLISECONDS.sleep(sleepMillis);
    } catch (InterruptedException e) {
      throw new JedisClusterOperationException(e);
    }
  }

  private void releaseConnection(Jedis connection) {
    if (connection != null) {
      connection.close();
    }
  }

}
