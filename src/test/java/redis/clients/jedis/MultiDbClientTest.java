package redis.clients.jedis;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.exceptions.JedisValidationException;
import redis.clients.jedis.mcf.DatabaseSwitchEvent;
import redis.clients.jedis.mcf.SwitchReason;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Basic tests for MultiDbClient functionality. Tests are parameterized to run against both RESP2
 * and RESP3 protocols. Cache-related tests only run on RESP3 as client-side caching requires RESP3.
 */
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
@Tag("integration")
public class MultiDbClientTest {

  protected final RedisProtocol protocol;

  private MultiDbClient client;
  private static EndpointConfig endpoint1;
  private static EndpointConfig endpoint2;

  private static final ToxiproxyClient tp = new ToxiproxyClient("localhost", 8474);
  private static Proxy redisProxy1;
  private static Proxy redisProxy2;

  public MultiDbClientTest(RedisProtocol protocol) {
    this.protocol = protocol;
  }

  @BeforeAll
  public static void setupAdminClients() throws IOException {
    endpoint1 = Endpoints.getRedisEndpoint("redis-failover-1");
    endpoint2 = Endpoints.getRedisEndpoint("redis-failover-2");
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
  }

  @BeforeEach
  void setUp() {
    // Create a simple resilient client with mock endpoints for testing
    // Disable health checks for faster test execution
    MultiDbConfig clientConfig = MultiDbConfig.builder()
        .database(DatabaseConfig
            .builder(endpoint1.getHostAndPort(),
              endpoint1.getClientConfigBuilder().protocol(protocol).build())
            .weight(100.0f).healthCheckEnabled(false).build())
        .database(DatabaseConfig
            .builder(endpoint2.getHostAndPort(),
              endpoint2.getClientConfigBuilder().protocol(protocol).build())
            .weight(50.0f).healthCheckEnabled(false).build())
        .build();

    client = MultiDbClient.builder().multiDbConfig(clientConfig).build();
  }

  @AfterEach
  void tearDown() {
    if (client != null) {
      client.close();
    }
  }

  @Test
  void testAddRemoveDatabaseWithEndpointInterface() {
    Endpoint newEndpoint = new HostAndPort("unavailable", 6381);

    assertDoesNotThrow(() -> client.addDatabase(newEndpoint, 25.0f,
      endpoint1.getClientConfigBuilder().protocol(protocol).build()));

    assertThat(client.getDatabaseEndpoints(), hasItems(newEndpoint));

    assertDoesNotThrow(() -> client.removeDatabase(newEndpoint));

    assertThat(client.getDatabaseEndpoints(), not(hasItems(newEndpoint)));
  }

  @Test
  void testAddRemoveDatabaseWithDatabaseConfig() {
    // todo : (@ggivo) Replace HostAndPort with Endpoint
    HostAndPort newEndpoint = new HostAndPort("unavailable", 6381);

    DatabaseConfig newConfig = DatabaseConfig
        .builder(newEndpoint, endpoint1.getClientConfigBuilder().protocol(protocol).build())
        .weight(25.0f).build();

    assertDoesNotThrow(() -> client.addDatabase(newConfig));

    assertThat(client.getDatabaseEndpoints(), hasItems(newEndpoint));

    assertDoesNotThrow(() -> client.removeDatabase(newEndpoint));

    assertThat(client.getDatabaseEndpoints(), not(hasItems(newEndpoint)));
  }

  @Test
  void testSetActiveDatabase() {
    Endpoint endpoint = client.getActiveDatabaseEndpoint();

    awaitIsHealthy(endpoint1.getHostAndPort());
    awaitIsHealthy(endpoint2.getHostAndPort());
    // Find a different endpoint to switch to
    Endpoint newEndpoint = client.getDatabaseEndpoints().stream().filter(e -> !e.equals(endpoint))
        .findFirst().orElse(null);
    assertNotNull(newEndpoint);

    // Switch to the new endpoint
    client.setActiveDatabase(newEndpoint);

    assertEquals(newEndpoint, client.getActiveDatabaseEndpoint());
  }

