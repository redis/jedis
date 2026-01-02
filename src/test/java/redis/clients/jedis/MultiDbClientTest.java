package redis.clients.jedis;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;

import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.exceptions.JedisValidationException;
import redis.clients.jedis.mcf.DatabaseSwitchEvent;
import redis.clients.jedis.mcf.SwitchReason;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Basic tests for MultiDbClient functionality.
 */
@Tag("integration")
public class MultiDbClientTest {

  private MultiDbClient client;
  private static EndpointConfig endpoint1;
  private static EndpointConfig endpoint2;

  private static final ToxiproxyClient tp = new ToxiproxyClient("localhost", 8474);
  private static Proxy redisProxy1;
  private static Proxy redisProxy2;

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

  @BeforeEach
  void setUp() {
    // Create a simple resilient client with mock endpoints for testing
    MultiDbConfig clientConfig = MultiDbConfig.builder()
        .database(endpoint1.getHostAndPort(), 100.0f, endpoint1.getClientConfigBuilder().build())
        .database(endpoint2.getHostAndPort(), 50.0f, endpoint2.getClientConfigBuilder().build())
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

    assertDoesNotThrow(
      () -> client.addDatabase(newEndpoint, 25.0f, DefaultJedisClientConfig.builder().build()));

    assertThat(client.getDatabaseEndpoints(), hasItems(newEndpoint));

    assertDoesNotThrow(() -> client.removeDatabase(newEndpoint));

    assertThat(client.getDatabaseEndpoints(), not(hasItems(newEndpoint)));
  }

  @Test
  void testAddRemoveDatabaseWithDatabaseConfig() {
    // todo : (@ggivo) Replace HostAndPort with Endpoint
    HostAndPort newEndpoint = new HostAndPort("unavailable", 6381);

    DatabaseConfig newConfig = DatabaseConfig
        .builder(newEndpoint, DefaultJedisClientConfig.builder().build()).weight(25.0f).build();

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
    // Ensure we have a healthy endpoint to switch to
    Endpoint newEndpoint = client.getDatabaseEndpoints().stream()
        .filter(e -> e.equals(endpoint) && client.isHealthy(e)).findFirst().orElse(null);
    assertNotNull(newEndpoint);

    // Switch to the new endpoint
    client.setActiveDatabase(newEndpoint);

    assertEquals(newEndpoint, client.getActiveDatabaseEndpoint());
  }

  @Test
  void testBuilderWithMultipleEndpointTypes() {
    MultiDbConfig clientConfig = MultiDbConfig.builder()
        .database(endpoint1.getHostAndPort(), 100.0f, DefaultJedisClientConfig.builder().build())
        .database(DatabaseConfig
            .builder(endpoint2.getHostAndPort(), DefaultJedisClientConfig.builder().build())
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
    Endpoint newEndpoint = client.getDatabaseEndpoints().stream()
        .filter(e -> e.equals(endpoint) && client.isHealthy(e)).findFirst().orElse(null);
    assertNotNull(newEndpoint);

    // Force switch to the new endpoint for 10 seconds
    client.forceActiveDatabase(newEndpoint, Duration.ofMillis(100).toMillis());

    // Verify the active endpoint has changed
    assertEquals(newEndpoint, client.getActiveDatabaseEndpoint());
  }

  @Test
  public void testForceActiveDatabaseWithNonHealthyEndpoint() {
    Endpoint newEndpoint = new HostAndPort("unavailable", 6381);
    client.addDatabase(newEndpoint, 25.0f, DefaultJedisClientConfig.builder().build());

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
            .builder(endpoint1.getHostAndPort(), endpoint1.getClientConfigBuilder().build())
            .weight(100.0f).build())
        .database(DatabaseConfig
            .builder(endpoint2.getHostAndPort(), endpoint2.getClientConfigBuilder().build())
            .weight(50.0f).build())
        .build();

    Consumer<DatabaseSwitchEvent> eventConsumer;
    List<DatabaseSwitchEvent> events = new ArrayList<>();
    eventConsumer = events::add;

    try (MultiDbClient testClient = MultiDbClient.builder().databaseSwitchListener(eventConsumer)
        .multiDbConfig(endpointsConfig).build()) {

      assertThat(events.size(), equalTo(0));

      awaitIsHealthy(endpoint2.getHostAndPort());
      testClient.setActiveDatabase(endpoint2.getHostAndPort());

      assertThat(events.size(), equalTo(1));
      assertThat(events.get(0).getEndpoint(), equalTo(endpoint2.getHostAndPort()));
      assertThat(events.get(0).getReason(), equalTo(SwitchReason.FORCED));
    }
  }

  private void awaitIsHealthy(HostAndPort hostAndPort) {
    await().atMost(Duration.ofSeconds(1)).until(() -> client.isHealthy(hostAndPort));
  }

}
