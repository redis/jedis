package redis.clients.jedis.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.*;

public class PoolBenchmark {

  private static EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0");
  private static final int TOTAL_OPERATIONS = 100000;

  public static void main(String[] args) throws Exception {
    Jedis j = new Jedis(endpoint.getHostAndPort());
    j.connect();
    j.auth(endpoint.getPassword());
    j.flushAll();
    j.disconnect();
    long t = System.currentTimeMillis();
    // withoutPool();
    withPool();
    long elapsed = System.currentTimeMillis() - t;
    System.out.println(((1000 * 2 * TOTAL_OPERATIONS) / elapsed) + " ops");
  }

  private static void withPool() throws Exception {
    final JedisPool pool = new JedisPool(new GenericObjectPoolConfig<Jedis>(), endpoint.getHost(),
        endpoint.getPort(), 2000, endpoint.getPassword());
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
              j.close();
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

    pool.destroy();
  }
}
