package redis.clients.jedis.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.*;
import redis.clients.jedis.util.TlsUtil;

/**
 * SSL/TLS tests for RedisSentinelClient using SslOptions builder pattern.
 * <p>
 * Tests various SSL/TLS connection configurations including:
 * <ul>
 * <li>Basic SSL connection with truststore</li>
 * <li>Insecure SSL mode (no certificate verification)</li>
 * <li>Custom SSL protocol</li>
 * <li>ACL authentication over SSL</li>
 * </ul>
 */
public class SSLOptionsRedisSentinelClientIT {

  private static final String MASTER_NAME = "aclmaster";

  private static EndpointConfig sentinel;
  private static EndpointConfig aclEndpoint;

  private static Set<HostAndPort> sentinels = new HashSet<>();

  private static final HostAndPortMapper SSL_PORT_MAPPER = (HostAndPort hap) -> new HostAndPort(
      hap.getHost(), hap.getPort() + 10000);

  private static final HostAndPortMapper SSL_PORT_MAPPER_PRIMARY = (
      HostAndPort hap) -> new HostAndPort(hap.getHost(), hap.getPort() + 11);

  private static final String trustStoreName = SSLOptionsRedisSentinelClientIT.class
      .getSimpleName();
  private static Path trustStorePath;
  private static SslOptions sslOptions;

  @BeforeAll
  public static void prepare() {
    sentinel = Endpoints.getRedisEndpoint("sentinel-standalone0");
    aclEndpoint = Endpoints.getRedisEndpoint("standalone0-acl-tls");
    List<Path> trustedCertLocation = Collections
        .singletonList(Paths.get("redis1-2-5-8-sentinel/work/tls"));
    trustStorePath = TlsUtil.createAndSaveTestTruststore(trustStoreName, trustedCertLocation,
      "changeit");
    sslOptions = SslOptions.builder().truststore(trustStorePath.toFile()).trustStoreType("jceks")
        .sslVerifyMode(SslVerifyMode.CA).build();

    sentinels.add(sentinel.getHostAndPort());
  }

  @Test
  public void connectWithClientConfig() {
    DefaultJedisClientConfig masterConfig = aclEndpoint.getClientConfigBuilder()
        .clientName("master-client").sslOptions(sslOptions)
        .hostAndPortMapper(SSL_PORT_MAPPER_PRIMARY).build();

    DefaultJedisClientConfig sentinelConfig = Endpoints.getRedisEndpoint("sentinel-standalone0-tls")
        .getClientConfigBuilder().clientName("sentinel-client").sslOptions(sslOptions)
        .hostAndPortMapper(SSL_PORT_MAPPER).build();

    try (RedisSentinelClient client = RedisSentinelClient.builder().masterName(MASTER_NAME)
        .sentinels(sentinels).clientConfig(masterConfig).sentinelClientConfig(sentinelConfig)
        .build()) {
      assertEquals("PONG", client.ping());
    }
  }

  @Test
  public void connectWithSslInsecure() {
    SslOptions insecureSslOptions = SslOptions.builder().sslVerifyMode(SslVerifyMode.INSECURE)
        .build();

    DefaultJedisClientConfig masterConfig = aclEndpoint.getClientConfigBuilder()
        .clientName("master-client").sslOptions(insecureSslOptions)
        .hostAndPortMapper(SSL_PORT_MAPPER_PRIMARY).build();

    DefaultJedisClientConfig sentinelConfig = Endpoints.getRedisEndpoint("sentinel-standalone0-tls")
        .getClientConfigBuilder().clientName("sentinel-client").sslOptions(insecureSslOptions)
        .hostAndPortMapper(SSL_PORT_MAPPER).build();

    try (RedisSentinelClient client = RedisSentinelClient.builder().masterName(MASTER_NAME)
        .sentinels(sentinels).clientConfig(masterConfig).sentinelClientConfig(sentinelConfig)
        .build()) {
      assertEquals("PONG", client.ping());
    }
  }

  @Test
  public void connectWithSslContextProtocol() {
    SslOptions protocolSslOptions = SslOptions.builder().sslProtocol("SSL")
        .truststore(trustStorePath.toFile()).trustStoreType("jceks").sslVerifyMode(SslVerifyMode.CA)
        .build();

    DefaultJedisClientConfig masterConfig = aclEndpoint.getClientConfigBuilder()
        .clientName("master-client").sslOptions(protocolSslOptions)
        .hostAndPortMapper(SSL_PORT_MAPPER_PRIMARY).build();

    DefaultJedisClientConfig sentinelConfig = Endpoints.getRedisEndpoint("sentinel-standalone0-tls")
        .getClientConfigBuilder().clientName("sentinel-client").sslOptions(protocolSslOptions)
        .hostAndPortMapper(SSL_PORT_MAPPER).build();

    try (RedisSentinelClient client = RedisSentinelClient.builder().masterName(MASTER_NAME)
        .sentinels(sentinels).clientConfig(masterConfig).sentinelClientConfig(sentinelConfig)
        .build()) {
      assertEquals("PONG", client.ping());
    }
  }

  @Test
  public void connectWithAcl() {
    DefaultJedisClientConfig masterConfig = aclEndpoint.getClientConfigBuilder()
        .clientName("master-client").sslOptions(sslOptions)
        .hostAndPortMapper(SSL_PORT_MAPPER_PRIMARY).build();

    DefaultJedisClientConfig sentinelConfig = Endpoints.getRedisEndpoint("sentinel-standalone0-tls")
        .getClientConfigBuilder().clientName("sentinel-client").sslOptions(sslOptions)
        .hostAndPortMapper(SSL_PORT_MAPPER).build();

    try (RedisSentinelClient client = RedisSentinelClient.builder().masterName(MASTER_NAME)
        .sentinels(sentinels).clientConfig(masterConfig).sentinelClientConfig(sentinelConfig)
        .build()) {
      assertEquals("PONG", client.ping());
    }
  }

  @Test
  public void sentinelWithoutSslConnectsToRedisWithSsl() {
    DefaultJedisClientConfig masterConfig = aclEndpoint.getClientConfigBuilder()
        .clientName("master-client").sslOptions(sslOptions)
        .hostAndPortMapper(SSL_PORT_MAPPER_PRIMARY).build();

    DefaultJedisClientConfig sentinelConfig = sentinel.getClientConfigBuilder()
        .clientName("sentinel-client").build();

    try (RedisSentinelClient client = RedisSentinelClient.builder().masterName(MASTER_NAME)
        .sentinels(sentinels).clientConfig(masterConfig).sentinelClientConfig(sentinelConfig)
        .build()) {
      assertEquals("PONG", client.ping());
    }
  }
}
