package redis.clients.jedis.resps;

import redis.clients.jedis.util.Keyed;
import redis.clients.jedis.util.SafeEncoder;

/**
 * This class is used to represent a SortedSet element when it is returned with respective key name.
 * @deprecated Use {@link Keyed}&lt;{@link Tuple}&gt;.
 */
@Deprecated
public class KeyedZSetElement extends Keyed<Tuple> {

  public KeyedZSetElement(byte[] key, byte[] element, Double score) {
    this(SafeEncoder.encode(key), SafeEncoder.encode(element), score);
  }

  public KeyedZSetElement(String key, String element, Double score) {
    super(element, new Tuple(element, score));
  }

  @Deprecated
  public String getElement() {
    return getValue().getElement();
  }

  @Deprecated
  public byte[] getBinaryElement() {
    return getValue().getBinaryElement();
  }

  @Deprecated
  public double getScore() {
    return getValue().getScore();
  }
}
