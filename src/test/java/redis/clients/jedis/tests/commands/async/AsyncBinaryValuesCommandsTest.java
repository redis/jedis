package redis.clients.jedis.tests.commands.async;

import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.tests.commands.async.util.CommandWithWaiting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AsyncBinaryValuesCommandsTest extends AsyncJedisCommandTestBase {
  byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
  byte[] bxx = { 0x78, 0x78 };
  byte[] bnx = { 0x6E, 0x78 };
  byte[] bex = { 0x65, 0x78 };
  byte[] bpx = { 0x70, 0x78 };
  long expireSeconds = 2;
  long expireMillis = expireSeconds * 1000;
  byte[] binaryValue;

  @Before
  public void startUp() {
    StringBuilder sb = new StringBuilder();

    for (int n = 0; n < 1000; n++) {
      sb.append("A");
    }

    binaryValue = sb.toString().getBytes();
  }

  @Test
  public void setAndGet() {
    asyncJedis.set(STRING_CALLBACK.withReset(), bfoo, binaryValue);
    assertTrue(Keyword.OK.name().equalsIgnoreCase(STRING_CALLBACK.getResponseWithWaiting(1000)));

    asyncJedis.get(BYTE_ARRAY_CALLBACK.withReset(), bfoo);
    assertTrue(Arrays.equals(binaryValue, BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000)));

    asyncJedis.get(BYTE_ARRAY_CALLBACK.withReset(), bbar);
    assertNull(BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void setNxExAndGet() {
    asyncJedis.set(STRING_CALLBACK.withReset(), bfoo, binaryValue, bnx, bex, expireSeconds);
    assertTrue(Keyword.OK.name().equalsIgnoreCase(STRING_CALLBACK.getResponseWithWaiting(1000)));

    asyncJedis.get(BYTE_ARRAY_CALLBACK.withReset(), bfoo);
    assertTrue(Arrays.equals(binaryValue, BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000)));

    asyncJedis.get(BYTE_ARRAY_CALLBACK.withReset(), bbar);
    assertNull(BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000));
    assertTrue(BYTE_ARRAY_CALLBACK.isComplete());
  }

  @Test
  public void setIfNotExistAndGet() {
    CommandWithWaiting.set(asyncJedis, bfoo, binaryValue);

    // nx should fail if value exists
    asyncJedis.set(STRING_CALLBACK.withReset(), bfoo, binaryValue, bnx, bex, expireSeconds);
    assertNull(STRING_CALLBACK.getResponseWithWaiting(1000));

    // borrowed blocking API
    byte[] value = jedis.get(bfoo);
    assertTrue(Arrays.equals(binaryValue, value));

    assertNull(jedis.get(bbar));
  }

  @Test
  public void setIfExistAndGet() {
    CommandWithWaiting.set(asyncJedis, bfoo, binaryValue);

    // nx should fail if value exists
    asyncJedis.set(STRING_CALLBACK.withReset(), bfoo, binaryValue, bxx, bex, expireSeconds);
    assertEquals(Keyword.OK.name(), STRING_CALLBACK.getResponseWithWaiting(1000));

    // borrowed blocking API
    byte[] value = jedis.get(bfoo);
    assertTrue(Arrays.equals(binaryValue, value));

    assertNull(jedis.get(bbar));
  }

  @Test
  public void setFailIfNotExistAndGet() {
    // xx should fail if value does NOT exists
    asyncJedis.set(STRING_CALLBACK.withReset(), bfoo, binaryValue, bxx, bex, expireSeconds);
    assertNull(STRING_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void setAndExpireMillis() {
    asyncJedis.set(STRING_CALLBACK.withReset(), bfoo, binaryValue, bnx, bpx, expireMillis);
    assertEquals(Keyword.OK.name(), STRING_CALLBACK.getResponseWithWaiting(1000));

    // borrowed blocking API
    long ttl = jedis.ttl(bfoo);
    assertTrue(ttl > 0 && ttl <= expireSeconds);
  }

  @Test
  public void setAndExpire() {
    asyncJedis.set(STRING_CALLBACK.withReset(), bfoo, binaryValue, bnx, bex, expireSeconds);
    assertEquals(Keyword.OK.name(), STRING_CALLBACK.getResponseWithWaiting(1000));

    // borrowed blocking API
    long ttl = jedis.ttl(bfoo);
    assertTrue(ttl > 0 && ttl <= expireSeconds);
  }

  @Test
  public void getSet() {
    asyncJedis.getSet(BYTE_ARRAY_CALLBACK.withReset(), bfoo, binaryValue);
    assertNull(BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000));

    // borrowed blocking API
    assertTrue(Arrays.equals(binaryValue, jedis.get(bfoo)));
  }

  @Test
  public void mget() {
    asyncJedis.mget(BYTE_ARRAY_LIST_CALLBACK.withReset(), bfoo, bbar);

    List<byte[]> expected = new ArrayList<byte[]>();
    expected.add(null);
    expected.add(null);

    assertEquals(expected, BYTE_ARRAY_LIST_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, bfoo, binaryValue);

    expected = new ArrayList<byte[]>();
    expected.add(binaryValue);
    expected.add(null);

    asyncJedis.mget(BYTE_ARRAY_LIST_CALLBACK.withReset(), bfoo, bbar);
    assertEquals(expected, BYTE_ARRAY_LIST_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, bbar, bfoo);

    expected = new ArrayList<byte[]>();
    expected.add(binaryValue);
    expected.add(bfoo);

    asyncJedis.mget(BYTE_ARRAY_LIST_CALLBACK.withReset(), bfoo, bbar);
    assertEquals(expected, BYTE_ARRAY_LIST_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void setnx() {
    asyncJedis.setnx(LONG_CALLBACK.withReset(), bfoo, binaryValue);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    // borrowed blocking API
    assertTrue(Arrays.equals(binaryValue, jedis.get(bfoo)));

    asyncJedis.setnx(LONG_CALLBACK.withReset(), bfoo, binaryValue);
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    // borrowed blocking API
    assertTrue(Arrays.equals(binaryValue, jedis.get(bfoo)));
  }

  @Test
  public void setex() {
    asyncJedis.setex(STRING_CALLBACK.withReset(), bfoo, 20, binaryValue);
    assertEquals(Keyword.OK.name(), STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.ttl(LONG_CALLBACK.withReset(), bfoo);
    long ttl = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertTrue(ttl > 0 && ttl <= 20);
  }

  @Test
  public void mset() {
    asyncJedis.mset(STRING_CALLBACK.withReset(), bfoo, binaryValue, bbar, bfoo);
    assertEquals(Keyword.OK.name(), STRING_CALLBACK.getResponseWithWaiting(1000));

    // borrowed blocking API
    assertTrue(Arrays.equals(binaryValue, jedis.get(bfoo)));
    assertTrue(Arrays.equals(bfoo, jedis.get(bbar)));
  }

  @Test
  public void msetnx() {
    asyncJedis.msetnx(LONG_CALLBACK.withReset(), bfoo, binaryValue, bbar, bfoo);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    // borrowed blocking API
    assertTrue(Arrays.equals(binaryValue, jedis.get(bfoo)));
    assertTrue(Arrays.equals(bfoo, jedis.get(bbar)));

    asyncJedis.msetnx(LONG_CALLBACK.withReset(), bfoo, bbar, "bar2".getBytes(), "foo2".getBytes());
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    // borrowed blocking API
    assertTrue(Arrays.equals(binaryValue, jedis.get(bfoo)));
    assertTrue(Arrays.equals(bfoo, jedis.get(bbar)));
  }

  @Test(expected = JedisDataException.class)
  public void incrWrongValue() {
    CommandWithWaiting.set(asyncJedis, bfoo, binaryValue);

    asyncJedis.incr(LONG_CALLBACK.withReset(), bfoo);
    LONG_CALLBACK.getResponseWithWaiting(1000);
    fail("JedisDataException should be occurred");
  }

  @Test
  public void incr() {
    asyncJedis.incr(LONG_CALLBACK.withReset(), bfoo);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.incr(LONG_CALLBACK.withReset(), bfoo);
    assertEquals(new Long(2), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test(expected = JedisDataException.class)
  public void incrByWrongValue() {
    CommandWithWaiting.set(asyncJedis, bfoo, binaryValue);

    asyncJedis.incrBy(LONG_CALLBACK.withReset(), bfoo, 2);
    LONG_CALLBACK.getResponseWithWaiting(1000);
    fail("JedisDataException should be occurred");
  }

  @Test
  public void incrBy() {
    asyncJedis.incrBy(LONG_CALLBACK.withReset(), bfoo, 2);
    assertEquals(new Long(2), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.incrBy(LONG_CALLBACK.withReset(), bfoo, 2);
    assertEquals(new Long(4), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test(expected = JedisDataException.class)
  public void decrWrongValue() {
    CommandWithWaiting.set(asyncJedis, bfoo, binaryValue);

    asyncJedis.decr(LONG_CALLBACK.withReset(), bfoo);
    LONG_CALLBACK.getResponseWithWaiting(1000);
    fail("JedisDataException should be occurred");
  }

  @Test
  public void decr() {
    asyncJedis.decr(LONG_CALLBACK.withReset(), bfoo);
    assertEquals(new Long(-1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.decr(LONG_CALLBACK.withReset(), bfoo);
    assertEquals(new Long(-2), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test(expected = JedisDataException.class)
  public void decrByWrongValue() {
    CommandWithWaiting.set(asyncJedis, bfoo, binaryValue);

    asyncJedis.decrBy(LONG_CALLBACK.withReset(), bfoo, -2);
    LONG_CALLBACK.getResponseWithWaiting(1000);
    fail("JedisDataException should be occurred");
  }

  @Test
  public void decrBy() {
    asyncJedis.decrBy(LONG_CALLBACK.withReset(), bfoo, 2);
    assertEquals(new Long(-2), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.decrBy(LONG_CALLBACK.withReset(), bfoo, 2);
    assertEquals(new Long(-4), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void append() {
    byte[] first512 = new byte[512];
    System.arraycopy(binaryValue, 0, first512, 0, 512);

    asyncJedis.append(LONG_CALLBACK.withReset(), bfoo, first512);
    assertEquals(new Long(512), LONG_CALLBACK.getResponseWithWaiting(1000));

    // borrowed from blocking API
    assertTrue(Arrays.equals(first512, jedis.get(bfoo)));

    byte[] rest = new byte[binaryValue.length - 512];
    System.arraycopy(binaryValue, 512, rest, 0, binaryValue.length - 512);

    asyncJedis.append(LONG_CALLBACK.withReset(), bfoo, rest);
    assertEquals(new Long(binaryValue.length), LONG_CALLBACK.getResponseWithWaiting(1000));

    // borrowed from blocking API
    assertTrue(Arrays.equals(binaryValue, jedis.get(bfoo)));
  }

  @Test
  public void substr() {
    CommandWithWaiting.set(asyncJedis, bfoo, binaryValue);

    byte[] first512 = new byte[512];
    System.arraycopy(binaryValue, 0, first512, 0, 512);

    asyncJedis.substr(BYTE_ARRAY_CALLBACK.withReset(), bfoo, 0, 511);
    byte[] rfirst512 = BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000);
    assertTrue(Arrays.equals(first512, rfirst512));

    byte[] last512 = new byte[512];
    System.arraycopy(binaryValue, binaryValue.length - 512, last512, 0, 512);

    asyncJedis.substr(BYTE_ARRAY_CALLBACK.withReset(), bfoo, -512, -1);
    assertTrue(Arrays.equals(last512, BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000)));

    asyncJedis.substr(BYTE_ARRAY_CALLBACK.withReset(), bfoo, 0, -1);
    assertTrue(Arrays.equals(binaryValue, BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000)));

    asyncJedis.substr(BYTE_ARRAY_CALLBACK.withReset(), bfoo, binaryValue.length - 512, 100000);
    assertTrue(Arrays.equals(last512, BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000)));
  }

  @Test
  public void strlen() {
    CommandWithWaiting.set(asyncJedis, bfoo, binaryValue);

    asyncJedis.strlen(LONG_CALLBACK.withReset(), bfoo);
    assertEquals(binaryValue.length, LONG_CALLBACK.getResponseWithWaiting(1000).longValue());
  }
}