package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Reset type for command cluster reset
 */
public enum ClusterResetType implements Rawable {

  SOFT, HARD;

  private final byte[] raw;

  private ClusterResetType() {
    this.raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
