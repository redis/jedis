package redis.clients.jedis.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import redis.clients.jedis.ClientSideCache;

public class GuavaCSC extends ClientSideCache {

  private static final int DEFAULT_MAXIMUM_SIZE = 10_000;

  private final Cache<Long, Object> cache;

  public GuavaCSC() {
    this(DEFAULT_MAXIMUM_SIZE);
  }

  public GuavaCSC(int maximumSize) {
    this(CacheBuilder.newBuilder().maximumSize(maximumSize).build());
  }

  public GuavaCSC(int maximumSize, int ttlSeconds) {
    this(CacheBuilder.newBuilder().maximumSize(maximumSize)
        .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS).build());
  }

  public GuavaCSC(Cache<Long, Object> guavaCache) {
    this.cache = guavaCache;
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
