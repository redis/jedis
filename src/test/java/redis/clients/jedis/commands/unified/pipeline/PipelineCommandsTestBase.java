package redis.clients.jedis.commands.unified.pipeline;

import org.junit.After;
import org.junit.Before;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.commands.unified.pooled.PooledCommandsTestHelper;

public abstract class PipelineCommandsTestBase {

  protected static JedisPooled jedis;
  protected Pipeline pipe;

  public PipelineCommandsTestBase() {
  }

  @Before
  public void setUp() {
    PooledCommandsTestHelper.clearData();
    pipe = jedis.pipelined();
  }

  @After
  public void tearDown() {
    pipe.close();
  }
}
