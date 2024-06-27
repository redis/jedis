package redis.clients.jedis.csc;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.util.SafeEncoder;

/**
 * The class to manage the client-side caching. User can provide any of implementation of this class to the client
 * object; e.g. {@link redis.clients.jedis.csc.CaffeineClientSideCache CaffeineClientSideCache} or
 * {@link redis.clients.jedis.csc.GuavaClientSideCache GuavaClientSideCache} or a custom implementation of their own.
 */
@Experimental
public abstract class ClientSideCache {

  protected static final int DEFAULT_MAXIMUM_SIZE = 10_000;
  protected static final int DEFAULT_EXPIRE_SECONDS = 100;

  private final Map<ByteBuffer, Set<CommandObject<?>>> keyToCommandHashes = new ConcurrentHashMap<>();
  private final ClientSideCacheable cacheable;

  protected ClientSideCache() {
    this(DefaultClientSideCacheable.INSTANCE);
  }

  protected ClientSideCache(ClientSideCacheable cacheable) {
    this.cacheable = cacheable;
  }

  protected abstract void invalidateFullCache();

  protected abstract void invalidateCache(Iterable<CommandObject<?>> commands);

  protected abstract <T> void putValue(CommandObject<T> command, T value);

  protected abstract <T> T getValue(CommandObject<T> command);

  public final void clear() {
    invalidateAllKeysAndCommandHashes();
  }

  public final void removeKey(Object key) {
    invalidateKeyAndRespectiveCommandHashes(key);
  }

  public final void invalidate(List list) {
    if (list == null) {
      invalidateAllKeysAndCommandHashes();
      return;
    }

    list.forEach(this::invalidateKeyAndRespectiveCommandHashes);
  }

  private void invalidateAllKeysAndCommandHashes() {
    invalidateFullCache();
    keyToCommandHashes.clear();
  }

  private void invalidateKeyAndRespectiveCommandHashes(Object key) {
//    if (!(key instanceof byte[])) {
//      // This should be called internally. That's why throwing AssertionError instead of IllegalArgumentException.
//      throw new AssertionError("" + key.getClass().getSimpleName() + " is not supported. Value: " + String.valueOf(key));
//    }
//
//    final ByteBuffer mapKey = makeKeyForKeyToCommandHashes((byte[]) key);
    final ByteBuffer mapKey = makeKeyForKeyToCommandHashes(key);

    Set<CommandObject<?>> commands = keyToCommandHashes.get(mapKey);
    if (commands != null) {
      invalidateCache(commands);
      keyToCommandHashes.remove(mapKey);
    }
  }

  public final <T> T get(Function<CommandObject<T>, T> loader, CommandObject<T> command, Object... keys) {

    if (!cacheable.isCacheable(command.getArguments().getCommand(), keys)) {
      return loader.apply(command);
    }

    T value = getValue(command);
    if (value != null) {
      return value;
    }

    value = loader.apply(command);
    if (value != null) {
      putValue(command, value);
      for (Object key : keys) {
        ByteBuffer mapKey = makeKeyForKeyToCommandHashes(key);
        if (keyToCommandHashes.containsKey(mapKey)) {
          keyToCommandHashes.get(mapKey).add(command);
        } else {
          Set<CommandObject<?>> set = ConcurrentHashMap.newKeySet();
          set.add(command);
          keyToCommandHashes.put(mapKey, set);
        }
      }
    }

    return value;
  }

  private ByteBuffer makeKeyForKeyToCommandHashes(Object key) {
    if (key instanceof byte[]) return makeKeyForKeyToCommandHashes((byte[]) key);
    else if (key instanceof String) return makeKeyForKeyToCommandHashes(SafeEncoder.encode((String) key));
    else throw new IllegalArgumentException("" + key.getClass().getSimpleName() + " is not supported. Value: " + String.valueOf(key));
  }

  private static ByteBuffer makeKeyForKeyToCommandHashes(byte[] b) {
    return ByteBuffer.wrap(b);
  }
}
