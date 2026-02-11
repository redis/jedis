package redis.clients.jedis.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import redis.clients.jedis.*;

public class RedisClientBenchmark {

  private static EndpointConfig endpoint = Endpoints.getRedisEndpoint(Endpoints.STANDALONE0);
  private static final int TOTAL_OPERATIONS = 100000;

  public static void main(String[] args) throws Exception {
    try (Jedis j = new Jedis(endpoint.getHost(), endpoint.getPort())) {
      j.auth(endpoint.getPassword());
      j.flushAll();
      j.disconnect();
    }
    long t = System.currentTimeMillis();
    withPool();
    long elapsed = System.currentTimeMillis() - t;
    System.out.println(((1000 * 2 * TOTAL_OPERATIONS) / elapsed) + " ops");
  }

  private static void withPool() throws Exception {
    final RedisClient j = RedisClient.builder()
        .hostAndPort(endpoint.getHost(), endpoint.getPort())
        .clientConfig(DefaultJedisClientConfig.builder()
            .password(endpoint.getPassword())
            .build())
        .build();
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
