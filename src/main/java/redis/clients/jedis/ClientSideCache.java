package redis.clients.jedis;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.SafeEncoder;

public class ClientSideCache {

  private final Map<ByteBuffer, Object> cache = new HashMap<>();

  protected ClientSideCache() {
  }

  protected void invalidateKeys(List list) {
    if (list == null) {
      cache.clear();
      return;
    }

    list.forEach(this::invalidateKey);
  }

  private void invalidateKey(Object key) {
    if (key instanceof byte[]) {
      cache.remove(convertKey((byte[]) key));
    } else {
      throw new JedisException("" + key.getClass().getSimpleName() + " is not supported. Value: " + String.valueOf(key));
    }
  }

  protected void setKey(Object key, Object value) {
    cache.put(getMapKey(key), value);
  }

  protected <T> T getValue(Object key) {
    return (T) getMapValue(key);
  }

  private Object getMapValue(Object key) {
    return cache.get(getMapKey(key));
  }

  private ByteBuffer getMapKey(Object key) {
    if (key instanceof byte[]) {
      return convertKey((byte[]) key);
    } else {
      return convertKey(SafeEncoder.encode(String.valueOf(key)));
    }
  }

  private static ByteBuffer convertKey(byte[] b) {
    return ByteBuffer.wrap(b);
  }
}
