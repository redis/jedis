package redis.clients.jedis.gears.resps;

import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;

import java.util.List;
import java.util.stream.Collectors;

@Deprecated
public class FunctionStreamInfo {
  private final String name;
  private final String idToReadFrom;
  private final String lastError;
  private final long lastLag;
  private final long lastProcessedTime;
  private final long totalLag;
  private final long totalProcessedTime;
  private final long totalRecordProcessed;
  private final List<String> pendingIds;

  public String getName() {
    return name;
  }

  public String getIdToReadFrom() {
    return idToReadFrom;
  }

  public String getLastError() {
    return lastError;
  }

  public long getLastLag() {
    return lastLag;
  }

  public long getLastProcessedTime() {
    return lastProcessedTime;
  }

  public long getTotalLag() {
    return totalLag;
  }

  public long getTotalProcessedTime() {
    return totalProcessedTime;
  }

  public long getTotalRecordProcessed() {
    return totalRecordProcessed;
  }

  public List<String> getPendingIds() {
    return pendingIds;
  }

  public FunctionStreamInfo(String name, String idToReadFrom, String lastError,
    long lastProcessedTime, long lastLag, long totalLag, long totalProcessedTime, long totalRecordProcessed,
    List<String> pendingIds) {
    this.name = name;
    this.idToReadFrom = idToReadFrom;
    this.lastError = lastError;
    this.lastProcessedTime = lastProcessedTime;
    this.lastLag = lastLag;
    this.totalLag = totalLag;
    this.totalProcessedTime = totalProcessedTime;
    this.totalRecordProcessed = totalRecordProcessed;
    this.pendingIds = pendingIds;
  }

  @Deprecated
  public static final Builder<List<FunctionStreamInfo>> STREAM_INFO_LIST = new Builder<List<FunctionStreamInfo>>() {
    @Override
    public List<FunctionStreamInfo> build(Object data) {
      return ((List<Object>) data).stream().map((pairObject) -> (List<Object>) pairObject)
        .map((pairList) -> new FunctionStreamInfo(
          BuilderFactory.STRING.build(pairList.get(9)),       // name
          BuilderFactory.STRING.build(pairList.get(1)),       // id_to_read_from
          BuilderFactory.STRING.build(pairList.get(3)),       // last_error
          BuilderFactory.LONG.build(pairList.get(7)),         // last_processed_time
          BuilderFactory.LONG.build(pairList.get(5)),         // last_lag
          BuilderFactory.LONG.build(pairList.get(13)),        // total_lag
          BuilderFactory.LONG.build(pairList.get(15)),        // total_processed_time
          BuilderFactory.LONG.build(pairList.get(17)),        // total_record_processed
          BuilderFactory.STRING_LIST.build(pairList.get(11))  // pending_ids
        ))//
        .collect(Collectors.toList());
    }
  };
}
