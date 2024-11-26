package redis.clients.jedis.commands.unified.pipeline;

import java.util.Collection;

import redis.clients.jedis.util.EnabledOnCommandRule;
import redis.clients.jedis.util.RedisVersionRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runners.Parameterized;
import redis.clients.jedis.*;
import redis.clients.jedis.commands.CommandsTestsParameters;
import redis.clients.jedis.commands.unified.pooled.PooledCommandsTestHelper;

public abstract class PipelineCommandsTestBase {

  /**
   * Input data for parameterized tests. In principle all subclasses of this
   * class should be parameterized tests, to run with several versions of RESP.
   *
   * @see CommandsTestsParameters#respVersions()
   */
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return CommandsTestsParameters.respVersions();
  }

  protected JedisPooled jedis;
  protected Pipeline pipe;

  protected final RedisProtocol protocol;

  @Rule
  public RedisVersionRule versionRule = new RedisVersionRule(PooledCommandsTestHelper.nodeInfo);
  @Rule
  public EnabledOnCommandRule enabledOnCommandRule = new EnabledOnCommandRule(PooledCommandsTestHelper.nodeInfo);
  /**
   * The RESP protocol is to be injected by the subclasses, usually via JUnit
   * parameterized tests, because most of the subclassed tests are meant to be
   * executed against multiple RESP versions. For the special cases where a single
   * RESP version is relevant, we still force the subclass to be explicit and
   * call this constructor.
   *
   * @param protocol The RESP protocol to use during the tests.
   */
  public PipelineCommandsTestBase(RedisProtocol protocol) {
    this.protocol = protocol;
  }

  @Before
  public void setUp() {
    jedis = PooledCommandsTestHelper.getPooled(protocol);
    PooledCommandsTestHelper.clearData();
    pipe = jedis.pipelined();
  }

  @After
  public void tearDown() {
    pipe.close();
    jedis.close();
  }
}
