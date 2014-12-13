package redis.clients.jedis.tests.commands.async;

import org.junit.Test;
import redis.clients.jedis.tests.commands.async.util.CommandWithWaiting;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AsyncSetCommandsTest extends AsyncJedisCommandTestBase {
  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
  final byte[] bcar = { 0x09, 0x0A, 0x0B, 0x0C };
  final byte[] ba = { 0x0A };
  final byte[] bb = { 0x0B };
  final byte[] bc = { 0x0C };
  final byte[] bd = { 0x0D };
  final byte[] bx = { 0x42 };

  @Test
  public void sadd() {
    asyncJedis.sadd(LONG_CALLBACK.withReset(), "foo", "a");
    long status = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, status);

    asyncJedis.sadd(LONG_CALLBACK.withReset(), "foo", "a");
    status = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(0, status);

    // binary
    asyncJedis.sadd(LONG_CALLBACK.withReset(), bfoo, ba);
    long bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, bstatus);

    asyncJedis.sadd(LONG_CALLBACK.withReset(), bfoo, ba);
    bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(0, bstatus);
  }

  @Test
  public void smembers() {
    CommandWithWaiting.sadd(asyncJedis, "foo", "a");
    CommandWithWaiting.sadd(asyncJedis, "foo", "b");

    Set<String> expected = new HashSet<String>();
    expected.add("a");
    expected.add("b");

    asyncJedis.smembers(STRING_SET_CALLBACK.withReset(), "foo");
    Set<String> members = STRING_SET_CALLBACK.getResponseWithWaiting(1000);

    assertEquals(expected, members);

    // Binary
    CommandWithWaiting.sadd(asyncJedis, bfoo, ba);
    CommandWithWaiting.sadd(asyncJedis, bfoo, bb);

    Set<byte[]> bexpected = new HashSet<byte[]>();
    bexpected.add(bb);
    bexpected.add(ba);

    asyncJedis.smembers(BYTE_ARRAY_SET_CALLBACK.withReset(), bfoo);
    Set<byte[]> bmembers = BYTE_ARRAY_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(bexpected, bmembers);
  }

  @Test
  public void srem() {
    CommandWithWaiting.sadd(asyncJedis, "foo", "a");
    CommandWithWaiting.sadd(asyncJedis, "foo", "b");

    asyncJedis.srem(LONG_CALLBACK.withReset(), "foo", "a");
    long status = LONG_CALLBACK.getResponseWithWaiting(1000);

    Set<String> expected = new HashSet<String>();
    expected.add("b");

    assertEquals(1, status);
    // blocking API
    assertEquals(expected, jedis.smembers("foo"));

    asyncJedis.srem(LONG_CALLBACK.withReset(), "foo", "bar");
    status = LONG_CALLBACK.getResponseWithWaiting(1000);

    assertEquals(0, status);

    // Binary
    CommandWithWaiting.sadd(asyncJedis, bfoo, ba);
    CommandWithWaiting.sadd(asyncJedis, bfoo, bb);

    asyncJedis.srem(LONG_CALLBACK.withReset(), bfoo, ba);
    long bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);

    Set<byte[]> bexpected = new HashSet<byte[]>();
    bexpected.add(bb);

    assertEquals(1, bstatus);
    // blocking API
    assertEquals(bexpected, jedis.smembers(bfoo));

    asyncJedis.srem(LONG_CALLBACK.withReset(), bfoo, bbar);
    bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);

    assertEquals(0, bstatus);
  }

  @Test
  public void spop() {
    CommandWithWaiting.sadd(asyncJedis, "foo", "a");
    CommandWithWaiting.sadd(asyncJedis, "foo", "b");

    asyncJedis.spop(STRING_CALLBACK.withReset(), "foo");
    String member = STRING_CALLBACK.getResponseWithWaiting(1000);

    assertTrue("a".equals(member) || "b".equals(member));
    // blocking API
    assertEquals(1, jedis.smembers("foo").size());

    asyncJedis.spop(STRING_CALLBACK.withReset(), "bar");
    member = STRING_CALLBACK.getResponseWithWaiting(1000);
    assertNull(member);

    // Binary
    CommandWithWaiting.sadd(asyncJedis, bfoo, ba);
    CommandWithWaiting.sadd(asyncJedis, bfoo, bb);

    asyncJedis.spop(BYTE_ARRAY_CALLBACK.withReset(), bfoo);
    byte[] bmember = BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000);

    assertTrue(Arrays.equals(ba, bmember) || Arrays.equals(bb, bmember));
    // blocking API
    assertEquals(1, jedis.smembers(bfoo).size());

    asyncJedis.spop(BYTE_ARRAY_CALLBACK.withReset(), bbar);
    bmember = BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000);
    assertNull(bmember);
  }

  @Test
  public void smove() {
    CommandWithWaiting.sadd(asyncJedis, "foo", "a");
    CommandWithWaiting.sadd(asyncJedis, "foo", "b");

    CommandWithWaiting.sadd(asyncJedis, "bar", "c");

    asyncJedis.smove(LONG_CALLBACK.withReset(), "foo", "bar", "a");
    long status = LONG_CALLBACK.getResponseWithWaiting(1000);

    Set<String> expectedSrc = new HashSet<String>();
    expectedSrc.add("b");

    Set<String> expectedDst = new HashSet<String>();
    expectedDst.add("c");
    expectedDst.add("a");

    assertEquals(status, 1);
    assertEquals(expectedSrc, jedis.smembers("foo"));
    assertEquals(expectedDst, jedis.smembers("bar"));

    asyncJedis.smove(LONG_CALLBACK.withReset(), "foo", "bar", "a");
    status = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(status, 0);

    // Binary
    CommandWithWaiting.sadd(asyncJedis, bfoo, ba);
    CommandWithWaiting.sadd(asyncJedis, bfoo, bb);

    CommandWithWaiting.sadd(asyncJedis, bbar, bc);

    asyncJedis.smove(LONG_CALLBACK.withReset(), bfoo, bbar, ba);
    long bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);

    Set<byte[]> bexpectedSrc = new HashSet<byte[]>();
    bexpectedSrc.add(bb);

    Set<byte[]> bexpectedDst = new HashSet<byte[]>();
    bexpectedDst.add(bc);
    bexpectedDst.add(ba);

    assertEquals(bstatus, 1);
    assertEquals(bexpectedSrc, jedis.smembers(bfoo));
    assertEquals(bexpectedDst, jedis.smembers(bbar));

    asyncJedis.smove(LONG_CALLBACK.withReset(), bfoo, bbar, ba);
    bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(bstatus, 0);
  }

  @Test
  public void scard() {
    CommandWithWaiting.sadd(asyncJedis, "foo", "a");
    CommandWithWaiting.sadd(asyncJedis, "foo", "b");

    asyncJedis.scard(LONG_CALLBACK.withReset(), "foo");
    long card = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(2, card);

    asyncJedis.scard(LONG_CALLBACK.withReset(), "bar");
    card = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(0, card);

    // Binary
    CommandWithWaiting.sadd(asyncJedis, bfoo, ba);
    CommandWithWaiting.sadd(asyncJedis, bfoo, bb);

    asyncJedis.scard(LONG_CALLBACK.withReset(), bfoo);
    long bcard = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(2, bcard);

    asyncJedis.scard(LONG_CALLBACK.withReset(), bbar);
    bcard = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(0, bcard);
  }

  @Test
  public void sismember() {
    CommandWithWaiting.sadd(asyncJedis, "foo", "a");
    CommandWithWaiting.sadd(asyncJedis, "foo", "b");

    asyncJedis.sismember(BOOLEAN_CALLBACK.withReset(), "foo", "a");
    assertTrue(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.sismember(BOOLEAN_CALLBACK.withReset(), "foo", "c");
    assertFalse(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));

    // Binary
    CommandWithWaiting.sadd(asyncJedis, bfoo, ba);
    CommandWithWaiting.sadd(asyncJedis, bfoo, bb);

    asyncJedis.sismember(BOOLEAN_CALLBACK.withReset(), bfoo, ba);
    assertTrue(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.sismember(BOOLEAN_CALLBACK.withReset(), bfoo, bc);
    assertFalse(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void sinter() {
    CommandWithWaiting.sadd(asyncJedis, "foo", "a");
    CommandWithWaiting.sadd(asyncJedis, "foo", "b");

    CommandWithWaiting.sadd(asyncJedis, "bar", "b");
    CommandWithWaiting.sadd(asyncJedis, "bar", "c");

    Set<String> expected = new HashSet<String>();
    expected.add("b");

    asyncJedis.sinter(STRING_SET_CALLBACK.withReset(), "foo", "bar");
    Set<String> intersection = STRING_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(expected, intersection);

    // Binary
    CommandWithWaiting.sadd(asyncJedis, bfoo, ba);
    CommandWithWaiting.sadd(asyncJedis, bfoo, bb);

    CommandWithWaiting.sadd(asyncJedis, bbar, bb);
    CommandWithWaiting.sadd(asyncJedis, bbar, bc);

    Set<byte[]> bexpected = new HashSet<byte[]>();
    bexpected.add(bb);

    asyncJedis.sinter(BYTE_ARRAY_SET_CALLBACK.withReset(), bfoo, bbar);
    Set<byte[]> bintersection = BYTE_ARRAY_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(bexpected, bintersection);
  }

  @Test
  public void sinterstore() {
    CommandWithWaiting.sadd(asyncJedis, "foo", "a");
    CommandWithWaiting.sadd(asyncJedis, "foo", "b");

    CommandWithWaiting.sadd(asyncJedis, "bar", "b");
    CommandWithWaiting.sadd(asyncJedis, "bar", "c");

    Set<String> expected = new HashSet<String>();
    expected.add("b");

    asyncJedis.sinterstore(LONG_CALLBACK.withReset(), "car", "foo", "bar");
    long status = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, status);

    assertEquals(expected, jedis.smembers("car"));

    // Binary
    CommandWithWaiting.sadd(asyncJedis, bfoo, ba);
    CommandWithWaiting.sadd(asyncJedis, bfoo, bb);

    CommandWithWaiting.sadd(asyncJedis, bbar, bb);
    CommandWithWaiting.sadd(asyncJedis, bbar, bc);

    Set<byte[]> bexpected = new HashSet<byte[]>();
    bexpected.add(bb);

    asyncJedis.sinterstore(LONG_CALLBACK.withReset(), bcar, bfoo, bbar);
    long bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, bstatus);

    assertEquals(bexpected, jedis.smembers(bcar));
  }

  @Test
  public void sunion() {
    CommandWithWaiting.sadd(asyncJedis, "foo", "a");
    CommandWithWaiting.sadd(asyncJedis, "foo", "b");

    CommandWithWaiting.sadd(asyncJedis, "bar", "b");
    CommandWithWaiting.sadd(asyncJedis, "bar", "c");

    Set<String> expected = new HashSet<String>();
    expected.add("a");
    expected.add("b");
    expected.add("c");

    asyncJedis.sunion(STRING_SET_CALLBACK.withReset(), "foo", "bar");
    Set<String> union = STRING_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(expected, union);

    // Binary
    CommandWithWaiting.sadd(asyncJedis, bfoo, ba);
    CommandWithWaiting.sadd(asyncJedis, bfoo, bb);

    CommandWithWaiting.sadd(asyncJedis, bbar, bb);
    CommandWithWaiting.sadd(asyncJedis, bbar, bc);

    Set<byte[]> bexpected = new HashSet<byte[]>();
    bexpected.add(bb);
    bexpected.add(bc);
    bexpected.add(ba);

    asyncJedis.sunion(BYTE_ARRAY_SET_CALLBACK.withReset(), bfoo, bbar);
    Set<byte[]> bunion = BYTE_ARRAY_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(bexpected, bunion);
  }

  @Test
  public void sunionstore() {
    CommandWithWaiting.sadd(asyncJedis, "foo", "a");
    CommandWithWaiting.sadd(asyncJedis, "foo", "b");

    CommandWithWaiting.sadd(asyncJedis, "bar", "b");
    CommandWithWaiting.sadd(asyncJedis, "bar", "c");

    Set<String> expected = new HashSet<String>();
    expected.add("a");
    expected.add("b");
    expected.add("c");

    asyncJedis.sunionstore(LONG_CALLBACK.withReset(), "car", "foo", "bar");
    long status = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(3, status);

    assertEquals(expected, jedis.smembers("car"));

    // Binary
    CommandWithWaiting.sadd(asyncJedis, bfoo, ba);
    CommandWithWaiting.sadd(asyncJedis, bfoo, bb);

    CommandWithWaiting.sadd(asyncJedis, bbar, bb);
    CommandWithWaiting.sadd(asyncJedis, bbar, bc);

    Set<byte[]> bexpected = new HashSet<byte[]>();
    bexpected.add(bb);
    bexpected.add(bc);
    bexpected.add(ba);

    asyncJedis.sunionstore(LONG_CALLBACK.withReset(), bcar, bfoo, bbar);
    long bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(3, bstatus);

    assertEquals(bexpected, jedis.smembers(bcar));
  }

  @Test
  public void sdiff() {
    CommandWithWaiting.sadd(asyncJedis, "foo", "x");
    CommandWithWaiting.sadd(asyncJedis, "foo", "a");
    CommandWithWaiting.sadd(asyncJedis, "foo", "b");
    CommandWithWaiting.sadd(asyncJedis, "foo", "c");

    CommandWithWaiting.sadd(asyncJedis, "bar", "c");

    CommandWithWaiting.sadd(asyncJedis, "car", "a");
    CommandWithWaiting.sadd(asyncJedis, "car", "d");

    Set<String> expected = new HashSet<String>();
    expected.add("x");
    expected.add("b");

    asyncJedis.sdiff(STRING_SET_CALLBACK.withReset(), "foo", "bar", "car");
    Set<String> diff = STRING_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(expected, diff);

    // Binary
    CommandWithWaiting.sadd(asyncJedis, bfoo, bx);
    CommandWithWaiting.sadd(asyncJedis, bfoo, ba);
    CommandWithWaiting.sadd(asyncJedis, bfoo, bb);
    CommandWithWaiting.sadd(asyncJedis, bfoo, bc);

    CommandWithWaiting.sadd(asyncJedis, bbar, bc);

    CommandWithWaiting.sadd(asyncJedis, bcar, ba);
    CommandWithWaiting.sadd(asyncJedis, bcar, bd);

    Set<byte[]> bexpected = new HashSet<byte[]>();
    bexpected.add(bb);
    bexpected.add(bx);

    asyncJedis.sdiff(BYTE_ARRAY_SET_CALLBACK.withReset(), bfoo, bbar, bcar);
    Set<byte[]> bdiff = BYTE_ARRAY_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(bexpected, bdiff);
  }

  @Test
  public void sdiffstore() {
    CommandWithWaiting.sadd(asyncJedis, "foo", "x");
    CommandWithWaiting.sadd(asyncJedis, "foo", "a");
    CommandWithWaiting.sadd(asyncJedis, "foo", "b");
    CommandWithWaiting.sadd(asyncJedis, "foo", "c");

    CommandWithWaiting.sadd(asyncJedis, "bar", "c");

    CommandWithWaiting.sadd(asyncJedis, "car", "a");
    CommandWithWaiting.sadd(asyncJedis, "car", "d");

    Set<String> expected = new HashSet<String>();
    expected.add("d");
    expected.add("a");

    asyncJedis.sdiffstore(LONG_CALLBACK.withReset(), "tar", "foo", "bar", "car");
    long status = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(2, status);
    assertEquals(expected, jedis.smembers("car"));

    // Binary
    CommandWithWaiting.sadd(asyncJedis, bfoo, bx);
    CommandWithWaiting.sadd(asyncJedis, bfoo, ba);
    CommandWithWaiting.sadd(asyncJedis, bfoo, bb);
    CommandWithWaiting.sadd(asyncJedis, bfoo, bc);

    CommandWithWaiting.sadd(asyncJedis, bbar, bc);

    CommandWithWaiting.sadd(asyncJedis, bcar, ba);
    CommandWithWaiting.sadd(asyncJedis, bcar, bd);

    Set<byte[]> bexpected = new HashSet<byte[]>();
    bexpected.add(bd);
    bexpected.add(ba);

    asyncJedis.sdiffstore(LONG_CALLBACK.withReset(), "tar".getBytes(), bfoo, bbar, bcar);
    long bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(2, bstatus);
    assertEquals(bexpected, jedis.smembers(bcar));
  }

  @Test
  public void srandmember() {
    CommandWithWaiting.sadd(asyncJedis, "foo", "a");
    CommandWithWaiting.sadd(asyncJedis, "foo", "b");

    asyncJedis.srandmember(STRING_CALLBACK.withReset(), "foo");
    String member = STRING_CALLBACK.getResponseWithWaiting(1000);

    assertTrue("a".equals(member) || "b".equals(member));
    assertEquals(2, jedis.smembers("foo").size());

    asyncJedis.srandmember(STRING_CALLBACK.withReset(), "bar");
    member = STRING_CALLBACK.getResponseWithWaiting(1000);
    assertNull(member);

    // Binary
    CommandWithWaiting.sadd(asyncJedis, bfoo, ba);
    CommandWithWaiting.sadd(asyncJedis, bfoo, bb);

    asyncJedis.srandmember(BYTE_ARRAY_CALLBACK.withReset(), bfoo);
    byte[] bmember = BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000);

    assertTrue(Arrays.equals(ba, bmember) || Arrays.equals(bb, bmember));
    assertEquals(2, jedis.smembers(bfoo).size());

    asyncJedis.srandmember(BYTE_ARRAY_CALLBACK.withReset(), bbar);
    bmember = BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000);
    assertNull(bmember);
  }
}