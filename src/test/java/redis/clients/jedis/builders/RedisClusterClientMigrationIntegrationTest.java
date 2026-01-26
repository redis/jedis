package redis.clients.jedis.builders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import redis.clients.jedis.*;

/**
 * Integration test that verifies migration compatibility from the legacy JedisCluster constructor
 * to the new RedisClusterClient.Builder pattern.
 * <p>
 * This test demonstrates that the following legacy JedisCluster constructor:
 * 
 * <pre>
 * public JedisCluster(Set&lt;HostAndPort&gt; clusterNodes, JedisClientConfig clientConfig, 
 *                     int maxAttempts, GenericObjectPoolConfig&lt;Connection&gt; poolConfig)
 * </pre>
 * 
 * can be replaced with the RedisClusterClient.Builder pattern while maintaining the same
 * functionality.
 */
@Tag("integration")
public class RedisClusterClientMigrationIntegrationTest {

  private static EndpointConfig endpoint;

  private static Set<HostAndPort> CLUSTER_NODES;
  private static String PASSWORD;
  private static final int MAX_ATTEMPTS = 3;

  private JedisCluster legacyCluster;
  private RedisClusterClient newCluster;

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("cluster-stable");
    CLUSTER_NODES = new HashSet<>(endpoint.getHostsAndPorts());
    PASSWORD = endpoint.getPassword();
  }

  @BeforeEach
  public void setUp() {
    // Clean up any existing data before each test
    cleanClusterData();
  }

  @AfterEach
  public void tearDown() {
    // Close connections
    if (legacyCluster != null) {
      legacyCluster.close();
      legacyCluster = null;
    }
    if (newCluster != null) {
      newCluster.close();
      newCluster = null;
    }

    // Clean up data after tests
    cleanClusterData();
  }

  /**
   * Test that verifies both approaches can handle cluster operations correctly. Tests constructor:
   * JedisCluster(Set&lt;HostAndPort&gt;, JedisClientConfig, int,
   * GenericObjectPoolConfig&lt;Connection&gt;)
   */
  @Test
  public void testSpringDataRedisConstructor() {
    // Prepare common configuration
    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().password(PASSWORD)
        .socketTimeoutMillis(2000).connectionTimeoutMillis(2000).build();

    GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();

    // Create both cluster clients
    legacyCluster = new JedisCluster(CLUSTER_NODES, clientConfig, MAX_ATTEMPTS, poolConfig);
    newCluster = RedisClusterClient.builder().nodes(CLUSTER_NODES).clientConfig(clientConfig)
        .maxAttempts(MAX_ATTEMPTS).poolConfig(poolConfig).build();

    verifyBothClients(legacyCluster, newCluster);
  }

  /**
   * Test migration from constructor with username, password, clientName, and SSL. Tests
   * constructor: JedisCluster(Set&lt;HostAndPort&gt;, int, int, int, String, String, String,
   * GenericObjectPoolConfig&lt;Connection&gt;, boolean)
   */
  @Test
  public void testConstructorWithUsernamePasswordClientNameAndSsl() {
    int connectionTimeout = 2000;
    int socketTimeout = 2000;
    String username = "default";
    String clientName = "test-client";
    boolean ssl = false; // SSL requires special cluster setup

    GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();

    // Legacy constructor with username
    legacyCluster = new JedisCluster(CLUSTER_NODES, connectionTimeout, socketTimeout, MAX_ATTEMPTS,
        username, PASSWORD, clientName, poolConfig, ssl);

    // New Builder pattern
    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(connectionTimeout).socketTimeoutMillis(socketTimeout)
        .user(username).password(PASSWORD).clientName(clientName).ssl(ssl).build();

    newCluster = RedisClusterClient.builder().nodes(CLUSTER_NODES).clientConfig(clientConfig)
        .maxAttempts(MAX_ATTEMPTS).poolConfig(poolConfig).build();

    verifyBothClients(legacyCluster, newCluster);
  }

  /**
   * Test migration from constructor with password, clientName, and SSL (no username). Tests
   * constructor: JedisCluster(Set&lt;HostAndPort&gt;, int, int, int, String, String,
   * GenericObjectPoolConfig&lt;Connection&gt;, boolean)
   */
  @Test
  public void testConstructorWithPasswordClientNameAndSsl() {
    int connectionTimeout = 2000;
    int socketTimeout = 2000;
    String clientName = "test-client";
    boolean ssl = false; // SSL requires special cluster setup

    GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();

    // Legacy constructor without username
    legacyCluster = new JedisCluster(CLUSTER_NODES, connectionTimeout, socketTimeout, MAX_ATTEMPTS,
        PASSWORD, clientName, poolConfig, ssl);

    // New Builder pattern
    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(connectionTimeout).socketTimeoutMillis(socketTimeout)
        .password(PASSWORD).clientName(clientName).ssl(ssl).build();

    newCluster = RedisClusterClient.builder().nodes(CLUSTER_NODES).clientConfig(clientConfig)
        .maxAttempts(MAX_ATTEMPTS).poolConfig(poolConfig).build();

    verifyBothClients(legacyCluster, newCluster);
  }

  /**
   * Test migration from simple constructor with just nodes and poolConfig. Tests constructor:
   * JedisCluster(Set&lt;HostAndPort&gt;, GenericObjectPoolConfig&lt;Connection&gt;)
   */
  @Test
  public void testConstructorWithNodesAndPoolConfig() {
    GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(8);
    poolConfig.setMaxIdle(8);
    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().password(PASSWORD).build();

    // Legacy constructor
    legacyCluster = new JedisCluster(CLUSTER_NODES, clientConfig, poolConfig);

    // New Builder pattern - need to add password via clientConfig
    newCluster = RedisClusterClient.builder().nodes(CLUSTER_NODES).clientConfig(clientConfig)
        .poolConfig(poolConfig).build();

    verifyBothClients(legacyCluster, newCluster);
  }

  /**
   * Test migration from constructor with connection timeout, socket timeout, maxAttempts, password,
   * and poolConfig. Tests constructor: JedisCluster(Set&lt;HostAndPort&gt;, int, int, int, String,
   * GenericObjectPoolConfig&lt;Connection&gt;)
   */
  @Test
  public void testConstructorWithTimeoutsPasswordAndPoolConfig() {
    int connectionTimeout = 2000;
    int socketTimeout = 2000;

    GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();

    // Legacy constructor
    legacyCluster = new JedisCluster(CLUSTER_NODES, connectionTimeout, socketTimeout, MAX_ATTEMPTS,
        PASSWORD, poolConfig);

    // New Builder pattern
    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(connectionTimeout).socketTimeoutMillis(socketTimeout)
        .password(PASSWORD).build();

    newCluster = RedisClusterClient.builder().nodes(CLUSTER_NODES).clientConfig(clientConfig)
        .maxAttempts(MAX_ATTEMPTS).poolConfig(poolConfig).build();

    verifyBothClients(legacyCluster, newCluster);
  }

  /**
   * Test migration from constructor with timeout, maxAttempts, and poolConfig. Tests constructor:
   * JedisCluster(Set&lt;HostAndPort&gt;, int, int, GenericObjectPoolConfig&lt;Connection&gt;)
   */
  @Test
  public void testConstructorWithTimeoutMaxAttemptsAndPoolConfig() {
    int timeout = 2000;

    GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();
    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().timeoutMillis(timeout)
        .password(PASSWORD).build();

    // Legacy constructor - uses same timeout for connection and socket
    legacyCluster = new JedisCluster(CLUSTER_NODES, timeout, timeout, MAX_ATTEMPTS, PASSWORD,
        poolConfig);

    // New Builder pattern
    newCluster = RedisClusterClient.builder().nodes(CLUSTER_NODES).clientConfig(clientConfig)
        .maxAttempts(MAX_ATTEMPTS).poolConfig(poolConfig).build();

    verifyBothClients(legacyCluster, newCluster);
  }

  /**
   * Test migration from single HostAndPort constructor with poolConfig. Tests constructor:
   * JedisCluster(HostAndPort, GenericObjectPoolConfig&lt;Connection&gt;)
   */
  @Test
  public void testConstructorWithSingleNodeAndPoolConfig() {
    int timeout = 2000;
    HostAndPort singleNode = endpoint.getHostsAndPorts().get(0);
    GenericObjectPoolConfig<Connection> poolConfig = new ConnectionPoolConfig();
    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().password(PASSWORD).build();

    // Legacy constructor with single node
    legacyCluster = new JedisCluster(singleNode, timeout, timeout, MAX_ATTEMPTS, PASSWORD,
        poolConfig);

    // New Builder pattern - need to add password and wrap single node in a Set
    Set<HostAndPort> singleNodeSet = new HashSet<>();
    singleNodeSet.add(singleNode);

    newCluster = RedisClusterClient.builder().nodes(singleNodeSet).clientConfig(clientConfig)
        .poolConfig(poolConfig).build();

    verifyBothClients(legacyCluster, newCluster);
  }

  protected void verifyBothClients(JedisCluster legacyCluster, RedisClusterClient newCluster) {
    // Verify both discovered the cluster nodes
    assertNotNull(legacyCluster.getClusterNodes(), "Legacy cluster should discover cluster nodes");
    assertNotNull(newCluster.getClusterNodes(), "New cluster should discover cluster nodes");

    // Both should have discovered the same number of nodes
    assertEquals(legacyCluster.getClusterNodes().size(), newCluster.getClusterNodes().size(),
      "Both approaches should discover the same number of cluster nodes");

    // Test basic string operations
    String key1 = "test-string-key";
    legacyCluster.set(key1, "value1");
    assertEquals("value1", newCluster.get(key1));

    // Test increment operations
    String key2 = "test-counter-key";
    legacyCluster.set(key2, "0");
    newCluster.incr(key2);
    assertEquals("1", legacyCluster.get(key2));

    // Clean up
    newCluster.del(key1);
    newCluster.del(key2);
  }

  /**
   * Helper method to clean up cluster data before and after tests.
   */
  private void cleanClusterData() {
    // Connect to each stable cluster node and flush data
    for (HostAndPort node : endpoint.getHostsAndPorts()) {
      try (redis.clients.jedis.Jedis jedis = new redis.clients.jedis.Jedis(node)) {
        jedis.auth(PASSWORD);
        jedis.flushDB();
      } catch (Exception e) {
        // Ignore errors during cleanup
      }
    }
  }
}
