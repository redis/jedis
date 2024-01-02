package redis.clients.jedis.misc;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.AbstractPipeline;
import redis.clients.jedis.AbstractTransaction;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiClusterClientConfig;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;

public class AutomaticFailoverTest {

  private final HostAndPort hostAndPort1 = HostAndPorts.getRedisServers().get(0);
  private final HostAndPort hostAndPort2 = HostAndPorts.getRedisServers().get(1);

  private final JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().password("foobared").build();

  private Jedis jedis1;
  private Jedis jedis2;

  private MultiClusterPooledConnectionProvider provider;

  @Before
  public void setUp() {

    MultiClusterClientConfig.ClusterConfig[] clusterConfigs = new MultiClusterClientConfig.ClusterConfig[2];
    clusterConfigs[0] = new MultiClusterClientConfig.ClusterConfig(hostAndPort1, clientConfig);
    clusterConfigs[1] = new MultiClusterClientConfig.ClusterConfig(hostAndPort2, clientConfig);

    provider = new MultiClusterPooledConnectionProvider(new MultiClusterClientConfig.Builder(clusterConfigs).build());

    jedis1 = new Jedis(hostAndPort1, clientConfig);
    jedis1.flushAll();
    jedis2 = new Jedis(hostAndPort2, clientConfig);
    jedis2.flushAll();
  }

  @After
  public void cleanUp() {

    provider.close();

    jedis1.close();
    jedis2.close();
  }

  @Test
  public void pipelineWithSwitch() {
    try (UnifiedJedis client = new UnifiedJedis(provider)) {
      AbstractPipeline pipe = client.pipelined();
      pipe.set("pstr", "foobar");
      pipe.hset("phash", "foo", "bar");
      provider.incrementActiveMultiClusterIndex();
      pipe.sync();
    }

    assertEquals("foobar", jedis2.get("pstr"));
    assertEquals("bar", jedis2.hget("phash", "foo"));
  }

  @Test
  public void transactionWithSwitch() {
    try (UnifiedJedis client = new UnifiedJedis(provider)) {
      AbstractTransaction tx = client.multi();
      tx.set("tstr", "foobar");
      tx.hset("thash", "foo", "bar");
      provider.incrementActiveMultiClusterIndex();
      assertEquals(Arrays.asList("OK", Long.valueOf(1L)), tx.exec());
    }

    assertEquals("foobar", jedis2.get("tstr"));
    assertEquals("bar", jedis2.hget("thash", "foo"));
  }
}
