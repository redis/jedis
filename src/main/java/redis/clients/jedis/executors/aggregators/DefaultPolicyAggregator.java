package redis.clients.jedis.executors.aggregators;

import redis.clients.jedis.exceptions.UnsupportedAggregationException;
import redis.clients.jedis.util.JedisByteHashMap;
import redis.clients.jedis.util.JedisByteMap;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Aggregator for DEFAULT policy.
 *
 * Lazily creates a delegate aggregator based on the first non-null sample value.
 * All subsequent additions are delegated to the same aggregator.
 * If all values are null, getResult() returns null.
 */
class DefaultPolicyAggregator<T> implements Aggregator<T, T> {

    private Aggregator<T, T> delegate;

    @Override
    public void add(T sample) {
        if (sample == null) {
            return; // ignore nulls
        }

        // Lazy initialization of delegate aggregator
        if (delegate == null) {
            delegate = createDelegateAggregator(sample);
        }

        delegate.add(sample);
    }

    @Override
    public T getResult() {
        return delegate == null ? null : delegate.getResult();
    }

    @SuppressWarnings("unchecked")
    private Aggregator<T, T> createDelegateAggregator(T sample) {
        Objects.requireNonNull(sample, "Sample value must not be null");

        if (sample instanceof List) {
            return (Aggregator<T, T>) new ListAggregator<>();
        }

        if (sample instanceof Set) {
            return (Aggregator<T, T>) new SetAggregator<>();
        }

        if (sample instanceof JedisByteHashMap) {
            return (Aggregator<T, T>) new JedisByteHashMapAggregator();
        }

        if (sample instanceof JedisByteMap) {
            return (Aggregator<T, T>) new JedisByteMapAggregator<>();
        }

        if (sample instanceof Map) {
            return (Aggregator<T, T>) new MapAggregator<>();
        }

        throw new UnsupportedAggregationException(
                "DEFAULT policy requires List, Set, Map, JedisByteHashMap, or JedisByteMap types, but got: "
                        + sample.getClass().getName());
    }
}
