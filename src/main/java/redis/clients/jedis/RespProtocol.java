package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisProtocolNotSupportedException;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Internal enum representing the two concrete RESP protocol versions negotiated with the server
 * during the handshake. Used only by {@link ProtocolHandshake} and {@link Connection} to model the
 * resolved (post-handshake) protocol; external callers should rely on
 * {@link Connection#getRedisProtocol()} which returns the corresponding {@link RedisProtocol}.
 */
enum RespProtocol {

  RESP2("2", SafeEncoder.encode("2")), RESP3("3", SafeEncoder.encode("3"));

  private final String version;
  private final byte[] versionRaw;

  RespProtocol(String ver, byte[] verRaw) {
    this.version = ver;
    this.versionRaw = verRaw;
  }

  static RespProtocol of(Long proto) {
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

  String version() {
    return version;
  }

  byte[] versionRaw() {
    return versionRaw;
  }
}
