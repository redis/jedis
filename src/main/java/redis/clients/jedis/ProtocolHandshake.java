package redis.clients.jedis;

import java.util.Collections;

import redis.clients.jedis.exceptions.JedisAccessControlException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisProtocolNotSupportedException;

/**
 * Encapsulates the RESP protocol handshake logic for {@link Connection}.
 * <p>
 * Owns HELLO-based protocol negotiation, RESP3 fallback handling, and the recovery path for the
 * Redis 6.0.x NOAUTH bug. Authentication itself remains on {@link Connection}; this class delegates
 * to {@link Connection#authenticate(RedisCredentials)} where required.
 * </p>
 */
final class ProtocolHandshake {

  private final Connection connection;

  ProtocolHandshake(Connection connection) {
    this.connection = connection;
  }

  /**
   * Establish the RESP protocol version and authenticate if needed. Performs protocol negotiation
   * using the {@code HELLO} command and optional authentication, and resolves the effective RESP
   * protocol used for the connection.
   * <p>
   * This method supports both explicit protocol selection and legacy compatibility mode.
   * </p>
   * <p>
   * Behavior:
   * </p>
   * <ul>
   * <li>If {@code requestedProtocol} is {@code null}, no {@code HELLO} is sent. The connection
   * assumes RESP2 as the default protocol and only {@code AUTH} is performed if credentials are
   * provided.</li>
   * <li>If {@code RESP2} is requested, a strict {@code HELLO 2} handshake is performed.</li>
   * <li>If {@code RESP3} is requested, a strict {@code HELLO 3} handshake is performed.</li>
   * <li>If {@code RESP3_PREFERRED} is requested, the client first attempts {@code HELLO 3} and
   * falls back to RESP2 if RESP3 is not supported.</li>
   * </ul>
   * <p>
   * Legacy behavior:
   * </p>
   * <ul>
   * <li>When no protocol is explicitly configured, the client assumes RESP2 without performing
   * protocol negotiation.</li>
   * </ul>
   * @param requestedProtocol the requested RESP protocol (may be {@code null} for legacy mode)
   * @param credentials credentials used for authentication (may be {@code null})
   * @return the {@link HelloResult} carrying negotiated protocol and server metadata
   * @throws IllegalArgumentException if the requested protocol is not supported
   * @throws JedisProtocolNotSupportedException if protocol negotiation fails
   * @throws JedisDataException if the server returns an error during handshake
   */
  HelloResult establish(final RedisProtocol requestedProtocol, final RedisCredentials credentials) {
    boolean noProtocolRequested = requestedProtocol == null;

    // This is needed to keep the compatibility with legacy Jedis class and
    // avoid sending hello command when user haven't provided any protocol version and credentials.
    // if no protocol requested we assume server default is RESP2,
    // and configure connection to expect RESP2
    if (noProtocolRequested) {
      connection.authenticate(credentials);
      return new HelloResult(
          Collections.singletonMap("proto", Long.valueOf(RespProtocol.RESP2.version())));
    } else if (requestedProtocol == RedisProtocol.RESP2) {
      return enforceProtocolWithAuth(RespProtocol.RESP2, credentials);
    } else if (requestedProtocol == RedisProtocol.RESP3) {
      return enforceProtocolWithAuth(RespProtocol.RESP3, credentials);
    } else if (requestedProtocol == RedisProtocol.RESP3_PREFERRED) {
      return negotiateResp3WithFallback(credentials);
    } else {
      throw new IllegalArgumentException("Unsupported protocol: " + requestedProtocol);
    }
  }

  /**
   * Send HELLO command to the server to negotiate the protocol version and authenticate if needed.
   * <p>
   * Attempts RESP3 handshake, falls back to RESP2 if not supported.
   * </p>
   * @param credentials credentials for authentication
   * @return {@link HelloResult} the actual negotiated protocol version
   */
  private HelloResult negotiateResp3WithFallback(final RedisCredentials credentials) {
    try {
      return enforceProtocolWithAuth(RespProtocol.RESP3, credentials);
    } catch (JedisProtocolNotSupportedException e) {
      // fall back to resp2
      return establishLegacyResp2(credentials);
    } catch (JedisDataException e) {
      // fall back to resp2
      if (isUnknownCommandError(e)) {
        return establishLegacyResp2(credentials);
      }
      throw e;
    }
  }

