package redis.clients.jedis.sentinel.api;

/**
 * Client interface for connecting to Redis Sentinel instances.
 * <p>
 * This interface provides direct access to Sentinel administrative commands for monitoring and
 * managing Redis Sentinel deployments. It extends {@link SentinelNodeCommands} to provide all
 * Sentinel-specific operations.
 * </p>
 * <p>
 * Usage example:
 * </p>
 * 
 * <pre>
 * try (SentinelInstanceClient client = ...) {
 *     // Direct access to Sentinel commands
 *     List&lt;Map&lt;String, String&gt;&gt; masters = client.sentinelMasters();
 *     String myId = client.sentinelMyId();
 *     client.sentinelFailover("mymaster");
 * }
 * </pre>
 *
 * @see SentinelNodeCommands
 * @since 7.5.0
 */
public interface SentinelInstanceClient extends SentinelNodeCommands, AutoCloseable {
}
