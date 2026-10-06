package redis.clients.jedis.csc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import redis.clients.jedis.exceptions.JedisCacheException;

public class CacheEntry<T> {

  private final CacheKey<T> cacheKey;
  private final WeakReference<CacheConnection> connection;
  private final byte[] bytes;

  private enum Format {
    JAVA, JSON_OBJECT, JSON_ARRAY
  }

  private final Format format;

  public CacheEntry(CacheKey<T> cacheKey, T value, CacheConnection connection) {
    this.cacheKey = cacheKey;
    this.connection = new WeakReference<>(connection);

    if (value instanceof JSONObject) {
      this.format = Format.JSON_OBJECT;
    } else if (value instanceof JSONArray) {
      this.format = Format.JSON_ARRAY;
    } else {
      this.format = Format.JAVA;
    }

    this.bytes = toBytes(value, this.format);
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

  private static byte[] toBytes(Object object, Format format) {
    // JSON text normalizes numbers; serialize their Java values to retain types and scale.
    if (format == Format.JSON_OBJECT) {
      object = ((JSONObject) object).toMap();
    } else if (format == Format.JSON_ARRAY) {
      object = ((JSONArray) object).toList();
    }
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
      Object object = ois.readObject();
      return (T) (format == Format.JAVA ? object : restoreJson(object));
    } catch (IOException | ClassNotFoundException e) {
      throw new JedisCacheException("Failed to deserialize object", e);
    }
  }

  private static Object restoreJson(Object value) {
    if (value == null) {
      return JSONObject.NULL;
    }
    if (value instanceof Map) {
      JSONObject object = new JSONObject();
      // JSONObject(Map) omits null-valued keys, so restore each entry explicitly.
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
        object.put((String) entry.getKey(), restoreJson(entry.getValue()));
      }
      return object;
    }
    if (value instanceof List) {
      JSONArray array = new JSONArray();
      for (Object element : (List<?>) value) {
        array.put(restoreJson(element));
      }
      return array;
    }
    return value;
  }
}
