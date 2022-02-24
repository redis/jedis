package redis.clients.jedis.util;

import java.util.AbstractMap.SimpleImmutableEntry;

public class Keyed<E> extends SimpleImmutableEntry<String, E> {

  public Keyed(String key, E value) {
    super(key, value);
  }

  @Override
  public int hashCode() {
    return 31 * getKey().hashCode() + getValue().hashCode();
  }

  @Override
  public String toString() {
    return "Keyed{" + "key=" + getKey() + ", value='" + getValue() + "} ";
  }
}
