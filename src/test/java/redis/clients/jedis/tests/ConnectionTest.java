package redis.clients.jedis.tests;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class ConnectionTest extends Assert {
    private Connection client;

    @Before
    public void setUp() throws Exception {
	client = new Connection();
    }

    @After
    public void tearDown() throws Exception {
	client.disconnect();
    }

    @Test(expected = JedisConnectionException.class)
    public void checkUnkownHost() {
	client.setHost("someunknownhost");
	client.connect();
    }

    @Test(expected = JedisConnectionException.class)
    public void checkWrongPort() {
	client.setHost("localhost");
	client.setPort(55665);
	client.connect();
    }

    @Test
    public void connectIfNotConnectedWhenSettingTimeoutInfinite() {
	client.setHost("localhost");
	client.setPort(6379);
	client.setTimeoutInfinite();
    }

    @Test
    public void lala() throws InterruptedException {
	final JedisPool jedisPool = new JedisPool("localhost");
	ExecutorService executor = Executors.newFixedThreadPool(10);
	final AtomicBoolean ended = new AtomicBoolean(false);

	for (int n = 0; n < 10; n++) {
	    executor.execute(new Runnable() {
		@Override
		public void run() {
		    while (!ended.get()) {
			Jedis jedis = jedisPool.getResource();
			SortingParams sortingParameters = new SortingParams();
			String sortBy = "1:2:*->status";// assume key is 1:2:
			String filterSetName = "1:2:jobIds";
			sortingParameters.get("a", "b", "c");// assume that
							     // col1,
							     // col2, col3 are
							     // defined
			sortingParameters.by(sortBy);
			List<String> filteredAndsortedList = null;
			try {
			    filteredAndsortedList = jedis.sort(filterSetName,
				    sortingParameters);
			    System.out.println("Sorted List size "
				    + filteredAndsortedList.size());
			    for (String str : filteredAndsortedList) {
				// System.out.println(str);
			    }
			} catch (Exception e) {
			    System.out.println("-----Exception thrown-----");
			    System.out.println(e);
			    System.out.println(" returned value is "
				    + filteredAndsortedList);
			    e.printStackTrace();
			} finally {
			    jedisPool.returnResource(jedis);
			}
		    }
		}
	    });
	}
	Thread.sleep(10000);
	ended.set(true);
	executor.shutdown();
	executor.awaitTermination(10, TimeUnit.SECONDS);
    }
}