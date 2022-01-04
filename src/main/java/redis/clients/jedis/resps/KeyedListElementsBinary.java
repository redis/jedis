package redis.clients.jedis.resps;

import java.io.Serializable;
import java.util.List;

/**
 * elements from the first non-empty list key from the list of provided key names
 */
public class KeyedListElementsBinary implements Serializable {

  /**
   * list key
   * */
  private final byte[] key;

  /**
   * pop elements
   */
  private final List<byte[]> elements;

  public KeyedListElementsBinary(byte[] key, List<byte[]> elements) {
    this.key = key;
    this.elements = elements;
  }

  public byte[] getKey() {
    return key;
  }

  public List<byte[]> getElements() {
    return elements;
  }
}
