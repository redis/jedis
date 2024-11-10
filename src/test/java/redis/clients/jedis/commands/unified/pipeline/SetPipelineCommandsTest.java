package redis.clients.jedis.commands.unified.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START_BINARY;
import static redis.clients.jedis.util.AssertUtil.assertByteArrayCollectionContainsAll;
import static redis.clients.jedis.util.AssertUtil.assertByteArraySetEquals;
import static redis.clients.jedis.util.AssertUtil.assertCollectionContainsAll;
import static redis.clients.jedis.util.ByteArrayUtil.byteArrayCollectionRemoveAll;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.Response;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

@RunWith(Parameterized.class)
public class SetPipelineCommandsTest extends PipelineCommandsTestBase {

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

  public SetPipelineCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void sadd() {
    pipe.sadd("foo", "a");
    pipe.sadd("foo", "a");

    assertThat(pipe.syncAndReturnAll(), contains(
        1L,
        0L
    ));

    pipe.sadd(bfoo, ba);
    pipe.sadd(bfoo, ba);

    assertThat(pipe.syncAndReturnAll(), contains(
        1L,
        0L
    ));
  }

  @Test
  public void smembers() {
    pipe.sadd("foo", "a");
    pipe.sadd("foo", "b");

    Response<Set<String>> members = pipe.smembers("foo");

    pipe.sync();

    assertThat(members.get(), containsInAnyOrder("a", "b"));

    // Binary
    pipe.sadd(bfoo, ba);
    pipe.sadd(bfoo, bb);

    Response<Set<byte[]>> bmembers = pipe.smembers(bfoo);

    pipe.sync();

    assertThat(bmembers.get(), containsInAnyOrder(ba, bb));
  }

  @Test
  public void srem() {
    pipe.sadd("foo", "a");
    pipe.sadd("foo", "b");

    Response<Long> status1 = pipe.srem("foo", "a");
    Response<Set<String>> members = pipe.smembers("foo");
    Response<Long> status2 = pipe.srem("foo", "bar");

    pipe.sync();

    assertThat(status1.get(), equalTo(1L));
    assertThat(members.get(), containsInAnyOrder("b"));
    assertThat(status2.get(), equalTo(0L));

    // Binary

    pipe.sadd(bfoo, ba);
    pipe.sadd(bfoo, bb);

    Response<Long> bstatus1 = pipe.srem(bfoo, ba);
    Response<Set<byte[]>> bmembers = pipe.smembers(bfoo);
    Response<Long> bstatus2 = pipe.srem(bfoo, bbar);

    pipe.sync();

    assertThat(bstatus1.get(), equalTo(1L));
    assertThat(bmembers.get(), containsInAnyOrder(bb));
    assertThat(bstatus2.get(), equalTo(0L));
  }

  @Test
  public void spop() {
    pipe.sadd("foo", "a");
    pipe.sadd("foo", "b");

    Response<String> member1 = pipe.spop("foo");
    Response<Set<String>> members = pipe.smembers("foo");
    Response<String> member2 = pipe.spop("bar");

    pipe.sync();

    assertThat(member1.get(), anyOf(equalTo("a"), equalTo("b")));
    assertThat(members.get(), hasSize(1));
    assertThat(member2.get(), nullValue());

    // Binary
    pipe.sadd(bfoo, ba);
    pipe.sadd(bfoo, bb);

    Response<byte[]> bmember1 = pipe.spop(bfoo);
    Response<Set<byte[]>> bmembers = pipe.smembers(bfoo);
    Response<byte[]> bmember2 = pipe.spop(bbar);

    pipe.sync();

    assertThat(bmember1.get(), anyOf(equalTo(ba), equalTo(bb)));
    assertThat(bmembers.get(), hasSize(1));
    assertThat(bmember2.get(), nullValue());
  }

