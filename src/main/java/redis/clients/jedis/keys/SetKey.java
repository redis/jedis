package redis.clients.jedis.keys;

/**
 *  A type-safe way of using the Redis keys for different target data structures.
 */
public class SetKey extends TypeSafeKey {
  public SetKey(String key) {
    super(key);
  }
}
