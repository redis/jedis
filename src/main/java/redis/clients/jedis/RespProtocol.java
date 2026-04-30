package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisProtocolNotSupportedException;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Enum representing supported versions of the RESP protocol.
 * <p>
 * Two modes are supported:
 * <ul>
 * <li>{@link #RESP2} – RESP2 protocol.</li>
 * <li>{@link #RESP3} – RESP3 protocol.</li>
 * </ul>
 */
public enum RespProtocol {

  RESP2("2", SafeEncoder.encode("2")), RESP3("3", SafeEncoder.encode("3"));

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
   * @return true if this protocol targets RESP3
   */
  public boolean isResp3() {
    return this == RESP3;
  }

  /**
   * Returns {@code true} if this protocol targets RESP2.
   * @return true if this protocol targets RESP2
   */
  public boolean isResp2() {
    return this == RESP2;
  }
}
