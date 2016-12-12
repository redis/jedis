package redis.clients.jedis;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import redis.clients.jedis.exceptions.JedisConnectionException;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * @author Louis Morgan
 */
@RunWith(MockitoJUnitRunner.class)
public class JedisClusterCommandTest {

  @Mock
  private Jedis connection;

  @Mock
  private JedisClusterConnectionHandler connectionHandler;

  @Test
  public void onConnectionExceptionClusterRenewsSlotCacheAndThenRetries() {
    when(connectionHandler.getConnectionFromSlot(anyInt())).thenReturn(connection);
    when(connection.get("foo")).thenThrow(new JedisConnectionException("Command failed"));

    try {
      new JedisClusterCommand<String>(connectionHandler, 3) {
        @Override
        public String execute(Jedis connection) {
          return connection.get("foo");
        }
      }.run("foo");
    } catch (JedisConnectionException expected) {
      // We expect this to be thrown
    }

    // maxAttempts is 3, so we expect 2 attempts to get the key, followed by a call to
    // renewSlotCache(), followed by a further attempt to get the key
    InOrder inOrder = Mockito.inOrder(connection, connectionHandler);
    inOrder.verify(connection).get("foo");
    inOrder.verify(connection).close();
    inOrder.verify(connection).get("foo");
    inOrder.verify(connection).close();
    inOrder.verify(connectionHandler).renewSlotCache();
    inOrder.verify(connection).get("foo");
    inOrder.verify(connection).close();
  }
}
