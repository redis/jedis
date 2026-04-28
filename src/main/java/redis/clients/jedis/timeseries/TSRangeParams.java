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
 * Represents optional arguments of TS.RANGE and TS.REVRANGE commands.
 */
public class TSRangeParams implements IParams {

  private Long fromTimestamp;
  private Long toTimestamp;

  private boolean latest;

  private long[] filterByTimestamps;
  private double[] filterByValues;

  private Integer count;

  private byte[] align;

  private AggregationType[] aggregators;
  private long bucketDuration;
  private byte[] bucketTimestamp;

  private boolean empty;

  public TSRangeParams(long fromTimestamp, long toTimestamp) {
    this.fromTimestamp = fromTimestamp;
    this.toTimestamp = toTimestamp;
  }

  public static TSRangeParams rangeParams(long fromTimestamp, long toTimestamp) {
    return new TSRangeParams(fromTimestamp, toTimestamp);
  }

  public TSRangeParams() {
  }

  public static TSRangeParams rangeParams() {
    return new TSRangeParams();
  }

  public TSRangeParams fromTimestamp(long fromTimestamp) {
    this.fromTimestamp = fromTimestamp;
    return this;
  }

  public TSRangeParams toTimestamp(long toTimestamp) {
    this.toTimestamp = toTimestamp;
    return this;
  }

  public TSRangeParams latest() {
    this.latest = true;
    return this;
  }

  public TSRangeParams filterByTS(long... timestamps) {
    this.filterByTimestamps = timestamps;
    return this;
  }

  public TSRangeParams filterByValues(double min, double max) {
    this.filterByValues = new double[]{min, max};
    return this;
  }

  public TSRangeParams count(int count) {
    this.count = count;
    return this;
  }

  private TSRangeParams align(byte[] raw) {
    this.align = raw;
    return this;
  }

  /**
   * This requires AGGREGATION.
   */
  public TSRangeParams align(long timestamp) {
    return align(toByteArray(timestamp));
  }

  /**
   * This requires AGGREGATION.
   */
  public TSRangeParams alignStart() {
    return align(MINUS);
  }

  /**
   * This requires AGGREGATION.
   */
  public TSRangeParams alignEnd() {
    return align(PLUS);
  }

  public TSRangeParams aggregation(AggregationType aggregationType, long bucketDuration) {
    this.aggregators = new AggregationType[] { aggregationType };
    this.bucketDuration = bucketDuration;
    return this;
  }

  /**
   * Specifies multiple aggregators to be applied in a single {@code TS.RANGE} / {@code TS.REVRANGE}
   * call. Aggregators are sent on the wire in the given order and the response values appear in the
   * same order in {@link TSElement#getValues()}. Single-element arrays are accepted and behave like
   * {@link #aggregation(AggregationType, long)}.
   * @param aggregators ordered, non-empty list of aggregators
   * @param bucketDuration aggregation bucket duration in milliseconds
   * @return this
   * @throws IllegalArgumentException if {@code aggregators} is {@code null} or empty
   */
  public TSRangeParams aggregation(AggregationType[] aggregators, long bucketDuration) {
    if (aggregators == null || aggregators.length == 0) {
      throw new IllegalArgumentException("aggregators must be non-null and non-empty");
    }
    this.aggregators = aggregators;
    this.bucketDuration = bucketDuration;
    return this;
  }

  /**
   * This requires AGGREGATION.
   */
  public TSRangeParams bucketTimestamp(String bucketTimestamp) {
    this.bucketTimestamp = encode(bucketTimestamp);
    return this;
  }

  /**
   * This requires AGGREGATION.
   */
  public TSRangeParams bucketTimestampLow() {
    this.bucketTimestamp = MINUS;
    return this;
  }

  /**
   * This requires AGGREGATION.
   */
  public TSRangeParams bucketTimestampHigh() {
    this.bucketTimestamp = PLUS;
    return this;
  }

  /**
   * This requires AGGREGATION.
   */
  public TSRangeParams bucketTimestampMid() {
    this.bucketTimestamp = BYTES_TILDE;
    return this;
  }

  /**
   * This requires AGGREGATION.
   */
  public TSRangeParams empty() {
    this.empty = true;
    return this;
  }

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

      args.add(AGGREGATION).add(AggregationType.joinRaw(aggregators))
          .add(toByteArray(bucketDuration));

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

    TSRangeParams that = (TSRangeParams) o;
    return latest == that.latest && bucketDuration == that.bucketDuration && empty == that.empty &&
        Objects.equals(fromTimestamp, that.fromTimestamp) &&
        Objects.equals(toTimestamp, that.toTimestamp) &&
        Arrays.equals(filterByTimestamps, that.filterByTimestamps) &&
        Arrays.equals(filterByValues, that.filterByValues) &&
        Objects.equals(count, that.count) && Arrays.equals(align, that.align) &&
        Arrays.equals(aggregators, that.aggregators) &&
        Arrays.equals(bucketTimestamp, that.bucketTimestamp);
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
    result = 31 * result + Arrays.hashCode(aggregators);
    result = 31 * result + Long.hashCode(bucketDuration);
    result = 31 * result + Arrays.hashCode(bucketTimestamp);
    result = 31 * result + Boolean.hashCode(empty);
    return result;
  }
}
