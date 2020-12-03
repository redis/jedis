package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static redis.clients.jedis.ScanParams.SCAN_POINTER_START;
import static redis.clients.jedis.ScanParams.SCAN_POINTER_START_BINARY;
import static redis.clients.jedis.tests.utils.AssertUtil.assertByteArrayCollectionContainsAll;
import static redis.clients.jedis.tests.utils.AssertUtil.assertByteArraySetEquals;
import static redis.clients.jedis.tests.utils.AssertUtil.assertCollectionContainsAll;
import static redis.clients.jedis.tests.utils.ByteArrayUtil.byteArrayCollectionRemoveAll;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

public class SetCommandsTest extends JedisCommandTestBase {
  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
  final byte[] bcar = { 0x09, 0x0A, 0x0B, 0x0C };
  final byte[] ba = { 0x0A };
  final byte[] bb = { 0x0B };
  final byte[] bc = { 0x0C };
  final byte[] bd = { 0x0D };
  final byte[] bx = { 0x42 };

  final byte[] bbar1 = { 0x05, 0x06, 0x07, 0x08, 0x0A };
  final byte[] bbar2 = { 0x05, 0x06, 0x07, 0x08, 0x0B };
  final byte[] bbar3 = { 0x05, 0x06, 0x07, 0x08, 0x0C };
  final byte[] bbarstar = { 0x05, 0x06, 0x07, 0x08, '*' };

  @Test
  public void sadd() {
    long status = jedis.sadd("foo", "a");
    assertEquals(1, status);

    status = jedis.sadd("foo", "a");
    assertEquals(0, status);

    long bstatus = jedis.sadd(bfoo, ba);
    assertEquals(1, bstatus);

    bstatus = jedis.sadd(bfoo, ba);
    assertEquals(0, bstatus);

  }

  @Test
  public void smembers() {
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");

    Set<String> expected = new HashSet<String>();
    expected.add("a");
    expected.add("b");

    Set<String> members = jedis.smembers("foo");

    assertEquals(expected, members);

    // Binary
    jedis.sadd(bfoo, ba);
    jedis.sadd(bfoo, bb);

    Set<byte[]> bexpected = new HashSet<byte[]>();
    bexpected.add(bb);
    bexpected.add(ba);

    Set<byte[]> bmembers = jedis.smembers(bfoo);

    assertByteArraySetEquals(bexpected, bmembers);
  }

  @Test
  public void srem() {
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");

    long status = jedis.srem("foo", "a");

    Set<String> expected = new HashSet<String>();
    expected.add("b");

    assertEquals(1, status);
    assertEquals(expected, jedis.smembers("foo"));

    status = jedis.srem("foo", "bar");

    assertEquals(0, status);

    // Binary

    jedis.sadd(bfoo, ba);
    jedis.sadd(bfoo, bb);

    long bstatus = jedis.srem(bfoo, ba);

    Set<byte[]> bexpected = new HashSet<byte[]>();
    bexpected.add(bb);

    assertEquals(1, bstatus);
    assertByteArraySetEquals(bexpected, jedis.smembers(bfoo));

    bstatus = jedis.srem(bfoo, bbar);

    assertEquals(0, bstatus);

  }

  @Test
  public void spop() {
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");

    String member = jedis.spop("foo");

    assertTrue("a".equals(member) || "b".equals(member));
    assertEquals(1, jedis.smembers("foo").size());

    member = jedis.spop("bar");
    assertNull(member);

    // Binary
    jedis.sadd(bfoo, ba);
    jedis.sadd(bfoo, bb);

    byte[] bmember = jedis.spop(bfoo);

    assertTrue(Arrays.equals(ba, bmember) || Arrays.equals(bb, bmember));
    assertEquals(1, jedis.smembers(bfoo).size());

    bmember = jedis.spop(bbar);
    assertNull(bmember);

  }

