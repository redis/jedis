package redis.clients.jedis.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import redis.clients.jedis.*;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.TestCache;

public class CSCPooleadBenchmark {

    private static EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0");
    private static final int TOTAL_OPERATIONS = 1000000;
    private static final int NUMBER_OF_THREADS = 50;

    public static void main(String[] args) throws Exception {

        try (Jedis j = new Jedis(endpoint.getHost(), endpoint.getPort())) {
            j.auth(endpoint.getPassword());
            j.flushAll();
            j.disconnect();
        }

        int totalRounds = 50;
        long withoutCache = 0;
        long withCache = 0;

        for (int i = 0; i < totalRounds; i++) {
            withoutCache += runBenchmark(null);
            withCache += runBenchmark(new TestCache());
        }
        for (int i = 0; i < totalRounds; i++) {
        }
        System.out.println(String.format("after %d rounds withoutCache: %d ms,  withCache: %d ms", totalRounds,
                withoutCache, withCache));
        System.out.println("execution time ratio: " + (double) withCache / withoutCache);
    }

    private static long runBenchmark(Cache cache) throws Exception {
        long start = System.currentTimeMillis();
        withPool(cache);
        long elapsed = System.currentTimeMillis() - start;
        System.out.println(String.format("%s round elapsed: %d ms", cache == null ? "no cache" : "cached", elapsed));
        return elapsed;
    }

    private static void withPool(Cache cache) throws Exception {
        JedisClientConfig config = DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP3)
                .password(endpoint.getPassword()).build();
        List<Thread> tds = new ArrayList<>();
        final AtomicInteger ind = new AtomicInteger();
        try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), config, cache)) {
            for (int i = 0; i < NUMBER_OF_THREADS; i++) {
                Thread hj = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; (i = ind.getAndIncrement()) < TOTAL_OPERATIONS;) {
                            try {
                                final String key = "foo" + i;
                                jedis.set(key, key);
                                jedis.get(key);
                            } catch (Exception e) {
                                e.printStackTrace();
                                throw e;
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
        } 
    }
}
