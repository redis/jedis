package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class UnifiedJedisConnectionManagementCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testPing() {
    when(commandObjects.ping()).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn("foo");

    String result = jedis.ping();

    assertThat(result, equalTo("foo"));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).ping();
  }

}
