package redis.clients.jedis.commands.unified.cluster;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.HashesCommandsTestBase;
import redis.clients.jedis.util.EnabledOnCommandRule;
import redis.clients.jedis.util.RedisVersionRule;

@RunWith(Parameterized.class)
public class ClusterHashesCommandsTest extends HashesCommandsTestBase {

  @Rule
  public RedisVersionRule versionRule = new RedisVersionRule(
          HostAndPorts.getStableClusterServers().get(0),
          DefaultJedisClientConfig.builder().password("cluster") .build());
  @Rule
  public EnabledOnCommandRule enabledOnCommandRule = new EnabledOnCommandRule(
          HostAndPorts.getStableClusterServers().get(0),
          DefaultJedisClientConfig.builder().password("cluster").build());

  public ClusterHashesCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Before
  public void setUp() {
    jedis = ClusterCommandsTestHelper.getCleanCluster(protocol);
  }

  @After
  public void tearDown() {
    jedis.close();
    ClusterCommandsTestHelper.clearClusterData();
  }
}
