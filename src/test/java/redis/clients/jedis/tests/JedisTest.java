package redis.clients.jedis.tests;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.Assert;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.tests.commands.JedisCommandTestBase;
import redis.clients.util.RedisOutputStream;

public class JedisTest extends JedisCommandTestBase {
    @Test
    public void useWithoutConnecting() {
	Jedis jedis = new Jedis("localhost");
	jedis.auth("foobared");
	jedis.dbSize();
    }

    @Test
    public void checkBinaryData() {
	byte[] bigdata = new byte[1777];
	for (int b = 0; b < bigdata.length; b++) {
	    bigdata[b] = (byte) ((byte) b % 255);
	}
	Map<String, String> hash = new HashMap<String, String>();
	hash.put("data", new String(bigdata, Protocol.UTF8));

	String status = jedis.hmset("foo", hash);
	assertEquals("OK", status);
	assertEquals(hash, jedis.hgetAll("foo"));
    }

    @Test
    public void connectWithShardInfo() {
	JedisShardInfo shardInfo = new JedisShardInfo("localhost",
		Protocol.DEFAULT_PORT);
	shardInfo.setPassword("foobared");
	Jedis jedis = new Jedis(shardInfo);
	jedis.get("foo");
    }
    
    @SuppressWarnings("rawtypes")
	public static void compareList(List expected, List result) {
    	final Iterator expectedit = expected.iterator();
    	final Iterator responseit = result.iterator();
    	while(expectedit.hasNext()) {
    		final Object exp = expectedit.next();
    		final Object resp = responseit.next();
    		if(exp instanceof byte[]) {
    			final byte[] bexp = (byte[]) exp;
    			final byte[] bresp = (byte[]) resp;
    			Assert.assertArrayEquals(bexp, bresp);
    		} else if (exp instanceof List) {
    			final List subexp = (List) exp;
    			final List subresp = (List) resp;
    			compareList(subexp, subresp);
    		} else {
    			assertEquals(exp, resp);
    		}
    	}
    }

}
