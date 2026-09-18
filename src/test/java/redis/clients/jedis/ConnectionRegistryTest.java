package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ConnectionRegistryTest {

  @Test
  public void visitsRegisteredLiveConnections() {
    ConnectionRegistry registry = new ConnectionRegistry();
    Connection a = new Connection();
    Connection b = new Connection();
    registry.register(a);
    registry.register(b);

    List<Connection> visited = new ArrayList<>();
    registry.forEachLive(visited::add);
    assertEquals(2, visited.size());
  }
}