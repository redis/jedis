package redis.clients.jedis;

import redis.clients.jedis.util.SafeEncoder;

import java.util.Arrays;

public class ScoreValueTuple extends Tuple {
  private byte[] key;

  public ScoreValueTuple(byte[] key, byte[] element, Double score) {
    super(element, score);
    this.key = key;
  }

  public ScoreValueTuple(String key, String element, Double score) {
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
    if (!(o instanceof ScoreValueTuple)) return false;
    if (!super.equals(o)) return false;

    ScoreValueTuple that = (ScoreValueTuple) o;
    return Arrays.equals(key, that.key);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (key != null ? Arrays.hashCode(key) : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ScoreValueTuple{" +
            "key=" + SafeEncoder.encode(key) +
            "} " + super.toString();
  }
}
