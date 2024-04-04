package redis.clients.jedis.mocked.unified;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.mocked.MockedCommandObjectsTestBase;
import redis.clients.jedis.providers.ConnectionProvider;

/**
 * Base class for {@link UnifiedJedis} mocked unit tests. Exposes a {@link UnifiedJedis} instance that
 * uses mocked executors, providers and command objects, which can be asserted upon.
 */
public abstract class UnifiedJedisMockedTestBase extends MockedCommandObjectsTestBase {

  /**
   * The {@link UnifiedJedis} instance under-test.
   */
  protected UnifiedJedis jedis;

  /**
   * Mocked {@link CommandExecutor} instance. Instead of going to the wire and exchanging data
   * with a real Redis server, this instance is trained to returned pre-packaged response data,
   * depending on what is being tested.
   */
  @Mock
  protected CommandExecutor commandExecutor;

  /**
   * Mocked {@link ConnectionProvider}. This is not really used in tests, except in some very
   * specific test cases.
   */
  @Mock
  protected ConnectionProvider connectionProvider;

  /**
   * {@link CommandObjects} instance used by the {@link UnifiedJedis} under-test. Depending on
   * the test case, it is trained to return one of the mock {@link CommandObject} instances inherited
   * from the superclass.
   */
  @Mock
  protected CommandObjects commandObjects;

  @Before
  public void setUp() {
    jedis = new UnifiedJedis(commandExecutor, connectionProvider, commandObjects);
  }

  @After
  public void tearDown() {
    // We want to be accurate about our mocks, hence we verify no more interactions here.
    // This might mean that some methods need to verify their interactions in a more verbose way,
    // but overall the benefit should be greater than the cost.
    verify(connectionProvider).getConnection();
    verifyNoMoreInteractions(connectionProvider);
    verifyNoMoreInteractions(commandExecutor);
    verifyNoMoreInteractions(commandObjects);
  }

}
