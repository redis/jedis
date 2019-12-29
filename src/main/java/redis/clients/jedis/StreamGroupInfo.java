package redis.clients.jedis;

import java.io.Serializable;
import java.util.Map;

public class StreamGroupInfo implements Serializable {

  public final static String NAME = "name";
  public final static String CONSUMERS = "consumers";
  public final static String PENDING = "pending";
  public final static String LAST_DELIVERED = "last-delivered-id";
  public static final String GROUP_INFO = "groups";


  private final String name;
  private final long consumers;
  private final long pending;
  private final String lastDeliveredId;


  public StreamGroupInfo(Map<String, Object> map) {
    if (map!= null && map.size()>0) {
      name = (String) map.get(NAME);
      consumers = (long) map.get(CONSUMERS);
      pending = (long) map.get(PENDING);
      lastDeliveredId = (String) map.get(LAST_DELIVERED);


    } else throw new IllegalArgumentException();
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

  public String getLastDeliveredId() {
    return lastDeliveredId;
  }

  public static class StreamGroupInfoType {

    private static final StreamGroupInfoType streamInfoType = new StreamGroupInfoType();
    private StreamGroupInfoType() {
      //Should not be used
    };

    public static StreamGroupInfoType getStreamGroupInfoType() {
      return streamInfoType;
    }
  }
}
