package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

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
