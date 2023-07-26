package redis.clients.jedis.resps;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import redis.clients.jedis.StreamEntryID;

/**
 * This class holds information about a stream consumer with command
 * {@code xinfo stream mystream full}. They can be accessed via getters. There is also
 * {@link StreamConsumerFullInfo#getConsumerInfo()} method that returns a generic {@link Map} in
 * case more info are returned from the server.
 */
public class StreamConsumerFullInfo implements Serializable {

  public static final String NAME = "name";
  public static final String SEEN_TIME = "seen-time";
  public static final String ACTIVE_TIME = "active-time";
  public static final String PEL_COUNT = "pel-count";
  public static final String PENDING = "pending";

  private final String name;
  private final Long seenTime;
  private final Long activeTime; // since Redis 7.2
  private final Long pelCount;
  private final List<List<Object>> pending;
  private final Map<String, Object> consumerInfo;

  @SuppressWarnings("unchecked")
  public StreamConsumerFullInfo(Map<String, Object> map) {
    consumerInfo = map;
    name = (String) map.get(NAME);
    seenTime = (Long) map.get(SEEN_TIME);
    activeTime = (Long) map.get(ACTIVE_TIME);
    pending = (List<List<Object>>) map.get(PENDING);
    pelCount = (Long) map.get(PEL_COUNT);

    pending.forEach(entry -> entry.set(0, new StreamEntryID((String) entry.get(0))));
  }

  public String getName() {
    return name;
  }

  public Long getSeenTime() {
    return seenTime;
  }

  /**
   * Since Redis 7.2.
   */
  public Long getActiveTime() {
    return activeTime;
  }

  public Long getPelCount() {
    return pelCount;
  }

  public List<List<Object>> getPending() {
    return pending;
  }

  /**
   * All data.
   */
  public Map<String, Object> getConsumerInfo() {
    return consumerInfo;
  }
}