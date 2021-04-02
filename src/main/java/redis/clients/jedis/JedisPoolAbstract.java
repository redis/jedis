package redis.clients.jedis;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.util.Pool;

/**
 * @deprecated This class will be removed in future. If you are directly manipulating this class,
 *             you are suggested to change your code to use {@link Pool Pool&lt;Jedis&gt;} instead.
 */
@Deprecated
public class JedisPoolAbstract extends Pool<Jedis> {

  /**
   * Using this constructor means you have to set and initialize the internalPool yourself.
   * @deprecated This constructor will be removed in future.
   */
  @Deprecated
  public JedisPoolAbstract() {
    super();
  }

  public JedisPoolAbstract(GenericObjectPoolConfig<Jedis> poolConfig,
      PooledObjectFactory<Jedis> factory) {
    super(poolConfig, factory);
  }
}
