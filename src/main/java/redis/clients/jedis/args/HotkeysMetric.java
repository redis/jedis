package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Enum representing the metrics that can be tracked by the HOTKEYS command.
 */
public enum HotkeysMetric implements Rawable {

  /**
   * Track CPU time consumption per key.
   */
  CPU,

  /**
   * Track network bytes transferred per key.
   */
  NET;

  private final byte[] raw;

  private HotkeysMetric() {
    raw = SafeEncoder.encode(this.name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
