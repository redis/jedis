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
import redis.clients.jedis.EntryID;
import redis.clients.jedis.StreamEntry;
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
    EntryID id1 = jedis.xadd("xadd-stream1", null, map1);
    assertNotNull(id1);	

    Map<String,String> map2 = new HashMap<String, String>();
    map2.put("f1", "v1");
    map2.put("f2", "v2");
    EntryID id2 = jedis.xadd("xadd-stream1", null, map2);
    assertTrue(id2.compareTo(id1) > 0);

    Map<String,String> map3 = new HashMap<String, String>();
    map3.put("f2", "v2");
    map3.put("f3", "v3");
    EntryID id3 = jedis.xadd("xadd-stream2", null, map3);

    Map<String,String> map4 = new HashMap<String, String>();
    map4.put("f2", "v2");
    map4.put("f3", "v3");
    EntryID idIn = new EntryID(id3.getTime()+1, 1L);
    EntryID id4 = jedis.xadd("xadd-stream2", idIn, map4);
    assertEquals(idIn, id4);
    assertTrue(id4.compareTo(id3) > 0);
    
    Map<String,String> map5 = new HashMap<String, String>();
    map3.put("f4", "v4");
    map3.put("f5", "v5");
    EntryID id5 = jedis.xadd("xadd-stream2", null, map3);
    assertTrue(id5.compareTo(id4) > 0);    
    
    Map<String,String> map6 = new HashMap<String, String>();
    map3.put("f4", "v4");
    map3.put("f5", "v5");
    EntryID id6 = jedis.xadd("xadd-stream2", null, map3, 3, true);
    assertTrue(id6.compareTo(id5) > 0);
    assertEquals(3L, jedis.xlen("xadd-stream2").longValue());
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
    List<StreamEntry> range = jedis.xrange("xrange-stream", (EntryID)null, (EntryID)null, Integer.MAX_VALUE); 
    assertEquals(0, range.size());
        
    Map<String,String> map = new HashMap<String, String>();
    map.put("f1", "v1");
    EntryID id1 = jedis.xadd("xrange-stream", null, map);
    EntryID id2 = jedis.xadd("xrange-stream", null, map);
    List<StreamEntry> range2 = jedis.xrange("xrange-stream", (EntryID)null, (EntryID)null, 3); 
    assertEquals(2, range2.size());
    
    List<StreamEntry> range3 = jedis.xrange("xrange-stream", id1, null, 2); 
    assertEquals(2, range3.size());
    
    List<StreamEntry> range4 = jedis.xrange("xrange-stream", id1, id2, 2); 
    assertEquals(2, range4.size());

    List<StreamEntry> range5 = jedis.xrange("xrange-stream", id1, id2, 1); 
    assertEquals(1, range5.size());
    
    List<StreamEntry> range6 = jedis.xrange("xrange-stream", id2, null, 4); 
    assertEquals(1, range6.size());
    
    EntryID id3 = jedis.xadd("xrange-stream", null, map);
    List<StreamEntry> range7 = jedis.xrange("xrange-stream", id2, id2, 4); 
    assertEquals(1, range7.size());
  }
  
  @Test
  public void xread() {
    
    List<Entry<String, EntryID>> streamsQuery1 = new ArrayList<Entry<String, EntryID>>();
    streamsQuery1.add(new AbstractMap.SimpleImmutableEntry<String, EntryID>("xread-stream1", new EntryID()));

    // Empty Stream
    List<Entry<String, List<StreamEntry>>> range = jedis.xread(1, 1L, streamsQuery1); 
    assertEquals(0, range.size());
    
    Map<String,String> map = new HashMap<String, String>();
    map.put("f1", "v1");
    EntryID id1 = jedis.xadd("xread-stream1", null, map);
    EntryID id2 = jedis.xadd("xread-stream2", null, map);
    
    // Read only a single Stream
    List<Entry<String, List<StreamEntry>>> streams1 = jedis.xread(1, 1L, streamsQuery1); 
    assertEquals(1, streams1.size());

    // Read from two Streams
    List<Entry<String, EntryID>> streamsQuery2 = new ArrayList<Entry<String, EntryID>>();
    streamsQuery2.add(new AbstractMap.SimpleImmutableEntry<String, EntryID>("xread-stream1", new EntryID()));
    streamsQuery2.add(new AbstractMap.SimpleImmutableEntry<String, EntryID>("xread-stream2", new EntryID()));
    List<Entry<String, List<StreamEntry>>> streams2 = jedis.xread(2, 1L, streamsQuery2); 
    assertEquals(2, streams2.size());

  }
  
  @Test
  public void xack() {
    
    List<Entry<String, EntryID>> streamsACK = new ArrayList<Entry<String, EntryID>>();
    streamsACK.add(new AbstractMap.SimpleImmutableEntry<String, EntryID>("xack-stream1", new EntryID()));
    
    Map<String,String> map = new HashMap<String, String>();
    map.put("f1", "v1");
    EntryID id1 = jedis.xadd("xack-stream", null, map);
    
    // Read only a single Stream
    List<Entry<String, List<StreamEntry>>> streams1 = jedis.xread(1, 1L, streamsACK); 
    assertEquals(1, streams1.size());

//   jedis.xack(key, group, ids)

  }
  

}
