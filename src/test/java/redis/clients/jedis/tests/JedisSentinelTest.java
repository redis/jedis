package redis.clients.jedis.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.tests.utils.JedisSentinelTestUtil;

public class JedisSentinelTest extends JedisTestBase {
	private static final String MASTER_NAME = "mymaster";

	protected static HostAndPort master = HostAndPortUtil.getRedisServers()
			.get(0);
	protected static HostAndPort slave = HostAndPortUtil.getRedisServers()
			.get(1);
	protected static HostAndPort sentinel = HostAndPortUtil
			.getSentinelServers().get(0);

	protected static Jedis masterJedis;
	protected static Jedis slaveJedis;
	protected static Jedis sentinelJedis;

	@Before
	public void setup() throws InterruptedException {
		masterJedis = new Jedis(master.getHost(), master.getPort());

		slaveJedis = new Jedis(slave.getHost(), slave.getPort());
		slaveJedis.auth("foobared");
		slaveJedis.configSet("masterauth", "foobared");
		slaveJedis.slaveof(master.getHost(), master.getPort());

		List<HostAndPort> slaves = new ArrayList<HostAndPort>();
		slaves.add(slave);

		JedisSentinelTestUtil.waitForSentinelRecognizeRedisReplication(sentinel,
				MASTER_NAME, master, slaves);
		
		// No need to wait for sentinels to recognize each other
	}

	@After
	public void clear() throws InterruptedException {
		Jedis j = new Jedis("localhost", 6380);
		j.auth("foobared");
		j.slaveofNoOne();

		JedisSentinelTestUtil.waitForSentinelRecognizeRedisReplication(sentinel,
				MASTER_NAME, master, new ArrayList<HostAndPort>());
	}

	@Test
	public void sentinel() {
		Jedis j = new Jedis("localhost", 26379);
		List<Map<String, String>> masters = j.sentinelMasters();
		final String masterName = masters.get(0).get("name");

		assertEquals(MASTER_NAME, masterName);

		List<String> masterHostAndPort = j
				.sentinelGetMasterAddrByName(masterName);
		assertEquals("127.0.0.1", masterHostAndPort.get(0));
		assertEquals("6379", masterHostAndPort.get(1));

		List<Map<String, String>> slaves = j.sentinelSlaves(masterName);
		assertTrue(slaves.size() > 0);
		assertEquals("6379", slaves.get(0).get("master-port"));

		List<? extends Object> isMasterDownByAddr = j
				.sentinelIsMasterDownByAddr("127.0.0.1", 6379);
		assertEquals(Long.valueOf(0), (Long) isMasterDownByAddr.get(0));
		assertFalse("?".equals(isMasterDownByAddr.get(1)));

		isMasterDownByAddr = j.sentinelIsMasterDownByAddr("127.0.0.1", 1);
		assertEquals(Long.valueOf(0), (Long) isMasterDownByAddr.get(0));
		assertTrue("?".equals(isMasterDownByAddr.get(1)));

		// DO NOT RE-RUN TEST TOO FAST, RESET TAKES SOME TIME TO... RESET
		assertEquals(Long.valueOf(1), j.sentinelReset(masterName));
		assertEquals(Long.valueOf(0), j.sentinelReset("woof" + masterName));

	}
}
