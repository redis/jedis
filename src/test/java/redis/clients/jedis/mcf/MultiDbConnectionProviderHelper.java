package redis.clients.jedis.mcf;

import redis.clients.jedis.Endpoint;

public class MultiDbConnectionProviderHelper {

  public static void onHealthStatusChange(MultiDbConnectionProvider provider, Endpoint endpoint,
      HealthStatus oldStatus, HealthStatus newStatus) {
    provider.onHealthStatusChange(new HealthStatusChangeEvent(endpoint, oldStatus, newStatus));
  }

  public static void periodicFailbackCheck(MultiDbConnectionProvider provider) {
    provider.periodicFailbackCheck();
  }

  public static Endpoint switchToHealthyDatabase(MultiDbConnectionProvider provider,
      SwitchReason reason, MultiDbConnectionProvider.Database iterateFrom) {
    return provider.switchToHealthyDatabase(reason, iterateFrom);
  }

  public static MultiDbConnectionProvider.Database waitForInitializationPolicy(
      MultiDbConnectionProvider provider, StatusTracker statusTracker) {
    return provider.waitForInitializationPolicy(statusTracker);
  }
}
