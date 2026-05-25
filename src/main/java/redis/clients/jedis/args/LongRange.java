package redis.clients.jedis.args;

import java.util.Objects;

/**
 * Inclusive {@code [start, end]} index range, used by array commands such as {@code ARDELRANGE} and
 * {@code AROP}. Each range is serialized on the wire as two integer arguments {@code start end}.
 */
public final class LongRange {

  private final long start;
  private final long end;

  private LongRange(long start, long end) {
    this.start = start;
    this.end = end;
  }

  /**
   * Create a new {@link LongRange}.
   * @param start zero-based start index (inclusive)
   * @param end zero-based end index (inclusive)
   * @return a new {@link LongRange}
   */
  public static LongRange of(long start, long end) {
    return new LongRange(start, end);
  }

  public long start() {
    return start;
  }

  public long end() {
    return end;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof LongRange)) return false;
    LongRange that = (LongRange) o;
    return start == that.start && end == that.end;
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
  }

  @Override
  public String toString() {
    return "[" + start + ", " + end + "]";
  }
}
