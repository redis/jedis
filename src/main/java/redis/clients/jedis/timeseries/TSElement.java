package redis.clients.jedis.timeseries;

import java.util.Collections;
import java.util.List;

/**
 * A single sample of a time series.
 * <p>1
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
  private final double value;

  public TSElement(long timestamp, double value) {
    this.timestamp = timestamp;
    this.value = value;
  }

  public long getTimestamp() {
    return timestamp;
  }

  /**
   * @return the first value of this sample. Equivalent to {@code getValues().get(0)}.
   */
  public double getValue() {
    return value;
  }

  /**
   * @return all values of this sample, in the order in which they were returned by the
   *         server (matching the order of aggregators when the query was issued with
   *         multiple aggregators)
   */
  public List<Double> getValues() {
    return Collections.singletonList(value);
  }

  @Override
  public int hashCode() {
    // Matches Collections.singletonList(value).hashCode() (= 31 + Double.hashCode(value))
    // so a TSElement and a MultiValueTSElement holding the same single value hash alike.
    return 31 * Long.hashCode(timestamp) + 31 + Double.hashCode(value);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (!(obj instanceof TSElement)) return false;
    TSElement other = (TSElement) obj;
    if (this.timestamp != other.timestamp) return false;
    if (this.getClass() == TSElement.class && other.getClass() == TSElement.class) {
      return Double.doubleToLongBits(this.value) == Double.doubleToLongBits(other.value);
    }
    return this.getValues().equals(other.getValues());
  }

  @Override
  public String toString() {
    return "(" + timestamp + ":" + value + ")";
  }

  /**
   * Variant produced by {@link TimeSeriesBuilderFactory} when a query returned more than
   * one value per sample (multiple aggregators). Holds the parser's list as-is and is
   * never instantiated for single-value samples, so callers can assume
   * {@code values.size() >= 2}.
   */
  static final class MultiValueTSElement extends TSElement {

    private final List<Double> values;

    MultiValueTSElement(long timestamp, List<Double> values) {
      super(timestamp, values.get(0));
      this.values = values;
    }

    @Override
    public List<Double> getValues() {
      return values;
    }

    @Override
    public int hashCode() {
      return 31 * Long.hashCode(getTimestamp()) + values.hashCode();
    }

    @Override
    public String toString() {
      return "(" + getTimestamp() + ":" + values + ")";
    }
  }
}