package redis.clients.jedis.search.aggr;

import java.util.Map;

/**
 * Created by mnunberg on 5/17/18.
 *
 * Row in aggregation result-set
 */
public class Row {

  private final Map<String, Object> fields;

  public Row(Map<String, Object> fields) {
    this.fields = fields;
  }

  public boolean containsKey(String key) {
    return fields.containsKey(key);
  }

  public String getString(String key) {
    if (!containsKey(key)) {
      return "";
    }
    return new String((byte[]) fields.get(key));
  }

  public long getLong(String key) {
    if (!containsKey(key)) {
      return 0;
    }
    return Long.parseLong(getString(key));
  }

  public double getDouble(String key) {
    if (!containsKey(key)) {
      return 0;
    }
    return Double.parseDouble(getString(key));
  }
}
