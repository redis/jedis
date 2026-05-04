package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import redis.clients.jedis.exceptions.JedisAccessControlException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisProtocolNotSupportedException;

@ExtendWith(MockitoExtension.class)
class ProtocolHandshakeTest {

  @Mock
  Connection connection;

  ProtocolHandshake handshake;

  @BeforeEach
  void setUp() {
    handshake = new ProtocolHandshake(connection);
  }

  // ---------------------------------------------------------------------------
  // requested = null (legacy mode: no HELLO sent, server default assumed RESP2)
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("requested = null")
  class NullProtocolRequested {

    @Test
    void authenticatesAndInfersResp2() {
      RedisCredentials creds = new DefaultRedisCredentials("user", "pwd".toCharArray());

      HelloResult result = handshake.establish(null, creds);

      assertEquals(RespProtocol.RESP2, result.getProtocol());
      verify(connection).authenticate(creds);
      verify(connection, never()).hello(any(RespProtocol.class), any());
    }

    @Test
    void nullCreds_authenticatesAndInfersResp2() {
      HelloResult result = handshake.establish(null, null);

      assertEquals(RespProtocol.RESP2, result.getProtocol());
      verify(connection).authenticate(null);
      verify(connection, never()).hello(any(RespProtocol.class), any());
    }
  }

  // ---------------------------------------------------------------------------
  // requested = RESP2 (strict)
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("requested = RESP2 (strict)")
  class Resp2StrictRequested {

    @Test
    void callsHello2() {
      when(connection.hello(RespProtocol.RESP2, null)).thenReturn(helloResult(RespProtocol.RESP2));

      HelloResult result = handshake.establish(RedisProtocol.RESP2, null);

      assertEquals(RespProtocol.RESP2, result.getProtocol());
      verify(connection).hello(RespProtocol.RESP2, null);
      verify(connection, never()).authenticate(any());
    }

    @Test
    void unknownCommand_throwsProtocolNotSupported() {
      when(connection.hello(RespProtocol.RESP2, null))
          .thenThrow(new JedisDataException("ERR unknown command 'HELLO'"));

      JedisProtocolNotSupportedException ex = assertThrows(JedisProtocolNotSupportedException.class,
        () -> handshake.establish(RedisProtocol.RESP2, null));
      assertTrue(ex.getMessage().contains("Server does not support HELLO"));
    }

    @Test
    void genericDataException_propagates() {
      when(connection.hello(RespProtocol.RESP2, null))
          .thenThrow(new JedisDataException("ERR something else"));

      JedisDataException ex = assertThrows(JedisDataException.class,
        () -> handshake.establish(RedisProtocol.RESP2, null));
      assertFalse(ex instanceof JedisProtocolNotSupportedException);
    }

    @Test
    void withCreds_succeedsInSingleHello() {
      RedisCredentials creds = new DefaultRedisCredentials("user", "pwd".toCharArray());
      when(connection.hello(RespProtocol.RESP2, creds)).thenReturn(helloResult(RespProtocol.RESP2));

      HelloResult result = handshake.establish(RedisProtocol.RESP2, creds);

      assertEquals(RespProtocol.RESP2, result.getProtocol());
      verify(connection).hello(RespProtocol.RESP2, creds);
      verify(connection, never()).authenticate(any());
    }

    @Test
    void noAuthRecovery_authThenRetryHello() {
      RedisCredentials creds = new DefaultRedisCredentials("user", "pwd".toCharArray());
      when(connection.hello(RespProtocol.RESP2, creds))
          .thenThrow(new JedisAccessControlException("NOAUTH Authentication required."))
          .thenReturn(helloResult(RespProtocol.RESP2));

      HelloResult result = handshake.establish(RedisProtocol.RESP2, creds);

      assertEquals(RespProtocol.RESP2, result.getProtocol());
      InOrder inOrder = inOrder(connection);
      inOrder.verify(connection).hello(RespProtocol.RESP2, creds);
      inOrder.verify(connection).authenticate(creds);
      inOrder.verify(connection).hello(RespProtocol.RESP2, creds);
    }

    @Test
    void nonNoAuthAccessControl_propagatesWithoutRetry() {
      RedisCredentials creds = new DefaultRedisCredentials("u", "p".toCharArray());
      when(connection.hello(RespProtocol.RESP2, creds)).thenThrow(new JedisAccessControlException(
          "NOPERM this user has no permissions to run the 'hello' command"));

      assertThrows(JedisAccessControlException.class,
        () -> handshake.establish(RedisProtocol.RESP2, creds));
      verify(connection, never()).authenticate(any());
      verify(connection, times(1)).hello(any(RespProtocol.class), any());
    }

