package redis.clients.jedis.tests.commands;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class ZaddCommandTest extends JedisCommandTestBase {

	
	/**
	 * Test to prove that with the current Jedis interface is impossible to add two different
	 * members with the same score in a single command to an ordered set. This, however, is possible with Redis commands
	 */
	@Test
	public void zaddAll() {
		Map<Double,String> scoreMembers=new HashMap<Double, String>();
		scoreMembers.put(1D, "member_a");
		scoreMembers.put(1D, "member_b");
        jedis.zadd("ordered-set", scoreMembers);	
        assertEquals(1, jedis.zcard("ordered-set").longValue());
        

	}
	
	/**
	 * Test to prove that with the "fixed" Jedis interface is possible to add two different
	 * members with the same score in a single command to an ordered set.
	 */
	@Test
	public void zaddAllFixed() {
		Map<String,Double> scoreMembers=new HashMap<String,Double >();
		scoreMembers.put("member_a",1D );
		scoreMembers.put("member_b",1D );
        jedis.zaddFixed("ordered-set", scoreMembers);	
        Set<String> members=jedis.zrangeByScore("ordered-set", 0D, 2D);
        //The cardinality of the set must be 2
        assertEquals(2, jedis.zcard("ordered-set").longValue());
        
        Iterator<String> it=members.iterator();
        assertEquals("member_a", it.next());
        assertEquals("member_b", it.next());
        

	}

}