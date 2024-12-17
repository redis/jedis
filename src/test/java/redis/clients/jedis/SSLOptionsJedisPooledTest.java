package redis.clients.jedis;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class SSLOptionsJedisPooledTest {

  protected static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0-tls");

  protected static final EndpointConfig aclEndpoint = HostAndPorts.getRedisEndpoint("standalone0-acl-tls");

  @Test
  public void connectWithClientConfig() {
    try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder()
            .sslOptions(SslOptions.builder()
                .truststore(new File("src/test/resources/truststore.jceks"))
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
                .truststore(new File("src/test/resources/truststore.jceks"))
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
                .truststore(new File("src/test/resources/truststore.jceks"))
                .trustStoreType("jceks")
                .build()).build())) {
      assertEquals("PONG", jedis.ping());
    }
  }
}
