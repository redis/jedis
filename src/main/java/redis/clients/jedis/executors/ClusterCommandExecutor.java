package redis.clients.jedis.executors;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.*;
import redis.clients.jedis.annots.VisibleForTesting;
import redis.clients.jedis.exceptions.*;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.util.JedisAsserts;

public class ClusterCommandExecutor implements CommandExecutor {

  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Connection resolver used for keyed commands, to acquire connection based on slot.
   */
  private final ConnectionResolver slotBasedConnectionResolver;

  /**
   * Connection resolver used for keyless commands, to acquire connection in round-robin fashion
   * from arbitrary node.
   */
  private final ConnectionResolver roundRobinConnectionResolver;

  /**
   * Connection resolver used to enforce command execution on replicas.
   *
   * @see #executeCommandToReplica(CommandObject)
   */
  private final ConnectionResolver replicaOnlyConnectionResolver;

  public final ClusterConnectionProvider provider;
  protected final int maxAttempts;
  protected final Duration maxTotalRetriesDuration;
  protected final CommandFlagsRegistry flags;

  /**
   * @deprecated use {@link #ClusterCommandExecutor(ClusterConnectionProvider, int, Duration, CommandFlagsRegistry)}
   * instead. This constructor will be removed in the next major version.
   */
  @Deprecated
  public ClusterCommandExecutor(ClusterConnectionProvider provider, int maxAttempts,
      Duration maxTotalRetriesDuration) {
    this(provider, maxAttempts, maxTotalRetriesDuration, StaticCommandFlagsRegistry.registry());
  }

  public ClusterCommandExecutor(ClusterConnectionProvider provider, int maxAttempts,
      Duration maxTotalRetriesDuration, CommandFlagsRegistry flags) {
    JedisAsserts.notNull(flags, "CommandFlagsRegistry must not be null");
    JedisAsserts.notNull(provider, "provider must not be null");
    JedisAsserts.isTrue(maxAttempts > 0, "maxAttempts must be greater than 0");
    JedisAsserts.notNull(maxTotalRetriesDuration, "maxTotalRetriesDuration must not be null");

    this.provider = provider;
    this.maxAttempts = maxAttempts;
    this.maxTotalRetriesDuration = maxTotalRetriesDuration;
    this.flags = flags;

    this.slotBasedConnectionResolver = ConnectionResolverFactory.createSlotBasedResolver(
        provider);
    this.roundRobinConnectionResolver = ConnectionResolverFactory.createRoundRobinResolver(
        provider, flags);
    this.replicaOnlyConnectionResolver = ConnectionResolverFactory.createReplicaOnlyResolver(
        provider);
  }

  @Override
  public void close() {
    this.provider.close();
  }

  /**
   * Broadcast a command to cluster nodes.
   * <p>
   * This method uses {@link #doExecuteCommand} with a {@link SingleConnectionResolver} for each
   * node, which adds retry logic and connection failure handling to broadcast commands.
   * Redirections are not followed since we want to execute on specific nodes.
   * <p>
   * Error handling depends on the command's response policy:
   * <ul>
   *   <li>{@code ONE_SUCCEEDED}: Returns success if at least one node succeeds</li>
   *   <li>Other policies: Throws {@link JedisBroadcastException} if any node fails</li>
   * </ul>
   *
   * @param commandObject the command to broadcast
   * @param primaryOnly if true, broadcast only to primary nodes; if false, broadcast to all nodes
   *        including replicas
   * @return the reply from the command
   * @throws JedisBroadcastException if error handling criteria based on response policy are not met
   */
  public final <T> T broadcastCommand(CommandObject<T> commandObject, boolean primaryOnly) {
    Map<String, ConnectionPool> connectionMap = primaryOnly ? provider.getPrimaryNodesConnectionMap()
        : provider.getConnectionMap();

    // Get the response policy for aggregation
    CommandFlagsRegistry.ResponsePolicy responsePolicy = flags.getResponsePolicy(commandObject
        .getArguments());

    MultiNodeResultAggregator<T> aggregator = new MultiNodeResultAggregator<>(responsePolicy);

    for (Map.Entry<String, ConnectionPool> entry : connectionMap.entrySet()) {
      HostAndPort node = HostAndPort.from(entry.getKey());
      ConnectionPool pool = entry.getValue();
      try {
        // Create a resolver that acquires connections from this specific node's pool
        // A fresh connection is obtained on each resolve() call, allowing retries to work correctly
        // The doExecuteCommand method will close the connection after each attempt
        SingleConnectionResolver resolver = new SingleConnectionResolver(pool);
        // Don't follow redirections - we want to execute on specific nodes
        T aReply = doExecuteCommand(commandObject, resolver, false);
        aggregator.addSuccess(node, aReply);
      } catch (Exception anError) {
        aggregator.addError(node, anError);
      }
    }

    return aggregator.getResult();
  }

