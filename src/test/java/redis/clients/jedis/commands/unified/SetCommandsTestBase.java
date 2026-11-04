package redis.clients.jedis.commands.unified;

import static io.redis.test.utils.RedisVersion.V8_10_0_RC2_STRING;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START_BINARY;
import static redis.clients.jedis.util.AssertUtil.assertByteArrayCollectionContainsAll;
import static redis.clients.jedis.util.AssertUtil.assertByteArraySetEquals;
import static redis.clients.jedis.util.AssertUtil.assertCollectionContainsAll;
import static redis.clients.jedis.util.ByteArrayUtil.byteArrayCollectionRemoveAll;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.redis.test.annotations.EnabledOnCommand;
import io.redis.test.annotations.ConditionalOnEnv;
import io.redis.test.annotations.SinceRedisVersion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.ExpiryOption;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.SAddExParams;
import redis.clients.jedis.params.SDiffCardParams;
import redis.clients.jedis.params.SUnionCardParams;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.util.TestEnvUtil;

@Tag("integration")
public abstract class SetCommandsTestBase extends UnifiedJedisCommandsTestBase {
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

  public SetCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

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
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
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
  public void smismember() {
    jedis.sadd("foo", "a", "b");

    assertEquals(Arrays.asList(true, false), jedis.smismember("foo", "a", "c"));

    // Binary
    jedis.sadd(bfoo, ba, bb);

    assertEquals(Arrays.asList(true, false), jedis.smismember(bfoo, ba, bc));
  }

  @Test
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
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
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
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
  @SinceRedisVersion(value="7.0.0")
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
  public void sintercard() {
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");

    jedis.sadd("bar", "a");
    jedis.sadd("bar", "b");
    jedis.sadd("bar", "c");

    long card = jedis.sintercard("foo", "bar");
    assertEquals(2, card);
    long limitedCard = jedis.sintercard(1, "foo", "bar");
    assertEquals(1, limitedCard);

    // Binary
    jedis.sadd(bfoo, ba);
    jedis.sadd(bfoo, bb);

    jedis.sadd(bbar, ba);
    jedis.sadd(bbar, bb);
    jedis.sadd(bbar, bc);

    long bcard = jedis.sintercard(bfoo, bbar);
    assertEquals(2, bcard);
    long blimitedCard = jedis.sintercard(1, bfoo, bbar);
    assertEquals(1, blimitedCard);
  }

  @Test
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
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
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
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
  @SinceRedisVersion(V8_10_0_RC2_STRING)
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
  public void sunioncard() {
    String foo = keys.key("foo");
    String bar = keys.key("bar");
    jedis.sadd(foo, "a", "b", "c");
    jedis.sadd(bar, "c", "d");

    assertEquals(4, jedis.sunioncard(foo, bar));
    // non-existing keys are treated as empty sets
    assertEquals(3, jedis.sunioncard(foo, keys.key("nosuchset")));

    // Binary
    byte[] bfooKey = keys.bKey("bfoo");
    byte[] bbarKey = keys.bKey("bbar");
    jedis.sadd(bfooKey, ba, bb, bc);
    jedis.sadd(bbarKey, bc, bd);

    assertEquals(4, jedis.sunioncard(bfooKey, bbarKey));
  }

  @Test
  @SinceRedisVersion(V8_10_0_RC2_STRING)
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
  public void sunioncardKeysList() {
    String foo = keys.key("foo");
    String bar = keys.key("bar");
    jedis.sadd(foo, "a", "b", "c");
    jedis.sadd(bar, "c", "d");

    assertEquals(4, jedis.sunioncard(Arrays.asList(foo, bar)));
    // non-existing keys are treated as empty sets
    assertEquals(3, jedis.sunioncard(Arrays.asList(foo, keys.key("nosuchset"))));
  }

