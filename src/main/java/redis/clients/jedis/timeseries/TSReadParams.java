package redis.clients.jedis.timeseries;

import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.DOLLAR;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.MINUS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.PLUS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.BLOCK;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.MAX_COUNT;

import java.util.Arrays;
import java.util.Objects;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;

/**
 * Represents the cursor and optional arguments of the {@code TS.READ} command.
 * <p>
 * Wire shape: {@code TS.READ key timestamp [BLOCK milliseconds min_count] [MAX_COUNT max_count]}.
 * The cursor selects samples whose timestamp is greater than or equal to it, in ascending timestamp
 * order. It is either a non-negative literal (Unix milliseconds) or one of the sentinels {@code -}
 * (earliest), {@code +} (latest existing sample, inclusive) or {@code $} (only samples added after
 * the call). The sentinels are sent as-is and resolved by the server.
 * <p>
 * The {@code BLOCK} group is all-or-nothing: whenever blocking is requested both
 * {@code milliseconds} and {@code minCount} are emitted. {@code BLOCK 0} waits indefinitely.
 * @since 8.0
 */
public class TSReadParams implements IParams {

  // Cursor bytes; defaults to earliest ("-") when left unset.
  private byte[] timestamp = MINUS;

  private Long blockMilliseconds;
  private Integer blockMinCount;

  private Integer maxCount;

  public TSReadParams() {
  }

  public static TSReadParams readParams() {
    return new TSReadParams();
  }

  /**
   * Literal cursor: samples with {@code sample_timestamp >= timestamp} qualify. {@code 0} reads
   * from the beginning. Negative values are rejected by the server.
   */
  public TSReadParams timestamp(long timestamp) {
    this.timestamp = toByteArray(timestamp);
    return this;
  }

  /**
   * Cursor sentinel {@code -}: no lower bound, read from the earliest sample.
   */
  public TSReadParams earliest() {
    this.timestamp = MINUS;
    return this;
  }

  /**
   * Cursor sentinel {@code +}: the latest existing sample's timestamp, inclusive. On an empty or
   * missing series it resolves to {@code 0}.
   */
  public TSReadParams latest() {
    this.timestamp = PLUS;
    return this;
  }

  /**
   * Cursor sentinel {@code $}: the latest sample's timestamp + 1, so only samples added after the
   * command is received qualify. Meaningful only together with {@link #block(long, int)}; without
   * blocking it always yields an empty reply.
   */
  public TSReadParams newSamples() {
    this.timestamp = DOLLAR;
    return this;
  }

  /**
   * Opt into blocking. Both values are always emitted on the wire inside the {@code BLOCK} group.
   * @param milliseconds maximum wait, non-negative; {@code 0} means wait indefinitely
   * @param minCount unblock threshold, positive; the call returns once this many samples qualify
   * @return this
   */
  public TSReadParams block(long milliseconds, int minCount) {
    if (milliseconds < 0) {
      throw new IllegalArgumentException("BLOCK milliseconds must be a non-negative integer");
    }
    if (minCount <= 0) {
      throw new IllegalArgumentException("BLOCK min_count must be a positive integer");
    }
    this.blockMilliseconds = milliseconds;
    this.blockMinCount = minCount;
    return this;
  }

  /**
   * Reply cap. When more samples qualify than {@code maxCount}, the oldest {@code maxCount} are
   * returned so callers can page forward. Omitted means unlimited.
   * @param maxCount positive integer
   * @return this
   */
  public TSReadParams maxCount(int maxCount) {
    if (maxCount <= 0) {
      throw new IllegalArgumentException("MAX_COUNT must be a positive integer");
    }
    this.maxCount = maxCount;
    return this;
  }

  /**
   * @return true when the {@code BLOCK} group is present, so the command must be issued with
   *         blocking-command connection handling
   */
  public boolean isBlocking() {
    return blockMilliseconds != null;
  }

  @Override
  public void addParams(CommandArguments args) {

    // min_count <= max_count is required by the server when both are set; validate locally too.
    if (blockMinCount != null && maxCount != null && blockMinCount > maxCount) {
      throw new IllegalArgumentException("BLOCK min_count must be <= MAX_COUNT");
    }

    args.add(timestamp);

    if (blockMilliseconds != null) {
      args.add(BLOCK).add(toByteArray(blockMilliseconds)).add(toByteArray(blockMinCount));
    }

    if (maxCount != null) {
      args.add(MAX_COUNT).add(toByteArray(maxCount));
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TSReadParams that = (TSReadParams) o;
    return Arrays.equals(timestamp, that.timestamp)
        && Objects.equals(blockMilliseconds, that.blockMilliseconds)
        && Objects.equals(blockMinCount, that.blockMinCount)
        && Objects.equals(maxCount, that.maxCount);
  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(timestamp);
    result = 31 * result + Objects.hashCode(blockMilliseconds);
    result = 31 * result + Objects.hashCode(blockMinCount);
    result = 31 * result + Objects.hashCode(maxCount);
    return result;
  }
}
