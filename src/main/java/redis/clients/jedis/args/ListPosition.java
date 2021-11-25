package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

public enum ListPosition implements Rawable {

  BEFORE, AFTER;

  private final byte[] raw;

  private ListPosition() {
    raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
