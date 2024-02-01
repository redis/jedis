package redis.clients.jedis;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import redis.clients.jedis.util.SafeEncoder;

public abstract class ClientSideCache {

  private final Map<ByteBuffer, Set<Long>> keyHashes;
  private final ReentrantLock writeLock = new ReentrantLock();

  protected ClientSideCache() {
    this.keyHashes = new ConcurrentHashMap<>();
  }

  public abstract void invalidateAll();

  protected abstract void invalidateAll(Iterable<Long> hashes);

  final void invalidate(List list) {
    if (list == null) {
      invalidateAll();
      return;
    }

    list.forEach(this::invalidate0);
  }

  private void invalidate0(Object key) {
    if (!(key instanceof byte[])) {
      throw new AssertionError("" + key.getClass().getSimpleName() + " is not supported. Value: " + String.valueOf(key));
    }

    final ByteBuffer mapKey = makeKey((byte[]) key);

    Set<Long> hashes = keyHashes.get(mapKey);
    if (hashes != null) {
      writeLock.lock();
      try {
        invalidateAll(hashes);
        keyHashes.remove(mapKey);
      } finally {
        writeLock.unlock();
      }
    }
  }

  protected abstract void put(long hash, Object value);

  protected abstract Object get(long hash);

  final <T> T getValue(Function<CommandObject<T>, T> loader, CommandObject<T> command, String... keys) {

    final long hash = getHash(command);

    T value = (T) get(hash);
    if (value != null) {
      return value;
    }

    value = loader.apply(command);
    if (value != null) {
      writeLock.lock();
      try {
        put(hash, value);
        for (String key : keys) {
          ByteBuffer mapKey = makeKey(key);
          if (keyHashes.containsKey(mapKey)) {
            keyHashes.get(mapKey).add(hash);
          } else {
            Set<Long> set = new HashSet<>();
            set.add(hash);
            keyHashes.put(mapKey, set);
          }
        }
      } finally {
        writeLock.unlock();
      }
    }

    return value;
  }

  protected abstract long getHash(CommandObject command);

  private ByteBuffer makeKey(String key) {
    return makeKey(SafeEncoder.encode(key));
  }

  private static ByteBuffer makeKey(byte[] b) {
    return ByteBuffer.wrap(b);
  }
}
