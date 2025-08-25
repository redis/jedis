package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Bit operations for {@code BITOP} command.
 */
public enum BitOP implements Rawable {

  AND, OR, XOR, NOT, DIFF, DIFF1, ANDOR, ONE;

  private final byte[] raw;

  private BitOP() {
    raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
