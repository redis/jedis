package redis.clients.jedis.failover;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.Toxic;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiClusterClientConfig;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;
import redis.clients.jedis.scenario.RecommendedSettings;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

// TODO: Dynamic port for proxy
@Tag("failover")
public class FailoverIntegrationTest {

  private static final EndpointConfig endpoint1 = HostAndPorts.getRedisEndpoint("redis-failover-1");
  private static final EndpointConfig endpoint2 = HostAndPorts.getRedisEndpoint("redis-failover-2");

  private static final ToxiproxyClient tp = new ToxiproxyClient("localhost", 8474);
  public static ExecutorService executor = Executors.newCachedThreadPool();
  public static Pattern pattern = Pattern.compile("run_id:([a-f0-9]+)");
  private static Proxy redisProxy1;
  private static Proxy redisProxy2;
  private static UnifiedJedis jedis1;
  private static UnifiedJedis jedis2;
  private static String JEDIS1_ID = "";
  private static String JEDIS2_ID = "";
  MultiClusterPooledConnectionProvider provider;
  private UnifiedJedis failoverClient;

  @BeforeAll
  public static void setupAdminClients() throws IOException {
    if (tp.getProxyOrNull("redis-1") != null) {
      tp.getProxy("redis-1").delete();
    }
    if (tp.getProxyOrNull("redis-2") != null) {
      tp.getProxy("redis-2").delete();
    }

    redisProxy1 = tp.createProxy("redis-1", "0.0.0.0:29379", "redis-failover-1:9379");
    redisProxy2 = tp.createProxy("redis-2", "0.0.0.0:29380", "redis-failover-2:9380");
  }

  @AfterAll
  public static void cleanupAdminClients() throws IOException {
    if (redisProxy1 != null) redisProxy1.delete();
    if (redisProxy2 != null) redisProxy2.delete();

    jedis1.close();
    jedis2.close();

    executor.shutdown();
  }

  private static String getNodeId(UnifiedJedis client) {

    return getNodeId(client.info("server"));
  }

  private static String getNodeId(String info) {

    Matcher m = pattern.matcher(info);
    if (m.find()) {
      return m.group(1);
    }
    return null;
  }

  /**
   * Generates a string of a specific byte size.
   * @param byteSize The desired size in bytes
   * @return A string of the specified byte size
   */
  private static String generateTestValue(int byteSize) {
    StringBuilder value = new StringBuilder(byteSize);
    for (int i = 0; i < byteSize; i++) {
      value.append('x');
    }
    return value.toString();
  }

