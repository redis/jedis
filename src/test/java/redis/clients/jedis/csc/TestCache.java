package redis.clients.jedis.csc;

import java.util.HashMap;
import java.util.Map;

public class TestCache extends DefaultCache {

  public TestCache() {
    this(new HashMap<CacheKey, CacheEntry>());
  }

  public TestCache(Map<CacheKey, CacheEntry> map) {
    super(1000, map);
  }

  public TestCache(Map<CacheKey, CacheEntry> map, ClientSideCacheable cacheable) {

    super(1000, map, cacheable, new LRUEviction(1000));
  }

  public TestCache(int maxSize, Map<CacheKey, CacheEntry> map, ClientSideCacheable cacheable) {
    super(maxSize, map, cacheable, new LRUEviction(maxSize));
  }

}
