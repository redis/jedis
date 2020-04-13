package redis.clients.jedis.tests.commands;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static redis.clients.jedis.StreamGroupInfo.CONSUMERS;
import static redis.clients.jedis.StreamGroupInfo.LAST_DELIVERED;
import static redis.clients.jedis.StreamGroupInfo.NAME;
import static redis.clients.jedis.StreamGroupInfo.PENDING;
import static redis.clients.jedis.StreamInfo.FIRST_ENTRY;
import static redis.clients.jedis.StreamInfo.GROUPS;
import static redis.clients.jedis.StreamInfo.LAST_ENTRY;
import static redis.clients.jedis.StreamInfo.LAST_GENERATED_ID;
import static redis.clients.jedis.StreamInfo.LENGTH;
import static redis.clients.jedis.StreamInfo.RADIX_TREE_KEYS;
import static redis.clients.jedis.StreamInfo.RADIX_TREE_NODES;
import static redis.clients.jedis.StreamConsumersInfo.IDLE;


import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import redis.clients.jedis.*;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.SafeEncoder;

public class StreamsCommandsTest extends JedisCommandTestBase {

  @Test
  public void xadd() {
    
    try {
      Map<String,String> map1 = new HashMap<String, String>();
      jedis.xadd("stream1", null, map1);
      fail();
    } catch (JedisDataException expected) {
      assertEquals("ERR wrong number of arguments for 'xadd' command", expected.getMessage());
    }
    
    Map<String,String> map1 = new HashMap<String, String>();
    map1.put("f1", "v1");
    StreamEntryID id1 = jedis.xadd("xadd-stream1", null, map1);
    assertNotNull(id1);	

    Map<String,String> map2 = new HashMap<String, String>();
    map2.put("f1", "v1");
    map2.put("f2", "v2");
    StreamEntryID id2 = jedis.xadd("xadd-stream1", null, map2);
    assertTrue(id2.compareTo(id1) > 0);

    Map<String,String> map3 = new HashMap<String, String>();
    map3.put("f2", "v2");
    map3.put("f3", "v3");
    StreamEntryID id3 = jedis.xadd("xadd-stream2", null, map3);

    Map<String,String> map4 = new HashMap<String, String>();
    map4.put("f2", "v2");
    map4.put("f3", "v3");
    StreamEntryID idIn = new StreamEntryID(id3.getTime()+1, 1L);
    StreamEntryID id4 = jedis.xadd("xadd-stream2", idIn, map4);
    assertEquals(idIn, id4);
    assertTrue(id4.compareTo(id3) > 0);
    
    Map<String,String> map5 = new HashMap<String, String>();
    map3.put("f4", "v4");
    map3.put("f5", "v5");
    StreamEntryID id5 = jedis.xadd("xadd-stream2", null, map3);
    assertTrue(id5.compareTo(id4) > 0);    
    
    Map<String,String> map6 = new HashMap<String, String>();
    map3.put("f4", "v4");
    map3.put("f5", "v5");
    StreamEntryID id6 = jedis.xadd("xadd-stream2", null, map3, 3, false);
    assertTrue(id6.compareTo(id5) > 0);
    assertEquals(3L, jedis.xlen("xadd-stream2").longValue());
  }
  
  @Test
  public void xdel() {
    Map<String,String> map1 = new HashMap<String, String>();
    map1.put("f1", "v1");
    
    StreamEntryID id1 = jedis.xadd("xdel-stream", null, map1);
    assertNotNull(id1); 
    
    StreamEntryID id2 = jedis.xadd("xdel-stream", null, map1);
    assertNotNull(id2);
    assertEquals(2L, jedis.xlen("xdel-stream").longValue());


    assertEquals(1L, jedis.xdel("xdel-stream", id1));
    assertEquals(1L, jedis.xlen("xdel-stream").longValue());
  }

  @Test
  public void xlen() {
    assertEquals(0L, jedis.xlen("xlen-stream").longValue());
    
    Map<String,String> map = new HashMap<String, String>();
    map.put("f1", "v1");
    jedis.xadd("xlen-stream", null, map);
    assertEquals(1L, jedis.xlen("xlen-stream").longValue());
    
    jedis.xadd("xlen-stream", null, map);
    assertEquals(2L, jedis.xlen("xlen-stream").longValue());
  }

