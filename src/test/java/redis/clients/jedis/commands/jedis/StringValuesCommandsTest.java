package redis.clients.jedis.commands.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeFalse;
import static redis.clients.jedis.util.RedisVersionUtil.checkRedisMajorVersionNumber;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import redis.clients.jedis.params.LCSParams;
import redis.clients.jedis.resps.LCSMatchResult;
import redis.clients.jedis.resps.LCSMatchResult.MatchedPosition;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.GetExParams;
import redis.clients.jedis.params.StrAlgoLCSParams;

public class StringValuesCommandsTest extends JedisCommandsTestBase {
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

    assertEquals("bar", jedis.getEx("foo", GetExParams.getExParams().exAt(System.currentTimeMillis() / 1000 + 30)));
    ttl = jedis.ttl("foo");
    assertTrue(ttl > 20 && ttl <= 30);

    assertEquals("bar", jedis.getEx("foo", GetExParams.getExParams().pxAt(System.currentTimeMillis() + 40000l)));
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

  @Test(expected = JedisDataException.class)
  public void incrWrongValue() {
    jedis.set("foo", "bar");
    jedis.incr("foo");
  }

  @Test
  public void incrBy() {
    assertEquals(2, jedis.incrBy("foo", 2));
    assertEquals(5, jedis.incrBy("foo", 3));
  }

  @Test(expected = JedisDataException.class)
  public void incrByWrongValue() {
    jedis.set("foo", "bar");
    jedis.incrBy("foo", 2);
  }

  @Test
  public void incrByFloat() {
    assertEquals(10.5, jedis.incrByFloat("foo", 10.5), 0.0);
    assertEquals(10.6, jedis.incrByFloat("foo", 0.1), 0.0);
  }

  @Test(expected = JedisDataException.class)
  public void incrByFloatWrongValue() {
    jedis.set("foo", "bar");
    jedis.incrByFloat("foo", 2d);
  }

  @Test(expected = JedisDataException.class)
  public void decrWrongValue() {
    jedis.set("foo", "bar");
    jedis.decr("foo");
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

  @Test(expected = JedisDataException.class)
  public void decrByWrongValue() {
    jedis.set("foo", "bar");
    jedis.decrBy("foo", 2);
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

  @Test(expected = JedisDataException.class)
  public void incrReallyLargeNumbers() {
    jedis.set("foo", Long.toString(Long.MAX_VALUE));
    jedis.incr("foo"); // Should throw an exception 
  }

  @Test
  public void psetex() {
    String status = jedis.psetex("foo", 20000, "bar");
    assertEquals("OK", status);
    long ttl = jedis.ttl("foo");
    assertTrue(ttl > 0 && ttl <= 20000);
  }

  @Test
  public void strAlgoLcsWithLen() {
    assumeFalse(checkRedisMajorVersionNumber(7));
    LCSMatchResult stringMatchResult = jedis.strAlgoLCSStrings("ohmytext", "mynewtext",
        StrAlgoLCSParams.StrAlgoLCSParams().len());
    assertEquals(stringMatchResult.getLen(), 6);
  }

  @Test
  public void strAlgoLcs() {
    assumeFalse(checkRedisMajorVersionNumber(7));
    LCSMatchResult stringMatchResult = jedis.strAlgoLCSStrings("ohmytext", "mynewtext",
        StrAlgoLCSParams.StrAlgoLCSParams());
    assertEquals(stringMatchResult.getMatchString(), "mytext");
  }

  @Test
  public void strAlgoLcsWithIdx() {
    assumeFalse(checkRedisMajorVersionNumber(7));
    LCSMatchResult stringMatchResult = jedis.strAlgoLCSStrings("ohmytext", "mynewtext",
        StrAlgoLCSParams.StrAlgoLCSParams().idx().withMatchLen());
    assertEquals(stringMatchResult.getLen(), 6);
    assertEquals(2, stringMatchResult.getMatches().size());

    MatchedPosition position0 = stringMatchResult.getMatches().get(0);
    assertEquals(position0.getA().getStart(), 4);
    assertEquals(position0.getA().getEnd(), 7);
    assertEquals(position0.getB().getStart(), 5);
    assertEquals(position0.getB().getEnd(), 8);
    assertEquals(position0.getMatchLen(), 4);

    MatchedPosition position1 = stringMatchResult.getMatches().get(1);
    assertEquals(position1.getA().getStart(), 2);
    assertEquals(position1.getA().getEnd(), 3);
    assertEquals(position1.getB().getStart(), 0);
    assertEquals(position1.getB().getEnd(), 1);
    assertEquals(position1.getMatchLen(), 2);
  }

  @Test
  public void strAlgoLcsWithKey() {
    assumeFalse(checkRedisMajorVersionNumber(7));
    jedis.mset("key1", "ohmytext", "key2", "mynewtext");

    LCSMatchResult stringMatchResult = jedis.strAlgoLCSKeys("key1", "key2",
        StrAlgoLCSParams.StrAlgoLCSParams());
    assertEquals("mytext", stringMatchResult.getMatchString());
  }

  @Test
  public void strAlgoLcsWithKeyAndIdx() {
    assumeFalse(checkRedisMajorVersionNumber(7));
    jedis.mset("key1", "ohmytext", "key2", "mynewtext");

    LCSMatchResult stringMatchResult = jedis.strAlgoLCSKeys( "key1", "key2",
        StrAlgoLCSParams.StrAlgoLCSParams().idx().withMatchLen());
    assertEquals(stringMatchResult.getLen(), 6);
    assertEquals(2, stringMatchResult.getMatches().size());

    MatchedPosition position0 = stringMatchResult.getMatches().get(0);
    assertEquals(position0.getA().getStart(), 4);
    assertEquals(position0.getA().getEnd(), 7);
    assertEquals(position0.getB().getStart(), 5);
    assertEquals(position0.getB().getEnd(), 8);
    assertEquals(position0.getMatchLen(), 4);

    MatchedPosition position1 = stringMatchResult.getMatches().get(1);
    assertEquals(position1.getA().getStart(), 2);
    assertEquals(position1.getA().getEnd(), 3);
    assertEquals(position1.getB().getStart(), 0);
    assertEquals(position1.getB().getEnd(), 1);
    assertEquals(position1.getMatchLen(), 2);
  }

  @Test
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

}
