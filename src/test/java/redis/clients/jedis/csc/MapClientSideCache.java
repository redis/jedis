package redis.clients.jedis.csc;

import java.util.HashMap;
import java.util.Map;

import redis.clients.jedis.csc.hash.SimpleCommandHasher;

public class MapClientSideCache extends ClientSideCache {

  private final Map<Long, Object> cache;

  public MapClientSideCache() {
    this(new HashMap<>());
  }

  public MapClientSideCache(Map<Long, Object> map) {
    super(SimpleCommandHasher.INSTANCE);
    this.cache = map;
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
