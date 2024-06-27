package redis.clients.jedis.csc;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import redis.clients.jedis.CommandObject;

public class GuavaClientSideCache extends ClientSideCache {

  private final Cache<CommandObject, Object> cache;

  public GuavaClientSideCache(Cache<CommandObject, Object> guavaCache) {
    super();
    this.cache = guavaCache;
  }

  public GuavaClientSideCache(Cache<CommandObject, Object> cache, ClientSideCacheable cacheable) {
    super(cacheable);
    this.cache = cache;
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

    public Builder cacheable(ClientSideCacheable cacheable) {
      this.cacheable = cacheable;
      return this;
    }

    public GuavaClientSideCache build() {
      CacheBuilder cb = CacheBuilder.newBuilder();

      cb.maximumSize(maximumSize);

      cb.expireAfterWrite(expireTime, expireTimeUnit);

      return new GuavaClientSideCache(cb.build(), cacheable);
    }
  }
}
