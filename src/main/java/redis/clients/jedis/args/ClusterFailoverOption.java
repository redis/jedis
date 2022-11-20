package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * The Args of cluster failover.
 * Enum object describing cluster failover.
 * Used by {@link redis.clients.jedis.commands.ClusterCommands#clusterFailover(ClusterFailoverOption)}
 * */
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
