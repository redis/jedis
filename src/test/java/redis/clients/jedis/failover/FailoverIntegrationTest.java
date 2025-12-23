package redis.clients.jedis.failover;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.Toxic;
import eu.rekawek.toxiproxy.model.ToxicDirection;
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
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiDbClient;
import redis.clients.jedis.MultiDbConfig;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.mcf.MultiDbConnectionProvider;
import redis.clients.jedis.scenario.RecommendedSettings;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("failover")
public class FailoverIntegrationTest {

  private static final EndpointConfig endpoint1 = Endpoints.getRedisEndpoint("redis-failover-1");
  private static final EndpointConfig endpoint2 = Endpoints.getRedisEndpoint("redis-failover-2");

  private static final ToxiproxyClient tp = new ToxiproxyClient("localhost", 8474);
  public static ExecutorService executor = Executors.newCachedThreadPool();
  public static Pattern pattern = Pattern.compile("run_id:([a-f0-9]+)");
  private static Proxy redisProxy1;
  private static Proxy redisProxy2;
  private static UnifiedJedis jedis1;
  private static UnifiedJedis jedis2;
  private static String JEDIS1_ID = "";
  private static String JEDIS2_ID = "";
  private MultiDbConnectionProvider provider;
  private MultiDbClient failoverClient;

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

