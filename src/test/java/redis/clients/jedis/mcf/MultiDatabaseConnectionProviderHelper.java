package redis.clients.jedis.mcf;

import redis.clients.jedis.Endpoint;

public class MultiDatabaseConnectionProviderHelper {

  public static void onHealthStatusChange(MultiDatabaseConnectionProvider provider,
      Endpoint endpoint, HealthStatus oldStatus, HealthStatus newStatus) {
    provider.onHealthStatusChange(new HealthStatusChangeEvent(endpoint, oldStatus, newStatus));
  }

  public static void periodicFailbackCheck(MultiDatabaseConnectionProvider provider) {
    provider.periodicFailbackCheck();
  }

  public static Endpoint switchToHealthyCluster(MultiDatabaseConnectionProvider provider,
      SwitchReason reason, MultiDatabaseConnectionProvider.Database iterateFrom) {
    return provider.switchToHealthyDatabase(reason, iterateFrom);
  }
}
