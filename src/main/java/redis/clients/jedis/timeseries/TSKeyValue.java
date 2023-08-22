package redis.clients.jedis.timeseries;

import java.util.Map;
import redis.clients.jedis.util.KeyValue;

/**
 * @deprecated This class will be removed in next major released.
 */
@Deprecated
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
