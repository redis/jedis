package redis.clients.jedis.executors.aggregators;

import java.util.ArrayList;
import java.util.List;

class ListAggregator<T> implements Aggregator<List<T>, List<T>> {

  // Parts stores references to the maps added to the aggregator.
  // Defines the initial capacity of the list holding the parts.
  // Hard to come up with a reasonable default.
  // Start with 3 as min redis cluster has 3 masters.
  private static final int INITIAL_CAPACITY = 3;

  private List<List<T>> parts;
  private int totalSize;

  @Override
  public void add(List<T> list) {

    if (list == null) {
      return;
    }

    if (parts == null) {
      parts = new ArrayList<>(INITIAL_CAPACITY);
    }

    parts.add(list);
    totalSize += list.size();
  }

  @Override
  public List<T> getResult() {

    if (parts == null) {
      return null;
    }

    if (parts.size() == 1) {
      return parts.get(0);
    }

    List<T> result = new ArrayList<>(totalSize);

    for (List<T> part : parts) {
      result.addAll(part);
    }

    return result;
  }
}