package redis.clients.jedis.commands;

import redis.clients.jedis.params.HotkeysParams;
import redis.clients.jedis.resps.HotkeysInfo;

/**
 * Commands for Redis hotkeys tracking.
 * <p>
 * These commands track hot keys on a specific Redis node and are only available for single-node
 * connections (e.g., {@code Jedis}). They are NOT suitable for cluster connections because:
 * <ul>
 * <li>Hotkey data is collected and stored per-node</li>
 * <li>Each node tracks its own key access patterns independently</li>
 * <li>Results cannot be meaningfully aggregated across cluster nodes</li>
 * </ul>
 * <p>
 * <b>Note: This command is not supported in Redis Cluster mode.</b> For cluster deployments, hotkey
 * tracking should be performed by connecting directly to individual nodes.
 * @see ServerCommands for the full set of server commands
 */
public interface HotkeysCommands {

  /**
   * Start hotkey tracking with the specified parameters.
   * <p>
   * The HOTKEYS command is used to identify hot keys in your Redis instance by tracking keys by CPU
   * time consumption and network bytes transferred.
   * <p>
   * <b>Note: This command is not supported in Redis Cluster mode.</b> HOTKEYS is a node-local
   * operation and there is no built-in mechanism to aggregate hotkeys data across cluster nodes.
   * Calling this method on a cluster client will throw {@link UnsupportedOperationException}. To
   * use HOTKEYS in a cluster, connect directly to individual nodes.
   * @param params the parameters for hotkey tracking (metrics, count, duration, sample)
   * @return OK
   * @throws UnsupportedOperationException if called on a cluster client
   */
  String hotkeysStart(HotkeysParams params);

  /**
   * Stop hotkey tracking. Data is preserved until RESET or next START.
   * <p>
   * <b>Note: This command is not supported in Redis Cluster mode.</b> See
   * {@link #hotkeysStart(HotkeysParams)} for details.
   * @return OK
   * @throws UnsupportedOperationException if called on a cluster client
   */
  String hotkeysStop();

  /**
   * Clear all collected hotkey data and return to empty state.
   * <p>
   * <b>Note: This command is not supported in Redis Cluster mode.</b> See
   * {@link #hotkeysStart(HotkeysParams)} for details.
   * @return OK
   * @throws UnsupportedOperationException if called on a cluster client
   */
  String hotkeysReset();

  /**
   * Retrieve collected hotkey statistics.
   * <p>
   * Returns null if hotkey tracking has never been started.
   * <p>
   * <b>Note: This command is not supported in Redis Cluster mode.</b> See
   * {@link #hotkeysStart(HotkeysParams)} for details.
   * @return HotkeysInfo containing tracking statistics, or null if never started
   * @throws UnsupportedOperationException if called on a cluster client
   */
  HotkeysInfo hotkeysGet();
}
