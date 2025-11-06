package redis.clients.jedis.commands.unified;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static redis.clients.jedis.params.SetParams.setParams;

import java.util.ArrayList;
import java.util.List;

import io.redis.test.annotations.SinceRedisVersion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.conditions.ValueCondition;
import redis.clients.jedis.params.LCSParams;
import redis.clients.jedis.resps.LCSMatchResult;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.GetExParams;

@Tag("integration")
public abstract class StringValuesCommandsTestBase extends UnifiedJedisCommandsTestBase {

  public StringValuesCommandsTestBase(RedisProtocol protocol) {
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
  public void setGetWithParams() {
    jedis.del("foo");

    // no previous, return null
    assertNull(jedis.setGet("foo", "bar", setParams().nx()));

    // key already exists, new value should not be set, previous value should be bbar
    assertEquals("bar", jedis.setGet("foo", "foobar", setParams().nx()));

    assertEquals("bar", jedis.setGet("foo", "foobar", setParams().xx()));
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

    assertEquals("bar", jedis.getEx("foo", GetExParams.getExParams().px(20000L)));
    ttl = jedis.ttl("foo");
    assertTrue(ttl > 10 && ttl <= 20);

    assertEquals("bar", jedis.getEx("foo", GetExParams.getExParams().exAt(System.currentTimeMillis() / 1000 + 30)));
    ttl = jedis.ttl("foo");
    assertTrue(ttl > 20 && ttl <= 30);

    assertEquals("bar", jedis.getEx("foo", GetExParams.getExParams().pxAt(System.currentTimeMillis() + 40000L)));
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
    assertThrows(JedisDataException.class, ()->jedis.incr("foo"));
  }

  @Test
  public void incrBy() {
    assertEquals(2, jedis.incrBy("foo", 2));
    assertEquals(5, jedis.incrBy("foo", 3));
  }

  @Test
  public void incrByWrongValue() {
    jedis.set("foo", "bar");
    assertThrows(JedisDataException.class, ()->jedis.incrBy("foo", 2));
  }

  @Test
  public void incrByFloat() {
    assertEquals(10.5, jedis.incrByFloat("foo", 10.5), 0.0);
    assertEquals(10.6, jedis.incrByFloat("foo", 0.1), 0.0);
  }

  @Test
  public void incrByFloatWrongValue() {
    jedis.set("foo", "bar");
    assertThrows(JedisDataException.class, ()->jedis.incrByFloat("foo", 2d));
  }

  @Test
  public void decrWrongValue() {
    jedis.set("foo", "bar");
    assertThrows(JedisDataException.class, ()->jedis.decr("foo"));
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
    assertThrows(JedisDataException.class, ()->jedis.decrBy("foo", 2));
  }

  @Test
  public void append() {
    assertEquals(3, jedis.append("foo", "bar"));
    assertEquals("bar", jedis.get("foo"));
    assertEquals(6, jedis.append("foo", "bar"));
    assertEquals("barbar", jedis.get("foo"));
  }

  @Test
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
    assertThrows(JedisDataException.class, ()->jedis.incr("foo")); // Should throw an exception
  }

  @Test
  public void psetex() {
    String status = jedis.psetex("foo", 20000, "bar");
    assertEquals("OK", status);
    long ttl = jedis.ttl("foo");
    assertTrue(ttl > 0 && ttl <= 20000);
  }

  @Test
  @SinceRedisVersion(value="7.0.0")
  public void lcs() {
    jedis.mset("key1", "ohmytext", "key2", "mynewtext");

    LCSMatchResult stringMatchResult = jedis.lcs("key1", "key2",
            LCSParams.LCSParams());
    assertEquals("mytext", stringMatchResult.getMatchString());

    stringMatchResult = jedis.lcs( "key1", "key2",
            LCSParams.LCSParams().idx().withMatchLen());
    assertEquals(stringMatchResult.getLen(), 6);
    assertEquals(2, stringMatchResult.getMatches().size());

    stringMatchResult = jedis.lcs( "key1", "key2",
            LCSParams.LCSParams().idx().minMatchLen(10));
    assertEquals(0, stringMatchResult.getMatches().size());
  }

  @Test
  @SinceRedisVersion("8.3.224")
  public void digestBasic() {
    jedis.del("dg");
    assertNull(jedis.digestKey("dg"));
    jedis.set("dg", "val");
    String hex = jedis.digestKey("dg");
    assertTrue(hex != null && (hex.length() == 16));
  }

  @Test
  @SinceRedisVersion("8.3.224")
  public void setWithIfConditions() {
    jedis.set("kif", "v1");

    // IFEQ matches -> set
    assertEquals("OK", jedis.set("kif", "v2", ValueCondition.valueEq("v1")));
    assertEquals("v2", jedis.get("kif"));

    // IFEQ fails -> no set
    assertNull(jedis.set("kif", "v3", ValueCondition.valueEq("nope")));
    assertEquals("v2", jedis.get("kif"));

    // IFNE matches -> set
    assertEquals("OK", jedis.set("kif", "v4", ValueCondition.valueNe("nope")));
    assertEquals("v4", jedis.get("kif"));

    // Missing key semantics
    jedis.del("kif_missing");
    assertNull(jedis.set("kif_missing", "x", ValueCondition.valueEq("anything"))); // missing + IFEQ should fail
    assertEquals("OK", jedis.set("kif_missing", "x", ValueCondition.valueNe("anything"))); // missing + IFNE should pass
  }


