package redis.clients.jedis.keys;

/**
 *  A type-safe way of using the Redis keys for different target data structures.
 */
public class NumberKey extends TypeSafeKey {
  public NumberKey(String key) {
    super(key);
  }
}
