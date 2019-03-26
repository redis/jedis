package redis.clients.jedis.tests.commands;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.StreamPendingEntry;
import redis.clients.jedis.StreamEntry;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.exceptions.JedisDataException;

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

    //TODO test xgroupDelConsumer
  }
  
  @Test
  public void xreadGroup() {
    
    // Simple xreadGroup with NOACK
    Map<String,String> map = new HashMap<>();
    map.put("f1", "v1");
    StreamEntryID id1 = jedis.xadd("xreadGroup-stream1", null, map);
    String status1 = jedis.xgroupCreate("xreadGroup-stream1", "xreadGroup-group", null, false);
    Entry<String, StreamEntryID> streamQeury1 = new AbstractMap.SimpleImmutableEntry<>("xreadGroup-stream1", new StreamEntryID());
    List<Entry<String, List<StreamEntry>>> range = jedis.xreadGroup("xreadGroup-group", "xreadGroup-consumer", 1, 0, true, streamQeury1); 
    assertEquals(1, range.size());
    assertEquals(1, range.get(0).getValue().size());
    
    StreamEntryID id2 = jedis.xadd("xreadGroup-stream1", null, map);
    StreamEntryID id3 = jedis.xadd("xreadGroup-stream2", null, map);
    String status2 = jedis.xgroupCreate("xreadGroup-stream2", "xreadGroup-group", null, false);
   
    // Read only a single Stream
    Entry<String, StreamEntryID> streamQeury11 = new AbstractMap.SimpleImmutableEntry<>("xreadGroup-stream1", range.get(0).getValue().get(0).getID());
    List<Entry<String, List<StreamEntry>>> streams1 = jedis.xreadGroup("xreadGroup-group", "xreadGroup-consumer", 1, 1L, true, streamQeury11); 
    assertEquals(1, streams1.size());
    assertEquals(1, range.get(0).getValue().size());

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
    
    Entry<String, StreamEntryID> streamQeury1 = new AbstractMap.SimpleImmutableEntry<String, StreamEntryID>("xack-stream", new StreamEntryID());

    // Empty Stream
    List<Entry<String, List<StreamEntry>>> range = jedis.xreadGroup("xack-group", "xack-consumer", 1, 1L, false, streamQeury1); 
    assertEquals(1, range.size());

    assertEquals(1L, jedis.xack("xack-stream", "xack-group", range.get(0).getValue().get(0).getID()));
  }
  
  @Test
  public void xpendeing() {       
    Map<String,String> map = new HashMap<String, String>();
    map.put("image", "data:image/jpeg;base64,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");
    StreamEntryID id1 = jedis.xadd("camera:0:yolo", null, map);
    
//    assertEquals("OK", jedis.xgroupCreate("xpendeing-stream", "xpendeing-group", null, false));
    StreamEntryID id2 = jedis.xadd("camera:0:yolo", null, map);
    
    Entry<String, StreamEntryID> streamQeury1 = new AbstractMap.SimpleImmutableEntry<String, StreamEntryID>("xpendeing-stream", new StreamEntryID());

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
    
    jedis.xclaim("xpendeing-stream", "xpendeing-group", "xpendeing-consumer2", 500, 0, 0, false, pendingRange.get(0).getID());
  }  
}
