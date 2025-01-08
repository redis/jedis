package redis.clients.jedis;

import static org.junit.Assert.assertEquals;
import static redis.clients.jedis.util.TlsUtil.envTruststore;

import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.util.TlsUtil;

public class SSLOptionsJedisTest {

  protected static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0-tls");

  protected static final EndpointConfig aclEndpoint = HostAndPorts.getRedisEndpoint("standalone0-acl-tls");

  @BeforeClass
  public static void prepare() {
    TlsUtil.createAndSaveEnvTruststore("redis1-2-5-8-sentinel", "changeit");
  }

  @Test
  public void connectWithSsl() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
        DefaultJedisClientConfig.builder()
            .sslOptions(SslOptions.builder()
                .truststore(envTruststore("redis1-2-5-8-sentinel").toFile())
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
                .truststore(envTruststore("redis1-2-5-8-sentinel").toFile())
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
                .truststore(envTruststore("redis1-2-5-8-sentinel").toFile())
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
                .truststore(envTruststore("redis1-2-5-8-sentinel").toFile())
                .trustStoreType("jceks")
                .build()).build())) {
      assertEquals("PONG", jedis.ping());
    }
  }
}
