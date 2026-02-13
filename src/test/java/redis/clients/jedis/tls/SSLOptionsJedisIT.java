package redis.clients.jedis.tls;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.*;
import redis.clients.jedis.util.TlsUtil;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SSLOptionsJedisIT {

  protected static EndpointConfig endpoint;

  protected static EndpointConfig aclEndpoint;

  private static final String trustStoreName = SSLOptionsJedisIT.class.getSimpleName();
  private static Path trustStorePath;
  @BeforeAll
  public static void prepare() {
    endpoint = Endpoints.getRedisEndpoint("standalone0-tls");
    aclEndpoint = Endpoints.getRedisEndpoint("standalone0-acl-tls");
    List<Path> trustedCertLocation = Arrays.asList(endpoint.getCertificatesLocation(),aclEndpoint.getCertificatesLocation());
    trustStorePath = TlsUtil.createAndSaveTestTruststore(trustStoreName, trustedCertLocation,"changeit");
  }

  @Test
  public void connectWithSsl() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
        DefaultJedisClientConfig.builder()
            .sslOptions(SslOptions.builder()
                .truststore(trustStorePath.toFile())
                .trustStoreType("jceks")
                .build()).build())) {
      jedis.auth(endpoint.getPassword());
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithClientConfig() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder()
            .sslOptions(SslOptions.builder()
                .truststore(trustStorePath.toFile())
                .trustStoreType("jceks")
                .build()).build())) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithSslInsecure() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder()
            .sslOptions(SslOptions.builder()
                .sslVerifyMode(SslVerifyMode.INSECURE)
                .build()).build())) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithSslContextProtocol() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder()
            .sslOptions(SslOptions.builder()
                .sslProtocol("SSL")
                .truststore(trustStorePath.toFile())
                .trustStoreType("jceks")
                .build()).build())) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithAcl() {
    try (Jedis jedis = new Jedis(aclEndpoint.getHostAndPort(),
        aclEndpoint.getClientConfigBuilder()
            .sslOptions(SslOptions.builder()
                .truststore(trustStorePath.toFile())
                .trustStoreType("jceks")
                .build()).build())) {
      assertEquals("PONG", jedis.ping());
    }
  }
}
