package redis.clients.jedis.commands.unified.pipeline;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import io.redis.test.annotations.SinceRedisVersion;
import io.redis.test.utils.RedisVersion;
import redis.clients.jedis.util.RedisVersionUtil;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.Response;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.XAddParams;
import redis.clients.jedis.params.XAutoClaimParams;
import redis.clients.jedis.params.XClaimParams;
import redis.clients.jedis.params.XPendingParams;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.params.XTrimParams;
import redis.clients.jedis.resps.StreamConsumerFullInfo;
import redis.clients.jedis.resps.StreamConsumerInfo;
import redis.clients.jedis.resps.StreamConsumersInfo;
import redis.clients.jedis.resps.StreamEntry;
import redis.clients.jedis.resps.StreamFullInfo;
import redis.clients.jedis.resps.StreamGroupFullInfo;
import redis.clients.jedis.resps.StreamGroupInfo;
import redis.clients.jedis.resps.StreamInfo;
import redis.clients.jedis.resps.StreamPendingEntry;
import redis.clients.jedis.util.SafeEncoder;

@RunWith(Parameterized.class)
public class StreamsPipelineCommandsTest extends PipelineCommandsTestBase {

  public StreamsPipelineCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void xaddWrongNumberOfArguments() {
    Map<String, String> map1 = new HashMap<>();
    pipe.xadd("stream1", (StreamEntryID) null, map1);

    assertThat(pipe.syncAndReturnAll(),
        contains(
            both(instanceOf(JedisDataException.class)).and(hasToString(containsString("wrong number of arguments")))
        ));
  }

  @Test
  public void xadd() {
    Map<String, String> map1 = new HashMap<>();
    map1.put("f1", "v1");
    pipe.xadd("xadd-stream1", (StreamEntryID) null, map1);

    Map<String, String> map2 = new HashMap<>();
    map2.put("f1", "v1");
    map2.put("f2", "v2");
    pipe.xadd("xadd-stream1", (StreamEntryID) null, map2);

    List<?> results = pipe.syncAndReturnAll();

    assertThat(results, contains(
        instanceOf(StreamEntryID.class),
        instanceOf(StreamEntryID.class)
    ));

    assertThat((StreamEntryID) results.get(1),
        greaterThan((StreamEntryID) results.get(0)));
  }

  @Test
  public void xaddMaxLen() {
    Map<String, String> map4 = new HashMap<>();
    map4.put("f2", "v2");
    map4.put("f3", "v3");
    StreamEntryID idIn = new StreamEntryID(1000, 1L);
    pipe.xadd("xadd-stream2", idIn, map4);

    Map<String, String> map5 = new HashMap<>();
    map5.put("f4", "v4");
    map5.put("f5", "v5");
    pipe.xadd("xadd-stream2", (StreamEntryID) null, map5);

    pipe.xlen("xadd-stream2");

    Map<String, String> map6 = new HashMap<>();
    map6.put("f4", "v4");
    map6.put("f5", "v5");
    pipe.xadd("xadd-stream2", map6, XAddParams.xAddParams().maxLen(2));

    pipe.xlen("xadd-stream2");

    List<?> results = pipe.syncAndReturnAll();

    assertThat(results, contains(
        equalTo(idIn),
        instanceOf(StreamEntryID.class),
        equalTo(2L),
        instanceOf(StreamEntryID.class),
        equalTo(2L)
    ));

    assertThat((StreamEntryID) results.get(1),
        greaterThan((StreamEntryID) results.get(0)));

    assertThat((StreamEntryID) results.get(3),
        greaterThan((StreamEntryID) results.get(1)));
  }

  @Test
  public void xaddWithParamsWrongNumberOfArguments() {
    pipe.xadd("stream1", new HashMap<>(), XAddParams.xAddParams());
    pipe.xadd("stream1", XAddParams.xAddParams(), new HashMap<>());

    assertThat(pipe.syncAndReturnAll(),
        contains(
            both(instanceOf(JedisDataException.class)).and(hasToString(containsString("wrong number of arguments"))),
            both(instanceOf(JedisDataException.class)).and(hasToString(containsString("wrong number of arguments")))
        ));
  }

  @Test
  public void xaddWithParams() {
    pipe.xadd("xadd-stream1", (StreamEntryID) null, singletonMap("f1", "v1"));

    Map<String, String> map2 = new HashMap<>();
    map2.put("f1", "v1");
    map2.put("f2", "v2");
    pipe.xadd("xadd-stream1", map2, XAddParams.xAddParams());

    List<?> results = pipe.syncAndReturnAll();

    assertThat(results, contains(
        instanceOf(StreamEntryID.class),
        instanceOf(StreamEntryID.class)
    ));

    assertThat((StreamEntryID) results.get(1),
        greaterThan((StreamEntryID) results.get(0)));
  }

  @Test
  public void xaddWithParamsTrim() {
    Map<String, String> map3 = new HashMap<>();
    map3.put("f2", "v2");
    map3.put("f3", "v3");
    StreamEntryID idIn = new StreamEntryID(1000, 1L);
    pipe.xadd("xadd-stream2", XAddParams.xAddParams().id(idIn), map3);

    Map<String, String> map4 = new HashMap<>();
    map4.put("f2", "v2");
    map4.put("f3", "v3");
    StreamEntryID idIn2 = new StreamEntryID(2000, 1L);
    pipe.xadd("xadd-stream2", map4, XAddParams.xAddParams().id(idIn2));

    Map<String, String> map5 = new HashMap<>();
    map5.put("f4", "v4");
    map5.put("f5", "v5");
    pipe.xadd("xadd-stream2", XAddParams.xAddParams(), map5);

    pipe.xlen("xadd-stream2");

    Map<String, String> map6 = new HashMap<>();
    map6.put("f4", "v4");
    map6.put("f5", "v5");
    pipe.xadd("xadd-stream2", map6, XAddParams.xAddParams().maxLen(3).exactTrimming());

    pipe.xlen("xadd-stream2");

    List<?> results = pipe.syncAndReturnAll();

    assertThat(results, contains(
        equalTo(idIn),
        equalTo(idIn2),
        instanceOf(StreamEntryID.class),
        equalTo(3L),
        instanceOf(StreamEntryID.class),
        equalTo(3L)
    ));

    assertThat((StreamEntryID) results.get(2),
        greaterThan((StreamEntryID) results.get(1)));

    assertThat((StreamEntryID) results.get(4),
        greaterThan((StreamEntryID) results.get(2)));
  }

  @Test
  public void xaddWithParamsNoMkStream() {
    pipe.xadd("xadd-stream3", XAddParams.xAddParams().noMkStream().maxLen(3).exactTrimming(), singletonMap("f1", "v1"));

    assertThat(pipe.syncAndReturnAll(),
        contains(
            nullValue()
        ));

    assertFalse(jedis.exists("xadd-stream3"));
  }

