package redis.clients.jedis.timeseries;

import java.util.List;
import java.util.Map;
import redis.clients.jedis.util.KeyValue;

public class TSMRangeElements extends KeyValue<String, List<TSElement>> {

  private final Map<String, String> labels;
  private final List<AggregationType> aggregators;
  private final List<String> reducers;
  private final List<String> sources;

  public TSMRangeElements(String key, Map<String, String> labels, List<TSElement> value) {
    super(key, value);
    this.labels = labels;
    this.aggregators = null;
    this.reducers = null;
    this.sources = null;
  }

  public TSMRangeElements(String key, Map<String, String> labels, List<AggregationType> aggregators, List<TSElement> value) {
    super(key, value);
    this.labels = labels;
    this.aggregators = aggregators;
    this.reducers = null;
    this.sources = null;
  }

  public TSMRangeElements(String key, Map<String, String> labels, List<String> reducers, List<String> sources, List<TSElement> value) {
    super(key, value);
    this.labels = labels;
    this.aggregators = null;
    this.reducers = reducers;
    this.sources = sources;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public List<AggregationType> getAggregators() {
    return aggregators;
  }

  public List<String> getReducers() {
    return reducers;
  }

  public List<String> getSources() {
    return sources;
  }

  public List<TSElement> getElements() {
    return getValue();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder().append(getClass().getSimpleName())
        .append("{key=").append(getKey()).append(", labels=").append(labels);
    if (aggregators != null) {
      sb.append(", aggregators=").append(aggregators);
    }
    if (reducers != null && sources != null) {
      sb.append(", reducers").append(reducers).append(", sources").append(sources);
    }
    return sb.append(", elements=").append(getElements()).append('}').toString();
  }
}