  @Test
  public void spopWithCount() {
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");
    jedis.sadd("foo", "c");

    Set<String> superSet = new HashSet<String>();
    superSet.add("c");
    superSet.add("b");
    superSet.add("a");

    Set<String> members = jedis.spop("foo", 2);

    assertEquals(2, members.size());
    assertCollectionContainsAll(superSet, members);
    superSet.removeAll(members);

    members = jedis.spop("foo", 2);
    assertEquals(1, members.size());
    assertEquals(superSet, members);

    assertTrue(jedis.spop("foo", 2).isEmpty());

    // Binary
    jedis.sadd(bfoo, ba);
    jedis.sadd(bfoo, bb);
    jedis.sadd(bfoo, bc);

    Set<byte[]> bsuperSet = new HashSet<byte[]>();
    bsuperSet.add(bc);
    bsuperSet.add(bb);
    bsuperSet.add(ba);

    Set<byte[]> bmembers = jedis.spop(bfoo, 2);

    assertEquals(2, bmembers.size());
    assertByteArrayCollectionContainsAll(bsuperSet, bmembers);
    byteArrayCollectionRemoveAll(bsuperSet, bmembers);

    bmembers = jedis.spop(bfoo, 2);
    assertEquals(1, bmembers.size());
    assertByteArraySetEquals(bsuperSet, bmembers);

    assertTrue(jedis.spop(bfoo, 2).isEmpty());
  }

  @Test
  public void smove() {
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");

    jedis.sadd("bar", "c");

    long status = jedis.smove("foo", "bar", "a");

    Set<String> expectedSrc = new HashSet<String>();
    expectedSrc.add("b");

    Set<String> expectedDst = new HashSet<String>();
    expectedDst.add("c");
    expectedDst.add("a");

    assertEquals(status, 1);
    assertEquals(expectedSrc, jedis.smembers("foo"));
    assertEquals(expectedDst, jedis.smembers("bar"));

    status = jedis.smove("foo", "bar", "a");

    assertEquals(status, 0);

    // Binary
    jedis.sadd(bfoo, ba);
    jedis.sadd(bfoo, bb);

    jedis.sadd(bbar, bc);

    long bstatus = jedis.smove(bfoo, bbar, ba);

    Set<byte[]> bexpectedSrc = new HashSet<byte[]>();
    bexpectedSrc.add(bb);

    Set<byte[]> bexpectedDst = new HashSet<byte[]>();
    bexpectedDst.add(bc);
    bexpectedDst.add(ba);

    assertEquals(bstatus, 1);
    assertByteArraySetEquals(bexpectedSrc, jedis.smembers(bfoo));
    assertByteArraySetEquals(bexpectedDst, jedis.smembers(bbar));

    bstatus = jedis.smove(bfoo, bbar, ba);
    assertEquals(bstatus, 0);

  }

  @Test
  public void scard() {
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");

    long card = jedis.scard("foo");

    assertEquals(2, card);

    card = jedis.scard("bar");
    assertEquals(0, card);

    // Binary
    jedis.sadd(bfoo, ba);
    jedis.sadd(bfoo, bb);

    long bcard = jedis.scard(bfoo);

    assertEquals(2, bcard);

    bcard = jedis.scard(bbar);
    assertEquals(0, bcard);

  }

  @Test
  public void sismember() {
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");

    assertTrue(jedis.sismember("foo", "a"));

    assertFalse(jedis.sismember("foo", "c"));

    // Binary
    jedis.sadd(bfoo, ba);
    jedis.sadd(bfoo, bb);

    assertTrue(jedis.sismember(bfoo, ba));

    assertFalse(jedis.sismember(bfoo, bc));

  }

  @Test
  public void sinter() {
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");

    jedis.sadd("bar", "b");
    jedis.sadd("bar", "c");

    Set<String> expected = new HashSet<String>();
    expected.add("b");

    Set<String> intersection = jedis.sinter("foo", "bar");
    assertEquals(expected, intersection);

    // Binary
    jedis.sadd(bfoo, ba);
    jedis.sadd(bfoo, bb);

    jedis.sadd(bbar, bb);
    jedis.sadd(bbar, bc);

    Set<byte[]> bexpected = new HashSet<byte[]>();
    bexpected.add(bb);

    Set<byte[]> bintersection = jedis.sinter(bfoo, bbar);
    assertByteArraySetEquals(bexpected, bintersection);
  }

