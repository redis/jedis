package redis.clients.jedis.timeseries;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class KeyedTSElements implements Iterable<TSElement> {

  private final String key;
  private final Map<String, String> labels;
  private final List<TSElement> elements;
  private final TSElement element;

  public KeyedTSElements(String key, Map<String, String> labels, List<TSElement> elements) {
    this.key = key;
    this.labels = labels;
    this.elements = elements;
    this.element = null;
  }

  public KeyedTSElements(String key, Map<String, String> labels, TSElement element) {
    this.key = key;
    this.labels = labels;
    this.element = element;
    this.elements = element != null ? Collections.singletonList(element) : Collections.emptyList();
  }

  public String getKey() {
    return key;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public List<TSElement> getElements() {
    return elements;
  }

  /**
   * Valid for only MGET command.
   *
   * @return element
   */
  public TSElement getElement() {
    return element;
  }

  @Override
  public Iterator<TSElement> iterator() {
    return elements.iterator();
  }
}
