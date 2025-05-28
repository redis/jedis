package redis.clients.jedis;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.providers.ManagedConnectionProvider;
import redis.clients.jedis.util.IOUtils;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

public class ManagedConnectionProviderTest {

  private Connection connection;

  @BeforeEach
  public void setUp() {
    EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0");
    connection = new Connection(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build());
  }

  @AfterEach
  public void tearDown() {
    IOUtils.closeQuietly(connection);
  }

  @Test
  public void test() {
    ManagedConnectionProvider managed = new ManagedConnectionProvider();
    try (UnifiedJedis jedis = new UnifiedJedis(managed)) {
      try {
        jedis.get("any");
        fail("Should get NPE.");
      } catch (NullPointerException npe) { }
      managed.setConnection(connection);
      assertNull(jedis.get("any"));
    }
  }
}
