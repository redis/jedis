package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

public enum ZSetOption implements Rawable {

  MIN, MAX;

  private final byte[] raw;

  private ZSetOption() {
    raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
