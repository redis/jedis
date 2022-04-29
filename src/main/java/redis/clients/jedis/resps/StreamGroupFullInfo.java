package redis.clients.jedis.resps;

import redis.clients.jedis.StreamEntryID;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * This class holds information about a stream group with command <code>xinfo stream mystream full<code/>.
 * They can be access via getters. For future purpose there is also {@link #getGroupFullInfo()} method
 * that returns a generic {@code Map} - in case where more info is returned from the server.
 */
public class StreamGroupFullInfo implements Serializable {

  public static final String NAME = "name";
  public static final String CONSUMERS = "consumers";
  public static final String PENDING = "pending";
  public static final String LAST_DELIVERED = "last-delivered-id";
  public static final String PEL_COUNT = "pel-count";

  private final String name;
  private final List<StreamConsumerFullInfo> consumers;
  private final List<List<Object>> pending;
  private final Long pelCount;
  private final StreamEntryID lastDeliveredId;
  private final Map<String, Object> groupFullInfo;

  /**
   * @param map contains key-value pairs with group info
   */
  @SuppressWarnings("unchecked")
  public StreamGroupFullInfo(Map<String, Object> map) {

    groupFullInfo = map;
    name = (String) map.get(NAME);
    consumers = (List<StreamConsumerFullInfo>) map.get(CONSUMERS);
    pending = (List<List<Object>>) map.get(PENDING);
    lastDeliveredId = (StreamEntryID) map.get(LAST_DELIVERED);
    pelCount = (Long) map.get(PEL_COUNT);

    pending.stream().forEach(entry -> entry.set(0, new StreamEntryID((String) entry.get(0))));
  }

  public String getName() {
    return name;
  }

  public List<StreamConsumerFullInfo> getConsumers() {
    return consumers;
  }

  public List<List<Object>> getPending() {
    return pending;
  }

  public StreamEntryID getLastDeliveredId() {
    return lastDeliveredId;
  }

  /**
   * @return Generic map containing all key-value pairs returned by the server
   */
  public Map<String, Object> getGroupFullInfo() {
    return groupFullInfo;
  }

  public Long getPelCount() {
    return pelCount;
  }

}