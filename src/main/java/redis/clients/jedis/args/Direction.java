package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Direction for {@code LMOVE} and {@code BLMOVE} command.
 */
public enum Direction implements Rawable {
  LEFT, RIGHT;

  private final byte[] raw;

  Direction() {
    raw = SafeEncoder.encode(this.name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
