package redis.clients.jedis.resps;

import redis.clients.jedis.util.SafeEncoder;

import java.awt.*;
import java.util.ArrayList;

/**
 * This class is used to represent a List element when it is returned with respective key name.
 */
public class KeyedListElement {

  private final String key;
  private final String element;
  private final List elementList;

  public KeyedListElement(byte[] key, byte[] element) {
    this(SafeEncoder.encode(key), SafeEncoder.encode(element));
  }

  public KeyedListElement(String key, String element) {
    this.key = key;
    this.element = element;
    this.elementList = new List();
  }

  public KeyedListElement(String key, List elementList) {
    this.key = key;
    this.elementList = elementList;
    this.element = "";
  }

  public String getKey() {
    return key;
  }

  public String getElement() {
    return element;
  }

  public List getElementList() { return elementList; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof KeyedListElement)) return false;

    KeyedListElement other = (KeyedListElement) o;
    return key.equals(other.key) && element.equals(other.element) && elementList.equals(other.elementList);
  }

  @Override
  public int hashCode() {
    return 31 * key.hashCode() + element.hashCode() + elementList.hashCode();
  }

  @Override
  public String toString() {
    return "KeyedListElement{" +
            "key='" + key + '\'' +
            ", element='" + element + '\'' +
            ", elementList=" + elementList +
            '}';
  }
}
