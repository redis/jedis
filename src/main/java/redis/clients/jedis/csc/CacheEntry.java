package redis.clients.jedis.csc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.Objects;

import redis.clients.jedis.Builder;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisCacheException;

public class CacheEntry<T> {

  private final CacheKey<T> cacheKey;
  private final WeakReference<CacheConnection> connection;
  private final Object reply;
  private final Builder<T> builder;

  /**
   * Create an entry from an already-built value. This compatibility constructor retains Java
   * serialization; the connection's protocol-reply path does not require serializable values.
   * @param cacheKey cache key
   * @param value an already-built, serializable value
   * @param connection connection that owns the entry
   */
  public CacheEntry(CacheKey<T> cacheKey, T value, CacheConnection connection) {
    this(cacheKey, toBytes(value), new Builder<T>() {
      @Override
      public T build(Object data) {
        return toObject((byte[]) data);
      }
    }, connection);
  }

  CacheEntry(CacheKey<T> cacheKey, Object reply, Builder<T> builder, CacheConnection connection) {
    this.cacheKey = cacheKey;
    this.connection = new WeakReference<>(connection);
    this.reply = Protocol.copyReply(reply);
    this.builder = Objects.requireNonNull(builder);
  }

  public CacheKey<T> getCacheKey() {
    return cacheKey;
  }

  public T getValue() {
    // Some builders return their input or consume it while building the result.
    return builder.build(Protocol.copyReply(reply));
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

  @SuppressWarnings("unchecked")
  private static <T> T toObject(byte[] data) {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais)) {
      return (T) ois.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new JedisCacheException("Failed to deserialize object", e);
    }
  }
}
