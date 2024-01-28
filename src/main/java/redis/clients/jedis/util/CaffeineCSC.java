package redis.clients.jedis.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import redis.clients.jedis.ClientSideCache;

public class CaffeineCSC extends ClientSideCache {

  private static final int DEFAULT_MAXIMUM_SIZE = 10_000;

  private final Cache<Long, Object> cache;

  public CaffeineCSC() {
    this(DEFAULT_MAXIMUM_SIZE);
  }

  public CaffeineCSC(int maximumSize) {
    this(Caffeine.newBuilder().maximumSize(maximumSize).build());
  }

  public CaffeineCSC(int maximumSize, int ttlSeconds) {
    this(Caffeine.newBuilder().maximumSize(maximumSize)
        .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS).build());
  }

  public CaffeineCSC(Cache<Long, Object> caffeineCache) {
    this.cache = caffeineCache;
  }

  @Override
  public final void invalidateAll() {
    cache.invalidateAll();
  }

  @Override
  protected void invalidateAll(Iterable<Long> hashes) {
    cache.invalidateAll(hashes);
  }

  @Override
  protected void put(long hash, Object value) {
    cache.put(hash, value);
  }

  @Override
  protected Object get(long hash) {
    return cache.getIfPresent(hash);
  }
}
