package redis.clients.jedis.executors;

import java.util.Arrays;

import redis.clients.jedis.CommandFlagsRegistry;
import redis.clients.jedis.exceptions.ClusterAggregationException;
import redis.clients.jedis.exceptions.UnsupportedAggregationException;

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
 * <li>{@code ALL_SUCCEEDED} - Return first reply if all are equal, throw exception if
 * different</li>
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

      case SPECIAL:
        // NOTE(imalinovskyi): Handling of special commands (SCAN, FT.CURSOR, etc.) should happen
        // in the custom abstractions.
      case ONE_SUCCEEDED:
      case DEFAULT:
      default:
        return existing;
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
    throw new UnsupportedAggregationException(
        "AGG_SUM policy requires numeric types (Long, Integer, Double), but got: "
            + existing.getClass().getSimpleName() + " and " + newReply.getClass().getSimpleName());
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
    throw new UnsupportedAggregationException("AGG_MIN policy requires Comparable types, but got: "
        + existing.getClass().getSimpleName() + " and " + newReply.getClass().getSimpleName());
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
    throw new UnsupportedAggregationException("AGG_MAX policy requires Comparable types, but got: "
        + existing.getClass().getSimpleName() + " and " + newReply.getClass().getSimpleName());
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
    throw new UnsupportedAggregationException(
        "AGG_LOGICAL_AND policy requires Boolean or Long types, but got: "
            + existing.getClass().getSimpleName() + " and " + newReply.getClass().getSimpleName());
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
    throw new UnsupportedAggregationException(
        "AGG_LOGICAL_OR policy requires Boolean or Long types, but got: "
            + existing.getClass().getSimpleName() + " and " + newReply.getClass().getSimpleName());
  }

  /**
   * Return first reply if all are equal, throw exception if different. Uses Arrays.equals() for
   * byte array comparison.
   */
  public static <T> T aggregateAllSucceeded(T existing, T newReply) {
    if (areEqual(existing, newReply)) {
      return existing;
    }
    throw new ClusterAggregationException(
        "ALL_SUCCEEDED policy requires all replies to be equal, but got different values: "
            + existing + " vs " + newReply);
  }

  /**
   * Compare two values for equality, with special handling for byte arrays.
   */
  private static <T> boolean areEqual(T existing, T newReply) {
    if (existing instanceof byte[] && newReply instanceof byte[]) {
      return Arrays.equals((byte[]) existing, (byte[]) newReply);
    }
    return existing.equals(newReply);
  }
}
