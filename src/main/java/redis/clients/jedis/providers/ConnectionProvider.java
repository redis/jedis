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
   * Connection map of the primary nodes only. Defaults to {@link #getConnectionMap()}, for
   * providers that do not distinguish primaries from replicas.
   */
  default Map<?, ?> getPrimaryNodesConnectionMap() {
    return getConnectionMap();
  }
}
