package redis.clients.jedis.timeseries;

import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.*;

import java.util.Map;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;

public class TSCreateParams implements IParams {

  private Long retentionTime;
  private boolean uncompressed;
  private boolean compressed;
  private Long chunkSize;
  private DuplicatePolicy duplicatePolicy;
  private Map<String, String> labels;

  public TSCreateParams() {
  }

  public static TSCreateParams createParams() {
    return new TSCreateParams();
  }

  public TSCreateParams retention(long retentionTime) {
    this.retentionTime = retentionTime;
    return this;
  }

  public TSCreateParams uncompressed() {
    this.uncompressed = true;
    return this;
  }

  public TSCreateParams compressed() {
    this.compressed = true;
    return this;
  }

  public TSCreateParams chunkSize(long chunkSize) {
    this.chunkSize = chunkSize;
    return this;
  }

  public TSCreateParams duplicatePolicy(DuplicatePolicy duplicatePolicy) {
    this.duplicatePolicy = duplicatePolicy;
    return this;
  }

  public TSCreateParams labels(Map<String, String> labels) {
    this.labels = labels;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {

    if (retentionTime != null) {
      args.add(RETENTION).add(toByteArray(retentionTime));
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
