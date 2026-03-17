package redis.clients.jedis.executors.aggregators;

import java.util.ArrayList;
import java.util.List;

class ListAggregator<T> implements Aggregator<List<T>, List<T>> {

  private List<List<T>> parts;
  private int totalSize;

  @Override
  public void add(List<T> list) {

    if (list == null) {
      return;
    }

    if (parts == null) {
      parts = new ArrayList<>(4);
    }

    parts.add(list);
    totalSize += list.size();
  }

  @Override
  public List<T> getResult() {

    if (parts == null) {
      return null;
    }

    if (parts.size() == 1) {
      return parts.get(0);
    }

    List<T> result = new ArrayList<>(totalSize);

    for (List<T> part : parts) {
      result.addAll(part);
    }

    return result;
  }
}