    // Create default provider and client for most tests
    provider = createProvider();
    failoverClient = MultiDbClient.builder().connectionProvider(provider).build();
  }

  @AfterEach
  public void cleanup() throws IOException {
    failoverClient.close();
    jedis1.close();
    jedis2.close();
  }

  /**
   * Tests the automatic failover behavior when a Redis server becomes unavailable. This test
   * verifies:
   * <ol>
   * <li>Initial connection to the first Redis server works correctly</li>
   * <li>Disable access, the first command throws</li>
   * <li>Command failure is propagated to the caller</li>
   * <li>CB transitions to OPEN, failover is initiated and following commands are sent to the next
   * endpoint</li>
   * <li>Second server is also disabled, all commands fail with JedisConnectionException and error
   * is propagated to the caller</li>
   * </ol>
   */
  @Test
  public void testAutomaticFailoverWhenServerBecomesUnavailable() throws Exception {
    assertThat(getNodeId(failoverClient.info("server")), equalTo(JEDIS1_ID));

    await().atMost(1, TimeUnit.SECONDS).pollInterval(50, TimeUnit.MILLISECONDS)
        .until(() -> provider.getDatabase(endpoint2.getHostAndPort()).isHealthy());

    // Disable redisProxy1
    redisProxy1.disable();

    // Endpoint 1 not available
    // 1. First call should throw JedisConnectionException and trigger failover
    // 2. Endpoint 1 CB transitions to OPEN
    // 3. Subsequent calls should be routed to Endpoint 2
    assertThrows(JedisConnectionException.class, () -> failoverClient.info("server"));

    assertThat(provider.getDatabase(endpoint1.getHostAndPort()).getCircuitBreaker().getState(),
      equalTo(CircuitBreaker.State.FORCED_OPEN));

    // Check that the failoverClient is now using Endpoint 2
    assertThat(getNodeId(failoverClient.info("server")), equalTo(JEDIS2_ID));

    // Disable also second proxy
    redisProxy2.disable();

    // Endpoint1 and Endpoint2 are NOT available,
    assertThrows(JedisConnectionException.class, () -> failoverClient.info("server"));
    assertThat(provider.getDatabase(endpoint2.getHostAndPort()).getCircuitBreaker().getState(),
      equalTo(CircuitBreaker.State.FORCED_OPEN));

    // and since no other nodes are available, it should propagate the errors to the caller
    // subsequent calls
    assertThrows(JedisConnectionException.class, () -> failoverClient.info("server"));
  }

  @Test
  public void testManualFailoverNewCommandsAreSentToActiveDatabase() throws InterruptedException {
    assertThat(getNodeId(failoverClient.info("server")), equalTo(JEDIS1_ID));

    await().atMost(1, TimeUnit.SECONDS).pollInterval(50, TimeUnit.MILLISECONDS)
        .until(() -> provider.getDatabase(endpoint2.getHostAndPort()).isHealthy());

    provider.setActiveDatabase(endpoint2.getHostAndPort());

    assertThat(getNodeId(failoverClient.info("server")), equalTo(JEDIS2_ID));
  }

  private List<MultiDbConfig.DatabaseConfig> getDatabaseConfigs(JedisClientConfig clientConfig,
      EndpointConfig... endpoints) {

    int weight = endpoints.length;
    AtomicInteger weightCounter = new AtomicInteger(weight);
    return Arrays.stream(endpoints)
        .map(e -> MultiDbConfig.DatabaseConfig.builder(e.getHostAndPort(), clientConfig)
            .weight(1.0f / weightCounter.getAndIncrement()).healthCheckEnabled(false).build())
        .collect(Collectors.toList());
  }

  @Test
  @Timeout(5)
  public void testManualFailoverInflightCommandsCompleteGracefully()
      throws ExecutionException, InterruptedException {

    await().atMost(1, TimeUnit.SECONDS).pollInterval(50, TimeUnit.MILLISECONDS)
        .until(() -> provider.getDatabase(endpoint2.getHostAndPort()).isHealthy());

    assertThat(getNodeId(failoverClient.info("server")), equalTo(JEDIS1_ID));

    // We will trigger failover while this command is in-flight
    Future<List<String>> blpop = executor.submit(() -> failoverClient.blpop(1000, "test-list"));

    provider.setActiveDatabase(endpoint2.getHostAndPort());

    // After the manual failover, commands should be executed against Endpoint 2
    assertThat(getNodeId(failoverClient.info("server")), equalTo(JEDIS2_ID));

    // Failover was manually triggered, and there were no errors
    // previous endpoint CB should still be in CLOSED state
    assertThat(provider.getDatabase(endpoint1.getHostAndPort()).getCircuitBreaker().getState(),
      equalTo(CircuitBreaker.State.CLOSED));

    jedis1.rpush("test-list", "somevalue");

    assertThat(blpop.get(), equalTo(Arrays.asList("test-list", "somevalue")));
  }

  /**
   * Verify that in-flight commands that complete with error during manual failover will propagate
   * the error to the caller and toggle CB to OPEN state.
   */
  @Test
  public void testManualFailoverInflightCommandsWithErrorsPropagateError() throws Exception {
    assertThat(getNodeId(failoverClient.info("server")), equalTo(JEDIS1_ID));

    await().atMost(1, TimeUnit.SECONDS).pollInterval(50, TimeUnit.MILLISECONDS)
        .until(() -> provider.getDatabase(endpoint2.getHostAndPort()).isHealthy());

    Future<List<String>> blpop = executor.submit(() -> failoverClient.blpop(10000, "test-list-1"));

    // trigger failover manually
    provider.setActiveDatabase(endpoint2.getHostAndPort());
    Future<String> infoCmd = executor.submit(() -> failoverClient.info("server"));

    // After the manual failover, commands should be executed against Endpoint 2
    assertThat(getNodeId(infoCmd.get()), equalTo(JEDIS2_ID));

    // Disable redisProxy1 to drop active connections and trigger an error
    redisProxy1.disable();

    // previously submitted command should fail with JedisConnectionException
    ExecutionException exception = assertThrows(ExecutionException.class, blpop::get);
    assertThat(exception.getCause(), instanceOf(JedisConnectionException.class));

    // Check that the circuit breaker for Endpoint 1 is open after the error
    assertThat(provider.getDatabase(endpoint1.getHostAndPort()).getCircuitBreaker().getState(),
      equalTo(CircuitBreaker.State.OPEN));

    // Ensure that the active cluster is still Endpoint 2
    assertThat(getNodeId(failoverClient.info("server")), equalTo(JEDIS2_ID));
  }

  /**
   * Tests that the CircuitBreaker counts each command error separately, and not just after all
   * retries are exhausted. This ensures that the circuit breaker opens based on the actual number
   * of send commands with failures, and not based on the number of logical operations.
   */
  @Test
  public void testCircuitBreakerCountsEachConnectionErrorSeparately() throws IOException {
    MultiDbConfig failoverConfig = new MultiDbConfig.Builder(getDatabaseConfigs(
      DefaultJedisClientConfig.builder().socketTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS)
          .connectionTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS).build(),
      endpoint1, endpoint2))
          .commandRetry(MultiDbConfig.RetryConfig.builder().maxAttempts(2).waitDuration(1).build())
          .failureDetector(MultiDbConfig.CircuitBreakerConfig.builder().slidingWindowSize(3)
              .minNumOfFailures(2).failureRateThreshold(50f) // %50 failure rate
              .build())
          .build();

    MultiDbConnectionProvider provider = new MultiDbConnectionProvider(failoverConfig);
    try (MultiDbClient client = MultiDbClient.builder().connectionProvider(provider).build()) {
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
      assertThat(provider.getDatabase(endpoint1.getHostAndPort()).getCircuitBreaker().getState(),
        equalTo(CircuitBreaker.State.FORCED_OPEN));

      // Next command should be routed to the second endpoint
      // Command 2
      assertThat(getNodeId(client.info("server")), equalTo(JEDIS2_ID));

      // Command 3
      assertThat(getNodeId(client.info("server")), equalTo(JEDIS2_ID));

    }

  }

  /**
   * Tests that in-flight commands are retried after automatic failover when retry is enabled.
   */
  @Test
  public void testInflightCommandsAreRetriedAfterFailover() throws Exception {

    MultiDbConnectionProvider customProvider = createProvider(
      builder -> builder.retryOnFailover(true));

    // Create a custom client with retryOnFailover enabled for this specific test
    try (MultiDbClient customClient = MultiDbClient.builder().connectionProvider(customProvider)
        .build()) {

      assertThat(getNodeId(customClient.info("server")), equalTo(JEDIS1_ID));
      Thread.sleep(1000);

      // We will trigger failover while this command is in-flight
      Future<List<String>> blpop = executor.submit(() -> customClient.blpop(10000, "test-list-1"));

      // Simulate error by sending more than 100 bytes. This causes the connection close, and
      // CB -> OPEN, failover will be actually triggered by the next command
      redisProxy1.toxics().limitData("simulate-socket-failure", ToxicDirection.UPSTREAM, 100);
      assertThrows(JedisConnectionException.class,
        () -> customClient.set("test-key", generateTestValue(150)));

      // Actual failover is performed on first command received after CB is OPEN
      // TODO : Remove second command. Once we Refactor existing code to perform actual failover
      // immediately when CB state change to OPEN/FORCED_OPENs
      assertThat(getNodeId(customClient.info("server")), equalTo(JEDIS2_ID));
      // Check that the circuit breaker for Endpoint 1 is open
      assertThat(
        customProvider.getDatabase(endpoint1.getHostAndPort()).getCircuitBreaker().getState(),
        equalTo(CircuitBreaker.State.FORCED_OPEN));

      // Disable redisProxy1 to enforce connection drop for the in-flight (blpop) command
      redisProxy1.disable();

      // The in-flight command should be retried and succeed after failover
      customClient.rpush("test-list-1", "somevalue");
      assertThat(blpop.get(), equalTo(Arrays.asList("test-list-1", "somevalue")));
    }
  }

  /**
   * Tests that in-flight commands are not retried after automatic failover when retry is disabled.
   */
  @Test
  public void testInflightCommandsAreNotRetriedAfterFailover() throws Exception {
    // Create a custom provider and client with retry disabled for this specific test
    MultiDbConnectionProvider customProvider = createProvider(
      builder -> builder.retryOnFailover(false));

    try (MultiDbClient customClient = MultiDbClient.builder().connectionProvider(customProvider)
        .build()) {

      assertThat(getNodeId(customClient.info("server")), equalTo(JEDIS1_ID));
      Future<List<String>> blpop = executor.submit(() -> customClient.blpop(500, "test-list-2"));

      // Simulate error by sending more than 100 bytes. This causes connection close, and triggers
      // failover
      redisProxy1.toxics().limitData("simulate-socket-failure", ToxicDirection.UPSTREAM, 100);
      assertThrows(JedisConnectionException.class,
        () -> customClient.set("test-key", generateTestValue(150)));

      // Check that the circuit breaker for Endpoint 1 is open
      assertThat(
        customProvider.getDatabase(endpoint1.getHostAndPort()).getCircuitBreaker().getState(),
        equalTo(CircuitBreaker.State.FORCED_OPEN));

      // Disable redisProxy1 to enforce the current blpop command failure
      redisProxy1.disable();

      // The in-flight command should fail since the retry is disabled
      ExecutionException exception = assertThrows(ExecutionException.class,
        () -> blpop.get(1, TimeUnit.SECONDS));
      assertThat(exception.getCause(), instanceOf(JedisConnectionException.class));
    }
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

  /**
   * Creates a MultiDbConnectionProvider with standard configuration
   * @return A configured provider
   */
  private MultiDbConnectionProvider createProvider() {
    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .socketTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS)
        .connectionTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS).build();

    MultiDbConfig failoverConfig = new MultiDbConfig.Builder(
        getDatabaseConfigs(clientConfig, endpoint1, endpoint2))
            .commandRetry(
              MultiDbConfig.RetryConfig.builder().maxAttempts(1).waitDuration(1).build())
            .failureDetector(MultiDbConfig.CircuitBreakerConfig.builder().slidingWindowSize(3)
                .minNumOfFailures(1).failureRateThreshold(50f).build())
            .build();

    return new MultiDbConnectionProvider(failoverConfig);
  }

  /**
   * Creates a MultiDbConnectionProvider with standard configuration
   * @return A configured provider
   */
  private MultiDbConnectionProvider createProvider(
      Function<MultiDbConfig.Builder, MultiDbConfig.Builder> configCustomizer) {
    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .socketTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS)
        .connectionTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS).build();

    MultiDbConfig.Builder builder = new MultiDbConfig.Builder(
        getDatabaseConfigs(clientConfig, endpoint1, endpoint2))
            .commandRetry(
              MultiDbConfig.RetryConfig.builder().maxAttempts(1).waitDuration(1).build())
            .failureDetector(MultiDbConfig.CircuitBreakerConfig.builder().slidingWindowSize(3)
                .minNumOfFailures(1).failureRateThreshold(50f).build());

    if (configCustomizer != null) {
      builder = configCustomizer.apply(builder);
    }

    return new MultiDbConnectionProvider(builder.build());
  }
}
