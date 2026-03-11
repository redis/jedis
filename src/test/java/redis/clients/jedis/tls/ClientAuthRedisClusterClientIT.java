package redis.clients.jedis.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.*;

/**
 * Integration tests for mTLS (mutual TLS) certificate-based authentication with Redis Cluster.
 * <p>
 * Extends {@link ClientAuthIT} to provide cluster-specific client creation and command execution.
 * Also includes cluster-specific tests like node discovery.
 */
public class ClientAuthRedisClusterClientIT extends ClientAuthIT {

  private static final int DEFAULT_REDIRECTIONS = 5;
  private static final ConnectionPoolConfig DEFAULT_POOL_CONFIG = new ConnectionPoolConfig();

  @BeforeAll
  public static void setUpClusterMtlsStores() {
    endpoint = Endpoints.getRedisEndpoint("cluster-mtls");
    setUpMtlsStoresForEndpoint(endpoint,
      ClientAuthRedisClusterClientIT.class.getSimpleName());
  }

  @Override
  protected UnifiedJedis createClient(SslOptions sslOptions) {
    return RedisClusterClient.builder().nodes(new HashSet<>(endpoint.getHostsAndPorts()))
        .clientConfig(DefaultJedisClientConfig.builder().sslOptions(sslOptions).build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build();
  }

  @Override
  protected String executeAclWhoAmI(UnifiedJedis client) {
    RedisClusterClient clusterClient = (RedisClusterClient) client;
    return clusterClient.executeCommand(new CommandObject<>(
        new ClusterCommandArguments(Protocol.Command.ACL).add("WHOAMI"), BuilderFactory.STRING));
  }

  /**
   * Cluster-specific test: Verifies that cluster node discovery works with mTLS.
   */
  @Test
  public void discoverClusterNodesWithMtls() {
    SslOptions sslOptions = createMtlsSslOptionsUser1();

    try (RedisClusterClient cluster = (RedisClusterClient) createClient(sslOptions)) {
      Map<String, ?> clusterNodes = cluster.getClusterNodes();
      // Should discover all 3 cluster nodes
      assertEquals(3, clusterNodes.size());
      assertTrue(clusterNodes.containsKey(endpoint.getHostAndPort(0).toString()));
      assertTrue(clusterNodes.containsKey(endpoint.getHostAndPort(1).toString()));
      assertTrue(clusterNodes.containsKey(endpoint.getHostAndPort(2).toString()));
    }
  }
}
