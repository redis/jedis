package redis.clients.jedis.commands.unified;

import java.util.Collection;

import org.junit.runners.Parameterized;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.CommandsTestsParameters;

public abstract class UnifiedJedisCommandsTestBase {

  /**
   * Input data for parameterized tests. In principle all subclasses of this
   * class should be parameterized tests, to run with several version of RESP.
   */
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return CommandsTestsParameters.respVersions();
  }

  protected final RedisProtocol protocol;

  protected UnifiedJedis jedis;

  public UnifiedJedisCommandsTestBase(RedisProtocol protocol) {
    this.protocol = protocol;
  }
}
