package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import redis.clients.jedis.RedisProtocol;
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

/**
 * Tests related to <a href="https://redis.io/commands/?group=stream">Stream</a> commands.
 */
public class CommandObjectsStreamCommandsTest extends CommandObjectsStandaloneTestBase {

  public CommandObjectsStreamCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testXaddAndXlen() {
    String streamKey = "testStream";
    StreamEntryID entryID = StreamEntryID.NEW_ENTRY;

    Map<String, String> entryData = new HashMap<>();
    entryData.put("field1", "value1");
    entryData.put("field2", "value2");

    StreamEntryID addedEntryId = exec(commandObjects.xadd(streamKey, entryID, entryData));
    assertThat(addedEntryId, notNullValue());

    XAddParams params = new XAddParams().maxLen(1000);
    StreamEntryID addedEntryIdWithParams = exec(commandObjects.xadd(streamKey, params, entryData));
    assertThat(addedEntryIdWithParams, notNullValue());

    Long streamLength = exec(commandObjects.xlen(streamKey));
    assertThat(streamLength, equalTo(2L));
  }

  @Test
  public void testXaddAndXlenBinary() {
    byte[] streamKey = "streamKey".getBytes();

    Map<byte[], byte[]> entryData = new HashMap<>();
    entryData.put("field1".getBytes(), "value1".getBytes());
    entryData.put("field2".getBytes(), "value2".getBytes());

    XAddParams params = new XAddParams().maxLen(1000);
    byte[] addedEntryId = exec(commandObjects.xadd(streamKey, params, entryData));
    assertThat(addedEntryId, notNullValue());

    Long streamLengthBytes = exec(commandObjects.xlen(streamKey));
    assertThat(streamLengthBytes, equalTo(1L));
  }

