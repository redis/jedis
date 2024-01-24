package redis.clients.jedis;

import java.nio.ByteBuffer;
import java.util.List;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.SafeEncoder;

public abstract class ClientSideCache {

  protected ClientSideCache() { }

  public abstract void clear();

  protected abstract void remove(ByteBuffer key);

  protected abstract void put(ByteBuffer key, Object value);

  protected abstract Object get(ByteBuffer key);

  final void invalidateKeys(List list) {
    if (list == null) {
      clear();
    } else {
      list.forEach(this::invalidateKey);
    }
  }

  private void invalidateKey(Object key) {
    if (key instanceof byte[]) {
      remove(convertKey((byte[]) key));
    } else {
      throw new JedisException("" + key.getClass().getSimpleName() + " is not supported. Value: " + String.valueOf(key));
    }
  }

  final void set(Object key, Object value) {
    put(makeKey(key), value);
  }

  final <T> T get(Object key) {
    return (T) get(makeKey(key));
  }

  private ByteBuffer makeKey(Object key) {
    if (key instanceof byte[]) {
      return convertKey((byte[]) key);
    } else {
      return convertKey(SafeEncoder.encode(String.valueOf(key)));
    }
  }

  private static ByteBuffer convertKey(byte[] b) {
    return ByteBuffer.wrap(b);
  }
}
