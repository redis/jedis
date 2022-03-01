package redis.clients.jedis.resps;

import redis.clients.jedis.util.SafeEncoder;

/**
 * This class is used to represent a SortedSet element when it is returned with respective key name.
 */
public class KeyedZSetElement extends Tuple {

  private final String key;

  public KeyedZSetElement(byte[] key, byte[] element, Double score) {
    super(element, score);
    this.key = SafeEncoder.encode(key);
  }

  public KeyedZSetElement(String key, String element, Double score) {
    super(element, score);
    this.key = key;
  }

  public String getKey() {
    return key;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof KeyedZSetElement)) return false;

    if (!key.equals(((KeyedZSetElement) o).key)) return false;
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return 31 * key.hashCode() + super.hashCode();
  }

  @Override
  public String toString() {
    return "KeyedZSetElement{" + "key=" + key + ", element='" + getElement() + "'"
        + ", score=" + getScore() + "} ";
  }
}
