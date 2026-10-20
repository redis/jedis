package redis.clients.jedis.providers;

import java.util.Collections;
import java.util.Map;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;

public interface ConnectionProvider extends AutoCloseable {

  Connection getConnection();

  Connection getConnection(CommandArguments args);

  default Map<?, ?> getConnectionMap() {
    final Connection c = getConnection();
    return Collections.singletonMap(c.toString(), c);
  }

  /**
   * Connections to the primary nodes only. Providers that serve replicas (clusters) return a map
   * without them, so commands that must observe every piece of data once (such as an iteration over
   * all keys) can use it.
   * <p>
   * The default implementation delegates to {@link #getConnectionMap()}, which keeps custom
   * providers that only override that method working: they keep reporting all of their connections
   * instead of falling back to a single connection obtained from {@link #getConnection()}.
   * @return a map of connections to the primary nodes
   */
  default Map<?, ?> getPrimaryNodesConnectionMap() {
    return getConnectionMap();
  }
}
