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
 * Represents optional arguments of TS.MRANGE and TS.MREVRANGE commands.
 */
public class TSMRangeParams implements IParams {

  private Long fromTimestamp;
  private Long toTimestamp;

  private boolean latest;

  private long[] filterByTimestamps;
  private double[] filterByValues;

  private boolean withLabels;
  private String[] selectedLabels;

  private Integer count;

  private byte[] align;

  private AggregationType aggregationType;
  private long bucketDuration;
  private byte[] bucketTimestamp;

  private boolean empty;

  private String[] filters;

  private String groupByLabel;
  private String groupByReduce;

  public TSMRangeParams(long fromTimestamp, long toTimestamp) {
    this.fromTimestamp = fromTimestamp;
    this.toTimestamp = toTimestamp;
  }

  public static TSMRangeParams multiRangeParams(long fromTimestamp, long toTimestamp) {
    return new TSMRangeParams(fromTimestamp, toTimestamp);
  }

  public TSMRangeParams() {
  }

  public static TSMRangeParams multiRangeParams() {
    return new TSMRangeParams();
  }

  public TSMRangeParams fromTimestamp(long fromTimestamp) {
    this.fromTimestamp = fromTimestamp;
    return this;
  }

  public TSMRangeParams toTimestamp(long toTimestamp) {
    this.toTimestamp = toTimestamp;
    return this;
  }

  public TSMRangeParams latest() {
    this.latest = true;
    return this;
  }

  public TSMRangeParams filterByTS(long... timestamps) {
    this.filterByTimestamps = timestamps;
    return this;
  }

  public TSMRangeParams filterByValues(double min, double max) {
    this.filterByValues = new double[] {min, max};
    return this;
  }

  public TSMRangeParams withLabels(boolean withLabels) {
    this.withLabels = withLabels;
    return this;
  }

  public TSMRangeParams withLabels() {
    return withLabels(true);
  }

  public TSMRangeParams selectedLabels(String... labels) {
    this.selectedLabels = labels;
    return this;
  }

  public TSMRangeParams count(int count) {
    this.count = count;
    return this;
  }

  private TSMRangeParams align(byte[] raw) {
    this.align = raw;
    return this;
  }

  /**
   * This requires AGGREGATION.
   */
  public TSMRangeParams align(long timestamp) {
    return align(toByteArray(timestamp));
  }

  /**
   * This requires AGGREGATION.
   */
  public TSMRangeParams alignStart() {
    return align(MINUS);
  }

  /**
   * This requires AGGREGATION.
   */
  public TSMRangeParams alignEnd() {
    return align(PLUS);
  }

  public TSMRangeParams aggregation(AggregationType aggregationType, long bucketDuration) {
    this.aggregationType = aggregationType;
    this.bucketDuration = bucketDuration;
    return this;
  }

  /**
   * This requires AGGREGATION.
   */
  public TSMRangeParams bucketTimestamp(String bucketTimestamp) {
    this.bucketTimestamp = encode(bucketTimestamp);
    return this;
  }

  /**
   * This requires AGGREGATION.
   */
  public TSMRangeParams bucketTimestampLow() {
    this.bucketTimestamp = MINUS;
    return this;
  }

  /**
   * This requires AGGREGATION.
   */
  public TSMRangeParams bucketTimestampHigh() {
    this.bucketTimestamp = PLUS;
    return this;
  }

  /**
   * This requires AGGREGATION.
   */
  public TSMRangeParams bucketTimestampMid() {
    this.bucketTimestamp = BYTES_TILDE;
    return this;
  }

  /**
   * This requires AGGREGATION.
   */
  public TSMRangeParams empty() {
    this.empty = true;
    return this;
  }

  public TSMRangeParams filter(String... filters) {
    this.filters = filters;
    return this;
  }

  public TSMRangeParams groupBy(String label, String reduce) {
    this.groupByLabel = label;
    this.groupByReduce = reduce;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {

    if (filters == null) {
      throw new IllegalArgumentException("FILTER arguments must be set.");
    }

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

    if (withLabels) {
      args.add(WITHLABELS);
    } else if (selectedLabels != null) {
      args.add(SELECTED_LABELS);
      for (String label : selectedLabels) {
        args.add(label);
      }
    }

    if (count != null) {
      args.add(COUNT).add(toByteArray(count));
    }

    if (aggregationType != null) {

      if (align != null) {
        args.add(ALIGN).add(align);
      }

      args.add(AGGREGATION).add(aggregationType).add(toByteArray(bucketDuration));

      if (bucketTimestamp != null) {
        args.add(BUCKETTIMESTAMP).add(bucketTimestamp);
      }

      if (empty) {
        args.add(EMPTY);
      }
    }

    args.add(FILTER);
    for (String filter : filters) {
      args.add(filter);
    }

    if (groupByLabel != null && groupByReduce != null) {
      args.add(GROUPBY).add(groupByLabel).add(REDUCE).add(groupByReduce);
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

    TSMRangeParams that = (TSMRangeParams) o;
    return latest == that.latest && withLabels == that.withLabels &&
        bucketDuration == that.bucketDuration && empty == that.empty &&
        Objects.equals(fromTimestamp, that.fromTimestamp) &&
        Objects.equals(toTimestamp, that.toTimestamp) &&
        Arrays.equals(filterByTimestamps, that.filterByTimestamps) &&
        Arrays.equals(filterByValues, that.filterByValues) &&
        Arrays.equals(selectedLabels, that.selectedLabels) &&
        Objects.equals(count, that.count) && Arrays.equals(align, that.align) &&
        aggregationType == that.aggregationType &&
        Arrays.equals(bucketTimestamp, that.bucketTimestamp) &&
        Arrays.equals(filters, that.filters) &&
        Objects.equals(groupByLabel, that.groupByLabel) &&
        Objects.equals(groupByReduce, that.groupByReduce);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(fromTimestamp);
    result = 31 * result + Objects.hashCode(toTimestamp);
    result = 31 * result + Boolean.hashCode(latest);
    result = 31 * result + Arrays.hashCode(filterByTimestamps);
    result = 31 * result + Arrays.hashCode(filterByValues);
    result = 31 * result + Boolean.hashCode(withLabels);
    result = 31 * result + Arrays.hashCode(selectedLabels);
    result = 31 * result + Objects.hashCode(count);
    result = 31 * result + Arrays.hashCode(align);
    result = 31 * result + Objects.hashCode(aggregationType);
    result = 31 * result + Long.hashCode(bucketDuration);
    result = 31 * result + Arrays.hashCode(bucketTimestamp);
    result = 31 * result + Boolean.hashCode(empty);
    result = 31 * result + Arrays.hashCode(filters);
    result = 31 * result + Objects.hashCode(groupByLabel);
    result = 31 * result + Objects.hashCode(groupByReduce);
    return result;
  }
}