  @Test
  public void spopWithCount() {
    pipe.sadd("foo", "a");
    pipe.sadd("foo", "b");
    pipe.sadd("foo", "c");

    Response<Set<String>> members1 = pipe.spop("foo", 2);
    Response<Set<String>> members2 = pipe.spop("foo", 2);
    Response<Set<String>> members3 = pipe.spop("foo", 2);

    pipe.sync();

    assertThat(members1.get(), hasSize(2));
    assertThat(members2.get(), hasSize(1));
    assertThat(members3.get(), empty());

    Set<String> superSet = new HashSet<>();
    superSet.add("c");
    superSet.add("b");
    superSet.add("a");

    assertCollectionContainsAll(superSet, members1.get());
    superSet.removeAll(members1.get());

    assertThat(members2.get(), equalTo(superSet));

    // Binary
    pipe.sadd(bfoo, ba);
    pipe.sadd(bfoo, bb);
    pipe.sadd(bfoo, bc);

    Response<Set<byte[]>> bmembers1 = pipe.spop(bfoo, 2);
    Response<Set<byte[]>> bmembers2 = pipe.spop(bfoo, 2);
    Response<Set<byte[]>> bmembers3 = pipe.spop(bfoo, 2);

    pipe.sync();

    assertThat(bmembers1.get(), hasSize(2));
    assertThat(bmembers2.get(), hasSize(1));
    assertThat(bmembers3.get(), empty());

    Set<byte[]> bsuperSet = new HashSet<>();
    bsuperSet.add(bc);
    bsuperSet.add(bb);
    bsuperSet.add(ba);

    assertByteArrayCollectionContainsAll(bsuperSet, bmembers1.get());
    byteArrayCollectionRemoveAll(bsuperSet, bmembers1.get());

    assertByteArraySetEquals(bsuperSet, bmembers2.get());
  }

  @Test
  public void smove() {
    pipe.sadd("foo", "a");
    pipe.sadd("foo", "b");

    pipe.sadd("bar", "c");

    Response<Long> status1 = pipe.smove("foo", "bar", "a");
    Response<Set<String>> srcMembers = pipe.smembers("foo");
    Response<Set<String>> dstMembers = pipe.smembers("bar");
    Response<Long> status2 = pipe.smove("foo", "bar", "a");

    pipe.sync();

    assertThat(status1.get(), equalTo(1L));
    assertThat(srcMembers.get(), containsInAnyOrder("b"));
    assertThat(dstMembers.get(), containsInAnyOrder("a", "c"));
    assertThat(status2.get(), equalTo(0L));

    // Binary
    pipe.sadd(bfoo, ba);
    pipe.sadd(bfoo, bb);

    pipe.sadd(bbar, bc);

    Response<Long> bstatus1 = pipe.smove(bfoo, bbar, ba);
    Response<Set<byte[]>> bsrcMembers = pipe.smembers(bfoo);
    Response<Set<byte[]>> bdstMembers = pipe.smembers(bbar);
    Response<Long> bstatus2 = pipe.smove(bfoo, bbar, ba);

    pipe.sync();

    assertThat(bstatus1.get(), equalTo(1L));
    assertThat(bsrcMembers.get(), containsInAnyOrder(bb));
    assertThat(bdstMembers.get(), containsInAnyOrder(ba, bc));
    assertThat(bstatus2.get(), equalTo(0L));
  }

  @Test
  public void scard() {
    pipe.sadd("foo", "a");
    pipe.sadd("foo", "b");
    pipe.sync();

    pipe.scard("foo");
    pipe.scard("bar");

    assertThat(pipe.syncAndReturnAll(), contains(
        2L,
        0L
    ));

    // Binary
    pipe.sadd(bfoo, ba);
    pipe.sadd(bfoo, bb);
    pipe.sync();

    pipe.scard(bfoo);
    pipe.scard(bbar);

    assertThat(pipe.syncAndReturnAll(), contains(
        2L,
        0L
    ));
  }

