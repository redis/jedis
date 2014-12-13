package redis.clients.jedis.tests.commands.async;

import org.junit.Test;
import redis.clients.jedis.Client;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.tests.commands.async.util.CommandWithWaiting;

import java.util.ArrayList;
import java.util.List;

public class AsyncListCommandsTest extends AsyncJedisCommandTestBase {
  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
  final byte[] bcar = { 0x09, 0x0A, 0x0B, 0x0C };
  final byte[] bA = { 0x0A };
  final byte[] bB = { 0x0B };
  final byte[] bC = { 0x0C };
  final byte[] b1 = { 0x01 };
  final byte[] b2 = { 0x02 };
  final byte[] b3 = { 0x03 };
  final byte[] bhello = { 0x04, 0x02 };
  final byte[] bx = { 0x02, 0x04 };
  final byte[] bdst = { 0x11, 0x12, 0x13, 0x14 };

  @Test
  public void rpush() {
    asyncJedis.rpush(LONG_CALLBACK.withReset(), "foo", "bar");
    long size = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, size);

    asyncJedis.rpush(LONG_CALLBACK.withReset(), "foo", "foo");
    size = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(2, size);

    asyncJedis.rpush(LONG_CALLBACK.withReset(), "foo", "bar", "foo");
    size = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(4, size);

