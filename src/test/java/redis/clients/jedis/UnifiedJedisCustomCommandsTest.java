package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.mocked.unified.UnifiedJedisMockedTestBase;

/**
 * These tests are part of the mocked tests for {@link UnifiedJedis}, but, due to {@code protected}
 * visibility of some methods, they must reside in the same package as the tested class.
 */
public class UnifiedJedisCustomCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testSendCommandWithProtocolCommand() {
    ProtocolCommand cmd = mock(ProtocolCommand.class);
    CommandArguments commandArguments = mock(CommandArguments.class);

    when(commandObjects.commandArguments(cmd)).thenReturn(commandArguments);
    when(commandExecutor.executeCommand(any())).thenReturn("OK");

    Object result = jedis.sendCommand(cmd);

    ArgumentCaptor<CommandObject<Object>> argumentCaptor = ArgumentCaptor.forClass(CommandObject.class);
    verify(commandExecutor).executeCommand(argumentCaptor.capture());

    CommandObject<Object> commandObject = argumentCaptor.getValue();
    assertThat(commandObject.getArguments(), sameInstance(commandArguments));

    assertThat(result, equalTo("OK"));

    verify(commandObjects).commandArguments(cmd);
  }

  @Test
  public void testSendCommandWithProtocolCommandAndByteArrayArgs() {
    ProtocolCommand cmd = mock(ProtocolCommand.class);
    byte[][] args = { "arg1".getBytes(), "arg2".getBytes() };
    CommandArguments commandArguments = mock(CommandArguments.class);
    CommandArguments commandArgumentsWithArgs = mock(CommandArguments.class);
    when(commandArguments.addObjects((Object[]) args)).thenReturn(commandArgumentsWithArgs);

    when(commandObjects.commandArguments(cmd)).thenReturn(commandArguments);
    when(commandExecutor.executeCommand(any())).thenReturn("OK");

    Object result = jedis.sendCommand(cmd, args);

    ArgumentCaptor<CommandObject<Object>> argumentCaptor = ArgumentCaptor.forClass(CommandObject.class);
    verify(commandExecutor).executeCommand(argumentCaptor.capture());

    CommandObject<Object> commandObject = argumentCaptor.getValue();
    assertThat(commandObject.getArguments(), sameInstance(commandArgumentsWithArgs));

    assertThat(result, equalTo("OK"));

    verify(commandObjects).commandArguments(cmd);
  }

  @Test
  public void testSendBlockingCommandWithProtocolCommandAndByteArrayArgs() {
    ProtocolCommand cmd = mock(ProtocolCommand.class);
    byte[][] args = { "arg1".getBytes(), "arg2".getBytes() };
    CommandArguments commandArguments = mock(CommandArguments.class);

    CommandArguments commandArgumentsBlocking = mock(CommandArguments.class);
    CommandArguments commandArgumentsWithArgs = mock(CommandArguments.class);
    when(commandArgumentsWithArgs.blocking()).thenReturn(commandArgumentsBlocking);

    when(commandArguments.addObjects((Object[]) args)).thenReturn(commandArgumentsWithArgs);

    when(commandObjects.commandArguments(cmd)).thenReturn(commandArguments);
    when(commandExecutor.executeCommand(any())).thenReturn("OK");

    Object result = jedis.sendBlockingCommand(cmd, args);

    ArgumentCaptor<CommandObject<Object>> argumentCaptor = ArgumentCaptor.forClass(CommandObject.class);
    verify(commandExecutor).executeCommand(argumentCaptor.capture());

    CommandObject<Object> commandObject = argumentCaptor.getValue();
    assertThat(commandObject.getArguments(), sameInstance(commandArgumentsBlocking));

    assertThat(result, equalTo("OK"));

    verify(commandObjects).commandArguments(cmd);
  }

  @Test
  public void testSendCommandWithProtocolCommandAndStringArgs() {
    ProtocolCommand cmd = mock(ProtocolCommand.class);
    String[] args = { "arg1", "arg2" };
    CommandArguments commandArguments = mock(CommandArguments.class);
    CommandArguments commandArgumentsWithArgs = mock(CommandArguments.class);
    when(commandArguments.addObjects((Object[]) args)).thenReturn(commandArgumentsWithArgs);

    when(commandObjects.commandArguments(cmd)).thenReturn(commandArguments);
    when(commandExecutor.executeCommand(any())).thenReturn("OK");

    Object result = jedis.sendCommand(cmd, args);

    ArgumentCaptor<CommandObject<Object>> argumentCaptor = ArgumentCaptor.forClass(CommandObject.class);
    verify(commandExecutor).executeCommand(argumentCaptor.capture());

    CommandObject<Object> commandObject = argumentCaptor.getValue();
    assertThat(commandObject.getArguments(), sameInstance(commandArgumentsWithArgs));

    assertThat(result, equalTo("OK"));

    verify(commandObjects).commandArguments(cmd);
  }

  @Test
  public void testSendBlockingCommandWithProtocolCommandAndStringArgs() {
    ProtocolCommand cmd = mock(ProtocolCommand.class);
    String[] args = { "arg1", "arg2" };
    CommandArguments commandArguments = mock(CommandArguments.class);

    CommandArguments commandArgumentsBlocking = mock(CommandArguments.class);
    CommandArguments commandArgumentsWithArgs = mock(CommandArguments.class);
    when(commandArgumentsWithArgs.blocking()).thenReturn(commandArgumentsBlocking);

    when(commandArguments.addObjects((Object[]) args)).thenReturn(commandArgumentsWithArgs);

    when(commandObjects.commandArguments(cmd)).thenReturn(commandArguments);
    when(commandExecutor.executeCommand(any())).thenReturn("OK");

    Object result = jedis.sendBlockingCommand(cmd, args);

    ArgumentCaptor<CommandObject<Object>> argumentCaptor = ArgumentCaptor.forClass(CommandObject.class);
    verify(commandExecutor).executeCommand(argumentCaptor.capture());

    CommandObject<Object> commandObject = argumentCaptor.getValue();
    assertThat(commandObject.getArguments(), sameInstance(commandArgumentsBlocking));

    assertThat(result, equalTo("OK"));

    verify(commandObjects).commandArguments(cmd);
  }

  @Test
  public void testSendCommandWithSampleKeyProtocolCommandAndByteArrayArgs() {
    byte[] sampleKey = "key".getBytes();
    ProtocolCommand cmd = mock(ProtocolCommand.class);
    byte[][] args = { "arg1".getBytes(), "arg2".getBytes() };
    CommandArguments commandArguments = mock(CommandArguments.class);
    CommandArguments commandArgumentsWithArgs = mock(CommandArguments.class);
    CommandArguments commandArgumentsWithKey = mock(CommandArguments.class);

    when(commandArguments.addObjects((Object[]) args)).thenReturn(commandArgumentsWithArgs);
    when(commandArgumentsWithArgs.processKey(sampleKey)).thenReturn(commandArgumentsWithKey);

    when(commandObjects.commandArguments(cmd)).thenReturn(commandArguments);
    when(commandExecutor.executeCommand(any())).thenReturn("OK");

    Object result = jedis.sendCommand(sampleKey, cmd, args);

    ArgumentCaptor<CommandObject<Object>> argumentCaptor = ArgumentCaptor.forClass(CommandObject.class);
    verify(commandExecutor).executeCommand(argumentCaptor.capture());

    CommandObject<Object> commandObject = argumentCaptor.getValue();
    assertThat(commandObject.getArguments(), sameInstance(commandArgumentsWithKey));

    assertThat(result, equalTo("OK"));

    verify(commandObjects).commandArguments(cmd);
  }

  @Test
  public void testSendBlockingCommandWithSampleKeyProtocolCommandAndByteArrayArgs() {
    byte[] sampleKey = "key".getBytes();
    ProtocolCommand cmd = mock(ProtocolCommand.class);
    byte[][] args = { "arg1".getBytes(), "arg2".getBytes() };
    CommandArguments commandArguments = mock(CommandArguments.class);
    CommandArguments commandArgumentsWithArgs = mock(CommandArguments.class);
    CommandArguments commandArgumentsBlocking = mock(CommandArguments.class);
    CommandArguments commandArgumentsWithKey = mock(CommandArguments.class);

    when(commandArguments.addObjects((Object[]) args)).thenReturn(commandArgumentsWithArgs);
    when(commandArgumentsWithArgs.blocking()).thenReturn(commandArgumentsBlocking);
    when(commandArgumentsBlocking.processKey(sampleKey)).thenReturn(commandArgumentsWithKey);

    when(commandObjects.commandArguments(cmd)).thenReturn(commandArguments);
    when(commandExecutor.executeCommand(any())).thenReturn("OK");

    Object result = jedis.sendBlockingCommand(sampleKey, cmd, args);

    ArgumentCaptor<CommandObject<Object>> argumentCaptor = ArgumentCaptor.forClass(CommandObject.class);
    verify(commandExecutor).executeCommand(argumentCaptor.capture());

    CommandObject<Object> commandObject = argumentCaptor.getValue();
    assertThat(commandObject.getArguments(), sameInstance(commandArgumentsWithKey));

    assertThat(result, equalTo("OK"));

    verify(commandObjects).commandArguments(cmd);
  }

  @Test
  public void testSendCommandWithStringSampleKeyProtocolCommandAndStringArgs() {
    String sampleKey = "key";
    ProtocolCommand cmd = mock(ProtocolCommand.class);
    String[] args = { "arg1", "arg2" };
    CommandArguments commandArguments = mock(CommandArguments.class);
    CommandArguments commandArgumentsWithArgs = mock(CommandArguments.class);
    CommandArguments commandArgumentsWithKey = mock(CommandArguments.class);

    when(commandArguments.addObjects((Object[]) args)).thenReturn(commandArgumentsWithArgs);
    when(commandArgumentsWithArgs.processKey(sampleKey)).thenReturn(commandArgumentsWithKey);

    when(commandObjects.commandArguments(cmd)).thenReturn(commandArguments);
    when(commandExecutor.executeCommand(any())).thenReturn("OK");

    Object result = jedis.sendCommand(sampleKey, cmd, args);

    ArgumentCaptor<CommandObject<Object>> argumentCaptor = ArgumentCaptor.forClass(CommandObject.class);
    verify(commandExecutor).executeCommand(argumentCaptor.capture());

    CommandObject<Object> commandObject = argumentCaptor.getValue();
    assertThat(commandObject.getArguments(), sameInstance(commandArgumentsWithKey));

    assertThat(result, equalTo("OK"));

    verify(commandObjects).commandArguments(cmd);
  }

  @Test
  public void testSendBlockingCommandWithStringSampleKeyProtocolCommandAndStringArgs() {
    String sampleKey = "key";
    ProtocolCommand cmd = mock(ProtocolCommand.class);
    String[] args = { "arg1", "arg2" };
    CommandArguments commandArguments = mock(CommandArguments.class);
    CommandArguments commandArgumentsWithArgs = mock(CommandArguments.class);
    CommandArguments commandArgumentsBlocking = mock(CommandArguments.class);
    CommandArguments commandArgumentsWithKey = mock(CommandArguments.class);

    when(commandArguments.addObjects((Object[]) args)).thenReturn(commandArgumentsWithArgs);
    when(commandArgumentsWithArgs.blocking()).thenReturn(commandArgumentsBlocking);
    when(commandArgumentsBlocking.processKey(sampleKey)).thenReturn(commandArgumentsWithKey);

    when(commandObjects.commandArguments(cmd)).thenReturn(commandArguments);
    when(commandExecutor.executeCommand(any())).thenReturn("OK");

    Object result = jedis.sendBlockingCommand(sampleKey, cmd, args);

    ArgumentCaptor<CommandObject<Object>> argumentCaptor = ArgumentCaptor.forClass(CommandObject.class);
    verify(commandExecutor).executeCommand(argumentCaptor.capture());

    CommandObject<Object> commandObject = argumentCaptor.getValue();
    assertThat(commandObject.getArguments(), sameInstance(commandArgumentsWithKey));

    assertThat(result, equalTo("OK"));

    verify(commandObjects).commandArguments(cmd);
  }

}
