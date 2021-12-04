package redis.clients.jedis.resps;

import java.io.Serializable;
import java.util.Map;
import redis.clients.jedis.StreamEntryID;

public class StreamPendingSummary implements Serializable {

  private static final long serialVersionUID = 1L;

  private final long total;
  private final StreamEntryID minId;
  private final StreamEntryID maxId;
  private final Map<String, Long> consumerMessageCount;

  public StreamPendingSummary(long total, StreamEntryID minId, StreamEntryID maxId,
      Map<String, Long> consumerMessageCount) {
    this.total = total;
    this.minId = minId;
    this.maxId = maxId;
    this.consumerMessageCount = consumerMessageCount;
  }

  public long getTotal() {
    return total;
  }

  public StreamEntryID getMinId() {
    return minId;
  }

  public StreamEntryID getMaxId() {
    return maxId;
  }

  public Map<String, Long> getConsumerMessageCount() {
    return consumerMessageCount;
  }
}