  @Test
  public void testXrangeWithIdParameters() {
    String key = "testStream";

    Map<String, String> entryData1 = new HashMap<>();
    entryData1.put("field1", "value1");

    Map<String, String> entryData2 = new HashMap<>();
    entryData2.put("field2", "value2");

    StreamEntryID startID = exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, entryData1));
    StreamEntryID endID = exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, entryData2));

    List<StreamEntry> xrangeAll = exec(commandObjects.xrange(key, null, (StreamEntryID) null));
    assertThat(xrangeAll.size(), equalTo(2));
    assertThat(xrangeAll.get(0).getFields(), equalTo(entryData1));
    assertThat(xrangeAll.get(1).getFields(), equalTo(entryData2));

    List<StreamEntry> xrangeAllCount = exec(commandObjects.xrange(key, null, (StreamEntryID) null, 1));
    assertThat(xrangeAllCount.size(), equalTo(1));
    assertThat(xrangeAllCount.get(0).getFields(), equalTo(entryData1));

    List<StreamEntry> xrangeStartEnd = exec(commandObjects.xrange(key, startID, endID));
    assertThat(xrangeStartEnd.size(), equalTo(2));
    assertThat(xrangeStartEnd.get(0).getFields(), equalTo(entryData1));
    assertThat(xrangeStartEnd.get(1).getFields(), equalTo(entryData2));

    List<StreamEntry> xrangeStartEndCount = exec(commandObjects.xrange(key, startID, endID, 1));
    assertThat(xrangeStartEndCount.size(), equalTo(1));
    assertThat(xrangeStartEndCount.get(0).getFields(), equalTo(entryData1));

    List<StreamEntry> xrangeUnknown = exec(commandObjects.xrange("nonExistingStream", null, (StreamEntryID) null));
    assertThat(xrangeUnknown, empty());
  }

  @Test
  public void testXrangeWithStringParameters() {
    String key = "testStreamWithString";

    Map<String, String> entryData1 = new HashMap<>();
    entryData1.put("field1", "value1");

    Map<String, String> entryData2 = new HashMap<>();
    entryData2.put("field2", "value2");

    StreamEntryID startID = exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, entryData1));
    StreamEntryID endID = exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, entryData2));

    String start = startID.toString();
    String end = endID.toString();

    List<StreamEntry> xrangeStartEnd = exec(commandObjects.xrange(key, start, end));
    assertThat(xrangeStartEnd.size(), equalTo(2));
    assertThat(xrangeStartEnd.get(0).getFields(), equalTo(entryData1));
    assertThat(xrangeStartEnd.get(1).getFields(), equalTo(entryData2));

    List<StreamEntry> xrangeStartEndCount = exec(commandObjects.xrange(key, start, end, 1));
    assertThat(xrangeStartEndCount.size(), equalTo(1));
    assertThat(xrangeStartEndCount.get(0).getFields(), equalTo(entryData1));

    List<StreamEntry> xrangeUnknown = exec(commandObjects.xrange("nonExistingStream", start, end));
    assertThat(xrangeUnknown, empty());
  }

  @Test
  public void testXrangeWithBinaryParameters() {
    String keyStr = "testStreamWithBytes";
    byte[] key = keyStr.getBytes();

    Map<String, String> entryData1 = new HashMap<>();
    entryData1.put("field1", "value1");

    Map<String, String> entryData2 = new HashMap<>();
    entryData2.put("field2", "value2");

    StreamEntryID startID = exec(commandObjects.xadd(keyStr, StreamEntryID.NEW_ENTRY, entryData1));
    StreamEntryID endID = exec(commandObjects.xadd(keyStr, StreamEntryID.NEW_ENTRY, entryData2));

    byte[] start = startID.toString().getBytes();
    byte[] end = endID.toString().getBytes();

    List<Object> xrangeAll = exec(commandObjects.xrange(key, null, null));
    assertThat(xrangeAll, hasSize(2));
    assertThat(xrangeAll.get(0), instanceOf(List.class));
    assertThat(((List<?>) xrangeAll.get(0)).get(0), equalTo(start));
    assertThat(((List<?>) xrangeAll.get(1)).get(0), equalTo(end));

    List<Object> xrangeStartEnd = exec(commandObjects.xrange(key, start, end));
    assertThat(xrangeStartEnd, hasSize(2));
    assertThat(xrangeStartEnd.get(0), instanceOf(List.class));
    assertThat(((List<?>) xrangeStartEnd.get(0)).get(0), equalTo(start));
    assertThat(((List<?>) xrangeStartEnd.get(1)).get(0), equalTo(end));

    List<Object> xrangeAllCount = exec(commandObjects.xrange(key, null, null, 1));
    assertThat(xrangeAllCount, hasSize(1));
    assertThat(xrangeAllCount.get(0), instanceOf(List.class));
    assertThat(((List<?>) xrangeAllCount.get(0)).get(0), equalTo(start));

    List<Object> xrangeStartEndCount = exec(commandObjects.xrange(key, start, end, 1));
    assertThat(xrangeStartEndCount, hasSize(1));
    assertThat(xrangeStartEndCount.get(0), instanceOf(List.class));
    assertThat(((List<?>) xrangeStartEndCount.get(0)).get(0), equalTo(start));

    List<Object> xrangeUnknown = exec(commandObjects.xrange("nonExistingStream".getBytes(), start, end));
    assertThat(xrangeUnknown, empty());
  }

  @Test
  public void testXrevrangeWithIdParameters() {
    String key = "testStreamForXrevrange";

    Map<String, String> entryData1 = new HashMap<>();
    entryData1.put("field1", "value1");

    Map<String, String> entryData2 = new HashMap<>();
    entryData2.put("field2", "value2");

    StreamEntryID startID = exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, entryData1));
    StreamEntryID endID = exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, entryData2));

    List<StreamEntry> xrevrangeAll = exec(commandObjects.xrevrange(key, null, (StreamEntryID) null));
    assertThat(xrevrangeAll.size(), equalTo(2));
    assertThat(xrevrangeAll.get(0).getFields(), equalTo(entryData2)); // The latest entry comes first
    assertThat(xrevrangeAll.get(1).getFields(), equalTo(entryData1));

    List<StreamEntry> xrevrangeAllCount = exec(commandObjects.xrevrange(key, null, (StreamEntryID) null, 1));
    assertThat(xrevrangeAllCount.size(), equalTo(1));
    assertThat(xrevrangeAllCount.get(0).getFields(), equalTo(entryData2)); // Only the latest entry is returned

    List<StreamEntry> xrevrangeEndStart = exec(commandObjects.xrevrange(key, endID, startID));
    assertThat(xrevrangeEndStart.size(), equalTo(2));
    assertThat(xrevrangeEndStart.get(0).getFields(), equalTo(entryData2));
    assertThat(xrevrangeEndStart.get(1).getFields(), equalTo(entryData1));

    List<StreamEntry> xrevrangeStartEndCount = exec(commandObjects.xrevrange(key, endID, startID, 1));
    assertThat(xrevrangeStartEndCount.size(), equalTo(1));
    assertThat(xrevrangeStartEndCount.get(0).getFields(), equalTo(entryData2));

    List<StreamEntry> xrevrangeUnknown = exec(commandObjects.xrevrange("nonExistingStream", null, (StreamEntryID) null));
    assertThat(xrevrangeUnknown, empty());
  }

  @Test
  public void testXrevrangeWithStringParameters() {
    String key = "testStreamForXrevrangeString";

    Map<String, String> entryData1 = new HashMap<>();
    entryData1.put("field1", "value1");

    Map<String, String> entryData2 = new HashMap<>();
    entryData2.put("field2", "value2");

    StreamEntryID startID = exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, entryData1));
    StreamEntryID endID = exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, entryData2));

    String start = startID.toString();
    String end = endID.toString();

    List<StreamEntry> xrevrangeAll = exec(commandObjects.xrevrange(key, null, (StreamEntryID) null));
    assertThat(xrevrangeAll.size(), equalTo(2));
    assertThat(xrevrangeAll.get(0).getFields(), equalTo(entryData2)); // The latest entry comes first
    assertThat(xrevrangeAll.get(1).getFields(), equalTo(entryData1));

    List<StreamEntry> xrevrangeEndStart = exec(commandObjects.xrevrange(key, end, start));
    assertThat(xrevrangeEndStart.size(), equalTo(2));
    assertThat(xrevrangeEndStart.get(0).getFields(), equalTo(entryData2));
    assertThat(xrevrangeEndStart.get(1).getFields(), equalTo(entryData1));

    List<StreamEntry> xrevrangeAllCount = exec(commandObjects.xrevrange(key, null, (StreamEntryID) null, 1));
    assertThat(xrevrangeAllCount.size(), equalTo(1));
    assertThat(xrevrangeAllCount.get(0).getFields(), equalTo(entryData2));

    List<StreamEntry> xrevrangeEndStartCount = exec(commandObjects.xrevrange(key, end, start, 1));
    assertThat(xrevrangeEndStartCount.size(), equalTo(1));
    assertThat(xrevrangeEndStartCount.get(0).getFields(), equalTo(entryData2));

    List<StreamEntry> xrevrangeUnknown = exec(commandObjects.xrevrange("nonExistingStream", end, start));
    assertThat(xrevrangeUnknown, empty());
  }

  @Test
  public void testXrevrangeWithBinaryParameters() {
    String keyStr = "testStreamForXrevrangeBytes";
    byte[] key = keyStr.getBytes();

    Map<String, String> entryData1 = new HashMap<>();
    entryData1.put("field1", "value1");

    Map<String, String> entryData2 = new HashMap<>();
    entryData2.put("field2", "value2");

    StreamEntryID startID = exec(commandObjects.xadd(keyStr, StreamEntryID.NEW_ENTRY, entryData1));
    StreamEntryID endID = exec(commandObjects.xadd(keyStr, StreamEntryID.NEW_ENTRY, entryData2));

    byte[] start = startID.toString().getBytes();
    byte[] end = endID.toString().getBytes();

    List<Object> xrevrangeAll = exec(commandObjects.xrevrange(key, null, null));
    assertThat(xrevrangeAll, hasSize(2));
    assertThat(xrevrangeAll.get(0), instanceOf(List.class));
    assertThat(((List<?>) xrevrangeAll.get(0)).get(0), equalTo(end));
    assertThat(((List<?>) xrevrangeAll.get(1)).get(0), equalTo(start));

    List<Object> xrevrangeEndStart = exec(commandObjects.xrevrange(key, end, start));
    assertThat(xrevrangeEndStart, hasSize(2));
    assertThat(xrevrangeEndStart.get(0), instanceOf(List.class));
    assertThat(((List<?>) xrevrangeEndStart.get(0)).get(0), equalTo(end));
    assertThat(((List<?>) xrevrangeEndStart.get(1)).get(0), equalTo(start));

    List<Object> xrevrangeAllCount = exec(commandObjects.xrevrange(key, null, null, 1));
    assertThat(xrevrangeAllCount, hasSize(1));
    assertThat(xrevrangeAllCount.get(0), instanceOf(List.class));
    assertThat(((List<?>) xrevrangeAllCount.get(0)).get(0), equalTo(end));

    List<Object> xrevrangeEndStartCount = exec(commandObjects.xrevrange(key, end, start, 1));
    assertThat(xrevrangeEndStartCount, hasSize(1));
    assertThat(xrevrangeEndStartCount.get(0), instanceOf(List.class));
    assertThat(((List<?>) xrevrangeEndStartCount.get(0)).get(0), equalTo(end));

    List<Object> xrevrangeUnknown = exec(commandObjects.xrevrange("nonExistingStream".getBytes(), end, start));
    assertThat(xrevrangeUnknown, empty());
  }

  @Test
  public void testXaddWithNullId() {
    String key = "testStreamWithString";

    // Add two entries, don't specify the IDs
    StreamEntryID firstId = exec(commandObjects.xadd(key, (StreamEntryID) null, Collections.singletonMap("field", "value")));
    assertThat(firstId, notNullValue());

    StreamEntryID secondId = exec(commandObjects.xadd(key, (StreamEntryID) null, Collections.singletonMap("field", "value")));
    assertThat(secondId, notNullValue());

    assertThat(secondId, not(equalTo(firstId)));
    assertThat(secondId.getSequence(), greaterThanOrEqualTo(firstId.getSequence()));

    List<StreamEntry> xrangeAll = exec(commandObjects.xrange(key, (StreamEntryID) null, null));
    assertThat(xrangeAll.size(), equalTo(2));
    assertThat(xrangeAll.get(0).getID(), equalTo(firstId));
    assertThat(xrangeAll.get(1).getID(), equalTo(secondId));
  }

  @Test
  public void testXackXpending() {
    String key = "testStreamForXackEffect";
    String group = "testGroup";
    String consumer = "testConsumer";

    Map<String, String> entryData = new HashMap<>();
    entryData.put("field1", "value1");

    exec(commandObjects.xgroupCreate(key, group, new StreamEntryID(), true));

    StreamEntryID entryID = exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, entryData));

    Map<String, StreamEntryID> streams = Collections.singletonMap(key, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);

    XReadGroupParams params = new XReadGroupParams();

    List<Map.Entry<String, List<StreamEntry>>> messages = exec(commandObjects.xreadGroup(group, consumer, params, streams));

    assertThat(messages, hasSize(1));
    assertThat(messages.get(0).getKey(), equalTo(key));
    assertThat(messages.get(0).getValue(), hasSize(1));
    assertThat(messages.get(0).getValue().get(0).getID(), equalTo(entryID));

    StreamPendingSummary pendingSummary = exec(commandObjects.xpending(key, group));
    assertThat(pendingSummary.getTotal(), equalTo(1L));

    XPendingParams xPendingParams = new XPendingParams()
        .start(StreamEntryID.MINIMUM_ID).end(StreamEntryID.MAXIMUM_ID).count(1000);
    List<StreamPendingEntry> pendingSummaryWithParams = exec(commandObjects.xpending(key, group, xPendingParams));

    assertThat(pendingSummaryWithParams, hasSize(1));
    assertThat(pendingSummaryWithParams.get(0).getConsumerName(), equalTo(consumer));
    assertThat(pendingSummaryWithParams.get(0).getID(), equalTo(entryID));

    Long ack = exec(commandObjects.xack(key, group, entryID));
    assertThat(ack, equalTo(1L));

    pendingSummary = exec(commandObjects.xpending(key, group));
    assertThat(pendingSummary.getTotal(), equalTo(0L));

    pendingSummaryWithParams = exec(commandObjects.xpending(key, group, xPendingParams));
    assertThat(pendingSummaryWithParams, empty());
  }

  @Test
  public void testXackXPendingBinary() {
    String keyStr = "testStreamForXackEffect";
    byte[] key = keyStr.getBytes();
    byte[] group = "testGroup".getBytes();
    byte[] consumer = "testConsumer".getBytes();

    Map<String, String> entryData = new HashMap<>();
    entryData.put("field1", "value1");

    exec(commandObjects.xgroupCreate(key, group, new StreamEntryID().toString().getBytes(), true));

    StreamEntryID entryID = exec(commandObjects.xadd(keyStr, StreamEntryID.NEW_ENTRY, entryData));

    Map.Entry<byte[], byte[]> stream = new AbstractMap.SimpleEntry<>(key, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY.toString().getBytes());

    XReadGroupParams params = new XReadGroupParams();

    List<Object> messages = exec(commandObjects.xreadGroup(group, consumer, params, stream));
    assertThat(messages, hasSize(1));

    Object pendingSummary = exec(commandObjects.xpending(key, group));

    assertThat(pendingSummary, instanceOf(List.class));
    assertThat(((List<?>) pendingSummary).get(0), equalTo(1L));

    XPendingParams xPendingParams = new XPendingParams()
        .start(StreamEntryID.MINIMUM_ID).end(StreamEntryID.MAXIMUM_ID).count(1000);

    List<Object> pendingList = exec(commandObjects.xpending(key, group, xPendingParams));
    assertThat(pendingList, hasSize(1));

    Long ack = exec(commandObjects.xack(key, group, entryID.toString().getBytes()));
    assertThat(ack, equalTo(1L));

    pendingSummary = exec(commandObjects.xpending(key, group));
    assertThat(pendingSummary, instanceOf(List.class));
    assertThat(((List<?>) pendingSummary).get(0), equalTo(0L));

    pendingList = exec(commandObjects.xpending(key, group, xPendingParams));
    assertThat(pendingList, empty());
  }

  @Test
  public void testXGroupSetID() {
    String key = "testStream";
    String groupName = "testGroup";

    StreamEntryID initialId = new StreamEntryID();
    StreamEntryID newId = new StreamEntryID("0-1");
    StreamEntryID newId2 = new StreamEntryID("0-2");

    exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, Collections.singletonMap("field", "value")));

    exec(commandObjects.xgroupCreate(key, groupName, initialId, false));

    List<StreamGroupInfo> groupIdBefore = exec(commandObjects.xinfoGroups(key));

    assertThat(groupIdBefore, hasSize(1));
    assertThat(groupIdBefore.get(0).getName(), equalTo(groupName));
    assertThat(groupIdBefore.get(0).getLastDeliveredId(), equalTo(initialId));

    String xgroupSetId = exec(commandObjects.xgroupSetID(key, groupName, newId));
    assertThat(xgroupSetId, equalTo("OK"));

    List<StreamGroupInfo> groupIdAfter = exec(commandObjects.xinfoGroups(key));

    assertThat(groupIdAfter, hasSize(1));
    assertThat(groupIdAfter.get(0).getName(), equalTo(groupName));
    assertThat(groupIdAfter.get(0).getLastDeliveredId(), equalTo(newId));

    String xgroupSetIdBinary = exec(commandObjects.xgroupSetID(key.getBytes(), groupName.getBytes(), newId2.toString().getBytes()));
    assertThat(xgroupSetIdBinary, equalTo("OK"));

    List<StreamGroupInfo> groupIdAfterBinary = exec(commandObjects.xinfoGroups(key));

    assertThat(groupIdAfterBinary, hasSize(1));
    assertThat(groupIdAfterBinary.get(0).getName(), equalTo(groupName));
    assertThat(groupIdAfterBinary.get(0).getLastDeliveredId(), equalTo(newId2));

    List<Object> binaryGroupIdAfterBinary = exec(commandObjects.xinfoGroups(key.getBytes()));
    assertThat(binaryGroupIdAfterBinary, notNullValue());
  }

  @Test
  public void testXGroupDestroy() {
    String key = "testStream";
    String groupName = "testGroup";

    StreamEntryID initialId = new StreamEntryID();

    exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, Collections.singletonMap("field", "value")));

    exec(commandObjects.xgroupCreate(key, groupName, initialId, false));

    List<StreamGroupInfo> groupIdBefore = exec(commandObjects.xinfoGroups(key));

    assertThat(groupIdBefore, hasSize(1));
    assertThat(groupIdBefore.get(0).getName(), equalTo(groupName));
    assertThat(groupIdBefore.get(0).getLastDeliveredId(), equalTo(initialId));

    Long xgroupDestroy = exec(commandObjects.xgroupDestroy(key, groupName));
    assertThat(xgroupDestroy, equalTo(1L));

    List<StreamGroupInfo> groupInfoAfter = exec(commandObjects.xinfoGroups(key));
    assertThat(groupInfoAfter, empty());

    // Re-create the group
    exec(commandObjects.xgroupCreate(key, groupName, initialId, false));

    List<StreamGroupInfo> groupIdBeforeBinary = exec(commandObjects.xinfoGroups(key));

    assertThat(groupIdBeforeBinary, hasSize(1));
    assertThat(groupIdBeforeBinary.get(0).getName(), equalTo(groupName));
    assertThat(groupIdBeforeBinary.get(0).getLastDeliveredId(), equalTo(initialId));

    Long xgroupDestroyBinary = exec(commandObjects.xgroupDestroy(key.getBytes(), groupName.getBytes()));
    assertThat(xgroupDestroyBinary, equalTo(1L));

    List<StreamGroupInfo> groupInfoAfterBinary = exec(commandObjects.xinfoGroups(key));
    assertThat(groupInfoAfterBinary, empty());
  }

  @Test
  public void testXGroupConsumer() {
    String key = "testStream";
    String groupName = "testGroup";
    String consumerName = "testConsumer";

    StreamEntryID initialId = new StreamEntryID();

    exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, Collections.singletonMap("field", "value")));

    exec(commandObjects.xgroupCreate(key, groupName, initialId, false));

    List<StreamGroupInfo> groupIdBefore = exec(commandObjects.xinfoGroups(key));

    assertThat(groupIdBefore, hasSize(1));
    assertThat(groupIdBefore.get(0).getName(), equalTo(groupName));
    assertThat(groupIdBefore.get(0).getConsumers(), equalTo(0L));

    Boolean createConsumer = exec(commandObjects.xgroupCreateConsumer(key, groupName, consumerName));
    assertThat(createConsumer, equalTo(true));

    List<StreamGroupInfo> groupIdAfterCreateConsumer = exec(commandObjects.xinfoGroups(key));

    assertThat(groupIdAfterCreateConsumer, hasSize(1));
    assertThat(groupIdAfterCreateConsumer.get(0).getName(), equalTo(groupName));
    assertThat(groupIdAfterCreateConsumer.get(0).getConsumers(), equalTo(1L));

    Long deleteConsumer = exec(commandObjects.xgroupDelConsumer(key, groupName, consumerName));
    assertThat(deleteConsumer, equalTo(0L));

    List<StreamGroupInfo> groupIdAfterDeleteConsumer = exec(commandObjects.xinfoGroups(key));

    assertThat(groupIdAfterDeleteConsumer, hasSize(1));
    assertThat(groupIdAfterDeleteConsumer.get(0).getName(), equalTo(groupName));
    assertThat(groupIdAfterDeleteConsumer.get(0).getConsumers(), equalTo(0L));

    Boolean createConsumerBinary = exec(commandObjects.xgroupCreateConsumer(
        key.getBytes(), groupName.getBytes(), consumerName.getBytes()));
    assertThat(createConsumerBinary, equalTo(true));

    List<StreamGroupInfo> groupIdAfterCreateConsumerBinary = exec(commandObjects.xinfoGroups(key));

    assertThat(groupIdAfterCreateConsumerBinary, hasSize(1));
    assertThat(groupIdAfterCreateConsumerBinary.get(0).getName(), equalTo(groupName));
    assertThat(groupIdAfterCreateConsumerBinary.get(0).getConsumers(), equalTo(1L));

    Long deleteConsumerBinary = exec(commandObjects.xgroupDelConsumer(
        key.getBytes(), groupName.getBytes(), consumerName.getBytes()));
    assertThat(deleteConsumerBinary, equalTo(0L));

    List<StreamGroupInfo> groupIdAfterDeleteConsumerBinary = exec(commandObjects.xinfoGroups(key));

    assertThat(groupIdAfterDeleteConsumerBinary, hasSize(1));
    assertThat(groupIdAfterDeleteConsumerBinary.get(0).getName(), equalTo(groupName));
    assertThat(groupIdAfterDeleteConsumerBinary.get(0).getConsumers(), equalTo(0L));
  }

  @Test
  public void testXDelWithStreamSize() {
    String key = "testStream";

    StreamEntryID id1 = exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, Collections.singletonMap("field1", "value1")));
    StreamEntryID id2 = exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, Collections.singletonMap("field2", "value2")));

    Long sizeBefore = exec(commandObjects.xlen(key));
    assertThat(sizeBefore, equalTo(2L));

    Long xdel = exec(commandObjects.xdel(key, id1, id2));
    assertThat(xdel, equalTo(2L));

    Long sizeAfterStringDeletion = exec(commandObjects.xlen(key));
    assertThat(sizeAfterStringDeletion, equalTo(0L));

    StreamEntryID id3 = exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, Collections.singletonMap("field3", "value3")));
    StreamEntryID id4 = exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, Collections.singletonMap("field4", "value4")));

    Long sizeBeforeBinaryDeletion = exec(commandObjects.xlen(key));
    assertThat(sizeBeforeBinaryDeletion, equalTo(2L));

    Long xdelBinary = exec(commandObjects.xdel(
        key.getBytes(), id3.toString().getBytes(), id4.toString().getBytes()));
    assertThat(xdelBinary, equalTo(2L));

    Long sizeAfterBinaryDeletion = exec(commandObjects.xlen(key));
    assertThat(sizeAfterBinaryDeletion, equalTo(0L));
  }

  @Test
  public void testXTrimCommands() {
    String key = "testStream";

    // Populate the stream with more entries than we intend to keep
    for (int i = 0; i < 10; i++) {
      exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, Collections.singletonMap("field" + i, "value" + i)));
    }

    Long sizeBeforeTrim = exec(commandObjects.xlen(key));
    assertThat(sizeBeforeTrim, equalTo(10L));

    Long xtrim = exec(commandObjects.xtrim(key, 6, false));
    assertThat(xtrim, equalTo(4L));

    Long sizeAfterTrim = exec(commandObjects.xlen(key));
    assertThat(sizeAfterTrim, equalTo(6L));

    Long xtrimApproximate = exec(commandObjects.xtrim(key, 3, true));
    assertThat(xtrimApproximate, lessThanOrEqualTo(3L));

    Long sizeAfterApproximateTrim = exec(commandObjects.xlen(key));
    assertThat(sizeAfterApproximateTrim, greaterThanOrEqualTo(3L));
  }

  @Test
  public void testXTrimCommandsBinary() {
    String keyStr = "testStream";
    byte[] key = keyStr.getBytes();

    // Populate the stream with more entries than we intend to keep
    for (int i = 0; i < 10; i++) {
      exec(commandObjects.xadd(keyStr, StreamEntryID.NEW_ENTRY, Collections.singletonMap("field" + i, "value" + i)));
    }

    Long sizeBeforeBinaryTrim = exec(commandObjects.xlen(key));
    assertThat(sizeBeforeBinaryTrim, equalTo(10L));

    Long xtrimBinary = exec(commandObjects.xtrim(key, 6, false));
    assertThat(xtrimBinary, equalTo(4L));

    Long sizeAfterBinaryTrim = exec(commandObjects.xlen(key));
    assertThat(sizeAfterBinaryTrim, equalTo(6L));

    Long xtrimApproximateBinary = exec(commandObjects.xtrim(key, 3, true));
    assertThat(xtrimApproximateBinary, lessThanOrEqualTo(3L));

    Long sizeAfterApproximateBinaryTrim = exec(commandObjects.xlen(key));
    assertThat(sizeAfterApproximateBinaryTrim, greaterThanOrEqualTo(3L));
  }

  @Test
  public void testXTrimWithParams() {
    String key = "testStream";

    // Populate the stream with more entries than we intend to keep
    for (int i = 0; i < 10; i++) {
      exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, Collections.singletonMap("field" + i, "value" + i)));
    }

    Long sizeBeforeTrim = exec(commandObjects.xlen(key));
    assertThat(sizeBeforeTrim, equalTo(10L));

    XTrimParams params = new XTrimParams().maxLen(6);

    Long xtrim = exec(commandObjects.xtrim(key, params));
    assertThat(xtrim, equalTo(4L));

    Long sizeAfterTrim = exec(commandObjects.xlen(key));
    assertThat(sizeAfterTrim, equalTo(6L));
  }

  @Test
  public void testXTrimWithParamsBinary() {
    String keyStr = "testStream";
    byte[] key = keyStr.getBytes();

    // Populate the stream with more entries than we intend to keep
    for (int i = 0; i < 10; i++) {
      exec(commandObjects.xadd(keyStr, StreamEntryID.NEW_ENTRY, Collections.singletonMap("field" + i, "value" + i)));
    }

    Long sizeBeforeTrim = exec(commandObjects.xlen(key));
    assertThat(sizeBeforeTrim, equalTo(10L));

    XTrimParams params = new XTrimParams().maxLen(6);

    Long xtrimBinary = exec(commandObjects.xtrim(key, params));
    assertThat(xtrimBinary, equalTo(4L));

    Long sizeAfterTrim = exec(commandObjects.xlen(key));
    assertThat(sizeAfterTrim, equalTo(6L));
  }

  @Test
  public void testXClaim() throws InterruptedException {
    String key = "testStream";
    String group = "testGroup";
    String consumer1 = "consumer1";
    String consumer2 = "consumer2";

    StreamEntryID initialId = new StreamEntryID();

    exec(commandObjects.xgroupCreate(key, group, initialId, true));

    StreamEntryID messageId = exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, Collections.singletonMap("field", "value2")));

    // Consumer1 reads the message to make it pending
    Map<String, StreamEntryID> stream = Collections.singletonMap(key, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    List<Map.Entry<String, List<StreamEntry>>> readEntries = exec(
        commandObjects.xreadGroup(group, consumer1, new XReadGroupParams().count(1), stream));

    assertThat(readEntries, hasSize(1));
    assertThat(readEntries.get(0).getKey(), equalTo(key));
    assertThat(readEntries.get(0).getValue(), hasSize(1));
    assertThat(readEntries.get(0).getValue().get(0).getID(), equalTo(messageId));

    Thread.sleep(200); // Wait a bit

    // Claim the message for consumer2
    List<StreamEntry> claimedMessages = exec(
        commandObjects.xclaim(key, group, consumer2, 1, new XClaimParams(), messageId));

    assertThat(claimedMessages, hasSize(1));
    assertThat(claimedMessages.get(0).getID(), equalTo(messageId));
  }

  @Test
  public void testXClaimBinary() throws InterruptedException {
    String key = "testStream";
    String group = "testGroup";
    String consumer1 = "consumer1";
    String consumer2 = "consumer2";

    StreamEntryID initialId = new StreamEntryID();

    exec(commandObjects.xgroupCreate(key, group, initialId, true));

    StreamEntryID messageId = exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, Collections.singletonMap("field", "value2")));

    // Consumer1 reads the message to make it pending
    Map<String, StreamEntryID> stream = Collections.singletonMap(key, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    List<Map.Entry<String, List<StreamEntry>>> readEntries = exec(
        commandObjects.xreadGroup(group, consumer1, new XReadGroupParams().count(1), stream));

    assertThat(readEntries, hasSize(1));
    assertThat(readEntries.get(0).getKey(), equalTo(key));
    assertThat(readEntries.get(0).getValue(), hasSize(1));
    assertThat(readEntries.get(0).getValue().get(0).getID(), equalTo(messageId));

    Thread.sleep(200); // Wait a bit

    byte[] bMessageId = messageId.toString().getBytes();

    // Claim the message for consumer2
    List<byte[]> claimedMessagesBytes = exec(
        commandObjects.xclaim(key.getBytes(), group.getBytes(), consumer2.getBytes(), 1, new XClaimParams(), bMessageId));
    assertThat(claimedMessagesBytes, hasSize(1));
    // Good luck with asserting the content of this!
  }

  @Test
  public void testXClaimJustId() throws InterruptedException {
    String key = "testStream";
    String group = "testGroup";
    String consumer1 = "consumer1";
    String consumer2 = "consumer2";

    StreamEntryID initialId = new StreamEntryID();

    exec(commandObjects.xgroupCreate(key, group, initialId, true));

    StreamEntryID messageId = exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, Collections.singletonMap("field", "value2")));

    // Consumer1 reads the message to make it pending
    Map<String, StreamEntryID> stream = Collections.singletonMap(key, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    List<Map.Entry<String, List<StreamEntry>>> readEntries = exec(
        commandObjects.xreadGroup(group, consumer1, new XReadGroupParams().count(1), stream));

    assertThat(readEntries, hasSize(1));
    assertThat(readEntries.get(0).getKey(), equalTo(key));
    assertThat(readEntries.get(0).getValue(), hasSize(1));
    assertThat(readEntries.get(0).getValue().get(0).getID(), equalTo(messageId));

    Thread.sleep(200); // Wait a bit

    // Claim the message for consumer2 with String parameters
    List<StreamEntryID> claimedMessagesString = exec(
        commandObjects.xclaimJustId(key, group, consumer2, 1, new XClaimParams(), messageId));
    assertThat(claimedMessagesString, hasSize(1));
    assertThat(claimedMessagesString.get(0), equalTo(messageId));
  }

  @Test
  public void testXClaimJustIdBinary() throws InterruptedException {
    String key = "testStream";
    String group = "testGroup";
    String consumer1 = "consumer1";
    String consumer2 = "consumer2";

    StreamEntryID initialId = new StreamEntryID();

    exec(commandObjects.xgroupCreate(key, group, initialId, true));

    StreamEntryID messageId = exec(commandObjects.xadd(key, StreamEntryID.NEW_ENTRY, Collections.singletonMap("field", "value2")));

    // Consumer1 reads the message to make it pending
    Map<String, StreamEntryID> stream = Collections.singletonMap(key, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    List<Map.Entry<String, List<StreamEntry>>> readEntries = exec(
        commandObjects.xreadGroup(group, consumer1, new XReadGroupParams().count(1), stream));

    assertThat(readEntries, hasSize(1));
    assertThat(readEntries.get(0).getKey(), equalTo(key));
    assertThat(readEntries.get(0).getValue(), hasSize(1));
    assertThat(readEntries.get(0).getValue().get(0).getID(), equalTo(messageId));

    Thread.sleep(200); // Wait a bit

    byte[] bMessageId = messageId.toString().getBytes();

    // Claim the message for consumer2 with byte[] parameters
    List<byte[]> claimedMessagesBytes = exec(
        commandObjects.xclaimJustId(key.getBytes(), group.getBytes(), consumer2.getBytes(), 1, new XClaimParams(), bMessageId));
    assertThat(claimedMessagesBytes, hasSize(1));
    // Good luck with asserting the content of this!
  }

  @Test
  public void testXAutoClaim() throws InterruptedException {
    String streamKey = "testStream";
    String group = "testGroup";
    String consumer1 = "consumer1";
    String consumer2 = "consumer2";

    exec(commandObjects.xgroupCreate(streamKey, group, new StreamEntryID(), true));

    Map<String, String> messageBody = Collections.singletonMap("field", "value");
    StreamEntryID initialEntryId = exec(commandObjects.xadd(streamKey, StreamEntryID.NEW_ENTRY, messageBody));

    Map<String, StreamEntryID> stream = Collections.singletonMap(streamKey, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    exec(commandObjects.xreadGroup(group, consumer1, new XReadGroupParams().count(1), stream));

    Thread.sleep(200); // Wait a bit

    StreamEntryID startId = new StreamEntryID(initialEntryId.getTime() - 1, initialEntryId.getSequence());
    XAutoClaimParams params = new XAutoClaimParams().count(1);

    // Auto claim message for consumer2
    Map.Entry<StreamEntryID, List<StreamEntry>> autoClaimResult = exec(
        commandObjects.xautoclaim(streamKey, group, consumer2, 1, startId, params));

    assertThat(autoClaimResult.getValue(), hasSize(1));
    assertThat(autoClaimResult.getValue().get(0).getFields(), equalTo(messageBody));
  }

  @Test
  public void testXAutoClaimBinary() throws InterruptedException {
    byte[] streamKey = "testStream".getBytes();
    byte[] group = "testGroup".getBytes();
    byte[] consumer1 = "consumer1".getBytes();
    byte[] consumer2 = "consumer2".getBytes();

    exec(commandObjects.xgroupCreate(streamKey, group, new StreamEntryID().toString().getBytes(), true));

    Map<byte[], byte[]> messageBody = Collections.singletonMap("field".getBytes(), "value".getBytes());
    byte[] initialEntryId = exec(commandObjects.xadd(streamKey, new XAddParams(), messageBody));

    Map.Entry<byte[], byte[]> entry = new AbstractMap.SimpleEntry<>(streamKey, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY.toString().getBytes());
    exec(commandObjects.xreadGroup(group, consumer1, new XReadGroupParams().count(1), entry));

    Thread.sleep(200); // Wait a bit

    StreamEntryID initialStreamEntryID = new StreamEntryID(new String(initialEntryId));
    byte[] startId = new StreamEntryID(initialStreamEntryID.getTime() - 1, 0).toString().getBytes();
    XAutoClaimParams params = new XAutoClaimParams().count(1);

    // Auto claim message for consumer2 in binary
    List<Object> autoClaimResultBinary = exec(commandObjects.xautoclaim(streamKey, group, consumer2, 1, startId, params));
    assertThat(autoClaimResultBinary, not(empty()));
  }

  @Test
  public void testXAutoClaimJustId() throws InterruptedException {
    String streamKey = "testStream";
    String group = "testGroup";
    String consumer1 = "consumer1";
    String consumer2 = "consumer2";

    exec(commandObjects.xgroupCreate(streamKey, group, new StreamEntryID(), true));

    Map<String, String> messageBody = Collections.singletonMap("fieldSingle", "valueSingle");
    StreamEntryID initialEntryId = exec(commandObjects.xadd(streamKey, StreamEntryID.NEW_ENTRY, messageBody));

    Map<String, StreamEntryID> stream = Collections.singletonMap(streamKey, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    exec(commandObjects.xreadGroup(group, consumer1, new XReadGroupParams().count(1), stream));

    Thread.sleep(200); // Wait a bit

    StreamEntryID startId = new StreamEntryID(initialEntryId.getTime() - 1, initialEntryId.getSequence());
    XAutoClaimParams params = new XAutoClaimParams().count(1);

    Map.Entry<StreamEntryID, List<StreamEntryID>> autoClaimResult = exec(
        commandObjects.xautoclaimJustId(streamKey, group, consumer2, 1, startId, params));

    assertThat(autoClaimResult.getValue(), hasSize(1));
    assertThat(autoClaimResult.getValue().get(0), equalTo(initialEntryId));
  }

  @Test
  public void testXAutoClaimJustIdBinary() throws InterruptedException {
    byte[] streamKey = "testStream".getBytes();
    byte[] group = "testGroup".getBytes();
    byte[] consumer1 = "consumer1".getBytes();
    byte[] consumer2 = "consumer2".getBytes();

    exec(commandObjects.xgroupCreate(streamKey, group, new StreamEntryID().toString().getBytes(), true));

    Map<byte[], byte[]> messageBody = Collections.singletonMap("fieldBinary".getBytes(), "valueBinary".getBytes());
    byte[] initialEntryId = exec(commandObjects.xadd(streamKey, new XAddParams(), messageBody));

    Map.Entry<byte[], byte[]> stream = new AbstractMap.SimpleEntry<>(streamKey, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY.toString().getBytes());
    exec(commandObjects.xreadGroup(group, consumer1, new XReadGroupParams().count(1), stream));

    Thread.sleep(200); // Wait a bit

    StreamEntryID initialStreamEntryID = new StreamEntryID(new String(initialEntryId));
    byte[] startId = new StreamEntryID(initialStreamEntryID.getTime() - 1, 0).toString().getBytes();
    XAutoClaimParams params = new XAutoClaimParams().count(1);

    List<Object> autoClaimResultBinary = exec(
        commandObjects.xautoclaimJustId(streamKey, group, consumer2, 1, startId, params));
    assertThat(autoClaimResultBinary, not(empty()));
  }

  @Test
  public void testXInfoStream() {
    String streamKey = "testStreamInfo";

    Map<String, String> messageBody = Collections.singletonMap("fieldInfo", "valueInfo");

    exec(commandObjects.xadd(streamKey, StreamEntryID.NEW_ENTRY, messageBody));

    StreamInfo streamInfo = exec(commandObjects.xinfoStream(streamKey));

    assertThat(streamInfo, notNullValue());
    assertThat(streamInfo.getLength(), equalTo(1L));
    assertThat(streamInfo.getFirstEntry().getFields(), equalTo(messageBody));

    Object streamInfoBinary = exec(commandObjects.xinfoStream(streamKey.getBytes()));
    assertThat(streamInfoBinary, notNullValue());
  }

  @Test
  public void testXInfoStreamFull() {
    String streamKey = "testStreamFullInfo";

    Map<String, String> messageBody = Collections.singletonMap("fieldFull", "valueFull");

    exec(commandObjects.xadd(streamKey, StreamEntryID.NEW_ENTRY, messageBody));

    StreamFullInfo streamFullInfo = exec(commandObjects.xinfoStreamFull(streamKey));

    assertThat(streamFullInfo, notNullValue());
    assertThat(streamFullInfo.getEntries(), not(empty()));
    assertThat(streamFullInfo.getEntries().get(0).getFields(), equalTo(messageBody));

    StreamFullInfo streamFullInfoWithCount = exec(commandObjects.xinfoStreamFull(streamKey, 1));
    assertThat(streamFullInfoWithCount, notNullValue());
    assertThat(streamFullInfoWithCount.getEntries(), hasSize(1));

    Object streamInfoBinaryFull = exec(commandObjects.xinfoStreamFull(streamKey.getBytes()));
    assertThat(streamInfoBinaryFull, notNullValue());

    Object streamInfoBinaryFullWithCount = exec(commandObjects.xinfoStreamFull(streamKey.getBytes(), 1));
    assertThat(streamInfoBinaryFullWithCount, notNullValue());
  }

  @Test
  @Deprecated
  public void testXInfoConsumersWithActiveConsumers() {
    String streamKey = "testStreamWithConsumers";
    String group = "testConsumerGroup";
    String consumer1 = "consumer1";
    String consumer2 = "consumer2";

    Map<String, String> messageBody1 = Collections.singletonMap("field1", "value1");
    Map<String, String> messageBody2 = Collections.singletonMap("field2", "value2");

    exec(commandObjects.xadd(streamKey, StreamEntryID.NEW_ENTRY, messageBody1));
    exec(commandObjects.xadd(streamKey, StreamEntryID.NEW_ENTRY, messageBody2));

    exec(commandObjects.xgroupCreate(streamKey, group, new StreamEntryID(), true));

    XReadGroupParams xReadGroupParams = new XReadGroupParams().count(1);
    Map<String, StreamEntryID> stream = Collections.singletonMap(streamKey, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    exec(commandObjects.xreadGroup(group, consumer1, xReadGroupParams, stream));
    exec(commandObjects.xreadGroup(group, consumer2, xReadGroupParams, stream));

    List<StreamConsumersInfo> consumersInfoList = exec(commandObjects.xinfoConsumers(streamKey, group));
    assertThat(consumersInfoList, notNullValue());
    assertThat(consumersInfoList, hasSize(2));

    Optional<StreamConsumersInfo> consumersInfo1 = consumersInfoList.stream().filter(c -> c.getName().equals(consumer1)).findFirst();
    Optional<StreamConsumersInfo> consumersInfo2 = consumersInfoList.stream().filter(c -> c.getName().equals(consumer2)).findFirst();

    assertThat(consumersInfo1.isPresent(), equalTo(true));
    assertThat(consumersInfo1.get().getPending(), equalTo(1L));

    assertThat(consumersInfo2.isPresent(), equalTo(true));
    assertThat(consumersInfo2.get().getPending(), equalTo(1L));

    List<StreamConsumerInfo> consumerInfoList = exec(commandObjects.xinfoConsumers2(streamKey, group));
    assertThat(consumerInfoList, notNullValue());
    assertThat(consumerInfoList, hasSize(2));

    Optional<StreamConsumerInfo> consumerInfo1 = consumerInfoList.stream().filter(c -> c.getName().equals(consumer1)).findFirst();
    Optional<StreamConsumerInfo> consumerInfo2 = consumerInfoList.stream().filter(c -> c.getName().equals(consumer2)).findFirst();

    assertThat(consumerInfo1.isPresent(), equalTo(true));
    assertThat(consumerInfo1.get().getPending(), equalTo(1L));

    assertThat(consumerInfo2.isPresent(), equalTo(true));
    assertThat(consumerInfo2.get().getPending(), equalTo(1L));

    List<Object> consumersInfoBinary = exec(commandObjects.xinfoConsumers(streamKey.getBytes(), group.getBytes()));
    assertThat(consumersInfoBinary, notNullValue());
  }

  @Test
  public void testXRead() {
    String streamKey1 = "testStream1";
    String streamKey2 = "testStream2";

    Map<String, String> messageBody1 = Collections.singletonMap("field1", "value1");
    Map<String, String> messageBody2 = Collections.singletonMap("field2", "value2");

    StreamEntryID messageId1 = exec(commandObjects.xadd(streamKey1, StreamEntryID.NEW_ENTRY, messageBody1));
    StreamEntryID messageId2 = exec(commandObjects.xadd(streamKey2, StreamEntryID.NEW_ENTRY, messageBody2));

    XReadParams params = XReadParams.xReadParams().count(1).block(1000);
    Map<String, StreamEntryID> streams = new HashMap<>();
    streams.put(streamKey1, new StreamEntryID());
    streams.put(streamKey2, new StreamEntryID());

    List<Map.Entry<String, List<StreamEntry>>> xread = exec(commandObjects.xread(params, streams));

    assertThat(xread, not(empty()));
    assertThat(xread.size(), equalTo(2));
    assertThat(xread.get(0).getKey(), equalTo(streamKey1));
    assertThat(xread.get(1).getKey(), equalTo(streamKey2));
    assertThat(xread.get(0).getValue().get(0).getID(), equalTo(messageId1));
    assertThat(xread.get(1).getValue().get(0).getID(), equalTo(messageId2));
    assertThat(xread.get(0).getValue().get(0).getFields(), equalTo(messageBody1));
    assertThat(xread.get(1).getValue().get(0).getFields(), equalTo(messageBody2));

    byte[] streamKey1Binary = streamKey1.getBytes();
    byte[] streamKey2Binary = streamKey2.getBytes();
    Map.Entry<byte[], byte[]> stream1 = new AbstractMap.SimpleEntry<>(streamKey1Binary, new StreamEntryID().toString().getBytes());
    Map.Entry<byte[], byte[]> stream2 = new AbstractMap.SimpleEntry<>(streamKey2Binary, new StreamEntryID().toString().getBytes());

    List<Object> xreadBinary = exec(commandObjects.xread(params, stream1, stream2));
    assertThat(xreadBinary, not(empty()));
  }

  @Test
  public void testXReadAsMap() {
    String streamKey1 = "testStreamMap1";
    String streamKey2 = "testStreamMap2";

    Map<String, String> messageBody1 = Collections.singletonMap("fieldMap1", "valueMap1");
    Map<String, String> messageBody2 = Collections.singletonMap("fieldMap2", "valueMap2");

    exec(commandObjects.xadd(streamKey1, StreamEntryID.NEW_ENTRY, messageBody1));
    exec(commandObjects.xadd(streamKey2, StreamEntryID.NEW_ENTRY, messageBody2));

    XReadParams params = new XReadParams().count(1).block(1000);

    Map<String, StreamEntryID> streams = new HashMap<>();
    streams.put(streamKey1, new StreamEntryID());
    streams.put(streamKey2, new StreamEntryID());

    Map<String, List<StreamEntry>> xreadAsMap = exec(commandObjects.xreadAsMap(params, streams));
    assertThat(xreadAsMap, notNullValue());
    assertThat(xreadAsMap.keySet(), hasSize(2)); // Expecting keys for both streams
    assertThat(xreadAsMap.get(streamKey1).get(0).getFields(), equalTo(messageBody1));
    assertThat(xreadAsMap.get(streamKey2).get(0).getFields(), equalTo(messageBody2));
  }

  @Test
  public void testXReadGroupAsMap() {
    String streamKey = "testStreamGroupMap";
    String group = "testGroupMap";
    String consumer1 = "testConsumerMap1";
    String consumer2 = "testConsumerMap2";

    Map<String, String> messageBody = Collections.singletonMap("fieldGroupMap", "valueGroupMap");

    exec(commandObjects.xgroupCreate(streamKey, group, new StreamEntryID(), true));

    StreamEntryID initialMessageId = exec(commandObjects.xadd(streamKey, StreamEntryID.NEW_ENTRY, messageBody));
    StreamEntryID secondMessageId = exec(commandObjects.xadd(streamKey, StreamEntryID.NEW_ENTRY, messageBody));

    XReadGroupParams params = new XReadGroupParams().count(1);

    Map<String, StreamEntryID> streams = new HashMap<>();
    streams.put(streamKey, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);

    Map<String, List<StreamEntry>> xreadGroupConsumer1 = exec(commandObjects.xreadGroupAsMap(group, consumer1, params, streams));

    assertThat(xreadGroupConsumer1, notNullValue());
    assertThat(xreadGroupConsumer1.keySet(), hasSize(1));
    assertThat(xreadGroupConsumer1.get(streamKey), not(empty()));
    assertThat(xreadGroupConsumer1.get(streamKey).get(0).getID(), equalTo(initialMessageId));
    assertThat(xreadGroupConsumer1.get(streamKey).get(0).getFields(), equalTo(messageBody));

    Map<String, List<StreamEntry>> xreadGroupConsumer2 = exec(commandObjects.xreadGroupAsMap(group, consumer2, params, streams));

    assertThat(xreadGroupConsumer2, notNullValue());
    assertThat(xreadGroupConsumer2.keySet(), hasSize(1)); // Expecting keys for the stream
    assertThat(xreadGroupConsumer2.get(streamKey), not(empty())); // Expecting at least one message
    assertThat(xreadGroupConsumer2.get(streamKey).get(0).getID(), equalTo(secondMessageId));
    assertThat(xreadGroupConsumer2.get(streamKey).get(0).getFields(), equalTo(messageBody));
  }
}
