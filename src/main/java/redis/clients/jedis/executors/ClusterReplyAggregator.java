package redis.clients.jedis.executors;

import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.CommandFlagsRegistry;
import redis.clients.jedis.exceptions.JedisClusterOperationException;

/**
 * Utility class for aggregating replies from multiple Redis cluster nodes.
 * <p>
 * This class centralizes the reply aggregation logic used when executing commands across multiple
 * cluster nodes (broadcast commands) or multiple hash slots (multi-shard commands). It supports
 * different aggregation strategies based on the {@link CommandFlagsRegistry.ResponsePolicy} enum.
 * </p>
 * <p>
 * Supported aggregation policies:
 * <ul>
 * <li>{@code AGG_SUM} - Sum numeric values (Long, Integer, Double)</li>
 * <li>{@code AGG_MIN} - Return minimum value for comparable types</li>
 * <li>{@code AGG_MAX} - Return maximum value for comparable types</li>
 * <li>{@code AGG_LOGICAL_AND} - Perform logical AND on Boolean/Long values</li>
 * <li>{@code AGG_LOGICAL_OR} - Perform logical OR on Boolean/Long values</li>
 * <li>{@code ALL_SUCCEEDED} - Return first reply if all are equal, throw exception if different</li>
 * <li>{@code ONE_SUCCEEDED} - Return first non-null reply</li>
 * <li>{@code SPECIAL}/{@code DEFAULT} - Use legacy hardcoded logic for backward compatibility</li>
 * </ul>
 */
public final class ClusterReplyAggregator {

  private ClusterReplyAggregator() {
    // Utility class, prevent instantiation
  }

  /**
   * Aggregate replies from multiple sub-commands based on the response policy.
   * @param existing the existing aggregated reply
   * @param newReply the new reply to aggregate
   * @param policy the response policy that determines how to aggregate
   * @param <T> the reply type
   * @return the aggregated reply
   */
  @SuppressWarnings("unchecked")
  public static <T> T aggregate(T existing, T newReply,
      CommandFlagsRegistry.ResponsePolicy policy) {
    if (existing == null) {
      return newReply;
    }
    if (newReply == null) {
      return existing;
    }

    switch (policy) {
      case AGG_SUM:
        return aggregateSum(existing, newReply);

      case AGG_MIN:
        return aggregateMin(existing, newReply);

      case AGG_MAX:
        return aggregateMax(existing, newReply);

      case AGG_LOGICAL_AND:
        return aggregateLogicalAnd(existing, newReply);

      case AGG_LOGICAL_OR:
        return aggregateLogicalOr(existing, newReply);

      case ALL_SUCCEEDED:
        return aggregateAllSucceeded(existing, newReply);

      case ONE_SUCCEEDED:
        // Return first non-null reply (already handled above)
        return existing;

      case SPECIAL:
      case DEFAULT:
      default:
        // Use legacy hardcoded logic for backward compatibility
        return aggregateLegacy(existing, newReply);
    }
  }

  /**
   * Sum numeric values (for DEL, UNLINK, etc.).
   */
  @SuppressWarnings("unchecked")
  public static <T> T aggregateSum(T existing, T newReply) {
    if (existing instanceof Long && newReply instanceof Long) {
      return (T) Long.valueOf((Long) existing + (Long) newReply);
    }
    if (existing instanceof Integer && newReply instanceof Integer) {
      return (T) Integer.valueOf((Integer) existing + (Integer) newReply);
    }
    if (existing instanceof Double && newReply instanceof Double) {
      return (T) Double.valueOf((Double) existing + (Double) newReply);
    }
    // Fallback to legacy behavior
    return aggregateLegacy(existing, newReply);
  }

  /**
   * Return minimum value for comparable types.
   */
  @SuppressWarnings("unchecked")
  public static <T> T aggregateMin(T existing, T newReply) {
    if (existing instanceof Comparable && newReply instanceof Comparable) {
      Comparable<Object> existingComp = (Comparable<Object>) existing;
      return existingComp.compareTo(newReply) <= 0 ? existing : newReply;
    }
    // Fallback to legacy behavior
    return aggregateLegacy(existing, newReply);
  }

  /**
   * Return maximum value for comparable types.
   */
  @SuppressWarnings("unchecked")
  public static <T> T aggregateMax(T existing, T newReply) {
    if (existing instanceof Comparable && newReply instanceof Comparable) {
      Comparable<Object> existingComp = (Comparable<Object>) existing;
      return existingComp.compareTo(newReply) >= 0 ? existing : newReply;
    }
    // Fallback to legacy behavior
    return aggregateLegacy(existing, newReply);
  }

  /**
   * Perform logical AND on Boolean values.
   */
  @SuppressWarnings("unchecked")
  public static <T> T aggregateLogicalAnd(T existing, T newReply) {
    if (existing instanceof Boolean && newReply instanceof Boolean) {
      return (T) Boolean.valueOf((Boolean) existing && (Boolean) newReply);
    }
    // Handle Long as boolean (0 = false, non-zero = true) - common in Redis
    if (existing instanceof Long && newReply instanceof Long) {
      boolean existingBool = (Long) existing != 0;
      boolean newBool = (Long) newReply != 0;
      return (T) Long.valueOf((existingBool && newBool) ? 1L : 0L);
    }
    // Fallback to legacy behavior
    return aggregateLegacy(existing, newReply);
  }

  /**
   * Perform logical OR on Boolean values.
   */
  @SuppressWarnings("unchecked")
  public static <T> T aggregateLogicalOr(T existing, T newReply) {
    if (existing instanceof Boolean && newReply instanceof Boolean) {
      return (T) Boolean.valueOf((Boolean) existing || (Boolean) newReply);
    }
    // Handle Long as boolean (0 = false, non-zero = true) - common in Redis
    if (existing instanceof Long && newReply instanceof Long) {
      boolean existingBool = (Long) existing != 0;
      boolean newBool = (Long) newReply != 0;
      return (T) Long.valueOf((existingBool || newBool) ? 1L : 0L);
    }
    // Fallback to legacy behavior
    return aggregateLegacy(existing, newReply);
  }

  /**
   * Return first reply if all are equal, throw exception if different.
   */
  public static <T> T aggregateAllSucceeded(T existing, T newReply) {
    if (existing.equals(newReply)) {
      return existing;
    }
    throw new JedisClusterOperationException(
        "ALL_SUCCEEDED policy requires all replies to be equal, but got different values: "
            + existing + " vs " + newReply);
  }

  /**
   * Legacy aggregation logic for backward compatibility. Handles List concatenation and other
   * types.
   */
  @SuppressWarnings("unchecked")
  public static <T> T aggregateLegacy(T existing, T newReply) {
    // Sum Long values (for DEL, UNLINK, etc.)
    if (existing instanceof Long && newReply instanceof Long) {
      return (T) Long.valueOf((Long) existing + (Long) newReply);
    }

    // Concatenate Lists (for MGET, etc.)
    if (existing instanceof List && newReply instanceof List) {
      List<Object> result = new ArrayList<>((List<Object>) existing);
      result.addAll((List<Object>) newReply);
      return (T) result;
    }

    // For other types, just return the existing value
    // This handles cases like String "OK" responses
    return existing;
  }
}
