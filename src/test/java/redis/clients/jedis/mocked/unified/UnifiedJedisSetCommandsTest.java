package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public class UnifiedJedisSetCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testSadd() {
    String key = "setKey";
    String[] members = { "member1", "member2" };
    long expectedAdded = 2L; // Assuming both members were added

    when(commandObjects.sadd(key, members)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedAdded);

    long result = jedis.sadd(key, members);

    assertThat(result, equalTo(expectedAdded));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sadd(key, members);
  }

  @Test
  public void testSaddBinary() {
    byte[] key = "setKey".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };
    long expectedAdded = 2L; // Assuming both members were added

    when(commandObjects.sadd(key, members)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedAdded);

    long result = jedis.sadd(key, members);

    assertThat(result, equalTo(expectedAdded));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sadd(key, members);
  }

  @Test
  public void testScard() {
    String key = "setKey";
    long expectedCardinality = 3L; // Assuming the set has 3 members

    when(commandObjects.scard(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.scard(key);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).scard(key);
  }

  @Test
  public void testScardBinary() {
    byte[] key = "setKey".getBytes();
    long expectedCardinality = 3L; // Assuming the set has 3 members

    when(commandObjects.scard(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.scard(key);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).scard(key);
  }

  @Test
  public void testSdiff() {
    String[] keys = { "setKey1", "setKey2" };
    Set<String> expectedDifference = new HashSet<>(Arrays.asList("member1", "member3")); // Assuming these members are in setKey1 but not in setKey2

    when(commandObjects.sdiff(keys)).thenReturn(setStringCommandObject);
    when(commandExecutor.executeCommand(setStringCommandObject)).thenReturn(expectedDifference);

    Set<String> result = jedis.sdiff(keys);

    assertThat(result, equalTo(expectedDifference));

    verify(commandExecutor).executeCommand(setStringCommandObject);
    verify(commandObjects).sdiff(keys);
  }

  @Test
  public void testSdiffBinary() {
    byte[][] keys = { "setKey1".getBytes(), "setKey2".getBytes() };
    Set<byte[]> expectedDifference = new HashSet<>(Arrays.asList("member1".getBytes(), "member3".getBytes())); // Assuming these members are in setKey1 but not in setKey2

    when(commandObjects.sdiff(keys)).thenReturn(setBytesCommandObject);
    when(commandExecutor.executeCommand(setBytesCommandObject)).thenReturn(expectedDifference);

    Set<byte[]> result = jedis.sdiff(keys);

    assertThat(result, equalTo(expectedDifference));

    verify(commandExecutor).executeCommand(setBytesCommandObject);
    verify(commandObjects).sdiff(keys);
  }

  @Test
  public void testSdiffstore() {
    String dstkey = "destinationKey";
    String[] keys = { "setKey1", "setKey2" };
    long expectedStored = 2L; // Assuming two members were stored in the destination set

    when(commandObjects.sdiffstore(dstkey, keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStored);

    long result = jedis.sdiffstore(dstkey, keys);

    assertThat(result, equalTo(expectedStored));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sdiffstore(dstkey, keys);
  }

  @Test
  public void testSdiffstoreBinary() {
    byte[] dstkey = "destinationKey".getBytes();
    byte[][] keys = { "setKey1".getBytes(), "setKey2".getBytes() };
    long expectedStored = 2L; // Assuming two members were stored in the destination set

    when(commandObjects.sdiffstore(dstkey, keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStored);

    long result = jedis.sdiffstore(dstkey, keys);

    assertThat(result, equalTo(expectedStored));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sdiffstore(dstkey, keys);
  }

  @Test
  public void testSinter() {
    String[] keys = { "setKey1", "setKey2" };
    Set<String> expectedIntersection = new HashSet<>(Arrays.asList("member2", "member4")); // Assuming these members are common to setKey1 and setKey2

    when(commandObjects.sinter(keys)).thenReturn(setStringCommandObject);
    when(commandExecutor.executeCommand(setStringCommandObject)).thenReturn(expectedIntersection);

    Set<String> result = jedis.sinter(keys);

    assertThat(result, equalTo(expectedIntersection));

    verify(commandExecutor).executeCommand(setStringCommandObject);
    verify(commandObjects).sinter(keys);
  }

  @Test
  public void testSinterBinary() {
    byte[][] keys = { "setKey1".getBytes(), "setKey2".getBytes() };
    Set<byte[]> expectedIntersection = new HashSet<>(Arrays.asList("member2".getBytes(), "member4".getBytes())); // Assuming these members are common to setKey1 and setKey2

    when(commandObjects.sinter(keys)).thenReturn(setBytesCommandObject);
    when(commandExecutor.executeCommand(setBytesCommandObject)).thenReturn(expectedIntersection);

    Set<byte[]> result = jedis.sinter(keys);

    assertThat(result, equalTo(expectedIntersection));

    verify(commandExecutor).executeCommand(setBytesCommandObject);
    verify(commandObjects).sinter(keys);
  }

  @Test
  public void testSintercard() {
    String[] keys = { "setKey1", "setKey2" };
    long expectedCardinality = 2L; // Assuming there are two common members

    when(commandObjects.sintercard(keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.sintercard(keys);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sintercard(keys);
  }

  @Test
  public void testSintercardBinary() {
    byte[][] keys = { "setKey1".getBytes(), "setKey2".getBytes() };
    long expectedCardinality = 2L; // Assuming there are two common members

    when(commandObjects.sintercard(keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.sintercard(keys);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sintercard(keys);
  }

  @Test
  public void testSintercardWithLimit() {
    int limit = 1;
    String[] keys = { "setKey1", "setKey2" };
    long expectedCardinality = 1L; // Assuming the limit is set to 1 and there is at least one common member

    when(commandObjects.sintercard(limit, keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.sintercard(limit, keys);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sintercard(limit, keys);
  }

  @Test
  public void testSintercardWithLimitBinary() {
    int limit = 1;
    byte[][] keys = { "setKey1".getBytes(), "setKey2".getBytes() };
    long expectedCardinality = 1L; // Assuming the limit is set to 1 and there is at least one common member

    when(commandObjects.sintercard(limit, keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.sintercard(limit, keys);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sintercard(limit, keys);
  }

  @Test
  public void testSinterstore() {
    String dstkey = "destinationKey";
    String[] keys = { "setKey1", "setKey2" };
    long expectedStored = 2L; // Assuming two members were stored in the destination set

    when(commandObjects.sinterstore(dstkey, keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStored);

    long result = jedis.sinterstore(dstkey, keys);

    assertThat(result, equalTo(expectedStored));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sinterstore(dstkey, keys);
  }

  @Test
  public void testSinterstoreBinary() {
    byte[] dstkey = "destinationKey".getBytes();
    byte[][] keys = { "setKey1".getBytes(), "setKey2".getBytes() };
    long expectedStored = 2L; // Assuming two members were stored in the destination set

    when(commandObjects.sinterstore(dstkey, keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStored);

    long result = jedis.sinterstore(dstkey, keys);

    assertThat(result, equalTo(expectedStored));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sinterstore(dstkey, keys);
  }

  @Test
  public void testSismember() {
    String key = "setKey";
    String member = "member1";
    boolean expectedIsMember = true; // Assuming the member is part of the set

    when(commandObjects.sismember(key, member)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(expectedIsMember);

    boolean result = jedis.sismember(key, member);

    assertThat(result, equalTo(expectedIsMember));

    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).sismember(key, member);
  }

  @Test
  public void testSismemberBinary() {
    byte[] key = "setKey".getBytes();
    byte[] member = "member1".getBytes();
    boolean expectedIsMember = true; // Assuming the member is part of the set

    when(commandObjects.sismember(key, member)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(expectedIsMember);

    boolean result = jedis.sismember(key, member);

    assertThat(result, equalTo(expectedIsMember));

    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).sismember(key, member);
  }

  @Test
  public void testSmembers() {
    String key = "setKey";
    Set<String> expectedMembers = new HashSet<>(Arrays.asList("member1", "member2"));

    when(commandObjects.smembers(key)).thenReturn(setStringCommandObject);
    when(commandExecutor.executeCommand(setStringCommandObject)).thenReturn(expectedMembers);

    Set<String> result = jedis.smembers(key);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(setStringCommandObject);
    verify(commandObjects).smembers(key);
  }

  @Test
  public void testSmembersBinary() {
    byte[] key = "setKey".getBytes();
    Set<byte[]> expectedMembers = new HashSet<>(Arrays.asList("member1".getBytes(), "member2".getBytes()));

    when(commandObjects.smembers(key)).thenReturn(setBytesCommandObject);
    when(commandExecutor.executeCommand(setBytesCommandObject)).thenReturn(expectedMembers);

    Set<byte[]> result = jedis.smembers(key);

    assertThat(result, equalTo(expectedMembers));

    verify(commandExecutor).executeCommand(setBytesCommandObject);
    verify(commandObjects).smembers(key);
  }

  @Test
  public void testSmismember() {
    String key = "setKey";
    String[] members = { "member1", "member2", "member3" };
    List<Boolean> expectedMembership = Arrays.asList(true, false, true); // Assuming the first and last members are part of the set

    when(commandObjects.smismember(key, members)).thenReturn(listBooleanCommandObject);
    when(commandExecutor.executeCommand(listBooleanCommandObject)).thenReturn(expectedMembership);

    List<Boolean> result = jedis.smismember(key, members);

    assertThat(result, equalTo(expectedMembership));

    verify(commandExecutor).executeCommand(listBooleanCommandObject);
    verify(commandObjects).smismember(key, members);
  }

  @Test
  public void testSmismemberBinary() {
    byte[] key = "setKey".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes(), "member3".getBytes() };
    List<Boolean> expectedMembership = Arrays.asList(true, false, true); // Assuming the first and last members are part of the set

    when(commandObjects.smismember(key, members)).thenReturn(listBooleanCommandObject);
    when(commandExecutor.executeCommand(listBooleanCommandObject)).thenReturn(expectedMembership);

    List<Boolean> result = jedis.smismember(key, members);

    assertThat(result, equalTo(expectedMembership));

    verify(commandExecutor).executeCommand(listBooleanCommandObject);
    verify(commandObjects).smismember(key, members);
  }

  @Test
  public void testSmove() {
    String srckey = "sourceKey";
    String dstkey = "destinationKey";
    String member = "member1";
    long expectedMoved = 1L; // Assuming the member was successfully moved

    when(commandObjects.smove(srckey, dstkey, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedMoved);

    long result = jedis.smove(srckey, dstkey, member);

    assertThat(result, equalTo(expectedMoved));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).smove(srckey, dstkey, member);
  }

  @Test
  public void testSmoveBinary() {
    byte[] srckey = "sourceKey".getBytes();
    byte[] dstkey = "destinationKey".getBytes();
    byte[] member = "member1".getBytes();
    long expectedMoved = 1L; // Assuming the member was successfully moved

    when(commandObjects.smove(srckey, dstkey, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedMoved);

    long result = jedis.smove(srckey, dstkey, member);

    assertThat(result, equalTo(expectedMoved));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).smove(srckey, dstkey, member);
  }

  @Test
  public void testSpop() {
    String key = "setKey";
    String expectedPopped = "member1"; // Assuming "member1" was popped

    when(commandObjects.spop(key)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedPopped);

    String result = jedis.spop(key);

    assertThat(result, equalTo(expectedPopped));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).spop(key);
  }

  @Test
  public void testSpopBinary() {
    byte[] key = "setKey".getBytes();
    byte[] expectedPopped = "member1".getBytes(); // Assuming "member1" was popped

    when(commandObjects.spop(key)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedPopped);

    byte[] result = jedis.spop(key);

    assertThat(result, equalTo(expectedPopped));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).spop(key);
  }

  @Test
  public void testSpopCount() {
    String key = "setKey";
    long count = 2;
    Set<String> expectedPopped = new HashSet<>(Arrays.asList("member1", "member2")); // Assuming these members were popped

    when(commandObjects.spop(key, count)).thenReturn(setStringCommandObject);
    when(commandExecutor.executeCommand(setStringCommandObject)).thenReturn(expectedPopped);

    Set<String> result = jedis.spop(key, count);

    assertThat(result, equalTo(expectedPopped));

    verify(commandExecutor).executeCommand(setStringCommandObject);
    verify(commandObjects).spop(key, count);
  }

  @Test
  public void testSpopCountBinary() {
    byte[] key = "setKey".getBytes();
    long count = 2;
    Set<byte[]> expectedPopped = new HashSet<>(Arrays.asList("member1".getBytes(), "member2".getBytes())); // Assuming these members were popped

    when(commandObjects.spop(key, count)).thenReturn(setBytesCommandObject);
    when(commandExecutor.executeCommand(setBytesCommandObject)).thenReturn(expectedPopped);

    Set<byte[]> result = jedis.spop(key, count);

    assertThat(result, equalTo(expectedPopped));

    verify(commandExecutor).executeCommand(setBytesCommandObject);
    verify(commandObjects).spop(key, count);
  }

  @Test
  public void testSrandmember() {
    String key = "setKey";
    String expectedRandomMember = "member1"; // Assuming "member1" is randomly selected
    when(commandObjects.srandmember(key)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedRandomMember);

    String result = jedis.srandmember(key);

    assertThat(result, equalTo(expectedRandomMember));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).srandmember(key);
  }

  @Test
  public void testSrandmemberBinary() {
    byte[] key = "setKey".getBytes();
    byte[] expectedRandomMember = "member1".getBytes(); // Assuming "member1" is randomly selected

    when(commandObjects.srandmember(key)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedRandomMember);

    byte[] result = jedis.srandmember(key);

    assertThat(result, equalTo(expectedRandomMember));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).srandmember(key);
  }

  @Test
  public void testSrandmemberCount() {
    String key = "setKey";
    int count = 2;
    List<String> expectedRandomMembers = Arrays.asList("member1", "member2"); // Assuming these members are randomly selected

    when(commandObjects.srandmember(key, count)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedRandomMembers);

    List<String> result = jedis.srandmember(key, count);

    assertThat(result, equalTo(expectedRandomMembers));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).srandmember(key, count);
  }

  @Test
  public void testSrandmemberCountBinary() {
    byte[] key = "setKey".getBytes();
    int count = 2;
    List<byte[]> expectedRandomMembers = Arrays.asList("member1".getBytes(), "member2".getBytes()); // Assuming these members are randomly selected

    when(commandObjects.srandmember(key, count)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedRandomMembers);

    List<byte[]> result = jedis.srandmember(key, count);

    assertThat(result, equalTo(expectedRandomMembers));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).srandmember(key, count);
  }

  @Test
  public void testSrem() {
    String key = "setKey";
    String[] members = { "member1", "member2" };
    long expectedRemoved = 2L; // Assuming both members were removed

    when(commandObjects.srem(key, members)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedRemoved);

    long result = jedis.srem(key, members);

    assertThat(result, equalTo(expectedRemoved));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).srem(key, members);
  }

  @Test
  public void testSremBinary() {
    byte[] key = "setKey".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };
    long expectedRemoved = 2L; // Assuming both members were removed

    when(commandObjects.srem(key, members)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedRemoved);

    long result = jedis.srem(key, members);

    assertThat(result, equalTo(expectedRemoved));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).srem(key, members);
  }

  @Test
  public void testSscan() {
    String key = "setKey";
    String cursor = "0";
    ScanParams params = new ScanParams().match("*").count(10);
    List<String> scanResultData = Arrays.asList("member1", "member2", "member3");
    ScanResult<String> expectedScanResult = new ScanResult<>(cursor, scanResultData);

    when(commandObjects.sscan(key, cursor, params)).thenReturn(scanResultStringCommandObject);
    when(commandExecutor.executeCommand(scanResultStringCommandObject)).thenReturn(expectedScanResult);

    ScanResult<String> result = jedis.sscan(key, cursor, params);

    assertThat(result, equalTo(expectedScanResult));

    verify(commandExecutor).executeCommand(scanResultStringCommandObject);
    verify(commandObjects).sscan(key, cursor, params);
  }

  @Test
  public void testSscanBinary() {
    byte[] key = "setKey".getBytes();
    byte[] cursor = ScanParams.SCAN_POINTER_START_BINARY;
    ScanParams params = new ScanParams().match("*".getBytes()).count(10);
    List<byte[]> scanResultData = Arrays.asList("member1".getBytes(), "member2".getBytes(), "member3".getBytes());
    ScanResult<byte[]> expectedScanResult = new ScanResult<>(cursor, scanResultData);

    when(commandObjects.sscan(key, cursor, params)).thenReturn(scanResultBytesCommandObject);
    when(commandExecutor.executeCommand(scanResultBytesCommandObject)).thenReturn(expectedScanResult);

    ScanResult<byte[]> result = jedis.sscan(key, cursor, params);

    assertThat(result, equalTo(expectedScanResult));

    verify(commandExecutor).executeCommand(scanResultBytesCommandObject);
    verify(commandObjects).sscan(key, cursor, params);
  }

  @Test
  public void testSunion() {
    String[] keys = { "setKey1", "setKey2" };
    Set<String> expectedUnion = new HashSet<>(Arrays.asList("member1", "member2", "member3", "member4")); // Assuming these members are in either setKey1 or setKey2

    when(commandObjects.sunion(keys)).thenReturn(setStringCommandObject);
    when(commandExecutor.executeCommand(setStringCommandObject)).thenReturn(expectedUnion);

    Set<String> result = jedis.sunion(keys);

    assertThat(result, equalTo(expectedUnion));

    verify(commandExecutor).executeCommand(setStringCommandObject);
    verify(commandObjects).sunion(keys);
  }

  @Test
  public void testSunionBinary() {
    byte[][] keys = { "setKey1".getBytes(), "setKey2".getBytes() };
    Set<byte[]> expectedUnion = new HashSet<>(Arrays.asList("member1".getBytes(), "member2".getBytes(), "member3".getBytes(), "member4".getBytes())); // Assuming these members are in either setKey1 or setKey2

    when(commandObjects.sunion(keys)).thenReturn(setBytesCommandObject);
    when(commandExecutor.executeCommand(setBytesCommandObject)).thenReturn(expectedUnion);

    Set<byte[]> result = jedis.sunion(keys);

    assertThat(result, equalTo(expectedUnion));

    verify(commandExecutor).executeCommand(setBytesCommandObject);
    verify(commandObjects).sunion(keys);
  }

  @Test
  public void testSunionstore() {
    String dstkey = "destinationKey";
    String[] keys = { "setKey1", "setKey2" };
    long expectedStored = 4L; // Assuming four unique members were stored in the destination set

    when(commandObjects.sunionstore(dstkey, keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStored);

    long result = jedis.sunionstore(dstkey, keys);

    assertThat(result, equalTo(expectedStored));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sunionstore(dstkey, keys);
  }

  @Test
  public void testSunionstoreBinary() {
    byte[] dstkey = "destinationKey".getBytes();
    byte[][] keys = { "setKey1".getBytes(), "setKey2".getBytes() };
    long expectedStored = 4L; // Assuming four unique members were stored in the destination set

    when(commandObjects.sunionstore(dstkey, keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedStored);

    long result = jedis.sunionstore(dstkey, keys);

    assertThat(result, equalTo(expectedStored));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sunionstore(dstkey, keys);
  }

}
