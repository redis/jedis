package redis.clients.jedis.tests;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPipeline;

public class PipeliningTest extends Assert {
    @Test
    public void pipeline() throws UnknownHostException, IOException {
	Jedis jedis = new Jedis("localhost");
	jedis.connect();
	jedis.auth("foobared");
	jedis.flushAll();

	List<Object> results = jedis.pipelined(new JedisPipeline() {
	    public void execute() {
		client.set("foo", "bar");
		client.get("foo");
	    }
	});

	assertEquals(2, results.size());
	assertEquals("OK", results.get(0));
	assertEquals("bar", results.get(1));
    }
}
