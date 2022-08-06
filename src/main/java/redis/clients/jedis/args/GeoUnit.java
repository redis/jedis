package redis.clients.jedis.args;

import java.util.Locale;
import redis.clients.jedis.util.SafeEncoder;

public enum GeoUnit implements Rawable {

  M, KM, MI, FT;

  private final byte[] raw;
  private final String unit;

  GeoUnit() {
    unit = name().toLowerCase(Locale.ENGLISH);
    raw = SafeEncoder.encode(unit);
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }

  @Override
  public String toString() {
    return unit;
  }
}
