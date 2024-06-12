package redis.clients.jedis.timeseries;

import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.*;

import java.util.LinkedHashMap;
import java.util.Map;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;

/**
 * Represents optional arguments of TS.INCRBY or TS.DECRBY commands.
 */
public class TSIncrByDecrByParams implements IParams {

  private Long timestamp;
  private Long retentionPeriod;
  private boolean uncompressed;
  private boolean compressed;
  private Long chunkSize;
  private DuplicatePolicy duplicatePolicy;
  private Map<String, String> labels;

  public TSIncrByDecrByParams() {
  }

  public static TSIncrByDecrByParams params() {
    return new TSIncrByDecrByParams();
  }

  public TSIncrByDecrByParams timestamp(long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public TSIncrByDecrByParams retention(long retentionPeriod) {
    this.retentionPeriod = retentionPeriod;
    return this;
  }

  /**
   * ENCODING UNCOMPRESSED
   * @return this
   */
  public TSIncrByDecrByParams uncompressed() {
    this.uncompressed = true;
    this.compressed = false;
    return this;
  }

  /**
   * ENCODING COMPRESSED
   * @return this
   */
  public TSIncrByDecrByParams compressed() {
    this.compressed = true;
    this.uncompressed = false;
    return this;
  }

  public TSIncrByDecrByParams chunkSize(long chunkSize) {
    this.chunkSize = chunkSize;
    return this;
  }

  public TSIncrByDecrByParams duplicatePolicy(DuplicatePolicy duplicatePolicy) {
    this.duplicatePolicy = duplicatePolicy;
    return this;
  }

  /**
   * Set label-value pairs
   *
   * @param labels label-value pairs
   * @return the object itself
   */
  public TSIncrByDecrByParams labels(Map<String, String> labels) {
    this.labels = labels;
    return this;
  }

  /**
   * Add label-value pair. Multiple pairs can be added through chaining.
   */
  public TSIncrByDecrByParams label(String label, String value) {
    if (this.labels == null) {
      this.labels = new LinkedHashMap<>();
    }
    this.labels.put(label, value);
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {

    if (timestamp != null) {
      args.add(TIMESTAMP).add(timestamp);
    }

    if (retentionPeriod != null) {
      args.add(RETENTION).add(toByteArray(retentionPeriod));
    }

    if (uncompressed) {
      args.add(ENCODING).add(UNCOMPRESSED);
    } else if (compressed) {
      args.add(ENCODING).add(COMPRESSED);
    }

    if (chunkSize != null) {
      args.add(CHUNK_SIZE).add(toByteArray(chunkSize));
    }

    if (duplicatePolicy != null) {
      args.add(DUPLICATE_POLICY).add(duplicatePolicy);
    }

    if (labels != null) {
      args.add(LABELS);
      labels.entrySet().forEach((entry) -> args.add(entry.getKey()).add(entry.getValue()));
    }
  }
}
