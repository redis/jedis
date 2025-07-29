package redis.clients.jedis;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

abstract class AbstractListenerHandler<T> implements ListenerHandler<T> {
  private final List<T> listeners = new CopyOnWriteArrayList<>();

  public void addListener(T listener) {
    listeners.add(listener);
  }

  public void removeListener(T listener) {
    listeners.remove(listener);
  }

  public void removeAllListeners() {
    listeners.clear();
  }

  public Collection<T> getListeners() {
    return listeners;
  }
}
