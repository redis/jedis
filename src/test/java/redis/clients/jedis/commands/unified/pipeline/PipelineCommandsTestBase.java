package redis.clients.jedis.commands.unified.pipeline;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.RedisVersionCondition;
import redis.clients.jedis.*;
import redis.clients.jedis.commands.CommandsTestsParameters;
import redis.clients.jedis.commands.unified.pooled.PooledCommandsTestHelper;

import java.util.stream.Stream;

public abstract class PipelineCommandsTestBase {

  private static final Logger logger = LoggerFactory.getLogger(PipelineCommandsTestBase.class);

  protected BaseRedisClient jedis;
  protected Pipeline pipe;
  /**
   * Input data for parameterized tests. In principle all subclasses of this class should be
   * parameterized tests, to run with several versions of RESP.
   * @see CommandsTestsParameters#respVersions()
   */
  protected final RedisProtocol protocol;
  protected final Class<? extends BaseRedisClient> clientType;

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(PooledCommandsTestHelper.nodeInfo);
  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(PooledCommandsTestHelper.nodeInfo);
  /**
   * The RESP protocol is to be injected by the subclasses, usually via JUnit
   * parameterized tests, because most of the subclassed tests are meant to be
   * executed against multiple RESP versions. For the special cases where a single
   * RESP version is relevant, we still force the subclass to be explicit and
   * call this constructor.
   *
   * @param protocol The RESP protocol to use during the tests.
   * @param clientType The client type to use during the tests.
   */
  public PipelineCommandsTestBase(RedisProtocol protocol, Class<? extends BaseRedisClient> clientType) {
    this.protocol = protocol;
    this.clientType = clientType;
  }

  @BeforeEach
  public void setUp() {
    jedis = PooledCommandsTestHelper.getCleanClient(protocol, clientType);
    pipe = (Pipeline) jedis.pipelined();
  }

  @AfterEach
  public void tearDown() {
    pipe.close();
    try {
      jedis.close();
    } catch (Exception e) {
      logger.warn("Exception while closing jedis", e);
    }
    PooledCommandsTestHelper.clearData();
  }

}