  @Test
  void testBuilderWithMultipleEndpointTypes() {
    MultiDbConfig clientConfig = MultiDbConfig.builder()
        .database(endpoint1.getHostAndPort(), 100.0f,
          endpoint1.getClientConfigBuilder().protocol(protocol).build())
        .database(
          DatabaseConfig
              .builder(endpoint2.getHostAndPort(),
                endpoint2.getClientConfigBuilder().protocol(protocol).build())
              .weight(50.0f).build())
        .build();

    try (MultiDbClient testClient = MultiDbClient.builder().multiDbConfig(clientConfig).build()) {
      assertThat(testClient.getDatabaseEndpoints().size(), equalTo(2));
      assertThat(testClient.getDatabaseEndpoints(),
        hasItems(endpoint1.getHostAndPort(), endpoint2.getHostAndPort()));
    }
  }

  @Test
  public void testForceActiveDatabase() {
    Endpoint endpoint = client.getActiveDatabaseEndpoint();

    // Ensure we have a healthy endpoint to switch to
    awaitIsHealthy(endpoint1.getHostAndPort());
    awaitIsHealthy(endpoint2.getHostAndPort());
    // Find an endpoint that is not the current one
    Endpoint newEndpoint = client.getDatabaseEndpoints().stream().filter(e -> !e.equals(endpoint))
        .findFirst().orElse(null);
    assertNotNull(newEndpoint);

    // Force switch to the new endpoint for 100ms
    client.forceActiveDatabase(newEndpoint, Duration.ofMillis(100).toMillis());

    // Verify the active endpoint has changed
    assertEquals(newEndpoint, client.getActiveDatabaseEndpoint());
  }

  @Test
  public void testForceActiveDatabaseWithNonHealthyEndpoint() {
    // This test needs health checks enabled to detect an unhealthy endpoint
    Endpoint newEndpoint = new HostAndPort("unavailable", 6381);
    DatabaseConfig config = DatabaseConfig
        .builder(newEndpoint, endpoint1.getClientConfigBuilder().protocol(protocol).build())
        .weight(25.0f).healthCheckEnabled(true) // Enable health check to detect unavailable
                                                // endpoint
        .build();
    client.addDatabase(config);

    // Wait for health check to mark it as unhealthy (unavailable endpoint will fail health check)
    await().atMost(Duration.ofSeconds(3)).until(() -> !client.isHealthy(newEndpoint));

    // Now attempting to force switch to an unhealthy endpoint should throw exception
    assertThrows(JedisValidationException.class,
      () -> client.forceActiveDatabase(newEndpoint, Duration.ofMillis(100).toMillis()));
  }

  @Test
  public void testForceActiveDatabaseWithNonExistingEndpoint() {
    Endpoint newEndpoint = new HostAndPort("unavailable", 6381);
    assertThrows(JedisValidationException.class,
      () -> client.forceActiveDatabase(newEndpoint, Duration.ofMillis(100).toMillis()));
  }

  @Test
  public void testWithDatabaseSwitchListener() {

    MultiDbConfig endpointsConfig = MultiDbConfig.builder()
        .database(DatabaseConfig
            .builder(endpoint1.getHostAndPort(),
              endpoint1.getClientConfigBuilder().protocol(protocol).build())
            .weight(100.0f).healthCheckEnabled(false).build())
        .database(DatabaseConfig
            .builder(endpoint2.getHostAndPort(),
              endpoint2.getClientConfigBuilder().protocol(protocol).build())
            .weight(50.0f).healthCheckEnabled(false).build())
        .build();

    Consumer<DatabaseSwitchEvent> eventConsumer;
    List<DatabaseSwitchEvent> events = new ArrayList<>();
    eventConsumer = events::add;

    try (MultiDbClient testClient = MultiDbClient.builder().databaseSwitchListener(eventConsumer)
        .multiDbConfig(endpointsConfig).build()) {

      assertThat(events.size(), equalTo(0));

      // With healthCheckEnabled=false, no need to wait
      testClient.setActiveDatabase(endpoint2.getHostAndPort());

      assertThat(events.size(), equalTo(1));
      assertThat(events.get(0).getEndpoint(), equalTo(endpoint2.getHostAndPort()));
      assertThat(events.get(0).getReason(), equalTo(SwitchReason.FORCED));
    }
  }

