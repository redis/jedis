package redis.clients.jedis;

import java.util.Map;

public class StreamConsumersInfo {

  public final static String NAME = "name";
  public final static String IDLE = "idle";
  public final static String PENDING = "pending";
  public static final String CONSUMERS_INFO = "consumers";


  private final String name;
  private final long idle;
  private final long pending;

  public StreamConsumersInfo(Map<String,Object> map) {

    if (map!= null && map.size()>0) {
      name = (String) map.get(NAME);
      idle = (long) map.get(IDLE);
      pending = (long) map.get(PENDING);


    } else throw new IllegalArgumentException();

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

  public static class StreamConsumersInfoType {

    private static final StreamConsumersInfoType streamConsumersInfoType = new StreamConsumersInfoType();
    private StreamConsumersInfoType() {
      //Should not be used
    };

    public static StreamConsumersInfoType getStreamGroupInfoType() {
      return streamConsumersInfoType;
    }
  }
}
