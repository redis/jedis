package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Reset type for command cluster reset. Before reset cluster status you should make true no data in Redis.
 * It is generally used to create/expand clusters.
 */
public enum ClusterResetType implements Rawable {

  /**
   * Soft reset: Reset only the cluster info.
   */
  SOFT,

  /**
   * Hard reset: Reset the cluster info, set epochs to 0, change node ID.
   */
  HARD;

  private final byte[] raw;

  private ClusterResetType() {
    this.raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
