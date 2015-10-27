package redis.clients.jedis;

import redis.clients.util.SafeEncoder;

public enum GeoUnit {
  M, KM, MI, FT;

  public final byte[] raw;

  GeoUnit() {
    raw = SafeEncoder.encode(this.name().toLowerCase());
  }
}
