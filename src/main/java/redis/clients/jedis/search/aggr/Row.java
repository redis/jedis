package redis.clients.jedis.search.aggr;

import java.util.Map;
import redis.clients.jedis.util.DoublePrecision;

public class Row {

  private final Map<String, Object> fields;

  public Row(Map<String, Object> fields) {
    this.fields = fields;
  }

  public boolean containsKey(String key) {
    return fields.containsKey(key);
  }

  public Object get(String key) {
    return fields.get(key);
  }

  public String getString(String key) {
    if (!containsKey(key)) {
      return "";
    }
    return (String) fields.get(key);
  }

  public long getLong(String key) {
    if (!containsKey(key)) {
      return 0;
    }
    return Long.parseLong((String) fields.get(key));
  }

  public double getDouble(String key) {
    if (!containsKey(key)) {
      return 0;
    }
    return DoublePrecision.parseFloatingPointNumber((String) fields.get(key));
  }

  @Override
  public String toString() {
    return String.valueOf(fields);
  }
}
