package redis.clients.jedis.timeseries;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.util.Keyed;

public class KeyedTSElements extends Keyed<List<TSElement>> {

  private final Map<String, String> labels;
  private final TSElement element;

  public KeyedTSElements(String key, Map<String, String> labels, List<TSElement> elements) {
    super(key, elements);
    this.labels = labels;
    this.element = null;
  }

  public KeyedTSElements(String key, Map<String, String> labels, TSElement element) {
    super(key, element != null ? Collections.singletonList(element) : Collections.emptyList());
    this.labels = labels;
    this.element = element;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public List<TSElement> getElements() {
    return getValue();
  }

  /**
   * Valid for only MGET command.
   *
   * @return element
   */
  public TSElement getElement() {
    return element;
  }
}
