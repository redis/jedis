package redis.clients.jedis.tests.commands;

import java.util.List;

import org.junit.Test;

import redis.clients.jedis.DebugParams;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisMonitor;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.tests.HostAndPortUtil;
import redis.clients.util.SafeEncoder;

public class ControlCommandsTest extends JedisCommandTestBase {
    @Test
    public void save() {
	try {
	    String status = jedis.save();
	    assertEquals("OK", status);
	} catch (JedisDataException e) {
	    assertTrue("ERR Background save already in progress"
		    .equalsIgnoreCase(e.getMessage()));
	}
    }

    @Test
    public void bgsave() {
	try {
	    String status = jedis.bgsave();
	    assertEquals("Background saving started", status);
	} catch (JedisDataException e) {
	    assertTrue("ERR Background save already in progress"
		    .equalsIgnoreCase(e.getMessage()));
	}
    }

    @Test
    public void bgrewriteaof() {
	String scheduled = "Background append only file rewriting scheduled";
	String started = "Background append only file rewriting started";

	String status = jedis.bgrewriteaof();

	boolean ok = status.equals(scheduled) || status.equals(started);
	assertTrue(ok);
    }

    @Test
    public void lastsave() throws InterruptedException {
	long saved = jedis.lastsave();
	assertTrue(saved > 0);
    }

    @Test
    public void info() {
	String info = jedis.info();
	assertNotNull(info);
	info = jedis.info("server");
	assertNotNull(info);
    }

    @Test
    public void monitor() {
	new Thread(new Runnable() {
	    public void run() {
		try {
		    // sleep 100ms to make sure that monitor thread runs first
		    Thread.sleep(100);
		} catch (InterruptedException e) {
		}
		Jedis j = new Jedis("localhost");
		j.auth("foobared");
		for (int i = 0; i < 5; i++) {
		    j.incr("foobared");
		}
		j.disconnect();
	    }
	}).start();

	jedis.monitor(new JedisMonitor() {
	    private int count = 0;

	    public void onCommand(String command) {
		if (command.contains("INCR")) {
		    count++;
		}
		if (count == 5) {
		    client.disconnect();
		}
	    }
	});
    }

    @Test
    public void configGet() {
	List<String> info = jedis.configGet("m*");
	assertNotNull(info);
    }

    @Test
    public void configSet() {
	List<String> info = jedis.configGet("maxmemory");
	String memory = info.get(1);
	String status = jedis.configSet("maxmemory", "200");
	assertEquals("OK", status);
	jedis.configSet("maxmemory", memory);
    }

    @Test
    public void sync() {
	jedis.sync();
    }

    @Test
    public void debug() {
	jedis.set("foo", "bar");
	String resp = jedis.debug(DebugParams.OBJECT("foo"));
	assertNotNull(resp);
	resp = jedis.debug(DebugParams.RELOAD());
	assertNotNull(resp);
    }

    @Test
    public void waitReplicas() {
	Long replicas = jedis.waitReplicas(1, 100);
	assertEquals(1, replicas.longValue());
    }

    @Test
    public void roleWithMasterAndSlave() {
	// master including slave
	List<Object> role = jedis.role();

	assertNotNull(role);
	assertTrue(role.size() >= 3);

	assertEquals("master", SafeEncoder.encode((byte[]) role.get(0)));
	assertTrue(role.get(1) instanceof Long);

	List<Object> slaves = (List<Object>) role.get(2);
	List<Object> slave = (List<Object>) slaves.get(0);
	assertEquals(3, slave.size());

	// get slave
	String slaveNodeHost = SafeEncoder.encode((byte[]) slave.get(0));
	int slaveNodePort = Integer.parseInt(SafeEncoder.encode((byte[]) slave
		.get(1)));

	// slave
	Jedis slaveJedis = new Jedis(slaveNodeHost, slaveNodePort);
	slaveJedis.auth("foobared");
	List<Object> slaveRole = slaveJedis.role();

	assertNotNull(slaveRole);
	assertTrue(slaveRole.size() == 5);

	assertEquals("slave", SafeEncoder.encode((byte[]) slaveRole.get(0)));

	// 2nd & 3rd argument must point to master node
	HostAndPort masterHnP = new HostAndPort(
		SafeEncoder.encode((byte[]) slaveRole.get(1)),
		((Long) slaveRole.get(2)).intValue());
	HostAndPort jedisHnP = new HostAndPort(jedis.getClient().getHost(),
		jedis.getClient().getPort());
	assertEquals(masterHnP, jedisHnP);
    }

    @Test
    public void roleWithSentinelNode() {
	HostAndPort sentinelHnP = HostAndPortUtil.getSentinelServers().get(0);

	Jedis sentinelJedis = new Jedis(sentinelHnP.getHost(),
		sentinelHnP.getPort());
	List<Object> role = sentinelJedis.role();

	assertNotNull(role);
	assertTrue(role.size() > 1);

	assertEquals("sentinel", SafeEncoder.encode((byte[]) role.get(0)));

	List<Object> master = (List<Object>) role.get(1);
	assertEquals("mymaster", SafeEncoder.encode((byte[]) master.get(0)));
	
	sentinelJedis.close();
    }
}