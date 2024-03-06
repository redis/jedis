package redis.clients.jedis.csc;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.HashFunction;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.csc.hash.GuavaCommandHash;
import redis.clients.jedis.csc.hash.CommandLongHash;

public class GuavaClientSideCache extends ClientSideCache {

  private final Cache<Long, Object> cache;

  public GuavaClientSideCache(Cache<Long, Object> guavaCache) {
    this(guavaCache, GuavaCommandHash.DEFAULT_HASH_FUNCTION);
  }

  public GuavaClientSideCache(Cache<Long, Object> guavaCache, HashFunction hashFunction) {
    this(guavaCache, new GuavaCommandHash(hashFunction));
  }

  public GuavaClientSideCache(Cache<Long, Object> guavaCache, CommandLongHash hashing) {
    super(hashing);
    this.cache = guavaCache;
  }

  public GuavaClientSideCache(Cache<Long, Object> guavaCache, ClientSideCacheable cacheable) {
    this(guavaCache, new GuavaCommandHash(GuavaCommandHash.DEFAULT_HASH_FUNCTION), cacheable);
  }

  public GuavaClientSideCache(Cache<Long, Object> cache, CommandLongHash hashing, ClientSideCacheable cacheable) {
    super(hashing, cacheable);
    this.cache = cache;
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

    // not using a default value to avoid an object creation like 'new GuavaHashing(hashFunction)'
    private HashFunction hashFunction = null;
    private CommandLongHash longHashing = null;

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

    public Builder hashFunction(HashFunction function) {
      this.hashFunction = function;
      this.longHashing = null;
      return this;
    }

    public Builder hash(CommandLongHash hashing) {
      this.longHashing = hashing;
      this.hashFunction = null;
      return this;
    }

    public Builder cacheable(ClientSideCacheable cacheable) {
      this.cacheable = cacheable;
      return this;
    }

    public GuavaClientSideCache build() {
      CacheBuilder cb = CacheBuilder.newBuilder();

      cb.maximumSize(maximumSize);

      cb.expireAfterWrite(expireTime, expireTimeUnit);

      return longHashing != null ? new GuavaClientSideCache(cb.build(), longHashing, cacheable)
          : hashFunction != null ? new GuavaClientSideCache(cb.build(), new GuavaCommandHash(hashFunction), cacheable)
              : new GuavaClientSideCache(cb.build(), cacheable);
    }
  }
}
