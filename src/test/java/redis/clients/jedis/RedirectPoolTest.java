package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import io.redis.test.annotations.ConditionalOnEnv;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.args.ClientPauseMode;
import redis.clients.jedis.providers.RedirectConnectionProvider;
import redis.clients.jedis.util.EnvCondition;
import redis.clients.jedis.util.TestEnvUtil;

@Tag("integration")
@ConditionalOnEnv(value = TestEnvUtil.ENV_OSS_DOCKER, enabled = true)
public class RedirectPoolTest {

  private static final Logger logger = LoggerFactory.getLogger(RedirectPoolTest.class);
  private static final String NODE1_ENDPOINT = "standalone9-failover";
  private static final String NODE2_ENDPOINT = "standalone10-replica-of-standalone9";

  @RegisterExtension
  public static EnvCondition envCondition = new EnvCondition();

  private static EndpointConfig node1;
  private static EndpointConfig node2;
  private static JedisClientConfig node1ClientConfig;
  private static JedisClientConfig node2ClientConfig;

  private HostAndPort masterAddress;
  private HostAndPort replicaAddress;
  private JedisClientConfig masterClientConfig;

  @BeforeAll
  public static void setUp() {
    node1 = Endpoints.getRedisEndpoint(NODE1_ENDPOINT);
    node2 = Endpoints.getRedisEndpoint(NODE2_ENDPOINT);
    node1ClientConfig = createClientConfig(node1);
    node2ClientConfig = createClientConfig(node2);
    Assumptions.assumeTrue(isRedirectCapabilityAvailable(),
      "Server does not support CLIENT CAPA REDIRECT.");
  }

  private static boolean isRedirectCapabilityAvailable() {
    try (Jedis jedis = new Jedis(node1.getHostAndPort(), node1ClientConfig)) {
      return "PONG".equals(jedis.ping());
    } catch (Exception e) {
      return false;
    }
  }

  @BeforeEach
  public void prepare() {
    String role1;
    String role2;
    try (Jedis jedis1 = new Jedis(node1.getHostAndPort(), node1ClientConfig)) {
      role1 = roleOf(jedis1);
    }
    try (Jedis jedis2 = new Jedis(node2.getHostAndPort(), node2ClientConfig)) {
      role2 = roleOf(jedis2);
    }

    if (isMaster(role1) && isReplica(role2)) {
      masterAddress = node1.getHostAndPort();
      replicaAddress = node2.getHostAndPort();
      masterClientConfig = node1ClientConfig;
    } else if (isMaster(role2) && isReplica(role1)) {
      masterAddress = node2.getHostAndPort();
      replicaAddress = node1.getHostAndPort();
      masterClientConfig = node2ClientConfig;
    } else {
      fail("role not match, node1=" + role1 + ", node2=" + role2);
    }
  }

  private static JedisClientConfig createClientConfig(EndpointConfig endpoint) {
    return endpoint.getClientConfigBuilder().socketTimeoutMillis(60000).autoNegotiateProtocol(false)
        .clientSetInfoConfig(ClientSetInfoConfig.DISABLED)
        .clientCapaConfig(ClientCapaConfig.withRedirect()).build();
  }

  private static String roleOf(Jedis jedis) {
    return ((String) jedis.role().get(0)).toLowerCase(Locale.ROOT);
  }

  private static boolean isMaster(String role) {
    return "master".equals(role);
  }

  private static boolean isReplica(String role) {
    return "slave".equals(role) || "replica".equals(role);
  }

  private JedisClientConfig clientConfigFor(HostAndPort hostAndPort) {
    if (node1.getHostAndPort().equals(hostAndPort)) {
      return node1ClientConfig;
    }
    if (node2.getHostAndPort().equals(hostAndPort)) {
      return node2ClientConfig;
    }
    throw new IllegalArgumentException("Unknown node: " + hostAndPort);
  }

  private void changeMaster() throws Exception {
    prepare();
    try (Jedis masterJedis = new Jedis(masterAddress, clientConfigFor(masterAddress));
        Jedis replicaJedis = new Jedis(replicaAddress, clientConfigFor(replicaAddress))) {
      replicaJedis.readonly();
      masterJedis.clientPause(60000, ClientPauseMode.WRITE);
      try {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (masterJedis.dbSize() != replicaJedis.dbSize()) {
          if (System.nanoTime() > deadline) {
            fail("Replication did not catch up before redirect master change.");
          }
          Thread.sleep(100);
        }
        replicaJedis.replicaofNoOne();
        masterJedis.replicaof(replicaAddress.getHost(), replicaAddress.getPort());
      } finally {
        masterJedis.clientUnpause();
      }
    }
  }

  @Test
  @Timeout(60)
  public void basicRedirect() throws Exception {
    AtomicReference<Throwable> failure = new AtomicReference<>();
    try (UnifiedJedis unifiedJedis = new UnifiedJedis(
        new RedirectConnectionProvider(masterAddress, masterClientConfig), 60,
        Duration.ofSeconds(60))) {
      Thread[] workers = new Thread[4];
      for (int i = 0; i < workers.length; i++) {
        final int workerId = i;
        workers[i] = new Thread(() -> {
          try {
            for (int j = 0; j < 120; j++) {
              Thread.sleep(50);
              String key = "redirect:" + workerId + ':' + j;
              String value = workerId + ":" + j;
              assertEquals("OK", unifiedJedis.set(key, value));
              assertEquals(value, unifiedJedis.get(key));
            }
          } catch (Throwable t) {
            failure.compareAndSet(null, t);
          }
        });
      }

      Thread failover = new Thread(() -> {
        try {
          for (int i = 0; i < 3; i++) {
            Thread.sleep(1500);
            changeMaster();
            logger.info("Changed master {} time(s)", i + 1);
          }
        } catch (Throwable t) {
          failure.compareAndSet(null, t);
        }
      });

      for (Thread worker : workers) {
        worker.start();
      }
      failover.start();
      for (Thread worker : workers) {
        worker.join();
      }
      failover.join();
    }

    Throwable throwable = failure.get();
    if (throwable != null) {
      fail("Redirect should not throw exception", throwable);
    }
  }
}
