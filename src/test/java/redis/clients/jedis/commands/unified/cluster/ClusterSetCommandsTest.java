package redis.clients.jedis.commands.unified.cluster;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.SetCommandsTestBase;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.SDiffCardParams;
import redis.clients.jedis.params.SUnionCardParams;

import static io.redis.test.utils.RedisVersion.V8_10_0_RC2_STRING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
@Tag("integration")
public class ClusterSetCommandsTest extends SetCommandsTestBase {

  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] bfoo_same_hashslot = { 0x01, 0x02, 0x03, 0x04, 0x03, 0x00, 0x03, 0x1b };
  final byte[] ba = { 0x0A };
  final byte[] bb = { 0x0B };
  final byte[] bc = { 0x0C };

  public ClusterSetCommandsTest(RedisProtocol protocol) {
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
  public void smove() {
    jedis.sadd("{.}foo", "a");
    jedis.sadd("{.}foo", "b");

    jedis.sadd("{.}bar", "c");

    long status = jedis.smove("{.}foo", "{.}bar", "a");
    assertEquals(status, 1);

    Set<String> expectedSrc = new HashSet<>();
    expectedSrc.add("b");

    Set<String> expectedDst = new HashSet<>();
    expectedDst.add("c");
    expectedDst.add("a");

    assertEquals(expectedSrc, jedis.smembers("{.}foo"));
    assertEquals(expectedDst, jedis.smembers("{.}bar"));

    status = jedis.smove("{.}foo", "{.}bar", "a");
    assertEquals(status, 0);
  }

  @Test
  @Override
  public void sinter() {
    jedis.sadd("foo{.}", "a");
    jedis.sadd("foo{.}", "b");

    jedis.sadd("bar{.}", "b");
    jedis.sadd("bar{.}", "c");

    Set<String> expected = new HashSet<>();
    expected.add("b");

    Set<String> intersection = jedis.sinter("foo{.}", "bar{.}");
    assertEquals(expected, intersection);
  }

  @Test
  @Override
  public void sinterstore() {
    jedis.sadd("foo{.}", "a");
    jedis.sadd("foo{.}", "b");

    jedis.sadd("bar{.}", "b");
    jedis.sadd("bar{.}", "c");

    Set<String> expected = new HashSet<>();
    expected.add("b");

    long status = jedis.sinterstore("car{.}", "foo{.}", "bar{.}");
    assertEquals(1, status);

    assertEquals(expected, jedis.smembers("car{.}"));
  }

  @Test
  @Override
  public void sunion() {
    jedis.sadd("{.}foo", "a");
    jedis.sadd("{.}foo", "b");

    jedis.sadd("{.}bar", "b");
    jedis.sadd("{.}bar", "c");

    Set<String> expected = new HashSet<>();
    expected.add("a");
    expected.add("b");
    expected.add("c");

    Set<String> union = jedis.sunion("{.}foo", "{.}bar");
    assertEquals(expected, union);
  }

  @Test
  @Override
  public void sunionstore() {
    jedis.sadd("{.}foo", "a");
    jedis.sadd("{.}foo", "b");

    jedis.sadd("{.}bar", "b");
    jedis.sadd("{.}bar", "c");

    Set<String> expected = new HashSet<>();
    expected.add("a");
    expected.add("b");
    expected.add("c");

    long status = jedis.sunionstore("{.}car", "{.}foo", "{.}bar");
    assertEquals(3, status);

    assertEquals(expected, jedis.smembers("{.}car"));
  }

  @Test
  @Override
  public void sdiff() {
    jedis.sadd("foo{.}", "x");
    jedis.sadd("foo{.}", "a");
    jedis.sadd("foo{.}", "b");
    jedis.sadd("foo{.}", "c");

    jedis.sadd("bar{.}", "c");

    jedis.sadd("car{.}", "a");
    jedis.sadd("car{.}", "d");

    Set<String> expected = new HashSet<>();
    expected.add("x");
    expected.add("b");

    Set<String> diff = jedis.sdiff("foo{.}", "bar{.}", "car{.}");
    assertEquals(expected, diff);
  }

  @Test
  @Override
  public void sdiffstore() {
    jedis.sadd("foo{.}", "x");
    jedis.sadd("foo{.}", "a");
    jedis.sadd("foo{.}", "b");
    jedis.sadd("foo{.}", "c");

    jedis.sadd("bar{.}", "c");

    jedis.sadd("car{.}", "a");
    jedis.sadd("car{.}", "d");

    Set<String> expected = new HashSet<>();
    expected.add("x");
    expected.add("b");

    long status = jedis.sdiffstore("tar{.}", "foo{.}", "bar{.}", "car{.}");
    assertEquals(2, status);
    assertEquals(expected, jedis.smembers("tar{.}"));
  }

  @Test
  @SinceRedisVersion(value="7.0.0")
  public void sintercard() {
    jedis.sadd("foo{.}", "a");
    jedis.sadd("foo{.}", "b");

    jedis.sadd("bar{.}", "a");
    jedis.sadd("bar{.}", "b");
    jedis.sadd("bar{.}", "c");

    long card = jedis.sintercard("foo{.}", "bar{.}");
    assertEquals(2, card);
    long limitedCard = jedis.sintercard(1, "foo{.}", "bar{.}");
    assertEquals(1, limitedCard);

    // Binary
    jedis.sadd(bfoo, ba);
    jedis.sadd(bfoo, bb);

    jedis.sadd(bfoo_same_hashslot, ba);
    jedis.sadd(bfoo_same_hashslot, bb);
    jedis.sadd(bfoo_same_hashslot, bc);

    long bcard = jedis.sintercard(bfoo, bfoo_same_hashslot);
    assertEquals(2, bcard);
    long blimitedCard = jedis.sintercard(1, bfoo, bfoo_same_hashslot);
    assertEquals(1, blimitedCard);
  }


  @Test
  @Override
  @SinceRedisVersion(V8_10_0_RC2_STRING)
  public void sunioncard() {
    // Hash-tag the per-test prefix so all of one test's keys share a cluster slot.
    String foo = keys.key("{%test%}:foo");
    String bar = keys.key("{%test%}:bar");
    jedis.sadd(foo, "a", "b", "c");
    jedis.sadd(bar, "c", "d");

    assertEquals(4, jedis.sunioncard(foo, bar));

    // Binary
    byte[] bfooKey = keys.bKey("{%test%}:bfoo");
    byte[] bbarKey = keys.bKey("{%test%}:bbar");
    jedis.sadd(bfooKey, ba, bb);
    jedis.sadd(bbarKey, bb, bc);

    assertEquals(3, jedis.sunioncard(bfooKey, bbarKey));
  }

  @Test
  @Override
  @SinceRedisVersion(V8_10_0_RC2_STRING)
  public void sunioncardKeysList() {
    String foo = keys.key("{%test%}:foo");
    String bar = keys.key("{%test%}:bar");
    jedis.sadd(foo, "a", "b", "c");
    jedis.sadd(bar, "c", "d");

    assertEquals(4, jedis.sunioncard(Arrays.asList(foo, bar)));
  }

  @Test
  @Override
  @SinceRedisVersion(V8_10_0_RC2_STRING)
  public void sunioncardWithParams() {
    String foo = keys.key("{%test%}:foo");
    String bar = keys.key("{%test%}:bar");
    jedis.sadd(foo, "a", "b", "c");
    jedis.sadd(bar, "c", "d");

    assertEquals(3, jedis.sunioncard(foo, bar, new SUnionCardParams().limit(3)));

    // Binary
    byte[] bfooKey = keys.bKey("{%test%}:bfoo");
    byte[] bbarKey = keys.bKey("{%test%}:bbar");
    jedis.sadd(bfooKey, ba, bb);
    jedis.sadd(bbarKey, bb, bc);

    assertEquals(2, jedis.sunioncard(bfooKey, bbarKey, new SUnionCardParams().limit(2)));
  }

  @Test
  @Override
  @SinceRedisVersion(V8_10_0_RC2_STRING)
  public void sunioncardKeysListWithParams() {
    String foo = keys.key("{%test%}:foo");
    String bar = keys.key("{%test%}:bar");
    jedis.sadd(foo, "a", "b", "c");
    jedis.sadd(bar, "c", "d");

    assertEquals(4, jedis.sunioncard(Arrays.asList(foo, bar), new SUnionCardParams().approx()));

    // Binary
    byte[] bfooKey = keys.bKey("{%test%}:bfoo");
    byte[] bbarKey = keys.bKey("{%test%}:bbar");
    jedis.sadd(bfooKey, ba, bb);
    jedis.sadd(bbarKey, bb, bc);

    assertEquals(3,
      jedis.sunioncard(new byte[][] { bfooKey, bbarKey }, new SUnionCardParams().limit(0)));
  }

  @Test
  @Override
  @SinceRedisVersion(V8_10_0_RC2_STRING)
  public void sunioncardWrongTypeKey() {
    String foo = keys.key("{%test%}:foo");
    String strkey = keys.key("{%test%}:strkey");
    jedis.sadd(foo, "a");
    jedis.set(strkey, "value");
    assertThrows(JedisDataException.class, () -> jedis.sunioncard(foo, strkey));
  }

  @Test
  @Override
  @SinceRedisVersion(V8_10_0_RC2_STRING)
  public void sdiffcard() {
    // Hash-tag the per-test prefix so all of one test's keys share a cluster slot.
    String foo = keys.key("{%test%}:foo");
    String bar = keys.key("{%test%}:bar");
    String car = keys.key("{%test%}:car");
    jedis.sadd(foo, "x", "a", "b", "c");
    jedis.sadd(bar, "c");
    jedis.sadd(car, "a", "d");

    assertEquals(2, jedis.sdiffcard(foo, bar, car));
    assertEquals(0, jedis.sdiffcard(keys.key("{%test%}:nosuchset"), foo));

    // Binary
    byte[] bfooKey = keys.bKey("{%test%}:bfoo");
    byte[] bbarKey = keys.bKey("{%test%}:bbar");
    jedis.sadd(bfooKey, ba, bb);
    jedis.sadd(bbarKey, bb);

    assertEquals(1, jedis.sdiffcard(bfooKey, bbarKey));
  }

  @Test
  @Override
  @SinceRedisVersion(V8_10_0_RC2_STRING)
  public void sdiffcardKeysList() {
    String foo = keys.key("{%test%}:foo");
    String bar = keys.key("{%test%}:bar");
    String car = keys.key("{%test%}:car");
    jedis.sadd(foo, "x", "a", "b", "c");
    jedis.sadd(bar, "c");
    jedis.sadd(car, "a", "d");

    assertEquals(2, jedis.sdiffcard(Arrays.asList(foo, bar, car)));
  }

  @Test
  @Override
  @SinceRedisVersion(V8_10_0_RC2_STRING)
  public void sdiffcardWithParams() {
    String foo = keys.key("{%test%}:foo");
    String bar = keys.key("{%test%}:bar");
    jedis.sadd(foo, "x", "a", "b", "c");
    jedis.sadd(bar, "c");

    assertEquals(1, jedis.sdiffcard(foo, bar, new SDiffCardParams().limit(1)));
  }

  @Test
  @Override
  @SinceRedisVersion(V8_10_0_RC2_STRING)
  public void sdiffcardKeysListWithParams() {
    String foo = keys.key("{%test%}:foo");
    String bar = keys.key("{%test%}:bar");
    String car = keys.key("{%test%}:car");
    jedis.sadd(foo, "x", "a", "b", "c");
    jedis.sadd(bar, "c");
    jedis.sadd(car, "a", "d");

    assertEquals(1, jedis.sdiffcard(Arrays.asList(foo, bar, car), new SDiffCardParams().limit(1)));

    // Binary
    byte[] bfooKey = keys.bKey("{%test%}:bfoo");
    byte[] bbarKey = keys.bKey("{%test%}:bbar");
    jedis.sadd(bfooKey, ba, bb);
    jedis.sadd(bbarKey, bb);

    assertEquals(1,
      jedis.sdiffcard(new byte[][] { bfooKey, bbarKey }, new SDiffCardParams().limit(0)));
  }

}
