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
import redis.clients.jedis.exceptions.JedisConnectionException;

import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.EnvCondition;

import redis.clients.jedis.util.TestEnvUtil;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

  private static JedisPool poolForBrokenJedis1;
  private static Thread threadForBrokenJedis1;
  private static Jedis brokenJedis1;

  @BeforeAll
  public static void setup() throws IOException, InterruptedException {
    unavailableNode = Endpoints.getRedisEndpoint(PROXY_NAME).getHostAndPort();

    if (tp.getProxyOrNull(PROXY_NAME) != null) {
      tp.getProxy(PROXY_NAME).delete();
    }
    redisProxy = tp.createProxy(PROXY_NAME, PROXY_LISTEN, PROXY_UPSTREAM);

    setupAvoidQuitInDestroyObject();

    // Drop active connections and stop listening on the proxy port, simulating an
    // unreachable Redis instance without actually shutting down the container.
    redisProxy.disable();
  }

  @AfterAll
  public static void cleanup() throws IOException {
    cleanupAvoidQuitInDestroyObject();
    if (redisProxy != null) redisProxy.delete();
  }

  public static void setupAvoidQuitInDestroyObject() throws InterruptedException {
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
    try {
      brokenJedis1.close(); // we need capture/mock to test this properly
    } catch (JedisException e) {
      // Behavior change in commons-pool2 2.13.1: GenericObjectPool#invalidateObject now
      // attempts to replace the invalidated instance, which fails when
      // ConnectionFactory#makeObject() cannot reach the (unavailable) server. The
      // underlying socket failure varies by environment (Connection refused / reset /
      // etc.), so we only assert on the exception type.
      assertInstanceOf(JedisConnectionException.class, e.getCause(),
          "Unexpected cause for broken-resource return: " + e.getCause());
    }
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
