package redis.clients.jedis.bloom;

import java.util.ArrayList;
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
   * element in place of its count, and by definition the command then fails as a whole: the first
   * error handed over by {@link BuilderFactory#LONG_WITH_ERROR_LIST} is thrown.
   */
  public static final Builder<List<Long>> CMS_INCRBY_LONG_LIST = new Builder<List<Long>>() {
    @Override
    public List<Long> build(Object data) {
      List<Object> elements = BuilderFactory.LONG_WITH_ERROR_LIST.build(data);
      if (elements == null) {
        return null;
      }
      List<Long> counts = new ArrayList<>(elements.size());
      for (Object element : elements) {
        if (element instanceof JedisDataException) {
          throw (JedisDataException) element;
        }
        counts.add((Long) element);
      }
      return counts;
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
