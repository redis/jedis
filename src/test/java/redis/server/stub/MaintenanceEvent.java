package redis.server.stub;

/**
 * Utility for constructing maintenance event push messages for testing. These messages simulate
 * Redis server-initiated notifications about maintenance operations like migrations, failovers, and
 * endpoint moves.
 * <p>
 * Usage example:
 *
 * <pre>
 * {
 *   &#64;code
 *   // Create and inject MIGRATING push message
 *   String[] migrating = MaintenanceEvent.migrating(1, 15, "shard-1", "shard-2");
 *   server.injectPushMessage(client, migrating);
 *
 *   // Create and inject MOVING push message
 *   String[] moving = MaintenanceEvent.moving(1, 30, "new-host:6380");
 *   server.injectPushMessage(client, moving);
 * }
 * </pre>
 */
public class MaintenanceEvent {

  private MaintenanceEvent() {
    // Utility class - no instantiation
  }

  /**
   * Create MIGRATING message: A shard migration is going to start within {@code timeSeconds}
   * seconds.
   * <p>
   * Format: MIGRATING {@code <seq_number> <time> <shard_id-s>}
   * @param sequenceNumber sequence number
   * @param timeSeconds time in seconds until migration starts
   * @param shardIds shard IDs (varargs, can be multiple)
   * @return push message arguments [MIGRATING, seq, time, shard1, shard2, ...]
   */
  public static String[] migrating(int sequenceNumber, int timeSeconds, String... shardIds) {
    String[] result = new String[3 + shardIds.length];
    result[0] = "MIGRATING";
    result[1] = String.valueOf(sequenceNumber);
    result[2] = String.valueOf(timeSeconds);
    System.arraycopy(shardIds, 0, result, 3, shardIds.length);
    return result;
  }

  /**
   * Create MIGRATED message: A shard migration ended.
   * <p>
   * Format: MIGRATED {@code <seq_number> <shard_id-s>}
   * @param sequenceNumber sequence number
   * @param shardIds shard IDs (varargs, can be multiple)
   * @return push message arguments [MIGRATED, seq, shard1, shard2, ...]
   */
  public static String[] migrated(int sequenceNumber, String... shardIds) {
    String[] result = new String[2 + shardIds.length];
    result[0] = "MIGRATED";
    result[1] = String.valueOf(sequenceNumber);
    System.arraycopy(shardIds, 0, result, 2, shardIds.length);
    return result;
  }

  /**
   * Create FAILING_OVER message: A shard failover of a healthy shard started.
   * <p>
   * Format: FAILING_OVER {@code <seq_number> <time> <shard_id-s>}
   * @param sequenceNumber sequence number
   * @param timeSeconds time in seconds until failover starts
   * @param shardIds shard IDs (varargs, can be multiple)
   * @return push message arguments [FAILING_OVER, seq, time, shard1, shard2, ...]
   */
  public static String[] failingOver(int sequenceNumber, int timeSeconds, String... shardIds) {
    String[] result = new String[3 + shardIds.length];
    result[0] = "FAILING_OVER";
    result[1] = String.valueOf(sequenceNumber);
    result[2] = String.valueOf(timeSeconds);
    System.arraycopy(shardIds, 0, result, 3, shardIds.length);
    return result;
  }

  /**
   * Create FAILED_OVER message: A shard failover of a healthy shard ended.
   * <p>
   * Format: FAILED_OVER {@code <seq_number> <shard_id-s>}
   * @param sequenceNumber sequence number
   * @param shardIds shard IDs (varargs, can be multiple)
   * @return push message arguments [FAILED_OVER, seq, shard1, shard2, ...]
   */
  public static String[] failedOver(int sequenceNumber, String... shardIds) {
    String[] result = new String[2 + shardIds.length];
    result[0] = "FAILED_OVER";
    result[1] = String.valueOf(sequenceNumber);
    System.arraycopy(shardIds, 0, result, 2, shardIds.length);
    return result;
  }

  /**
   * Create MOVING message: A specific endpoint is going to move to another node within
   * {@code timeSeconds} seconds.
   * <p>
   * Format: MOVING {@code <seq_number> <time> <endpoint>}
   * @param sequenceNumber sequence number
   * @param timeSeconds time in seconds until move
   * @param endpoint target endpoint (host:port)
   * @return push message arguments [MOVING, seq, time, endpoint]
   */
  public static String[] moving(int sequenceNumber, int timeSeconds, String endpoint) {
    return new String[] { "MOVING", String.valueOf(sequenceNumber), String.valueOf(timeSeconds),
        endpoint };
  }

}
