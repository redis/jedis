package redis.clients.jedis.bloom;

import java.util.List;

import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.exceptions.JedisDataException;

/**
 * Builders specific to RedisBloom replies.
 * @since 8.1
 */
public final class RedisBloomBuilderFactory {

  /**
   * Counts returned by {@code CMS.INCRBY}. The server rejects an increment per item, with an error
   * element in place of its count, and the command then fails as a whole: the first such error is
   * thrown instead of being left in the list as {@link BuilderFactory#LONG_LIST} would.
   */
  public static final Builder<List<Long>> CMS_INCRBY_LONG_LIST = new Builder<List<Long>>() {
    @Override
    @SuppressWarnings("unchecked")
    public List<Long> build(Object data) {
      if (null == data) {
        return null;
      }
      for (Object element : (List<Object>) data) {
        if (element instanceof JedisDataException) {
          throw (JedisDataException) element;
        }
      }
      return (List<Long>) data;
    }

    @Override
    public String toString() {
      return "List<Long>";
    }
  };

  private RedisBloomBuilderFactory() {
    throw new InstantiationError("Must not instantiate this class");
  }
}
