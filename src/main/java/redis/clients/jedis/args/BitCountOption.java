package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * The args for the command bitcount
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
