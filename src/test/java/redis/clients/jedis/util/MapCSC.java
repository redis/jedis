package redis.clients.jedis.util;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import redis.clients.jedis.ClientSideCache;
import redis.clients.jedis.csc.hash.ByteArrayHashing;

public class MapCSC extends ClientSideCache {

  private static final ByteArrayHashing HASHING = new ByteArrayHashing() {

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
    this(new ConcurrentHashMap<>());
  }

  public MapCSC(Map<Long, Object> map) {
    super(HASHING);
    this.cache = map;
  }

  @Override
  protected final void invalidateAllCommandHashes() {
    cache.clear();
  }

  @Override
  protected void invalidateCommandHashes(Iterable<Long> hashes) {
    hashes.forEach(hash -> cache.remove(hash));
  }

  @Override
  protected void put(long hash, Object value) {
    cache.put(hash, value);
  }

  @Override
  protected Object get(long hash) {
    return cache.get(hash);
  }
}