  @Test
  public void sinterstore() {
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");

    jedis.sadd("bar", "b");
    jedis.sadd("bar", "c");

    Set<String> expected = new HashSet<String>();
    expected.add("b");

    long status = jedis.sinterstore("car", "foo", "bar");
    assertEquals(1, status);

    assertEquals(expected, jedis.smembers("car"));

    // Binary
    jedis.sadd(bfoo, ba);
    jedis.sadd(bfoo, bb);

    jedis.sadd(bbar, bb);
    jedis.sadd(bbar, bc);

    Set<byte[]> bexpected = new HashSet<byte[]>();
    bexpected.add(bb);

    long bstatus = jedis.sinterstore(bcar, bfoo, bbar);
    assertEquals(1, bstatus);

    assertByteArraySetEquals(bexpected, jedis.smembers(bcar));

  }

  @Test
  public void sunion() {
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");

    jedis.sadd("bar", "b");
    jedis.sadd("bar", "c");

    Set<String> expected = new HashSet<String>();
    expected.add("a");
    expected.add("b");
    expected.add("c");

    Set<String> union = jedis.sunion("foo", "bar");
    assertEquals(expected, union);

    // Binary
    jedis.sadd(bfoo, ba);
    jedis.sadd(bfoo, bb);

    jedis.sadd(bbar, bb);
    jedis.sadd(bbar, bc);

    Set<byte[]> bexpected = new HashSet<byte[]>();
    bexpected.add(bb);
    bexpected.add(bc);
    bexpected.add(ba);

    Set<byte[]> bunion = jedis.sunion(bfoo, bbar);
    assertByteArraySetEquals(bexpected, bunion);

  }

  @Test
  public void sunionstore() {
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");

    jedis.sadd("bar", "b");
    jedis.sadd("bar", "c");

    Set<String> expected = new HashSet<String>();
    expected.add("a");
    expected.add("b");
    expected.add("c");

    long status = jedis.sunionstore("car", "foo", "bar");
    assertEquals(3, status);

    assertEquals(expected, jedis.smembers("car"));

    // Binary
    jedis.sadd(bfoo, ba);
    jedis.sadd(bfoo, bb);

    jedis.sadd(bbar, bb);
    jedis.sadd(bbar, bc);

    Set<byte[]> bexpected = new HashSet<byte[]>();
    bexpected.add(bb);
    bexpected.add(bc);
    bexpected.add(ba);

    long bstatus = jedis.sunionstore(bcar, bfoo, bbar);
    assertEquals(3, bstatus);

    assertByteArraySetEquals(bexpected, jedis.smembers(bcar));

  }

  @Test
  public void sdiff() {
    jedis.sadd("foo", "x");
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");
    jedis.sadd("foo", "c");

    jedis.sadd("bar", "c");

    jedis.sadd("car", "a");
    jedis.sadd("car", "d");

    Set<String> expected = new HashSet<String>();
    expected.add("x");
    expected.add("b");

    Set<String> diff = jedis.sdiff("foo", "bar", "car");
    assertEquals(expected, diff);

    // Binary
    jedis.sadd(bfoo, bx);
    jedis.sadd(bfoo, ba);
    jedis.sadd(bfoo, bb);
    jedis.sadd(bfoo, bc);

    jedis.sadd(bbar, bc);

    jedis.sadd(bcar, ba);
    jedis.sadd(bcar, bd);

    Set<byte[]> bexpected = new HashSet<byte[]>();
    bexpected.add(bb);
    bexpected.add(bx);

    Set<byte[]> bdiff = jedis.sdiff(bfoo, bbar, bcar);
    assertByteArraySetEquals(bexpected, bdiff);

  }

