package redis.clients.jedis.tests;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.tests.commands.JedisCommandTestBase;

public class UriTest extends JedisCommandTestBase {

	private String validUri = "redis://:foobared@localhost:"+Protocol.DEFAULT_PORT;
	
	@Test
    public void validUrl() throws URISyntaxException {
        Jedis jedis = new Jedis(new URI(validUri));
        jedis.set("foo", "0");
        jedis.incr("foo");
        jedis.disconnect();
    }
	
	@Test
    public void withDb() throws URISyntaxException {
        Jedis jedis = new Jedis(new URI("redis://:foobared@localhost:"+Protocol.DEFAULT_PORT + "/0"));
        jedis.dbSize();
        jedis.connect();
        jedis.disconnect();
    }
	
	@Test(expected = JedisException.class)
    public void invalidSchemaShouldFail() throws URISyntaxException {
        new Jedis(new URI("http://localhost"));
    }
	
	@Test(expected = JedisException.class)
    public void withUserShouldFail() throws URISyntaxException {
        new Jedis(new URI("http://bla:foobared@localhost"));
    }
	
	@Test
    public void pool() throws URISyntaxException {
        JedisPool pool = new JedisPool(new JedisPoolConfig(), new URI(validUri));
        Jedis jedis = pool.getResource();
        jedis.set("foo", "0");
        pool.returnResource(jedis);

        jedis = pool.getResource();
        jedis.incr("foo");
        pool.returnResource(jedis);
        pool.destroy();
    }
}