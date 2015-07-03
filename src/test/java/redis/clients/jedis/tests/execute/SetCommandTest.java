package redis.clients.jedis.tests.execute;

import static redis.clients.jedis.ScanParams.SCAN_POINTER_START;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import redis.clients.jedis.ScanParams;
import redis.clients.jedis.tests.commands.JedisCommandTestBase;

public class SetCommandTest extends JedisCommandTestBase {

  @Test
  public void sadd() {
    String status = jedis.execute("SADD foo testA");
    assertEquals("1", status);
    status = jedis.execute("SADD foo testA");
    assertEquals("0", status);
  }

  @Test
  public void smembers() {
    jedis.execute("SADD foo a");
    jedis.execute("SADD foo b");

    String members = jedis.execute("smembers foo");

    assertTrue(members.contains("a"));
    assertTrue(members.contains("b"));
  }

  @Test
  public void srem() {
    jedis.execute("sadd foo a");
    jedis.execute("sadd foo b");

    String result = jedis.execute("srem foo a");
    assertEquals("1", result);

    result = jedis.execute("srem foo abb");
    assertEquals("0", result);

    Set<String> expected = new HashSet<String>();
    expected.add("b");

    String members = jedis.execute("smembers foo");
    assertFalse(members.contains("a"));
    assertTrue(members.contains("b"));
  }

  @Test
  public void spop() {
    jedis.execute("sadd foo a");
    jedis.execute("sadd foo b");

    String member = jedis.execute("spop foo");

    assertTrue("a".equals(member) || "b".equals(member));
    assertEquals(1, jedis.smembers("foo").size());

    member = jedis.execute("spop bar");
    assertEquals("", member);
  }

  @Test
  public void spopWithCount() {
    jedis.execute("sadd foo a");
    jedis.execute("sadd foo b");

    String members = jedis.execute("spop foo 2");
    assertTrue(members.contains("a"));
    assertTrue(members.contains("b"));

    members = jedis.execute("spop foo 2");
    assertTrue(members.isEmpty());
  }

  @Test
  public void smove() {
    jedis.execute("sadd foo a");
    jedis.execute("sadd foo b");
    jedis.execute("sadd foo c");

    String status = jedis.execute("smove foo bar a");

    Set<String> expectedSrc = new HashSet<String>();
    expectedSrc.add("c");
    expectedSrc.add("b");

    Set<String> expectedDst = new HashSet<String>();

    expectedDst.add("a");

    assertEquals(status, "1");
    assertEquals(expectedSrc, jedis.smembers("foo"));
    assertEquals(expectedDst, jedis.smembers("bar"));

    status = jedis.execute("smove foo bar a");

    assertEquals(status, "0");
  }

  @Test
  public void scard() {
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");

    String card = jedis.execute("scard foo");

    assertEquals("2", card);

    card = jedis.execute("scard bar");
    assertEquals("0", card);
  }

  @Test
  public void sismember() {
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");

    assertEquals(jedis.execute("sismember foo a"), "1");

    assertEquals(jedis.execute("sismember foo c"), "0");
  }

  @Test
  public void sinterstore() {
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");

    jedis.sadd("bar", "b");
    jedis.sadd("bar", "c");

    Set<String> expected = new HashSet<String>();
    expected.add("b");

    String status = jedis.execute("sinterstore car foo bar");
    assertEquals("1", status);

    assertEquals(expected, jedis.smembers("car"));

  }

  @Test
  public void sunion() {
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");

    jedis.sadd("bar", "b");
    jedis.sadd("bar", "c");

    String union = jedis.execute("sunion foo bar");

    assertTrue(union.contains("a"));
    assertTrue(union.contains("b"));
    assertTrue(union.contains("c"));
  }

  @Test
  public void sunionstore() {
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");

    jedis.sadd("bar", "b");
    jedis.sadd("bar", "c");

    Set<String> expected = new HashSet<String>();
    expected.add("a");
    expected.add("b");
    expected.add("c");

    String status = jedis.execute("sunionstore car foo bar");
    assertEquals("3", status);

    assertEquals(expected, jedis.smembers("car"));
  }

  @Test
  public void sdiff() {
    jedis.sadd("foo", "x");
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");
    jedis.sadd("foo", "c");

    jedis.sadd("bar", "c");

    jedis.sadd("car", "a");
    jedis.sadd("car", "d");

    Set<String> expected = new HashSet<String>();
    expected.add("x");
    expected.add("b");

    String diff = jedis.execute("sdiff foo barcar");
    assertTrue(diff.contains("x"));
    assertTrue(diff.contains("b"));
  }

  @Test
  public void sdiffstore() {
    jedis.sadd("foo", "x");
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");
    jedis.sadd("foo", "c");

    jedis.sadd("bar", "c");

    jedis.sadd("car", "a");
    jedis.sadd("car", "d");

    Set<String> expected = new HashSet<String>();
    expected.add("d");
    expected.add("a");

    String status = jedis.execute("sdiffstore tar foo bar car");
    assertEquals("2", status);
    assertEquals(expected, jedis.smembers("car"));
  }

  @Test
  public void srandmember() {
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");

    String member = jedis.execute("srandmember foo");

    assertTrue("a".equals(member) || "b".equals(member));
    assertEquals(2, jedis.smembers("foo").size());

    member = jedis.execute("srandmember bar");
    assertTrue(member.isEmpty());
  }

  @Test
  public void sscan() {
    jedis.sadd("foo", "a", "b");

    String result = jedis.execute("sscan foo " + SCAN_POINTER_START);

    assertTrue(result.startsWith("0"));
    assertEquals(result.split("\n").length, 3);
  }

  @Test
  public void sscanMatch() {
    ScanParams params = new ScanParams();
    params.match("a*");

    jedis.sadd("foo", "b", "a", "aa");
    String result = jedis.execute("sscan foo " + SCAN_POINTER_START+" match a*");
    assertTrue(result.startsWith("0"));
    assertEquals(result.split("\n").length, 3);
  }
  
   @Test
   public void sscanCount() {
   ScanParams params = new ScanParams();
   params.count(2);
  
   jedis.sadd("foo", "a1", "a2", "a3", "a4", "a5");
  
   String result = jedis.execute("sscan foo "+ SCAN_POINTER_START+" count 2");
   assertTrue(result.split("\n").length>1);
   }
}
