package redis.clients.jedis.executors;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.annots.VisibleForTesting;
import redis.clients.jedis.exceptions.*;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.util.JedisAsserts;

public class ClusterCommandExecutor implements CommandExecutor {

  private final Logger log = LoggerFactory.getLogger(getClass());

  public final ClusterConnectionProvider provider;
  protected final int maxAttempts;
  protected final Duration maxTotalRetriesDuration;

  // Round-robin counter for keyless command distribution
  private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

  public ClusterCommandExecutor(ClusterConnectionProvider provider, int maxAttempts,
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
  public final <T> T broadcastCommand(CommandObject<T> commandObject) {
    Map<String, ConnectionPool> connectionMap = provider.getPrimaryNodesConnectionMap();

    boolean isErrored = false;
    T reply = null;
    JedisBroadcastException bcastError = new JedisBroadcastException();
    for (Map.Entry<String, ConnectionPool> entry : connectionMap.entrySet()) {
      HostAndPort node = HostAndPort.from(entry.getKey());
      ConnectionPool pool = entry.getValue();
      try (Connection connection = pool.getResource()) {
        T aReply = execute(connection, commandObject);
        bcastError.addReply(node, aReply);
        if (isErrored) { // already errored
        } else if (reply == null) {
          reply = aReply; // ok
        } else if (reply.equals(aReply)) {
          // ok
        } else {
          isErrored = true;
          reply = null;
        }
      } catch (Exception anError) {
        bcastError.addReply(node, anError);
        isErrored = true;
      }
    }
    if (isErrored) {
      throw bcastError;
    }
    return reply;
  }

  @Override
  public final <T> T executeCommand(CommandObject<T> commandObject) {
    return doExecuteCommand(commandObject, false);
  }

  @Override
  public final <T> T executeKeylessCommand(CommandObject<T> commandObject) {
    Instant deadline = Instant.now().plus(maxTotalRetriesDuration);
    int consecutiveConnectionFailures = 0;
    Exception lastException = null;

    RequiredConnectionType connectionType;
    if (commandObject.getFlags().contains(CommandObject.CommandFlag.READONLY)) {
      connectionType = RequiredConnectionType.REPLICA;
    } else {
      connectionType = RequiredConnectionType.PRIMARY;
    }

    for (int attemptsLeft = this.maxAttempts; attemptsLeft > 0; attemptsLeft--) {
      Connection connection = null;
      try {
        // Use round-robin distribution for keyless commands
        connection = getNextConnection(connectionType);
        return execute(connection, commandObject);

      } catch (JedisConnectionException jce) {
        lastException = jce;
        ++consecutiveConnectionFailures;
        log.debug("Failed connecting to Redis: {}", connection, jce);

        if (consecutiveConnectionFailures < 2) {
          continue;
        }

        boolean reset = handleConnectionProblem(attemptsLeft - 1, consecutiveConnectionFailures,
          deadline);
        if (reset) {
          consecutiveConnectionFailures = 0;
        }
      } catch (JedisRedirectionException jre) {
        // For keyless commands, we don't follow redirections since we're not targeting a specific
        // slot
        // Just retry with a different random node
        lastException = jre;
        log.debug("Received redirection for keyless command, retrying with different node: {}",
          jre.getMessage());
        consecutiveConnectionFailures = 0;
      } finally {
        IOUtils.closeQuietly(connection);
      }
      if (Instant.now().isAfter(deadline)) {
        throw new JedisClusterOperationException("Cluster retry deadline exceeded.", lastException);
      }
    }

    JedisClusterOperationException maxAttemptsException = new JedisClusterOperationException(
        "No more cluster attempts left.");
    maxAttemptsException.addSuppressed(lastException);
    throw maxAttemptsException;
  }

  public final <T> T executeCommandToReplica(CommandObject<T> commandObject) {
    return doExecuteCommand(commandObject, true);
  }

  private <T> T doExecuteCommand(CommandObject<T> commandObject, boolean toReplica) {
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
          connection = toReplica ? provider.getReplicaConnection(commandObject.getArguments())
              : provider.getConnection(commandObject.getArguments());
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
        throw new JedisClusterOperationException("Cluster retry deadline exceeded.", lastException);
      }
    }

    JedisClusterOperationException maxAttemptsException
        = new JedisClusterOperationException("No more cluster attempts left.");
    if (lastException != null) {
      maxAttemptsException.addSuppressed(lastException);
    }
    throw maxAttemptsException;
  }

  private enum RequiredConnectionType {
    PRIMARY, REPLICA
  }

  /**
   * Gets a connection using round-robin distribution across all cluster nodes. This ensures even
   * distribution of keyless commands across the cluster.
   * @return Connection from the next node in round-robin sequence
   * @throws JedisClusterOperationException if no cluster nodes are available
   */
  private Connection getNextConnection(RequiredConnectionType connectionType) {
    List<Map.Entry<String, ConnectionPool>> nodeList = selectNextConnectionPool(connectionType);
    // Select node using round-robin distribution for true unified distribution
    // Use modulo directly on the node list size to create a circular counter
    int roundRobinIndex = roundRobinCounter
        .getAndUpdate(current -> (current + 1) % nodeList.size());
    Map.Entry<String, ConnectionPool> selectedEntry = nodeList.get(roundRobinIndex);
    ConnectionPool pool = selectedEntry.getValue();

    return pool.getResource();
  }

  private List<Map.Entry<String, ConnectionPool>> selectNextConnectionPool(
      RequiredConnectionType connectionType) {
    Map<String, ConnectionPool> connectionMap;

    // NOTE(imalinovskyi): If we need to connect to replica, we use all nodes, otherwise we use only
    // primary nodes
    if (connectionType == RequiredConnectionType.REPLICA) {
      connectionMap = provider.getConnectionMap();
    } else {
      connectionMap = provider.getPrimaryNodesConnectionMap();
    }

    if (connectionMap.isEmpty()) {
      throw new JedisClusterOperationException("No cluster nodes available.");
    }

    // Convert connection map to list for round-robin access
    return new ArrayList<>(connectionMap.entrySet());
  }

    /**
   * WARNING: This method is accessible for the purpose of testing.
   * This should not be used or overriden.
   */
  @VisibleForTesting
  protected <T> T execute(Connection connection, CommandObject<T> commandObject) {
    return connection.executeCommand(commandObject);
  }

  /**
   * Related values should be reset if <code>TRUE</code> is returned.
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
  @VisibleForTesting
  protected void sleep(long sleepMillis) {
    try {
      TimeUnit.MILLISECONDS.sleep(sleepMillis);
    } catch (InterruptedException e) {
      throw new JedisClusterOperationException(e);
    }
  }
}
