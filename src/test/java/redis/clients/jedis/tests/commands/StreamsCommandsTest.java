package redis.clients.jedis.tests.commands;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import redis.clients.jedis.EntryID;
import redis.clients.jedis.exceptions.JedisDataException;

public class StreamsCommandsTest extends JedisCommandTestBase {

  @Test(expected = JedisDataException.class) 
  public void xaddFailed() {
    Map<String,String> map1 = new HashMap<String, String>();
    jedis.xadd("steam1", null, map1);
  }	

  @Test
  public void xadd() {
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
    List<Entry<EntryID, Map<String, String>>> range = jedis.xrange("xlen-xrange", (EntryID)null, (EntryID)null, Integer.MAX_VALUE); 
    assertEquals(0, range.size());
        
    Map<String,String> map = new HashMap<String, String>();
    map.put("f1", "v1");
    EntryID id1 = jedis.xadd("xlen-xrange", null, map);
    EntryID id2 = jedis.xadd("xlen-xrange", null, map);
    List<Entry<EntryID, Map<String, String>>> range2 = jedis.xrange("xlen-xrange", (EntryID)null, (EntryID)null, 3); 
    assertEquals(2, range2.size());
    
    List<Entry<EntryID, Map<String, String>>> range3 = jedis.xrange("xlen-xrange", id1, null, 2); 
    assertEquals(2, range3.size());
    
    List<Entry<EntryID, Map<String, String>>> range4 = jedis.xrange("xlen-xrange", id1, id2, 2); 
    assertEquals(2, range4.size());

    List<Entry<EntryID, Map<String, String>>> range5 = jedis.xrange("xlen-xrange", id1, id2, 1); 
    assertEquals(1, range5.size());
    
    List<Entry<EntryID, Map<String, String>>> range6 = jedis.xrange("xlen-xrange", id2, null, 4); 
    assertEquals(1, range6.size());
    
    EntryID id3 = jedis.xadd("xlen-xrange", null, map);
    List<Entry<EntryID, Map<String, String>>> range7 = jedis.xrange("xlen-xrange", id2, id2, 4); 
    assertEquals(1, range7.size());
  }

}
