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

  public TestCache(Map<CacheKey, CacheEntry> map, ClientSideCacheable cacheable) {

    super(10000, map, cacheable, new LRUEviction(10000));
  }

  public TestCache(int maximumSize, Map<CacheKey, CacheEntry> map, ClientSideCacheable cacheable) {
    this(maximumSize, map, cacheable, new LRUEviction(maximumSize));
  }

  public TestCache(int maximumSize, Map<CacheKey, CacheEntry> map, ClientSideCacheable cacheable,
      EvictionPolicy evictionPolicy) {
    super(maximumSize, map, cacheable, evictionPolicy);
  }

}
