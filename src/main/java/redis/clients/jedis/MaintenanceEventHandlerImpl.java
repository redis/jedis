package redis.clients.jedis;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MaintenanceEventHandlerImpl implements MaintenanceEventHandler {
  private final List<MaintenanceEventListener> listeners = new CopyOnWriteArrayList<>();

  @Override
  public void addListener(MaintenanceEventListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(MaintenanceEventListener listener) {
    listeners.remove(listener);
  }

  @Override
  public Collection<MaintenanceEventListener> getListeners() {
    return listeners;
  }
}
