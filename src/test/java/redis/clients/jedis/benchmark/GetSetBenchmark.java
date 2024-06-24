package redis.clients.jedis.benchmark;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Calendar;

import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.HostAndPorts;

public class GetSetBenchmark {

  private static EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0");
  private static final int TOTAL_OPERATIONS = 100000;

  public static void main(String[] args) throws UnknownHostException, IOException {
    Jedis jedis = new Jedis(endpoint.getHostAndPort());
    jedis.connect();
    jedis.auth(endpoint.getPassword());
    jedis.flushAll();

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
