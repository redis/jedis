package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

public enum ZMPopOption implements Rawable {
  MIN, MAX;

  private final byte[] raw;

  private ZMPopOption() {
    raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
