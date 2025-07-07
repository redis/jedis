package redis.clients.jedis;

import java.lang.ref.WeakReference;
import redis.clients.jedis.annots.Experimental;

/**
 * Implementation of {@link PushConsumer } that manages connection timeout relaxation
 * during Redis server maintenance events like migration and failover.
 */
@Experimental
public class AdaptiveTimeoutHandler implements PushConsumer {

  private final WeakReference<Connection> connectionRef;

  /**
   * Creates a new maintenance listener for the specified connection.
   *
   * @param connection The connection to manage timeouts for
   */
  public AdaptiveTimeoutHandler(Connection connection) {
    this.connectionRef = new WeakReference<>(connection);
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
    Connection connection = connectionRef.get();
    if (connection != null) {
      connection.relaxTimeouts();
    }
  }

  private void onMigrated() {
    Connection connection = connectionRef.get();
    if (connection != null) {
      connection.disableRelaxedTimeout();
    }
  }

  private void onFailOver() {
    Connection connection = connectionRef.get();
    if (connection != null) {
      connection.relaxTimeouts();
    }
  }

  private void onFailedOver() {
    Connection connection = connectionRef.get();
    if (connection != null) {
      connection.disableRelaxedTimeout();
    }
  }
}