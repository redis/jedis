package redis.clients.jedis.util;

import java.util.AbstractMap.SimpleImmutableEntry;

public class KeyValue<K, V> extends SimpleImmutableEntry<K, V> {

  public KeyValue(K key, V value) {
    super(key, value);
  }

  public static <K, V> KeyValue<K, V> of(K key, V value) {
    return new KeyValue<>(key, value);
  }
}
