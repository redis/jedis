package redis.clients.jedis.executors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.CommandFlagsRegistry;
import redis.clients.jedis.exceptions.UnsupportedAggregationException;
import redis.clients.jedis.util.JedisByteHashMap;
import redis.clients.jedis.util.JedisByteMap;
import redis.clients.jedis.util.KeyValue;

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
 * <li>{@code DEFAULT} - Aggregate replies by merging collections (concatenate lists, merge
 * maps)</li>
 * <li>{@code SPECIAL} - Use custom handling logic for special commands</li>
 * </ul>
 */
final class ClusterReplyAggregator {

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

      case DEFAULT:
        return aggregateDefault(existing, newReply);

      case SPECIAL:
        // NOTE(imalinovskyi): Handling of special commands (SCAN, FT.CURSOR, etc.) should happen
        // in the custom abstractions.
      case ONE_SUCCEEDED:
      default:
        return existing;
    }
  }

  /**
   * Aggregate replies using DEFAULT policy. For keyless commands, merges replies into a single
   * nested data structure by concatenating lists, merging maps, or combining sets.
   * <p>
   * Supports:
   * <ul>
   * <li>List types (e.g., ArrayList&lt;String&gt;, ArrayList&lt;byte[]&gt;) - concatenates all
   * elements</li>
   * <li>Map types - merges all entries, with entries from newReply overwriting existing entries on
   * key collision</li>
   * <li>Set types (e.g., HashSet&lt;String&gt;, SetFromList&lt;byte[]&gt;) - combines all elements
   * into a single set</li>
   * </ul>
   */
  @SuppressWarnings("unchecked")
  public static <T> T aggregateDefault(T existing, T newReply) {
    // Handle List types - mutate in place if ArrayList, otherwise create once
    if (existing instanceof List && newReply instanceof List) {
      List<Object> existingList = (List<Object>) existing;
      List<Object> newList = (List<Object>) newReply;

      if (existingList instanceof ArrayList) {
        // Mutate existing ArrayList in place - avoids creating new collection
        ((ArrayList<Object>) existingList).ensureCapacity(existingList.size() + newList.size());
      }
      existingList.addAll(newList);
      return (T) existingList;
    }

    // Handle JedisByteHashMap types - mutate in place
    // NOTE: Must be checked before generic Map since JedisByteHashMap implements Map
    if (existing instanceof JedisByteHashMap && newReply instanceof JedisByteHashMap) {
      JedisByteHashMap existingMap = (JedisByteHashMap) existing;
      JedisByteHashMap newMap = (JedisByteHashMap) newReply;
      existingMap.putAll(newMap);
      return (T) existingMap;
    }

    // Handle JedisByteMap types - mutate in place
    // NOTE: Must be checked before generic Map since JedisByteMap implements Map
    if (existing instanceof JedisByteMap && newReply instanceof JedisByteMap) {
      JedisByteMap<Object> existingMap = (JedisByteMap<Object>) existing;
      JedisByteMap<Object> newMap = (JedisByteMap<Object>) newReply;
      existingMap.putAll(newMap);
      return (T) existingMap;
    }

    // Handle Map types - mutate in place
    if (existing instanceof Map && newReply instanceof Map) {
      Map<Object, Object> existingMap = (Map<Object, Object>) existing;
      Map<Object, Object> newMap = (Map<Object, Object>) newReply;
      existingMap.putAll(newMap);
      return (T) existingMap;
    }

    // Handle Set types - mutate in place if HashSet, otherwise create once
    if (existing instanceof Set && newReply instanceof Set) {
      Set<Object> existingSet = (Set<Object>) existing;
      Set<Object> newSet = (Set<Object>) newReply;
      existingSet.addAll(newSet);
      return (T) existingSet;
    }

    // For other types, throw UnsupportedAggregationException
    throw new UnsupportedAggregationException(
        "DEFAULT policy requires List, Map, Set, JedisByteHashMap, or JedisByteMap types, but got: "
            + (existing != null ? existing.getClass().getName() : "null") + " and "
            + (newReply != null ? newReply.getClass().getName() : "null"));
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
   * <p>
   * Also supports {@link KeyValue} types where both key and value are {@link Comparable}. For
   * KeyValue, returns a new KeyValue with the minimum of each component (used by WAITAOF).
   */
  @SuppressWarnings("unchecked")
  public static <T> T aggregateMin(T existing, T newReply) {
    // Handle KeyValue types (e.g., KeyValue<Long, Long> from WAITAOF)
    // Returns a new KeyValue with the minimum of each component
    if (existing instanceof KeyValue && newReply instanceof KeyValue) {
      return (T) aggregateKeyValueMin((KeyValue<?, ?>) existing, (KeyValue<?, ?>) newReply);
    }

    if (existing instanceof Comparable && newReply instanceof Comparable) {
      Comparable<Object> existingComp = (Comparable<Object>) existing;
      return existingComp.compareTo(newReply) <= 0 ? existing : newReply;
    }
    throw new UnsupportedAggregationException("AGG_MIN policy requires Comparable types, but got: "
        + existing.getClass().getSimpleName() + " and " + newReply.getClass().getSimpleName());
  }

  /**
   * Aggregates two KeyValue pairs by taking the minimum of each component.
   */
  @SuppressWarnings("unchecked")
  private static KeyValue<?, ?> aggregateKeyValueMin(KeyValue<?, ?> existing,
      KeyValue<?, ?> newReply) {
    Object minKey;
    Object minValue;

    // Get minimum key
    if (existing.getKey() instanceof Comparable && newReply.getKey() instanceof Comparable) {
      Comparable<Object> existingKey = (Comparable<Object>) existing.getKey();
      minKey = existingKey.compareTo(newReply.getKey()) <= 0 ? existing.getKey()
          : newReply.getKey();
    } else {
      throw new UnsupportedAggregationException(
          "AGG_MIN for KeyValue requires Comparable key types, but got: "
              + (existing.getKey() != null ? existing.getKey().getClass().getSimpleName()
                  : "null"));
    }

    // Get minimum value
    if (existing.getValue() instanceof Comparable && newReply.getValue() instanceof Comparable) {
      Comparable<Object> existingValue = (Comparable<Object>) existing.getValue();
      minValue = existingValue.compareTo(newReply.getValue()) <= 0 ? existing.getValue()
          : newReply.getValue();
    } else {
      throw new UnsupportedAggregationException(
          "AGG_MIN for KeyValue requires Comparable value types, but got: "
              + (existing.getValue() != null ? existing.getValue().getClass().getSimpleName()
                  : "null"));
    }

    return KeyValue.of(minKey, minValue);
  }

  /**
   * Return maximum value for comparable types.
   * <p>
   * Also supports {@link KeyValue} types where both key and value are {@link Comparable}. For
   * KeyValue, returns a new KeyValue with the maximum of each component.
   */
  @SuppressWarnings("unchecked")
  public static <T> T aggregateMax(T existing, T newReply) {
    // Handle KeyValue types (e.g., KeyValue<Long, Long>)
    // Returns a new KeyValue with the maximum of each component
    if (existing instanceof KeyValue && newReply instanceof KeyValue) {
      return (T) aggregateKeyValueMax((KeyValue<?, ?>) existing, (KeyValue<?, ?>) newReply);
    }

    if (existing instanceof Comparable && newReply instanceof Comparable) {
      Comparable<Object> existingComp = (Comparable<Object>) existing;
      return existingComp.compareTo(newReply) >= 0 ? existing : newReply;
    }
    throw new UnsupportedAggregationException("AGG_MAX policy requires Comparable types, but got: "
        + existing.getClass().getSimpleName() + " and " + newReply.getClass().getSimpleName());
  }

  /**
   * Aggregates two KeyValue pairs by taking the maximum of each component.
   */
  @SuppressWarnings("unchecked")
  private static KeyValue<?, ?> aggregateKeyValueMax(KeyValue<?, ?> existing,
      KeyValue<?, ?> newReply) {
    Object maxKey;
    Object maxValue;

    // Get maximum key
    if (existing.getKey() instanceof Comparable && newReply.getKey() instanceof Comparable) {
      Comparable<Object> existingKey = (Comparable<Object>) existing.getKey();
      maxKey = existingKey.compareTo(newReply.getKey()) >= 0 ? existing.getKey()
          : newReply.getKey();
    } else {
      throw new UnsupportedAggregationException(
          "AGG_MAX for KeyValue requires Comparable key types, but got: "
              + (existing.getKey() != null ? existing.getKey().getClass().getSimpleName()
                  : "null"));
    }

    // Get maximum value
    if (existing.getValue() instanceof Comparable && newReply.getValue() instanceof Comparable) {
      Comparable<Object> existingValue = (Comparable<Object>) existing.getValue();
      maxValue = existingValue.compareTo(newReply.getValue()) >= 0 ? existing.getValue()
          : newReply.getValue();
    } else {
      throw new UnsupportedAggregationException(
          "AGG_MAX for KeyValue requires Comparable value types, but got: "
              + (existing.getValue() != null ? existing.getValue().getClass().getSimpleName()
                  : "null"));
    }

    return KeyValue.of(maxKey, maxValue);
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
    // Handle ArrayList<Boolean> - element-wise logical AND
    if (existing instanceof ArrayList && newReply instanceof ArrayList) {
      ArrayList<?> existingList = (ArrayList<?>) existing;
      ArrayList<?> newList = (ArrayList<?>) newReply;
      if (existingList.size() != newList.size()) {
        throw new UnsupportedAggregationException(
            "AGG_LOGICAL_AND policy requires ArrayLists of equal size, but got sizes: "
                + existingList.size() + " and " + newList.size());
      }
      if (!existingList.isEmpty()) {
        Object firstExisting = existingList.get(0);
        Object firstNew = newList.get(0);
        // Handle ArrayList<Boolean>
        if (firstExisting instanceof Boolean && firstNew instanceof Boolean) {
          ArrayList<Boolean> existingBoolList = (ArrayList<Boolean>) existing;
          ArrayList<Boolean> newBoolList = (ArrayList<Boolean>) newReply;
          ArrayList<Boolean> result = new ArrayList<>(existingBoolList.size());
          for (int i = 0; i < existingBoolList.size(); i++) {
            result.add(existingBoolList.get(i) && newBoolList.get(i));
          }
          return (T) result;
        }
        // Handle ArrayList<Long> - treat 0 as false, non-zero as true
        if (firstExisting instanceof Long && firstNew instanceof Long) {
          ArrayList<Long> existingLongList = (ArrayList<Long>) existing;
          ArrayList<Long> newLongList = (ArrayList<Long>) newReply;
          ArrayList<Long> result = new ArrayList<>(existingLongList.size());
          for (int i = 0; i < existingLongList.size(); i++) {
            boolean existingBool = existingLongList.get(i) != 0;
            boolean newBool = newLongList.get(i) != 0;
            result.add((existingBool && newBool) ? 1L : 0L);
          }
          return (T) result;
        }
      } else {
        // Empty lists - return empty ArrayList
        return (T) new ArrayList<>();
      }
    }
    throw new UnsupportedAggregationException(
        "AGG_LOGICAL_AND policy requires Boolean, Long, ArrayList<Boolean>, or ArrayList<Long> types, but got: "
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
    // Handle ArrayList<Boolean> - element-wise logical OR
    if (existing instanceof ArrayList && newReply instanceof ArrayList) {
      ArrayList<?> existingList = (ArrayList<?>) existing;
      ArrayList<?> newList = (ArrayList<?>) newReply;
      if (existingList.size() != newList.size()) {
        throw new UnsupportedAggregationException(
            "AGG_LOGICAL_OR policy requires ArrayLists of equal size, but got sizes: "
                + existingList.size() + " and " + newList.size());
      }
      if (!existingList.isEmpty()) {
        Object firstExisting = existingList.get(0);
        Object firstNew = newList.get(0);
        // Handle ArrayList<Boolean>
        if (firstExisting instanceof Boolean && firstNew instanceof Boolean) {
          ArrayList<Boolean> existingBoolList = (ArrayList<Boolean>) existing;
          ArrayList<Boolean> newBoolList = (ArrayList<Boolean>) newReply;
          ArrayList<Boolean> result = new ArrayList<>(existingBoolList.size());
          for (int i = 0; i < existingBoolList.size(); i++) {
            result.add(existingBoolList.get(i) || newBoolList.get(i));
          }
          return (T) result;
        }
        // Handle ArrayList<Long> - treat 0 as false, non-zero as true
        if (firstExisting instanceof Long && firstNew instanceof Long) {
          ArrayList<Long> existingLongList = (ArrayList<Long>) existing;
          ArrayList<Long> newLongList = (ArrayList<Long>) newReply;
          ArrayList<Long> result = new ArrayList<>(existingLongList.size());
          for (int i = 0; i < existingLongList.size(); i++) {
            boolean existingBool = existingLongList.get(i) != 0;
            boolean newBool = newLongList.get(i) != 0;
            result.add((existingBool || newBool) ? 1L : 0L);
          }
          return (T) result;
        }
      } else {
        // Empty lists - return empty ArrayList
        return (T) new ArrayList<>();
      }
    }
    throw new UnsupportedAggregationException(
        "AGG_LOGICAL_OR policy requires Boolean, Long, ArrayList<Boolean>, or ArrayList<Long> types, but got: "
            + existing.getClass().getSimpleName() + " and " + newReply.getClass().getSimpleName());
  }

  /**
   * Return the first reply. Per Redis ALL_SUCCEEDED response policy spec, this policy returns
   * successfully only if there are no error replies. Error handling is done separately by the
   * caller (e.g., MultiNodeResultAggregator.addError()), so this method simply returns the first
   * reply when aggregating successful responses.
   */
  public static <T> T aggregateAllSucceeded(T existing, T newReply) {
    return existing;
  }
}