  @Test
  void testGetWeight() {
    // Verify we can get the initial weight set during configuration
    float weight1 = client.getWeight(endpoint1.getHostAndPort());
    float weight2 = client.getWeight(endpoint2.getHostAndPort());

    assertEquals(100.0f, weight1);
    assertEquals(50.0f, weight2);
  }

  @Test
  void testSetWeight() {
    Endpoint endpoint = endpoint1.getHostAndPort();

    // Verify initial weight
    assertEquals(100.0f, client.getWeight(endpoint));

    // Set a new weight
    client.setWeight(endpoint, 75.0f);

    // Verify the weight has changed
    assertEquals(75.0f, client.getWeight(endpoint));
  }

  @Test
  void testSetWeightToZero() {
    Endpoint endpoint = endpoint2.getHostAndPort();
    assertThrows(IllegalArgumentException.class, () -> client.setWeight(endpoint, 0.0f));
  }

  @Test
  void testSetWeightMultipleTimes() {
    Endpoint endpoint = endpoint1.getHostAndPort();

    // Set weight multiple times
    client.setWeight(endpoint, 25.0f);
    assertEquals(25.0f, client.getWeight(endpoint));

    client.setWeight(endpoint, 80.0f);
    assertEquals(80.0f, client.getWeight(endpoint));

    client.setWeight(endpoint, 1.0f);
    assertEquals(1.0f, client.getWeight(endpoint));
  }

  @Test
  void testCacheWithMultiDbClient() {
    // Client-side caching requires RESP3
    assumeTrue(protocol == null || protocol == RedisProtocol.RESP3,
      "Client-side caching is only supported with RESP3");

    // Create MultiDbClient with cache enabled
    MultiDbConfig clientConfig = MultiDbConfig.builder()
        .database(DatabaseConfig
            .builder(endpoint1.getHostAndPort(), endpoint1.getClientConfigBuilder().resp3().build())
            .weight(100.0f).healthCheckEnabled(false).build())
        .database(DatabaseConfig
            .builder(endpoint2.getHostAndPort(), endpoint2.getClientConfigBuilder().resp3().build())
            .weight(50.0f).healthCheckEnabled(false).build())
        .build();

    try (MultiDbClient cachedClient = MultiDbClient.builder().multiDbConfig(clientConfig)
        .cacheConfig(CacheConfig.builder().build()).build()) {

      // Verify cache is available
      Cache cache = cachedClient.getCache();
      assertNotNull(cache, "Cache should be available");
      assertEquals(0, cache.getSize(), "Cache should be empty initially");

      // Set some values
      cachedClient.set("key1", "value1");
      cachedClient.set("key2", "value2");
      cachedClient.set("key3", "value3");

      // First read - should cache the values
      assertEquals("value1", cachedClient.get("key1"));
      assertEquals("value2", cachedClient.get("key2"));
      assertEquals("value3", cachedClient.get("key3"));

      // Verify cache has entries
      assertEquals(3, cache.getSize(), "Cache should contain 3 entries");

      // Second read - should hit the cache
      assertEquals("value1", cachedClient.get("key1"));
      assertEquals("value2", cachedClient.get("key2"));

      // Flush the cache
      cache.flush();
      assertEquals(0, cache.getSize(), "Cache should be empty after flush");

      // Read again - should populate cache again
      assertEquals("value1", cachedClient.get("key1"));
      assertEquals(1, cache.getSize(), "Cache should contain 1 entry after reading");
    }
  }

