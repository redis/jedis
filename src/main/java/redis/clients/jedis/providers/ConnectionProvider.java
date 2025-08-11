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

  default Map<?, ?> getPrimaryNodesConnectionMap() {
    final Connection c = getConnection();
    return Collections.singletonMap(c.toString(), c);
  }
}
