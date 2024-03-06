package redis.clients.jedis.csc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.args.Rawable;

public class MapCSC extends ClientSideCache {

  private final Map<Long, Object> cache;

  public MapCSC() {
    this(new HashMap<>());
  }

  public MapCSC(Map<Long, Object> map) {
    this.cache = map;
  }

  public MapCSC(Map<Long, Object> cache, ClientSideCacheable cacheable) {
    super(cacheable);
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

  @Override
  protected final long getHash(CommandObject command) {
    long result = 1;
    for (Rawable raw : command.getArguments()) {
      result = 31 * result + Arrays.hashCode(raw.getRaw());
    }
    return 31 * result + command.getBuilder().hashCode();
  }
}
