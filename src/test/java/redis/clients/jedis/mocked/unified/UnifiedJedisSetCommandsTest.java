package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.args.ExpiryOption;
import redis.clients.jedis.params.SAddExParams;
import redis.clients.jedis.params.SDiffCardParams;
import redis.clients.jedis.params.SUnionCardParams;
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
  public void testSdiffcard() {
    String[] keys = { "setKey1", "setKey2" };
    long expectedCardinality = 2L;

    when(commandObjects.sdiffcard(keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.sdiffcard(keys);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sdiffcard(keys);
  }

  @Test
  public void testSdiffcardList() {
    List<String> keys = Arrays.asList("setKey1", "setKey2");
    long expectedCardinality = 2L;

    when(commandObjects.sdiffcard(keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.sdiffcard(keys);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sdiffcard(keys);
  }

  @Test
  public void testSdiffcardTwoKeysWithParams() {
    SDiffCardParams params = new SDiffCardParams().limit(1);
    long expectedCardinality = 1L;

    when(commandObjects.sdiffcard("setKey1", "setKey2", params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.sdiffcard("setKey1", "setKey2", params);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sdiffcard("setKey1", "setKey2", params);
  }

  @Test
  public void testSdiffcardListWithParams() {
    List<String> keys = Arrays.asList("setKey1", "setKey2");
    SDiffCardParams params = new SDiffCardParams().limit(1);
    long expectedCardinality = 1L;

    when(commandObjects.sdiffcard(keys, params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.sdiffcard(keys, params);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sdiffcard(keys, params);
  }

  @Test
  public void testSdiffcardBinary() {
    byte[][] keys = { "setKey1".getBytes(), "setKey2".getBytes() };
    long expectedCardinality = 2L;

    when(commandObjects.sdiffcard(keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.sdiffcard(keys);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sdiffcard(keys);
  }

  @Test
  public void testSdiffcardTwoKeysWithParamsBinary() {
    byte[] key1 = "setKey1".getBytes();
    byte[] key2 = "setKey2".getBytes();
    SDiffCardParams params = new SDiffCardParams().limit(1);
    long expectedCardinality = 1L;

    when(commandObjects.sdiffcard(key1, key2, params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.sdiffcard(key1, key2, params);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sdiffcard(key1, key2, params);
  }

  @Test
  public void testSdiffcardWithParamsBinary() {
    byte[][] keys = { "setKey1".getBytes(), "setKey2".getBytes() };
    SDiffCardParams params = new SDiffCardParams().limit(1);
    long expectedCardinality = 1L;

    when(commandObjects.sdiffcard(keys, params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.sdiffcard(keys, params);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sdiffcard(keys, params);
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

  @Test
  public void testSunioncard() {
    String[] keys = { "setKey1", "setKey2" };
    long expectedCardinality = 2L;

    when(commandObjects.sunioncard(keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.sunioncard(keys);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sunioncard(keys);
  }

  @Test
  public void testSunioncardList() {
    List<String> keys = Arrays.asList("setKey1", "setKey2");
    long expectedCardinality = 2L;

    when(commandObjects.sunioncard(keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.sunioncard(keys);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sunioncard(keys);
  }

  @Test
  public void testSunioncardTwoKeysWithParams() {
    SUnionCardParams params = new SUnionCardParams().limit(1);
    long expectedCardinality = 1L;

    when(commandObjects.sunioncard("setKey1", "setKey2", params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.sunioncard("setKey1", "setKey2", params);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sunioncard("setKey1", "setKey2", params);
  }

  @Test
  public void testSunioncardListWithParams() {
    List<String> keys = Arrays.asList("setKey1", "setKey2");
    SUnionCardParams params = new SUnionCardParams().limit(1);
    long expectedCardinality = 1L;

    when(commandObjects.sunioncard(keys, params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.sunioncard(keys, params);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sunioncard(keys, params);
  }

  @Test
  public void testSunioncardBinary() {
    byte[][] keys = { "setKey1".getBytes(), "setKey2".getBytes() };
    long expectedCardinality = 2L;

    when(commandObjects.sunioncard(keys)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.sunioncard(keys);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sunioncard(keys);
  }

  @Test
  public void testSunioncardTwoKeysWithParamsBinary() {
    byte[] key1 = "setKey1".getBytes();
    byte[] key2 = "setKey2".getBytes();
    SUnionCardParams params = new SUnionCardParams().limit(1);
    long expectedCardinality = 1L;

    when(commandObjects.sunioncard(key1, key2, params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.sunioncard(key1, key2, params);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sunioncard(key1, key2, params);
  }

  @Test
  public void testSunioncardWithParamsBinary() {
    byte[][] keys = { "setKey1".getBytes(), "setKey2".getBytes() };
    SUnionCardParams params = new SUnionCardParams().limit(1);
    long expectedCardinality = 1L;

    when(commandObjects.sunioncard(keys, params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedCardinality);

    long result = jedis.sunioncard(keys, params);

    assertThat(result, equalTo(expectedCardinality));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sunioncard(keys, params);
  }

  @Test
  public void testSexpire() {
    String key = "setKey";
    String[] members = { "member1", "member2" };
    List<Long> expected = Arrays.asList(1L, -2L);

    when(commandObjects.sexpire(key, 20, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.sexpire(key, 20, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).sexpire(key, 20, members);
  }

  @Test
  public void testSexpireWithCondition() {
    String key = "setKey";
    String[] members = { "member1", "member2" };
    List<Long> expected = Arrays.asList(1L, 0L);

    when(commandObjects.sexpire(key, 20, ExpiryOption.NX, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.sexpire(key, 20, ExpiryOption.NX, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).sexpire(key, 20, ExpiryOption.NX, members);
  }

  @Test
  public void testSpexpire() {
    String key = "setKey";
    String[] members = { "member1", "member2" };
    List<Long> expected = Arrays.asList(1L, -2L);

    when(commandObjects.spexpire(key, 20000, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.spexpire(key, 20000, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).spexpire(key, 20000, members);
  }

  @Test
  public void testSpexpireWithCondition() {
    String key = "setKey";
    String[] members = { "member1", "member2" };
    List<Long> expected = Arrays.asList(1L, 0L);

    when(commandObjects.spexpire(key, 20000, ExpiryOption.NX, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.spexpire(key, 20000, ExpiryOption.NX, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).spexpire(key, 20000, ExpiryOption.NX, members);
  }

  @Test
  public void testSexpireAt() {
    String key = "setKey";
    String[] members = { "member1", "member2" };
    List<Long> expected = Arrays.asList(1L, -2L);

    when(commandObjects.sexpireAt(key, 1700000000, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.sexpireAt(key, 1700000000, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).sexpireAt(key, 1700000000, members);
  }

  @Test
  public void testSexpireAtWithCondition() {
    String key = "setKey";
    String[] members = { "member1", "member2" };
    List<Long> expected = Arrays.asList(1L, 0L);

    when(commandObjects.sexpireAt(key, 1700000000, ExpiryOption.NX, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.sexpireAt(key, 1700000000, ExpiryOption.NX, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).sexpireAt(key, 1700000000, ExpiryOption.NX, members);
  }

  @Test
  public void testSpexpireAt() {
    String key = "setKey";
    String[] members = { "member1", "member2" };
    List<Long> expected = Arrays.asList(1L, -2L);

    when(commandObjects.spexpireAt(key, 1700000000000L, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.spexpireAt(key, 1700000000000L, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).spexpireAt(key, 1700000000000L, members);
  }

  @Test
  public void testSpexpireAtWithCondition() {
    String key = "setKey";
    String[] members = { "member1", "member2" };
    List<Long> expected = Arrays.asList(1L, 0L);

    when(commandObjects.spexpireAt(key, 1700000000000L, ExpiryOption.NX, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.spexpireAt(key, 1700000000000L, ExpiryOption.NX, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).spexpireAt(key, 1700000000000L, ExpiryOption.NX, members);
  }

  @Test
  public void testSttl() {
    String key = "setKey";
    String[] members = { "member1", "member2" };
    List<Long> expected = Arrays.asList(-1L, -2L);

    when(commandObjects.sttl(key, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.sttl(key, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).sttl(key, members);
  }

  @Test
  public void testSpttl() {
    String key = "setKey";
    String[] members = { "member1", "member2" };
    List<Long> expected = Arrays.asList(-1L, -2L);

    when(commandObjects.spttl(key, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.spttl(key, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).spttl(key, members);
  }

  @Test
  public void testSexpireTime() {
    String key = "setKey";
    String[] members = { "member1", "member2" };
    List<Long> expected = Arrays.asList(-1L, -2L);

    when(commandObjects.sexpireTime(key, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.sexpireTime(key, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).sexpireTime(key, members);
  }

  @Test
  public void testSpexpireTime() {
    String key = "setKey";
    String[] members = { "member1", "member2" };
    List<Long> expected = Arrays.asList(-1L, -2L);

    when(commandObjects.spexpireTime(key, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.spexpireTime(key, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).spexpireTime(key, members);
  }

  @Test
  public void testSpersist() {
    String key = "setKey";
    String[] members = { "member1", "member2" };
    List<Long> expected = Arrays.asList(-1L, -2L);

    when(commandObjects.spersist(key, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.spersist(key, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).spersist(key, members);
  }

  @Test
  public void testSaddex() {
    String key = "setKey";
    String[] members = { "member1", "member2" };
    SAddExParams params = SAddExParams.sAddExParams().mnx().ex(100);
    long expected = 1L;

    when(commandObjects.saddex(key, params, members)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.saddex(key, params, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).saddex(key, params, members);
  }

  @Test
  public void testSexpireBinary() {
    byte[] key = "setKey".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };
    List<Long> expected = Arrays.asList(1L, -2L);

    when(commandObjects.sexpire(key, 20, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.sexpire(key, 20, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).sexpire(key, 20, members);
  }

  @Test
  public void testSexpireWithConditionBinary() {
    byte[] key = "setKey".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };
    List<Long> expected = Arrays.asList(1L, 0L);

    when(commandObjects.sexpire(key, 20, ExpiryOption.NX, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.sexpire(key, 20, ExpiryOption.NX, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).sexpire(key, 20, ExpiryOption.NX, members);
  }

  @Test
  public void testSpexpireBinary() {
    byte[] key = "setKey".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };
    List<Long> expected = Arrays.asList(1L, -2L);

    when(commandObjects.spexpire(key, 20000, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.spexpire(key, 20000, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).spexpire(key, 20000, members);
  }

  @Test
  public void testSpexpireWithConditionBinary() {
    byte[] key = "setKey".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };
    List<Long> expected = Arrays.asList(1L, 0L);

    when(commandObjects.spexpire(key, 20000, ExpiryOption.NX, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.spexpire(key, 20000, ExpiryOption.NX, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).spexpire(key, 20000, ExpiryOption.NX, members);
  }

  @Test
  public void testSexpireAtBinary() {
    byte[] key = "setKey".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };
    List<Long> expected = Arrays.asList(1L, -2L);

    when(commandObjects.sexpireAt(key, 1700000000, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.sexpireAt(key, 1700000000, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).sexpireAt(key, 1700000000, members);
  }

  @Test
  public void testSexpireAtWithConditionBinary() {
    byte[] key = "setKey".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };
    List<Long> expected = Arrays.asList(1L, 0L);

    when(commandObjects.sexpireAt(key, 1700000000, ExpiryOption.NX, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.sexpireAt(key, 1700000000, ExpiryOption.NX, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).sexpireAt(key, 1700000000, ExpiryOption.NX, members);
  }

  @Test
  public void testSpexpireAtBinary() {
    byte[] key = "setKey".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };
    List<Long> expected = Arrays.asList(1L, -2L);

    when(commandObjects.spexpireAt(key, 1700000000000L, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.spexpireAt(key, 1700000000000L, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).spexpireAt(key, 1700000000000L, members);
  }

  @Test
  public void testSpexpireAtWithConditionBinary() {
    byte[] key = "setKey".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };
    List<Long> expected = Arrays.asList(1L, 0L);

    when(commandObjects.spexpireAt(key, 1700000000000L, ExpiryOption.NX, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.spexpireAt(key, 1700000000000L, ExpiryOption.NX, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).spexpireAt(key, 1700000000000L, ExpiryOption.NX, members);
  }

  @Test
  public void testSttlBinary() {
    byte[] key = "setKey".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };
    List<Long> expected = Arrays.asList(-1L, -2L);

    when(commandObjects.sttl(key, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.sttl(key, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).sttl(key, members);
  }

  @Test
  public void testSpttlBinary() {
    byte[] key = "setKey".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };
    List<Long> expected = Arrays.asList(-1L, -2L);

    when(commandObjects.spttl(key, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.spttl(key, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).spttl(key, members);
  }

  @Test
  public void testSexpireTimeBinary() {
    byte[] key = "setKey".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };
    List<Long> expected = Arrays.asList(-1L, -2L);

    when(commandObjects.sexpireTime(key, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.sexpireTime(key, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).sexpireTime(key, members);
  }

  @Test
  public void testSpexpireTimeBinary() {
    byte[] key = "setKey".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };
    List<Long> expected = Arrays.asList(-1L, -2L);

    when(commandObjects.spexpireTime(key, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.spexpireTime(key, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).spexpireTime(key, members);
  }

  @Test
  public void testSpersistBinary() {
    byte[] key = "setKey".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };
    List<Long> expected = Arrays.asList(-1L, -2L);

    when(commandObjects.spersist(key, members)).thenReturn(listLongCommandObject);
    when(commandExecutor.executeCommand(listLongCommandObject)).thenReturn(expected);

    List<Long> result = jedis.spersist(key, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(listLongCommandObject);
    verify(commandObjects).spersist(key, members);
  }

  @Test
  public void testSaddexBinary() {
    byte[] key = "setKey".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };
    SAddExParams params = SAddExParams.sAddExParams().mnx().ex(100);
    long expected = 1L;

    when(commandObjects.saddex(key, params, members)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.saddex(key, params, members);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).saddex(key, params, members);
  }

  @Test
  public void testSexpireSingleMember() {
    String key = "setKey";
    String member = "member1";
    long expected = 1L;

    when(commandObjects.sexpire(key, 20, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.sexpire(key, 20, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sexpire(key, 20, member);
  }

  @Test
  public void testSexpireWithConditionSingleMember() {
    String key = "setKey";
    String member = "member1";
    long expected = 1L;

    when(commandObjects.sexpire(key, 20, ExpiryOption.GT, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.sexpire(key, 20, ExpiryOption.GT, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sexpire(key, 20, ExpiryOption.GT, member);
  }

  @Test
  public void testSpexpireSingleMember() {
    String key = "setKey";
    String member = "member1";
    long expected = 1L;

    when(commandObjects.spexpire(key, 20000, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.spexpire(key, 20000, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).spexpire(key, 20000, member);
  }

  @Test
  public void testSpexpireWithConditionSingleMember() {
    String key = "setKey";
    String member = "member1";
    long expected = 1L;

    when(commandObjects.spexpire(key, 20000, ExpiryOption.GT, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.spexpire(key, 20000, ExpiryOption.GT, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).spexpire(key, 20000, ExpiryOption.GT, member);
  }

  @Test
  public void testSexpireAtSingleMember() {
    String key = "setKey";
    String member = "member1";
    long expected = 1L;

    when(commandObjects.sexpireAt(key, 1700000000, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.sexpireAt(key, 1700000000, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sexpireAt(key, 1700000000, member);
  }

  @Test
  public void testSexpireAtWithConditionSingleMember() {
    String key = "setKey";
    String member = "member1";
    long expected = 1L;

    when(commandObjects.sexpireAt(key, 1700000000, ExpiryOption.GT, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.sexpireAt(key, 1700000000, ExpiryOption.GT, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sexpireAt(key, 1700000000, ExpiryOption.GT, member);
  }

  @Test
  public void testSpexpireAtSingleMember() {
    String key = "setKey";
    String member = "member1";
    long expected = 1L;

    when(commandObjects.spexpireAt(key, 1700000000000L, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.spexpireAt(key, 1700000000000L, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).spexpireAt(key, 1700000000000L, member);
  }

  @Test
  public void testSpexpireAtWithConditionSingleMember() {
    String key = "setKey";
    String member = "member1";
    long expected = 1L;

    when(commandObjects.spexpireAt(key, 1700000000000L, ExpiryOption.GT, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.spexpireAt(key, 1700000000000L, ExpiryOption.GT, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).spexpireAt(key, 1700000000000L, ExpiryOption.GT, member);
  }

  @Test
  public void testSttlSingleMember() {
    String key = "setKey";
    String member = "member1";
    long expected = 1L;

    when(commandObjects.sttl(key, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.sttl(key, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sttl(key, member);
  }

  @Test
  public void testSpttlSingleMember() {
    String key = "setKey";
    String member = "member1";
    long expected = 1L;

    when(commandObjects.spttl(key, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.spttl(key, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).spttl(key, member);
  }

  @Test
  public void testSexpireTimeSingleMember() {
    String key = "setKey";
    String member = "member1";
    long expected = 1L;

    when(commandObjects.sexpireTime(key, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.sexpireTime(key, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sexpireTime(key, member);
  }

  @Test
  public void testSpexpireTimeSingleMember() {
    String key = "setKey";
    String member = "member1";
    long expected = 1L;

    when(commandObjects.spexpireTime(key, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.spexpireTime(key, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).spexpireTime(key, member);
  }

  @Test
  public void testSpersistSingleMember() {
    String key = "setKey";
    String member = "member1";
    long expected = 1L;

    when(commandObjects.spersist(key, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.spersist(key, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).spersist(key, member);
  }

  @Test
  public void testSexpireSingleMemberBinary() {
    byte[] key = "setKey".getBytes();
    byte[] member = "member1".getBytes();
    long expected = 1L;

    when(commandObjects.sexpire(key, 20, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.sexpire(key, 20, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sexpire(key, 20, member);
  }

  @Test
  public void testSexpireWithConditionSingleMemberBinary() {
    byte[] key = "setKey".getBytes();
    byte[] member = "member1".getBytes();
    long expected = 1L;

    when(commandObjects.sexpire(key, 20, ExpiryOption.GT, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.sexpire(key, 20, ExpiryOption.GT, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sexpire(key, 20, ExpiryOption.GT, member);
  }

  @Test
  public void testSpexpireSingleMemberBinary() {
    byte[] key = "setKey".getBytes();
    byte[] member = "member1".getBytes();
    long expected = 1L;

    when(commandObjects.spexpire(key, 20000, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.spexpire(key, 20000, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).spexpire(key, 20000, member);
  }

  @Test
  public void testSpexpireWithConditionSingleMemberBinary() {
    byte[] key = "setKey".getBytes();
    byte[] member = "member1".getBytes();
    long expected = 1L;

    when(commandObjects.spexpire(key, 20000, ExpiryOption.GT, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.spexpire(key, 20000, ExpiryOption.GT, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).spexpire(key, 20000, ExpiryOption.GT, member);
  }

  @Test
  public void testSexpireAtSingleMemberBinary() {
    byte[] key = "setKey".getBytes();
    byte[] member = "member1".getBytes();
    long expected = 1L;

    when(commandObjects.sexpireAt(key, 1700000000, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.sexpireAt(key, 1700000000, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sexpireAt(key, 1700000000, member);
  }

  @Test
  public void testSexpireAtWithConditionSingleMemberBinary() {
    byte[] key = "setKey".getBytes();
    byte[] member = "member1".getBytes();
    long expected = 1L;

    when(commandObjects.sexpireAt(key, 1700000000, ExpiryOption.GT, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.sexpireAt(key, 1700000000, ExpiryOption.GT, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sexpireAt(key, 1700000000, ExpiryOption.GT, member);
  }

  @Test
  public void testSpexpireAtSingleMemberBinary() {
    byte[] key = "setKey".getBytes();
    byte[] member = "member1".getBytes();
    long expected = 1L;

    when(commandObjects.spexpireAt(key, 1700000000000L, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.spexpireAt(key, 1700000000000L, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).spexpireAt(key, 1700000000000L, member);
  }

  @Test
  public void testSpexpireAtWithConditionSingleMemberBinary() {
    byte[] key = "setKey".getBytes();
    byte[] member = "member1".getBytes();
    long expected = 1L;

    when(commandObjects.spexpireAt(key, 1700000000000L, ExpiryOption.GT, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.spexpireAt(key, 1700000000000L, ExpiryOption.GT, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).spexpireAt(key, 1700000000000L, ExpiryOption.GT, member);
  }

  @Test
  public void testSttlSingleMemberBinary() {
    byte[] key = "setKey".getBytes();
    byte[] member = "member1".getBytes();
    long expected = 1L;

    when(commandObjects.sttl(key, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.sttl(key, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sttl(key, member);
  }

  @Test
  public void testSpttlSingleMemberBinary() {
    byte[] key = "setKey".getBytes();
    byte[] member = "member1".getBytes();
    long expected = 1L;

    when(commandObjects.spttl(key, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.spttl(key, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).spttl(key, member);
  }

  @Test
  public void testSexpireTimeSingleMemberBinary() {
    byte[] key = "setKey".getBytes();
    byte[] member = "member1".getBytes();
    long expected = 1L;

    when(commandObjects.sexpireTime(key, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.sexpireTime(key, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).sexpireTime(key, member);
  }

  @Test
  public void testSpexpireTimeSingleMemberBinary() {
    byte[] key = "setKey".getBytes();
    byte[] member = "member1".getBytes();
    long expected = 1L;

    when(commandObjects.spexpireTime(key, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.spexpireTime(key, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).spexpireTime(key, member);
  }

  @Test
  public void testSpersistSingleMemberBinary() {
    byte[] key = "setKey".getBytes();
    byte[] member = "member1".getBytes();
    long expected = 1L;

    when(commandObjects.spersist(key, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.spersist(key, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).spersist(key, member);
  }

  @Test
  public void testSaddexSingleMember() {
    String key = "setKey";
    String member = "member1";
    SAddExParams params = SAddExParams.sAddExParams().mxx().px(1000);
    long expected = 1L;

    when(commandObjects.saddex(key, params, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.saddex(key, params, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).saddex(key, params, member);
  }

  @Test
  public void testSaddexSingleMemberBinary() {
    byte[] key = "setKey".getBytes();
    byte[] member = "member1".getBytes();
    SAddExParams params = SAddExParams.sAddExParams().mxx().px(1000);
    long expected = 1L;

    when(commandObjects.saddex(key, params, member)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expected);

    long result = jedis.saddex(key, params, member);

    assertThat(result, equalTo(expected));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).saddex(key, params, member);
  }

}
