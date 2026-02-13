package redis.clients.jedis.tls;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.*;
import redis.clients.jedis.util.TlsUtil;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SSLOptionsRedisClientIT {

  protected static EndpointConfig endpoint;

  protected static EndpointConfig aclEndpoint;

  private static final String trustStoreName = SSLACLJedisIT.class.getSimpleName();
  private static Path trustStorePath;

  @BeforeAll
  public static void prepare() {
    endpoint = Endpoints.getRedisEndpoint("standalone0-tls");
    aclEndpoint = Endpoints.getRedisEndpoint("standalone0-acl-tls");
    List<Path> trustedCertLocation = Arrays.asList(endpoint.getCertificatesLocation(),
      aclEndpoint.getCertificatesLocation());
    trustStorePath = TlsUtil.createAndSaveTestTruststore(trustStoreName, trustedCertLocation,
      "changeit");
  }

  @Test
  public void connectWithClientConfig() {
    try (
        RedisClient jedis = RedisClient.builder().hostAndPort(endpoint.getHostAndPort())
            .clientConfig(endpoint.getClientConfigBuilder().sslOptions(SslOptions.builder()
                .truststore(trustStorePath.toFile()).trustStoreType("jceks").build()).build())
            .build()) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithSslInsecure() {
    try (RedisClient jedis = RedisClient.builder().hostAndPort(endpoint.getHostAndPort())
        .clientConfig(endpoint.getClientConfigBuilder()
            .sslOptions(SslOptions.builder().sslVerifyMode(SslVerifyMode.INSECURE).build()).build())
        .build()) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithSslContextProtocol() {
    try (
        RedisClient jedis = RedisClient.builder().hostAndPort(endpoint.getHostAndPort())
            .clientConfig(endpoint.getClientConfigBuilder()
                .sslOptions(SslOptions.builder().sslProtocol("SSL")
                    .truststore(trustStorePath.toFile()).trustStoreType("jceks").build())
                .build())
            .build()) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithAcl() {
    try (
        RedisClient jedis = RedisClient.builder().hostAndPort(aclEndpoint.getHostAndPort())
            .clientConfig(aclEndpoint.getClientConfigBuilder().sslOptions(SslOptions.builder()
                .truststore(trustStorePath.toFile()).trustStoreType("jceks").build()).build())
            .build()) {
      assertEquals("PONG", jedis.ping());
    }
  }
}
