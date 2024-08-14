package redis.clients.jedis.csc;

import java.util.HashMap;
import java.util.Map;

public class TestCache extends DefaultCache {

  public TestCache() {
    this(new HashMap<CacheKey, CacheEntry>());
  }

  public TestCache(Map<CacheKey, CacheEntry> map) {
    super(10000, map);
  }

  public TestCache(Map<CacheKey, CacheEntry> map, Cacheable cacheable) {

    super(10000, map, cacheable, new LRUEviction(10000));
  }

  public TestCache(int maxSize, Map<CacheKey, CacheEntry> map, Cacheable cacheable) {
    this(maxSize, map, cacheable, new LRUEviction(maxSize));
  }

  public TestCache(int maxSize, Map<CacheKey, CacheEntry> map, Cacheable cacheable,
      EvictionPolicy evictionPolicy) {
    super(maxSize, map, cacheable, evictionPolicy);
  }

}
