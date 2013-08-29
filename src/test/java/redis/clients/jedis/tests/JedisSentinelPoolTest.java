package redis.clients.jedis.tests;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.DebugParams;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.tests.HostAndPortUtil.HostAndPort;

public class JedisSentinelPoolTest extends JedisTestBase {
	
	protected static HostAndPort master = HostAndPortUtil.getRedisServers().get(2);
	protected static HostAndPort slave1 = HostAndPortUtil.getRedisServers().get(3);	
	protected static HostAndPort sentinel1 = HostAndPortUtil.getSentinelServers().get(1);
	protected static HostAndPort sentinel2 = HostAndPortUtil.getSentinelServers().get(2);

	protected static Jedis masterJedis;
	protected static Jedis slaveJedis1;	
	protected static Jedis sentinelJedis1;
	
	protected Set<String> sentinels = new HashSet<String>();
	
	@Before
    public void setUp() throws Exception {    
						
		// set up master and slaves
		masterJedis = new Jedis(master.host, master.port);
		masterJedis.slaveofNoOne();
		
		slaveJedis1 = new Jedis(slave1.host, slave1.port);
		slaveJedis1.slaveof(master.host, master.port);
		
		sentinelJedis1 = new Jedis(sentinel1.host, sentinel1.port);
		sentinels.add(sentinel1.toString());
		sentinels.add(sentinel2.toString());	
		
		// FIXME: The following allows the master/slave relationship to
		// be established. We can do this more elegantly.		
		Thread.sleep(10000);					   
    }
        
    @Test
    public void segfaultMaster() throws InterruptedException {
    	
    	JedisSentinelPool pool = new JedisSentinelPool("mymaster", sentinels);
    	
    	Jedis jedis = pool.getResource();
    	assertEquals("PONG", jedis.ping());
    	
    	try { masterJedis.debug(DebugParams.SEGFAULT()); } catch (Exception e) {}
    	
    	// wait for the sentinel to promote a master
    	// FIXME: we can query the sentinel and sleep
    	// right until the master is promoted
    	Thread.sleep(35000);
    	
    	jedis = pool.getResource();
    	assertEquals("PONG", jedis.ping());
    }
}
