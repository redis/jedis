package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class UnifiedJedisPubSubCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testPublishWithStringChannelAndMessage() {
    String channel = "myChannel";
    String message = "Hello, World!";
    long expectedPublishCount = 10L;

    when(commandObjects.publish(channel, message)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedPublishCount);

    long result = jedis.publish(channel, message);

    assertThat(result, equalTo(expectedPublishCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).publish(channel, message);
  }

  @Test
  public void testPublishWithByteArrayChannelAndMessage() {
    byte[] channel = "myChannel".getBytes();
    byte[] message = "Hello, World!".getBytes();
    long expectedPublishCount = 10L;

    when(commandObjects.publish(channel, message)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedPublishCount);

    long result = jedis.publish(channel, message);

    assertThat(result, equalTo(expectedPublishCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).publish(channel, message);
  }

}
