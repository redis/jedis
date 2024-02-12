package redis.clients.jedis.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import java.util.concurrent.TimeUnit;
import redis.clients.jedis.ClientSideCache;
import redis.clients.jedis.CommandObject;

public class GuavaCSC extends ClientSideCache {

  private static final HashFunction DEFAULT_HASH_FUNCTION = com.google.common.hash.Hashing.fingerprint2011();

  private final Cache<Long, Object> cache;
  private final HashFunction function;

  public GuavaCSC(Cache<Long, Object> guavaCache, HashFunction hashFunction) {
    this.cache = guavaCache;
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
    Hasher hasher = function.newHasher();
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

    public GuavaCSC build() {
      CacheBuilder cb = CacheBuilder.newBuilder();

      cb.maximumSize(maximumSize);

      cb.expireAfterWrite(expireTime, expireTimeUnit);

      return new GuavaCSC(cb.build(), hashFunction);
    }
  }
}
