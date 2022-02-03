package redis.clients.jedis.timeseries;

import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.MINUS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.PLUS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.AGGREGATION;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.ALIGN;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.COUNT;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.FILTER_BY_TS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.FILTER_BY_VALUE;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;

public class TSRangeParams implements IParams {

  private Long fromTimestamp;
  private Long toTimestamp;

  private long[] filterByTimestamps;
  private double[] filterByValues;

  private Integer count;

  private byte[] align;

  private AggregationType aggregationType;
  private long timeBucket;

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

  public TSRangeParams align(long timestamp) {
    return align(toByteArray(timestamp));
  }

  public TSRangeParams alignStart() {
    return align(MINUS);
  }

  public TSRangeParams alignEnd() {
    return align(PLUS);
  }

  public TSRangeParams aggregation(AggregationType aggregationType, long timeBucket) {
    this.aggregationType = aggregationType;
    this.timeBucket = timeBucket;
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

    if (align != null) {
      args.add(ALIGN).add(align);
    }

    if (aggregationType != null) {
      args.add(AGGREGATION).add(aggregationType).add(toByteArray(timeBucket));
    }
  }
}
