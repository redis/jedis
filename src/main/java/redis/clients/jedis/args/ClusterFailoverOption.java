package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Enumeration of cluster failover options.
 * <p>
 * Used by {@link redis.clients.jedis.commands.ClusterCommands#clusterFailover(ClusterFailoverOption)}.
 */
public enum ClusterFailoverOption implements Rawable {

  FORCE, TAKEOVER;

  private final byte[] raw;

  private ClusterFailoverOption() {
    this.raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
