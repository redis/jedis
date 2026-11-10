package redis.clients.jedis.bloom;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.args.Rawable;

/**
 * Counter cell size of a Count-Min Sketch, the {@code CELL_SIZE} option of {@code CMS.INITBYDIM}
 * and {@code CMS.INITBYPROB}. Smaller cells reduce memory usage but lower the maximum count a cell
 * can hold before {@code CMS.INCRBY} fails with an overflow error. The server default is
 * {@link #FOUR_BYTES}.
 * @since 8.1
 */
public enum CmsCellSize implements Rawable {

  /** 1-byte counters, max count 255. */
  ONE_BYTE(1),
  /** 2-byte counters, max count 65535. */
  TWO_BYTES(2),
  /** 4-byte counters, the server default. */
  FOUR_BYTES(4),
  /** 8-byte counters, for very large totals. */
  EIGHT_BYTES(8);

  private final byte[] raw;

  private CmsCellSize(int bytes) {
    raw = Protocol.toByteArray(bytes);
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
