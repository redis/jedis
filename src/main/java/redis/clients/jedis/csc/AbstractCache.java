package redis.clients.jedis.csc;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.util.SafeEncoder;

/**
 * The class to manage the client-side caching. User can provide an of implementation of this class
 * to the client object.
 */
@Experimental
public abstract class AbstractCache implements Cache {

  private Cacheable cacheable;
  private final Map<ByteBuffer, Set<CacheKey<?>>> redisKeysToCacheKeys = new ConcurrentHashMap<>();
  private final int maximumSize;
  private ReentrantLock lock = new ReentrantLock();
  private volatile CacheStats stats = new CacheStats();

  protected AbstractCache(int maximumSize) {
    this(maximumSize, DefaultCacheable.INSTANCE);
  }

  protected AbstractCache(int maximumSize, Cacheable cacheable) {
    this.maximumSize = maximumSize;
    this.cacheable = cacheable;
  }

  // Cache interface methods

  @Override
  public int getMaxSize() {
    return maximumSize;
  }

  @Override
  public abstract int getSize();

  @Override
  public abstract Collection<CacheEntry> getCacheEntries();

  @Override
  public CacheEntry get(CacheKey cacheKey) {
    CacheEntry entry = getFromStore(cacheKey);
    if (entry != null) {
      getEvictionPolicy().touch(cacheKey);
    }
    return entry;
  }

  @Override
  public CacheEntry set(CacheKey cacheKey, CacheEntry entry) {
    lock.lock();
    try {
      entry = putIntoStore(cacheKey, entry);
      EvictionPolicy policy = getEvictionPolicy();
      policy.touch(cacheKey);
      CacheKey evictedKey = policy.evictNext();
      if (evictedKey != null) {
        delete(evictedKey);
        stats.evict();
      }
      for (Object redisKey : cacheKey.getRedisKeys()) {
        ByteBuffer mapKey = makeKeyForRedisKeysToCacheKeys(redisKey);
        if (redisKeysToCacheKeys.containsKey(mapKey)) {
          redisKeysToCacheKeys.get(mapKey).add(cacheKey);
        } else {
          Set<CacheKey<?>> set = ConcurrentHashMap.newKeySet();
          set.add(cacheKey);
          redisKeysToCacheKeys.put(mapKey, set);
        }
      }
      stats.load();
      return entry;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean delete(CacheKey cacheKey) {
    lock.lock();
    try {
      boolean removed = removeFromStore(cacheKey);
      getEvictionPolicy().reset(cacheKey);

      // removing it from redisKeysToCacheKeys as well
      // TODO: considering not doing it, what is the impact of not doing it ??
      for (Object redisKey : cacheKey.getRedisKeys()) {
        ByteBuffer mapKey = makeKeyForRedisKeysToCacheKeys(redisKey);
        Set<CacheKey<?>> cacheKeysRelatedtoRedisKey = redisKeysToCacheKeys.get(mapKey);
        if (cacheKeysRelatedtoRedisKey != null) {
          cacheKeysRelatedtoRedisKey.remove(cacheKey);
        }
      }
      return removed;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public List<Boolean> delete(List<CacheKey> cacheKeys) {
    lock.lock();
    try {
      return cacheKeys.stream().map(this::delete).collect(Collectors.toList());
    } finally {
      lock.unlock();
    }
  }

  @Override
  public List<CacheKey> deleteByRedisKey(Object key) {
    lock.lock();
    try {
      final ByteBuffer mapKey = makeKeyForRedisKeysToCacheKeys(key);

      Set<CacheKey<?>> commands = redisKeysToCacheKeys.get(mapKey);
      List<CacheKey> cacheKeys = new ArrayList<>();
      if (commands != null) {
        cacheKeys.addAll(commands.stream().filter(this::removeFromStore).collect(Collectors.toList()));
        stats.invalidationByServer(cacheKeys.size());
        redisKeysToCacheKeys.remove(mapKey);
      }
      stats.invalidationMessages();
      return cacheKeys;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public List<CacheKey> deleteByRedisKeys(List keys) {
    if (keys == null) {
      flush();
      return null;
    }
    lock.lock();
    try {
      return ((List<Object>) keys).stream()
          .map(this::deleteByRedisKey).flatMap(List::stream).collect(Collectors.toList());
    } finally {
      lock.unlock();
    }
  }

  @Override
  public int flush() {
    lock.lock();
    try {
      int result = this.getSize();
      clearStore();
      redisKeysToCacheKeys.clear();
      getEvictionPolicy().resetAll();
      getStats().flush();
      return result;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean isCacheable(CacheKey cacheKey) {
    return cacheable.isCacheable(cacheKey.getRedisCommand(), cacheKey.getRedisKeys());
  }

  @Override
  public boolean hasCacheKey(CacheKey cacheKey) {
    return containsKeyInStore(cacheKey);
  }

  @Override
  public abstract EvictionPolicy getEvictionPolicy();

  @Override
  public CacheStats getStats() {
    return stats;
  }

  @Override
  public CacheStats getAndResetStats() {
    CacheStats result = stats;
    stats = new CacheStats();
    return result;
  }

  @Override
  public boolean compatibilityMode() {
    return false;
  }
  // End of Cache interface methods

  // abstract methods to be implemented by the concrete classes
  protected abstract CacheEntry getFromStore(CacheKey cacheKey);

  protected abstract CacheEntry putIntoStore(CacheKey cacheKey, CacheEntry entry);

  protected abstract boolean removeFromStore(CacheKey cacheKey);

  // protected abstract Collection<CacheKey> remove(Set<CacheKey<?>> commands);

  protected abstract void clearStore();

  protected abstract boolean containsKeyInStore(CacheKey cacheKey);

  // End of abstract methods to be implemented by the concrete classes

  private ByteBuffer makeKeyForRedisKeysToCacheKeys(Object key) {
    if (key instanceof byte[]) {
      return makeKeyForRedisKeysToCacheKeys((byte[]) key);
    } else if (key instanceof String) {
      return makeKeyForRedisKeysToCacheKeys(SafeEncoder.encode((String) key));
    } else {
      throw new IllegalArgumentException(key.getClass().getSimpleName() + " is not supported."
          + " Value: \"" + String.valueOf(key) + "\".");
    }
  }

  private static ByteBuffer makeKeyForRedisKeysToCacheKeys(byte[] b) {
    return ByteBuffer.wrap(b);
  }

}
