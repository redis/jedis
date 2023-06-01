package redis.clients.jedis.timeseries;

import java.util.List;
import java.util.Map;
import redis.clients.jedis.util.KeyValue;

public class TSMRangeElements extends KeyValue<String, List<TSElement>> {

  private final Map<String, String> labels;
  private final List<AggregationType> aggregators;

  public TSMRangeElements(String key, Map<String, String> labels, List<TSElement> value) {
    super(key, value);
    this.labels = labels;
    this.aggregators = null;
  }

  public TSMRangeElements(String key, Map<String, String> labels, List<AggregationType> aggregators, List<TSElement> value) {
    super(key, value);
    this.labels = labels;
    this.aggregators = aggregators;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public List<AggregationType> getAggregators() {
    return aggregators;
  }

  public List<TSElement> getElements() {
    return getValue();
  }

  @Override
  public String toString() {
    return new StringBuilder().append(getClass().getSimpleName())
        .append("{key=").append(getKey())
        .append(", labels=").append(labels)
        .append(", elements=").append(getElements())
        .append('}').toString();
  }
}
