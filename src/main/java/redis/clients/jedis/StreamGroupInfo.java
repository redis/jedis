package redis.clients.jedis;

import java.io.Serializable;
import java.util.Map;

/**
 * This class holds information about a stream group
 * They can be access via getters.
 * For future purpose there is also {@link #getGroupInfo()}  method
 * that returns a generic {@code Map} - in case where more info is returned from a server
 *
 */
public class StreamGroupInfo implements Serializable {

  public final static String NAME = "name";
  public final static String CONSUMERS = "consumers";
  public final static String PENDING = "pending";
  public final static String LAST_DELIVERED = "last-delivered-id";


  private final String name;
  private final long consumers;
  private final long pending;
  private final StreamEntryID lastDeliveredId;
  private final Map<String,Object> groupInfo;

  /**
   * @param map contains key-value pairs with group info
   *
   */
  public StreamGroupInfo(Map<String, Object> map) {

    groupInfo = map;
    name = (String) map.get(NAME);
    consumers = (long) map.get(CONSUMERS);
    pending = (long) map.get(PENDING);
    lastDeliveredId = (StreamEntryID) map.get(LAST_DELIVERED);

  }

  public String getName() {
    return name;
  }

  public long getConsumers() {
    return consumers;
  }

  public long getPending() {
    return pending;
  }

  public StreamEntryID getLastDeliveredId() {
    return lastDeliveredId;
  }

  /**
   * @return Generic map containing all key-value pairs returned by the server
   */
  public Map<String, Object> getGroupInfo() {
   return groupInfo;
 }

}
