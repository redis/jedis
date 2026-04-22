package redis.clients.jedis;

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
public enum RedisProtocol {

  RESP2("2"),
  RESP3("3"),

  /**
   * Try to use RESP3 by default and fall back to RESP2 if the server does not support it.
   */
  RESP3_PREFERRED("AUTO");

  private final String version;

  private RedisProtocol(String ver) {
    this.version = ver;
  }

  public String version() {
    return version;
  }

  /**
   * Returns {@code true} if this protocol targets RESP3 (either strict or preferred).
   */
  public boolean isResp3() {
    return this == RESP3 || this == RESP3_PREFERRED;
  }

  /**
   * Returns {@code true} if the given protocol targets RESP3 (either strict or preferred).
   * A {@code null} protocol returns {@code false}.
   */
  public static boolean isResp3(RedisProtocol protocol) {
    return protocol != null && protocol.isResp3();
  }

  /**
   * Returns the RedisProtocol enum value corresponding to the given protocol version number.
   * @param proto the protocol version number (2 or 3)
   * @return the corresponding RedisProtocol enum value
   * @throws IllegalArgumentException if the protocol version is not recognized
   */
  public static RedisProtocol from(Long proto) {
    if (proto == null) return null;
    if (proto == 2) return RESP2;
    if (proto == 3) return RESP3;
    throw new IllegalArgumentException("Unknown protocol version: " + proto);
  }
}