  @Test
  public void xrange() {
    List<StreamEntry> range = jedis.xrange("xrange-stream", (StreamEntryID)null, (StreamEntryID)null, Integer.MAX_VALUE); 
    assertEquals(0, range.size());
        
    Map<String,String> map = new HashMap<String, String>();
    map.put("f1", "v1");
    StreamEntryID id1 = jedis.xadd("xrange-stream", null, map);
    StreamEntryID id2 = jedis.xadd("xrange-stream", null, map);
    List<StreamEntry> range2 = jedis.xrange("xrange-stream", (StreamEntryID)null, (StreamEntryID)null, 3); 
    assertEquals(2, range2.size());
    
    List<StreamEntry> range3 = jedis.xrange("xrange-stream", id1, null, 2); 
    assertEquals(2, range3.size());
    
    List<StreamEntry> range4 = jedis.xrange("xrange-stream", id1, id2, 2); 
    assertEquals(2, range4.size());

    List<StreamEntry> range5 = jedis.xrange("xrange-stream", id1, id2, 1); 
    assertEquals(1, range5.size());
    
    List<StreamEntry> range6 = jedis.xrange("xrange-stream", id2, null, 4); 
    assertEquals(1, range6.size());
    
    StreamEntryID id3 = jedis.xadd("xrange-stream", null, map);
    List<StreamEntry> range7 = jedis.xrange("xrange-stream", id2, id2, 4); 
    assertEquals(1, range7.size());
  }
  
  @Test
  public void xread() {
    
    Entry<String, StreamEntryID> streamQeury1 = new AbstractMap.SimpleImmutableEntry<String, StreamEntryID>("xread-stream1", new StreamEntryID());

    // Empty Stream
    List<Entry<String, List<StreamEntry>>> range = jedis.xread(1, 1L, streamQeury1); 
    assertEquals(0, range.size());
    
    Map<String,String> map = new HashMap<String, String>();
    map.put("f1", "v1");
    StreamEntryID id1 = jedis.xadd("xread-stream1", null, map);
    StreamEntryID id2 = jedis.xadd("xread-stream2", null, map);
    
    // Read only a single Stream
    List<Entry<String, List<StreamEntry>>> streams1 = jedis.xread(1, 1L, streamQeury1); 
    assertEquals(1, streams1.size());

    // Read from two Streams
    Entry<String, StreamEntryID> streamQuery2 = new AbstractMap.SimpleImmutableEntry<String, StreamEntryID>("xread-stream1", new StreamEntryID());
    Entry<String, StreamEntryID> streamQuery3 = new AbstractMap.SimpleImmutableEntry<String, StreamEntryID>("xread-stream2", new StreamEntryID());
    List<Entry<String, List<StreamEntry>>> streams2 = jedis.xread(2, 1L, streamQuery2, streamQuery3); 
    assertEquals(2, streams2.size());

  }
  
  @Test
  public void xtrim() {
    Map<String,String> map1 = new HashMap<String, String>();
    map1.put("f1", "v1");
    
    jedis.xadd("xtrim-stream", null, map1);
    jedis.xadd("xtrim-stream", null, map1);
    jedis.xadd("xtrim-stream", null, map1);
    jedis.xadd("xtrim-stream", null, map1);
    jedis.xadd("xtrim-stream", null, map1);
    assertEquals(5L, jedis.xlen("xtrim-stream").longValue());

    jedis.xtrim("xtrim-stream", 3, false);
    assertEquals(3L, jedis.xlen("xtrim-stream").longValue());
  }
  
  @Test
  public void xrevrange() {
    List<StreamEntry> range = jedis.xrevrange("xrevrange-stream", (StreamEntryID)null, (StreamEntryID)null, Integer.MAX_VALUE); 
    assertEquals(0, range.size());
        
    Map<String,String> map = new HashMap<String, String>();
    map.put("f1", "v1");
    StreamEntryID id1 = jedis.xadd("xrevrange-stream", null, map);
    StreamEntryID id2 = jedis.xadd("xrevrange-stream", null, map);
    List<StreamEntry> range2 = jedis.xrange("xrevrange-stream", (StreamEntryID)null, (StreamEntryID)null, 3); 
    assertEquals(2, range2.size());
    
    List<StreamEntry> range3 = jedis.xrevrange("xrevrange-stream", null, id1, 2); 
    assertEquals(2, range3.size());
    
    List<StreamEntry> range4 = jedis.xrevrange("xrevrange-stream", id2, id1, 2); 
    assertEquals(2, range4.size());

    List<StreamEntry> range5 = jedis.xrevrange("xrevrange-stream", id2, id1, 1); 
    assertEquals(1, range5.size());
    
    List<StreamEntry> range6 = jedis.xrevrange("xrevrange-stream", null, id2, 4); 
    assertEquals(1, range6.size());
    
    StreamEntryID id3 = jedis.xadd("xrevrange-stream", null, map);
    List<StreamEntry> range7 = jedis.xrevrange("xrevrange-stream", id2, id2, 4); 
    assertEquals(1, range7.size());

  }
  
