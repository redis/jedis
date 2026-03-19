package redis.clients.jedis.executors.aggregators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MapAggregator<K, V> implements Aggregator<Map<K, V>, Map<K, V>> {

  // Parts stores references to the maps added to the aggregator.
  // Defines the initial capacity of the list holding the parts.
  // Hard to come up with a reasonable default.
  // Start with 3 as min redis cluster has 3 masters.
  private static final int INITIAL_CAPACITY = 3;

  private List<Map<K, V>> parts;
  private int totalSize;

  @Override
  public void add(Map<K, V> map) {

    if (map == null) {
      return;
    }

    if (parts == null) {
      parts = new ArrayList<>(INITIAL_CAPACITY);
    }

    parts.add(map);
    totalSize += map.size();
  }

  @Override
  public Map<K, V> getResult() {

    if (parts == null) {
      return null;
    }

    if (parts.size() == 1) {
      return parts.get(0);
    }

    Map<K, V> result = new HashMap<>(totalSize);

    for (Map<K, V> part : parts) {
      result.putAll(part); // last write wins
    }

    return result;
  }
}