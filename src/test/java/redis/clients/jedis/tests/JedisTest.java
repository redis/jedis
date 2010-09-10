package redis.clients.jedis.tests;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisException;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.tests.commands.JedisCommandTestBase;

public class JedisTest extends JedisCommandTestBase {
    @Test
    public void useWithoutConnecting() {
	Jedis jedis = new Jedis("localhost");
	jedis.dbSize();
    }

    @Test
    public void checkBinaryData() {
	byte[] bigdata = new byte[1777];
	for (int b = 0; b < bigdata.length; b++) {
	    bigdata[b] = (byte) ((byte) b % 255);
	}
	Map<String, String> hash = new HashMap<String, String>();
	hash.put("data", new String(bigdata, Protocol.CHARSET));

	String status = jedis.hmset("foo", hash);
	assertEquals("OK", status);
	assertEquals(hash, jedis.hgetAll("foo"));
    }

}
