package redis.clients.jedis.tests;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.tests.commands.JedisCommandTestBase;
import redis.clients.util.RedisOutputStream;
import redis.clients.util.ShardInfo;

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
	hash.put("data", new String(bigdata, RedisOutputStream.CHARSET));

	String status = jedis.hmset("foo", hash);
	assertEquals("OK", status);
	assertEquals(hash, jedis.hgetAll("foo"));
    }

    @Test
    public void connectWithShardInfo() {
	ShardInfo shardInfo = new ShardInfo("localhost", Protocol.DEFAULT_PORT);
	shardInfo.setPassword("foobared");
	Jedis jedis = new Jedis(shardInfo);
	jedis.get("foo");
    }
}
