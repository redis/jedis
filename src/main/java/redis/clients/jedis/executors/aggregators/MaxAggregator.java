package redis.clients.jedis.executors.aggregators;

import redis.clients.jedis.exceptions.UnsupportedAggregationException;
import redis.clients.jedis.util.KeyValue;

/**
 * Aggregator that returns the maximum value added so far. Supports Comparable types and
 * KeyValue<K,V> where both key and value are Comparable.
 */
class MaxAggregator<T> implements Aggregator<T, T> {

  private T max;

  @Override
  public void add(T input) {
    if (input == null) {
      return;
    }

    if (max == null) {
      max = input;
      return;
    }

    // Handle KeyValue types
    if (max instanceof KeyValue && input instanceof KeyValue) {
      max = (T) aggregateKeyValueMax((KeyValue<?, ?>) max, (KeyValue<?, ?>) input);
      return;
    }

    // Handle Comparable types
    if (max instanceof Comparable && input instanceof Comparable) {
      Comparable<Object> maxComp = (Comparable<Object>) max;
      if (maxComp.compareTo(input) < 0) {
        max = input;
      }
      return;
    }

    throw new UnsupportedAggregationException(
        "AGG_MAX policy requires Comparable types or KeyValue, but got: " + max.getClass().getName()
            + " and " + input.getClass().getName());
  }

  @Override
  public T getResult() {
    return max;
  }

  @SuppressWarnings("unchecked")
  private KeyValue<?, ?> aggregateKeyValueMax(KeyValue<?, ?> kv1, KeyValue<?, ?> kv2) {
    Object maxKey = ((Comparable<Object>) kv1.getKey()).compareTo(kv2.getKey()) >= 0 ? kv1.getKey()
        : kv2.getKey();
    Object maxValue = ((Comparable<Object>) kv1.getValue()).compareTo(kv2.getValue()) >= 0
        ? kv1.getValue()
        : kv2.getValue();
    return new KeyValue<>(maxKey, maxValue);
  }
}