    @Test
    void noAuthThenWrongPass_propagatesAfterAuthAttempt() {
      RedisCredentials creds = new DefaultRedisCredentials("u", "p".toCharArray());
      when(connection.hello(RespProtocol.RESP2, creds))
          .thenThrow(new JedisAccessControlException("NOAUTH Authentication required."))
          .thenThrow(new JedisAccessControlException("WRONGPASS invalid username-password pair"));

      JedisAccessControlException ex = assertThrows(JedisAccessControlException.class,
        () -> handshake.establish(RedisProtocol.RESP2, creds));
      assertTrue(ex.getMessage().startsWith("WRONGPASS"));
      InOrder inOrder = inOrder(connection);
      inOrder.verify(connection).hello(RespProtocol.RESP2, creds);
      inOrder.verify(connection).authenticate(creds);
    }
  }

  // ---------------------------------------------------------------------------
  // requested = RESP3 (strict) — no fallback on negotiation failures
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("requested = RESP3 (strict)")
  class Resp3StrictRequested {

    @Test
    void callsHello3() {
      when(connection.hello(RespProtocol.RESP3, null)).thenReturn(helloResult(RespProtocol.RESP3));

      HelloResult result = handshake.establish(RedisProtocol.RESP3, null);

      assertEquals(RespProtocol.RESP3, result.getProtocol());
      verify(connection).hello(RespProtocol.RESP3, null);
    }

    @Test
    void unknownCommand_throwsProtocolNotSupported() {
      when(connection.hello(RespProtocol.RESP3, null))
          .thenThrow(new JedisDataException("ERR unknown command 'HELLO'"));

      JedisProtocolNotSupportedException ex = assertThrows(JedisProtocolNotSupportedException.class,
        () -> handshake.establish(RedisProtocol.RESP3, null));
      assertTrue(ex.getMessage().contains("Server does not support HELLO"));
    }

    @Test
    void genericDataException_propagates() {
      when(connection.hello(RespProtocol.RESP3, null))
          .thenThrow(new JedisDataException("ERR something else"));

      JedisDataException ex = assertThrows(JedisDataException.class,
        () -> handshake.establish(RedisProtocol.RESP3, null));
      assertFalse(ex instanceof JedisProtocolNotSupportedException);
    }

    @Test
    void withCreds_succeedsInSingleHello() {
      RedisCredentials creds = new DefaultRedisCredentials("user", "pwd".toCharArray());
      when(connection.hello(RespProtocol.RESP3, creds)).thenReturn(helloResult(RespProtocol.RESP3));

      HelloResult result = handshake.establish(RedisProtocol.RESP3, creds);

      assertEquals(RespProtocol.RESP3, result.getProtocol());
      verify(connection).hello(RespProtocol.RESP3, creds);
      verify(connection, never()).authenticate(any());
    }

    @Test
    void noAuthRecovery_authThenRetryHello() {
      RedisCredentials creds = new DefaultRedisCredentials("user", "pwd".toCharArray());
      when(connection.hello(RespProtocol.RESP3, creds))
          .thenThrow(new JedisAccessControlException("NOAUTH Authentication required."))
          .thenReturn(helloResult(RespProtocol.RESP3));

      HelloResult result = handshake.establish(RedisProtocol.RESP3, creds);

      assertEquals(RespProtocol.RESP3, result.getProtocol());
      InOrder inOrder = inOrder(connection);
      inOrder.verify(connection).hello(RespProtocol.RESP3, creds);
      inOrder.verify(connection).authenticate(creds);
      inOrder.verify(connection).hello(RespProtocol.RESP3, creds);
    }

    @Test
    void nonNoAuthAccessControl_propagatesWithoutRetry() {
      RedisCredentials creds = new DefaultRedisCredentials("u", "p".toCharArray());
      when(connection.hello(RespProtocol.RESP3, creds)).thenThrow(new JedisAccessControlException(
          "NOPERM this user has no permissions to run the 'hello' command"));

      assertThrows(JedisAccessControlException.class,
        () -> handshake.establish(RedisProtocol.RESP3, creds));
      verify(connection, never()).authenticate(any());
      verify(connection, times(1)).hello(any(RespProtocol.class), any());
    }

