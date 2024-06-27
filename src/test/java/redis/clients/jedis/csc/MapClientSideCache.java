package redis.clients.jedis.csc;

import java.util.HashMap;
import java.util.Map;

import redis.clients.jedis.CommandObject;

public class MapClientSideCache extends ClientSideCache {

  private final Map<CommandObject, Object> cache;

  public MapClientSideCache() {
    this(new HashMap<>());
  }

  public MapClientSideCache(Map<CommandObject, Object> map) {
    super();
    this.cache = map;
  }

  public MapClientSideCache(Map<CommandObject, Object> cache, ClientSideCacheable cacheable) {
    super(cacheable);
    this.cache = cache;
  }

  @Override
  protected final void invalidateFullCache() {
    cache.clear();
  }

  @Override
  protected void invalidateCache(Iterable<CommandObject<?>> commands) {
    commands.forEach(hash -> cache.remove(hash));
  }

  @Override
  protected <T> void putValue(CommandObject<T> command, T value) {
    cache.put(command, value);
  }

  @Override
  protected <T> T getValue(CommandObject<T> command) {
    return (T) cache.get(command);
  }
}
