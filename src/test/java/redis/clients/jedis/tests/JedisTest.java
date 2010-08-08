package redis.clients.jedis.tests;

import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisException;

public class JedisTest {
    @Test(expected = JedisException.class)
    public void useWithoutConnecting() {
	Jedis jedis = new Jedis("localhost");
	jedis.dbSize();
    }
}