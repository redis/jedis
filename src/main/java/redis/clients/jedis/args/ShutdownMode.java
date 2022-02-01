package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

public enum ShutdownMode implements Rawable {

  /**
   * kips waiting for lagging replicas, i.e. it bypasses the first step in the shutdown sequence.
   */
  NOW,
  /**
   * ignores any errors that would normally prevent the server from exiting.
   * For details, see the following section.
   */
  FORCE,
  /**
   * cancels an ongoing shutdown and cannot be combined with other flags.
   */
  ABORT;

  private final byte[] raw;

  private ShutdownMode() {
    raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
