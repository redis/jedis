package redis.clients.jedis.timeseries;

import static redis.clients.jedis.Protocol.BYTES_TILDE;
import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.MINUS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.PLUS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.*;
import static redis.clients.jedis.util.SafeEncoder.encode;

import java.util.Arrays;
import java.util.Objects;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;

/**
 * Represents optional arguments of the multi-key pivot commands {@code TS.NRANGE} and
 * {@code TS.NREVRANGE}.
 * <p>
 * The command keys ({@code numkeys key [key ...]}) are supplied through the command method and are
 * <b>not</b> part of these params; this object carries only
 * {@code fromTimestamp}/{@code toTimestamp} and the optional range arguments, which are emitted on
 * the wire in the canonical order documented on {@link #addParams(CommandArguments)}.
 * @since 8.0
 */
public class TSNRangeParams implements IParams {

  private Long fromTimestamp;
  private Long toTimestamp;

  private boolean latest;

  private long[] filterByTimestamps;
  private double[] filterByValues;

  private Integer count;

  private byte[] align;

  // One entry per key argument; each entry is the (comma-joined on the wire) list of aggregators
  // for that key. AGGREGATION token count must equal numkeys (enforced by the server).
  private AggregationType[][] aggregators;
  private long bucketDuration;
  private byte[] bucketTimestamp;

  private boolean empty;

  public TSNRangeParams(long fromTimestamp, long toTimestamp) {
    this.fromTimestamp = fromTimestamp;
    this.toTimestamp = toTimestamp;
  }

  public static TSNRangeParams nrangeParams(long fromTimestamp, long toTimestamp) {
    return new TSNRangeParams(fromTimestamp, toTimestamp);
  }

  public TSNRangeParams() {
  }

  public static TSNRangeParams nrangeParams() {
    return new TSNRangeParams();
  }

  public TSNRangeParams fromTimestamp(long fromTimestamp) {
    this.fromTimestamp = fromTimestamp;
    return this;
  }

  public TSNRangeParams toTimestamp(long toTimestamp) {
    this.toTimestamp = toTimestamp;
    return this;
  }

  public TSNRangeParams latest() {
    this.latest = true;
    return this;
  }

  public TSNRangeParams filterByTS(long... timestamps) {
    this.filterByTimestamps = timestamps;
    return this;
  }

  public TSNRangeParams filterByValues(double min, double max) {
    this.filterByValues = new double[] { min, max };
    return this;
  }

  public TSNRangeParams count(int count) {
    this.count = count;
    return this;
  }

  private TSNRangeParams align(byte[] raw) {
    this.align = raw;
    return this;
  }

  /**
   * This requires AGGREGATION.
   */
  public TSNRangeParams align(long timestamp) {
    return align(toByteArray(timestamp));
  }

  /**
   * This requires AGGREGATION.
   */
  public TSNRangeParams alignStart() {
    return align(MINUS);
  }

  /**
   * This requires AGGREGATION.
   */
  public TSNRangeParams alignEnd() {
    return align(PLUS);
  }

  /**
   * Applies exactly one aggregator per key, in key order. The number of aggregators must equal the
   * number of keys sent to the command (the server rejects a mismatch). On the wire each aggregator
   * is emitted as its own {@code AGGREGATION} token.
   * @param aggregators ordered, non-empty list holding one aggregator per key
   * @param bucketDuration aggregation bucket duration in milliseconds
   * @return this
   * @throws IllegalArgumentException if {@code aggregators} is empty or contains a null element
   */
  public TSNRangeParams aggregation(AggregationType[] aggregators, long bucketDuration) {
    if (aggregators == null) {
      this.aggregators = null;
      this.bucketDuration = 0;
      return this;
    }
    if (aggregators.length == 0) {
      throw new IllegalArgumentException("Aggregators must be non-null and non-empty");
    }
    AggregationType[][] perKey = new AggregationType[aggregators.length][];
    for (int i = 0; i < aggregators.length; i++) {
      if (aggregators[i] == null) {
        throw new IllegalArgumentException("Aggregators must not contain null elements");
      }
      perKey[i] = new AggregationType[] { aggregators[i] };
    }
    this.aggregators = perKey;
    this.bucketDuration = bucketDuration;
    return this;
  }

  /**
   * Applies one or more aggregators per key, in key order. The outer array length must equal the
   * number of keys sent to the command (the server rejects a mismatch); each inner array holds the
   * aggregators for that key and is emitted as a single comma-joined {@code AGGREGATION} token
   * (e.g. {@code AVG,MAX}). The response returns one value column per aggregator, flattened in key
   * then aggregator order.
   * @param aggregators ordered, non-empty per-key lists of aggregators
   * @param bucketDuration aggregation bucket duration in milliseconds
   * @return this
   * @throws IllegalArgumentException if {@code aggregators} (or any inner array) is empty or
   *           contains a null element
   */
  public TSNRangeParams aggregation(AggregationType[][] aggregators, long bucketDuration) {
    if (aggregators == null) {
      this.aggregators = null;
      this.bucketDuration = 0;
      return this;
    }
    if (aggregators.length == 0) {
      throw new IllegalArgumentException("Aggregators must be non-null and non-empty");
    }
    for (AggregationType[] perKey : aggregators) {
      if (perKey == null || perKey.length == 0) {
        throw new IllegalArgumentException("Aggregators must be non-null and non-empty");
      }
      for (AggregationType a : perKey) {
        if (a == null) {
          throw new IllegalArgumentException("Aggregators must not contain null elements");
        }
      }
    }
    this.aggregators = aggregators;
    this.bucketDuration = bucketDuration;
    return this;
  }

