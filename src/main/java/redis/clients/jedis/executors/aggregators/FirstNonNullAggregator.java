package redis.clients.jedis.executors.aggregators;

/**
 * Aggregator that returns the first non-null value added.
 * Subsequent additions are ignored.
 */
class FirstNonNullAggregator<T> implements Aggregator<T, T> {

    private T value;

    @Override
    public void add(T input) {
        if (value == null && input != null) {
            value = input;
        }
    }

    @Override
    public T getResult() {
        return value;
    }
}