  @Test
  public void xgroup() {
    
    Map<String,String> map = new HashMap<String, String>();
    map.put("f1", "v1");
    StreamEntryID id1 = jedis.xadd("xgroup-stream", null, map);
    
    String status = jedis.xgroupCreate("xgroup-stream", "consumer-group-name", null, false);
    assertTrue(Keyword.OK.name().equalsIgnoreCase(status));


    status = jedis.xgroupSetID("xgroup-stream", "consumer-group-name", id1);
    assertTrue(Keyword.OK.name().equalsIgnoreCase(status));

    status = jedis.xgroupCreate("xgroup-stream", "consumer-group-name1", StreamEntryID.LAST_ENTRY, false);
    assertTrue(Keyword.OK.name().equalsIgnoreCase(status));
    
    jedis.xgroupDestroy("xgroup-stream", "consumer-group-name");

    Long pendingMessageNum = jedis.xgroupDelConsumer("xgroup-stream", "consumer-group-name1", "myconsumer1");
    assertEquals(0L, pendingMessageNum.longValue());  
  }
  
  @Test
  public void xreadGroup() {
    
    // Simple xreadGroup with NOACK
    Map<String,String> map = new HashMap<>();
    map.put("f1", "v1");
    StreamEntryID id1 = jedis.xadd("xreadGroup-stream1", null, map);
    String status1 = jedis.xgroupCreate("xreadGroup-stream1", "xreadGroup-group", null, false);
    Entry<String, StreamEntryID> streamQeury1 = new AbstractMap.SimpleImmutableEntry<>("xreadGroup-stream1", StreamEntryID.UNRECEIVED_ENTRY);
    List<Entry<String, List<StreamEntry>>> range = jedis.xreadGroup("xreadGroup-group", "xreadGroup-consumer", 1, 0, true, streamQeury1); 
    assertEquals(1, range.size());
    assertEquals(1, range.get(0).getValue().size());
    
    StreamEntryID id2 = jedis.xadd("xreadGroup-stream1", null, map);
    StreamEntryID id3 = jedis.xadd("xreadGroup-stream2", null, map);
    String status2 = jedis.xgroupCreate("xreadGroup-stream2", "xreadGroup-group", null, false);
   
    // Read only a single Stream
    Entry<String, StreamEntryID> streamQeury11 = new AbstractMap.SimpleImmutableEntry<>("xreadGroup-stream1", StreamEntryID.UNRECEIVED_ENTRY);
    List<Entry<String, List<StreamEntry>>> streams1 = jedis.xreadGroup("xreadGroup-group", "xreadGroup-consumer", 1, 1L, true, streamQeury11); 
    assertEquals(1, streams1.size());
    assertEquals(1, streams1.get(0).getValue().size());

    // Read from two Streams
    Entry<String, StreamEntryID> streamQuery2 = new AbstractMap.SimpleImmutableEntry<String, StreamEntryID>("xreadGroup-stream1", new StreamEntryID());
    Entry<String, StreamEntryID> streamQuery3 = new AbstractMap.SimpleImmutableEntry<String, StreamEntryID>("xreadGroup-stream2", new StreamEntryID());
    List<Entry<String, List<StreamEntry>>> streams2 = jedis.xreadGroup("xreadGroup-group", "xreadGroup-consumer", 1, 1L, true, streamQuery2, streamQuery3); 
    assertEquals(2, streams2.size());

    // Read only fresh messages
    StreamEntryID id4 = jedis.xadd("xreadGroup-stream1", null, map);
    Entry<String, StreamEntryID> streamQeuryFresh = new AbstractMap.SimpleImmutableEntry<String, StreamEntryID>("xreadGroup-stream1", StreamEntryID.UNRECEIVED_ENTRY);
    List<Entry<String, List<StreamEntry>>> streams3 = jedis.xreadGroup("xreadGroup-group", "xreadGroup-consumer", 4, 100L, true, streamQeuryFresh); 
    assertEquals(1, streams3.size());  
    assertEquals(id4, streams3.get(0).getValue().get(0).getID());
  }

  
  
