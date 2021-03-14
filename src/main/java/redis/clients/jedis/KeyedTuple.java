package redis.clients.jedis;

import redis.clients.jedis.util.SafeEncoder;

import java.util.Arrays;

public class KeyedTuple extends Tuple {
  private byte[] key;

  public KeyedTuple(byte[] key, byte[] element, Double score) {
    super(element, score);
    this.key = key;
  }

  public KeyedTuple(String key, String element, Double score) {
    super(element, score);
    this.key = SafeEncoder.encode(key);
  }

  public String getKey() {
    if (null != key) {
      return SafeEncoder.encode(key);
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof KeyedTuple)) return false;
    if (!super.equals(o)) return false;

    KeyedTuple that = (KeyedTuple) o;
    return Arrays.equals(key, that.key);
  }

  @Override
  public int hashCode() {
    return 31 * (key != null ? Arrays.hashCode(key) : 0) + super.hashCode();
  }

  @Override
  public String toString() {
    return "KeyedTuple{" + "key=" + SafeEncoder.encode(key) + ", element='" + getElement() + "'"
        + ", score=" + getScore() + "} ";
  }
}
