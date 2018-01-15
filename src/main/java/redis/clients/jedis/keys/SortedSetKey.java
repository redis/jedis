package redis.clients.jedis.keys;

/**
 *  A type-safe way of using the Redis keys for different target data structures.
 */
public class SortedSetKey extends TypeSafeKey {
  public SortedSetKey(String key) {
    super(key);
  }
}
