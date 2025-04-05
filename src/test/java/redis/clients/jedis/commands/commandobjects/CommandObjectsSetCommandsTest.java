package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;

import java.util.List;
import java.util.Set;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.Test;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

/**
 * Tests related to <a href="https://redis.io/commands/?group=set">Set</a> commands.
 */
public class CommandObjectsSetCommandsTest extends CommandObjectsStandaloneTestBase {

  public CommandObjectsSetCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testSetCommands() {
    String key = "testSet";
    String member1 = "member1";
    String member2 = "member2";
    String member3 = "member3";

    Long sadd = exec(commandObjects.sadd(key, member1, member2, member3));
    assertThat(sadd, equalTo(3L));

    Set<String> members = exec(commandObjects.smembers(key));
    assertThat(members, containsInAnyOrder(member1, member2, member3));

    Long srem = exec(commandObjects.srem(key, member1));
    assertThat(srem, equalTo(1L));

    Set<String> membersAfterSrem = exec(commandObjects.smembers(key));
    assertThat(membersAfterSrem, containsInAnyOrder(member2, member3));
  }

  @Test
  public void testSetCommandsBinary() {
    byte[] key = "testSetB".getBytes();
    byte[] member1 = "member1".getBytes();
    byte[] member2 = "member2".getBytes();
    byte[] member3 = "member3".getBytes();

    Long sadd = exec(commandObjects.sadd(key, member1, member2, member3));
    assertThat(sadd, equalTo(3L));

    Set<byte[]> members = exec(commandObjects.smembers(key));
    assertThat(members, containsInAnyOrder(member1, member2, member3));

    Long srem = exec(commandObjects.srem(key, member1));
    assertThat(srem, equalTo(1L));

    Set<byte[]> membersAfterSrem = exec(commandObjects.smembers(key));
    assertThat(membersAfterSrem, containsInAnyOrder(member2, member3));
  }

  @Test
  public void testSpop() {
    String key = "testSetPop";
    String member1 = "member1";
    String member2 = "member2";
    String member3 = "member3";

    Long sadd = exec(commandObjects.sadd(key, member1, member2, member3));
    assertThat(sadd, equalTo(3L));

    String spop = exec(commandObjects.spop(key));
    assertThat(spop, anyOf(equalTo(member1), equalTo(member2), equalTo(member3)));

    Set<String> spopMultiple = exec(commandObjects.spop(key, 2));
    assertThat(spopMultiple, hasSize(2));
    assertThat(spopMultiple, everyItem(anyOf(equalTo(member1), equalTo(member2), equalTo(member3))));
    assertThat(spopMultiple, not(contains(spop)));
  }

  @Test
  public void testSpopBinary() {
    byte[] bkey = "testSetPopB".getBytes();
    byte[] member1 = "member1".getBytes();
    byte[] member2 = "member2".getBytes();
    byte[] member3 = "member3".getBytes();

    Long sadd = exec(commandObjects.sadd(bkey, member1, member2, member3));
    assertThat(sadd, equalTo(3L));

    byte[] spop = exec(commandObjects.spop(bkey));
    assertThat(spop, anyOf(equalTo(member1), equalTo(member2), equalTo(member3)));

    Set<byte[]> spopMultiple = exec(commandObjects.spop(bkey, 2));
    assertThat(spopMultiple, hasSize(2));
    assertThat(spopMultiple, everyItem(anyOf(equalTo(member1), equalTo(member2), equalTo(member3))));
    assertThat(spopMultiple, not(contains(spop)));
  }

  @Test
  public void testSetMembershipCommands() {
    String key = "testSetMembership";
    String member1 = "member1";
    String member2 = "member2";

    exec(commandObjects.sadd(key, member1, member2));

    Long scard = exec(commandObjects.scard(key));
    assertThat(scard, equalTo(2L));

    Long scardBinary = exec(commandObjects.scard(key.getBytes()));
    assertThat(scardBinary, equalTo(2L));

    Boolean isMember = exec(commandObjects.sismember(key, member1));
    assertThat(isMember, equalTo(true));

    Boolean isMemberBinary = exec(commandObjects.sismember(key.getBytes(), member1.getBytes()));
    assertThat(isMemberBinary, equalTo(true));

    List<Boolean> mIsMember = exec(commandObjects.smismember(key, member1, "nonMember"));
    assertThat(mIsMember, contains(true, false));

    List<Boolean> mIsMemberBinary = exec(commandObjects.smismember(key.getBytes(), member1.getBytes(), "nonMember".getBytes()));
    assertThat(mIsMemberBinary, contains(true, false));
  }

