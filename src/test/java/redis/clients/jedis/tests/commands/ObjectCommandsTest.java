package redis.clients.jedis.tests.commands;

import org.junit.Test;

import redis.clients.util.SafeEncoder;

public class ObjectCommandsTest extends JedisCommandTestBase {

  private String key = "mylist";
  private byte[] binaryKey = SafeEncoder.encode(key);

  @Test
  public void objectRefcount() {
    jedis.lpush(key, "hello world");
    Long refcount = jedis.objectRefcount(key);
    assertEquals(new Long(1), refcount);

    // Binary
    refcount = jedis.objectRefcount(binaryKey);
    assertEquals(new Long(1), refcount);

  }

  @Test
  public void objectEncoding() {
    jedis.lpush(key, "hello world");
    String encoding = jedis.objectEncoding(key);
    assertEquals("quicklist", encoding);

    // Binary
    encoding = SafeEncoder.encode(jedis.objectEncoding(binaryKey));
    assertEquals("quicklist", encoding);
  }

  @Test
  public void objectIdletime() throws InterruptedException {
    jedis.lpush(key, "hello world");

    Long time = jedis.objectIdletime(key);
    assertEquals(new Long(0), time);

    // Binary
    time = jedis.objectIdletime(binaryKey);
    assertEquals(new Long(0), time);
  }
}