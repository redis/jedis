package redis.clients.jedis.timeseries;

import java.util.List;
import java.util.Map;
import redis.clients.jedis.util.KeyValue;

public class TSMRangeElements extends KeyValue<String, List<TSElement>> {

  private final Map<String, String> labels;

  public TSMRangeElements(String key, Map<String, String> labels, List<TSElement> value) {
    super(key, value);
    this.labels = labels;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public List<TSElement> getElements() {
    return getValue();
  }

  @Override
  public String toString() {
    return new StringBuilder().append(getClass().getSimpleName())
        .append("{key=").append(getKey())
        .append(", labels=").append(labels)
        .append(", element = ").append(getElements())
        .append('}').toString();
  }
}
