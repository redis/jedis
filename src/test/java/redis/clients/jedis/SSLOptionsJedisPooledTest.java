package redis.clients.jedis;

import static org.junit.Assert.assertEquals;
import static redis.clients.jedis.util.TlsUtil.envTruststore;

import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.util.TlsUtil;

public class SSLOptionsJedisPooledTest {

  protected static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0-tls");

  protected static final EndpointConfig aclEndpoint = HostAndPorts.getRedisEndpoint("standalone0-acl-tls");

  @BeforeClass
  public static void prepare() {
    TlsUtil.createAndSaveEnvTruststore("redis1-2-5-8-sentinel", "changeit");
  }

  @Test
  public void connectWithClientConfig() {
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder()
            .sslOptions(SslOptions.builder()
                .truststore(envTruststore("redis1-2-5-8-sentinel").toFile())
                .trustStoreType("jceks")
                .build()).build())) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithSslInsecure() {
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder()
            .sslOptions(SslOptions.builder()
                .sslVerifyMode(SslVerifyMode.INSECURE)
                .build()).build())) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithSslContextProtocol() {
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder()
            .sslOptions(SslOptions.builder()
                .sslProtocol("SSL")
                .truststore(envTruststore("redis1-2-5-8-sentinel").toFile())
                .trustStoreType("jceks")
                .build()).build())) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithAcl() {
    try (JedisPooled jedis = new JedisPooled(aclEndpoint.getHostAndPort(),
        aclEndpoint.getClientConfigBuilder()
            .sslOptions(SslOptions.builder()
                .truststore(envTruststore("redis1-2-5-8-sentinel").toFile())
                .trustStoreType("jceks")
                .build()).build())) {
      assertEquals("PONG", jedis.ping());
    }
  }
}
