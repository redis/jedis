package redis.clients.jedis.timeseries;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.util.SafeEncoder;

public enum AggregationType implements Rawable {

  AVG, SUM, MIN, MAX,
  RANGE, COUNT, FIRST, LAST,
  STD_P("STD.P"), STD_S("STD.S"),
  VAR_P("VAR.P"), VAR_S("VAR.S"),
  TWA;

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
      return AggregationType.valueOf(str.replace('.', '_'));
    } catch (IllegalArgumentException iae) {
      return null;
    }
  }
}
