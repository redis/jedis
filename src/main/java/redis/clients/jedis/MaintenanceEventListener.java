package redis.clients.jedis;

public interface MaintenanceEventListener {

  void onEvent(MaintenanceEvent e);

}
