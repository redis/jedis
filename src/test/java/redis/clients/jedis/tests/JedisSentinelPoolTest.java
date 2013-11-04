package redis.clients.jedis.tests;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.DebugParams;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.tests.HostAndPortUtil.HostAndPort;

public class JedisSentinelPoolTest extends JedisTestBase {

    protected static HostAndPort master = HostAndPortUtil.getRedisServers()
	    .get(2);
    protected static HostAndPort slave1 = HostAndPortUtil.getRedisServers()
	    .get(3);
    protected static HostAndPort slave2 = HostAndPortUtil.getRedisServers()
    	.get(4);
    protected static HostAndPort sentinel1 = HostAndPortUtil
	    .getSentinelServers().get(1);
    protected static HostAndPort sentinel2 = HostAndPortUtil
	    .getSentinelServers().get(2);

    protected static Jedis masterJedis;
    protected static Jedis slaveJedis1;
    protected static Jedis slaveJedis2;
    
    protected static int slaveCount = 0;

    protected Set<String> sentinels = new HashSet<String>();

    @Before
    public void setUp() throws Exception {

	// set up master and slaves
	masterJedis = new Jedis(master.host, master.port);
	masterJedis.auth("foobared");
	masterJedis.slaveofNoOne();

	slaveJedis1 = new Jedis(slave1.host, slave1.port);
	slaveJedis1.auth("foobared");
	slaveJedis1.slaveof(master.host, master.port);
	slaveCount++;
	
	slaveJedis2 = new Jedis(slave2.host, slave2.port);
	slaveJedis2.auth("foobared");
	slaveJedis2.slaveof(master.host, master.port);
	slaveCount++;

	sentinels.add(sentinel1.toString());
	sentinels.add(sentinel2.toString());

	// FIXME: The following allows the master/slave relationship to
	// be established, and let sentinels know about this relationship. 
	// We can do this more elegantly.
	Thread.sleep(10000);
    }

    @Test
    public void ensureSafeTwiceFailover() throws InterruptedException {
    	JedisSentinelPool pool = new JedisSentinelPool("mymaster", sentinels,
    			new Config(), 1000, "foobared", 2);
    		
    	// perform failover
    	doSegFaultMaster(pool);
    	
    	// perform failover once again
    	doSegFaultMaster(pool);
    	
    	// you can test failover as much as possible
    	// but you need to prepare additional slave per failover
    }
    
    private void doSegFaultMaster(JedisSentinelPool pool) throws InterruptedException {
    	// jedis connection should be master
    	Jedis jedis = pool.getResource();
    	assertEquals("PONG", jedis.ping());

    	try {
    		jedis.debug(DebugParams.SEGFAULT());
    	} catch (Exception e) {
    	}

    	// wait for the sentinel to promote a master
    	// FIXME: we can query the sentinel and sleep
    	// right until the master is promoted
    	Thread.sleep(35000);

    	jedis = pool.getResource();
    	assertEquals("PONG", jedis.ping());
    	assertEquals("foobared", jedis.configGet("requirepass").get(1));
    	assertEquals(2, jedis.getDB().intValue());
    }
}
