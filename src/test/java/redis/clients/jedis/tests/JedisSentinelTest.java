package redis.clients.jedis.tests;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;

public class JedisSentinelTest extends JedisTestBase {
    private static final String MASTER_NAME = "mymaster";

    protected static HostAndPort master = HostAndPortUtil.getRedisServers()
	    .get(0);
    protected static HostAndPort slave = HostAndPortUtil.getRedisServers().get(
	    5);
    protected static HostAndPort sentinel = HostAndPortUtil
	    .getSentinelServers().get(0);

    protected static Jedis masterJedis;
    protected static Jedis slaveJedis;
    protected static Jedis sentinelJedis;

    @Before
    public void setup() throws InterruptedException {

    }

    @After
    public void clear() throws InterruptedException {
	// New Sentinel (after 2.8.1)
	// when slave promoted to master (slave of no one), New Sentinel force
	// to restore it (demote)
	// so, promote(slaveof) slave to master has no effect, not same to old
	// Sentinel's behavior
    }

    @Test
    public void sentinel() {
	Jedis j = new Jedis(sentinel.getHost(), sentinel.getPort());
	List<Map<String, String>> masters = j.sentinelMasters();
	final String masterName = masters.get(0).get("name");

	assertEquals(MASTER_NAME, masterName);

	List<String> masterHostAndPort = j
		.sentinelGetMasterAddrByName(masterName);
	HostAndPort masterFromSentinel = new HostAndPort(
		masterHostAndPort.get(0), Integer.parseInt(masterHostAndPort
			.get(1)));
	assertEquals(master, masterFromSentinel);

	List<Map<String, String>> slaves = j.sentinelSlaves(masterName);
	assertTrue(slaves.size() > 0);
	assertEquals(master.getPort(),
		Integer.parseInt(slaves.get(0).get("master-port")));

	// DO NOT RE-RUN TEST TOO FAST, RESET TAKES SOME TIME TO... RESET
	assertEquals(Long.valueOf(1), j.sentinelReset(masterName));
	assertEquals(Long.valueOf(0), j.sentinelReset("woof" + masterName));
    }
}
