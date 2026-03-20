package redis.clients.jedis.executors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import redis.clients.jedis.CommandFlagsRegistry;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.providers.ClusterConnectionProvider;

/**
 * Connection resolver for keyless commands that acquires connections in round-robin fashion.
 * <p>
 * This resolver distributes keyless commands evenly across cluster nodes using round-robin
 * selection. Read operations can go to any node for load distribution, while write operations go to
 * primary nodes only.
 */
final class RoundRobinConnectionResolver implements ConnectionResolver {

  private final ClusterConnectionProvider provider;
  private final CommandFlagsRegistry flags;
  private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

  RoundRobinConnectionResolver(ClusterConnectionProvider provider, CommandFlagsRegistry flags) {
    this.provider = provider;
    this.flags = flags;
  }

  @Override
  public Connection resolve(CommandObject<?> cmd) {
    ConnectionResolver.ConnectionIntent intent = getIntent(cmd, flags);
    List<Map.Entry<String, ConnectionPool>> nodeList = selectConnectionPool(intent);

    int size = nodeList.size();
    // Get and increment counter, then apply modulo with the current list size.
    // This handles the race condition where another thread may have updated the counter
    // based on a different (larger) node list size, which could result in an index
    // that is out of bounds for the current thread's smaller node list after topology change.
    int roundRobinIndex = Math.abs(roundRobinCounter.getAndIncrement() % size);
    Map.Entry<String, ConnectionPool> selectedEntry = nodeList.get(roundRobinIndex);
    ConnectionPool pool = selectedEntry.getValue();

    return pool.getResource();
  }

  private List<Map.Entry<String, ConnectionPool>> selectConnectionPool(
      ConnectionResolver.ConnectionIntent intent) {
    Map<String, ConnectionPool> connectionMap;

    if (intent == ConnectionResolver.ConnectionIntent.READ) {
      // For keyless READ commands, use all nodes for load distribution
      connectionMap = provider.getConnectionMap();
    } else {
      // Write operations always go to primary nodes
      connectionMap = provider.getPrimaryNodesConnectionMap();
    }

    if (connectionMap.isEmpty()) {
      throw new JedisClusterOperationException("No cluster nodes available.");
    }

    return new ArrayList<>(connectionMap.entrySet());
  }
}
