package redis.clients.jedis.csc;

import java.lang.ref.WeakReference;

import redis.clients.jedis.Connection;
import redis.clients.jedis.annots.Internal;

@Internal
public class CacheEntry<T> {

  private final CacheKey<T> cacheKey;
  private final T value;
  private final WeakReference<Connection> connection;

  public CacheEntry(CacheKey<T> cacheKey, T value, WeakReference<Connection> connection) {
    this.cacheKey = cacheKey;
    this.value = value;
    this.connection = connection;
  }

  public CacheKey<T> getCacheKey() {
    return cacheKey;
  }

  public T getValue() {
    return value;
  }

  public WeakReference<Connection> getConnection() {
    return connection;
  }
}