    @Test
    void noAuthThenWrongPass_propagatesAfterAuthAttempt() {
      RedisCredentials creds = new DefaultRedisCredentials("u", "p".toCharArray());
      when(connection.hello(RespProtocol.RESP3, creds))
          .thenThrow(new JedisAccessControlException("NOAUTH Authentication required."))
          .thenThrow(new JedisAccessControlException("WRONGPASS invalid username-password pair"));

      JedisAccessControlException ex = assertThrows(JedisAccessControlException.class,
        () -> handshake.establish(RedisProtocol.RESP3, creds));
      assertTrue(ex.getMessage().startsWith("WRONGPASS"));
      InOrder inOrder = inOrder(connection);
      inOrder.verify(connection).hello(RespProtocol.RESP3, creds);
      inOrder.verify(connection).authenticate(creds);
    }
  }

  // ---------------------------------------------------------------------------
  // requested = RESP3_PREFERRED — fall back to RESP2 when RESP3/HELLO unsupported
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("requested = RESP3_PREFERRED")
  class Resp3PreferredRequested {

    @Test
    void callsHello3OnHappyPath() {
      when(connection.hello(RespProtocol.RESP3, null)).thenReturn(helloResult(RespProtocol.RESP3));

      HelloResult result = handshake.establish(RedisProtocol.RESP3_PREFERRED, null);

      assertEquals(RespProtocol.RESP3, result.getProtocol());
      verify(connection).hello(RespProtocol.RESP3, null);
    }

    @Test
    void unknownCommand_fallsBackToResp2Inferred() {
      // First HELLO 3 -> unknown -> fallback path: AUTH then HELLO 2 -> unknown -> infer RESP2
      when(connection.hello(RespProtocol.RESP3, null))
          .thenThrow(new JedisDataException("ERR unknown command 'HELLO'"));
      when(connection.hello(RespProtocol.RESP2, null))
          .thenThrow(new JedisDataException("ERR unknown command 'HELLO'"));

      HelloResult result = handshake.establish(RedisProtocol.RESP3_PREFERRED, null);

      assertEquals(RespProtocol.RESP2, result.getProtocol());
      assertNull(result.getServer());
      verify(connection).authenticate(null);
    }

    @Test
    void protocolNotSupported_fallsBackToResp2() {
      when(connection.hello(RespProtocol.RESP3, null))
          .thenThrow(new JedisDataException("ERR unknown command 'HELLO'"));
      when(connection.hello(RespProtocol.RESP2, null)).thenReturn(helloResult(RespProtocol.RESP2));

      HelloResult result = handshake.establish(RedisProtocol.RESP3_PREFERRED, null);

      assertEquals(RespProtocol.RESP2, result.getProtocol());
    }

    @Test
    void otherDataException_propagates() {
      when(connection.hello(RespProtocol.RESP3, null))
          .thenThrow(new JedisDataException("ERR something else"));

      assertThrows(JedisDataException.class,
        () -> handshake.establish(RedisProtocol.RESP3_PREFERRED, null));
    }
  }

  // ---------------------------------------------------------------------------
  // Error-message classifiers
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("error-message classifiers")
  class ErrorClassifierTests {

    @Test
    void isUnknownCommandError_recognizesUnknownCommandMessages() {
      assertTrue(ProtocolHandshake
          .isUnknownCommandError(new JedisDataException("ERR unknown command 'HELLO'")));
      assertFalse(
        ProtocolHandshake.isUnknownCommandError(new JedisDataException("ERR something else")));
      assertFalse(ProtocolHandshake
          .isUnknownCommandError(new JedisDataException("NOAUTH Authentication required.")));
    }

    @Test
    void isNoAuthError_recognizesNoAuthMessages() {
      assertTrue(
        ProtocolHandshake.isNoAuthError(new JedisDataException("NOAUTH Authentication required.")));
      assertFalse(ProtocolHandshake
          .isNoAuthError(new JedisDataException("WRONGPASS invalid username-password pair")));
      assertFalse(ProtocolHandshake.isNoAuthError(new JedisDataException("ERR something else")));
    }
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static HelloResult helloResult(RespProtocol protocol) {
    Map<String, Object> raw = new HashMap<>();
    raw.put("proto", Long.valueOf(protocol.version()));
    raw.put("server", "redis");
    raw.put("version", "7.0.0");
    return new HelloResult(raw);
  }
}