  @Test
  public void sismember() {
    pipe.sadd("foo", "a");
    pipe.sadd("foo", "b");
    pipe.sync();

    pipe.sismember("foo", "a");
    pipe.sismember("foo", "c");

    assertThat(pipe.syncAndReturnAll(), contains(
        true,
        false
    ));

    // Binary
    pipe.sadd(bfoo, ba);
    pipe.sadd(bfoo, bb);
    pipe.sync();

    pipe.sismember(bfoo, ba);
    pipe.sismember(bfoo, bc);

    assertThat(pipe.syncAndReturnAll(), contains(
        true,
        false
    ));
  }

  @Test
  public void smismember() {
    pipe.sadd("foo", "a", "b");

    Response<List<Boolean>> response = pipe.smismember("foo", "a", "c");

    pipe.sync();

    assertThat(response.get(), contains(true, false));

    // Binary
    pipe.sadd(bfoo, ba, bb);

    Response<List<Boolean>> bresponse = pipe.smismember(bfoo, ba, bc);

    pipe.sync();

    assertThat(bresponse.get(), contains(true, false));
  }

  @Test
  public void sinter() {
    pipe.sadd("foo", "a");
    pipe.sadd("foo", "b");

    pipe.sadd("bar", "b");
    pipe.sadd("bar", "c");

    Response<Set<String>> intersection = pipe.sinter("foo", "bar");

    pipe.sync();

    assertThat(intersection.get(), containsInAnyOrder("b"));

    // Binary
    pipe.sadd(bfoo, ba);
    pipe.sadd(bfoo, bb);

    pipe.sadd(bbar, bb);
    pipe.sadd(bbar, bc);

    Response<Set<byte[]>> bintersection = pipe.sinter(bfoo, bbar);

    pipe.sync();

    assertThat(bintersection.get(), containsInAnyOrder(bb));
  }

  @Test
  public void sinterstore() {
    pipe.sadd("foo", "a");
    pipe.sadd("foo", "b");

    pipe.sadd("bar", "b");
    pipe.sadd("bar", "c");

    Response<Long> status = pipe.sinterstore("car", "foo", "bar");
    Response<Set<String>> members = pipe.smembers("car");

    pipe.sync();

    assertThat(status.get(), equalTo(1L));
    assertThat(members.get(), containsInAnyOrder("b"));

    // Binary
    pipe.sadd(bfoo, ba);
    pipe.sadd(bfoo, bb);

    pipe.sadd(bbar, bb);
    pipe.sadd(bbar, bc);

    Response<Long> bstatus = pipe.sinterstore(bcar, bfoo, bbar);
    Response<Set<byte[]>> bmembers = pipe.smembers(bcar);

    pipe.sync();

    assertThat(bstatus.get(), equalTo(1L));
    assertThat(bmembers.get(), containsInAnyOrder(bb));
  }

  @Test
  @SinceRedisVersion(value="7.0.0")
  public void sintercard() {
    pipe.sadd("foo", "a");
    pipe.sadd("foo", "b");

    pipe.sadd("bar", "a");
    pipe.sadd("bar", "b");
    pipe.sadd("bar", "c");

    Response<Long> card = pipe.sintercard("foo", "bar");
    Response<Long> limitedCard = pipe.sintercard(1, "foo", "bar");

    pipe.sync();

    assertThat(card.get(), equalTo(2L));
    assertThat(limitedCard.get(), equalTo(1L));

    // Binary
    pipe.sadd(bfoo, ba);
    pipe.sadd(bfoo, bb);

    pipe.sadd(bbar, ba);
    pipe.sadd(bbar, bb);
    pipe.sadd(bbar, bc);

    Response<Long> bcard = pipe.sintercard(bfoo, bbar);
    Response<Long> blimitedCard = pipe.sintercard(1, bfoo, bbar);

    pipe.sync();

    assertThat(bcard.get(), equalTo(2L));
    assertThat(blimitedCard.get(), equalTo(1L));
  }

