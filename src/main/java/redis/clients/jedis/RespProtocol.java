package redis.clients.jedis;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.exceptions.JedisProtocolNotSupportedException;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Enum representing the Redis protocol version to use when connecting to a Redis server.
 *
 * <p>Four modes are supported:
 * <ul>
 *   <li>{@code null} – used by the legacy {@link Jedis} class to avoid sending the
 *       {@code HELLO} command altogether.</li>
 *   <li>{@link #RESP2} – sends a {@code HELLO 2} command requesting RESP2; the connection
 *       fails if the server rejects the request.</li>
 *   <li>{@link #RESP3} – sends a {@code HELLO 3} command requesting RESP3; the connection
 *       fails if the server rejects the request.</li>
 *   <li>{@link #RESP3_PREFERRED} – sends a {@code HELLO 3} command to request the latest
 *       protocol version and gracefully falls back to RESP2 if the server does not support
 *       RESP3 or rejects the request. This mode is not supported by legacy Jedis class and silently ignored</li>
 * </ul>
 */
public enum RespProtocol {

  RESP2("2", SafeEncoder.encode("2")),
  RESP3("3", SafeEncoder.encode("3"));

  private final String version;
  private final byte[] versionRaw;

  private RespProtocol(String ver, byte[] verRaw) {
    this.version = ver;
    this.versionRaw = verRaw;
  }

  public static RespProtocol of(Long proto) {
    if (proto == null) {
      return null;
    }
    if (proto == 2) {
      return RESP2;
    }
    if (proto == 3) {
      return RESP3;
    }
    throw new JedisProtocolNotSupportedException("Unknown protocol version: " + proto);
  }

  public String version() {
    return version;
  }
  public byte[] versionRaw() {
    return versionRaw;
  }

  /**
   * Returns {@code true} if this protocol targets RESP3.
   *
   * @return true if this protocol targets RESP3
   */
  public boolean isResp3() {
    return this == RESP3;
  }

  /**
   * Returns {@code true} if this protocol targets RESP2.
   *
   * @return true if this protocol targets RESP2
   */
  public boolean isResp2() {
    return this == RESP2;
  }
}

