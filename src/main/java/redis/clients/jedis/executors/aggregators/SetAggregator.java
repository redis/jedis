package redis.clients.jedis.executors.aggregators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class SetAggregator<T> implements Aggregator<Set<T>, Set<T>> {

    private List<Set<T>> parts;
    private int totalSize;

    @Override
    public void add(Set<T> set) {

        if (set == null) {
            return;
        }

        if (parts == null) {
            parts = new ArrayList<>(4);
        }

        parts.add(set);
        totalSize += set.size();
    }

    @Override
    public Set<T> getResult() {

        if (parts == null) {
            return null;
        }

        if (parts.size() == 1) {
            return parts.get(0);
        }

        Set<T> result = new HashSet<>(totalSize);

        for (Set<T> part : parts) {
            result.addAll(part);
        }

        return result;
    }
}
