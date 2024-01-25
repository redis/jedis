package redis.clients.jedis.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import redis.clients.jedis.ClientSideCache;

public class GuavaCSC extends ClientSideCache {

  private static final int DEFAULT_MAXIMUM_SIZE = 10_000;

  private final Cache<ByteBuffer, Object> cache;

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

  public GuavaCSC(Cache<ByteBuffer, Object> caffeineCache) {
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
