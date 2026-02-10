package redis.clients.jedis.commands.jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.redis.test.annotations.EnabledOnCommand;
import io.redis.test.annotations.SinceRedisVersion;
import io.redis.test.annotations.ConditionalOnEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.params.LCSParams;
import redis.clients.jedis.params.MSetExParams;

import redis.clients.jedis.resps.LCSMatchResult;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.GetExParams;
import redis.clients.jedis.util.TestEnvUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
@Tag("integration")
public class StringValuesCommandsTest extends JedisCommandsTestBase {

  public StringValuesCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void setAndGet() {
    String status = jedis.set("foo", "bar");
    assertEquals("OK", status);

    String value = jedis.get("foo");
    assertEquals("bar", value);

    assertNull(jedis.get("bar"));
  }

  @Test
  public void getSet() {
    String value = jedis.getSet("foo", "bar");
    assertNull(value);
    value = jedis.get("foo");
    assertEquals("bar", value);
  }

  @Test
  public void getDel() {
    String status = jedis.set("foo", "bar");
    assertEquals("OK", status);

    String value = jedis.getDel("foo");
    assertEquals("bar", value);

    assertNull(jedis.get("foo"));
  }

  @Test
  public void getEx() {
    assertNull(jedis.getEx("foo", GetExParams.getExParams().ex(1)));
    jedis.set("foo", "bar");

    assertEquals("bar", jedis.getEx("foo", GetExParams.getExParams().ex(10)));
    long ttl = jedis.ttl("foo");
    assertTrue(ttl > 0 && ttl <= 10);

    assertEquals("bar", jedis.getEx("foo", GetExParams.getExParams().px(20000l)));
    ttl = jedis.ttl("foo");
    assertTrue(ttl > 10 && ttl <= 20);

    assertEquals("bar",
      jedis.getEx("foo", GetExParams.getExParams().exAt(System.currentTimeMillis() / 1000 + 30)));
    ttl = jedis.ttl("foo");
    assertTrue(ttl > 20 && ttl <= 30);

    assertEquals("bar",
      jedis.getEx("foo", GetExParams.getExParams().pxAt(System.currentTimeMillis() + 40000l)));
    ttl = jedis.ttl("foo");
    assertTrue(ttl > 30 && ttl <= 40);

    assertEquals("bar", jedis.getEx("foo", GetExParams.getExParams().persist()));
    assertEquals(-1, jedis.ttl("foo"));
  }

  @Test
  public void mget() {
    List<String> values = jedis.mget("foo", "bar");
    List<String> expected = new ArrayList<String>();
    expected.add(null);
    expected.add(null);

    assertEquals(expected, values);

    jedis.set("foo", "bar");

    expected = new ArrayList<String>();
    expected.add("bar");
    expected.add(null);
    values = jedis.mget("foo", "bar");

    assertEquals(expected, values);

    jedis.set("bar", "foo");

    expected = new ArrayList<String>();
    expected.add("bar");
    expected.add("foo");
    values = jedis.mget("foo", "bar");

    assertEquals(expected, values);
  }

  @Test
  public void setnx() {
    assertEquals(1, jedis.setnx("foo", "bar"));
    assertEquals("bar", jedis.get("foo"));

    assertEquals(0, jedis.setnx("foo", "bar2"));
    assertEquals("bar", jedis.get("foo"));
  }

  @Test
  public void setex() {
    String status = jedis.setex("foo", 20, "bar");
    assertEquals("OK", status);
    long ttl = jedis.ttl("foo");
    assertTrue(ttl > 0 && ttl <= 20);
  }

  @Test
  public void mset() {
    String status = jedis.mset("foo", "bar", "bar", "foo");
    assertEquals("OK", status);
    assertEquals("bar", jedis.get("foo"));
    assertEquals("foo", jedis.get("bar"));
  }

  @Test
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
  public void msetnx() {
    assertEquals(1, jedis.msetnx("foo", "bar", "bar", "foo"));
    assertEquals("bar", jedis.get("foo"));
    assertEquals("foo", jedis.get("bar"));

    assertEquals(0, jedis.msetnx("foo", "bar1", "bar2", "foo2"));
    assertEquals("bar", jedis.get("foo"));
    assertEquals("foo", jedis.get("bar"));
  }

  @Test
  public void incr() {
    assertEquals(1, jedis.incr("foo"));
    assertEquals(2, jedis.incr("foo"));
  }

  @Test
  public void incrWrongValue() {
    jedis.set("foo", "bar");
    assertThrows(JedisDataException.class, () -> jedis.incr("foo"));
  }

  @Test
  public void incrBy() {
    assertEquals(2, jedis.incrBy("foo", 2));
    assertEquals(5, jedis.incrBy("foo", 3));
  }

  @Test
  public void incrByWrongValue() {
    jedis.set("foo", "bar");
    assertThrows(JedisDataException.class, () -> jedis.incrBy("foo", 2));
  }

  @Test
  public void incrByFloat() {
    assertEquals(10.5, jedis.incrByFloat("foo", 10.5), 0.0);
    assertEquals(10.6, jedis.incrByFloat("foo", 0.1), 0.0);
  }

