package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class UnifiedJedisServerManagementCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testConfigGet() {
    String pattern = "slowlog-max-len";
    Map<String, String> expected = Collections.singletonMap("slowlog-max-len", "128");

    when(commandObjects.configGet(pattern)).thenReturn(mapStringStringCommandObject);
    when(commandExecutor.executeCommand(mapStringStringCommandObject)).thenReturn(expected);

    Map<String, String> result = jedis.configGet(pattern);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(mapStringStringCommandObject);
    verify(commandObjects).configGet(pattern);
  }

  @Test
  public void testConfigGetMultiplePatterns() {
    String[] patterns = new String[] { "slowlog-max-len", "slowlog-log-slower-than" };
    Map<String, String> expected = new HashMap<>();
    expected.put("slowlog-max-len", "128");
    expected.put("slowlog-log-slower-than", "10000");

    when(commandObjects.configGet(patterns)).thenReturn(mapStringStringCommandObject);
    when(commandExecutor.executeCommand(mapStringStringCommandObject)).thenReturn(expected);

    Map<String, String> result = jedis.configGet(patterns);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(mapStringStringCommandObject);
    verify(commandObjects).configGet(patterns);
  }

  @Test
  public void testConfigSet() {
    String parameter = "param";
    String value = "value";

    when(commandObjects.configSet(parameter, value)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn("OK");

    String result = jedis.configSet(parameter, value);

    assertThat(result, equalTo("OK"));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).configSet(parameter, value);
  }

  @Test
  public void testDbSize() {
    long expectedSize = 42L;

    when(commandObjects.dbSize()).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedSize);

    long result = jedis.dbSize();

    assertThat(result, equalTo(expectedSize));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).dbSize();
  }

  @Test
  public void testFlushAll() {
    when(commandObjects.flushAll()).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn("OK");

    String result = jedis.flushAll();

    assertThat(result, equalTo("OK"));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).flushAll();
  }

  @Test
  public void testFlushDB() {
    when(commandObjects.flushDB()).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn("OK");

    String result = jedis.flushDB();

    assertThat(result, equalTo("OK"));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).flushDB();
  }

  @Test
  public void testMemoryUsage() {
    String key = "key1";
    Long expectedMemoryUsage = 1024L;

    when(commandObjects.memoryUsage(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedMemoryUsage);

    Long result = jedis.memoryUsage(key);

    assertThat(result, equalTo(expectedMemoryUsage));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).memoryUsage(key);
  }

  @Test
  public void testMemoryUsageWithSamples() {
    String key = "key1";
    int samples = 5;
    Long expectedMemoryUsage = 2048L;

    when(commandObjects.memoryUsage(key, samples)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedMemoryUsage);

    Long result = jedis.memoryUsage(key, samples);

    assertThat(result, equalTo(expectedMemoryUsage));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).memoryUsage(key, samples);
  }

  @Test
  public void testMemoryUsageBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };
    Long expectedMemoryUsage = 512L;

    when(commandObjects.memoryUsage(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedMemoryUsage);

    Long result = jedis.memoryUsage(key);

    assertThat(result, equalTo(expectedMemoryUsage));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).memoryUsage(key);
  }

  @Test
  public void testMemoryUsageWithSamplesBinary() {
    byte[] key = new byte[]{ 1, 2, 3 };
    int samples = 5;
    Long expectedMemoryUsage = 1024L;

    when(commandObjects.memoryUsage(key, samples)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedMemoryUsage);

    Long result = jedis.memoryUsage(key, samples);

    assertThat(result, equalTo(expectedMemoryUsage));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).memoryUsage(key, samples);
  }

  @Test
  public void testSlowlogReset() {
    String expectedResponse = "OK";

    when(commandObjects.slowlogReset()).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.slowlogReset();

    assertThat(result, equalTo(expectedResponse));
    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).slowlogReset();
  }

}
