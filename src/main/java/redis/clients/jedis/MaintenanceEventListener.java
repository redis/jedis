package redis.clients.jedis;

import java.time.Duration;

public interface MaintenanceEventListener {

  default void onMigrating() {
  };

  default void onMigrated() {
  };

  default void onFailOver() {
  };

  default void onFailedOver() {
  };

  default void onRebind(HostAndPort target, Duration rebindTimeout) {
  };
}