  @Test
  @SinceRedisVersion(V8_10_0_RC2_STRING)
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
  public void sunioncardWithParams() {
    String foo = keys.key("foo");
    String bar = keys.key("bar");
    jedis.sadd(foo, "a", "b", "c");
    jedis.sadd(bar, "c", "d");

    // APPROX is exact for small sets; LIMIT caps the result
    assertEquals(4, jedis.sunioncard(foo, bar, new SUnionCardParams().approx()));
    assertEquals(3, jedis.sunioncard(foo, bar, new SUnionCardParams().approx().limit(3)));

    // Binary
    byte[] bfooKey = keys.bKey("bfoo");
    byte[] bbarKey = keys.bKey("bbar");
    jedis.sadd(bfooKey, ba, bb, bc);
    jedis.sadd(bbarKey, bc, bd);

    assertEquals(3, jedis.sunioncard(bfooKey, bbarKey, new SUnionCardParams().limit(3)));
  }

  @Test
  @SinceRedisVersion(V8_10_0_RC2_STRING)
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
  public void sunioncardKeysListWithParams() {
    String foo = keys.key("foo");
    String bar = keys.key("bar");
    jedis.sadd(foo, "a", "b", "c");
    jedis.sadd(bar, "c", "d");

    // LIMIT caps the result; LIMIT 0 means no limit
    assertEquals(3, jedis.sunioncard(Arrays.asList(foo, bar), new SUnionCardParams().limit(3)));
    assertEquals(4, jedis.sunioncard(Arrays.asList(foo, bar), new SUnionCardParams().limit(0)));

    // Binary
    byte[] bfooKey = keys.bKey("bfoo");
    byte[] bbarKey = keys.bKey("bbar");
    jedis.sadd(bfooKey, ba, bb, bc);
    jedis.sadd(bbarKey, bc, bd);

    assertEquals(4,
      jedis.sunioncard(new byte[][] { bfooKey, bbarKey }, new SUnionCardParams().approx()));
  }

  @Test
  @SinceRedisVersion(V8_10_0_RC2_STRING)
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
  public void sunioncardWrongTypeKey() {
    String foo = keys.key("foo");
    String strkey = keys.key("strkey");
    jedis.sadd(foo, "a");
    jedis.set(strkey, "value");
    assertThrows(JedisDataException.class, () -> jedis.sunioncard(foo, strkey));
  }

  @Test
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
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
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
  public void sdiffstore() {
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

    long status = jedis.sdiffstore("tar", "foo", "bar", "car");
    assertEquals(2, status);
    assertEquals(expected, jedis.smembers("tar"));

    // Binary
    jedis.sadd(bfoo, bx);
    jedis.sadd(bfoo, ba);
    jedis.sadd(bfoo, bb);
    jedis.sadd(bfoo, bc);

    jedis.sadd(bbar, bc);

    jedis.sadd(bcar, ba);
    jedis.sadd(bcar, bd);

    Set<byte[]> bexpected = new HashSet<byte[]>();
    bexpected.add(bx);
    bexpected.add(bb);

    long bstatus = jedis.sdiffstore("tar".getBytes(), bfoo, bbar, bcar);
    assertEquals(2, bstatus);
    assertByteArraySetEquals(bexpected, jedis.smembers("tar".getBytes()));

  }

  @Test
  @SinceRedisVersion(V8_10_0_RC2_STRING)
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
  public void sdiffcard() {
    String foo = keys.key("foo");
    String bar = keys.key("bar");
    String car = keys.key("car");
    jedis.sadd(foo, "x", "a", "b", "c");
    jedis.sadd(bar, "c");
    jedis.sadd(car, "a", "d");

    // foo \ (bar U car) = {x, b}
    assertEquals(2, jedis.sdiffcard(foo, bar, car));

    // a missing subtrahend key has no effect; a missing first key yields 0
    assertEquals(4, jedis.sdiffcard(foo, keys.key("nosuchset")));
    assertEquals(0, jedis.sdiffcard(keys.key("nosuchset"), foo));

    // Binary
    byte[] bfooKey = keys.bKey("bfoo");
    byte[] bbarKey = keys.bKey("bbar");
    byte[] bcarKey = keys.bKey("bcar");
    jedis.sadd(bfooKey, bx, ba, bb, bc);
    jedis.sadd(bbarKey, bc);
    jedis.sadd(bcarKey, ba, bd);

    assertEquals(2, jedis.sdiffcard(bfooKey, bbarKey, bcarKey));
  }

