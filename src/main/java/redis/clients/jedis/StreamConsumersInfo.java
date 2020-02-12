package redis.clients.jedis;

import java.util.Map;

/**
 * This class holds information about a consumer
 * They can be access via getters.
 * For future purpose there is also {@link #getConsumerInfo()} ()}  method
 * that returns a generic {@code Map} - in case where more info is returned from a server
 *
 */
public class StreamConsumersInfo {

  public final static String NAME = "name";
  public final static String IDLE = "idle";
  public final static String PENDING = "pending";

  private final String name;
  private final long idle;
  private final long pending;
  private final Map<String,Object> consumerInfo;

  /**
   * @param map contains key-value pairs with consumer info
   *
   */
  public StreamConsumersInfo(Map<String,Object> map) {

    consumerInfo = map;
    name = (String) map.get(NAME);
    idle = (long) map.get(IDLE);
    pending = (long) map.get(PENDING);

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
   * @return Generic map containing all key-value pairs returned by the server
   */
  public Map<String,Object> getConsumerInfo() {
    return consumerInfo;
  }

}