  @Test
  public void testSrandmemberCommands() {
    String key = "testSetRandomMember";
    String member1 = "member1";
    String member2 = "member2";
    String member3 = "member3";

    exec(commandObjects.sadd(key, member1, member2, member3));

    String randomMember = exec(commandObjects.srandmember(key));
    assertThat(randomMember, anyOf(equalTo(member1), equalTo(member2), equalTo(member3)));

    byte[] randomMemberBinary = exec(commandObjects.srandmember(key.getBytes()));
    assertThat(new String(randomMemberBinary), anyOf(equalTo(member1), equalTo(member2), equalTo(member3)));

    List<String> randomMembers = exec(commandObjects.srandmember(key, 2));
    assertThat(randomMembers, hasSize(2));
    assertThat(randomMembers, everyItem(anyOf(equalTo(member1), equalTo(member2), equalTo(member3))));
    assertThat(randomMembers, not(contains(randomMember)));

    List<byte[]> randomMembersBinary = exec(commandObjects.srandmember(key.getBytes(), 2));
    assertThat(randomMembersBinary, hasSize(2));
    assertThat(randomMembersBinary, everyItem(anyOf(equalTo(member1.getBytes()), equalTo(member2.getBytes()), equalTo(member3.getBytes()))));
    assertThat(randomMembersBinary, not(contains(randomMemberBinary)));
  }

  @Test
  public void testSscanCommands() {
    String key = "testSetScan";
    String member1 = "member1";
    String member2 = "member2";
    String member3 = "member3";

    exec(commandObjects.sadd(key, member1, member2, member3));

    ScanParams params = new ScanParams().count(2);

    ScanResult<String> scan = exec(commandObjects.sscan(key, ScanParams.SCAN_POINTER_START, params));

    assertThat(scan.getResult(), hasSize(lessThanOrEqualTo(3)));
    assertThat(scan.getResult(), everyItem(anyOf(equalTo(member1), equalTo(member2), equalTo(member3))));

    ScanResult<byte[]> scanBinary = exec(commandObjects.sscan(key.getBytes(), ScanParams.SCAN_POINTER_START_BINARY, params));

    assertThat(scanBinary.getResult(), hasSize(lessThanOrEqualTo(3)));
    assertThat(scanBinary.getResult(), everyItem(anyOf(equalTo(member1.getBytes()), equalTo(member2.getBytes()), equalTo(member3.getBytes()))));
  }

  @Test
  public void testSdiff() {
    String key1 = "testSet1";
    String key2 = "testSet2";

    exec(commandObjects.sadd(key1, "member1", "member2", "member3"));
    exec(commandObjects.sadd(key2, "member2", "member3", "member4"));

    Set<String> diff = exec(commandObjects.sdiff(key1, key2));
    assertThat(diff, contains("member1"));

    Set<byte[]> diffBinary = exec(commandObjects.sdiff(key1.getBytes(), key2.getBytes()));
    assertThat(diffBinary, contains("member1".getBytes()));
  }

  @Test
  public void testSdiffstore() {
    String key1 = "testSet1";
    String key2 = "testSet2";
    String dstKey = "testSetDiff";

    exec(commandObjects.sadd(key1, "member1", "member2", "member3"));
    exec(commandObjects.sadd(key2, "member2", "member3", "member4"));

    Long diffStore = exec(commandObjects.sdiffstore(dstKey, key1, key2));
    assertThat(diffStore, equalTo(1L));

    Set<String> dstSet = exec(commandObjects.smembers(dstKey));
    assertThat(dstSet, contains("member1"));
  }

