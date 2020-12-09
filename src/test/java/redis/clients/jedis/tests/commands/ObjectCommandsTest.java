package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.tests.HostAndPortUtil;
import redis.clients.jedis.util.SafeEncoder;

import java.util.List;

public class ObjectCommandsTest extends JedisCommandTestBase {

  private String key = "mylist";
  private byte[] binaryKey = SafeEncoder.encode(key);
  private static final HostAndPort lfuHnp = HostAndPortUtil.getRedisServers().get(7);
  private Jedis lfuJedis;

  @Before
  public void setUp() throws Exception {
    super.setUp();

    lfuJedis = new Jedis(lfuHnp.getHost(), lfuHnp.getPort(), 500);
    lfuJedis.connect();
    lfuJedis.flushAll();
  }

  @After
  public void tearDown() throws Exception {
    lfuJedis.disconnect();
    super.tearDown();
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
    lfuJedis.set(key, "test1");
    lfuJedis.get(key);
    // String
    Long count = lfuJedis.objectFreq(key);
    assertTrue(count > 0);

    // Binary
    count = lfuJedis.objectFreq(binaryKey);
    assertTrue(count > 0);
    
    assertNull(lfuJedis.objectFreq("no_such_key"));
    
    try {
      jedis.set(key, "test2");
      jedis.objectFreq(key);
      fail("Freq is only allowed with LFU policy");
    }catch(JedisDataException e) {}
  }
}
