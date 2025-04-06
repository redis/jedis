package redis.clients.jedis;

import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.exceptions.JedisClusterOperationException;

public class JedisClusterWithoutSetupTest {

  @Test
  public void noStartNodes() {
    JedisClusterOperationException operationException = assertThrows(
        JedisClusterOperationException.class, () -> new JedisCluster(emptySet()));
    assertEquals("No nodes to initialize cluster slots cache.", operationException.getMessage());
    assertEquals(0, operationException.getSuppressed().length);
  }

  @Test
  public void uselessStartNodes() {
    JedisClusterOperationException operationException = assertThrows(
        JedisClusterOperationException.class,
        () -> new JedisCluster(new HostAndPort("localhost", 7378)));
    assertEquals("Could not initialize cluster slots cache.", operationException.getMessage());
    assertEquals(1, operationException.getSuppressed().length);
  }

}
