package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

  private static final byte[] AUTH_OK_REPLY = OK_REPLY;

  private static final byte[] NOAUTH_ERR = "-NOAUTH Authentication required.\r\n".getBytes();

  private static final byte[] UNKNOWN_CMD_ERR = "-ERR unknown command 'HELLO'\r\n".getBytes();

  /** RESP error for NOPROTO (Redis Enterprise / Cloud when protocol is disabled). */
  private static final byte[] NOPROTO_ERR = "-NOPROTO unsupported protocol version\r\n".getBytes();

  /** RESP error for NOPERM (ACL: user not allowed to run the command). */
  private static final byte[] NOPERM_ERR = "-NOPERM this user has no permissions to run the 'hello' command or its subcommand\r\n"
      .getBytes();

  /** Minimal RESP3 map without proto field: %2\r\n+server\r\n+redis\r\n+version\r\n+7.0.0\r\n */
  private static final byte[] HELLO_OK_MAP = "%2\r\n+server\r\n+redis\r\n+version\r\n+7.0.0\r\n"
      .getBytes();

  /** RESP3 map with proto=3: %3\r\n+server\r\n+redis\r\n+version\r\n+7.0.0\r\n+proto\r\n:3\r\n */
  private static final byte[] HELLO_OK_MAP_PROTO3 = "%3\r\n+server\r\n+redis\r\n+version\r\n+7.0.0\r\n+proto\r\n:3\r\n"
      .getBytes();

  /** RESP3 map with proto=2: %3\r\n+server\r\n+redis\r\n+version\r\n+7.0.0\r\n+proto\r\n:2\r\n */
  private static final byte[] HELLO_OK_MAP_PROTO2 = "%3\r\n+server\r\n+redis\r\n+version\r\n+7.0.0\r\n+proto\r\n:2\r\n"
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
      // (HELLO -> NOAUTH) -> fallback to (authenticate, hello -> NOAUTH) -> propagate error
      assertThrows(JedisAccessControlException.class,
        () -> new Connection(fakeSocketFactory(concat(NOAUTH_ERR, NOAUTH_ERR)),
            noAuthConfig(RedisProtocol.RESP3)),
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
    @DisplayName("No HELLO sent when protocol is null — connection defaults to RESP2")
    void noHelloWhenProtocolNull() {
      Connection conn = new Connection(fakeSocketFactory(new byte[0]), noAuthConfig(null));
      // When protocol is null, no HELLO is sent but the connection resolves to RESP2.
      assertNull(conn.getRedisProtocol());
      assertEquals(RespProtocol.RESP2, conn.getEstablishedProtocol());
      assertFalse(conn.isBroken());
      conn.close();
    }

    @Test
    @DisplayName("HELLO with proto=3 in response — RESP3 confirmed via protocol negotiation")
    void helloWithProto3InResponse() {
      Connection conn = new Connection(fakeSocketFactory(HELLO_OK_MAP_PROTO3),
          noAuthConfig(RedisProtocol.RESP3));
      assertEquals(RedisProtocol.RESP3, conn.getRedisProtocol());
      assertEquals(RespProtocol.RESP3, conn.getEstablishedProtocol());
      assertFalse(conn.isBroken());
      conn.close();
    }

    @Test
    @DisplayName("HELLO with proto=2 in response — RESP2 confirmed via protocol negotiation")
    void helloWithProto2InResponse() {
      Connection conn = new Connection(fakeSocketFactory(HELLO_OK_MAP_PROTO2),
          noAuthConfig(RedisProtocol.RESP2));
      assertEquals(RedisProtocol.RESP2, conn.getRedisProtocol());
      assertEquals(RespProtocol.RESP2, conn.getEstablishedProtocol());
      assertFalse(conn.isBroken());
      conn.close();
    }

    @Test
    @DisplayName("RESP3_PREFERRED with proto=3 in response — returns RESP3 from proto field")
    void resp3PreferredWithProto3InResponse() {
      Connection conn = new Connection(fakeSocketFactory(HELLO_OK_MAP_PROTO3),
          noAuthConfig(RedisProtocol.RESP3_PREFERRED));
      assertEquals(RedisProtocol.RESP3_PREFERRED, conn.getRedisProtocol());
      assertEquals(RespProtocol.RESP3, conn.getEstablishedProtocol());
      assertFalse(conn.isBroken());
      conn.close();
    }

    @Test
    @DisplayName("RESP3_PREFERRED with proto=2 in response — returns RESP2 from proto field")
    void resp3PreferredWithProto2InResponse() {
      Connection conn = new Connection(fakeSocketFactory(HELLO_OK_MAP_PROTO2),
          noAuthConfig(RedisProtocol.RESP3_PREFERRED));
      assertEquals(RedisProtocol.RESP3_PREFERRED, conn.getRedisProtocol());
      assertEquals(RespProtocol.RESP2, conn.getEstablishedProtocol());
      assertFalse(conn.isBroken());
      conn.close();
    }

    @Test
    @DisplayName("HELLO without proto field — explicit RESP3 still resolves normally")
    void helloWithoutProtoFieldExplicitResp3() {
      // When proto field is absent, no mismatch check is done — protocol resolves as requested.
      Connection conn = new Connection(fakeSocketFactory(HELLO_OK_MAP),
          noAuthConfig(RedisProtocol.RESP3));
      assertEquals(RedisProtocol.RESP3, conn.getRedisProtocol());
      assertFalse(conn.isBroken());
      conn.close();
    }

    @Test
    @DisplayName("RESP3_PREFERRED with HELLO unknown command and no creds — falls back to RESP2")
    void resp3PreferredUnknownCommandResolvesToResp2() {
      // Pre-6.0 server: HELLO 3 -> unknown -> establishLegacyResp2 -> authenticate(null) (no-op)
      // -> HELLO 2 -> unknown -> infer RESP2.
      Connection conn = new Connection(fakeSocketFactory(concat(UNKNOWN_CMD_ERR, UNKNOWN_CMD_ERR)),
          noAuthConfig(RedisProtocol.RESP3_PREFERRED));
      assertEquals(RedisProtocol.RESP3_PREFERRED, conn.getRedisProtocol());
      assertEquals(RespProtocol.RESP2, conn.getEstablishedProtocol());
      assertFalse(conn.isBroken());
      conn.close();
    }
  }

  // ---- Tests: HELLO with AUTH (credentials with user) ----

  @Nested
  @DisplayName("HELLO with AUTH (credentials with user)")
  class HelloWithAuthTests {

    @Test
    @DisplayName("HELLO with AUTH succeeds with RESP3 — protocol is negotiated normally")
    void helloWithAuthResp3Succeeds() {
      Connection conn = new Connection(fakeSocketFactory(HELLO_OK_MAP_PROTO3),
          authConfig(RedisProtocol.RESP3));
      assertEquals(RedisProtocol.RESP3, conn.getRedisProtocol());
      assertEquals(RespProtocol.RESP3, conn.getEstablishedProtocol());
      assertFalse(conn.isBroken());
      conn.close();
    }

    @Test
    @DisplayName("HELLO AUTH succeeds with RESP2 — protocol is negotiated normally")
    void helloWithAuthResp2Succeeds() {
      Connection conn = new Connection(fakeSocketFactory(concat(HELLO_OK_MAP_PROTO2)),
          authConfig(RedisProtocol.RESP2));
      assertEquals(RedisProtocol.RESP2, conn.getRedisProtocol());
      assertFalse(conn.isBroken());
      conn.close();
    }

    @Test
    @DisplayName("HELLO not supported with RESP2 — propagates error")
    void helloNotSupportedResp2PropagatesError() {
      assertThrows(JedisProtocolNotSupportedException.class, () -> {
        Connection conn = new Connection(fakeSocketFactory(concat(UNKNOWN_CMD_ERR)),
            authConfig(RedisProtocol.RESP2));
      });
    }

    @Test
    @DisplayName("HELLO AUTH with proto=3 in response — RESP3 confirmed")
    void helloAuthWithProto3InResponse() {
      Connection conn = new Connection(fakeSocketFactory(HELLO_OK_MAP_PROTO3),
          authConfig(RedisProtocol.RESP3));
      assertEquals(RedisProtocol.RESP3, conn.getRedisProtocol());
      assertFalse(conn.isBroken());
      conn.close();
    }

    @Test
    @DisplayName("RESP3_PREFERRED with AUTH and proto=3 in response — returns RESP3")
    void resp3PreferredAuthWithProto3() {
      Connection conn = new Connection(fakeSocketFactory(HELLO_OK_MAP_PROTO3),
          authConfig(RedisProtocol.RESP3_PREFERRED));
      assertEquals(RedisProtocol.RESP3_PREFERRED, conn.getRedisProtocol());
      assertEquals(RespProtocol.RESP3, conn.getEstablishedProtocol());
      assertFalse(conn.isBroken());
      conn.close();
    }

    @Test
    @DisplayName("RESP3_PREFERRED with AUTH HELLO not supported — resolves to RESP2")
    void resp3PreferredWithAuthHelloNotSupportedResolvesToProto2() {
      // -> hello(3,user,pass) -> unknown command
      // -> (fallback to establishLegacyResp2)
      // -> auth -> ok ->
      // -> not require (hello(2)) -> unknown command
      Connection conn = new Connection(
          fakeSocketFactory(concat(UNKNOWN_CMD_ERR, AUTH_OK_REPLY, UNKNOWN_CMD_ERR)),
          authConfig(RedisProtocol.RESP3_PREFERRED));
      assertEquals(RedisProtocol.RESP3_PREFERRED, conn.getRedisProtocol());
      assertEquals(RespProtocol.RESP2, conn.getEstablishedProtocol());
      assertFalse(conn.isBroken());
      conn.close();
    }

    @Test
    @DisplayName("RESP3_PREFERRED with AUTH and NOPROTO on HELLO 3 — falls back to RESP2 via HELLO 2")
    void resp3PreferredAuthHelloNoProtoFallsBackToResp2() {
      // -> hello(3,user,pass) -> NOPROTO
      // -> (fallback to establishLegacyResp2)
      // -> auth -> ok
      // -> hello(2) -> ok with proto=2
      Connection conn = new Connection(
          fakeSocketFactory(concat(NOPROTO_ERR, AUTH_OK_REPLY, HELLO_OK_MAP_PROTO2)),
          authConfig(RedisProtocol.RESP3_PREFERRED));
      assertEquals(RedisProtocol.RESP3_PREFERRED, conn.getRedisProtocol());
      assertEquals(RespProtocol.RESP2, conn.getEstablishedProtocol());
      assertFalse(conn.isBroken());
      conn.close();
    }

    @Test
    @DisplayName("Explicit RESP3 with AUTH and NOPERM on HELLO — propagates JedisAccessControlException")
    void explicitResp3HelloNoPermPropagates() {
      // No fallback for explicit RESP3 — NOPERM (not NOAUTH) must propagate.
      assertThrows(JedisAccessControlException.class,
        () -> new Connection(fakeSocketFactory(NOPERM_ERR), authConfig(RedisProtocol.RESP3)),
        "NOPERM from HELLO with explicit RESP3 should propagate");
    }
  }
}
