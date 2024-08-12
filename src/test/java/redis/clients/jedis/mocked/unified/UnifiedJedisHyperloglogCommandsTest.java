package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class UnifiedJedisHyperloglogCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testPfadd() {
    String key = "hll";
    String[] elements = { "element1", "element2" };
    long expectedAdded = 1L;

    when(commandObjects.pfadd(key, elements)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedAdded);

    long result = jedis.pfadd(key, elements);

    assertThat(result, equalTo(expectedAdded));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).pfadd(key, elements);
  }

  @Test
  public void testPfaddBinary() {
    byte[] key = "hll".getBytes();
    byte[][] elements = { "element1".getBytes(), "element2".getBytes() };
    long expectedAdded = 1L;

    when(commandObjects.pfadd(key, elements)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedAdded);

    long result = jedis.pfadd(key, elements);

    assertThat(result, equalTo(expectedAdded));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).pfadd(key, elements);
  }

  @Test
  public void testPfcount() {
    String key = "hll";
    long expectedCount = 42L;

    when(commandObjects.pfcount(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCount);

    long result = jedis.pfcount(key);

    assertThat(result, equalTo(expectedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).pfcount(key);
  }

  @Test
  public void testPfcountBinary() {
    byte[] key = "hll".getBytes();
    long expectedCount = 42L;

    when(commandObjects.pfcount(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCount);

    long result = jedis.pfcount(key);

    assertThat(result, equalTo(expectedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).pfcount(key);
  }

  @Test
  public void testPfcountMultipleKeys() {
    String[] keys = { "hll1", "hll2" };
    long expectedCount = 84L;

    when(commandObjects.pfcount(keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCount);

    long result = jedis.pfcount(keys);

    assertThat(result, equalTo(expectedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).pfcount(keys);
  }

  @Test
  public void testPfcountMultipleKeysBinary() {
    byte[][] keys = { "hll1".getBytes(), "hll2".getBytes() };
    long expectedCount = 84L;

    when(commandObjects.pfcount(keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCount);

    long result = jedis.pfcount(keys);

    assertThat(result, equalTo(expectedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).pfcount(keys);
  }

  @Test
  public void testPfmergeString() {
    String destkey = "hll1";
    String[] sourcekeys = { "hll2", "hll3" };
    String expectedStatus = "OK";
    when(commandObjects.pfmerge(destkey, sourcekeys)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedStatus);

    String result = jedis.pfmerge(destkey, sourcekeys);

    assertThat(result, equalTo(expectedStatus));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).pfmerge(destkey, sourcekeys);
  }

  @Test
  public void testPfmergeBinary() {
    byte[] destkey = "hll1".getBytes();
    byte[][] sourcekeys = { "hll2".getBytes(), "hll3".getBytes() };
    String expectedStatus = "OK";

    when(commandObjects.pfmerge(destkey, sourcekeys)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedStatus);

    String result = jedis.pfmerge(destkey, sourcekeys);

    assertThat(result, equalTo(expectedStatus));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).pfmerge(destkey, sourcekeys);
  }

}
