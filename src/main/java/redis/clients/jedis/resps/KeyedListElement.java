package redis.clients.jedis.resps;

import redis.clients.jedis.util.KeyValue;
import redis.clients.jedis.util.SafeEncoder;

/**
 * This class is used to represent a List element when it is returned with respective key name.
 * @deprecated Use {@link KeyValue}.
 */
@Deprecated
public class KeyedListElement extends KeyValue<String, String> {

  public KeyedListElement(byte[] key, byte[] element) {
    this(SafeEncoder.encode(key), SafeEncoder.encode(element));
  }

  public KeyedListElement(String key, String element) {
    super(key, element);
  }

  /**
   * @deprecated Use {@link java.util.Map.Entry#getValue()}.
   */
  @Deprecated
  public String getElement() {
    return getValue();
  }
}
