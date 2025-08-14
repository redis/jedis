package redis.clients.jedis.commands.unified.cluster;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.FunctionCommandsTestBase;
import redis.clients.jedis.exceptions.JedisBroadcastException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.RedisVersionCondition;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class ClusterFunctionCommandsTest extends FunctionCommandsTestBase {

  public ClusterFunctionCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(
      HostAndPorts.getStableClusterServers().get(0),
      DefaultJedisClientConfig.builder().password("cluster").build());
  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(
      HostAndPorts.getStableClusterServers().get(0),
      DefaultJedisClientConfig.builder().password("cluster").build());

  @Override
  protected UnifiedJedis createTestClient() {
    return ClusterCommandsTestHelper.getCleanCluster(protocol);
  }

  @AfterEach
  public void tearDown() {
    ClusterCommandsTestHelper.clearClusterData();
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  @Override
  public void testFunctionKill() {
    JedisException e = assertThrows(JedisException.class, () -> jedis.functionKill());
    assertThat(e, instanceOf(JedisBroadcastException.class));
    JedisBroadcastException jbe = (JedisBroadcastException) e;
    List<String> replies = jbe.getReplies().values().stream().map(e1 -> (Exception) e1)
        .map(Exception::getMessage).collect(Collectors.toList());
    assertThat(replies.size(), equalTo(3));
    assertThat(replies, everyItem(containsString("No scripts in execution right now")));
  }

}
