package redis.clients.jedis.resps;


import java.util.List;


public class KeyedListValueElements<K,V> {

  private final K key;
  private final List<V> elements;


  public KeyedListValueElements(K key, List<V> elements) {
    this.key = key;
    this.elements = elements;
  }

  public K getKey() {
    return key;
  }

  public List<V> getElements() {
    return elements;
  }

}