  @Test
  public void testSdiffstoreBinary() {
    byte[] key1 = "testSet1".getBytes();
    byte[] key2 = "testSet2".getBytes();
    byte[] dstKey = "testSetDiff".getBytes();

    exec(commandObjects.sadd(key1, "member1".getBytes(), "member2".getBytes(), "member3".getBytes()));
    exec(commandObjects.sadd(key2, "member2".getBytes(), "member3".getBytes(), "member4".getBytes()));

    Long diffStore = exec(commandObjects.sdiffstore(dstKey, key1, key2));
    assertThat(diffStore, equalTo(1L));

    Set<byte[]> dstSet = exec(commandObjects.smembers(dstKey));
    assertThat(dstSet, contains("member1".getBytes()));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testSinterAndSinterCard() {
    String key1 = "testSetInter1";
    String key2 = "testSetInter2";

    exec(commandObjects.sadd(key1, "member1", "member2", "member3"));
    exec(commandObjects.sadd(key2, "member2", "member3", "member4"));

    Set<String> inter = exec(commandObjects.sinter(key1, key2));
    assertThat(inter, containsInAnyOrder("member2", "member3"));

    Set<byte[]> interBinary = exec(commandObjects.sinter(key1.getBytes(), key2.getBytes()));
    assertThat(interBinary, containsInAnyOrder("member2".getBytes(), "member3".getBytes()));

    Long interCard = exec(commandObjects.sintercard(key1, key2));
    assertThat(interCard, equalTo(2L));

    Long interCardBinary = exec(commandObjects.sintercard(key1.getBytes(), key2.getBytes()));
    assertThat(interCardBinary, equalTo(2L));

    Long interCardLimited = exec(commandObjects.sintercard(1, key1, key2));
    assertThat(interCardLimited, equalTo(1L));

    Long interCardLimitedBinary = exec(commandObjects.sintercard(1, key1.getBytes(), key2.getBytes()));
    assertThat(interCardLimitedBinary, equalTo(1L));
  }

  @Test
  public void testSinterstore() {
    String key1 = "testSetInter1";
    String key2 = "testSetInter2";
    String dstKey = "testSetInterResult";

    exec(commandObjects.sadd(key1, "member1", "member2", "member3"));
    exec(commandObjects.sadd(key2, "member2", "member3", "member4"));

    Long interStore = exec(commandObjects.sinterstore(dstKey, key1, key2));
    assertThat(interStore, equalTo(2L));

    Set<String> dstSet = exec(commandObjects.smembers(dstKey));
    assertThat(dstSet, containsInAnyOrder("member2", "member3"));
  }

  @Test
  public void testSinterstoreBinary() {
    byte[] key1 = "testSetInter1B".getBytes();
    byte[] key2 = "testSetInter2B".getBytes();
    byte[] dstKey = "testSetInterResultB".getBytes();

    exec(commandObjects.sadd(key1, "member1".getBytes(), "member2".getBytes(), "member3".getBytes()));
    exec(commandObjects.sadd(key2, "member2".getBytes(), "member3".getBytes(), "member4".getBytes()));

    Long interStore = exec(commandObjects.sinterstore(dstKey, key1, key2));
    assertThat(interStore, equalTo(2L));

    Set<byte[]> dstSet = exec(commandObjects.smembers(dstKey));
    assertThat(dstSet, containsInAnyOrder("member2".getBytes(), "member3".getBytes()));
  }

  @Test
  public void testSunion() {
    String key1 = "testSetUnion1";
    String key2 = "testSetUnion2";

    exec(commandObjects.sadd(key1, "member1", "member2", "member3"));
    exec(commandObjects.sadd(key2, "member3", "member4", "member5"));

    Set<String> unionResult = exec(commandObjects.sunion(key1, key2));

    assertThat(unionResult, containsInAnyOrder(
        "member1", "member2", "member3", "member4", "member5"));

    Set<byte[]> bunionResult = exec(commandObjects.sunion(key1.getBytes(), key2.getBytes()));

    assertThat(bunionResult, containsInAnyOrder(
        "member1".getBytes(), "member2".getBytes(), "member3".getBytes(), "member4".getBytes(), "member5".getBytes()));
  }

  @Test
  public void testSunionstore() {
    String key1 = "testSetUnion1";
    String key2 = "testSetUnion2";
    String dstKey = "testSetUnionResult";

    exec(commandObjects.sadd(key1, "member1", "member2", "member3"));
    exec(commandObjects.sadd(key2, "member3", "member4", "member5"));

    Long unionStore = exec(commandObjects.sunionstore(dstKey, key1, key2));

    assertThat(unionStore, equalTo(5L));

    Set<String> dstSet = exec(commandObjects.smembers(dstKey));

    assertThat(dstSet, containsInAnyOrder(
        "member1", "member2", "member3", "member4", "member5"));
  }

  @Test
  public void testSunionstoreBinary() {
    byte[] key1 = "testSetUnion1".getBytes();
    byte[] key2 = "testSetUnion2".getBytes();
    byte[] dstKey = "testSetUnionResult".getBytes();

    exec(commandObjects.sadd(key1, "member1".getBytes(), "member2".getBytes(), "member3".getBytes()));
    exec(commandObjects.sadd(key2, "member3".getBytes(), "member4".getBytes(), "member5".getBytes()));

    Long unionStore = exec(commandObjects.sunionstore(dstKey, key1, key2));
    assertThat(unionStore, equalTo(5L));

    Set<byte[]> dstSet = exec(commandObjects.smembers(dstKey));
    assertThat(dstSet, containsInAnyOrder(
        "member1".getBytes(), "member2".getBytes(), "member3".getBytes(), "member4".getBytes(), "member5".getBytes()));
  }

  @Test
  public void testSmove() {
    String srcKey = "testSetSrc";
    String dstKey = "testSetDst";
    String member = "memberToMove";

    exec(commandObjects.sadd(srcKey, member));

    Long smove = exec(commandObjects.smove(srcKey, dstKey, member));
    assertThat(smove, equalTo(1L));

    Set<String> dstSet = exec(commandObjects.smembers(dstKey));
    assertThat(dstSet, contains(member));
  }

  @Test
  public void testSmoveBinary() {
    byte[] srcKey = "testSetSrc".getBytes();
    byte[] dstKey = "testSetDst".getBytes();
    byte[] member = "memberToMove".getBytes();

    exec(commandObjects.sadd(srcKey, member));

    Long smove = exec(commandObjects.smove(srcKey, dstKey, member));
    assertThat(smove, equalTo(1L));

    Set<byte[]> dstSet = exec(commandObjects.smembers(dstKey));
    assertThat(dstSet, contains(member));
  }
}
