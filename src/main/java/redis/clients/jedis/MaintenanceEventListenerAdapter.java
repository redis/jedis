package redis.clients.jedis;

import java.util.concurrent.atomic.AtomicReference;

import redis.clients.jedis.MaintenanceEvent.FailedOverEvent;
import redis.clients.jedis.MaintenanceEvent.FailingOverEvent;
import redis.clients.jedis.MaintenanceEvent.MigratedEvent;
import redis.clients.jedis.MaintenanceEvent.MigratingEvent;
import redis.clients.jedis.MaintenanceEvent.MovingEvent;

class MaintenanceEventListenerAdapter implements MaintenanceEventHandler {

  private final MaintenanceEventListener listener;
  private final AtomicReference<MaintenanceEvent> lastEvent = new AtomicReference<>();

  public MaintenanceEventListenerAdapter(MaintenanceEventListener listener) {
    this.listener = listener;
  }

  @Override
  public void onMoving(MovingEvent e, Connection c) {
    onEvent(e);
  }

  @Override
  public void onMigrating(MigratingEvent e, Connection c) {
    onEvent(e);
  }

  @Override
  public void onMigrated(MigratedEvent e, Connection c) {
    onEvent(e);
  }

  @Override
  public void onFailingOver(FailingOverEvent e, Connection c) {
    onEvent(e);
  }

  @Override
  public void onFailedOver(FailedOverEvent e, Connection c) {
    onEvent(e);
  }

  private void onEvent(MaintenanceEvent e) {
    MaintenanceEvent last;
    do {
      last = lastEvent.get();
      if (last != null && last.seq >= e.seq) {
        return; // stale or duplicate event, drop it
      }
    } while (!lastEvent.compareAndSet(last, e));

    listener.onEvent(e);
  }
}