  @Test
  @SinceRedisVersion(V8_10_0_RC2_STRING)
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
  public void sdiffcardKeysList() {
    String foo = keys.key("foo");
    String bar = keys.key("bar");
    String car = keys.key("car");
    jedis.sadd(foo, "x", "a", "b", "c");
    jedis.sadd(bar, "c");
    jedis.sadd(car, "a", "d");

    // foo \ (bar U car) = {x, b}
    assertEquals(2, jedis.sdiffcard(Arrays.asList(foo, bar, car)));
    // a missing first key yields 0
    assertEquals(0, jedis.sdiffcard(Arrays.asList(keys.key("nosuchset"), foo)));
  }

  @Test
  @SinceRedisVersion(V8_10_0_RC2_STRING)
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
  public void sdiffcardWithParams() {
    String foo = keys.key("foo");
    String bar = keys.key("bar");
    jedis.sadd(foo, "x", "a", "b", "c");
    jedis.sadd(bar, "c");

    // foo \ bar = {x, a, b}; LIMIT caps the result
    assertEquals(1, jedis.sdiffcard(foo, bar, new SDiffCardParams().limit(1)));

    // Binary
    byte[] bfooKey = keys.bKey("bfoo");
    byte[] bbarKey = keys.bKey("bbar");
    jedis.sadd(bfooKey, bx, ba, bb, bc);
    jedis.sadd(bbarKey, bc);

    assertEquals(1, jedis.sdiffcard(bfooKey, bbarKey, new SDiffCardParams().limit(1)));
  }

  @Test
  @SinceRedisVersion(V8_10_0_RC2_STRING)
  @ConditionalOnEnv(value = TestEnvUtil.ENV_REDIS_ENTERPRISE, enabled = false)
  public void sdiffcardKeysListWithParams() {
    String foo = keys.key("foo");
    String bar = keys.key("bar");
    String car = keys.key("car");
    jedis.sadd(foo, "x", "a", "b", "c");
    jedis.sadd(bar, "c");
    jedis.sadd(car, "a", "d");

    // LIMIT caps the result; LIMIT 0 means no limit
    assertEquals(1, jedis.sdiffcard(Arrays.asList(foo, bar, car), new SDiffCardParams().limit(1)));
    assertEquals(2, jedis.sdiffcard(Arrays.asList(foo, bar, car), new SDiffCardParams().limit(0)));

    // Binary
    byte[] bfooKey = keys.bKey("bfoo");
    byte[] bbarKey = keys.bKey("bbar");
    byte[] bcarKey = keys.bKey("bcar");
    jedis.sadd(bfooKey, bx, ba, bb, bc);
    jedis.sadd(bbarKey, bc);
    jedis.sadd(bcarKey, ba, bd);

    assertEquals(2,
      jedis.sdiffcard(new byte[][] { bfooKey, bbarKey, bcarKey }, new SDiffCardParams().limit(0)));
  }

