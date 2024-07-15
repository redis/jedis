package redis.clients.jedis.timeseries;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Specifies the series samples encoding format.
 */
public enum EncodingFormat implements Rawable {

  COMPRESSED,
  UNCOMPRESSED;

  private final byte[] raw;

  private EncodingFormat() {
    raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
