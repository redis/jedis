package redis.clients.jedis;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MaintenanceEventHandler {

  private final List<MaintenanceEventListener> listeners = new CopyOnWriteArrayList<>();

  public void addListener(MaintenanceEventListener listener) {
    listeners.add(listener);
  }

  public void removeListener(MaintenanceEventListener listener) {
    listeners.remove(listener);
  }

  public void removeAllListeners() {
    listeners.clear();
  }

  public Collection<MaintenanceEventListener> getListeners() {
    return listeners;
  }
}