  /**
   * Performs strict protocol negotiation using the {@code HELLO} command.
   * <p>
   * This method enforces the provided {@code protocol} version and expects the server to support
   * the {@code HELLO} command. It does not perform protocol fallback (e.g., RESP3 → RESP2).
   * </p>
   * <p>
   * Behavior:
   * </p>
   * <ul>
   * <li>Attempts negotiation via {@code HELLO <protocol>} without authentication.</li>
   * <li>If the server rejects the request with a NOAUTH error (observed in Redis 6.0.x prior to
   * 6.2.2), performs an {@code AUTH} using the provided credentials and retries the handshake.</li>
   * <li>Any non-authentication-related errors are propagated to the caller.</li>
   * </ul>
   * <p>
   * Notes:
   * </p>
   * <ul>
   * <li>Some Redis 6.0.x versions require authentication before allowing {@code HELLO}, even though
   * {@code HELLO AUTH} is supported in later versions.</li>
   * <li>This method assumes the server supports the requested protocol; unsupported protocol errors
   * are not handled and will be propagated.</li>
   * </ul>
   * @param protocol the RESP protocol version to negotiate (must not be {@code null})
   * @param credentials credentials used for authentication if required (may be {@code null})
   * @return the {@code HELLO} response containing negotiated protocol and server metadata
   * @throws IllegalArgumentException if {@code protocol} is {@code null}
   * @throws JedisProtocolNotSupportedException if the server does not support the requested
   *           protocol
   * @throws JedisAccessControlException if authentication fails and cannot be recovered
   */
  private HelloResult enforceProtocolWithAuth(RespProtocol protocol, RedisCredentials credentials) {
    if (protocol == null) {
      throw new IllegalArgumentException("protocol must not be null");
    }

    try {
      try {
        return connection.hello(protocol, null);
      } catch (JedisDataException e) {
        if (isUnknownCommandError(e)) {
          throw new JedisProtocolNotSupportedException("Server does not support HELLO", e);
        } else {
          throw e;
        }
      }
    } catch (JedisAccessControlException e) {
      // Redis 6.0.x (before 6.2.2) has a bug where HELLO with AUTH fails if the default user
      // requires authentication — the server demands AUTH before allowing HELLO.
      // See: https://github.com/redis/redis/issues/8558
      // See: https://github.com/redis/lettuce/issues/2592
      if (isNoAuthError(e)) {
        connection.authenticate(credentials);
        return connection.hello(protocol, credentials);
      } else {
        throw e;
      }
    }

  }

  /**
   * Fallback handshake used when RESP3 or {@code HELLO} is not supported by the server.
   * <p>
   * This method provides compatibility with legacy Redis servers that do not support the
   * {@code HELLO} command.
   * </p>
   * <p>
   * Behavior:
   * </p>
   * <ul>
   * <li>Performs {@code AUTH} if credentials are provided.</li>
   * <li>Attempts a {@code HELLO 2} command to retrieve server metadata.</li>
   * <li>If the server does not support {@code HELLO}, assumes RESP2 as the default protocol.</li>
   * </ul>
   * <p>
   * Fallback logic:
   * </p>
   * <ul>
   * <li>If {@code HELLO} succeeds → uses returned protocol and metadata.</li>
   * <li>If {@code HELLO} fails with unknown command → assumes RESP2 and continues.</li>
   * <li>Any other errors are propagated to the caller.</li>
   * </ul>
   * <p>
   * Note:
   * </p>
   * <ul>
   * <li>This method exists solely for backward compatibility with Redis versions prior to 6.0.</li>
   * <li>Server version and protocol information may be incomplete when fallback is used.</li>
   * </ul>
   * @param credentials credentials used for authentication (may be {@code null})
   * @return {@link HelloResult} containing protocol and server metadata (may be inferred for legacy
   *         servers)
   * @throws JedisDataException if a non-recoverable server error occurs
   */
  private HelloResult establishLegacyResp2(final RedisCredentials credentials) {
    // authenticate first to support legacy behavior on server not supporting HELLO
    connection.authenticate(credentials);

    try {
      return connection.hello(RespProtocol.RESP2, null);
    } catch (JedisDataException e) {
      // if server does not support hello, we assume RESP2
      if (isUnknownCommandError(e)) {
        return new HelloResult(
            Collections.singletonMap("proto", Long.valueOf(RespProtocol.RESP2.version())));
      }

      throw e;
    }
  }

  static boolean isNoAuthError(JedisDataException e) {
    return e.getMessage().startsWith("NOAUTH");
  }

  static boolean isUnknownCommandError(JedisDataException e) {
    return e.getMessage().startsWith("ERR") && e.getMessage().contains("unknown command");
  }
}
