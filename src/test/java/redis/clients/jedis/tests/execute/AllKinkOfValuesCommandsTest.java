/**
 * @author weigao<weiga@iflytek.com>
 * @version 1.0.0
 */
package redis.clients.jedis.tests.execute;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import redis.clients.jedis.tests.commands.JedisCommandTestBase;

public class AllKinkOfValuesCommandsTest extends JedisCommandTestBase {

  @Test
  public void ping() {
    String status = jedis.execute("ping");
    assertEquals("PONG", status);
  }

  @Test
  public void exists() {
    String status = jedis.execute("set foo bar");
    assertEquals("OK", status);

    String reply = jedis.execute("exists foo");
    assertEquals("1", reply);

    String lreply = jedis.execute("del foo");
    assertEquals("1", lreply);

    reply = jedis.execute("exists foo");
    assertEquals("0", reply);
  }

  @Test
  public void del() {
    jedis.set("foo1", "bar1");
    jedis.set("foo2", "bar2");
    jedis.set("foo3", "bar3");

    String reply = jedis.execute("del foo1 foo2 foo3");
    assertEquals("3", reply);

    Boolean breply = jedis.exists("foo1");
    assertFalse(breply);
    breply = jedis.exists("foo2");
    assertFalse(breply);
    breply = jedis.exists("foo3");
    assertFalse(breply);

    jedis.set("foo1", "bar1");

    reply = jedis.execute("del foo1 foo2");
    assertEquals("1", reply);

    reply = jedis.execute("del foo1 foo2");
    assertEquals("0", reply);
  }

  @Test
  public void keys() {
    jedis.set("foo", "bar");
    jedis.set("foobar", "bar");

    String keys = jedis.execute("keys foo*");

    assertTrue(keys.contains("foo"));
    assertTrue(keys.contains("foobar"));
    assertEquals(2, keys.split("\n").length);

    keys = jedis.execute("keys bar*");
    assertTrue(keys.isEmpty());
  }

  @Test
  public void randomKey() {
    String text = jedis.execute("randomKey");
    assertTrue(text.isEmpty());

    jedis.set("foo", "bar");

    text = jedis.execute("randomKey");
    assertEquals("foo", text);

    jedis.set("bar", "foo");

    String randomkey = jedis.execute("randomKey");
    assertTrue(randomkey.equals("foo") || randomkey.equals("bar"));
  }

  @Test
  public void rename() {
    jedis.set("foo", "bar");
    String status = jedis.execute("rename foo bar");
    assertEquals("OK", status);

    String value = jedis.execute("get foo");
    assertTrue(value.isEmpty());

    value = jedis.execute("get bar");
    assertEquals("bar", value);
  }

  @Test
  public void renamenx() {
    jedis.set("foo", "bar");
    String status = jedis.execute("renamenx foo bar");
    assertEquals("1", status);

    jedis.set("foo", "bar");
    status = jedis.execute("renamenx foo bar");
    assertEquals("0", status);
  }

  @Test
  public void dbSize() {
    String size = jedis.execute("dbSize");
    assertEquals("0", size);

    jedis.set("foo", "bar");
    size = jedis.execute("dbSize");
    assertEquals("1", size);
  }

  @Test
  public void expire() {
    String status = jedis.execute("expire foo 20");
    assertEquals("0", status);

    jedis.set("foo", "bar");
    status = jedis.execute("expire foo 20");
    assertEquals("1", status);
  }

  @Test
  public void expireAt() {
    long unixTime = (System.currentTimeMillis() / 1000L) + 20;

    String status = jedis.execute("expireAt foo "+unixTime);
    assertEquals("0", status);

    jedis.set("foo", "bar");
    unixTime = (System.currentTimeMillis() / 1000L) + 20;
    status = jedis.execute("expireAt foo "+ unixTime);
    assertEquals("1", status);
  }
  
  @Test
  public void ttl() {
    String ttl = jedis.execute("ttl foo");
    assertEquals("-2", ttl);

    jedis.set("foo", "bar");
    ttl = jedis.execute("ttl foo");
    assertEquals("-1", ttl);

    jedis.expire("foo", 20);
    ttl = jedis.execute("ttl foo");
    int t = Integer.valueOf(ttl);
    assertTrue(t >= 0 && t <= 20);
    }
  
}
