package redis.clients.jedis.commands.unified.cluster;

import java.util.ArrayList;
import java.util.List;

import io.redis.test.annotations.EnabledOnCommand;
import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.StringValuesCommandsTestBase;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.params.LCSParams;
import redis.clients.jedis.params.MSetExParams;

import redis.clients.jedis.resps.LCSMatchResult;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.RedisVersionCondition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
@Tag("integration")
public class ClusterStringValuesCommandsTest extends StringValuesCommandsTestBase {

  private static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("cluster-stable");

  public ClusterStringValuesCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return ClusterCommandsTestHelper.getCleanCluster(protocol);
  }

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(
      endpoint.getHostsAndPorts().get(0), endpoint.getClientConfigBuilder().build());
  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(
      endpoint.getHostsAndPorts().get(0), endpoint.getClientConfigBuilder().build());

  @AfterEach
  public void tearDown() {
    ClusterCommandsTestHelper.clearClusterData();
  }

  @Test
  @Override
  public void mget() {
    List<String> values = jedis.mget("foo{^}", "bar{^}");
    List<String> expected = new ArrayList<>();
    expected.add(null);
    expected.add(null);

    assertEquals(expected, values);

    jedis.set("foo{^}", "bar");

    expected = new ArrayList<>();
    expected.add("bar");
    expected.add(null);
    values = jedis.mget("foo{^}", "bar{^}");

    assertEquals(expected, values);

    jedis.set("bar{^}", "foo");

    expected = new ArrayList<>();
    expected.add("bar");
    expected.add("foo");
    values = jedis.mget("foo{^}", "bar{^}");

    assertEquals(expected, values);
  }

  @Test
  @Override
  public void mset() {
    String status = jedis.mset("{^}foo", "bar", "{^}bar", "foo");
    assertEquals("OK", status);
    assertEquals("bar", jedis.get("{^}foo"));
    assertEquals("foo", jedis.get("{^}bar"));
  }

  @Test
  @Override
  public void msetnx() {
    assertEquals(1, jedis.msetnx("{^}foo", "bar", "{^}bar", "foo"));
    assertEquals("bar", jedis.get("{^}foo"));
    assertEquals("foo", jedis.get("{^}bar"));

    assertEquals(0, jedis.msetnx("{^}foo", "bar1", "{^}bar2", "foo2"));
    assertEquals("bar", jedis.get("{^}foo"));
    assertEquals("foo", jedis.get("{^}bar"));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void lcs() {
    jedis.mset("key1{.}", "ohmytext", "key2{.}", "mynewtext");

    LCSMatchResult stringMatchResult = jedis.lcs("key1{.}", "key2{.}", LCSParams.LCSParams());
    assertEquals("mytext", stringMatchResult.getMatchString());

    stringMatchResult = jedis.lcs("key1{.}", "key2{.}", LCSParams.LCSParams().idx().withMatchLen());
    assertEquals(stringMatchResult.getLen(), 6);
    assertEquals(2, stringMatchResult.getMatches().size());
    stringMatchResult = jedis.lcs("key1{.}", "key2{.}",
      LCSParams.LCSParams().idx().minMatchLen(10));
    assertEquals(0, stringMatchResult.getMatches().size());
  }

  @Test
  @EnabledOnCommand("MSETEX")
  public void msetex_crossslot_throws_server_error() {
    // Intentionally use keys without a hashtag so they map to different hash slots
    String k1 = "cross:k1";
    String k2 = "other:k2";

    MSetExParams params = new MSetExParams().nx().ex(5);

    JedisClusterOperationException ex = assertThrows(JedisClusterOperationException.class,
      () -> jedis.msetex(params, k1, "v1", k2, "v2"));
    assertTrue(ex.getMessage().contains("Keys must belong to same hashslot"),
      () -> "Expected server \" Keys must belong to same hashslot \" error, but got: "
          + ex.getMessage());
  }

}
