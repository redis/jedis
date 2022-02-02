package redis.clients.jedis;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.exceptions.JedisConnectionException;

public class UnavailableConnectionTest {

  private static final HostAndPort unavailableNode = new HostAndPort("localhost", 6400);

  @BeforeClass
  public static void setup() {
    setupAvoidQuitInDestroyObject();

    try (Jedis j = new Jedis(unavailableNode)) {
      j.shutdown();
    }
  }

  public static void cleanup() {
    cleanupAvoidQuitInDestroyObject();
  }

  private static JedisPool poolForBrokenJedis1;
  private static Thread threadForBrokenJedis1;
  private static Jedis brokenJedis1;

  public static void setupAvoidQuitInDestroyObject() {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    poolForBrokenJedis1 = new JedisPool(config, unavailableNode.getHost(),
        unavailableNode.getPort());
    brokenJedis1 = poolForBrokenJedis1.getResource();
    threadForBrokenJedis1 = new Thread(new Runnable() {
      @Override
      public void run() {
        brokenJedis1.blpop(0, "broken-key-1");
      }
    });
    threadForBrokenJedis1.start();
  }

  @Test(timeout = 5000)
  public void testAvoidQuitInDestroyObjectForBrokenConnection() throws InterruptedException {
    threadForBrokenJedis1.join();
    assertFalse(threadForBrokenJedis1.isAlive());
    assertTrue(brokenJedis1.isBroken());
    brokenJedis1.close(); // we need capture/mock to test this properly

    try {
      poolForBrokenJedis1.getResource();
      fail("Should not get connection from pool");
    } catch (Exception ex) {
      assertSame(JedisConnectionException.class, ex.getClass());
      // assertSame(JedisConnectionException.class, ex.getCause().getClass());
      // assertSame(java.net.ConnectException.class, ex.getCause().getCause().getClass());
      // assertSame(java.net.ConnectException.class, ex.getCause().getClass());
    }
  }

  public static void cleanupAvoidQuitInDestroyObject() {
    poolForBrokenJedis1.close();
  }
}
