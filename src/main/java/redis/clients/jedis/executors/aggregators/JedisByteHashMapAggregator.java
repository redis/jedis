package redis.clients.jedis.executors.aggregators;

import redis.clients.jedis.util.JedisByteHashMap;

import java.util.ArrayList;
import java.util.List;

class JedisByteHashMapAggregator implements Aggregator<JedisByteHashMap, JedisByteHashMap> {

  // Parts stores references to the maps added to the aggregator.
  // Defines the initial capacity of the list holding the parts.
  // Hard to come up with a reasonable default.
  // Start with 3 as min redis cluster has 3 masters.
  private static final int INITIAL_CAPACITY = 3;

  private List<JedisByteHashMap> parts;

  @Override
  public void add(JedisByteHashMap map) {
    if (map == null) {
      return;
    }

    if (parts == null) {
      parts = new ArrayList<>(INITIAL_CAPACITY);
    }

    parts.add(map);
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

    for (JedisByteHashMap part : parts) {
      result.putAll(part);
    }

    return result;
  }
}