package redis.clients.jedis.tls;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.provider.Arguments;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPortMapper;
import redis.clients.jedis.SslOptions;
import redis.clients.jedis.SslVerifyMode;
import redis.clients.jedis.util.TlsUtil;

/**
 * Abstract base class for Redis Sentinel TLS integration tests.
 */
public abstract class RedisSentinelTlsTestBase {

  protected static final String MASTER_NAME = "aclmaster";
  private static final String TRUSTSTORE_PASSWORD = "changeit";
  private static final String TRUSTSTORE_TYPE = "jceks";

  protected static EndpointConfig sentinel;
  protected static Set<HostAndPort> sentinels = new HashSet<>();
  protected static Path trustStorePath;
  protected static SslOptions sslOptions;

  protected static final HostAndPortMapper SENTINEL_SSL_PORT_MAPPER = (
      HostAndPort hap) -> new HostAndPort(hap.getHost(), hap.getPort() + 10000);

  protected static final HostAndPortMapper PRIMARY_SSL_PORT_MAPPER = (
      HostAndPort hap) -> new HostAndPort(hap.getHost(), hap.getPort() + 11);

  @BeforeAll
  public static void setupSentinelTls() {
    sentinel = Endpoints.getRedisEndpoint("sentinel-standalone0");
    sentinels.add(sentinel.getHostAndPort());

    List<Path> trustedCertLocation = Collections
        .singletonList(Paths.get("redis1-2-5-8-sentinel/work/tls"));
    trustStorePath = TlsUtil.createAndSaveTestTruststore(
      RedisSentinelTlsTestBase.class.getSimpleName(), trustedCertLocation, TRUSTSTORE_PASSWORD);
    sslOptions = createTruststoreSslOptions();

    TlsUtil.setCustomTrustStore(trustStorePath, TRUSTSTORE_PASSWORD);
  }

  @AfterAll
  public static void teardownTrustStore() {
    TlsUtil.restoreOriginalTrustStore();
  }

  protected static SslOptions createTruststoreSslOptions() {
    return SslOptions.builder().truststore(trustStorePath.toFile()).trustStoreType(TRUSTSTORE_TYPE)
        .sslVerifyMode(SslVerifyMode.CA).build();
  }

  protected static DefaultJedisClientConfig createSentinelConfigWithSsl(SslOptions ssl) {
    return Endpoints.getRedisEndpoint("sentinel-standalone0-tls").getClientConfigBuilder()
        .clientName("sentinel-client").sslOptions(ssl).hostAndPortMapper(SENTINEL_SSL_PORT_MAPPER)
        .build();
  }

  protected static DefaultJedisClientConfig createSentinelConfigWithoutSsl() {
    return sentinel.getClientConfigBuilder().clientName("sentinel-client").build();
  }

  protected static Stream<Arguments> sslOptionsProvider() {
    return Stream.of(Arguments.of("truststore", createTruststoreSslOptions()),
      Arguments.of("insecure", SslOptions.builder().sslVerifyMode(SslVerifyMode.INSECURE).build()),
      Arguments.of("ssl-protocol",
        SslOptions.builder().sslProtocol("SSL").truststore(trustStorePath.toFile())
            .trustStoreType(TRUSTSTORE_TYPE).sslVerifyMode(SslVerifyMode.CA).build()));
  }
}