  @Test
  void testCacheWithMultiDbClientPoolRecreation() {
    // Client-side caching requires RESP3
    assumeTrue(protocol == null || protocol == RedisProtocol.RESP3,
      "Client-side caching is only supported with RESP3");

    // Create MultiDbClient with cache enabled and fast failover enabled
    // This tests the scenario where TrackingConnectionPool.from() is called
    // when switching back to a database whose pool was closed during manual switch
    MultiDbConfig clientConfig = MultiDbConfig.builder()
        .database(DatabaseConfig
            .builder(endpoint1.getHostAndPort(), endpoint1.getClientConfigBuilder().resp3().build())
            .weight(100.0f).healthCheckEnabled(false).build())
        .database(DatabaseConfig
            .builder(endpoint2.getHostAndPort(), endpoint2.getClientConfigBuilder().resp3().build())
            .weight(50.0f).healthCheckEnabled(false).build())
        .fastFailover(true) // Close pools when switching databases
        .build();

    try (MultiDbClient cachedClient = MultiDbClient.builder().multiDbConfig(clientConfig)
        .cacheConfig(CacheConfig.builder().build()).build()) {

      // Verify cache is available
      Cache cache = cachedClient.getCache();
      assertNotNull(cache, "Cache should be available");
      assertEquals(0, cache.getSize(), "Cache should be empty initially");

      // Determine the two endpoints
      Endpoint firstEndpoint = endpoint1.getHostAndPort();
      Endpoint secondEndpoint = endpoint2.getHostAndPort();

      // Make sure we start on the first endpoint
      cachedClient.setActiveDatabase(firstEndpoint);
      assertEquals(firstEndpoint, cachedClient.getActiveDatabaseEndpoint());

      // Set some values and cache them on first endpoint
      cachedClient.set("poolrec1", "value1");
      cachedClient.set("poolrec2", "value2");
      assertEquals("value1", cachedClient.get("poolrec1"));
      assertEquals("value2", cachedClient.get("poolrec2"));
      assertEquals(2, cache.getSize(), "Cache should contain 2 entries");

      // Manually switch to the second endpoint
      // With fastFailover=true, this closes the first endpoint's pool
      cachedClient.setActiveDatabase(secondEndpoint);
      assertEquals(secondEndpoint, cachedClient.getActiveDatabaseEndpoint());

      // Perform operations on the second endpoint
      cachedClient.set("poolrec3", "value3");
      assertEquals("value3", cachedClient.get("poolrec3"));

      // Cache should still be functional
      assertSame(cache, cachedClient.getCache(), "Cache instance should be preserved");

      // Now switch back to the first endpoint
      // This triggers TrackingConnectionPool.from() because the pool was closed
      cachedClient.setActiveDatabase(firstEndpoint);
      assertEquals(firstEndpoint, cachedClient.getActiveDatabaseEndpoint());

      // Verify cache continues to work after pool recreation
      // This is the key test - the TrackingConnectionPool.from() code path
      cachedClient.set("poolrec4", "value4");
      assertEquals("value4", cachedClient.get("poolrec4"));

      // Verify cache is still the same instance and functional
      assertSame(cache, cachedClient.getCache(), "Cache instance should still be preserved");
      assertTrue(cachedClient.getCache().getSize() > 0, "Cache should have entries");
    }
  }

