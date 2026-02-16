package redis.clients.jedis.timeseries;

import java.util.Locale;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.util.SafeEncoder;

public enum AggregationType implements Rawable {

  AVG, SUM, MIN, MAX,
  RANGE, COUNT, FIRST, LAST,
  STD_P("STD.P"), STD_S("STD.S"),
  VAR_P("VAR.P"), VAR_S("VAR.S"),
  TWA,
  /**
   * Count the number of NaN values in the bucket.
   * @since RedisTimeSeries 8.6.0
   */
  COUNTNAN,
  /**
   * Count all values in the bucket, including NaN values.
   * @since RedisTimeSeries 8.6.0
   */
  COUNTALL;

  private final byte[] raw;

  private AggregationType() {
    raw = SafeEncoder.encode(name());
  }

  private AggregationType(String alt) {
    raw = SafeEncoder.encode(alt);
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }

  public static AggregationType safeValueOf(String str) {
    try {
      return AggregationType.valueOf(str.replace('.', '_').toUpperCase(Locale.ENGLISH));
    } catch (IllegalArgumentException iae) {
      return null;
    }
  }
}
