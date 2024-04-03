package redis.clients.jedis.mocked.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import org.junit.Test;
import redis.clients.jedis.Response;

public class PipeliningBaseHyperloglogCommandsTest extends PipeliningBaseMockedTestBase {

  @Test
  public void testPfadd() {
    when(commandObjects.pfadd("key", "element1", "element2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pfadd("key", "element1", "element2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPfaddBinary() {
    byte[] key = "hll".getBytes();
    byte[] element1 = "element1".getBytes();
    byte[] element2 = "element2".getBytes();

    when(commandObjects.pfadd(key, element1, element2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pfadd(key, element1, element2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPfcount() {
    when(commandObjects.pfcount("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pfcount("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPfcountBinary() {
    byte[] key = "hll".getBytes();

    when(commandObjects.pfcount(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pfcount(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPfcountMultipleKeys() {
    when(commandObjects.pfcount("key1", "key2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pfcount("key1", "key2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPfcountMultipleKeysBinary() {
    byte[] key1 = "hll1".getBytes();
    byte[] key2 = "hll2".getBytes();

    when(commandObjects.pfcount(key1, key2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pfcount(key1, key2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPfmerge() {
    when(commandObjects.pfmerge("destkey", "sourcekey1", "sourcekey2")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.pfmerge("destkey", "sourcekey1", "sourcekey2");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPfmergeBinary() {
    byte[] destkey = "hll_dest".getBytes();
    byte[] sourcekey1 = "hll1".getBytes();
    byte[] sourcekey2 = "hll2".getBytes();

    when(commandObjects.pfmerge(destkey, sourcekey1, sourcekey2)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.pfmerge(destkey, sourcekey1, sourcekey2);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

}
