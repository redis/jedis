package redis.clients.jedis.csc;

class CacheEntry<T> {

  private final CacheKey<T> cacheKey;
  private final T value;
  private final CacheConnection connection;

  public CacheEntry(CacheKey<T> cacheKey, T value, CacheConnection connection) {
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

  public CacheConnection getConnection() {
    return connection;
  }
}
