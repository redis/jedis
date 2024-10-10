package redis.clients.jedis.timeseries;

import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;

/**
 * Represents optional arguments of TS.INCRBY or TS.DECRBY commands.
 */
class TSArithByParams<T extends TSArithByParams<?>> implements IParams {

  private Long timestamp;
  private Long retentionPeriod;
  private EncodingFormat encoding;
  private Long chunkSize;
  private DuplicatePolicy duplicatePolicy;

  private boolean ignore;
  private long ignoreMaxTimediff;
  private double ignoreMaxValDiff;

  private Map<String, String> labels;

  TSArithByParams() {
  }

  public T timestamp(long timestamp) {
    this.timestamp = timestamp;
    return (T) this;
  }

  public T retention(long retentionPeriod) {
    this.retentionPeriod = retentionPeriod;
    return (T) this;
  }

  public T encoding(EncodingFormat encoding) {
    this.encoding = encoding;
    return (T) this;
  }

  public T chunkSize(long chunkSize) {
    this.chunkSize = chunkSize;
    return (T) this;
  }

  public T duplicatePolicy(DuplicatePolicy duplicatePolicy) {
    this.duplicatePolicy = duplicatePolicy;
    return (T) this;
  }

  public T ignore(long maxTimediff, double maxValDiff) {
    this.ignore = true;
    this.ignoreMaxTimediff = maxTimediff;
    this.ignoreMaxValDiff = maxValDiff;
    return (T) this;
  }

  /**
   * Set label-value pairs
   *
   * @param labels label-value pairs
   * @return the object itself
   */
  public T labels(Map<String, String> labels) {
    this.labels = labels;
    return (T) this;
  }

  /**
   * Add label-value pair. Multiple pairs can be added through chaining.
   * @param label
   * @param value
   * @return the object itself
   */
  public T label(String label, String value) {
    if (this.labels == null) {
      this.labels = new LinkedHashMap<>();
    }
    this.labels.put(label, value);
    return (T) this;
  }

  @Override
  public void addParams(CommandArguments args) {

    if (timestamp != null) {
      args.add(TIMESTAMP).add(timestamp);
    }

    if (retentionPeriod != null) {
      args.add(RETENTION).add(toByteArray(retentionPeriod));
    }

    if (encoding != null) {
      args.add(ENCODING).add(encoding);
    }

    if (chunkSize != null) {
      args.add(CHUNK_SIZE).add(toByteArray(chunkSize));
    }

    if (duplicatePolicy != null) {
      args.add(DUPLICATE_POLICY).add(duplicatePolicy);
    }

    if (ignore) {
      args.add(IGNORE).add(ignoreMaxTimediff).add(ignoreMaxValDiff);
    }

    if (labels != null) {
      args.add(LABELS);
      labels.entrySet().forEach((entry) -> args.add(entry.getKey()).add(entry.getValue()));
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

    TSArithByParams<?> that = (TSArithByParams<?>) o;
    return ignore == that.ignore && ignoreMaxTimediff == that.ignoreMaxTimediff &&
        Double.compare(ignoreMaxValDiff, that.ignoreMaxValDiff) == 0 &&
        Objects.equals(timestamp, that.timestamp) &&
        Objects.equals(retentionPeriod, that.retentionPeriod) &&
        encoding == that.encoding && Objects.equals(chunkSize, that.chunkSize) &&
        duplicatePolicy == that.duplicatePolicy && Objects.equals(labels, that.labels);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(timestamp);
    result = 31 * result + Objects.hashCode(retentionPeriod);
    result = 31 * result + Objects.hashCode(encoding);
    result = 31 * result + Objects.hashCode(chunkSize);
    result = 31 * result + Objects.hashCode(duplicatePolicy);
    result = 31 * result + Boolean.hashCode(ignore);
    result = 31 * result + Long.hashCode(ignoreMaxTimediff);
    result = 31 * result + Double.hashCode(ignoreMaxValDiff);
    result = 31 * result + Objects.hashCode(labels);
    return result;
  }
}