  @Test
  public void sunion() {
    pipe.sadd("foo", "a");
    pipe.sadd("foo", "b");

    pipe.sadd("bar", "b");
    pipe.sadd("bar", "c");

    Response<Set<String>> union = pipe.sunion("foo", "bar");

    pipe.sync();

    assertThat(union.get(), containsInAnyOrder("a", "b", "c"));

    // Binary
    pipe.sadd(bfoo, ba);
    pipe.sadd(bfoo, bb);

    pipe.sadd(bbar, bb);
    pipe.sadd(bbar, bc);

    Response<Set<byte[]>> bunion = pipe.sunion(bfoo, bbar);

    pipe.sync();

    assertThat(bunion.get(), containsInAnyOrder(ba, bb, bc));
  }

  @Test
  public void sunionstore() {
    pipe.sadd("foo", "a");
    pipe.sadd("foo", "b");

    pipe.sadd("bar", "b");
    pipe.sadd("bar", "c");

    Response<Long> status = pipe.sunionstore("car", "foo", "bar");
    Response<Set<String>> members = pipe.smembers("car");

    pipe.sync();

    assertThat(status.get(), equalTo(3L));
    assertThat(members.get(), containsInAnyOrder("a", "b", "c"));

    // Binary
    pipe.sadd(bfoo, ba);
    pipe.sadd(bfoo, bb);

    pipe.sadd(bbar, bb);
    pipe.sadd(bbar, bc);

    Response<Long> bstatus = pipe.sunionstore(bcar, bfoo, bbar);
    Response<Set<byte[]>> bmembers = pipe.smembers(bcar);

    pipe.sync();

    assertThat(bstatus.get(), equalTo(3L));
    assertThat(bmembers.get(), containsInAnyOrder(ba, bb, bc));
  }

  @Test
  public void sdiff() {
    pipe.sadd("foo", "x");
    pipe.sadd("foo", "a");
    pipe.sadd("foo", "b");
    pipe.sadd("foo", "c");

    pipe.sadd("bar", "c");

    pipe.sadd("car", "a");
    pipe.sadd("car", "d");

    Response<Set<String>> diff = pipe.sdiff("foo", "bar", "car");

    pipe.sync();

    assertThat(diff.get(), containsInAnyOrder("b", "x"));

    // Binary
    pipe.sadd(bfoo, bx);
    pipe.sadd(bfoo, ba);
    pipe.sadd(bfoo, bb);
    pipe.sadd(bfoo, bc);

    pipe.sadd(bbar, bc);

    pipe.sadd(bcar, ba);
    pipe.sadd(bcar, bd);

    Response<Set<byte[]>> bdiff = pipe.sdiff(bfoo, bbar, bcar);

    pipe.sync();

    assertThat(bdiff.get(), containsInAnyOrder(bb, bx));
  }

  @Test
  public void sdiffstore() {
    pipe.sadd("foo", "x");
    pipe.sadd("foo", "a");
    pipe.sadd("foo", "b");
    pipe.sadd("foo", "c");

    pipe.sadd("bar", "c");

    pipe.sadd("car", "a");
    pipe.sadd("car", "d");

    Response<Long> status = pipe.sdiffstore("tar", "foo", "bar", "car");
    Response<Set<String>> members = pipe.smembers("tar");

    pipe.sync();

    assertThat(status.get(), equalTo(2L));
    assertThat(members.get(), containsInAnyOrder("b", "x"));

    // Binary
    pipe.sadd(bfoo, bx);
    pipe.sadd(bfoo, ba);
    pipe.sadd(bfoo, bb);
    pipe.sadd(bfoo, bc);

    pipe.sadd(bbar, bc);

    pipe.sadd(bcar, ba);
    pipe.sadd(bcar, bd);

    Response<Long> bstatus = pipe.sdiffstore("tar".getBytes(), bfoo, bbar, bcar);
    Response<Set<byte[]>> bmembers = pipe.smembers("tar".getBytes());

    pipe.sync();

    assertThat(bstatus.get(), equalTo(2L));
    assertThat(bmembers.get(), containsInAnyOrder(bb, bx));
  }

