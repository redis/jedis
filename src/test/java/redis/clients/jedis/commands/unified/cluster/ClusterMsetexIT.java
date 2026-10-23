package redis.clients.jedis.commands.unified.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.redis.test.annotations.EnabledOnCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisClusterClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.params.MSetExParams;
import redis.clients.jedis.util.EnabledOnCommandCondition;

/**
 * Integration tests for {@code msetex} on a {@link RedisClusterClient} with keys in several hash
 * slots.
 */
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class ClusterMsetexIT {

  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(
      () -> Endpoints.getRedisEndpoint("cluster-stable"));

  private final RedisProtocol protocol;
  private RedisClusterClient cluster;

  public ClusterMsetexIT(RedisProtocol protocol) {
    this.protocol = protocol;
  }

  @BeforeEach
  public void setUp() {
    cluster = ClusterCommandsTestHelper.getCleanCluster(protocol);
  }

  @AfterEach
  public void tearDown() {
    cluster.flushAll();
    cluster.close();
  }

  /** Regression test for #4806. */
  @Test
  @EnabledOnCommand("MSETEX")
  public void msetexReturnsFalseIfTheConditionFailsInAnySlot() {
    String k1 = "cross:k1";
    String k2 = "other:k2";
    cluster.set(k2, "existing");

    // NX holds in the first slot only
    assertFalse(cluster.msetex(new MSetExParams().nx(), k1, "v1", k2, "v2"));

    // The condition is checked per slot, so the first slot is still set
    assertEquals("v1", cluster.get(k1));
    assertEquals("existing", cluster.get(k2));
  }
}
