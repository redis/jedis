package redis.clients.jedis;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisClusterMaxAttemptsException;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.jedis.exceptions.JedisNoReachableClusterNodeException;
import redis.clients.jedis.exceptions.JedisRedirectionException;
import redis.clients.jedis.util.JedisClusterCRC16;

public abstract class JedisClusterCommand<T> {

  private static final Logger LOG = LoggerFactory.getLogger(JedisClusterCommand.class);

  private final JedisClusterConnectionHandler connectionHandler;
  private final int maxAttempts;
  private final Duration timeout;

  public JedisClusterCommand(JedisClusterConnectionHandler connectionHandler, int maxAttempts,
      Duration timeout) {
    this.connectionHandler = connectionHandler;
    this.maxAttempts = maxAttempts;
    this.timeout = timeout;
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

  private boolean shouldBackOff(int attemptsLeft) {
    int attemptsDone = maxAttempts - attemptsLeft;
    return attemptsDone >= maxAttempts / 3;
  }

  private static long getBackoffSleepMillis(int attemptsLeft, Instant deadline) {
    if (attemptsLeft <= 0) {
      return 0;
    }

    long millisLeft = Duration.between(Instant.now(), deadline).toMillis();
    if (millisLeft < 0) {
      throw new JedisClusterMaxAttemptsException("Deadline exceeded");
    }

    return millisLeft / (attemptsLeft * attemptsLeft);
  }

  private T runWithRetries(final int slot) {
    Instant deadline = Instant.now().plus(timeout);
    Supplier<Jedis> connectionSupplier = () -> connectionHandler.getConnectionFromSlot(slot);

    // If we got one redirection, stick with that and don't try anything else
    Supplier<Jedis> redirectionSupplier = null;

    for (int currentAttempt = 0; currentAttempt < this.maxAttempts; currentAttempt++) {
      Jedis connection = null;
      try {
        if (redirectionSupplier != null) {
          connection = redirectionSupplier.get();
        } else {
          connection = connectionSupplier.get();
        }

        if (shouldBackOff(maxAttempts - currentAttempt)) {
          // Don't just stick to this any more, start asking around
          redirectionSupplier = null;
        }

        final T result = execute(connection);
        if (currentAttempt > 0) {
          LOG.info("Success after {} attempts", currentAttempt + 1);
        }
        return result;
      } catch (JedisNoReachableClusterNodeException e) {
        throw e;
      } catch (JedisConnectionException e) {
        LOG.warn("Failed connecting to Redis: {}", connection, e);
        // "- 1" because we just did one, but the currentAttempt counter hasn't increased yet
        int attemptsLeft = maxAttempts - currentAttempt - 1;
        connectionSupplier = handleConnectionProblem(connection, slot, attemptsLeft, deadline);
      } catch (JedisRedirectionException e) {
        redirectionSupplier = handleRedirection(connection, e);
      } finally {
        releaseConnection(connection);
      }

      LOG.info("{} retries left...", maxAttempts - currentAttempt - 1);
    }

    throw new JedisClusterMaxAttemptsException("No more cluster attempts left.");
  }

  protected void sleep(long sleepMillis) {
    try {
      LOG.info("Backing off, sleeping {}ms before trying again...", sleepMillis);
      TimeUnit.MILLISECONDS.sleep(sleepMillis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private Supplier<Jedis> handleConnectionProblem(Jedis failedConnection, final int slot, int attemptsLeft,
      Instant doneDeadline) {
    if (!shouldBackOff(attemptsLeft)) {
      return () -> {
        Jedis connection = connectionHandler.getConnectionFromSlot(slot);
        LOG.info("Retrying with {}", connection);
        return connection;
      };
    }

    // Must release current connection before renewing the slot cache below. If we fail to do this,
    // then JedisClusterTest.testReturnConnectionOnJedisClusterConnection will start failing
    // intermittently.
    releaseConnection(failedConnection);

    //We need this because if node is not reachable anymore - we need to finally initiate slots
    //renewing, or we can stuck with cluster state without one node in opposite case.
    //TODO make tracking of successful/unsuccessful operations for node - do renewing only
    //if there were no successful responses from this node last few seconds
    this.connectionHandler.renewSlotCache();

    return () -> {
      sleep(getBackoffSleepMillis(attemptsLeft, doneDeadline));
      // Get a random connection, it will redirect us if it's not the right one
      LOG.info("Retrying with a random node...");
      Jedis connection = connectionHandler.getConnection();
      LOG.info("Retrying with random pick: {}", connection);
      return connection;
    };
  }

  private Supplier<Jedis> handleRedirection(Jedis connection, final JedisRedirectionException jre) {
    LOG.debug("Redirected by server to {}", jre.getTargetNode());

    // if MOVED redirection occurred,
    if (jre instanceof JedisMovedDataException) {
      // it rebuilds cluster's slot cache recommended by Redis cluster specification
      this.connectionHandler.renewSlotCache(connection);
    }

    // release current connection before iteration
    releaseConnection(connection);

    return () -> {
      Jedis redirectedConnection = connectionHandler.getConnectionFromNode(jre.getTargetNode());
      LOG.info("Retrying with redirection target {}", connection);
      if (jre instanceof JedisAskDataException) {
        // TODO: Pipeline asking with the original command to make it faster....
        redirectedConnection.asking();
      }

      return redirectedConnection;
    };
  }

  private void releaseConnection(Jedis connection) {
    if (connection != null) {
      connection.close();
    }
  }

}
