package redis.clients.jedis;

/**
 * Implementation of MaintenanceListener that manages connection timeout relaxation
 * during Redis server maintenance events like migration and failover.
 */
public class AdaptiveTimeoutHandler implements PushConsumer {

  Connection connection;


  /**
   * Creates a new maintenance listener for the specified connection.
   * 
   * @param connection The connection to manage timeouts for
   */
  public AdaptiveTimeoutHandler(Connection connection) {
    this.connection = connection;
  }

  @Override
  public void accept(PushConsumerContext context) {
    String type = context.getMessage().getType();

    switch (type) {
    case "MIGRATING":
      onMigrating();
      break;
    case "MIGRATED":
      onMigrated();;
      break;
    case "FAILING_OVER":
      onFailOver();
      break;
    case "FAILED_OVER":
      onFailedOver();
      break;
    }
  }

  private void onMigrating() {
    connection.relaxTimeouts();
  }

  private void onMigrated() {
    connection.disableRelaxedTimeout();
  }

  private void onFailOver() {
    connection.relaxTimeouts();
  }

  private void onFailedOver() {
    connection.disableRelaxedTimeout();
  }
}