  @Test
  public void sdiffstore() {
    jedis.sadd("foo", "x");
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");
    jedis.sadd("foo", "c");

    jedis.sadd("bar", "c");

    jedis.sadd("car", "a");
    jedis.sadd("car", "d");

    Set<String> expected = new HashSet<String>();
    expected.add("d");
    expected.add("a");

    long status = jedis.sdiffstore("tar", "foo", "bar", "car");
    assertEquals(2, status);
    assertEquals(expected, jedis.smembers("car"));

    // Binary
    jedis.sadd(bfoo, bx);
    jedis.sadd(bfoo, ba);
    jedis.sadd(bfoo, bb);
    jedis.sadd(bfoo, bc);

    jedis.sadd(bbar, bc);

    jedis.sadd(bcar, ba);
    jedis.sadd(bcar, bd);

    Set<byte[]> bexpected = new HashSet<byte[]>();
    bexpected.add(bd);
    bexpected.add(ba);

    long bstatus = jedis.sdiffstore("tar".getBytes(), bfoo, bbar, bcar);
    assertEquals(2, bstatus);
    assertByteArraySetEquals(bexpected, jedis.smembers(bcar));

  }

  @Test
  public void srandmember() {
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");

    String member = jedis.srandmember("foo");

    assertTrue("a".equals(member) || "b".equals(member));
    assertEquals(2, jedis.smembers("foo").size());

    member = jedis.srandmember("bar");
    assertNull(member);

    // Binary
    jedis.sadd(bfoo, ba);
    jedis.sadd(bfoo, bb);

    byte[] bmember = jedis.srandmember(bfoo);

    assertTrue(Arrays.equals(ba, bmember) || Arrays.equals(bb, bmember));
    assertEquals(2, jedis.smembers(bfoo).size());

    bmember = jedis.srandmember(bbar);
    assertNull(bmember);
  }

  @Test
  public void sscan() {
    jedis.sadd("foo", "a", "b");

    ScanResult<String> result = jedis.sscan("foo", SCAN_POINTER_START);

    assertEquals(SCAN_POINTER_START, result.getCursor());
    assertFalse(result.getResult().isEmpty());

    // binary
    jedis.sadd(bfoo, ba, bb);

    ScanResult<byte[]> bResult = jedis.sscan(bfoo, SCAN_POINTER_START_BINARY);

    assertArrayEquals(SCAN_POINTER_START_BINARY, bResult.getCursorAsBytes());
    assertFalse(bResult.getResult().isEmpty());
  }

  @Test
  public void sscanMatch() {
    ScanParams params = new ScanParams();
    params.match("a*");

    jedis.sadd("foo", "b", "a", "aa");
    ScanResult<String> result = jedis.sscan("foo", SCAN_POINTER_START, params);

    assertEquals(SCAN_POINTER_START, result.getCursor());
    assertFalse(result.getResult().isEmpty());

    // binary
    params = new ScanParams();
    params.match(bbarstar);

    jedis.sadd(bfoo, bbar1, bbar2, bbar3);
    ScanResult<byte[]> bResult = jedis.sscan(bfoo, SCAN_POINTER_START_BINARY, params);

    assertArrayEquals(SCAN_POINTER_START_BINARY, bResult.getCursorAsBytes());
    assertFalse(bResult.getResult().isEmpty());
  }

  @Test
  public void sscanCount() {
    ScanParams params = new ScanParams();
    params.count(2);

    jedis.sadd("foo", "a1", "a2", "a3", "a4", "a5");

    ScanResult<String> result = jedis.sscan("foo", SCAN_POINTER_START, params);

    assertFalse(result.getResult().isEmpty());

    // binary
    params = new ScanParams();
    params.count(2);

    jedis.sadd(bfoo, bbar1, bbar2, bbar3);
    ScanResult<byte[]> bResult = jedis.sscan(bfoo, SCAN_POINTER_START_BINARY, params);

    assertFalse(bResult.getResult().isEmpty());
  }
}
