package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

public class UnifiedJedisConnectionManagementCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testPing() {
    when(commandObjects.ping()).thenReturn(stringCommandObject);

    String result = jedis.ping();

    assertThat(result, equalTo("foo"));

    verify(commandObjects).ping();
  }

}
