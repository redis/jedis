package redis.clients.jedis;

import java.util.function.Function;
import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisClusterMaxAttemptsException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.jedis.exceptions.JedisNoReachableClusterNodeException;
import redis.clients.jedis.exceptions.JedisRedirectionException;

public class DefaultRetryer extends Retryer {

  private final JedisClusterConnectionHandler connectionHandler;
  private final int maxAttempts;

  public DefaultRetryer(JedisClusterConnectionHandler connectionHandler, int maxAttempts) {
    this.connectionHandler = connectionHandler;
    this.maxAttempts = maxAttempts;
  }

  @Override
  protected <R> R runWithRetries(int slot, Function<Jedis, R> command) {
    return runWithRetries(command, slot, maxAttempts, false, null);
  }

  @Override
  public <R> R runWithRetries(Function<Jedis, R> command) {
    Jedis connection = null;
    try {
      connection = connectionHandler.getConnection();
      return command.apply(connection);
    } finally {
      releaseConnection(connection);
    }
  }

  @Override
  public void close() {
    if (connectionHandler != null) {
      connectionHandler.close();
    }
  }

  private <R> R runWithRetries(Function<Jedis, R> command, final int slot, int attempts, boolean tryRandomNode, JedisRedirectionException redirect) {
    if (attempts <= 0) {
      throw new JedisClusterMaxAttemptsException("No more cluster attempts left.");
    }

    Jedis connection = null;
    try {

      if (redirect != null) {
        connection = this.connectionHandler.getConnectionFromNode(redirect.getTargetNode());
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
        this.connectionHandler.renewSlotCache();
      }

      return runWithRetries(command, slot, attempts - 1, tryRandomNode, redirect);
    } catch (JedisRedirectionException jre) {
      // if MOVED redirection occurred,
      if (jre instanceof JedisMovedDataException) {
        // it rebuilds cluster's slot cache recommended by Redis cluster specification
        this.connectionHandler.renewSlotCache(connection);
      }

      // release current connection before recursion
      releaseConnection(connection);
      connection = null;

      return runWithRetries(command, slot, attempts - 1, false, jre);
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
