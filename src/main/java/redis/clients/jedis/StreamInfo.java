package redis.clients.jedis;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class StreamInfo implements Serializable {

  private Map<String, Object> infoMap;

  public StreamInfo(Map<String,Object> map) {

    if (map!= null) {
      infoMap = map;
    } else throw  new IllegalArgumentException("InfoMap can not be null");
  }

  public Map<String,Object> getInfoMap() {

    return infoMap;
  }
}
