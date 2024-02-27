package redis.clients.jedis.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import redis.clients.jedis.ClientSideCache;
import redis.clients.jedis.csc.hash.CommandLongHashing;
import redis.clients.jedis.csc.hash.OpenHftHashing;

public class CaffeineCSC extends ClientSideCache {

  private final Cache<Long, Object> cache;

  public CaffeineCSC(Cache<Long, Object> caffeineCache) {
    this(caffeineCache, new OpenHftHashing(OpenHftHashing.DEFAULT_HASH_FUNCTION));
  }

  public CaffeineCSC(Cache<Long, Object> caffeineCache, CommandLongHashing hashing) {
    super(hashing);
    this.cache = caffeineCache;
  }

  @Override
  protected final void invalidateAllCommandHashes() {
    cache.invalidateAll();
  }

  @Override
  protected void invalidateCommandHashes(Iterable<Long> hashes) {
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

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private long maximumSize = DEFAULT_MAXIMUM_SIZE;
    private long expireTime = DEFAULT_EXPIRE_SECONDS;
    private final TimeUnit expireTimeUnit = TimeUnit.SECONDS;

    private CommandLongHashing longHashing = null;

    private Builder() { }

    public Builder maximumSize(int size) {
      this.maximumSize = size;
      return this;
    }

    public Builder ttl(int seconds) {
      this.expireTime = seconds;
      return this;
    }

    public Builder hashing(CommandLongHashing hashing) {
      this.longHashing = hashing;
      return this;
    }

    public CaffeineCSC build() {
      Caffeine cb = Caffeine.newBuilder();

      cb.maximumSize(maximumSize);

      cb.expireAfterWrite(expireTime, expireTimeUnit);

      return longHashing != null ? new CaffeineCSC(cb.build(), longHashing) : new CaffeineCSC(cb.build());
    }
  }
}
