package redis.clients.jedis.mocked.pipeline;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.mockito.Mock;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.PipeliningBase;
import redis.clients.jedis.Response;
import redis.clients.jedis.mocked.MockedCommandObjectsTestBase;

/**
 * Base class for unit tests for {@link PipeliningBase}, using Mockito. Given that {@link PipeliningBase}
 * is, essentially, only requesting commands from a {@link CommandObjects} instance and sending them
 * to its subclasses, and given that it has many methods, using mocks is the most convenient and
 * reliable way to completely test it.
 */
public abstract class PipeliningBaseMockedTestBase extends MockedCommandObjectsTestBase {

  /**
   * A concrete implementation of {@link PipeliningBase} that collects all commands
   * in a list (so that asserts can be run on the content of the list), and always returns a
   * predefined response (so that the response can be asserted).
   */
  private static class TestPipeliningBase extends PipeliningBase {

    private final Response<?> predefinedResponse;
    private final List<CommandObject<?>> commands;

    public TestPipeliningBase(CommandObjects commandObjects,
                              Response<?> predefinedResponse,
                              List<CommandObject<?>> commands) {
      super(commandObjects);
      this.predefinedResponse = predefinedResponse;
      this.commands = commands;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> Response<T> appendCommand(CommandObject<T> commandObject) {
      // Collect the command in the list.
      commands.add(commandObject);
      // Return a well known response, that can be asserted in the test cases.
      return (Response<T>) predefinedResponse;
    }
  }

  /**
   * {@link PipeliningBase} under-test. Given that it is an abstract class, an in-place implementation
   * is used, that collects commands in a list.
   */
  protected PipeliningBase pipeliningBase;

  /**
   * Accumulates commands sent by the {@link PipeliningBase} under-test to its subclass.
   */
  protected final List<CommandObject<?>> commands = new ArrayList<>();

  /**
   * {@link CommandObjects} instance used by the {@link PipeliningBase} under-test. Depending on
   * the test case, it is trained to return one of the mock {@link CommandObject} instances below.
   */
  @Mock
  protected CommandObjects commandObjects;

  /**
   * Mock {@link Response} that is returned by {@link PipeliningBase} from each method.
   */
  @Mock
  protected Response<?> predefinedResponse;

  @Before
  public void setUp() {
    pipeliningBase = new TestPipeliningBase(commandObjects, predefinedResponse, commands);
  }
}
