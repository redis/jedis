package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * Verifies that the URI-based {@link Jedis} constructor carries {@link SslOptions} from the
 * supplied config, matching the {@link HostAndPort}-based constructor. The mock server speaks
 * plaintext RESP, so a client that honours {@code SslOptions} attempts a TLS handshake (which fails
 * here), while one that drops it connects in the clear.
 */
public class JedisUriSslOptionsTest {

  private TcpMockServer mockServer;

  @BeforeEach
  public void setUp() throws IOException {
    mockServer = new TcpMockServer();
    mockServer.start();
  }

  @AfterEach
  public void tearDown() throws IOException {
    if (mockServer != null) {
      mockServer.stop();
    }
  }

  @Test
  public void uriConstructorHonoursSslOptions() {
    SslOptions sslOptions = SslOptions.builder().sslVerifyMode(SslVerifyMode.INSECURE).build();
    JedisClientConfig config = DefaultJedisClientConfig.builder().sslOptions(sslOptions)
        .connectionTimeoutMillis(1000).socketTimeoutMillis(1000).build();

    HostAndPort hostPort = new HostAndPort("localhost", mockServer.getPort());

    // The HostAndPort constructor has always carried SslOptions: TLS is attempted against the
    // plaintext mock and the handshake fails.
    assertThrows(JedisException.class, () -> new Jedis(hostPort, config).close());

    // The URI constructor must behave the same. Before the fix it silently dropped SslOptions and
    // connected in plaintext, so nothing was thrown.
    URI uri = URI.create("redis://localhost:" + mockServer.getPort());
    assertThrows(JedisException.class, () -> new Jedis(uri, config).close());
  }
}
