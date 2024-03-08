package redis.clients.jedis.commands.unified.pipeline;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.CommandsTestsParameters;
import redis.clients.jedis.commands.unified.pooled.PooledCommandsTestHelper;

public abstract class PipelineCommandsTestBase {

  /**
   * Input data for parameterized tests. In principle all subclasses of this
   * class should be parameterized tests, to run with several version of RESP.
   */
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return CommandsTestsParameters.respVersions();
  }

  protected JedisPooled jedis;
  protected Pipeline pipe;

  protected final RedisProtocol protocol;

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
