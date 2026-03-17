package redis.clients.jedis.executors.aggregators;

import redis.clients.jedis.util.JedisByteMap;

import java.util.ArrayList;
import java.util.List;

class JedisByteMapAggregator<T> implements Aggregator<JedisByteMap<T>, JedisByteMap<T>> {

  private List<JedisByteMap<T>> parts;
  private int totalSize;

  @Override
  public void add(JedisByteMap<T> map) {
    if (map == null || map.isEmpty()) {
      return;
    }

    if (parts == null) {
      parts = new ArrayList<>(4);
    }

    parts.add(map);
    totalSize += map.size();
  }

  @Override
  public JedisByteMap<T> getResult() {
    if (parts == null) {
      return null;
    }

    // Fast path: only one non-null map
    if (parts.size() == 1) {
      return parts.get(0);
    }

    JedisByteMap<T> result = new JedisByteMap<>();

    for (JedisByteMap<T> part : parts) {
      result.putAll(part);
    }

    return result;
  }
}