  @Test
  public void xack() {
       
    Map<String,String> map = new HashMap<String, String>();
    map.put("f1", "v1");
    StreamEntryID id1 = jedis.xadd("xack-stream", null, map);
    
    String status = jedis.xgroupCreate("xack-stream", "xack-group", null, false);
    
    Entry<String, StreamEntryID> streamQeury1 = new AbstractMap.SimpleImmutableEntry<String, StreamEntryID>("xack-stream", StreamEntryID.UNRECEIVED_ENTRY);

    // Empty Stream
    List<Entry<String, List<StreamEntry>>> range = jedis.xreadGroup("xack-group", "xack-consumer", 1, 1L, false, streamQeury1); 
    assertEquals(1, range.size());

    assertEquals(1L, jedis.xack("xack-stream", "xack-group", range.get(0).getValue().get(0).getID()));
  }
  
  @Test
  public void xpendeing() {       
    Map<String,String> map = new HashMap<String, String>();
    map.put("f1", "v1");
    StreamEntryID id1 = jedis.xadd("xpendeing-stream", null, map);
    
    String status = jedis.xgroupCreate("xpendeing-stream", "xpendeing-group", null, false);
    
    Entry<String, StreamEntryID> streamQeury1 = new AbstractMap.SimpleImmutableEntry<String, StreamEntryID>("xpendeing-stream", StreamEntryID.UNRECEIVED_ENTRY);

    // Empty Stream
    List<Entry<String, List<StreamEntry>>> range = jedis.xreadGroup("xpendeing-group", "xpendeing-consumer", 1, 1L, false, streamQeury1); 
    assertEquals(1, range.size());
    assertEquals(1, range.get(0).getValue().size());
    
    List<StreamPendingEntry> pendingRange = jedis.xpending("xpendeing-stream", "xpendeing-group", null, null, 3, "xpendeing-consumer");
    assertEquals(1, pendingRange.size());
    
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    
    List<StreamEntry> claimRange = jedis.xclaim("xpendeing-stream", "xpendeing-group", "xpendeing-consumer2", 500, 0, 0, false, pendingRange.get(0).getID());
    assertEquals(1, pendingRange.size());
    
    Long pendingMessageNum = jedis.xgroupDelConsumer("xpendeing-stream", "xpendeing-group", "xpendeing-consumer2");
    assertEquals(1L, pendingMessageNum.longValue()); 
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
    StreamEntryID id1 = jedis.xadd(STREAM_NAME, null, map1);
    map1.put(F1, V2);
    StreamEntryID id2 = jedis.xadd(STREAM_NAME, null, map1);
    assertNotNull(id1);
    StreamInfo streamInfo =jedis.xinfoStream(STREAM_NAME);
    assertNotNull(id2);

    jedis.xgroupCreate(STREAM_NAME,G1, StreamEntryID.LAST_ENTRY,false);
    Entry<String, StreamEntryID> streamQeury11 = new AbstractMap.SimpleImmutableEntry<>(STREAM_NAME, new StreamEntryID("0-0"));
    jedis.xreadGroup(G1, MY_CONSUMER,1,0,false,streamQeury11);

    Thread.sleep(1);

    List<StreamGroupInfo> groupInfo = jedis.xinfoGroup(STREAM_NAME);
    List<StreamConsumersInfo> consumersInfo = jedis.xinfoConsumers(STREAM_NAME, G1);

    //Stream info test
    assertEquals(2L,streamInfo.getStreamInfo().get(LENGTH));
    assertEquals(1L,streamInfo.getStreamInfo().get(RADIX_TREE_KEYS));
    assertEquals(2L,streamInfo.getStreamInfo().get(RADIX_TREE_NODES));
    assertEquals(0L,streamInfo.getStreamInfo().get(GROUPS));
    assertEquals(V1,((StreamEntry)streamInfo.getStreamInfo().get(FIRST_ENTRY)).getFields().get(F1));
    assertEquals(V2,((StreamEntry)streamInfo.getStreamInfo().get(LAST_ENTRY)).getFields().get(F1));
    assertEquals(id2,streamInfo.getStreamInfo().get(LAST_GENERATED_ID));

    //Using getters
    assertEquals(2,streamInfo.getLength());
    assertEquals(1,streamInfo.getRadixTreeKeys());
    assertEquals(2,streamInfo.getRadixTreeNodes());
    assertEquals(0,streamInfo.getGroups());
    assertEquals(V1,streamInfo.getFirstEntry().getFields().get(F1));
    assertEquals(V2,streamInfo.getLastEntry().getFields().get(F1));
    assertEquals(id2,streamInfo.getLastGeneratedId());


    //Group info test
    assertEquals(1,groupInfo.size());
    assertEquals(G1,groupInfo.get(0).getGroupInfo().get(NAME));
    assertEquals(1L,groupInfo.get(0).getGroupInfo().get(CONSUMERS));
    assertEquals(0L,groupInfo.get(0).getGroupInfo().get(PENDING));
    assertEquals(id2,groupInfo.get(0).getGroupInfo().get(LAST_DELIVERED));

    //Using getters
    assertEquals(1,groupInfo.size());
    assertEquals(G1,groupInfo.get(0).getName());
    assertEquals(1,groupInfo.get(0).getConsumers());
    assertEquals(0,groupInfo.get(0).getPending());
    assertEquals(id2,groupInfo.get(0).getLastDeliveredId());

    //Consumer info test
    assertEquals(MY_CONSUMER,consumersInfo.get(0).getConsumerInfo().get(redis.clients.jedis.StreamConsumersInfo.NAME));
    assertEquals(0L,consumersInfo.get(0).getConsumerInfo().get(StreamConsumersInfo.PENDING));
    assertTrue((Long)consumersInfo.get(0).getConsumerInfo().get(IDLE)>0);

    //Using getters
    assertEquals(MY_CONSUMER,consumersInfo.get(0).getName());
    assertEquals(0L,consumersInfo.get(0).getPending());
    assertTrue(consumersInfo.get(0).getIdle()>0);

    //test with more groups and consumers
    jedis.xgroupCreate(STREAM_NAME,G2, StreamEntryID.LAST_ENTRY,false);
    jedis.xreadGroup(G1, MY_CONSUMER2,1,0,false,streamQeury11);
    jedis.xreadGroup(G2, MY_CONSUMER,1,0,false,streamQeury11);
    jedis.xreadGroup(G2, MY_CONSUMER2,1,0,false,streamQeury11);

    List<StreamGroupInfo> manyGroupsInfo = jedis.xinfoGroup(STREAM_NAME);
    List<StreamConsumersInfo> manyConsumersInfo = jedis.xinfoConsumers(STREAM_NAME, G2);

    assertEquals(2,manyGroupsInfo.size());
    assertEquals(2,manyConsumersInfo.size());

    //Not existing key - redis cli return error so we expect exception
    try {
      jedis.xinfoStream("random");
      fail("Command should fail");
    } catch (JedisException e) {
      assertEquals("ERR no such key", e.getMessage());
    }

  }

