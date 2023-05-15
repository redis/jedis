package redis.clients.jedis.timeseries;

import java.util.Map;
import redis.clients.jedis.util.KeyValue;

public class TSKeyValue<V> extends KeyValue<String, V> {

  private final Map<String, String> labels;

  public TSKeyValue(String key, Map<String, String> labels, V value) {
    super(key, value);
    this.labels = labels;
  }

  public Map<String, String> getLabels() {
    return labels;
  }
}
