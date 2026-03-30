package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.exceptions.JedisAccessControlException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisProtocolNotSupportedException;

/**
 * Unit tests for the {@code helloAndAuth} behaviour inside {@link Connection}.
 * <p>
 * These tests simulate different Redis server responses to the HELLO command by providing a fake
 * socket that returns pre-built RESP protocol bytes.
 * </p>
 */
public class ConnectionHelloAuthTest {

  // ---- RESP response constants ----

  private static final byte[] OK_REPLY = "+OK\r\n".getBytes();

  private static final byte[] NOAUTH_ERR = "-NOAUTH Authentication required.\r\n".getBytes();

  private static final byte[] UNKNOWN_CMD_ERR = "-ERR unknown command 'HELLO'\r\n".getBytes();

  /** RESP error for NOPROTO (Redis Enterprise / Cloud when protocol is disabled). */
  private static final byte[] NOPROTO_ERR = "-NOPROTO unsupported protocol version\r\n".getBytes();

  /** Minimal RESP3 map: %2\r\n+server\r\n+redis\r\n+version\r\n+7.0.0\r\n */
  private static final byte[] HELLO_OK_MAP = "%2\r\n+server\r\n+redis\r\n+version\r\n+7.0.0\r\n"
      .getBytes();

  // ---- helpers ----

  private static byte[] concat(byte[]... arrays) {
    int len = 0;
    for (byte[] a : arrays)
      len += a.length;
    byte[] result = new byte[len];
    int pos = 0;
    for (byte[] a : arrays) {
      System.arraycopy(a, 0, result, pos, a.length);
      pos += a.length;
    }
    return result;
  }

  private static JedisSocketFactory fakeSocketFactory(byte[] respBytes) {
    return () -> new FakeSocket(respBytes);
  }

  private static class FakeSocket extends Socket {
    private final InputStream in;
    private final OutputStream out = new ByteArrayOutputStream();

    FakeSocket(byte[] input) {
      this.in = new ByteArrayInputStream(input);
    }

    @Override
    public InputStream getInputStream() {
      return in;
    }

    @Override
    public OutputStream getOutputStream() {
      return out;
    }

    @Override
    public boolean isConnected() {
      return true;
    }

    @Override
    public boolean isClosed() {
      return false;
    }

    @Override
    public boolean isBound() {
      return true;
    }

    @Override
    public boolean isInputShutdown() {
      return false;
    }

    @Override
    public boolean isOutputShutdown() {
      return false;
    }

    @Override
    public int getSoTimeout() {
      return 0;
    }

    @Override
    public void setSoTimeout(int t) {
    }

    @Override
    public void close() {
    }
  }

  private static JedisClientConfig noAuthConfig(RedisProtocol proto) {
    return DefaultJedisClientConfig.builder().protocol(proto)
        .clientSetInfoConfig(ClientSetInfoConfig.DISABLED).build();
  }

  private static JedisClientConfig authConfig(RedisProtocol proto) {
    return DefaultJedisClientConfig.builder().protocol(proto).user("default").password("secret")
        .clientSetInfoConfig(ClientSetInfoConfig.DISABLED).build();
  }

  // ---- Tests: bare HELLO (no credentials) ----

  @Nested
  @DisplayName("Bare HELLO (no credentials)")
  class BareHelloTests {

    @Test
    @DisplayName("HELLO succeeds — protocol is negotiated normally")
    void helloSucceeds() {
      Connection conn = new Connection(fakeSocketFactory(HELLO_OK_MAP),
          noAuthConfig(RedisProtocol.RESP3));
      assertEquals(RedisProtocol.RESP3, conn.getRedisProtocol());
      assertFalse(conn.isBroken());
      conn.close();
    }

    @Test
    @DisplayName("HELLO rejected with NOAUTH — propagates auth error instead of silently downgrading")
    void helloRejectedNoAuthThrows() {
      // If the user asked for a specific protocol but didn't provide credentials,
      // the NOAUTH error must propagate so the caller knows auth is missing.
      assertThrows(JedisAccessControlException.class,
        () -> new Connection(fakeSocketFactory(NOAUTH_ERR), noAuthConfig(RedisProtocol.RESP3)),
        "NOAUTH from HELLO should propagate as JedisAccessControlException");
    }

    @Test
    @DisplayName("HELLO rejected with NOPROTO — propagates error because protocol is explicitly disabled")
    void helloRejectedNoProtoThrows() {
      // Redis Enterprise / Cloud: the requested protocol version has been
      // explicitly disabled. Must propagate so the caller knows the
      // requested protocol is not available on this server.
      assertThrows(JedisProtocolNotSupportedException.class,
        () -> new Connection(fakeSocketFactory(NOPROTO_ERR), noAuthConfig(RedisProtocol.RESP3)),
        "NOPROTO from HELLO should propagate as JedisProtocolNotSupportedException");
    }

    @Test
    @DisplayName("HELLO rejected with unknown command (pre-6.0) — propagates as JedisProtocolNotSupportedException")
    void helloRejectedUnknownCommandThrows() {
      // Redis < 6.0 does not know the HELLO command. When the user explicitly
      // requested a protocol version, this must be reported as an error rather
      // than silently falling back.
      assertThrows(JedisProtocolNotSupportedException.class,
        () -> new Connection(fakeSocketFactory(UNKNOWN_CMD_ERR), noAuthConfig(RedisProtocol.RESP3)),
        "Unknown command HELLO on pre-6.0 should propagate as JedisProtocolNotSupportedException");
    }

    @Test
    @DisplayName("HELLO rejected with unexpected error — propagates as JedisDataException")
    void helloRejectedUnexpectedErrorThrows() {
      // An error that is neither unknown-command, NOPROTO, nor NOAUTH should
      // not be silently swallowed — it must propagate.
      byte[] unexpectedErr = "-ERR some unexpected server error\r\n".getBytes();
      assertThrows(JedisDataException.class,
        () -> new Connection(fakeSocketFactory(unexpectedErr), noAuthConfig(RedisProtocol.RESP3)),
        "Unexpected errors from HELLO should propagate as JedisDataException");
    }

    @Test
    @DisplayName("No HELLO sent when protocol is null")
    void noHelloWhenProtocolNull() {
      Connection conn = new Connection(fakeSocketFactory(new byte[0]), noAuthConfig(null));
      assertNull(conn.getRedisProtocol());
      assertFalse(conn.isBroken());
      conn.close();
    }
  }

  // ---- Tests: HELLO with AUTH (credentials with user) ----

  @Nested
  @DisplayName("HELLO with AUTH (credentials with user)")
  class HelloWithAuthTests {

    @Test
    @DisplayName("HELLO AUTH succeeds — protocol is negotiated normally")
    void helloAuthSucceeds() {
      Connection conn = new Connection(fakeSocketFactory(HELLO_OK_MAP),
          authConfig(RedisProtocol.RESP3));
      assertEquals(RedisProtocol.RESP3, conn.getRedisProtocol());
      assertFalse(conn.isBroken());
      conn.close();
    }

    @Test
    @DisplayName("HELLO AUTH succeeds with RESP2 — protocol is negotiated normally")
    void helloAuthResp2Succeeds() {
      // RESP2 HELLO returns a flat array, but for simplicity we use the map reply
      // since the builder handles both; what matters is no exception.
      Connection conn = new Connection(fakeSocketFactory(HELLO_OK_MAP),
          authConfig(RedisProtocol.RESP2));
      assertEquals(RedisProtocol.RESP2, conn.getRedisProtocol());
      assertFalse(conn.isBroken());
      conn.close();
    }
  }
}
