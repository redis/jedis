package redis.clients.jedis.tests.benchmark;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.tests.HostAndPortUtil;
import redis.clients.jedis.tests.HostAndPortUtil.HostAndPort;
import redis.clients.util.FixedResourcePool;

public class PoolBenchmark {
    private static HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);
    private static final int TOTAL_OPERATIONS = 100000;

    public static void main(String[] args) throws UnknownHostException,
            IOException, TimeoutException, InterruptedException {
        Logger logger = Logger.getLogger(FixedResourcePool.class.getName());
        logger.setLevel(Level.OFF);

        Jedis j = new Jedis(hnp.host, hnp.port);
        j.connect();
        j.auth("foobared");
        j.flushAll();
        j.quit();
        j.disconnect();
        long t = System.currentTimeMillis();
        // withoutPool();
        withPool();
        long elapsed = System.currentTimeMillis() - t;
        System.out.println(((1000 * 2 * TOTAL_OPERATIONS) / elapsed) + " ops");
    }

    private static void withPool() throws InterruptedException {
        final JedisPool pool = new JedisPool(hnp.host, hnp.port, 2000,
                "foobared");
        pool.setResourcesNumber(50);
        pool.setDefaultPoolWait(1000000);
        pool.init();
        List<Thread> tds = new ArrayList<Thread>();

        final AtomicInteger ind = new AtomicInteger();
        for (int i = 0; i < 50; i++) {
            Thread hj = new Thread(new Runnable() {
                public void run() {
                    for (int i = 0; (i = ind.getAndIncrement()) < TOTAL_OPERATIONS;) {
                        try {
                            Jedis j = pool.getResource();
                            final String key = "foo" + i;
                            j.set(key, key);
                            j.get(key);
                            pool.returnResource(j);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            tds.add(hj);
            hj.start();
        }

        for (Thread t : tds)
            t.join();

        pool.destroy();
    }
}