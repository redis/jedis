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

  public CacheEntry(CacheKey<T> cacheKey, T value, CacheConnection connection) {
    this.cacheKey = cacheKey;
    this.connection = new WeakReference<>(connection);
    this.bytes = toBytes(value);
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
}
