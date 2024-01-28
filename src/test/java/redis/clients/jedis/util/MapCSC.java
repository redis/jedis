package redis.clients.jedis.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import redis.clients.jedis.ClientSideCache;

public class MapCSC extends ClientSideCache {

  private final Map<Long, Object> cache;

  public MapCSC() {
    this(new ConcurrentHashMap<>());
  }

  public MapCSC(Map<Long, Object> map) {
    this.cache = map;
  }

  @Override
  public final void invalidateAll() {
    cache.clear();
  }

  @Override
  protected void invalidateAll(Iterable<Long> hashes) {
    hashes.forEach(hash -> cache.remove(hash));
  }

  @Override
  protected void put(long hash, Object value) {
    cache.put(hash, value);
  }

  @Override
  protected Object get(long hash) {
    return cache.get(hash);
  }
}
