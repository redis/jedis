package redis.clients.jedis.commands.unified;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.BaseRedisClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.CommandsTestsParameters;

public abstract class UnifiedJedisCommandsTestBase {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Input data for parameterized tests. In principle all subclasses of this
   * class should be parameterized tests, to run with several versions of RESP.
   *
   * @see CommandsTestsParameters#respVersions()
   */
  protected final RedisProtocol protocol;

  /**
   * The {@link BaseRedisClient} instance to use for the tests. This is the subject-under-test.
   */
  protected BaseRedisClient jedis;


  protected Class<? extends BaseRedisClient> clientType;

  /**
   * The RESP protocol is to be injected by the subclasses, usually via JUnit
   * parameterized tests, because most of the subclassed tests are meant to be
   * executed against multiple RESP versions. For the special cases where a single
   * RESP version is relevant, we still force the subclass to be explicit and
   * call this constructor.
   *
   * @param protocol The RESP protocol to use during the tests.
   */
  public UnifiedJedisCommandsTestBase(RedisProtocol protocol, Class<? extends BaseRedisClient> clientType) {
    this.protocol = protocol;
    this.clientType = clientType;
  }
}
