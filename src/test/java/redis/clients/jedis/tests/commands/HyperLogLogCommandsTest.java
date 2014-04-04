package redis.clients.jedis.tests.commands;

import org.junit.Test;

public class HyperLogLogCommandsTest extends JedisCommandTestBase {


    @Test
    public void pfadd() {
	long status = jedis.pfadd("foo", "a");
	assertEquals(1, status);
	
	status = jedis.pfadd("foo", "a");
	assertEquals(0, status);
    }
    
    @Test
    public void pfcount() {
	long status = jedis.pfadd("hll", "foo", "bar", "zap");
	assertEquals(1, status);
	
	status = jedis.pfadd("hll", "zap", "zap", "zap");
	assertEquals(0, status);
	
	status = jedis.pfadd("hll", "foo", "bar");
	assertEquals(0, status);
	
	status = jedis.pfcount("hll");
	assertEquals(3, status);
    }
    
    @Test
    public void pfmerge() {
	long status = jedis.pfadd("hll1", "foo", "bar", "zap", "a");
	assertEquals(1, status);
	
	status = jedis.pfadd("hll2", "a", "b", "c", "foo");
	assertEquals(1, status);
	
	String mergeStatus = jedis.pfmerge("hll3", "hll1", "hll2");
	assertEquals("OK", mergeStatus);
	
	status = jedis.pfcount("hll3");
	assertEquals(6, status);
    }
}