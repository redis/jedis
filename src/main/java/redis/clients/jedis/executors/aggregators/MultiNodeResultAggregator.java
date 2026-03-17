package redis.clients.jedis.executors.aggregators;

import redis.clients.jedis.CommandFlagsRegistry;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.annots.Internal;
import redis.clients.jedis.exceptions.JedisBroadcastException;
import redis.clients.jedis.exceptions.JedisClusterOperationException;

/**
 * Internal use only. Aggregates results from multiple node/shard executions based on response policy.
 * <p>
 * This class centralizes the logic for collecting results and errors from multi-node command
 * execution (broadcast commands, multi-shard commands) and determining the final result based on
 * the {@link CommandFlagsRegistry.ResponsePolicy}.
 * <p>
 * Key behavior differences by policy:
 * <ul>
 * <li>{@code ONE_SUCCEEDED}: Returns success if at least one node succeeds, even if others fail.
 * Only throws if ALL nodes fail.</li>
 * <li>All other policies: Throws {@link JedisBroadcastException} if ANY node fails.</li>
 * </ul>
 * @param <T> the type of the command result
 */
@Internal
public final class MultiNodeResultAggregator<T> {

  private static final HostAndPort UNKNOWN_NODE = HostAndPort.from("unknown:0");

  private final CommandFlagsRegistry.ResponsePolicy responsePolicy;
  private final JedisBroadcastException bcastError;
  private final Aggregator<T, T> responseAggregator;
  private boolean hasError;
  private boolean hasSuccess;
  /**
   * Creates a new aggregator with the specified response policy.
   * @param responsePolicy the policy that determines how to aggregate results and handle errors
   */
  public MultiNodeResultAggregator(CommandFlagsRegistry.ResponsePolicy responsePolicy) {
    this.responsePolicy = responsePolicy;
    this.responseAggregator = Aggregator.create(responsePolicy);
    this.bcastError = new JedisBroadcastException();
    this.hasError = false;
    this.hasSuccess = false;
  }

  /**
   * Records a successful result from a node.
   * @param node the node that returned the result
   * @param result the result from the node
   */
  public void addSuccess(HostAndPort node, T result) {
    if (node != null) {
      bcastError.addReply(node, result);
    }
    aggregateSuccess(result);
  }

  /**
   * Records a successful result without node information.
   * <p>
   * Use this method when the node information is not readily available, such as in multi-shard
   * commands where extracting the node would require additional computation.
   * @param result the result from the operation
   */
  public void addSuccess(T result) {
    aggregateSuccess(result);
  }

  /**
   * Aggregates a successful result into the accumulated reply.
   * @param result the result to aggregate
   */
  private void aggregateSuccess(T result) {
    hasSuccess = true;

    // Always aggregate successful results, even if we've seen errors
    // This is important for ONE_SUCCEEDED policy where we need to return
    // a successful result even if some nodes failed
    responseAggregator.add(result);
  }

  /**
   * Records an error from a node.
   * @param node the node that returned the error
   * @param error the exception from the node
   */
  public void addError(HostAndPort node, Exception error) {
    bcastError.addReply(node, error);
    hasError = true;
  }

  /**
   * Records an error, extracting the node information from the exception if available.
   * <p>
   * This method extracts node information from {@link JedisClusterOperationException} if present,
   * otherwise uses a placeholder "unknown:0" node.
   * @param error the exception from the failed operation
   */
  public void addError(Exception error) {
    HostAndPort node = extractNodeFromException(error);
    addError(node, error);
  }

  /**
   * Extracts the node information from an exception if available.
   * @param error the exception to extract node info from
   * @return the node that caused the error, or a placeholder if unknown
   */
  private static HostAndPort extractNodeFromException(Exception error) {
    if (error instanceof JedisClusterOperationException) {
      HostAndPort node = ((JedisClusterOperationException) error).getNode();
      if (node != null) {
        return node;
      }
    }
    return UNKNOWN_NODE;
  }

  /**
   * Returns the aggregated result based on the response policy.
   * <p>
   * For {@code ONE_SUCCEEDED} policy: returns the aggregated result if at least one node succeeded,
   * throws {@link JedisBroadcastException} only if all nodes failed.
   * <p>
   * For all other policies: throws {@link JedisBroadcastException} if any node failed, otherwise
   * returns the aggregated result.
   * @return the aggregated result
   * @throws JedisBroadcastException if the policy criteria are not met
   */
  public T getResult() {
    if (responsePolicy == CommandFlagsRegistry.ResponsePolicy.ONE_SUCCEEDED) {
      // ONE_SUCCEEDED: return success if at least one node succeeded
      if (hasSuccess) {
        return responseAggregator.getResult();
      }
      // All nodes failed
      throw bcastError.prepareToThrow();
    } else {
      // All other policies: throw if any node failed
      if (hasError) {
        throw bcastError.prepareToThrow();
      }
      return responseAggregator.getResult();
    }
  }

  /**
   * Returns the response policy being used by this aggregator.
   * @return the response policy
   */
  public CommandFlagsRegistry.ResponsePolicy getResponsePolicy() {
    return responsePolicy;
  }


}
