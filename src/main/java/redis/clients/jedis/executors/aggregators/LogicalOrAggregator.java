package redis.clients.jedis.executors.aggregators;

import redis.clients.jedis.exceptions.UnsupportedAggregationException;

import java.util.ArrayList;

/**
 * Aggregator that performs logical OR on added values.
 * Supports Boolean, Long, ArrayList<Boolean>, ArrayList<Long>.
 */
class LogicalOrAggregator<T> implements Aggregator<T, T> {

    private T result;

    @Override
    public void add(T input) {
        if (input == null) {
            return; // ignore nulls
        }

        if (result == null) {
            // First non-null input initializes the result
            if (input instanceof Boolean || input instanceof Long || input instanceof ArrayList) {
                result = input;
                return;
            } else {
                throw new UnsupportedAggregationException(
                        "AGG_LOGICAL_OR requires Boolean, Long, ArrayList<Boolean>, or ArrayList<Long>, but got: "
                                + input.getClass().getName());
            }
        }

        // Handle Boolean
        if (result instanceof Boolean && input instanceof Boolean) {
            result = (T) Boolean.valueOf((Boolean) result || (Boolean) input);
            return;
        }

        // Handle Long
        if (result instanceof Long && input instanceof Long) {
            boolean existingBool = (Long) result != 0;
            boolean newBool = (Long) input != 0;
            result = (T) Long.valueOf((existingBool || newBool) ? 1L : 0L);
            return;
        }

        // Handle ArrayList
        if (result instanceof ArrayList && input instanceof ArrayList) {
            ArrayList<?> existingList = (ArrayList<?>) result;
            ArrayList<?> newList = (ArrayList<?>) input;

            if (existingList.size() != newList.size()) {
                throw new UnsupportedAggregationException(
                        "AGG_LOGICAL_OR requires ArrayLists of equal size, but got sizes: "
                                + existingList.size() + " and " + newList.size());
            }

            if (!existingList.isEmpty()) {
                Object firstExisting = existingList.get(0);
                Object firstNew = newList.get(0);

                // ArrayList<Boolean>
                if (firstExisting instanceof Boolean && firstNew instanceof Boolean) {
                    ArrayList<Boolean> res = new ArrayList<>(existingList.size());
                    for (int i = 0; i < existingList.size(); i++) {
                        res.add((Boolean) existingList.get(i) || (Boolean) newList.get(i));
                    }
                    result = (T) res;
                    return;
                }

                // ArrayList<Long> treated as boolean
                if (firstExisting instanceof Long && firstNew instanceof Long) {
                    ArrayList<Long> res = new ArrayList<>(existingList.size());
                    for (int i = 0; i < existingList.size(); i++) {
                        boolean e = ((Long) existingList.get(i)) != 0;
                        boolean n = ((Long) newList.get(i)) != 0;
                        res.add((e || n) ? 1L : 0L);
                    }
                    result = (T) res;
                    return;
                }
            } else {
                // Empty lists → result remains empty list
                result = (T) new ArrayList<>();
                return;
            }
        }

        throw new UnsupportedAggregationException(
                "AGG_LOGICAL_OR requires Boolean, Long, ArrayList<Boolean>, or ArrayList<Long>, but got: "
                        + result.getClass().getName() + " and " + input.getClass().getName());
    }

    @Override
    public T getResult() {
        return result;
    }
}
