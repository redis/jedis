package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;

import redis.clients.jedis.args.LatencyEvent;
import redis.clients.jedis.resps.LatencyHistoryInfo;
import redis.clients.jedis.resps.LatencyLatestInfo;

/**
 * Commands for Redis latency monitoring.
 * <p>
 * These commands operate on node-local latency data and are only available for single-node
 * connections (e.g., {@code Jedis}). They are NOT suitable for pooled or cluster connections
 * because:
 * <ul>
 * <li>Latency data is collected and stored per-node</li>
 * <li>Each node tracks its own latency events independently</li>
 * <li>Results cannot be meaningfully aggregated across nodes</li>
 * </ul>
 * <p>
 * For cluster deployments, latency monitoring should be performed by connecting directly to
 * individual nodes.
 * @see ServerCommands for the full set of server commands
 */
public interface LatencyCommands {

  /**
   * The LATENCY DOCTOR command reports about different latency-related issues and advises about
   * possible remedies.
   * <p>
   * This command is the most powerful analysis tool in the latency monitoring framework, and is
   * able to provide additional statistical data like the average period between latency spikes, the
   * median deviation, and a human-readable analysis of the event. For certain events, like fork,
   * additional information is provided, like the rate at which the system forks processes.
   * <p>
   * This is the output you should post in the Redis mailing list if you are looking for help about
   * Latency related issues.
   * @return the report
   */
  String latencyDoctor();

  /**
   * Returns the latest latency samples for all events.
   * @return a map of event names to their latest latency information
   */
  Map<String, LatencyLatestInfo> latencyLatest();

  /**
   * Returns the latency history for the specified event.
   * @param events the latency event type
   * @return a list of latency history entries
   */
  List<LatencyHistoryInfo> latencyHistory(LatencyEvent events);

  /**
   * Resets the latency data for the specified events. If no events are specified, all events are
   * reset.
   * @param events the events to reset (optional)
   * @return the number of event time series that were reset
   */
  long latencyReset(LatencyEvent... events);
}
