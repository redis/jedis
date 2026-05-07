package redis.clients.jedis.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.SslOptions;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * SSL/TLS tests for {@link RedisClient} using SslOptions builder pattern.
 * <p>
 * Tests various SSL/TLS connection configurations including:
 * <ul>
 * <li>Basic SSL connection with truststore</li>
 * <li>Insecure SSL mode (no certificate verification)</li>
 * <li>Custom SSL protocol</li>
 * <li>ACL authentication over SSL</li>
 * </ul>
 */
public class SSLOptionsRedisClientIT extends RedisClientTlsTestBase {

  @ParameterizedTest(name = "connectWithSsl_{0}")
  @MethodSource("sslOptionsProvider")
  void connectWithSsl(String testName, SslOptions ssl) {
    try (RedisClient client = RedisClient.builder().hostAndPort(endpoint.getHostAndPort())
        .clientConfig(endpoint.getClientConfigBuilder().sslOptions(ssl).build()).build()) {
      assertEquals("PONG", client.ping());
    }
  }

  /**
   * Tests ACL authentication over SSL.
   */
  @Test
  public void connectWithAcl() {
    try (RedisClient client = RedisClient.builder().hostAndPort(aclEndpoint.getHostAndPort())
        .clientConfig(aclEndpoint.getClientConfigBuilder().sslOptions(sslOptions).build())
        .build()) {
      assertEquals("PONG", client.ping());
    }
  }

  @Test
  public void connectWithSslOptionsDefaults() {
    SslOptions sslOptions = SslOptions.defaults();

    DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .sslOptions(sslOptions).password(endpoint.getPassword()).build();

    try (RedisClient client = RedisClient.builder()
        .hostAndPort(endpoint.getHost(), endpoint.getPort()).clientConfig(clientConfig).build()) {
      assertEquals("PONG", client.ping());
    }
  }

  @Test
  public void connectWithSslOptionsDefaultsWrongHost() {
    SslOptions sslOptions = SslOptions.defaults();

    DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .sslOptions(sslOptions).password(wrongHostEndpoint.getPassword()).build();

    try (RedisClient client = RedisClient.builder()
        .hostAndPort(wrongHostEndpoint.getHost(), wrongHostEndpoint.getPort())
        .clientConfig(clientConfig).build()) {
      assertThrows(JedisConnectionException.class, client::ping);
    }
  }

  @Test
  public void connectWithSslOptionsCustomTrustStore() {
    SslOptions sslOptions = SslOptions.builder().truststore(trustStorePath.toFile())
        .trustStoreType("jceks").build();

    DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .sslOptions(sslOptions).password(endpoint.getPassword()).build();

    try (RedisClient client = RedisClient.builder()
        .hostAndPort(endpoint.getHost(), endpoint.getPort()).clientConfig(clientConfig).build()) {
      assertEquals("PONG", client.ping());
    }
  }

}