  /**
   * Validates this aggregation configuration against the actual number of command keys.
   * {@code TS.NRANGE}/{@code TS.NREVRANGE} require exactly one aggregation spec per key, so when
   * aggregation is set the number of specs must equal {@code numKeys}. This mirrors the server rule
   * client-side so callers fail fast instead of relying on the {@code the number of AGGREGATION
   * arguments must be equal to numkeys} error. No-op when aggregation is not set.
   * @param numKeys number of keys passed to the command
   * @throws IllegalArgumentException if aggregation is set and the spec count differs from
   *           {@code numKeys}
   */
  public void validateAggregationForKeys(int numKeys) {
    if (aggregators != null && aggregators.length != numKeys) {
      throw new IllegalArgumentException("Aggregation requires exactly one spec per key: numkeys="
          + numKeys + " but got " + aggregators.length + " aggregation spec(s)");
    }
  }

  /**
   * This requires AGGREGATION.
   */
  public TSNRangeParams bucketTimestamp(String bucketTimestamp) {
    this.bucketTimestamp = encode(bucketTimestamp);
    return this;
  }

  /**
   * This requires AGGREGATION.
   */
  public TSNRangeParams bucketTimestampLow() {
    this.bucketTimestamp = MINUS;
    return this;
  }

  /**
   * This requires AGGREGATION.
   */
  public TSNRangeParams bucketTimestampHigh() {
    this.bucketTimestamp = PLUS;
    return this;
  }

  /**
   * This requires AGGREGATION.
   */
  public TSNRangeParams bucketTimestampMid() {
    this.bucketTimestamp = BYTES_TILDE;
    return this;
  }

  /**
   * This requires AGGREGATION.
   */
  public TSNRangeParams empty() {
    this.empty = true;
    return this;
  }

  /**
   * Emits, in order: {@code fromTimestamp toTimestamp} (defaulting to the {@code -}/{@code +}
   * sentinels), then the optional tokens {@code LATEST}, {@code FILTER_BY_TS},
   * {@code FILTER_BY_VALUE}, {@code COUNT}, and the aggregation clause
   * ({@code [ALIGN align] AGGREGATION agg [agg ...]
   * bucketDuration [BUCKETTIMESTAMP bt] [EMPTY]}). One {@code AGGREGATION} token is emitted per
   * key.
   */
  @Override
  public void addParams(CommandArguments args) {

    if (fromTimestamp == null) {
      args.add(MINUS);
    } else {
      args.add(toByteArray(fromTimestamp));
    }

    if (toTimestamp == null) {
      args.add(PLUS);
    } else {
      args.add(toByteArray(toTimestamp));
    }

    if (latest) {
      args.add(LATEST);
    }

    if (filterByTimestamps != null) {
      args.add(FILTER_BY_TS);
      for (long ts : filterByTimestamps) {
        args.add(toByteArray(ts));
      }
    }

    if (filterByValues != null) {
      args.add(FILTER_BY_VALUE);
      for (double value : filterByValues) {
        args.add(toByteArray(value));
      }
    }

    if (count != null) {
      args.add(COUNT).add(toByteArray(count));
    }

    if (aggregators != null) {

      if (align != null) {
        args.add(ALIGN).add(align);
      }

      args.add(AGGREGATION);
      for (AggregationType[] perKey : aggregators) {
        args.add(AggregationType.joinRaw(perKey));
      }
      args.add(toByteArray(bucketDuration));

      if (bucketTimestamp != null) {
        args.add(BUCKETTIMESTAMP).add(bucketTimestamp);
      }

      if (empty) {
        args.add(EMPTY);
      }
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

    TSNRangeParams that = (TSNRangeParams) o;
    return latest == that.latest && bucketDuration == that.bucketDuration && empty == that.empty
        && Objects.equals(fromTimestamp, that.fromTimestamp)
        && Objects.equals(toTimestamp, that.toTimestamp)
        && Arrays.equals(filterByTimestamps, that.filterByTimestamps)
        && Arrays.equals(filterByValues, that.filterByValues) && Objects.equals(count, that.count)
        && Arrays.equals(align, that.align) && Arrays.deepEquals(aggregators, that.aggregators)
        && Arrays.equals(bucketTimestamp, that.bucketTimestamp);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(fromTimestamp);
    result = 31 * result + Objects.hashCode(toTimestamp);
    result = 31 * result + Boolean.hashCode(latest);
    result = 31 * result + Arrays.hashCode(filterByTimestamps);
    result = 31 * result + Arrays.hashCode(filterByValues);
    result = 31 * result + Objects.hashCode(count);
    result = 31 * result + Arrays.hashCode(align);
    result = 31 * result + Arrays.deepHashCode(aggregators);
    result = 31 * result + Long.hashCode(bucketDuration);
    result = 31 * result + Arrays.hashCode(bucketTimestamp);
    result = 31 * result + Boolean.hashCode(empty);
    return result;
  }
}
