package redis.clients.jedis.tests.benchmark;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Calendar;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.tests.HostAndPortUtil;
import redis.clients.jedis.tests.HostAndPortUtil.HostAndPort;

public class PipelinedGetSetBenchmark {
    private static HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);
    private static final int TOTAL_OPERATIONS = 200000;
    private static final boolean TEST_ASYNC = true;

    public static void main(String[] args) throws UnknownHostException,
            IOException, InterruptedException {
        Jedis jedis = new Jedis(hnp.host, hnp.port);
        jedis.connect();
        jedis.auth("foobared");
        jedis.flushAll();

        long begin = Calendar.getInstance().getTimeInMillis();

        Response<String> lastResponse = null;

        Pipeline p = jedis.pipelined();
        for (int n = 0; n <= TOTAL_OPERATIONS; n++) {
            String key = "foo" + n;
            p.set(key, "bar" + n);
            lastResponse = p.get(key);
        }
        if(TEST_ASYNC){
          p.flushAsync();
          lastResponse.await();
        }else{
            p.sync();
        }

        long elapsed = Calendar.getInstance().getTimeInMillis() - begin;

        jedis.disconnect();

        System.out.println(((1000 * 2 * TOTAL_OPERATIONS) / elapsed) + " ops");
    }
}