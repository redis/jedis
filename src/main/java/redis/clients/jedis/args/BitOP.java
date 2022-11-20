package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * The operations of the command bitop
 */
public enum BitOP implements Rawable {

  AND, OR, XOR, NOT;

  private final byte[] raw;

  private BitOP() {
    raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
