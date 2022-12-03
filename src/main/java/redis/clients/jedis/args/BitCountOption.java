package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Bit operations for {@code BITOP} command.
 */
public enum BitCountOption implements Rawable {

  BYTE, BIT;

  private final byte[] raw;

  private BitCountOption() {
    raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