  @Test
  void testCacheWithDynamicDatabaseAddition() {
    // Client-side caching requires RESP3
    assumeTrue(protocol == null || protocol == RedisProtocol.RESP3,
      "Client-side caching is only supported with RESP3");

    // Create MultiDbClient with cache enabled - start with just one database
    MultiDbConfig clientConfig = MultiDbConfig.builder()
        .database(DatabaseConfig
            .builder(endpoint1.getHostAndPort(), endpoint1.getClientConfigBuilder().resp3().build())
            .weight(100.0f).healthCheckEnabled(false).build())
        .build();

    try (MultiDbClient cachedClient = MultiDbClient.builder().multiDbConfig(clientConfig)
        .cacheConfig(CacheConfig.builder().build()).build()) {

      Cache cache = cachedClient.getCache();
      assertNotNull(cache, "Cache should be available");

      // Set and cache some values on the first endpoint
      cachedClient.set("dynamic1", "value1");
      assertEquals("value1", cachedClient.get("dynamic1"));
      assertTrue(cache.getSize() > 0, "Cache should have entries");

      // Dynamically add a second database with the same cache
      cachedClient.addDatabase(DatabaseConfig
          .builder(endpoint2.getHostAndPort(), endpoint2.getClientConfigBuilder().resp3().build())
          .weight(50.0f).healthCheckEnabled(false).build());

      // No need to wait for health check since healthCheckEnabled=false

      // Verify both endpoints are available
      assertEquals(2, cachedClient.getDatabaseEndpoints().size());

      // Continue operations - cache should work across database additions
      cachedClient.set("dynamic2", "value2");
      assertEquals("value2", cachedClient.get("dynamic2"));

      // Switch to the newly added database
      cachedClient.setActiveDatabase(endpoint2.getHostAndPort());
      assertEquals(endpoint2.getHostAndPort(), cachedClient.getActiveDatabaseEndpoint());

      // Cache should still be functional after switching
      cachedClient.set("dynamic3", "value3");
      assertEquals("value3", cachedClient.get("dynamic3"));

      assertNotNull(cachedClient.getCache(),
        "Cache should still be available after database switch");
    }
  }

  @Test
  void testCachePreservedAcrossDatabaseSwitches() {
    // Client-side caching requires RESP3
    assumeTrue(protocol == null || protocol == RedisProtocol.RESP3,
      "Client-side caching is only supported with RESP3");

    // Test that cache instance is shared across database switches
    MultiDbConfig clientConfig = MultiDbConfig.builder()
        .database(DatabaseConfig
            .builder(endpoint1.getHostAndPort(), endpoint1.getClientConfigBuilder().resp3().build())
            .weight(100.0f).healthCheckEnabled(false).build())
        .database(DatabaseConfig
            .builder(endpoint2.getHostAndPort(), endpoint2.getClientConfigBuilder().resp3().build())
            .weight(50.0f).healthCheckEnabled(false).build())
        .build();

    try (MultiDbClient cachedClient = MultiDbClient.builder().multiDbConfig(clientConfig)
        .cacheConfig(CacheConfig.builder().maxSize(100).build()).build()) {

      Cache cache = cachedClient.getCache();
      assertNotNull(cache);

      Endpoint firstEndpoint = cachedClient.getActiveDatabaseEndpoint();
      // No need to wait for health checks since healthCheckEnabled=false

      Endpoint secondEndpoint = cachedClient.getDatabaseEndpoints().stream()
          .filter(e -> !e.equals(firstEndpoint)).findFirst().orElse(null);
      assertNotNull(secondEndpoint, "Should have a second endpoint");

      // Set data on first endpoint
      cachedClient.set("shared1", "value1");
      assertEquals("value1", cachedClient.get("shared1"));

      // Switch to second endpoint
      cachedClient.setActiveDatabase(secondEndpoint);
      assertEquals(secondEndpoint, cachedClient.getActiveDatabaseEndpoint());

      // Verify cache is the same instance
      assertSame(cache, cachedClient.getCache(),
        "Cache instance should be preserved across switches");

      // Set data on second endpoint
      cachedClient.set("shared2", "value2");
      assertEquals("value2", cachedClient.get("shared2"));

      // Switch back to first endpoint
      cachedClient.setActiveDatabase(firstEndpoint);
      assertEquals(firstEndpoint, cachedClient.getActiveDatabaseEndpoint());

      // Cache should still be the same instance
      assertSame(cache, cachedClient.getCache(),
        "Cache instance should be the same after switching back");

      // Verify operations still work
      cachedClient.set("shared3", "value3");
      assertEquals("value3", cachedClient.get("shared3"));
    }
  }

  private void awaitIsHealthy(HostAndPort hostAndPort) {
    await().atMost(Duration.ofSeconds(1)).until(() -> client.isHealthy(hostAndPort));
  }

}
