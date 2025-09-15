package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.*;
import redis.clients.jedis.commands.CommandsTestsParameters;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.executors.DefaultCommandExecutor;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.providers.PooledConnectionProvider;

/**
 * Base class for CommandObjects tests. The tests are parameterized to run with
 * several versions of RESP. The idea is to test commands at this low level, using
 * a simple executor. Higher level concepts like {@link redis.clients.jedis.UnifiedJedis},
 * or {@link redis.clients.jedis.PipeliningBase} can be tested separately with mocks.
 * <p>
 * This class provides the basic setup, except the {@link HostAndPort} for connecting
 * to a running Redis server. That one is provided by abstract subclasses, depending
 * on if a Redis Stack server is needed, or a standalone suffices.
 * <p>
 * In principle all subclasses of this class should be parameterized tests,
 * to run with several versions of RESP. {@link CommandsTestsParameters#respVersions}
 */
@Tag("integration")
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public abstract class CommandObjectsTestBase {

  /**
   * RESP protocol used in the tests. Injected from subclasses.
   */
  protected final RedisProtocol protocol;

  /**
   * Host and port of the Redis server to connect to. Injected from subclasses.
   */
  protected final EndpointConfig endpoint;

  /**
   * The {@link CommandObjects} to use for the tests. This is the subject-under-test.
   */
  protected final CommandObjects commandObjects;

  /**
   * A {@link CommandExecutor} that can execute commands against the running Redis server.
   * Not exposed to subclasses, which should use a convenience method instead.
   */
  private CommandExecutor commandExecutor;

  public CommandObjectsTestBase(RedisProtocol protocol, EndpointConfig endpoint) {
    this.protocol = protocol;
    this.endpoint = endpoint;
    commandObjects = new CommandObjects();
    commandObjects.setProtocol(protocol);
  }

  @BeforeEach
  public void setUp() {
    // Configure a default command executor.
    DefaultJedisClientConfig clientConfig = endpoint.getClientConfigBuilder().protocol(protocol)
        .build();

    ConnectionProvider connectionProvider = new PooledConnectionProvider(endpoint.getHostAndPort(),
        clientConfig);

    commandExecutor = new DefaultCommandExecutor(connectionProvider);

    // Cleanup before each test.
    assertThat(exec(commandObjects.flushAll()), equalTo("OK"));
  }

  /**
   * Convenience method for subclasses, for running any {@link CommandObject}.
   */
  protected <T> T exec(CommandObject<T> commandObject) {
    return commandExecutor.executeCommand(commandObject);
  }

}
