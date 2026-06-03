package redis.clients.jedis;

import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.providers.ClusterConnectionProvider;


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

  @Test
  public void refreshClusterTopology() {
    ClusterConnectionProvider provider = mock(ClusterConnectionProvider.class);
    JedisCluster cluster = new JedisCluster(provider, JedisCluster.DEFAULT_MAX_ATTEMPTS,
        Duration.ofMillis(JedisCluster.DEFAULT_TIMEOUT * JedisCluster.DEFAULT_MAX_ATTEMPTS));

    cluster.refreshClusterTopology();

    verify(provider).renewSlotCache();
  }

}
