package redis.clients.jedis.tests;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.DebugParams;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.tests.utils.JedisSentinelTestUtil;

public class JedisSentinelPoolTest extends JedisTestBase {
	private static final String MASTER_NAME = "mymaster";

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
	protected static Jedis sentinelJedis1;
	protected static Jedis sentinelJedis2;

	protected Set<String> sentinels = new HashSet<String>();

	@Before
	public void setUp() throws Exception {

		// set up master and slaves
		masterJedis = new Jedis(master.getHost(), master.getPort());
		masterJedis.auth("foobared");
		masterJedis.slaveofNoOne();

		slaveJedis1 = new Jedis(slave1.getHost(), slave1.getPort());
		slaveJedis1.auth("foobared");
		slaveJedis1.slaveof(master.getHost(), master.getPort());

		slaveJedis2 = new Jedis(slave2.getHost(), slave2.getPort());
		slaveJedis2.auth("foobared");
		slaveJedis2.slaveof(master.getHost(), master.getPort());

		sentinels.add(sentinel1.toString());
		sentinels.add(sentinel2.toString());

		List<HostAndPort> slaves = new ArrayList<HostAndPort>();
		slaves.add(slave1);
		slaves.add(slave2);

		JedisSentinelTestUtil.waitForSentinelRecognizeRedisReplication(sentinel1, 
				MASTER_NAME, master, slaves);
		JedisSentinelTestUtil.waitForSentinelRecognizeRedisReplication(sentinel2, 
				MASTER_NAME, master, slaves);
		
		// No need to wait for sentinels to recognize each other
	}	

	@Test
	public void ensureSafeTwiceFailover() throws InterruptedException {
		JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels,
				new Config(), 1000, "foobared", 2);

		// perform failover
		doSegFaultMaster(pool);

		// perform failover once again
		doSegFaultMaster(pool);

		// you can test failover as much as possible
		// but you need to prepare additional slave per failover
	}

	private void doSegFaultMaster(JedisSentinelPool pool) throws InterruptedException {
		HostAndPort oldMaster = pool.getCurrentHostMaster();

		// jedis connection should be master
		Jedis jedis = pool.getResource();
		assertEquals("PONG", jedis.ping());

		try {
			jedis.debug(DebugParams.SEGFAULT());
		} catch (Exception e) {
		}

		waitForFailover(pool, oldMaster);

		jedis = pool.getResource();
		assertEquals("PONG", jedis.ping());
		assertEquals("foobared", jedis.configGet("requirepass").get(1));
		assertEquals(2, jedis.getDB().intValue());
	}

	private void waitForFailover(JedisSentinelPool pool, HostAndPort oldMaster) throws InterruptedException {
		HostAndPort newMaster = JedisSentinelTestUtil.waitForNewPromotedMaster(sentinel1, 
				MASTER_NAME, oldMaster);
		JedisSentinelTestUtil.waitForNewPromotedMaster(sentinel2, MASTER_NAME, oldMaster);
		JedisSentinelTestUtil.waitForSentinelsRecognizeEachOthers();
		waitForJedisSentinelPoolRecognizeNewMaster(pool, newMaster);
	}

	private void waitForJedisSentinelPoolRecognizeNewMaster(JedisSentinelPool pool,
			HostAndPort newMaster) throws InterruptedException {

		while (true) {
			String host = pool.getCurrentHostMaster().getHost();
			int port = pool.getCurrentHostMaster().getPort();

			if (host.equals(newMaster.getHost()) && port == newMaster.getPort())
				break;

			System.out.println("JedisSentinelPool's master is not yet changed, sleep...");

			Thread.sleep(1000);
		}
	}

}
