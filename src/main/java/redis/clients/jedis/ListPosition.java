package redis.clients.jedis;

import redis.clients.util.SafeEncoder;

public enum ListPosition {
  BEFORE, AFTER;
  public final byte[] raw;

  private ListPosition() {
    raw = SafeEncoder.encode(name());
  }
}
