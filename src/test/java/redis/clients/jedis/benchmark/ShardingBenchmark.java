package redis.clients.jedis.benchmark;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Calendar;

import redis.clients.jedis.*;

public class ShardingBenchmark {

  private static EndpointConfig endpoint1 = HostAndPorts.getRedisEndpoint("standalone0");
  private static EndpointConfig endpoint2 = HostAndPorts.getRedisEndpoint("standalone1");
  private static final int TOTAL_OPERATIONS = 100000;

  public static void main(String[] args) throws UnknownHostException, IOException {
    try (JedisSharding jedis = new JedisSharding(Arrays.asList(endpoint1.getHostAndPort(), endpoint2.getHostAndPort()),
        endpoint1.getClientConfigBuilder().build())) {

      long begin = Calendar.getInstance().getTimeInMillis();

      for (int n = 0; n <= TOTAL_OPERATIONS; n++) {
        String key = "foo" + n;
        jedis.set(key, "bar" + n);
        jedis.get(key);
      }

      long elapsed = Calendar.getInstance().getTimeInMillis() - begin;

      System.out.println(((1000 * 2 * TOTAL_OPERATIONS) / elapsed) + " ops");
    }
  }
}
