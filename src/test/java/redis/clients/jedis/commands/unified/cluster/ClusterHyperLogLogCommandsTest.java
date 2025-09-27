package redis.clients.jedis.commands.unified.cluster;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.HyperLogLogCommandsTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class ClusterHyperLogLogCommandsTest extends HyperLogLogCommandsTestBase {

  public ClusterHyperLogLogCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return ClusterCommandsTestHelper.getCleanCluster(protocol);
  }

  @AfterEach
  public void tearDown() {
    ClusterCommandsTestHelper.clearClusterData();
  }

  @Test
  @Override
  public void pfcounts() {
    long status = jedis.pfadd("{hll}_1", "foo", "bar", "zap");
    assertEquals(1, status);
    status = jedis.pfadd("{hll}_2", "foo", "bar", "zap");
    assertEquals(1, status);

    status = jedis.pfadd("{hll}_3", "foo", "bar", "baz");
    assertEquals(1, status);
    status = jedis.pfcount("{hll}_1");
    assertEquals(3, status);
    status = jedis.pfcount("{hll}_2");
    assertEquals(3, status);
    status = jedis.pfcount("{hll}_3");
    assertEquals(3, status);

    status = jedis.pfcount("{hll}_1", "{hll}_2");
    assertEquals(3, status);

    status = jedis.pfcount("{hll}_1", "{hll}_2", "{hll}_3");
    assertEquals(4, status);
  }

  @Test
  @Override
  public void pfmerge() {
    long status = jedis.pfadd("{hll}1", "foo", "bar", "zap", "a");
    assertEquals(1, status);

    status = jedis.pfadd("{hll}2", "a", "b", "c", "foo");
    assertEquals(1, status);

    String mergeStatus = jedis.pfmerge("{hll}3", "{hll}1", "{hll}2");
    assertEquals("OK", mergeStatus);

    status = jedis.pfcount("{hll}3");
    assertEquals(6, status);
  }

  @Disabled
  @Override
  public void pfmergeBinary() {
  }
}
