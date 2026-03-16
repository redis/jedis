package redis.clients.jedis.tls;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.provider.Arguments;

import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.SslOptions;
import redis.clients.jedis.SslVerifyMode;
import redis.clients.jedis.util.TlsUtil;

/**
 * Abstract base class for SSL/TLS tests for {@link redis.clients.jedis.Jedis}.
 */
public abstract class JedisTlsTestBase {

  private static final String TRUSTSTORE_PASSWORD = "changeit";

  protected static EndpointConfig endpoint;
  protected static EndpointConfig aclEndpoint;
  protected static Path trustStorePath;
  protected static SslOptions sslOptions;

  @BeforeAll
  public static void setUpTrustStore() {
    endpoint = Endpoints.getRedisEndpoint("standalone0-tls");
    aclEndpoint = Endpoints.getRedisEndpoint("standalone0-acl-tls");

    List<Path> trustedCertLocation = Arrays.asList(endpoint.getCertificatesLocation(),
      aclEndpoint.getCertificatesLocation());
    trustStorePath = TlsUtil.createAndSaveTestTruststore(JedisTlsTestBase.class.getSimpleName(),
      trustedCertLocation, TRUSTSTORE_PASSWORD);

    TlsUtil.setCustomTrustStore(trustStorePath, TRUSTSTORE_PASSWORD);
    sslOptions = createTruststoreSslOptions();
  }

  @AfterAll
  public static void tearDownTrustStore() {
    TlsUtil.restoreOriginalTrustStore();
  }

  protected static SslOptions createTruststoreSslOptions() {
    return SslOptions.builder().truststore(trustStorePath.toFile()).trustStoreType("jceks")
        .sslVerifyMode(SslVerifyMode.CA).build();
  }

  protected static Stream<Arguments> sslOptionsProvider() {
    return Stream.of(Arguments.of("truststore", createTruststoreSslOptions()),
      Arguments.of("insecure", SslOptions.builder().sslVerifyMode(SslVerifyMode.INSECURE).build()),
      Arguments.of("ssl-protocol",
        SslOptions.builder().sslProtocol("SSL").truststore(trustStorePath.toFile())
            .trustStoreType("jceks").sslVerifyMode(SslVerifyMode.CA).build()));
  }
}
