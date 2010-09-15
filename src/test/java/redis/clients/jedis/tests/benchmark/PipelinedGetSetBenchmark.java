package redis.clients.jedis.tests.benchmark;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Calendar;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPipeline;
import redis.clients.jedis.tests.HostAndPortUtil;
import redis.clients.jedis.tests.HostAndPortUtil.HostAndPort;

public class PipelinedGetSetBenchmark {
	private static HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);
    private static final int TOTAL_OPERATIONS = 200000;

    public static void main(String[] args) throws UnknownHostException,
	    IOException {
	Jedis jedis = new Jedis(hnp.host, hnp.port);
	jedis.connect();
	jedis.auth("foobared");
	jedis.flushAll();

	long begin = Calendar.getInstance().getTimeInMillis();

	jedis.pipelined(new JedisPipeline() {
	    public void execute() {
		for (int n = 0; n <= TOTAL_OPERATIONS; n++) {
		    String key = "foo" + n;
		    client.set(key, "bar" + n);
		    client.get(key);
		}
	    }
	});

	long elapsed = Calendar.getInstance().getTimeInMillis() - begin;

	jedis.disconnect();

	System.out.println(((1000 * 2 * TOTAL_OPERATIONS) / elapsed) + " ops");
    }
}