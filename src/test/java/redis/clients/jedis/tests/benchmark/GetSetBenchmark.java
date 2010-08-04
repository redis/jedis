package redis.clients.jedis.tests.benchmark;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Calendar;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisException;

public class GetSetBenchmark {
    private static final int TOTAL_OPERATIONS = 100000;

    public static void main(String[] args) throws UnknownHostException,
	    IOException, JedisException {
	Jedis jedis = new Jedis("localhost");
	jedis.connect();
	jedis.auth("foobared");

	long begin = Calendar.getInstance().getTimeInMillis();

	for (int n = 0; n <= TOTAL_OPERATIONS; n++) {
	    String key = "foo" + n;
	    jedis.set(key, "bar" + n);
	    jedis.get(key);
	}

	long elapsed = Calendar.getInstance().getTimeInMillis() - begin;

	jedis.disconnect();

	System.out.println(((1000 * 2 * TOTAL_OPERATIONS) / elapsed) + " ops");
    }
}
