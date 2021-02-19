package redis.clients.jedis;

import java.util.function.Function;
import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisClusterMaxAttemptsException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.jedis.exceptions.JedisNoReachableClusterNodeException;
import redis.clients.jedis.exceptions.JedisRedirectionException;

/**
 * Retries failed and redirected Jedis connections, without any backoff.
 *
 * @see Retryer
 */
public class DefaultRetryer implements Retryer {

  private final int maxAttempts;

  public DefaultRetryer(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  @Override
  public <R> R runWithRetries(JedisClusterConnectionHandler connectionHandler,
      Function<Jedis, R> command) {
    Jedis connection = null;
    try {
      connection = connectionHandler.getConnection();
      return command.apply(connection);
    } finally {
      releaseConnection(connection);
    }
  }

  @Override
  public <R> R runWithRetries(JedisClusterConnectionHandler connectionHandler, int slot,
      Function<Jedis, R> command) {
    return runWithRetries(connectionHandler, command, slot, maxAttempts, false, null);
  }

  private <R> R runWithRetries(JedisClusterConnectionHandler connectionHandler, Function<Jedis, R> command,
      final int slot, int attempts, boolean tryRandomNode, JedisRedirectionException redirect) {
    if (attempts <= 0) {
      throw new JedisClusterMaxAttemptsException("No more cluster attempts left.");
    }

    Jedis connection = null;
    try {

      if (redirect != null) {
        connection = connectionHandler.getConnectionFromNode(redirect.getTargetNode());
        if (redirect instanceof JedisAskDataException) {
          // TODO: Pipeline asking with the original command to make it faster....
          connection.asking();
        }
      } else {
        if (tryRandomNode) {
          connection = connectionHandler.getConnection();
        } else {
          connection = connectionHandler.getConnectionFromSlot(slot);
        }
      }

      return command.apply(connection);

    } catch (JedisNoReachableClusterNodeException jnrcne) {
      throw jnrcne;
    } catch (JedisConnectionException jce) {
      // release current connection before recursion
      releaseConnection(connection);
      connection = null;

      if (attempts <= 1) {
        //We need this because if node is not reachable anymore - we need to finally initiate slots
        //renewing, or we can stuck with cluster state without one node in opposite case.
        //But now if maxAttempts = [1 or 2] we will do it too often.
        //TODO make tracking of successful/unsuccessful operations for node - do renewing only
        //if there were no successful responses from this node last few seconds
        connectionHandler.renewSlotCache();
      }

      return runWithRetries(connectionHandler, command, slot, attempts - 1, tryRandomNode, redirect);
    } catch (JedisRedirectionException jre) {
      // if MOVED redirection occurred,
      if (jre instanceof JedisMovedDataException) {
        // it rebuilds cluster's slot cache recommended by Redis cluster specification
        connectionHandler.renewSlotCache(connection);
      }

      // release current connection before recursion
      releaseConnection(connection);
      connection = null;

      return runWithRetries(connectionHandler, command, slot, attempts - 1, false, jre);
    } finally {
      releaseConnection(connection);
    }
  }

  private void releaseConnection(Jedis connection) {
    if (connection != null) {
      connection.close();
    }
  }

}
