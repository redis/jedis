package redis.clients.jedis.params;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import redis.clients.util.SafeEncoder;

public abstract class Params {

  private Map<String, Object> params;

  @SuppressWarnings("unchecked")
  public <T> T getParam(String name) {
    if (params == null) return null;

    return (T) params.get(name);
  }

  public byte[][] getByteParams() {
    ArrayList<byte[]> byteParams = new ArrayList<byte[]>();

    for (Entry<String, Object> param : params.entrySet()) {
      byteParams.add(SafeEncoder.encode(param.getKey()));
      if (param.getValue() != null) {
        byteParams.add(SafeEncoder.encode(String.valueOf(param.getValue())));
      }
    }

    return byteParams.toArray(new byte[byteParams.size()][]);
  }

  public boolean contains(String name) {
    if (params == null) return false;

    return params.containsKey(name);
  }

  protected void addParam(String name, Object value) {
    if (params == null) {
      params = new HashMap<String, Object>();
    }
    params.put(name, value);
  }

  protected void addParam(String name) {
    if (params == null) {
      params = new HashMap<String, Object>();
    }
    params.put(name, null);
  }

}