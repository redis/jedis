package redis.clients.jedis.timeseries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single sample of a time series.
 * <p>
 * A {@code TSElement} carries a timestamp and one or more values. Most queries return one
 * value per sample, in which case {@link #getValue()} returns it directly. Queries that
 * request multiple aggregators (see
 * {@link TSRangeParams#aggregation(AggregationType[], long)}) return one value per
 * aggregator per bucket; in that case the values are accessible in declaration order via
 * {@link #getValues()} and {@link #getValue()} returns the first value for backward
 * compatibility.
 */
public class TSElement {

  private final long timestamp;
  private final List<Double> values;

  public TSElement(long timestamp, double value) {
    this.timestamp = timestamp;
    this.values = new ArrayList<>();
    this.values.add(value);
  }

  TSElement(long timestamp, List<Double> values) {
    this.timestamp = timestamp;
    this.values = values;
  }

  public long getTimestamp() {
    return timestamp;
  }

  /**
   * @return the first value of this sample. Equivalent to {@code getValues().get(0)}.
   */
  public double getValue() {
    return values.get(0);
  }

  /**
   * @return all values of this sample, in the order in which they were returned by the
   *         server (matching the order of aggregators when the query was issued with
   *         multiple aggregators)
   */
  public List<Double> getValues() {
    return values;
  }

  @Override
  public int hashCode() {
    return 31 * Long.hashCode(timestamp) + values.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (obj == this) return true;
    if (!(obj instanceof TSElement)) return false;

    TSElement other = (TSElement) obj;
    return this.timestamp == other.timestamp && this.values.equals(other.values);
  }

  @Override
  public String toString() {
    return "(" + timestamp + ":" + (values.size()== 1 ?values.get(0) : values) + ")";
  }
}
