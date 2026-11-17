package redis.clients.jedis.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;

class ConnectionProviderTest {

  /** A provider that does not override getPrimaryNodesConnectionMap() exposes the same nodes. */
  @Test
  void primaryNodesConnectionMapDefaultsToConnectionMap() {
    Map<String, Object> nodes = Collections.singletonMap("node", new Object());
    ConnectionProvider provider = new ConnectionProvider() {
      @Override
      public Connection getConnection() {
        throw new UnsupportedOperationException();
      }

      @Override
      public Connection getConnection(CommandArguments args) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Map<?, ?> getConnectionMap() {
        return nodes;
      }

      @Override
      public void close() {
      }
    };

    assertEquals(nodes, provider.getPrimaryNodesConnectionMap());
  }
}
