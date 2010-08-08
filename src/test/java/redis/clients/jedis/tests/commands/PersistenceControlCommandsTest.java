package redis.clients.jedis.tests.commands;

import org.junit.Test;

import redis.clients.jedis.JedisException;

public class PersistenceControlCommandsTest extends JedisCommandTestBase {
    @Test
    public void save() {
	String status = jedis.save();
	assertEquals("OK", status);
    }

    @Test
    public void bgsave() {
	String status = jedis.bgsave();
	assertEquals("Background saving started", status);
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
}