  @Test
  @SinceRedisVersion("8.3.224")
  public void setGetWithIFConditions() {
    jedis.del("sgk");
    // Missing + IFNE should set and return previous (null)
    assertNull(jedis.setGet("sgk", "v1", ValueCondition.valueNe("x")));
    assertEquals("v1", jedis.get("sgk"));

    // IFEQ matches -> returns old value and sets
    assertEquals("v1", jedis.setGet("sgk", "v2", ValueCondition.valueEq("v1")));
    assertEquals("v2", jedis.get("sgk"));

    // IFEQ fails -> returns old value and does not set
    assertEquals("v2", jedis.setGet("sgk", "v3", ValueCondition.valueEq("nope")));
    assertEquals("v2", jedis.get("sgk"));
  }

  @Test
  @SinceRedisVersion("8.3.224")
  public void setWithIFDigestConditions() {
    jedis.set("dk", "abc");
    String dig = jedis.digestKey("dk");

    // IFDEQ matches -> set
    assertEquals("OK", jedis.set("dk", "def", ValueCondition.digestEq(dig)));
    String newDig = jedis.digestKey("dk");
    assertTrue(newDig != null && newDig.length() == 16);

    // IFDEQ fails -> no set
    assertNull(jedis.set("dk", "ghi", ValueCondition.digestEq(dig)));
    assertEquals("def", jedis.get("dk"));

    // IFDNE equal digest -> fail (no set)
    assertNull(jedis.set("dk", "ghi", ValueCondition.digestNe(newDig)));
    assertEquals("def", jedis.get("dk"));

    // Missing key semantics
    jedis.del("dm");
    assertNull(jedis.set("dm", "x", ValueCondition.digestEq("0000000000000000")));
    jedis.del("dm");
    assertEquals("OK", jedis.set("dm", "x", ValueCondition.digestNe("0000000000000000")));
  }

  @Test
  @SinceRedisVersion("8.3.224")
  public void casCadEndToEndExample() {
    final String k = "cas:ex";
    jedis.del(k);

    // 1) Create initial value
    assertEquals("OK", jedis.set(k, "v1"));
    assertEquals("v1", jedis.get(k));

    // 2) Read digest and use it to CAS to v2
    String d1 = jedis.digestKey(k);
    assertTrue(d1 != null && d1.length() == 16);

    // Wrong digest must not set
    assertNull(jedis.set(k, "bad", ValueCondition.digestEq("0000000000000000")));
    assertEquals("v1", jedis.get(k));

    // Correct digest sets the new value
    assertEquals("OK", jedis.set(k, "v2", ValueCondition.digestEq(d1)));
    assertEquals("v2", jedis.get(k));

    // 3) Delete using DELEX guarded by the latest digest
    String d2 = jedis.digestKey(k);
    assertEquals(0L, jedis.delex(k, ValueCondition.digestEq("0000000000000000")));
    assertEquals(1L, jedis.delex(k, ValueCondition.digestEq(d2)));
    assertFalse(jedis.exists(k));
  }
  @Test
  @SinceRedisVersion("8.3.224")
  public void casCadEndToEndExample_Experimental() {
    final String k = "cas:ex2";
    jedis.del(k);

    assertEquals("OK", jedis.set(k, "v1"));

    String d1 = jedis.digestKey(k);
    ValueCondition cond1 = ValueCondition.digestEq(d1);
    assertEquals("OK", jedis.set(k, "v2", cond1));
    assertEquals("v2", jedis.get(k));

    String d2 = jedis.digestKey(k);
    ValueCondition cond2 = ValueCondition.digestEq(d2);
    assertEquals(1L, jedis.delex(k, cond2));
    assertFalse(jedis.exists(k));
  }

  @Test
  @SinceRedisVersion("8.3.224")
  public void setWithParamsAndIFCondition() {
    jedis.del("comb1");
    // missing key: NX + IFNE should set
    assertEquals("OK", jedis.set("comb1", "v1", setParams().nx(), ValueCondition.valueNe("x")));
    assertEquals("v1", jedis.get("comb1"));

    // existing key: XX + IFEQ should set
    assertEquals("OK", jedis.set("comb1", "v2", setParams().xx(), ValueCondition.valueEq("v1")));
    assertEquals("v2", jedis.get("comb1"));

    // existing key: XX + wrong IFEQ should not set
    assertNull(jedis.set("comb1", "no", setParams().xx(), ValueCondition.valueEq("nope")));
    assertEquals("v2", jedis.get("comb1"));
  }

  @Test
  @SinceRedisVersion("8.3.224")
  public void setGetWithParamsAndIFCondition() {
    jedis.set("comb2", "v1");

    // existing key: XX + IFEQ should set and return previous
    String prev = jedis.setGet("comb2", "v2", setParams().xx(), ValueCondition.valueEq("v1"));
    assertEquals("v1", prev);
    assertEquals("v2", jedis.get("comb2"));

    // failing condition: returns current and does not set
    prev = jedis.setGet("comb2", "no", setParams().xx(), ValueCondition.valueEq("nope"));
    assertEquals("v2", prev);
    assertEquals("v2", jedis.get("comb2"));
  }

}
