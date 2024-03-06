package redis.clients.jedis.csc.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.csc.ClientSideCache;
import redis.clients.jedis.csc.ClientSideCacheable;
import redis.clients.jedis.csc.DefaultClientSideCacheable;

public class GuavaCSC extends ClientSideCache {

  private static final HashFunction DEFAULT_HASH_FUNCTION = com.google.common.hash.Hashing.fingerprint2011();

  private final Cache<Long, Object> cache;
  private final HashFunction hashFunction;

  public GuavaCSC(Cache<Long, Object> guavaCache) {
    this(guavaCache, DEFAULT_HASH_FUNCTION);
  }

  public GuavaCSC(Cache<Long, Object> guavaCache, HashFunction hashFunction) {
    super();
    this.cache = guavaCache;
    this.hashFunction = hashFunction;
  }

  public GuavaCSC(Cache<Long, Object> guavaCache, ClientSideCacheable cacheable) {
    this(guavaCache, DEFAULT_HASH_FUNCTION, cacheable);
  }

  public GuavaCSC(Cache<Long, Object> cache, HashFunction function, ClientSideCacheable cacheable) {
    super(cacheable);
    this.cache = cache;
    this.hashFunction = function;
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

  @Override
  protected final long getHash(CommandObject command) {
    Hasher hasher = hashFunction.newHasher();
    command.getArguments().forEach(raw -> hasher.putBytes(raw.getRaw()));
    hasher.putInt(command.getBuilder().hashCode());
    return hasher.hash().asLong();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private long maximumSize = DEFAULT_MAXIMUM_SIZE;
    private long expireTime = DEFAULT_EXPIRE_SECONDS;
    private final TimeUnit expireTimeUnit = TimeUnit.SECONDS;

    private HashFunction hashFunction = DEFAULT_HASH_FUNCTION;

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

      return new GuavaCSC(cb.build(), hashFunction, cacheable);
    }
  }
}
