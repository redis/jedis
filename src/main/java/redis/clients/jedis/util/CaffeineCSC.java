package redis.clients.jedis.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import redis.clients.jedis.ClientSideCache;

public class CaffeineCSC extends ClientSideCache {

  private static final int DEFAULT_MAXIMUM_SIZE = 10_000;

  private final Cache<ByteBuffer, Object> cache;

  public CaffeineCSC() {
    this(DEFAULT_MAXIMUM_SIZE);
  }

  public CaffeineCSC(int maximumSize) {
    this(Caffeine.newBuilder().maximumSize(maximumSize).build());
  }

  public CaffeineCSC(int maximumSize, int ttlSeconds) {
    this(Caffeine.newBuilder().maximumSize(maximumSize).expireAfterWrite(ttlSeconds, TimeUnit.SECONDS).build());
  }

  public CaffeineCSC(Cache<ByteBuffer, Object> caffeineCache) {
    this.cache = caffeineCache;
  }

  @Override
  public final void clear() {
    cache.invalidateAll();
  }

  @Override
  protected void remove(ByteBuffer key) {
    cache.invalidate(key);
  }

  @Override
  protected void put(ByteBuffer key, Object value) {
    cache.put(key, value);
  }

  @Override
  protected Object get(ByteBuffer key) {
    return cache.getIfPresent(key);
  }
}
