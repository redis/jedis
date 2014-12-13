package redis.clients.jedis.tests.benchmark;

import java.util.Calendar;

import redis.clients.util.JedisClusterCRC16;

public class CRC16Benchmark {
  private static final int TOTAL_OPERATIONS = 100000000;

  private static String[] TEST_SET = { "", "123456789", "sfger132515",
      "hae9Napahngaikeethievubaibogiech", "AAAAAAAAAAAAAAAAAAAAAA", "Hello, World!" };

  public static void main(String[] args) {
    long begin = Calendar.getInstance().getTimeInMillis();

    for (int n = 0; n <= TOTAL_OPERATIONS; n++) {
      JedisClusterCRC16.getSlot(TEST_SET[n % TEST_SET.length]);
    }

    long elapsed = Calendar.getInstance().getTimeInMillis() - begin;

    System.out.println(((1000 * TOTAL_OPERATIONS) / elapsed) + " ops");
  }

}