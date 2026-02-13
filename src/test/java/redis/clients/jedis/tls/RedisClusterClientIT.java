package redis.clients.jedis.tls;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static redis.clients.jedis.util.TlsUtil.*;

import java.util.Collections;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.*;
import redis.clients.jedis.util.TlsUtil;
import redis.clients.jedis.exceptions.JedisClusterOperationException;

@SinceRedisVersion(value = "7.0.0", message = "Redis 6.2.x returns non-tls port in CLUSTER SLOTS command. Enable for  6.2.x after test is fixed.")
public class RedisClusterClientIT extends RedisClusterTestBase {

  private static final int DEFAULT_REDIRECTIONS = 5;
  private static final ConnectionPoolConfig DEFAULT_POOL_CONFIG = new ConnectionPoolConfig();

  @Test
  public void testSSLDiscoverNodesAutomatically() {
    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword())
            .sslOptions(createSslOptions()).hostAndPortMapper(hostAndPortMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {
      Map<String, ?> clusterNodes = jc.getClusterNodes();
      assertEquals(6, clusterNodes.size());
      assertTrue(clusterNodes.containsKey(tlsEndpoint.getHostAndPort(0).toString()));
      assertTrue(clusterNodes.containsKey(tlsEndpoint.getHostAndPort(1).toString()));
      assertTrue(clusterNodes.containsKey(tlsEndpoint.getHostAndPort(2).toString()));
      jc.get("foo");
    }
  }

  @Test
  public void testSSLWithoutPortMap() {
    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(
          DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword()).ssl(true).build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {
      Map<String, ?> clusterNodes = jc.getClusterNodes();
      assertEquals(6, clusterNodes.size());
      assertTrue(clusterNodes.containsKey(tlsEndpoint.getHostAndPort(0).toString()));
      assertTrue(clusterNodes.containsKey(tlsEndpoint.getHostAndPort(1).toString()));
      assertTrue(clusterNodes.containsKey(tlsEndpoint.getHostAndPort(2).toString()));
      jc.get("foo");
    }
  }

  @Test
  public void connectByIpAddress() {
    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword())
            .sslOptions(createSslOptions()).hostAndPortMapper(hostAndPortMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {
      jc.get("foo");
    }
  }

  @Test
  public void connectToNodesFailsWithSSLParametersAndNoHostMapping() {
    final SSLParameters sslParameters = new SSLParameters();
    sslParameters.setEndpointIdentificationAlgorithm("HTTPS");

    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(new HostAndPort("localhost", tlsEndpoint.getPort())))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword())
            .sslOptions(createSslOptions()).sslParameters(sslParameters).hostAndPortMapper(portMap)
            .build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {
      jc.get("foo");
      fail("It should fail after all cluster attempts.");
    } catch (JedisClusterOperationException e) {
      // initial connection to localhost works, but subsequent connections to nodes use 127.0.0.1
      // and fail hostname verification
      assertThat(e.getMessage(), anyOf(containsString("No more cluster attempts left."),
        containsString("Cluster retry deadline exceeded.")));
    }
  }

  @Test
  public void connectToNodesSucceedsWithSSLParametersAndHostMapping() {
    final SSLParameters sslParameters = new SSLParameters();
    sslParameters.setEndpointIdentificationAlgorithm("HTTPS");

    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword())
            .sslOptions(createSslOptions()).sslParameters(sslParameters)
            .hostAndPortMapper(hostAndPortMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {
      jc.get("foo");
    }
  }

  @Test
  public void connectByIpAddressFailsWithSSLParameters() {
    final SSLParameters sslParameters = new SSLParameters();
    sslParameters.setEndpointIdentificationAlgorithm("HTTPS");

    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword())
            .sslOptions(createSslOptions()).sslParameters(sslParameters)
            .hostAndPortMapper(hostAndPortMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {
    } catch (JedisClusterOperationException e) {
      assertEquals("Could not initialize cluster slots cache.", e.getMessage());
    }
  }

  @Test
  public void connectWithCustomHostNameVerifier() {
    HostnameVerifier hostnameVerifier = new TlsUtil.BasicHostnameVerifier();
    HostnameVerifier localhostVerifier = new TlsUtil.LocalhostVerifier();

    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(new HostAndPort("localhost", tlsEndpoint.getPort())))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword())
            .ssl(true).hostnameVerifier(hostnameVerifier).hostAndPortMapper(portMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {
      jc.get("foo");
      fail("It should fail after all cluster attempts.");
    } catch (JedisClusterOperationException e) {
      // initial connection made with 'localhost' but subsequent connections to nodes use 127.0.0.1
      // which causes custom hostname verification to fail
      assertThat(e.getMessage(), anyOf(containsString("No more cluster attempts left."),
        containsString("Cluster retry deadline exceeded.")));
    }

    try (RedisClusterClient jc2 = RedisClusterClient.builder()
        .nodes(Collections.singleton(new HostAndPort("127.0.0.1", tlsEndpoint.getPort())))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword())
            .ssl(true).hostnameVerifier(hostnameVerifier).hostAndPortMapper(portMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {
    } catch (JedisClusterOperationException e) {
      // JedisNoReachableClusterNodeException exception occurs from not being able to connect
      // since the socket factory fails the hostname verification
      assertEquals("Could not initialize cluster slots cache.", e.getMessage());
    }

    try (RedisClusterClient jc3 = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword())
            .ssl(true).hostnameVerifier(localhostVerifier).hostAndPortMapper(portMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {
      jc3.get("foo");
    }
  }

  @Test
  public void connectWithCustomSocketFactory() throws Exception {
    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(
          DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword()).ssl(true)
              .sslSocketFactory(sslSocketFactoryForEnv(tlsEndpoint.getCertificatesLocation()))
              .hostAndPortMapper(portMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {
      assertEquals(6, jc.getClusterNodes().size());
      jc.get("foo");
    }
  }

  @Test
  public void connectWithEmptyTrustStore() throws Exception {
    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword())
            .ssl(true).sslSocketFactory(createTrustNoOneSslSocketFactory()).build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {
    } catch (JedisClusterOperationException e) {
      assertEquals("Could not initialize cluster slots cache.", e.getMessage());
    }
  }

}
