package redis.clients.jedis;

import java.util.Collection;

public interface MaintenanceEventHandler {

  void addListener(MaintenanceEventListener listener);

  void removeListener(MaintenanceEventListener listener);

  Collection<MaintenanceEventListener> getListeners();
}