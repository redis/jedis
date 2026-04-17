package redis.clients.jedis;

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
}
