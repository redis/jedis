package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

public enum ClientPauseMode implements Rawable {

  ALL, WRITE;

  private final byte[] raw;

  private ClientPauseMode() {
    raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
