package redis.clients.jedis.resps;

import redis.clients.jedis.util.SafeEncoder;

/**
 * This class is used to represent a List element when it is returned with respective key name.
 */
public class KeyedListElement {

  private final String key;
  private final String element;

  public KeyedListElement(byte[] key, byte[] element) {
    this(SafeEncoder.encode(key), SafeEncoder.encode(element));
  }

  public KeyedListElement(String key, String element) {
    this.key = key;
    this.element = element;
  }

  public String getKey() {
    return key;
  }

  public String getElement() {
    return element;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof KeyedZSetElement)) return false;

    KeyedListElement other = (KeyedListElement) o;
    return key.equals(other.key) && element.equals(other.element);
  }

  @Override
  public int hashCode() {
    return 31 * key.hashCode() + element.hashCode();
  }

  @Override
  public String toString() {
    return "KeyedListElement{" + "key=" + key + ", element='" + element + "} ";
  }
}
