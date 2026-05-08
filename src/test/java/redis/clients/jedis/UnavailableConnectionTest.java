package redis.clients.jedis;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import io.redis.test.annotations.ConditionalOnEnv;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.util.EnvCondition;

import redis.clients.jedis.util.TestEnvUtil;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies that broken connections are not returned to the pool and that {@link Jedis#close()} on a
 * broken connection does not attempt to send {@code QUIT}.
 * <p>
 * The unavailable Redis instance is simulated through a Toxiproxy proxy fronting the
 * {@code redis-unavailable-1} container. {@code redisProxy.disable()} closes any in-flight
 * connection and refuses new ones, taking the place of issuing {@code SHUTDOWN} against a real
 * Redis (the previous oss-source approach).
 */
@Tag("integration")
@ConditionalOnEnv(value = TestEnvUtil.ENV_OSS_DOCKER, enabled = true)
public class UnavailableConnectionTest {

  @RegisterExtension
  public static EnvCondition envCondition = new EnvCondition();

  private static final String PROXY_NAME = "redis-unavailable";
  private static final String PROXY_LISTEN = "0.0.0.0:26400";
  private static final String PROXY_UPSTREAM = "redis-unavailable-1:6400";

  private static final ToxiproxyClient tp = new ToxiproxyClient("localhost", 8474);
  private static Proxy redisProxy;
  private static HostAndPort unavailableNode;

  private static Logger log = LoggerFactory.getLogger(UnavailableConnectionTest.class);

  @BeforeAll
  public static void setup() throws IOException {
    unavailableNode = Endpoints.getRedisEndpoint(PROXY_NAME).getHostAndPort();

    if (tp.getProxyOrNull(PROXY_NAME) != null) {
      tp.getProxy(PROXY_NAME).delete();
    }
    redisProxy = tp.createProxy(PROXY_NAME, PROXY_LISTEN, PROXY_UPSTREAM);

    setupAvoidQuitInDestroyObject();

    // Simulate the server going away: close existing connections and refuse new ones.
    redisProxy.disable();
  }

  @AfterAll
  public static void cleanup() throws IOException {
    cleanupAvoidQuitInDestroyObject();
    if (redisProxy != null) {
      redisProxy.delete();
    }
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
