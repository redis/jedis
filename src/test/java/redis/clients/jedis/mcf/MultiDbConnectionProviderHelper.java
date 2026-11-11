package redis.clients.jedis.mcf;

import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.Endpoint;
import redis.clients.jedis.MultiDbClient;
import redis.clients.jedis.util.ClientTestUtil;

public class MultiDbConnectionProviderHelper {

  /** Connection pool of the client's active database. */
  public static ConnectionPool activeConnectionPool(MultiDbClient client) {
    MultiDbConnectionProvider provider = ClientTestUtil.getConnectionProvider(client);
    return provider.getDatabase().getConnectionPool();
  }

  public static void onHealthStatusChange(MultiDbConnectionProvider provider, Endpoint endpoint,
      HealthStatus oldStatus, HealthStatus newStatus) {
    provider.onHealthStatusChange(new HealthStatusChangeEvent(endpoint, oldStatus, newStatus));
  }

  public static void periodicFailbackCheck(MultiDbConnectionProvider provider) {
    provider.periodicFailbackCheck();
  }

  /** Switches and runs the post-switch operations, as the lock-free production callers do. */
  public static Endpoint switchToHealthyDatabase(MultiDbConnectionProvider provider,
      SwitchReason reason, MultiDbConnectionProvider.Database iterateFrom) {
    MultiDbConnectionProvider.SwitchInfo switchInfo = provider.switchToHealthyDatabase(reason,
      iterateFrom);
    provider.postSwitch(switchInfo);
    return switchInfo == null ? null : switchInfo.newEndpoint;
  }

  public static MultiDbConnectionProvider.Database waitForInitializationPolicy(
      MultiDbConnectionProvider provider, StatusTracker statusTracker) {
    return provider.waitForInitializationPolicy(statusTracker);
  }
}
