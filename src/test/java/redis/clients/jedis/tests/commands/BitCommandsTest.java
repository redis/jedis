package redis.clients.jedis.tests.commands;

import org.junit.Test;

public class BitCommandsTest extends JedisCommandTestBase {
    @Test
    public void setAndgetbit() {
        boolean bit = jedis.setbit("foo", 0, true);
        assertEquals(false, bit);

        bit = jedis.getbit("foo", 0);
        assertEquals(true, bit);

        boolean bbit = jedis.setbit("bfoo".getBytes(), 0, "1".getBytes());
        assertFalse(bbit);

        bbit = jedis.getbit("bfoo".getBytes(), 0);
        assertTrue(bbit);
    }

    @Test
    public void setAndgetrange() {
        jedis.set("key1", "Hello World");
        long reply = jedis.setrange("key1", 6, "Jedis");
        assertEquals(11, reply);

        assertEquals(jedis.get("key1"), "Hello Jedis");

        assertEquals("Hello", jedis.getrange("key1", 0, 4));
        assertEquals("Jedis", jedis.getrange("key1", 6, 11));
    }
}