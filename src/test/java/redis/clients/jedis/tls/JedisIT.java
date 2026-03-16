package redis.clients.jedis.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;

/**
 * SSL/TLS tests for {@link Jedis} with basic authentication (password-only, no ACL).
 * <p>
 * Uses the system truststore (ssl=true flag) for SSL connections.
 */
public class JedisIT extends JedisTlsTestBase {

  @Test
  public void connectWithSsl() {
    try (Jedis jedis = new Jedis(endpoint.getHost(), endpoint.getPort(), true)) {
      jedis.auth(endpoint.getPassword());
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithConfig() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
        DefaultJedisClientConfig.builder().ssl(true).build())) {
      jedis.auth(endpoint.getPassword());
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithConfigInterface() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(), new JedisClientConfig() {
      @Override
      public boolean isSsl() {
        return true;
      }
    })) {
      jedis.auth(endpoint.getPassword());
      assertEquals("PONG", jedis.ping());
    }
  }

  /**
   * Tests opening a default SSL/TLS connection to redis using "rediss://" scheme url.
   */
  @Test
  public void connectWithUrl() {
    // The "rediss" scheme instructs jedis to open a SSL/TLS connection.
    try (Jedis jedis = new Jedis(endpoint.getURI().toString())) {
      jedis.auth(endpoint.getPassword());
      assertEquals("PONG", jedis.ping());
    }
  }

  /**
   * Tests opening a default SSL/TLS connection to redis.
   */
  @Test
  public void connectWithUri() {
    // The "rediss" scheme instructs jedis to open a SSL/TLS connection.
    try (Jedis jedis = new Jedis(endpoint.getURI())) {
      jedis.auth(endpoint.getPassword());
      assertEquals("PONG", jedis.ping());
    }
  }
}