  @Test
  public void xaddWithParamsMinId() {
    Map<String, String> map6 = new HashMap<>();
    map6.put("f4", "v4");
    map6.put("f5", "v5");

    StreamEntryID id = new StreamEntryID(2);
    pipe.xadd("xadd-stream3", map6, XAddParams.xAddParams().minId("2").id(id));

    pipe.xlen("xadd-stream3");

    StreamEntryID id1 = new StreamEntryID(3);
    pipe.xadd("xadd-stream3", XAddParams.xAddParams().minId("4").id(id1), map6);

    pipe.xlen("xadd-stream3");

    List<?> results = pipe.syncAndReturnAll();

    assertThat(results, contains(
        equalTo(id),
        equalTo(1L),
        equalTo(id1),
        equalTo(0L)
    ));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0", message = "Added support for XADD ID auto sequence is introduced in 7.0.0")
  public void xaddParamsId() {
    String key = "kk";
    Map<String, String> map = singletonMap("ff", "vv");

    pipe.xadd(key, XAddParams.xAddParams().id(new StreamEntryID(0, 1)), map);
    pipe.xadd(key, XAddParams.xAddParams().id(2, 3), map);
    pipe.xadd(key, XAddParams.xAddParams().id(4), map);
    pipe.xadd(key, XAddParams.xAddParams().id("5-6"), map);
    pipe.xadd(key, XAddParams.xAddParams().id("7-8".getBytes()), map);
    pipe.xadd(key, XAddParams.xAddParams(), map);

    List<Object> results = pipe.syncAndReturnAll();

    assertThat(results, contains(
        equalTo(new StreamEntryID(0, 1)),
        equalTo(new StreamEntryID(2, 3)),
        equalTo(new StreamEntryID(4, 0)),
        equalTo(new StreamEntryID(5, 6)),
        equalTo(new StreamEntryID(7, 8)),
        instanceOf(StreamEntryID.class)
    ));

    assertThat((StreamEntryID) results.get(5),
        greaterThan((StreamEntryID) results.get(4)));
  }

  @Test
  public void xdel() {
    Map<String, String> map1 = new HashMap<>();
    map1.put("f1", "v1");

    pipe.xadd("xdel-stream", (StreamEntryID) null, map1);
    pipe.xadd("xdel-stream", (StreamEntryID) null, map1);

    List<Object> results = pipe.syncAndReturnAll();

    assertThat(results, contains(
        instanceOf(StreamEntryID.class),
        instanceOf(StreamEntryID.class)
    ));

    StreamEntryID id1 = (StreamEntryID) results.get(1);

    pipe.xlen("xdel-stream");
    pipe.xdel("xdel-stream", id1);
    pipe.xlen("xdel-stream");

    assertThat(pipe.syncAndReturnAll(), contains(
        2L,
        1L,
        1L
    ));
  }

  @Test
  public void xlen() {
    pipe.xlen("xlen-stream");

    Map<String, String> map = new HashMap<>();
    map.put("f1", "v1");
    pipe.xadd("xlen-stream", (StreamEntryID) null, map);

    pipe.xlen("xlen-stream");

    pipe.xadd("xlen-stream", (StreamEntryID) null, map);

    pipe.xlen("xlen-stream");

    assertThat(pipe.syncAndReturnAll(), contains(
        equalTo(0L),
        instanceOf(StreamEntryID.class),
        equalTo(1L),
        instanceOf(StreamEntryID.class),
        equalTo(2L)
    ));
  }

  @Test
  public void xrange() {
    Response<List<StreamEntry>> range = pipe.xrange("xrange-stream", null, (StreamEntryID) null, Integer.MAX_VALUE);

    Map<String, String> map1 = singletonMap("f1", "v1");
    Map<String, String> map2 = singletonMap("f2", "v2");
    Map<String, String> map3 = singletonMap("f3", "v3");

    Response<StreamEntryID> id1Response = pipe.xadd("xrange-stream", (StreamEntryID) null, map1);

    Response<StreamEntryID> id2Response = pipe.xadd("xrange-stream", (StreamEntryID) null, map2);

    pipe.sync();

    assertThat(range.get(), empty());
    assertThat(id1Response.get(), notNullValue());
    assertThat(id2Response.get(), notNullValue());

    StreamEntryID id1 = id1Response.get();
    StreamEntryID id2 = id2Response.get();

    Response<List<StreamEntry>> range2 = pipe.xrange("xrange-stream", null, (StreamEntryID) null, 3);
    Response<List<StreamEntry>> range3 = pipe.xrange("xrange-stream", id1, null, 2);
    Response<List<StreamEntry>> range4 = pipe.xrange("xrange-stream", id1, id2, 2);
    Response<List<StreamEntry>> range5 = pipe.xrange("xrange-stream", id1, id2, 1);
    Response<List<StreamEntry>> range6 = pipe.xrange("xrange-stream", id2, null, 4);

    Response<StreamEntryID> id3Response = pipe.xadd("xrange-stream", (StreamEntryID) null, map3);

    pipe.sync();

    assertThat(range2.get().stream().map(StreamEntry::getID).collect(Collectors.toList()), contains(id1, id2));
    assertThat(range3.get().stream().map(StreamEntry::getID).collect(Collectors.toList()), contains(id1, id2));
    assertThat(range4.get().stream().map(StreamEntry::getID).collect(Collectors.toList()), contains(id1, id2));
    assertThat(range5.get().stream().map(StreamEntry::getID).collect(Collectors.toList()), contains(id1));
    assertThat(range6.get().stream().map(StreamEntry::getID).collect(Collectors.toList()), contains(id2));

    assertThat(id3Response.get(), notNullValue());

    StreamEntryID id3 = id3Response.get();

    Response<List<StreamEntry>> range7 = pipe.xrange("xrange-stream", id3, id3, 4);
    Response<List<StreamEntry>> range8 = pipe.xrange("xrange-stream", null, (StreamEntryID) null);
    Response<List<StreamEntry>> range9 = pipe.xrange("xrange-stream", StreamEntryID.MINIMUM_ID, StreamEntryID.MAXIMUM_ID);

    pipe.sync();

    assertThat(range7.get().stream().map(StreamEntry::getID).collect(Collectors.toList()), contains(id3));
    assertThat(range8.get().stream().map(StreamEntry::getID).collect(Collectors.toList()), contains(id1, id2, id3));
    assertThat(range9.get().stream().map(StreamEntry::getID).collect(Collectors.toList()), contains(id1, id2, id3));
  }

  @Test
  public void xrangeExclusive() {
    StreamEntryID id1 = jedis.xadd("xrange-stream", (StreamEntryID) null, singletonMap("f1", "v1"));
    StreamEntryID id2 = jedis.xadd("xrange-stream", (StreamEntryID) null, singletonMap("f2", "v2"));

    Response<List<StreamEntry>> range1 = pipe.xrange("xrange-stream", id1.toString(), "+", 2);
    Response<List<StreamEntry>> range2 = pipe.xrange("xrange-stream", "(" + id1, "+", 2);

    pipe.sync();

    assertThat(range1.get().stream().map(StreamEntry::getID).collect(Collectors.toList()), contains(id1, id2));
    assertThat(range2.get().stream().map(StreamEntry::getID).collect(Collectors.toList()), contains(id2));
  }

  @Test
  public void xreadWithParams() {

    final String stream1 = "xread-stream1";
    final String stream2 = "xread-stream2";

    Map<String, StreamEntryID> streamQuery1 = singletonMap(stream1, new StreamEntryID());

    // Before creating Stream
    pipe.xread(XReadParams.xReadParams().block(1), streamQuery1);
    pipe.xread(XReadParams.xReadParams(), streamQuery1);

    assertThat(pipe.syncAndReturnAll(), contains(
        nullValue(),
        nullValue()
    ));

    Map<String, String> map1 = singletonMap("f1", "v1");
    StreamEntryID id1 = jedis.xadd(stream1, (StreamEntryID) null, map1);

    Map<String, String> map2 = singletonMap("f2", "v2");
    StreamEntryID id2 = jedis.xadd(stream2, (StreamEntryID) null, map2);

    // Read only a single Stream
    Response<List<Entry<String, List<StreamEntry>>>> streams1 =
        pipe.xread(XReadParams.xReadParams().count(1).block(1), streamQuery1);

    Response<List<Entry<String, List<StreamEntry>>>> streams2 =
        pipe.xread(XReadParams.xReadParams().block(1), singletonMap(stream1, id1));

    Response<List<Entry<String, List<StreamEntry>>>> streams3 =
        pipe.xread(XReadParams.xReadParams(), singletonMap(stream1, id1));

    pipe.sync();

    assertThat(streams1.get().stream().map(Entry::getKey).collect(Collectors.toList()),
        contains(stream1));

    assertThat(streams1.get().stream().map(Entry::getValue).flatMap(List::stream)
        .map(StreamEntry::getID).collect(Collectors.toList()), contains(id1));

    assertThat(streams1.get().stream().map(Entry::getValue).flatMap(List::stream)
        .map(StreamEntry::getFields).collect(Collectors.toList()), contains(map1));

    assertThat(streams2.get(), nullValue());

    assertThat(streams3.get(), nullValue());

    // Read from two Streams
    Map<String, StreamEntryID> streamQuery2 = new LinkedHashMap<>();
    streamQuery2.put(stream1, new StreamEntryID());
    streamQuery2.put(stream2, new StreamEntryID());

    Response<List<Entry<String, List<StreamEntry>>>> streams4 =
        pipe.xread(XReadParams.xReadParams().count(2).block(1), streamQuery2);

    pipe.sync();

    assertThat(streams4.get().stream().map(Entry::getKey).collect(Collectors.toList()),
        contains(stream1, stream2));

    assertThat(streams4.get().stream().map(Entry::getValue).flatMap(List::stream)
        .map(StreamEntry::getID).collect(Collectors.toList()), contains(id1, id2));

    assertThat(streams4.get().stream().map(Entry::getValue).flatMap(List::stream)
        .map(StreamEntry::getFields).collect(Collectors.toList()), contains(map1, map2));
  }

  @Test
  public void xreadBlockZero() throws InterruptedException {
    final AtomicReference<List<Entry<String, List<StreamEntry>>>> readRef = new AtomicReference<>();
    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        long startTime = System.currentTimeMillis();
        Pipeline blockPipe = jedis.pipelined();
        Map<String, StreamEntryID> streamQuery = singletonMap("block0-stream", new StreamEntryID());
        Response<List<Entry<String, List<StreamEntry>>>> read =
            blockPipe.xread(XReadParams.xReadParams().block(0), streamQuery);
        blockPipe.sync();
        long endTime = System.currentTimeMillis();
        assertTrue(endTime - startTime > 500);
        assertNotNull(read);
        readRef.set(read.get());
      }
    }, "xread-block-0-thread");
    t.start();
    Thread.sleep(1000);
    StreamEntryID addedId = jedis.xadd("block0-stream", (StreamEntryID) null, singletonMap("foo", "bar"));
    t.join();

    assertThat(readRef.get().stream().map(Entry::getKey).collect(Collectors.toList()),
        contains("block0-stream"));

    assertThat(readRef.get().stream().map(Entry::getValue).flatMap(List::stream)
        .map(StreamEntry::getID).collect(Collectors.toList()), contains(addedId));
  }

  @Test
  public void xtrim() {
    Map<String, String> map1 = new HashMap<String, String>();
    map1.put("f1", "v1");

    for (int i = 1; i <= 5; i++) {
      pipe.xadd("xtrim-stream", (StreamEntryID) null, map1);
    }

    pipe.xlen("xtrim-stream");

    pipe.xtrim("xtrim-stream", 3, false);

    pipe.xlen("xtrim-stream");

    assertThat(pipe.syncAndReturnAll(), contains(
        instanceOf(StreamEntryID.class),
        instanceOf(StreamEntryID.class),
        instanceOf(StreamEntryID.class),
        instanceOf(StreamEntryID.class),
        instanceOf(StreamEntryID.class),
        equalTo(5L),
        equalTo(2L),
        equalTo(3L)
    ));
  }

  @Test
  public void xtrimWithParams() {
    Map<String, String> map1 = new HashMap<>();
    map1.put("f1", "v1");
    for (int i = 1; i <= 5; i++) {
      pipe.xadd("xtrim-stream", new StreamEntryID("0-" + i), map1);
    }

    pipe.xlen("xtrim-stream");

    pipe.xtrim("xtrim-stream", XTrimParams.xTrimParams().maxLen(3).exactTrimming());

    pipe.xlen("xtrim-stream");

    // minId
    pipe.xtrim("xtrim-stream", XTrimParams.xTrimParams().minId("0-4").exactTrimming());

    pipe.xlen("xtrim-stream");

    assertThat(pipe.syncAndReturnAll(), contains(
        instanceOf(StreamEntryID.class),
        instanceOf(StreamEntryID.class),
        instanceOf(StreamEntryID.class),
        instanceOf(StreamEntryID.class),
        instanceOf(StreamEntryID.class),
        equalTo(5L),
        equalTo(2L),
        equalTo(3L),
        equalTo(1L),
        equalTo(2L)
    ));
  }

  @Test
  public void xrevrange() {
    Response<List<StreamEntry>> range = pipe.xrevrange("xrevrange-stream", null, (StreamEntryID) null, Integer.MAX_VALUE);

    Map<String, String> map1 = singletonMap("f1", "v1");
    Response<StreamEntryID> id1Response = pipe.xadd("xrevrange-stream", (StreamEntryID) null, map1);
    Map<String, String> map2 = singletonMap("f2", "v2");
    Response<StreamEntryID> id2Response = pipe.xadd("xrevrange-stream", (StreamEntryID) null, map2);

    pipe.sync();

    assertThat(range.get(), empty());
    assertThat(id1Response.get(), notNullValue());
    assertThat(id2Response.get(), notNullValue());

    StreamEntryID id1 = id1Response.get();
    StreamEntryID id2 = id2Response.get();

    Response<List<StreamEntry>> range2 = pipe.xrevrange("xrevrange-stream", null, (StreamEntryID) null, 3);
    Response<List<StreamEntry>> range3 = pipe.xrevrange("xrevrange-stream", null, id1, 2);
    Response<List<StreamEntry>> range4 = pipe.xrevrange("xrevrange-stream", id2, id1, 2);
    Response<List<StreamEntry>> range5 = pipe.xrevrange("xrevrange-stream", id2, id1, 1);
    Response<List<StreamEntry>> range6 = pipe.xrevrange("xrevrange-stream", null, id2, 4);

    Map<String, String> map3 = singletonMap("f3", "v3");
    Response<StreamEntryID> id3Response = pipe.xadd("xrevrange-stream", (StreamEntryID) null, map3);

    pipe.sync();

    assertThat(range2.get().stream().map(StreamEntry::getID).collect(Collectors.toList()), contains(id2, id1));
    assertThat(range3.get().stream().map(StreamEntry::getID).collect(Collectors.toList()), contains(id2, id1));
    assertThat(range4.get().stream().map(StreamEntry::getID).collect(Collectors.toList()), contains(id2, id1));
    assertThat(range5.get().stream().map(StreamEntry::getID).collect(Collectors.toList()), contains(id2));
    assertThat(range6.get().stream().map(StreamEntry::getID).collect(Collectors.toList()), contains(id2));

    assertThat(id3Response.get(), notNullValue());

    StreamEntryID id3 = id3Response.get();

    Response<List<StreamEntry>> range7 = pipe.xrevrange("xrevrange-stream", id3, id3, 4);
    Response<List<StreamEntry>> range8 = pipe.xrevrange("xrevrange-stream", null, (StreamEntryID) null);
    Response<List<StreamEntry>> range9 = pipe.xrevrange("xrevrange-stream", StreamEntryID.MAXIMUM_ID, StreamEntryID.MINIMUM_ID);

    pipe.sync();

    assertThat(range7.get().stream().map(StreamEntry::getID).collect(Collectors.toList()), contains(id3));
    assertThat(range8.get().stream().map(StreamEntry::getID).collect(Collectors.toList()), contains(id3, id2, id1));
    assertThat(range9.get().stream().map(StreamEntry::getID).collect(Collectors.toList()), contains(id3, id2, id1));
  }

  @Test
  public void xrevrangeExclusive() {
    StreamEntryID id1 = jedis.xadd("xrange-stream", (StreamEntryID) null, singletonMap("f1", "v1"));
    StreamEntryID id2 = jedis.xadd("xrange-stream", (StreamEntryID) null, singletonMap("f2", "v2"));

    Response<List<StreamEntry>> range1 = pipe.xrevrange("xrange-stream", "+", id1.toString(), 2);
    Response<List<StreamEntry>> range2 = pipe.xrevrange("xrange-stream", "+", "(" + id1, 2);

    pipe.sync();

    assertThat(range1.get().stream().map(StreamEntry::getID).collect(Collectors.toList()), contains(id2, id1));
    assertThat(range2.get().stream().map(StreamEntry::getID).collect(Collectors.toList()), contains(id2));
  }

  @Test
  public void xgroup() {
    StreamEntryID id1 = jedis.xadd("xgroup-stream", (StreamEntryID) null, singletonMap("f1", "v1"));

    pipe.xgroupCreate("xgroup-stream", "consumer-group-name", null, false);
    pipe.xgroupSetID("xgroup-stream", "consumer-group-name", id1);
    pipe.xgroupCreate("xgroup-stream", "consumer-group-name1", StreamEntryID.XGROUP_LAST_ENTRY, false);

    pipe.xgroupDestroy("xgroup-stream", "consumer-group-name");
    pipe.xgroupDelConsumer("xgroup-stream", "consumer-group-name1", "myconsumer1");
    pipe.xgroupCreateConsumer("xgroup-stream", "consumer-group-name1", "myconsumer2");
    pipe.xgroupDelConsumer("xgroup-stream", "consumer-group-name1", "myconsumer2");

    assertThat(pipe.syncAndReturnAll(), contains(
        "OK",
        "OK",
        "OK",
        1L,
        0L,
        true,
        0L
    ));
  }

  @Test
  public void xreadGroupWithParams() {
    // Simple xreadGroup with NOACK
    Map<String, String> map1 = singletonMap("f1", "v1");
    StreamEntryID id1 = jedis.xadd("xreadGroup-stream1", (StreamEntryID) null, map1);

    jedis.xgroupCreate("xreadGroup-stream1", "xreadGroup-group", null, false);

    Map<String, StreamEntryID> streamQuery1 = singletonMap("xreadGroup-stream1", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);

    Response<List<Entry<String, List<StreamEntry>>>> streams1 =
        pipe.xreadGroup("xreadGroup-group", "xreadGroup-consumer",
            XReadGroupParams.xReadGroupParams().count(1).noAck(), streamQuery1);

    pipe.sync();

    assertThat(streams1.get().stream().map(Entry::getKey).collect(Collectors.toList()),
        contains("xreadGroup-stream1"));

    assertThat(streams1.get().stream().map(Entry::getValue).flatMap(List::stream)
        .map(StreamEntry::getID).collect(Collectors.toList()), contains(id1));

    assertThat(streams1.get().stream().map(Entry::getValue).flatMap(List::stream)
        .map(StreamEntry::getFields).collect(Collectors.toList()), contains(map1));

    Map<String, String> map2 = singletonMap("f2", "v2");
    StreamEntryID id2 = jedis.xadd("xreadGroup-stream1", (StreamEntryID) null, map2);

    Map<String, String> map3 = singletonMap("f3", "v3");
    StreamEntryID id3 = jedis.xadd("xreadGroup-stream2", (StreamEntryID) null, map3);

    jedis.xgroupCreate("xreadGroup-stream2", "xreadGroup-group", null, false);

    // Read only a single Stream
    Response<List<Entry<String, List<StreamEntry>>>> streams2 = pipe.xreadGroup("xreadGroup-group", "xreadGroup-consumer",
        XReadGroupParams.xReadGroupParams().count(1).block(1).noAck(), streamQuery1);

    // Read from two Streams
    Map<String, StreamEntryID> streamQuery2 = new LinkedHashMap<>();
    streamQuery2.put("xreadGroup-stream1", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    streamQuery2.put("xreadGroup-stream2", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);

    Response<List<Entry<String, List<StreamEntry>>>> streams3 = pipe.xreadGroup("xreadGroup-group", "xreadGroup-consumer",
        XReadGroupParams.xReadGroupParams().count(1).noAck(), streamQuery2);

    pipe.sync();

    assertThat(streams2.get().stream().map(Entry::getKey).collect(Collectors.toList()),
        contains("xreadGroup-stream1"));

    assertThat(streams2.get().stream().map(Entry::getValue).flatMap(List::stream)
        .map(StreamEntry::getID).collect(Collectors.toList()), contains(id2));

    assertThat(streams2.get().stream().map(Entry::getValue).flatMap(List::stream)
        .map(StreamEntry::getFields).collect(Collectors.toList()), contains(map2));

    assertThat(streams3.get().stream().map(Entry::getKey).collect(Collectors.toList()),
        contains("xreadGroup-stream2"));

    assertThat(streams3.get().stream().map(Entry::getValue).flatMap(List::stream)
        .map(StreamEntry::getID).collect(Collectors.toList()), contains(id3));

    assertThat(streams3.get().stream().map(Entry::getValue).flatMap(List::stream)
        .map(StreamEntry::getFields).collect(Collectors.toList()), contains(map3));

    // Read only fresh messages
    Map<String, String> map4 = singletonMap("f4", "v4");
    StreamEntryID id4 = jedis.xadd("xreadGroup-stream1", (StreamEntryID) null, map4);

    Map<String, StreamEntryID> streamQueryFresh = singletonMap("xreadGroup-stream1", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    Response<List<Entry<String, List<StreamEntry>>>> streams4 = pipe.xreadGroup("xreadGroup-group", "xreadGroup-consumer",
        XReadGroupParams.xReadGroupParams().count(4).block(100).noAck(), streamQueryFresh);

    pipe.sync();

    assertThat(streams4.get().stream().map(Entry::getKey).collect(Collectors.toList()),
        contains("xreadGroup-stream1"));

    assertThat(streams4.get().stream().map(Entry::getValue).flatMap(List::stream)
        .map(StreamEntry::getID).collect(Collectors.toList()), contains(id4));

    assertThat(streams4.get().stream().map(Entry::getValue).flatMap(List::stream)
        .map(StreamEntry::getFields).collect(Collectors.toList()), contains(map4));
  }

  @Test
  public void xreadGroupWithParamsWhenPendingMessageIsDiscarded() {
    // Add two message to stream
    Map<String, String> map1 = singletonMap("f1", "v1");

    XAddParams xAddParams = XAddParams.xAddParams().id(StreamEntryID.NEW_ENTRY).maxLen(2);
    StreamEntryID firstMessageEntryId = jedis.xadd("xreadGroup-discard-stream1", xAddParams, map1);

    jedis.xadd("xreadGroup-discard-stream1", xAddParams, singletonMap("f2", "v2"));

    pipe.xgroupCreate("xreadGroup-discard-stream1", "xreadGroup-group", null, false);

    Map<String, StreamEntryID> streamQuery1 = singletonMap("xreadGroup-discard-stream1", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);

    Response<List<Entry<String, List<StreamEntry>>>> streams1 =
        pipe.xreadGroup("xreadGroup-group", "xreadGroup-consumer",
            XReadGroupParams.xReadGroupParams().count(1), streamQuery1);

    pipe.sync();

    assertThat(streams1.get().stream().map(Entry::getKey).collect(Collectors.toList()),
        contains("xreadGroup-discard-stream1"));

    assertThat(streams1.get().stream().map(Entry::getValue).flatMap(List::stream)
        .map(StreamEntry::getID).collect(Collectors.toList()), contains(firstMessageEntryId));

    assertThat(streams1.get().stream().map(Entry::getValue).flatMap(List::stream)
        .map(StreamEntry::getFields).collect(Collectors.toList()), contains(map1));

    // Add third message, the fields of pending message1 will be discarded by redis-server
    jedis.xadd("xreadGroup-discard-stream1", xAddParams, singletonMap("f3", "v3"));

    Map<String, StreamEntryID> streamQueryPending = singletonMap("xreadGroup-discard-stream1", new StreamEntryID());

    Response<List<Entry<String, List<StreamEntry>>>> pendingMessages =
        pipe.xreadGroup("xreadGroup-group", "xreadGroup-consumer",
            XReadGroupParams.xReadGroupParams().count(1).noAck(), streamQueryPending);

    pipe.sync();

    assertThat(pendingMessages.get().stream().map(Entry::getKey).collect(Collectors.toList()),
        contains("xreadGroup-discard-stream1"));

    assertThat(pendingMessages.get().stream().map(Entry::getValue).flatMap(List::stream)
        .map(StreamEntry::getID).collect(Collectors.toList()), contains(firstMessageEntryId));

    assertThat(pendingMessages.get().stream().map(Entry::getValue).flatMap(List::stream)
        .map(StreamEntry::getFields).collect(Collectors.toList()), contains(nullValue()));
  }

  @Test
  public void xack() {
    pipe.xadd("xack-stream", (StreamEntryID) null, singletonMap("f1", "v1"));

    pipe.xgroupCreate("xack-stream", "xack-group", null, false);

    Map<String, StreamEntryID> streamQuery1 = singletonMap("xack-stream", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);

    // Empty Stream
    Response<List<Entry<String, List<StreamEntry>>>> streams1 =
        pipe.xreadGroup("xack-group", "xack-consumer",
            XReadGroupParams.xReadGroupParams().count(1).block(1), streamQuery1);

    pipe.sync();

    List<StreamEntryID> ids = streams1.get().stream().map(Entry::getValue).flatMap(List::stream)
        .map(StreamEntry::getID).collect(Collectors.toList());
    assertThat(ids, hasSize(1));

    Response<Long> xackResponse = pipe.xack("xack-stream", "xack-group", ids.get(0));

    pipe.sync();

    assertThat(xackResponse.get(), equalTo(1L));
  }

  @Test
  public void xpendingWithParams() {
    Map<String, String> map = singletonMap("f1", "v1");

    StreamEntryID id1 = jedis.xadd("xpending-stream", (StreamEntryID) null, map);

    assertEquals("OK", jedis.xgroupCreate("xpending-stream", "xpending-group", null, false));

    Map<String, StreamEntryID> streamQeury1 = singletonMap("xpending-stream", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);

    // Read the event from Stream put it on pending
    Response<List<Entry<String, List<StreamEntry>>>> range = pipe.xreadGroup("xpending-group",
        "xpending-consumer", XReadGroupParams.xReadGroupParams().count(1).block(1), streamQeury1);

    // Get the pending event
    Response<List<StreamPendingEntry>> pending1 =
        pipe.xpending("xpending-stream", "xpending-group",
            new XPendingParams().count(3).consumer("xpending-consumer"));

    // Without consumer
    Response<List<StreamPendingEntry>> pending2 =
        pipe.xpending("xpending-stream", "xpending-group",
            new XPendingParams().count(3));

    // with idle
    Response<List<StreamPendingEntry>> pending3 =
        pipe.xpending("xpending-stream", "xpending-group",
            new XPendingParams().idle(Duration.ofMinutes(1).toMillis()).count(3));

    pipe.sync();

    assertThat(pending1.get().stream().map(StreamPendingEntry::getConsumerName).collect(Collectors.toList()),
        contains("xpending-consumer"));

    assertThat(pending1.get().stream().map(StreamPendingEntry::getID).collect(Collectors.toList()),
        contains(id1));

    assertThat(pending1.get().stream().map(StreamPendingEntry::getDeliveredTimes).collect(Collectors.toList()),
        contains(1L));

    assertThat(pending2.get().stream().map(StreamPendingEntry::getConsumerName).collect(Collectors.toList()),
        contains("xpending-consumer"));

    assertThat(pending2.get().stream().map(StreamPendingEntry::getID).collect(Collectors.toList()),
        contains(id1));

    assertThat(pending2.get().stream().map(StreamPendingEntry::getDeliveredTimes).collect(Collectors.toList()),
        contains(1L));

    assertThat(pending3.get(), empty());
  }

  @Test
  public void xpendingRange() {
    StreamEntryID id1 = jedis.xadd("xpending-stream", (StreamEntryID) null, singletonMap("f1", "v1"));
    StreamEntryID id2 = jedis.xadd("xpending-stream", (StreamEntryID) null, singletonMap("f2", "v2"));

    pipe.xgroupCreate("xpending-stream", "xpending-group", null, false);

    // read 1 message from the group with each consumer
    Map<String, StreamEntryID> streamQeury = singletonMap("xpending-stream", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    pipe.xreadGroup("xpending-group", "consumer1", XReadGroupParams.xReadGroupParams().count(1), streamQeury);
    pipe.xreadGroup("xpending-group", "consumer2", XReadGroupParams.xReadGroupParams().count(1), streamQeury);

    Response<List<StreamPendingEntry>> pending1 = pipe.xpending("xpending-stream", "xpending-group",
        XPendingParams.xPendingParams("(0", "+", 5));

    Response<List<StreamPendingEntry>> pending2 = pipe.xpending("xpending-stream", "xpending-group",
        XPendingParams.xPendingParams(StreamEntryID.MINIMUM_ID, StreamEntryID.MAXIMUM_ID, 5));

    pipe.sync();

    assertThat(pending1.get().stream().map(StreamPendingEntry::getConsumerName).collect(Collectors.toList()),
        contains("consumer1", "consumer2"));

    assertThat(pending1.get().stream().map(StreamPendingEntry::getID).collect(Collectors.toList()),
        contains(id1, id2));

    assertThat(pending2.get().stream().map(StreamPendingEntry::getConsumerName).collect(Collectors.toList()),
        contains("consumer1", "consumer2"));

    assertThat(pending2.get().stream().map(StreamPendingEntry::getID).collect(Collectors.toList()),
        contains(id1, id2));
  }

  @Test
  public void xclaimWithParams() throws InterruptedException {
    Map<String, String> map1 = singletonMap("f1", "v1");
    StreamEntryID id1 = jedis.xadd("xpending-stream", (StreamEntryID) null, map1);

    pipe.xgroupCreate("xpending-stream", "xpending-group", null, false);

    // Read the event from Stream put it on pending
    pipe.xreadGroup("xpending-group", "xpending-consumer",
        XReadGroupParams.xReadGroupParams().count(1).block(1),
        singletonMap("xpending-stream", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY));

    // Get the pending event
    Response<List<StreamPendingEntry>> pending =
        pipe.xpending("xpending-stream", "xpending-group",
            XPendingParams.xPendingParams().count(3).consumer("xpending-consumer"));

    // must sync before the sleep
    pipe.sync();

    // Sleep a bit so we can claim events pending for more than 50ms
    Thread.sleep(100);

    Response<List<StreamEntry>> claimed =
        pipe.xclaim("xpending-stream", "xpending-group", "xpending-consumer2", 50,
            XClaimParams.xClaimParams().idle(0).retryCount(0), id1);

    pipe.sync();

    assertThat(pending.get().stream().map(StreamPendingEntry::getConsumerName).collect(Collectors.toList()),
        contains("xpending-consumer"));

    assertThat(pending.get().stream().map(StreamPendingEntry::getID).collect(Collectors.toList()),
        contains(id1));

    assertThat(claimed.get().stream().map(StreamEntry::getID).collect(Collectors.toList()),
        contains(id1));

    assertThat(claimed.get().stream().map(StreamEntry::getFields).collect(Collectors.toList()),
        contains(map1));
  }

  @Test
  public void xclaimJustId() throws InterruptedException {
    Map<String, String> map1 = singletonMap("f1", "v1");
    StreamEntryID id1 = jedis.xadd("xpending-stream", (StreamEntryID) null, map1);

    pipe.xgroupCreate("xpending-stream", "xpending-group", null, false);

    // Read the event from Stream put it on pending
    pipe.xreadGroup("xpending-group", "xpending-consumer",
        XReadGroupParams.xReadGroupParams().count(1).block(1),
        singletonMap("xpending-stream", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY));

    // Get the pending event
    Response<List<StreamPendingEntry>> pending =
        pipe.xpending("xpending-stream", "xpending-group",
            XPendingParams.xPendingParams().count(3).consumer("xpending-consumer"));

    // must sync before the sleep
    pipe.sync();

    // Sleep for 100ms so we can claim events pending for more than 50ms
    Thread.sleep(100);

    Response<List<StreamEntryID>> claimedIds =
        pipe.xclaimJustId("xpending-stream", "xpending-group", "xpending-consumer2", 50,
            XClaimParams.xClaimParams().idle(0).retryCount(0), id1);

    pipe.sync();

    assertThat(pending.get().stream().map(StreamPendingEntry::getConsumerName).collect(Collectors.toList()),
        contains("xpending-consumer"));

    assertThat(pending.get().stream().map(StreamPendingEntry::getID).collect(Collectors.toList()),
        contains(id1));

    assertThat(claimedIds.get(), contains(id1));
  }

  @Test
  public void xautoclaim() throws InterruptedException {
    Map<String, String> map1 = singletonMap("f1", "v1");
    StreamEntryID id1 = jedis.xadd("xpending-stream", (StreamEntryID) null, map1);

    pipe.xgroupCreate("xpending-stream", "xpending-group", null, false);

    // Read the event from Stream put it on pending
    pipe.xreadGroup("xpending-group", "xpending-consumer",
        XReadGroupParams.xReadGroupParams().count(1).block(1),
        singletonMap("xpending-stream", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY));

    // Get the pending event
    Response<List<StreamPendingEntry>> pending = pipe.xpending("xpending-stream", "xpending-group",
        XPendingParams.xPendingParams().count(3).consumer("xpending-consumer"));

    pipe.sync();

    // Sleep for 100ms so we can auto claim events pending for more than 50ms
    Thread.sleep(100);

    // Auto claim pending events to different consumer
    Response<Entry<StreamEntryID, List<StreamEntry>>> autoclaimed = pipe.xautoclaim("xpending-stream", "xpending-group",
        "xpending-consumer2", 50, new StreamEntryID(), new XAutoClaimParams().count(1));

    pipe.sync();

    assertThat(pending.get().stream().map(StreamPendingEntry::getConsumerName).collect(Collectors.toList()),
        contains("xpending-consumer"));

    assertThat(pending.get().stream().map(StreamPendingEntry::getID).collect(Collectors.toList()),
        contains(id1));

    assertThat(autoclaimed.get().getValue().stream().map(StreamEntry::getID).collect(Collectors.toList()),
        contains(id1));

    assertThat(autoclaimed.get().getValue().stream().map(StreamEntry::getFields).collect(Collectors.toList()),
        contains(map1));
  }

  @Test
  public void xautoclaimBinary() throws InterruptedException {
    Map<String, String> map1 = singletonMap("f1", "v1");
    StreamEntryID id1 = jedis.xadd("xpending-stream", XAddParams.xAddParams(), map1);

    pipe.xgroupCreate("xpending-stream", "xpending-group", null, false);

    // Read the event from Stream put it on pending
    pipe.xreadGroup("xpending-group", "xpending-consumer",
        XReadGroupParams.xReadGroupParams().count(1).block(1),
        singletonMap("xpending-stream", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY));

    // Get the pending event
    Response<List<StreamPendingEntry>> pending = pipe.xpending("xpending-stream", "xpending-group",
        XPendingParams.xPendingParams().count(3).consumer("xpending-consumer"));

    pipe.sync();

    // Sleep for 100ms so we can auto claim events pending for more than 50ms
    Thread.sleep(100);

    // Auto claim pending events to different consumer
    Response<List<Object>> autoclaimed = pipe.xautoclaim(SafeEncoder.encode("xpending-stream"),
        SafeEncoder.encode("xpending-group"), SafeEncoder.encode("xpending-consumer2"),
        50, SafeEncoder.encode(new StreamEntryID().toString()), new XAutoClaimParams().count(1));

    pipe.sync();

    assertThat(pending.get().stream().map(StreamPendingEntry::getConsumerName).collect(Collectors.toList()),
        contains("xpending-consumer"));

    assertThat(pending.get().stream().map(StreamPendingEntry::getID).collect(Collectors.toList()),
        contains(id1));

    Map.Entry<StreamEntryID, List<StreamEntry>> autoclaimedParsed =
        BuilderFactory.STREAM_AUTO_CLAIM_RESPONSE.build(autoclaimed.get());

    assertThat(autoclaimedParsed.getValue().stream().map(StreamEntry::getID).collect(Collectors.toList()),
        contains(id1));

    assertThat(autoclaimedParsed.getValue().stream().map(StreamEntry::getFields).collect(Collectors.toList()),
        contains(map1));
  }

  @Test
  public void xautoclaimJustId() throws InterruptedException {
    Map<String, String> map1 = singletonMap("f1", "v1");
    StreamEntryID id1 = jedis.xadd("xpending-stream", XAddParams.xAddParams(), map1);

    pipe.xgroupCreate("xpending-stream", "xpending-group", null, false);

    // Read the event from Stream put it on pending
    pipe.xreadGroup("xpending-group", "xpending-consumer", XReadGroupParams.xReadGroupParams().count(1).block(1),
        singletonMap("xpending-stream", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY));

    // Get the pending event
    Response<List<StreamPendingEntry>> pending = pipe.xpending("xpending-stream", "xpending-group",
        XPendingParams.xPendingParams().count(3).consumer("xpending-consumer"));

    pipe.sync();

    // Sleep for 100ms so we can auto claim events pending for more than 50ms
    Thread.sleep(100);

    // Auto claim pending events to different consumer
    Response<Entry<StreamEntryID, List<StreamEntryID>>> claimedIds = pipe.xautoclaimJustId("xpending-stream", "xpending-group",
        "xpending-consumer2", 50, new StreamEntryID(), new XAutoClaimParams().count(1));

    pipe.sync();

    assertThat(pending.get().stream().map(StreamPendingEntry::getConsumerName).collect(Collectors.toList()),
        contains("xpending-consumer"));

    assertThat(pending.get().stream().map(StreamPendingEntry::getID).collect(Collectors.toList()),
        contains(id1));

    assertThat(claimedIds.get().getValue(), contains(id1));
  }

  @Test
  public void xautoclaimJustIdBinary() throws InterruptedException {
    Map<String, String> map1 = singletonMap("f1", "v1");
    StreamEntryID id1 = jedis.xadd("xpending-stream", XAddParams.xAddParams(), map1);

    pipe.xgroupCreate("xpending-stream", "xpending-group", null, false);

    // Read the event from Stream put it on pending
    pipe.xreadGroup("xpending-group", "xpending-consumer",
        XReadGroupParams.xReadGroupParams().count(1).block(1),
        singletonMap("xpending-stream", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY));

    // Get the pending event
    Response<List<StreamPendingEntry>> pending = pipe.xpending("xpending-stream", "xpending-group",
        XPendingParams.xPendingParams().count(3).consumer("xpending-consumer"));

    pipe.sync();

    // Sleep for 100ms so we can auto claim events pending for more than 50ms
    Thread.sleep(100);

    // Auto claim pending events to different consumer
    Response<List<Object>> autoclaimed = pipe.xautoclaimJustId(SafeEncoder.encode("xpending-stream"),
        SafeEncoder.encode("xpending-group"), SafeEncoder.encode("xpending-consumer2"),
        50, SafeEncoder.encode(new StreamEntryID().toString()), new XAutoClaimParams().count(1));

    pipe.sync();

    assertThat(pending.get().stream().map(StreamPendingEntry::getConsumerName).collect(Collectors.toList()),
        contains("xpending-consumer"));

    assertThat(pending.get().stream().map(StreamPendingEntry::getID).collect(Collectors.toList()),
        contains(id1));

    Entry<StreamEntryID, List<StreamEntryID>> autoclaimedParsed =
        BuilderFactory.STREAM_AUTO_CLAIM_JUSTID_RESPONSE.build(autoclaimed.get());

    assertThat(autoclaimedParsed.getValue(), contains(id1));
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

    Map<String, String> map1 = new HashMap<>();
    map1.put(F1, V1);
    StreamEntryID id1 = jedis.xadd(STREAM_NAME, (StreamEntryID) null, map1);
    map1.put(F1, V2);
    StreamEntryID id2 = jedis.xadd(STREAM_NAME, (StreamEntryID) null, map1);

    Response<StreamInfo> streamInfoResponse = pipe.xinfoStream(STREAM_NAME);

    pipe.xgroupCreate(STREAM_NAME, G1, StreamEntryID.XGROUP_LAST_ENTRY, false);

    Map<String, StreamEntryID> streamQuery1 = singletonMap(STREAM_NAME, new StreamEntryID("0-0"));

    pipe.xreadGroup(G1, MY_CONSUMER, XReadGroupParams.xReadGroupParams().count(1), streamQuery1);

    pipe.sync();

    Thread.sleep(1);

    Response<List<StreamGroupInfo>> groupInfoResponse = pipe.xinfoGroups(STREAM_NAME);
    Response<List<StreamConsumersInfo>> consumersInfoResponse = pipe.xinfoConsumers(STREAM_NAME, G1);
    Response<List<StreamConsumerInfo>> consumerInfoResponse = pipe.xinfoConsumers2(STREAM_NAME, G1);

    pipe.sync();

    // Stream info test
    StreamInfo streamInfo = streamInfoResponse.get();

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
    List<StreamGroupInfo> groupInfo = groupInfoResponse.get();

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
    List<StreamConsumersInfo> consumersInfo = consumersInfoResponse.get();

    assertEquals(MY_CONSUMER,
        consumersInfo.get(0).getConsumerInfo().get(StreamConsumersInfo.NAME));
    assertEquals(0L, consumersInfo.get(0).getConsumerInfo().get(StreamConsumersInfo.PENDING));
    assertTrue((Long) consumersInfo.get(0).getConsumerInfo().get(StreamConsumersInfo.IDLE) > 0);

    // Using getters
    assertEquals(MY_CONSUMER, consumersInfo.get(0).getName());
    assertEquals(0L, consumersInfo.get(0).getPending());
    assertThat(consumersInfo.get(0).getIdle(), Matchers.greaterThanOrEqualTo(0L));
    if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V7_0_0)) {
      assertThat(consumersInfo.get(0).getInactive(), Matchers.any(Long.class));
    }

    // Consumer info test
    List<StreamConsumerInfo> consumerInfo = consumerInfoResponse.get();

    assertEquals(MY_CONSUMER,
        consumerInfo.get(0).getConsumerInfo().get(StreamConsumerInfo.NAME));
    assertEquals(0L, consumerInfo.get(0).getConsumerInfo().get(StreamConsumerInfo.PENDING));
    assertTrue((Long) consumerInfo.get(0).getConsumerInfo().get(StreamConsumerInfo.IDLE) > 0);

    // Using getters
    assertEquals(MY_CONSUMER, consumerInfo.get(0).getName());
    assertEquals(0L, consumerInfo.get(0).getPending());
    assertThat(consumerInfo.get(0).getIdle(), Matchers.greaterThanOrEqualTo(0L));
    if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V7_0_0)) {
      assertThat(consumerInfo.get(0).getInactive(), Matchers.any(Long.class));
    }

    // test with more groups and consumers
    pipe.xgroupCreate(STREAM_NAME, G2, StreamEntryID.XGROUP_LAST_ENTRY, false);
    pipe.xreadGroup(G1, MY_CONSUMER2, XReadGroupParams.xReadGroupParams().count(1), streamQuery1);
    pipe.xreadGroup(G2, MY_CONSUMER, XReadGroupParams.xReadGroupParams().count(1), streamQuery1);
    pipe.xreadGroup(G2, MY_CONSUMER2, XReadGroupParams.xReadGroupParams().count(1), streamQuery1);

    Response<List<StreamGroupInfo>> manyGroupsInfoResponse = pipe.xinfoGroups(STREAM_NAME);
    Response<List<StreamConsumersInfo>> manyConsumersInfoResponse = pipe.xinfoConsumers(STREAM_NAME, G2);
    Response<List<StreamConsumerInfo>> manyConsumerInfoResponse = pipe.xinfoConsumers2(STREAM_NAME, G2);
    Response<StreamFullInfo> streamInfoFullResponse = pipe.xinfoStreamFull(STREAM_NAME);
    Response<StreamFullInfo> streamInfoFull10Response = pipe.xinfoStreamFull(STREAM_NAME, 10);

    pipe.sync();

    List<StreamGroupInfo> manyGroupsInfo = manyGroupsInfoResponse.get();
    List<StreamConsumersInfo> manyConsumersInfo = manyConsumersInfoResponse.get();
    List<StreamConsumerInfo> manyConsumerInfo = manyConsumerInfoResponse.get();
    StreamFullInfo streamInfoFull = streamInfoFullResponse.get();
    StreamFullInfo streamInfoFull10 = streamInfoFull10Response.get();

    assertEquals(2, manyGroupsInfo.size());
    assertEquals(2, manyConsumersInfo.size());
    assertEquals(2, manyConsumerInfo.size());

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

    assertEquals(G1, streamInfoFull10.getGroups().get(0).getName());
    assertEquals(G2, streamInfoFull10.getGroups().get(1).getName());
    assertEquals(V1, streamInfoFull10.getEntries().get(0).getFields().get(F1));
    assertEquals(V2, streamInfoFull10.getEntries().get(1).getFields().get(F1));
    assertEquals(id2, streamInfoFull10.getLastGeneratedId());

    // Not existing key - redis cli return error so we expect exception
    pipe.xinfoStream("random");

    assertThat(pipe.syncAndReturnAll(), contains(
        both(instanceOf(JedisDataException.class)).and(hasToString(containsString("ERR no such key")))
    ));
  }

  @Test
  public void xinfoStreamFullWithPending() {
    Map<String, String> map = singletonMap("f1", "v1");
    StreamEntryID id1 = jedis.xadd("streamfull2", (StreamEntryID) null, map);
    StreamEntryID id2 = jedis.xadd("streamfull2", (StreamEntryID) null, map);
    jedis.xgroupCreate("streamfull2", "xreadGroup-group", null, false);

    Map<String, StreamEntryID> streamQeury1 = singletonMap("streamfull2", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    Response<List<Entry<String, List<StreamEntry>>>> pending = pipe.xreadGroup("xreadGroup-group", "xreadGroup-consumer",
        XReadGroupParams.xReadGroupParams().count(1), streamQeury1);

    Response<StreamFullInfo> fullResult = pipe.xinfoStreamFull("streamfull2");

    pipe.sync();

    assertThat(pending.get(), hasSize(1));

    StreamFullInfo full = fullResult.get();
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
    if (RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V7_0_0)) {
      assertThat(consumer.getActiveTime(), Matchers.greaterThanOrEqualTo(0L));
    }
    assertEquals(1, consumer.getPending().size());
    List<Object> consumerPendingEntry = consumer.getPending().get(0);
    assertEquals(id1, consumerPendingEntry.get(0));
  }
}
