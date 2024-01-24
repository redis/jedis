package redis.clients.jedis.util;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import redis.clients.jedis.ClientSideCache;

public class MapCSC extends ClientSideCache {

  private final Map<ByteBuffer, Object> cache;

  public MapCSC() {
    this(new HashMap<>());
  }

  public MapCSC(Map<ByteBuffer, Object> map) {
    this.cache = map;
  }

  @Override
  public final void clear() {
    cache.clear();
  }

  @Override
  protected void remove(ByteBuffer key) {
    cache.remove(key);
  }

  @Override
  protected void put(ByteBuffer key, Object value) {
    cache.put(key, value);
  }

  @Override
  protected Object get(ByteBuffer key) {
    return cache.get(key);
  }
}
