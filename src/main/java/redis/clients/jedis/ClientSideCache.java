package redis.clients.jedis;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import redis.clients.jedis.util.SafeEncoder;

public abstract class ClientSideCache {

  private final Map<ByteBuffer, Set<Long>> keyHashes;
  private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
  private final Lock readLock = rwl.readLock();
  private final Lock writeLock = rwl.writeLock();

  protected ClientSideCache() {
    this.keyHashes = new ConcurrentHashMap<>();
  }

  protected ClientSideCache(Map<ByteBuffer, Set<Long>> keyHashes) {
    this.keyHashes = keyHashes;
  }

  public abstract void invalidateAll();

  protected abstract void invalidateAll(Iterable<Long> hashes);

  protected abstract void put(long hash, Object value);

  protected abstract Object get(long hash);

  final void invalidateKeys(List list) {
    if (list == null) {
      invalidateAll();
      return;
    }

    Set<Long> hashes = new HashSet<>();
    list.forEach(key -> hashes.addAll(getHashes(key)));
    invalidateAll(hashes);
  }

  private Set<Long> getHashes(Object key) {
    if (!(key instanceof byte[])) {
      throw new AssertionError("" + key.getClass().getSimpleName() + " is not supported. Value: " + String.valueOf(key));
    }

    final ByteBuffer mapKey = makeKey((byte[]) key);
    readLock.lock();
    try {
      Set<Long> hashes = keyHashes.get(mapKey);
      return hashes != null ? hashes : Collections.emptySet();
    } finally {
      readLock.unlock();
    }
  }

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

  private long getHash(CommandObject command) {
    // TODO:
    return 0;
  }

  private ByteBuffer makeKey(String key) {
    return makeKey(SafeEncoder.encode(key));
  }

  private static ByteBuffer makeKey(byte[] b) {
    return ByteBuffer.wrap(b);
  }
}
