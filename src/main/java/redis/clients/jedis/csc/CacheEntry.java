package redis.clients.jedis.csc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;

import redis.clients.jedis.Connection;
import redis.clients.jedis.annots.Internal;

@Internal
public class CacheEntry<T> {

  private final CacheKey<T> cacheKey;
  private final WeakReference<Connection> connection;
  private final byte[] bytes;

  public CacheEntry(CacheKey<T> cacheKey, T value, WeakReference<Connection> connection) {
    this.cacheKey = cacheKey;
    this.connection = connection;
    this.bytes = toBytes(value);
  }

  public CacheKey<T> getCacheKey() {
    return cacheKey;
  }

  public T getValue() {
    return toObject(bytes);
  }

  public WeakReference<Connection> getConnection() {
    return connection;
  }

  private static byte[] toBytes(Object object) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(object);
      oos.flush();
      oos.close();
      return baos.toByteArray();
    } catch (Exception e) {
      // TODO: handle this properly
      throw new RuntimeException(e);
    }
  }

  private T toObject(byte[] data) {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais)) {
      return (T) ois.readObject();
    } catch (Exception e) {
      // TODO: handle this properly
      throw new RuntimeException(e);
    }
  }
}
