package redis.clients.jedis.util;

import java.util.Iterator;
import java.util.List;

public class KeyedList<E> extends Keyed<List<E>> implements Iterable<E> {

  private final List<E> list;

  public KeyedList(String key, List<E> list) {
    super(key, list);
    this.list = list;
  }

  @Override
  public Iterator<E> iterator() {
    return this.list.iterator();
  }
}
