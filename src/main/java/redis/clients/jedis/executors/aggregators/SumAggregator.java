package redis.clients.jedis.executors.aggregators;

import redis.clients.jedis.exceptions.UnsupportedAggregationException;

/**
 * Aggregator that sums numeric values of the same type.
 * Null inputs are ignored; if all inputs are null, the result is null.
 *
 * Supports Integer, Long, and Double.
 */
class SumAggregator<T extends Number> implements Aggregator<T, T> {

    private T sum;

    @Override
    public void add(T value) {
        if (value == null) {
            return;
        }

        if (sum == null) {
            sum = value;
            return;
        }

        if (sum instanceof Long && value instanceof Long) {
            sum = (T) Long.valueOf(sum.longValue() + value.longValue());
        } else if (sum instanceof Integer && value instanceof Integer) {
            sum = (T) Integer.valueOf(sum.intValue() + value.intValue());
        } else if (sum instanceof Double && value instanceof Double) {
            sum = (T) Double.valueOf(sum.doubleValue() + value.doubleValue());
        } else {
            throw new UnsupportedAggregationException(
                    "SumAggregator requires numeric types of the same kind (Integer, Long, Double), but got: "
                            + sum.getClass().getSimpleName() + " and " + value.getClass().getSimpleName());
        }
    }

    @Override
    public T getResult() {
        return sum;
    }
}
