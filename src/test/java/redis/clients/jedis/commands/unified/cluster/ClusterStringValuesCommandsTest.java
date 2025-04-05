package redis.clients.jedis.commands.unified.cluster;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.StringValuesCommandsTestBase;
import redis.clients.jedis.params.LCSParams;
import redis.clients.jedis.resps.LCSMatchResult;
import redis.clients.jedis.util.EnabledOnCommandRule;
import redis.clients.jedis.util.RedisVersionRule;

@RunWith(Parameterized.class)
public class ClusterStringValuesCommandsTest extends StringValuesCommandsTestBase {

  public ClusterStringValuesCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Rule
  public RedisVersionRule versionRule = new RedisVersionRule(
          HostAndPorts.getStableClusterServers().get(0),
          DefaultJedisClientConfig.builder().password("cluster").build());
  @Rule
  public EnabledOnCommandRule enabledOnCommandRule = new EnabledOnCommandRule(
          HostAndPorts.getStableClusterServers().get(0),
          DefaultJedisClientConfig.builder().password("cluster").build());

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
  @SinceRedisVersion(value="7.0.0")
  public void lcs() {
    jedis.mset("key1{.}", "ohmytext", "key2{.}", "mynewtext");

    LCSMatchResult stringMatchResult = jedis.lcs("key1{.}", "key2{.}",
        LCSParams.LCSParams());
    assertEquals("mytext", stringMatchResult.getMatchString());

    stringMatchResult = jedis.lcs("key1{.}", "key2{.}",
        LCSParams.LCSParams().idx().withMatchLen());
    assertEquals(stringMatchResult.getLen(), 6);
    assertEquals(2, stringMatchResult.getMatches().size());

    stringMatchResult = jedis.lcs("key1{.}", "key2{.}",
        LCSParams.LCSParams().idx().minMatchLen(10));
    assertEquals(0, stringMatchResult.getMatches().size());
  }

}
