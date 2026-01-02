package redis.clients.jedis;

import io.redis.test.annotations.EnabledOnEnv;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Tag;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.util.TestEnvUtil;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("integration")
@EnabledOnEnv(TestEnvUtil.ENV_LEGACY)
public class UnavailableConnectionTest {

  private static final HostAndPort unavailableNode = new HostAndPort("localhost", 6400);

  @BeforeAll
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

  @Test
  @Timeout(5)
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
    }
  }

  public static void cleanupAvoidQuitInDestroyObject() {
    poolForBrokenJedis1.close();
  }
}
