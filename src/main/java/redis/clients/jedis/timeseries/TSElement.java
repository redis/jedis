package redis.clients.jedis.timeseries;

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

  public double getValue() {
    return value;
  }

  @Override
  public int hashCode() {
    return 31 * Long.hashCode(timestamp) + Long.hashCode(Double.doubleToLongBits(value));
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (obj == this) return true;
    if (!(obj instanceof TSElement)) return false;

    TSElement other = (TSElement) obj;
    return this.timestamp == other.timestamp
        && this.value == other.value;
  }

  @Override
  public String toString() {
    return "(" + timestamp + ":" + value + ")";
  }
}
