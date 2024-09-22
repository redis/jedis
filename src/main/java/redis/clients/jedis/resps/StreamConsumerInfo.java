package redis.clients.jedis.resps;

import java.util.Map;

/**
 * This class holds information about a consumer. They can be accessed via getters. There is also
 * {@link StreamConsumersInfo#getConsumerInfo()}} method that returns a generic {@code Map} in case
 * more info are returned from the server.
 */
public class StreamConsumerInfo {

  public static final String NAME = "name";
  public static final String IDLE = "idle";
  public static final String PENDING = "pending";
  public static final String INACTIVE = "inactive";

  private final String name;
  private final long idle;
  private final long pending;
  private final Long inactive;
  private final Map<String, Object> consumerInfo;

  /**
   * @param map contains key-value pairs with consumer info
   */
  public StreamConsumerInfo(Map<String, Object> map) {
    consumerInfo = map;
    name = (String) map.get(NAME);
    idle = (Long) map.get(IDLE);
    pending = (Long) map.get(PENDING);
    inactive = (Long) map.get(INACTIVE);
  }

  public String getName() {
    return name;
  }

  public long getIdle() {
    return idle;
  }

  public long getPending() {
    return pending;
  }

  /**
   * Since Redis 7.2.
   */
  public Long getInactive() {
    return inactive;
  }

  /**
   * All data.
   * @return Generic map containing all key-value pairs returned by the server
   */
  public Map<String, Object> getConsumerInfo() {
    return consumerInfo;
  }
}