  @Test
  public void xinfoBinary() throws InterruptedException {

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
    StreamEntryID id1 = jedis.xadd(STREAM_NAME, null, map1);
    map1.put(F1, V2);
    StreamEntryID id2 = jedis.xadd(STREAM_NAME, null, map1);
    assertNotNull(id1);
    StreamInfo streamInfo = jedis.xinfoStream(SafeEncoder.encode(STREAM_NAME));
    assertNotNull(id2);

    jedis.xgroupCreate(STREAM_NAME,G1, StreamEntryID.LAST_ENTRY,false);
    Entry<String, StreamEntryID> streamQeury11 = new AbstractMap.SimpleImmutableEntry<>(STREAM_NAME, new StreamEntryID("0-0"));
    jedis.xreadGroup(G1, MY_CONSUMER,1,0,false,streamQeury11);

    Thread.sleep(1);

    List<StreamGroupInfo> groupInfo = jedis.xinfoGroup(SafeEncoder.encode(STREAM_NAME));
    List<StreamConsumersInfo> consumersInfo = jedis.xinfoConsumers(SafeEncoder.encode(STREAM_NAME), SafeEncoder.encode(G1));

    //Stream info test
    assertEquals(2L,streamInfo.getStreamInfo().get(LENGTH));
    assertEquals(1L,streamInfo.getStreamInfo().get(RADIX_TREE_KEYS));
    assertEquals(2L,streamInfo.getStreamInfo().get(RADIX_TREE_NODES));
    assertEquals(0L,streamInfo.getStreamInfo().get(GROUPS));
    assertEquals(V1,((StreamEntry)streamInfo.getStreamInfo().get(FIRST_ENTRY)).getFields().get(F1));
    assertEquals(V2,((StreamEntry)streamInfo.getStreamInfo().get(LAST_ENTRY)).getFields().get(F1));
    assertEquals(id2,streamInfo.getStreamInfo().get(LAST_GENERATED_ID));

    //Group info test
    assertEquals(1,groupInfo.size());
    assertEquals(G1,groupInfo.get(0).getGroupInfo().get(NAME));
    assertEquals(1L,groupInfo.get(0).getGroupInfo().get(CONSUMERS));
    assertEquals(0L,groupInfo.get(0).getGroupInfo().get(PENDING));
    assertEquals(id2,groupInfo.get(0).getGroupInfo().get(LAST_DELIVERED));

    //Consumer info test
    assertEquals(MY_CONSUMER,consumersInfo.get(0).getConsumerInfo().get(redis.clients.jedis.StreamConsumersInfo.NAME));
    assertEquals(0L,consumersInfo.get(0).getConsumerInfo().get(StreamConsumersInfo.PENDING));
    assertTrue((Long)consumersInfo.get(0).getConsumerInfo().get(IDLE)>0);

    //test with more groups and consumers
    jedis.xgroupCreate(STREAM_NAME,G2, StreamEntryID.LAST_ENTRY,false);
    jedis.xreadGroup(G1, MY_CONSUMER2,1,0,false,streamQeury11);
    jedis.xreadGroup(G2, MY_CONSUMER,1,0,false,streamQeury11);
    jedis.xreadGroup(G2, MY_CONSUMER2,1,0,false,streamQeury11);

    List<StreamGroupInfo> manyGroupsInfo = jedis.xinfoGroup(STREAM_NAME);
    List<StreamConsumersInfo> manyConsumersInfo = jedis.xinfoConsumers(STREAM_NAME, G2);

    assertEquals(2,manyGroupsInfo.size());
    assertEquals(2,manyConsumersInfo.size());

    //Not existing key - redis cli return error so we expect exception
    try {
      jedis.xinfoStream(SafeEncoder.encode("random"));
      fail("Command should fail");
    } catch (JedisException e) {
      assertEquals("ERR no such key", e.getMessage());
    }

  }



