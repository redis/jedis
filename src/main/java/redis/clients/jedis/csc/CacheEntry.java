package redis.clients.jedis.csc;

import redis.clients.jedis.Connection;
import redis.clients.jedis.annots.Internal;

@Internal
public class CacheEntry<T> {

  private final CacheKey<T> cacheKey;
  private final T value;
  private final Connection connection;

  public CacheEntry(CacheKey<T> cacheKey, T value, Connection connection) {
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

  public Connection getConnection() {
    return connection;
  }
}
