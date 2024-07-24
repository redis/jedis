package redis.clients.jedis.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import redis.clients.jedis.*;
import redis.clients.jedis.csc.ClientSideCache;
import redis.clients.jedis.csc.MapClientSideCache;

public class CSCPooleadBenchmark {

    private static EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0");
    private static final int TOTAL_OPERATIONS = 100000;

    public static void main(String[] args) throws Exception {
        try (Jedis j = new Jedis(endpoint.getHost(), endpoint.getPort())) {
            j.auth(endpoint.getPassword());
            j.flushAll();
            j.disconnect();
        }

        int totalRounds = 10;
        long withoutCache = 0;
        long withCache = 0;
        for (int i = 0; i < totalRounds; i++) {
            withoutCache += runBenchmark(null);
        }
        for (int i = 0; i < totalRounds; i++) {
            withCache += runBenchmark(new MapClientSideCache());
        }
        System.out.println(String.format("after first round withoutCache: %d ms,  withCache: %d ms", withoutCache, withCache));

        for (int i = 0; i < totalRounds; i++) {
            withoutCache += runBenchmark(null);
        }
        for (int i = 0; i < totalRounds; i++) {
            withCache += runBenchmark(new MapClientSideCache());
        }
        System.out.println(String.format("after second round withoutCache: %d ms,  withCache: %d ms", withoutCache, withCache));
        System.out.println("execution time ratio: " + (double) withCache / withoutCache);
    }

    private static long runBenchmark(ClientSideCache cache) throws Exception {
        long start = System.currentTimeMillis();
        withPool(cache);
        long elapsed = System.currentTimeMillis() - start;
        System.out.println(String.format("%s round elapsed: %d ms", cache == null ? "no cache" : "cached", elapsed));
        return elapsed;
    }

    private static void withPool(ClientSideCache cache) throws Exception {
        JedisPooled jedis = null;
        JedisClientConfig config = DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP3).password(endpoint.getPassword()).build();
        jedis = new JedisPooled(endpoint.getHostAndPort(), config, cache);
        final JedisPooled j = jedis;
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
