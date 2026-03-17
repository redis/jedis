package redis.clients.jedis.executors.aggregators;

import redis.clients.jedis.util.JedisByteHashMap;

import java.util.ArrayList;
import java.util.List;

class JedisByteHashMapAggregator implements Aggregator<JedisByteHashMap, JedisByteHashMap> {

  private List<JedisByteHashMap> parts;
  private int totalSize;

  @Override
  public void add(JedisByteHashMap map) {
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
  public JedisByteHashMap getResult() {
    if (parts == null) {
      return null;
    }

    // Fast path: only one map added → return it
    if (parts.size() == 1) {
      return parts.get(0);
    }

    JedisByteHashMap result = new JedisByteHashMap();

    // Pre-allocate internal map if needed (optional)
    // Note: JedisByteHashMap internally uses LinkedHashMap, which grows automatically
    for (JedisByteHashMap part : parts) {
      result.putAll(part);
    }

    return result;
  }
}