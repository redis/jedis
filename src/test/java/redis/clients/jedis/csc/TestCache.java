package redis.clients.jedis.csc;

import java.util.HashMap;
import java.util.Map;

public class TestCache extends DefaultCache {

  public TestCache() {
    this(new HashMap<CacheKey, CacheEntry>());
  }

  public TestCache(Map<CacheKey, CacheEntry> map) {
    super(10000, 5000, map);
  }

  public TestCache(Map<CacheKey, CacheEntry> map, Cacheable cacheable) {
    super(10000, 5000, map, cacheable, new LRUEviction(10000));
  }

  public TestCache(int maximumSize, EvictionPolicy evictionPolicy ) {
    super(maximumSize, 5000, new HashMap<CacheKey, CacheEntry>(), DefaultCacheable.INSTANCE, evictionPolicy);
  }

  public TestCache(int maximumSize, EvictionPolicy evictionPolicy, Cacheable cacheable ) {
    super(maximumSize, 5000, new HashMap<CacheKey, CacheEntry>(), cacheable, evictionPolicy);
  }

}
