package redis.clients.jedis.mcf;

public interface HealthStatusListener {

  void onStatusChange(HealthStatusChangeEvent event);

}
