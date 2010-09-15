package redis.clients.jedis.tests.commands;

import java.util.List;

import org.junit.Test;

import redis.clients.jedis.DebugParams;
import redis.clients.jedis.JedisException;
import redis.clients.jedis.JedisMonitor;

public class ControlCommandsTest extends JedisCommandTestBase {
    @Test
    public void save() {
	String status = jedis.save();
	assertEquals("OK", status);
    }

    @Test
    public void bgsave() {
	try {
	    String status = jedis.bgsave();
	    assertEquals("Background saving started", status);
	} catch (JedisException e) {
	    assertEquals("ERR background save already in progress", e
		    .getMessage());
	}
    }

    @Test
    public void bgrewriteaof() {
	String status = jedis.bgrewriteaof();
	assertEquals("Background append only file rewriting started", status);
    }

    @Test
    public void lastsave() throws InterruptedException {
	int before = jedis.lastsave();
	String st = "";
	while (!st.equals("OK")) {
	    try {
		Thread.sleep(1000);
		st = jedis.save();
	    } catch (JedisException e) {

	    }
	}
	int after = jedis.lastsave();
	assertTrue((after - before) > 0);
    }

    @Test
    public void info() {
	String info = jedis.info();
	assertNotNull(info);
    }

    @Test
    public void monitor() {
	jedis.monitor(new JedisMonitor() {
	    public void onCommand(String command) {
		assertTrue(command.contains("OK"));
		client.disconnect();
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
	resp = jedis.debug(DebugParams.SWAPIN("foo"));
	assertNotNull(resp);
	resp = jedis.debug(DebugParams.RELOAD());
	assertNotNull(resp);
    }
}