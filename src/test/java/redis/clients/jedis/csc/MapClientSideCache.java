package redis.clients.jedis.csc;

import java.util.HashMap;
import java.util.Map;

public class MapClientSideCache extends ClientSideCache {

  private final Map<CacheKey, CacheEntry> cache;

  public MapClientSideCache() {
    this(new HashMap<>());
  }

  public MapClientSideCache(Map<CacheKey, CacheEntry> map) {
    super();
    this.cache = map;
  }

  @Override
  protected final void clear() {
    cache.clear();
  }

  @Override
  protected void remove(Iterable<CacheKey<?>> keys) {
    keys.forEach(hash -> cache.remove(hash));
  }

  @Override
  protected void put(CacheKey key, CacheEntry entry) {
    cache.put(key, entry);
  }

  @Override
  protected CacheEntry get(CacheKey key) {
    return cache.get(key);
  }
}
