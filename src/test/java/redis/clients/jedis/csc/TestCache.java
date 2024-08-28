package redis.clients.jedis.csc;

import java.util.Map;

public class TestCache extends DefaultCache {

  public TestCache() {
    this(CacheConfig.DEFAULT_CACHE_MAX_SIZE);
  }

  TestCache(int maximumSize) {
    super(maximumSize);
  }

  TestCache(Map<CacheKey, CacheEntry> map) {
    this(CacheConfig.DEFAULT_CACHE_MAX_SIZE, map);
  }

  TestCache(int maximumSize, Map<CacheKey, CacheEntry> map) {
    super(maximumSize, map);
  }

  TestCache(Cacheable cacheable) {
    super(CacheConfig.DEFAULT_CACHE_MAX_SIZE, cacheable);
  }

}
