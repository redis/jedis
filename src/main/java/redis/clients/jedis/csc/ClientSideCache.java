package redis.clients.jedis.csc;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.util.SafeEncoder;

/**
 * The class to manage the client-side caching. User can provide any of implementation of this class to the client
 * object; e.g. {@link redis.clients.jedis.csc.util.CaffeineCSC CaffeineCSC} or
 * {@link redis.clients.jedis.csc.util.GuavaCSC GuavaCSC} or a custom implementation of their own.
 */
public abstract class ClientSideCache {

  protected static final int DEFAULT_MAXIMUM_SIZE = 10_000;
  protected static final int DEFAULT_EXPIRE_SECONDS = 100;

  private final Map<ByteBuffer, Set<Long>> keyToCommandHashes;

  protected ClientSideCache() {
    this.keyToCommandHashes = new ConcurrentHashMap<>();
  }

  protected abstract void invalidateAllCommandHashes();

  protected abstract void invalidateCommandHashes(Iterable<Long> hashes);

  protected abstract void put(long hash, Object value);

  protected abstract Object get(long hash);

  protected abstract long getCommandHash(CommandObject command);

  public final void clear() {
    invalidateAllKeysAndCommandHashes();
  }

  public final void invalidate(List list) {
    if (list == null) {
      invalidateAllKeysAndCommandHashes();
      return;
    }

    list.forEach(this::invalidateKeyAndRespectiveCommandHashes);
  }

  private void invalidateAllKeysAndCommandHashes() {
    invalidateAllCommandHashes();
    keyToCommandHashes.clear();
  }

  private void invalidateKeyAndRespectiveCommandHashes(Object key) {
    if (!(key instanceof byte[])) {
      throw new AssertionError("" + key.getClass().getSimpleName() + " is not supported. Value: " + String.valueOf(key));
    }

    final ByteBuffer mapKey = makeKeyForKeyToCommandHashes((byte[]) key);

    Set<Long> hashes = keyToCommandHashes.get(mapKey);
    if (hashes != null) {
      invalidateCommandHashes(hashes);
      keyToCommandHashes.remove(mapKey);
    }
  }

  public final <T> T getValue(Function<CommandObject<T>, T> loader, CommandObject<T> command, Object... keys) {

    final long hash = getCommandHash(command);

    T value = (T) get(hash);
    if (value != null) {
      return value;
    }

    value = loader.apply(command);
    if (value != null) {
      put(hash, value);
      for (Object key : keys) {
        ByteBuffer mapKey = makeKeyForKeyToCommandHashes(key);
        if (keyToCommandHashes.containsKey(mapKey)) {
          keyToCommandHashes.get(mapKey).add(hash);
        } else {
          Set<Long> set = new HashSet<>();
          set.add(hash);
          keyToCommandHashes.put(mapKey, set);
        }
      }
    }

    return value;
  }

  private ByteBuffer makeKeyForKeyToCommandHashes(Object key) {
    if (key instanceof byte[]) return makeKeyForKeyToCommandHashes((byte[]) key);
    else if (key instanceof String) return makeKeyForKeyToCommandHashes(SafeEncoder.encode((String) key));
    else throw new AssertionError("" + key.getClass().getSimpleName() + " is not supported. Value: " + String.valueOf(key));
  }

  private static ByteBuffer makeKeyForKeyToCommandHashes(byte[] b) {
    return ByteBuffer.wrap(b);
  }
}
