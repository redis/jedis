package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.params.XAddParams;
import redis.clients.jedis.params.XAutoClaimParams;
import redis.clients.jedis.params.XClaimParams;
import redis.clients.jedis.params.XPendingParams;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.params.XTrimParams;
import redis.clients.jedis.resps.StreamConsumerInfo;
import redis.clients.jedis.resps.StreamConsumersInfo;
import redis.clients.jedis.resps.StreamEntry;
import redis.clients.jedis.resps.StreamFullInfo;
import redis.clients.jedis.resps.StreamGroupInfo;
import redis.clients.jedis.resps.StreamInfo;
import redis.clients.jedis.resps.StreamPendingEntry;
import redis.clients.jedis.resps.StreamPendingSummary;

public class UnifiedJedisStreamCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testXack() {
    String key = "mystream";
    String group = "mygroup";
    StreamEntryID[] ids = { new StreamEntryID("0-0"), new StreamEntryID("0-1") };
    long expectedAcked = 2L;

    when(commandObjects.xack(key, group, ids)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedAcked);

    long result = jedis.xack(key, group, ids);

    assertThat(result, equalTo(expectedAcked));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).xack(key, group, ids);
  }

  @Test
  public void testXackBinary() {
    byte[] key = "mystream".getBytes();
    byte[] group = "mygroup".getBytes();
    byte[][] ids = { "0-0".getBytes(), "0-1".getBytes() };
    long expectedAcked = 2L;

    when(commandObjects.xack(key, group, ids)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedAcked);

    long result = jedis.xack(key, group, ids);

    assertThat(result, equalTo(expectedAcked));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).xack(key, group, ids);
  }

  @Test
  public void testXadd() {
    String key = "mystream";
    StreamEntryID id = new StreamEntryID("0-0");
    Map<String, String> hash = new HashMap<>();
    hash.put("field1", "value1");
    hash.put("field2", "value2");
    StreamEntryID expectedEntryID = new StreamEntryID("0-1");

    when(commandObjects.xadd(key, id, hash)).thenReturn(streamEntryIdCommandObject);
    when(commandExecutor.executeCommand(streamEntryIdCommandObject)).thenReturn(expectedEntryID);

    StreamEntryID result = jedis.xadd(key, id, hash);

    assertThat(result, equalTo(expectedEntryID));

    verify(commandExecutor).executeCommand(streamEntryIdCommandObject);
    verify(commandObjects).xadd(key, id, hash);
  }

  @Test
  public void testXaddBinary() {
    byte[] key = "mystream".getBytes();
    XAddParams params = new XAddParams().id("0-1");
    Map<byte[], byte[]> hash = new HashMap<>();
    hash.put("field1".getBytes(), "value1".getBytes());
    byte[] expectedEntryId = "0-1".getBytes();

    when(commandObjects.xadd(key, params, hash)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedEntryId);

    byte[] result = jedis.xadd(key, params, hash);

    assertThat(result, equalTo(expectedEntryId));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).xadd(key, params, hash);
  }

  @Test
  public void testXaddWithParams() {
    String key = "mystream";
    XAddParams params = new XAddParams();
    Map<String, String> hash = new HashMap<>();
    hash.put("field1", "value1");
    hash.put("field2", "value2");
    StreamEntryID expectedEntryID = new StreamEntryID("0-1");

    when(commandObjects.xadd(key, params, hash)).thenReturn(streamEntryIdCommandObject);
    when(commandExecutor.executeCommand(streamEntryIdCommandObject)).thenReturn(expectedEntryID);

    StreamEntryID result = jedis.xadd(key, params, hash);

    assertThat(result, equalTo(expectedEntryID));

    verify(commandExecutor).executeCommand(streamEntryIdCommandObject);
    verify(commandObjects).xadd(key, params, hash);
  }

  @Test
  public void testXautoclaim() {
    String key = "mystream";
    String group = "mygroup";
    String consumerName = "myconsumer";
    long minIdleTime = 10000L;
    StreamEntryID start = new StreamEntryID("0-0");
    XAutoClaimParams params = new XAutoClaimParams();
    StreamEntryID nextStart = new StreamEntryID("0-1");
    List<StreamEntry> claimedEntries = new ArrayList<>();
    AbstractMap.SimpleImmutableEntry<StreamEntryID, List<StreamEntry>> expectedResponse = new AbstractMap.SimpleImmutableEntry<>(nextStart, claimedEntries);

    when(commandObjects.xautoclaim(key, group, consumerName, minIdleTime, start, params)).thenReturn(entryStreamEntryIdListStreamEntryCommandObject);
    when(commandExecutor.executeCommand(entryStreamEntryIdListStreamEntryCommandObject)).thenReturn(expectedResponse);

    Map.Entry<StreamEntryID, List<StreamEntry>> result = jedis.xautoclaim(key, group, consumerName, minIdleTime, start, params);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(entryStreamEntryIdListStreamEntryCommandObject);
    verify(commandObjects).xautoclaim(key, group, consumerName, minIdleTime, start, params);
  }

  @Test
  public void testXautoclaimBinary() {
    byte[] key = "mystream".getBytes();
    byte[] groupName = "mygroup".getBytes();
    byte[] consumerName = "myconsumer".getBytes();
    long minIdleTime = 10000L;
    byte[] start = "0-0".getBytes();
    XAutoClaimParams params = new XAutoClaimParams();
    List<Object> expectedAutoClaimResult = new ArrayList<>();

    when(commandObjects.xautoclaim(key, groupName, consumerName, minIdleTime, start, params)).thenReturn(listObjectCommandObject);
    when(commandExecutor.executeCommand(listObjectCommandObject)).thenReturn(expectedAutoClaimResult);

    List<Object> result = jedis.xautoclaim(key, groupName, consumerName, minIdleTime, start, params);

    assertThat(result, equalTo(expectedAutoClaimResult));

    verify(commandExecutor).executeCommand(listObjectCommandObject);
    verify(commandObjects).xautoclaim(key, groupName, consumerName, minIdleTime, start, params);
  }

  @Test
  public void testXautoclaimJustId() {
    String key = "mystream";
    String group = "mygroup";
    String consumerName = "myconsumer";
    long minIdleTime = 10000L;
    StreamEntryID start = new StreamEntryID("0-0");
    XAutoClaimParams params = new XAutoClaimParams();
    StreamEntryID nextStart = new StreamEntryID("0-1");
    List<StreamEntryID> claimedEntryIds = Arrays.asList(new StreamEntryID("0-0"), new StreamEntryID("0-1"));
    AbstractMap.SimpleImmutableEntry<StreamEntryID, List<StreamEntryID>> expectedResponse = new AbstractMap.SimpleImmutableEntry<>(nextStart, claimedEntryIds);

    when(commandObjects.xautoclaimJustId(key, group, consumerName, minIdleTime, start, params)).thenReturn(entryStreamEntryIdListStreamEntryIdCommandObject);
    when(commandExecutor.executeCommand(entryStreamEntryIdListStreamEntryIdCommandObject)).thenReturn(expectedResponse);

    Map.Entry<StreamEntryID, List<StreamEntryID>> result = jedis.xautoclaimJustId(key, group, consumerName, minIdleTime, start, params);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(entryStreamEntryIdListStreamEntryIdCommandObject);
    verify(commandObjects).xautoclaimJustId(key, group, consumerName, minIdleTime, start, params);
  }

  @Test
  public void testXautoclaimJustIdBinary() {
    byte[] key = "mystream".getBytes();
    byte[] groupName = "mygroup".getBytes();
    byte[] consumerName = "myconsumer".getBytes();
    long minIdleTime = 10000L;
    byte[] start = "0-0".getBytes();
    XAutoClaimParams params = new XAutoClaimParams();
    List<Object> expectedAutoClaimResult = new ArrayList<>();

    when(commandObjects.xautoclaimJustId(key, groupName, consumerName, minIdleTime, start, params)).thenReturn(listObjectCommandObject);
    when(commandExecutor.executeCommand(listObjectCommandObject)).thenReturn(expectedAutoClaimResult);

    List<Object> result = jedis.xautoclaimJustId(key, groupName, consumerName, minIdleTime, start, params);

    assertThat(result, equalTo(expectedAutoClaimResult));

    verify(commandExecutor).executeCommand(listObjectCommandObject);
    verify(commandObjects).xautoclaimJustId(key, groupName, consumerName, minIdleTime, start, params);
  }

  @Test
  public void testXclaim() {
    String key = "mystream";
    String group = "mygroup";
    String consumerName = "myconsumer";
    long minIdleTime = 10000L;
    XClaimParams params = new XClaimParams();
    StreamEntryID[] ids = { new StreamEntryID("0-0"), new StreamEntryID("0-1") };
    List<StreamEntry> expectedEntries = new ArrayList<>();

    when(commandObjects.xclaim(key, group, consumerName, minIdleTime, params, ids)).thenReturn(listStreamEntryCommandObject);
    when(commandExecutor.executeCommand(listStreamEntryCommandObject)).thenReturn(expectedEntries);

    List<StreamEntry> result = jedis.xclaim(key, group, consumerName, minIdleTime, params, ids);

    assertThat(result, equalTo(expectedEntries));

    verify(commandExecutor).executeCommand(listStreamEntryCommandObject);
    verify(commandObjects).xclaim(key, group, consumerName, minIdleTime, params, ids);
  }

  @Test
  public void testXclaimBinary() {
    byte[] key = "mystream".getBytes();
    byte[] group = "mygroup".getBytes();
    byte[] consumerName = "myconsumer".getBytes();
    long minIdleTime = 10000L;
    XClaimParams params = new XClaimParams();
    byte[][] ids = { "0-0".getBytes(), "0-1".getBytes() };
    List<byte[]> expectedClaimedIds = Arrays.asList("0-0".getBytes(), "0-1".getBytes());

    when(commandObjects.xclaim(key, group, consumerName, minIdleTime, params, ids)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedClaimedIds);

    List<byte[]> result = jedis.xclaim(key, group, consumerName, minIdleTime, params, ids);

    assertThat(result, equalTo(expectedClaimedIds));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).xclaim(key, group, consumerName, minIdleTime, params, ids);
  }

  @Test
  public void testXclaimJustId() {
    String key = "mystream";
    String group = "mygroup";
    String consumerName = "myconsumer";
    long minIdleTime = 10000L;
    XClaimParams params = new XClaimParams();
    StreamEntryID[] ids = { new StreamEntryID("0-0"), new StreamEntryID("0-1") };
    List<StreamEntryID> expectedEntryIds = Arrays.asList(ids);

    when(commandObjects.xclaimJustId(key, group, consumerName, minIdleTime, params, ids)).thenReturn(listStreamEntryIdCommandObject);
    when(commandExecutor.executeCommand(listStreamEntryIdCommandObject)).thenReturn(expectedEntryIds);

    List<StreamEntryID> result = jedis.xclaimJustId(key, group, consumerName, minIdleTime, params, ids);

    assertThat(result, equalTo(expectedEntryIds));

    verify(commandExecutor).executeCommand(listStreamEntryIdCommandObject);
    verify(commandObjects).xclaimJustId(key, group, consumerName, minIdleTime, params, ids);
  }

  @Test
  public void testXclaimJustIdBinary() {
    byte[] key = "mystream".getBytes();
    byte[] group = "mygroup".getBytes();
    byte[] consumerName = "myconsumer".getBytes();
    long minIdleTime = 10000L;
    XClaimParams params = new XClaimParams();
    byte[][] ids = { "0-0".getBytes(), "0-1".getBytes() };
    List<byte[]> expectedClaimedIds = Arrays.asList("0-0".getBytes(), "0-1".getBytes());

    when(commandObjects.xclaimJustId(key, group, consumerName, minIdleTime, params, ids)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject)).thenReturn(expectedClaimedIds);

    List<byte[]> result = jedis.xclaimJustId(key, group, consumerName, minIdleTime, params, ids);

    assertThat(result, equalTo(expectedClaimedIds));

    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).xclaimJustId(key, group, consumerName, minIdleTime, params, ids);
  }

  @Test
  public void testXdel() {
    String key = "mystream";
    StreamEntryID[] ids = { new StreamEntryID("0-0"), new StreamEntryID("0-1") };
    long expectedDeletedCount = 2L; // Assuming the entries were successfully deleted

    when(commandObjects.xdel(key, ids)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedDeletedCount);

    long result = jedis.xdel(key, ids);

    assertThat(result, equalTo(expectedDeletedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).xdel(key, ids);
  }

  @Test
  public void testXdelBinary() {
    byte[] key = "mystream".getBytes();
    byte[][] ids = { "0-0".getBytes(), "0-1".getBytes() };
    long expectedDeleted = 2L;

    when(commandObjects.xdel(key, ids)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedDeleted);

    long result = jedis.xdel(key, ids);

    assertThat(result, equalTo(expectedDeleted));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).xdel(key, ids);
  }

  @Test
  public void testXgroupCreate() {
    String key = "mystream";
    String groupName = "mygroup";
    StreamEntryID id = new StreamEntryID("0-0");
    boolean makeStream = true;
    String expectedResponse = "OK";

    when(commandObjects.xgroupCreate(key, groupName, id, makeStream)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.xgroupCreate(key, groupName, id, makeStream);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).xgroupCreate(key, groupName, id, makeStream);
  }

  @Test
  public void testXgroupCreateBinary() {
    byte[] key = "mystream".getBytes();
    byte[] groupName = "mygroup".getBytes();
    byte[] id = "0-0".getBytes();
    boolean makeStream = true;
    String expectedResponse = "OK";

    when(commandObjects.xgroupCreate(key, groupName, id, makeStream)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.xgroupCreate(key, groupName, id, makeStream);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).xgroupCreate(key, groupName, id, makeStream);
  }

  @Test
  public void testXgroupCreateConsumer() {
    String key = "mystream";
    String groupName = "mygroup";
    String consumerName = "myconsumer";
    boolean expectedResponse = true; // Assuming the consumer was successfully created

    when(commandObjects.xgroupCreateConsumer(key, groupName, consumerName)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(expectedResponse);

    boolean result = jedis.xgroupCreateConsumer(key, groupName, consumerName);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).xgroupCreateConsumer(key, groupName, consumerName);
  }

  @Test
  public void testXgroupCreateConsumerBinary() {
    byte[] key = "mystream".getBytes();
    byte[] groupName = "mygroup".getBytes();
    byte[] consumerName = "myconsumer".getBytes();
    boolean expectedResponse = true;

    when(commandObjects.xgroupCreateConsumer(key, groupName, consumerName)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(expectedResponse);

    boolean result = jedis.xgroupCreateConsumer(key, groupName, consumerName);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).xgroupCreateConsumer(key, groupName, consumerName);
  }

  @Test
  public void testXgroupDelConsumer() {
    String key = "mystream";
    String groupName = "mygroup";
    String consumerName = "myconsumer";
    long expectedDeletedCount = 1L; // Assuming the consumer was successfully deleted

    when(commandObjects.xgroupDelConsumer(key, groupName, consumerName)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedDeletedCount);

    long result = jedis.xgroupDelConsumer(key, groupName, consumerName);

    assertThat(result, equalTo(expectedDeletedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).xgroupDelConsumer(key, groupName, consumerName);
  }

  @Test
  public void testXgroupDelConsumerBinary() {
    byte[] key = "mystream".getBytes();
    byte[] groupName = "mygroup".getBytes();
    byte[] consumerName = "myconsumer".getBytes();
    long expectedDeleted = 1L;

    when(commandObjects.xgroupDelConsumer(key, groupName, consumerName)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedDeleted);

    long result = jedis.xgroupDelConsumer(key, groupName, consumerName);

    assertThat(result, equalTo(expectedDeleted));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).xgroupDelConsumer(key, groupName, consumerName);
  }

  @Test
  public void testXgroupDestroy() {
    String key = "mystream";
    String groupName = "mygroup";
    long expectedResponse = 1L; // Assuming the group was successfully destroyed

    when(commandObjects.xgroupDestroy(key, groupName)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.xgroupDestroy(key, groupName);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).xgroupDestroy(key, groupName);
  }

  @Test
  public void testXgroupDestroyBinary() {
    byte[] key = "mystream".getBytes();
    byte[] groupName = "mygroup".getBytes();
    long expectedDestroyed = 1L;

    when(commandObjects.xgroupDestroy(key, groupName)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedDestroyed);

    long result = jedis.xgroupDestroy(key, groupName);

    assertThat(result, equalTo(expectedDestroyed));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).xgroupDestroy(key, groupName);
  }

  @Test
  public void testXgroupSetID() {
    String key = "mystream";
    String groupName = "mygroup";
    StreamEntryID id = new StreamEntryID("0-0");
    String expectedResponse = "OK";

    when(commandObjects.xgroupSetID(key, groupName, id)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.xgroupSetID(key, groupName, id);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).xgroupSetID(key, groupName, id);
  }

  @Test
  public void testXgroupSetIDBinary() {
    byte[] key = "mystream".getBytes();
    byte[] groupName = "mygroup".getBytes();
    byte[] id = "0-1".getBytes();
    String expectedResponse = "OK";

    when(commandObjects.xgroupSetID(key, groupName, id)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.xgroupSetID(key, groupName, id);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).xgroupSetID(key, groupName, id);
  }

  @Test
  public void testXinfoConsumers() {
    String key = "mystream";
    String group = "mygroup";
    List<StreamConsumersInfo> expectedConsumers = Collections.singletonList(mock(StreamConsumersInfo.class));

    when(commandObjects.xinfoConsumers(key, group)).thenReturn(listStreamConsumersInfoCommandObject);
    when(commandExecutor.executeCommand(listStreamConsumersInfoCommandObject)).thenReturn(expectedConsumers);

    List<StreamConsumersInfo> result = jedis.xinfoConsumers(key, group);

    assertThat(result, equalTo(expectedConsumers));

    verify(commandExecutor).executeCommand(listStreamConsumersInfoCommandObject);
    verify(commandObjects).xinfoConsumers(key, group);
  }

  @Test
  public void testXinfoConsumersBinary() {
    byte[] key = "mystream".getBytes();
    byte[] group = "mygroup".getBytes();
    List<Object> expectedConsumersInfo = new ArrayList<>();

    when(commandObjects.xinfoConsumers(key, group)).thenReturn(listObjectCommandObject);
    when(commandExecutor.executeCommand(listObjectCommandObject)).thenReturn(expectedConsumersInfo);

    List<Object> result = jedis.xinfoConsumers(key, group);

    assertThat(result, equalTo(expectedConsumersInfo));

    verify(commandExecutor).executeCommand(listObjectCommandObject);
    verify(commandObjects).xinfoConsumers(key, group);
  }

  @Test
  public void testXinfoConsumers2() {
    String key = "mystream";
    String group = "mygroup";
    List<StreamConsumerInfo> expectedConsumerInfos = Collections.singletonList(mock(StreamConsumerInfo.class));

    when(commandObjects.xinfoConsumers2(key, group)).thenReturn(listStreamConsumerInfoCommandObject);
    when(commandExecutor.executeCommand(listStreamConsumerInfoCommandObject)).thenReturn(expectedConsumerInfos);

    List<StreamConsumerInfo> result = jedis.xinfoConsumers2(key, group);

    assertThat(result, equalTo(expectedConsumerInfos));

    verify(commandExecutor).executeCommand(listStreamConsumerInfoCommandObject);
    verify(commandObjects).xinfoConsumers2(key, group);
  }

  @Test
  public void testXinfoGroups() {
    String key = "mystream";
    List<StreamGroupInfo> expectedGroups = Collections.singletonList(mock(StreamGroupInfo.class));

    when(commandObjects.xinfoGroups(key)).thenReturn(listStreamGroupInfoCommandObject);
    when(commandExecutor.executeCommand(listStreamGroupInfoCommandObject)).thenReturn(expectedGroups);

    List<StreamGroupInfo> result = jedis.xinfoGroups(key);

    assertThat(result, equalTo(expectedGroups));

    verify(commandExecutor).executeCommand(listStreamGroupInfoCommandObject);
    verify(commandObjects).xinfoGroups(key);
  }

  @Test
  public void testXinfoGroupsBinary() {
    byte[] key = "mystream".getBytes();
    List<Object> expectedGroupsInfo = new ArrayList<>();

    when(commandObjects.xinfoGroups(key)).thenReturn(listObjectCommandObject);
    when(commandExecutor.executeCommand(listObjectCommandObject)).thenReturn(expectedGroupsInfo);

    List<Object> result = jedis.xinfoGroups(key);

    assertThat(result, equalTo(expectedGroupsInfo));

    verify(commandExecutor).executeCommand(listObjectCommandObject);
    verify(commandObjects).xinfoGroups(key);
  }

  @Test
  public void testXinfoStream() {
    String key = "mystream";
    StreamInfo expectedStreamInfo = mock(StreamInfo.class);

    when(commandObjects.xinfoStream(key)).thenReturn(streamInfoCommandObject);
    when(commandExecutor.executeCommand(streamInfoCommandObject)).thenReturn(expectedStreamInfo);

    StreamInfo result = jedis.xinfoStream(key);

    assertThat(result, sameInstance(expectedStreamInfo));

    verify(commandExecutor).executeCommand(streamInfoCommandObject);
    verify(commandObjects).xinfoStream(key);
  }

  @Test
  public void testXinfoStreamBinary() {
    byte[] key = "mystream".getBytes();
    Object expectedStreamInfo = new Object();

    when(commandObjects.xinfoStream(key)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedStreamInfo);

    Object result = jedis.xinfoStream(key);

    assertThat(result, sameInstance(expectedStreamInfo));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).xinfoStream(key);
  }

  @Test
  public void testXinfoStreamFull() {
    String key = "mystream";
    StreamFullInfo expectedStreamFullInfo = mock(StreamFullInfo.class);

    when(commandObjects.xinfoStreamFull(key)).thenReturn(streamFullInfoCommandObject);
    when(commandExecutor.executeCommand(streamFullInfoCommandObject)).thenReturn(expectedStreamFullInfo);

    StreamFullInfo result = jedis.xinfoStreamFull(key);

    assertThat(result, sameInstance(expectedStreamFullInfo));

    verify(commandExecutor).executeCommand(streamFullInfoCommandObject);
    verify(commandObjects).xinfoStreamFull(key);
  }

  @Test
  public void testXinfoStreamFullBinary() {
    byte[] key = "mystream".getBytes();
    Object expectedStreamInfoFull = new Object();

    when(commandObjects.xinfoStreamFull(key)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedStreamInfoFull);

    Object result = jedis.xinfoStreamFull(key);

    assertThat(result, sameInstance(expectedStreamInfoFull));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).xinfoStreamFull(key);
  }

  @Test
  public void testXinfoStreamFullWithCount() {
    String key = "mystream";
    int count = 10;
    StreamFullInfo expectedStreamFullInfo = mock(StreamFullInfo.class);

    when(commandObjects.xinfoStreamFull(key, count)).thenReturn(streamFullInfoCommandObject);
    when(commandExecutor.executeCommand(streamFullInfoCommandObject)).thenReturn(expectedStreamFullInfo);

    StreamFullInfo result = jedis.xinfoStreamFull(key, count);

    assertThat(result, sameInstance(expectedStreamFullInfo));

    verify(commandExecutor).executeCommand(streamFullInfoCommandObject);
    verify(commandObjects).xinfoStreamFull(key, count);
  }

  @Test
  public void testXinfoStreamFullWithCountBinary() {
    byte[] key = "mystream".getBytes();
    int count = 10;
    Object expectedStreamInfoFull = new Object();

    when(commandObjects.xinfoStreamFull(key, count)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedStreamInfoFull);

    Object result = jedis.xinfoStreamFull(key, count);

    assertThat(result, sameInstance(expectedStreamInfoFull));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).xinfoStreamFull(key, count);
  }

  @Test
  public void testXlen() {
    String key = "mystream";
    long expectedLength = 10L;

    when(commandObjects.xlen(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedLength);

    long result = jedis.xlen(key);

    assertThat(result, equalTo(expectedLength));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).xlen(key);
  }

  @Test
  public void testXlenBinary() {
    byte[] key = "mystream".getBytes();
    long expectedLength = 100L;

    when(commandObjects.xlen(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedLength);

    long result = jedis.xlen(key);

    assertThat(result, equalTo(expectedLength));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).xlen(key);
  }

  @Test
  public void testXpending() {
    String key = "mystream";
    String groupName = "mygroup";
    StreamPendingSummary expectedSummary = new StreamPendingSummary(10L,
        new StreamEntryID("0-0"), new StreamEntryID("0-1"), Collections.emptyMap());

    when(commandObjects.xpending(key, groupName)).thenReturn(streamPendingSummaryCommandObject);
    when(commandExecutor.executeCommand(streamPendingSummaryCommandObject)).thenReturn(expectedSummary);

    StreamPendingSummary result = jedis.xpending(key, groupName);

    assertThat(result, equalTo(expectedSummary));

    verify(commandExecutor).executeCommand(streamPendingSummaryCommandObject);
    verify(commandObjects).xpending(key, groupName);
  }

  @Test
  public void testXpendingBinary() {
    byte[] key = "mystream".getBytes();
    byte[] groupName = "mygroup".getBytes();
    Object expectedPendingInfo = new Object();

    when(commandObjects.xpending(key, groupName)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedPendingInfo);

    Object result = jedis.xpending(key, groupName);

    assertThat(result, sameInstance(expectedPendingInfo));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).xpending(key, groupName);
  }

  @Test
  public void testXpendingWithParams() {
    String key = "mystream";
    String groupName = "mygroup";
    XPendingParams params = new XPendingParams();
    List<StreamPendingEntry> expectedPendingEntries = new ArrayList<>();

    when(commandObjects.xpending(key, groupName, params)).thenReturn(listStreamPendingEntryCommandObject);
    when(commandExecutor.executeCommand(listStreamPendingEntryCommandObject)).thenReturn(expectedPendingEntries);

    List<StreamPendingEntry> result = jedis.xpending(key, groupName, params);

    assertThat(result, equalTo(expectedPendingEntries));

    verify(commandExecutor).executeCommand(listStreamPendingEntryCommandObject);
    verify(commandObjects).xpending(key, groupName, params);
  }

  @Test
  public void testXpendingWithParamsBinary() {
    byte[] key = "mystream".getBytes();
    byte[] groupName = "mygroup".getBytes();
    XPendingParams params = new XPendingParams().count(10);
    List<Object> expectedPendingList = new ArrayList<>();

    when(commandObjects.xpending(key, groupName, params)).thenReturn(listObjectCommandObject);
    when(commandExecutor.executeCommand(listObjectCommandObject)).thenReturn(expectedPendingList);

    List<Object> result = jedis.xpending(key, groupName, params);

    assertThat(result, equalTo(expectedPendingList));

    verify(commandExecutor).executeCommand(listObjectCommandObject);
    verify(commandObjects).xpending(key, groupName, params);
  }

  @Test
  public void testXrange() {
    String key = "mystream";
    String start = "-";
    String end = "+";
    Map<String, String> hash = new HashMap<>();
    hash.put("field1", "value1");
    hash.put("field2", "value2");
    List<StreamEntry> expectedEntries = Collections.singletonList(new StreamEntry(new StreamEntryID("0-1"), hash));

    when(commandObjects.xrange(key, start, end)).thenReturn(listStreamEntryCommandObject);
    when(commandExecutor.executeCommand(listStreamEntryCommandObject)).thenReturn(expectedEntries);

    List<StreamEntry> result = jedis.xrange(key, start, end);

    assertThat(result, equalTo(expectedEntries));

    verify(commandExecutor).executeCommand(listStreamEntryCommandObject);
    verify(commandObjects).xrange(key, start, end);
  }

  @Test
  public void testXrangeBinary() {
    byte[] key = "mystream".getBytes();
    byte[] start = "0-0".getBytes();
    byte[] end = "+".getBytes();
    List<Object> expectedRange = Arrays.asList(
        new StreamEntry(new StreamEntryID("0-0"), Collections.singletonMap("field1", "value1")),
        new StreamEntry(new StreamEntryID("0-1"), Collections.singletonMap("field2", "value2")));

    when(commandObjects.xrange(key, start, end)).thenReturn(listObjectCommandObject);
    when(commandExecutor.executeCommand(listObjectCommandObject)).thenReturn(expectedRange);

    List<Object> result = jedis.xrange(key, start, end);

    assertThat(result, equalTo(expectedRange));

    verify(commandExecutor).executeCommand(listObjectCommandObject);
    verify(commandObjects).xrange(key, start, end);
  }

  @Test
  public void testXrangeWithCount() {
    String key = "mystream";
    String start = "-";
    String end = "+";
    int count = 10;
    Map<String, String> hash = new HashMap<>();
    hash.put("field1", "value1");
    hash.put("field2", "value2");
    List<StreamEntry> expectedEntries = Collections.singletonList(new StreamEntry(new StreamEntryID("0-1"), hash));

    when(commandObjects.xrange(key, start, end, count)).thenReturn(listStreamEntryCommandObject);
    when(commandExecutor.executeCommand(listStreamEntryCommandObject)).thenReturn(expectedEntries);

    List<StreamEntry> result = jedis.xrange(key, start, end, count);

    assertThat(result, equalTo(expectedEntries));

    verify(commandExecutor).executeCommand(listStreamEntryCommandObject);
    verify(commandObjects).xrange(key, start, end, count);
  }

  @Test
  public void testXrangeWithCountBinary() {
    byte[] key = "mystream".getBytes();
    byte[] start = "0-0".getBytes();
    byte[] end = "+".getBytes();
    int count = 2;
    List<Object> expectedRange = Arrays.asList(
        new StreamEntry(new StreamEntryID("0-0"), Collections.singletonMap("field1", "value1")),
        new StreamEntry(new StreamEntryID("0-1"), Collections.singletonMap("field2", "value2")));

    when(commandObjects.xrange(key, start, end, count)).thenReturn(listObjectCommandObject);
    when(commandExecutor.executeCommand(listObjectCommandObject)).thenReturn(expectedRange);

    List<Object> result = jedis.xrange(key, start, end, count);

    assertThat(result, equalTo(expectedRange));

    verify(commandExecutor).executeCommand(listObjectCommandObject);
    verify(commandObjects).xrange(key, start, end, count);
  }

  @Test
  public void testXrangeIds() {
    String key = "mystream";
    StreamEntryID start = new StreamEntryID("0-0");
    StreamEntryID end = new StreamEntryID("0-1");
    List<StreamEntry> expectedEntries = new ArrayList<>();
    Map<String, String> hash = new HashMap<>();
    hash.put("field1", "value1");
    hash.put("field2", "value2");
    expectedEntries.add(new StreamEntry(new StreamEntryID("0-1"), hash));

    when(commandObjects.xrange(key, start, end)).thenReturn(listStreamEntryCommandObject);
    when(commandExecutor.executeCommand(listStreamEntryCommandObject)).thenReturn(expectedEntries);

    List<StreamEntry> result = jedis.xrange(key, start, end);

    assertThat(result, equalTo(expectedEntries));

    verify(commandExecutor).executeCommand(listStreamEntryCommandObject);
    verify(commandObjects).xrange(key, start, end);
  }

  @Test
  public void testXrangeIdsWithCount() {
    String key = "mystream";
    StreamEntryID start = new StreamEntryID("0-0");
    StreamEntryID end = new StreamEntryID("0-1");
    int count = 10;
    Map<String, String> hash = new HashMap<>();
    hash.put("field1", "value1");
    hash.put("field2", "value2");
    List<StreamEntry> expectedEntries = Collections.singletonList(new StreamEntry(new StreamEntryID("0-1"), hash));

    when(commandObjects.xrange(key, start, end, count)).thenReturn(listStreamEntryCommandObject);
    when(commandExecutor.executeCommand(listStreamEntryCommandObject)).thenReturn(expectedEntries);

    List<StreamEntry> result = jedis.xrange(key, start, end, count);

    assertThat(result, equalTo(expectedEntries));

    verify(commandExecutor).executeCommand(listStreamEntryCommandObject);
    verify(commandObjects).xrange(key, start, end, count);
  }

  @Test
  public void testXread() {
    XReadParams xReadParams = new XReadParams().count(2).block(0);
    Map<String, StreamEntryID> streams = Collections.singletonMap("mystream", new StreamEntryID("0-0"));
    List<Map.Entry<String, List<StreamEntry>>> expectedEntries = new ArrayList<>();

    when(commandObjects.xread(xReadParams, streams)).thenReturn(listEntryStringListStreamEntryCommandObject);
    when(commandExecutor.executeCommand(listEntryStringListStreamEntryCommandObject)).thenReturn(expectedEntries);

    List<Map.Entry<String, List<StreamEntry>>> result = jedis.xread(xReadParams, streams);

    assertThat(result, equalTo(expectedEntries));
    verify(commandExecutor).executeCommand(listEntryStringListStreamEntryCommandObject);
    verify(commandObjects).xread(xReadParams, streams);
  }

  @Test
  public void testXreadBinary() {
    XReadParams xReadParams = new XReadParams().count(2).block(0);
    Map.Entry<byte[], byte[]> stream1 = new AbstractMap.SimpleEntry<>("mystream".getBytes(), "0-0".getBytes());
    List<Object> expectedReadResult = new ArrayList<>();

    when(commandObjects.xread(xReadParams, stream1)).thenReturn(listObjectCommandObject);
    when(commandExecutor.executeCommand(listObjectCommandObject)).thenReturn(expectedReadResult);

    List<Object> result = jedis.xread(xReadParams, stream1);

    assertThat(result, equalTo(expectedReadResult));

    verify(commandExecutor).executeCommand(listObjectCommandObject);
    verify(commandObjects).xread(xReadParams, stream1);
  }

  @Test
  public void testXreadAsMap() {
    XReadParams xReadParams = new XReadParams().count(2).block(0);
    Map<String, StreamEntryID> stream = Collections.singletonMap("mystream", new StreamEntryID("0-0"));
    Map<String, List<StreamEntry>> expectedResult = new HashMap<>();

    when(commandObjects.xreadAsMap(xReadParams, stream)).thenReturn(mapStringListStreamEntryCommandObject);
    when(commandExecutor.executeCommand(mapStringListStreamEntryCommandObject)).thenReturn(expectedResult);

    Map<String, List<StreamEntry>> result = jedis.xreadAsMap(xReadParams, stream);

    assertThat(result, sameInstance(expectedResult));
    verify(commandExecutor).executeCommand(mapStringListStreamEntryCommandObject);
    verify(commandObjects).xreadAsMap(xReadParams, stream);
  }

  @Test
  public void testXreadGroup() {
    String groupName = "mygroup";
    String consumer = "myconsumer";
    XReadGroupParams xReadGroupParams = new XReadGroupParams().count(2).block(0);
    Map<String, StreamEntryID> streams = Collections.singletonMap("mystream", new StreamEntryID("0-0"));
    List<Map.Entry<String, List<StreamEntry>>> expectedEntries = new ArrayList<>();

    when(commandObjects.xreadGroup(groupName, consumer, xReadGroupParams, streams)).thenReturn(listEntryStringListStreamEntryCommandObject);
    when(commandExecutor.executeCommand(listEntryStringListStreamEntryCommandObject)).thenReturn(expectedEntries);

    List<Map.Entry<String, List<StreamEntry>>> result = jedis.xreadGroup(groupName, consumer, xReadGroupParams, streams);

    assertThat(result, equalTo(expectedEntries));

    verify(commandExecutor).executeCommand(listEntryStringListStreamEntryCommandObject);
    verify(commandObjects).xreadGroup(groupName, consumer, xReadGroupParams, streams);
  }

  @Test
  public void testXreadGroupBinary() {
    byte[] groupName = "mygroup".getBytes();
    byte[] consumer = "myconsumer".getBytes();
    XReadGroupParams xReadGroupParams = new XReadGroupParams().count(2).block(0);
    Map.Entry<byte[], byte[]> stream1 = new AbstractMap.SimpleEntry<>("mystream".getBytes(), "0-0".getBytes());
    List<Object> expectedReadGroupResult = new ArrayList<>();

    when(commandObjects.xreadGroup(groupName, consumer, xReadGroupParams, stream1)).thenReturn(listObjectCommandObject);
    when(commandExecutor.executeCommand(listObjectCommandObject)).thenReturn(expectedReadGroupResult);

    List<Object> result = jedis.xreadGroup(groupName, consumer, xReadGroupParams, stream1);

    assertThat(result, equalTo(expectedReadGroupResult));

    verify(commandExecutor).executeCommand(listObjectCommandObject);
    verify(commandObjects).xreadGroup(groupName, consumer, xReadGroupParams, stream1);
  }

  @Test
  public void testXreadGroupAsMap() {
    String groupName = "mygroup";
    String consumer = "myconsumer";
    XReadGroupParams xReadGroupParams = new XReadGroupParams().count(2).block(0);
    Map<String, StreamEntryID> stream1 = Collections.singletonMap("mystream", new StreamEntryID());
    Map<String, List<StreamEntry>> expectedReadGroupAsMapResult = new HashMap<>();

    when(commandObjects.xreadGroupAsMap(groupName, consumer, xReadGroupParams, stream1)).thenReturn(mapStringListStreamEntryCommandObject);
    when(commandExecutor.executeCommand(mapStringListStreamEntryCommandObject)).thenReturn(expectedReadGroupAsMapResult);

    Map<String, List<StreamEntry>> result = jedis.xreadGroupAsMap(groupName, consumer, xReadGroupParams, stream1);

    assertThat(result, sameInstance(expectedReadGroupAsMapResult));

    verify(commandExecutor).executeCommand(mapStringListStreamEntryCommandObject);
    verify(commandObjects).xreadGroupAsMap(groupName, consumer, xReadGroupParams, stream1);
  }

  @Test
  public void testXrevrange() {
    String key = "mystream";
    String end = "+";
    String start = "-";
    Map<String, String> hash = new HashMap<>();
    hash.put("field1", "value1");
    hash.put("field2", "value2");
    List<StreamEntry> expectedEntries = Collections.singletonList(new StreamEntry(new StreamEntryID("0-0"), hash));

    when(commandObjects.xrevrange(key, end, start)).thenReturn(listStreamEntryCommandObject);
    when(commandExecutor.executeCommand(listStreamEntryCommandObject)).thenReturn(expectedEntries);

    List<StreamEntry> result = jedis.xrevrange(key, end, start);

    assertThat(result, equalTo(expectedEntries));

    verify(commandExecutor).executeCommand(listStreamEntryCommandObject);
    verify(commandObjects).xrevrange(key, end, start);
  }

  @Test
  public void testXrevrangeBinary() {
    byte[] key = "mystream".getBytes();
    byte[] end = "+".getBytes();
    byte[] start = "0-0".getBytes();
    List<Object> expectedReverseRange = Arrays.asList(
        new StreamEntry(new StreamEntryID("0-1"), Collections.singletonMap("field2", "value2")),
        new StreamEntry(new StreamEntryID("0-0"), Collections.singletonMap("field1", "value1")));

    when(commandObjects.xrevrange(key, end, start)).thenReturn(listObjectCommandObject);
    when(commandExecutor.executeCommand(listObjectCommandObject)).thenReturn(expectedReverseRange);

    List<Object> result = jedis.xrevrange(key, end, start);

    assertThat(result, equalTo(expectedReverseRange));

    verify(commandExecutor).executeCommand(listObjectCommandObject);
    verify(commandObjects).xrevrange(key, end, start);
  }

  @Test
  public void testXrevrangeWithCount() {
    String key = "mystream";
    String end = "+";
    String start = "-";
    int count = 10;
    Map<String, String> hash = new HashMap<>();
    hash.put("field1", "value1");
    hash.put("field2", "value2");
    List<StreamEntry> expectedEntries = Collections.singletonList(new StreamEntry(new StreamEntryID("0-0"), hash));

    when(commandObjects.xrevrange(key, end, start, count)).thenReturn(listStreamEntryCommandObject);
    when(commandExecutor.executeCommand(listStreamEntryCommandObject)).thenReturn(expectedEntries);

    List<StreamEntry> result = jedis.xrevrange(key, end, start, count);

    assertThat(result, equalTo(expectedEntries));

    verify(commandExecutor).executeCommand(listStreamEntryCommandObject);
    verify(commandObjects).xrevrange(key, end, start, count);
  }

  @Test
  public void testXrevrangeWithCountBinary() {
    byte[] key = "mystream".getBytes();
    byte[] end = "+".getBytes();
    byte[] start = "0-0".getBytes();
    int count = 1;
    List<Object> expectedReverseRange = Collections.singletonList(
        new StreamEntry(new StreamEntryID("0-1"), Collections.singletonMap("field2", "value2")));

    when(commandObjects.xrevrange(key, end, start, count)).thenReturn(listObjectCommandObject);
    when(commandExecutor.executeCommand(listObjectCommandObject)).thenReturn(expectedReverseRange);

    List<Object> result = jedis.xrevrange(key, end, start, count);

    assertThat(result, equalTo(expectedReverseRange));

    verify(commandExecutor).executeCommand(listObjectCommandObject);
    verify(commandObjects).xrevrange(key, end, start, count);
  }

  @Test
  public void testXrevrangeIds() {
    String key = "mystream";
    StreamEntryID end = new StreamEntryID("0-1");
    StreamEntryID start = new StreamEntryID("0-0");
    Map<String, String> hash = new HashMap<>();
    hash.put("field1", "value1");
    hash.put("field2", "value2");
    List<StreamEntry> expectedEntries = Collections.singletonList(new StreamEntry(new StreamEntryID("0-0"), hash));

    when(commandObjects.xrevrange(key, end, start)).thenReturn(listStreamEntryCommandObject);
    when(commandExecutor.executeCommand(listStreamEntryCommandObject)).thenReturn(expectedEntries);

    List<StreamEntry> result = jedis.xrevrange(key, end, start);

    assertThat(result, equalTo(expectedEntries));

    verify(commandExecutor).executeCommand(listStreamEntryCommandObject);
    verify(commandObjects).xrevrange(key, end, start);
  }

  @Test
  public void testXrevrangeIdsWithCount() {
    String key = "mystream";
    StreamEntryID end = new StreamEntryID("0-1");
    StreamEntryID start = new StreamEntryID("0-0");
    int count = 10;
    Map<String, String> hash = new HashMap<>();
    hash.put("field1", "value1");
    hash.put("field2", "value2");
    List<StreamEntry> expectedEntries = Collections.singletonList(new StreamEntry(new StreamEntryID("0-0"), hash));

    when(commandObjects.xrevrange(key, end, start, count)).thenReturn(listStreamEntryCommandObject);
    when(commandExecutor.executeCommand(listStreamEntryCommandObject)).thenReturn(expectedEntries);

    List<StreamEntry> result = jedis.xrevrange(key, end, start, count);

    assertThat(result, equalTo(expectedEntries));

    verify(commandExecutor).executeCommand(listStreamEntryCommandObject);
    verify(commandObjects).xrevrange(key, end, start, count);
  }

  @Test
  public void testXtrim() {
    String key = "mystream";
    long maxLen = 1000L;
    boolean approximate = false;
    long expectedTrimmedCount = 10L; // Assuming 10 entries were trimmed

    when(commandObjects.xtrim(key, maxLen, approximate)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedTrimmedCount);

    long result = jedis.xtrim(key, maxLen, approximate);

    assertThat(result, equalTo(expectedTrimmedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).xtrim(key, maxLen, approximate);
  }

  @Test
  public void testXtrimBinary() {
    byte[] key = "mystream".getBytes();
    long maxLen = 1000L;
    boolean approximateLength = true;
    long expectedTrimmed = 10L;

    when(commandObjects.xtrim(key, maxLen, approximateLength)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedTrimmed);

    long result = jedis.xtrim(key, maxLen, approximateLength);

    assertThat(result, equalTo(expectedTrimmed));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).xtrim(key, maxLen, approximateLength);
  }

  @Test
  public void testXtrimWithParams() {
    String key = "mystream";
    XTrimParams params = new XTrimParams().maxLen(1000L);
    long expectedTrimmedCount = 10L; // Assuming 10 entries were trimmed

    when(commandObjects.xtrim(key, params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedTrimmedCount);

    long result = jedis.xtrim(key, params);

    assertThat(result, equalTo(expectedTrimmedCount));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).xtrim(key, params);
  }

  @Test
  public void testXtrimWithParamsBinary() {
    byte[] key = "mystream".getBytes();
    XTrimParams params = new XTrimParams().maxLen(1000L);
    long expectedTrimmed = 10L;

    when(commandObjects.xtrim(key, params)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedTrimmed);

    long result = jedis.xtrim(key, params);

    assertThat(result, equalTo(expectedTrimmed));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).xtrim(key, params);
  }

}
