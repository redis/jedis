package redis.clients.jedis.commands.unified.cluster;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.RedisVersionCondition;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.BitOP;
import redis.clients.jedis.commands.unified.BitCommandsTestBase;
import redis.clients.jedis.exceptions.JedisDataException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class ClusterBitCommandsTest extends BitCommandsTestBase {

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(
            HostAndPorts.getStableClusterServers().get(0),
            DefaultJedisClientConfig.builder().password("cluster").build());
  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(
          HostAndPorts.getStableClusterServers().get(0),
          DefaultJedisClientConfig.builder().password("cluster").build());

  public ClusterBitCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @BeforeEach
  public void setUp() {
    jedis = ClusterCommandsTestHelper.getCleanCluster(protocol);
  }

  @AfterEach
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

  @Disabled
  @Override
  public void bitOpBinary() {
  }

  @Test
  @Override
  public void bitOpNotMultiSourceShouldFail() {
    assertThrows(JedisDataException.class,
        () -> jedis.bitop(BitOP.NOT, "{!}dest", "{!}src1", "{!}src2"));
  }

}
