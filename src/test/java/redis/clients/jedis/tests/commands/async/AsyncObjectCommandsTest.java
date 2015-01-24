package redis.clients.jedis.tests.commands.async;

import org.junit.Test;
import redis.clients.jedis.tests.commands.async.util.CommandWithWaiting;
import redis.clients.util.SafeEncoder;

public class AsyncObjectCommandsTest extends AsyncJedisCommandTestBase {

  private String key = "mylist";
  private byte[] binaryKey = SafeEncoder.encode(key);

  @Test
  public void objectRefcount() throws InterruptedException {
    CommandWithWaiting.lpush(asyncJedis, key, "hello world");

    asyncJedis.objectRefcount(LONG_CALLBACK.withReset(), key);
    assertEquals(new Long(1L), LONG_CALLBACK.getResponseWithWaiting(1000));

    // Binary
    asyncJedis.objectRefcount(LONG_CALLBACK.withReset(), binaryKey);
    assertEquals(new Long(1L), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void objectEncoding() {
    CommandWithWaiting.lpush(asyncJedis, key, "hello world");

    asyncJedis.objectEncoding(STRING_CALLBACK.withReset(), key);
    assertEquals("quicklist", STRING_CALLBACK.getResponseWithWaiting(1000));

    // Binary
    CommandWithWaiting.lpush(asyncJedis, binaryKey, "hello world".getBytes());

    asyncJedis.objectEncoding(BYTE_ARRAY_CALLBACK.withReset(), binaryKey);
    assertArrayEquals("quicklist".getBytes(), BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void objectIdletime() throws InterruptedException {
    CommandWithWaiting.lpush(asyncJedis, key, "hello world");

    asyncJedis.objectIdletime(LONG_CALLBACK.withReset(), key);
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    // Binary
    asyncJedis.objectIdletime(LONG_CALLBACK.withReset(), binaryKey);
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

}