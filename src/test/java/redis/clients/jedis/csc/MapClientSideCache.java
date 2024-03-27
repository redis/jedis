package redis.clients.jedis.csc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import redis.clients.jedis.csc.hash.PrimitiveArrayCommandHasher;

public class MapClientSideCache extends ClientSideCache {

  private static final PrimitiveArrayCommandHasher HASHING = new PrimitiveArrayCommandHasher() {

    @Override
    protected long hashLongs(long[] longs) {
      return Arrays.hashCode(longs);
    }

    @Override
    protected long hashBytes(byte[] bytes) {
      return Arrays.hashCode(bytes);
    }
  };

  private final Map<Long, Object> cache;

  public MapClientSideCache() {
    this(new HashMap<>());
  }

  public MapClientSideCache(Map<Long, Object> map) {
    super(HASHING);
    this.cache = map;
  }

  public MapClientSideCache(Map<Long, Object> cache, ClientSideCacheable cacheable) {
    super(HASHING, cacheable);
    this.cache = cache;
  }

  @Override
  protected final void invalidateAllHashes() {
    cache.clear();
  }

  @Override
  protected void invalidateHashes(Iterable<Long> hashes) {
    hashes.forEach(hash -> cache.remove(hash));
  }

  @Override
  protected void putValue(long hash, Object value) {
    cache.put(hash, value);
  }

  @Override
  protected Object getValue(long hash) {
    return cache.get(hash);
  }
}