    // Binary
    asyncJedis.rpush(LONG_CALLBACK.withReset(), bfoo, bbar);
    size = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, size);

    asyncJedis.rpush(LONG_CALLBACK.withReset(), bfoo, bfoo);
    size = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(2, size);

    asyncJedis.rpush(LONG_CALLBACK.withReset(), bfoo, bbar, bfoo);
    size = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(4, size);
  }

  @Test
  public void lpush() {
    asyncJedis.lpush(LONG_CALLBACK.withReset(), "foo", "bar");
    long size = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, size);

    asyncJedis.lpush(LONG_CALLBACK.withReset(), "foo", "foo");
    size = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(2, size);

    asyncJedis.lpush(LONG_CALLBACK.withReset(), "foo", "bar", "foo");
    size = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(4, size);

    // Binary
    asyncJedis.lpush(LONG_CALLBACK.withReset(), bfoo, bbar);
    size = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, size);

    asyncJedis.lpush(LONG_CALLBACK.withReset(), bfoo, bfoo);
    size = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(2, size);

    asyncJedis.lpush(LONG_CALLBACK.withReset(), bfoo, bbar, bfoo);
    size = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(4, size);
  }

  @Test
  public void llen() {
    asyncJedis.llen(LONG_CALLBACK.withReset(), "foo");
    assertEquals(0, LONG_CALLBACK.getResponseWithWaiting(1000).intValue());

    CommandWithWaiting.lpush(asyncJedis, "foo", "bar");
    CommandWithWaiting.lpush(asyncJedis, "foo", "car");

    asyncJedis.llen(LONG_CALLBACK.withReset(), "foo");
    assertEquals(2, LONG_CALLBACK.getResponseWithWaiting(1000).intValue());

    // binary
    asyncJedis.llen(LONG_CALLBACK.withReset(), bfoo);
    assertEquals(0, LONG_CALLBACK.getResponseWithWaiting(1000).intValue());

    CommandWithWaiting.lpush(asyncJedis, bfoo, bbar);
    CommandWithWaiting.lpush(asyncJedis, bfoo, bcar);

    asyncJedis.llen(LONG_CALLBACK.withReset(), bfoo);
    assertEquals(2, LONG_CALLBACK.getResponseWithWaiting(1000).intValue());
  }

  @Test
  public void llenNotOnList() {
    try {
      CommandWithWaiting.set(asyncJedis, "foo", "bar");

      asyncJedis.llen(LONG_CALLBACK.withReset(), "foo");
      LONG_CALLBACK.getResponseWithWaiting(1000);
      fail("JedisDataException expected");
    } catch (final JedisDataException e) {
    }

    // Binary
    try {
      CommandWithWaiting.set(asyncJedis, bfoo, bbar);

      asyncJedis.llen(LONG_CALLBACK.withReset(), bfoo);
      LONG_CALLBACK.getResponseWithWaiting(1000);
      fail("JedisDataException expected");
    } catch (final JedisDataException e) {
    }

  }

  @Test
  public void lrange() {
    CommandWithWaiting.rpush(asyncJedis, "foo", "a");
    CommandWithWaiting.rpush(asyncJedis, "foo", "b");
    CommandWithWaiting.rpush(asyncJedis, "foo", "c");

    List<String> expected = new ArrayList<String>();
    expected.add("a");
    expected.add("b");
    expected.add("c");

    asyncJedis.lrange(STRING_LIST_CALLBACK.withReset(), "foo", 0, 2);
    List<String> range = STRING_LIST_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(expected, range);

    asyncJedis.lrange(STRING_LIST_CALLBACK.withReset(), "foo", 0, 20);
    range = STRING_LIST_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(expected, range);

    expected = new ArrayList<String>();
    expected.add("b");
    expected.add("c");

    asyncJedis.lrange(STRING_LIST_CALLBACK.withReset(), "foo", 1, 2);
    range = STRING_LIST_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(expected, range);

    expected = new ArrayList<String>();
    asyncJedis.lrange(STRING_LIST_CALLBACK.withReset(), "foo", 2, 1);
    range = STRING_LIST_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(expected, range);

    // Binary
    CommandWithWaiting.rpush(asyncJedis, bfoo, bA);
    CommandWithWaiting.rpush(asyncJedis, bfoo, bB);
    CommandWithWaiting.rpush(asyncJedis, bfoo, bC);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bA);
    bexpected.add(bB);
    bexpected.add(bC);

    asyncJedis.lrange(BYTE_ARRAY_LIST_CALLBACK.withReset(), bfoo, 0, 2);
    List<byte[]> brange = BYTE_ARRAY_LIST_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(bexpected, brange);

    asyncJedis.lrange(BYTE_ARRAY_LIST_CALLBACK.withReset(), bfoo, 0, 20);
    brange = BYTE_ARRAY_LIST_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(bexpected, brange);

    bexpected = new ArrayList<byte[]>();
    bexpected.add(bB);
    bexpected.add(bC);

    asyncJedis.lrange(BYTE_ARRAY_LIST_CALLBACK.withReset(), bfoo, 1, 2);
    brange = BYTE_ARRAY_LIST_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(bexpected, brange);

    bexpected = new ArrayList<byte[]>();
    asyncJedis.lrange(BYTE_ARRAY_LIST_CALLBACK.withReset(), bfoo, 2, 1);
    brange = BYTE_ARRAY_LIST_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(bexpected, brange);
  }

  @Test
  public void ltrim() {
    CommandWithWaiting.lpush(asyncJedis, "foo", "1");
    CommandWithWaiting.lpush(asyncJedis, "foo", "2");
    CommandWithWaiting.lpush(asyncJedis, "foo", "3");

    asyncJedis.ltrim(STRING_CALLBACK.withReset(), "foo", 0, 1);
    String status = STRING_CALLBACK.getResponseWithWaiting(1000);

    List<String> expected = new ArrayList<String>();
    expected.add("3");
    expected.add("2");

    assertEquals("OK", status);
    assertEquals(2, jedis.llen("foo").intValue());
    assertEquals(expected, jedis.lrange("foo", 0, 100));

    // Binary
    CommandWithWaiting.lpush(asyncJedis, bfoo, b1);
    CommandWithWaiting.lpush(asyncJedis, bfoo, b2);
    CommandWithWaiting.lpush(asyncJedis, bfoo, b3);

    asyncJedis.ltrim(STRING_CALLBACK.withReset(), bfoo, 0, 1);
    String bstatus = STRING_CALLBACK.getResponseWithWaiting(1000);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(b3);
    bexpected.add(b2);

    assertEquals("OK", bstatus);
    assertEquals(2, jedis.llen(bfoo).intValue());
    assertEquals(bexpected, jedis.lrange(bfoo, 0, 100));
  }

  @Test
  public void lset() {
    CommandWithWaiting.lpush(asyncJedis, "foo", "1");
    CommandWithWaiting.lpush(asyncJedis, "foo", "2");
    CommandWithWaiting.lpush(asyncJedis, "foo", "3");

    List<String> expected = new ArrayList<String>();
    expected.add("3");
    expected.add("bar");
    expected.add("1");

    asyncJedis.lset(STRING_CALLBACK.withReset(), "foo", 1, "bar");
    String status = STRING_CALLBACK.getResponseWithWaiting(1000);

    assertEquals("OK", status);
    assertEquals(expected, jedis.lrange("foo", 0, 100));

    // Binary
    CommandWithWaiting.lpush(asyncJedis, bfoo, b1);
    CommandWithWaiting.lpush(asyncJedis, bfoo, b2);
    CommandWithWaiting.lpush(asyncJedis, bfoo, b3);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(b3);
    bexpected.add(bbar);
    bexpected.add(b1);

    asyncJedis.lset(STRING_CALLBACK.withReset(), bfoo, 1, bbar);
    String bstatus = STRING_CALLBACK.getResponseWithWaiting(1000);

    assertEquals("OK", bstatus);
    assertEquals(bexpected, jedis.lrange(bfoo, 0, 100));
  }

  @Test
  public void lindex() {
    CommandWithWaiting.lpush(asyncJedis, "foo", "1");
    CommandWithWaiting.lpush(asyncJedis, "foo", "2");
    CommandWithWaiting.lpush(asyncJedis, "foo", "3");

    asyncJedis.lindex(STRING_CALLBACK.withReset(), "foo", 0);
    assertEquals("3", STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.lindex(STRING_CALLBACK.withReset(), "foo", 100);
    assertEquals(null, STRING_CALLBACK.getResponseWithWaiting(1000));

    // Binary
    CommandWithWaiting.lpush(asyncJedis, bfoo, b1);
    CommandWithWaiting.lpush(asyncJedis, bfoo, b2);
    CommandWithWaiting.lpush(asyncJedis, bfoo, b3);

    asyncJedis.lindex(BYTE_ARRAY_CALLBACK.withReset(), bfoo, 0);
    assertArrayEquals(b3, BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.lindex(BYTE_ARRAY_CALLBACK.withReset(), bfoo, 100);
    assertEquals(null, BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void lrem() {
    CommandWithWaiting.lpush(asyncJedis, "foo", "hello");
    CommandWithWaiting.lpush(asyncJedis, "foo", "hello");
    CommandWithWaiting.lpush(asyncJedis, "foo", "x");
    CommandWithWaiting.lpush(asyncJedis, "foo", "hello");
    CommandWithWaiting.lpush(asyncJedis, "foo", "c");
    CommandWithWaiting.lpush(asyncJedis, "foo", "b");
    CommandWithWaiting.lpush(asyncJedis, "foo", "a");

    asyncJedis.lrem(LONG_CALLBACK.withReset(), "foo", -2, "hello");
    long count = LONG_CALLBACK.getResponseWithWaiting(1000);

    List<String> expected = new ArrayList<String>();
    expected.add("a");
    expected.add("b");
    expected.add("c");
    expected.add("hello");
    expected.add("x");

    assertEquals(2, count);

    // blocking API
    assertEquals(expected, jedis.lrange("foo", 0, 1000));

    asyncJedis.lrem(LONG_CALLBACK.withReset(), "bar", 100, "foo");
    assertEquals(0, LONG_CALLBACK.getResponseWithWaiting(1000).intValue());

    // Binary
    CommandWithWaiting.lpush(asyncJedis, bfoo, bhello);
    CommandWithWaiting.lpush(asyncJedis, bfoo, bhello);
    CommandWithWaiting.lpush(asyncJedis, bfoo, bx);
    CommandWithWaiting.lpush(asyncJedis, bfoo, bhello);
    CommandWithWaiting.lpush(asyncJedis, bfoo, bC);
    CommandWithWaiting.lpush(asyncJedis, bfoo, bB);
    CommandWithWaiting.lpush(asyncJedis, bfoo, bA);

    asyncJedis.lrem(LONG_CALLBACK.withReset(), bfoo, -2, bhello);
    long bcount = LONG_CALLBACK.getResponseWithWaiting(1000);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bA);
    bexpected.add(bB);
    bexpected.add(bC);
    bexpected.add(bhello);
    bexpected.add(bx);

    assertEquals(2, bcount);
    assertEquals(bexpected, jedis.lrange(bfoo, 0, 1000));

    asyncJedis.lrem(LONG_CALLBACK.withReset(), bbar, 100, bfoo);
    assertEquals(0, LONG_CALLBACK.getResponseWithWaiting(1000).intValue());
  }

  @Test
  public void lpop() {
    CommandWithWaiting.rpush(asyncJedis, "foo", "a");
    CommandWithWaiting.rpush(asyncJedis, "foo", "b");
    CommandWithWaiting.rpush(asyncJedis, "foo", "c");

    asyncJedis.lpop(STRING_CALLBACK.withReset(), "foo");
    String element = STRING_CALLBACK.getResponseWithWaiting(1000);
    assertEquals("a", element);

    List<String> expected = new ArrayList<String>();
    expected.add("b");
    expected.add("c");

    assertEquals(expected, jedis.lrange("foo", 0, 1000));

    CommandWithWaiting.lpop(asyncJedis, "foo");
    CommandWithWaiting.lpop(asyncJedis, "foo");

    asyncJedis.lpop(STRING_CALLBACK.withReset(), "foo");
    element = STRING_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(null, element);

    // Binary
    CommandWithWaiting.rpush(asyncJedis, bfoo, bA);
    CommandWithWaiting.rpush(asyncJedis, bfoo, bB);
    CommandWithWaiting.rpush(asyncJedis, bfoo, bC);

    asyncJedis.lpop(BYTE_ARRAY_CALLBACK.withReset(), bfoo);
    byte[] belement = BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000);
    assertArrayEquals(bA, belement);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bB);
    bexpected.add(bC);

    assertEquals(bexpected, jedis.lrange(bfoo, 0, 1000));

    CommandWithWaiting.lpop(asyncJedis, bfoo);
    CommandWithWaiting.lpop(asyncJedis, bfoo);

    asyncJedis.lpop(BYTE_ARRAY_CALLBACK.withReset(), bfoo);
    belement = BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(null, belement);
  }

  @Test
  public void rpop() {
    CommandWithWaiting.rpush(asyncJedis, "foo", "a");
    CommandWithWaiting.rpush(asyncJedis, "foo", "b");
    CommandWithWaiting.rpush(asyncJedis, "foo", "c");

    asyncJedis.rpop(STRING_CALLBACK.withReset(), "foo");
    String element = STRING_CALLBACK.getResponseWithWaiting(1000);
    assertEquals("c", element);

    List<String> expected = new ArrayList<String>();
    expected.add("a");
    expected.add("b");

    assertEquals(expected, jedis.lrange("foo", 0, 1000));

    CommandWithWaiting.rpop(asyncJedis, "foo");
    CommandWithWaiting.rpop(asyncJedis, "foo");

    asyncJedis.rpop(STRING_CALLBACK.withReset(), "foo");
    element = STRING_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(null, element);

    // Binary
    CommandWithWaiting.rpush(asyncJedis, bfoo, bA);
    CommandWithWaiting.rpush(asyncJedis, bfoo, bB);
    CommandWithWaiting.rpush(asyncJedis, bfoo, bC);

    asyncJedis.rpop(BYTE_ARRAY_CALLBACK.withReset(), bfoo);
    byte[] belement = BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000);
    assertArrayEquals(bC, belement);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bA);
    bexpected.add(bB);

    assertEquals(bexpected, jedis.lrange(bfoo, 0, 1000));

    CommandWithWaiting.rpop(asyncJedis, bfoo);
    CommandWithWaiting.rpop(asyncJedis, bfoo);

    asyncJedis.rpop(BYTE_ARRAY_CALLBACK.withReset(), bfoo);
    belement = BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(null, belement);
  }

  @Test
  public void rpoplpush() {
    CommandWithWaiting.rpush(asyncJedis, "foo", "a");
    CommandWithWaiting.rpush(asyncJedis, "foo", "b");
    CommandWithWaiting.rpush(asyncJedis, "foo", "c");

    CommandWithWaiting.rpush(asyncJedis, "dst", "foo");
    CommandWithWaiting.rpush(asyncJedis, "dst", "bar");

    asyncJedis.rpoplpush(STRING_CALLBACK.withReset(), "foo", "dst");
    String element = STRING_CALLBACK.getResponseWithWaiting(1000);

    assertEquals("c", element);

    List<String> srcExpected = new ArrayList<String>();
    srcExpected.add("a");
    srcExpected.add("b");

    List<String> dstExpected = new ArrayList<String>();
    dstExpected.add("c");
    dstExpected.add("foo");
    dstExpected.add("bar");

    asyncJedis.lrange(STRING_LIST_CALLBACK.withReset(), "foo", 0, 1000);
    assertEquals(srcExpected, STRING_LIST_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.lrange(STRING_LIST_CALLBACK.withReset(), "dst", 0, 1000);
    assertEquals(dstExpected, STRING_LIST_CALLBACK.getResponseWithWaiting(1000));

    // Binary
    CommandWithWaiting.rpush(asyncJedis, bfoo, bA);
    CommandWithWaiting.rpush(asyncJedis, bfoo, bB);
    CommandWithWaiting.rpush(asyncJedis, bfoo, bC);

    CommandWithWaiting.rpush(asyncJedis, bdst, bfoo);
    CommandWithWaiting.rpush(asyncJedis, bdst, bbar);

    asyncJedis.rpoplpush(BYTE_ARRAY_CALLBACK.withReset(), bfoo, bdst);
    byte[] belement = BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000);
    assertArrayEquals(bC, belement);

    List<byte[]> bsrcExpected = new ArrayList<byte[]>();
    bsrcExpected.add(bA);
    bsrcExpected.add(bB);

    List<byte[]> bdstExpected = new ArrayList<byte[]>();
    bdstExpected.add(bC);
    bdstExpected.add(bfoo);
    bdstExpected.add(bbar);

    asyncJedis.lrange(BYTE_ARRAY_LIST_CALLBACK.withReset(), bfoo, 0, 1000);
    assertEquals(bsrcExpected, BYTE_ARRAY_LIST_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.lrange(BYTE_ARRAY_LIST_CALLBACK.withReset(), bdst, 0, 1000);
    assertEquals(bdstExpected, BYTE_ARRAY_LIST_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void lpushx() {
    asyncJedis.lpushx(LONG_CALLBACK.withReset(), "foo", "bar");
    long status = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(0, status);

    CommandWithWaiting.lpush(asyncJedis, "foo", "a");

    asyncJedis.lpushx(LONG_CALLBACK.withReset(), "foo", "b");
    status = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(2, status);

    // Binary
    asyncJedis.lpushx(LONG_CALLBACK.withReset(), bfoo, bbar);
    long bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(0, bstatus);

    CommandWithWaiting.lpush(asyncJedis, bfoo, bA);

    asyncJedis.lpushx(LONG_CALLBACK.withReset(), bfoo, bB);
    bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(2, bstatus);
  }

  @Test
  public void rpushx() {
    asyncJedis.rpushx(LONG_CALLBACK.withReset(), "foo", "bar");
    long status = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(0, status);

    CommandWithWaiting.lpush(asyncJedis, "foo", "a");

    asyncJedis.rpushx(LONG_CALLBACK.withReset(), "foo", "b");
    status = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(2, status);

    // Binary
    asyncJedis.lpushx(LONG_CALLBACK.withReset(), bfoo, bbar);
    long bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(0, bstatus);

    CommandWithWaiting.lpush(asyncJedis, bfoo, bA);

    asyncJedis.rpushx(LONG_CALLBACK.withReset(), bfoo, bB);
    bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(2, bstatus);
  }

  @Test
  public void linsert() {
    asyncJedis.linsert(LONG_CALLBACK.withReset(), "foo", Client.LIST_POSITION.BEFORE, "bar", "car");
    long status = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(0, status);

    CommandWithWaiting.lpush(asyncJedis, "foo", "a");

    asyncJedis.linsert(LONG_CALLBACK.withReset(), "foo", Client.LIST_POSITION.AFTER, "a", "b");
    status = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(2, status);

    List<String> actual = jedis.lrange("foo", 0, 100);
    List<String> expected = new ArrayList<String>();
    expected.add("a");
    expected.add("b");

    assertEquals(expected, actual);

    asyncJedis.linsert(LONG_CALLBACK.withReset(), "foo", Client.LIST_POSITION.BEFORE, "bar", "car");
    status = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(-1, status);

    // Binary
    asyncJedis.linsert(LONG_CALLBACK.withReset(), bfoo, Client.LIST_POSITION.BEFORE, bbar, bcar);
    long bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(0, bstatus);

    CommandWithWaiting.lpush(asyncJedis, bfoo, bA);

    asyncJedis.linsert(LONG_CALLBACK.withReset(), bfoo, Client.LIST_POSITION.AFTER, bA, bB);
    bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(2, bstatus);

    List<byte[]> bactual = jedis.lrange(bfoo, 0, 100);
    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bA);
    bexpected.add(bB);

    assertEquals(bexpected, bactual);

    asyncJedis.linsert(LONG_CALLBACK.withReset(), bfoo, Client.LIST_POSITION.BEFORE, bbar, bcar);
    bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(-1, bstatus);
  }

}
