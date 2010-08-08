package redis.clients.jedis.tests.commands;

import org.junit.Test;

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

}