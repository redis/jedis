package redis.clients.jedis.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import net.openhft.hashing.LongHashFunction;
import redis.clients.jedis.ClientSideCache;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.args.Rawable;

public class CaffeineCSC extends ClientSideCache {

  private static final LongHashFunction DEFAULT_HASH_FUNCTION = LongHashFunction.xx3();

  private final Cache<Long, Object> cache;
  private final LongHashFunction function;

  public CaffeineCSC(Cache<Long, Object> caffeineCache, LongHashFunction hashFunction) {
    this.cache = caffeineCache;
    this.function = hashFunction;
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

  @Override
  protected final long getCommandHash(CommandObject command) {
    long[] nums = new long[command.getArguments().size() + 1];
    int idx = 0;
    for (Rawable raw : command.getArguments()) {
      nums[idx++] = function.hashBytes(raw.getRaw());
    }
    nums[idx] = function.hashInt(command.getBuilder().hashCode());
    return function.hashLongs(nums);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private long maximumSize = DEFAULT_MAXIMUM_SIZE;
    private long expireTime = DEFAULT_EXPIRE_SECONDS;
    private final TimeUnit expireTimeUnit = TimeUnit.SECONDS;

    private LongHashFunction hashFunction = DEFAULT_HASH_FUNCTION;

    private Builder() { }

    public Builder maximumSize(int size) {
      this.maximumSize = size;
      return this;
    }

    public Builder ttl(int seconds) {
      this.expireTime = seconds;
      return this;
    }

    public Builder hashFunction(LongHashFunction function) {
      this.hashFunction = function;
      return this;
    }

    public CaffeineCSC build() {
      Caffeine cb = Caffeine.newBuilder();

      cb.maximumSize(maximumSize);

      cb.expireAfterWrite(expireTime, expireTimeUnit);

      return new CaffeineCSC(cb.build(), hashFunction);
    }
  }
}