  @Test
  public void srandmember() {
    jedis.sadd("foo", "a");
    jedis.sadd("foo", "b");

    String member = jedis.srandmember("foo");

    assertTrue("a".equals(member) || "b".equals(member));
    assertEquals(2, jedis.smembers("foo").size());

    List<String> members = jedis.srandmember("foo", 2);
    members.sort(Comparator.naturalOrder());
    assertEquals( Arrays.asList("a", "b"), members);

    member = jedis.srandmember("bar");
    assertNull(member);

    members = jedis.srandmember("bar", 2);
    assertEquals(0, members.size());

    // Binary
    jedis.sadd(bfoo, ba);
    jedis.sadd(bfoo, bb);

    byte[] bmember = jedis.srandmember(bfoo);

    assertTrue(Arrays.equals(ba, bmember) || Arrays.equals(bb, bmember));
    assertEquals(2, jedis.smembers(bfoo).size());

    List<byte[]> bmembers = jedis.srandmember(bfoo, 2);
    assertEquals(2, bmembers.size());

    bmember = jedis.srandmember(bbar);
    assertNull(bmember);

    members = jedis.srandmember("bbar", 2);
    assertEquals(0, members.size());
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

  // Set member expiration (Redis 8.12). Expected codes come from the server's
  // tests/unit/type/set-member-expire.tcl.

  @Test
  @EnabledOnCommand("SADDEX")
  public void sttlFamilyMissingKey() {
    List<Long> missing = Arrays.asList(-2L, -2L, -2L);
    assertEquals(missing, jedis.sttl("foo", "a", "b", "c"));
    assertEquals(missing, jedis.spttl("foo", "a", "b", "c"));
    assertEquals(missing, jedis.sexpireTime("foo", "a", "b", "c"));
    assertEquals(missing, jedis.spexpireTime("foo", "a", "b", "c"));
    assertEquals(missing, jedis.spersist("foo", "a", "b", "c"));
    assertEquals(Arrays.asList(-2L, -2L), jedis.sexpire("foo", 1000, "a", "b"));
  }

  @Test
  @EnabledOnCommand("SADDEX")
  public void sexpireAndSttl() {
    long seconds = 20;
    jedis.sadd("foo", "a", "b", "c");
    assertEquals(Arrays.asList(1L, -2L), jedis.sexpire("foo", seconds, "a", "nosuch"));

    assertThat(jedis.sttl("foo", "a", "b", "nosuch"), contains(
      both(greaterThanOrEqualTo(seconds - 1)).and(lessThanOrEqualTo(seconds)), equalTo(-1L),
      equalTo(-2L)));
  }

  @Test
  @EnabledOnCommand("SADDEX")
  public void sexpireAndSttlBinary() {
    long seconds = 20;
    jedis.sadd(bfoo, ba, bb, bc);
    assertEquals(Arrays.asList(1L, -2L), jedis.sexpire(bfoo, seconds, ba, bx));

    assertThat(jedis.sttl(bfoo, ba, bb, bx), contains(
      both(greaterThanOrEqualTo(seconds - 1)).and(lessThanOrEqualTo(seconds)), equalTo(-1L),
      equalTo(-2L)));
  }

  @Test
  @EnabledOnCommand("SADDEX")
  public void sexpireConditionsPerMember() {
    jedis.sadd("foo", "a", "b", "c");
    long base = System.currentTimeMillis() + 100_000_000L;
    assertEquals(Arrays.asList(1L), jedis.spexpireAt("foo", base, "b"));

    // NX: only members without an expiration
    assertEquals(Arrays.asList(1L, 0L, -2L),
      jedis.spexpireAt("foo", base, ExpiryOption.NX, "a", "b", "nosuch"));
    // XX: only members with an expiration
    assertEquals(Arrays.asList(0L, 1L),
      jedis.spexpireAt("foo", base + 1000, ExpiryOption.XX, "c", "a"));
    // GT: a member without an expiration counts as never expiring, so GT never holds for it
    assertEquals(Arrays.asList(0L, 1L),
      jedis.spexpireAt("foo", base + 500, ExpiryOption.GT, "a", "b"));
    assertEquals(Arrays.asList(0L), jedis.spexpireAt("foo", base + 500, ExpiryOption.GT, "c"));
    // LT: always holds for a member without an expiration
    assertEquals(Arrays.asList(1L, 1L),
      jedis.spexpireAt("foo", base + 100, ExpiryOption.LT, "a", "c"));
    assertEquals(Arrays.asList(0L), jedis.spexpireAt("foo", base + 100, ExpiryOption.LT, "a"));
  }

  @Test
  @EnabledOnCommand("SADDEX")
  public void sexpirePastTimeDeletes() {
    jedis.sadd("foo", "a", "b", "c");
    assertEquals(Arrays.asList(2L, -2L), jedis.sexpireAt("foo", 1, "a", "nosuch"));
    assertEquals(new HashSet<>(Arrays.asList("b", "c")), jedis.smembers("foo"));

    assertEquals(Arrays.asList(2L, 2L), jedis.spexpireAt("foo", 1, "b", "c"));
    assertFalse(jedis.exists("foo"));
  }

  @Test
  @EnabledOnCommand("SADDEX")
  public void sexpirePastTimeDeletesBinary() {
    jedis.sadd(bfoo, ba, bb, bc);
    assertEquals(Arrays.asList(2L, -2L), jedis.sexpireAt(bfoo, 1, ba, bx));
    assertByteArraySetEquals(new HashSet<>(Arrays.asList(bb, bc)), jedis.smembers(bfoo));

    assertEquals(Arrays.asList(2L, 2L), jedis.spexpireAt(bfoo, 1, bb, bc));
    assertFalse(jedis.exists(bfoo));
  }

  @Test
  @EnabledOnCommand("SADDEX")
  public void spexpireAndSpttl() {
    long millis = 20_000;
    jedis.sadd("foo", "a");
    assertEquals(Arrays.asList(1L), jedis.spexpire("foo", millis, "a"));
    assertThat(jedis.spttl("foo", "a"),
      contains(both(greaterThan(millis - 1000)).and(lessThanOrEqualTo(millis))));
  }

  @Test
  @EnabledOnCommand("SADDEX")
  public void sexpireAtAndSexpireTime() {
    long at = System.currentTimeMillis() / 1000 + 1000;
    jedis.sadd("foo", "a", "b");
    assertEquals(Arrays.asList(1L), jedis.sexpireAt("foo", at, "a"));
    assertEquals(Arrays.asList(at, -1L), jedis.sexpireTime("foo", "a", "b"));
  }

  @Test
  @EnabledOnCommand("SADDEX")
  public void spexpireAtAndSpexpireTime() {
    long at = System.currentTimeMillis() + 1_000_000;
    jedis.sadd("foo", "a", "b");
    assertEquals(Arrays.asList(1L), jedis.spexpireAt("foo", at, "a"));
    assertEquals(Arrays.asList(at, -1L), jedis.spexpireTime("foo", "a", "b"));
  }

  @Test
  @EnabledOnCommand("SADDEX")
  public void spersist() {
    jedis.sadd("foo", "a", "b", "c");
    assertEquals(Arrays.asList(1L, 1L), jedis.sexpire("foo", 1000, "a", "b"));

    assertEquals(Arrays.asList(1L, -1L, -2L, 1L), jedis.spersist("foo", "a", "c", "nosuch", "b"));
    assertEquals(Arrays.asList(-1L, -1L, -1L), jedis.sttl("foo", "a", "b", "c"));
  }

  @Test
  @EnabledOnCommand("SADDEX")
  public void spersistBinary() {
    jedis.sadd(bfoo, ba, bb, bc);
    assertEquals(Arrays.asList(1L, 1L), jedis.sexpire(bfoo, 1000, ba, bb));

    assertEquals(Arrays.asList(1L, -1L, -2L, 1L), jedis.spersist(bfoo, ba, bc, bx, bb));
    assertEquals(Arrays.asList(-1L, -1L, -1L), jedis.sttl(bfoo, ba, bb, bc));
  }

  @Test
  @EnabledOnCommand("SADDEX")
  public void saddexAddsWithExpiration() {
    assertEquals(1, jedis.saddex("foo", SAddExParams.sAddExParams().ex(1000), "a", "b", "c"));
    assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), jedis.smembers("foo"));
    assertThat(jedis.sttl("foo", "a", "b", "c"), everyItem(greaterThan(900L)));

