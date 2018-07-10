package redis.clients.jedis.tests.commands;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
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
		EntryID id1 = jedis.xadd("stream1", null, map1);
		assertNotNull(id1);	
		
		Map<String,String> map2 = new HashMap<String, String>();
		map2.put("f1", "v1");
		map2.put("f2", "v2");
		EntryID id2 = jedis.xadd("stream1", null, map2);
		assertTrue(id2.compareTo(id1) > 0);

		Map<String,String> map3 = new HashMap<String, String>();
		map3.put("f2", "v2");
		map3.put("f3", "v3");
		EntryID id3 = jedis.xadd("stream2", null, map3);
		assertTrue(id3.compareTo(id2) > 0);

		Map<String,String> map4 = new HashMap<String, String>();
		map4.put("f2", "v2");
		map4.put("f3", "v3");
		EntryID idIn = new EntryID(id3.getTime()+1, 1L);
		EntryID id4 = jedis.xadd("a53453425", idIn, map4);
		assertEquals(idIn, id4);
	}

	@Test
	public void xlen() {
//		assertEquals(0L, jedis.xlen("xlenStream").longValue());
//		
//		
//		Map<String,String> map1 = new HashMap<String, String>();
//		map1.put("f1", "v1");
//		EntryID id1 = jedis.xadd("xlenStream", null, map1);
//		assertEquals((Long)1L, jedis.xlen("xlenStream"));
//		assertNotNull(id1);	
//		
//		Map<String,String> map2 = new HashMap<String, String>();
//		map2.put("f1", "v1");
//		map2.put("f2", "v2");
//		EntryID id2 = jedis.xadd("stream1", null, map2);
//		assertTrue(id2.compareTo(id1) > 0);
//
//		Map<String,String> map3 = new HashMap<String, String>();
//		map3.put("f2", "v2");
//		map3.put("f3", "v3");
//		EntryID id3 = jedis.xadd("stream2", null, map3);
//		assertTrue(id3.compareTo(id2) > 0);

	}

	@Test
	public void xrange() {
		//    prepareGeoData();
		//
		//    List<GeoCoordinate> coordinates = jedis.geopos("foo", "a", "b", "notexist");
		//    assertEquals(3, coordinates.size());
		//    assertTrue(equalsWithinEpsilon(3.0, coordinates.get(0).getLongitude()));
		//    assertTrue(equalsWithinEpsilon(4.0, coordinates.get(0).getLatitude()));
		//    assertTrue(equalsWithinEpsilon(2.0, coordinates.get(1).getLongitude()));
		//    assertTrue(equalsWithinEpsilon(3.0, coordinates.get(1).getLatitude()));
		//    assertNull(coordinates.get(2));
		//
		//    List<GeoCoordinate> bcoordinates = jedis.geopos(bfoo, bA, bB, bNotexist);
		//    assertEquals(3, bcoordinates.size());
		//    assertTrue(equalsWithinEpsilon(3.0, bcoordinates.get(0).getLongitude()));
		//    assertTrue(equalsWithinEpsilon(4.0, bcoordinates.get(0).getLatitude()));
		//    assertTrue(equalsWithinEpsilon(2.0, bcoordinates.get(1).getLongitude()));
		//    assertTrue(equalsWithinEpsilon(3.0, bcoordinates.get(1).getLatitude()));
		//    assertNull(bcoordinates.get(2));
	}

}
