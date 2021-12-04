package redis.clients.jedis.benchmark;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Calendar;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisSharding;

public class ShardingBenchmark {

  private static HostAndPort hnp1 = HostAndPorts.getRedisServers().get(0);
  private static HostAndPort hnp2 = HostAndPorts.getRedisServers().get(1);
  private static final int TOTAL_OPERATIONS = 100000;

  public static void main(String[] args) throws UnknownHostException, IOException {
    try (JedisSharding jedis = new JedisSharding(Arrays.asList(hnp1, hnp2),
        DefaultJedisClientConfig.builder().password("foobared").build())) {

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
