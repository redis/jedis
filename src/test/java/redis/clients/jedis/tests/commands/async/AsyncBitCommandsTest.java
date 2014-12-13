package redis.clients.jedis.tests.commands.async;

import org.junit.Test;
import redis.clients.jedis.BitOP;
import redis.clients.jedis.tests.commands.async.util.CommandWithWaiting;
import redis.clients.jedis.tests.commands.async.util.DoNothingCallback;

public class AsyncBitCommandsTest extends AsyncJedisCommandTestBase {
  @Test
  public void setAndgetbit() {
    asyncJedis.setbit(BOOLEAN_CALLBACK.withReset(), "foo", 0, true);
    assertFalse(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.getbit(BOOLEAN_CALLBACK.withReset(), "foo", 0);
    assertTrue(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));

    // binary
    asyncJedis.setbit(BOOLEAN_CALLBACK.withReset(), "bfoo".getBytes(), 0, "1".getBytes());
    assertFalse(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.getbit(BOOLEAN_CALLBACK.withReset(), "bfoo".getBytes(), 0);
    assertTrue(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void setAndgetrange() {
    CommandWithWaiting.set(asyncJedis, "key1", "Hello World");

    asyncJedis.setrange(LONG_CALLBACK.withReset(), "key1", 6, "Jedis");
    assertEquals(new Long(11), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.get(STRING_CALLBACK.withReset(), "key1");
    assertEquals("Hello Jedis", STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.getrange(STRING_CALLBACK.withReset(), "key1", 0, 4);
    assertEquals("Hello", STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.getrange(STRING_CALLBACK.withReset(), "key1", 6, 11);
    assertEquals("Jedis", STRING_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void bitCount() {
    CommandWithWaiting.setbit(asyncJedis, "foo", 16, true);
    CommandWithWaiting.setbit(asyncJedis, "foo", 24, true);
    CommandWithWaiting.setbit(asyncJedis, "foo", 40, true);
    CommandWithWaiting.setbit(asyncJedis, "foo", 56, true);

    asyncJedis.bitcount(LONG_CALLBACK.withReset(), "foo");
    assertEquals(new Long(4), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.bitcount(LONG_CALLBACK.withReset(), "foo", 2L, 5L);
    assertEquals(new Long(3), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void bitOp() {
    CommandWithWaiting.set(asyncJedis, "key1", "\u0060");
    CommandWithWaiting.set(asyncJedis, "key2", "\u0044");

    asyncJedis.bitop(new DoNothingCallback<Long>(), BitOP.AND, "resultAnd", "key1", "key2");

    asyncJedis.get(STRING_CALLBACK.withReset(), "resultAnd");
    assertEquals("\u0040", STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.bitop(new DoNothingCallback<Long>(), BitOP.OR, "resultOr", "key1", "key2");

    asyncJedis.get(STRING_CALLBACK.withReset(), "resultOr");
    assertEquals("\u0064", STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.bitop(new DoNothingCallback<Long>(), BitOP.XOR, "resultXor", "key1", "key2");

    asyncJedis.get(STRING_CALLBACK.withReset(), "resultXor");
    assertEquals("\u0024", STRING_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void bitOpNot() {
    CommandWithWaiting.setbit(asyncJedis, "key", 0, true);
    CommandWithWaiting.setbit(asyncJedis, "key", 4, true);

    asyncJedis.bitop(new DoNothingCallback<Long>(), BitOP.NOT, "resultNot", "key");

    asyncJedis.get(STRING_CALLBACK.withReset(), "resultNot");
    assertEquals("\u0077", STRING_CALLBACK.getResponseWithWaiting(1000));
  }
}
