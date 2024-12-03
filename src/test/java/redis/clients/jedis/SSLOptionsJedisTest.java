package redis.clients.jedis;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class SSLOptionsJedisTest {

  protected static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0-tls");

  protected static final EndpointConfig aclEndpoint = HostAndPorts.getRedisEndpoint("standalone0-acl-tls");

  @Test
  public void connectWithSsl() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
        DefaultJedisClientConfig.builder()
            .sslOptions(SslOptions.builder()
                .truststore(new File("src/test/resources/truststore.jceks"))
                .trustStoreType("jceks")
                .build()).build())) {
      jedis.auth(endpoint.getPassword());
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithConfig() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
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
                .truststore(new File("src/test/resources/truststore.jceks"))
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
                .truststore(new File("src/test/resources/truststore.jceks"))
                .trustStoreType("jceks")
                .build()).build())) {
      assertEquals("PONG", jedis.ping());
    }
  }
}
