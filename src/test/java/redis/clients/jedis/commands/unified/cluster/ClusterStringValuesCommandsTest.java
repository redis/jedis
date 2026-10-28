package redis.clients.jedis.commands.unified.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.redis.test.annotations.EnabledOnCommand;
import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.StringValuesCommandsTestBase;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.params.LCSParams;
import redis.clients.jedis.params.MSetExParams;

import redis.clients.jedis.resps.LCSMatchResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
@Tag("integration")
public class ClusterStringValuesCommandsTest extends StringValuesCommandsTestBase {

  public ClusterStringValuesCommandsTest(RedisProtocol protocol) {
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

  static Stream<Arguments> msetexCrossSlotParams() {
    return Stream.of(Arguments.of("unconditional", MSetExParams.setParams().ex(60)),
      Arguments.of("NX", MSetExParams.setParams().nx().ex(60)),
      Arguments.of("XX", MSetExParams.setParams().xx().ex(60)));
  }

  @ParameterizedTest(name = "MSETEX {0} rejects cross-slot keys")
  @MethodSource("msetexCrossSlotParams")
  @EnabledOnCommand("MSETEX")
  public void msetexRejectsCrossSlotKeysBeforeWrites(String condition, MSetExParams params) {
    String existing = "{a}msetex:existing";
    String missing = "{b}msetex:missing";
    jedis.set(existing, "old");

    assertThrows(JedisClusterOperationException.class,
      () -> jedis.msetex(params, missing, "new", existing, "new"));

    assertEquals("old", jedis.get(existing));
    assertEquals(-1L, jedis.ttl(existing));
    assertNull(jedis.get(missing));
  }

  static Stream<Arguments> msetexSameSlotConditions() {
    return Stream.of(Arguments.of("NX", MSetExParams.setParams().nx().ex(60)),
      Arguments.of("XX", MSetExParams.setParams().xx().ex(60)));
  }

  @ParameterizedTest(name = "MSETEX {0} checks all same-slot keys")
  @MethodSource("msetexSameSlotConditions")
  @EnabledOnCommand("MSETEX")
  public void msetexSameSlotConditionFailureLeavesKeysUnchanged(String condition,
      MSetExParams params) {
    String existing = "{a}msetex:existing";
    String missing = "{a}msetex:missing";
    jedis.set(existing, "old");

    assertFalse(jedis.msetex(params, missing, "new", existing, "new"));

    assertEquals("old", jedis.get(existing));
    assertEquals(-1L, jedis.ttl(existing));
    assertNull(jedis.get(missing));
  }

  @Test
  @EnabledOnCommand("MSETEX")
  public void msetexSameSlotWithoutConditionSetsAllKeys() {
    String k1 = "{a}msetex:first";
    String k2 = "{a}msetex:second";

    assertTrue(jedis.msetex(MSetExParams.setParams().ex(60), k1, "v1", k2, "v2"));

    assertEquals("v1", jedis.get(k1));
    assertEquals("v2", jedis.get(k2));
    assertTrue(jedis.ttl(k1) > 0);
    assertTrue(jedis.ttl(k2) > 0);
  }

}