    assertEquals(1, jedis.saddex("foo", SAddExParams.sAddExParams().px(2_000_000), "d"));
    assertThat(jedis.spttl("foo", "d"), contains(greaterThan(1_900_000L)));

    long exAt = System.currentTimeMillis() / 1000 + 1000;
    assertEquals(1, jedis.saddex("foo", SAddExParams.sAddExParams().exAt(exAt), "e"));
    assertEquals(Arrays.asList(exAt), jedis.sexpireTime("foo", "e"));

    long pxAt = System.currentTimeMillis() + 1_000_000;
    assertEquals(1, jedis.saddex("foo", SAddExParams.sAddExParams().pxAt(pxAt), "f"));
    assertEquals(Arrays.asList(pxAt), jedis.spexpireTime("foo", "f"));
  }

  @Test
  @EnabledOnCommand("SADDEX")
  public void saddexAddsWithExpirationBinary() {
    assertEquals(1, jedis.saddex(bfoo, SAddExParams.sAddExParams().ex(1000), ba, bb, bc));
    assertByteArraySetEquals(new HashSet<>(Arrays.asList(ba, bb, bc)), jedis.smembers(bfoo));
    assertThat(jedis.sttl(bfoo, ba, bb, bc), everyItem(greaterThan(900L)));

    assertEquals(1, jedis.saddex(bfoo, SAddExParams.sAddExParams().px(2_000_000), bd));
    assertThat(jedis.spttl(bfoo, bd), contains(greaterThan(1_900_000L)));
  }

  @Test
  @EnabledOnCommand("SADDEX")
  public void saddexExistingMemberExpiration() {
    jedis.sadd("foo", "a", "b", "c");
    jedis.sexpire("foo", 1000, "a", "b", "c");

    // an existing member gets the new expiration
    assertEquals(1, jedis.saddex("foo", SAddExParams.sAddExParams().ex(5000), "a"));
    assertThat(jedis.sttl("foo", "a"), contains(greaterThan(4000L)));
    // without an expiration option the existing expiration is discarded
    assertEquals(1, jedis.saddex("foo", SAddExParams.sAddExParams(), "b"));
    assertEquals(Arrays.asList(-1L), jedis.sttl("foo", "b"));
    // KEEPTTL keeps it; a new member gets none
    assertEquals(1, jedis.saddex("foo", SAddExParams.sAddExParams().keepTtl(), "c", "new"));
    assertThat(jedis.sttl("foo", "c", "new"), contains(greaterThan(900L), equalTo(-1L)));
  }

  @Test
  @EnabledOnCommand("SADDEX")
  public void saddexConditions() {
    jedis.sadd("foo", "a", "b");

    // MNX: none of the members may exist
    assertEquals(0, jedis.saddex("foo", SAddExParams.sAddExParams().mnx().ex(1000), "a", "x"));
    assertFalse(jedis.sismember("foo", "x"));
    assertEquals(1, jedis.saddex("foo", SAddExParams.sAddExParams().mnx().ex(1000), "x", "y"));
    assertThat(jedis.sttl("foo", "x", "y"), everyItem(greaterThan(900L)));

    // MXX: all of the members must exist
    assertEquals(0, jedis.saddex("foo", SAddExParams.sAddExParams().mxx().ex(1000), "a", "nosuch"));
    assertEquals(Arrays.asList(-1L), jedis.sttl("foo", "a"));
    assertEquals(1, jedis.saddex("foo", SAddExParams.sAddExParams().mxx().ex(1000), "a", "b"));
    assertThat(jedis.sttl("foo", "a", "b"), everyItem(greaterThan(900L)));
  }

  @Test
  @EnabledOnCommand("SADDEX")
  public void saddexConditionsBinary() {
    jedis.sadd(bfoo, ba, bb);

    assertEquals(0, jedis.saddex(bfoo, SAddExParams.sAddExParams().mnx().ex(1000), ba, bx));
    assertFalse(jedis.sismember(bfoo, bx));
    assertEquals(1, jedis.saddex(bfoo, SAddExParams.sAddExParams().mnx().ex(1000), bx, bd));

    assertEquals(0, jedis.saddex(bfoo, SAddExParams.sAddExParams().mxx().ex(1000), ba, bc));
    assertEquals(Arrays.asList(-1L), jedis.sttl(bfoo, ba));
    assertEquals(1, jedis.saddex(bfoo, SAddExParams.sAddExParams().mxx().ex(1000), ba, bb));
    assertThat(jedis.sttl(bfoo, ba, bb), everyItem(greaterThan(900L)));
  }

  @Test
  @EnabledOnCommand("SADDEX")
  public void saddexPastTimeDeletes() {
    jedis.sadd("foo", "a", "b", "c");
    // existing members are deleted, the new one is not added
    assertEquals(1, jedis.saddex("foo", SAddExParams.sAddExParams().pxAt(1), "a", "new"));
    assertEquals(new HashSet<>(Arrays.asList("b", "c")), jedis.smembers("foo"));

    jedis.del("foo");
    assertEquals(0, jedis.saddex("foo", SAddExParams.sAddExParams().pxAt(1), "a", "b"));
    assertFalse(jedis.exists("foo"));

    jedis.sadd("foo", "a", "b");
    assertEquals(1, jedis.saddex("foo", SAddExParams.sAddExParams().exAt(1), "a", "b"));
    assertFalse(jedis.exists("foo"));
  }

  @Test
  @EnabledOnCommand("SADDEX")
  public void saddexRepliesZeroWhenNothingModified() {
    long when = System.currentTimeMillis() + 1_000_000;
    assertEquals(1, jedis.saddex("foo", SAddExParams.sAddExParams().pxAt(when), "a", "b"));
    // the same members with the same expiration: nothing changes
    assertEquals(0, jedis.saddex("foo", SAddExParams.sAddExParams().pxAt(when), "a", "b"));
    // one new member is enough
    assertEquals(1, jedis.saddex("foo", SAddExParams.sAddExParams().pxAt(when), "a", "c"));
    // a different expiration is a modification
    assertEquals(1, jedis.saddex("foo", SAddExParams.sAddExParams().pxAt(when + 1), "a"));

    jedis.del("foo");
    jedis.sadd("foo", "a", "b");
    // members without an expiration, no expiration given or kept: nothing changes
    assertEquals(0, jedis.saddex("foo", SAddExParams.sAddExParams(), "a", "b"));
    assertEquals(0, jedis.saddex("foo", SAddExParams.sAddExParams().keepTtl(), "a", "b"));
  }

  @Test
  @EnabledOnCommand("SADDEX")
  public void saddexMxxMissingKey() {
    assertEquals(0, jedis.saddex("foo", SAddExParams.sAddExParams().mxx(), "a"));
    assertFalse(jedis.exists("foo"));
  }

  @Test
  @EnabledOnCommand("SADDEX")
  public void sexpireWrongType() {
    jedis.set("foo", "bar");
    assertWrongType(() -> jedis.sexpire("foo", 10, "a"));
    assertWrongType(() -> jedis.sttl("foo", "a"));
    assertWrongType(() -> jedis.spersist("foo", "a"));
    assertWrongType(() -> jedis.saddex("foo", SAddExParams.sAddExParams(), "a"));
  }

  private static void assertWrongType(org.junit.jupiter.api.function.Executable executable) {
    JedisDataException e = assertThrows(JedisDataException.class, executable);
    assertTrue(e.getMessage().startsWith("WRONGTYPE"), e.getMessage());
  }
}