  @Test
  public void pipeline() {
    Map<String,String> map = new HashMap<>();
    map.put("a", "b");
    Pipeline p = jedis.pipelined();
    Response<StreamEntryID> id1 = p.xadd("stream1", StreamEntryID.NEW_ENTRY, map);
    Response<StreamEntryID> id2 = p.xadd("stream1", StreamEntryID.NEW_ENTRY, map);
    Response<List<StreamEntry>> results = p.xrange("stream1", null, null, 2);
    p.sync();

    List<StreamEntry> entries = results.get();
    assertEquals(2, entries.size());
    assertEquals(id1.get(), entries.get(0).getID());
    assertEquals(map, entries.get(0).getFields());
    assertEquals(id2.get(), entries.get(1).getID());
    assertEquals(map, entries.get(1).getFields());
  }

  @Test
  public void transaction() {
    Map<String,String> map = new HashMap<>();
    map.put("a", "b");
    Transaction t = jedis.multi();
    Response<StreamEntryID> id1 = t.xadd("stream1", StreamEntryID.NEW_ENTRY, map);
    Response<StreamEntryID> id2 = t.xadd("stream1", StreamEntryID.NEW_ENTRY, map);
    Response<List<StreamEntry>> results = t.xrange("stream1", null, null, 2);
    t.exec();

    List<StreamEntry> entries = results.get();
    assertEquals(2, entries.size());
    assertEquals(id1.get(), entries.get(0).getID());
    assertEquals(map, entries.get(0).getFields());
    assertEquals(id2.get(), entries.get(1).getID());
    assertEquals(map, entries.get(1).getFields());
  }
}
