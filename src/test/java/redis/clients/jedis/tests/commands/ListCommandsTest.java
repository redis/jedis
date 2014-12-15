package redis.clients.jedis.tests.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisDataException;

public class ListCommandsTest extends JedisCommandTestBase {
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
    long size = jedis.rpush("foo", "bar");
    assertEquals(1, size);
    size = jedis.rpush("foo", "foo");
    assertEquals(2, size);
    size = jedis.rpush("foo", "bar", "foo");
    assertEquals(4, size);

    // Binary
    long bsize = jedis.rpush(bfoo, bbar);
    assertEquals(1, bsize);
    bsize = jedis.rpush(bfoo, bfoo);
    assertEquals(2, bsize);
    bsize = jedis.rpush(bfoo, bbar, bfoo);
    assertEquals(4, bsize);

  }

  @Test
  public void lpush() {
    long size = jedis.lpush("foo", "bar");
    assertEquals(1, size);
    size = jedis.lpush("foo", "foo");
    assertEquals(2, size);
    size = jedis.lpush("foo", "bar", "foo");
    assertEquals(4, size);

    // Binary
    long bsize = jedis.lpush(bfoo, bbar);
    assertEquals(1, bsize);
    bsize = jedis.lpush(bfoo, bfoo);
    assertEquals(2, bsize);
    bsize = jedis.lpush(bfoo, bbar, bfoo);
    assertEquals(4, bsize);

  }

  @Test
  public void llen() {
    assertEquals(0, jedis.llen("foo").intValue());
    jedis.lpush("foo", "bar");
    jedis.lpush("foo", "car");
    assertEquals(2, jedis.llen("foo").intValue());

    // Binary
    assertEquals(0, jedis.llen(bfoo).intValue());
    jedis.lpush(bfoo, bbar);
    jedis.lpush(bfoo, bcar);
    assertEquals(2, jedis.llen(bfoo).intValue());

  }

  @Test
  public void llenNotOnList() {
    try {
      jedis.set("foo", "bar");
      jedis.llen("foo");
      fail("JedisDataException expected");
    } catch (final JedisDataException e) {
    }

    // Binary
    try {
      jedis.set(bfoo, bbar);
      jedis.llen(bfoo);
      fail("JedisDataException expected");
    } catch (final JedisDataException e) {
    }

  }

  @Test
  public void lrange() {
    jedis.rpush("foo", "a");
    jedis.rpush("foo", "b");
    jedis.rpush("foo", "c");

    List<String> expected = new ArrayList<String>();
    expected.add("a");
    expected.add("b");
    expected.add("c");

    List<String> range = jedis.lrange("foo", 0, 2);
    assertEquals(expected, range);

    range = jedis.lrange("foo", 0, 20);
    assertEquals(expected, range);

    expected = new ArrayList<String>();
    expected.add("b");
    expected.add("c");

    range = jedis.lrange("foo", 1, 2);
    assertEquals(expected, range);

    expected = new ArrayList<String>();
    range = jedis.lrange("foo", 2, 1);
    assertEquals(expected, range);

    // Binary
    jedis.rpush(bfoo, bA);
    jedis.rpush(bfoo, bB);
    jedis.rpush(bfoo, bC);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bA);
    bexpected.add(bB);
    bexpected.add(bC);

    List<byte[]> brange = jedis.lrange(bfoo, 0, 2);
    assertEquals(bexpected, brange);

    brange = jedis.lrange(bfoo, 0, 20);
    assertEquals(bexpected, brange);

    bexpected = new ArrayList<byte[]>();
    bexpected.add(bB);
    bexpected.add(bC);

    brange = jedis.lrange(bfoo, 1, 2);
    assertEquals(bexpected, brange);

    bexpected = new ArrayList<byte[]>();
    brange = jedis.lrange(bfoo, 2, 1);
    assertEquals(bexpected, brange);

  }

  @Test
  public void ltrim() {
    jedis.lpush("foo", "1");
    jedis.lpush("foo", "2");
    jedis.lpush("foo", "3");
    String status = jedis.ltrim("foo", 0, 1);

    List<String> expected = new ArrayList<String>();
    expected.add("3");
    expected.add("2");

    assertEquals("OK", status);
    assertEquals(2, jedis.llen("foo").intValue());
    assertEquals(expected, jedis.lrange("foo", 0, 100));

    // Binary
    jedis.lpush(bfoo, b1);
    jedis.lpush(bfoo, b2);
    jedis.lpush(bfoo, b3);
    String bstatus = jedis.ltrim(bfoo, 0, 1);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(b3);
    bexpected.add(b2);

    assertEquals("OK", bstatus);
    assertEquals(2, jedis.llen(bfoo).intValue());
    assertEquals(bexpected, jedis.lrange(bfoo, 0, 100));

  }

  @Test
  public void lindex() {
    jedis.lpush("foo", "1");
    jedis.lpush("foo", "2");
    jedis.lpush("foo", "3");

    List<String> expected = new ArrayList<String>();
    expected.add("3");
    expected.add("bar");
    expected.add("1");

    String status = jedis.lset("foo", 1, "bar");

    assertEquals("OK", status);
    assertEquals(expected, jedis.lrange("foo", 0, 100));

    // Binary
    jedis.lpush(bfoo, b1);
    jedis.lpush(bfoo, b2);
    jedis.lpush(bfoo, b3);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(b3);
    bexpected.add(bbar);
    bexpected.add(b1);

    String bstatus = jedis.lset(bfoo, 1, bbar);

    assertEquals("OK", bstatus);
    assertEquals(bexpected, jedis.lrange(bfoo, 0, 100));
  }

  @Test
  public void lset() {
    jedis.lpush("foo", "1");
    jedis.lpush("foo", "2");
    jedis.lpush("foo", "3");

    assertEquals("3", jedis.lindex("foo", 0));
    assertEquals(null, jedis.lindex("foo", 100));

    // Binary
    jedis.lpush(bfoo, b1);
    jedis.lpush(bfoo, b2);
    jedis.lpush(bfoo, b3);

    assertArrayEquals(b3, jedis.lindex(bfoo, 0));
    assertEquals(null, jedis.lindex(bfoo, 100));

  }

  @Test
  public void lrem() {
    jedis.lpush("foo", "hello");
    jedis.lpush("foo", "hello");
    jedis.lpush("foo", "x");
    jedis.lpush("foo", "hello");
    jedis.lpush("foo", "c");
    jedis.lpush("foo", "b");
    jedis.lpush("foo", "a");

    long count = jedis.lrem("foo", -2, "hello");

    List<String> expected = new ArrayList<String>();
    expected.add("a");
    expected.add("b");
    expected.add("c");
    expected.add("hello");
    expected.add("x");

    assertEquals(2, count);
    assertEquals(expected, jedis.lrange("foo", 0, 1000));
    assertEquals(0, jedis.lrem("bar", 100, "foo").intValue());

    // Binary
    jedis.lpush(bfoo, bhello);
    jedis.lpush(bfoo, bhello);
    jedis.lpush(bfoo, bx);
    jedis.lpush(bfoo, bhello);
    jedis.lpush(bfoo, bC);
    jedis.lpush(bfoo, bB);
    jedis.lpush(bfoo, bA);

    long bcount = jedis.lrem(bfoo, -2, bhello);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bA);
    bexpected.add(bB);
    bexpected.add(bC);
    bexpected.add(bhello);
    bexpected.add(bx);

    assertEquals(2, bcount);
    assertEquals(bexpected, jedis.lrange(bfoo, 0, 1000));
    assertEquals(0, jedis.lrem(bbar, 100, bfoo).intValue());

  }

  @Test
  public void lpop() {
    jedis.rpush("foo", "a");
    jedis.rpush("foo", "b");
    jedis.rpush("foo", "c");

    String element = jedis.lpop("foo");
    assertEquals("a", element);

    List<String> expected = new ArrayList<String>();
    expected.add("b");
    expected.add("c");

    assertEquals(expected, jedis.lrange("foo", 0, 1000));
    jedis.lpop("foo");
    jedis.lpop("foo");

    element = jedis.lpop("foo");
    assertEquals(null, element);

    // Binary
    jedis.rpush(bfoo, bA);
    jedis.rpush(bfoo, bB);
    jedis.rpush(bfoo, bC);

    byte[] belement = jedis.lpop(bfoo);
    assertArrayEquals(bA, belement);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bB);
    bexpected.add(bC);

    assertEquals(bexpected, jedis.lrange(bfoo, 0, 1000));
    jedis.lpop(bfoo);
    jedis.lpop(bfoo);

    belement = jedis.lpop(bfoo);
    assertEquals(null, belement);

  }

  @Test
  public void rpop() {
    jedis.rpush("foo", "a");
    jedis.rpush("foo", "b");
    jedis.rpush("foo", "c");

    String element = jedis.rpop("foo");
    assertEquals("c", element);

    List<String> expected = new ArrayList<String>();
    expected.add("a");
    expected.add("b");

    assertEquals(expected, jedis.lrange("foo", 0, 1000));
    jedis.rpop("foo");
    jedis.rpop("foo");

    element = jedis.rpop("foo");
    assertEquals(null, element);

    // Binary
    jedis.rpush(bfoo, bA);
    jedis.rpush(bfoo, bB);
    jedis.rpush(bfoo, bC);

    byte[] belement = jedis.rpop(bfoo);
    assertArrayEquals(bC, belement);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bA);
    bexpected.add(bB);

    assertEquals(bexpected, jedis.lrange(bfoo, 0, 1000));
    jedis.rpop(bfoo);
    jedis.rpop(bfoo);

    belement = jedis.rpop(bfoo);
    assertEquals(null, belement);

  }

  @Test
  public void rpoplpush() {
    jedis.rpush("foo", "a");
    jedis.rpush("foo", "b");
    jedis.rpush("foo", "c");

    jedis.rpush("dst", "foo");
    jedis.rpush("dst", "bar");

    String element = jedis.rpoplpush("foo", "dst");

    assertEquals("c", element);

    List<String> srcExpected = new ArrayList<String>();
    srcExpected.add("a");
    srcExpected.add("b");

    List<String> dstExpected = new ArrayList<String>();
    dstExpected.add("c");
    dstExpected.add("foo");
    dstExpected.add("bar");

    assertEquals(srcExpected, jedis.lrange("foo", 0, 1000));
    assertEquals(dstExpected, jedis.lrange("dst", 0, 1000));

    // Binary
    jedis.rpush(bfoo, bA);
    jedis.rpush(bfoo, bB);
    jedis.rpush(bfoo, bC);

    jedis.rpush(bdst, bfoo);
    jedis.rpush(bdst, bbar);

    byte[] belement = jedis.rpoplpush(bfoo, bdst);

    assertArrayEquals(bC, belement);

    List<byte[]> bsrcExpected = new ArrayList<byte[]>();
    bsrcExpected.add(bA);
    bsrcExpected.add(bB);

    List<byte[]> bdstExpected = new ArrayList<byte[]>();
    bdstExpected.add(bC);
    bdstExpected.add(bfoo);
    bdstExpected.add(bbar);

    assertEquals(bsrcExpected, jedis.lrange(bfoo, 0, 1000));
    assertEquals(bdstExpected, jedis.lrange(bdst, 0, 1000));

  }

  @Test
  public void blpop() throws InterruptedException {
    List<String> result = jedis.blpop(1, "foo");
    assertNull(result);

    jedis.lpush("foo", "bar");
    result = jedis.blpop(1, "foo");

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("foo", result.get(0));
    assertEquals("bar", result.get(1));

    // Binary
    jedis.lpush(bfoo, bbar);
    List<byte[]> bresult = jedis.blpop(1, bfoo);

    assertNotNull(bresult);
    assertEquals(2, bresult.size());
    assertArrayEquals(bfoo, bresult.get(0));
    assertArrayEquals(bbar, bresult.get(1));

  }

  @Test
  public void brpop() throws InterruptedException {
    List<String> result = jedis.brpop(1, "foo");
    assertNull(result);

    jedis.lpush("foo", "bar");
    result = jedis.brpop(1, "foo");
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("foo", result.get(0));
    assertEquals("bar", result.get(1));

    // Binary

    jedis.lpush(bfoo, bbar);
    List<byte[]> bresult = jedis.brpop(1, bfoo);
    assertNotNull(bresult);
    assertEquals(2, bresult.size());
    assertArrayEquals(bfoo, bresult.get(0));
    assertArrayEquals(bbar, bresult.get(1));

  }

  @Test
  public void lpushx() {
    long status = jedis.lpushx("foo", "bar");
    assertEquals(0, status);

    jedis.lpush("foo", "a");
    status = jedis.lpushx("foo", "b");
    assertEquals(2, status);

    // Binary
    long bstatus = jedis.lpushx(bfoo, bbar);
    assertEquals(0, bstatus);

    jedis.lpush(bfoo, bA);
    bstatus = jedis.lpushx(bfoo, bB);
    assertEquals(2, bstatus);

  }

  @Test
  public void rpushx() {
    long status = jedis.rpushx("foo", "bar");
    assertEquals(0, status);

    jedis.lpush("foo", "a");
    status = jedis.rpushx("foo", "b");
    assertEquals(2, status);

    // Binary
    long bstatus = jedis.rpushx(bfoo, bbar);
    assertEquals(0, bstatus);

    jedis.lpush(bfoo, bA);
    bstatus = jedis.rpushx(bfoo, bB);
    assertEquals(2, bstatus);
  }

  @Test
  public void linsert() {
    long status = jedis.linsert("foo", Client.LIST_POSITION.BEFORE, "bar", "car");
    assertEquals(0, status);

    jedis.lpush("foo", "a");
    status = jedis.linsert("foo", Client.LIST_POSITION.AFTER, "a", "b");
    assertEquals(2, status);

    List<String> actual = jedis.lrange("foo", 0, 100);
    List<String> expected = new ArrayList<String>();
    expected.add("a");
    expected.add("b");

    assertEquals(expected, actual);

    status = jedis.linsert("foo", Client.LIST_POSITION.BEFORE, "bar", "car");
    assertEquals(-1, status);

    // Binary
    long bstatus = jedis.linsert(bfoo, Client.LIST_POSITION.BEFORE, bbar, bcar);
    assertEquals(0, bstatus);

    jedis.lpush(bfoo, bA);
    bstatus = jedis.linsert(bfoo, Client.LIST_POSITION.AFTER, bA, bB);
    assertEquals(2, bstatus);

    List<byte[]> bactual = jedis.lrange(bfoo, 0, 100);
    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bA);
    bexpected.add(bB);

    assertEquals(bexpected, bactual);

    bstatus = jedis.linsert(bfoo, Client.LIST_POSITION.BEFORE, bbar, bcar);
    assertEquals(-1, bstatus);

  }

  @Test
  public void brpoplpush() {
    (new Thread(new Runnable() {
      public void run() {
        try {
          Thread.sleep(100);
          Jedis j = createJedis();
          j.lpush("foo", "a");
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    })).start();

    String element = jedis.brpoplpush("foo", "bar", 0);

    assertEquals("a", element);
    assertEquals(1, jedis.llen("bar").longValue());
    assertEquals("a", jedis.lrange("bar", 0, -1).get(0));

    (new Thread(new Runnable() {
      public void run() {
        try {
          Thread.sleep(100);
          Jedis j = createJedis();
          j.lpush("foo", "a");
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    })).start();

    byte[] brpoplpush = jedis.brpoplpush("foo".getBytes(), "bar".getBytes(), 0);

    assertTrue(Arrays.equals("a".getBytes(), brpoplpush));
    assertEquals(1, jedis.llen("bar").longValue());
    assertEquals("a", jedis.lrange("bar", 0, -1).get(0));

  }
}
