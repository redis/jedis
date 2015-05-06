package redis.clients.collections;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A decorator to implement Set from List. Assume that given List do not contains duplicated values.
 * The resulting set displays the same ordering, concurrency, and performance characteristics as the
 * backing list.
 * This class is used for Redis commands which return Set result.
 * @param <E>
 */
public class SetFromList<E> extends AbstractSet<E> {
  private final List<E> list;

  private SetFromList(List<E> list) {
    if (list == null) {
      throw new NullPointerException("list");
    }
    this.list = list;
  }

  public void clear() {
    list.clear();
  }

  public int size() {
    return list.size();
  }

  public boolean isEmpty() {
    return list.isEmpty();
  }

  public boolean contains(Object o) {
    return list.contains(o);
  }

  public boolean remove(Object o) {
    return list.remove(o);
  }

  public boolean add(E e) {
    return !contains(e) && list.add(e);
  }

  public Iterator<E> iterator() {
    return list.iterator();
  }

  public Object[] toArray() {
    return list.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return list.toArray(a);
  }

  public String toString() {
    return list.toString();
  }

  public int hashCode() {
    return list.hashCode();
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    Collection c = (Collection) o;
    if (c.size() != size()) {
      return false;
    }
    try {
      return containsAll(c);
    } catch (ClassCastException unused) {
      return false;
    } catch (NullPointerException unused) {
      return false;
    }
  }

  public boolean containsAll(Collection<?> c) {
    return list.containsAll(c);
  }

  public boolean removeAll(Collection<?> c) {
    return list.removeAll(c);
  }

  public boolean retainAll(Collection<?> c) {
    return list.retainAll(c);
  }

  public static <E> SetFromList<E> of(List<E> list) {
    return new SetFromList<E>(list);
  }
}
