package redis.clients.jedis.mcf;

import redis.clients.jedis.Endpoint;

public class HealthStatusChangeEvent {

  private final Endpoint endpoint;
  private final HealthStatus oldStatus;
  private final HealthStatus newStatus;

  public HealthStatusChangeEvent(Endpoint endpoint, HealthStatus oldStatus,
      HealthStatus newStatus) {
    this.endpoint = endpoint;
    this.oldStatus = oldStatus;
    this.newStatus = newStatus;
  }

  public Endpoint getEndpoint() {
    return endpoint;
  }

  public HealthStatus getOldStatus() {
    return oldStatus;
  }

  public HealthStatus getNewStatus() {
    return newStatus;
  }
}
