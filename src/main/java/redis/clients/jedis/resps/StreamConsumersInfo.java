package redis.clients.jedis.resps;

import java.util.Map;

/**
 * This class holds information about a consumer. They can be accessed via getters. There is also
 * {@link StreamConsumersInfo#getConsumerInfo()}} method that returns a generic {@code Map} in case
 * more info are returned from the server.
 * @deprecated Use {@link StreamConsumerInfo}.
 */
// TODO: rename to StreamConsumerInfo ?
@Deprecated
public class StreamConsumersInfo extends StreamConsumerInfo {

  /**
   * @param map contains key-value pairs with consumer info
   */
  public StreamConsumersInfo(Map<String, Object> map) {
    super(map);
  }
}
