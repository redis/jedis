package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisProtocolNotSupportedException;

/**
 * Enum representing the Redis protocol version to use when connecting to a Redis server.
 *
 * <p>Three modes are supported:
 * <ul>
 *   <li>{@code null} – combined with {@link JedisClientConfig#isAutoNegotiateProtocol()} the
 *       client either skips the {@code HELLO} command altogether (auto-negotiation disabled,
 *       legacy {@link Jedis} behaviour) or attempts {@code HELLO 3} with a graceful RESP2
 *       fallback (auto-negotiation enabled, the default for {@link UnifiedJedis}-based
 *       clients).</li>
 *   <li>{@link #RESP2} – sends a {@code HELLO 2} command requesting RESP2; the connection
 *       fails if the server rejects the request.</li>
 *   <li>{@link #RESP3} – sends a {@code HELLO 3} command requesting RESP3; the connection
 *       fails if the server rejects the request.</li>
 * </ul>
 */
public enum RedisProtocol {

  RESP2("2"),
  RESP3("3");

  private final String version;

  private RedisProtocol(String ver) {
    this.version = ver;
  }

  public String version() {
    return version;
  }

  /**
   * Returns the RedisProtocol enum value corresponding to the given protocol version number.
   * @param proto the protocol version number (2 or 3)
   * @return the corresponding RedisProtocol enum value
   * @throws JedisProtocolNotSupportedException if the protocol version is not recognized
   */
  public static RedisProtocol from(Long proto) {
    if (proto == null) return null;
    if (proto == 2) return RESP2;
    if (proto == 3) return RESP3;
    throw new JedisProtocolNotSupportedException("Unknown protocol version: " + proto);
  }
}
