package redis.clients.jedis.commands.unified.cluster;

import static org.junit.Assert.assertEquals;

import redis.clients.jedis.util.EnabledOnCommandRule;
import redis.clients.jedis.util.RedisVersionRule;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.BitOP;
import redis.clients.jedis.commands.unified.BitCommandsTestBase;
import redis.clients.jedis.exceptions.JedisDataException;

@RunWith(Parameterized.class)
public class ClusterBitCommandsTest extends BitCommandsTestBase {

  @Rule
  public RedisVersionRule versionRule = new RedisVersionRule(
            HostAndPorts.getStableClusterServers().get(0),
            DefaultJedisClientConfig.builder().password("cluster").build());
  @Rule
  public EnabledOnCommandRule enabledOnCommandRule = new EnabledOnCommandRule(
          HostAndPorts.getStableClusterServers().get(0),
          DefaultJedisClientConfig.builder().password("cluster").build());

  public ClusterBitCommandsTest(RedisProtocol protocol) {
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

  @Test
  @Override
  public void bitOp() {
    jedis.set("{key}1", "\u0060");
    jedis.set("{key}2", "\u0044");

    jedis.bitop(BitOP.AND, "resultAnd{key}", "{key}1", "{key}2");
    String resultAnd = jedis.get("resultAnd{key}");
    assertEquals("\u0040", resultAnd);

    jedis.bitop(BitOP.OR, "resultOr{key}", "{key}1", "{key}2");
    String resultOr = jedis.get("resultOr{key}");
    assertEquals("\u0064", resultOr);

    jedis.bitop(BitOP.XOR, "resultXor{key}", "{key}1", "{key}2");
    String resultXor = jedis.get("resultXor{key}");
    assertEquals("\u0024", resultXor);
  }

  @Test
  @Override
  public void bitOpNot() {
    jedis.setbit("key", 0, true);
    jedis.setbit("key", 4, true);

    jedis.bitop(BitOP.NOT, "resultNot{key}", "key");
    String resultNot = jedis.get("resultNot{key}");
    assertEquals("\u0077", resultNot);
  }

  @Ignore
  @Override
  public void bitOpBinary() {
  }

  @Test(expected = JedisDataException.class)
  @Override
  public void bitOpNotMultiSourceShouldFail() {
    jedis.bitop(BitOP.NOT, "{!}dest", "{!}src1", "{!}src2");
  }

}
