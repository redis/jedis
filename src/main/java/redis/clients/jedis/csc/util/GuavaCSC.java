package redis.clients.jedis.csc.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.HashFunction;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.csc.ClientSideCache;
import redis.clients.jedis.csc.ClientSideCacheable;
import redis.clients.jedis.csc.DefaultClientSideCacheable;
import redis.clients.jedis.csc.hash.CommandLongHashing;
import redis.clients.jedis.csc.hash.GuavaHashing;

public class GuavaCSC extends ClientSideCache {

  private final Cache<Long, Object> cache;

  public GuavaCSC(Cache<Long, Object> guavaCache) {
    this(guavaCache, GuavaHashing.DEFAULT_HASH_FUNCTION);
  }

  public GuavaCSC(Cache<Long, Object> guavaCache, HashFunction hashFunction) {
    this(guavaCache, new GuavaHashing(hashFunction));
  }

  public GuavaCSC(Cache<Long, Object> guavaCache, CommandLongHashing hashing) {
    super(hashing);
    this.cache = guavaCache;
  }

  public GuavaCSC(Cache<Long, Object> guavaCache, ClientSideCacheable cacheable) {
    this(guavaCache, new GuavaHashing(GuavaHashing.DEFAULT_HASH_FUNCTION), cacheable);
  }

  public GuavaCSC(Cache<Long, Object> cache, CommandLongHashing hashing, ClientSideCacheable cacheable) {
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
    private CommandLongHashing longHashing = null;

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

    public Builder hashing(CommandLongHashing hashing) {
      this.longHashing = hashing;
      this.hashFunction = null;
      return this;
    }

    public Builder cacheable(ClientSideCacheable cacheable) {
      this.cacheable = cacheable;
      return this;
    }

    public GuavaCSC build() {
      CacheBuilder cb = CacheBuilder.newBuilder();

      cb.maximumSize(maximumSize);

      cb.expireAfterWrite(expireTime, expireTimeUnit);

      return longHashing != null ? new GuavaCSC(cb.build(), longHashing)
          : hashFunction != null ? new GuavaCSC(cb.build(), hashFunction)
              : new GuavaCSC(cb.build());
    }
  }
}
