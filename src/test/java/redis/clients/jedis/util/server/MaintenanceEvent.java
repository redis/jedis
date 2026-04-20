package redis.clients.jedis.util.server;

import java.util.Arrays;
import java.util.List;

/**
 * Utility for constructing maintenance event push messages for testing. These messages simulate
 * Redis server-initiated notifications about maintenance operations like migrations, failovers, and
 * endpoint moves.
 * <p>
 * Message format follows the latest Redis specification where:
 * <ul>
 * <li>Sequence numbers and time values are encoded as RESP3 integers</li>
 * <li>Shard IDs are encoded as a JSON array string</li>
 * </ul>
 * <p>
 * Usage example:
 *
 * <pre>
 * {
 *   &#64;code
 *   // Create MIGRATING push message
 *   Object[] migrating = MaintenanceEvent.migrating(6, 2, Arrays.asList("2", "4"));
 *   // Results in: ["MIGRATING", 6 (int), 2 (int), "[\"2\", \"4\"]" (string)]
 *
 *   // Create MOVING push message
 *   Object[] moving = MaintenanceEvent.moving(1, 30, "new-host:6380");
 * }
 * </pre>
 */
public class MaintenanceEvent {

  private MaintenanceEvent() {
    // Utility class - no instantiation
  }

  /**
   * Convert shard IDs to JSON array format.
   * @param shardIds list of shard IDs
   * @return JSON array string, e.g., ["1", "2", "3"]
   */
  private static String toJsonArray(List<String> shardIds) {
    if (shardIds == null || shardIds.isEmpty()) {
      return "[]";
    }
    StringBuilder json = new StringBuilder("[");
    for (int i = 0; i < shardIds.size(); i++) {
      if (i > 0) {
        json.append(", ");
      }
      json.append("\"").append(shardIds.get(i)).append("\"");
    }
    json.append("]");
    return json.toString();
  }

  /**
   * Create MIGRATING message: A shard migration is going to start within {@code timeSeconds}
   * seconds.
   * <p>
   * Format: MIGRATING {@code <seq_number> <time_s> <shard_id-s>}
   * <p>
   * Example: ["MIGRATING", 6 (Integer), 2 (Integer), "[\"2\", \"4\"]" (String)]
   * @param sequenceNumber sequence number (encoded as RESP3 integer)
   * @param timeSeconds time in seconds until migration starts (encoded as RESP3 integer)
   * @param shardIds list of shard IDs (encoded as JSON array string)
   * @return push message arguments [MIGRATING, seq (Integer), time (Integer), shardIds (JSON
   *         String)]
   */
  public static Object[] migrating(int sequenceNumber, int timeSeconds, List<String> shardIds) {
    return new Object[] { "MIGRATING", sequenceNumber, timeSeconds, toJsonArray(shardIds) };
  }

  /**
   * Create MIGRATED message: A shard migration ended.
   * <p>
   * Format: MIGRATED {@code <seq_number> <shard_id-s>}
   * <p>
   * Example: ["MIGRATED", 7 (Integer), "[\"2\", \"4\"]" (String)]
   * @param sequenceNumber sequence number (encoded as RESP3 integer)
   * @param shardIds list of shard IDs (encoded as JSON array string)
   * @return push message arguments [MIGRATED, seq (Integer), shardIds (JSON String)]
   */
  public static Object[] migrated(int sequenceNumber, List<String> shardIds) {
    return new Object[] { "MIGRATED", sequenceNumber, toJsonArray(shardIds) };
  }

  /**
   * Create FAILING_OVER message: A shard failover of a healthy shard started.
   * <p>
   * Format: FAILING_OVER {@code <seq_number> <time_s> <shard_id-s>}
   * <p>
   * Example: ["FAILING_OVER", 6 (Integer), 2 (Integer), "[\"2\", \"4\"]" (String)]
   * @param sequenceNumber sequence number (encoded as RESP3 integer)
   * @param timeSeconds time in seconds until failover starts (encoded as RESP3 integer)
   * @param shardIds list of shard IDs (encoded as JSON array string)
   * @return push message arguments [FAILING_OVER, seq (Integer), time (Integer), shardIds (JSON
   *         String)]
   */
  public static Object[] failingOver(int sequenceNumber, int timeSeconds, List<String> shardIds) {
    return new Object[] { "FAILING_OVER", sequenceNumber, timeSeconds, toJsonArray(shardIds) };
  }

  /**
   * Create FAILED_OVER message: A shard failover of a healthy shard ended.
   * <p>
   * Format: FAILED_OVER {@code <seq_number> <shard_id-s>}
   * <p>
   * Example: ["FAILED_OVER", 7 (Integer), "[\"2\", \"4\"]" (String)]
   * @param sequenceNumber sequence number (encoded as RESP3 integer)
   * @param shardIds list of shard IDs (encoded as JSON array string)
   * @return push message arguments [FAILED_OVER, seq (Integer), shardIds (JSON String)]
   */
  public static Object[] failedOver(int sequenceNumber, List<String> shardIds) {
    return new Object[] { "FAILED_OVER", sequenceNumber, toJsonArray(shardIds) };
  }

  /**
   * Create MOVING message: A specific endpoint is going to move to another node within
   * {@code timeSeconds} seconds.
   * <p>
   * Format: MOVING {@code <seq_number> <time_s> <endpoint>}
   * <p>
   * Example: ["MOVING", 30 (Integer), 15 (Integer), "localhost:6380" (String)]
   * @param sequenceNumber sequence number (encoded as RESP3 integer)
   * @param timeSeconds time in seconds until move (encoded as RESP3 integer)
   * @param endpoint target endpoint (host:port)
   * @return push message arguments [MOVING, seq (Integer), time (Integer), endpoint (String)]
   */
  public static Object[] moving(int sequenceNumber, int timeSeconds, String endpoint) {
    return new Object[] { "MOVING", sequenceNumber, timeSeconds, endpoint };
  }

}
