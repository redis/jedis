package redis.clients.jedis.mcf;

import redis.clients.jedis.Endpoint;
import redis.clients.jedis.mcf.MultiDbConnectionProvider.Database;

public class DatabaseSwitchEvent {

  private final SwitchReason reason;
  private final String databaseName;
  private final Endpoint endpoint;

  public DatabaseSwitchEvent(SwitchReason reason, Endpoint endpoint, Database database) {
    this.reason = reason;
    this.databaseName = database.getCircuitBreaker().getName();
    this.endpoint = endpoint;
  }

  public SwitchReason getReason() {
    return reason;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public Endpoint getEndpoint() {
    return endpoint;
  }

}
