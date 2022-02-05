package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

public enum ExpiryOption implements Rawable {

  NX, XX, GT, LT;

  private final byte[] raw;

  private ExpiryOption() {
    raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