  @Test
  public void srandmember() {
    pipe.sadd("foo", "a");
    pipe.sadd("foo", "b");

    Response<String> member1 = pipe.srandmember("foo");
    Response<Set<String>> allMembers = pipe.smembers("foo");
    Response<List<String>> members1 = pipe.srandmember("foo", 2);
    Response<String> member2 = pipe.srandmember("bar");
    Response<List<String>> members2 = pipe.srandmember("bar", 2);

    pipe.sync();

    assertThat(member1.get(), anyOf(equalTo("a"), equalTo("b")));
    assertThat(allMembers.get(), containsInAnyOrder("a", "b"));
    assertThat(members1.get(), containsInAnyOrder("a", "b"));
    assertThat(member2.get(), nullValue());
    assertThat(members2.get(), empty());

    // Binary
    pipe.sadd(bfoo, ba);
    pipe.sadd(bfoo, bb);

    Response<byte[]> bmember1 = pipe.srandmember(bfoo);
    Response<List<byte[]>> bmembers1 = pipe.srandmember(bfoo, 2);
    Response<byte[]> bmember2 = pipe.srandmember(bbar);
    Response<List<String>> bmembers2 = pipe.srandmember("bbar", 2);

    pipe.sync();

    assertThat(bmember1.get(), anyOf(equalTo(ba), equalTo(bb)));
    assertThat(bmembers1.get(), containsInAnyOrder(ba, bb));
    assertThat(bmember2.get(), nullValue());
    assertThat(bmembers2.get(), empty());
  }

  @Test
  public void sscan() {
    pipe.sadd("foo", "a", "b");

    Response<ScanResult<String>> result = pipe.sscan("foo", SCAN_POINTER_START);

    pipe.sync();

    assertThat(result.get().getCursor(), equalTo(SCAN_POINTER_START));
    assertThat(result.get().getResult(), not(empty()));

    // binary
    pipe.sadd(bfoo, ba, bb);

    Response<ScanResult<byte[]>> bResult = pipe.sscan(bfoo, SCAN_POINTER_START_BINARY);

    pipe.sync();

    assertThat(bResult.get().getCursor(), equalTo(SCAN_POINTER_START));
    assertThat(bResult.get().getResult(), not(empty()));
  }

  @Test
  public void sscanMatch() {
    ScanParams params = new ScanParams();
    params.match("a*");

    pipe.sadd("foo", "b", "a", "aa");
    Response<ScanResult<String>> result = pipe.sscan("foo", SCAN_POINTER_START, params);

    pipe.sync();

    assertThat(result.get().getCursor(), equalTo(SCAN_POINTER_START));
    assertThat(result.get().getResult(), not(empty()));

    // binary
    pipe.sadd(bfoo, bbar1, bbar2, bbar3);

    params = new ScanParams();
    params.match(bbarstar);

    Response<ScanResult<byte[]>> bResult = pipe.sscan(bfoo, SCAN_POINTER_START_BINARY, params);

    pipe.sync();

    assertThat(bResult.get().getCursor(), equalTo(SCAN_POINTER_START));
    assertThat(bResult.get().getResult(), not(empty()));
  }

  @Test
  public void sscanCount() {
    ScanParams params = new ScanParams();
    params.count(2);

    pipe.sadd("foo", "a1", "a2", "a3", "a4", "a5");

    Response<ScanResult<String>> result = pipe.sscan("foo", SCAN_POINTER_START, params);

    pipe.sync();

    assertThat(result.get().getResult(), not(empty()));

    // binary
    pipe.sadd(bfoo, bbar1, bbar2, bbar3);

    params = new ScanParams();
    params.count(2);

    Response<ScanResult<byte[]>> bResult = pipe.sscan(bfoo, SCAN_POINTER_START_BINARY, params);

    pipe.sync();
    assertThat(bResult.get().getResult(), not(empty()));
  }
}
