package redis.clients.jedis.commands.jedis;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static redis.clients.jedis.util.AssertUtil.assertByteArrayListEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.args.ListPosition;
import redis.clients.jedis.args.ListDirection;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.LPosParams;
import redis.clients.jedis.resps.KeyedListElement;
import redis.clients.jedis.util.KeyValue;

public class ListCommandsTest extends JedisCommandsTestBase {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] bfoo1 = { 0x01, 0x02, 0x03, 0x04, 0x05 };
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
    assertEquals(1, jedis.rpush("foo", "bar"));
    assertEquals(2, jedis.rpush("foo", "foo"));
    assertEquals(4, jedis.rpush("foo", "bar", "foo"));

    // Binary
    assertEquals(1, jedis.rpush(bfoo, bbar));
    assertEquals(2, jedis.rpush(bfoo, bfoo));
    assertEquals(4, jedis.rpush(bfoo, bbar, bfoo));
  }

  @Test
  public void lpush() {
    assertEquals(1, jedis.lpush("foo", "bar"));
    assertEquals(2, jedis.lpush("foo", "foo"));
    assertEquals(4, jedis.lpush("foo", "bar", "foo"));

    // Binary
    assertEquals(1, jedis.lpush(bfoo, bbar));
    assertEquals(2, jedis.lpush(bfoo, bfoo));
    assertEquals(4, jedis.lpush(bfoo, bbar, bfoo));
  }

  @Test
  public void llen() {
    assertEquals(0, jedis.llen("foo"));
    jedis.lpush("foo", "bar");
    jedis.lpush("foo", "car");
    assertEquals(2, jedis.llen("foo"));

    // Binary
    assertEquals(0, jedis.llen(bfoo));
    jedis.lpush(bfoo, bbar);
    jedis.lpush(bfoo, bcar);
    assertEquals(2, jedis.llen(bfoo));

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

    range = jedis.lrange("foo", 2, 1);
    assertEquals(Collections.<String> emptyList(), range);

    // Binary
    jedis.rpush(bfoo, bA);
    jedis.rpush(bfoo, bB);
    jedis.rpush(bfoo, bC);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bA);
    bexpected.add(bB);
    bexpected.add(bC);

    List<byte[]> brange = jedis.lrange(bfoo, 0, 2);
    assertByteArrayListEquals(bexpected, brange);

    brange = jedis.lrange(bfoo, 0, 20);
    assertByteArrayListEquals(bexpected, brange);

    bexpected = new ArrayList<byte[]>();
    bexpected.add(bB);
    bexpected.add(bC);

    brange = jedis.lrange(bfoo, 1, 2);
    assertByteArrayListEquals(bexpected, brange);

    brange = jedis.lrange(bfoo, 2, 1);
    assertByteArrayListEquals(Collections.<byte[]> emptyList(), brange);
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
    assertEquals(2, jedis.llen("foo"));
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
    assertEquals(2, jedis.llen(bfoo));
    assertByteArrayListEquals(bexpected, jedis.lrange(bfoo, 0, 100));
  }

  @Test
  public void lset() {
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
    assertByteArrayListEquals(bexpected, jedis.lrange(bfoo, 0, 100));
  }

  @Test
  public void lindex() {
    jedis.lpush("foo", "1");
    jedis.lpush("foo", "2");
    jedis.lpush("foo", "3");

    assertEquals("3", jedis.lindex("foo", 0));
    assertNull(jedis.lindex("foo", 100));

    // Binary
    jedis.lpush(bfoo, b1);
    jedis.lpush(bfoo, b2);
    jedis.lpush(bfoo, b3);

    assertArrayEquals(b3, jedis.lindex(bfoo, 0));
    assertNull(jedis.lindex(bfoo, 100));
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

    List<String> expected = new ArrayList<String>();
    expected.add("a");
    expected.add("b");
    expected.add("c");
    expected.add("hello");
    expected.add("x");

    assertEquals(2, jedis.lrem("foo", -2, "hello"));
    assertEquals(expected, jedis.lrange("foo", 0, 1000));
    assertEquals(0, jedis.lrem("bar", 100, "foo"));

    // Binary
    jedis.lpush(bfoo, bhello);
    jedis.lpush(bfoo, bhello);
    jedis.lpush(bfoo, bx);
    jedis.lpush(bfoo, bhello);
    jedis.lpush(bfoo, bC);
    jedis.lpush(bfoo, bB);
    jedis.lpush(bfoo, bA);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bA);
    bexpected.add(bB);
    bexpected.add(bC);
    bexpected.add(bhello);
    bexpected.add(bx);

    assertEquals(2, jedis.lrem(bfoo, -2, bhello));
    assertByteArrayListEquals(bexpected, jedis.lrange(bfoo, 0, 1000));
    assertEquals(0, jedis.lrem(bbar, 100, bfoo));

  }

  @Test
  public void lpop() {

    assertNull(jedis.lpop("foo"));
    assertNull(jedis.lpop("foo", 0));

    jedis.rpush("foo", "a");
    jedis.rpush("foo", "b");
    jedis.rpush("foo", "c");

    assertEquals("a", jedis.lpop("foo"));
    assertEquals(Arrays.asList("b", "c"), jedis.lpop("foo", 10));

    assertNull(jedis.lpop("foo"));
    assertNull(jedis.lpop("foo", 1));

    // Binary

    assertNull(jedis.lpop(bfoo));
    assertNull(jedis.lpop(bfoo, 0));

    jedis.rpush(bfoo, bA);
    jedis.rpush(bfoo, bB);
    jedis.rpush(bfoo, bC);

    assertArrayEquals(bA, jedis.lpop(bfoo));
    assertByteArrayListEquals(Arrays.asList(bB, bC), jedis.lpop(bfoo, 10));

    assertNull(jedis.lpop(bfoo));
    assertNull(jedis.lpop(bfoo, 1));

  }

  @Test
  public void rpop() {

    assertNull(jedis.rpop("foo"));
    assertNull(jedis.rpop("foo", 0));

    jedis.rpush("foo", "a");
    jedis.rpush("foo", "b");
    jedis.rpush("foo", "c");

    assertEquals("c", jedis.rpop("foo"));
    assertEquals(Arrays.asList("b", "a"), jedis.rpop("foo", 10));

    assertNull(jedis.rpop("foo"));
    assertNull(jedis.rpop("foo", 1));

    // Binary

    assertNull(jedis.rpop(bfoo));
    assertNull(jedis.rpop(bfoo, 0));

    jedis.rpush(bfoo, bA);
    jedis.rpush(bfoo, bB);
    jedis.rpush(bfoo, bC);

    assertArrayEquals(bC, jedis.rpop(bfoo));
    assertByteArrayListEquals(Arrays.asList(bB, bA), jedis.rpop(bfoo, 10));

    assertNull(jedis.rpop(bfoo));
    assertNull(jedis.rpop(bfoo, 1));

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

    assertByteArrayListEquals(bsrcExpected, jedis.lrange(bfoo, 0, 1000));
    assertByteArrayListEquals(bdstExpected, jedis.lrange(bdst, 0, 1000));

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

    // Multi keys
    result = jedis.blpop(1, "foo", "foo1");
    assertNull(result);

    jedis.lpush("foo", "bar");
    jedis.lpush("foo1", "bar1");
    result = jedis.blpop(1, "foo1", "foo");

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("foo1", result.get(0));
    assertEquals("bar1", result.get(1));

    // Binary
    jedis.lpush(bfoo, bbar);
    List<byte[]> bresult = jedis.blpop(1, bfoo);

    assertNotNull(bresult);
    assertEquals(2, bresult.size());
    assertArrayEquals(bfoo, bresult.get(0));
    assertArrayEquals(bbar, bresult.get(1));

    // Binary Multi keys
    bresult = jedis.blpop(1, bfoo, bfoo1);
    assertNull(bresult);

    jedis.lpush(bfoo, bbar);
    jedis.lpush(bfoo1, bcar);
    bresult = jedis.blpop(1, bfoo, bfoo1);

    assertNotNull(bresult);
    assertEquals(2, bresult.size());
    assertArrayEquals(bfoo, bresult.get(0));
    assertArrayEquals(bbar, bresult.get(1));
  }

  @Test
  public void blpopDouble() throws InterruptedException {
    KeyedListElement result = jedis.blpop(0.1, "foo");
    assertNull(result);

    jedis.lpush("foo", "bar");
    result = jedis.blpop(3.2, "foo");

    assertNotNull(result);
    assertEquals("foo", result.getKey());
    assertEquals("bar", result.getElement());

    // Multi keys
    result = jedis.blpop(0.18, "foo", "foo1");
    assertNull(result);

    jedis.lpush("foo", "bar");
    jedis.lpush("foo1", "bar1");
    result = jedis.blpop(1d, "foo1", "foo");

    assertNotNull(result);
    assertEquals("foo1", result.getKey());
    assertEquals("bar1", result.getElement());

    // Binary
    jedis.lpush(bfoo, bbar);
    List<byte[]> bresult = jedis.blpop(3.12, bfoo);

    assertNotNull(bresult);
    assertEquals(2, bresult.size());
    assertArrayEquals(bfoo, bresult.get(0));
    assertArrayEquals(bbar, bresult.get(1));

    // Binary Multi keys
    bresult = jedis.blpop(0.11, bfoo, bfoo1);
    assertNull(bresult);

    jedis.lpush(bfoo, bbar);
    jedis.lpush(bfoo1, bcar);
    bresult = jedis.blpop(1d, bfoo, bfoo1);

    assertNotNull(bresult);
    assertEquals(2, bresult.size());
    assertArrayEquals(bfoo, bresult.get(0));
    assertArrayEquals(bbar, bresult.get(1));
  }

  @Test
  public void blpopDoubleWithSleep() {
    long startMillis, totalMillis;

    startMillis = System.currentTimeMillis();
    KeyedListElement result = jedis.blpop(0.04, "foo");
    totalMillis = System.currentTimeMillis() - startMillis;
    assertTrue("TotalMillis=" + totalMillis, totalMillis < 200);
    assertNull(result);

    startMillis = System.currentTimeMillis();
    new Thread(() -> {
      try {
        Thread.sleep(30);
      } catch(InterruptedException e) {
        logger.error("", e);
      }
      try (Jedis j = createJedis()) {
        j.lpush("foo", "bar");
      }
    }).start();
    result = jedis.blpop(1.2, "foo");
    totalMillis = System.currentTimeMillis() - startMillis;
    assertTrue("TotalMillis=" + totalMillis, totalMillis < 200);

    assertNotNull(result);
    assertEquals("foo", result.getKey());
    assertEquals("bar", result.getElement());
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

    // Multi keys
    result = jedis.brpop(1, "foo", "foo1");
    assertNull(result);

    jedis.lpush("foo", "bar");
    jedis.lpush("foo1", "bar1");
    result = jedis.brpop(1, "foo1", "foo");

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("foo1", result.get(0));
    assertEquals("bar1", result.get(1));

    // Binary
    jedis.lpush(bfoo, bbar);
    List<byte[]> bresult = jedis.brpop(1, bfoo);
    assertNotNull(bresult);
    assertEquals(2, bresult.size());
    assertArrayEquals(bfoo, bresult.get(0));
    assertArrayEquals(bbar, bresult.get(1));

    // Binary Multi keys
    bresult = jedis.brpop(1, bfoo, bfoo1);
    assertNull(bresult);

    jedis.lpush(bfoo, bbar);
    jedis.lpush(bfoo1, bcar);
    bresult = jedis.brpop(1, bfoo, bfoo1);

    assertNotNull(bresult);
    assertEquals(2, bresult.size());
    assertArrayEquals(bfoo, bresult.get(0));
    assertArrayEquals(bbar, bresult.get(1));
  }

  @Test
  public void brpopDouble() throws InterruptedException {
    KeyedListElement result = jedis.brpop(0.1, "foo");
    assertNull(result);

    jedis.lpush("foo", "bar");
    result = jedis.brpop(3.2, "foo");

    assertNotNull(result);
    assertEquals("foo", result.getKey());
    assertEquals("bar", result.getElement());

    // Multi keys
    result = jedis.brpop(0.18, "foo", "foo1");
    assertNull(result);

    jedis.lpush("foo", "bar");
    jedis.lpush("foo1", "bar1");
    result = jedis.brpop(1d, "foo1", "foo");

    assertNotNull(result);
    assertEquals("foo1", result.getKey());
    assertEquals("bar1", result.getElement());

    // Binary
    jedis.lpush(bfoo, bbar);
    List<byte[]> bresult = jedis.brpop(3.12, bfoo);

    assertNotNull(bresult);
    assertEquals(2, bresult.size());
    assertArrayEquals(bfoo, bresult.get(0));
    assertArrayEquals(bbar, bresult.get(1));

    // Binary Multi keys
    bresult = jedis.brpop(0.11, bfoo, bfoo1);
    assertNull(bresult);

    jedis.lpush(bfoo, bbar);
    jedis.lpush(bfoo1, bcar);
    bresult = jedis.brpop(1d, bfoo, bfoo1);

    assertNotNull(bresult);
    assertEquals(2, bresult.size());
    assertArrayEquals(bfoo, bresult.get(0));
    assertArrayEquals(bbar, bresult.get(1));
  }

  @Test
  public void brpopDoubleWithSleep() {
    long startMillis, totalMillis;

    startMillis = System.currentTimeMillis();
    KeyedListElement result = jedis.brpop(0.04, "foo");
    totalMillis = System.currentTimeMillis() - startMillis;
    assertTrue("TotalMillis=" + totalMillis, totalMillis < 200);
    assertNull(result);

    startMillis = System.currentTimeMillis();
    new Thread(() -> {
      try {
        Thread.sleep(30);
      } catch(InterruptedException e) {
        logger.error("", e);
      }
      try (Jedis j = createJedis()) {
        j.lpush("foo", "bar");
      }
    }).start();
    result = jedis.brpop(1.2, "foo");
    totalMillis = System.currentTimeMillis() - startMillis;
    assertTrue("TotalMillis=" + totalMillis, totalMillis < 200);

    assertNotNull(result);
    assertEquals("foo", result.getKey());
    assertEquals("bar", result.getElement());
  }

  @Test
  public void lpushx() {
    assertEquals(0, jedis.lpushx("foo", "bar"));

    jedis.lpush("foo", "a");
    assertEquals(2, jedis.lpushx("foo", "b"));

    // Binary
    assertEquals(0, jedis.lpushx(bfoo, bbar));

    jedis.lpush(bfoo, bA);
    assertEquals(2, jedis.lpushx(bfoo, bB));
  }

  @Test
  public void rpushx() {
    assertEquals(0, jedis.rpushx("foo", "bar"));

    jedis.lpush("foo", "a");
    assertEquals(2, jedis.rpushx("foo", "b"));

    // Binary
    assertEquals(0, jedis.rpushx(bfoo, bbar));

    jedis.lpush(bfoo, bA);
    assertEquals(2, jedis.rpushx(bfoo, bB));
  }

  @Test
  public void linsert() {
    assertEquals(0, jedis.linsert("foo", ListPosition.BEFORE, "bar", "car"));

    jedis.lpush("foo", "a");
    assertEquals(2, jedis.linsert("foo", ListPosition.AFTER, "a", "b"));

    List<String> expected = new ArrayList<String>();
    expected.add("a");
    expected.add("b");

    assertEquals(expected, jedis.lrange("foo", 0, 100));

    assertEquals(-1, jedis.linsert("foo", ListPosition.BEFORE, "bar", "car"));

    // Binary
    assertEquals(0, jedis.linsert(bfoo, ListPosition.BEFORE, bbar, bcar));

    jedis.lpush(bfoo, bA);
    assertEquals(2, jedis.linsert(bfoo, ListPosition.AFTER, bA, bB));

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bA);
    bexpected.add(bB);

    assertByteArrayListEquals(bexpected, jedis.lrange(bfoo, 0, 100));

    assertEquals(-1, jedis.linsert(bfoo, ListPosition.BEFORE, bbar, bcar));

  }

  @Test
  public void brpoplpush() {

    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          logger.error("", e);
        }
        try (Jedis j = createJedis()) {
          j.lpush("foo", "a");
        }
      }
    }).start();

    String element = jedis.brpoplpush("foo", "bar", 0);

    assertEquals("a", element);
    assertEquals(1, jedis.llen("bar"));
    assertEquals("a", jedis.lrange("bar", 0, -1).get(0));

    // Binary

    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          logger.error("", e);
        }
        try (Jedis j = createJedis()) {
          j.lpush(bfoo, bA);
        }
      }
    }).start();

    byte[] belement = jedis.brpoplpush(bfoo, bbar, 0);

    assertArrayEquals(bA, belement);
    assertEquals(1, jedis.llen("bar"));
    assertArrayEquals(bA, jedis.lrange(bbar, 0, -1).get(0));
  }

  @Test
  public void lpos() {
    jedis.rpush("foo", "a");
    jedis.rpush("foo", "b");
    jedis.rpush("foo", "c");

    Long pos = jedis.lpos("foo", "b");
    assertEquals(1, pos.intValue());
    pos = jedis.lpos("foo", "d");
    assertNull(pos);

    jedis.rpush("foo", "a");
    jedis.rpush("foo", "b");
    jedis.rpush("foo", "b");

    pos = jedis.lpos("foo", "b", LPosParams.lPosParams());
    assertEquals(1, pos.intValue());
    pos = jedis.lpos("foo", "b", LPosParams.lPosParams().rank(3));
    assertEquals(5, pos.intValue());
    pos = jedis.lpos("foo", "b", LPosParams.lPosParams().rank(-2));
    assertEquals(4, pos.intValue());
    pos = jedis.lpos("foo", "b", LPosParams.lPosParams().rank(-5));
    assertNull(pos);

    pos = jedis.lpos("foo", "b", LPosParams.lPosParams().rank(1).maxlen(2));
    assertEquals(1, pos.intValue());
    pos = jedis.lpos("foo", "b", LPosParams.lPosParams().rank(2).maxlen(2));
    assertNull(pos);
    pos = jedis.lpos("foo", "b", LPosParams.lPosParams().rank(-2).maxlen(2));
    assertEquals(4, pos.intValue());

    List<Long> expected = new ArrayList<Long>();
    expected.add(1L);
    expected.add(4L);
    expected.add(5L);
    List<Long> posList = jedis.lpos("foo", "b", LPosParams.lPosParams(), 2);
    assertEquals(expected.subList(0, 2), posList);
    posList = jedis.lpos("foo", "b", LPosParams.lPosParams(), 0);
    assertEquals(expected, posList);
    posList = jedis.lpos("foo", "b", LPosParams.lPosParams().rank(2), 0);
    assertEquals(expected.subList(1, 3), posList);
    posList = jedis.lpos("foo", "b", LPosParams.lPosParams().rank(2).maxlen(5), 0);
    assertEquals(expected.subList(1, 2), posList);

    Collections.reverse(expected);
    posList = jedis.lpos("foo", "b", LPosParams.lPosParams().rank(-2), 0);
    assertEquals(expected.subList(1, 3), posList);
    posList = jedis.lpos("foo", "b", LPosParams.lPosParams().rank(-1).maxlen(5), 2);
    assertEquals(expected.subList(0, 2), posList);

    // Binary
    jedis.rpush(bfoo, bA);
    jedis.rpush(bfoo, bB);
    jedis.rpush(bfoo, bC);

    pos = jedis.lpos(bfoo, bB);
    assertEquals(1, pos.intValue());
    pos = jedis.lpos(bfoo, b3);
    assertNull(pos);

    jedis.rpush(bfoo, bA);
    jedis.rpush(bfoo, bB);
    jedis.rpush(bfoo, bA);

    pos = jedis.lpos(bfoo, bB, LPosParams.lPosParams().rank(2));
    assertEquals(4, pos.intValue());
    pos = jedis.lpos(bfoo, bB, LPosParams.lPosParams().rank(-2).maxlen(5));
    assertEquals(1, pos.intValue());

    expected.clear();
    expected.add(0L);
    expected.add(3L);
    expected.add(5L);

    posList = jedis.lpos(bfoo, bA, LPosParams.lPosParams().maxlen(6), 0);
    assertEquals(expected, posList);
    posList = jedis.lpos(bfoo, bA, LPosParams.lPosParams().maxlen(6).rank(2), 1);
    assertEquals(expected.subList(1, 2), posList);

  }

  @Test
  public void lmove() {
    jedis.rpush("foo", "bar1", "bar2", "bar3");
    assertEquals("bar3", jedis.lmove("foo", "bar", ListDirection.RIGHT, ListDirection.LEFT));
    assertEquals(Collections.singletonList("bar3"), jedis.lrange("bar", 0, -1));
    assertEquals(Arrays.asList("bar1", "bar2"), jedis.lrange("foo", 0, -1));

    // Binary
    jedis.rpush(bfoo, b1, b2, b3);
    assertArrayEquals(b3, jedis.lmove(bfoo, bbar, ListDirection.RIGHT, ListDirection.LEFT));
    assertByteArrayListEquals(Collections.singletonList(b3), jedis.lrange(bbar, 0, -1));
    assertByteArrayListEquals(Arrays.asList(b1, b2), jedis.lrange(bfoo, 0, -1));
  }

  @Test
  public void blmove() {
    new Thread(() -> {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        logger.error("", e);
      }
      try (Jedis j = createJedis()) {
        j.rpush("foo", "bar1", "bar2", "bar3");
      }
    }).start();

    assertEquals("bar3", jedis.blmove("foo", "bar", ListDirection.RIGHT, ListDirection.LEFT, 0));
    assertEquals(Collections.singletonList("bar3"), jedis.lrange("bar", 0, -1));
    assertEquals(Arrays.asList("bar1", "bar2"), jedis.lrange("foo", 0, -1));

    // Binary
    new Thread(() -> {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        logger.error("", e);
      }
      try (Jedis j = createJedis()) {
        j.rpush(bfoo, b1, b2, b3);
      }
    }).start();
    assertArrayEquals(b3, jedis.blmove(bfoo, bbar, ListDirection.RIGHT, ListDirection.LEFT, 0));
    assertByteArrayListEquals(Collections.singletonList(b3), jedis.lrange(bbar, 0, -1));
    assertByteArrayListEquals(Arrays.asList(b1, b2), jedis.lrange(bfoo, 0, -1));
  }

  @Test
  public void lmpop() {
    String mylist1 = "mylist1";
    String mylist2 = "mylist2";

    // add elements to list
    jedis.lpush(mylist1, "one", "two", "three", "four", "five");
    jedis.lpush(mylist2, "one", "two", "three", "four", "five");

    KeyValue<String, List<String>> elements = jedis.lmpop(ListDirection.LEFT, mylist1, mylist2);
    assertEquals(mylist1, elements.getKey());
    assertEquals(1, elements.getValue().size());

    elements = jedis.lmpop(ListDirection.LEFT, 5, mylist1, mylist2);
    assertEquals(mylist1, elements.getKey());
    assertEquals(4, elements.getValue().size());

    elements = jedis.lmpop(ListDirection.RIGHT, 100, mylist1, mylist2);
    assertEquals(mylist2, elements.getKey());
    assertEquals(5, elements.getValue().size());

    elements = jedis.lmpop(ListDirection.RIGHT, mylist1, mylist2);
    assertNull(elements);
  }

  @Test
  public void blmpopSimple() {
    String mylist1 = "mylist1";
    String mylist2 = "mylist2";

    // add elements to list
    jedis.lpush(mylist1, "one", "two", "three", "four", "five");
    jedis.lpush(mylist2, "one", "two", "three", "four", "five");

    KeyValue<String, List<String>> elements = jedis.blmpop(1L, ListDirection.LEFT, mylist1, mylist2);
    assertEquals(mylist1, elements.getKey());
    assertEquals(1, elements.getValue().size());

    elements = jedis.blmpop(1L, ListDirection.LEFT, 5, mylist1, mylist2);
    assertEquals(mylist1, elements.getKey());
    assertEquals(4, elements.getValue().size());

    elements = jedis.blmpop(1L, ListDirection.RIGHT, 100, mylist1, mylist2);
    assertEquals(mylist2, elements.getKey());
    assertEquals(5, elements.getValue().size());

    elements = jedis.blmpop(1L, ListDirection.RIGHT, mylist1, mylist2);
    assertNull(elements);
  }
}
