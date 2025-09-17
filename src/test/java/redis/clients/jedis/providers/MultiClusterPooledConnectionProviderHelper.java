package redis.clients.jedis.providers;

import redis.clients.jedis.Endpoint;
import redis.clients.jedis.mcf.HealthStatus;
import redis.clients.jedis.mcf.HealthStatusChangeEvent;

public class MultiClusterPooledConnectionProviderHelper {

  public static void onHealthStatusChange(MultiClusterPooledConnectionProvider provider,
      Endpoint endpoint, HealthStatus oldStatus, HealthStatus newStatus) {
    provider.onHealthStatusChange(new HealthStatusChangeEvent(endpoint, oldStatus, newStatus));
  }

  public static void periodicFailbackCheck(MultiClusterPooledConnectionProvider provider) {
    provider.periodicFailbackCheck();
  }
}
