package redis.clients.jedis.executors.aggregators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class SetAggregator<T> implements Aggregator<Set<T>, Set<T>> {

  // Parts stores references to the maps added to the aggregator.
  // Defines the initial capacity of the list holding the parts.
  // Hard to come up with a reasonable default.
  // Start with 3 as min redis cluster has 3 masters.
  private static final int INITIAL_CAPACITY = 3;

  private List<Set<T>> parts;
  private int totalSize;

  @Override
  public void add(Set<T> set) {

    if (set == null) {
      return;
    }

    if (parts == null) {
      parts = new ArrayList<>(INITIAL_CAPACITY);
    }

    parts.add(set);
    totalSize += set.size();
  }

  @Override
  public Set<T> getResult() {

    if (parts == null) {
      return null;
    }

    if (parts.size() == 1) {
      return parts.get(0);
    }

    // Check if we're dealing with Set<byte[]> - need special handling for proper deduplication
    for (Set<T> set : parts) {
      if (!set.isEmpty()) {
        Object firstElement = set.iterator().next();
        if (firstElement instanceof byte[]) {
          return mergeByteArraySets();
        }
        break;
      }
    }

    Set<T> result = new HashSet<>(totalSize);

    for (Set<T> part : parts) {
      result.addAll(part);
    }

    return result;
  }

  @SuppressWarnings("unchecked")
  private Set<T> mergeByteArraySets() {
    // Wrap byte arrays for proper equals/hashCode, deduplicate, then unwrap
    Set<ByteArrayWrapper> wrappedSet = new HashSet<>(totalSize);

    for (Set<T> part : parts) {
      for (T element : part) {
        wrappedSet.add(new ByteArrayWrapper((byte[]) element));
      }
    }

    // Unwrap back to byte[]
    Set<Object> result = new HashSet<>(wrappedSet.size());
    for (ByteArrayWrapper wrapper : wrappedSet) {
      result.add(wrapper.unwrap());
    }

    return (Set<T>) result;
  }

  static final class ByteArrayWrapper {
    private final byte[] data;

    ByteArrayWrapper(byte[] data) {
      this.data = data;
    }

    byte[] unwrap() {
      return data;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof ByteArrayWrapper)) {
        return false;
      }
      return Arrays.equals(data, ((ByteArrayWrapper) o).data);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(data);
    }
  }
}
