package redis.clients.jedis.commands.unified.pipeline;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.EnvCondition;
import redis.clients.jedis.util.RedisVersionCondition;
import redis.clients.jedis.*;
import redis.clients.jedis.commands.CommandsTestsParameters;
import redis.clients.jedis.commands.unified.client.RedisClientCommandsTestHelper;

@Tag("integration")
public abstract class PipelineCommandsTestBase {

  protected RedisClient client;
  protected Pipeline pipe;
  /**
   * Input data for parameterized tests. In principle all subclasses of this class should be
   * parameterized tests, to run with several versions of RESP.
   * @see CommandsTestsParameters#respVersions()
   */
  protected final RedisProtocol protocol;

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(
      RedisClientCommandsTestHelper::getEndpointConfig);
  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(
      RedisClientCommandsTestHelper::getEndpointConfig);
  @RegisterExtension
  public EnvCondition conditionalOnEnvCondition = new EnvCondition();

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

  @BeforeEach
  public void setUp() {
    client = RedisClientCommandsTestHelper.getClient(protocol);
    RedisClientCommandsTestHelper.clearData();
    pipe = client.pipelined();
  }

  @AfterEach
  public void tearDown() {
    pipe.close();
    client.close();
  }
}