  @BeforeEach
  public void setup() throws IOException {
    tp.getProxies().forEach(proxy -> {
      try {
        proxy.enable();
        for (Toxic toxic : proxy.toxics().getAll()) {

          toxic.remove();
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    jedis1 = new UnifiedJedis(endpoint1.getHostAndPort(),
        DefaultJedisClientConfig.builder().build());
    jedis2 = new UnifiedJedis(endpoint2.getHostAndPort(),
        DefaultJedisClientConfig.builder().build());

    jedis1.flushAll();
    jedis2.flushAll();

    JEDIS1_ID = getNodeId(jedis1);
    JEDIS2_ID = getNodeId(jedis2);

    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .socketTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS)
        .connectionTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS).build();

    MultiClusterClientConfig failoverConfig = new MultiClusterClientConfig.Builder(
        getClusterConfigs(clientConfig, endpoint1, endpoint2)).retryMaxAttempts(1)
        .retryWaitDuration(1).circuitBreakerSlidingWindowType(COUNT_BASED)
        .circuitBreakerSlidingWindowSize(1).circuitBreakerFailureRateThreshold(100)
        .circuitBreakerSlidingWindowMinCalls(1).build();

    provider = new MultiClusterPooledConnectionProvider(failoverConfig);
    failoverClient = new UnifiedJedis(provider);
  }

  @AfterEach
  public void cleanup() throws IOException {
    failoverClient.close();
    jedis1.close();
    jedis2.close();
  }

  /**
   * Tests the automatic failover behavior when a Redis server becomes unavailable. This test
   * verifies: 1. Initial connection to the first Redis server works correctly 2. When the first
   * server is disabled, the first command throws a JedisConnectionException 3. The circuit breaker
   * for the first endpoint transitions to OPEN state 4. Subsequent commands are automatically
   * routed to the second available endpoint 5. When the second server is also disabled, all
   * commands fail with JedisConnectionException 6. The circuit breaker for the second endpoint also
   * transitions to OPEN state
   */
  @Test
  public void testAutomaticFailoverWhenServerBecomesUnavailable() throws Exception {
    assertThat(getNodeId(failoverClient.info("server")), equalTo(JEDIS1_ID));

    // Disable redisProxy1
    redisProxy1.disable();

    // Endpoint 1 not available
    // 1. First call should should throw JedisConnectionException and trigger failover
    // 1.1. Endpoint1 CB transitions to open
    // 2. Subsequent calls should be routed to Endpoint2
    assertThrows(JedisConnectionException.class, () -> failoverClient.info("server"));

    // Check that the circuit breaker for Endpoint 1 is open
    assertThat(provider.getCluster(1).getCircuitBreaker().getState(),
      equalTo(CircuitBreaker.State.OPEN));

    // Check that the failoverClient is now using Endpoint 2
    assertThat(getNodeId(failoverClient.info("server")), equalTo(JEDIS2_ID));

    // Disable also second proxy
    redisProxy2.disable();

    // Endpoint1 and Endpoint2 are not available,
    assertThrows(JedisConnectionException.class, () -> failoverClient.info("server"));
    assertThat(provider.getCluster(2).getCircuitBreaker().getState(),
      equalTo(CircuitBreaker.State.OPEN));

    // and since no other nodes are available, it should throw an exception for subsequent calls
    assertThrows(JedisConnectionException.class, () -> failoverClient.info("server"));
  }

  @Test
  public void testManualFailoverNewCommandsAreSentToActiveCluster() throws InterruptedException {
    assertThat(getNodeId(failoverClient.info("server")), equalTo(JEDIS1_ID));

    provider.setActiveMultiClusterIndex(2);

    assertThat(getNodeId(failoverClient.info("server")), equalTo(JEDIS2_ID));
  }

  private List<MultiClusterClientConfig.ClusterConfig> getClusterConfigs(
      JedisClientConfig clientConfig, EndpointConfig... endpoints) {

    return Arrays.stream(endpoints)
        .map(e -> new MultiClusterClientConfig.ClusterConfig(e.getHostAndPort(), clientConfig))
        .collect(Collectors.toList());
  }

  @Test
  @Timeout(5)
  public void testManualFailoverInflightCommandsCompleteGracefully()
      throws ExecutionException, InterruptedException {

    assertThat(getNodeId(failoverClient.info("server")), equalTo(JEDIS1_ID));
    Future<List<String>> blpop = executor.submit(() -> {
      try {
        // This command will block until a value is pushed to the list or timeout occurs
        // We will trigger failover while this command is blocking
        return failoverClient.blpop(1000, "test-list");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    provider.setActiveMultiClusterIndex(2);

    // new command should be executed against the new endpoint
    assertThat(getNodeId(failoverClient.info("server")), equalTo(JEDIS2_ID));
    // Since failover was manually triggered and there were no errors
    // previous endpoint CB should be still in CLOSED
    assertThat(provider.getCluster(1).getCircuitBreaker().getState(),
      equalTo(CircuitBreaker.State.CLOSED));

    jedis1.rpush("test-list", "somevalue");

    assertThat(blpop.get(), equalTo(Arrays.asList("test-list", "somevalue")));
  }

  /**
   * Verify that in-flight commands during manual failover fail gracefully with an error will
   * propagate the error to the caller and will toggle CB to OPEN state.
   */
  @Test
  public void testManualFailoverInflightCommandsWithErrorsPropagateError() throws Exception {
    assertThat(getNodeId(failoverClient.info("server")), equalTo(JEDIS1_ID));

    Future<List<String>> blpop = executor.submit(() -> failoverClient.blpop(10000, "test-list-1"));

    // trigger failover manually
    provider.setActiveMultiClusterIndex(2);
    Future<String> infoCmd = executor.submit(() -> failoverClient.info("server"));
    // new command should be executed against the new endpoint
    assertThat(getNodeId(infoCmd.get()), equalTo(JEDIS2_ID));
    // Disable redisProxy1 to simulate an error
    redisProxy1.disable();

    // previously submitted command should fail with JedisConnectionException
    ExecutionException exception = assertThrows(ExecutionException.class, blpop::get);
    assertThat(exception.getCause(), instanceOf(JedisConnectionException.class));

    // Check that the circuit breaker for Endpoint 1 is open after the error
    assertThat(provider.getCluster(1).getCircuitBreaker().getState(),
      equalTo(CircuitBreaker.State.OPEN));

    // Ensure that active cluster is still Endpoint 2
    assertThat(getNodeId(failoverClient.info("server")), equalTo(JEDIS2_ID));
  }

  /**
   * Tests that the CircuitBreaker counts each command error separately, and not just after all
   * retries are exhausted. This ensures that the circuit breaker opens based on the actual number
   * of send commands with failures, and not based on the number of logical operations.
   */
  @Test
  public void testCircuitBreakerCountsEachConnectionErrorSeparately() throws IOException {
    MultiClusterClientConfig failoverConfig = new MultiClusterClientConfig.Builder(
        getClusterConfigs(
          DefaultJedisClientConfig.builder()
              .socketTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS)
              .connectionTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS).build(),
          endpoint1, endpoint2))
        .retryMaxAttempts(2).retryWaitDuration(1).circuitBreakerSlidingWindowType(COUNT_BASED)
        .circuitBreakerSlidingWindowSize(3).circuitBreakerFailureRateThreshold(50) // 50% failure
                                                                                   // rate threshold
        .circuitBreakerSlidingWindowMinCalls(3).build();

    MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
        failoverConfig);
    try (UnifiedJedis client = new UnifiedJedis(provider)) {
      // Verify initial connection to first endpoint
      assertThat(getNodeId(client.info("server")), equalTo(JEDIS1_ID));

      // Disable first endpoint
      redisProxy1.disable();

      // First command should fail and OPEN the circuit breaker immediately
      //
      // If CB is applied after retries:
      // - It would take 2 commands to OPEN CB (error is propagated for both commands)
      // - Failover to the next Endpoint happens on the 3rd command
      //
      // If CB is applied before retries:
      // - It should open after just 1 command with retries
      // - CB is OPEN after the 2nd retry of the first command
      // - Failover to the next Endpoint happens on the 2nd command
      //
      // This test verifies the second case by checking that:
      // 1. CB opens after the first command (with retries)
      // 2. The second command is routed to the second endpoint
      // Command 1
      assertThrows(JedisConnectionException.class, () -> client.info("server"));

      // Circuit breaker should be open after just one command with retries
      assertThat(provider.getCluster(1).getCircuitBreaker().getState(),
        equalTo(CircuitBreaker.State.OPEN));

      // Next command should be routed to the second endpoint
      // Command 2
      assertThat(getNodeId(client.info("server")), equalTo(JEDIS2_ID));

      // Command 3
      assertThat(getNodeId(client.info("server")), equalTo(JEDIS2_ID));

    }
  }

}
