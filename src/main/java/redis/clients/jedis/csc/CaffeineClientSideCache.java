package redis.clients.jedis.csc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.csc.hash.CommandLongHasher;
import redis.clients.jedis.csc.hash.SimpleCommandHasher;

public class CaffeineClientSideCache extends ClientSideCache {

  private final Cache<Long, Object> cache;

  public CaffeineClientSideCache(Cache<Long, Object> caffeineCache) {
    this(caffeineCache, SimpleCommandHasher.INSTANCE);
  }

  public CaffeineClientSideCache(Cache<Long, Object> caffeineCache, CommandLongHasher commandHasher) {
    this(caffeineCache, commandHasher, DefaultClientSideCacheable.INSTANCE);
  }

  public CaffeineClientSideCache(Cache<Long, Object> caffeineCache, ClientSideCacheable cacheable) {
    this(caffeineCache, SimpleCommandHasher.INSTANCE, cacheable);
  }

  public CaffeineClientSideCache(Cache<Long, Object> caffeineCache, CommandLongHasher commandHasher, ClientSideCacheable cacheable) {
    super(commandHasher, cacheable);
    this.cache = caffeineCache;
  }

  @Override
  protected final void invalidateAllHashes() {
    cache.invalidateAll();
  }

  @Override
  protected void invalidateHashes(Iterable<Long> hashes) {
    cache.invalidateAll(hashes);
  }

  @Override
  protected void putValue(long hash, Object value) {
    cache.put(hash, value);
  }

  @Override
  protected Object getValue(long hash) {
    return cache.getIfPresent(hash);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private long maximumSize = DEFAULT_MAXIMUM_SIZE;
    private long expireTime = DEFAULT_EXPIRE_SECONDS;
    private final TimeUnit expireTimeUnit = TimeUnit.SECONDS;

    // not using a default value to avoid an object creation like 'new OpenHftHashing(hashFunction)'
    private CommandLongHasher commandHasher = SimpleCommandHasher.INSTANCE;

    private ClientSideCacheable cacheable = DefaultClientSideCacheable.INSTANCE;

    private Builder() { }

    public Builder maximumSize(int size) {
      this.maximumSize = size;
      return this;
    }

    public Builder ttl(int seconds) {
      this.expireTime = seconds;
      return this;
    }

    public Builder commandHasher(CommandLongHasher commandHasher) {
      this.commandHasher = commandHasher;
      return this;
    }

    public Builder cacheable(ClientSideCacheable cacheable) {
      this.cacheable = cacheable;
      return this;
    }

    public CaffeineClientSideCache build() {
      Caffeine cb = Caffeine.newBuilder();

      cb.maximumSize(maximumSize);

      cb.expireAfterWrite(expireTime, expireTimeUnit);

      return new CaffeineClientSideCache(cb.build(), commandHasher, cacheable);
    }
  }
}
