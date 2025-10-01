package redis.clients.jedis;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;

import redis.clients.jedis.MultiClusterClientConfig.ClusterConfig;
import redis.clients.jedis.exceptions.JedisValidationException;
import redis.clients.jedis.mcf.ClusterSwitchEventArgs;
import redis.clients.jedis.mcf.SwitchReason;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Basic tests for ResilientJedisClient functionality.
 */
public class ResilientRedisClientTest {

  private ResilientRedisClient client;
  private static final EndpointConfig endpoint1 = HostAndPorts.getRedisEndpoint("redis-failover-1");
  private static final EndpointConfig endpoint2 = HostAndPorts.getRedisEndpoint("redis-failover-2");

  private static final ToxiproxyClient tp = new ToxiproxyClient("localhost", 8474);
  private static Proxy redisProxy1;
  private static Proxy redisProxy2;

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


  @BeforeEach
  void setUp() {
    // Create a simple resilient client with mock endpoints for testing
    MultiClusterClientConfig clientConfig = MultiClusterClientConfig.builder()
            .endpoint(endpoint1.getHostAndPort(), 100.0f, endpoint1.getClientConfigBuilder().build())
            .endpoint(endpoint2.getHostAndPort(), 50.0f, endpoint2.getClientConfigBuilder().build())
            .build();

    client = ResilientRedisClient.builder()
            .multiClusterConfig(clientConfig)
            .build();
  }


  @AfterEach
  void tearDown() {
    if (client != null) {
      client.close();
    }
  }

  @Test
  void testAddRemoveEndpointWithEndpointInterface() {
    Endpoint newEndpoint = new HostAndPort("unavailable", 6381);
    
    assertDoesNotThrow(() -> client.addEndpoint(newEndpoint, 25.0f, DefaultJedisClientConfig.builder().build()));

    assertThat(client.getEndpoints(), hasItems(newEndpoint));

    assertDoesNotThrow(() -> client.removeEndpoint(newEndpoint));

    assertThat(client.getEndpoints(), not(hasItems(newEndpoint)));
  }

  @Test
  void testAddRemoveEndpointWithClusterConfig() {
    // todo : (@ggivo) Replace HostAndPort with Endpoint
    HostAndPort newEndpoint = new HostAndPort("unavailable", 6381);

    ClusterConfig newConfig = ClusterConfig.builder(newEndpoint, DefaultJedisClientConfig.builder().build())
        .weight(25.0f)
        .build();

    assertDoesNotThrow(() -> client.addEndpoint(newConfig));

    assertThat(client.getEndpoints(), hasItems(newEndpoint));

    assertDoesNotThrow(() -> client.removeEndpoint(newEndpoint));

    assertThat(client.getEndpoints(), not(hasItems(newEndpoint)));
  }

  @Test
  void testSetActiveEndpoint() {
    Endpoint endpoint = client.getActiveEndpoint();
    client.setActiveEndpoint(endpoint);

    // Ensure we have a healthy endpoint to switch to
    Endpoint newEndpoint = client.getEndpoints().stream()
            .filter(e -> e.equals(endpoint) && client.isHealthy(e)).findFirst().orElse(null);
    assertNotNull(newEndpoint);

    // Switch to the new endpoint
    client.setActiveEndpoint(newEndpoint);

    assertEquals(newEndpoint, client.getMultiClusterProvider().getCluster().getEndpoint());
  }

  @Test
  void testBuilderWithMultipleEndpointTypes() {
    MultiClusterClientConfig clientConfig = MultiClusterClientConfig.builder()
            .endpoint(endpoint1.getHostAndPort(), 100.0f, DefaultJedisClientConfig.builder().build())
            .endpoint(ClusterConfig.builder(endpoint2.getHostAndPort(), DefaultJedisClientConfig.builder().build()).weight(50.0f).build())
            .build();

    try (ResilientRedisClient testClient = ResilientRedisClient.builder().multiClusterConfig(clientConfig).build()) {
      assertThat(testClient.getEndpoints().size(), equalTo(2));
      assertThat(testClient.getEndpoints(), hasItems(endpoint1.getHostAndPort(), endpoint2.getHostAndPort()));
    }
  }

  @Test
  public void testForceActiveEndpoint() {
    Endpoint endpoint = client.getActiveEndpoint();
    client.setActiveEndpoint(endpoint);

    // Ensure we have a healthy endpoint to switch to
    Endpoint newEndpoint = client.getEndpoints().stream()
            .filter(e -> e.equals(endpoint) && client.isHealthy(e)).findFirst().orElse(null);
    assertNotNull(newEndpoint);

    // Force switch to the new endpoint for 10 seconds
    client.forceActiveEndpoint(newEndpoint, Duration.ofMillis(100).toMillis());

    // Verify the active endpoint has changed
    assertEquals(newEndpoint, client.getMultiClusterProvider().getCluster().getEndpoint());
  }

  @Test
  public void testForceActiveEndpointWithNonHealthyEndpoint() {
    Endpoint newEndpoint = new HostAndPort("unavailable", 6381);
    client.addEndpoint(newEndpoint, 25.0f, DefaultJedisClientConfig.builder().build());

    assertThrows(JedisValidationException.class, () -> client.forceActiveEndpoint(newEndpoint, Duration.ofMillis(100).toMillis()));
  }

  @Test
  public void testForceActiveEndpointWithNonExistingEndpoint() {
    Endpoint newEndpoint = new HostAndPort("unavailable", 6381);
    assertThrows(JedisValidationException.class, () -> client.forceActiveEndpoint(newEndpoint, Duration.ofMillis(100).toMillis()));
  }

  @Test
  public void testWithClusterSwitchListener() {

    MultiClusterClientConfig endpointsConfig = MultiClusterClientConfig.builder().endpoint(
                    ClusterConfig.builder(endpoint1.getHostAndPort(), endpoint1.getClientConfigBuilder().build()).weight(100.0f)
                            .build()).endpoint(
                    ClusterConfig.builder(endpoint2.getHostAndPort(), endpoint2.getClientConfigBuilder().build()).weight(50.0f).build())
            .build();

    Consumer<ClusterSwitchEventArgs> eventConsumer;
    List<ClusterSwitchEventArgs> events = new ArrayList<>();
    eventConsumer = events::add;

    try ( ResilientRedisClient testClient = ResilientRedisClient.builder()
                .clusterSwitchListener(eventConsumer).multiClusterConfig(endpointsConfig).build()) {

      assertThat(events.size(), equalTo(0));
      testClient.setActiveEndpoint(endpoint2.getHostAndPort());

      assertThat(events.size(), equalTo(1));
      assertThat(events.get(0).getEndpoint(), equalTo(endpoint2.getHostAndPort()));
      assertThat(events.get(0).getReason(), equalTo(SwitchReason.FORCED));
    }
  }
}
