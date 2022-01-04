package redis.clients.jedis.resps;

import java.io.Serializable;
import java.util.List;

/**
 * elements from the first non-empty list key from the list of provided key names
 */
public class KeyedListElements implements Serializable {

  /**
   * list key
   */
  private final String key;

  /**
   * pop elements
   */
  private final List<String> elements;

  public KeyedListElements(String key, List<String> elements) {
    this.key = key;
    this.elements = elements;
  }

  public String getKey() {
    return key;
  }

  public List<String> getElements() {
    return elements;
  }
}
