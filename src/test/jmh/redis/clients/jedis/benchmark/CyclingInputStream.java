package redis.clients.jedis.benchmark;

import java.io.InputStream;

/**
 * InputStream that endlessly cycles a fixed record's bytes. Designed for JMH benchmarks that
 * consume stream content per invocation: a single instance can serve an unbounded number of reads
 * without re-allocation, removing the need for {@code @Setup(Level.Invocation)}.
 * <p>
 * The source record is pre-tiled into a chunk of at least 64KB so each downstream buffer refill is
 * satisfied by a single {@link System#arraycopy} rather than many small copies of the raw record.
 */
public final class CyclingInputStream extends InputStream {

  private static final int MIN_TILE_BYTES = 64 * 1024;

  private final byte[] tile;
  private int pos;

  public CyclingInputStream(byte[] record) {
    if (record == null || record.length == 0) {
      throw new IllegalArgumentException("record must be non-empty");
    }
    int copies = Math.max(1, MIN_TILE_BYTES / record.length);
    this.tile = new byte[record.length * copies];
    for (int i = 0; i < copies; i++) {
      System.arraycopy(record, 0, tile, i * record.length, record.length);
    }
  }

  @Override
  public int read() {
    int b = tile[pos] & 0xFF;
    if (++pos == tile.length) pos = 0;
    return b;
  }

  @Override
  public int read(byte[] b, int off, int len) {
    int written = 0;
    while (written < len) {
      int chunk = Math.min(len - written, tile.length - pos);
      System.arraycopy(tile, pos, b, off + written, chunk);
      pos += chunk;
      if (pos == tile.length) pos = 0;
      written += chunk;
    }
    return len;
  }

  @Override
  public int available() {
    return Integer.MAX_VALUE;
  }
}
