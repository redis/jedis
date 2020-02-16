package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.util.SafeEncoder;

import java.util.List;

public class ObjectCommandsTest extends JedisCommandTestBase {

  private String key = "mylist";
  private byte[] binaryKey = SafeEncoder.encode(key);
  private int port = 6386;
  private Jedis jedis1;

  @Before
  public void setUp() throws Exception {
    jedis = new Jedis(hnp.getHost(), hnp.getPort(), 500);
    jedis.connect();
    jedis.auth("foobared");
    jedis.flushAll();

    jedis1 = new Jedis(hnp.getHost(), port, 500);
    jedis1.connect();
    jedis1.auth("foobared");
    jedis1.flushAll();
  }

  @After
  public void tearDown() throws Exception {
    jedis.disconnect();
    jedis1.disconnect();
  }

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
    // String
    List<String> helpTexts = jedis.objectHelp();
    assertNotNull(helpTexts);

    // Binary
    List<byte[]> helpBinaryTexts = jedis.objectHelpBinary();
    assertNotNull(helpBinaryTexts);
  }

  @Test
  public void objectFreq() {
    jedis1.set(key, "test1");
    jedis1.get(key);
    // String
    Long count = jedis1.objectFreq(key);
    assertTrue(count > 0);

    // Binary
    count = jedis1.objectFreq(binaryKey);
    assertTrue(count > 0);
  }
}