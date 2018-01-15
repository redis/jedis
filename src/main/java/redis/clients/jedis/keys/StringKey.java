package redis.clients.jedis.keys;

/**
 *  A type-safe way of using the Redis keys for different target data structures.
 */
public class StringKey extends TypeSafeKey {
  public StringKey(String key) {
    super(key);
  }
}
