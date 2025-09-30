package redis.clients.jedis.mcf;

import redis.clients.jedis.Endpoint;

public class MultiClusterPooledConnectionProviderHelper {

  public static void onHealthStatusChange(MultiClusterPooledConnectionProvider provider,
      Endpoint endpoint, HealthStatus oldStatus, HealthStatus newStatus) {
    provider.onHealthStatusChange(new HealthStatusChangeEvent(endpoint, oldStatus, newStatus));
  }

  public static void periodicFailbackCheck(MultiClusterPooledConnectionProvider provider) {
    provider.periodicFailbackCheck();
  }

  public static Endpoint switchToHealthyCluster(MultiClusterPooledConnectionProvider provider,
      SwitchReason reason, MultiClusterPooledConnectionProvider.Cluster iterateFrom) {
    return provider.switchToHealthyCluster(reason, iterateFrom);
  }
}
