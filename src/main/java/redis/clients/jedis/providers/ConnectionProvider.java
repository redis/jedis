package redis.clients.jedis.providers;

import java.util.Collections;
import java.util.Map;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
import redis.clients.jedis.csc.Cache;

public interface ConnectionProvider extends AutoCloseable {

  Connection getConnection();

  Connection getConnection(CommandArguments args);

  default Cache getCache() {
    return null;
  }

  default Map<?, ?> getConnectionMap() {
    final Connection c = getConnection();
    return Collections.singletonMap(c.toString(), c);
  }

  default Map<?, ?> getPrimaryNodesConnectionMap() {
    final Connection c = getConnection();
    return Collections.singletonMap(c.toString(), c);
  }
}
