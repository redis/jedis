package redis.clients.jedis.timeseries;

import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.LABELS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.RETENTION;

import java.util.Collections;
import java.util.Map;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.IParams;

public class TSAlterParams implements IParams {

  private Long retentionTime;
  private Map<String, String> labels;

  public TSAlterParams() {
  }

  public static TSAlterParams alterParams() {
    return new TSAlterParams();
  }

  public TSAlterParams retentionTime(long retentionTime) {
    this.retentionTime = retentionTime;
    return this;
  }

  public TSAlterParams labels(Map<String, String> labels) {
    this.labels = labels;
    return this;
  }

  public TSAlterParams labelsReset() {
    return this.labels(Collections.emptyMap());
  }

  @Override
  public void addParams(CommandArguments args) {

    if (retentionTime == null && labels == null) {
      throw new JedisException("Either RETENTION or LABELS must be set.");
    }

    if (retentionTime != null) {
      args.add(RETENTION).add(toByteArray(retentionTime));
    }

    if (labels != null) {
      args.add(LABELS);
      labels.entrySet().forEach((entry) -> args.add(entry.getKey()).add(entry.getValue()));
    }
  }
}