  /**
   * Execute multiple command objects across different cluster shards and aggregate the results.
   * <p>
   * This method is designed for commands that need to operate on keys distributed across multiple
   * hash slots (e.g., DEL, EXISTS, MGET with keys from different slots). Each CommandObject in the
   * list is executed on its appropriate shard based on the key's hash slot, and the results are
   * aggregated using the command's response policy.
   * <p>
   * Error handling depends on the command's response policy:
   * <ul>
   *   <li>{@code ONE_SUCCEEDED}: Returns success if at least one shard succeeds</li>
   *   <li>Other policies: Throws {@link JedisBroadcastException} if any shard fails</li>
   * </ul>
   *
   * @param commandObjects list of CommandObject instances, each targeting keys in the same hash slot
   * @param <T> the return type of the command
   * @return the aggregated reply from all shards
   * @throws JedisBroadcastException if error handling criteria based on response policy are not met
   */
  public final <T> T executeMultiShardCommand(List<CommandObject<T>> commandObjects) {
    if (commandObjects == null || commandObjects.isEmpty()) {
      throw new IllegalArgumentException("commandObjects must not be null or empty");
    }

    // Get the response policy from the first command (all commands should have the same policy)
    CommandFlagsRegistry.ResponsePolicy responsePolicy = flags.getResponsePolicy(
        commandObjects.get(0).getArguments());

    MultiNodeResultAggregator<T> aggregator = new MultiNodeResultAggregator<>(responsePolicy);

    for (CommandObject<T> commandObject : commandObjects) {
      try {
        // Execute each command on its appropriate shard using the existing retry logic
        T aReply = doExecuteCommand(commandObject, slotBasedConnectionResolver, true);
        aggregator.addSuccess(aReply);
      } catch (Exception anError) {
        // Extract node from exception (JedisClusterOperationException includes node info)
        aggregator.addError(anError);
      }
    }

    return aggregator.getResult();
  }

  @Override
  public final <T> T executeCommand(CommandObject<T> commandObject) {
    CommandArguments args = commandObject.getArguments();
    CommandFlagsRegistry.RequestPolicy requestPolicy = flags.getRequestPolicy(args);

    switch (requestPolicy) {
      case ALL_SHARDS:
        // Execute on all primary nodes (shards)
        return broadcastCommand(commandObject, true);

      case ALL_NODES:
        // Execute on all nodes including replicas
        return broadcastCommand(commandObject, false);

      // NOTE(imalinovskyi): Handling of special commands (SCAN, FT.CURSOR, etc.) should happen
      // in the custom abstractions and dedicated executor methods.
      case MULTI_SHARD: // Here we assume that MULTI_SHARD is already split into single-shard commands
                        // and we just execute them one by one
      case SPECIAL:
      case DEFAULT:
      default:
        // Default behavior: check if keyless, otherwise use single-shard routing
        if (args.isKeyless()) {
          return executeKeylessCommand(commandObject);
        } else {
          return executeKeyedCommand(commandObject);
        }
    }
  }

  public final <T> T executeCommandToReplica(CommandObject<T> commandObject) {
    return doExecuteCommand(commandObject, replicaOnlyConnectionResolver, true);
  }

  private <T> T executeKeylessCommand(CommandObject<T> commandObject) {
    // For keyless commands, don't follow redirections - just retry with a different random node
    return doExecuteCommand(commandObject, roundRobinConnectionResolver, false);
  }

  private <T> T executeKeyedCommand(CommandObject<T> commandObject) {
    return doExecuteCommand(commandObject, slotBasedConnectionResolver, true);
  }

  /**
   * Executes a command with retry logic using the provided connection resolver.
   *
   * @param commandObject the command to execute
   * @param resolver the connection resolver to use for acquiring connections
   * @param followRedirections whether to follow cluster redirections (MOVED/ASK). Set to false for
   *     keyless commands that should retry with the resolver instead.
   * @return the command result
   * @throws JedisClusterOperationException if all retry attempts fail, includes the last node tried
   */
  private <T> T doExecuteCommand(CommandObject<T> commandObject, ConnectionResolver resolver,
      boolean followRedirections) {
    Instant deadline = Instant.now().plus(maxTotalRetriesDuration);

    JedisRedirectionException redirect = null;
    int consecutiveConnectionFailures = 0;
    Exception lastException = null;
    HostAndPort lastNode = null;  // Track the last node we attempted to use
    for (int attemptsLeft = this.maxAttempts; attemptsLeft > 0; attemptsLeft--) {
      Connection connection = null;
      try {
        if (followRedirections && redirect != null) {
          // Following redirection, we need to use connection to the target node
          connection = provider.getConnection(redirect.getTargetNode());
          if (redirect instanceof JedisAskDataException) {
            // TODO: Pipeline asking with the original command to make it faster....
            connection.executeCommand(Protocol.Command.ASKING);
          }
        } else {
          connection = resolver.resolve(commandObject);
        }

        // Track the node we're using for error reporting
        lastNode = connection.getHostAndPort();

        return execute(connection, commandObject);

      } catch (JedisClusterOperationException jcoe) {
        throw jcoe;
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
        if (followRedirections) {
          log.debug("Redirected by server to {}", jre.getTargetNode());
          redirect = jre;
          // if MOVED redirection occurred,
          if (jre instanceof JedisMovedDataException) {
            // it rebuilds cluster's slot cache recommended by Redis cluster specification
            provider.renewSlotCache(connection);
          }
        } else {
          // When followRedirections is false, throw the redirection exception immediately
          // instead of silently handling or ignoring it
          throw jre;
        }
        consecutiveConnectionFailures = 0;
      } finally {
        IOUtils.closeQuietly(connection);
      }
      if (Instant.now().isAfter(deadline)) {
        throw new JedisClusterOperationException("Cluster retry deadline exceeded.", lastException,
            lastNode);
      }
    }

    JedisClusterOperationException maxAttemptsException =
        new JedisClusterOperationException("No more cluster attempts left.", lastException,
            lastNode);
    throw maxAttemptsException;
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
