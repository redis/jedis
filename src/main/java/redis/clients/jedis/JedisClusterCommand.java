package redis.clients.jedis;

import java.util.function.Supplier;
import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisClusterMaxAttemptsException;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.jedis.exceptions.JedisNoReachableClusterNodeException;
import redis.clients.jedis.exceptions.JedisRedirectionException;
import redis.clients.jedis.util.JedisClusterCRC16;

public abstract class JedisClusterCommand<T> {

  private final JedisClusterConnectionHandler connectionHandler;
  private final int maxAttempts;

  public JedisClusterCommand(JedisClusterConnectionHandler connectionHandler, int maxAttempts) {
    this.connectionHandler = connectionHandler;
    this.maxAttempts = maxAttempts;
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
    Supplier<Jedis> connectionSupplier = () -> connectionHandler.getConnectionFromSlot(slot);

    // If we got one redirection, stick with that and don't try anything else
    Supplier<Jedis> redirectionSupplier = null;

    Exception lastException = null;
    for (int currentAttempt = 0; currentAttempt < this.maxAttempts; currentAttempt++) {
      Jedis connection = null;
      try {
        if (redirectionSupplier != null) {
          connection = redirectionSupplier.get();
        } else {
          connection = connectionSupplier.get();
        }
        return execute(connection);
      } catch (JedisNoReachableClusterNodeException e) {
        throw e;
      } catch (JedisConnectionException e) {
        lastException = e;
        connectionSupplier = handleConnectionProblem(slot, currentAttempt);
      } catch (JedisRedirectionException e) {
        lastException = e;
        redirectionSupplier = handleRedirection(connection, e);
      } finally {
        releaseConnection(connection);
      }
    }

    throw new JedisClusterMaxAttemptsException("No more cluster attempts left.", lastException);
  }

  private Supplier<Jedis> handleConnectionProblem(final int slot, int currentAttempt) {
    int attemptsLeft = (maxAttempts - currentAttempt) - 1;
    if (attemptsLeft <= 1) {
      //We need this because if node is not reachable anymore - we need to finally initiate slots
      //renewing, or we can stuck with cluster state without one node in opposite case.
      //But now if maxAttempts = [1 or 2] we will do it too often.
      //TODO make tracking of successful/unsuccessful operations for node - do renewing only
      //if there were no successful responses from this node last few seconds
      this.connectionHandler.renewSlotCache();
    }

    return () -> connectionHandler.getConnectionFromSlot(slot);
  }

  private Supplier<Jedis> handleRedirection(Jedis connection, final JedisRedirectionException jre) {
    // if MOVED redirection occurred,
    if (jre instanceof JedisMovedDataException) {
      // it rebuilds cluster's slot cache recommended by Redis cluster specification
      this.connectionHandler.renewSlotCache(connection);
    }


    return () -> {
      Jedis redirectedConnection = connectionHandler.getConnectionFromNode(jre.getTargetNode());
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
