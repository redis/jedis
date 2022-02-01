package redis.clients.jedis.timeseries;

import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.MINUS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.PLUS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.*;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;

public class TSMRangeParams implements IParams {

  private Long fromTimestamp;
  private Long toTimestamp;

  private long[] filterByTimestamps;
  private double[] filterByValues;

  private Integer count;

  private byte[] align;

  private AggregationType aggregationType;
  private long timeBucket;

  private boolean withLabels;
  private String[] selectedLabels;

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

  public TSMRangeParams filterByTS(long... timestamps) {
    this.filterByTimestamps = timestamps;
    return this;
  }

  public TSMRangeParams filterByValues(double min, double max) {
    this.filterByValues = new double[] {min, max};
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

  public TSMRangeParams align(long timestamp) {
    return align(toByteArray(timestamp));
  }

  public TSMRangeParams alignStart() {
    return align(MINUS);
  }

  public TSMRangeParams alignEnd() {
    return align(PLUS);
  }

  public TSMRangeParams aggregation(AggregationType aggregationType, long timeBucket) {
    this.aggregationType = aggregationType;
    this.timeBucket = timeBucket;
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

    if (align != null) {
      args.add(ALIGN).add(align);
    }

    if (aggregationType != null) {
      args.add(AGGREGATION).add(aggregationType).add(toByteArray(timeBucket));
    }

    args.add(FILTER);
    for (String filter : filters) {
      args.add(filter);
    }

    if (groupByLabel != null && groupByReduce != null) {
      args.add(GROUPBY).add(groupByLabel).add(REDUCE).add(groupByReduce);
    }
  }
}
