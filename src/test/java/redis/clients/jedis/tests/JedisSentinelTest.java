package redis.clients.jedis.tests;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;

public class JedisSentinelTest {
    private static final String MASTER_NAME = "mymaster";

    @Before
    public void setup() throws InterruptedException {
	Jedis j = new Jedis("localhost", 6380);
	j.auth("foobared");
	j.configSet("masterauth", "foobared");
	j.slaveof("localhost", 6379);
	// TODO: The sleep is to give time to the slave to synchronize with the
	// master and also let know the sentinels about this new topology. We
	// should find a better way to do this.
	Thread.sleep(10000);
    }

    @After
    public void clear() {
	Jedis j = new Jedis("localhost", 6380);
	j.auth("foobared");
	j.slaveofNoOne();
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
