package redis.clients.jedis;

import redis.clients.jedis.MaintenanceEvent.FailedOverEvent;
import redis.clients.jedis.MaintenanceEvent.FailingOverEvent;
import redis.clients.jedis.MaintenanceEvent.MigratedEvent;
import redis.clients.jedis.MaintenanceEvent.MigratingEvent;
import redis.clients.jedis.MaintenanceEvent.MovingEvent;

interface MaintenanceEventHandler {

  void onMoving(MovingEvent e, Connection c);

  void onMigrating(MigratingEvent e, Connection c);

  void onMigrated(MigratedEvent e, Connection c);

  void onFailingOver(FailingOverEvent e, Connection c);

  void onFailedOver(FailedOverEvent e, Connection c);
}
