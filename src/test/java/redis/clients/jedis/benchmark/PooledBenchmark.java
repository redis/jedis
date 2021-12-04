package redis.clients.jedis.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPooled;

public class PooledBenchmark {

  private static HostAndPort hnp = HostAndPorts.getRedisServers().get(0);
  private static final int TOTAL_OPERATIONS = 100000;

  public static void main(String[] args) throws Exception {
    try (Jedis j = new Jedis(hnp.getHost(), hnp.getPort())) {
      j.auth("foobared");
      j.flushAll();
      j.quit();
    }
    long t = System.currentTimeMillis();
    withPool();
    long elapsed = System.currentTimeMillis() - t;
    System.out.println(((1000 * 2 * TOTAL_OPERATIONS) / elapsed) + " ops");
  }

  private static void withPool() throws Exception {
    final JedisPooled j = new JedisPooled(hnp.getHost(), hnp.getPort(), null, "foobared");
    List<Thread> tds = new ArrayList<>();

    final AtomicInteger ind = new AtomicInteger();
    for (int i = 0; i < 50; i++) {
      Thread hj = new Thread(new Runnable() {
        @Override
        public void run() {
          for (int i = 0; (i = ind.getAndIncrement()) < TOTAL_OPERATIONS;) {
            try {
              final String key = "foo" + i;
              j.set(key, key);
              j.get(key);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      });
      tds.add(hj);
      hj.start();
    }

    for (Thread t : tds) {
      t.join();
    }

    j.close();
  }
}
