package redis.clients.jedis.search;

import java.util.HashMap;
import java.util.Map;
import redis.clients.jedis.util.SafeEncoder;

public class RediSearchUtil {

  public static Map<String, String> toStringMap(Map<String, Object> input) {
    Map<String, String> output = new HashMap<>(input.size());
    for (Map.Entry<String, Object> entry : input.entrySet()) {
      String key = entry.getKey();
      Object obj = entry.getValue();
      if (key == null || obj == null) {
        throw new NullPointerException("A null argument cannot be sent to Redis.");
      }
      String str;
      if (obj instanceof byte[]) {
        str = SafeEncoder.encode((byte[]) obj);
      } else if (obj instanceof redis.clients.jedis.GeoCoordinate) {
        redis.clients.jedis.GeoCoordinate geo = (redis.clients.jedis.GeoCoordinate) obj;
        str = geo.getLongitude() + "," + geo.getLatitude();
      } else if (obj instanceof String) {
        str = (String) obj;
      } else {
        str = obj.toString();
      }
      output.put(key, str);
    }
    return output;
  }

  private RediSearchUtil() {
    throw new InstantiationError("Must not instantiate this class");
  }
}
