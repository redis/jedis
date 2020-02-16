package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import redis.clients.jedis.util.SafeEncoder;

import java.util.List;

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

  @Test
  public void objectHelp() {
    List<String> helpTexts = jedis.objectHelp();
    assertNotNull(helpTexts);
  }

  @Test
  public void objectFreq() {
    jedis.set(key, "test1");
    // Before we test objectFreq command, we must config maxmemory-policy or will throw "An LFU maxmemory policy is not selected, access frequency not tracked. Please note that when switching between policies at runtime LRU and LFU data will take some time to adjust."
    jedis.configSet("maxmemory-policy", "allkeys-lfu");
    jedis.get(key);
    // String
    Long count = jedis.objectFreq(key);
    assertTrue(count > 0);

    // Binary
    count = jedis.objectFreq(binaryKey);
    assertTrue(count > 0);
    // Reset default config for other test case.
    jedis.configSet("maxmemory-policy", "noeviction");
  }
}