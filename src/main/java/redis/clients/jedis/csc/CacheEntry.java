package redis.clients.jedis.csc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;

import redis.clients.jedis.exceptions.JedisCacheException;

public class CacheEntry<T> {

  private final CacheKey<T> cacheKey;
  private final WeakReference<CacheConnection> connection;
  private final byte[] bytes;
  private final boolean inProgress;

  private CacheEntry(CacheKey<T> cacheKey, T value, CacheConnection connection, boolean inProgress) {
    this.cacheKey = cacheKey;
    this.connection = new WeakReference<>(connection);
    this.bytes = toBytes(value);
    this.inProgress = inProgress;
  }

  public CacheKey<T> getCacheKey() {
    return cacheKey;
  }

  public T getValue() {
    return toObject(bytes);
  }

  public CacheConnection getConnection() {
    return connection.get();
  }

  public boolean inProgress() {
    return inProgress;
  }

  private static byte[] toBytes(Object object) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(object);
      oos.flush();
      oos.close();
      return baos.toByteArray();
    } catch (IOException e) {
      throw new JedisCacheException("Failed to serialize object", e);
    }
  }

  private T toObject(byte[] data) {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais)) {
      return (T) ois.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new JedisCacheException("Failed to deserialize object", e);
    }
  }

  /**
   * Creates a new {@link CacheEntry} that is still being processed before being put into the cache.
   *
   * @param cacheKey The key of the cache entry
   * @param connection The connection that is currently processing the cache entry
   * @return A new CacheEntry
   */
  public static <T> CacheEntry<T> inProgress(CacheKey<T> cacheKey, CacheConnection connection) {
    return new CacheEntry<T>(cacheKey, null, connection, true);
  }

  /**
   * Creates a new {@link CacheEntry} that is ready to be put into the cache.
   *
   * @param cacheKey The key of the cache entry
   * @param connection The connection that is currently processing the cache entry
   * @param value The value of the cache entry
   * @return A new CacheEntry
   */
  public  static <T> CacheEntry<T> newCacheEntry(CacheKey<T>  cacheKey, CacheConnection connection, T value) {
    return new CacheEntry<T>(cacheKey, value, connection, false);
  }
}
