package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import redis.clients.jedis.gears.TFunctionListParams;
import redis.clients.jedis.gears.TFunctionLoadParams;
import redis.clients.jedis.gears.resps.GearsLibraryInfo;

@Ignore
public class UnifiedJedisTriggersAndFunctionsCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testTFunctionCall() {
    String library = "mylib";
    String function = "myfunc";
    List<String> keys = Arrays.asList("key1", "key2");
    List<String> args = Arrays.asList("arg1", "arg2");
    Object expectedResponse = "result";

    when(commandObjects.tFunctionCall(library, function, keys, args)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedResponse);

    Object result = jedis.tFunctionCall(library, function, keys, args);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).tFunctionCall(library, function, keys, args);
  }

  @Test
  public void testTFunctionCallAsync() {
    String library = "mylib";
    String function = "myfunc";
    List<String> keys = Arrays.asList("key1", "key2");
    List<String> args = Arrays.asList("arg1", "arg2");
    Object expectedResponse = "result";

    when(commandObjects.tFunctionCallAsync(library, function, keys, args)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedResponse);

    Object result = jedis.tFunctionCallAsync(library, function, keys, args);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).tFunctionCallAsync(library, function, keys, args);
  }

  @Test
  public void testTFunctionDelete() {
    String libraryName = "mylib";
    String expectedResponse = "OK";

    when(commandObjects.tFunctionDelete(libraryName)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.tFunctionDelete(libraryName);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).tFunctionDelete(libraryName);
  }

  @Test
  public void testTFunctionList() {
    TFunctionListParams params = new TFunctionListParams();
    List<GearsLibraryInfo> expectedResponse = new ArrayList<>();
    expectedResponse.add(mock(GearsLibraryInfo.class));

    when(commandObjects.tFunctionList(params)).thenReturn(listGearsLibraryInfoCommandObject);
    when(commandExecutor.executeCommand(listGearsLibraryInfoCommandObject)).thenReturn(expectedResponse);

    List<GearsLibraryInfo> result = jedis.tFunctionList(params);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(listGearsLibraryInfoCommandObject);
    verify(commandObjects).tFunctionList(params);
  }

  @Test
  public void testTFunctionLoad() {
    String libraryCode = "function code";
    TFunctionLoadParams params = new TFunctionLoadParams();
    String expectedResponse = "OK";

    when(commandObjects.tFunctionLoad(libraryCode, params)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.tFunctionLoad(libraryCode, params);

    assertThat(result, sameInstance(expectedResponse));
    
    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).tFunctionLoad(libraryCode, params);
  }

}