  @Test
  public void incrByFloatWrongValue() {
    jedis.set("foo", "bar");
    assertThrows(JedisDataException.class, () -> jedis.incrByFloat("foo", 2d));
  }

  @Test
  public void decrWrongValue() {
    jedis.set("foo", "bar");
    assertThrows(JedisDataException.class, () -> jedis.decr("foo"));
  }

  @Test
  public void decr() {
    assertEquals(-1, jedis.decr("foo"));
    assertEquals(-2, jedis.decr("foo"));
  }

  @Test
  public void decrBy() {
    assertEquals(-2, jedis.decrBy("foo", 2));
    assertEquals(-4, jedis.decrBy("foo", 2));
  }

  @Test
  public void decrByWrongValue() {
    jedis.set("foo", "bar");
    assertThrows(JedisDataException.class, () -> jedis.decrBy("foo", 2));
  }

  @Test
  public void append() {
    assertEquals(3, jedis.append("foo", "bar"));
    assertEquals("bar", jedis.get("foo"));
    assertEquals(6, jedis.append("foo", "bar"));
    assertEquals("barbar", jedis.get("foo"));
  }

  @Test
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
  public void substr() {
    jedis.set("s", "This is a string");
    assertEquals("This", jedis.substr("s", 0, 3));
    assertEquals("ing", jedis.substr("s", -3, -1));
    assertEquals("This is a string", jedis.substr("s", 0, -1));
    assertEquals(" string", jedis.substr("s", 9, 100000));
  }

  @Test
  public void strlen() {
    String str = "This is a string";
    jedis.set("s", str);
    assertEquals(str.length(), jedis.strlen("s"));
  }

  @Test
  public void incrLargeNumbers() {
    assertEquals(1, jedis.incr("foo"));
    assertEquals(1L + Integer.MAX_VALUE, jedis.incrBy("foo", Integer.MAX_VALUE));
  }

  @Test
  public void incrReallyLargeNumbers() {
    jedis.set("foo", Long.toString(Long.MAX_VALUE));
    assertThrows(JedisDataException.class, () -> jedis.incr("foo"));
  }

  @Test
  public void psetex() {
    String status = jedis.psetex("foo", 20000, "bar");
    assertEquals("OK", status);
    long ttl = jedis.ttl("foo");
    assertTrue(ttl > 0 && ttl <= 20000);
  }

  @Test
  @SinceRedisVersion("7.0.0")
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
  public void lcs() {
    jedis.mset("key1", "ohmytext", "key2", "mynewtext");

    LCSMatchResult stringMatchResult = jedis.lcs("key1", "key2", LCSParams.LCSParams());
    assertEquals("mytext", stringMatchResult.getMatchString());

    stringMatchResult = jedis.lcs("key1", "key2", LCSParams.LCSParams().idx().withMatchLen());
    assertEquals(stringMatchResult.getLen(), 6);
    assertEquals(2, stringMatchResult.getMatches().size());

    stringMatchResult = jedis.lcs("key1", "key2", LCSParams.LCSParams().idx().minMatchLen(10));
    assertEquals(0, stringMatchResult.getMatches().size());
  }

  // MSETEX NX + expiration matrix
  static Stream<Arguments> msetexNxArgsProvider() {
    return Stream.of(Arguments.of("EX", new MSetExParams().nx().ex(5)),
      Arguments.of("PX", new MSetExParams().nx().px(5000)),
      Arguments.of("EXAT", new MSetExParams().nx().exAt(System.currentTimeMillis() / 1000 + 5)),
      Arguments.of("PXAT", new MSetExParams().nx().pxAt(System.currentTimeMillis() + 5000)),
      Arguments.of("KEEPTTL", new MSetExParams().nx().keepTtl()));
  }

  @ParameterizedTest(name = "MSETEX NX + {0}")
  @MethodSource("msetexNxArgsProvider")
  @EnabledOnCommand("MSETEX")
  public void msetexNx_parametrized(String optionLabel, MSetExParams params) {
    String k1 = "{t}msetex:js:k1";
    String k2 = "{t}msetex:js:k2";

    boolean result = jedis.msetex(params, k1, "v1", k2, "v2");
    assertTrue(result);

    long ttl = jedis.ttl(k1);
    if ("KEEPTTL".equals(optionLabel)) {
      assertEquals(-1L, ttl);
    } else {
      assertTrue(ttl > 0L);
    }
  }

  @Test
  @EnabledOnCommand("MSETEX")
  public void msetexXxEx() {
    String k1 = "{t}msetex:js:xx:k1";
    String k2 = "{t}msetex:js:xx:k2";

    // First set the keys so they exist (XX requires existing keys)
    jedis.set(k1, "initial1");
    jedis.set(k2, "initial2");

    // Now use MSETEX with XX and EX
    MSetExParams params = new MSetExParams().xx().ex(5);
    boolean result = jedis.msetex(params, k1, "v1", k2, "v2");
    assertTrue(result);

    // Verify values were updated
    assertEquals("v1", jedis.get(k1));
    assertEquals("v2", jedis.get(k2));

    // Verify TTL is set
    long ttl = jedis.ttl(k1);
    assertTrue(ttl > 0L);
  }

}
