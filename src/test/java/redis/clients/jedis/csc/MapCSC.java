package redis.clients.jedis.csc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.csc.hash.PrimitiveArrayHashing;

public class MapCSC extends ClientSideCache {

  private static final PrimitiveArrayHashing HASHING = new PrimitiveArrayHashing() {

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

  public MapCSC() {
    this(new HashMap<>());
  }

  public MapCSC(Map<Long, Object> map) {
    super(HASHING);
    this.cache = map;
  }

  public MapCSC(Map<Long, Object> cache, ClientSideCacheable cacheable) {
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
