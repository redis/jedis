package redis.clients.jedis.csc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import redis.clients.jedis.CommandObject;

public class CaffeineClientSideCache extends ClientSideCache {

  private final Cache<CommandObject, Object> cache;

  public CaffeineClientSideCache(Cache<CommandObject, Object> caffeineCache) {
    this.cache = caffeineCache;
  }

  @Override
  protected final void invalidateFullCache() {
    cache.invalidateAll();
  }

  @Override
  protected void invalidateCache(Iterable<CommandObject<?>> commands) {
    cache.invalidateAll(commands);
  }

  @Override
  protected <T> void putValue(CommandObject<T> command, T value) {
    cache.put(command, value);
  }

  @Override
  protected <T> T getValue(CommandObject<T> command) {
    return (T) cache.getIfPresent(command);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private long maximumSize = DEFAULT_MAXIMUM_SIZE;
    private long expireTime = DEFAULT_EXPIRE_SECONDS;
    private final TimeUnit expireTimeUnit = TimeUnit.SECONDS;

    private Builder() { }

    public Builder maximumSize(int size) {
      this.maximumSize = size;
      return this;
    }

    public Builder ttl(int seconds) {
      this.expireTime = seconds;
      return this;
    }

    public CaffeineClientSideCache build() {
      Caffeine cb = Caffeine.newBuilder();

      cb.maximumSize(maximumSize);

      cb.expireAfterWrite(expireTime, expireTimeUnit);

      return new CaffeineClientSideCache(cb.build());
    }
  }
}
