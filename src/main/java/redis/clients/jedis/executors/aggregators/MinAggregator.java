package redis.clients.jedis.executors.aggregators;

import redis.clients.jedis.exceptions.UnsupportedAggregationException;
import redis.clients.jedis.util.KeyValue;

/**
 * Aggregator that returns the minimum value added so far.
 * Supports Comparable types and KeyValue<K,V> where both key and value are Comparable.
 */
class MinAggregator<T> implements Aggregator<T, T> {

    private T min;

    @Override
    public void add(T input) {
        if (input == null) {
            return;
        }

        if (min == null) {
            min = input;
            return;
        }

        // Handle KeyValue types
        if (min instanceof KeyValue && input instanceof KeyValue) {
            min = (T) aggregateKeyValueMin((KeyValue<?, ?>) min, (KeyValue<?, ?>) input);
            return;
        }

        // Handle Comparable types
        if (min instanceof Comparable && input instanceof Comparable) {
            Comparable<Object> minComp = (Comparable<Object>) min;
            if (minComp.compareTo(input) > 0) {
                min = input;
            }
            return;
        }

        throw new UnsupportedAggregationException(
                "AGG_MIN policy requires Comparable types or KeyValue, but got: "
                        + min.getClass().getName() + " and " + input.getClass().getName());
    }

    @Override
    public T getResult() {
        return min;
    }

    @SuppressWarnings("unchecked")
    private KeyValue<?, ?> aggregateKeyValueMin(KeyValue<?, ?> kv1, KeyValue<?, ?> kv2) {
        Object minKey = ((Comparable<Object>) kv1.getKey()).compareTo(kv2.getKey()) <= 0 ? kv1.getKey() : kv2.getKey();
        Object minValue = ((Comparable<Object>) kv1.getValue()).compareTo(kv2.getValue()) <= 0 ? kv1.getValue() : kv2.getValue();
        return new KeyValue<>(minKey, minValue);
    }
}