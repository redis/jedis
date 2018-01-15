package redis.clients.jedis.keys;

/**
 *  A type-safe way of using the Redis keys for different target data structures.
 */
public class HashKey extends TypeSafeKey {
  public HashKey(String key) {
    super(key);
  }
}
