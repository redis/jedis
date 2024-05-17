package redis.clients.jedis;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.providers.ManagedConnectionProvider;
import redis.clients.jedis.util.IOUtils;

public class ManagedConnectionProviderTest {

  private Connection connection;

  @Before
  public void setUp() {
    EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0");
    connection = new Connection(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build());
  }

  @After
  public void tearDown() {
    IOUtils.closeQuietly(connection);
  }

  @Test
  public void test() {
    ManagedConnectionProvider managed = new ManagedConnectionProvider();
    try (UnifiedJedis jedis = new UnifiedJedis(managed)) {
      try {
        jedis.get("any");
        Assert.fail("Should get NPE.");
      } catch (NullPointerException npe) { }
      managed.setConnection(connection);
      Assert.assertNull(jedis.get("any"));
    }
  }
}
