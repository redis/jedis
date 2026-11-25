package redis.clients.jedis;

/**
 * RESP protocol versions supported by the client.
 * <p>
 * How a connection chooses its protocol depends on the configured
 * {@linkplain JedisClientConfig#getRedisProtocol() protocol} and on
 * {@linkplain JedisClientConfig#isAutoNegotiateProtocol() auto-negotiation}.
 */
public enum RedisProtocol {

  RESP2("2"),
  RESP3("3");

  /**
   * The protocol the client assumes when the protocol is not enforced.
   */
  public static final RedisProtocol REDIS_SERVER_DEFAULT_PROTO = RESP2;

  private final String version;

  private RedisProtocol(String ver) {
    this.version = ver;
  }

  public String version() {
    return version;
  }

  /**
   * Returns the protocol for the given version number.
   * @param proto the protocol version number (2 or 3), may be {@code null}
   * @return the matching protocol, or {@code null} if {@code proto} is {@code null}
   * @throws IllegalArgumentException if the version is not recognized
   */
  public static RedisProtocol from(Long proto) {
    if (proto == null) return null;
    if (proto == 2) return RESP2;
    if (proto == 3) return RESP3;
    throw new IllegalArgumentException("Unknown protocol version: " + proto);
  }

  /**
   * Returns the given protocol, or {@link #REDIS_SERVER_DEFAULT_PROTO} if it is {@code null}.
   * @param proto the protocol, may be {@code null}
   * @return {@code proto}, or the server default
   */
  public static RedisProtocol orServerDefault(RedisProtocol proto) {
    return (proto == null) ? REDIS_SERVER_DEFAULT_PROTO : proto;
  }
}
