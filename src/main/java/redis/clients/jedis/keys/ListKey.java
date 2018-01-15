package redis.clients.jedis.keys;

/**
 *  A type-safe way of using the Redis keys for different target data structures.
 */
public class ListKey extends TypeSafeKey {
  public ListKey(String key) {
    super(key);
  }
}
