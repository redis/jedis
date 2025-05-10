package redis.clients.jedis.commands.jedis;

import io.redis.test.annotations.SinceRedisVersion;
import io.redis.test.utils.RedisVersion;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.*;
import redis.clients.jedis.util.RedisVersionUtil;
import redis.clients.jedis.util.SafeEncoder;

import java.time.Duration;
import java.util.*;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test replicated from the existing {@link StreamsCommandsTest}
 * Tests for xread, xreadBinaryAsMap, xreadGroup, xreadGroupAsMap methods modified to work with Binary data (xreadBinary, xreadGroupBinary)
 */
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class StreamsBinaryCommandsTest extends JedisCommandsTestBase {

  public StreamsBinaryCommandsTest(RedisProtocol protocol) {
            super(protocol);
  }

    @Test
    public void xreadWithParams() {

        final String key1 = "xread-stream1";
        final String key2 = "xread-stream2";

        Map.Entry<byte[], StreamEntryID> streamQeury1 = new AbstractMap.SimpleImmutableEntry<>(key1.getBytes(), new StreamEntryID());

        // Before creating Stream
        assertNull(jedis.xreadBinary(XReadParams.xReadParams().block(1), streamQeury1));
        assertNull(jedis.xreadBinary(XReadParams.xReadParams(), streamQeury1));

        Map<String, String> map = new HashMap<>();
        map.put("f1", "v1");
        StreamEntryID id1 = jedis.xadd(key1, (StreamEntryID) null, map);
        StreamEntryID id2 = jedis.xadd(key2, (StreamEntryID) null, map);

        // Read only a single Stream
        List<Map.Entry<byte[], List<StreamEntryBinary>>> streams1 = jedis.xreadBinary(XReadParams.xReadParams().count(1).block(1), streamQeury1);
        assertEquals(1, streams1.size());
        assertArrayEquals(key1.getBytes(), streams1.get(0).getKey());
        assertEquals(1, streams1.get(0).getValue().size());
        assertEquals(id1, streams1.get(0).getValue().get(0).getID());
        for (Map.Entry<byte[], byte[]> entry : streams1.get(0).getValue().get(0).getFields().entrySet()) {
            String key = SafeEncoder.encode(entry.getKey());
            String value = SafeEncoder.encode(entry.getValue());
            String validate = map.get(key);

            assertEquals(validate, value);
        }

        assertNull(jedis.xreadBinary(XReadParams.xReadParams().block(1), new AbstractMap.SimpleImmutableEntry<>(key1.getBytes(), id1)));
        assertNull(jedis.xreadBinary(XReadParams.xReadParams(), new AbstractMap.SimpleImmutableEntry<>(key1.getBytes(), id1)));

        // Read from two Streams
        Map.Entry<byte[], StreamEntryID> streamQuery2 = new AbstractMap.SimpleImmutableEntry<>(key1.getBytes(), new StreamEntryID());
        Map.Entry<byte[], StreamEntryID> streamQuery3 = new AbstractMap.SimpleImmutableEntry<>(key2.getBytes(), new StreamEntryID());
        List<Map.Entry<byte[], List<StreamEntryBinary>>> streams2 = jedis.xreadBinary(XReadParams.xReadParams().count(2).block(1), streamQuery2, streamQuery3);
        assertEquals(2, streams2.size());
    }

    @Test
    public void xreadAsMap() {

        final String stream1 = "xread-stream1";
        final String stream2 = "xread-stream2";

        Map<byte[] , StreamEntryID> streamQeury1 = singletonMap(stream1.getBytes(), new StreamEntryID());

        // Before creating Stream
        assertNull(jedis.xreadBinaryAsMap(XReadParams.xReadParams().block(1), streamQeury1));
        assertNull(jedis.xreadBinaryAsMap(XReadParams.xReadParams(), streamQeury1));

        Map<String, String> map = new HashMap<>();
        map.put("f1", "v1");
        StreamEntryID id1 = new StreamEntryID(1);
        StreamEntryID id2 = new StreamEntryID(2);
        StreamEntryID id3 = new StreamEntryID(3);

        assertEquals(id1, jedis.xadd(stream1, id1, map));
        assertEquals(id2, jedis.xadd(stream2, id2, map));
        assertEquals(id3, jedis.xadd(stream1, id3, map));

        // Read only a single Stream
        Map<byte[], List<StreamEntryBinary>> streams1 = jedis.xreadBinaryAsMap(XReadParams.xReadParams().count(2), streamQeury1);
        assertArrayEquals(stream1.getBytes(), streams1.keySet().iterator().next());
        List<StreamEntryBinary> list1 = streams1.get(findKey(streams1.keySet(), stream1));
        assertEquals(2, list1.size());
        assertEquals(id1, list1.get(0).getID());
        Map<String, String> map1 = convertToMap(list1.get(0).getFields());
        assertEquals(map, map1);
        assertEquals(id3, list1.get(1).getID());
        Map<String, String> map2 = convertToMap(list1.get(1).getFields());
        assertEquals(map, map2);

        // Read from two Streams
        Map<byte[], StreamEntryID> streamQuery2 = new LinkedHashMap<>();
        streamQuery2.put(stream1.getBytes(), new StreamEntryID());
        streamQuery2.put(stream2.getBytes(), new StreamEntryID());
        Map<byte[], List<StreamEntryBinary>> streams2 = jedis.xreadBinaryAsMap(XReadParams.xReadParams().count(1), streamQuery2);
        assertEquals(2, streams2.size());
        assertEquals(id1, streams2.get(findKey(streams2.keySet(), stream1)).get(0).getID());
        assertEquals(id2, streams2.get(findKey(streams2.keySet(), stream2)).get(0).getID());
    }

    @Test
    @SinceRedisVersion(value = "7.4.0", message = "From Redis 7.4, you can use the + sign as a special ID to request last entry")
    public void xreadAsMapLastEntry() {

        final String stream1 = "xread-stream1";
        final String stream2 = "xread-stream2";

        Map<byte[], StreamEntryID> streamQeury1 = singletonMap(stream1.getBytes(), new StreamEntryID());

        // Before creating Stream
        assertNull(jedis.xreadBinaryAsMap(XReadParams.xReadParams().block(1), streamQeury1));
        assertNull(jedis.xreadBinaryAsMap(XReadParams.xReadParams(), streamQeury1));

        Map<String, String> map = new HashMap<>();
        map.put("f1", "v1");
        StreamEntryID id1 = new StreamEntryID(1);
        StreamEntryID id2 = new StreamEntryID(2);
        StreamEntryID id3 = new StreamEntryID(3);

        assertEquals(id1, jedis.xadd(stream1, id1, map));
        assertEquals(id2, jedis.xadd(stream2, id2, map));
        assertEquals(id3, jedis.xadd(stream1, id3, map));


        // Read from last entry
        Map<byte[], StreamEntryID> streamQueryLE = singletonMap(stream1.getBytes(), StreamEntryID.XREAD_LAST_ENTRY);
        Map<byte[], List<StreamEntryBinary>> streamsLE = jedis.xreadBinaryAsMap(XReadParams.xReadParams().count(1), streamQueryLE);
        assertArrayEquals(stream1.getBytes(), streamsLE.keySet().iterator().next());
        assertEquals(1, streamsLE.get(findKey(streamsLE.keySet(), stream1)).size());
        assertEquals(id3, streamsLE.get(findKey(streamsLE.keySet(), stream1)).get(0).getID());
        assertEquals(map, convertToMap(streamsLE.get(findKey(streamsLE.keySet(), stream1)).get(0).getFields()));
    }

    @Test
    public void xreadGroupWithParams() {

        // Simple xreadGroup with NOACK
        Map<String, String> map = new HashMap<>();
        map.put("f1", "v1");
        jedis.xadd("xreadGroup-stream1", (StreamEntryID) null, map);
        jedis.xgroupCreate("xreadGroup-stream1", "xreadGroup-group", null, false);
        Map<byte[], StreamEntryID> streamQeury1 = singletonMap("xreadGroup-stream1".getBytes(), StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
        List<Map.Entry<byte[], List<StreamEntryBinary>>> range = jedis.xreadGroupBinary("xreadGroup-group".getBytes(), "xreadGroup-consumer".getBytes(),
                XReadGroupParams.xReadGroupParams().count(1).noAck(), streamQeury1);
        assertEquals(1, range.size());
        assertEquals(1, range.get(0).getValue().size());

        jedis.xadd("xreadGroup-stream1", (StreamEntryID) null, map);
        jedis.xadd("xreadGroup-stream2", (StreamEntryID) null, map);
        jedis.xgroupCreate("xreadGroup-stream2", "xreadGroup-group", null, false);

        // Read only a single Stream
        List<Map.Entry<byte[], List<StreamEntryBinary>>> streams1 = jedis.xreadGroupBinary("xreadGroup-group".getBytes(), "xreadGroup-consumer".getBytes(),
                XReadGroupParams.xReadGroupParams().count(1).block(1).noAck(), streamQeury1);
        assertEquals(1, streams1.size());
        assertEquals(1, streams1.get(0).getValue().size());

        // Read from two Streams
        Map<byte[], StreamEntryID> streamQuery2 = new LinkedHashMap<>();
        streamQuery2.put("xreadGroup-stream1".getBytes(), new StreamEntryID());
        streamQuery2.put("xreadGroup-stream2".getBytes(), new StreamEntryID());
        List<Map.Entry<byte[], List<StreamEntryBinary>>> streams2 = jedis.xreadGroupBinary("xreadGroup-group".getBytes(), "xreadGroup-consumer".getBytes(),
                XReadGroupParams.xReadGroupParams().count(1).block(1).noAck(), streamQuery2);
        assertEquals(2, streams2.size());

        // Read only fresh messages
        StreamEntryID id4 = jedis.xadd("xreadGroup-stream1", (StreamEntryID) null, map);
        Map<byte[], StreamEntryID> streamQeuryFresh = singletonMap("xreadGroup-stream1".getBytes(), StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
        List<Map.Entry<byte[], List<StreamEntryBinary>>> streamsFresh = jedis.xreadGroupBinary("xreadGroup-group".getBytes(), "xreadGroup-consumer".getBytes(),
                XReadGroupParams.xReadGroupParams().count(4).block(100).noAck(), streamQeuryFresh);
        assertEquals(1, streamsFresh.size());
        assertEquals(id4, streamsFresh.get(0).getValue().get(0).getID());
    }

    @Test
    public void xreadGroupAsMap() {

        final String stream1 = "xreadGroup-stream1";
        Map<String, String> map = singletonMap("f1", "v1");

        StreamEntryID id1 = jedis.xadd(stream1, StreamEntryID.NEW_ENTRY, map);
        jedis.xgroupCreate(stream1, "xreadGroup-group", null, false);
        Map<byte[], StreamEntryID> streamQeury1 = singletonMap(stream1.getBytes(), StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
        Map<byte[], List<StreamEntryBinary>> range = jedis.xreadGroupBinaryAsMap("xreadGroup-group".getBytes(), "xreadGroup-consumer".getBytes(),
                XReadGroupParams.xReadGroupParams().noAck(), streamQeury1);
        assertArrayEquals(stream1.getBytes(), range.keySet().iterator().next());
        List<StreamEntryBinary> list = range.get(findKey(range.keySet(), stream1));
        assertEquals(1, list.size());
        assertEquals(id1, list.get(0).getID());
        assertEquals(map, convertToMap(list.get(0).getFields()));
    }

    @Test
    public void xreadGroupWithParamsWhenPendingMessageIsDiscarded() {
        // Add two message to stream
        Map<String, String> map1 = new HashMap<>();
        map1.put("f1", "v1");

        Map<String, String> map2 = new HashMap<>();
        map2.put("f2", "v2");

        XAddParams xAddParams = XAddParams.xAddParams().id(StreamEntryID.NEW_ENTRY).maxLen(2);
        StreamEntryID firstMessageEntryId = jedis.xadd("xreadGroup-discard-stream1", xAddParams, map1);
        jedis.xadd("xreadGroup-discard-stream1", xAddParams, map2);

        jedis.xgroupCreate("xreadGroup-discard-stream1", "xreadGroup-group", null, false);
        Map<byte[], StreamEntryID> streamQuery1 = singletonMap("xreadGroup-discard-stream1".getBytes(), StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
        List<Map.Entry<byte[], List<StreamEntryBinary>>> range = jedis.xreadGroupBinary("xreadGroup-group".getBytes(), "xreadGroup-consumer".getBytes(),
                XReadGroupParams.xReadGroupParams().count(1), streamQuery1);
        assertEquals(1, range.size());
        assertEquals(1, range.get(0).getValue().size());

        assertEquals(firstMessageEntryId, range.get(0).getValue().get(0).getID());
        assertEquals(map1, convertToMap(range.get(0).getValue().get(0).getFields()));

        // Add third message, the fields of pending message1 will be discarded by redis-server
        Map<String, String> map3 = new HashMap<>();
        map3.put("f3", "v3");
        jedis.xadd("xreadGroup-discard-stream1", xAddParams, map3);

        Map<byte[], StreamEntryID> streamQueryPending = singletonMap("xreadGroup-discard-stream1".getBytes(), new StreamEntryID());
        List<Map.Entry<byte[], List<StreamEntryBinary>>> pendingMessages = jedis.xreadGroupBinary("xreadGroup-group".getBytes(), "xreadGroup-consumer".getBytes(),
                XReadGroupParams.xReadGroupParams().count(1).noAck(), streamQueryPending);

        assertEquals(1, pendingMessages.size());
        assertEquals(1, pendingMessages.get(0).getValue().size());

        assertEquals(firstMessageEntryId, pendingMessages.get(0).getValue().get(0).getID());
        assertNull(pendingMessages.get(0).getValue().get(0).getFields());
    }

    @Test
    public void xack() {
        Map<String, String> map = new HashMap<>();
        map.put("f1", "v1");
        jedis.xadd("xack-stream", (StreamEntryID) null, map);

        jedis.xgroupCreate("xack-stream", "xack-group", null, false);

        Map<byte[], StreamEntryID> streamQeury1 = singletonMap("xack-stream".getBytes(), StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);

        // Empty Stream
        List<Map.Entry<byte[], List<StreamEntryBinary>>> range = jedis.xreadGroupBinary("xack-group".getBytes(), "xack-consumer".getBytes(),
                XReadGroupParams.xReadGroupParams().count(1).block(1), streamQeury1);
        assertEquals(1, range.size());

        assertEquals(1L,
                jedis.xack("xack-stream", "xack-group", range.get(0).getValue().get(0).getID()));
    }

    @Test
    public void xpendingWithParams() {
        final String stream = "xpendeing-stream";

        assertEquals("OK", jedis.xgroupCreate(stream, "xpendeing-group", null, true));

        // Get the summary from empty stream
        StreamPendingSummary emptySummary = jedis.xpending(stream, "xpendeing-group");
        assertEquals(0, emptySummary.getTotal());
        assertNull(emptySummary.getMinId());
        assertNull(emptySummary.getMaxId());
        assertNull(emptySummary.getConsumerMessageCount());

        Map<String, String> map = new HashMap<>();
        map.put("f1", "v1");
        StreamEntryID id1 = jedis.xadd(stream, (StreamEntryID) null, map);

        Map<byte[], StreamEntryID> streamQeury1 = singletonMap(stream.getBytes(), StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);

        // Read the event from Stream put it on pending
        List<Map.Entry<byte[], List<StreamEntryBinary>>> range = jedis.xreadGroupBinary("xpendeing-group".getBytes(),
                "xpendeing-consumer".getBytes(), XReadGroupParams.xReadGroupParams().count(1).block(1), streamQeury1);
        assertEquals(1, range.size());
        assertEquals(1, range.get(0).getValue().size());
        assertEquals(map, convertToMap(range.get(0).getValue().get(0).getFields()));

        // Get the summary about the pending messages
        StreamPendingSummary pendingSummary = jedis.xpending(stream, "xpendeing-group");
        assertEquals(1, pendingSummary.getTotal());
        assertEquals(id1, pendingSummary.getMinId());
        assertEquals(1l, pendingSummary.getConsumerMessageCount().get("xpendeing-consumer").longValue());

        // Get the pending event
        List<StreamPendingEntry> pendingRange = jedis.xpending(stream, "xpendeing-group",
                new XPendingParams().count(3).consumer("xpendeing-consumer"));
        assertEquals(1, pendingRange.size());
        assertEquals(id1, pendingRange.get(0).getID());
        assertEquals(1, pendingRange.get(0).getDeliveredTimes());
        assertEquals("xpendeing-consumer", pendingRange.get(0).getConsumerName());
        assertTrue(pendingRange.get(0).toString().contains("xpendeing-consumer"));

        // Without consumer
        pendingRange = jedis.xpending(stream, "xpendeing-group", new XPendingParams().count(3));
        assertEquals(1, pendingRange.size());
        assertEquals(id1, pendingRange.get(0).getID());
        assertEquals(1, pendingRange.get(0).getDeliveredTimes());
        assertEquals("xpendeing-consumer", pendingRange.get(0).getConsumerName());

        // with idle
        pendingRange = jedis.xpending(stream, "xpendeing-group",
                new XPendingParams().idle(Duration.ofMinutes(1).toMillis()).count(3));
        assertEquals(0, pendingRange.size());
    }

    @Test
    public void xpendingRange() {
        final String stream = "xpendeing-stream";
        Map<String, String> map = new HashMap<>();
        map.put("foo", "bar");
        StreamEntryID m1 = jedis.xadd(stream, (StreamEntryID) null, map);
        StreamEntryID m2 = jedis.xadd(stream, (StreamEntryID) null, map);
        jedis.xgroupCreate(stream, "xpendeing-group", null, false);

        // read 1 message from the group with each consumer
        Map<byte[], StreamEntryID> streamQeury = singletonMap(stream.getBytes(), StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
        jedis.xreadGroupBinary("xpendeing-group".getBytes(), "consumer1".getBytes(), XReadGroupParams.xReadGroupParams().count(1), streamQeury);
        jedis.xreadGroupBinary("xpendeing-group".getBytes(), "consumer2".getBytes(), XReadGroupParams.xReadGroupParams().count(1), streamQeury);

        List<StreamPendingEntry> response = jedis.xpending(stream, "xpendeing-group",
                XPendingParams.xPendingParams("(0", "+", 5));
        assertEquals(2, response.size());
        assertEquals(m1, response.get(0).getID());
        assertEquals("consumer1", response.get(0).getConsumerName());
        assertEquals(m2, response.get(1).getID());
        assertEquals("consumer2", response.get(1).getConsumerName());

        response = jedis.xpending(stream, "xpendeing-group",
                XPendingParams.xPendingParams(StreamEntryID.MINIMUM_ID, StreamEntryID.MAXIMUM_ID, 5));
        assertEquals(2, response.size());
        assertEquals(m1, response.get(0).getID());
        assertEquals("consumer1", response.get(0).getConsumerName());
        assertEquals(m2, response.get(1).getID());
        assertEquals("consumer2", response.get(1).getConsumerName());
    }

    @Test
    public void xclaimWithParams() {
        final String stream = "xpendeing-stream";
        Map<String, String> map = new HashMap<>();
        map.put("f1", "v1");
        jedis.xadd(stream, (StreamEntryID) null, map);

        assertEquals("OK", jedis.xgroupCreate(stream, "xpendeing-group", null, false));

        // Read the event from Stream put it on pending
        jedis.xreadGroupBinary("xpendeing-group".getBytes(), "xpendeing-consumer".getBytes(), XReadGroupParams.xReadGroupParams().count(1).block(1),
                singletonMap(stream.getBytes(), StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY));

        // Get the pending event
        List<StreamPendingEntry> pendingRange = jedis.xpending(stream, "xpendeing-group",
                XPendingParams.xPendingParams().count(3).consumer("xpendeing-consumer"));

        // Sleep for 100ms so we can claim events pending for more than 50ms
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<StreamEntry> streamEntrys = jedis.xclaim(stream, "xpendeing-group",
                "xpendeing-consumer2", 50, XClaimParams.xClaimParams().idle(0).retryCount(0),
                pendingRange.get(0).getID());
        assertEquals(1, streamEntrys.size());
        assertEquals(pendingRange.get(0).getID(), streamEntrys.get(0).getID());
        assertEquals("v1", streamEntrys.get(0).getFields().get("f1"));
    }

    @Test
    public void xclaimJustId() {
        final String stream = "xpendeing-stream";
        Map<String, String> map = new HashMap<>();
        map.put("f1", "v1");
        jedis.xadd(stream, (StreamEntryID) null, map);

        assertEquals("OK", jedis.xgroupCreate(stream, "xpendeing-group", null, false));

        // Read the event from Stream put it on pending
        jedis.xreadGroupBinary("xpendeing-group".getBytes(), "xpendeing-consumer".getBytes(), XReadGroupParams.xReadGroupParams().count(1).block(1),
                singletonMap(stream.getBytes(), StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY));

        // Get the pending event
        List<StreamPendingEntry> pendingRange = jedis.xpending(stream, "xpendeing-group",
                XPendingParams.xPendingParams().count(3).consumer("xpendeing-consumer"));
        // Sleep for 100ms so we can claim events pending for more than 50ms
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<StreamEntryID> streamEntryIDS = jedis.xclaimJustId(stream, "xpendeing-group",
                "xpendeing-consumer2", 50, XClaimParams.xClaimParams().idle(0).retryCount(0),
                pendingRange.get(0).getID());
        assertEquals(1, streamEntryIDS.size());
        assertEquals(pendingRange.get(0).getID(), streamEntryIDS.get(0));
    }

    @Test
    public void xautoclaim() {
        Map<String, String> map = new HashMap<>();
        map.put("f1", "v1");
        jedis.xadd("xpending-stream", (StreamEntryID) null, map);

        assertEquals("OK", jedis.xgroupCreate("xpending-stream", "xpending-group", null, false));

        // Read the event from Stream put it on pending
        jedis.xreadGroupBinary("xpending-group".getBytes(), "xpending-consumer".getBytes(), XReadGroupParams.xReadGroupParams().count(1).block(1),
                singletonMap("xpending-stream".getBytes(), StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY));

        // Get the pending event
        List<StreamPendingEntry> pendingRange = jedis.xpending("xpending-stream", "xpending-group",
                XPendingParams.xPendingParams().count(3).consumer("xpending-consumer"));
        // Sleep for 100ms so we can auto claim events pending for more than 50ms
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Auto claim pending events to different consumer
        Map.Entry<StreamEntryID, List<StreamEntry>> streamEntrys = jedis.xautoclaim("xpending-stream", "xpending-group",
                "xpending-consumer2", 50, new StreamEntryID(), new XAutoClaimParams().count(1));
        assertEquals(1, streamEntrys.getValue().size());
        assertEquals(pendingRange.get(0).getID(), streamEntrys.getValue().get(0).getID());
        assertEquals("v1", streamEntrys.getValue().get(0).getFields().get("f1"));
    }

    @Test
    public void xautoclaimBinary() {
        Map<String, String> map = new HashMap<>();
        map.put("f1", "v1");
        jedis.xadd("xpending-stream", XAddParams.xAddParams(), map);

        assertEquals("OK", jedis.xgroupCreate("xpending-stream", "xpending-group", null, false));

        // Read the event from Stream put it on pending
        jedis.xreadGroupBinary("xpending-group".getBytes(), "xpending-consumer".getBytes(), XReadGroupParams.xReadGroupParams().count(1).block(1),
                singletonMap("xpending-stream".getBytes(), StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY));

        // Get the pending event
        List<StreamPendingEntry> pendingRange = jedis.xpending("xpending-stream", "xpending-group",
                XPendingParams.xPendingParams().count(3).consumer("xpending-consumer"));
        // Sleep for 100ms so we can auto claim events pending for more than 50ms
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Auto claim pending events to different consumer
        List<Object> streamEntrys = jedis.xautoclaim(SafeEncoder.encode("xpending-stream"),
                SafeEncoder.encode("xpending-group"), SafeEncoder.encode("xpending-consumer2"),
                50, SafeEncoder.encode(new StreamEntryID().toString()), new XAutoClaimParams().count(1));
        Map.Entry<StreamEntryID, List<StreamEntry>> res = BuilderFactory.STREAM_AUTO_CLAIM_RESPONSE.build(streamEntrys);
        assertEquals(1, res.getValue().size());
        assertEquals(pendingRange.get(0).getID(), res.getValue().get(0).getID());
        assertEquals("v1", res.getValue().get(0).getFields().get("f1"));
    }

    @Test
    public void xautoclaimJustId() {
        Map<String, String> map = new HashMap<>();
        map.put("f1", "v1");
        jedis.xadd("xpending-stream", (StreamEntryID) null, map);

        assertEquals("OK", jedis.xgroupCreate("xpending-stream", "xpending-group", null, false));

        // Read the event from Stream put it on pending
        jedis.xreadGroupBinary("xpending-group".getBytes(), "xpending-consumer".getBytes(), XReadGroupParams.xReadGroupParams().count(1).block(1),
                singletonMap("xpending-stream".getBytes(), StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY));

        // Get the pending event
        List<StreamPendingEntry> pendingRange = jedis.xpending("xpending-stream", "xpending-group",
                XPendingParams.xPendingParams().count(3).consumer("xpending-consumer"));
        // Sleep for 100ms so we can auto claim events pending for more than 50ms
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Auto claim pending events to different consumer
        Map.Entry<StreamEntryID, List<StreamEntryID>> streamEntrys = jedis.xautoclaimJustId("xpending-stream", "xpending-group",
                "xpending-consumer2", 50, new StreamEntryID(), new XAutoClaimParams().count(1));
        assertEquals(1, streamEntrys.getValue().size());
        assertEquals(pendingRange.get(0).getID().getTime(), streamEntrys.getValue().get(0).getTime());
        assertEquals(pendingRange.get(0).getID().getSequence(), streamEntrys.getValue().get(0).getSequence());
    }

    @Test
    public void xautoclaimJustIdBinary() {
        Map<String, String> map = new HashMap<>();
        map.put("f1", "v1");
        jedis.xadd("xpending-stream", XAddParams.xAddParams(), map);

        assertEquals("OK", jedis.xgroupCreate("xpending-stream", "xpending-group", null, false));

        // Read the event from Stream put it on pending
        jedis.xreadGroupBinary("xpending-group".getBytes(), "xpending-consumer".getBytes(), XReadGroupParams.xReadGroupParams().count(1).block(1),
                singletonMap("xpending-stream".getBytes(), StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY));

        // Get the pending event
        List<StreamPendingEntry> pendingRange = jedis.xpending("xpending-stream", "xpending-group",
                XPendingParams.xPendingParams().count(3).consumer("xpending-consumer"));
        // Sleep for 100ms so we can auto claim events pending for more than 50ms
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Auto claim pending events to different consumer
        List<Object> streamEntrys = jedis.xautoclaimJustId(SafeEncoder.encode("xpending-stream"),
                SafeEncoder.encode("xpending-group"), SafeEncoder.encode("xpending-consumer2"),
                50, SafeEncoder.encode(new StreamEntryID().toString()), new XAutoClaimParams().count(1));
        Map.Entry<StreamEntryID, List<StreamEntryID>> res = BuilderFactory.STREAM_AUTO_CLAIM_JUSTID_RESPONSE.build(streamEntrys);
        assertEquals(1, res.getValue().size());
        assertEquals(pendingRange.get(0).getID().getTime(), res.getValue().get(0).getTime());
        assertEquals(pendingRange.get(0).getID().getSequence(), res.getValue().get(0).getSequence());
    }

    @Test
    public void xinfo() throws InterruptedException {

        final String STREAM_NAME = "xadd-stream1";
        final String F1 = "f1";
        final String V1 = "v1";
        final String V2 = "v2";
        final String G1 = "G1";
        final String G2 = "G2";
        final String MY_CONSUMER = "myConsumer";
        final String MY_CONSUMER2 = "myConsumer2";

        final RedisVersion redisVersion = RedisVersionUtil.getRedisVersion(jedis);

        Map<String, String> map1 = new HashMap<>();
        map1.put(F1, V1);
        StreamEntryID id1 = jedis.xadd(STREAM_NAME, (StreamEntryID) null, map1);
        map1.put(F1, V2);
        StreamEntryID id2 = jedis.xadd(STREAM_NAME, (StreamEntryID) null, map1);
        assertNotNull(id1);
        StreamInfo streamInfo = jedis.xinfoStream(STREAM_NAME);
        assertNotNull(id2);

        jedis.xgroupCreate(STREAM_NAME, G1, StreamEntryID.XGROUP_LAST_ENTRY, false);
        Map<byte[], StreamEntryID> streamQeury11 = singletonMap(
                STREAM_NAME.getBytes(), new StreamEntryID("0-0"));
        jedis.xreadGroupBinary(G1.getBytes(), MY_CONSUMER.getBytes(), XReadGroupParams.xReadGroupParams().count(1), streamQeury11);

        Thread.sleep(1);

        List<StreamGroupInfo> groupInfo = jedis.xinfoGroups(STREAM_NAME);
        List<StreamConsumersInfo> consumersInfo = jedis.xinfoConsumers(STREAM_NAME, G1);
        List<StreamConsumerInfo> consumerInfo = jedis.xinfoConsumers2(STREAM_NAME, G1);

        // Stream info test
        assertEquals(2L, streamInfo.getStreamInfo().get(StreamInfo.LENGTH));
        assertEquals(1L, streamInfo.getStreamInfo().get(StreamInfo.RADIX_TREE_KEYS));
        assertEquals(2L, streamInfo.getStreamInfo().get(StreamInfo.RADIX_TREE_NODES));
        assertEquals(0L, streamInfo.getStreamInfo().get(StreamInfo.GROUPS));
        assertEquals(V1, ((StreamEntry) streamInfo.getStreamInfo().get(StreamInfo.FIRST_ENTRY)).getFields().get(F1));
        assertEquals(V2, ((StreamEntry) streamInfo.getStreamInfo().get(StreamInfo.LAST_ENTRY)).getFields().get(F1));
        assertEquals(id2, streamInfo.getStreamInfo().get(StreamInfo.LAST_GENERATED_ID));

        // Using getters
        assertEquals(2, streamInfo.getLength());
        assertEquals(1, streamInfo.getRadixTreeKeys());
        assertEquals(2, streamInfo.getRadixTreeNodes());
        assertEquals(0, streamInfo.getGroups());
        assertEquals(V1, streamInfo.getFirstEntry().getFields().get(F1));
        assertEquals(V2, streamInfo.getLastEntry().getFields().get(F1));
        assertEquals(id2, streamInfo.getLastGeneratedId());

        // Group info test
        assertEquals(1, groupInfo.size());
        assertEquals(G1, groupInfo.get(0).getGroupInfo().get(StreamGroupInfo.NAME));
        assertEquals(1L, groupInfo.get(0).getGroupInfo().get(StreamGroupInfo.CONSUMERS));
        assertEquals(0L, groupInfo.get(0).getGroupInfo().get(StreamGroupInfo.PENDING));
        assertEquals(id2, groupInfo.get(0).getGroupInfo().get(StreamGroupInfo.LAST_DELIVERED));

        // Using getters
        assertEquals(1, groupInfo.size());
        assertEquals(G1, groupInfo.get(0).getName());
        assertEquals(1, groupInfo.get(0).getConsumers());
        assertEquals(0, groupInfo.get(0).getPending());
        assertEquals(id2, groupInfo.get(0).getLastDeliveredId());

        // Consumers info test
        assertEquals(MY_CONSUMER,
                consumersInfo.get(0).getConsumerInfo().get(StreamConsumersInfo.NAME));
        assertEquals(0L, consumersInfo.get(0).getConsumerInfo().get(StreamConsumersInfo.PENDING));
        assertTrue((Long) consumersInfo.get(0).getConsumerInfo().get(StreamConsumersInfo.IDLE) > 0);

        // Using getters
        assertEquals(MY_CONSUMER, consumersInfo.get(0).getName());
        assertEquals(0L, consumersInfo.get(0).getPending());
        assertThat(consumersInfo.get(0).getIdle(), Matchers.greaterThanOrEqualTo(0L));

        if ( redisVersion.isGreaterThanOrEqualTo(RedisVersion.V7_2_0)) {
            assertThat(consumersInfo.get(0).getInactive(), Matchers.any(Long.class));
        }

        // Consumer info test
        assertEquals(MY_CONSUMER,
                consumerInfo.get(0).getConsumerInfo().get(StreamConsumerInfo.NAME));
        assertEquals(0L, consumerInfo.get(0).getConsumerInfo().get(StreamConsumerInfo.PENDING));
        assertTrue((Long) consumerInfo.get(0).getConsumerInfo().get(StreamConsumerInfo.IDLE) > 0);

        // Using getters
        assertEquals(MY_CONSUMER, consumerInfo.get(0).getName());
        assertEquals(0L, consumerInfo.get(0).getPending());
        assertThat(consumerInfo.get(0).getIdle(), Matchers.greaterThanOrEqualTo(0L));
        if (redisVersion.isGreaterThanOrEqualTo(RedisVersion.V7_2_0)) {
            assertThat(consumerInfo.get(0).getInactive(), Matchers.any(Long.class));
        }

        // test with more groups and consumers
        jedis.xgroupCreate(STREAM_NAME, G2, StreamEntryID.XGROUP_LAST_ENTRY, false);
        jedis.xreadGroupBinary(G1.getBytes(), MY_CONSUMER2.getBytes(), XReadGroupParams.xReadGroupParams().count(1), streamQeury11);
        jedis.xreadGroupBinary(G2.getBytes(), MY_CONSUMER.getBytes(), XReadGroupParams.xReadGroupParams().count(1), streamQeury11);
        jedis.xreadGroupBinary(G2.getBytes(), MY_CONSUMER2.getBytes(), XReadGroupParams.xReadGroupParams().count(1), streamQeury11);

        List<StreamGroupInfo> manyGroupsInfo = jedis.xinfoGroups(STREAM_NAME);
        List<StreamConsumersInfo> manyConsumersInfo = jedis.xinfoConsumers(STREAM_NAME, G2);
        List<StreamConsumerInfo> manyConsumerInfo = jedis.xinfoConsumers2(STREAM_NAME, G2);

        assertEquals(2, manyGroupsInfo.size());
        assertEquals(2, manyConsumersInfo.size());
        assertEquals(2, manyConsumerInfo.size());

        StreamFullInfo streamInfoFull = jedis.xinfoStreamFull(STREAM_NAME);

        assertEquals(2, streamInfoFull.getEntries().size());
        assertEquals(2, streamInfoFull.getGroups().size());
        assertEquals(2, streamInfoFull.getLength());
        assertEquals(1, streamInfoFull.getRadixTreeKeys());
        assertEquals(2, streamInfoFull.getRadixTreeNodes());
        assertEquals(0, streamInfo.getGroups());
        assertEquals(G1, streamInfoFull.getGroups().get(0).getName());
        assertEquals(G2, streamInfoFull.getGroups().get(1).getName());
        assertEquals(V1, streamInfoFull.getEntries().get(0).getFields().get(F1));
        assertEquals(V2, streamInfoFull.getEntries().get(1).getFields().get(F1));
        assertEquals(id2, streamInfoFull.getLastGeneratedId());

        streamInfoFull = jedis.xinfoStreamFull(STREAM_NAME, 10);
        assertEquals(G1, streamInfoFull.getGroups().get(0).getName());
        assertEquals(G2, streamInfoFull.getGroups().get(1).getName());
        assertEquals(V1, streamInfoFull.getEntries().get(0).getFields().get(F1));
        assertEquals(V2, streamInfoFull.getEntries().get(1).getFields().get(F1));
        assertEquals(id2, streamInfoFull.getLastGeneratedId());

        // Not existing key - redis cli return error so we expect exception
        try {
            jedis.xinfoStream("random");
            fail("Command should fail");
        } catch (JedisException e) {
            assertEquals("ERR no such key", e.getMessage());
        }
    }

    @Test
    public void xinfoStreamFullWithPending() {

        Map<String, String> map = singletonMap("f1", "v1");
        StreamEntryID id1 = jedis.xadd("streamfull2", (StreamEntryID) null, map);
        StreamEntryID id2 = jedis.xadd("streamfull2", (StreamEntryID) null, map);
        jedis.xgroupCreate("streamfull2", "xreadGroup-group", null, false);

        Map<byte[], StreamEntryID> streamQeury1 = singletonMap("streamfull2".getBytes(), StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
        List<Map.Entry<byte[], List<StreamEntryBinary>>> range = jedis.xreadGroupBinary("xreadGroup-group".getBytes(), "xreadGroup-consumer".getBytes(),
                XReadGroupParams.xReadGroupParams().count(1), streamQeury1);
        assertEquals(1, range.size());
        assertEquals(1, range.get(0).getValue().size());

        StreamFullInfo full = jedis.xinfoStreamFull("streamfull2");
        assertEquals(1, full.getGroups().size());
        StreamGroupFullInfo group = full.getGroups().get(0);
        assertEquals("xreadGroup-group", group.getName());

        assertEquals(1, group.getPending().size());
        List<Object> groupPendingEntry = group.getPending().get(0);
        assertEquals(id1, groupPendingEntry.get(0));
        assertEquals("xreadGroup-consumer", groupPendingEntry.get(1));

        assertEquals(1, group.getConsumers().size());
        StreamConsumerFullInfo consumer = group.getConsumers().get(0);
        assertEquals("xreadGroup-consumer", consumer.getName());
        assertThat(consumer.getSeenTime(), Matchers.greaterThanOrEqualTo(0L));
        if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V7_2_0)) {
            assertThat(consumer.getActiveTime(), Matchers.greaterThanOrEqualTo(0L));
        }
        assertEquals(1, consumer.getPending().size());
        List<Object> consumerPendingEntry = consumer.getPending().get(0);
        assertEquals(id1, consumerPendingEntry.get(0));
    }

    private Map<String, String> convertToMap(Map<byte[], byte[]> map) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<byte[], byte[]> entry : map.entrySet()) {
            result.put(SafeEncoder.encode(entry.getKey()), SafeEncoder.encode(entry.getValue()));
        }
        return result;
    }

    private byte[] findKey(Set<byte[]> map, String key) {
        for (byte[] entry : map) {
            if (key.equals(SafeEncoder.encode(entry))) {
                return entry;
            }
        }
        return null;
    }
}
