package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

public enum GeoUnit implements Rawable {
  M, KM, MI, FT;

  private final byte[] raw;

  private GeoUnit() {
    raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
